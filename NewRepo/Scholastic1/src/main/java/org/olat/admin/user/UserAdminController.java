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

import java.util.Locale;

import org.olat.admin.policy.PolicyController;
import org.olat.admin.user.groups.GroupOverviewController;
import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.basesecurity.Constants;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.properties.Property;
import org.olat.user.ChangePrefsController;
import org.olat.user.DisplayPortraitController;
import org.olat.user.ProfileAndHomePageEditController;
import org.olat.user.PropFoundEvent;
import org.olat.user.UserPropertiesController;

/**
 * Initial Date: Jul 29, 2003
 * 
 * @author Sabina Jeger
 * 
 *         <pre>
 *  Complete rebuild on 17. jan 2006 by Florian Gnaegi
 *  
 *  Functionality to change or view all kind of things for this user 
 *  based on the configuration for the user manager. 
 *  This controller should only be used by the UserAdminMainController.
 * 
 * </pre>
 */
public class UserAdminController extends BasicController implements Activateable {

	// NLS support

	private static final String NLS_ERROR_NOACCESS_TO_USER = "error.noaccess.to.user";
	private static final String NLS_FOUND_PROPERTY = "found.property";
	private static final String NLS_EDIT_UPROFILE = "edit.uprofile";
	private static final String NLS_EDIT_UPREFS = "edit.uprefs";
	private static final String NLS_EDIT_UPWD = "edit.upwd";
	private static final String NLS_EDIT_UAUTH = "edit.uauth";
	private static final String NLS_EDIT_UPROP = "edit.uprop";
	private static final String NLS_EDIT_UPOLICIES = "edit.upolicies";
	private static final String NLS_EDIT_UROLES = "edit.uroles";
	private static final String NLS_EDIT_UQUOTA = "edit.uquota";
	private static final String NLS_VIEW_GROUPS = "view.groups";

	private VelocityContainer myContent;

	private Identity myIdentity = null;

	// controllers used in tabbed pane
	private TabbedPane userTabP;
	private Controller prefsCtr, propertiesCtr, pwdCtr, quotaCtr, policiesCtr, rolesCtr, userShortDescrCtr;
	private DisplayPortraitController portraitCtr;
	private UserAuthenticationsEditorController authenticationsCtr;
	private Link backLink;
	private ProfileAndHomePageEditController userProfileCtr;
	private GroupOverviewController grpCtr;

	/**
	 * Constructor that creates a back - link as default
	 * 
	 * @param ureq
	 * @param wControl
	 * @param identity
	 */
	public UserAdminController(final UserRequest ureq, final WindowControl wControl, final Identity identity) {
		super(ureq, wControl);
		final BaseSecurity mgr = BaseSecurityManager.getInstance();
		if (!mgr.isIdentityPermittedOnResourceable(ureq.getIdentity(), Constants.PERMISSION_ACCESS, OresHelper.lookupType(this.getClass()))) { throw new OLATSecurityException(
				"Insufficient permissions to access UserAdminController"); }

		myIdentity = identity;

		if (allowedToManageUser(ureq, myIdentity)) {
			myContent = this.createVelocityContainer("udispatcher");
			backLink = LinkFactory.createLinkBack(myContent, this);
			userShortDescrCtr = new UserShortDescription(ureq, wControl, identity);
			myContent.put("userShortDescription", userShortDescrCtr.getInitialComponent());

			setBackButtonEnabled(true); // default
			initTabbedPane(myIdentity, ureq);
			exposeUserDataToVC(ureq, myIdentity);
			this.putInitialPanel(myContent);
		} else {
			final String supportAddr = WebappHelper.getMailConfig("mailSupport");
			this.showWarning(NLS_ERROR_NOACCESS_TO_USER, supportAddr);
			this.putInitialPanel(new Panel("empty"));
		}
	}

	/**
	 * Possible activation parameters are: edit.uprofile edit.uprefs edit.upwd edit.uauth edit.uprop edit.upolicies edit.uroles edit.uquota
	 * 
	 * @param ureq
	 * @param viewIdentifier
	 */
	@Override
	public void activate(final UserRequest ureq, final String viewIdentifier) {
		if (userTabP != null) {
			userTabP.setSelectedPane(translate(viewIdentifier));
			// do nothing if not initialized
		}
	}

