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

package org.olat.core.configuration;

/**
 * Used for enable/disable Spring beans. Default value is 'enabled'.
 * 
 * @author Christian Guretzki
 */
public abstract class AbstractConfigOnOff implements ConfigOnOff {

	private boolean configEnabled = true;

	/**
	 * [used by spring]
	 * 
	 * @param configEnabled
	 */
	public void setEnabled(boolean configEnabled) {
		this.configEnabled = configEnabled;
	}

	/**
	 * @return true it the extension is enabled otherwise false
	 */
	@Override
	public boolean isEnabled() {
		return configEnabled;
	}

}
