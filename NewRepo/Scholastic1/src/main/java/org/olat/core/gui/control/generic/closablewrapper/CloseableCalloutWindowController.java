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
package org.olat.core.gui.control.generic.closablewrapper;

import org.apache.commons.lang.StringEscapeUtils;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.winmgr.Command;
import org.olat.core.gui.control.winmgr.JSCommand;
import org.olat.core.logging.OLATRuntimeException;

/**
 * Description:<br>
 * <p>
 * The closeable callout window controller provides an overlay view right next to the target component. The callout can be positioned left, right top or below the target.
 * <p>
 * In non-ajax mode this view will fall back to the CloseableModalController
 * <p>
 * To close this callout window you have to dispose the controller.
 * <p>
 * Events fired by this controller:
 * <ul>
 * <li>CLOSE_WINDOW_EVENT is fired when the callout window has been closed by the user (only possible when the option "closable" is set to true"</li>
 * </ul>
 * <p>
 * Initial Date: 08.07.2010 <br>
 * 
 * @author gnaegi
 */
public class CloseableCalloutWindowController extends BasicController {
	public static final Event CLOSE_WINDOW_EVENT = new Event("CLOSE_WINDOW_EVENT");

	private VelocityContainer calloutVC;
	private CloseableModalController cmc;
	private boolean requiresJSCleanup = false;

