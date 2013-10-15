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

package org.olat.core.util.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.olat.core.helpers.Settings;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.StartupException;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.AlwaysEmptyMap;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Formatter;
import org.olat.core.util.SortedProperties;
import org.olat.core.util.StringHelper;
import org.olat.core.util.UserSession;
import org.olat.core.util.WebappHelper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Description: <br>
 * I18nManager is responsible for internationalization issues, to store and load properties files in the available languages. 
 * 
 * <P>
 * Initial Date: 10.11.2004 <br>
 * 
 * @author Felix Jost
 */

public class I18nManager extends BasicManager {
	private static final String BUNDLE_INLINE_TRANSLATION_INTERCEPTOR = "org.olat.core.util.i18n.ui";
	private static final String BUNDLE_EXCEPTION = "org.olat.core.gui.exception";
	private static I18nManager INSTANCE;
	private static final OLog log = Tracing.createLoggerFor(I18nManager.class);
	public static final String FILE_NOT_FOUND_ERROR_PREFIX = ":::file not found";
	public static final String USESS_KEY_I18N_MARK_LOCALIZED_STRINGS = "I18N_MARK_LOCALIZED_STRINGS";

	public static final String IDENT_END_POSTFIX = "_end@";
	public static final String IDENT_START_POSTFIX = "_start@";
	public static final String IDENT_PREFIX = "@itt_";
	private static final String METADATA_KEY = "METADATA";
	private static final String METADATA_FILENAME = "i18nBundleMetadata.properties";
	public static final String METADATA_ANNOTATION_POSTFIX = ".annotation";
	public static final String METADATA_BUNDLE_PRIORITY_KEY = "bundle.priority";
	public static final String METADATA_KEY_PRIORITY_POSTFIX = ".priority";
	public static final String METADATA_KEY_INLINEREANSLATION_POSTFIX = ".inlinetranslation";
	public static final String METADATA_KEY_INLINEREANSLATION_VALUE_DISABLED = "disabled";
	public static final String I18N_DIRNAME = "_i18n";

	// pattern to find recursive keys in values: $org.olat.package:my.key
	private static final Pattern resolvingKeyPattern = Pattern.compile("\\$\\{?([\\w\\.\\-]*):([\\w\\.\\-]*[\\w\\-])\\}?");
	public static final int DEFAULT_BUNDLE_PRIORITY = 500;
	public static final int DEFAULT_KEY_PRIORITY = 500;

	/**
	 * Per-thread singleton holding the currently used Locale for translated messages and a flag if the translated strings should be marked with some markup.
	 */
	private static ThreadLocalLocale threadLocalLocale = new ThreadLocalLocale();
	private static ThreadLocalMarkLocalizedStrings threadLocalIsMarkLocalizedStringsEnabled = new ThreadLocalMarkLocalizedStrings();

	// value: name of
	// helpcourse

	// keys: bundlename ":" locale.toString() (e.g. "org.olat.admin:de_DE");
	// values: PropertyFile
	private Map<String, Properties> cachedBundles = new HashMap<String, Properties>();
	private Map<String, String> cachedJSTranslatorData = new HashMap<String, String>();
	private Map<String, Set<String>> referencingBundlesIndex = new HashMap<String, Set<String>>();
	private boolean cachingEnabled = true;

