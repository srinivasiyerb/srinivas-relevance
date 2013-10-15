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
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.Util;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.fileresource.FileResourceManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.controllers.AddFileResourceController;
import org.olat.repository.controllers.IAddController;
import org.olat.repository.controllers.RepositoryAddCallback;
import org.olat.resource.references.ReferenceManager;

/**
 * Common super class for all file-based handlers.
 * 
 * @author Christian Guretzki
 */
public abstract class FileHandler {

	private static final String PACKAGE = Util.getPackageName(RepositoryManager.class);

	/**
	 * 
	 */
	public FileHandler() {}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getAsMediaResource(org.olat.core.id.OLATResourceable
	 */
	public MediaResource getAsMediaResource(final OLATResourceable res) {
		return FileResourceManager.getInstance().getAsDownloadeableMediaResource(res);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getAddController(org.olat.repository.controllers.RepositoryAddCallback, java.lang.Object,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	public IAddController createAddController(final RepositoryAddCallback callback, final Object userObject, final UserRequest ureq, final WindowControl wControl) {
		return new AddFileResourceController(callback, getSupportedTypes(), new String[] { "zip" }, ureq, wControl);
	}

	public Controller createDetailsForm(final UserRequest ureq, final WindowControl wControl, final OLATResourceable res) {
		return FileResourceManager.getInstance().getDetailsForm(ureq, wControl, res);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#cleanupOnDelete(org.olat.core.id.OLATResourceable org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	public boolean cleanupOnDelete(final OLATResourceable res) {
		// notify all current users of this resource (content packaging file resource) that it will be deleted now.
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new OLATResourceableJustBeforeDeletedEvent(res), res);
		FileResourceManager.getInstance().deleteFileResource(res);
		return true;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#readyToDelete(org.olat.core.id.OLATResourceable org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	public boolean readyToDelete(final OLATResourceable res, final UserRequest ureq, final WindowControl wControl) {
		final String referencesSummary = ReferenceManager.getInstance().getReferencesToSummary(res, ureq.getLocale());
		if (referencesSummary != null) {
			final Translator translator = new PackageTranslator(PACKAGE, ureq.getLocale());
			wControl.setError(translator.translate("details.delete.error.references", new String[] { referencesSummary }));
			return false;
		}
		return true;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#createCopy(org.olat.core.id.OLATResourceable org.olat.core.gui.UserRequest)
	 */
	public OLATResourceable createCopy(final OLATResourceable res, final UserRequest ureq) {
		return FileResourceManager.getInstance().createCopy(res);
	}

	public String archive(final Identity archiveOnBehalfOf, final String archivFilePath, final RepositoryEntry repoEntry) {
		final String exportFileName = getDeletedFilePrefix() + repoEntry.getOlatResource().getResourceableId() + ".zip";
		final String fullFilePath = archivFilePath + File.separator + exportFileName;
		final File rootFile = FileResourceManager.getInstance().getFileResourceRoot(repoEntry.getOlatResource());
		ZipUtil.zipAll(rootFile, new File(fullFilePath), false);
		return exportFileName;
	}

	abstract protected String getDeletedFilePrefix();

	abstract protected List getSupportedTypes();

}
