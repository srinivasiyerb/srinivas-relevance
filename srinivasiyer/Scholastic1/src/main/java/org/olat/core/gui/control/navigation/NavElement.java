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

package org.olat.core.gui.control.navigation;

/**
 * Description:<br>
 * TODO: Felix Jost Class Description for TopNavElem
 * <P>
 * Initial Date: 12.07.2005 <br>
 * 
 * @author Felix Jost
 */
public interface NavElement {

	/**
	 * [used by velocity]
	 * 
	 * @return
	 */
	public String getTitle();

	/**
	 * [used by velocity]
	 * 
	 * @return
	 */
	public String getDescription();

	/**
	 * [used by velocity]
	 * 
	 * @return
	 */
	public String getIconCSSClass();

	/**
	 * @return The access key as string or NULL if no access key is used.
	 */
	public Character getAccessKey();

	/**
	 * Define an access key if this site is an important site for the user. Be very carefull and see first which access keys are already used by the application.
	 * <ul>
	 * <li>Don't use 0-1, those numbers are reserved for the dynamic sites</li>
	 * <li>Don't use n, o, m, c, t, d, b; those are used for the general navigation</li>
	 * <li>Check what the other sites already use</li>
	 * 
	 * @param accessKey One character or NULL
	 */
	public void setAccessKey(Character accessKey);

}
