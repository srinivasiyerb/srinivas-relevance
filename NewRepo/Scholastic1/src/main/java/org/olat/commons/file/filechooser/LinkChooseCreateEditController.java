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

package org.olat.commons.file.filechooser;

import org.olat.core.commons.controllers.linkchooser.CustomLinkTreeModel;
import org.olat.core.commons.editor.htmleditor.WysiwygFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.vfs.VFSContainer;

/**
 * Controller to create link chooser (select link to file or select internal link e.g. link to course node).
 * 
 * @author Christian Guretzki
 */
public class LinkChooseCreateEditController extends FileChooseCreateEditController {

	private final CustomLinkTreeModel customLinkTreeModel;

	/**
	 * 
	 */
	public LinkChooseCreateEditController(final UserRequest ureq, final WindowControl wControl, final String chosenFile, final Boolean allowRelativeLinks,
			final VFSContainer rootContainer, final String target, final String fieldSetLegend, final CustomLinkTreeModel customLinkTreeModel) {
		super(ureq, wControl, chosenFile, allowRelativeLinks, rootContainer, target, fieldSetLegend);
		this.customLinkTreeModel = customLinkTreeModel;
	}

	/**
	 * 
	 */
	public LinkChooseCreateEditController(final UserRequest ureq, final WindowControl wControl, final String chosenFile, final Boolean allowRelativeLinks,
			final VFSContainer rootContainer, final CustomLinkTreeModel customLinkTreeModel) {
		super(ureq, wControl, chosenFile, allowRelativeLinks, rootContainer);
		this.customLinkTreeModel = customLinkTreeModel;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		super.doDispose();
	}

	/**
	 * Creates a Controller with internal-link support.
	 * 
	 * @see org.olat.commons.file.filechooser.FileChooseCreateEditController#createWysiwygController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, org.olat.core.util.vfs.VFSContainer, java.lang.String)
	 */
	@Override
	protected Controller createWysiwygController(final UserRequest ureq, final WindowControl windowControl, final VFSContainer rootContainer, final String chosenFile) {
		return WysiwygFactory.createWysiwygControllerWithInternalLink(ureq, windowControl, rootContainer, chosenFile, true, customLinkTreeModel);
	}

}
