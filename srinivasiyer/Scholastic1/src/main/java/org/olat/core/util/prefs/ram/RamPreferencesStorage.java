/**
 * This software is based on OLAT, www.olat.org
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
 * Copyright (c) JLS goodsolutions GmbH, Zurich, Switzerland. http://www.goodsolutions.ch <br>
 * All rights reserved.
 * <p>
 */
package org.olat.core.util.prefs.ram;

import java.util.HashMap;
import java.util.Map;

import org.olat.core.id.Identity;
import org.olat.core.util.prefs.Preferences;
import org.olat.core.util.prefs.PreferencesStorage;

/**
 * Description:<br>
 * <P>
 * Initial Date: 04.01.2007 <br>
 * 
 * @author Felix Jost
 */
public class RamPreferencesStorage implements PreferencesStorage {
	private Map identToPrefs;

	public RamPreferencesStorage() {
		identToPrefs = new HashMap();
	}

	/**
	 * @see org.olat.core.util.prefs.PreferencesStorage#getPreferencesFor(org.olat.core.id.Identity, boolean)
	 */
	@Override
	public Preferences getPreferencesFor(Identity identity, boolean useTransientPreferences) {
		Preferences p;
		synchronized (this) { // o_clusterOK by:fj is not persistent, for session only
			p = (Preferences) identToPrefs.get(identity.getName());
			if (p == null) {
				p = new RamPreferences();
				identToPrefs.put(identity.getName(), p);
			}
		}
		return p;
	}

}