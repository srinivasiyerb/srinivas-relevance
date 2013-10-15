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

package org.olat.admin.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.admin.user.bulkChange.UserBulkChangeManager;
import org.olat.admin.user.bulkChange.UserBulkChangeStep00;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.PermissionOnResourceable;
import org.olat.basesecurity.SecurityGroup;
import org.olat.basesecurity.events.SingleIdentityChosenEvent;
import org.olat.core.commons.fullWebApp.popup.BaseFullWebappPopupLayoutFactory;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.SelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.table.TableMultiSelectEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.gui.control.generic.popup.PopupBrowserWindow;
import org.olat.core.gui.control.generic.wizard.Step;
import org.olat.core.gui.control.generic.wizard.StepRunnerCallback;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.logging.AssertException;
import org.olat.core.servlets.WebDAVManager;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.ContactMessage;
import org.olat.login.LoginModule;
import org.olat.login.auth.AuthenticationProvider;
import org.olat.login.auth.WebDAVAuthManager;
import org.olat.modules.co.ContactFormController;
import org.olat.user.UserInfoMainController;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * Initial Date: Jan 31, 2006
 * 
 * @author gnaegi Description: This workflow has two constructors. The first one provides the user an advanced user search form with many search criterias that can be
 *         defined. The second one has the criterias in the constructor as attributes, so the search form won't appear. The following is a list with the search results.
 *         Form the list an identity can be selected which results in a SingleIdentityChosenEvent Alternatively a Canceled Event is fired.
 */
public class UsermanagerUserSearchController extends BasicController {

	private static final String CMD_MAIL = "exeMail";
	private static final String CMD_BULKEDIT = "bulkEditUsers";

	private final VelocityContainer userListVC;
	private VelocityContainer userSearchVC;
	private final VelocityContainer mailVC;
	private final Panel panel;

	private UsermanagerUserSearchForm searchform;
	private TableController tableCtr;
	private List<Identity> identitiesList, selectedIdentities;
	private final ArrayList<String> notUpdatedIdentities = new ArrayList<String>();
	private ExtendedIdentitiesTableDataModel tdm;
	private Identity foundIdentity = null;
	private ContactFormController contactCtr;
	private final Link backFromMail;
	private Link backFromList;
	private boolean showEmailButton = true;
	private StepsMainRunController userBulkChangeStepsController;
	private boolean isAdministrativeUser = false;

	/**
	 * Constructor to trigger the user search workflow using a generic search form
	 * 
	 * @param ureq
	 * @param wControl
	 */
	public UsermanagerUserSearchController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		userSearchVC = createVelocityContainer("usermanagerUsersearch");

		mailVC = createVelocityContainer("usermanagerMail");
		backFromMail = LinkFactory.createLinkBack(mailVC, this);

		userListVC = createVelocityContainer("usermanagerUserlist");

		backFromList = LinkFactory.createLinkBack(userListVC, this);

		userListVC.contextPut("showBackButton", Boolean.TRUE);
		userListVC.contextPut("emptyList", Boolean.FALSE);
		userListVC.contextPut("showTitle", Boolean.TRUE);

		searchform = new UsermanagerUserSearchForm(ureq, wControl);
		listenTo(searchform);

		userSearchVC.put("usersearch", searchform.getInitialComponent());

