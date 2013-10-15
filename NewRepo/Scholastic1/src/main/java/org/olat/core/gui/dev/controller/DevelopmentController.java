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
 * Copyright (c) 2005-2006 by JGS goodsolutions GmbH, Switzerland<br>
 * http://www.goodsolutions.ch All rights reserved.
 * <p>
 */
package org.olat.core.gui.dev.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.Container;
import org.olat.core.gui.components.Window;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.util.ComponentUtil;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.floatingresizabledialog.FloatingResizableDialogController;
import org.olat.core.gui.control.generic.spacesaver.ExpColController;
import org.olat.core.gui.control.winmgr.WindowBackOfficeImpl;
import org.olat.core.gui.control.winmgr.WindowManagerImpl;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.intercept.DebugHelper;
import org.olat.core.gui.util.bandwidth.SlowBandWidthSimulator;

/**
 * Description:<br>
 * <P>
 * Initial Date: 20.05.2006 <br>
 * 
 * @author Felix Jost
 */
public class DevelopmentController extends BasicController {
	private VelocityContainer myContent;
	private Panel mainpanel;

	private final WindowBackOfficeImpl wboImpl;
	private ExpColController spacesaverController;
	private Component mainComp;

	private Link web10Link;
	private Link web20Link;
	private Link web20hlLink;
	private Link web21Link;

	private Link showJson;
	private Link showComponentTree;

	private List<Link> modes = new ArrayList<Link>();

	private Link chosenMode;
	private WindowManagerImpl winMgrImpl;
	private Link debugLink;
	private boolean treeShown = false;

	private int pageCnt = 0; // only for visual indication

	// fast polling while developing: no more need to push browser reload!
	private Link toggleAutorefresh;
	private boolean autorefresh = false;
	private Controller floatCtr;
	private Controller bandwithController;
	private Link devToolLink;

	/**
	 * @param ureq
	 * @param wControl
	 * @param navElem
	 */
	public DevelopmentController(UserRequest ureq, WindowControl wControl, WindowBackOfficeImpl wboImpl) {
		super(ureq, wControl);
		this.wboImpl = wboImpl;
		this.winMgrImpl = wboImpl.getWinmgrImpl();

		// set up the main layout
		myContent = createVelocityContainer("index");

		// create four links to switch between modes.
		// a special case here: these link must work in regular mode (normal uri with full screen refresh (as
		// opposed to partial page refresh )in order to switch modes correctly.
		// (grouping only needed for coloring)
		modes.add(web10Link = LinkFactory.deAjaxify(LinkFactory.createButtonSmall("web10", myContent, this)));
		modes.add(web20Link = LinkFactory.deAjaxify(LinkFactory.createButtonSmall("web20", myContent, this)));
		modes.add(web20hlLink = LinkFactory.deAjaxify(LinkFactory.createButtonSmall("web20hl", myContent, this)));
		modes.add(web21Link = LinkFactory.deAjaxify(LinkFactory.createButtonSmall("web21", myContent, this)));
		modes.add(debugLink = LinkFactory.deAjaxify(LinkFactory.createButtonSmall("debug", myContent, this)));
		modes.add(showJson = LinkFactory.deAjaxify(LinkFactory.createButtonSmall("showJson", myContent, this)));

		// commands
		showComponentTree = LinkFactory.deAjaxify(LinkFactory.createButton("showComponentTree", myContent, this));
		myContent.contextPut("compdump", "");
		// boolean iframepost = wboImpl.getGlobalSettings().getAjaxFlags().isIframePostEnabled();
		myContent.contextPut("sys", this);

		toggleAutorefresh = LinkFactory.createButtonSmall("toggleAutorefresh", myContent, this);
		// do it with web 1.0 full page reload timer
		myContent.contextPut("autorefresh", "false");

		// slow bandwidth simulation
		SlowBandWidthSimulator sbs = Windows.getWindows(ureq).getSlowBandWidthSimulator();
		bandwithController = sbs.createAdminGUI().createController(ureq, getWindowControl());
		myContent.put("bandwidth", bandwithController.getInitialComponent());

		mainpanel = new Panel("developermainpanel");
		Component protectedMainPanel = DebugHelper.createDebugProtectedWrapper(mainpanel);

		devToolLink = LinkFactory.createCustomLink("devTool", "devTool", "", Link.NONTRANSLATED, myContent, this);
		devToolLink.setCustomEnabledLinkCSS("b_dev");
		devToolLink.setTitle(translate("devTool"));
		spacesaverController = new ExpColController(ureq, getWindowControl(), false, protectedMainPanel, devToolLink);

		mainComp = DebugHelper.createDebugProtectedWrapper(spacesaverController.getInitialComponent());
		putInitialPanel(mainComp);
	}

	/**
	 * [used by velocity]
	 * 
	 * @return
	 */
	public String time() {
		return "" + System.currentTimeMillis();
	}

	/**
	 * [used by velocity]
	 * 
	 * @return a hex color
	 */

