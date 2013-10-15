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
package org.olat.core.gui.components.form.flexible.impl;

import java.util.Map;

import org.olat.core.gui.components.form.flexible.FormDecorator;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SpacerElement;

/**
 * Description:<br>
 * TODO: patrickb Class Description for FormDecorator
 * <P>
 * Initial Date: 06.12.2006 <br>
 * 
 * @author patrickb
 */
public class FormDecoratorImpl implements FormDecorator {

	FormItemContainer container;

	public FormDecoratorImpl(FormItemContainer container) {
		this.container = container;
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#hasError(java.lang.String)
	 */
	@Override
	public boolean hasError(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.hasError();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#hasExample(java.lang.String)
	 */
	@Override
	public boolean hasExample(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.hasExample();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#hasLabel(java.lang.String)
	 */
	@Override
	public boolean hasLabel(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.hasLabel();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#isMandatory(java.lang.String)
	 */
	@Override
	public boolean isMandatory(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.isMandatory();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#isEnabled(java.lang.String)
	 */
	@Override
	public boolean isEnabled(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.isEnabled();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#isVisible(java.lang.String)
	 */
	@Override
	public boolean isVisible(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.isVisible();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#getItemId(java.lang.String)
	 */
	@Override
	public String getItemId(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? "" : foco.getFormDispatchId();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#isSpacerElement(java.lang.String)
	 */
	@Override
	public boolean isSpacerElement(String formItemName) {
		FormItem item = getFormItem(formItemName);
		if (item == null) return false;
		else return (item instanceof SpacerElement);
	}

	/**
	 * Internal helper to get a form item for the given name
	 * 
	 * @param formItemName
	 * @return
	 */
	private FormItem getFormItem(String formItemName) {
		Map<String, FormItem> comps = container.getFormComponents();
		FormItem foco = comps.get(formItemName);
		return foco;
	}

}
