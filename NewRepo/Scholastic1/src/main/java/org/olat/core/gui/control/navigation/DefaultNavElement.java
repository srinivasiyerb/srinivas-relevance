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
 * TODO: Felix Jost Class Description for DefaultNavElement
 * <P>
 * Initial Date: 19.07.2005 <br>
 * 
 * @author Felix Jost
 */
public class DefaultNavElement implements NavElement {
	private String title, description, iconCSSClass;
	private Character accessKey;

	/**
	 * @param title
	 * @param description
	 * @param iconCSSClass
	 */
	public DefaultNavElement(String title, String description, String iconCSSClass) {
		this.title = title;
		this.description = description;
		this.iconCSSClass = iconCSSClass;
	}

	/**
	 * clones the original Navigation Element
	 * 
	 * @param orig
	 */
	public DefaultNavElement(NavElement orig) {
		this.title = orig.getTitle();
		this.description = orig.getDescription();
		this.iconCSSClass = orig.getIconCSSClass();
		this.accessKey = orig.getAccessKey();
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getIconCSSClass() {
		return iconCSSClass;
	}

	public void setIconCSSClass(String iconCSSClass) {
		this.iconCSSClass = iconCSSClass;
	}

	@Override
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public void setAccessKey(Character accessKey) {
		this.accessKey = accessKey;
	}

	@Override
	public Character getAccessKey() {
		return accessKey;
	}
}
