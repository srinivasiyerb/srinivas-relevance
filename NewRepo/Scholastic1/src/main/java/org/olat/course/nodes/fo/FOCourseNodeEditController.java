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

package org.olat.course.nodes.fo;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.condition.Condition;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.NodeEditController;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.FOCourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseEditorTreeModel;

/**
 * Initial Date: Apr 7, 2004
 * 
 * @author gnaegi
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class FOCourseNodeEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	private static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	static final String[] paneKeys = { PANE_TAB_ACCESSIBILITY };

	private final FOCourseNode foNode;
	private final VelocityContainer myContent;

	private final ConditionEditController readerCondContr, posterCondContr, moderatorCondContr;
	private TabbedPane myTabbedPane;

	/**
	 * Edit controller for form building blocks
	 * 
	 * @param ureq The user request
	 * @param forumNode The forum node
	 * @param course
	 */
	public FOCourseNodeEditController(final UserRequest ureq, final WindowControl wControl, final FOCourseNode forumNode, final ICourse course,
			final UserCourseEnvironment euce) {
		super(ureq, wControl);
		this.foNode = forumNode;

		myContent = this.createVelocityContainer("edit");

		final CourseGroupManager groupMgr = course.getCourseEnvironment().getCourseGroupManager();
		final CourseEditorTreeModel editorModel = course.getEditorTreeModel();
		// Reader precondition
		final Condition readerCondition = foNode.getPreConditionReader();
		readerCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, readerCondition, "readerConditionForm", AssessmentHelper.getAssessableNodes(
				editorModel, forumNode), euce);
		this.listenTo(readerCondContr);
		myContent.put("readerCondition", readerCondContr.getInitialComponent());

		// Poster precondition
		final Condition posterCondition = foNode.getPreConditionPoster();
		posterCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, posterCondition, "posterConditionForm", AssessmentHelper.getAssessableNodes(
				editorModel, forumNode), euce);
		this.listenTo(posterCondContr);
		myContent.put("posterCondition", posterCondContr.getInitialComponent());

		// Moderator precondition
		final Condition moderatorCondition = foNode.getPreConditionModerator();
		moderatorCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, moderatorCondition, "moderatorConditionForm",
				AssessmentHelper.getAssessableNodes(editorModel, forumNode), euce);
		this.listenTo(moderatorCondContr);
		myContent.put("moderatorCondition", moderatorCondContr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest urequest, final Controller source, final Event event) {
		if (source == readerCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = readerCondContr.getCondition();
				foNode.setPreConditionReader(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == posterCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = posterCondContr.getCondition();
				foNode.setPreConditionPoster(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == moderatorCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = moderatorCondContr.getCondition();
				foNode.setPreConditionModerator(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableDefaultController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	@Override
	public void addTabs(final TabbedPane tabbedPane) {
		myTabbedPane = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), myContent);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// child controllers registered with listenTo() get disposed in BasicController
	}

	@Override
	public String[] getPaneKeys() {
		return paneKeys;
	}

	@Override
	public TabbedPane getTabbedPane() {
		return myTabbedPane;
	}
}