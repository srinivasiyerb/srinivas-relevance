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

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.course.nodes.cp.CPRunController;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.ImsCPFileResource;
import org.olat.ims.cp.ui.CPEditMainController;
import org.olat.ims.cp.ui.CreateNewCPController;
import org.olat.modules.cp.CPUIFactory;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.controllers.IAddController;
import org.olat.repository.controllers.RepositoryAddCallback;
import org.olat.repository.controllers.WizardCloseResourceController;

/**
 * Initial Date: Apr 6, 2004
 * 
 * @author Mike Stock Comment:
 */
public class ImsCPHandler extends FileHandler implements RepositoryHandler {

	public static final String PROCESS_CREATENEW = "new";
	public static final String PROCESS_IMPORT = "add";

	private static final boolean LAUNCHEABLE = true;
	private static final boolean DOWNLOADEABLE = true;
	private static final boolean EDITABLE = true;
	private static final boolean WIZARD_SUPPORT = false;
	private static final List supportedTypes;

	/**
	 * 
	 */
	public ImsCPHandler() {
		//
	}

	static { // initialize supported types
		supportedTypes = new ArrayList(1);
		supportedTypes.add(ImsCPFileResource.TYPE_NAME);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getSupportedTypes()
	 */
	@Override
	public List getSupportedTypes() {
		return supportedTypes;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsLaunch()
	 */
	@Override
	public boolean supportsLaunch(final RepositoryEntry repoEntry) {
		return LAUNCHEABLE;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsDownload()
	 */
	@Override
	public boolean supportsDownload(final RepositoryEntry repoEntry) {
		return DOWNLOADEABLE;
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
	 * @see org.olat.repository.handlers.RepositoryHandler#getCreateWizardController(org.olat.core.id.OLATResourceable, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createWizardController(final OLATResourceable res, final UserRequest ureq, final WindowControl wControl) {
		throw new AssertException("Trying to get wizard where no creation wizard is provided for this type.");
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getLaunchController(org.olat.core.id.OLATResourceable java.lang.String, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public MainLayoutController createLaunchController(final OLATResourceable res, final String initialViewIdentifier, final UserRequest ureq,
			final WindowControl wControl) {
		final File cpRoot = FileResourceManager.getInstance().unzipFileResource(res);
		final LocalFolderImpl vfsWrapper = new LocalFolderImpl(cpRoot);
		final Controller realController = null;

		// jump to either the forum or the folder if the business-launch-path says so.
		final BusinessControl bc = wControl.getBusinessControl();
		final ContextEntry ce = bc.popLauncherContextEntry();
		if (ce != null) { // a context path is left for me
			Tracing.logDebug("businesscontrol (for further jumps) would be:" + bc, CPRunController.class);
			final OLATResourceable ores = ce.getOLATResourceable();
			Tracing.logDebug("OLATResourceable=" + ores, CPRunController.class);
			final String typeName = ores.getResourceableTypeName();
			// typeName format: 'path=/test1/test2/readme.txt'
			// First remove prefix 'path='
			final String path = typeName.substring("path=".length());
			if (path.length() > 0) {
				Tracing.logDebug("direct navigation to container-path=" + path, CPRunController.class);
				return CPUIFactory.getInstance().createMainLayoutResourceableListeningWrapperController(res, ureq, wControl, vfsWrapper, true, false, path);
			} else {
				return CPUIFactory.getInstance().createMainLayoutResourceableListeningWrapperController(res, ureq, wControl, vfsWrapper);
			}
		} else {
			return CPUIFactory.getInstance().createMainLayoutResourceableListeningWrapperController(res, ureq, wControl, vfsWrapper);
		}

	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getEditorController(org.olat.core.id.OLATResourceable org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createEditorController(final OLATResourceable res, final UserRequest ureq, final WindowControl wControl) {
		// only unzips, if not already unzipped
		final File cpRoot = FileResourceManager.getInstance().unzipFileResource(res);
		final LocalFolderImpl vfsWrapper = new LocalFolderImpl(cpRoot);
		return new CPEditMainController(ureq, wControl, vfsWrapper, res);

	}

	/**
	 * @see org.olat.repository.handlers.FileHandler#getAddController(org.olat.repository.controllers.RepositoryAddCallback, java.lang.Object,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public IAddController createAddController(final RepositoryAddCallback callback, final Object userObject, final UserRequest ureq, final WindowControl wControl) {
		if (userObject == null || userObject.equals(PROCESS_CREATENEW)) {
			return new CreateNewCPController(callback, ureq, wControl);
		} else {
			return super.createAddController(callback, userObject, ureq, wControl);
		}
	}

	@Override
	protected String getDeletedFilePrefix() {
		return "del_imscp_";
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

	@Override
	public WizardCloseResourceController createCloseResourceController(final UserRequest ureq, final WindowControl wControl, final RepositoryEntry repositoryEntry) {
		throw new AssertException("not implemented");
	}
}
