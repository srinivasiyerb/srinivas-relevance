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
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.course.ICourse;
import org.olat.course.nodes.ProjectBrokerCourseNode;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * @author guretzki
 */

public class ProjectBrokerControllerFactory {

	public static ProjectBrokerCourseEditorController createCourseEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course,
			final UserCourseEnvironment euce, final ProjectBrokerCourseNode projectBrokerCourseNode) {
		return new ProjectBrokerCourseEditorController(ureq, wControl, course, projectBrokerCourseNode, course.getCourseEnvironment().getCourseGroupManager(), euce);
	}

	public static Controller createRunController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		return new ProjectListController(ureq, wControl, userCourseEnv, ne, false);
	}

	public static Controller createPreviewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv,
			final NodeEvaluation ne) {
		return new ProjectListController(ureq, wControl, userCourseEnv, ne, true);
	}

	public static Controller createPeekViewRunController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv,
			final NodeEvaluation ne) {
		return new ProjectBrokerPeekViewRunController(ureq, wControl, userCourseEnv, ne);
	}

}
