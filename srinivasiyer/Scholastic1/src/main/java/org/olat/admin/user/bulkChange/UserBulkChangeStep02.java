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
import java.util.List;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.BasicStep;
import org.olat.core.gui.control.generic.wizard.PrevNextFinishConfig;
import org.olat.core.gui.control.generic.wizard.Step;
import org.olat.core.gui.control.generic.wizard.StepFormBasicController;
import org.olat.core.gui.control.generic.wizard.StepFormController;
import org.olat.core.gui.control.generic.wizard.StepsEvent;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * Description:<br>
 * last step presenting an overview of every change per selected user which will be done after finish has been pressed
 * <P>
 * Initial Date: 30.01.2008 <br>
 * 
 * @author rhaag
 */
class UserBulkChangeStep02 extends BasicStep {

	static final String usageIdentifyer = UserBulkChangeStep00.class.getCanonicalName();
	public List<UserPropertyHandler> userPropertyHandlers;
	private static VelocityEngine velocityEngine;
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

	public UserBulkChangeStep02(final UserRequest ureq) {
		super(ureq);
		setI18nTitleAndDescr("step2.description", null);
		setNextStep(Step.NOSTEP);
		ubcMan = UserBulkChangeManager.getInstance();
	}

	/**
	 * @see org.olat.core.gui.control.generic.wizard.Step#getInitialPrevNextFinishConfig()
	 */
	@Override
	public PrevNextFinishConfig getInitialPrevNextFinishConfig() {
		return new PrevNextFinishConfig(true, false, true);
	}

	/**
	 * @see org.olat.core.gui.control.generic.wizard.Step#getStepController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.control.generic.wizard.StepsRunContext, org.olat.core.gui.components.form.flexible.impl.Form)
	 */
	@Override
	public StepFormController getStepController(final UserRequest ureq, final WindowControl windowControl, final StepsRunContext stepsRunContext, final Form form) {
		final StepFormController stepI = new UserBulkChangeStepForm02(ureq, windowControl, form, stepsRunContext);
		return stepI;
	}

	private final class UserBulkChangeStepForm02 extends StepFormBasicController {

		private FormLayoutContainer textContainer;

		public UserBulkChangeStepForm02(final UserRequest ureq, final WindowControl control, final Form rootForm, final StepsRunContext runContext) {
			super(ureq, control, rootForm, runContext, LAYOUT_VERTICAL, null);
			// use custom translator with fallback to user properties translator
			final UserManager um = UserManager.getInstance();
			setTranslator(um.getPropertyHandlerTranslator(getTranslator()));
			flc.setTranslator(getTranslator());
			initForm(ureq);
		}

		@Override
		protected void doDispose() {
			// TODO Auto-generated method stub
		}

		@Override
		protected void formOK(final UserRequest ureq) {
			fireEvent(ureq, StepsEvent.ACTIVATE_NEXT);
		}

