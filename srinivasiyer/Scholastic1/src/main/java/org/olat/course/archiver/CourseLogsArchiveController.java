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

package org.olat.course.archiver;

import java.io.File;
import java.util.Date;
import java.util.Locale;

import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.htmlheader.jscss.JSAndCSSComponent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Util;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.statistic.AsyncExportManager;
import org.olat.home.HomeMainController;
import org.olat.repository.RepositoryManager;
import org.olat.user.UserManager;

/**
 * Description: Archives the user chosen courselogfiles Initial Date: Dec 6, 2004
 * 
 * @author Alex
 */
public class CourseLogsArchiveController extends BasicController {

	private final Panel myPanel;
	private final VelocityContainer myContent;

	private LogFileChooserForm logFileChooserForm;
	private Link showFileButton;
	private final OLATResourceable ores;

	private CloseableModalController cmc;

	/**
	 * Constructor for the course logs archive controller
	 * 
	 * @param ureq
	 * @param wControl
	 * @param course
	 */
	public CourseLogsArchiveController(final UserRequest ureq, final WindowControl wControl, final OLATResourceable ores) {
		super(ureq, wControl);
		this.ores = ores;
		this.myPanel = new Panel("myPanel");
		myPanel.addListener(this);

		myContent = createVelocityContainer("start_courselogs");

		final boolean isOLATAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		final boolean isOresOwner = RepositoryManager.getInstance().isOwnerOfRepositoryEntry(ureq.getIdentity(),
				RepositoryManager.getInstance().lookupRepositoryEntry(ores, false));
		final boolean isOresInstitutionalManager = RepositoryManager.getInstance().isInstitutionalRessourceManagerFor(
				RepositoryManager.getInstance().lookupRepositoryEntry(ores, false), ureq.getIdentity());
		final boolean aLogV = isOresOwner || isOresInstitutionalManager;
		final boolean uLogV = isOLATAdmin;
		final boolean sLogV = isOresOwner || isOresInstitutionalManager;

		if (AsyncExportManager.getInstance().asyncArchiveCourseLogOngoingFor(ureq.getIdentity())) {
			// then show the ongoing feedback
			final ICourse course = CourseFactory.loadCourse(ores);
			final String courseTitle = course.getCourseTitle();
			showExportOngoing(courseTitle);
		} else if (isOLATAdmin || aLogV || uLogV || sLogV) {
			myContent.contextPut("hasLogArchiveAccess", true);
			logFileChooserForm = new LogFileChooserForm(ureq, wControl, isOLATAdmin, aLogV, uLogV, sLogV);
			listenTo(logFileChooserForm);
			myContent.put("logfilechooserform", logFileChooserForm.getInitialComponent());
			final ICourse course = CourseFactory.loadCourse(ores);
			myContent.contextPut("body", translate("course.logs.existingarchiveintro", course.getCourseTitle()));
			showFileButton = LinkFactory.createButton("showfile", myContent, this);
			final File exportDir = CourseFactory.getDataExportDirectory(ureq.getIdentity(), course.getCourseTitle());
			boolean exportDirExists = false;
			if (exportDir != null && exportDir.exists() && exportDir.isDirectory()) {
				exportDirExists = true;
			}
			myContent.contextPut("hascourselogarchive", new Boolean(exportDirExists));
			myPanel.setContent(myContent);
		} else {
			myContent.contextPut("hasLogArchiveAccess", new Boolean(false));
			myPanel.setContent(myContent);
		}

		putInitialPanel(myPanel);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == showFileButton) {
			final ICourse course = CourseFactory.loadCourse(ores);
			final String personalFolderDir = CourseFactory.getPersonalDirectory(ureq.getIdentity()).getPath();
			String targetDir = CourseFactory.getOrCreateDataExportDirectory(ureq.getIdentity(), course.getCourseTitle()).getPath();

			String relPath = "";

			if (targetDir.startsWith(personalFolderDir)) {
				// that should always be the case

				relPath = targetDir.substring(personalFolderDir.length()).replace("\\", "/");
				targetDir = targetDir.substring(0, personalFolderDir.length());
			}

			final VFSContainer targetFolder = new LocalFolderImpl(new File(targetDir));
			final FolderRunController bcrun = new FolderRunController(targetFolder, true, ureq, getWindowControl());
			final Component folderComponent = bcrun.getInitialComponent();
			if (relPath.length() != 0) {
				if (!relPath.endsWith("/")) {
					relPath = relPath + "/";
				}
				bcrun.activate(ureq, relPath);
			}

			final String personalFolder = Util.createPackageTranslator(HomeMainController.class, ureq.getLocale(), null).translate("menu.bc");

			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), folderComponent, true, personalFolder);
			listenTo(cmc);

			cmc.activate();
		}
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == logFileChooserForm) {
			if (event == Event.DONE_EVENT) {
				final boolean logAdminChecked = logFileChooserForm.logAdminChecked();
				final boolean logUserChecked = logFileChooserForm.logUserChecked();
				final boolean logStatisticChecked = logFileChooserForm.logStatChecked();

				final Date begin = logFileChooserForm.getBeginDate();
				final Date end = logFileChooserForm.getEndDate();

				if (end != null) {
					// shift time from beginning to end of day
					end.setTime(end.getTime() + 24 * 60 * 60 * 1000);
				}

				final UserManager um = UserManager.getInstance();
				final String charset = um.getUserCharset(ureq.getIdentity());

				final ICourse course = CourseFactory.loadCourse(ores);
				final String courseTitle = course.getCourseTitle();
				final String targetDir = CourseFactory.getOrCreateDataExportDirectory(ureq.getIdentity(), courseTitle).getPath();

				final Long resId = ores.getResourceableId();
				final Locale theLocale = ureq.getLocale();
				final String email = ureq.getIdentity().getUser().getProperty(UserConstants.EMAIL, ureq.getLocale());

				AsyncExportManager.getInstance().asyncArchiveCourseLogFiles(ureq.getIdentity(), new Runnable() {

					@Override
					public void run() {
						showExportFinished();
					}

				}, resId, targetDir, begin, end, logAdminChecked, logUserChecked, logStatisticChecked, charset, theLocale, email);

				showExportOngoing(courseTitle);
			} else if (event == Event.DONE_EVENT) {
				myPanel.setContent(myContent);
			}
		}
	}

	private void showExportOngoing(final String courseTitle) {
		final VelocityContainer vcOngoing = createVelocityContainer("courselogs_ongoing");
		vcOngoing.contextPut("body", translate("course.logs.ongoing", courseTitle));
		myPanel.setContent(vcOngoing);

		// initialize polling
		myPanel.put("updatecontrol", new JSAndCSSComponent("intervall", this.getClass(), null, null, false, null, 3000));
	}

	protected void showExportFinished() {
		final ICourse course = CourseFactory.loadCourse(ores);
		final VelocityContainer vcFeedback = createVelocityContainer("courselogs_feedback");
		showFileButton = LinkFactory.createButton("showfile", vcFeedback, this);
		vcFeedback.contextPut("body", translate("course.logs.feedback", course.getCourseTitle()));
		myPanel.setContent(vcFeedback);

		// note: polling can't be switched off unfortunatelly
		// this is due to the fact that the jsandcsscomponent can only modify
		// certain parts of the page and it would require a full page refresh
		// to get rid of the poller - and that's not possible currently

		showInfo("course.logs.finished", course.getCourseTitle());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// has nothing to dispose so far
	}

}
