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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.restapi;

import org.olat.core.configuration.PersistedProperties;
import org.olat.core.configuration.PersistedPropertiesChangedEvent;
import org.olat.core.gui.control.Event;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;

/**
 * Description:<br>
 * Configuration of the REST API
 * <P>
 * Initial Date: 18 juin 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
public class RestModule implements GenericEventListener {

	private static final String ENABLED = "enabled";

	private Boolean enabled;
	private Boolean defaultEnabled;
	private PersistedProperties persistedProperties;

	public RestModule() {
		//
	}

	public void setCoordinator(final CoordinatorManager coordinatorManager) {
		// nothing to do
	}

	/**
	 * [used by spring]
	 * 
	 * @param persistedProperties
	 */
	public void setPersistedProperties(final PersistedProperties persistedProperties) {
		this.persistedProperties = persistedProperties;
	}

	public Boolean getDefaultEnabled() {
		return defaultEnabled;
	}

	public void setDefaultEnabled(final Boolean defaultEnabled) {
		this.defaultEnabled = defaultEnabled;
	}

	public boolean isEnabled() {
		if (enabled == null) {
			final String enabledStr = persistedProperties.getStringPropertyValue(ENABLED, true);
			enabled = StringHelper.containsNonWhitespace(enabledStr) ? "enabled".equals(enabledStr) : defaultEnabled.booleanValue();
		}
		return enabled.booleanValue();
	}

	public void setEnabled(final boolean enabled) {
		if (getPersistedProperties() != null) {
			final String enabledStr = enabled ? "enabled" : "disabled";
			getPersistedProperties().setStringProperty(ENABLED, enabledStr, true);
		}
	}

	/**
	 * @return the persisted properties
	 */
	private PersistedProperties getPersistedProperties() {
		return persistedProperties;
	}

	@Override
	public void event(final Event event) {
		if (event instanceof PersistedPropertiesChangedEvent) {
			// Reload the properties
			if (!((PersistedPropertiesChangedEvent) event).isEventOnThisNode()) {
				persistedProperties.loadPropertiesFromFile();
			}
			enabled = null;
		}
	}

}