		panel = putInitialPanel(userSearchVC);
	}

	/**
	 * Constructor to trigger the user search workflow using the given attributes. The user has no possibility to manually search, the search will be performed using the
	 * constructor attributes.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param searchGroups
	 * @param searchPermissionOnResources
	 * @param searchAuthProviders
	 * @param searchCreatedAfter
	 * @param searchCreatedBefore
	 */
	public UsermanagerUserSearchController(final UserRequest ureq, final WindowControl wControl, final SecurityGroup[] searchGroups,
			final PermissionOnResourceable[] searchPermissionOnResources, final String[] searchAuthProviders, final Date searchCreatedAfter,
			final Date searchCreatedBefore, final Integer status, final boolean showEmailButton) {
		super(ureq, wControl);

		mailVC = createVelocityContainer("usermanagerMail");

		backFromMail = LinkFactory.createLinkBack(mailVC, this);

		userListVC = createVelocityContainer("usermanagerUserlist");
		this.showEmailButton = showEmailButton;

		userListVC.contextPut("showBackButton", Boolean.FALSE);
		userListVC.contextPut("showTitle", Boolean.TRUE);

		identitiesList = BaseSecurityManager.getInstance().getIdentitiesByPowerSearch(null, null, true, searchGroups, searchPermissionOnResources, searchAuthProviders,
				searchCreatedAfter, searchCreatedBefore, null, null, status);

		initUserListCtr(ureq, identitiesList, status);
		userListVC.put("userlist", tableCtr.getInitialComponent());
		userListVC.contextPut("emptyList", (identitiesList.size() == 0 ? Boolean.TRUE : Boolean.FALSE));

		panel = putInitialPanel(userListVC);
	}

	/**
	 * Constructor to trigger the user search workflow using the predefined list of identities. The user has no possibility to manually search.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param identitiesList
	 * @param status
	 * @param showEmailButton
	 */
	public UsermanagerUserSearchController(final UserRequest ureq, final WindowControl wControl, final List<Identity> identitiesList, final Integer status,
			final boolean showEmailButton, final boolean showTitle) {
		super(ureq, wControl);

		mailVC = createVelocityContainer("usermanagerMail");

		backFromMail = LinkFactory.createLinkBack(mailVC, this);

		userListVC = createVelocityContainer("usermanagerUserlist");
		this.showEmailButton = showEmailButton;

		userListVC.contextPut("showBackButton", Boolean.FALSE);
		userListVC.contextPut("showTitle", new Boolean(showTitle));

		initUserListCtr(ureq, identitiesList, status);
		userListVC.put("userlist", tableCtr.getInitialComponent());
		userListVC.contextPut("emptyList", (identitiesList.size() == 0 ? Boolean.TRUE : Boolean.FALSE));

		panel = putInitialPanel(userListVC);
	}

	/**
	 * Remove the given identites from the list of identites in the table model and reinitialize the table controller
	 * 
	 * @param ureq
	 * @param tobeRemovedIdentities
	 */
	public void removeIdentitiesFromSearchResult(final UserRequest ureq, final List<Identity> tobeRemovedIdentities) {
		PersistenceHelper.removeObjectsFromList(identitiesList, tobeRemovedIdentities);
		initUserListCtr(ureq, identitiesList, null);
		userListVC.put("userlist", tableCtr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {

		if (source == backFromMail) {
			panel.setContent(userListVC);
		} else if (source == backFromList) {
			panel.setContent(userSearchVC);
		}
	}

	/**
	 * Initialize the table controller using the list of identities
	 * 
	 * @param ureq
	 * @param identitiesList
	 */
	private void initUserListCtr(final UserRequest ureq, final List<Identity> myIdentities, final Integer searchStatusField) {
		boolean actionEnabled = true;
		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("error.no.user.found"));
		if ((searchStatusField != null) && (searchStatusField.equals(Identity.STATUS_DELETED))) {
			actionEnabled = false;
		}
		tdm = ExtendedIdentitiesTableControllerFactory.createTableDataModel(ureq, myIdentities, actionEnabled);

		removeAsListenerAndDispose(tableCtr);
		tableCtr = ExtendedIdentitiesTableControllerFactory.createController(tdm, ureq, getWindowControl(), actionEnabled);
		listenTo(tableCtr);

		if (showEmailButton) {
			tableCtr.addMultiSelectAction("command.mail", CMD_MAIL);
		}
		if (actionEnabled) {
			tableCtr.addMultiSelectAction("action.bulkedit", CMD_BULKEDIT);
		}
		if (showEmailButton || actionEnabled) {
			tableCtr.setMultiSelect(true);
		}
	}

	/**
	 * @return List of identities that match the criterias from the search form
	 */
	private List<Identity> findIdentitiesFromSearchForm() {
		final BaseSecurity secMgr = BaseSecurityManager.getInstance();
		// get user attributes from form
		final String login = searchform.getStringValue("login");
		Integer status = null;

		// get user fields from form
		// build user fields search map
		Map<String, String> userPropertiesSearch = new HashMap<String, String>();
		for (final UserPropertyHandler userPropertyHandler : searchform.getPropertyHandlers()) {
			if (userPropertyHandler == null) {
				continue;
			}
			final FormItem ui = searchform.getItem(userPropertyHandler.getName());
			final String uiValue = userPropertyHandler.getStringValue(ui);
			if (StringHelper.containsNonWhitespace(uiValue)) {
				userPropertiesSearch.put(userPropertyHandler.getName(), uiValue);
			}
		}
		if (userPropertiesSearch.isEmpty()) {
			userPropertiesSearch = null;
		}

		// get group memberships from form
		final List<SecurityGroup> groupsList = new ArrayList<SecurityGroup>();
		if (searchform.getRole("admin")) {
			final SecurityGroup group = secMgr.findSecurityGroupByName(org.olat.basesecurity.Constants.GROUP_ADMIN);
			groupsList.add(group);
		}
		if (searchform.getRole("author")) {
			final SecurityGroup group = secMgr.findSecurityGroupByName(org.olat.basesecurity.Constants.GROUP_AUTHORS);
			groupsList.add(group);
		}
		if (searchform.getRole("groupmanager")) {
			final SecurityGroup group = secMgr.findSecurityGroupByName(org.olat.basesecurity.Constants.GROUP_GROUPMANAGERS);
			groupsList.add(group);
		}
		if (searchform.getRole("usermanager")) {
			final SecurityGroup group = secMgr.findSecurityGroupByName(org.olat.basesecurity.Constants.GROUP_USERMANAGERS);
			groupsList.add(group);
		}
		if (searchform.getRole("oresmanager")) {
			final SecurityGroup group = secMgr.findSecurityGroupByName(org.olat.basesecurity.Constants.GROUP_INST_ORES_MANAGER);
			groupsList.add(group);
		}

		status = searchform.getStatus();

		final SecurityGroup[] groups = groupsList.toArray(new SecurityGroup[groupsList.size()]);

		// no permissions in this form so far
		final PermissionOnResourceable[] permissionOnResources = null;

		final String[] authProviders = searchform.getAuthProviders();

		// get date constraints from form
		final Date createdBefore = searchform.getBeforeDate();
		final Date createdAfter = searchform.getAfterDate();
		final Date userLoginBefore = searchform.getUserLoginBefore();
		final Date userLoginAfter = searchform.getUserLoginAfter();

		// now perform power search
		final List<Identity> myIdentities = secMgr.getIdentitiesByPowerSearch((login.equals("") ? null : login), userPropertiesSearch, true, groups,
				permissionOnResources, authProviders, createdAfter, createdBefore, userLoginAfter, userLoginBefore, status);

		return myIdentities;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == searchform) {
			if (event == Event.DONE_EVENT) {
				// form validation was ok
				identitiesList = findIdentitiesFromSearchForm();
				initUserListCtr(ureq, identitiesList, null);
				userListVC.put("userlist", tableCtr.getInitialComponent());
				userListVC.contextPut("emptyList", (identitiesList.size() == 0 ? Boolean.TRUE : Boolean.FALSE));
				panel.setContent(userListVC);
			} else if (event == Event.CANCELLED_EVENT) {
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
		} else if (source == tableCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				final TableEvent te = (TableEvent) event;
				final String actionid = te.getActionId();
				if (actionid.equals(ExtendedIdentitiesTableControllerFactory.COMMAND_SELECTUSER)) {
					final int rowid = te.getRowId();
					foundIdentity = tdm.getIdentityAt(rowid);
					// Tell parentController that a subject has been found
					fireEvent(ureq, new SingleIdentityChosenEvent(foundIdentity));
				} else if (actionid.equals(ExtendedIdentitiesTableControllerFactory.COMMAND_VCARD)) {
					// get identitiy and open new visiting card controller in new window
					final int rowid = te.getRowId();
					final Identity identity = tdm.getIdentityAt(rowid);
					final ControllerCreator userInfoMainControllerCreator = new ControllerCreator() {
						public Controller createController(final UserRequest lureq, final WindowControl lwControl) {
							return new UserInfoMainController(lureq, lwControl, identity);
						}
					};
					// wrap the content controller into a full header layout
					final ControllerCreator layoutCtrlr = BaseFullWebappPopupLayoutFactory.createAuthMinimalPopupLayout(ureq, userInfoMainControllerCreator);
					// open in new browser window
					final PopupBrowserWindow pbw = getWindowControl().getWindowBackOffice().getWindowManager().createNewPopupBrowserWindowFor(ureq, layoutCtrlr);
					pbw.open(ureq);
					//
				}
			}
			if (event instanceof TableMultiSelectEvent) {
				// Multiselect events
				final TableMultiSelectEvent tmse = (TableMultiSelectEvent) event;
				if (tmse.getAction().equals(CMD_BULKEDIT)) {
					if (tmse.getSelection().isEmpty()) {
						// empty selection
						showWarning("msg.selectionempty");
						return;
					}
					selectedIdentities = tdm.getIdentities(tmse.getSelection());
					final UserBulkChangeManager ubcMan = UserBulkChangeManager.getInstance();
					// valid selection: load in wizard
					final Step start = new UserBulkChangeStep00(ureq, selectedIdentities);
					final Roles roles = ureq.getUserSession().getRoles();
					isAdministrativeUser = (roles.isAuthor() || roles.isGroupManager() || roles.isUserManager() || roles.isOLATAdmin());

					// callback executed in case wizard is finished.
					final StepRunnerCallback finish = new StepRunnerCallback() {
						public Step execute(final UserRequest ureq1, final WindowControl wControl1, final StepsRunContext runContext) {
							// all information to do now is within the runContext saved
							boolean hasChanges = false;
							try {
								if (runContext.containsKey("validChange") && ((Boolean) runContext.get("validChange")).booleanValue()) {
									final HashMap<String, String> attributeChangeMap = (HashMap<String, String>) runContext.get("attributeChangeMap");
									final HashMap<String, String> roleChangeMap = (HashMap<String, String>) runContext.get("roleChangeMap");
									if (!(attributeChangeMap.size() == 0 && roleChangeMap.size() == 0)) {
										ubcMan.changeSelectedIdentities(selectedIdentities, attributeChangeMap, roleChangeMap, notUpdatedIdentities,
												isAdministrativeUser, getTranslator());
										hasChanges = true;
									}
								}
							} catch (final Exception any) {
								// return new ErrorStep
							}
							// signal correct completion and tell if changes were made or not.
							return hasChanges ? StepsMainRunController.DONE_MODIFIED : StepsMainRunController.DONE_UNCHANGED;
						}
					};

					removeAsListenerAndDispose(userBulkChangeStepsController);
					userBulkChangeStepsController = new StepsMainRunController(ureq, getWindowControl(), start, finish, null, translate("bulkChange.title"));
					listenTo(userBulkChangeStepsController);

					getWindowControl().pushAsModalDialog(userBulkChangeStepsController.getInitialComponent());

				} else if (tmse.getAction().equals(CMD_MAIL)) {
					if (tmse.getSelection().isEmpty()) {
						// empty selection
						showWarning("msg.selectionempty");
						return;
					}
					// create e-mail message
					final ContactMessage cmsg = new ContactMessage(ureq.getIdentity());

					selectedIdentities = tdm.getIdentities(tmse.getSelection());
					final ContactList contacts = new ContactList(translate("mailto.userlist"));
					contacts.addAllIdentites(selectedIdentities);
					cmsg.addEmailTo(contacts);

					// create contact form controller with ContactMessage
					removeAsListenerAndDispose(contactCtr);
					contactCtr = new ContactFormController(ureq, getWindowControl(), false, true, false, false, cmsg);
					listenTo(contactCtr);

					mailVC.put("mailform", contactCtr.getInitialComponent());
					panel.setContent(mailVC);
				}
			}
		} else if (source == contactCtr) {
			// in any case go back to list (events: done, failed or cancel)
			panel.setContent(userListVC);
		} else if (source == userBulkChangeStepsController) {
			if (event == Event.CANCELLED_EVENT) {
				getWindowControl().pop();
			} else if (event == Event.CHANGED_EVENT) {
				getWindowControl().pop();
				final Integer selIdentCount = selectedIdentities.size();
				if (notUpdatedIdentities.size() > 0) {
					final Integer notUpdatedIdentCount = notUpdatedIdentities.size();
					final Integer sucChanges = selIdentCount - notUpdatedIdentCount;
					String changeErrors = "";
					for (final String err : notUpdatedIdentities) {
						changeErrors += err + "<br />";
					}
					getWindowControl().setError(translate("bulkChange.partialsuccess", new String[] { sucChanges.toString(), selIdentCount.toString(), changeErrors }));
				} else {
					showInfo("bulkChange.success");
				}
				// update table model - has changed
				reloadDataModel(ureq);

			} else if (event == Event.DONE_EVENT) {
				showError("bulkChange.failed");
			}

		}
	}

	/**
	 * Reload the currently used identitiesList and rebuild the table controller
	 * 
	 * @param ureq
	 */
	private void reloadDataModel(final UserRequest ureq) {
		if (identitiesList == null) { return; }
		final BaseSecurity secMgr = BaseSecurityManager.getInstance();
		for (int i = 0; i < identitiesList.size(); i++) {
			final Identity ident = identitiesList.get(i);
			final Identity refrshed = secMgr.loadIdentityByKey(ident.getKey());
			identitiesList.set(i, refrshed);
		}
		initUserListCtr(ureq, identitiesList, null);
		userListVC.put("userlist", tableCtr.getInitialComponent());
	}

	/**
	 * Reload the identity used currently in the workflow and in the currently activated user table list model. The identity will be reloaded from the database to have
	 * accurate values.
	 */
	public void reloadFoundIdentity() {
		if (foundIdentity == null) { throw new AssertException("reloadFoundIdentity called but foundIdentity is null"); }
		// reload the found identity
		foundIdentity = (Identity) DBFactory.getInstance().loadObject(foundIdentity);
		// replace the found identity in the table list model to display changed
		// values
		final List identities = tdm.getObjects();
		PersistenceHelper.replaceObjectInListByKey(identities, foundIdentity);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		//
	}

}

