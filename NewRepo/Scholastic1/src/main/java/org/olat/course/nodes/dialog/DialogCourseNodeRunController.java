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

package org.olat.course.nodes.dialog;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.course.nodes.DialogCourseNode;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.dialog.DialogElementsController;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description:<br>
 * chief controller for the run mode of the course node 'dialog elements'
 * <P>
 * Initial Date: 02.11.2005 <br>
 * 
 * @author guido
 */
public class DialogCourseNodeRunController extends BasicController {

	public DialogCourseNodeRunController(final UserRequest ureq, final UserCourseEnvironment userCourseEnv, final WindowControl wControl,
			final DialogCourseNode dialogCourseNode, final NodeEvaluation ne) {
		super(ureq, wControl);
		addLoggingResourceable(LoggingResourceable.wrap(dialogCourseNode));

		final Controller dialogCtr = new DialogElementsController(ureq, getWindowControl(), dialogCourseNode, userCourseEnv, ne);
		listenTo(dialogCtr);

		putInitialPanel(dialogCtr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// no events yet
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// child controllers disposed by basic controller
	}

}
