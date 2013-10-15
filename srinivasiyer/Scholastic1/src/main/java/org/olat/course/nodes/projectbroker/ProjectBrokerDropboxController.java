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

import java.io.File;
import java.util.Date;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.projectbroker.datamodel.Project.EventType;
import org.olat.course.nodes.projectbroker.datamodel.ProjectEvent;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerModuleConfiguration;
import org.olat.course.nodes.ta.DropboxController;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;

/**
 * @author Christian Guretzki
 */

public class ProjectBrokerDropboxController extends DropboxController {

	private final Project project;
	private final ProjectBrokerModuleConfiguration moduleConfig;

	public ProjectBrokerDropboxController(final UserRequest ureq, final WindowControl wControl, final ModuleConfiguration config, final CourseNode node,
			final UserCourseEnvironment userCourseEnv, final boolean previewMode, final Project project, final ProjectBrokerModuleConfiguration moduleConfig) {
		super(ureq, wControl);
		this.config = config;
		this.node = node;
		this.userCourseEnv = userCourseEnv;
		this.project = project;
		this.moduleConfig = moduleConfig;
		init(ureq, wControl, previewMode, false);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (isDropboxAccessible(project, moduleConfig)) {
			super.event(ureq, source, event);
		} else {
			getLogger().debug("Dropbos is no longer accessible");
			this.showInfo("dropbox.is.not.accessible");
		}
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

	/**
	 * Return dropbox base-path. e.g. course/<COURSE_ID>/dropbox/<NODE_id>/<USER_NAME>
	 * 
	 * @see org.olat.course.nodes.ta.DropboxController#getRelativeDropBoxFilePath(org.olat.core.id.Identity)
	 */
	@Override
	protected String getRelativeDropBoxFilePath(final Identity identity) {
		return getDropboxBasePathForProject(this.project, userCourseEnv.getCourseEnvironment(), node) + File.separator + identity.getName();
	}

	/**
	 * Return dropbox base-path. e.g. course/<COURSE_ID>/dropbox/<NODE_id> To have the path for certain user you must call method 'getRelativeDropBoxFilePath'
	 * 
	 * @param project
	 * @param courseEnv
	 * @param cNode
	 * @return
	 */
	public static String getDropboxBasePathForProject(final Project project, final CourseEnvironment courseEnv, final CourseNode cNode) {
		return getDropboxPathRelToFolderRoot(courseEnv, cNode) + File.separator + project.getKey();
	}
}
