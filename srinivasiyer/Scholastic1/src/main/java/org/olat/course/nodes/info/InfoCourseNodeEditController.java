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
 * Copyright (c) 2008 frentix GmbH, Switzerland<br>
 * <p>
 */

package org.olat.course.nodes.info;

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
import org.olat.course.nodes.InfoCourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseEditorTreeModel;
import org.olat.modules.ModuleConfiguration;

/**
 * Description:<br>
 * Edit the configuration of the info message course node
 * <P>
 * Initial Date: 3 aug. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoCourseNodeEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {
	private static final String PANE_TAB_CONFIG = "pane.tab.infos_config";
	private static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	private static final String[] paneKeys = { PANE_TAB_ACCESSIBILITY, PANE_TAB_CONFIG };

	private final InfoCourseNode courseNode;

	private TabbedPane myTabbedPane;
	private final VelocityContainer configContent;
	private final InfoConfigForm infoConfigForm;
	private final VelocityContainer editAccessVc;
	private final ConditionEditController accessCondContr;
	private final ConditionEditController editCondContr;
	private final ConditionEditController adminCondContr;

	public InfoCourseNodeEditController(final UserRequest ureq, final WindowControl wControl, final ModuleConfiguration config, final InfoCourseNode courseNode,
			final ICourse course, final UserCourseEnvironment euce) {
		super(ureq, wControl);

		this.courseNode = courseNode;

		infoConfigForm = new InfoConfigForm(ureq, wControl, config);
		listenTo(infoConfigForm);

		editAccessVc = createVelocityContainer("edit_access");
		final CourseGroupManager groupMgr = course.getCourseEnvironment().getCourseGroupManager();
		final CourseEditorTreeModel editorModel = course.getEditorTreeModel();
		// Accessibility precondition
		final Condition accessCondition = courseNode.getPreConditionAccess();
		accessCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, accessCondition, "accessConditionForm", AssessmentHelper.getAssessableNodes(
				editorModel, courseNode), euce);
		listenTo(accessCondContr);
		editAccessVc.put("readerCondition", accessCondContr.getInitialComponent());

		// read / write preconditions
		final Condition editCondition = courseNode.getPreConditionEdit();
		editCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, editCondition, "editConditionForm", AssessmentHelper.getAssessableNodes(
				editorModel, courseNode), euce);
		listenTo(editCondContr);
		editAccessVc.put("editCondition", editCondContr.getInitialComponent());

		// administration preconditions
		final Condition adminCondition = courseNode.getPreConditionAdmin();
		adminCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, adminCondition, "adminConditionForm", AssessmentHelper.getAssessableNodes(
				editorModel, courseNode), euce);
		listenTo(adminCondContr);
		editAccessVc.put("adminCondition", adminCondContr.getInitialComponent());

		configContent = createVelocityContainer("edit");
		configContent.put("infoConfigForm", infoConfigForm.getInitialComponent());
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	public String[] getPaneKeys() {
		return paneKeys;
	}

	@Override
	public TabbedPane getTabbedPane() {
		return myTabbedPane;
	}

	@Override
	public void addTabs(final TabbedPane tabbedPane) {
		myTabbedPane = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), editAccessVc);
		tabbedPane.addTab(translate(PANE_TAB_CONFIG), configContent);
	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == infoConfigForm) {
			if (event == Event.DONE_EVENT) {
				infoConfigForm.getUpdatedConfig();
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == accessCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = accessCondContr.getCondition();
				courseNode.setPreConditionAccess(cond);
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == editCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = editCondContr.getCondition();
				courseNode.setPreConditionEdit(cond);
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == adminCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = adminCondContr.getCondition();
				courseNode.setPreConditionAdmin(cond);
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		}
	}

	public static boolean getAutoSubscribe(final ModuleConfiguration config) {
		final String autoStr = config.getStringValue(InfoCourseNodeConfiguration.CONFIG_AUTOSUBSCRIBE);
		return ("on".equals(autoStr));
	}
}
