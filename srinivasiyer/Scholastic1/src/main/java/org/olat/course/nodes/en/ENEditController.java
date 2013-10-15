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

package org.olat.course.nodes.en;

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
import org.olat.course.nodes.ENCourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;

/**
 * Description:<BR/>
 * The enrollment edit controller implements the enrollment specific tabs to configure an enrollement node.
 * <P/>
 * Initial Date: Sep 8, 2004
 * 
 * @author Felix Jost, gnaegi
 */
public class ENEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	public static final String PANE_TAB_ENCONFIG = "pane.tab.enconfig";
	private static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";

	private ModuleConfiguration moduleConfiguration;
	private VelocityContainer myContent;

	private ENCourseNode courseNode;
	private ConditionEditController accessibilityCondContr;
	private CourseGroupManager groupMgr;
	private UserCourseEnvironment euce;
	private TabbedPane myTabbedPane;
	private ENEditGroupAreaFormController easyGroupEditCtrllr;
	final static String[] paneKeys = { PANE_TAB_ENCONFIG, PANE_TAB_ACCESSIBILITY };

	/**
	 * @param config
	 * @param ureq
	 * @param enCourseNode
	 * @param course
	 */
	public ENEditController(final ModuleConfiguration config, final UserRequest ureq, final WindowControl wControl, final ENCourseNode enCourseNode,
			final ICourse course, final UserCourseEnvironment euce) {
		super(ureq, wControl);
		init(config, ureq, enCourseNode, course, euce);
	}

	/**
	 * @param config
	 * @param ureq
	 * @param enCourseNode
	 * @param course
	 * @param euce
	 */
	private void init(final ModuleConfiguration config, final UserRequest ureq, final ENCourseNode enCourseNode, final ICourse courseP, final UserCourseEnvironment euceP) {
		this.moduleConfiguration = config;
		this.courseNode = enCourseNode;
		this.groupMgr = courseP.getCourseEnvironment().getCourseGroupManager();
		this.euce = euceP;

		myContent = this.createVelocityContainer("edit");
		doFormInit(ureq);

		// Accessibility precondition
		final Condition accessCondition = courseNode.getPreConditionAccess();
		accessibilityCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, accessCondition, "accessabilityConditionForm",
				AssessmentHelper.getAssessableNodes(courseP.getEditorTreeModel(), enCourseNode), euceP);
		this.listenTo(accessibilityCondContr);

		// not needed: setInitialComponent(myContent) since tabbable controller
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	@SuppressWarnings("unused")
	public void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	private void doFormInit(final UserRequest ureq) {
		easyGroupEditCtrllr = new ENEditGroupAreaFormController(ureq, getWindowControl(), moduleConfiguration, euce.getCourseEditorEnv());
		easyGroupEditCtrllr.addControllerListener(this);
		myContent.put("groupnameform", easyGroupEditCtrllr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest urequest, final Controller source, final Event event) {
		if (source == accessibilityCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = accessibilityCondContr.getCondition();
				courseNode.setPreConditionAccess(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == easyGroupEditCtrllr) {
			// somehting changed in the nodeconfig
			moduleConfiguration = easyGroupEditCtrllr.getModuleConfiguration();
			fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableDefaultController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	@Override
	public void addTabs(final TabbedPane tabbedPane) {
		myTabbedPane = tabbedPane;

		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), accessibilityCondContr.getWrappedDefaultAccessConditionVC(translate("condition.accessibility.title")));
		tabbedPane.addTab(translate(PANE_TAB_ENCONFIG), myContent);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// child controllers registered with listenTo() get disposed in BasicController
	}

	/**
	 * @param moduleConfiguration
	 * @return true if module configuration is valid
	 */
	public static boolean isConfigValid(final ModuleConfiguration moduleConfiguration) {
		return (moduleConfiguration.get(ENCourseNode.CONFIG_GROUPNAME) != null);
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
