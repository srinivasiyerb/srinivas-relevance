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
 * Copyright (c) since 2004 at frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.admin.user.bulkChange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.ValidationError;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.rules.RulesFactory;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.BasicStep;
import org.olat.core.gui.control.generic.wizard.PrevNextFinishConfig;
import org.olat.core.gui.control.generic.wizard.StepFormBasicController;
import org.olat.core.gui.control.generic.wizard.StepFormController;
import org.olat.core.gui.control.generic.wizard.StepsEvent;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.util.StringHelper;
import org.olat.core.util.i18n.I18nManager;
import org.olat.user.ProfileFormController;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

public/**
 * Description:<br>
 * First Step in bulk change wizard using flexiForm
 * 
 * <P>
 * Initial Date: 30.01.2008 <br>
 * 
 * @author rhaag
 */
class UserBulkChangeStep00 extends BasicStep {

	static final String usageIdentifyer = UserBulkChangeStep00.class.getCanonicalName();
	static final String usageIdentifyerForAllProperties = ProfileFormController.class.getCanonicalName();
	TextElement textAreaElement;
	List<Identity> identitiesToEdit;
	private static VelocityEngine velocityEngine;
	boolean isAdministrativeUser;
	boolean isOLATAdmin;
	static UserBulkChangeManager ubcMan;

	static {
		// init velocity engine
		Properties p = null;
		try {
			velocityEngine = new VelocityEngine();
			p = new Properties();
			p.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
			p.setProperty("runtime.log.logsystem.log4j.category", "syslog");
			velocityEngine.init(p);
		} catch (final Exception e) {
			throw new RuntimeException("config error " + p.toString());
		}
	}

	public UserBulkChangeStep00(final UserRequest ureq, final List<Identity> toEdit) {
		super(ureq);
		this.identitiesToEdit = toEdit;
		setI18nTitleAndDescr("step0.description", null);
		setNextStep(new UserBulkChangeStep01(ureq));
		ubcMan = UserBulkChangeManager.getInstance();
		final Roles roles = ureq.getUserSession().getRoles();
		isOLATAdmin = roles.isOLATAdmin();
		isAdministrativeUser = (roles.isAuthor() || roles.isGroupManager() || roles.isUserManager() || roles.isOLATAdmin());
	}

	/**
	 * @see org.olat.core.gui.control.generic.wizard.Step#getInitialPrevNextFinishConfig()
	 */
	@Override
	public PrevNextFinishConfig getInitialPrevNextFinishConfig() {
		return new PrevNextFinishConfig(false, true, false);
	}

	/**
	 * @see org.olat.core.gui.control.generic.wizard.Step#getStepController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.control.generic.wizard.StepsRunContext, org.olat.core.gui.components.form.flexible.impl.Form)
	 */
	@Override
	public StepFormController getStepController(final UserRequest ureq, final WindowControl windowControl, final StepsRunContext stepsRunContext, final Form form) {
		final StepFormController stepI = new UserBulkChangeStepForm00(ureq, windowControl, form, stepsRunContext);
		return stepI;
	}

	private final class UserBulkChangeStepForm00 extends StepFormBasicController {
		private FormLayoutContainer textContainer;
		private List<UserPropertyHandler> userPropertyHandlers;
		FormItem formitem;
		List<MultipleSelectionElement> checkBoxes;
		List<FormItem> formItems;

		public UserBulkChangeStepForm00(final UserRequest ureq, final WindowControl control, final Form rootForm, final StepsRunContext runContext) {
			super(ureq, control, rootForm, runContext, LAYOUT_VERTICAL, null);
			// use custom translator with fallback to user properties translator
			final UserManager um = UserManager.getInstance();
			setTranslator(um.getPropertyHandlerTranslator(getTranslator()));
			flc.setTranslator(getTranslator());
			initForm(ureq);
		}

		@Override
		protected void doDispose() {
			// nothing to dispose
		}

