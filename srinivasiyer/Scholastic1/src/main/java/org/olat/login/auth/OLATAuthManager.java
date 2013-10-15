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
package org.olat.login.auth;

import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.Encoder;
import org.olat.ldap.LDAPError;
import org.olat.ldap.LDAPLoginManager;
import org.olat.ldap.LDAPLoginModule;
import org.olat.ldap.ui.LDAPAuthenticationController;
import org.olat.login.OLATAuthenticationController;

/**
 * Description:<br>
 * TODO:
 * <P>
 * Initial Date: 26.09.2007 <br>
 * 
 * @author Felix Jost, http://www.goodsolutions.ch
 */
public class OLATAuthManager extends BasicManager {

	private static OLog log = Tracing.createLoggerFor(OLATAuthenticationController.class);

	/**
	 * Change the password of an identity
	 * 
	 * @param doer Identity who is changing the password
	 * @param identity Identity who's password is beeing changed.
	 * @param newPwd New password.
	 * @return True upon success.
	 */
	public static boolean changePassword(final Identity doer, Identity identity, final String newPwd) {

		if (doer == null) { throw new AssertException("password changing identity cannot be undefined!"); }

		if (identity.getKey() == null) { throw new AssertException("cannot change password on a nonpersisted identity"); }
		// password's length is limited to 128 chars.
		// The 128 bit MD5 hash is converted into a 32 character long String
		final String hashedPwd = Encoder.encrypt(newPwd);

		// DBFactory.getInstance().reputInHibernateSessionCache(identity);
		// o_clusterREVIEW
		identity = (Identity) DBFactory.getInstance().loadObject(identity);

		boolean allOk = false;

		final Authentication ldapAuth = BaseSecurityManager.getInstance().findAuthentication(identity, LDAPAuthenticationController.PROVIDER_LDAP);
		if (ldapAuth != null) {
			if (LDAPLoginModule.isPropagatePasswordChangedOnLdapServer()) {
				final LDAPError ldapError = new LDAPError();
				final LDAPLoginManager ldapLoginManager = (LDAPLoginManager) CoreSpringFactory.getBean("org.olat.ldap.LDAPLoginManager");
				ldapLoginManager.changePassword(identity, newPwd, ldapError);
				log.audit(doer.getName() + " change the password on the LDAP server for identity: " + identity.getName());
				allOk = ldapError.isEmpty();

				if (allOk && LDAPLoginModule.isCacheLDAPPwdAsOLATPwdOnLogin()) {
					allOk &= changeOlatPassword(doer, identity, hashedPwd);
				}
			}
		} else {
			allOk = changeOlatPassword(doer, identity, hashedPwd);
		}
		return allOk;
	}

	private static boolean changeOlatPassword(final Identity doer, final Identity identity, final String hashedPwd) {
		Authentication auth = BaseSecurityManager.getInstance().findAuthentication(identity, BaseSecurityModule.getDefaultAuthProviderIdentifier());
		if (auth == null) { // create new authentication for provider OLAT
			auth = BaseSecurityManager.getInstance().createAndPersistAuthentication(identity, BaseSecurityModule.getDefaultAuthProviderIdentifier(), identity.getName(),
					hashedPwd);
			log.audit(doer.getName() + " created new authenticatin for identity: " + identity.getName());
		}

		auth.setCredential(hashedPwd);
		DBFactory.getInstance().updateObject(auth);
		log.audit(doer.getName() + " set new password for identity: " + identity.getName());
		return true;
	}

	/**
	 * to change password without knowing exactly who is changing it -> change as admin
	 * 
	 * @param identity
	 * @param newPwd
	 * @return
	 */
	public static boolean changePasswordAsAdmin(final Identity identity, final String newPwd) {
		final Identity adminUserIdentity = BaseSecurityManager.getInstance().findIdentityByName("administrator");
		return changePassword(adminUserIdentity, identity, newPwd);
	}

	/**
	 * to change password by password forgotten link at login screen
	 * 
	 * @param identity
	 * @param newPwd
	 * @return
	 */
	public static boolean changePasswordByPasswordForgottenLink(final Identity identity, final String newPwd) {
		return changePassword(identity, identity, newPwd);
	}

}
