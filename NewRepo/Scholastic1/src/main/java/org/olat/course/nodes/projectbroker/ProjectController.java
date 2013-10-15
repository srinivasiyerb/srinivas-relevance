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
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerManagerFactory;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerModuleConfiguration;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * @author guretzki
 */

public class ProjectController extends BasicController {

	private final VelocityContainer contentVC;

	private final TabbedPane myTabbedPane;

	private final ProjectDetailsPanelController detailsController;

	private final ProjectFolderController projectFolderController;

	private ProjectGroupController projectGroupController;

	private Link backLink;

	// private InlineEditDetailsFormController inlineEditDetailsFormController;

	/**
	 * @param ureq
	 * @param wControl
	 * @param userCourseEnv
	 * @param ne
	 * @param previewMode
	 */
	public ProjectController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne,
			final Project project, final boolean newCreatedProject, final ProjectBrokerModuleConfiguration projectBrokerModuleConfiguration) {
		super(ureq, wControl);

		contentVC = createVelocityContainer("project");
		contentVC.contextPut("menuTitle", ne.getCourseNode().getShortTitle());

		if (!newCreatedProject) {
			backLink = LinkFactory.createLinkBack(contentVC, this);
		}
		myTabbedPane = new TabbedPane("projectTabbedPane", ureq.getLocale());
		detailsController = new ProjectDetailsPanelController(ureq, wControl, project, newCreatedProject, userCourseEnv.getCourseEnvironment(), ne.getCourseNode(),
				projectBrokerModuleConfiguration);
		detailsController.addControllerListener(this);
		myTabbedPane.addTab(translate("tab.project.details"), detailsController.getInitialComponent());
		projectFolderController = new ProjectFolderController(ureq, wControl, userCourseEnv, ne, false, project);
		myTabbedPane.addTab(translate("tab.project.folder"), projectFolderController.getInitialComponent());
		if (ProjectBrokerManagerFactory.getProjectGroupManager().isProjectManagerOrAdministrator(ureq, userCourseEnv.getCourseEnvironment(), project)) {
			projectGroupController = new ProjectGroupController(ureq, wControl, project, projectBrokerModuleConfiguration);
			myTabbedPane.addTab(translate("tab.project.members"), projectGroupController.getInitialComponent());
		}
		// inlineEditDetailsFormController = new InlineEditDetailsFormController(ureq, wControl, project, newCreatedProject, userCourseEnv.getCourseEnvironment(),
		// ne.getCourseNode(), projectBrokerModuleConfiguration);
		// myTabbedPane.addTab(translate("tab.project.details.inline"), inlineEditDetailsFormController.getInitialComponent());
		contentVC.put("projectTabbedPane", myTabbedPane);
		putInitialPanel(contentVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == backLink) {
			fireEvent(ureq, Event.BACK_EVENT);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest urequest, final Controller source, final Event event) {
		getLogger().debug("event" + event);
		if ((source == detailsController) && (event == Event.CHANGED_EVENT)) {
			if (backLink == null) {
				backLink = LinkFactory.createLinkBack(contentVC, this);
			}
		}
		// pass event
		fireEvent(urequest, event);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {

	}

}
