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

package org.olat.course.nodes.ms;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.condition.Condition;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.NodeEditController;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.MSCourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseEditorTreeModel;

/**
 * Initial Date: Jun 16, 2004
 * 
 * @author gnaegi
 */
public class MSCourseNodeEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	public static final String PANE_TAB_CONFIGURATION = "pane.tab.configuration";
	private static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	private static final String[] paneKeys = { PANE_TAB_CONFIGURATION, PANE_TAB_ACCESSIBILITY };

	private final MSCourseNode msNode;
	private final VelocityContainer configurationVC;
	private MSEditFormController modConfigController;

	private final ConditionEditController accessibilityCondContr;
	private TabbedPane myTabbedPane;

	private final boolean hasLogEntries;
	private final Link editScoringConfigButton;

	/**
	 * Constructor for a manual scoring course edit controller
	 * 
	 * @param ureq The user request
	 * @param msNode The manual scoring course node
	 * @param course
	 */
	public MSCourseNodeEditController(final UserRequest ureq, final WindowControl wControl, final MSCourseNode msNode, final ICourse course,
			final UserCourseEnvironment euce) {
		super(ureq, wControl);
		this.msNode = msNode;

		configurationVC = this.createVelocityContainer("edit");
		editScoringConfigButton = LinkFactory.createButtonSmall("scoring.config.enable.button", configurationVC, this);

		final CourseGroupManager groupMgr = course.getCourseEnvironment().getCourseGroupManager();
		final UserNodeAuditManager auditManager = course.getCourseEnvironment().getAuditManager();
		final CourseEditorTreeModel editorModel = course.getEditorTreeModel();

		// Accessibility precondition
		final Condition accessCondition = msNode.getPreConditionAccess();
		accessibilityCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, accessCondition, "accessabilityConditionForm",
				AssessmentHelper.getAssessableNodes(editorModel, msNode), euce);
		this.listenTo(accessibilityCondContr);

		modConfigController = new MSEditFormController(ureq, wControl, msNode.getModuleConfiguration());
		listenTo(modConfigController);
		configurationVC.put("mseditform", modConfigController.getInitialComponent());

		// if there is already user data available, make for read only
		// TODO:chg:a concurrency issues?
		hasLogEntries = auditManager.hasUserNodeLogs(msNode);
		configurationVC.contextPut("hasLogEntries", new Boolean(hasLogEntries));
		if (hasLogEntries) {
			modConfigController.setDisplayOnly(true);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == editScoringConfigButton) {
			modConfigController.setDisplayOnly(false);
			configurationVC.contextPut("isOverwriting", new Boolean(true));
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == accessibilityCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = accessibilityCondContr.getCondition();
				msNode.setPreConditionAccess(cond);
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == modConfigController) {
			if (event == Event.CANCELLED_EVENT) {
				// reset form

				if (modConfigController != null) {
					removeAsListenerAndDispose(modConfigController);
				}
				modConfigController = new MSEditFormController(ureq, getWindowControl(), msNode.getModuleConfiguration());
				listenTo(modConfigController);
				configurationVC.put("mseditform", modConfigController.getInitialComponent());
				if (hasLogEntries) {
					modConfigController.setDisplayOnly(true);
				}
				configurationVC.contextPut("isOverwriting", new Boolean(false));
				return;

			} else if (event == Event.DONE_EVENT) {
				modConfigController.updateModuleConfiguration(msNode.getModuleConfiguration());
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableDefaultController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	@Override
	public void addTabs(final TabbedPane tabbedPane) {
		myTabbedPane = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), accessibilityCondContr.getWrappedDefaultAccessConditionVC(translate("condition.accessibility.title")));
		tabbedPane.addTab(translate(PANE_TAB_CONFIGURATION), configurationVC);
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