	public String modthree() {
		int n = ++pageCnt % 3;
		switch (n) {
			case 0:
				return "FF0000";
			case 1:
				return "00FF00";
			case 2:
				return "0000FF";
			default:
				return "n/a"; // cannot happen
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == devToolLink) {
			removeAsListenerAndDispose(floatCtr);
			floatCtr = new FloatingResizableDialogController(ureq, getWindowControl(), myContent, "Brasato Development Tool", 1000, 200, 10, 60, true, true);
			listenTo(floatCtr);
			mainpanel.setContent(floatCtr.getInitialComponent());
		} else if (source == web10Link) {
			// choose regular mode
			winMgrImpl.setShowDebugInfo(false);
			winMgrImpl.setAjaxEnabled(false);
			winMgrImpl.setForScreenReader(false);
			winMgrImpl.setHighLightingEnabled(false);
			winMgrImpl.setShowJSON(false);
			winMgrImpl.setIdDivsForced(false);
			chosenMode = web10Link;
			updateUI();
		} else if (source == web20Link) {
			// enable ajax / generic-dom-replacement GDR mode
			winMgrImpl.setShowDebugInfo(false);
			winMgrImpl.setAjaxEnabled(true);
			winMgrImpl.setForScreenReader(false);
			winMgrImpl.setHighLightingEnabled(false);
			winMgrImpl.setShowJSON(false);
			winMgrImpl.setIdDivsForced(false);
			chosenMode = web20Link;
			updateUI();
		} else if (source == web20hlLink) {
			// ajax mode with highlighting
			winMgrImpl.setShowDebugInfo(false);
			winMgrImpl.setAjaxEnabled(true);
			winMgrImpl.setForScreenReader(false);
			winMgrImpl.setHighLightingEnabled(true);
			winMgrImpl.setShowJSON(false);
			// brasato:: setIdDivsForced is removed!! check if it works
			winMgrImpl.setIdDivsForced(false);
			chosenMode = web20hlLink;
			updateUI();
		} else if (source == web21Link) {
			// enable screenreader support:
			// - different html templates where appropriate.
			// - different Component-renderers where appropriate.
			// - mark changed components with jump-marker and allow usage of accesskey
			winMgrImpl.setShowDebugInfo(false);
			winMgrImpl.setAjaxEnabled(false);
			winMgrImpl.setForScreenReader(true);
			winMgrImpl.setHighLightingEnabled(false);
			winMgrImpl.setShowJSON(false);
			winMgrImpl.setIdDivsForced(false);
			chosenMode = web21Link;
			updateUI();
		} else if (source == debugLink) {
			// debug mode requires web 1.0 mode at the moment
			winMgrImpl.setShowDebugInfo(true);
			winMgrImpl.setAjaxEnabled(false);
			winMgrImpl.setForScreenReader(false);
			winMgrImpl.setHighLightingEnabled(false);
			winMgrImpl.setShowJSON(false);
			winMgrImpl.setIdDivsForced(false);
			chosenMode = debugLink;
		} else if (source == showComponentTree) {
			if (treeShown) {
				// hide component tree
				myContent.contextPut("compdump", "");
				winMgrImpl.setIdDivsForced(false);
			} else {
				winMgrImpl.setIdDivsForced(true);
				updateComponentTree();
			}
			treeShown = !treeShown;
		} else if (source == showJson) {
			winMgrImpl.setShowDebugInfo(false);
			winMgrImpl.setAjaxEnabled(true);
			winMgrImpl.setForScreenReader(false);
			winMgrImpl.setHighLightingEnabled(true);
			winMgrImpl.setShowJSON(true);
			winMgrImpl.setIdDivsForced(false);
			chosenMode = showJson;
			updateUI();
		} else if (source == toggleAutorefresh) {
			autorefresh = !autorefresh;
			if (autorefresh) {
				myContent.contextPut("autorefresh", "true");
			} else {
				myContent.contextPut("autorefresh", "false");
			}
		} else if (event == ComponentUtil.VALIDATE_EVENT) {
			// todo update mode
			if (treeShown) {
				updateComponentTree();
			}
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == floatCtr) {
			if (event.equals(Event.DONE_EVENT)) {
				spacesaverController.toggleUi();
			}
		}
	}

	private void updateComponentTree() {
		Window win = wboImpl.getWindow();
		StringOutput sb = new StringOutput();
		renderDebugInfo(win.getContentPane(), sb, true);
		myContent.contextPut("compdump", sb.toString());
	}

	private void updateUI() {
		// update mode.
		for (Link li : modes) {
			li.setCustomEnabledLinkCSS("o_main_button");
			li.setEnabled(true);
		}
		// (chosenMode.setCustomEnabledLinkCSS("o_main_button_sel");
		chosenMode.setEnabled(false);
		chosenMode.setCustomDisabledLinkCSS("o_dbg_button_sel");
		myContent.contextPut("compdump", "");

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// floatCtr auto disposed by basic controller
		if (spacesaverController != null) spacesaverController.dispose();
		if (bandwithController != null) bandwithController.dispose();
	}

