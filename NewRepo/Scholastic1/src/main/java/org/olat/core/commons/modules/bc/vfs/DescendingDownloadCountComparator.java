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
package org.olat.core.commons.modules.bc.vfs;

import java.util.Comparator;

/**
 * <p>
 * Ordering OlatRootFileImpls by their download count descendingly
 * <p>
 * Initial Date: Sep 16, 2009 <br>
 * 
 * @author gwassmann, gwassmann@frentix.com, www.frentix.com
 */
public class DescendingDownloadCountComparator implements Comparator<OlatRootFileImpl> {
	/**
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(OlatRootFileImpl o1, OlatRootFileImpl o2) {
		int d1 = o1.getMetaInfo().getDownloadCount();
		int d2 = o2.getMetaInfo().getDownloadCount();
		return d1 > d2 ? -1 : 1;
	}
}
