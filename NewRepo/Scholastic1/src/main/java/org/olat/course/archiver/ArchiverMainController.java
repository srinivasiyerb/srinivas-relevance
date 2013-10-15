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

package org.olat.course.archiver;

import java.util.Locale;

import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.extensions.ExtManager;
import org.olat.core.extensions.Extension;
import org.olat.core.extensions.action.ActionExtension;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.components.tree.MenuTree;
import org.olat.core.gui.components.tree.TreeModel;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.MainLayoutBasicController;
import org.olat.core.gui.control.generic.tool.ToolController;
import org.olat.core.gui.control.generic.tool.ToolFactory;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.activity.ActionType;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.nodes.DialogCourseNode;
import org.olat.course.nodes.FOCourseNode;
import org.olat.course.nodes.ProjectBrokerCourseNode;
import org.olat.course.nodes.TACourseNode;
import org.olat.course.nodes.WikiCourseNode;
import org.olat.ims.qti.export.CourseQTIArchiveController;

import ch.unizh.campusmgnt.controller.CampusManagementController;

/**
 * Initial Date: May 26, 2004
 * 
 * @author gnaegi
 */
public class ArchiverMainController extends MainLayoutBasicController {
	private static boolean extensionLogged = false;

	private static final String CMD_INDEX = "index";
	private static final String CMD_QTIRESULTS = "qtiresults";
	private static final String CMD_SCOREACCOUNTING = "scoreaccounting";
	private static final String CMD_ARCHIVELOGFILES = "archivelogfiles";
	private static final String CMD_HANDEDINTASKS = "handedintasks";
	private static final String CMD_PROJECTBROKER = "projectbroker";
	private static final String CMD_FORUMS = "forums";
	private static final String CMD_DIALOGS = "dialogs";
	private static final String CMD_WIKIS = "wikis";
	private static final String CMD_BRINGTOGETHER = "bringtogether";

	private final IArchiverCallback archiverCallback;
	private final MenuTree menuTree;
	private final VelocityContainer intro;
	private final Panel main;
	private Controller resC, contentCtr;

	private ToolController toolC;
	private final OLATResourceable ores;

	private final Locale locale;
	private LayoutMain3ColsController columnLayoutCtr;

