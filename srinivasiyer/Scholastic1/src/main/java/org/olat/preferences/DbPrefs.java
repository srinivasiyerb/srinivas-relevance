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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.olat.core.id.Identity;
import org.olat.core.util.prefs.Preferences;
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
public class DbPrefs implements Preferences {

	// keys: prefs-keys; values: any Prefs-Objects
	private final Map prefstore = new HashMap();

	// simply to indicate preferences version (serialized in the xstream, do not remove!)
	private final int version = 1;

	private transient Identity owner;

	// true: don't save to disk, only in ram
	transient boolean isTransient = false;
	transient Property dbProperty = null;

	public DbPrefs() {
		// must have a default constructor for serialization!
	}

	@Override
	public void save() {
		if (!isTransient) {
			final PropertyManager pm = PropertyManager.getInstance();
			// generate x-stream serialization of this object
			final String props = XStreamHelper.toXML(this);
			if (this.dbProperty == null) {
				// save as new property
				this.dbProperty = pm.createPropertyInstance(owner, null, null, null, DbStorage.USER_PROPERTY_KEY, null, null, null, props);
				pm.saveProperty(this.dbProperty);
			} else {
				// update exising property
				this.dbProperty.setTextValue(props);
				pm.updateProperty(this.dbProperty);
			}
		}
	}

	/**
	 * @param attributedClass
	 * @param key
	 * @return Object
	 */
	@Override
	public Object get(final Class attributedClass, final String key) {
		return prefstore.get(attributedClass.getName() + "::" + key);
	}

	/**
	 * @see org.olat.core.util.prefs.Preferences#get(java.lang.Class, java.lang.String, java.lang.Object)
	 */
	@Override
	public Object get(final Class attributedClass, final String key, final Object defaultValue) {
		final Object value = get(attributedClass, key);
		if (value == null) { return defaultValue; }
		return value;
	}

	/**
	 * @param attributedClass
	 * @param key
	 * @param value TODO: make value not object, but basetypemap or such?
	 */
	@Override
	public void put(final Class attributedClass, final String key, final Object value) {
		prefstore.put(attributedClass.getName() + "::" + key, value);
	}

	/**
	 * @param identity
	 */
	void setIdentity(final Identity identity) {
		this.owner = identity;
	}

	/**
	 * @see org.olat.core.util.prefs.Preferences#putAndSave(java.lang.Class, java.lang.String, java.lang.Object)
	 */
	@Override
	public void putAndSave(final Class attributedClass, final String key, final Object value) {
		put(attributedClass, key, value);
		save();
	}

	/**
	 * @see org.olat.core.util.prefs.Preferences#findPrefByKey(java.lang.String)
	 */
	@Override
	public Object findPrefByKey(final String partOfKey) {
		for (final Iterator iterator = prefstore.keySet().iterator(); iterator.hasNext();) {
			final String key = (String) iterator.next();
			if (key.endsWith(partOfKey)) { return prefstore.get(key); }
		}
		return null;
	}

}
