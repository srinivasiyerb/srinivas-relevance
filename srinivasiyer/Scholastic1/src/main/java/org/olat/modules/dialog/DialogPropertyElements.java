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

package org.olat.modules.dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Description:<br>
 * TODO: guido Class Description for DialogPropertyElements
 * <P>
 * Initial Date: 15.11.2005 <br>
 * 
 * @author guido
 */
public class DialogPropertyElements {
	private List<DialogElement> dialogPropertyElements = new ArrayList<DialogElement>();
	private transient String propertyCategory;
	private final int version = 1;

	// there is a feature in the admin console you can search properties by propertyName

	protected DialogPropertyElements(final String propertyCategory) {
		this.propertyCategory = propertyCategory;
	}

	/**
	 * @return Returns the dialogPropertyElements.
	 */
	public List<DialogElement> getDialogPropertyElements() {
		return dialogPropertyElements;
	}

	/**
	 * @param dialogPropertyElements The dialogPropertyElements to set.
	 */
	protected void setDialogPropertyElements(final List<DialogElement> dialogPropertyElements) {
		this.dialogPropertyElements = dialogPropertyElements;
	}

	protected void addElement(final DialogElement element) {
		dialogPropertyElements.add(element);
	}

	/**
	 * @return Returns the propertyCategory.
	 */
	protected String getPropertyCategory() {
		return propertyCategory;
	}

}
