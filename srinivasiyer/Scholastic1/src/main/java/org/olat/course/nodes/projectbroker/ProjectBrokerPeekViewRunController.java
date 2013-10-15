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
package org.olat.course.nodes.projectbroker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.htmlsite.OlatCmdEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerManagerFactory;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * The projectbroker peekview controller displays the selected and coached projects for certain user.
 * 
 * @author Christian Guretzki
 */
public class ProjectBrokerPeekViewRunController extends BasicController implements Controller {

	private static final int MAX_NBR_PROJECTS = 3;
	private final NodeEvaluation ne;

	/**
	 * Constructor
	 * 
	 * @param ureq The user request
	 * @param wControl The window control
	 */
	public ProjectBrokerPeekViewRunController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		// Use fallback translator from forum
		super(ureq, wControl);
		this.ne = ne;
		final CoursePropertyManager cpm = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		final Long projectBrokerId = ProjectBrokerManagerFactory.getProjectBrokerManager().getProjectBrokerId(cpm, ne.getCourseNode());
		getLogger().debug("projectBrokerId=" + projectBrokerId);
		final VelocityContainer peekviewVC = createVelocityContainer("peekview");
		List<Project> myProjects = null;
		if (projectBrokerId != null) {
			myProjects = ProjectBrokerManagerFactory.getProjectBrokerManager().getProjectsOf(ureq.getIdentity(), projectBrokerId);
		} else {
			// when projectBrokerId is null, created empty project list (course-preview)
			myProjects = new ArrayList<Project>();
		}
		// check nbr of projects and limit it
		if (myProjects.size() > MAX_NBR_PROJECTS) {
			peekviewVC.contextPut("peekviewMoreProjects", "true");
			myProjects = myProjects.subList(0, MAX_NBR_PROJECTS);
		}
		peekviewVC.contextPut("myProjects", myProjects);
		for (final Iterator iterator = myProjects.iterator(); iterator.hasNext();) {
			final Project project = (Project) iterator.next();
			// Add link to show all items (go to node)
			final Link nodeLink = LinkFactory.createLink("nodeLink_" + project.getKey(), peekviewVC, this);
			nodeLink.setCustomDisplayText(project.getTitle());
			nodeLink.setCustomEnabledLinkCSS("o_gotoNode");
			nodeLink.setUserObject(Long.toString(project.getKey().longValue()));
		}

		List<Project> myCoachedProjects = null;
		if (projectBrokerId != null) {
			myCoachedProjects = ProjectBrokerManagerFactory.getProjectBrokerManager().getCoachedProjectsOf(ureq.getIdentity(), projectBrokerId);
		} else {
			// when projectBrokerId is null, created empty project list (course-preview)
			myCoachedProjects = new ArrayList<Project>();
		}
		// check nbr of projects and limit it
		if (myCoachedProjects.size() > MAX_NBR_PROJECTS) {
			peekviewVC.contextPut("peekviewMoreCoachedProjects", "true");
			myCoachedProjects = myCoachedProjects.subList(0, MAX_NBR_PROJECTS);
		}
		peekviewVC.contextPut("myCoachedProjects", myCoachedProjects);
		for (final Iterator iterator = myCoachedProjects.iterator(); iterator.hasNext();) {
			final Project project = (Project) iterator.next();
			// Add link to show all items (go to node)
			final Link nodeLink = LinkFactory.createLink("coachedNodeLink_" + project.getKey(), peekviewVC, this);
			nodeLink.setCustomDisplayText(project.getTitle());
			nodeLink.setCustomEnabledLinkCSS("o_gotoNode");
			nodeLink.setUserObject(Long.toString(project.getKey().longValue()));
		}

		this.putInitialPanel(peekviewVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		if (source instanceof Link) {
			final Link projectLink = (Link) source;
			final String projectId = (String) projectLink.getUserObject();
			if (projectId == null) {
				fireEvent(ureq, new OlatCmdEvent(OlatCmdEvent.GOTONODE_CMD, ne.getCourseNode().getIdent()));
			} else {
				fireEvent(ureq, new OlatCmdEvent(OlatCmdEvent.GOTONODE_CMD, ne.getCourseNode().getIdent() + "/" + projectId));
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing to dispose
	}

}
