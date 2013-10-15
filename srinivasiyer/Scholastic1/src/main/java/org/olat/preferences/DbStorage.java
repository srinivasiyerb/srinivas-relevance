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
 * Copyright (c) 2005-2006 by JGS goodsolutions GmbH, Switzerland<br>
 * http://www.goodsolutions.ch All rights reserved.
 * <p>
 */
package org.olat.preferences;

import org.olat.core.id.Identity;
import org.olat.core.util.prefs.Preferences;
import org.olat.core.util.prefs.PreferencesStorage;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;

/**
 * Description:<br>
 * <P>
 * Initial Date: 21.06.2006 <br>
 * 
 * @author Felix Jost
 */
public class DbStorage implements PreferencesStorage {

	static final String USER_PROPERTY_KEY = "v2guipreferences";

	@Override
	public Preferences getPreferencesFor(final Identity identity, final boolean useTransientPreferences) {
		if (useTransientPreferences) {
			return createEmptyDbPrefs(identity, true);
		} else {
			return getPreferencesFor(identity);
		}
	}

	/**
	 * search x-stream serialization in properties table, create new if not found
	 * 
	 * @param identity
	 * @return
	 */
	private DbPrefs getPreferencesFor(final Identity identity) {
		final Property guiProperty = PropertyManager.getInstance().findProperty(identity, null, null, null, USER_PROPERTY_KEY);
		if (guiProperty == null) {
			return createEmptyDbPrefs(identity, false);
		} else {
			return getPreferencesForProperty(identity, guiProperty);
		}
	}

	private DbPrefs getPreferencesForProperty(final Identity identity, final Property guiProperty) {
		DbPrefs prefs;
		try {
			prefs = createDbPrefsFrom(identity, guiProperty, guiProperty.getTextValue());
		} catch (final Exception e) {
			prefs = doGuiPrefsMigration(guiProperty, identity);
		}
		return prefs;
	}

	private DbPrefs createEmptyDbPrefs(final Identity identity, final boolean isTransient) {
		final DbPrefs prefs = new DbPrefs();
		prefs.setIdentity(identity);
		prefs.isTransient = isTransient;
		return prefs;
	}

	private DbPrefs createDbPrefsFrom(final Identity identity, final Property guiProperty, final String textValue) {
		final DbPrefs prefs = (DbPrefs) XStreamHelper.fromXML(textValue);
		prefs.setIdentity(identity); // reset transient value
		prefs.dbProperty = guiProperty; // set property for later use
		return prefs;
	}

	private DbPrefs doGuiPrefsMigration(final Property guiProperty, final Identity identity) {
		final String migratedTextValue = doCalendarRefactoringMigration(guiProperty.getTextValue());
		// add new migration methode here
		try {
			return createDbPrefsFrom(identity, guiProperty, migratedTextValue);
		} catch (final Exception e) {
			// Migration failed => return empty db-prefs
			return createEmptyDbPrefs(identity, false);
		}
	}

	/**
	 * Migration for 5.1.x to 5.2.0 because the calendar package was changed. Rename 'org.olat.core.commons.calendar.model.KalendarConfig' to
	 * 'org.olat.commons.calendar.model.KalendarConfig'.
	 * 
	 * @param textValue
	 * @return Migrated textValue String
	 */
	private String doCalendarRefactoringMigration(final String textValue) {
		return textValue.replaceAll("org.olat.core.commons.calendar.model.KalendarConfig", "org.olat.commons.calendar.model.KalendarConfig");
	}

}
