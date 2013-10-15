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
package org.olat.modules.webFeed.models;

import java.util.Comparator;
import java.util.Date;

/**
 * Compares the publish date of two items.
 * <P>
 * Initial Date: Aug 4, 2009 <br>
 * 
 * @author gwassmann
 */
public class ItemPublishDateComparator implements Comparator<Item> {

	/**
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(final Item a, final Item b) {
		// reverse chronological order
		final Date d1 = a.getPublishDate();
		final Date d2 = b.getPublishDate();
		if (d1 == null) { return 1; }
		if (d2 == null) { return -1; }

		return d2.compareTo(d1);
	}
}
