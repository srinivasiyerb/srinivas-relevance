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

package org.olat.modules.cp;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.htmlsite.HtmlStaticPageComponent;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.iframe.IFrameDisplayController;
import org.olat.core.util.vfs.VFSContainer;

/**
 * Description:<br />
 * The WebsiteDisplayController shows raw HTML based content. In normal mode, an iframe wrapper is used, only for screenreader mode the content will be integrated into
 * the framework page
 * 
 * @author Felix Jost, Florian Gnägi
 */
public class WebsiteDisplayController extends BasicController {

	/**
	 * Constructor for a web site displayer
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootContainer The vfs root container
	 * @param startUri The relative start uri to the root container
	 */
	public WebsiteDisplayController(final UserRequest ureq, final WindowControl wControl, final VFSContainer rootContainer, final String startUri) {
		super(ureq, wControl);
		if (getWindowControl().getWindowBackOffice().getWindowManager().isForScreenReader()) {
			final HtmlStaticPageComponent display = new HtmlStaticPageComponent("display", rootContainer);
			display.setCurrentURI(startUri);
			putInitialPanel(display);
		} else {
			final IFrameDisplayController iframeCtr = new IFrameDisplayController(ureq, wControl, rootContainer);
			iframeCtr.setCurrentURI(startUri);
			listenTo(iframeCtr);
			putInitialPanel(iframeCtr.getInitialComponent());
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// auto disposed by basic controller
	}

}