		@Override
		protected boolean validateFormLogic(@SuppressWarnings("unused") final UserRequest ureq) {
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void initForm(final FormItemContainer formLayout, @SuppressWarnings("unused") final Controller listener, final UserRequest ureq) {
			final FormLayoutContainer formLayoutVertical = FormLayoutContainer.createVerticalFormLayout("vertical", getTranslator());
			formLayout.add(formLayoutVertical);

			setFormTitle("title");
			final List<List<String>> mergedDataChanges = new ArrayList<List<String>>();

			textContainer = FormLayoutContainer.createCustomFormLayout("index", getTranslator(), this.velocity_root + "/step2.html");
			formLayoutVertical.add(textContainer);
			final boolean validChange = (Boolean) getFromRunContext("validChange");
			textContainer.contextPut("validChange", validChange);
			if (!validChange) { return; }

			final List<Identity> selectedIdentities = (List<Identity>) getFromRunContext("identitiesToEdit");
			final HashMap<String, String> attributeChangeMap = (HashMap<String, String>) getFromRunContext("attributeChangeMap");
			final HashMap<String, String> roleChangeMap = (HashMap<String, String>) getFromRunContext("roleChangeMap");

			final Roles roles = ureq.getUserSession().getRoles();
			final boolean isAdministrativeUser = (roles.isAuthor() || roles.isGroupManager() || roles.isUserManager() || roles.isOLATAdmin());
			userPropertyHandlers = UserManager.getInstance().getUserPropertyHandlersFor(usageIdentifyer, isAdministrativeUser);

			final String[] securityGroups = { Constants.GROUP_USERMANAGERS, Constants.GROUP_GROUPMANAGERS, Constants.GROUP_AUTHORS, Constants.GROUP_ADMIN };

			// loop over users to be edited:
			for (final Identity identity : selectedIdentities) {
				final List<String> userDataArray = new ArrayList<String>();

				// add column for login
				userDataArray.add(identity.getName());
				// add columns for password
				if (attributeChangeMap.containsKey(UserBulkChangeManager.PWD_IDENTIFYER)) {
					userDataArray.add(attributeChangeMap.get(UserBulkChangeManager.PWD_IDENTIFYER));
				} else {
					userDataArray.add("***");
				}
				// add column for language
				final String userLanguage = identity.getUser().getPreferences().getLanguage();
				if (attributeChangeMap.containsKey(UserBulkChangeManager.LANG_IDENTIFYER)) {
					final String inputLanguage = attributeChangeMap.get(UserBulkChangeManager.LANG_IDENTIFYER);
					if (userLanguage.equals(inputLanguage)) {
						userDataArray.add(userLanguage);
					} else {
						userDataArray.add("<span class=\"b_wizard_table_changedcell\">" + inputLanguage + "</span>");
					}
				} else {
					userDataArray.add(userLanguage);
				}

				final Context vcContext = new VelocityContext();
				// set all properties as context
				ubcMan.setUserContext(identity, vcContext, isAdministrativeUser);
				// loop for each property configured in
				// src/serviceconfig/org/olat/_spring/olat_userconfig.xml -> Key:
				// org.olat.admin.user.bulkChange.UserBulkChangeStep00
				for (int k = 0; k < userPropertyHandlers.size(); k++) {
					final String propertyName = userPropertyHandlers.get(k).getName();
					final String userValue = identity.getUser().getProperty(propertyName, null);

					String inputFieldValue = "";
					if (attributeChangeMap.containsKey(propertyName)) {
						inputFieldValue = attributeChangeMap.get(propertyName);
						inputFieldValue = inputFieldValue.replace("$", "$!");
						final String evaluatedInputFieldValue = ubcMan.evaluateValueWithUserContext(inputFieldValue, vcContext);

						if (evaluatedInputFieldValue.equals(userValue)) {
							userDataArray.add(userValue);
						} else {
							// style italic:
							userDataArray.add("<span class=\"b_wizard_table_changedcell\">" + evaluatedInputFieldValue + "</span>");
						}
					} else {
						// property has not been checked in step00 but should be in
						// overview-table
						userDataArray.add(userValue);
					}

				} // for

				// add columns with roles
				// loop over securityGroups and get result...
				for (final String securityGroup : securityGroups) {
					final String roleStatus = getRoleStatusForIdentity(identity, securityGroup, roleChangeMap);
					userDataArray.add(roleStatus);
				}
				// add column with status
				userDataArray.add(roleChangeMap.get("Status"));

				// add each user:
				mergedDataChanges.add(userDataArray);
			}

			final FlexiTableColumnModel tableColumnModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
			// fixed fields:
			int colPos = 0;
			tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("table.user.login"));
			colPos++;
			tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("form.name.pwd"));
			colPos++;
			tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("form.name.language"));
			colPos++;

			for (int j = 0; j < userPropertyHandlers.size(); j++) {
				final UserPropertyHandler userPropertyHandler = userPropertyHandlers.get(j);
				tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel(userPropertyHandler.i18nColumnDescriptorLabelKey()));
				colPos++;
			}

			tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("table.role.useradmin"));
			colPos++;
			tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("table.role.groupadmin"));
			colPos++;
			tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("table.role.author"));
			colPos++;
			tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("table.role.admin"));
			colPos++;
			tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("table.role.status"));

			final FlexiTableDataModel tableDataModel = FlexiTableDataModelFactory.createFlexiTableDataModel(new OverviewModel(mergedDataChanges, colPos + 1),
					tableColumnModel);

			uifactory.addTableElement("newUsers", tableDataModel, formLayoutVertical);
		}

		/**
		 * compare roles of given identity with changes to be applied from wizard-step 01
		 * 
		 * @param identity
		 * @param securityGroup
		 * @param roleChangeMap
		 * @return
		 */
		private String getRoleStatusForIdentity(final Identity identity, final String securityGroup, final HashMap<String, String> roleChangeMap) {
			final BaseSecurity secMgr = BaseSecurityManager.getInstance();
			final SecurityGroup secGroup = secMgr.findSecurityGroupByName(securityGroup);
			Boolean isInGroup = secMgr.isIdentityInSecurityGroup(identity, secGroup);

			String thisRoleAction = "";
			if (roleChangeMap.containsKey(securityGroup)) {
				thisRoleAction = roleChangeMap.get(securityGroup);
			} else {
				return isInGroup.toString();
			}

			if ((isInGroup && thisRoleAction.equals("add")) || (!isInGroup && thisRoleAction.equals("remove"))) {
				return isInGroup.toString();
			} else {
				isInGroup = !isInGroup; // invert to represent the new state
				return "<span class=\"b_wizard_table_changedcell\">" + isInGroup.toString() + "</span>";
			}
		}
	}
}
