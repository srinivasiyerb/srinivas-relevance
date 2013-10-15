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

package org.olat.core.util.vfs.filters;

import java.util.Enumeration;
import java.util.Hashtable;

import org.olat.core.util.vfs.VFSItem;

/**
 * <h3>Description:</h3> The VFSItemExcludePrefixFilter filters VFSItems that start with the given prefixes.
 * <p>
 * Note that this is not restricted to VFSLeaves, it does also filter VFSContainers!
 */
public class VFSItemExcludePrefixFilter implements VFSItemFilter {

	private Hashtable excludedPrefixes = new Hashtable();

	/**
	 * Constrtuctor
	 * 
	 * @param prefixes
	 */
	public VFSItemExcludePrefixFilter(String[] prefixes) {
		for (int i = 0; i < prefixes.length; i++) {
			addExcludedPrefix(prefixes[i]);
		}
	}

	/**
	 * @param prefix
	 */
	public void addExcludedPrefix(String prefix) {
		prefix = prefix.toLowerCase();
		this.excludedPrefixes.put(prefix, prefix);
	}

	/**
	 * @param prefix
	 */
	public void removeSuffix(String prefix) {
		this.excludedPrefixes.remove(prefix.toLowerCase());
	}

	/**
	 * @see org.olat.core.util.vfs.filters.VFSItemFilter#accept(org.olat.core.util.vfs.VFSItem)
	 */
	@Override
	public boolean accept(VFSItem vfsItem) {
		String name = vfsItem.getName().toLowerCase();
		Enumeration elements = excludedPrefixes.elements();
		while (elements.hasMoreElements()) {
			String excludedPrefix = (String) elements.nextElement();
			if (name.startsWith(excludedPrefix)) { return false; }
		}
		return true;
	}

}
