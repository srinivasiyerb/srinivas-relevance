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

package org.olat.shibboleth;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.olat.core.configuration.AbstractOLATModule;
import org.olat.core.configuration.PersistedProperties;
import org.olat.core.logging.Tracing;
import org.olat.core.util.WebappHelper;
import org.olat.shibboleth.util.AttributeTranslator;

/**
 * Initial Date: 16.07.2004
 * 
 * @author Mike Stock Comment:
 */
public class ShibbolethModule extends AbstractOLATModule {
	/**
	 * Path identifier for shibboleth registration workflows.
	 */
	static final String PATH_REGISTER_SHIBBOLETH = "shibregister";
	private static final String CONF_ENABLE = "EnableShibbolethLogins";
	private static final String CONF_UNIQUEIDENTIFIER = "defaultUIDAttribute";
	private static final String CONF_USELANGUAGEINREQ = "UseLanguageInRequest";
	private static final String CONF_LANGUAGEPARAMNAM = "LanguageParamName";

	private static final String CONF_OLATUSERMAPPING_FIRSTNAME = "FirstName";
	private static final String CONF_OLATUSERMAPPING_LASTNAME = "LastName";
	private static final String CONF_OLATUSERMAPPING_EMAIL = "EMail";
	public static final String CONF_OLATUSERMAPPING_INSTITUTIONALNAME = "InstitutionalName";
	private static final String CONF_OLATUSERMAPPING_INSTITUTIONALEMAIL = "InstitutionalEMail";
	private static final String CONF_OLATUSERMAPPING_INSTITUTIONALUSERIDENTIFIER = "InstitutionalUserIdentifier";
	private static final String CONF_OLATUSERMAPPING_PREFERED_LANGUAGE = "PreferedLanguage";

	private static boolean enableShibbolethLogins = false;
	private static AttributeTranslator attributeTranslator;

	private static boolean useLanguageInReq = false;
	private static String languageParamName;
	private static List<String> operators;
	private static String defaultUIDAttribute;

	public static final String MULTIVALUE_SEPARATOR = ";";
	private static Map<String, String> userMapping;

	/**
	 * [used by spring]
	 */
	private ShibbolethModule() {
		//
	}

	@Override
	public void init() {}

	@Override
	protected void initDefaultProperties() {
		enableShibbolethLogins = getBooleanConfigParameter(CONF_ENABLE, false);
		if (enableShibbolethLogins) {
			Tracing.logInfo("Shibboleth logins enabled.", ShibbolethModule.class);
		} else {
			Tracing.logInfo("Shibboleth logins disabled.", ShibbolethModule.class);
		}

		useLanguageInReq = getBooleanConfigParameter(CONF_USELANGUAGEINREQ, true);
		languageParamName = getStringConfigParameter(CONF_LANGUAGEPARAMNAM, "en", true);
		if (useLanguageInReq) {
			Tracing.logInfo("Language code is sent as parameter in the AAI request with lang: " + languageParamName, ShibbolethModule.class);
		} else {
			Tracing.logInfo("Language code is not sent with AAI request.", ShibbolethModule.class);
		}

		defaultUIDAttribute = getStringConfigParameter(CONF_UNIQUEIDENTIFIER, "Shib-SwissEP-UniqueID", false);
	}

	/**
	 * [used by spring]
	 * 
	 * @param attributeTranslator
	 */
	public void setAttributeTranslator(final AttributeTranslator attributeTranslator) {
		ShibbolethModule.attributeTranslator = attributeTranslator;
	}

	/**
	 * [used by spring]
	 * 
	 * @param operators
	 */
	public void setOperators(final List<String> operators) {
		ShibbolethModule.operators = operators;
	}

	public static String getSanitizedFileLocation(String location) {
		if (location == null || location.length() == 0) { return null; }
		// try as URL
		try {
			new URL(location);
			return location;
		} catch (final MalformedURLException e) {
			// ok, we'll try files
		}
		// try as absolute file
		File fAbsFile = new File(location);
		if (fAbsFile.exists()) {
			try {
				return fAbsFile.toURL().toExternalForm();
			} catch (final MalformedURLException e2) {
				return null;
			}
		}
		// assemble as relative file
		if (!location.startsWith(location)) {
			location = "/" + location;
		}
		location = WebappHelper.getContextRoot() + location;
		fAbsFile = new File(location);
		if (fAbsFile.exists()) {
			try {
				return fAbsFile.toURL().toExternalForm();
			} catch (final MalformedURLException e2) {
				return null;
			}
		}
		return null;
	}

	// Getters and Setters //
	/**
	 * @return True if shibboleth logins are allowed.
	 */
	public static boolean isEnableShibbolethLogins() {
		return enableShibbolethLogins;
	}

	/**
	 * @return true if the language should be sent in the aai request
	 */
	public static boolean useLanguageInReq() {
		return useLanguageInReq;
	}

	/**
	 * @return the get request parameter name to be used sending the language code.
	 */
	public static String getLanguageParamName() {
		return languageParamName;
	}

	public static AttributeTranslator getAttributeTranslator() {
		return attributeTranslator;
	}

	public static String[] getRegisteredOperatorKeys() {
		return null;
	}

	public static List<String> getOperatorKeys() {
		return operators;
	}

	@Override
	protected void initFromChangedProperties() {
		// TODO Auto-generated method stub

	}

	public void setUserMapping(final Map<String, String> userMapping) {
		ShibbolethModule.userMapping = userMapping;
	}

	/**
	 * @return the shib. default attribute which identifies an user by an unique key
	 */
	public static String getDefaultUIDAttribute() {
		return defaultUIDAttribute;
	}

	/**
	 * @param attributesMap
	 * @return First Name value from shibboleth attributes.
	 */
	public static String getFirstName() {
		return userMapping.get(CONF_OLATUSERMAPPING_FIRSTNAME);
	}

	/**
	 * @return Last Name value from shibboleth attributes.
	 */
	public static String getLastName() {
		return userMapping.get(CONF_OLATUSERMAPPING_LASTNAME);
	}

	/**
	 * @return EMail value from shibboleth attributes.
	 */
	public static String getEMail() {
		return userMapping.get(CONF_OLATUSERMAPPING_EMAIL);
	}

	/**
	 * @return Institutional EMail value from shibboleth attributes.
	 */
	public static String getInstitutionalEMail() {
		return userMapping.get(CONF_OLATUSERMAPPING_INSTITUTIONALEMAIL);
	}

	/**
	 * @return Institutional Name value from shibboleth attributes.
	 */
	public static String getInstitutionalName() {
		return userMapping.get(CONF_OLATUSERMAPPING_INSTITUTIONALNAME);
	}

	/**
	 * @return Institutional User Identifyer value from shibboleth attributes.
	 */
	public static String getInstitutionalUserIdentifier() {
		return userMapping.get(CONF_OLATUSERMAPPING_INSTITUTIONALUSERIDENTIFIER);
	}

	/**
	 * @return Prefered language value from shibboleth attributes.
	 */
	public static String getPreferedLanguage() {
		return userMapping.get(CONF_OLATUSERMAPPING_PREFERED_LANGUAGE);
	}

	@Override
	public void setPersistedProperties(final PersistedProperties persistedProperties) {
		this.moduleConfigProperties = persistedProperties;
	}

	public static String getPreselectedAttributeKey(final String userAttribute) {
		final String shibKey = userMapping.get(userAttribute);
		return attributeTranslator.translateAttribute(shibKey);
	}

}
