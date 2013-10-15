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

package org.olat.repository.controllers;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.DBRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.repository.DetailsReadOnlyForm;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;

/**
 * Description:<br>
 * 
 * @author Felix Jost
 */
public class RepositoryCopyController extends BasicController {

	OLog log = Tracing.createLoggerFor(this.getClass());

	private static final String PACKAGE = Util.getPackageName(RepositoryManager.class);
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(RepositoryManager.class);

	private final VelocityContainer mainContainer;
	private final Link cancelButton;
	private final Link forwardButton;
	private final RepositoryEditDescriptionController descriptionController;
	private final BaseSecurity securityManager;
	private final RepositoryEntry sourceEntry;
	private RepositoryEntry newEntry;

	// flag is true when workflow has been finished successfully,
	// otherwhise when disposing the controller or in a case of
	// user abort / cancel the system will delete temporary data
	private boolean workflowSuccessful = false;

	/**
	 * Create a repository add controller that adds the given resourceable.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param sourceEntry
	 */
	public RepositoryCopyController(final UserRequest ureq, final WindowControl wControl, final RepositoryEntry sourceEntry) {
		super(ureq, wControl);

		setBasePackage(RepositoryManager.class);

		this.sourceEntry = sourceEntry;
		this.newEntry = null;

		securityManager = BaseSecurityManager.getInstance();

		mainContainer = createVelocityContainer("copy");
		cancelButton = LinkFactory.createButton("cmd.cancel", mainContainer, this);
		forwardButton = LinkFactory.createButton("cmd.forward", mainContainer, this);
		forwardButton.setEnabled(false);
		LinkFactory.markDownloadLink(forwardButton); // TODO:cg: for copy of large repositoryEntries => Remove when new long-running task is implemented
		forwardButton.setTextReasonForDisabling(translate("disabledforwardreason"));

		newEntry = createNewRepositoryEntry(sourceEntry, ureq);
		descriptionController = new RepositoryEditDescriptionController(ureq, getWindowControl(), newEntry, true);
		listenTo(descriptionController);

		mainContainer.put("details", descriptionController.getInitialComponent());

		putInitialPanel(mainContainer);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == descriptionController) {
			if (event == Event.CANCELLED_EVENT) {
				// abort transaction
				cleanup();
				fireEvent(ureq, Event.CANCELLED_EVENT);
				return;
			} else if (event == Event.DONE_EVENT) {
				forwardButton.setEnabled(true);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == forwardButton) {

			// check if repository entry is still available
			final RepositoryManager rm = RepositoryManager.getInstance();
			final RepositoryEntry checkEntry = rm.lookupRepositoryEntry(sourceEntry.getKey());
			if (checkEntry == null) { // entry has been deleted meanwhile
				showError("error.createcopy");
				fireEvent(ureq, Event.FAILED_EVENT);
				fireEvent(ureq, new EntryChangedEvent(sourceEntry, EntryChangedEvent.DELETED));
				return;
			}
			newEntry = descriptionController.getRepositoryEntry();
			// update needed to save changed name and desc.
			RepositoryManager.getInstance().updateRepositoryEntry(newEntry);
			final RepositoryHandler typeToCopy = RepositoryHandlerFactory.getInstance().getRepositoryHandler(sourceEntry);
			final IAddController addController = typeToCopy.createAddController(null, null, ureq, getWindowControl());
			addController.repositoryEntryCreated(newEntry);
			// dispose immediately (cleanup temp files), not really used
			// as a controller, should be in a business logic frontend manager instead!
			addController.dispose();

			showInfo("add.success");
			workflowSuccessful = true;
			fireEvent(ureq, Event.DONE_EVENT);
			return;
		} else if (source == cancelButton) {
			// abort transaction
			cleanup();
			fireEvent(ureq, Event.CANCELLED_EVENT);
			return;
		}
	}

