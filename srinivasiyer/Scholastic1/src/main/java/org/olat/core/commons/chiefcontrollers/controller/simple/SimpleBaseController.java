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
package org.olat.core.commons.chiefcontrollers.controller.simple;

import org.olat.core.gui.GUIMessage;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.OncePanel;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.ContentableController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowBackOffice;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.WindowControlInfoImpl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.guistack.GuiStack;
import org.olat.core.gui.control.info.WindowControlInfo;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.logging.AssertException;

/**
 * Description:<br>
 * TODO: Felix Jost Class Description for Trans
 * <P>
 * Initial Date: 23.01.2007 <br>
 * 
 * @author Felix Jost, http://www.goodsolutions.ch
 */
public class SimpleBaseController extends BasicController implements ContentableController {
	private GuiStack currentGuiStack;

	private Panel content;
	private Panel modalPanel;
	private GUIMessage guiMessage;
	private OncePanel guimsgPanel;
	private VelocityContainer guimsgVc;
	private Controller contentController;
	private WindowControl myWControl;

	private VelocityContainer mainVc;

	/**
	 * @param ureq
	 * @param wControl
	 */
	public SimpleBaseController(UserRequest ureq, WindowControl wControl) {
		super(ureq, null);
		mainVc = createVelocityContainer("simple");

		// GUI messages
		guimsgVc = createVelocityContainer("guimsg");

		guiMessage = new GUIMessage();
		guimsgVc.contextPut("guiMessage", guiMessage);
		guimsgPanel = new OncePanel("guimsgPanel");

		mainVc.put("guimessage", guimsgPanel);

		final WindowControl origWCo = wControl; // no need to use: super.getWindowControl();

		myWControl = new WindowControl() {
			private WindowControlInfo wci;

			{
				wci = new WindowControlInfoImpl(SimpleBaseController.this, origWCo.getWindowControlInfo());
			}

			/**
			 * @see org.olat.core.gui.control.WindowControl#pushToMainArea(org.olat.core.gui.components.Component)
			 */
			@Override
			public void pushToMainArea(Component newMainArea) {
				currentGuiStack.pushContent(newMainArea);
			}

			/**
			 * @see org.olat.core.gui.control.WindowControl#pushAsModalDialog(java.lang.String, org.olat.core.gui.components.Component)
			 */
			@Override
			public void pushAsModalDialog(Component newModalDialog) {
				currentGuiStack.pushModalDialog(newModalDialog);
			}

			/**
			 * @see org.olat.core.gui.control.WindowControl#pop()
			 */
			@Override
			public void pop() {
				// reactivate latest dialog from stack, dumping current one
				currentGuiStack.popContent();
			}

			/**
			 * @see org.olat.core.gui.control.WindowControl#setInfo(java.lang.String)
			 */
			@Override
			public void setInfo(String info) {
				guiMessage.setInfo(info);
				guimsgPanel.setContent(guimsgVc);
			}

			/**
			 * @see org.olat.core.gui.control.WindowControl#setError(java.lang.String)
			 */
			@Override
			public void setError(String error) {
				guiMessage.setError(error);
				guimsgPanel.setContent(guimsgVc);
			}

			/**
			 * @see org.olat.core.gui.control.WindowControl#setWarning(java.lang.String)
			 */
			@Override
			public void setWarning(String warning) {
				guiMessage.setWarn(warning);
				guimsgPanel.setContent(guimsgVc);
			}

			@Override
			public WindowControlInfo getWindowControlInfo() {
				return wci;
			}

			@Override
			public void makeFlat() {
				throw new AssertException("should never be called!");
			}

			@Override
			public BusinessControl getBusinessControl() {
				return origWCo.getBusinessControl();
			}

			@Override
			public WindowBackOffice getWindowBackOffice() {
				return origWCo.getWindowBackOffice();
			}

		};
		overrideWindowControl(myWControl);

		content = new Panel("content");
		mainVc.put("content", content);

		// panel for modal overlays, placed right after the olat-header-div
		modalPanel = new Panel("ccmodalpanel");
		mainVc.put("modalpanel", modalPanel);

		putInitialPanel(mainVc);
	}

	@Override
	// increase visibility brasato:: fj / pb
	public WindowControl getWindowControl() {
		return myWControl;
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		//
	}

	@Override
	protected void doDispose() {
		if (contentController != null) contentController.dispose();
	}

	private void setGuiStack(GuiStack guiStack) {
		currentGuiStack = guiStack;
		Panel guiStackPanel = currentGuiStack.getPanel();
		content.setContent(guiStackPanel);
		// place for modal dialogs, which are overlayd over the normal layout (using
		// css alpha blending)
		// maybe null if no current modal dialog -> clears the panel
		Panel modalStackP = currentGuiStack.getModalPanel();
		modalPanel.setContent(modalStackP);
	}

	@Override
	public void setContentController(Controller contentController) {
		if (this.contentController != null) throw new AssertException("can only set contentController once!");
		this.contentController = contentController;

		GuiStack gsh = getWindowControl().getWindowBackOffice().createGuiStack(contentController.getInitialComponent());
		setGuiStack(gsh);
	}

}
