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
public class TitleElement extends AbstractFormElement {
	private String titleKey;

	/**
	 * @param titleKey
	 */
	public TitleElement(String titleKey) {
		this.titleKey = titleKey;
		super.setReadOnly(true);
	}

	/**
	 * the titleElement ignores setValues
	 * 
	 * @param values
	 */
	@Override
	public void setValues(String[] values) {
		//
	}

	/**
	 * Returns the titleKey.
	 * 
	 * @return String
	 */
	public String getTitleKey() {
		return titleKey;
	}

	/**
	 * Sets the titleKey.
	 * 
	 * @param titleKey The titleKey to set
	 */
	/**
	 * @param titleKey
	 */
	public void setTitleKey(String titleKey) {
		this.titleKey = titleKey;
	}

	/**
	 * @see org.olat.core.gui.formelements.FormElement#setReadOnly(boolean)
	 */
	@Override
	public void setReadOnly(boolean readOnly) {
		// ignore, since we are always readonly
	}

	/**
	 * @see org.olat.core.gui.formelements.FormElement#isDirty()
	 */
	@Override
	public boolean isDirty() {
		return false;
	}

}