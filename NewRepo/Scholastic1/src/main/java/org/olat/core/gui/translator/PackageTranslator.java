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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.gui.translator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import org.apache.log4j.Level;
import org.olat.core.helpers.Settings;
import org.olat.core.logging.LogDelegator;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;

/**
 * @author Felix Jost
 */
public class PackageTranslator extends LogDelegator implements Translator {
	private boolean fallBack;
	private Translator fallBackTranslator;
	private String packageName;
	private Locale locale;

	private PackageTranslator(String packageName, Locale locale, boolean fallBack, Translator fallBackTranslator) {
		this.locale = locale;
		this.packageName = packageName;
		this.fallBackTranslator = fallBackTranslator;
		this.fallBack = fallBack;
	}

	/**
	 * @param packageName
	 * @param locale
	 * @param fallBack
	 */
	private PackageTranslator(String packageName, Locale locale, boolean fallBack) {
		this(packageName, locale, fallBack, null);
	}

	/**
	 * cascade two translators
	 * 
	 * @param main
	 * @param fallback
	 * @return
	 */
	public static Translator cascadeTranslators(PackageTranslator main, Translator fallback) {
		return new PackageTranslator(main.packageName, main.locale, fallback);
	}

	/**
	 * @param packageName only the package use "class.getPackage().getName()" for it!
	 * @param locale
	 * @param fallBackTranslator
	 */
	public PackageTranslator(String packageName, Locale locale, Translator fallBackTranslator) {
		this(packageName, locale, false, fallBackTranslator);
	}

	/**
	 * default with fallback mode
	 * 
	 * @param packageName only the package use "class.getPackage().getName()" for it!
	 * @param locale
	 */
	public PackageTranslator(String packageName, Locale locale) {
		this(packageName, locale, true);
	}

	/**
	 * Translates the string from the packageName localization file.
	 * 
	 * @param key The key to translate
	 * @return The internationalized strings
	 */
	@Override
	public String translate(String key) {
		String val = translate(key, null);
		return val;
	}

	@Override
	public String translate(String key, String[] args) {
		return translate(key, args, Level.WARN);
	}

	/**
	 * @see org.olat.core.gui.translator.Translator#translate(java.lang.String, java.lang.String[])
	 */
	@Override
	public String translate(String key, String[] args, Level missingTranslationLogLevel) {
		String val = translate(key, args, false);
		// if still null -> fallback to default locale (if not in debug mode)
		if (val == null) {
			if (Settings.isDebuging()) {
				val = getErrorMessage(key);
			} else {
				// try with fallBackToDefaultLocale
				val = translate(key, args, true);
			}
		}
		// else value got translated or there is at least an error message telling
		// which key was not found.
		// Note: val may be null if there is a localstrings file missing in the default language. use the online translation tool to double-check

		// Error: ! even in default language: missing translation key!
		if (val == null) {
			val = getErrorMessage(key);
			// TODO: 13.02.2009 Workaround to fix shibboleth-attribute WARN : 'no translation ... in org.olat.course.condition...'
			if (!packageName.startsWith("org.olat.course.condition")) {
				if (missingTranslationLogLevel != null && !missingTranslationLogLevel.equals(Level.OFF)) {
					if (missingTranslationLogLevel.equals(Level.ERROR)) {
						logError(val, null);
					} else if (missingTranslationLogLevel.equals(Level.WARN)) {
						logWarn(val, null);
					} else if (missingTranslationLogLevel.equals(Level.INFO)) {
						logInfo(val, null);
					}
				}
			}
			// don't use error message in GUI for production, use key instead (OLAT-5896)
			if (!Settings.isDebuging()) {
				val = key;
			}
		}
		return val;
	}

	/**
	 * Recoursive lookup for a key. Used in translate(String key, String[] args). Should not be called directly, use translate(String key, String[] args). Must be public,
	 * because the definition is in an interface.
	 * 
	 * @see org.olat.core.gui.translator.Translator#translate(java.lang.String, java.lang.String[], boolean)
	 */
	@Override
	public String translate(String key, String[] args, boolean fallBackToDefaultLocale) {
		I18nManager i18n = I18nManager.getInstance();
		boolean overlayEnabled = I18nModule.isOverlayEnabled();
		String val = i18n.getLocalizedString(packageName, key, args, locale, overlayEnabled, fallBackToDefaultLocale);
		if (val == null) {
			// if not found, try the fallBackTranslator
			if (fallBackTranslator != null) {
				val = fallBackTranslator.translate(key, args, fallBackToDefaultLocale);
			} else if (fallBack) { // both fallback and fallbacktranslator does not
				// make sense; latest translator in chain should
				// fallback to application fallback.
				val = i18n.getLocalizedString(I18nModule.getApplicationFallbackBundle(), key, args, locale, overlayEnabled, fallBackToDefaultLocale);
				if (val == null) {
					// lastly fall back to brasato framework fallback
					val = i18n.getLocalizedString(I18nModule.getCoreFallbackBundle(), key, args, locale, overlayEnabled, fallBackToDefaultLocale);
				}
			}
		}
		return val;
	}

	/**
	 * Internal helper to format an error message for the given key
	 * 
	 * @param key
	 * @return
	 */
	private String getErrorMessage(String key) {

		StringBuilder sb = new StringBuilder(150);
		sb.append(NO_TRANSLATION_ERROR_PREFIX).append(key);
		sb.append(": in ").append(packageName);
		sb.append(" (fallback:").append(fallBack);
		String babel = fallBackTranslator == null ? "-" : fallBackTranslator.toString();
		sb.append(", fallBackTranslator:").append(babel);
		sb.append(") for locale ").append(locale);
		OLATRuntimeException ore = new OLATRuntimeException("transl dummy", null);
		// use stracktrace to find out more where the missing translation comes from
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		ore.printStackTrace(printWriter);

		sb.append(result.toString());
		return sb.toString();
	}

	/**
	 * @see org.olat.core.gui.translator.Translator#getLocale()
	 */
	@Override
	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * Not used normally. Sets the locale. Use only if e.g. a translator (which should then be an instance variable of a controller) is needed in the DMZ area where no
	 * user is logged in yet
	 * 
	 * @param locale The locale to set
	 */
	@Override
	public void setLocale(Locale locale) {
		this.locale = locale;
		if (fallBackTranslator != null) {
			fallBackTranslator.setLocale(locale);
		}
	}

	/**
	 * The package of this package translator
	 * 
	 * @return
	 */
	public String getPackageName() {
		return packageName;
	}

}