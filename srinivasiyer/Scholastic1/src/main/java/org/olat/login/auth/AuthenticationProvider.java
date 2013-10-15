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

import java.util.Locale;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.creator.AutoCreator;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.StartupException;
import org.olat.core.util.Util;

/**
 * Initial Date: 04.08.2004
 * 
 * @author Mike Stock<br>
 *         Comment: An authentication provider authenticates users. It is initialized with the providerConfig.
 */

public class AuthenticationProvider implements ControllerCreator {

	private final String name;
	private final String clazz;
	private final boolean enabled;
	private final boolean isDefault;

	/**
	 * [used by spring] Authentication provider implementation. Gets its config from spring config file.
	 * 
	 * @param providerConfig
	 */
	private AuthenticationProvider(final String name, final String clazz, final boolean enabled, final boolean isDefault) {
		this.name = name;
		this.clazz = clazz;
		this.enabled = enabled;
		this.isDefault = isDefault;
		// double check config
		if (name == null || clazz == null) { throw new StartupException("Invalid AuthProvider: " + name + ". Please fix!"); }
	}

	/**
	 * @return True if this auth provider is enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @return True if this auth provider is the default provider
	 */
	public boolean isDefault() {
		return isDefault;
	}

	/**
	 * @return Name used to identify this authprovider.
	 */
	public String getName() {
		return name;
	}

	/**
	 * [used by velocity]
	 * 
	 * @param language
	 * @return Description text.
	 */
	@SuppressWarnings("unchecked")
	public String getDescription(final Locale locale) {
		final Translator trans = getPackageTranslatorForLocale(locale);
		final String desc = trans.translate("authentication.provider.description");
		return desc;
	}

	/**
	 * [used by velocity]
	 * 
	 * @param language
	 * @return Link text used to display a link to switch to this authentication provider.
	 */
	public String getLinktext(final Locale locale) {
		final Translator trans = getPackageTranslatorForLocale(locale);
		final String text = trans.translate("authentication.provider.linkText");
		return text;
	}

	@Override
	public Controller createController(final UserRequest lureq, final WindowControl lwControl) {
		final AutoCreator ac = new AutoCreator();
		ac.setClassName(clazz);
		return ac.createController(lureq, lwControl);
	}

	/**
	 * @param locale
	 * @return a translator for the package matching the authenticationProvider
	 */
	private Translator getPackageTranslatorForLocale(final Locale locale) {
		Class authProvClass = null;
		try {
			authProvClass = Class.forName(clazz);
		} catch (final ClassNotFoundException e) {
			throw new OLATRuntimeException("classname::" + clazz + " does no exist", e);
		}
		return Util.createPackageTranslator(authProvClass, locale);
	}

}