/**
 * Initial Date: Jan 31, 2006
 * 
 * @author gnaegi Description: Search form for the usermanager power search. Should only be used by the UserManagerSearchController
 */
class UsermanagerUserSearchForm extends FormBasicController {
	private static final String formIdentifyer = UsermanagerUserSearchForm.class.getCanonicalName();
	private TextElement login;
	private SelectionElement roles;
	private SingleSelection status;
	private SelectionElement auth;
	private DateChooser beforeDate, afterDate, userLoginBefore, userLoginAfter;
	private FormLink searchButton;

	private final List<UserPropertyHandler> userPropertyHandlers;

	private final String[] statusKeys, statusValues;
	private final String[] roleKeys, roleValues;
	private final String[] authKeys, authValues;

	Map<String, FormItem> items;

	/**
	 * @param name
	 * @param cancelbutton
	 */
	public UsermanagerUserSearchForm(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		final UserManager um = UserManager.getInstance();
		userPropertyHandlers = um.getUserPropertyHandlersFor(formIdentifyer, true);

		items = new HashMap<String, FormItem>();

		roleKeys = new String[] { "admin", "author", "groupmanager", "usermanager", "oresmanager" };

		roleValues = new String[] { translate("search.form.constraint.admin"), translate("search.form.constraint.author"),
				translate("search.form.constraint.groupmanager"), translate("search.form.constraint.usermanager"), translate("search.form.constraint.oresmanager") };

		statusKeys = new String[] { Integer.toString(Identity.STATUS_VISIBLE_LIMIT), Integer.toString(Identity.STATUS_ACTIV),
				Integer.toString(Identity.STATUS_PERMANENT), Integer.toString(Identity.STATUS_LOGIN_DENIED) };
		statusValues = new String[] { translate("rightsForm.status.any.visible"), translate("rightsForm.status.activ"), translate("rightsForm.status.permanent"),
				translate("rightsForm.status.login_denied") };

		// take all providers from the config file
		// convention is that a translation key "search.form.constraint.auth." +
		// providerName
		// must exist. the element is stored using the name "auth." + providerName
		final List<String> authKeyList = new ArrayList<String>();
		final List<String> authValueList = new ArrayList<String>();

		final Collection<AuthenticationProvider> providers = LoginModule.getAuthenticationProviders();
		for (final AuthenticationProvider provider : providers) {
			if (provider.isEnabled()) {
				authKeyList.add(provider.getName());
				authValueList.add(translate("search.form.constraint.auth." + provider.getName()));
			}
		}
		if (WebDAVManager.getInstance().isEnabled()) {
			authKeyList.add(WebDAVAuthManager.PROVIDER_WEBDAV);
			authValueList.add(translate("search.form.constraint.auth.WEBDAV"));
		}

		// add additional no authentication element
		authKeyList.add("noAuth");
		authValueList.add(translate("search.form.constraint.auth.none"));

		authKeys = authKeyList.toArray(new String[authKeyList.size()]);
		authValues = authValueList.toArray(new String[authValueList.size()]);

		initForm(ureq);
	}

