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
 * Technische Universitaet Chemnitz Lehrstuhl Technische Informatik Author Marcel Karras (toka@freebits.de) Author Norbert Englisch
 * (norbert.englisch@informatik.tu-chemnitz.de) Author Sebastian Fritzsche (seb.fritzsche@googlemail.com)
 */

package de.tuchemnitz.wizard.workflows.coursecreation.steps;

import java.util.HashSet;
import java.util.Set;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.FormUIFactory;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.rules.RulesFactory;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.BasicStep;
import org.olat.core.gui.control.generic.wizard.PrevNextFinishConfig;
import org.olat.core.gui.control.generic.wizard.Step;
import org.olat.core.gui.control.generic.wizard.StepFormBasicController;
import org.olat.core.gui.control.generic.wizard.StepFormController;
import org.olat.core.gui.control.generic.wizard.StepsEvent;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Util;

import de.tuchemnitz.wizard.workflows.coursecreation.CourseCreationHelper;
import de.tuchemnitz.wizard.workflows.coursecreation.model.CourseCreationConfiguration;

/**
 * Description:<br>
 * Third step of the course creation wizard: <br/>
 * - choose access condition <br/>
 * - publish?
 * <P>
 * 
 * @author Marcel Karras (toka@freebits.de)
 * @author Norbert Englisch (norbert.englisch@informatik.tu-chemnitz.de)
 * @author Sebastian Fritzsche (seb.fritzsche@googlemail.com)
 * @author skoeber
 */
public class CcStep02 extends BasicStep {

	private final PrevNextFinishConfig prevNextConfig;
	private final CourseCreationConfiguration courseConfig;

	/**
	 * Third step of the course creation wizard.
	 * 
	 * @param ureq
	 * @param courseConfig
	 */
	public CcStep02(final UserRequest ureq, final CourseCreationConfiguration courseConfig) {
		super(ureq);
		this.courseConfig = courseConfig;
		setI18nTitleAndDescr("coursecreation.publish.title", "coursecreation.publish.description");
		setNextStep(Step.NOSTEP);
		prevNextConfig = PrevNextFinishConfig.BACK_FINISH;
	}

	@Override
	public PrevNextFinishConfig getInitialPrevNextFinishConfig() {
		return prevNextConfig;
	}

	@Override
	public StepFormController getStepController(final UserRequest ureq, final WindowControl windowControl, final StepsRunContext stepsRunContext, final Form form) {
		final StepFormController stepP = new CcStep02Form(ureq, windowControl, form, stepsRunContext, null);
		return stepP;
	}

	public CourseCreationConfiguration getCourseConfig() {
		return courseConfig;
	}

	class CcStep02Form extends StepFormBasicController {

		private final Translator translator;
		private FormLayoutContainer fic;
		private String[] keys, values;
		private SingleSelection accessChooser;
		// publish checkbox
		private MultipleSelectionElement publishCheckbox;
		// double point text label
		private StaticTextElement warning;
		// BPS only, see OLAT-5534
		private final boolean showAclInst = false;

		public CcStep02Form(final UserRequest ureq, final WindowControl wControl, final Form rootForm, final StepsRunContext runContext, final String customLayoutPageName) {
			super(ureq, wControl, rootForm, runContext, LAYOUT_VERTICAL, customLayoutPageName);
			// trans for this class
			translator = Util.createPackageTranslator(CourseCreationHelper.class, ureq.getLocale());
			super.setTranslator(translator);
			initForm(ureq);
		}

		@Override
		protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
			super.formInnerEvent(ureq, source, event);
			finishWorkflowItem();
		}

		@Override
		protected void doDispose() {
			// nothing to dispose here
		}

		@Override
		protected void formOK(final UserRequest ureq) {
			finishWorkflowItem();
			this.addToRunContext("courseConfig", getCourseConfig());
			fireEvent(ureq, StepsEvent.INFORM_FINISHED);
		}

		@Override
		@SuppressWarnings({ "unused", "synthetic-access" })
		protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
			fic = FormLayoutContainer.createCustomFormLayout("cc02", this.getTranslator(), this.velocity_root + "/CcStep02_form.html");
			final FormLayoutContainer formItems = FormLayoutContainer.createDefaultFormLayout("formItems", this.getTranslator());
			formLayout.add(fic);
			formLayout.add(formItems);

			// create access limit checkbox
			publishCheckbox = FormUIFactory.getInstance().addCheckboxesVertical("publishCheckbox", formItems, new String[] { "ison" }, new String[] { "" }, null, 1);
			publishCheckbox.setLabel("car.makepublic", null);
			publishCheckbox.select("ison", true);

			publishCheckbox.addActionListener(this, FormEvent.ONCHANGE);
			// register for on click event to hide/disable other elements
			publishCheckbox.addActionListener(this, FormEvent.ONCLICK);

			final int numOpts = showAclInst ? 3 : 2;
			keys = new String[numOpts];
			values = new String[numOpts];

			keys[0] = CourseCreationConfiguration.ACL_GUEST;
			keys[1] = CourseCreationConfiguration.ACL_OLAT;
			values[0] = translator.translate("car.nologin");
			values[1] = translator.translate("car.olat");

			// BPS only, see OLAT-5534
			if (showAclInst) {
				String institution = ureq.getIdentity().getUser().getProperty(UserConstants.INSTITUTIONALNAME, ureq.getLocale());
				if (institution == null) {
					institution = ureq.getUserSession().getSessionInfo().getAuthProvider();
				}
				keys[2] = CourseCreationConfiguration.ACL_UNI;
				values[2] = translator.translate("car.university", new String[] { institution });
			}

			accessChooser = FormUIFactory.getInstance().addDropdownSingleselect("accessChooser", formItems, keys, values, null);
			accessChooser.setLabel("car.label", null);

			accessChooser.select(CourseCreationConfiguration.ACL_OLAT, true);
			accessChooser.addActionListener(this, FormEvent.ONCHANGE);

			// text warning
			warning = FormUIFactory.getInstance().addStaticTextElement("car.warning.text", "car.label", translate("car.warning.text"), formItems);

			// rules to hide / unhide
			final Set<FormItem> targetsDoPublish = new HashSet<FormItem>() {
				{
					add(accessChooser);
				}
			};
			final Set<FormItem> targetsDontPublish = new HashSet<FormItem>() {
				{
					add(warning);
				}
			};

			RulesFactory.createHideRule(publishCheckbox, null, targetsDoPublish, formItems);
			RulesFactory.createShowRule(publishCheckbox, "ison", targetsDoPublish, formItems);

			RulesFactory.createHideRule(publishCheckbox, "ison", targetsDontPublish, formItems);
			RulesFactory.createShowRule(publishCheckbox, null, targetsDontPublish, formItems);

			initWorkflowItem();
		}

		public void finishWorkflowItem() {
			getCourseConfig().setPublish(isPublishSelected());
			getCourseConfig().setAclType(accessChooser.getSelectedKey());
		}

		public void initWorkflowItem() {
			if (!getCourseConfig().getAclType().isEmpty()) {
				accessChooser.select(getCourseConfig().getAclType(), true);
			}

			if (getCourseConfig().getPublish()) {
				publishCheckbox.select("ison", true);
				warning.setVisible(false);
			} else {
				publishCheckbox.select("ison", false);
				accessChooser.setVisible(false);
				warning.setVisible(true);
			}
		}

		private final boolean isPublishSelected() {
			return publishCheckbox.isSelected(0);
		}
	}
}
