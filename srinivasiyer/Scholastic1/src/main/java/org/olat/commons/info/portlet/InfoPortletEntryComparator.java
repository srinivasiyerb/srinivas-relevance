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

import java.util.Comparator;
import java.util.Date;

import org.olat.core.gui.control.generic.portal.SortingCriteria;
import org.olat.core.util.notifications.items.SubscriptionListItem;

/**
 * Description:<br>
 * Comparator for InfoPortletEntry
 * <P>
 * Initial Date: 27 jul. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoPortletEntryComparator implements Comparator<InfoPortletEntry> {

	private final SortingCriteria criteria;

	public InfoPortletEntryComparator(final SortingCriteria criteria) {
		this.criteria = criteria;
	}

	@Override
	public int compare(final InfoPortletEntry e1, final InfoPortletEntry e2) {
		if (e1 == null) {
			return -1;
		} else if (e2 == null) { return 1; }

		final InfoSubscriptionItem isi1 = e1.getValue();
		final InfoSubscriptionItem isi2 = e2.getValue();
		if (isi1 == null) {
			return -1;
		} else if (isi2 == null) { return 1; }

		final SubscriptionListItem m1 = isi1.getItem();
		final SubscriptionListItem m2 = isi2.getItem();
		if (m1 == null) {
			return -1;
		} else if (m2 == null) { return 1; }

		// only sorting per date
		final Date d1 = m1.getDate();
		final Date d2 = m2.getDate();
		if (d1 == null) {
			return -1;
		} else if (d2 == null) { return 1; }

		final int result = d1.compareTo(d2);
		return criteria.isAscending() ? result : -result;
	}
}