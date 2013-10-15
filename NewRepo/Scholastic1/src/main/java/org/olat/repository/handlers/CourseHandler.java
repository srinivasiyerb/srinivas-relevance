/**
 * OLAT - Online Learning and Training<br>
 * http://www.olat.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.repository.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.control.generic.wizard.Step;
import org.olat.core.gui.control.generic.wizard.StepRunnerCallback;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.gui.media.CleanupAfterDeliveryFileMediaResource;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.i18n.I18nModule;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.repository.CreateNewCourseController;
import org.olat.course.repository.ImportCourseController;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.controllers.IAddController;
import org.olat.repository.controllers.RepositoryAddCallback;
import org.olat.repository.controllers.WizardCloseCourseController;
import org.olat.repository.controllers.WizardCloseResourceController;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.resource.references.ReferenceManager;
import org.olat.user.UserManager;

import de.tuchemnitz.wizard.workflows.coursecreation.CourseCreationHelper;
import de.tuchemnitz.wizard.workflows.coursecreation.CourseCreationMailHelper;
import de.tuchemnitz.wizard.workflows.coursecreation.model.CourseCreationConfiguration;
import de.tuchemnitz.wizard.workflows.coursecreation.steps.CcStep00;

/**
 * Initial Date: Apr 15, 2004
 * 
 * @author Comment: Mike Stock
 */
public class CourseHandler implements RepositoryHandler {

	/**
	 * Command to add (i.e. import) a course.
	 */
	public static final String PROCESS_IMPORT = "add";
	/**
	 * Command to create a new course.
	 */
	public static final String PROCESS_CREATENEW = "new";

	private static final String PACKAGE = Util.getPackageName(RepositoryManager.class);
	private static final boolean LAUNCHEABLE = true;
	private static final boolean DOWNLOADEABLE = true;
	private static final boolean EDITABLE = true;
	private static final boolean WIZARD_SUPPORT = true;
	private static final List supportedTypes;

