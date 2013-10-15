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

package org.olat.test.guidemo;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.dev.controller.SourceViewController;

public class GuiDemoWindowControlController extends BasicController {

	VelocityContainer vcMain;
	GuiDemoPushPopController pushpop;
	private final Link infoButton;
	private final Link warnButton;
	private final Link errorButton;
	private final Link pushButton;

	public GuiDemoWindowControlController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		vcMain = this.createVelocityContainer("guidemo-control");
		infoButton = LinkFactory.createButton("guidemo.window.control.info", vcMain, this);
		warnButton = LinkFactory.createButton("guidemo.window.control.warn", vcMain, this);
		errorButton = LinkFactory.createButton("guidemo.window.control.error", vcMain, this);
		pushButton = LinkFactory.createButton("guidemo.window.control.push", vcMain, this);

		// add source view control
		final Controller sourceview = new SourceViewController(ureq, wControl, this.getClass(), vcMain);
		vcMain.put("sourceview", sourceview.getInitialComponent());

		this.putInitialPanel(vcMain);
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == infoButton) {
			this.showInfo("guidemo.window.control.info.message", "");
		} else if (source == warnButton) {
			this.showWarning("guidemo.window.control.warn.message", "");
		} else if (source == errorButton) {
			this.showError("guidemo.window.control.error.message", "");
		} else if (source == pushButton) {
			pushpop = new GuiDemoPushPopController(ureq, getWindowControl());
			pushpop.addControllerListener(this);
			getWindowControl().pushToMainArea(pushpop.getInitialComponent());
		}
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == pushpop) {
			getWindowControl().pop();
			this.showInfo("guidemo.window.control.pushpop", "");
		}
	}

	@Override
	protected void doDispose() {}

}
