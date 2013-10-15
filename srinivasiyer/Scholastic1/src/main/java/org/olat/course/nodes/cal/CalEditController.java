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
 * frentix GmbH, Switzerland, http://www.frentix.com
 * <p>
 */

package org.olat.course.nodes.cal;

import java.util.Date;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController;
import org.olat.core.util.StringHelper;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.condition.Condition;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.NodeEditController;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.CalCourseNode;
import org.olat.course.run.calendar.CourseCalendarController;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseEditorTreeModel;
import org.olat.modules.ModuleConfiguration;

/**
 * <h3>Description:</h3> Edit controller for calendar course nodes<br/>
 * <p>
 * Initial Date: 4 nov. 2009 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, www.frentix.com
 */
public class CalEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {
	private static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	private static final String PANE_TAB_CALCONFIG = "pane.tab.calconfig";

	private static final String CONFIG_START_DATE = "startDate";
	private static final String CONFIG_AUTO_DATE = "autoDate";
	private static final String CONFIG_AUTO_SUBSCRIBE = "autoSubscribe";

	private static final String[] paneKeys = { PANE_TAB_CALCONFIG, PANE_TAB_ACCESSIBILITY };

	private final ModuleConfiguration moduleConfiguration;
	private final ConditionEditController accessCondContr;
	private final DisplayConfigTabForm displayForm;
	private TabbedPane tabs;
	private final Panel main;
	private CourseCalendarController calCtr;
	private final VelocityContainer editAccessVc;
	private final ConditionEditController editCondContr;

	private final CalCourseNode calCourseNode;

	/**
	 * Constructor for calendar page editor controller
	 * 
	 * @param config The node module configuration
	 * @param ureq The user request
	 * @param calCourseNode The current calendar page course node
	 * @param course
	 */
	public CalEditController(final ModuleConfiguration config, final UserRequest ureq, final WindowControl wControl, final CalCourseNode calCourseNode,
			final ICourse course, final UserCourseEnvironment euce) {
		super(ureq, wControl);
		this.moduleConfiguration = config;
		this.calCourseNode = calCourseNode;

		main = new Panel("calmain");

		editAccessVc = createVelocityContainer("edit_access");
		final CourseGroupManager groupMgr = course.getCourseEnvironment().getCourseGroupManager();
		final CourseEditorTreeModel editorModel = course.getEditorTreeModel();
		// Accessibility precondition
		final Condition accessCondition = calCourseNode.getPreConditionAccess();
		accessCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, accessCondition, "accessConditionForm", AssessmentHelper.getAssessableNodes(
				editorModel, calCourseNode), euce);
		this.listenTo(accessCondContr);
		editAccessVc.put("readerCondition", accessCondContr.getInitialComponent());

