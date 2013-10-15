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

package org.olat.course.nodes.projectbroker;

import java.util.Date;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.course.nodes.ProjectBrokerCourseNode;
import org.olat.course.nodes.ms.MSCourseNodeRunController;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.projectbroker.datamodel.Project.EventType;
import org.olat.course.nodes.projectbroker.datamodel.ProjectEvent;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerManagerFactory;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerModuleConfiguration;
import org.olat.course.nodes.ta.DropboxController;
import org.olat.course.nodes.ta.ReturnboxController;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;

/**
 * @author guretzki
 */

public class ProjectFolderController extends BasicController {

	private final ModuleConfiguration config;
	private boolean hasDropbox, hasScoring, hasReturnbox;
	private final VelocityContainer content;
	private DropboxController dropboxController;
	private Controller dropboxEditController;
	private ReturnboxController returnboxController;
	private MSCourseNodeRunController scoringController;

	/**
	 * @param ureq
	 * @param wControl
	 * @param userCourseEnv
	 * @param ne
	 * @param previewMode
	 */
	public ProjectFolderController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne,
			final boolean previewMode, final Project project) {
		super(ureq, wControl);
		this.config = ne.getCourseNode().getModuleConfiguration();
		final ProjectBrokerModuleConfiguration moduleConfig = new ProjectBrokerModuleConfiguration(ne.getCourseNode().getModuleConfiguration());

		content = createVelocityContainer("folder");

		if (ProjectBrokerManagerFactory.getProjectGroupManager().isProjectParticipant(ureq.getIdentity(), project)
				|| ProjectBrokerManagerFactory.getProjectGroupManager().isProjectManagerOrAdministrator(ureq, userCourseEnv.getCourseEnvironment(), project)) {
			content.contextPut("isParticipant", true);
			readConfig(config);
			// modify hasTask/hasDropbox/hasScoring according to accessability
			// TODO:cg 27.01.2010 ProjectBroker does not support assessement-tool in first version
			// if (hasScoring){
			// hasScoring = ne.isCapabilityAccessible("scoring");
			// }
			hasScoring = false;
			// no call 'ne.isCapabilityAccessible(ProjectBrokerCourseNode.ACCESS_DROPBOX);' because no dropbox/returnbox conditions
			if (!hasDropbox && !hasReturnbox) {
				// nothing to show => Show text message no folder
				content.contextPut("noFolder", Boolean.TRUE);
			} else {
				getLogger().debug("isDropboxAccessible(project, moduleConfig)=" + isDropboxAccessible(project, moduleConfig));
				if (ProjectBrokerManagerFactory.getProjectGroupManager().isProjectManager(ureq.getIdentity(), project)) {
					dropboxEditController = new ProjectBrokerDropboxScoringViewController(project, ureq, wControl, ne.getCourseNode(), userCourseEnv);
					content.put("dropboxController", dropboxEditController.getInitialComponent());
					content.contextPut("hasDropbox", Boolean.TRUE);
				} else {
					if (hasDropbox) {
						if (isDropboxAccessible(project, moduleConfig)) {
							dropboxController = new ProjectBrokerDropboxController(ureq, wControl, config, ne.getCourseNode(), userCourseEnv, previewMode, project,
									moduleConfig);
							content.put("dropboxController", dropboxController.getInitialComponent());
							content.contextPut("hasDropbox", Boolean.TRUE);
						} else {
							content.contextPut("hasDropbox", Boolean.FALSE);
							content.contextPut("DropboxIsNotAccessible", Boolean.TRUE);
						}
					}
					if (hasReturnbox) {
						if (!ProjectBrokerManagerFactory.getProjectGroupManager().isProjectManager(ureq.getIdentity(), project)) {
							returnboxController = new ProjectBrokerReturnboxController(ureq, wControl, config, ne.getCourseNode(), userCourseEnv, previewMode, project);
							content.put("returnboxController", returnboxController.getInitialComponent());
							content.contextPut("hasReturnbox", Boolean.TRUE);
						}
					}
				}
				// TODO:cg 27.01.2010 ProjectBroker does not support assessement-tool in first version
				// if (hasScoring && !previewMode) {
				// scoringController = new MSCourseNodeRunController(ureq, getWindowControl(), userCourseEnv, (AssessableCourseNode) ne.getCourseNode(), false);
				// content.put("scoringController", scoringController.getInitialComponent());
				// content.contextPut("hasScoring", Boolean.TRUE);
				// }
			}
			// push title
			content.contextPut("menuTitle", ne.getCourseNode().getShortTitle());
			content.contextPut("displayTitle", ne.getCourseNode().getLongTitle());

			// learning objectives, only visible on intro page: Adding learning objectives
			// TODO: cg 28.01.2010 : no Leaning objective for project-broker
			// String learningObj = ne.getCourseNode().getLearningObjectives();
			// if (learningObj != null) {
			// Component learningObjectives = ObjectivesHelper.createLearningObjectivesComponent(learningObj, ureq);
			// content.put("learningObjectives", learningObjectives);
			// content.contextPut("hasObjectives", learningObj); // dummy value, just an exists operator
			// }
		} else {
			content.contextPut("isParticipant", false);
		}
		putInitialPanel(content);
	}

	private boolean isDropboxAccessible(final Project project, final ProjectBrokerModuleConfiguration moduleConfig) {
		if (moduleConfig.isProjectEventEnabled(EventType.HANDOUT_EVENT)) {
			final ProjectEvent handoutEvent = project.getProjectEvent(EventType.HANDOUT_EVENT);
			final Date now = new Date();
			if (handoutEvent.getStartDate() != null) {
				if (now.before(handoutEvent.getStartDate())) { return false; }
			}
			if (handoutEvent.getEndDate() != null) {
				if (now.after(handoutEvent.getEndDate())) { return false; }
			}
		}
		return true;
	}

	private void readConfig(final ModuleConfiguration modConfig) {
		Boolean bValue = (Boolean) modConfig.get(ProjectBrokerCourseNode.CONF_DROPBOX_ENABLED);
		hasDropbox = (bValue != null) ? bValue.booleanValue() : false;
		bValue = (Boolean) modConfig.get(ProjectBrokerCourseNode.CONF_SCORING_ENABLED);
		hasScoring = (bValue != null) ? bValue.booleanValue() : false;
		bValue = (Boolean) modConfig.get(ProjectBrokerCourseNode.CONF_RETURNBOX_ENABLED);
		hasReturnbox = (bValue != null) ? bValue.booleanValue() : false;

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
		if (dropboxController != null) {
			dropboxController.dispose();
			dropboxController = null;
		}
		if (dropboxEditController != null) {
			dropboxEditController.dispose();
			dropboxEditController = null;
		}
		if (scoringController != null) {
			scoringController.dispose();
			scoringController = null;
		}
		if (returnboxController != null) {
			returnboxController.dispose();
			returnboxController = null;
		}

	}

}
