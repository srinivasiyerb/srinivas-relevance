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

package org.olat.commons.info.portlet;

import org.olat.core.gui.control.generic.portal.PortletEntry;

/**
 * Description:<br>
 * <P>
 * Initial Date: 27 jul. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoPortletEntry implements PortletEntry<InfoSubscriptionItem> {
	private final Long key;
	private final InfoSubscriptionItem value;

	public InfoPortletEntry(final Long key, final InfoSubscriptionItem info) {
		this.key = key;
		value = info;
	}

	@Override
	public Long getKey() {
		return key;
	}

	@Override
	public InfoSubscriptionItem getValue() {
		return value;
	}
}
