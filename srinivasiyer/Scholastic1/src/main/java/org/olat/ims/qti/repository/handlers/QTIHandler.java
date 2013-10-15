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

package org.olat.ims.qti.repository.handlers;

import java.io.File;

import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Util;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.FileResource;
import org.olat.ims.qti.fileresource.SurveyFileResource;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.ims.qti.process.ImsRepositoryResolver;
import org.olat.ims.qti.process.Resolver;
import org.olat.modules.iq.IQManager;
import org.olat.modules.iq.IQPreviewSecurityCallback;
import org.olat.modules.iq.IQSecurityCallback;
import org.olat.repository.RepositoryManager;
import org.olat.repository.handlers.FileHandler;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.resource.references.ReferenceManager;

/**
 * Initial Date: Apr 6, 2004
 * 
 * @author Mike Stock Comment:
 */
public abstract class QTIHandler extends FileHandler implements RepositoryHandler {

	private static final String PACKAGE = Util.getPackageName(RepositoryManager.class);

	/**
	 * Default constructor.
	 */
	QTIHandler() {
		// Implemented by specific sub-class
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getLaunchController(org.olat.core.id.OLATResourceable java.lang.String, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public MainLayoutController createLaunchController(final OLATResourceable res, final String initialViewIdentifier, final UserRequest ureq,
			final WindowControl wControl) {
		final Resolver resolver = new ImsRepositoryResolver(res);
		final IQSecurityCallback secCallback = new IQPreviewSecurityCallback();
		final MainLayoutController runController = res.getResourceableTypeName().equals(SurveyFileResource.TYPE_NAME) ? IQManager.getInstance()
				.createIQDisplayController(res, resolver, AssessmentInstance.QMD_ENTRY_TYPE_SURVEY, secCallback, ureq, wControl) : IQManager.getInstance()
				.createIQDisplayController(res, resolver, AssessmentInstance.QMD_ENTRY_TYPE_SELF, secCallback, ureq, wControl);
		// use on column layout
		final LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(ureq, wControl, null, null, runController.getInitialComponent(), null);
		layoutCtr.addDisposableChildController(runController); // dispose content on layout dispose
		return layoutCtr;
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
		if (CoordinatorManager.getInstance().getCoordinator().getLocker().isLocked(res, null)) {
			final Translator translator = new PackageTranslator(PACKAGE, ureq.getLocale());
			wControl.setError(translator.translate("details.delete.error.editor"));
			return false;
		}
		return true;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#acquireLock(org.olat.core.id.OLATResourceable, org.olat.core.id.Identity)
	 */
	@Override
	public LockResult acquireLock(final OLATResourceable ores, final Identity identity) {
		// nothing to do
		return null;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#releaseLock(org.olat.core.util.coordinate.LockResult)
	 */
	@Override
	public void releaseLock(final LockResult lockResult) {
		// nothing to do since nothing locked
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#isLocked(org.olat.core.id.OLATResourceable)
	 */
	@Override
	public boolean isLocked(final OLATResourceable ores) {
		return false;
	}

	/**
	 * @see org.olat.repository.handlers.FileHandler#createCopy(org.olat.core.id.OLATResourceable, org.olat.core.gui.UserRequest)
	 */
	@Override
	public OLATResourceable createCopy(final OLATResourceable res, final UserRequest ureq) {
		final OLATResourceable oLATResourceable = super.createCopy(res, ureq);

		final File sourceFile = FileResourceManager.getInstance().getFileResource(oLATResourceable);
		// unzip sourceFile in a temp dir, delete changelog if any, zip back, delete temp dir
		final FileResource tempFr = new FileResource();
		// move file to its new place
		final File fResourceFileroot = FileResourceManager.getInstance().getFileResourceRoot(tempFr);
		if (!FileUtils.copyFileToDir(sourceFile, fResourceFileroot, "create qti copy")) { return null; }
		final File fUnzippedDir = FileResourceManager.getInstance().unzipFileResource(tempFr);
		final File changeLogDir = new File(fUnzippedDir, "changelog");
		if (changeLogDir.exists()) {
			final boolean changeLogDeleted = FileUtils.deleteDirsAndFiles(changeLogDir, true, true);
		}
		final File targetZipFile = sourceFile;
		FileUtils.deleteDirsAndFiles(targetZipFile.getParentFile(), true, false);
		ZipUtil.zipAll(fUnzippedDir, targetZipFile, false);
		FileResourceManager.getInstance().deleteFileResource(tempFr);

		return oLATResourceable;
	}

}
