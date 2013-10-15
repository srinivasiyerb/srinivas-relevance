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
package org.olat.core.gui.components.form.flexible.elements;

import java.util.Set;

/**
 * Description:<br>
 * TODO: patrickb Class Description for MultipleSelectionElement
 * <P>
 * Initial Date: 04.01.2007 <br>
 * 
 * @author felix
 * @author patrickb
 */

public interface MultipleSelectionElement extends SelectionElement {

	/**
	 * @return a Set of Strings
	 */
	public Set<String> getSelectedKeys();

	/**
	 * @param keys
	 * @param values
	 * @param cssClasses The CSS classes that should be used in the form element for each key-value pair or NULL not not use special styling
	 */
	public void setKeysAndValues(String[] keys, String values[], String[] cssClasses);

	/**
	 * @param howmany
	 * @return
	 */
	public boolean isAtLeastSelected(int howmany);

	/**
	 * Select all selection elements.
	 */
	public void selectAll();

	/**
	 * Uncheck all selection elements.
	 */
	public void uncheckAll();

	/**
	 * Enables or disables a checkbox. Note that this is not the same as {@link org.olat.core.gui.components.form.flexible.FormItem#setesetEnabled(boolean)} which enables
	 * or disables <i>all</i> checkboxes of this form element.
	 * 
	 * @param key The key of the checkbox.
	 * @param isEnabled <code>true</code> means enabled.
	 */
	public void setEnabled(String key, boolean isEnabled);

	/**
	 * Enables or disables several checkboxes at once by applying {@link MultipleSelectionElement#setEnabled(String, boolean)} to the checkboxes with the keys given in
	 * <code>keys</code>.
	 * 
	 * @param keys Keys of the checkboxes.
	 * @param isEnabled Whether the checkboxes given in <code>keys</code> are to be enabled (<code>true</code>) or disabled (<code>false</code>).
	 */
	public void setEnabled(Set<String> keys, boolean isEnabled);

	/**
	 * Shows or hides a checkbox. Note that this is not the same as {@link org.olat.core.gui.components.form.flexible.FormItem#setVisible(boolean)} which shows or hides
	 * <i>all</i> checkboxes of this form element.
	 * 
	 * @param key The key of the checkbox.
	 * @param isVisible <code>true</code> means visible.
	 */
	public void setVisible(String key, boolean isVisible);

	/**
	 * Shows or hides several checkboxes at once by applying {@link MultipleSelectionElement#setVisible(String, boolean)} to the checkboxes with the keys given in
	 * <code>keys</code>.
	 * 
	 * @param keys Keys of the checkboxes.
	 * @param isVisible Whether the checkboxes given in <code>keys</code> are to be shown (<code>true</code>) or hidden (<code>false</code>).
	 */
	public void setVisible(Set<String> keys, boolean isEnabled);
}