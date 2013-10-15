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
 * Copyright (c) 2008 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.modules.scorm.assessment;

import java.util.Comparator;

/**
 * The comparator orders the CmiDatas with the following rule: ItemId of the sco first, then the key of the data.
 * <P>
 * Initial Date: 13 august 2009 <br>
 * 
 * @author srosse
 */
public class CmiDataComparator implements Comparator<CmiData> {

	@Override
	public int compare(final CmiData c1, final CmiData c2) {
		if (c1 == null && c2 != null) {
			return -1;
		} else if (c1 != null && c2 == null) { return 1; }

		final String i1 = c1.getItemId();
		final String i2 = c2.getItemId();
		if (i1 == null && i2 != null) {
			return -1;
		} else if (i1 != null && i2 == null) { return 1; }

		int c = i1.compareTo(i2);
		if (c == 0) {
			final String k1 = c1.getKey();
			final String k2 = c2.getKey();
			if (k1 == null && k2 != null) {
				return -1;
			} else if (k1 != null && k2 == null) { return 1; }

			c = k1.compareTo(k2);
		}

		return c;
	}
}
