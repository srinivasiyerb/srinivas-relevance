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
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.ims.qti.container;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Felix Jost
 */
public class ItemsInput implements Serializable {

	private final HashMap map;

	/**
	 * Constructor for ItemsInput.
	 */
	public ItemsInput() {
		super();
		map = new HashMap();
	}

	public void addItemInput(final ItemInput iip) {
		map.put(iip.getIdent(), iip);
	}

	public Iterator getItemInputIterator() {
		return map.values().iterator();
	}

	public ItemInput getItemInput(final String itemIdent) {
		return (ItemInput) map.get(itemIdent);
	}

	@Override
	public String toString() {
		return map.values().toString();
	}

	/**
	 * Method getItemCount.
	 * 
	 * @return int
	 */
	public int getItemCount() {
		return map.size();
	}
}