	public List<UserPropertyHandler> getPropertyHandlers() {
		return userPropertyHandlers;
	}

	protected Date getBeforeDate() {
		return beforeDate.getDate();
	}

	protected Date getAfterDate() {
		return afterDate.getDate();
	}

	protected Date getUserLoginBefore() {
		return userLoginBefore.getDate();
	}

	protected Date getUserLoginAfter() {
		return userLoginAfter.getDate();
	}

	protected FormItem getItem(final String name) {
		return items.get(name);
	}

	protected String getStringValue(final String key) {
		final FormItem f = items.get(key);
		if (f == null) { return null; }
		if (f instanceof TextElement) { return ((TextElement) f).getValue(); }
		return null;
	}

	protected boolean getRole(final String key) {
		return roles.isSelected(Arrays.asList(roleKeys).indexOf(key));
	}

	protected Integer getStatus() {
		return new Integer(status.getSelectedKey());
	}

	protected String[] getAuthProviders() {
		final List<String> apl = new ArrayList<String>();
		for (int i = 0; i < authKeys.length; i++) {
			if (auth.isSelected(i)) {
				if ("noAuth".equals(authKeys[i])) {
					apl.add(null);// special case
				} else {
					apl.add(authKeys[i]);
				}
			}
		}
		return apl.toArray(new String[apl.size()]);
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	@SuppressWarnings("unused")
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {

		login = uifactory.addTextElement("login", "search.form.login", 128, "", formLayout);
		items.put("login", login);

		final Translator tr = Util.createPackageTranslator(UserPropertyHandler.class, getLocale(), getTranslator());

		String currentGroup = null;
		// Add all available user fields to this form
		for (final UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			if (userPropertyHandler == null) {
				continue;
			}

			final FormItem fi = userPropertyHandler.addFormItem(getLocale(), null, getClass().getCanonicalName(), false, formLayout);

			fi.setTranslator(tr);
			items.put(fi.getName(), fi);

			final String group = userPropertyHandler.getGroup();
			if (!group.equals(currentGroup)) {
				if (currentGroup != null) {
					uifactory.addSpacerElement("spacer_" + group, formLayout, false);
				}
				currentGroup = group;
			}
		}

		uifactory.addSpacerElement("space1", formLayout, false);
		roles = uifactory.addCheckboxesVertical("roles", "search.form.title.roles", formLayout, roleKeys, roleValues, null, 1);

		uifactory.addSpacerElement("space2", formLayout, false);
		auth = uifactory.addCheckboxesVertical("auth", "search.form.title.authentications", formLayout, authKeys, authValues, null, 1);

		uifactory.addSpacerElement("space3", formLayout, false);
		status = uifactory.addRadiosVertical("status", "search.form.title.status", formLayout, statusKeys, statusValues);
		status.select(statusKeys[0], true);

		uifactory.addSpacerElement("space4", formLayout, false);
		afterDate = uifactory.addDateChooser("search.form.afterDate", "", formLayout);
		afterDate.setValidDateCheck("error.search.form.no.valid.datechooser");
		beforeDate = uifactory.addDateChooser("search.form.beforeDate", "", formLayout);
		beforeDate.setValidDateCheck("error.search.form.no.valid.datechooser");

		uifactory.addSpacerElement("space5", formLayout, false);
		userLoginAfter = uifactory.addDateChooser("search.form.userLoginAfterDate", "", formLayout);
		userLoginAfter.setValidDateCheck("error.search.form.no.valid.datechooser");
		userLoginBefore = uifactory.addDateChooser("search.form.userLoginBeforeDate", "", formLayout);
		userLoginBefore.setValidDateCheck("error.search.form.no.valid.datechooser");

		// creation date constraints
		/*
		 * addFormElement("space3", new SpacerElement(true, false)); addFormElement("title.date", new TitleElement("search.form.title.date")); afterDate = new
		 * DateElement("search.form.afterDate", getLocale()); addFormElement("afterDate", afterDate); beforeDate = new DateElement("search.form.beforeDate", getLocale());
		 * addFormElement("beforeDate", beforeDate); addSubmitKey("submit.search", "submit.search");
		 */

		uifactory.addSpacerElement("spaceBottom", formLayout, false);

		// Don't use submit button, form should not be marked as dirty since this is
		// not a configuration form but only a search form (OLAT-5626)
		searchButton = uifactory.addFormLink("search", formLayout, Link.BUTTON);
		searchButton.addActionListener(this, FormEvent.ONCLICK);

	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formInnerEvent(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.FormItem, org.olat.core.gui.components.form.flexible.impl.FormEvent)
	 */
	@Override
	@SuppressWarnings("unused")
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (source == searchButton) {
			source.getRootForm().submit(ureq);
		}
	}

	@Override
	protected void doDispose() {
		//
	}
}