	static { // initialize supported types
		supportedTypes = new ArrayList(1);
		supportedTypes.add(CourseModule.getCourseTypeName());
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getSupportedTypes()
	 */
	@Override
	public List getSupportedTypes() {
		return supportedTypes;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsDownload()
	 */
	@Override
	public boolean supportsDownload(final RepositoryEntry repoEntry) {
		return DOWNLOADEABLE;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsLaunch()
	 */
	@Override
	public boolean supportsLaunch(final RepositoryEntry repoEntry) {
		return LAUNCHEABLE;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsEdit()
	 */
	@Override
	public boolean supportsEdit(final RepositoryEntry repoEntry) {
		return EDITABLE;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsWizard(org.olat.repository.RepositoryEntry)
	 */
	@Override
	public boolean supportsWizard(final RepositoryEntry repoEntry) {
		return WIZARD_SUPPORT;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getLaunchController(org.olat.core.id.OLATResourceable java.lang.String, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public MainLayoutController createLaunchController(final OLATResourceable res, final String initialViewIdentifier, final UserRequest ureq,
			final WindowControl wControl) {
		return CourseFactory.createLaunchController(ureq, wControl, res, initialViewIdentifier);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getAsMediaResource(org.olat.core.id.OLATResourceable
	 */
	@Override
	public MediaResource getAsMediaResource(final OLATResourceable res) {
		final RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(res, true);
		String exportFileName = re.getDisplayname() + ".zip";
		exportFileName = StringHelper.transformDisplayNameToFileSystemName(exportFileName);
		final File fExportZIP = new File(System.getProperty("java.io.tmpdir") + File.separator + exportFileName);
		CourseFactory.exportCourseToZIP(res, fExportZIP);
		return new CleanupAfterDeliveryFileMediaResource(fExportZIP);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getEditorController(org.olat.core.id.OLATResourceable org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createEditorController(final OLATResourceable res, final UserRequest ureq, final WindowControl wControl) {
		// throw new AssertException("a course is not directly editable!!! (reason: lock is never released), res-id:"+res.getResourceableId());
		return CourseFactory.createEditorController(ureq, wControl, res);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getAddController(org.olat.repository.controllers.RepositoryAddCallback, java.lang.Object,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public IAddController createAddController(final RepositoryAddCallback callback, final Object userObject, final UserRequest ureq, final WindowControl wControl) {
		if (userObject == null || userObject.equals(PROCESS_CREATENEW)) {
			return new CreateNewCourseController(callback, ureq, wControl);
		} else if (userObject.equals(PROCESS_IMPORT)) {
			return new ImportCourseController(callback, ureq, wControl);
		} else {
			throw new AssertException("Command " + userObject + " not supported by CourseHandler.");
		}
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getCreateWizardController(org.olat.core.id.OLATResourceable, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createWizardController(final OLATResourceable res, final UserRequest ureq, final WindowControl wControl) {
		// load the course structure
		final RepositoryEntry repoEntry = (RepositoryEntry) res;
		final ICourse course = CourseFactory.loadCourse(repoEntry.getOlatResource());
		final Translator cceTranslator = Util.createPackageTranslator(CourseCreationHelper.class, ureq.getLocale());
		final CourseCreationConfiguration courseConfig = new CourseCreationConfiguration(course.getCourseTitle(), Settings.getServerContextPathURI()
				+ "/url/RepositoryEntry/" + repoEntry.getKey());
		// wizard finish callback called after "finish" is called
		final CourseCreationHelper ccHelper = new CourseCreationHelper(ureq.getLocale(), repoEntry, courseConfig, course);
		final StepRunnerCallback finishCallback = new StepRunnerCallback() {
			@Override
			public Step execute(final UserRequest ureq, final WindowControl control, final StepsRunContext runContext) {
				// here goes the code which reads out the wizards data from the runContext and then does some wizardry
				ccHelper.finalizeWorkflow(ureq);
				control.setInfo(CourseCreationMailHelper.getSuccessMessageString(ureq));
				// send notification mail
				final MailerResult mr = CourseCreationMailHelper.sentNotificationMail(ureq, ccHelper.getConfiguration());
				MailHelper.printErrorsAndWarnings(mr, control, ureq.getLocale());
				return StepsMainRunController.DONE_MODIFIED;
			}
		};
		final Step start = new CcStep00(ureq, courseConfig, repoEntry);
		final StepsMainRunController ccSMRC = new StepsMainRunController(ureq, wControl, start, finishCallback, null, cceTranslator.translate("coursecreation.title"));
		return ccSMRC;
	}

	@Override
	public Controller createDetailsForm(final UserRequest ureq, final WindowControl wControl, final OLATResourceable res) {
		return CourseFactory.getDetailsForm(ureq, wControl, res);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#cleanupOnDelete(org.olat.core.id.OLATResourceable org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public boolean cleanupOnDelete(final OLATResourceable res) {
		// notify all current users of this resource (course) that it will be deleted now.
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new OLATResourceableJustBeforeDeletedEvent(res), res);
		// archiving is done within readyToDelete
		CourseFactory.deleteCourse(res);
		// delete resourceable
		final OLATResourceManager rm = OLATResourceManager.getInstance();
		final OLATResource ores = rm.findResourceable(res);
		rm.deleteOLATResource(ores);
		return true;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#readyToDelete(org.olat.core.id.OLATResourceable org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public boolean readyToDelete(final OLATResourceable res, final UserRequest ureq, final WindowControl wControl) {
		final ReferenceManager refM = ReferenceManager.getInstance();
		final String referencesSummary = refM.getReferencesToSummary(res, ureq.getLocale());
		if (referencesSummary != null) {
			final Translator translator = new PackageTranslator(PACKAGE, ureq.getLocale());
			wControl.setError(translator.translate("details.delete.error.references", new String[] { referencesSummary }));
			return false;
		}
		/*
		 * make an archive of the course nodes with valuable data
		 */
		final UserManager um = UserManager.getInstance();
		final String charset = um.getUserCharset(ureq.getIdentity());
		CourseFactory.archiveCourse(res, charset, ureq.getLocale(), ureq.getIdentity());
		/*
		 * 
		 */
		return true;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#createCopy(org.olat.core.id.OLATResourceable org.olat.core.gui.UserRequest)
	 */
	@Override
	public OLATResourceable createCopy(final OLATResourceable res, final UserRequest ureq) {
		return CourseFactory.copyCourse(res, ureq);
	}

	/**
	 * Archive the hole course with runtime-data and course-structure-data.
	 * 
	 * @see org.olat.repository.handlers.RepositoryHandler#archive(java.lang.String, org.olat.repository.RepositoryEntry)
	 */
	@Override
	public String archive(final Identity archiveOnBehalfOf, final String archivFilePath, final RepositoryEntry entry) {
		final ICourse course = CourseFactory.loadCourse(entry.getOlatResource());
		// Archive course runtime data (like delete course, archive e.g. logfiles, node-data)
		final File tmpExportDir = new File(FolderConfig.getCanonicalTmpDir() + "/" + CodeHelper.getRAMUniqueID());
		tmpExportDir.mkdirs();
		CourseFactory.archiveCourse(archiveOnBehalfOf, course, WebappHelper.getDefaultCharset(), I18nModule.getDefaultLocale(), tmpExportDir, true);
		// Archive course run structure (like course export)
		final String courseExportFileName = "course_export.zip";
		final File courseExportZIP = new File(tmpExportDir, courseExportFileName);
		CourseFactory.exportCourseToZIP(entry.getOlatResource(), courseExportZIP);
		// Zip runtime data and course run structure data into one zip-file
		final String completeArchiveFileName = "del_course_" + entry.getOlatResource().getResourceableId() + ".zip";
		final String completeArchivePath = archivFilePath + File.separator + completeArchiveFileName;
		ZipUtil.zipAll(tmpExportDir, new File(completeArchivePath), false);
		FileUtils.deleteDirsAndFiles(tmpExportDir, true, true);
		return completeArchiveFileName;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#acquireLock(org.olat.core.id.OLATResourceable, org.olat.core.id.Identity)
	 */
	@Override
	public LockResult acquireLock(final OLATResourceable ores, final Identity identity) {
		return CoordinatorManager.getInstance().getCoordinator().getLocker().acquireLock(ores, identity, CourseFactory.COURSE_EDITOR_LOCK);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#releaseLock(org.olat.core.util.coordinate.LockResult)
	 */
	@Override
	public void releaseLock(final LockResult lockResult) {
		if (lockResult != null) {
			CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lockResult);
		}
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#isLocked(org.olat.core.id.OLATResourceable)
	 */
	@Override
	public boolean isLocked(final OLATResourceable ores) {
		return CoordinatorManager.getInstance().getCoordinator().getLocker().isLocked(ores, CourseFactory.COURSE_EDITOR_LOCK);
	}

	@Override
	public WizardCloseResourceController createCloseResourceController(final UserRequest ureq, final WindowControl wControl, final RepositoryEntry repositoryEntry) {
		return new WizardCloseCourseController(ureq, wControl, repositoryEntry);
	}

}
