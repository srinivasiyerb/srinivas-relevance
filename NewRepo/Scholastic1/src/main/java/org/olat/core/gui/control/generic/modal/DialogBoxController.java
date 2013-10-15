package org.olat.core.gui.control.generic.modal;

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
import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.util.StringHelper;

/**
 * <h3>Description:</h3> The dialog box controller creates a modal dialog box that blocks the user interface until the user clicked on any of the buttons. In most cases
 * developers will use the DialogBoxControllerFactory and not use the generic constructor here.
 * <p>
 * Note that this controller will activate the modal panel itself and also remove the modal panel when the dialog is finished.
 * <h3>Events thrown by this controller:</h3>
 * <ul>
 * <li>ButtonClickedEvent: when user clicks a button provided in the constructor</li>
 * <li>Event.CANCELLED_EVENT: when user clicks the close icon in the window bar</li>
 * </ul>
 * <p>
 * Initial Date: 26.11.2007<br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public class DialogBoxController extends BasicController {

	private VelocityContainer dialogBoxVC;
	private Link closeLink;
	private Object userObject = null;

	DialogBoxController(UserRequest ureq, WindowControl control, String title, String text, List<String> buttonLabels) {
		super(ureq, control);
		dialogBoxVC = createVelocityContainer("dialogbox");
		// add optional title to velocity
		if (StringHelper.containsNonWhitespace(title)) {
			dialogBoxVC.contextPut("title", title);
		}
		// add content to velocity
		dialogBoxVC.contextPut("text", text);
		// add optional buttons to velocity
		List buttons = new ArrayList();
		if (buttonLabels != null) {
			for (int i = 0; i < buttonLabels.size(); i++) {
				String buttonText = buttonLabels.get(i);
				String linkName = "link_" + i;
				Link link = LinkFactory.createButton(linkName, dialogBoxVC, this);
				link.setCustomDisplayText(buttonText);
				// Within a dialog all 'you will loose form data' messages should be
				// supporesse. this is obvious to a user and leads to impossible
				// workflows. See OLAT-4257
				link.setSuppressDirtyFormWarning(true);
				buttons.add(linkName);
			}
		}
		dialogBoxVC.contextPut("buttons", buttons);

		// configuration default values:
		setCloseWindowEnabled(true);
		setCssClass("b_dialog_icon");
		// activate modal dialog now
		putInitialPanel(dialogBoxVC);
	}

	public void setCloseWindowEnabled(boolean closeWindowEnabled) {
		// add optional close icon
		if (closeWindowEnabled) {
			closeLink = LinkFactory.createIconClose("close", dialogBoxVC, this);
			// Within a dialog all 'you will loose form data' messages should be
			// supporesse. this is obvious to a user and leads to impossible
			// workflows. See OLAT-4257
			closeLink.setSuppressDirtyFormWarning(true);
			dialogBoxVC.contextPut("closeIcon", Boolean.TRUE);
		} else {
			dialogBoxVC.contextPut("closeIcon", Boolean.FALSE);
		}
	}

	public void setCssClass(String cssClass) {
		if (StringHelper.containsNonWhitespace(cssClass)) dialogBoxVC.contextPut("cssClass", cssClass);
		else dialogBoxVC.contextPut("cssClass", "");
	}

	/**
	 * attach a object to the dialog which you later retrieve. TODO:pb:example for this
	 * 
	 * @param userObject
	 */
	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}

	/**
	 * retrieve attached user object.
	 * 
	 * @return null if no user object was previously set
	 */
	public Object getUserObject() {
		return this.userObject;
	}

	public void activate() {
		getWindowControl().pushAsModalDialog(this.getInitialComponent());
	}

	/**
	 * only needed if you want to remove the dialog without having the user clicking one of the buttons or the close icon!
	 */
	public void deactivate() {
		getWindowControl().pop();
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing to dispose
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		// in any case pop dialog from modal stack
		deactivate();
		if (source == closeLink) {
			fireEvent(ureq, Event.CANCELLED_EVENT);
		} else {
			// all events come from link components. detect which one it was
			String sourceName = ((Link) source).getComponentName();
			String linkId = sourceName.substring(sourceName.indexOf("_") + 1);
			int pos = Integer.parseInt(linkId);
			fireEvent(ureq, new ButtonClickedEvent(pos));
		}
	}

}