	private static FilenameFilter i18nFileFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			// don't add overlayLocales as selectable availableLanguages
			// (LocaleStrings_de__VENDOR.properties)
			if (name.startsWith(I18nModule.LOCAL_STRINGS_FILE_PREFIX) && name.indexOf("_") != 0 && name.endsWith(I18nModule.LOCAL_STRINGS_FILE_POSTFIX)) { return true; }
			return false;
		}
	};
	

	/**
	 * @return the manager
	 */
	public static I18nManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Get the translated value for a given i18n item. The returned translation is the pure translated value: no fallback mechanism, no overlay mechanism, no recursive
	 * resolving
	 * 
	 * @param i18nItem
	 * @param args the arguments used in this item or NULL if no such arguments needed
	 * @return The translated string.
	 */
	public String getLocalizedString(I18nItem i18nItem, Object[] args) {
		return getLocalizedString(i18nItem.getBundleName(), i18nItem.getKey(), args, i18nItem.getLocale(), false, false, false, false, 0);
	}

	/**
	 * Get a localized string. The translation order is as follows:
	 * <ol>
	 * <li>a) Look in overlay of given locale if overlay is configured</li>
	 * <li>b) Look in given locale</li>
	 * <li>c) Fallback to overlay of locale without variant if given locale has a variant</li>
	 * <li>d) Fallback to locale without variant if given locale has a variant</li>
	 * <li>e) Fallback to overlay of locale without country if given locale has a country</li>
	 * <li>f) Fallback to locale without country if given locale has a country</li>
	 * <li>g) Fallback to overlay of default locale</li>
	 * <li>h) Fallback to default locale</li>
	 * <li>i) Fallback to overlay of reference and fallback locale</li>
	 * <li>j) Fallback to reference and fallback locale</li>
	 * <li>k) Print error message</li>
	 * </ol>
	 * 
	 * @param bundlePath The bundle path
	 * @param key The key too lookup
	 * @param args The arguments used while formatting or NULL if no arguments
	 * @param locale The locale to use
	 * @param overlayEnabled true: lookup first in overlay; false: don't lookup in overlay
	 * @param fallBackToDefaultLocale true: fallback to default enabled; false: no fallback
	 * @return The formatted message in the given language or NULL if no fallback possible and not found
	 */
	public String getLocalizedString(String bundleName, String key, Object[] args, Locale locale, boolean overlayEnabled, boolean fallBackToDefaultLocale) {
		return getLocalizedString(bundleName, key, args, locale, overlayEnabled, fallBackToDefaultLocale, true, true, 0);
	}

	public String getLocalizedString(String bundleName, String key, Object[] args, Locale locale, boolean overlayEnabled, boolean fallBackToDefaultLocale,
			boolean fallBackToFallbackLocale, boolean resolveRecursively, int recursionLevel) {
		String msg = null;
		Properties properties = null;
		// a) If the overlay is enabled, lookup first in the overlay property
		// file
		if (overlayEnabled) {

			Locale overlayLocale = I18nModule.getOverlayLocales().get(locale);
			if (overlayLocale != null) {
				properties = getProperties(overlayLocale, bundleName, resolveRecursively, recursionLevel);
				if (properties != null) {
					msg = properties.getProperty(key);
					// if (log.isDebug() && msg == null) {
					// log.debug("Key::" + key + " not found in overlay::" + I18nModule.getOverlayName() + " for bundle::" + bundleName
					// + " and locale::" + locale.toString(), null);
					// }
				}
			}
		}
		// b) Otherwhise lookup in the regular bundle
		if (msg == null) {
			properties = getProperties(locale, bundleName, resolveRecursively, recursionLevel);
			// if LocalStrings File does not exist -> return error msg on screen
			// / fallback to default language
			if (properties == null) {
				if (Settings.isDebuging()) {
					log.warn(FILE_NOT_FOUND_ERROR_PREFIX + "! locale::" + locale.toString() + ", path::" + bundleName, null);
				}
			} else {
				msg = properties.getProperty(key);
			}
		}

		// The following fallback behaviour is similar to
		// java.util.ResourceBundle
		if (msg == null) {
			if (log.isDebug()) {
				log.debug("Key::" + key + " not found for bundle::" + bundleName + " and locale::" + locale.toString(), null);
			}
			// Fallback on the language if the locale has a country and/or a
			// variant
			// de_DE_variant -> de_DE -> de
			// Only after having all those checked we will fallback to the
			// default language
			// 1. Check on variant
			String variant = locale.getVariant();
			if (!variant.equals("")) {
				Locale newLoc = I18nModule.getAllLocales().get(locale.getLanguage() + "_" + locale.getCountry());
				if (newLoc != null) msg = getLocalizedString(bundleName, key, args, newLoc, overlayEnabled, false, fallBackToFallbackLocale, resolveRecursively,
						recursionLevel);
			}
			if (msg == null) {
				// 2. Check on country
				String country = locale.getCountry();
				if (!country.equals("")) {
					Locale newLoc = I18nModule.getAllLocales().get(locale.getLanguage());
					if (newLoc != null) msg = getLocalizedString(bundleName, key, args, newLoc, overlayEnabled, false, fallBackToFallbackLocale, resolveRecursively,
							recursionLevel);
				}
				// else we have an original locale with only a language given ->
				// no language specific fallbacks anymore
			}
		}

		if (msg == null) {
			// Message still empty? Use fallback to default language?
			// yes: return the call applied with the olatcore default locale
			// no: return null to indicate nothing was found so that callers may
			// use fallbacks
			if (fallBackToDefaultLocale) {
				return getLocalizedString(bundleName, key, args, I18nModule.getDefaultLocale(), overlayEnabled, false, fallBackToFallbackLocale, resolveRecursively,
						recursionLevel);
			} else {
				if (fallBackToFallbackLocale) {
					// fallback to fallback locale
					Locale fallbackLocale = I18nModule.getFallbackLocale();
					if (fallbackLocale.equals(locale)) {
						// finish when when already in fallback locale
						if (isLogDebugEnabled()) {
							logWarn("Could not find translation for bundle::" + bundleName + " and key::" + key + " ; not even in default or fallback packages", null);
						}
						return null;
					} else {
						return getLocalizedString(bundleName, key, args, fallbackLocale, overlayEnabled, false, false, resolveRecursively, recursionLevel);
					}
				} else {
					return null;
				}
			}
		}
		// When caching not enabled we need to check for keys contained in this
		// value. In caching mode this is already done while loading the
		// properties
		// file
		if (resolveRecursively && (!cachingEnabled || properties != null)) {
			msg = resolveValuesInternalKeys(locale, bundleName, key, properties, overlayEnabled, recursionLevel, msg);
		}

		// Add markup code to identify translated strings
		if (isCurrentThreadMarkLocalizedStringsEnabled() && !bundleName.startsWith(BUNDLE_INLINE_TRANSLATION_INTERCEPTOR) && !bundleName.startsWith(BUNDLE_EXCEPTION)
				&& isInlineTranslationEnabledForKey(bundleName, key)) {
			// identifyer consists of bundle name and key and an id to
			// distinguish multiple translations of the same key
			String identifyer = bundleName + ":" + key + ":" + CodeHelper.getRAMUniqueID();
			msg = IDENT_PREFIX + identifyer + IDENT_START_POSTFIX + msg + IDENT_PREFIX + identifyer + IDENT_END_POSTFIX;
		}
		// Add the the {0},{1} arguments to the GUI message
		if (args == null) {
			return msg;
		} else {
			// Escape single quotes with single quotes. Single quotes have special meaning in MessageFormat
			// See OLAT-5107, OLAT-5756
			if (msg.indexOf("'") > -1) {
				msg = msg.replaceAll("'", "''");
			}
			return MessageFormat.format(msg, args);
		}
	}

	/**
	 * Get the annotation for this i18n item. There exists only one annotation per key, it is shared between all languages
	 * 
	 * @param i18nItem
	 * @return the annotation or NULL if no annotation exists
	 */
	public String getAnnotation(I18nItem i18nItem) {
		Properties properties = getPropertiesWithoutResolvingRecursively(null, i18nItem.getBundleName());
		String key = i18nItem.getKey() + METADATA_ANNOTATION_POSTFIX;
		return properties.getProperty(key);
	}

	public synchronized void setAnnotation(I18nItem i18nItem, String annotation) {
		Properties properties = getPropertiesWithoutResolvingRecursively(null, i18nItem.getBundleName());
		String key = i18nItem.getKey() + METADATA_ANNOTATION_POSTFIX;
		if (StringHelper.containsNonWhitespace(annotation)) {
			properties.setProperty(key, annotation);
		} else if (properties.containsKey(key)) {
			properties.remove(key);
		}
		if (properties.size() == 0) {
			// delete empty files
			deleteProperties(null, i18nItem.getBundleName());
		} else {
			// update
			saveOrUpdateProperties(properties, null, i18nItem.getBundleName());
		}
	}

	/**
	 * Find all i18n items that exist in the target locale
	 * 
	 * @param targetLocale The locale that must be translated
	 * @param limitToBundleName
	 * @param includeBundlesChildren true: also find the keys in the bundles children; false: find only the keys in the exact bundle name. When limitToBundeName is set to
	 *            NULL the includeBundlesChildren will always be set to true
	 * @return List of i18n items
	 */
	public List<I18nItem> findExistingI18nItems(Locale targetLocale, String limitToBundleName, boolean includeBundlesChildren) {
		List<String> allBundles = I18nModule.getBundleNamesContainingI18nFiles();
		List<I18nItem> foundTranslationItems = new LinkedList<I18nItem>();
		for (String bundleName : allBundles) {
			if (limitToBundleName == null || limitToBundleName.equals(bundleName) || (includeBundlesChildren && bundleName.startsWith(limitToBundleName))) {
				Properties targetProperties = getResolvedProperties(targetLocale, bundleName);
				int bundlePriority = getBundlePriority(bundleName);
				Properties metadataProperties = getPropertiesWithoutResolvingRecursively(null, bundleName);
				Set<Object> keys = targetProperties.keySet(); // properties.stringPropertyNames()
				// is Java 1.6
				// only!
				for (Object keyObj : keys) {
					String key = (String) keyObj;
					int keyPriority = getKeyPriority(metadataProperties, key, bundleName);
					I18nItem i18nItem = new I18nItem(bundleName, key, targetLocale, bundlePriority, keyPriority);
					foundTranslationItems.add(i18nItem);
				}
			}
		}
		return foundTranslationItems;
	}

	/**
	 * Find all i18n items that exist in the source locale but not in the target locale
	 * 
	 * @param referenceLocale The locale that serves as the prototype
	 * @param targetLocale The locale that must be translated
	 * @param limitToBundleName
	 * @param includeBundlesChildren true: also find the keys in the bundles children; false: find only the keys in the exact bundle name. When limitToBundeName is set to
	 *            NULL the includeBundlesChildren will always be set to true
	 * @return List of i18n items
	 */
	public List<I18nItem> findMissingI18nItems(Locale referenceLocale, Locale targetLocale, String limitToBundleName, boolean includeBundlesChildren) {
		List<String> allBundles = I18nModule.getBundleNamesContainingI18nFiles();
		List<I18nItem> foundTranslationItems = new LinkedList<I18nItem>();
		for (String bundleName : allBundles) {
			if (limitToBundleName == null || limitToBundleName.equals(bundleName) || (includeBundlesChildren && bundleName.startsWith(limitToBundleName))) {
				Properties sourceProperties = getResolvedProperties(referenceLocale, bundleName);
				Properties targetProperties = getPropertiesWithoutResolvingRecursively(targetLocale, bundleName);
				Properties metadataProperties = getResolvedProperties(null, bundleName);
				int bundlePriority = getBundlePriority(bundleName);
				Set<Object> keys = sourceProperties.keySet(); // properties.stringPropertyNames()
				// is Java 1.6
				// only!
				for (Object keyObj : keys) {
					String key = (String) keyObj;
					if (!targetProperties.containsKey(key)) {
						int keyPriority = getKeyPriority(metadataProperties, key, bundleName);
						I18nItem i18nItem = new I18nItem(bundleName, key, targetLocale, bundlePriority, keyPriority);
						foundTranslationItems.add(i18nItem);
					}
				}
			}
		}
		return foundTranslationItems;
	}

	/**
	 * Find all i18n items that exist in the source locale and the target locale
	 * 
	 * @param referenceLocale The locale that serves as the prototype
	 * @param targetLocale The locale that must be translated
	 * @param limitToBundleName
	 * @param includeBundlesChildren true: also find the keys in the bundles children; false: find only the keys in the exact bundle name. When limitToBundeName is set to
	 *            NULL the includeBundlesChildren will always be set to true
	 * @return List of i18n items
	 */
	public List<I18nItem> findExistingAndMissingI18nItems(Locale referenceLocale, Locale targetLocale, String limitToBundleName, boolean includeBundlesChildren) {
		List<String> allBundles = I18nModule.getBundleNamesContainingI18nFiles();
		List<I18nItem> foundTranslationItems = new LinkedList<I18nItem>();
		for (String bundleName : allBundles) {
			if (limitToBundleName == null || limitToBundleName.equals(bundleName) || (includeBundlesChildren && bundleName.startsWith(limitToBundleName))) {
				// add from reference properties
				Properties referenceProperties = getResolvedProperties(referenceLocale, bundleName);
				Properties metadataProperties = getPropertiesWithoutResolvingRecursively(null, bundleName);
				int bundlePriority = getBundlePriority(bundleName);
				Set<Object> keys = referenceProperties.keySet(); // properties.stringPropertyNames()
				// is Java
				// 1.6 only!
				for (Object keyObj : keys) {
					String key = (String) keyObj;
					int keyPriority = getKeyPriority(metadataProperties, key, bundleName);
					I18nItem i18nItem = new I18nItem(bundleName, key, targetLocale, bundlePriority, keyPriority);
					foundTranslationItems.add(i18nItem);
				}
				// add from target properties
				Properties targetProperties = getResolvedProperties(targetLocale, bundleName);
				keys = targetProperties.keySet(); // properties.stringPropertyNames()
				// is Java 1.6 only!
				for (Object keyObj : keys) {
					String key = (String) keyObj;
					if (!referenceProperties.containsKey(key)) { // already
						// added
						int keyPriority = getKeyPriority(metadataProperties, key, bundleName);
						I18nItem i18nItem = new I18nItem(bundleName, key, targetLocale, bundlePriority, keyPriority);
						foundTranslationItems.add(i18nItem);
					}
				}
			}
		}
		return foundTranslationItems;
	}

	/**
	 * Find all i18n items that contain a given search string in their value. The search string can contain '*' as a wild-card
	 * 
	 * @param searchString The search string, case-insensitive. * are treated as wild-cards
	 * @param searchLocale The locale where to search
	 * @param targetLocale The locale that should be used as result target
	 * @param limitToBundleName The name of a bundle in which the keys should be searched or NULL to search in all bundles
	 * @param includeBundlesChildren true: also find the keys in the bundles children; false: find only the keys in the exact bundle name. When limitToBundeName is set to
	 *            NULL the includeBundlesChildren will always be set to true
	 * @return List of i18n items
	 */
	public List<I18nItem> findI18nItemsByValueSearch(String searchString, Locale searchLocale, Locale targetLocale, String limitToBundleName,
			boolean includeBundlesChildren) {
		List<String> allBundles = I18nModule.getBundleNamesContainingI18nFiles();
		List<I18nItem> foundTranslationItems = new LinkedList<I18nItem>();
		searchString = searchString.toLowerCase();
		String[] parts = searchString.split("\\*");
		// Build pattern
		String regexpSearchString = "^.*";
		for (String part : parts) {
			regexpSearchString += Pattern.quote(part) + ".*";
		}
		regexpSearchString += "$";
		Pattern p = Pattern.compile(regexpSearchString, Pattern.MULTILINE);
		// Search in all bundles and keys for that pattern
		for (String bundleName : allBundles) {
			if (limitToBundleName == null || limitToBundleName.equals(bundleName) || (includeBundlesChildren && bundleName.startsWith(limitToBundleName))) {
				Properties properties = getResolvedProperties(searchLocale, bundleName);
				Properties metadataProperties = getPropertiesWithoutResolvingRecursively(null, bundleName);
				int bundlePriority = getBundlePriority(bundleName);
				for (Map.Entry entry : properties.entrySet()) {
					String value = (String) entry.getValue();
					Matcher m = p.matcher(value.toLowerCase());
					if (m.find()) {
						String key = (String) entry.getKey();
						int keyPriority = getKeyPriority(metadataProperties, key, bundleName);
						I18nItem i18nItem = new I18nItem(bundleName, key, targetLocale, bundlePriority, keyPriority);
						foundTranslationItems.add(i18nItem);
					}
				}
			}
		}
		return foundTranslationItems;
	}

	/**
	 * Find all i18n items that contain the given search string in their key.
	 * 
	 * @param searchString The search string, case-insensitive
	 * @param searchLocale The language to search in for
	 * @param targetLocale The locale that should be used as result target
	 * @param limitToBundleName The name of a bundle in which the keys should be searched or NULL to search in all bundles
	 * @param includeBundlesChildren true: also find the keys in the bundles children; false: find only the keys in the exact bundle name. When limitToBundeName is set to
	 *            NULL the includeBundlesChildren will always be set to true
	 * @return List of i18n items
	 */
	public List<I18nItem> findI18nItemsByKeySearch(String searchString, Locale searchLocale, Locale targetLocale, String limitToBundleName, boolean includeBundlesChildren) {
		List<String> allBundles = I18nModule.getBundleNamesContainingI18nFiles();
		List<I18nItem> foundTranslationItems = new LinkedList<I18nItem>();
		searchString = searchString.toLowerCase();
		for (String bundleName : allBundles) {
			if (limitToBundleName == null || limitToBundleName.equals(bundleName) || (includeBundlesChildren && bundleName.startsWith(limitToBundleName))) {
				Properties properties = getPropertiesWithoutResolvingRecursively(searchLocale, bundleName);
				Properties metadataProperties = getPropertiesWithoutResolvingRecursively(null, bundleName);
				int bundlePriority = getBundlePriority(bundleName);
				Set<Object> keys = properties.keySet(); // properties.stringPropertyNames()
				// is Java 1.6 only!
				for (Object keyObj : keys) {
					String key = (String) keyObj;
					if (key.toLowerCase().indexOf(searchString) != -1) {
						int keyPriority = getKeyPriority(metadataProperties, key, bundleName);
						I18nItem i18nItem = new I18nItem(bundleName, key, targetLocale, bundlePriority, keyPriority);
						foundTranslationItems.add(i18nItem);
					}
				}
			}
		}
		return foundTranslationItems;
	}

	/**
	 * Factory method to create a single i18n item
	 * 
	 * @param bundleName
	 * @param key
	 * @param locale
	 * @return
	 */
	public I18nItem getI18nItem(String bundleName, String key, Locale locale) {
		int bundlePriority = getBundlePriority(bundleName);
		Properties metadataProperties = getPropertiesWithoutResolvingRecursively(null, bundleName);
		int keyPriority = getKeyPriority(metadataProperties, key, bundleName);
		return new I18nItem(bundleName, key, locale, bundlePriority, keyPriority);
	}

	/**
	 * Sort a list of i18n items. The list sorted alphabetically:
	 * <ol>
	 * <li>if afterBundlePriorities=true, the bundles are sorted by bundle priority</li>
	 * <li>if afterBundlePriorities=true and afterKeyPriorities=true, the bundles are sorted by bundle and then by key priority</li>
	 * <li>within the priorities, the bundles and keys are sorted alphabetically</li>
	 * </ol>
	 * 
	 * @param i18nItems
	 * @param afterBundlePriorities
	 * @param afterKeyPriorities
	 * @return
	 */
	public void sortI18nItems(List<I18nItem> i18nItems, final boolean afterBundlePriorities, final boolean afterKeyPriorities) {
		Comparator<I18nItem> comparator = new Comparator<I18nItem>() {
			public int compare(I18nItem item1, I18nItem item2) {
				// 1) compare bundle
				if (afterBundlePriorities) {
					int item1BundlePrio = item1.getBundlePriority();
					int item2BundlePrio = item2.getBundlePriority();
					if (item1BundlePrio < item2BundlePrio) return -1;
					if (item1BundlePrio > item2BundlePrio) return 1;
					// 2) in same bundle, compare key
					if (afterKeyPriorities) {
						int item1KeyPrio = item1.getKeyPriority();
						int item2KeyPrio = item2.getKeyPriority();
						if (item1KeyPrio < item2KeyPrio) return -1;
						if (item1KeyPrio > item2KeyPrio) return 1;
					}
				}
				// 3) same bundle or key prio or no prios used, compare
				// alphabetically
				// on bundle name
				int compareBundleNameResult = item1.getBundleName().compareTo(item2.getBundleName());
				if (compareBundleNameResult != 0) {
					return compareBundleNameResult;
				} else {
					// 4) in same bundle, compare alphabetically on key
					return item1.getKey().compareTo(item2.getKey());
				}
			}
		};
		Collections.sort(i18nItems, comparator);
	}

	/**
	 * Sort the bundle names alphabetically or by using the bundle priorities. Take care when sorting a list you previously got from the Module - this list is shared with
	 * all other users. Instead, use the sorted version
	 * 
	 * @param bundleNames
	 * @param afterBundlePriorities
	 */
	public void sortBundles(List<String> bundleNames, final boolean afterBundlePriorities) {
		Comparator<String> comparator = new Comparator<String>() {
			public int compare(String bundle1, String bundle2) {
				// 1) compare bundle priority
				if (afterBundlePriorities) {
					int bundle1Prio = getBundlePriority(bundle1);
					int bundle2Prio = getBundlePriority(bundle2);
					if (bundle1Prio < bundle2Prio) return -1;
					if (bundle1Prio > bundle2Prio) return 1;
				}
				// 2) compare alphabetically on bundle name
				return bundle1.compareTo(bundle2);
			}
		};
		Collections.sort(bundleNames, comparator);

	}

	/**
	 * Save the given value for the given i18nItem
	 * 
	 * @param i18nItem
	 * @param value
	 */
	public synchronized void saveOrUpdateI18nItem(I18nItem i18nItem, String value) {
		Properties properties = getPropertiesWithoutResolvingRecursively(i18nItem.getLocale(), i18nItem.getBundleName());
		// Add logging block to find bogus save issues
		if (isLogDebugEnabled()) {
			String itemIdent = i18nItem.getLocale() + ":" + buildI18nItemIdentifyer(i18nItem.getBundleName(), i18nItem.getKey());
			if (properties.containsKey(i18nItem.getKey())) {
				if (StringHelper.containsNonWhitespace(value)) {
					logDebug("Updating i18n item::" + itemIdent + " with new value::" + value, null);
				} else {
					logDebug("Deleting i18n item::" + itemIdent + " because new value is emty", null);
				}
			} else {
				if (StringHelper.containsNonWhitespace(value)) {
					logDebug("Creating i18n item::" + itemIdent + " with new value::" + value, null);
				}
			}
		}
		//
		if (StringHelper.containsNonWhitespace(value)) {
			properties.setProperty(i18nItem.getKey(), value);
		} else if (properties.containsKey(i18nItem.getKey())) {
			properties.remove(i18nItem.getKey());
		}
		if (properties.size() == 0) {
			// delete empty files
			deleteProperties(i18nItem.getLocale(), i18nItem.getBundleName());
		} else {
			// update
			saveOrUpdateProperties(properties, i18nItem.getLocale(), i18nItem.getBundleName());
		}
		// remove all properties files from cache that contain references to
		// this i18n item, rebuild them lazy on next demand.
		if (cachingEnabled) {
			String identifyer = buildI18nItemIdentifyer(i18nItem.getBundleName(), i18nItem.getKey());
			synchronized (referencingBundlesIndex) { // VM scope, clustersave
				Set<String> referencingBundles = referencingBundlesIndex.get(identifyer);
				if (referencingBundles != null) {
					// remove from index
					referencingBundlesIndex.remove(identifyer);
					// remove from bundles cache
					for (String bundleName : referencingBundles) {
						cachedBundles.remove(bundleName);
					}
				}
			}
		}

	}

	/**
	 * Count the i18n items in a bundle
	 * 
	 * @param locale
	 * @param limitToBundleName The name of a bundle for which the keys should be counted or NULL to count keys in every available bundle
	 * @param includeBundlesChildren true: also count the keys of the bundles children; false: count only the keys of the exact bundle name. When limitToBundeName is set
	 *            to NULL the includeBundlesChildren will always be set to true
	 * @return
	 */
	public int countI18nItems(Locale locale, String limitToBundleName, boolean includeBundlesChildren) {
		List<String> allBundles = I18nModule.getBundleNamesContainingI18nFiles();
		int counter = 0;
		for (String bundleName : allBundles) {
			if (limitToBundleName == null || limitToBundleName.equals(bundleName) || (includeBundlesChildren && bundleName.startsWith(limitToBundleName))) {
				Properties properties = getResolvedProperties(locale, bundleName);
				counter += properties.size();
			}
		}
		return counter;
	}

	/**
	 * Count the number of available bundles
	 * 
	 * @param limitToBundleName The name of a bundle or NULL to count every available bundle
	 * @param includeBundlesChildren true: also count the bundles children; false: count only the exact bundle name which will always be 1. When limitToBundeName is set
	 *            to NULL the includeBundlesChildren will always be set to true
	 * @return
	 */
	public int countBundles(String limitToBundleName, boolean includeBundlesChildren) {
		List<String> allBundles = I18nModule.getBundleNamesContainingI18nFiles();
		if (limitToBundleName == null) {
			return allBundles.size();
		} else if (!includeBundlesChildren) { return (allBundles.contains(limitToBundleName) ? 1 : 0); }
		// else count bundle plus its children
		int counter = 0;
		for (String bundleName : allBundles) {
			if (limitToBundleName == null || limitToBundleName.equals(bundleName) || (includeBundlesChildren && bundleName.startsWith(limitToBundleName))) {
				counter++;
			}
		}
		return counter;
	}

	/**
	 * @param locale
	 * @param bundleName
	 * @return the properties for the given locale and bundlename. When no file is found, an emtpy properties object will be returned
	 */
	public Properties getPropertiesWithoutResolvingRecursively(Locale locale, String bundleName) {
		return getProperties(locale, bundleName, false, 0);
	}

	public Properties getResolvedProperties(Locale locale, String bundleName) {
		return getProperties(locale, bundleName, true, 0);
	}

	private Properties getProperties(Locale locale, String bundleName, boolean resolveRecursively, int recursionLevel) {
		String key = calcPropertiesFileKey(locale, bundleName);
		// if (isLogDebugEnabled()) logDebug("getProperties for key::" + key + ", resolveRecursively::" + resolveRecursively
		// + ", recursionLevel::" + recursionLevel, null);
		Properties props;
		// boolean logDebug = isLogDebugEnabled();
		boolean logDebug = false; // hide messaged for the moment until we find the other issue
		// Try cache first, load if needed
		// o_clusterOK by:fj i18n files are static, read-only in production
		synchronized (cachedBundles) {
			props = cachedBundles.get(key);
			// Not loaded yet or use the unresolved version which is always read
			// from disk. When caching is disabled, the cache will always return
			// null (AlwaysEmptyMap).
			if (props == null || !resolveRecursively) {
				InputStream is = null;
				try {
					// Start with an empty property object
					// Use a sorted properties object that saves the keys sorted alphabetically to disk
					props = new SortedProperties();
					//
					// 1) Try to load the bundle from a configured source path
					// This is also used to load the overlay properties
					File baseDir = I18nModule.getPropertyFilesBaseDir(locale, bundleName);
					if (baseDir != null) {
						File f = getPropertiesFile(locale, bundleName, baseDir);
						// if file exists load properties from file, otherwise
						// proceed with 2)
						if (f.exists()) {
							is = new FileInputStream(f);
							if (logDebug) logDebug("loading LocalStrings from file::" + f.getAbsolutePath(), null);
						}
					}
					//
					// 2) Try to load from classpath
					String fileName = (locale == null ? METADATA_FILENAME : buildI18nFilename(locale));
					String relPath = bundleName.replace('.', '/') + "/" + I18N_DIRNAME + "/" + fileName;
					// No "/" at the beginning of the resource! since the
					// resource will not be found within jars
					if (is == null) {
						ClassLoader classLoader = this.getClass().getClassLoader();
						is = classLoader.getResourceAsStream(relPath);
						if (logDebug && is != null) logDebug("loading LocalStrings from classpath relpath::" + relPath, null);
					}
					
					// Now load the properties from resource (file, classpath or
					// langpacks)
					if (is != null) {
						props.load(is);
					}
				} catch (IOException e) {
					throw new AssertException("LocalStrings for key::" + key + " could not be loaded", e);
				} finally {
					try {
						if (is != null) is.close();
					} catch (IOException e) {
						// did our best
					}
				}

				// Try to resolve all keys within this properties and add to
				// cache
				if (resolveRecursively) {
					resolvePropertiesInternalKeys(locale, bundleName, props, I18nModule.isOverlayEnabled(), recursionLevel);
					cachedBundles.put(key, props);
				}
				if (locale == null) {
					// Add metadata files to cache as well
					cachedBundles.put(key, props);
				}
			}
		}
		return props;
	}

	/**
	 * Internal helper to resolve a key recursively within the property values. All values of the given properties are evaluated and replaced by the resolved values.
	 * <p>
	 * The recursion is limited to 10 levels to prevent endless loops
	 * 
	 * @param locale
	 * @param bundleName
	 * @param properties
	 * @param overlayEnabled true: lookup first in overlay; false: don't lookup in overlay
	 * @param recursionLevel the current recursion level. Incremented within this method
	 * @return true: the properties contains at least one resolved property; false: properties not modified
	 */
	private boolean resolvePropertiesInternalKeys(Locale locale, String bundleName, Properties properties, boolean overlayEnabled, int recursionLevel) {
		if (!cachingEnabled) {
			// Fails if caching is not enabled. In non-caching mode, the strings
			// are converted on the fly
			return false;
		}

		Set<Object> keys = properties.keySet();
		for (Object keyObj : keys) {
			String key = (String) keyObj;
			String value = properties.getProperty(key);
			String resolvedValue = resolveValuesInternalKeys(locale, bundleName, key, properties, overlayEnabled, recursionLevel, value);
			// Set new value
			properties.setProperty(key, resolvedValue);
		}
		return true;
	}

	/**
	 * Internal helper to resolve keys within the given value. The optional currentProperties can be used to improve performance when looking up something in the current
	 * properties file
	 * 
	 * @param locale
	 * @param bundleName
	 * @param key
	 * @param currentProperties
	 * @param overlayEnabled true: lookup first in overlay; false: don't lookup in overlay
	 * @param recursionLevel
	 * @param recursionLevel the current recursion level. Incremented within this method
	 * @return the resolved value
	 */
	private String resolveValuesInternalKeys(Locale locale, String bundleName, String key, Properties currentProperties, boolean overlayEnabled, int recursionLevel,
			String value) {
		if (recursionLevel > 9) {
			log.warn("Terminating resolving of properties after 10 levels, stopped in bundle::" + bundleName + " and key::" + key);
			return value;
		}
		recursionLevel++;

		StringBuffer resolvedValue = new StringBuffer();
		int lastPos = 0;
		Matcher matcher = resolvingKeyPattern.matcher(value);
		while (matcher.find()) {
			resolvedValue.append(value.substring(lastPos, matcher.start()));
			String toResolvedBundle = matcher.group(1);
			String toResolvedKey = matcher.group(2);
			if (toResolvedBundle == null || toResolvedBundle.equals("")) {
				// $:my.key is valid syntax, points to $current.bundle:my.key
				toResolvedBundle = bundleName;
			}
			if (toResolvedBundle.equals(bundleName) && currentProperties != null) {
				// Resolve within bundle
				String resolvedKey = currentProperties.getProperty(toResolvedKey);
				if (resolvedKey == null) {
					// Not found, use original (added in next iteration)
					lastPos = matcher.start();
				} else {
					resolvedValue.append(resolveValuesInternalKeys(locale, bundleName, toResolvedKey, currentProperties, overlayEnabled, recursionLevel, resolvedKey));
					lastPos = matcher.end();
				}
			} else {
				// Resolve using other bundle
				String resolvedKey = getLocalizedString(toResolvedBundle, toResolvedKey, null, locale, overlayEnabled, true, true, true, recursionLevel);
				if (StringHelper.containsNonWhitespace(resolvedKey)) {
					resolvedValue.append(resolvedKey);
					lastPos = matcher.end();
				} else {
					// Not found, use original (added in next iteration)
					lastPos = matcher.start();
				}
			}
			// add resolved key to references index
			if (cachingEnabled) {
				String identifyer = buildI18nItemIdentifyer(toResolvedBundle, toResolvedKey);
				synchronized (referencingBundlesIndex) { // VM scope,
					// clustersave
					Set<String> referencingBundles = referencingBundlesIndex.get(identifyer);
					if (referencingBundles == null) {
						referencingBundles = new HashSet<String>();
						referencingBundlesIndex.put(identifyer, referencingBundles);
					}
					referencingBundles.add(bundleName);
				}
			}
		}
		// Add rest of value
		resolvedValue.append(value.substring(lastPos));
		return resolvedValue.toString();
	}

	/**
	 * Create a new property file or update an existing property file form the given propeties object
	 * 
	 * @param properties The properties to persis
	 * @param locale The locale of the properties
	 * @param bundleName The properties bundle
	 */
	public void saveOrUpdateProperties(Properties properties, Locale locale, String bundleName) {
		String key = calcPropertiesFileKey(locale, bundleName);
		if (isLogDebugEnabled()) logDebug("saveOrUpdateProperties for key::" + key, null);
		synchronized (cachedBundles) {
			// 1) Save file to disk
			File baseDir = I18nModule.getPropertyFilesBaseDir(locale, bundleName);
			if (baseDir == null) { throw new AssertException("Can not save or update properties file for bundle::" + bundleName + " and language::" + locale.toString()
					+ " - no base directory found, probably loaded from jar!"); }
			File propertiesFile = getPropertiesFile(locale, bundleName, baseDir);
			OutputStream fileStream = null;
			try {
				// create necessary directories
				File directory = propertiesFile.getParentFile();
				if (!directory.exists()) directory.mkdirs();
				// write to file file now
				fileStream = new FileOutputStream(propertiesFile);
				properties.store(fileStream, null);
				fileStream.flush();
			} catch (FileNotFoundException e) {
				throw new OLATRuntimeException("Could not save or update to file::" + propertiesFile.getAbsolutePath(), e);
			} catch (IOException e) {
				throw new OLATRuntimeException("Could not save or update to file::" + propertiesFile.getAbsolutePath()
						+ ", maybe permission denied? Check your directory permissions", e);
			} finally {
				try {
					if (fileStream != null) fileStream.close();
				} catch (IOException e) {
					logError("Could not close stream after save or update to file::" + propertiesFile.getAbsolutePath(), e);
				}
			}
			// 2) Check if bundle was already in list of known bundles, add it
			List<String> knownBundles = I18nModule.getBundleNamesContainingI18nFiles();
			if (!knownBundles.contains(bundleName)) {
				knownBundles.add(bundleName);
				Collections.sort(knownBundles);
			}
			// 3) Replace in cache
			// not loaded yet or a non-resolved file (trans-tool)
			if (cachedBundles.containsValue(properties)) {
				// nothing to do with the property, a reused property
				// but remove from javascript translator cache.
				// re-initialization will happen lazy
				if (cachedJSTranslatorData.containsKey(key)) cachedJSTranslatorData.remove(key);
			} else {
				// Remove existing resolved property first from caches
				if (cachedBundles.containsKey(key)) cachedBundles.remove(key);
				if (cachedJSTranslatorData.containsKey(key)) cachedJSTranslatorData.remove(key);
				// Add new version to cache
				if (locale == null) {
					// Add metadata file to cache
					cachedBundles.put(key, properties);
				} else {
					// Getting the resolved property will add it to cache
					getResolvedProperties(locale, bundleName);
				}
			}
		}
	}

	/**
	 * Delete the given property file from disk
	 * 
	 * @param locale
	 * @param bundleName
	 */
	public void deleteProperties(Locale locale, String bundleName) {
		String key = calcPropertiesFileKey(locale, bundleName);
		if (isLogDebugEnabled()) logDebug("deleteProperties for key::" + key, null);
		synchronized (cachedBundles) {
			if (locale != null) { // metadata files are not in cache
				// 1) Remove from cache first
				if (cachedBundles.containsKey(key)) {
					cachedBundles.remove(key);
					// Remove also from javascript translator cache.
					// initialization will happen lazy
					if (cachedJSTranslatorData.containsKey(key)) cachedJSTranslatorData.remove(key);
				}
			}
			// 2) Remove from filesystem
			File baseDir = I18nModule.getPropertyFilesBaseDir(locale, bundleName);
			if (baseDir == null) {
				if (baseDir == null) { throw new AssertException("Can not delete properties file for bundle::" + bundleName + " and language::" + locale.toString()
						+ " - no base directory found, probably loaded from jar!"); }
			}
			File f = getPropertiesFile(locale, bundleName, baseDir);
			if (f.exists()) f.delete();
			// 3) Check if for this bundle any other language file exists, if
			// not remove
			// the bundle from the list of translatable bundles
			List<String> knownBundles = I18nModule.getBundleNamesContainingI18nFiles();
			Set<String> knownLangs = I18nModule.getAvailableLanguageKeys();
			boolean foundOther = false;
			for (String lang : knownLangs) {
				f = getPropertiesFile(getLocaleOrDefault(lang), bundleName, baseDir);
				if (f.exists()) {
					foundOther = true;
					break;
				}
			}
			if (!foundOther) {
				knownBundles.remove(bundleName);
			}
		}
	}

	/**
	 * Get the javascript translator data for this locale. The generated code is cached in a VM scope. The entry is removed from the cache whenever something on the
	 * property file changed <br>
	 * This method should only be called by the JSTranslatorMapper. If you need localized data in your javascript code use the following code snipplet: <code>;
	 * &lt;script type='text/javascript'&gt;
	 *   var translator = b_jsTranslatorFactory.getTranslator('de', 'org.olat.core');
	 *   alert(translator.translate('warn.beta.feature'));
	 * &lt;/script&gt;
	 * </code>
	 * 
	 * @param locale
	 * @param bundleName
	 * @return
	 */
	public String getJSTranslatorData(Locale locale, String bundleName) {
		String cacheKey = calcPropertiesFileKey(locale, bundleName);
		// First try to get from cache
		String jsTranslatorData = cachedJSTranslatorData.get(cacheKey);
		// Build the js data if it does not exist yet
		if (jsTranslatorData == null) {
			StringBuffer data = new StringBuffer();
			// we build an js object with key-value pairs
			data.append("var transData = {");
			Locale referenceLocale = I18nModule.getFallbackLocale();
			Properties properties = getPropertiesWithoutResolvingRecursively(referenceLocale, bundleName);
			Set<Object> keys = properties.keySet();
			boolean addComma = false;
			for (Object keyObject : keys) {
				String key = (String) keyObject;
				String value = getLocalizedString(bundleName, key, null, locale, I18nModule.isOverlayEnabled(), true);
				if (value == null) {
					// use bundlename:key as value in case the key can't be
					// translated
					value = buildI18nItemIdentifyer(bundleName, key);
				}
				// remove line breaks and escape double quotes
				value = StringHelper.stripLineBreaks(value);
				value = Formatter.escapeDoubleQuotes(value).toString();
				if (addComma) data.append(",");
				data.append("'").append(key).append("' : \"").append(value).append("\"");
				addComma = true;
			}
			// create a translator in the browser with this data
			data.append("}; b_jsTranslatorFactory._createTranslator(transData,'").append(locale.toString()).append("','").append(bundleName).append("');");
			jsTranslatorData = data.toString();
			// add to cache. don't synchronize, no problem if overwritten
			cachedJSTranslatorData.put(cacheKey, jsTranslatorData);
		}
		return jsTranslatorData;
	}

	/**
	 * Get the last modified date of this bundle and locale
	 * 
	 * @param locale
	 * @param bundleName
	 * @return
	 */
	public Long getLastModifiedDate(Locale locale, String bundleName) {
		File baseDir = I18nModule.getPropertyFilesBaseDir(locale, bundleName);
		if (baseDir != null) {
			File propertyFile = getPropertiesFile(locale, bundleName, baseDir);
			return (propertyFile.lastModified());
		} else {
			// must be loaded from a jar, use startup date of VM
			return WebappHelper.getTimeOfServerStartup();
		}
	}

	/**
	 * @param localeKey the locale in String form. For the moment we only accept locales with either a language ("de"), a language and a country ("de_CH"), or a language
	 *            and a country and a variant ("de_CH_bern").
	 *            <p>
	 *            Overlay locales are not not accepted
	 *            <p>
	 *            If localeKey is null, the default locale is used.
	 * @return the locale given the localeKey as returned by the Locale.toString() method, or the default locale if the language was not found
	 */
	public Locale getLocaleOrDefault(String localeKey) {
		if (localeKey == null || !I18nModule.getAvailableLanguageKeys().contains(localeKey)) { return I18nModule.getDefaultLocale(); }
		Locale loc = I18nModule.getAllLocales().get(localeKey);
		if (loc == null) loc = I18nModule.getDefaultLocale();
		return loc;
	}

	/**
	 * @param localeKey the locale in String form. For the moment we only accept locales with either a language ("de"), a language and a country ("de_CH"), or a language
	 *            and a country and a variant ("de_CH_bern").
	 *            <p>
	 *            In addition, all overlay locales are also accepted
	 *            <p>
	 *            If localeKey is null, null is returned
	 * @return the locale given the localeKey as returned by the Locale.toString() method, or if no language was found
	 */
	public Locale getLocaleOrNull(String localeKey) {
		if (localeKey == null || (!I18nModule.getAvailableLanguageKeys().contains(localeKey) && !I18nModule.getOverlayLanguageKeys().contains(localeKey))) { return null; }
		Locale loc = I18nModule.getAllLocales().get(localeKey);
		return loc;
	}

	/**
	 * Translate the given language key to the language itself. This is used in language selection boxes where each language is labeled in their language. This method
	 * uses the getLanguageInEnglish() method as a fallback in case the translation could not be found.
	 * 
	 * @param languageKey
	 * @return e.g. "Deutsch" for input de
	 */
	public String getLanguageTranslated(String languageKey, boolean overlayEnabled) {
		// Load it from package without fallback
		String translated = getLocalizedString(I18nModule.getCoreFallbackBundle(), "this.language.translated", null, I18nModule.getAllLocales().get(languageKey),
				overlayEnabled, false, false, false, 0);
		if (translated == null) {
			// Use the english version as callback
			translated = getLanguageInEnglish(languageKey, overlayEnabled);
		}
		return translated;
	}

	/**
	 * Get a map of the enabled language keys as keys with their translated name as values. The language list uses the overlay feature to modify the language name when
	 * the overlay is configured.
	 * 
	 * @return
	 */
	public Map<String, String> getEnabledLanguagesTranslated() {
		Map<String, String> translatedLangs = new HashMap<String, String>();
		Set<String> enabledLangs = I18nModule.getEnabledLanguageKeys();
		for (String langKey : enabledLangs) {
			translatedLangs.put(langKey, getLanguageTranslated(langKey, I18nModule.isOverlayEnabled()));
		}
		return translatedLangs;
	}

	/**
	 * Translate the given language key to english (for administrative interface). This method fallbacks to the language key and never returns an error message
	 * 
	 * @param languageKey
	 * @return e.g. "German" for input de
	 */
	public String getLanguageInEnglish(String languageKey, boolean overlayEnabled) {
		// Load it from package without fallback
		String inEnglish = getLocalizedString(I18nModule.getCoreFallbackBundle(), "this.language.in.english", null, I18nModule.getAllLocales().get(languageKey),
				overlayEnabled, false, false, false, 0);
		if (inEnglish == null) {
			// use key as fallback
			inEnglish = languageKey;
		}
		return inEnglish;
	}

	/**
	 * Get the authors of the language as they entered themselfes in the translation tool. This reads the key org.olat.core:this.language.translator.names
	 * 
	 * @param languageKey
	 * @return e.g. "Marion Weber, University of Zuerich"
	 */
	public String getLanguageAuthor(String languageKey) {
		// Load it from package without fallback
		String authors = getLocalizedString(I18nModule.getCoreFallbackBundle(), "this.language.translator.names", null, I18nModule.getAllLocales().get(languageKey),
				false, false, false, false, 0);
		if (authors == null) { return "-"; }
		return authors;
	}

	/**
	 * Create a string array that contains the css markup for country flags
	 * 
	 * @param languageKeys The source array of language keys
	 * @param additionalCssClass additional CSS classes that should be added or NULL. E.g. you could use 'b_with_small_icon_left'
	 * @return
	 */
	public String[] createLanguageFlagsCssClasses(String[] languageKeys, String additionalCssClass) {
		String[] flagsCssClasses = new String[languageKeys.length];
		for (int i = 0; i < languageKeys.length; i++) {
			String cssClasses = (additionalCssClass == null ? "" : additionalCssClass);
			String langKey = languageKeys[i];
			cssClasses += " b_flag_" + langKey;
			flagsCssClasses[i] = cssClasses;
		}
		return flagsCssClasses;

	}

	/**
	 * Attache some data to thread local variables needed by the i18nManager. Make sure you call remove18nInfoFromThread() when the thread is finished!
	 * 
	 * @param hreq The http servlet request
	 */
	public static void attachI18nInfoToThread(HttpServletRequest hreq) {
		UserSession usess = UserSession.getUserSession(hreq);
		if (threadLocalLocale == null) {
			I18nManager.getInstance().logError("can't attach i18n info to thread: threadLocalLocale is null", null);
		} else {
			if (threadLocalLocale.getThreadLocale() != null) {
				I18nManager.getInstance().logWarn("try to attach i18n info to thread, but threadLocalLocale is not null - a thread forgot to remove it!",
						new Exception("attachI18nInfoToThread"));
			}
			threadLocalLocale.setThredLocale(usess.getLocale());
		}
		if (threadLocalIsMarkLocalizedStringsEnabled == null) {
			I18nManager.getInstance().logError("can't attach i18n info to thread: threadLocalIsMarkLocalizedStringsEnabled is null", null);
		} else {
			if (threadLocalIsMarkLocalizedStringsEnabled.isMarkLocalizedStringsEnabled() != null) {
				I18nManager.getInstance().logWarn(
						"try to attach i18n info to thread, but threadLocalIsMarkLocalizedStringsEnabled is not null - a thread forgot to remove it!", null);
			}
			Boolean isMarkLocalizedStringsEnabled = (Boolean) usess.getEntry(USESS_KEY_I18N_MARK_LOCALIZED_STRINGS);
			if (isMarkLocalizedStringsEnabled != null) {
				threadLocalIsMarkLocalizedStringsEnabled.setMarkLocalizedStringsEnabled(isMarkLocalizedStringsEnabled);
			}
		}
	}

	public static void updateLocaleInfoToThread(UserSession usess) {
		if (threadLocalLocale == null) {
			I18nManager.getInstance().logError("can't attach i18n info to thread: threadLocalLocale is null", null);
		} else {
			threadLocalLocale.setThredLocale(usess.getLocale());
		}
	}

	/**
	 * Remove any thread local data that was set using the attachI18nInfoToThread() method
	 */
	public static void remove18nInfoFromThread() {
		if (threadLocalLocale != null) {
			threadLocalLocale.setThredLocale(null);
		}
		if (threadLocalIsMarkLocalizedStringsEnabled != null) {
			threadLocalIsMarkLocalizedStringsEnabled.setMarkLocalizedStringsEnabled(null);
		}
	}

	/**
	 * Get the locale used in the current thread or the default locale if no locale is set. Usually this is the users logged in Locale
	 * 
	 * @return the locale of this thread
	 */
	public Locale getCurrentThreadLocale() {
		if (threadLocalLocale != null) {
			Locale locale = threadLocalLocale.getThreadLocale();
			if (locale != null) return threadLocalLocale.getThreadLocale();
		}
		return I18nModule.getDefaultLocale();
	}

	/**
	 * Set the
	 * 
	 * @param usess
	 * @param isMarkLocalizedStringsEnabled
	 */
	public void setMarkLocalizedStringsEnabled(UserSession usess, boolean isMarkLocalizedStringsEnabled) {
		// save in user session for later requests
		Boolean markLocalizedStringsEnabled = Boolean.valueOf(isMarkLocalizedStringsEnabled);
		if (usess != null) { // allow null for junit testcases
			usess.putEntry(USESS_KEY_I18N_MARK_LOCALIZED_STRINGS, markLocalizedStringsEnabled);
		}
		// update current thread local variable
		if (threadLocalIsMarkLocalizedStringsEnabled != null) {
			if (isMarkLocalizedStringsEnabled) {
				threadLocalIsMarkLocalizedStringsEnabled.setMarkLocalizedStringsEnabled(markLocalizedStringsEnabled);
			} else {
				threadLocalIsMarkLocalizedStringsEnabled.setMarkLocalizedStringsEnabled(null);
			}
		}
	}

	/**
	 * Check if this thread should be rendered using markup arround the localized strings
	 * 
	 * @return
	 */
	public boolean isCurrentThreadMarkLocalizedStringsEnabled() {
		if (threadLocalIsMarkLocalizedStringsEnabled != null) {
			Boolean isMarkLocalizedStringsEnabled = threadLocalIsMarkLocalizedStringsEnabled.isMarkLocalizedStringsEnabled();
			if (isMarkLocalizedStringsEnabled != null) return isMarkLocalizedStringsEnabled.booleanValue();
		}
		return false;
	}

	/**
	 * Get the priority of the bundle within all bundles. Smaller means higher prio
	 * 
	 * @param bundleName
	 * @return
	 */
	int getBundlePriority(String bundleName) {
		Properties metadataProperties = getPropertiesWithoutResolvingRecursively(null, bundleName);
		String bundlePrioValue = metadataProperties.getProperty(METADATA_BUNDLE_PRIORITY_KEY);
		if (bundlePrioValue != null) {
			// 1) Bundle priority found, parse and return
			try {
				return (Integer.parseInt(bundlePrioValue.trim()));
			} catch (NumberFormatException e) {
				logWarn("Can not parse metadata priority for bundle::" + bundleName, e);
			}
		}
		// 2) Not found, try with parent bundle
		int dotPos = bundleName.lastIndexOf(".");
		if (dotPos != -1) {
			String parentBundleName = bundleName.substring(0, dotPos);
			return getBundlePriority(parentBundleName);
		}
		// 3) Still not found, use default
		return DEFAULT_BUNDLE_PRIORITY;
	}

	/**
	 * Get the priority of this item within the bundle. Smaller means higher prio
	 * 
	 * @param metadataProperties
	 * @param key
	 * @param bundleName
	 * @return
	 */
	int getKeyPriority(Properties metadataProperties, String key, String bundleName) {
		int keyPriority = DEFAULT_KEY_PRIORITY;
		String keyPriorityValue = metadataProperties.getProperty(key + METADATA_KEY_PRIORITY_POSTFIX);
		if (keyPriorityValue != null) {
			try {
				keyPriority = Integer.parseInt(keyPriorityValue.trim());
			} catch (NumberFormatException e) {
				logWarn("Can not parse metadata priority for key::" + bundleName + ":" + key, e);
			}
		}
		return keyPriority;
	}

	/**
	 * Set the bundle priority for a specific bundle. Does not update the priorities of the children bundles, but note that when reading priorities of children that do
	 * not have a priority set, the system will degregate to the next parent that does have a priority set. Thus, it is not necessary to set children bundles priorities
	 * unless you want to set a higher priority.
	 * <p>
	 * Use priorities as follows:
	 * <ul>
	 * <li>000 - 099 : ultimate priority, will appear on top of translators list</li>
	 * <li>100 - 399 : reserved for olatcore framework package - very high priority</li>
	 * <li>400 - 499 : Application bundles: high priority</li>
	 * <li>500 - 599 : Application bundles: normal priority</li>
	 * <li>600 - 699 : Application bundles: low priority</li>
	 * <li>700 - 899 : Extension and custom modules</li>
	 * <li>900 - 999 : Examples and demo code - usually no need to translate</li>
	 * </ul>
	 * If no priority is defined, the standard priority of 500 is used
	 * 
	 * @param bundleName
	 * @param priority
	 */
	public void setBundlePriority(String bundleName, int priority) {
		Properties metadataProperties = getPropertiesWithoutResolvingRecursively(null, bundleName);
		if (priority > 999) { throw new AssertException("Bundle priorities can not be higher than 999. The smaller the number, the higher the priority."); }
		NumberFormat formatter = new DecimalFormat("000");
		metadataProperties.setProperty(METADATA_BUNDLE_PRIORITY_KEY, formatter.format(priority));
		saveOrUpdateProperties(metadataProperties, null, bundleName);
	}

	/**
	 * Set the key priority within the bundle. The smaller the number, the higher is the priority. Higher priority items will appear on top of the list of to be
	 * translated keys
	 * <p>
	 * Priorities should be between 000 (hight prio, begin of list) and 999 (low prio, end of list)
	 * <p>
	 * If no priority is used, the standard priority of 500 is used
	 * 
	 * @param bundleName
	 * @param key
	 * @param priority
	 */
	public void setKeyPriority(String bundleName, String key, int priority) {
		Properties metadataProperties = getPropertiesWithoutResolvingRecursively(null, bundleName);
		if (priority > 999) { throw new AssertException("Bundle priorities can not be higher than 999. The smaller the number, the higher the priority."); }
		NumberFormat formatter = new DecimalFormat("00");
		metadataProperties.setProperty(key + METADATA_KEY_PRIORITY_POSTFIX, formatter.format(priority));
		saveOrUpdateProperties(metadataProperties, null, bundleName);
	}

	/**
	 * Check if the inline translation mode is possible for a certain key. By default this returns true, only specific keys that have been set to false will return false.
	 * 
	 * @param metadataProperties
	 * @param key
	 * @param bundleName
	 * @return
	 */
	boolean isInlineTranslationEnabledForKey(String bundleName, String key) {
		Properties metadataProperties = getPropertiesWithoutResolvingRecursively(null, bundleName);
		String propertyKey = key + METADATA_KEY_INLINEREANSLATION_POSTFIX;
		String keyInlineTranslationValue = metadataProperties.getProperty(propertyKey);
		boolean isEnabled = true; // default
		if (keyInlineTranslationValue != null) {
			keyInlineTranslationValue = keyInlineTranslationValue.toLowerCase();
			if (keyInlineTranslationValue.equals(METADATA_KEY_INLINEREANSLATION_VALUE_DISABLED) || keyInlineTranslationValue.equals("false")
					|| keyInlineTranslationValue.equals("no")) {
				isEnabled = false;
			}
		}
		return isEnabled;
	}

	/**
	 * Enable or disable the inline translation mode for a specific key. If disabled, the inline translation mode will not be available for this key.
	 * <p>
	 * Disable inline translation for keys that have issues with the inline translation system such as default values that are used in forms that have length checks.
	 * 
	 * @param bundleName
	 * @param key
	 * @param enable
	 */
	public void setInlineTranslationEnabledForKey(String bundleName, String key, boolean enable) {
		Properties metadataProperties = getPropertiesWithoutResolvingRecursively(null, bundleName);
		String propertyKey = key + METADATA_KEY_INLINEREANSLATION_POSTFIX;
		if (enable) {
			if (metadataProperties.contains(propertyKey)) {
				metadataProperties.remove(propertyKey);
			}
		} else {
			metadataProperties.setProperty(propertyKey, METADATA_KEY_INLINEREANSLATION_VALUE_DISABLED);
		}
		saveOrUpdateProperties(metadataProperties, null, bundleName);
	}

	/**
	 * Remove all bundles from caches to force reload from filesystem
	 */
	void clearCaches() {
		synchronized (cachedBundles) {
			cachedBundles.clear();
			cachedJSTranslatorData.clear();
			referencingBundlesIndex.clear();
		}
	}

	/**
	 * Method to enable / disable caching of loaded and resolved bundles
	 * 
	 * @param useCache
	 */
	public void setCachingEnabled(boolean useCache) {
		if (useCache) {
			cachedBundles = new HashMap<String, Properties>();
			cachedJSTranslatorData = new HashMap<String, String>();
			referencingBundlesIndex = new HashMap<String, Set<String>>();
		} else {
			cachedBundles = new AlwaysEmptyMap<String, Properties>();
			cachedJSTranslatorData = new AlwaysEmptyMap<String, String>();
			referencingBundlesIndex = new AlwaysEmptyMap<String, Set<String>>();
		}
		this.cachingEnabled = useCache;
	}

	/**
	 * @return true: manager uses cache; false: manager always reads from filesystem
	 */
	public boolean isCachingEnabled() {
		return this.cachingEnabled;
	}

	/**
	 * Helper method to create a locale from a given locale key ('de', 'de_CH', 'de_CH_ZH')
	 * 
	 * @param localeKey
	 * @return the locale or NULL if no locale could be generated from this string
	 */
	Locale createLocale(String localeKey) {
		Locale aloc = null;
		// de
		// de_CH
		// de_CH_zueri
		String[] parts = localeKey.split("_");
		switch (parts.length) {
			case 1:
				aloc = new Locale(parts[0]);
				break;
			case 2:
				aloc = new Locale(parts[0], parts[1]);
				break;
			case 3:
				String lastPart = parts[2];
				// Add all remaining parts to variant, variant can contain
				// underscores according to Locale spec
				for (int i = 3; i < parts.length; i++) {
					String part = parts[i];
					lastPart = lastPart + "_" + part;
				}
				aloc = new Locale(parts[0], parts[1], lastPart);
				break;
			default:
				return null;
		}
		// Test if the locale has been constructed correctly. E.g. when the
		// language part is not existing in the ISO chart, the locale can
		// convert to something else.
		// E.g. he_HE_HE will convert automatically to iw_HE_HE
		if (aloc.toString().equals(localeKey)) {
			return aloc;
		} else {
			return null;
		}
	}

	/**
	 * Create a local that represents the overlay locale for the given locale
	 * 
	 * @param locale The original locale
	 * @return The overlay locale
	 */
	Locale createOverlay(Locale locale) {
		String lang = locale.getLanguage();
		String country = (locale.getCountry() == null ? "" : locale.getCountry());
		String variant = createOverlayKeyForLanguage(locale.getVariant() == null ? "" : locale.getVariant());
		Locale overlay = new Locale(lang, country, variant);
		return overlay;
	}

	/**
	 * Add the overlay postfix to the given language key
	 * 
	 * @param langKey
	 * @return
	 */
	String createOverlayKeyForLanguage(String langKey) {
		return langKey + "__" + I18nModule.getOverlayName();
	}

	/**
	 * Helper method to build i18n filenames from a given locale. E.g. when locale=de_CH, the resulting i18n file name will be LocalStrings_de_CH.properties
	 * <p>
	 * This method will check if the locale is an overlay locale and remove unnecessary white space
	 * 
	 * @param locale
	 * @return
	 */
	public String buildI18nFilename(Locale locale) {
		String langKey = getLocaleKey(locale);
		return I18nModule.LOCAL_STRINGS_FILE_PREFIX + langKey + I18nModule.LOCAL_STRINGS_FILE_POSTFIX;
	}

	/**
	 * Calculate the locale key that identifies the given locale. Adds support for the overlay mechanism.
	 * 
	 * @param locale
	 * @return
	 */
	public String getLocaleKey(Locale locale) {
		String langKey = locale.getLanguage();
		String country = locale.getCountry();
		// Only add country when available - in case of an overlay country is
		// set to
		// an empty value
		if (StringHelper.containsNonWhitespace(country)) {
			langKey = langKey + "_" + country;
		}
		String variant = locale.getVariant();
		// Only add the _ separator if the variant contains something in
		// addition to
		// the overlay, otherways use the __ only
		if (StringHelper.containsNonWhitespace(variant)) {
			if (variant.startsWith("__" + I18nModule.getOverlayName())) {
				langKey += variant;
			} else {
				langKey = langKey + "_" + variant;
			}
		}
		return langKey;
	}

	/**
	 * Calculate the language key from the given overlay locale without the locale (the original language before adding the overlay postfix)
	 * 
	 * @param overlay
	 * @return The original language key or NULL if not found
	 */
	public String createOrigianlLocaleKeyForOverlay(Locale overlay) {
		Map<Locale, Locale> overlaysLooup = I18nModule.getOverlayLocales();
		Set<Map.Entry<Locale, Locale>> entries = overlaysLooup.entrySet();
		for (Map.Entry<Locale, Locale> entry : entries) {
			if (getLocaleKey(entry.getValue()).equals(getLocaleKey(overlay))) { return getLocaleKey(entry.getKey()); }
		}
		return null;
	}

	/**
	 * Helper method to build a unique identifyer for an i18n item from the given bundle name and key
	 * 
	 * @param bundleName
	 * @param key
	 * @return
	 */
	public String buildI18nItemIdentifyer(String bundleName, String key) {
		return bundleName + ":" + key;
	}

	/**
	 * Search in all packages on the source patch for packages that contain an _i18n directory that can be used to store olatcore localization files
	 * 
	 * @return set of bundles that contain olatcore i18n compatible localization files
	 */
	List<String> searchForBundleNamesContainingI18nFiles() {
		List<String> foundBundles;
		// 1) First search on normal source path of application
		String srcPath = null;
		File applicationDir = I18nModule.getTransToolApplicationLanguagesSrcDir();
		if (applicationDir != null) {
			srcPath = applicationDir.getAbsolutePath();
		} else {
			// Fall back to compiled classes
			srcPath = WebappHelper.getBuildOutputFolderRoot();
		}
		I18nDirectoriesVisitor srcVisitor = new I18nDirectoriesVisitor(srcPath);
		FileUtils.visitRecursively(new File(srcPath), srcVisitor);
		foundBundles = srcVisitor.getBundlesContainingI18nFiles();
		
		// 3) For jUnit tests, add also the I18n test dir
		if (Settings.isJUnitTest()) {
			Resource testres = new ClassPathResource("olat.local.properties");
			String jUnitSrcPath = null;
			try {
				jUnitSrcPath = testres.getFile().getAbsolutePath();
			} catch (IOException e) {
				throw new StartupException("Could not find classpath resource for: test-classes/olat.local.property ", e);
			}
			I18nDirectoriesVisitor juniSrcVisitor = new I18nDirectoriesVisitor(jUnitSrcPath);
			FileUtils.visitRecursively(new File(jUnitSrcPath), juniSrcVisitor);
			foundBundles.addAll(juniSrcVisitor.getBundlesContainingI18nFiles());
		}
		// Sort alphabetically
		Collections.sort(foundBundles);
		return foundBundles;
	}

	/**
	 * Search for available languages in the given directory. The translation files must start with 'LocalStrings_' and end with '.properties'. Everything in between is
	 * considered a language key.
	 * <p>
	 * If the directory contains jar files, those files are opened and searched for languages files as well. In this case, the algorythm only looks for translation files
	 * that are in the org/olat/core/_i18n package
	 * 
	 * @param i18nDir
	 * @return set of language keys the system will find translations for
	 */
	Set<String> searchForAvailableLanguages(File i18nDir) {
		Set<String> foundLanguages = new TreeSet<String>();
		i18nDir = new File(i18nDir.getAbsolutePath()+"/org/olat/_i18n");
		if (i18nDir.exists()) {
			// First check for locale files
			
			String[] langFiles = i18nDir.list(i18nFileFilter);
			for (String langFileName : langFiles) {
				String lang = langFileName.substring(I18nModule.LOCAL_STRINGS_FILE_PREFIX.length(), langFileName.lastIndexOf("."));
				foundLanguages.add(lang);
				if (isLogDebugEnabled()) logDebug("Adding lang::" + lang + " from filename::" + langFileName + " from dir::" + i18nDir.getAbsolutePath(), null);
			}
		}
		return foundLanguages;
	}

	/**
	 * Searches within a jar file for available languages.
	 * 
	 * @param jarFile
	 * @param checkForExecutables true: check if jar contains java or class files and return an empty set if such executable files are found; false don't check or care
	 * @return Set of language keys, can be empty but never null
	 */
	public Set<String> sarchForAvailableLanguagesInJarFile(File jarFile, boolean checkForExecutables) {
		Set<String> foundLanguages = new TreeSet<String>();
		JarFile jar;
		try {
			jar = new JarFile(jarFile);
			Enumeration<JarEntry> jarEntries = jar.entries();
			while (jarEntries.hasMoreElements()) {
				JarEntry jarEntry = jarEntries.nextElement();
				String jarEntryName = jarEntry.getName();
				// check for executables
				if (checkForExecutables && (jarEntryName.endsWith("java") || jarEntryName.endsWith("class"))) { return new TreeSet<String>(); }
				// search for core util in jar
				if (jarEntryName.indexOf(I18nModule.getCoreFallbackBundle().replace(".", "/") + "/" + I18N_DIRNAME) != -1) {
					// don't add overlayLocales as selectable
					// availableLanguages
					if (jarEntryName.indexOf("__") == -1 && jarEntryName.indexOf(I18nModule.LOCAL_STRINGS_FILE_PREFIX) != -1) {
						String lang = jarEntryName.substring(jarEntryName.indexOf(I18nModule.LOCAL_STRINGS_FILE_PREFIX) + I18nModule.LOCAL_STRINGS_FILE_PREFIX.length(),
								jarEntryName.lastIndexOf("."));
						foundLanguages.add(lang);
						if (isLogDebugEnabled()) logDebug("Adding lang::" + lang + " from filename::" + jarEntryName + " in jar::" + jar.getName(), null);
					}
				}
			}
		} catch (IOException e) {
			throw new OLATRuntimeException("Error when looking up i18n files in jar::" + jarFile.getAbsolutePath(), e);
		}
		return foundLanguages;
	}

	/**
	 * Copy the given set of languages from the given jar to the configured i18n source directories. This method can only be called in a translation server environment.
	 * 
	 * @param jarFile
	 * @param toCopyI18nKeys
	 */
	public void copyLanguagesFromJar(File jarFile, Set<String> toCopyI18nKeys) {
		if (!I18nModule.isTransToolEnabled()) { throw new AssertException(
				"Programming error - can only copy i18n files from a language pack to the source when in translation mode"); }
		JarFile jar;
		try {
			jar = new JarFile(jarFile);
			Enumeration<JarEntry> jarEntries = jar.entries();
			while (jarEntries.hasMoreElements()) {
				JarEntry jarEntry = jarEntries.nextElement();
				String jarEntryName = jarEntry.getName();
				// Check if this entry is a language file
				for (String i18nKey : toCopyI18nKeys) {
					if (jarEntryName.endsWith(I18N_DIRNAME + "/" + I18nModule.LOCAL_STRINGS_FILE_PREFIX + i18nKey + I18nModule.LOCAL_STRINGS_FILE_POSTFIX)) {
						File targetBaseDir;
						if (i18nKey.equals("de") || i18nKey.equals("en")) {
							targetBaseDir = I18nModule.getTransToolApplicationLanguagesSrcDir();
						} else {
							targetBaseDir = I18nModule.getTransToolApplicationOptLanguagesSrcDir();
						}
						// Copy file
						File targetFile = new File(targetBaseDir, jarEntryName);
						targetFile.getParentFile().mkdirs();
						FileUtils.save(jar.getInputStream(jarEntry), targetFile);
						// Check that saved properties file is empty, if so remove it
						Properties props = new Properties();
						props.load(new FileInputStream(targetFile));
						if (props.size() == 0) {
							targetFile.delete();
							// Delete empty parent dirs recursively
							File parent = targetFile.getParentFile();
							while (parent != null && parent.list() != null && parent.list().length == 0) {
								parent.delete();
								parent = parent.getParentFile();
							}
						}
						// Continue with next jar entry
						break;
					}
				}
			}
		} catch (IOException e) {
			throw new OLATRuntimeException("Error when copying up i18n files from a jar::" + jarFile.getAbsolutePath(), e);
		}
	}

	/**
	 * Get the property file for a given locale and bundle. If the locale is null, the metadata for this bundle are returned instead.
	 * 
	 * @param locale the locale or NULL to get the bundle metadata file
	 * @param bundleName
	 * @param sourceDir the source directory where to search for the properties file
	 * @return a file object. The file might not exist, but the mehod never return NULL!
	 */
	public File getPropertiesFile(Locale locale, String bundleName, File sourceDir) {
		if (bundleName == null) throw new AssertException("getPropertyFile(): bundleName can not be null");
		if (sourceDir == null) throw new AssertException("getPropertyFile(): sourceDir can not be null");
		// Create relative path to sourceDir
		bundleName = bundleName.replace('.', '/');
		String fileName = (locale == null ? METADATA_FILENAME : buildI18nFilename(locale));
		String relPath = "/" + bundleName + "/" + I18N_DIRNAME + "/" + fileName;
		// Load file from path
		File f = new File(sourceDir, relPath);
		if (f.exists() || I18nModule.isTransToolEnabled()) { return f; }
		return f;
	}

	public boolean createNewLanguage(String localeKey, String languageInEnglish, String languageTranslated, String authors) {
		if (!I18nModule.isTransToolEnabled()) { throw new AssertException(
				"Can not create a new language when the translation tool is not enabled and the transtool source pathes are not configured! Check your olat.properties files"); }
		if (I18nModule.getAvailableLanguageKeys().contains(localeKey)) { return false; }
		// Create new property file in the brasato bundle and re-initialize
		// everything
		String coreFallbackBundle = I18nModule.getCoreFallbackBundle();
		File transToolCoreLanguagesDir = I18nModule.getTransToolApplicationOptLanguagesSrcDir();
		String i18nDirRelPath = "/" + coreFallbackBundle.replace(".", "/") + "/" + I18nManager.I18N_DIRNAME;
		File transToolCoreLanguagesDir_I18n = new File(transToolCoreLanguagesDir, i18nDirRelPath);
		File newPropertiesFile = new File(transToolCoreLanguagesDir_I18n, I18nModule.LOCAL_STRINGS_FILE_PREFIX + localeKey + I18nModule.LOCAL_STRINGS_FILE_POSTFIX);
		// Prepare property file
		// Use a sorted properties object that saves the keys sorted alphabetically to disk
		Properties newProperties = new SortedProperties();
		if (StringHelper.containsNonWhitespace(languageInEnglish)) {
			newProperties.setProperty("this.language.in.english", languageInEnglish);
		}
		if (StringHelper.containsNonWhitespace(languageTranslated)) {
			newProperties.setProperty("this.language.translated", languageTranslated);
		}
		if (StringHelper.containsNonWhitespace(authors)) {
			newProperties.setProperty("this.language.translator.names", authors);
		}

		OutputStream fileStream = null;
		try {
			// Create necessary directories
			File directory = newPropertiesFile.getParentFile();
			if (!directory.exists()) directory.mkdirs();
			// Write to file file now
			fileStream = new FileOutputStream(newPropertiesFile);
			newProperties.store(fileStream, null);
			fileStream.flush();
			// Now set new language as enabled to allow user to translate the language.
			Set<String> enabledLangKeys = I18nModule.getEnabledLanguageKeys();
			enabledLangKeys.add(localeKey);
			// Reinitialize languages with new language
			I18nModule.reInitializeAndFlushCache();
			// Now add new language as new language (will re-initialize everything a second time)
			I18nModule.setEnabledLanguageKeys(enabledLangKeys);
			return true;

		} catch (FileNotFoundException e) {
			throw new OLATRuntimeException("Could not create new language file::" + newPropertiesFile.getAbsolutePath(), e);
		} catch (IOException e) {
			throw new OLATRuntimeException("Could not create new language file::" + newPropertiesFile.getAbsolutePath()
					+ ", maybe permission denied? Check your directory permissions", e);
		} finally {
			try {
				if (fileStream != null) fileStream.close();
			} catch (IOException e) {
				logError("Could not close stream after creating new language file::" + newPropertiesFile.getAbsolutePath(), e);
			}
		}
	}

	/**
	 * Method to delete an entire language.
	 * 
	 * @param deleteLangKey
	 * @param true: really delete the language; false: dry run with console logging
	 */
	public void deleteLanguage(String deleteLangKey, boolean reallyDeleteIt) {
		Locale deleteLoclae = I18nModule.getAllLocales().get(deleteLangKey);
		// copy bundles list to prevent concurrent modification exception
		List<String> bundlesCopy = new ArrayList<String>();
		bundlesCopy.addAll(I18nModule.getBundleNamesContainingI18nFiles());
		for (String bundleName : bundlesCopy) {
			if (reallyDeleteIt) {
				deleteProperties(deleteLoclae, bundleName);
				logDebug("Deleted bundle::" + bundleName + " and lang::" + deleteLangKey, null);
			} else {
				// just log
				logInfo("Dry-run-delete of bundle::" + bundleName + " and lang::" + deleteLangKey, null);
			}
		}
		// Now reinitialize everything
		if (reallyDeleteIt) {
			I18nModule.reInitializeAndFlushCache();
		}
	}

	/**
	 * Create a jar file that contains the translations for the given languages.
	 * <p>
	 * Note that this file is created in a temporary place in olatdata/tmp. It is in the responsibility of the caller of this method to remove the file when not needed
	 * anymore.
	 * 
	 * @param languageKeys
	 * @param fileName the name of the file.
	 * @return The file handle to the created file or NULL if no such file could be created (e.g. there already exists a file with this file name)
	 */
	public File createLanguageJarFile(Set<String> languageKeys, String fileName) {
		// Create file olatdata temporary directory
		File file = new File(WebappHelper.getUserDataRoot() + "/tmp/" + fileName);
		file.getParentFile().mkdirs();

		FileOutputStream stream = null;
		JarOutputStream out = null;
		try {
			// Open stream for jar file
			stream = new FileOutputStream(file);
			out = new JarOutputStream(stream, new Manifest());
			// Use now as last modified date of resources
			long now = System.currentTimeMillis();
			// Add all languages
			for (String langKey : languageKeys) {
				Locale locale = getLocaleOrNull(langKey);
				// Add all bundles in the current language
				for (String bundleName : I18nModule.getBundleNamesContainingI18nFiles()) {
					Properties propertyFile = getPropertiesWithoutResolvingRecursively(locale, bundleName);
					String entryFileName = bundleName.replace(".", "/") + "/" + I18N_DIRNAME + "/" + buildI18nFilename(locale);
					// Create jar entry for this path, name and last modified
					JarEntry jarEntry = new JarEntry(entryFileName);
					jarEntry.setTime(now);
					// Write properties to jar file
					out.putNextEntry(jarEntry);
					propertyFile.store(out, null);
					if (isLogDebugEnabled()) {
						logDebug("Adding file::" + entryFileName + " + to jar", null);
					}
				}
			}
			logDebug("Finished writing jar file::" + file.getAbsolutePath(), null);
		} catch (Exception e) {
			logError("Could not write jar file", e);
			return null;
		} finally {
			try {
				out.close();
				stream.close();
			} catch (IOException e) {
				logError("Could not close stream of jar file", e);
				return null;
			}
		}
		return file;
	}

	/*************************
	 * Private helper methods
	 *************************/

	/**
	 * [used by spring]
	 */
	private I18nManager() {
		INSTANCE = this;
	}

	/**
	 * Helper method to create a key that uniquely identifies a property file within the whole system using the bundle name and the locale. If the locale is null, the
	 * metadata for this bundle are returned
	 * 
	 * @param locale The locale or NULL to create the bundle metadata key
	 * @param bundleName
	 * @return a unique key as String
	 */
	private String calcPropertiesFileKey(Locale locale, String bundleName) {
		if (locale == null) {
			return bundleName + ":" + METADATA_KEY;
		} else {
			return bundleName + ":" + getLocaleKey(locale);
		}
	}

	/**
	 * Description:<br>
	 * A per-thread Locale that is used to translate messages that don't explicitly provide a locale
	 * <P>
	 * Initial Date: 19.09.2008 <br>
	 * 
	 * @author gnaegi
	 */
	private static class ThreadLocalLocale extends ThreadLocal<Locale> {
		/**
		 * @see java.lang.ThreadLocal#initialValue()
		 */
		public Locale initialValue() {
			return null;
		}

		/**
		 * @param threadLocale The thread locale
		 */
		public void setThredLocale(Locale threadLocale) {
			if (threadLocale == null) {
				super.remove();
			} else {
				super.set(threadLocale);
			}
		}

		/**
		 * @return the thread locale
		 */
		public Locale getThreadLocale() {
			return super.get();
		}

	}

	/**
	 * Description:<br>
	 * A per-thread Boolean to enable or disable markup around localized strings
	 * <P>
	 * Initial Date: 19.09.2008 <br>
	 * 
	 * @author gnaegi
	 */
	private static class ThreadLocalMarkLocalizedStrings extends ThreadLocal<Boolean> {
		/**
		 * @see java.lang.ThreadLocal#initialValue()
		 */
		public Boolean initialValue() {
			return null;
		}

		/**
		 * @param markLocalizedStrings true: add markup to localized strings; false: do normal translate strings
		 */
		public void setMarkLocalizedStringsEnabled(Boolean markLocalizedStrings) {
			if (markLocalizedStrings == null) {
				super.remove();
			} else {
				super.set(markLocalizedStrings);
			}
		}

		/**
		 * @return true: add markup to localized strings; false: do normal translate strings
		 */
		public Boolean isMarkLocalizedStringsEnabled() {
			return super.get();
		}

	}

}