	/**
	 * used by velocityrenderdecorator
	 * 
	 * @param target
	 */
	private void renderDebugInfo(Component root, StringOutput target, boolean showDebugInfo) {
		target.append("<div>");
		int cnt = cntTree(root);
		int size = DefaultController.getControllerCount();
		target.append("<strong>Component Tree:</strong> count: " + cnt + "&nbsp;&nbsp;|&nbsp;&nbsp;Controllers (global: active and not disposed): <strong>" + size
				+ "</strong>");
		target.append("</div><div>");
		Map<Controller, List<Component>> controllerInfos = new HashMap<Controller, List<Component>>();
		dumpTree(target, root, 0, controllerInfos);
		target.append("</div>");
		// now dump the controller info
		for (Controller controller : controllerInfos.keySet()) {
			try {
				Component initComp = controller.getInitialComponent();
				target.append("<div style=\"padding-bottom:2px; \"><strong>Controller " + controller.getClass().getName() + " :" + controller.hashCode());
				appendDivCodeForComponent("<i>Initial Component:</i> ", target, initComp, 20);
				List<Component> listenTo = controllerInfos.get(controller);
				for (Component component : listenTo) {
					appendDivCodeForComponent("", target, component, 20);
				}
				target.append("</strong></div><br />");
			} catch (Exception e) {
				// some components like window dont like being called for the initialcomponent
				// -> ignore
			}
		}
	}

	private void appendDivCodeForComponent(String pre, StringOutput sb, Component current, int marginLeft) {
		String pcid = Renderer.getComponentPrefix(current);
		sb.append("<div");
		if (current.isVisible() && current.isDomReplaceable()) {
			sb.append(" onMouseOver=\"this.style.background='#FFF';o_dbg_mark('").append(pcid).append("')\" onMouseOut=\"this.style.background='';o_dbg_unmark('")
					.append(pcid).append("')\"");
		}
		sb.append(" style=\"color:blue; padding-bottom:2px; font-size:10px\"><div style=\"margin-left:" + marginLeft + "px\">");

		String cname = current.getClass().getName();
		cname = cname.substring(cname.lastIndexOf('.') + 1);
		sb.append(pre + "<b>" + cname + "</b> (" + current.getComponentName() + " id " + current.getDispatchID() + ") ");
		sb.append((current.isVisible() ? "" : "INVISIBLE ") + (current.isEnabled() ? "" : " NOT ENABLED ") + current.getExtendedDebugInfo() + ", "
				+ current.getListenerInfo() + "<br />");
		sb.append("</div></div>");
	}

	private void dumpTree(StringOutput sb, Component current, int indent, Map<Controller, List<Component>> controllerInfos) {
		// add infos,
		Controller lController = org.olat.core.gui.dev.Util.getListeningControllerFor(current);
		if (lController != null) {
			List<Component> lcomps = controllerInfos.get(lController);
			if (lcomps == null) {
				// first entry
				lcomps = new ArrayList<Component>();
				controllerInfos.put(lController, lcomps);
			}
			lcomps.add(current);
		}

		int pxInd = indent * 25;
		String pcid = Renderer.getComponentPrefix(current);
		sb.append("<div");
		if (current.isVisible() && current.isDomReplaceable()) {
			sb.append(" onMouseOver=\"this.style.background='#FFF';o_dbg_mark('").append(pcid).append("')\" onMouseOut=\"this.style.background='';o_dbg_unmark('")
					.append(pcid).append("')\"");
		}
		sb.append(" style=\"color:blue; padding-bottom:2px; font-size:10px\"><div style=\"margin-left:" + pxInd + "px\">");

		String cname = current.getClass().getName();
		cname = cname.substring(cname.lastIndexOf('.') + 1);

		sb.append("<b>" + cname + "</b> (" + current.getComponentName() + " id " + current.getDispatchID() + ") ");
		if (current == mainComp) { // suppress detail and subtree for our controller here
			sb.append(" --suppressing output, since developmentcontroller --</div></div>");
		} else {
			sb.append((current.isVisible() ? "" : "INVISIBLE ") + (current.isEnabled() ? "" : " NOT ENABLED ") + current.getExtendedDebugInfo() + ", "
					+ current.getListenerInfo() + "<br />");
			sb.append("</div></div>");
			if (current instanceof Container) {
				Container co = (Container) current;
				Map children = co.getComponents();
				for (Iterator iter = children.values().iterator(); iter.hasNext();) {
					Component child = (Component) iter.next();
					dumpTree(sb, child, indent + 1, controllerInfos);
				}
			}
		}
	}

	private int cntTree(Component current) {
		int cnt = 1;
		if (current instanceof Container) {
			Container co = (Container) current;
			Map children = co.getComponents();
			for (Iterator iter = children.values().iterator(); iter.hasNext();) {
				Component child = (Component) iter.next();
				cnt += cntTree(child);
			}
		}
		return cnt;
	}

}

class ControllerInfo {
	private Controller controller;
	private List<Component> listeningTo;

	ControllerInfo(Controller controller) {
		this.controller = controller;
		listeningTo = new ArrayList<Component>();
	}

	void addListeningComponent(Component listener) {
		listeningTo.add(listener);
	}

}