	/**
	 * @param backButtonEnabled
	 */
	public void setBackButtonEnabled(final boolean backButtonEnabled) {
		if (myContent != null) {
			myContent.contextPut("showButton", Boolean.valueOf(backButtonEnabled));
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == backLink) {
			fireEvent(ureq, Event.BACK_EVENT);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == propertiesCtr) {
			if (event.getCommand().equals("PropFound")) {
				final PropFoundEvent foundEvent = (PropFoundEvent) event;
				final Property myfoundProperty = foundEvent.getProperty();
				this.showInfo(NLS_FOUND_PROPERTY, myfoundProperty.getKey().toString());
			}
		} else if (source == pwdCtr) {
			if (event == Event.DONE_EVENT) {
				// rebuild authentication tab, could be wrong now
				if (authenticationsCtr != null) {
					authenticationsCtr.rebuildAuthenticationsTableDataModel();
				}
			}
		} else if (source == userProfileCtr) {
			if (event == Event.DONE_EVENT) {
				// reload profile data on top
				myIdentity = (Identity) DBFactory.getInstance().loadObject(myIdentity);
				exposeUserDataToVC(ureq, myIdentity);
				userProfileCtr.resetForm(ureq, getWindowControl());
			}
		}
	}

	/**
	 * Check if user allowed to modify this identity. Only modification of user that have lower rights is allowed. No one exept admins can manage usermanager and admins
	 * 
	 * @param ureq
	 * @param identity
	 * @return boolean
	 */
	private boolean allowedToManageUser(final UserRequest ureq, final Identity identity) {
		final boolean isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		if (isOlatAdmin) { return true; }

		final BaseSecurity secmgr = BaseSecurityManager.getInstance();
		// only admins can administrate admin and usermanager users
		final boolean isAdmin = secmgr.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_ADMIN);
		final boolean isUserManager = secmgr.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_USERMANAGER);
		if (isAdmin || isUserManager) { return false; }
		// if user is author ony allowed to edit if configured
		final boolean isAuthor = secmgr.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);
		final Boolean canManageAuthor = BaseSecurityModule.USERMANAGER_CAN_MANAGE_AUTHORS;
		if (isAuthor && !canManageAuthor.booleanValue()) { return false; }
		// if user is groupmanager ony allowed to edit if configured
		final boolean isGroupManager = secmgr.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GROUPMANAGER);
		final Boolean canManageGroupmanager = BaseSecurityModule.USERMANAGER_CAN_MANAGE_GROUPMANAGERS;
		if (isGroupManager && !canManageGroupmanager.booleanValue()) { return false; }
		// if user is guest ony allowed to edit if configured
		final boolean isGuestOnly = secmgr.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GUESTONLY);
		final Boolean canManageGuest = BaseSecurityModule.USERMANAGER_CAN_MANAGE_GUESTS;
		if (isGuestOnly && !canManageGuest.booleanValue()) { return false; }
		// passed all tests, current user is allowed to edit given identity
		return true;
	}

	/**
	 * Initialize the tabbed pane according to the users rights and the system configuration
	 * 
	 * @param identity
	 * @param ureq
	 */
	private void initTabbedPane(final Identity identity, final UserRequest ureq) {
		// first Initialize the user details tabbed pane
		final boolean isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		userTabP = new TabbedPane("userTabP", ureq.getLocale());

		/**
		 * Determine, whether the user admin is or is not able to edit all fields in user profile form. The system admin is always able to do so.
		 */
		Boolean canEditAllFields = BaseSecurityModule.USERMANAGER_CAN_EDIT_ALL_PROFILE_FIELDS;
		if (BaseSecurityManager.getInstance().isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_ADMIN)) {
			canEditAllFields = Boolean.TRUE;
		}

		userProfileCtr = new ProfileAndHomePageEditController(ureq, getWindowControl(), identity, canEditAllFields.booleanValue());
		listenTo(userProfileCtr);
		userTabP.addTab(translate(NLS_EDIT_UPROFILE), userProfileCtr.getInitialComponent());

		prefsCtr = new ChangePrefsController(ureq, getWindowControl(), identity);
		userTabP.addTab(translate(NLS_EDIT_UPREFS), prefsCtr.getInitialComponent());

		final Boolean canChangePwd = BaseSecurityModule.USERMANAGER_CAN_MODIFY_PWD;
		if (canChangePwd.booleanValue() || isOlatAdmin) {
			// show pwd form only if user has also right to create new passwords in case
			// of a user that has no password yet
			final Boolean canCreatePwd = BaseSecurityModule.USERMANAGER_CAN_CREATE_PWD;
			final Authentication OLATAuth = BaseSecurityManager.getInstance().findAuthentication(identity, BaseSecurityModule.getDefaultAuthProviderIdentifier());
			if (OLATAuth != null || canCreatePwd.booleanValue() || isOlatAdmin) {
				pwdCtr = new UserChangePasswordController(ureq, getWindowControl(), identity);
				this.listenTo(pwdCtr); // listen when finished to update authentications model
				userTabP.addTab(translate(NLS_EDIT_UPWD), pwdCtr.getInitialComponent());
			}
		}

		final Boolean canAuth = BaseSecurityModule.USERMANAGER_ACCESS_TO_AUTH;
		if (canAuth.booleanValue() || isOlatAdmin) {
			authenticationsCtr = new UserAuthenticationsEditorController(ureq, getWindowControl(), identity);
			userTabP.addTab(translate(NLS_EDIT_UAUTH), authenticationsCtr.getInitialComponent());
		}

		final Boolean canProp = BaseSecurityModule.USERMANAGER_ACCESS_TO_PROP;
		if (canProp.booleanValue() || isOlatAdmin) {
			propertiesCtr = new UserPropertiesController(ureq, getWindowControl(), identity);
			this.listenTo(propertiesCtr);
			userTabP.addTab(translate(NLS_EDIT_UPROP), propertiesCtr.getInitialComponent());
		}

		final Boolean canPolicies = BaseSecurityModule.USERMANAGER_ACCESS_TO_POLICIES;
		if (canPolicies.booleanValue() || isOlatAdmin) {
			policiesCtr = new PolicyController(ureq, getWindowControl(), identity);
			userTabP.addTab(translate(NLS_EDIT_UPOLICIES), policiesCtr.getInitialComponent());
		}

		final Boolean canStartGroups = BaseSecurityModule.USERMANAGER_CAN_START_GROUPS;
		grpCtr = new GroupOverviewController(ureq, getWindowControl(), identity, canStartGroups);
		listenTo(grpCtr);
		userTabP.addTab(translate(NLS_VIEW_GROUPS), grpCtr.getInitialComponent());

		rolesCtr = new SystemRolesAndRightsController(getWindowControl(), ureq, identity);
		userTabP.addTab(translate(NLS_EDIT_UROLES), rolesCtr.getInitialComponent());

		final Boolean canQuota = BaseSecurityModule.USERMANAGER_ACCESS_TO_QUOTA;
		if (canQuota.booleanValue() || isOlatAdmin) {
			final String relPath = FolderConfig.getUserHomes() + "/" + identity.getName();
			quotaCtr = QuotaManager.getInstance().getQuotaEditorInstance(ureq, getWindowControl(), relPath, false);
			userTabP.addTab(translate(NLS_EDIT_UQUOTA), quotaCtr.getInitialComponent());
		}

		// now push to velocity
		myContent.put("userTabP", userTabP);
	}

	/**
	 * Add some user data to velocity container including the users portrait
	 * 
	 * @param ureq
	 * @param identity
	 */
	private void exposeUserDataToVC(final UserRequest ureq, final Identity identity) {
		final Locale loc = ureq.getLocale();
		myContent.contextPut("foundUserName", identity.getName());
		myContent.contextPut("foundFirstName", identity.getUser().getProperty(UserConstants.FIRSTNAME, loc));
		myContent.contextPut("foundLastName", identity.getUser().getProperty(UserConstants.LASTNAME, loc));
		myContent.contextPut("foundEmail", identity.getUser().getProperty(UserConstants.EMAIL, loc));
		removeAsListenerAndDispose(portraitCtr);
		portraitCtr = new DisplayPortraitController(ureq, getWindowControl(), identity, true, true);
		myContent.put("portrait", portraitCtr.getInitialComponent());
		removeAsListenerAndDispose(userShortDescrCtr);
		userShortDescrCtr = new UserShortDescription(ureq, getWindowControl(), identity);
		myContent.put("userShortDescription", userShortDescrCtr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// child controllers registered with listenTo get disposed in BasicController
		if (quotaCtr != null) {
			quotaCtr.dispose();
			quotaCtr = null;
		}
		if (authenticationsCtr != null) {
			authenticationsCtr.dispose();
			authenticationsCtr = null;
		}
		if (prefsCtr != null) {
			prefsCtr.dispose();
			prefsCtr = null;
		}
		if (policiesCtr != null) {
			policiesCtr.dispose();
			policiesCtr = null;
		}
		if (rolesCtr != null) {
			rolesCtr.dispose();
			rolesCtr = null;
		}
		if (portraitCtr != null) {
			portraitCtr.dispose();
			portraitCtr = null;
		}
		if (userShortDescrCtr != null) {
			userShortDescrCtr.dispose();
			userShortDescrCtr = null;
		}
	}

}