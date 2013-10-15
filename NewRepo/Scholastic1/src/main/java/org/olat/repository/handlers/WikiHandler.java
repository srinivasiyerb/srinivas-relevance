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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSMediaResource;
import org.olat.core.util.vfs.filters.VFSItemSuffixFilter;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.WikiResource;
import org.olat.modules.wiki.Wiki;
import org.olat.modules.wiki.WikiContainer;
import org.olat.modules.wiki.WikiManager;
import org.olat.modules.wiki.WikiPage;
import org.olat.modules.wiki.WikiSecurityCallback;
import org.olat.modules.wiki.WikiSecurityCallbackImpl;
import org.olat.modules.wiki.WikiToZipUtils;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.controllers.AddFileResourceController;
import org.olat.repository.controllers.IAddController;
import org.olat.repository.controllers.RepositoryAddCallback;
import org.olat.repository.controllers.WizardCloseResourceController;
import org.olat.resource.references.ReferenceManager;

/**
 * Description:<br>
 * Handles the type wiki in the repository
 * <P>
 * Initial Date: May 4, 2006 <br>
 * 
 * @author guido
 */
public class WikiHandler implements RepositoryHandler {

	private static final boolean LAUNCHEABLE = true;
	private static final boolean DOWNLOADEABLE = true;
	private static final boolean EDITABLE = false;
	private static final boolean WIZARD_SUPPORT = false;
	private static final List supportedTypes;

	/**
	 * Comment for <code>PROCESS_CREATENEW</code>
	 */
	public static final String PROCESS_CREATENEW = "cn";
	public static final String PROCESS_UPLOAD = "pu";

	public WikiHandler() {
		//
	}