		// cal read / write preconditions
		final Condition editCondition = calCourseNode.getPreConditionEdit();
		editCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, editCondition, "editConditionForm", AssessmentHelper.getAssessableNodes(
				editorModel, calCourseNode), euce);
		listenTo(editCondContr);
		editAccessVc.put("editCondition", editCondContr.getInitialComponent());

		displayForm = new DisplayConfigTabForm(moduleConfiguration, ureq, wControl);
		listenTo(displayForm);
		main.setContent(displayForm.getInitialComponent());
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
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == displayForm) {
			if (event == Event.DONE_EVENT) {
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == accessCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = accessCondContr.getCondition();
				calCourseNode.setPreConditionAccess(cond);
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == editCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = editCondContr.getCondition();
				calCourseNode.setPreConditionEdit(cond);
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	@Override
	public void addTabs(final TabbedPane tabbedPane) {
		tabs = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), editAccessVc);
		tabbedPane.addTab(translate(PANE_TAB_CALCONFIG), main);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// child controllers registered with listenTo() get disposed in
		// BasicController
		if (calCtr != null) {
			calCtr.dispose();
			calCtr = null;
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController#getPaneKeys()
	 */
	@Override
	public String[] getPaneKeys() {
		return paneKeys;
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController#getTabbedPane()
	 */
	@Override
	public TabbedPane getTabbedPane() {
		return tabs;
	}

	public static Date getStartDate(final ModuleConfiguration config) {
		final String timeStr = config.getStringValue(CONFIG_START_DATE);
		if (StringHelper.containsNonWhitespace(timeStr)) {
			try {
				final Long time = Long.parseLong(timeStr);
				return new Date(time);
			} catch (final Exception e) {
				return null;
			}
		} else {
			return null;
		}
	}

	public static void setStartDate(final ModuleConfiguration config, final Date startDate) {
		if (startDate == null) {
			config.setStringValue(CONFIG_START_DATE, "");
		} else {
			final String timeStr = String.valueOf(startDate.getTime());
			config.setStringValue(CONFIG_START_DATE, timeStr);
		}
	}

	public static boolean getAutoDate(final ModuleConfiguration config) {
		final String autoStr = config.getStringValue(CONFIG_AUTO_DATE);
		if (StringHelper.containsNonWhitespace(autoStr)) { return new Boolean(autoStr); }
		return Boolean.FALSE;
	}

	public static void setAutoDate(final ModuleConfiguration config, final boolean autoDate) {
		config.setStringValue(CONFIG_AUTO_DATE, Boolean.toString(autoDate));
	}

	public static boolean getAutoSubscribe(final ModuleConfiguration config) {
		final String autoStr = config.getStringValue(CONFIG_AUTO_SUBSCRIBE);
		if (StringHelper.containsNonWhitespace(autoStr)) { return new Boolean(autoStr); }
		return Boolean.FALSE;
	}

	public static void setAutoSubscribe(final ModuleConfiguration config, final boolean subscribe) {
		config.setStringValue(CONFIG_AUTO_SUBSCRIBE, Boolean.toString(subscribe));
	}

	private class DisplayConfigTabForm extends FormBasicController {
		private DateChooser dateChooser;
		private SingleSelection autoDateEl;
		private MultipleSelectionElement autoSubscribeEl;
		private final ModuleConfiguration config;

		public DisplayConfigTabForm(final ModuleConfiguration config, final UserRequest ureq, final WindowControl wControl) {
			super(ureq, wControl);
			this.config = config;
			initForm(ureq);
		}

		@Override
		protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
			setFormTitle("pane.tab.calconfig");
			// setFormDescription("pane.tab.calconfigdesc");
			setFormContextHelp(CalEditController.class.getPackage().getName(), "caledit.html", "help.hover.calendar");

			final boolean autoDate = getAutoDate(config);
			final String[] keys = new String[] { "auto", "selected" };
			final String[] values = new String[] { translate("pane.tab.auto_date"), translate("pane.tab.manual_date") };
			autoDateEl = uifactory.addRadiosVertical("pane.tab_auto_date", formLayout, keys, values);

			autoDateEl.select(autoDate ? keys[0] : keys[1], autoDate);
			autoDateEl.setLabel("pane.tab.start_date", null);
			autoDateEl.addActionListener(this, FormEvent.ONCLICK);

			final Date startDate = getStartDate(config);
			final Date selectedDate = startDate == null ? new Date() : startDate;
			dateChooser = uifactory.addDateChooser("pane.tab.start_date_chooser", null, "", formLayout);
			dateChooser.setDate(selectedDate);
			dateChooser.setVisible(!autoDate);

			final boolean autoSubscribe = getAutoSubscribe(config);
			final String[] subscribesKeys = new String[] { "" };
			final String[] subscribesValues = new String[] { translate("pane.tab.auto_subscribe.value") };
			autoSubscribeEl = uifactory.addCheckboxesHorizontal("pane.tab.auto_subscribe", formLayout, subscribesKeys, subscribesValues, null);
			autoSubscribeEl.select("", autoSubscribe);
			autoSubscribeEl.setLabel("pane.tab.auto_subscribe", null);

			// Create submit and cancel buttons
			final FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("buttonLayout", getTranslator());
			formLayout.add(buttonLayout);
			uifactory.addFormSubmitButton("save", buttonLayout);
		}

		@Override
		protected void doDispose() {
			//
		}

		@Override
		protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
			if (source == autoDateEl) {
				final boolean autoDate = isAutoDate();
				dateChooser.setVisible(!autoDate);
				flc.setDirty(true);
			}
		}

		@Override
		protected void formOK(final UserRequest ureq) {
			setStartDate(config, getDate());
			setAutoDate(config, isAutoDate());
			setAutoSubscribe(config, isAutoSubscribe());
			fireEvent(ureq, Event.DONE_EVENT);
		}

		public Date getDate() {
			return dateChooser.getDate();
		}

		public boolean isAutoDate() {
			return autoDateEl.isSelected(0);
		}

		public boolean isAutoSubscribe() {
			return autoSubscribeEl.isSelected(0);
		}
	}
}