		@Override
		protected void formOK(final UserRequest ureq) {
			// process changed attributes
			int i = 0;
			final HashMap<String, String> attributeChangeMap = new HashMap<String, String>();
			for (final Iterator<MultipleSelectionElement> iterator = checkBoxes.iterator(); iterator.hasNext();) {
				final MultipleSelectionElement checkbox = iterator.next();
				if (checkbox.isSelected(0)) {
					final FormItem formItem = formItems.get(i);
					// first get the values from the hardcoded items
					if (formItem.getName().equals(UserBulkChangeManager.LANG_IDENTIFYER)) {
						final SingleSelection selectField = (SingleSelection) formItem;
						attributeChangeMap.put(UserBulkChangeManager.LANG_IDENTIFYER, selectField.getSelectedKey());
					} else if (formItem.getName().equals(UserBulkChangeManager.PWD_IDENTIFYER)) {
						final TextElement propertyField = (TextElement) formItem;
						attributeChangeMap.put(UserBulkChangeManager.PWD_IDENTIFYER, propertyField.getValue());
					}
					// second get the values from all configured user properties
					else {
						// find corresponding user property handler
						for (final UserPropertyHandler propertyHanlder : userPropertyHandlers) {
							if (propertyHanlder.getName().equals(formItem.getName())) {
								String inputText;
								if (formItem instanceof DateChooser) {
									// special case: don't use getStringValue, this would encode
									// the date with the date formatter, use raw text input value
									// instead
									final DateChooser dateItem = (DateChooser) formItem;
									inputText = dateItem.getValue();
								} else {
									inputText = propertyHanlder.getStringValue(formItem);
								}

								attributeChangeMap.put(formItem.getName(), inputText);
							}
						}
					}
				}
				i++;
			}
			addToRunContext("attributeChangeMap", attributeChangeMap);
			addToRunContext("identitiesToEdit", identitiesToEdit);
			fireEvent(ureq, StepsEvent.ACTIVATE_NEXT);
		}

		@Override
		protected boolean validateFormLogic(final UserRequest ureq) {
			Boolean validChange = false;
			final UserManager um = UserManager.getInstance();

			// loop through and check if any checkbox has been selected
			int i = 0;

			for (final Iterator<MultipleSelectionElement> iterator = checkBoxes.iterator(); iterator.hasNext();) {
				final MultipleSelectionElement checkbox = iterator.next();
				if (checkbox.isSelected(0)) {
					final Context vcContext = ubcMan.getDemoContext(getTranslator(), isAdministrativeUser);
					validChange = true;
					final FormItem formItem = formItems.get(i);
					if (formItem instanceof TextElement) {
						final TextElement propertyField = (TextElement) formItem;
						String inputFieldValue = propertyField.getValue();

						// check validity of velocity-variables by using default values from
						// userproperties
						inputFieldValue = inputFieldValue.replace("$", "$!");
						final String evaluatedInputFieldValue = ubcMan.evaluateValueWithUserContext(inputFieldValue, vcContext);

						// check user property configuration
						for (final UserPropertyHandler handler : userPropertyHandlers) {
							if (handler.getName().equals(formItem.getName())) {
								// first check on mandatoryness
								if (um.isMandatoryUserProperty(usageIdentifyer, handler) && !StringHelper.containsNonWhitespace(evaluatedInputFieldValue)) {
									formItem.setErrorKey("form.name." + handler.getName() + ".error.empty", null);
									return false;
								}
								// second check on property content
								final ValidationError valicationError = new ValidationError();
								if (!handler.isValidValue(evaluatedInputFieldValue, valicationError, ureq.getLocale())) {
									formItem.setErrorKey(valicationError.getErrorKey(), null);
									return false;
								}
								// else validation was ok, reset previous errors
								formItem.clearError();
							}
						}

						// special case: check password-syntax:
						if (propertyField.getName().equals("password")) {
							if (!um.syntaxCheckOlatPassword(evaluatedInputFieldValue)) {
								propertyField.setErrorKey("error.password", new String[] { evaluatedInputFieldValue });
								return false;
							}
						}

						// already done by form.visitAll -> validate():
						// //check all other types according to its FormItem type
						// propertyField.validate(validationResults);
						//
						// //set value back to inputValue in order to process input in later
						// steps
						// propertyField.setValue(origInputFieldValue);
					}
				}
				i++;
			} // for

			addToRunContext("validChange", validChange);
			return true;
		}

