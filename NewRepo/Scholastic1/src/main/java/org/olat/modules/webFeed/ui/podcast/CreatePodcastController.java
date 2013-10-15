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
package org.olat.modules.webFeed.ui.podcast;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.OLATResourceable;
import org.olat.modules.webFeed.managers.FeedManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.controllers.IAddController;
import org.olat.repository.controllers.RepositoryAddCallback;

/**
 * Controller that handles the creation of a new podcast resource.
 * <P>
 * Initial Date: Mar 18, 2009 <br>
 * 
 * @author gwassmann
 */
public class CreatePodcastController extends DefaultController implements IAddController {
	private OLATResourceable feedResource;

	/**
	 * Constructor
	 * 
	 * @param addCallback
	 * @param ureq
	 * @param wControl
	 */
	protected CreatePodcastController(final RepositoryAddCallback addCallback, final UserRequest ureq, final WindowControl wControl) {
		super(wControl);
		if (addCallback != null) {
			final FeedManager manager = FeedManager.getInstance();
			// Create a new podcast feed resource
			feedResource = manager.createPodcastResource();
			final Translator trans = new PackageTranslator("org.olat.repository", ureq.getLocale());
			addCallback.setDisplayName(trans.translate(feedResource.getResourceableTypeName()));
			addCallback.setResourceable(feedResource);
			addCallback.setResourceName(manager.getFeedKind(feedResource));
			addCallback.finished(ureq);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// Nothing to dispose
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	@SuppressWarnings("unused")
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		// Nothing to catch
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#getTransactionComponent()
	 */
	@Override
	public Component getTransactionComponent() {
		// No additional workflow for feed creation
		return null;
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#repositoryEntryCreated(org.olat.repository.RepositoryEntry)
	 */
	@Override
	@SuppressWarnings("unused")
	public void repositoryEntryCreated(final RepositoryEntry re) {
		// Nothing to do here, but thanks for asking.
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#transactionAborted()
	 */
	@Override
	public void transactionAborted() {
		FeedManager.getInstance().delete(feedResource);
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#transactionFinishBeforeCreate()
	 */
	@Override
	public boolean transactionFinishBeforeCreate() {
		// Don't finish before creation (?!)
		return true;
	}

}
