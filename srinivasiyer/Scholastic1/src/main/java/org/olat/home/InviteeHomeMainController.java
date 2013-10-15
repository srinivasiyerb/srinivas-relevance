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
package org.olat.home;

import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.components.tree.MenuTree;
import org.olat.core.gui.components.tree.TreeModel;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.MainLayoutBasicController;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.core.util.nodes.INode;

/**
 * Description:<br>
 * this a home for invitee because Olat need at least 1 static tab to start.
 * <P>
 * Initial Date: 7 déc. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InviteeHomeMainController extends MainLayoutBasicController {
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(InviteeHomeMainController.class);
	private final MenuTree olatMenuTree;
	private final VelocityContainer welcome;
	private LayoutMain3ColsController columnLayoutCtr;

	/**
	 * Constructor of the guest home main controller
	 * 
	 * @param ureq The user request
	 * @param wControl The window control
	 */
	public InviteeHomeMainController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		olatMenuTree = new MenuTree("olatMenuTree");
		final TreeModel tm = buildTreeModel();
		olatMenuTree.setTreeModel(tm);
		olatMenuTree.setSelectedNodeId(tm.getRootNode().getIdent());
		olatMenuTree.addListener(this);

		welcome = createVelocityContainer("inviteewelcome");

		// Activate correct position in menu
		final INode firstNode = tm.getRootNode().getChildAt(0);
		olatMenuTree.setSelectedNodeId(firstNode.getIdent());

		columnLayoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), olatMenuTree, null, welcome, null);
		listenTo(columnLayoutCtr); // cleanup on dispose
		// add background image to home site
		columnLayoutCtr.addCssClassToMain("o_home");

		putInitialPanel(columnLayoutCtr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == olatMenuTree) {
			// process menu commands
			if (event.getCommand().equals(MenuTree.COMMAND_TREENODE_CLICKED)) {
				final TreeNode selTreeNode = olatMenuTree.getSelectedNode();
				final String cmd = (String) selTreeNode.getUserObject();
				if (cmd.equals("guestinfo")) {
					welcome.setPage(VELOCITY_ROOT + "/guestinfo.html");
				}
			}
		} else {
			Tracing.logWarn("Unhandled olatMenuTree event: " + event.getCommand(), InviteeHomeMainController.class);
		}
	}

	private TreeModel buildTreeModel() {
		GenericTreeNode root, gtn;

		final GenericTreeModel gtm = new GenericTreeModel();
		root = new GenericTreeNode();
		root.setTitle(translate("menu.guest"));
		root.setUserObject("guest");
		root.setAltText(translate("menu.guest.alt"));
		gtm.setRootNode(root);

		gtn = new GenericTreeNode();
		gtn.setTitle(translate("menu.guestinfo"));
		gtn.setUserObject("guestinfo");
		gtn.setAltText(translate("menu.guestinfo.alt"));
		root.addChild(gtn);

		return gtm;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// controller disposed by BasicController
		columnLayoutCtr = null;
	}
}