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

package org.olat.course.nodes.cp;

import java.io.File;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tree.TreeEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.course.editor.NodeEditController;
import org.olat.course.nodes.CPCourseNode;
import org.olat.course.nodes.TitledWrapperHelper;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.fileresource.FileResourceManager;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.cp.CPDisplayController;
import org.olat.modules.cp.CPManifestTreeModel;
import org.olat.modules.cp.CPUIFactory;
import org.olat.modules.cp.TreeNodeEvent;
import org.olat.repository.RepositoryEntry;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description: <BR/>
 * Run controller for content packaging course nodes
 * <P/>
 * Initial Date: Oct 13, 2004
 * 
 * @author Felix Jost
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class CPRunController extends BasicController implements ControllerEventListener {
	private static final OLog log = Tracing.createLoggerFor(CPRunController.class);

	private final String attrFromStartpage;

	private final ModuleConfiguration config;
	private File cpRoot;
	private final Panel main;
	private Link showCPButton;

	private CPDisplayController cpDispC;
	private final CPCourseNode cpNode;

	// for external menu representation
	private CPManifestTreeModel treeModel;
	private ControllerEventListener treeNodeClickListener;
	private String nodecmd;
	private String selNodeId;
	private final OLATResourceable ores;

	/**
	 * Use this constructor to launch a CP via Repository reference key set in the ModuleConfiguration. On the into page a title and the learning objectives can be
	 * placed.
	 * 
	 * @param config
	 * @param ureq
	 * @param userCourseEnv
	 * @param wControl
	 * @param cpNode
	 */
	public CPRunController(final ModuleConfiguration config, final UserRequest ureq, final UserCourseEnvironment userCourseEnv, final WindowControl wControl,
			final CPCourseNode cpNode, final String nodecmd, final OLATResourceable course) {
		super(ureq, wControl);
		this.nodecmd = nodecmd;
		this.ores = course;
		// assertion to make sure the moduleconfig is valid
		if (!CPEditController.isModuleConfigValid(config)) { throw new AssertException("cprun controller had an invalid module config:" + config.toString()); }
		this.config = config;
		this.cpNode = cpNode;
		this.attrFromStartpage = "fromStartpage:" + cpNode.getIdent();
		Object frmStrtPg = null;
		// REVIEW:pb:2009-07-14:see OLAT-4166 problem with JumpIn no Window available during Constructor call.
		if (Windows.getWindows(ureq) != null && Windows.getWindows(ureq).getWindow(ureq) != null) {
			frmStrtPg = Windows.getWindows(ureq).getWindow(ureq).getAttribute(attrFromStartpage);
		}
		if (frmStrtPg instanceof Boolean && (Boolean) frmStrtPg == Boolean.TRUE) {
			Windows.getWindows(ureq).getWindow(ureq).removeAttribute(attrFromStartpage);
		}
		addLoggingResourceable(LoggingResourceable.wrap(cpNode));

		// jump to either the forum or the folder if the business-launch-path says so.
		final BusinessControl bc = getWindowControl().getBusinessControl();
		final ContextEntry ce = bc.popLauncherContextEntry();
		if (ce != null) { // a context path is left for me
			if (log.isDebug()) {
				log.debug("businesscontrol (for further jumps) would be:" + bc);
			}
			final OLATResourceable popOres = ce.getOLATResourceable();
			if (log.isDebug()) {
				log.debug("OLATResourceable=" + popOres);
			}
			final String typeName = popOres.getResourceableTypeName();
			// typeName format: 'path=/test1/test2/readme.txt'
			// First remove prefix 'path='
			final String path = typeName.substring("path=".length());
			if (path.length() > 0) {
				if (log.isDebug()) {
					log.debug("direct navigation to container-path=" + path);
				}
				this.nodecmd = path;
			}
		}

		main = new Panel("cprunmain");
		doLaunch(ureq);
		putInitialPanel(main);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == showCPButton) { // those must be links
			Windows.getWindows(ureq).getWindow(ureq).setAttribute(attrFromStartpage, Boolean.TRUE);
			fireEvent(ureq, Event.CHANGED_EVENT);
			doLaunch(ureq);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == null) { // external source (from the course at this time being)
			if (event instanceof TreeEvent) {
				cpDispC.switchToPage(ureq, (TreeEvent) event);
			}
		} else if (source == cpDispC && treeNodeClickListener != null && (event instanceof TreeNodeEvent)) {
			// propagate TreeNodeEvent to the listener
			fireEvent(ureq, event);
		}
	}

	private void doLaunch(final UserRequest ureq) {
		if (cpRoot == null) {
			// it is the first time we start the contentpackaging from this instance
			// of this controller.
			// need to be strict when launching -> "true"
			final RepositoryEntry re = CPEditController.getCPReference(config, true);
			if (re == null) { throw new AssertException("configurationkey 'CONFIG_KEY_REPOSITORY_SOFTKEY' of BB CP was missing"); }
			cpRoot = FileResourceManager.getInstance().unzipFileResource(re.getOlatResource());
			// should always exist because references cannot be deleted as long as
			// nodes reference them
			if (cpRoot == null) { throw new AssertException("file of repository entry " + re.getKey() + " was missing"); }
		}
		// else cpRoot is already set (save some db access if the user opens /
		// closes / reopens the cp from the same CPRuncontroller instance)
		boolean activateFirstPage = true;
		if ((nodecmd != null) && !nodecmd.equals("")) {
			activateFirstPage = false;
		}
		cpDispC = CPUIFactory.getInstance().createContentOnlyCPDisplayController(ureq, getWindowControl(), new LocalFolderImpl(cpRoot), activateFirstPage, nodecmd, ores);
		cpDispC.setContentEncoding(getContentEncoding());
		cpDispC.setJSEncoding(getJSEncoding());
		cpDispC.addControllerListener(this);

		main.setContent(cpDispC.getInitialComponent());
		if (isExternalMenuConfigured()) {
			treeModel = cpDispC.getTreeModel();
			treeNodeClickListener = this;
			if (activateFirstPage) {
				selNodeId = cpDispC.getInitialSelectedNodeId();
			} else {
				String uri = nodecmd;
				if (uri.startsWith("/")) {
					uri = uri.substring(1, uri.length());
				}
				selNodeId = cpDispC.getNodeByUri(uri);
			}
		}
	}

	/**
	 * @return true if there is a treemodel and an event listener ready to be used in outside this controller
	 */
	private boolean isExternalMenuConfigured() {
		return (config.getBooleanEntry(NodeEditController.CONFIG_COMPONENT_MENU).booleanValue());
	}

	private String getContentEncoding() {
		final String encoding = (String) config.get(NodeEditController.CONFIG_CONTENT_ENCODING);
		if (!encoding.equals(NodeEditController.CONFIG_CONTENT_ENCODING_AUTO)) { return encoding; }
		return null;
	}

	private String getJSEncoding() {
		final String encoding = (String) config.get(NodeEditController.CONFIG_JS_ENCODING);
		if (!encoding.equals(NodeEditController.CONFIG_JS_ENCODING_AUTO)) { return encoding; }
		return null;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		if (cpDispC != null) {
			cpDispC.dispose();
			cpDispC = null;
		}
	}

	public NodeRunConstructionResult createNodeRunConstructionResult(final UserRequest ureq) {
		NodeRunConstructionResult ncr;
		if (isExternalMenuConfigured()) {
			// integrate it into the olat menu
			final Controller ctrl = TitledWrapperHelper.getWrapper(ureq, getWindowControl(), this, cpNode, "o_cp_icon");
			ncr = new NodeRunConstructionResult(ctrl, treeModel, selNodeId, treeNodeClickListener);
		} else { // no menu to integrate
			final Controller ctrl = TitledWrapperHelper.getWrapper(ureq, getWindowControl(), this, cpNode, "o_cp_icon");
			ncr = new NodeRunConstructionResult(ctrl);
		}
		return ncr;
	}
}