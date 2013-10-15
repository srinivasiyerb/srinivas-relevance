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

package org.olat.course.nodes;

import java.util.List;
import java.util.Map;

import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.sp.SPEditController;
import org.olat.course.nodes.sp.SPPeekviewController;
import org.olat.course.nodes.sp.SPRunController;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.repository.RepositoryEntry;

/**
 * Description:<br>
 * 
 * @author Felix Jost
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class SPCourseNode extends AbstractAccessableCourseNode {

	private static final String TYPE = "sp";

	/**
	 * Default constructor for course node of type single page
	 */
	public SPCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		final SPEditController childTabCntrllr = new SPEditController(getModuleConfiguration(), ureq, wControl, this, course, euce);
		final CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, course.getCourseEnvironment().getCourseGroupManager(), euce,
				childTabCntrllr);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv,
			final NodeEvaluation ne, final String nodecmd) {
		updateModuleConfigDefaults(false);

		final String nodeId = ne.getCourseNode().getIdent();
		// obtain a temporary (as long as the users visits the course) map to store
		// intermediate data
		final Map tmpstoremap = userCourseEnv.getTempMap(this.getClass(), nodeId);

		final VFSContainer container = userCourseEnv.getCourseEnvironment().getCourseFolderContainer();
		final SPRunController runController = new SPRunController(wControl, ureq, tmpstoremap, userCourseEnv, this, container);
		return new NodeRunConstructionResult(runController);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPeekViewRunController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPeekViewRunController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		if (ne.isAtLeastOneAccessible()) {
			final OLATResourceable ores = OresHelper.createOLATResourceableInstance(CourseModule.class, userCourseEnv.getCourseEnvironment().getCourseResourceableId());
			final ModuleConfiguration config = getModuleConfiguration();
			return new SPPeekviewController(ureq, wControl, userCourseEnv, config, ores);
		} else {
			// use standard peekview
			return super.createPeekViewRunController(ureq, wControl, userCourseEnv, ne);
		}
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPreviewController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPreviewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		return createNodeRunConstructionResult(ureq, wControl, userCourseEnv, ne, null).getRunController();
	}

	@Override
	protected String getDefaultTitleOption() {
		return CourseNode.DISPLAY_OPTS_CONTENT;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	@Override
	public StatusDescription isConfigValid() {/*
											 * first check the one click cache
											 */
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		final String file = (String) getModuleConfiguration().get(SPEditController.CONFIG_KEY_FILE);
		final boolean isValid = file != null;
		StatusDescription sd = StatusDescription.NOERROR;
		if (!isValid) {
			// FIXME: refine statusdescriptions by moving the statusdescription
			// generation to the MSEditForm
			final String shortKey = "error.missingfile.short";
			final String longKey = "error.missingfile.long";
			final String[] params = new String[] { this.getShortTitle() };
			final String translPackage = Util.getPackageName(SPEditController.class);
			sd = new StatusDescription(ValidationStatus.ERROR, shortKey, longKey, params, translPackage);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(SPEditController.PANE_TAB_SPCONFIG);
		}
		return sd;

	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public StatusDescription[] isConfigValid(final CourseEditorEnv cev) {
		oneClickStatusCache = null;
		// only here we know which translator to take for translating condition
		// error messages
		final String translatorStr = Util.getPackageName(SPEditController.class);
		final List<StatusDescription> sds = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	@Override
	public boolean needsReferenceToARepositoryEntry() {
		return false;
	}

	/**
	 * Update the module configuration to have all mandatory configuration flags set to usefull default values
	 * 
	 * @param isNewNode true: an initial configuration is set; false: upgrading from previous node configuration version, set default to maintain previous behaviour
	 */
	@Override
	public void updateModuleConfigDefaults(final boolean isNewNode) {
		final ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode) {
			// use defaults for new course building blocks
			config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, false);
			config.setBooleanEntry(SPEditController.CONFIG_KEY_ALLOW_RELATIVE_LINKS, false);
			// new since config version 3
			config.setConfigurationVersion(3);
		} else {
			config.remove(NodeEditController.CONFIG_INTEGRATION);
			final int version = config.getConfigurationVersion();
			if (version < 2) {
				// use values accoring to previous functionality
				config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, Boolean.FALSE.booleanValue());
				config.setBooleanEntry(SPEditController.CONFIG_KEY_ALLOW_RELATIVE_LINKS, Boolean.FALSE.booleanValue());
				config.setConfigurationVersion(2);
			}
			// there was a version 3 but all keys new in this version have been removed
		}
	}

	// Copy from BCCourseNode => Merge together
	// ///////////////////////
	/**
	 * @param courseEnv
	 * @param node
	 * @return the relative folder base path for this folder node
	 */
	// public static String getFoldernodePathRelToFolderBase(CourseEnvironment
	// courseEnv, CourseNode node) {
	// return getFoldernodesPathRelToFolderBase(courseEnv) + "/" +
	// node.getIdent();
	// }
	/**
	 * @param courseEnv
	 * @return the relative folder base path for folder nodes
	 */
	public static String getFoldernodesPathRelToFolderBase(final CourseEnvironment courseEnv) {
		return courseEnv.getCourseBaseContainer().getRelPath() + "/coursefolder";
	}

	/**
	 * Get a named container of a node with the node title as its name.
	 * 
	 * @param node
	 * @param courseEnv
	 * @return
	 */
	public static OlatNamedContainerImpl getNodeFolderContainer(final SPCourseNode node, final CourseEnvironment courseEnv) {
		final String path = getFoldernodesPathRelToFolderBase(courseEnv);
		final OlatRootFolderImpl rootFolder = new OlatRootFolderImpl(path, null);
		final OlatNamedContainerImpl namedFolder = new OlatNamedContainerImpl(node.getShortTitle(), rootFolder);
		return namedFolder;
	}
}