	/**
	 * Constructor for a closable callout window controller. After calling the constructor, the callout window will be visible immediately, there is no need to activate
	 * the window. In contrast to the modal dialogs you have to render the content of this controller your self with the getInitialComponent() method.
	 * 
	 * @param ureq The user request
	 * @param wControl The window control
	 * @param calloutWindowContent The component that should be displayed in the callout window
	 * @param targetDomID The DOM ID of the element which the callout should pop up from.
	 * @param title The title or NULL if no title should be used
	 * @param closable true: show close link; false: don't show close link. Note, in any case will the callout window be close when the user clickes on the background.
	 * @param cssClasses CSS classes that should be applied to the callout window or NULL to not use any css classes
	 */
	public CloseableCalloutWindowController(UserRequest ureq, WindowControl wControl, Component calloutWindowContent, String targetDomID, String title, boolean closable,
			String cssClasses) {
		super(ureq, wControl);

		boolean ajax = getWindowControl().getWindowBackOffice().getWindowManager().isAjaxEnabled();
		if (ajax) {
			calloutVC = createVelocityContainer("callout");
			//
			calloutVC.put("calloutWindowContent", calloutWindowContent);
			// Target link
			setDOMTarget(targetDomID);
			// Config options, see setter methods
			calloutVC.contextPut("closable", Boolean.toString(closable));
			calloutVC.contextPut("cssClasses", (cssClasses == null ? "b_small" : cssClasses));
			if (title != null) {
				String escapedTitle = StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(title));
				calloutVC.contextPut("title", escapedTitle);
			}
			//
			putInitialPanel(calloutVC);
		} else {
			// Fallback to old-school modal dialog
			cmc = new CloseableModalController(wControl, "close", calloutWindowContent, true, title, closable);
			putInitialPanel(new Panel("empty"));
		}
	}

	/**
	 * Constructor for a closable callout window controller. After calling the constructor, the callout window will be visible immediately, there is no need to activate
	 * the window. In contrast to the modal dialogs you have to render the content of this controller your self with the getInitialComponent() method.
	 * 
	 * @param ureq The user request
	 * @param wControl The window control
	 * @param calloutWindowContent The component that should be displayed in the callout window
	 * @param targetLink The link which the callout should popup from. The link object is not used to catch any events, it only used to get the DOM ID of the link for
	 *            rendering purpose.
	 */
	public CloseableCalloutWindowController(UserRequest ureq, WindowControl wControl, Component calloutWindowContent, Link targetLink, String title, boolean closable,
			String cssClasses) {
		this(ureq, wControl, calloutWindowContent, "o_lnk" + targetLink.getDispatchID(), title, closable, cssClasses);
	}

	/**
	 * Constructor for a closable callout window controller. After calling the constructor, the callout window will be visible immediately, there is no need to activate
	 * the window. In contrast to the modal dialogs you have to render the content of this controller your self with the getInitialComponent() method.
	 * 
	 * @param ureq The user request
	 * @param wControl The window control
	 * @param calloutWindowContent The component that should be displayed in the callout window
	 * @param targetFormLink The link which the callout should popup from. The link object is not used to catch any events, it only used to get the DOM ID of the link for
	 *            rendering purpose.
	 */
	public CloseableCalloutWindowController(UserRequest ureq, WindowControl wControl, Component calloutWindowContent, FormLink targetFormLink, String title,
			boolean closable, String cssClasses) {
		this(ureq, wControl, calloutWindowContent, "o_fi" + targetFormLink.getComponent().getDispatchID(), title, closable, cssClasses);
	}

	/**
	 * Set the DOM ID of the target element where the callout window should anchored.
	 * 
	 * @param targetDomID
	 */
	public void setDOMTarget(String targetDomID) {
		if (targetDomID == null) throw new OLATRuntimeException("Can not set NULL as target DOM ID", null);
		if (calloutVC != null) {
			// Setting the new target makes this callout VC dirty which redraws
			// the callout window and re-initializes the ExtJS tooltip
			String oldTarget = (String) calloutVC.getContext().get("target");
			if (oldTarget != null && !oldTarget.equals(targetDomID)) {
				// cleanup old window first
				// TODO
			}
			calloutVC.contextPut("target", targetDomID);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if (source == calloutVC) {
			if ("close".equals(event.getCommand())) {
				deactivate();
				// Forward event to parent controller
				fireEvent(ureq, CLOSE_WINDOW_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == cmc) {
			if (event == CloseableModalController.CLOSE_MODAL_EVENT) {
				// Forward event to parent controller
				fireEvent(ureq, CLOSE_WINDOW_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		if (cmc != null) {
			cmc.dispose();
			cmc = null;
		}
		if (requiresJSCleanup) {
			cleanupJSCode();
		}
	}

	/**
	 * Add a controller to this callout controller that should be cleaned up when this callout controller is diposed. In most scenarios you should hold a reference to the
	 * content controllers that controls the calloutWindowContent, but in rare cases this is not the case and you have no local reference to your controller. You can then
	 * use this method to add your controller. At the dispose time of the callout controller your controller will be disposed as well.
	 * 
	 * @param toBedisposedControllerOnDispose
	 */
	public void addDisposableChildController(Controller toBedisposedControllerOnDispose) {
		listenTo(toBedisposedControllerOnDispose);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#getInitialComponent()
	 */
	@Override
	public Component getInitialComponent() {
		throw new RuntimeException("please use activate() instead");
	}

	/**
	 * Call this method to display the callout window now. Use deactivate() to close it later on
	 */
	public void activate() {
		if (cmc != null) {
			// delegate if in non-ajax mode
			cmc.activate();
		} else {
			// push to modal stack
			getWindowControl().pushAsModalDialog(calloutVC);
			requiresJSCleanup = true;
		}
	}

	/**
	 * Call this method to close the callout window and continue with anything else.
	 */
	public void deactivate() {
		if (cmc != null) {
			// Delegate if in non-ajax mode to modal window
			cmc.deactivate();
		} else {
			cleanupJSCode();
			// Remove component from stack
			getWindowControl().pop();
		}
	}

	/**
	 * Cleanup the window and JS code on the browser side
	 */
	private void cleanupJSCode() {
		// Cleanup any resources on the browser/DOM side
		StringBuilder sb = new StringBuilder();
		sb.append("if( Ext.getCmp('callout_o_c").append(calloutVC.getDispatchID()).append("') != null ) {");
		sb.append("  Ext.getCmp('callout_o_c").append(calloutVC.getDispatchID()).append("').destroy();");
		sb.append("};");
		// JS command is sent via OLAT AJAX channel
		Command command = new JSCommand(sb.toString());
		getWindowControl().getWindowBackOffice().sendCommandTo(command);
		//
		requiresJSCleanup = false;
	}

}