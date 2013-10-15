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

package org.olat.course.nodes.tu;

import org.olat.core.commons.fullWebApp.LayoutMain3ColsPreviewController;
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
import org.olat.course.condition.Condition;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.NodeEditController;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.TUCourseNode;
import org.olat.course.run.preview.PreviewConfigHelper;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseEditorTreeModel;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.tu.IframeTunnelController;

/**
 * Description:<BR/>
 * The tunneling edit controller is used to edit a course building block of typ tu
 * <P/>
 * Initial Date: Oct 12, 2004
 * 
 * @author Felix Jost
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class TUEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	public static final String PANE_TAB_TUCONFIG = "pane.tab.tuconfig";
	public static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";

	private static final String[] paneKeys = { PANE_TAB_TUCONFIG, PANE_TAB_ACCESSIBILITY };

	private ModuleConfiguration config;
	private final VelocityContainer myContent;

	private final TUConfigForm tuConfigForm;
	private final TUCourseNode courseNode;
	private final ConditionEditController accessibilityCondContr;
	private TabbedPane myTabbedPane;
	private LayoutMain3ColsPreviewController previewLayoutCtr;
	private final Link previewButton;
	private final ICourse course; // used only for preview of the current node

	/**
	 * Constructor for tunneling editor controller
	 * 
	 * @param config The node module configuration
	 * @param ureq The user request
	 * @param wControl The window controller
	 * @param tuCourseNode The current single page course node
	 * @param course
	 */
	public TUEditController(final ModuleConfiguration config, final UserRequest ureq, final WindowControl wControl, final TUCourseNode tuCourseNode,
			final ICourse course, final UserCourseEnvironment euce) {
		super(ureq, wControl);

		this.config = config;
		this.courseNode = tuCourseNode;
		this.course = course;

		myContent = this.createVelocityContainer("edit");
		previewButton = LinkFactory.createButtonSmall("command.preview", myContent, this);

		tuConfigForm = new TUConfigForm(ureq, wControl, config, false);
		listenTo(tuConfigForm);
		myContent.put("tuConfigForm", tuConfigForm.getInitialComponent());

		final CourseGroupManager groupMgr = course.getCourseEnvironment().getCourseGroupManager();
		final CourseEditorTreeModel editorModel = course.getEditorTreeModel();
		// Accessibility precondition
		final Condition accessCondition = courseNode.getPreConditionAccess();
		accessibilityCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, accessCondition, "accessabilityConditionForm",
				AssessmentHelper.getAssessableNodes(editorModel, tuCourseNode), euce);
		this.listenTo(accessibilityCondContr);

		// Enable preview button only if node configuration is valid
		if (!(tuCourseNode.isConfigValid().isError())) {
			myContent.contextPut("showPreviewButton", Boolean.TRUE);
		} else {
			myContent.contextPut("showPreviewButton", Boolean.FALSE);
		}

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == previewButton) { // those must be links
			Controller tunnelRunCtr;
			if (config.getBooleanSafe(TUConfigForm.CONFIG_IFRAME)) {
				tunnelRunCtr = new IframeTunnelController(ureq, getWindowControl(), config);
			} else {
				tunnelRunCtr = new TURunController(getWindowControl(), config, ureq, courseNode, PreviewConfigHelper.getPreviewCourseEnvironment(true, true, course));
			}
			if (previewLayoutCtr != null) {
				previewLayoutCtr.dispose();
			}
			// preview layout: only center column (col3) used
			previewLayoutCtr = new LayoutMain3ColsPreviewController(ureq, getWindowControl(), null, null, tunnelRunCtr.getInitialComponent(), null);
			previewLayoutCtr.addDisposableChildController(tunnelRunCtr); // cascade dispose
			previewLayoutCtr.activate();
		}
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
		} else if (source == tuConfigForm) {
			if (event == Event.CANCELLED_EVENT) {
				// do nothing
			} else if (event == Event.DONE_EVENT) {
				config = tuConfigForm.getUpdatedConfig();
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
				// form valid -> node config valid -> show preview button
				myContent.contextPut("showPreviewButton", Boolean.TRUE);
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
		tabbedPane.addTab(translate(PANE_TAB_TUCONFIG), myContent);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// child controllers registered with listenTo() get disposed in BasicController
		if (previewLayoutCtr != null) {
			previewLayoutCtr.dispose();
			previewLayoutCtr = null;
		}
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