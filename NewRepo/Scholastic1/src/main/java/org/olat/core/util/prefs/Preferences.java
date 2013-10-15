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

package org.olat.core.util.prefs;

/**
 * Description:<br>
 * TODO: Felix Class Description for Preferences
 * <P>
 * Initial Date: 05.08.2005 <br>
 * 
 * @author Felix
 */
public interface Preferences {
	/**
	 * @param attributedClass
	 * @param key
	 * @return the object
	 */
	public Object get(Class attributedClass, String key);

	/**
	 * @param attributedClass
	 * @param key
	 * @param defaultValue the value returned if no such config exists
	 * @return the object
	 */
	public Object get(Class attributedClass, String key, Object defaultValue);

	/**
	 * put a value in the object. no persisting takes places (see save())
	 * 
	 * @param attributedClass
	 * @param key
	 * @param value the object to save, which class must have a public default contructor (for xstreams to save/load)
	 */
	public void put(Class attributedClass, String key, Object value);

	/**
	 * convenience method: as put, followed by a save()
	 * 
	 * @param attributedClass
	 * @param key
	 * @param value
	 */
	public void putAndSave(Class attributedClass, String key, Object value);

	// FIXME fj: remove method is missing

	/**
	 * to commit several put's
	 */
	public void save();

	/**
	 * checks if the whole key or the end of the key is in the preferences collection use only if you do not have access to the attributed class
	 * 
	 * @param string
	 * @return the pref object or null if nothing found
	 */
	public Object findPrefByKey(String string);
}
