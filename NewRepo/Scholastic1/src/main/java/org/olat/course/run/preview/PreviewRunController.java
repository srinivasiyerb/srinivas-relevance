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

package org.olat.course.run.preview;

import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.olat.core.commons.fullWebApp.LayoutMain3ColsPreviewController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.htmlsite.OlatCmdEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.MenuTree;
import org.olat.core.gui.components.tree.TreeEvent;
import org.olat.core.gui.components.tree.TreeModel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.MainLayoutBasicController;
import org.olat.core.id.IdentityEnvironment;
import org.olat.course.condition.Condition;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.navigation.NavigationHandler;
import org.olat.course.run.navigation.NodeClickedRef;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;

/**
 * Description: <br>
 * 
 * @author Felix Jost
 */
public class PreviewRunController extends MainLayoutBasicController {
	private MenuTree luTree;
	private Panel content;

	private NavigationHandler navHandler;
	private UserCourseEnvironment uce;

	private Controller currentNodeController; // the currently open node
	private VelocityContainer detail;
	private TreeModel treeModel;
	private Link configButton;

	/**
	 * Constructor for the run main controller
	 * 
	 * @param ureq The user request
	 * @param wControl The current window controller
	 * @param identEnv
	 * @param cenv
	 */
	public PreviewRunController(final UserRequest ureq, final WindowControl wControl, final IdentityEnvironment identEnv, final CourseEnvironment cenv,
			final String role, final LayoutMain3ColsPreviewController previewLayoutCtr) {
		super(ureq, wControl);
		// set up the components
		luTree = new MenuTree("luTreeRun", this);

		// build up the running structure for this user;
		uce = new UserCourseEnvironmentImpl(identEnv, cenv);
		navHandler = new NavigationHandler(uce, true);

		// evaluate scoring
		uce.getScoreAccounting().evaluateAll();

		// build menu (treemodel)
		final NodeClickedRef nclr = navHandler.evaluateJumpToCourseNode(ureq, getWindowControl(), null, null, null);
		if (!nclr.isVisible()) {
			getWindowControl().setWarning(translate("rootnode.invisible"));
			final VelocityContainer noaccess = createVelocityContainer("noaccess");
			configButton = LinkFactory.createButton("command.config", noaccess, this);
			previewLayoutCtr.setCol3(noaccess);
			return;
		}

		treeModel = nclr.getTreeModel();
		luTree.setTreeModel(treeModel);
		previewLayoutCtr.setCol1(luTree);

		detail = createVelocityContainer("detail");

		configButton = LinkFactory.createButton("command.config", detail, this);

		content = new Panel("building_block_content");
		currentNodeController = nclr.getRunController();
		currentNodeController.addControllerListener(this);
		content.setContent(currentNodeController.getInitialComponent());
		detail.put("content", content);
		detail.contextPut("time",
				DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, ureq.getLocale()).format(new Date(uce.getCourseEnvironment().getCurrentTimeMillis())));
		final CourseGroupManager cgm = uce.getCourseEnvironment().getCourseGroupManager();
		detail.contextPut("groups", assembleNamesFromList(cgm.getAllLearningGroupsFromAllContexts()));
		detail.contextPut("areas", assembleNamesFromList(cgm.getAllAreasFromAllContexts()));
		detail.contextPut("asRole", role);
		previewLayoutCtr.setCol3(detail);

	}

	private String assembleNamesFromList(final List nameList) {
		final StringBuilder sb = new StringBuilder();
		for (final Iterator iter = nameList.iterator(); iter.hasNext();) {
			sb.append((String) iter.next());
			sb.append(',');
		}
		if (sb.length() == 0) {
			return new String();
		} else {
			return sb.substring(0, sb.length() - 1); // truncate last colon
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == luTree) {
			if (event.getCommand().equals(MenuTree.COMMAND_TREENODE_CLICKED)) {
				final TreeEvent tev = (TreeEvent) event;

				// goto node:
				// after a click in the tree, evaluate the model anew, set the tree
				// model anew, and set the selection of the tree again
				final NodeClickedRef nclr = navHandler.evaluateJumpToTreeNode(ureq, getWindowControl(), treeModel, tev, this, null, currentNodeController);
				if (!nclr.isVisible()) {
					getWindowControl().setWarning(translate("warn.notvisible"));
					return;
				}
				if (nclr.isHandledBySubTreeModelListener()) { return; }

				// set the new treemodel
				treeModel = nclr.getTreeModel();
				luTree.setTreeModel(treeModel);

				// set the new tree selection
				luTree.setSelectedNodeId(nclr.getSelectedNodeId());

				// get the controller (in this case it is a preview controller). Dispose only if not already disposed in navHandler.evaluateJumpToTreeNode()
				if (currentNodeController != null && !currentNodeController.isDisposed()) {
					currentNodeController.dispose();
				}
				currentNodeController = nclr.getRunController();

				final CourseNode cn = nclr.getCalledCourseNode();
				final Condition c = cn.getPreConditionVisibility();
				final String visibilityExpr = (c.getConditionExpression() == null ? translate("details.visibility.none") : c.getConditionExpression());
				detail.contextPut("visibilityExpr", visibilityExpr);
				detail.contextPut("coursenode", cn);

				final Component nodeComp = currentNodeController.getInitialComponent();
				content.setContent(nodeComp);
			}
		} else if (source == configButton) {
			fireEvent(ureq, new Event("command.config"));
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == currentNodeController) {
			if (event instanceof OlatCmdEvent) {
				final OlatCmdEvent oe = (OlatCmdEvent) event;
				final String cmd = oe.getCommand();
				if (cmd.equals(OlatCmdEvent.GOTONODE_CMD)) {
					final String subcmd = oe.getSubcommand(); // "69680861018558";
					final CourseNode identNode = uce.getCourseEnvironment().getRunStructure().getNode(subcmd);
					updateTreeAndContent(ureq, identNode);
					oe.accept();
				}
			}
		}
	}

	/**
	 * side-effecty to content and luTree
	 * 
	 * @param ureq
	 * @param calledCourseNode the node to jump to, if null = jump to root node
	 * @return true if the node jumped to is visible
	 */
	private boolean updateTreeAndContent(final UserRequest ureq, final CourseNode calledCourseNode) {
		// build menu (treemodel)
		NodeClickedRef nclr = navHandler.evaluateJumpToCourseNode(ureq, getWindowControl(), calledCourseNode, this, null);
		if (!nclr.isVisible()) {
			// if not root -> fallback to root. e.g. when a direct node jump fails
			if (calledCourseNode != null) {
				nclr = navHandler.evaluateJumpToCourseNode(ureq, getWindowControl(), null, null, null);
			}
			if (!nclr.isVisible()) {
				getWindowControl().setWarning(translate("msg.nodenotavailableanymore"));
				content.setContent(null);
				luTree.setTreeModel(new GenericTreeModel());
				return false;
			}
		}

		treeModel = nclr.getTreeModel();
		luTree.setTreeModel(treeModel);
		final String selNodeId = nclr.getSelectedNodeId();
		luTree.setSelectedNodeId(selNodeId);

		// dispose old node controller
		if (currentNodeController != null) {
			currentNodeController.dispose();
		}
		currentNodeController = nclr.getRunController();
		content.setContent(currentNodeController.getInitialComponent());
		// enableCustomCourseCSS(ureq);
		return true;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		if (currentNodeController != null) {
			currentNodeController.dispose();
			currentNodeController = null;
		}
	}

}