	static { // initialize supported types
		supportedTypes = new ArrayList(1);
		supportedTypes.add(WikiResource.TYPE_NAME);
	}

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
	 * @see org.olat.repository.handlers.RepositoryHandler#getCreateWizardController(org.olat.core.id.OLATResourceable, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createWizardController(final OLATResourceable res, final UserRequest ureq, final WindowControl wControl) {
		throw new AssertException("Trying to get wizard where no creation wizard is provided for this type.");
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getLaunchController(org.olat.resource.OLATResourceable, java.lang.String, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public MainLayoutController createLaunchController(final OLATResourceable res, final String initialViewIdentifier, final UserRequest ureq,
			final WindowControl wControl) {
		Controller controller = null;

		// check role
		final boolean isOLatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		final boolean isGuestOnly = ureq.getUserSession().getRoles().isGuestOnly();
		boolean isResourceOwner = false;
		if (isOLatAdmin) {
			isResourceOwner = true;
		} else {
			final RepositoryManager repoMgr = RepositoryManager.getInstance();
			isResourceOwner = repoMgr.isOwnerOfRepositoryEntry(ureq.getIdentity(), repoMgr.lookupRepositoryEntry(res, true));
		}

		final BusinessControl bc = wControl.getBusinessControl();
		final ContextEntry ce = bc.popLauncherContextEntry();
		final SubscriptionContext subsContext = new SubscriptionContext(res, WikiManager.WIKI_RESOURCE_FOLDER_NAME);
		final WikiSecurityCallback callback = new WikiSecurityCallbackImpl(null, isOLatAdmin, isGuestOnly, false, isResourceOwner, subsContext);

		if (ce != null) { // jump to a certain context
			final OLATResourceable ores = ce.getOLATResourceable();
			final String typeName = ores.getResourceableTypeName();
			final String page = typeName.substring("page=".length());
			controller = WikiManager.getInstance().createWikiMainControllerDisposeOnOres(ureq, wControl, res, callback, page);
		} else {
			controller = WikiManager.getInstance().createWikiMainControllerDisposeOnOres(ureq, wControl, res, callback, null);
		}
		// use on column layout
		final LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(ureq, wControl, null, null, controller.getInitialComponent(), null);
		layoutCtr.addDisposableChildController(controller); // dispose content on layout dispose
		return layoutCtr;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getEditorController(org.olat.resource.OLATResourceable, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createEditorController(final OLATResourceable res, final UserRequest ureq, final WindowControl wControl) {
		// edit is always part of a wiki
		return null;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getAsMediaResource(org.olat.resource.OLATResourceable)
	 */
	@Override
	public MediaResource getAsMediaResource(final OLATResourceable res) {
		final VFSContainer rootContainer = FileResourceManager.getInstance().getFileResourceRootImpl(res);
		final VFSLeaf wikiZip = WikiToZipUtils.getWikiAsZip(rootContainer);
		return new VFSMediaResource(wikiZip);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#cleanupOnDelete(org.olat.resource.OLATResourceable, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public boolean cleanupOnDelete(final OLATResourceable res) {
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new OLATResourceableJustBeforeDeletedEvent(res), res);
		// delete also notifications
		NotificationsManager.getInstance().deletePublishersOf(res);
		FileResourceManager.getInstance().deleteFileResource(res);
		return true;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#readyToDelete(org.olat.resource.OLATResourceable, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public boolean readyToDelete(final OLATResourceable res, final UserRequest ureq, final WindowControl wControl) {
		final ReferenceManager refM = ReferenceManager.getInstance();
		final String referencesSummary = refM.getReferencesToSummary(res, ureq.getLocale());
		if (referencesSummary != null) {
			final Translator translator = Util.createPackageTranslator(RepositoryManager.class, ureq.getLocale());
			wControl.setError(translator.translate("details.delete.error.references", new String[] { referencesSummary }));
			return false;
		}
		return true;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#createCopy(org.olat.resource.OLATResourceable, org.olat.core.gui.UserRequest)
	 */
	@Override
	public OLATResourceable createCopy(final OLATResourceable res, final UserRequest ureq) {
		final FileResourceManager frm = FileResourceManager.getInstance();
		final VFSContainer wikiContainer = WikiManager.getInstance().getWikiContainer(res, WikiManager.WIKI_RESOURCE_FOLDER_NAME);
		if (wikiContainer == null) {
			// if the wiki container is null, let the WikiManager to create one
			WikiManager.getInstance().getOrLoadWiki(res);
		}
		final OLATResourceable copy = frm.createCopy(res, WikiManager.WIKI_RESOURCE_FOLDER_NAME);
		final VFSContainer rootContainer = frm.getFileResourceRootImpl(copy);
		// create folders
		final VFSContainer newMediaCont = rootContainer.createChildContainer(WikiContainer.MEDIA_FOLDER_NAME);
		rootContainer.createChildContainer(WikiManager.VERSION_FOLDER_NAME);
		// copy media files to folders
		final VFSContainer origRootContainer = frm.getFileResourceRootImpl(res);
		final VFSContainer origMediaCont = (VFSContainer) origRootContainer.resolve(WikiContainer.MEDIA_FOLDER_NAME);
		final List mediaFiles = origMediaCont.getItems();
		for (final Iterator iter = mediaFiles.iterator(); iter.hasNext();) {
			final VFSLeaf element = (VFSLeaf) iter.next();
			newMediaCont.copyFrom(element);
		}

		// reset properties files to default values
		final VFSContainer wikiCont = (VFSContainer) rootContainer.resolve(WikiManager.WIKI_RESOURCE_FOLDER_NAME);
		final List leafs = wikiCont.getItems(new VFSItemSuffixFilter(new String[] { WikiManager.WIKI_PROPERTIES_SUFFIX }));
		for (final Iterator iter = leafs.iterator(); iter.hasNext();) {
			final VFSLeaf leaf = (VFSLeaf) iter.next();
			final WikiPage page = Wiki.assignPropertiesToPage(leaf);
			// reset the copied pages to a the default values
			page.resetCopiedPage();
			WikiManager.getInstance().updateWikiPageProperties(copy, page);
		}

		return copy;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getAddController(org.olat.repository.controllers.RepositoryAddCallback, java.lang.Object,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public IAddController createAddController(final RepositoryAddCallback callback, final Object userObject, final UserRequest ureq, final WindowControl wControl) {
		if (userObject == null || userObject.equals(WikiHandler.PROCESS_UPLOAD)) {
			return new AddFileResourceController(callback, supportedTypes, new String[] { "zip" }, ureq, wControl);
		} else {
			return new WikiCreateController(callback, ureq, wControl);
		}
	}

	@Override
	public Controller createDetailsForm(final UserRequest ureq, final WindowControl wControl, final OLATResourceable res) {
		return FileResourceManager.getInstance().getDetailsForm(ureq, wControl, res);
	}

	@Override
	public String archive(final Identity archiveOnBehalfOf, final String archivFilePath, final RepositoryEntry repoEntry) {
		final VFSContainer rootContainer = FileResourceManager.getInstance().getFileResourceRootImpl(repoEntry.getOlatResource());
		final VFSLeaf wikiZip = WikiToZipUtils.getWikiAsZip(rootContainer);
		final String exportFileName = "del_wiki_" + repoEntry.getOlatResource().getResourceableId() + ".zip";
		final String fullFilePath = archivFilePath + File.separator + exportFileName;

		final File fExportZIP = new File(fullFilePath);
		final InputStream fis = wikiZip.getInputStream();

		try {
			FileUtils.bcopy(wikiZip.getInputStream(), fExportZIP, "archive wiki");
		} catch (final FileNotFoundException e) {
			Tracing.logWarn("Can not archive wiki repoEntry=" + repoEntry, this.getClass());
		} catch (final IOException ioe) {
			Tracing.logWarn("Can not archive wiki repoEntry=" + repoEntry, this.getClass());
		} finally {
			FileUtils.closeSafely(fis);
		}
		return exportFileName;
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
