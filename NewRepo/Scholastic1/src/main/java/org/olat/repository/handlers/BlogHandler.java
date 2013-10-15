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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.repository.handlers;

import java.util.ArrayList;
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
import org.olat.core.logging.AssertException;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.BlogFileResource;
import org.olat.modules.webFeed.FeedResourceSecurityCallback;
import org.olat.modules.webFeed.FeedSecurityCallback;
import org.olat.modules.webFeed.managers.FeedManager;
import org.olat.modules.webFeed.ui.blog.BlogUIFactory;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.controllers.AddFileResourceController;
import org.olat.repository.controllers.IAddController;
import org.olat.repository.controllers.RepositoryAddCallback;
import org.olat.repository.controllers.WizardCloseResourceController;
import org.olat.resource.references.ReferenceManager;

/**
 * Responsible class for handling any actions involving blog resources.
 * <P>
 * Initial Date: Feb 25, 2009 <br>
 * 
 * @author Gregor Wassmann
 */
// Loads of parameters are unused
@SuppressWarnings("unused")
public class BlogHandler implements RepositoryHandler {
	public static final String PROCESS_CREATENEW = "create_new";
	public static final String PROCESS_UPLOAD = "upload";

	private static final boolean DOWNLOADABLE = true;
	private static final boolean EDITABLE = true;
	private static final boolean LAUNCHABLE = true;
	private static final boolean WIZARD_SUPPORT = false;
	private static final List<String> supportedTypes;

	static { // initialize supported types
		supportedTypes = new ArrayList<String>(1);
		supportedTypes.add(BlogFileResource.TYPE_NAME);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#acquireLock(org.olat.core.id.OLATResourceable, org.olat.core.id.Identity)
	 */
	@Override
	public LockResult acquireLock(final OLATResourceable ores, final Identity identity) {
		return FeedManager.getInstance().acquireLock(ores, identity);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#archive(java.lang.String, org.olat.repository.RepositoryEntry)
	 */
	@Override
	public String archive(final Identity archiveOnBehalfOf, final String archivFilePath, final RepositoryEntry repoEntry) {
		// Apperantly, this method is used for backing up any user related content
		// (comments etc.) on deletion. Up to now, this doesn't exist in blogs.
		return null;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#cleanupOnDelete(org.olat.core.id.OLATResourceable, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public boolean cleanupOnDelete(final OLATResourceable res) {
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new OLATResourceableJustBeforeDeletedEvent(res), res);
		// For now, notifications are not implemented since a blog feed is meant
		// to be subscriped to anyway.
		// NotificationsManager.getInstance().deletePublishersOf(res);
		FeedManager.getInstance().delete(res);
		return true;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#createCopy(org.olat.core.id.OLATResourceable, org.olat.core.gui.UserRequest)
	 */
	@Override
	public OLATResourceable createCopy(final OLATResourceable res, final UserRequest ureq) {
		final FeedManager manager = FeedManager.getInstance();
		return manager.copy(res);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getAddController(org.olat.repository.controllers.RepositoryAddCallback, java.lang.Object,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public IAddController createAddController(final RepositoryAddCallback callback, final Object userObject, final UserRequest ureq, final WindowControl wControl) {
		IAddController addCtr = null;
		if (userObject == null || userObject.equals(PROCESS_UPLOAD)) {
			addCtr = new AddFileResourceController(callback, supportedTypes, new String[] { "zip" }, ureq, wControl);
		} else {
			addCtr = BlogUIFactory.getInstance(ureq.getLocale()).createAddController(callback, ureq, wControl);
		}
		return addCtr;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getAsMediaResource(org.olat.core.id.OLATResourceable)
	 */
	@Override
	public MediaResource getAsMediaResource(final OLATResourceable res) {
		final FeedManager manager = FeedManager.getInstance();
		return manager.getFeedArchiveMediaResource(res);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getDetailsComponent(org.olat.core.id.OLATResourceable, org.olat.core.gui.UserRequest)
	 */
	@Override
	public Controller createDetailsForm(final UserRequest ureq, final WindowControl wControl, final OLATResourceable res) {
		return FileResourceManager.getInstance().getDetailsForm(ureq, wControl, res);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getEditorController(org.olat.core.id.OLATResourceable, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createEditorController(final OLATResourceable res, final UserRequest ureq, final WindowControl control) {
		return createLaunchController(res, null, ureq, control);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getLaunchController(org.olat.core.id.OLATResourceable, java.lang.String, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public MainLayoutController createLaunchController(final OLATResourceable res, final String initialViewIdentifier, final UserRequest ureq,
			final WindowControl wControl) {
		final RepositoryEntry repoEntry = RepositoryManager.getInstance().lookupRepositoryEntry(res, false);
		final boolean isAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		final boolean isOwner = RepositoryManager.getInstance().isOwnerOfRepositoryEntry(ureq.getIdentity(), repoEntry);
		final FeedSecurityCallback callback = new FeedResourceSecurityCallback(isAdmin, isOwner);
		final Controller blogCtr = BlogUIFactory.getInstance(ureq.getLocale()).createMainController(res, ureq, wControl, callback);
		final LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(ureq, wControl, null, null, blogCtr.getInitialComponent(), null);
		layoutCtr.addDisposableChildController(blogCtr);
		return layoutCtr;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#getSupportedTypes()
	 */
	@Override
	public List<String> getSupportedTypes() {
		return supportedTypes;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#readyToDelete(org.olat.core.id.OLATResourceable, org.olat.core.gui.UserRequest,
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
	 * @see org.olat.repository.handlers.RepositoryHandler#releaseLock(org.olat.core.util.coordinate.LockResult)
	 */
	@Override
	public void releaseLock(final LockResult lockResult) {
		FeedManager.getInstance().releaseLock(lockResult);
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsDownload()
	 */
	@Override
	public boolean supportsDownload(final RepositoryEntry repoEntry) {
		return DOWNLOADABLE;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsEdit()
	 */
	@Override
	public boolean supportsEdit(final RepositoryEntry repoEntry) {
		return EDITABLE;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsLaunch()
	 */
	@Override
	public boolean supportsLaunch(final RepositoryEntry repoEntry) {
		return LAUNCHABLE;
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
	 * @see org.olat.repository.handlers.RepositoryHandler#getCloseResourceController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.repository.RepositoryEntry)
	 */
	@Override
	public WizardCloseResourceController createCloseResourceController(final UserRequest ureq, final WindowControl control, final RepositoryEntry repositoryEntry) {
		// No specific close wizard is implemented.
		throw new AssertException("not implemented");
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#isLocked(org.olat.core.id.OLATResourceable)
	 */
	@Override
	public boolean isLocked(final OLATResourceable ores) {
		return FeedManager.getInstance().isLocked(ores);
	}

}
