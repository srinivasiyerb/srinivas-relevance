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
 * Copyright (c) 2007 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.user;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

import org.olat.basesecurity.IdentityImpl;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.Preferences;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.AssertException;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.i18n.I18nModule;
import org.olat.core.util.mail.MailHelper;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * <h3>Description:</h3> This implementation of the user manager manipulates user objects based on a hibernate implementation
 * <p>
 * Initial Date: 31.07.2007 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public class UserManagerImpl extends UserManager {
	// used to save user data in the properties table
	private static final String CHARSET = "charset";

	/**
	 * Use UserManager.getInstance(), this is a spring factory method to load the correct user manager
	 */
	private UserManagerImpl() {
		INSTANCE = this;
	}

	/**
	 * @see org.olat.user.UserManager#createUser(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public User createUser(final String firstName, final String lastName, final String eMail) {
		final User newUser = new UserImpl(firstName, lastName, eMail);
		final Preferences prefs = newUser.getPreferences();

		Locale loc;
		// for junit test case: use German Locale
		if (Settings.isJUnitTest()) {
			loc = Locale.GERMAN;
		} else {
			loc = I18nModule.getDefaultLocale();
		}
		// Locale loc
		prefs.setLanguage(loc.toString());
		prefs.setFontsize("normal");
		prefs.setPresenceMessagesPublic(false);
		prefs.setInformSessionTimeout(false);
		return newUser;
	}

	/**
	 * @see org.olat.user.UserManager#createAndPersistUser(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public User createAndPersistUser(final String firstName, final String lastName, final String email) {
		final User user = new UserImpl(firstName, lastName, email);
		DBFactory.getInstance().saveObject(user);
		return user;
	}

	/**
	 * @see org.olat.user.UserManager#findIdentityByEmail(java.lang.String)
	 */
	@Override
	public Identity findIdentityByEmail(final String email) {
		if (!MailHelper.isValidEmailAddress(email)) { throw new AssertException("Identity cannot be searched by email, if email is not valid. Used address: " + email); }

		final DB db = DBFactory.getInstance();
		final StringBuilder sb = new StringBuilder("select identity from ").append(IdentityImpl.class.getName()).append(" identity ")
				.append(" inner join identity.user user ").append(" where ");

		// search email
		final StringBuilder emailSb = new StringBuilder(sb);
		emailSb.append(" user.properties['").append(UserConstants.EMAIL).append("'] =:email");
		final DBQuery emailQuery = db.createQuery(emailSb.toString());
		emailQuery.setString("email", email);
		final List<Identity> identities = emailQuery.list();
		if (identities.size() > 1) { throw new AssertException("more than one identity found with email::" + email); }

		// search institutional email
		final StringBuilder institutionalSb = new StringBuilder(sb);
		institutionalSb.append(" user.properties['").append(UserConstants.INSTITUTIONALEMAIL).append("'] =:email");
		final DBQuery institutionalQuery = db.createQuery(institutionalSb.toString());
		institutionalQuery.setString("email", email);
		final List<Identity> instIdentities = institutionalQuery.list();
		if (instIdentities.size() > 1) { throw new AssertException("more than one identity found with institutional-email::" + email); }

		// check if email found in both fields && identity is not the same
		if ((identities.size() > 0) && (instIdentities.size() > 0) && (identities.get(0) != instIdentities.get(0))) { throw new AssertException(
				"found two identites with same email::" + email + " identity1=" + identities.get(0) + " identity2=" + instIdentities.get(0)); }
		if (identities.size() == 1) { return identities.get(0); }
		if (instIdentities.size() == 1) { return instIdentities.get(0); }
		return null;
	}

	/**
	 * @see org.olat.user.UserManager#findUserByEmail(java.lang.String)
	 */
	@Override
	public User findUserByEmail(final String email) {
		if (isLogDebugEnabled()) {
			logDebug("Trying to find user with email '" + email + "'");
		}

		final Identity ident = findIdentityByEmail(email);
		// if no user found return null
		if (ident == null) {
			if (isLogDebugEnabled()) {
				logDebug("Could not find user '" + email + "'");
			}
			return null;
		}
		return ident.getUser();
	}

	@Override
	public boolean userExist(final String email) {
		final DB db = DBFactory.getInstance();
		final StringBuilder sb = new StringBuilder("select distinct count(user) from ").append(UserImpl.class.getName()).append(" user where ");

		// search email
		final StringBuilder emailSb = new StringBuilder(sb);
		emailSb.append(" user.properties['").append(UserConstants.EMAIL).append("'] =:email");
		final DBQuery emailQuery = db.createQuery(emailSb.toString());
		emailQuery.setString("email", email);
		Number count = (Number) emailQuery.uniqueResult();
		if (count.intValue() > 0) { return true; }
		// search institutional email
		final StringBuilder institutionalSb = new StringBuilder(sb);
		institutionalSb.append(" user.properties['").append(UserConstants.INSTITUTIONALEMAIL).append("'] =:email");
		final DBQuery institutionalQuery = db.createQuery(institutionalSb.toString());
		institutionalQuery.setString("email", email);
		count = (Number) institutionalQuery.uniqueResult();
		if (count.intValue() > 0) { return true; }
		return false;
	}

	/**
	 * @see org.olat.user.UserManager#loadUserByKey(java.lang.Long)
	 */
	@Override
	public User loadUserByKey(final Long key) {
		return (UserImpl) DBFactory.getInstance().loadObject(UserImpl.class, key);
		// User not loaded yet (lazy initialization). Need to access
		// a field first to really load user from database.
	}

	/**
	 * @see org.olat.user.UserManager#updateUser(org.olat.core.id.User)
	 */
	@Override
	public void updateUser(final User usr) {
		if (usr == null) { throw new AssertException("User object is null!"); }
		DBFactory.getInstance().updateObject(usr);
	}

	/**
	 * @see org.olat.user.UserManager#saveUser(org.olat.core.id.User)
	 */
	@Override
	public void saveUser(final User user) {
		DBFactory.getInstance().saveObject(user);
	}

	/**
	 * @see org.olat.user.UserManager#updateUserFromIdentity(org.olat.core.id.Identity)
	 */
	@Override
	public boolean updateUserFromIdentity(final Identity identity) {
		this.updateUser(identity.getUser());
		return true;
	}

	/**
	 * @see org.olat.user.UserManager#setUserCharset(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public void setUserCharset(final Identity identity, final String charset) {
		final PropertyManager pm = PropertyManager.getInstance();
		final Property p = pm.findProperty(identity, null, null, null, CHARSET);

		if (p != null) {
			p.setStringValue(charset);
			pm.updateProperty(p);
		} else {
			final Property newP = pm.createUserPropertyInstance(identity, null, CHARSET, null, null, charset, null);
			pm.saveProperty(newP);
		}
	}

	/**
	 * @see org.olat.user.UserManager#getUserCharset(org.olat.core.id.Identity)
	 */
	@Override
	public String getUserCharset(final Identity identity) {
		String charset;
		charset = WebappHelper.getDefaultCharset();
		final PropertyManager pm = PropertyManager.getInstance();
		final Property p = pm.findProperty(identity, null, null, null, CHARSET);
		if (p != null) {
			charset = p.getStringValue();
			// if after migration the system does not support the charset choosen by a
			// user
			// (a rather rare case)
			if (!Charset.isSupported(charset)) {
				charset = WebappHelper.getDefaultCharset();
			}
		} else {
			charset = WebappHelper.getDefaultCharset();
		}
		return charset;
	}

	/**
	 * Delete all user-properties which are deletable.
	 * 
	 * @param user
	 */
	@Override
	public void deleteUserProperties(User user) {
		// prevent stale objects, reload first
		user = loadUserByKey(user.getKey());
		// loop over user fields and remove them form the database if they are
		// deletable
		final List<UserPropertyHandler> propertyHandlers = userPropertiesConfig.getAllUserPropertyHandlers();
		for (final UserPropertyHandler propertyHandler : propertyHandlers) {
			final String fieldName = propertyHandler.getName();
			if (propertyHandler.isDeletable()) {
				user.setProperty(fieldName, null);
			}
		}
		// persist changes
		updateUser(user);
		if (isLogDebugEnabled()) {
			logDebug("Delete all user-attributtes for user=" + user);
		}
	}

}
