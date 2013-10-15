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

package org.olat.modules.glossary;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.fileresource.types.FileResource;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.controllers.IAddController;
import org.olat.repository.controllers.RepositoryAddCallback;

/**
 * Description:<br>
 * Repository workflow to create new glossary item.
 * <P>
 * Initial Date: Dec 04 2006 <br>
 * 
 * @author Florian Gnägi, frentix GmbH, http://www.frentix.com
 */
public class CreateNewGlossaryController extends DefaultController implements IAddController {
	private FileResource newFileResource;

	/**
	 * Constructor for the create new glossary workflow
	 * 
	 * @param addCallback
	 * @param ureq
	 * @param wControl
	 */
	public CreateNewGlossaryController(final RepositoryAddCallback addCallback, final UserRequest ureq, final WindowControl wControl) {
		super(wControl);
		if (addCallback != null) {
			newFileResource = GlossaryManager.getInstance().createGlossary();
			final Translator trnsltr = new PackageTranslator("org.olat.repository", ureq.getLocale());
			addCallback.setDisplayName(trnsltr.translate(newFileResource.getResourceableTypeName()));
			addCallback.setResourceable(newFileResource);
			addCallback.setResourceName("-");
			addCallback.finished(ureq);
		}
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#getTransactionComponent()
	 */
	@Override
	public Component getTransactionComponent() {
		return null;
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#transactionFinishBeforeCreate()
	 */
	@Override
	public boolean transactionFinishBeforeCreate() {
		return true;
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#transactionAborted()
	 */
	@Override
	public void transactionAborted() {
		// File resource already created. Cleanup file resource on abort.
		if (newFileResource != null) {
			GlossaryManager.getInstance().deleteGlossary(newFileResource);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// nothing to process here.
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#repositoryEntryCreated(org.olat.repository.RepositoryEntry)
	 */
	@Override
	public void repositoryEntryCreated(final RepositoryEntry re) {
		return;
	} // nothing to do here.

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}
}