		@SuppressWarnings({ "unchecked", "unchecked" })
		@Override
		protected void initForm(final FormItemContainer formLayout, final Controller listener, @SuppressWarnings("unused") final UserRequest ureq) {
			MultipleSelectionElement checkbox;
			checkBoxes = new ArrayList<MultipleSelectionElement>();
			formItems = new ArrayList<FormItem>();

			setFormTitle("title");
			// text description of this Step
			textContainer = FormLayoutContainer.createCustomFormLayout("index", getTranslator(), this.velocity_root + "/step0.html");
			formLayout.add(textContainer);
			textContainer.contextPut("userPropertyHandlers", UserManager.getInstance().getUserPropertyHandlersFor(usageIdentifyerForAllProperties, isAdministrativeUser));

			Set<FormItem> targets;
			// Main layout is a vertical layout without left side padding. To format
			// the checkboxes properly we need a default layout for the remaining form
			// elements
			final FormItemContainer innerFormLayout = FormLayoutContainer.createDefaultFormLayout("innerFormLayout", getTranslator());
			formLayout.add(innerFormLayout);

			// add input field for password
			final Boolean canChangePwd = BaseSecurityModule.USERMANAGER_CAN_MODIFY_PWD;
			if (canChangePwd.booleanValue() || isOLATAdmin) {
				checkbox = uifactory.addCheckboxesVertical("checkboxPWD", "form.name.pwd", innerFormLayout, new String[] { "changePWD" }, new String[] { "" }, null, 1);
				checkbox.select("changePWD", false);
				checkbox.addActionListener(listener, FormEvent.ONCLICK);
				formitem = uifactory.addTextElement(UserBulkChangeManager.PWD_IDENTIFYER, "password", 127, null, innerFormLayout);
				final TextElement formEl = (TextElement) formitem;
				formEl.setDisplaySize(35);
				formitem.setLabel(null, null);
				targets = new HashSet<FormItem>();
				targets.add(formitem);
				RulesFactory.createHideRule(checkbox, null, targets, innerFormLayout);
				RulesFactory.createShowRule(checkbox, "changePWD", targets, innerFormLayout);
				checkBoxes.add(checkbox);
				formItems.add(formitem);
			}

			// add SingleSelect for language
			final Map<String, String> locdescs = I18nManager.getInstance().getEnabledLanguagesTranslated();
			final Set lkeys = locdescs.keySet();
			final String[] languageKeys = new String[lkeys.size()];
			final String[] languageValues = new String[lkeys.size()];
			// fetch languages
			int p = 0;
			final I18nManager i18n = I18nManager.getInstance();
			for (final Iterator iter = lkeys.iterator(); iter.hasNext();) {
				final String key = (String) iter.next();
				languageKeys[p] = key;
				languageValues[p] = locdescs.get(key);
				p++;
			}
			checkbox = uifactory
					.addCheckboxesVertical("checkboxLang", "form.name.language", innerFormLayout, new String[] { "changeLang" }, new String[] { "" }, null, 1);
			checkbox.select("changeLang", false);
			checkbox.addActionListener(listener, FormEvent.ONCLICK);
			formitem = uifactory.addDropdownSingleselect(UserBulkChangeManager.LANG_IDENTIFYER, innerFormLayout, languageKeys, languageValues, null);
			formitem.setLabel(null, null);
			targets = new HashSet<FormItem>();
			targets.add(formitem);
			RulesFactory.createHideRule(checkbox, null, targets, innerFormLayout);
			RulesFactory.createShowRule(checkbox, "changeLang", targets, innerFormLayout);
			checkBoxes.add(checkbox);
			formItems.add(formitem);

			// add checkboxes/formitems for userProperties defined in
			// src/serviceconfig/org/olat/_spring/olat_userconfig.xml -> Key:
			// org.olat.admin.user.bulkChange.UserBulkChangeStep00
			userPropertyHandlers = UserManager.getInstance().getUserPropertyHandlersFor(usageIdentifyer, isAdministrativeUser);
			UserPropertyHandler userPropertyHandler;
			for (int i = 0; i < userPropertyHandlers.size(); i++) {
				userPropertyHandler = userPropertyHandlers.get(i);

				checkbox = uifactory.addCheckboxesVertical("checkbox" + i, "form.name." + userPropertyHandler.getName(), innerFormLayout, new String[] { "change"
						+ userPropertyHandler.getName() }, new String[] { "" }, null, 1);
				checkbox.select("change" + userPropertyHandler.getName(), false);
				checkbox.addActionListener(listener, FormEvent.ONCLICK);

				formitem = userPropertyHandler.addFormItem(getLocale(), null, usageIdentifyer, isAdministrativeUser, innerFormLayout);
				formitem.setLabel(null, null);

				targets = new HashSet<FormItem>();
				targets.add(formitem);

				RulesFactory.createHideRule(checkbox, null, targets, innerFormLayout);
				RulesFactory.createShowRule(checkbox, "change" + userPropertyHandler.getName(), targets, innerFormLayout);

				checkBoxes.add(checkbox);
				formItems.add(formitem);
			}

		}

	}

}
