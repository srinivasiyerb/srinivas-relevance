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

import java.util.List;

import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.manager.BasicManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * <h3>Description:</h3> The user manager provides methods to handle user objects. This includes some search methods and methods for the users property handling.
 * <p>
 * Most search methods are not implemented on the user manager but rather on the security manager from the base security package. See ManagerFactory.getManager()
 * <p>
 * Initial Date: Jun 23, 2004 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public abstract class UserManager extends BasicManager {
	protected static UserManager INSTANCE;

	// injected by spring configuration
	protected UserPropertiesConfig userPropertiesConfig;
	protected UserNameAndPasswordSyntaxChecker userNameAndPasswordSyntaxChecker;

	/**
	 * Use getInstance method
	 */
	protected UserManager() {
		//
	}

	/**
	 * Factory method: Loads the user manager that is configured in the spring config
	 * 
	 * @return Instance of a UserManager
	 */
	public static final synchronized UserManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Create a transient user object. Use SaveUser to persist the object or use the createAndPersistUser method.
	 * 
	 * @param firstName
	 * @param lastName
	 * @param eMail
	 * @return New user instance
	 */
	public abstract User createUser(String firstName, String lastName, String eMail);

	/**
	 * @param firstName
	 * @param lastName
	 * @param email
	 * @return a persistant User.
	 */
	public abstract User createAndPersistUser(String firstName, String lastName, String email);

	/**
	 * Find the identity (and the user) that match the given email address. The match is an exact match
	 * 
	 * @param email The email search parameter
	 * @return The identity found for this email or null if not found
	 */
	public abstract Identity findIdentityByEmail(String email);

	/**
	 * Find user by its email
	 * 
	 * @param email that has to be searched
	 * @return User if the user has been found or null if not found
	 * @deprecated use findIdentityByEmail() instead
	 */
	@Deprecated
	public abstract User findUserByEmail(String email);

	/**
	 * Check if a user already used the e-mail address
	 * 
	 * @param email
	 * @return
	 */
	public abstract boolean userExist(String email);

	/**
	 * Find user by its key (database primary key)
	 * 
	 * @param key the primary key
	 * @return User if the user has been found or null if not found
	 */
	public abstract User loadUserByKey(Long key);

	/**
	 * Updates a user in the database.
	 * 
	 * @param usr The user object to be updated
	 * @return The true if successfully updated
	 */
	public abstract void updateUser(User usr);

	/**
	 * @param user The user to be saved
	 */
	public abstract void saveUser(User user);

	/**
	 * Updates the user object for a given identity
	 * 
	 * @param identity
	 * @return true if successful.
	 */
	public abstract boolean updateUserFromIdentity(Identity identity);

	/**
	 * Saves or updates the stringValue of the user's charset property
	 * 
	 * @param identity
	 * @param charset
	 */
	public abstract void setUserCharset(Identity identity, String charset);

	/**
	 * Normaly returns the stringValue of the user's charset property. If there is no charset property or it's not supported the default value is returned.
	 * 
	 * @param identity
	 * @return String charset
	 */
	public abstract String getUserCharset(Identity identity);

	/**
	 * Validates an OLAT password on a syntactical level.
	 * 
	 * @param password The passwort to validate
	 * @return true if it is valid, false otherwhise
	 */
	public boolean syntaxCheckOlatPassword(final String password) {
		return userNameAndPasswordSyntaxChecker.syntaxCheckOlatPassword(password);
	}

	/**
	 * Check if the login matches.
	 * 
	 * @param login
	 * @return True if synatx is ok.
	 */
	public boolean syntaxCheckOlatLogin(final String login) {
		return userNameAndPasswordSyntaxChecker.syntaxCheckOlatLogin(login);
	}

	// only package scope, used by user impl
	UserPropertiesConfig getUserPropertiesConfig() {
		return userPropertiesConfig;
	}

	/**
	 * Delete all user-properties which are deletable.
	 * 
	 * @param user
	 */
	public abstract void deleteUserProperties(User user);

	public List<UserPropertyHandler> getUserPropertyHandlersFor(final String usageIdentifyer, final boolean isAdministrativeUser) {
		return userPropertiesConfig.getUserPropertyHandlersFor(usageIdentifyer, isAdministrativeUser);
	}

	public Translator getPropertyHandlerTranslator(final Translator fallBack) {
		return getUserPropertiesConfig().getTranslator(fallBack);
	}

	/**
	 * added to be usable by user-bulkChange
	 * 
	 * @return
	 */
	public List<UserPropertyHandler> getAllUserPropertyHandlers() {
		return userPropertiesConfig.getAllUserPropertyHandlers();
	}

	public boolean isMandatoryUserProperty(final String usageIdentifyer, final UserPropertyHandler propertyHandler) {
		return userPropertiesConfig.isMandatoryUserProperty(usageIdentifyer, propertyHandler);
	}

	public boolean isUserViewReadOnly(final String usageIdentifyer, final UserPropertyHandler propertyHandler) {
		return userPropertiesConfig.isUserViewReadOnly(usageIdentifyer, propertyHandler);
	}

	/**
	 * Spring setter
	 * 
	 * @param userNameAndPasswordSyntaxChecker
	 */
	public void setUserNameAndPasswordSyntaxChecker(final UserNameAndPasswordSyntaxChecker userNameAndPasswordSyntaxChecker) {
		this.userNameAndPasswordSyntaxChecker = userNameAndPasswordSyntaxChecker;
	}

	/**
	 * Spring setter
	 * 
	 * @param userPropertiesConfig
	 */
	public void setUserPropertiesConfig(final UserPropertiesConfig userPropertiesConfig) {
		this.userPropertiesConfig = userPropertiesConfig;
	}

}
