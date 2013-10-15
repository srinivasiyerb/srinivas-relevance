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

package org.olat.core.commons.controllers.linkchooser;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.util.vfs.VFSContainer;

/**
 * Offer tabbed pane with certain type of link chooser. Supports : File-chooser and internal-link chooser. The internal-link chooser can be disabled (when
 * internalLinkTreeModel is null).
 * 
 * @author Christian Guretzki
 */
public class LinkChooserController extends BasicController {

	private VelocityContainer tabbedPaneViewVC, closeVC;
	private Panel mainPanel;

	private TabbedPane linkChooserTabbedPane;
	private FileLinkChooserController fileLinkChooserController;
	private CustomLinkChooserController courseLinkChooserController;
	private CustomMediaChooserController customMediaChooserCtr;

	/**
	 * @param ureq
	 * @param wControl
	 * @param rootDir Root directory for file-chooser.
	 * @param uploadRelPath The relative path within the rootDir where uploaded files should be put into. If NULL, the root Dir is used
	 * @param suffixes Supported file suffixes for file-chooser.
	 * @param fileName Base file-path for file-chooser.
	 * @param userActivityLogger
	 * @param internalLinkTreeModel Model with internal links e.g. course-node tree model. The internal-link chooser tab won't be shown when the internalLinkTreeModel is
	 *            null.
	 */
	public LinkChooserController(UserRequest ureq, WindowControl wControl, VFSContainer rootDir, String uploadRelPath, String[] suffixes, String fileName,
			CustomLinkTreeModel customLinkTreeModel) {
		super(ureq, wControl);

		tabbedPaneViewVC = createVelocityContainer("linkchooser");

		linkChooserTabbedPane = new TabbedPane("linkChooserTabbedPane", ureq.getLocale());
		tabbedPaneViewVC.put("linkChooserTabbedPane", linkChooserTabbedPane);

		fileLinkChooserController = new FileLinkChooserController(ureq, wControl, rootDir, uploadRelPath, suffixes, fileName);
		listenTo(fileLinkChooserController);
		linkChooserTabbedPane.addTab(translate("linkchooser.tabbedpane.label.filechooser"), fileLinkChooserController.getInitialComponent());

		if (customLinkTreeModel != null) {
			courseLinkChooserController = new CustomLinkChooserController(ureq, wControl, customLinkTreeModel);
			listenTo(courseLinkChooserController);
			linkChooserTabbedPane.addTab(translate("linkchooser.tabbedpane.label.internallinkchooser"), courseLinkChooserController.getInitialComponent());
		}

		// try to add custom media chooser from spring configuration.
		// This one will be added as additional tab.
		if (CoreSpringFactory.containsBean(CustomMediaChooserController.class.getName())) {
			CustomMediaChooserController customMediaChooserFactory = (CustomMediaChooserController) CoreSpringFactory.getBean(CustomMediaChooserController.class
					.getName());
			customMediaChooserCtr = customMediaChooserFactory.getInstance(ureq, wControl, rootDir, suffixes, fileName);
			if (customMediaChooserCtr != null) {
				listenTo(customMediaChooserCtr);
				linkChooserTabbedPane.addTab(customMediaChooserCtr.getTabbedPaneTitle(), customMediaChooserCtr.getInitialComponent());
			}
		}
		mainPanel = putInitialPanel(tabbedPaneViewVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (event instanceof URLChoosenEvent) {
			// send choosen URL to parent window via JavaScript and close the window
			URLChoosenEvent urlChoosenEvent = (URLChoosenEvent) event;
			closeVC = createVelocityContainer("close");
			String url = urlChoosenEvent.getURL();
			closeVC.contextPut("isJsUrl", Boolean.FALSE);
			if (url.contains("gotonode")) {
				closeVC.contextPut("isJsUrl", Boolean.TRUE);
			}
			closeVC.contextPut("imagepath", url);
			mainPanel.setContent(closeVC);

		} else if (event == Event.CANCELLED_EVENT) {
			// Close the window, no URL selected
			closeVC = createVelocityContainer("close");
			closeVC.contextPut("imagepath", "");
			mainPanel.setContent(closeVC);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// controllers disposed by basic controller
	}
}