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

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.coordinate.LockResult;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.controllers.IAddController;
import org.olat.repository.controllers.RepositoryAddCallback;
import org.olat.repository.controllers.WizardCloseResourceController;

/**
 * Initial Date: Apr 5, 2004
 * 
 * @author Mike Stock Comment:
 */
public interface RepositoryHandler {

	/**
	 * @return Return the typeNames of OLATResourceable this Handler can handle.
	 */
	public List<String> getSupportedTypes();

	/**
	 * @return true if this handler supports donwloading Resourceables of its type.
	 */
	public boolean supportsDownload(RepositoryEntry repoEntry);

	/**
	 * @return true if this handler supports launching Resourceables of its type.
	 */
	public boolean supportsLaunch(RepositoryEntry repoEntry);

	/**
	 * @return true if this handler supports an editor for Resourceables of its type.
	 */
	public boolean supportsEdit(RepositoryEntry repoEntry);

	/**
	 * @param repoEntry
	 * @return true when this RepositoryHandler supports and can create a wizard for creating some initial content. This only has an effect when supportsEditor returns
	 *         true.
	 */
	public boolean supportsWizard(RepositoryEntry repoEntry);

	/**
	 * Called if a user launches a Resourceable that this handler can handle.
	 * 
	 * @param res
	 * @param initialViewIdentifier if null the default view will be started, otherwise a controllerfactory type dependant view will be activated (subscription subtype)
	 * @param ureq
	 * @param wControl
	 * @return Controller able to launch resourceable.
	 */
	public MainLayoutController createLaunchController(OLATResourceable res, String initialViewIdentifier, UserRequest ureq, WindowControl wControl);

	/**
	 * Called if a user wants to edit a Resourceable that this handler can provide an editor for. (it is given here that this method can only be called when the current
	 * user is either olat admin or in the owning group of this resource
	 * 
	 * @param res
	 * @param ureq
	 * @param wControl
	 * @return Controler able to edit resourceable.
	 */
	public Controller createEditorController(OLATResourceable res, UserRequest ureq, WindowControl wControl);

	/**
	 * Called if a user wants to create a Resourceable via wizard.
	 * 
	 * @param res
	 * @param ureq
	 * @param wControl
	 * @return Controller that guides trough the creation workflow via wizard.
	 */
	public Controller createWizardController(OLATResourceable res, UserRequest ureq, WindowControl wControl);

	/**
	 * @param ureq
	 * @param wControl
	 * @param repositoryEntry
	 * @return
	 */
	public WizardCloseResourceController createCloseResourceController(UserRequest ureq, WindowControl wControl, RepositoryEntry repositoryEntry);

	/**
	 * Called if a user downloads a Resourceable that this handler can handle.
	 * 
	 * @param res
	 * @return MediaResource delivering resourceable.
	 */
	public MediaResource getAsMediaResource(OLATResourceable res);

	/**
	 * Called if the repository entry referencing the given Resourceable will be deleted from the repository. Do any necessary cleanup work specific to this handler's
	 * type. The handler is responsible for deleting the resourceable aswell.
	 * 
	 * @param res
	 * @param ureq
	 * @param wControl
	 * @return true if delete successfull, false if not.
	 */
	public boolean cleanupOnDelete(OLATResourceable res);

	/**
	 * Called if the repository entry referencing the given Resourceable will be deleted from the repository. Return status wether to proceed with the delete action. If
	 * this method returns false, the entry will not be deleted.
	 * 
	 * @param res
	 * @param ureq
	 * @param wControl
	 * @return true if ressource is ready to delete, false if not.
	 */
	public boolean readyToDelete(OLATResourceable res, UserRequest ureq, WindowControl wControl);

	/**
	 * Create a copy of the given resourceable.
	 * 
	 * @param res
	 * @param ureq
	 * @return Copy of given resourceable.
	 */
	public OLATResourceable createCopy(OLATResourceable res, UserRequest ureq);

	/**
	 * Called the repository wants to add a new resourceable of this handler's type. Do any task necessary, set all fields of the provided RepositoryAddCallback and call
	 * callback.finished() upon success, or callback.canceled() / callback.failed(). The latter two will not create any repository entries. If you cancel or fail, do any
	 * cleanup work yourself. RepositoryHandler.delete() will not be called since (as outlined above) the repository does not create any entry at all. If
	 * callback.finished(), the user then enters repository details data. If the user finishes, AddController.finishTransaction() will be called. Do any final work now,
	 * since after this, the repository entry will be persisted. If the user aborts, AddController.abortTransaction() will be called. Do any cleanup work - no repository
	 * entry will be created in this state. The OLATResourceable set in the callback does not necessarily have to be persited.
	 * 
	 * @param callback
	 * @param userObject
	 * @param ureq
	 * @param wControl
	 * @return Controller implementing Add workflow.
	 */
	public IAddController createAddController(RepositoryAddCallback callback, Object userObject, UserRequest ureq, WindowControl wControl);

	/**
	 * If a handler likes to provied any details on a resourceable in the repository's details view, he may do so by providing a component that renders the details.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param res
	 * @return Controller displaying details or null, if no details are available.
	 */
	public Controller createDetailsForm(UserRequest ureq, WindowControl wControl, OLATResourceable res);

	public String archive(Identity archiveOnBehalfOf, String archivFilePath, RepositoryEntry repoEntry);

	/**
	 * Acquires lock for the input ores and identity.
	 * 
	 * @param ores
	 * @param identity
	 * @return the LockResult or null if no locking supported.
	 */
	public LockResult acquireLock(OLATResourceable ores, Identity identity);

	/**
	 * Releases the lock.
	 * 
	 * @param lockResult the LockResult received when locking
	 */
	public void releaseLock(LockResult lockResult);

	/**
	 * @param ores
	 * @return
	 */
	public boolean isLocked(OLATResourceable ores);

}
