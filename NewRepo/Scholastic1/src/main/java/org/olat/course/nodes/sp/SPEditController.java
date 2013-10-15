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

package org.olat.course.nodes.sp;

import org.olat.commons.file.filechooser.FileChooseCreateEditController;
import org.olat.commons.file.filechooser.LinkChooseCreateEditController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
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
import org.olat.course.nodes.SPCourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseEditorTreeModel;
import org.olat.course.tree.CourseInternalLinkTreeModel;
import org.olat.modules.ModuleConfiguration;

/**
 * Description:<BR/>
 * Edit controller for single page course nodes
 * <P/>
 * Initial Date: Oct 12, 2004
 * 
 * @author Felix Jost
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class SPEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	public static final String PANE_TAB_SPCONFIG = "pane.tab.spconfig";
	private static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	/** configuration key for the filename */
	public static final String CONFIG_KEY_FILE = "file";
	/** configuration key: should relative links like ../otherfolder/my.css be allowed? **/
	public static final String CONFIG_KEY_ALLOW_RELATIVE_LINKS = "allowRelativeLinks";

	private static final String[] paneKeys = { PANE_TAB_SPCONFIG, PANE_TAB_ACCESSIBILITY };

	// NLS support:

	private static final String NLS_CONDITION_ACCESSIBILITY_TITLE = "condition.accessibility.title";

	private final ModuleConfiguration moduleConfiguration;
	private final VelocityContainer myContent;
	private final Panel fcPanel;

	private final SPCourseNode courseNode;
	private Boolean allowRelativeLinks;

	private final ConditionEditController accessibilityCondContr;
	private final FileChooseCreateEditController fccecontr;
	private TabbedPane myTabbedPane;

	/**
	 * Constructor for single page editor controller
	 * 
	 * @param config The node module configuration
	 * @param ureq The user request
	 * @param wControl The window controller
	 * @param spCourseNode The current single page course node
	 * @param course
	 * @param euce
	 */
	public SPEditController(final ModuleConfiguration config, final UserRequest ureq, final WindowControl wControl, final SPCourseNode spCourseNode,
			final ICourse course, final UserCourseEnvironment euce) {
		super(ureq, wControl);
		this.moduleConfiguration = config;
		this.courseNode = spCourseNode;

		myContent = createVelocityContainer("edit");

		config.remove("iniframe");// on the fly remove deprecated stuff
		config.remove("statefulMicroWeb");
		final String chosenFile = (String) config.get(CONFIG_KEY_FILE);
		allowRelativeLinks = moduleConfiguration.getBooleanEntry(CONFIG_KEY_ALLOW_RELATIVE_LINKS);
		fccecontr = new LinkChooseCreateEditController(ureq, getWindowControl(), chosenFile, allowRelativeLinks, course.getCourseFolderContainer(),
				new CourseInternalLinkTreeModel(course.getEditorTreeModel()));
		listenTo(fccecontr);
		fccecontr.setAllFileSuffixesAllowed(true);

		fcPanel = new Panel("filechoosecreateedit");
		final Component fcContent = fccecontr.getInitialComponent();
		fcPanel.setContent(fcContent);
		myContent.put(fcPanel.getComponentName(), fcPanel);

		final CourseGroupManager groupMgr = course.getCourseEnvironment().getCourseGroupManager();
		final CourseEditorTreeModel editorModel = course.getEditorTreeModel();
		// Accessibility precondition
		final Condition accessCondition = courseNode.getPreConditionAccess();
		accessibilityCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, accessCondition, "accessabilityConditionForm",
				AssessmentHelper.getAssessableNodes(editorModel, spCourseNode), euce);
		this.listenTo(accessibilityCondContr);

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
		if (source == accessibilityCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = accessibilityCondContr.getCondition();
				courseNode.setPreConditionAccess(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == fccecontr) {
			if (event == FileChooseCreateEditController.FILE_CHANGED_EVENT) {
				final String chosenFile = fccecontr.getChosenFile();
				if (chosenFile != null) {
					moduleConfiguration.set(CONFIG_KEY_FILE, fccecontr.getChosenFile());
				} else {
					moduleConfiguration.remove(CONFIG_KEY_FILE);
				}
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			} else if (event == FileChooseCreateEditController.FILE_CONTENT_CHANGED_EVENT) {
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			} else if (event == FileChooseCreateEditController.ALLOW_RELATIVE_LINKS_CHANGED_EVENT) {
				allowRelativeLinks = fccecontr.getAllowRelativeLinks();
				courseNode.getModuleConfiguration().setBooleanEntry(CONFIG_KEY_ALLOW_RELATIVE_LINKS, allowRelativeLinks.booleanValue());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	@Override
	public void addTabs(final TabbedPane tabbedPane) {
		myTabbedPane = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), accessibilityCondContr.getWrappedDefaultAccessConditionVC(translate(NLS_CONDITION_ACCESSIBILITY_TITLE)));
		tabbedPane.addTab(translate(PANE_TAB_SPCONFIG), myContent);
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