	private RepositoryEntry createNewRepositoryEntry(final RepositoryEntry src, final UserRequest ureq) {
		final RepositoryEntry preparedEntry = RepositoryManager.getInstance().createRepositoryEntryInstance(ureq.getIdentity().getName());

		preparedEntry.setCanDownload(src.getCanDownload());
		preparedEntry.setCanLaunch(src.getCanLaunch());
		// FIXME:pb:ms translation for COPY OF
		String newDispalyname = "Copy of " + src.getDisplayname();
		if (newDispalyname.length() > DetailsReadOnlyForm.MAX_DISPLAYNAME) {
			newDispalyname = newDispalyname.substring(0, DetailsReadOnlyForm.MAX_DISPLAYNAME - 1);
		}
		preparedEntry.setDisplayname(newDispalyname);
		preparedEntry.setDescription(src.getDescription());
		String resName = src.getResourcename();
		if (resName == null) {
			resName = "";
		}
		preparedEntry.setResourcename(resName);
		final RepositoryHandler typeToCopy = RepositoryHandlerFactory.getInstance().getRepositoryHandler(src);
		final OLATResourceable newResourceable = typeToCopy.createCopy(sourceEntry.getOlatResource(), ureq);
		if (newResourceable == null) {
			getWindowControl().setError(this.getTranslator().translate("error.createcopy"));
			fireEvent(ureq, Event.FAILED_EVENT);
			return null;
		}

		final OLATResource ores = OLATResourceManager.getInstance().findOrPersistResourceable(newResourceable);
		preparedEntry.setOlatResource(ores);
		// create security group
		final SecurityGroup newGroup = securityManager.createAndPersistSecurityGroup();
		// member of this group may modify member's membership
		securityManager.createAndPersistPolicy(newGroup, Constants.PERMISSION_ACCESS, newGroup);
		// members of this group are always authors also
		securityManager.createAndPersistPolicy(newGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);

		securityManager.addIdentityToSecurityGroup(ureq.getIdentity(), newGroup);
		preparedEntry.setOwnerGroup(newGroup);

		RepositoryManager.getInstance().saveRepositoryEntry(preparedEntry);
		// copy image if available
		RepositoryEntryImageController.copyImage(src, preparedEntry);
		return preparedEntry;
	}

	protected RepositoryEntry getNewEntry() {
		return newEntry;
	}

	private void cleanup() {
		log.debug("Cleanup : newEntry=" + newEntry);
		// load newEntry again from DB because it could be changed (Exception object modified)
		// o_clusterREVIEW
		if (newEntry != null) {
			final RepositoryEntry entry = RepositoryManager.getInstance().lookupRepositoryEntry(newEntry.getKey());
			if (entry != null) {
				try {
					log.debug("Cleanup : started");
					newEntry = (RepositoryEntry) DBFactory.getInstance().loadObject(newEntry, true);
					final SecurityGroup secGroup = newEntry.getOwnerGroup();
					final RepositoryHandler repositoryHandler = RepositoryHandlerFactory.getInstance().getRepositoryHandler(newEntry);
					log.debug("Cleanup : repositoryHandler.cleanupOnDelete for olat-resource=" + newEntry.getOlatResource());
					repositoryHandler.cleanupOnDelete(newEntry.getOlatResource());
					log.debug("Cleanup : deleteRepositoryEntry");
					RepositoryManager.getInstance().deleteRepositoryEntry(newEntry);
					if (secGroup != null) { // delete owner group
						log.debug("Cleanup : deleteSecurityGroup secGroup=" + secGroup);
						securityManager.deleteSecurityGroup(secGroup);
					}
					newEntry = null;
				} catch (final DBRuntimeException ex) {
					log.error("Can not cleanup properly ", ex);
				}
			}
		}
		log.debug("Cleanup : finished");
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		log.debug("doDispose : newEntry=" + newEntry);
		if (!workflowSuccessful) {
			cleanup();
		}
	}
}
