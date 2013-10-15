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

package org.olat.course.nodes.ta;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.ObjectivesHelper;
import org.olat.course.nodes.TACourseNode;
import org.olat.course.nodes.ms.MSCourseNodeRunController;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;

/**
 * Initial Date: 30.08.2004
 * 
 * @author Mike Stock Comment:
 */

public class TACourseNodeRunController extends BasicController {

	private final ModuleConfiguration config;
	private boolean hasTask, hasDropbox, hasScoring, hasSolution;
	private boolean hasReturnbox = false;

	private final VelocityContainer content;
	private TaskController taskController;
	private DropboxController dropboxController;
	private ReturnboxController returnboxController;
	private SolutionController solutionController;
	private MSCourseNodeRunController scoringController;

	/**
	 * @param ureq
	 * @param wControl
	 * @param userCourseEnv
	 * @param ne
	 * @param previewMode
	 */
	public TACourseNodeRunController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne,
			final boolean previewMode) {
		super(ureq, wControl);
		this.config = ne.getCourseNode().getModuleConfiguration();

		readConfig(config);
		// modify hasTask/hasDropbox/hasScoring according to accessability
		if (hasTask) {
			hasTask = ne.isCapabilityAccessible("task");
		}
		if (hasDropbox) {
			hasDropbox = ne.isCapabilityAccessible("dropbox");
		}
		if (hasReturnbox) {
			hasReturnbox = ne.isCapabilityAccessible("returnbox");
		}
		if (hasScoring) {
			hasScoring = ne.isCapabilityAccessible("scoring");
		}
		if (hasSolution) {
			hasSolution = ne.isCapabilityAccessible("solution");
		}

		content = createVelocityContainer("run");
		if (hasTask) {
			taskController = new TaskController(ureq, wControl, config, ne.getCourseNode(), userCourseEnv.getCourseEnvironment());
			content.put("taskController", taskController.getInitialComponent());
			content.contextPut("hasTask", Boolean.TRUE);
		}

		if (hasDropbox) {
			dropboxController = new DropboxController(ureq, wControl, config, ne.getCourseNode(), userCourseEnv, previewMode);
			content.put("dropboxController", dropboxController.getInitialComponent());
			content.contextPut("hasDropbox", Boolean.TRUE);
		}
		if (hasReturnbox) {
			returnboxController = new ReturnboxController(ureq, wControl, config, ne.getCourseNode(), userCourseEnv, previewMode);
			content.put("returnboxController", returnboxController.getInitialComponent());
			content.contextPut("hasReturnbox", Boolean.TRUE);

		}

		if (hasSolution) {
			solutionController = new SolutionController(ureq, wControl, ne.getCourseNode(), userCourseEnv, previewMode);
			content.put("solutionController", solutionController.getInitialComponent());
			content.contextPut("hasSolution", Boolean.TRUE);
		}

		if (hasScoring && !previewMode) {
			scoringController = new MSCourseNodeRunController(ureq, getWindowControl(), userCourseEnv, (AssessableCourseNode) ne.getCourseNode(), false);
			content.put("scoringController", scoringController.getInitialComponent());
			content.contextPut("hasScoring", Boolean.TRUE);
		}

		// push title and learning objectives, only visible on intro page
		content.contextPut("menuTitle", ne.getCourseNode().getShortTitle());
		content.contextPut("displayTitle", ne.getCourseNode().getLongTitle());

		// Adding learning objectives
		final String learningObj = ne.getCourseNode().getLearningObjectives();
		if (learningObj != null) {
			final Component learningObjectives = ObjectivesHelper.createLearningObjectivesComponent(learningObj, ureq);
			content.put("learningObjectives", learningObjectives);
			content.contextPut("hasObjectives", learningObj); // dummy value, just an exists operator
		}

		putInitialPanel(content);
	}

	private void readConfig(final ModuleConfiguration modConfig) {
		Boolean bValue = (Boolean) modConfig.get(TACourseNode.CONF_TASK_ENABLED);
		hasTask = (bValue != null) ? bValue.booleanValue() : false;
		bValue = (Boolean) modConfig.get(TACourseNode.CONF_DROPBOX_ENABLED);
		hasDropbox = (bValue != null) ? bValue.booleanValue() : false;
		bValue = (Boolean) modConfig.get(TACourseNode.CONF_SCORING_ENABLED);
		hasScoring = (bValue != null) ? bValue.booleanValue() : false;
		bValue = (Boolean) modConfig.get(TACourseNode.CONF_SOLUTION_ENABLED);
		hasSolution = (bValue != null) ? bValue.booleanValue() : false;
		bValue = (Boolean) modConfig.get(TACourseNode.CONF_RETURNBOX_ENABLED);
		hasReturnbox = (bValue != null) ? bValue.booleanValue() : hasDropbox;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		if (taskController != null) {
			taskController.dispose();
			taskController = null;
		}
		if (dropboxController != null) {
			dropboxController.dispose();
			dropboxController = null;
		}
		if (scoringController != null) {
			scoringController.dispose();
			scoringController = null;
		}
		if (returnboxController != null) {
			returnboxController.dispose();
			returnboxController = null;
		}
		if (solutionController != null) {
			solutionController.dispose();
			solutionController = null;
		}
	}

}
