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

package org.olat.course.nodes.bc;

import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.Activateable;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.course.CourseModule;
import org.olat.course.nodes.BCCourseNode;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Initial Date: Apr 22, 2004
 * 
 * @author gnaegi
 */
public class BCCourseNodeRunController extends DefaultController implements Activateable {

	private FolderRunController frc;

	/**
	 * Constructor for a briefcase course building block runtime controller
	 * 
	 * @param ureq
	 * @param userCourseEnv
	 * @param wContr
	 * @param bcCourseNode
	 * @param scallback
	 */
	public BCCourseNodeRunController(final NodeEvaluation ne, final CourseEnvironment courseEnv, final UserRequest ureq, final WindowControl wContr) {
		super(wContr);

		final boolean isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		final boolean isGuestOnly = ureq.getUserSession().getRoles().isGuestOnly();
		// set logger on this run controller
		addLoggingResourceable(LoggingResourceable.wrap(ne.getCourseNode()));

		// offer subscription, but not to guests
		final SubscriptionContext nodefolderSubContext = (isGuestOnly ? null : CourseModule.createSubscriptionContext(courseEnv, ne.getCourseNode()));

		final OlatNamedContainerImpl namedContainer = BCCourseNode.getNodeFolderContainer((BCCourseNode) ne.getCourseNode(), courseEnv);
		final VFSSecurityCallback scallback = new FolderNodeCallback(namedContainer.getRelPath(), ne, isOlatAdmin, isGuestOnly, nodefolderSubContext);
		namedContainer.setLocalSecurityCallback(scallback);
		frc = new FolderRunController(namedContainer, false, true, ureq, getWindowControl());
		setInitialComponent(frc.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// no events to catch
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		if (frc != null) {
			frc.dispose();
			frc = null;
		}

	}

	/**
	 * @see org.olat.core.gui.control.generic.dtabs.Activateable#activate(org.olat.core.gui.UserRequest, java.lang.String)
	 */
	@Override
	public void activate(final UserRequest ureq, final String viewIdentifier) {
		// delegate to real controller
		if (frc != null) {
			frc.activate(ureq, viewIdentifier);
		}

	}
}
