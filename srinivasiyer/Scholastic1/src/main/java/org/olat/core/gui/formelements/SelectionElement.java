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

package org.olat.core.gui.formelements;

/**
 * @author Felix Jost
 */
public interface SelectionElement extends FormElement {

	/**
	 * @param which
	 * @return String
	 */
	public String getKey(int which);

	/**
	 * @param which
	 * @return String
	 */
	public String getValue(int which);

	/**
	 * @return integer
	 */
	public int getSize();

	/**
	 * @param which
	 * @return boolean
	 */
	public boolean isSelected(int which);

	/**
	 * @param key
	 * @param select
	 */
	public void select(String key, boolean select);

}