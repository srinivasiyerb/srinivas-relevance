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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.gui.control.generic.wizard;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;

/**
 * Description: <BR>
 * Generic wizard controller. Handles layout, title, cancel button and steps visibility.
 * <P/>
 * Initial Date: Sep 11, 2004
 * 
 * @author Florian Gnaegi
 */

public class WizardController extends BasicController {

	protected WindowControl wControl;
	private VelocityContainer wizardVC;
	private WizardInfoController wic;

	private int steps = 0;
	private int currentStep = 0;
	private Link finishButton;
	private Link cancelButton;

	/**
	 * @param ureq
	 * @param wControl
	 * @param steps
	 */
	public WizardController(UserRequest ureq, WindowControl wControl, int steps) {
		super(ureq, wControl);

		setBasePackage(WizardController.class);

		this.steps = steps;

		wizardVC = createVelocityContainer("wizard");
		finishButton = LinkFactory.createCustomLink("finish", "cmd.wizard.cancel", "cmd.wizard.finished", Link.BUTTON, this.wizardVC, this);
		cancelButton = LinkFactory.createCustomLink("cancel", "cmd.wizard.cancel", "cmd.wizard.cancel", Link.BUTTON, this.wizardVC, this);

		this.wic = new WizardInfoController(ureq, this.steps);
		listenTo(wic);

		this.wizardVC.put("wic", wic.getInitialComponent());

		putInitialPanel(wizardVC);
	}

	/**
	 * @param wizardTitle
	 */
	public void setWizardTitle(String wizardTitle) {
		this.wizardVC.contextPut("wizardTitle", wizardTitle);
	}

	/**
	 * @param stepTitle
	 * @param component
	 */
	public void setBackWizardStep(String stepTitle, Component component) {
		this.currentStep--;
		setWizardVcContent(stepTitle, component);
	}

	/**
	 * @param stepTitle
	 * @param component
	 */
	public void setNextWizardStep(String stepTitle, Component component) {
		this.currentStep++;
		setWizardVcContent(stepTitle, component);
	}

	/**
	 * @param title
	 * @param component
	 */
	private void setWizardVcContent(String title, Component component) {
		this.wic.setCurStep(this.currentStep);
		this.wizardVC.contextPut("currentStep", new Integer(this.currentStep));
		this.wizardVC.contextPut("steps", new Integer(this.steps));
		this.wizardVC.contextPut("title", title);
		if (this.currentStep == this.steps) {
			this.wizardVC.contextPut("lastStep", Boolean.TRUE);
		} else {
			this.wizardVC.contextPut("lastStep", Boolean.FALSE);
		}
		this.wizardVC.put("component", component);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == finishButton || source == cancelButton) {
			fireEvent(ureq, Event.CANCELLED_EVENT);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

}