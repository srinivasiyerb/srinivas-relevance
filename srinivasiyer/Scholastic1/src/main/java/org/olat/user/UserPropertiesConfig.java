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

import java.util.List;

import org.olat.core.gui.translator.Translator;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * <h3>Description:</h3> The user properties configuration defines which user property handlers are available and provide methods to find out which user property is
 * visible and manipulatable by the user.
 * <p>
 * Initial Date: 31.07.2007 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public interface UserPropertiesConfig {

	/**
	 * Factory method to create a translator that can translate the user properties. E.g. gender is translated.
	 * 
	 * @param fallBack
	 * @return
	 */
	public Translator getTranslator(Translator fallBack);

	/**
	 * Get the property handler with the given name
	 * 
	 * @param propertyHandlerName
	 * @return
	 */
	public UserPropertyHandler getPropertyHandler(String propertyHandlerName);

	/**
	 * Checks if the given property is mandatory in the context of the usageIdentifyer. In forms this means that the field is rendered as mandatory, in tables it means
	 * that the table column is displayed as default configuration if user has a configurable table.
	 * 
	 * @param usageIdentifyer
	 * @param propertyHandler
	 * @return
	 */
	public boolean isMandatoryUserProperty(String usageIdentifyer, UserPropertyHandler propertyHandler);

	/**
	 * Checks if the given property can be edited by the regular user in the context of the usageIdentifyer. Admin users will override this setting.
	 * 
	 * @param usageIdentifyer
	 * @param propertyHandler
	 * @return
	 */
	public boolean isUserViewReadOnly(String usageIdentifyer, UserPropertyHandler propertyHandler);

	/**
	 * Get the user property handlers for the context of the usageIdentifyer. When the isAdminstrativeUser flag is set, all the configured properties are returned
	 * 
	 * @param usageIdentifyer
	 * @param isAdministrativeUser
	 * @return
	 */
	public List<UserPropertyHandler> getUserPropertyHandlersFor(String usageIdentifyer, boolean isAdministrativeUser);

	/**
	 * Get all available property handlers. Do not use this for forms or tables, use this only to cleanup things
	 * 
	 * @return
	 */
	public List<UserPropertyHandler> getAllUserPropertyHandlers();

}