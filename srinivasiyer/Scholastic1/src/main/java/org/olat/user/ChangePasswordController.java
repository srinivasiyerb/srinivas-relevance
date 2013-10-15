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

package org.olat.user;

import java.util.Iterator;
import java.util.List;

import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.basesecurity.Constants;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.ldap.LDAPError;
import org.olat.ldap.LDAPLoginModule;
import org.olat.ldap.ui.LDAPAuthenticationController;
import org.olat.login.OLATAuthenticationController;
import org.olat.login.SupportsAfterLoginInterceptor;
import org.olat.login.auth.OLATAuthManager;

/**
 * Initial Date: Jul 29, 2003
 * 
 * @author Felix Jost, Florian Gnaegi Comment: Subworkflow that allows the user to search for a user and choose the user from the list of users that match the search
 *         criteria. Users can be searched by
 *         <ul>
 *         <li>Username</li>
 *         <li>First name</li>
 *         <li>Last name</li>
 *         <li>Email address</li>
 *         </ul>
 */
public class ChangePasswordController extends BasicController implements SupportsAfterLoginInterceptor {

	private VelocityContainer myContent;
	private ChangePasswordForm chPwdForm;

	/**
	 * @param ureq
	 * @param wControl
	 */
	public ChangePasswordController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		// if a user is not allowed to change his/her own password, say it here
		if (!UserModule.isPwdchangeallowed()) {
			final String text = translate("notallowedtochangepwd", new String[] { WebappHelper.getMailConfig("mailSupport") });
			final Controller simpleMsg = MessageUIFactory.createSimpleMessage(ureq, wControl, text);
			listenTo(simpleMsg);// register controller to be disposed automatically on dispose of Change password controller
			putInitialPanel(simpleMsg.getInitialComponent());
			return;
		}

		final BaseSecurity mgr = BaseSecurityManager.getInstance();
		if (!mgr.isIdentityPermittedOnResourceable(ureq.getIdentity(), Constants.PERMISSION_ACCESS, OresHelper.lookupType(this.getClass()))) { throw new OLATSecurityException(
				"Insufficient permission to access ChangePasswordController"); }

		myContent = createVelocityContainer("pwd");
		// adds "provider_..." variables to myContent
		exposePwdProviders(ureq.getIdentity());

		chPwdForm = new ChangePasswordForm(ureq, wControl);
		listenTo(chPwdForm);

		myContent.put("chpwdform", chPwdForm.getInitialComponent());

		putInitialPanel(myContent);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == chPwdForm) {
			if (event == Event.DONE_EVENT) {

				final String oldPwd = chPwdForm.getOldPasswordValue();
				Identity provenIdent = null;

				if (BaseSecurityManager.getInstance().findAuthentication(ureq.getIdentity(), LDAPAuthenticationController.PROVIDER_LDAP) != null) {
					final LDAPError ldapError = new LDAPError();
					// fallback to OLAT if enabled happen automatically in LDAPAuthenticationController
					provenIdent = LDAPAuthenticationController.authenticate(ureq.getIdentity().getName(), oldPwd, ldapError);
				} else if (BaseSecurityManager.getInstance().findAuthentication(ureq.getIdentity(), BaseSecurityModule.getDefaultAuthProviderIdentifier()) != null) {
					provenIdent = OLATAuthenticationController.authenticate(ureq.getIdentity().getName(), oldPwd);
				}

				if (provenIdent == null) {
					showError("error.password.noauth");
				} else {
					final String newPwd = chPwdForm.getNewPasswordValue();
					if (OLATAuthManager.changePassword(ureq.getIdentity(), provenIdent, newPwd)) {
						// TODO: verify that we are NOT in a transaction (changepwd should be commited immediately)
						getLogger().audit("Changed password for identity." + provenIdent.getName());
						showInfo("password.successful");
					} else {
						showError("password.failed");
					}
				}
			} else if (event == Event.CANCELLED_EVENT) {
				removeAsListenerAndDispose(chPwdForm);
				chPwdForm = new ChangePasswordForm(ureq, getWindowControl());
				listenTo(chPwdForm);
				myContent.put("chpwdform", chPwdForm.getInitialComponent());
			}
		}
	}

	private void exposePwdProviders(final Identity identity) {
		// check if user has OLAT provider
		final List<Authentication> authentications = BaseSecurityManager.getInstance().getAuthentications(identity);
		final Iterator<Authentication> iter = authentications.iterator();
		while (iter.hasNext()) {
			myContent.contextPut("provider_" + (iter.next()).getProvider(), Boolean.TRUE);
		}

		// LDAP Module propagate changes to password
		if (LDAPLoginModule.isPropagatePasswordChangedOnLdapServer()) {
			myContent.contextPut("provider_LDAP_pwdchange", Boolean.TRUE);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}
}
