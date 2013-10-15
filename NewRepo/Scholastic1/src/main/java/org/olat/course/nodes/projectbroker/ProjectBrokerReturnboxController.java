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

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.ta.ReturnboxController;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;

/**
 * @author Christian Guretzki
 */

public class ProjectBrokerReturnboxController extends ReturnboxController {

	private final Project project;

	/**
	 * Implements a dropbox.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param config
	 * @param node
	 * @param userCourseEnv
	 * @param previewMode
	 */
	public ProjectBrokerReturnboxController(final UserRequest ureq, final WindowControl wControl, final ModuleConfiguration config, final CourseNode node,
			final UserCourseEnvironment userCourseEnv, final boolean previewMode, final Project project) {
		super(ureq, wControl, config, node, userCourseEnv, previewMode, false);
		this.project = project;
		initReturnbox(ureq, wControl, config, node, userCourseEnv, previewMode);
	}

	/**
	 * Return returnbox base-path. e.g. course/<COURSE_ID>/returnbox/<NODE_id>/<USER_NAME>
	 * 
	 * @see org.olat.course.nodes.ta.ReturnboxController#getReturnboxPathFor(org.olat.course.run.environment.CourseEnvironment, org.olat.course.nodes.CourseNode,
	 *      org.olat.core.id.Identity)
	 */
	@Override
	public String getReturnboxPathFor(final CourseEnvironment courseEnv, final CourseNode cNode, final Identity identity) {
		return getReturnboxBasePathForProject(this.project, courseEnv, cNode) + File.separator + identity.getName();
	}

	/**
	 * Return returnbox base-path. e.g. course/<COURSE_ID>/returnbox/<NODE_id> To have the path for certain user you must call method 'getReturnboxPathFor'
	 * 
	 * @param project
	 * @param courseEnv
	 * @param cNode
	 * @return Returnbox path relative to folder root.
	 */
	public static String getReturnboxBasePathForProject(final Project project, final CourseEnvironment courseEnv, final CourseNode node) {
		return getReturnboxPathRelToFolderRoot(courseEnv, node) + File.separator + project.getKey();
	}

}