	/**
	 * Constructor for the archiver main controller. This main controller has several subcontrollers which implement different aspects of data that can be archived in an
	 * OLAT course
	 * 
	 * @param ureq
	 * @param wControl
	 * @param course
	 * @param archiverCallback
	 */
	public ArchiverMainController(final UserRequest ureq, final WindowControl wControl, final OLATResourceable ores, final IArchiverCallback archiverCallback) {
		super(ureq, wControl);
		getUserActivityLogger().setStickyActionType(ActionType.admin);

		this.ores = ores;
		this.archiverCallback = archiverCallback;
		this.locale = ureq.getLocale();

		main = new Panel("archivermain");

		// Intro page, static
		intro = createVelocityContainer("archiver_index");
		main.setContent(intro);

		// Navigation menu
		menuTree = new MenuTree("menuTree");
		final TreeModel tm = buildTreeModel();
		menuTree.setTreeModel(tm);
		menuTree.setSelectedNodeId(tm.getRootNode().getIdent());
		menuTree.addListener(this);

		// Tool and action box
		toolC = ToolFactory.createToolController(getWindowControl());
		listenTo(toolC);
		toolC.addHeader(translate("tool.name"));
		toolC.addLink("cmd.close", translate("command.closearchiver"), null, "b_toolbox_close");

		columnLayoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), menuTree, toolC.getInitialComponent(), main, "course" + ores.getResourceableId());
		listenTo(columnLayoutCtr); // cleanup on dispose
		putInitialPanel(columnLayoutCtr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == menuTree) {
			if (event.getCommand().equals(MenuTree.COMMAND_TREENODE_CLICKED)) { // goto node in edit mode
				final TreeNode selTreeNode = menuTree.getSelectedNode();
				final Object cmd = selTreeNode.getUserObject();
				if (cmd instanceof ActionExtension) {
					launchExtensionController(ureq, cmd);
				} else {
					launchArchiveControllers(ureq, (String) cmd);
				}
			}
		}
		// no events from main
		// no events from intro
	}

	/**
	 * TODO:gs:a add this extension point also to the doku!
	 * 
	 * @param ureq
	 * @param cmd
	 */
	private void launchExtensionController(final UserRequest ureq, final Object cmd) {
		final ActionExtension ae = (ActionExtension) cmd;
		removeAsListenerAndDispose(resC);
		final ICourse course = CourseFactory.loadCourse(ores);
		this.resC = ae.createController(ureq, getWindowControl(), course);
		listenTo(resC);
		main.setContent(this.resC.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == toolC) {
			if (event.getCommand().equals("cmd.close")) {
				fireEvent(ureq, Event.DONE_EVENT);
			}
		}
	}

	/**
	 * Generates the archiver menu
	 * 
	 * @return The generated menu tree model
	 */
	private TreeModel buildTreeModel() {
		GenericTreeNode root, gtn;

		final GenericTreeModel gtm = new GenericTreeModel();
		root = new GenericTreeNode();
		root.setTitle(translate("menu.index"));
		root.setUserObject(CMD_INDEX);
		root.setAltText(translate("menu.index.alt"));
		gtm.setRootNode(root);

		if (archiverCallback.mayArchiveQtiResults()) {
			gtn = new GenericTreeNode();
			gtn.setTitle(translate("menu.qtiresults"));
			gtn.setUserObject(CMD_QTIRESULTS);
			gtn.setAltText(translate("menu.qtiresults.alt"));
			root.addChild(gtn);
		}
		if (archiverCallback.mayArchiveProperties()) {
			gtn = new GenericTreeNode();
			gtn.setTitle(translate("menu.scoreaccounting"));
			gtn.setUserObject(CMD_SCOREACCOUNTING);
			gtn.setAltText(translate("menu.scoreaccounting.alt"));
			root.addChild(gtn);
		}
		if (archiverCallback.mayArchiveHandedInTasks()) {
			gtn = new GenericTreeNode();
			gtn.setTitle(translate("menu.handedintasks"));
			gtn.setUserObject(CMD_HANDEDINTASKS);
			gtn.setAltText(translate("menu.handedintasks.alt"));
			root.addChild(gtn);
		}
		if (archiverCallback.mayArchiveProjectBroker()) {
			gtn = new GenericTreeNode();
			gtn.setTitle(translate("menu.projectbroker"));
			gtn.setUserObject(CMD_PROJECTBROKER);
			gtn.setAltText(translate("menu.projectbroker.alt"));
			root.addChild(gtn);
		}
		if (archiverCallback.mayArchiveLogfiles()) {
			gtn = new GenericTreeNode();
			gtn.setTitle(translate("menu.archivelogfiles"));
			gtn.setUserObject(CMD_ARCHIVELOGFILES);
			gtn.setAltText(translate("menu.archivelogfiles.alt"));
			root.addChild(gtn);
		}
		if (archiverCallback.mayArchiveForums()) {
			gtn = new GenericTreeNode();
			gtn.setTitle(translate("menu.forums"));
			gtn.setUserObject(CMD_FORUMS);
			gtn.setAltText(translate("menu.forums.alt"));
			root.addChild(gtn);
		}
		if (archiverCallback.mayArchiveDialogs()) {
			gtn = new GenericTreeNode();
			gtn.setTitle(translate("menu.dialogs"));
			gtn.setUserObject(CMD_DIALOGS);
			gtn.setAltText(translate("menu.dialogs.alt"));
			root.addChild(gtn);
		}
		if (archiverCallback.mayArchiveWikis()) {
			gtn = new GenericTreeNode();
			gtn.setTitle(translate("menu.wikis"));
			gtn.setUserObject(CMD_WIKIS);
			gtn.setAltText(translate("menu.wikis.alt"));
			root.addChild(gtn);
		}

		// add extension menues
		final ExtManager extm = ExtManager.getInstance();
		final Class extensionPointMenu = this.getClass();
		final int cnt = extm.getExtensionCnt();
		for (int i = 0; i < cnt; i++) {
			final Extension anExt = extm.getExtension(i);
			// check for sites
			final ActionExtension ae = (ActionExtension) anExt.getExtensionFor(extensionPointMenu.getName());
			if (ae != null) {
				gtn = new GenericTreeNode();
				final String menuText = ae.getActionText(locale);
				gtn.setTitle(menuText);
				gtn.setUserObject(ae);
				gtn.setAltText(ae.getDescription(locale));
				root.addChild(gtn);
				// inform only once
				if (!extensionLogged) {
					extensionLogged = true;
					extm.inform(extensionPointMenu, anExt, "added menu entry (for locale " + locale.toString() + " '" + menuText + "'");
				}
			}
		}

		return gtm;
	}

	private void launchArchiveControllers(final UserRequest ureq, final String menuCommand) {
		if (menuCommand.equals(CMD_INDEX)) {
			main.setContent(intro);
		} else {
			removeAsListenerAndDispose(contentCtr);
			if (menuCommand.equals(CMD_QTIRESULTS)) {
				this.contentCtr = new CourseQTIArchiveController(ureq, getWindowControl(), ores);
				main.setContent(contentCtr.getInitialComponent());
			} else if (menuCommand.equals(CMD_SCOREACCOUNTING)) {
				this.contentCtr = new ScoreAccountingArchiveController(ureq, getWindowControl(), ores);
				main.setContent(contentCtr.getInitialComponent());
			} else if (menuCommand.equals(CMD_ARCHIVELOGFILES)) {
				this.contentCtr = new CourseLogsArchiveController(ureq, getWindowControl(), ores);
				main.setContent(contentCtr.getInitialComponent());
			} else if (menuCommand.equals(CMD_HANDEDINTASKS)) { // TACourseNode
				this.contentCtr = new GenericArchiveController(ureq, getWindowControl(), ores, new TACourseNode());
				main.setContent(contentCtr.getInitialComponent());
			} else if (menuCommand.equals(CMD_PROJECTBROKER)) {
				this.contentCtr = new GenericArchiveController(ureq, getWindowControl(), ores, new ProjectBrokerCourseNode());
				main.setContent(contentCtr.getInitialComponent());
			} else if (menuCommand.equals(CMD_FORUMS)) {
				this.contentCtr = new GenericArchiveController(ureq, getWindowControl(), ores, new FOCourseNode());
				main.setContent(contentCtr.getInitialComponent());
			} else if (menuCommand.equals(CMD_DIALOGS)) {
				this.contentCtr = new GenericArchiveController(ureq, getWindowControl(), ores, new DialogCourseNode());
				main.setContent(contentCtr.getInitialComponent());
			} else if (menuCommand.equals(CMD_WIKIS)) {
				this.contentCtr = new GenericArchiveController(ureq, getWindowControl(), ores, new WikiCourseNode());
				main.setContent(contentCtr.getInitialComponent());
			} else if (menuCommand.equals(CMD_BRINGTOGETHER)) {
				this.contentCtr = new CampusManagementController(ureq, getWindowControl(), ores);
				main.setContent(contentCtr.getInitialComponent());
			}
			listenTo(contentCtr);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// controllers disposed by BasicController:
		columnLayoutCtr = null;
		toolC = null;
		resC = null;
		contentCtr = null;
	}
}
