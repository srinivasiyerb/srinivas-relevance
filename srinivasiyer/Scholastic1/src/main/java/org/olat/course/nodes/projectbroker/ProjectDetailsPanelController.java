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

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.resource.OLATResourceableDeletedEvent;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.projectbroker.datamodel.ProjectBroker;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerManagerFactory;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerModuleConfiguration;
import org.olat.course.run.environment.CourseEnvironment;

/**
 * @author guretzki
 */
public class ProjectDetailsPanelController extends BasicController {

	private final Panel detailsPanel;
	private ProjectEditDetailsFormController editController;
	private ProjectDetailsDisplayController runController;

	private final Project project;
	private final CourseEnvironment courseEnv;
	private final CourseNode courseNode;
	private final ProjectBrokerModuleConfiguration projectBrokerModuleConfiguration;

	private boolean newCreatedProject;
	private final VelocityContainer editVC;
	private LockResult lock;

	/**
	 * @param ureq
	 * @param wControl
	 * @param hpc
	 */
	public ProjectDetailsPanelController(final UserRequest ureq, final WindowControl wControl, final Project project, final boolean newCreatedProject,
			final CourseEnvironment courseEnv, final CourseNode courseNode, final ProjectBrokerModuleConfiguration projectBrokerModuleConfiguration) {
		super(ureq, wControl);
		this.project = project;
		this.courseEnv = courseEnv;
		this.courseNode = courseNode;
		this.projectBrokerModuleConfiguration = projectBrokerModuleConfiguration;
		this.newCreatedProject = newCreatedProject;

		detailsPanel = new Panel("projectdetails_panel");
		runController = new ProjectDetailsDisplayController(ureq, wControl, project, courseEnv, courseNode, projectBrokerModuleConfiguration);
		runController.addControllerListener(this);
		detailsPanel.setContent(runController.getInitialComponent());

		editVC = createVelocityContainer("editProject");
		if (newCreatedProject && ProjectBrokerManagerFactory.getProjectGroupManager().isProjectManagerOrAdministrator(ureq, courseEnv, project)) {
			openEditController(ureq);
		}

		putInitialPanel(detailsPanel);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// nothing to catch
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if ((source == runController) && event.getCommand().equals("switchToEditMode")) {
			if (newCreatedProject) {
				newCreatedProject = false;
			}
			if (editController != null) {
				editController.doDispose();
			}
			openEditController(ureq);
		} else if ((source == editController) && event == Event.DONE_EVENT) {
			// switch back from edit mode to display-mode
			CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lock);
			detailsPanel.popContent();
			if (runController != null) {
				runController.dispose();
			}
			runController = new ProjectDetailsDisplayController(ureq, this.getWindowControl(), project, courseEnv, courseNode, projectBrokerModuleConfiguration);
			runController.addControllerListener(this);
			detailsPanel.setContent(runController.getInitialComponent());
			fireEvent(ureq, Event.CHANGED_EVENT);
		} else if ((source == runController) && (event == Event.BACK_EVENT)) {
			// go back to project-list
			fireEvent(ureq, Event.BACK_EVENT);
		} else if ((source == editController) && (event == Event.CANCELLED_EVENT)) {
			if (newCreatedProject) {
				// from cancelled and go back to project-list
				fireEvent(ureq, new CancelNewProjectEvent(project));
			}
			CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lock);
		}
	}

	private void openEditController(final UserRequest ureq) {
		if (ProjectBrokerManagerFactory.getProjectBrokerManager().existsProject(project.getKey())) {
			final OLATResourceable projectOres = OresHelper.createOLATResourceableInstance(Project.class, project.getKey());
			this.lock = CoordinatorManager.getInstance().getCoordinator().getLocker().acquireLock(projectOres, ureq.getIdentity(), null);
			if (lock.isSuccess()) {
				editController = new ProjectEditDetailsFormController(ureq, this.getWindowControl(), project, courseEnv, courseNode, projectBrokerModuleConfiguration,
						newCreatedProject);
				editController.addControllerListener(this);
				editVC.put("editController", editController.getInitialComponent());
				detailsPanel.pushContent(editVC);
			} else {
				this.showInfo("info.project.already.edit", project.getTitle());
			}
		} else {
			this.showInfo("info.project.nolonger.exist", project.getTitle());
			// fire event to update project list
			final ProjectBroker projectBroker = project.getProjectBroker();
			final OLATResourceableDeletedEvent delEv = new OLATResourceableDeletedEvent(projectBroker);
			CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(delEv, projectBroker);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// child controller sposed by basic controller
		if (lock != null) {
			CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lock);
		}
	}

}
