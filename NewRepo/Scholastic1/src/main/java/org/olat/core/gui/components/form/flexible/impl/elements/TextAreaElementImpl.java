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
 * Copyright (c) 1999-2008 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.core.gui.components.form.flexible.impl.elements;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;

/**
 * Description:<br>
 * TextArea-Element for FlexiForm
 * <P>
 * Initial Date: 31.01.2008 <br>
 * 
 * @author rhaag
 */
public abstract class TextAreaElementImpl extends AbstractTextElement {

	protected TextAreaElementComponent component;

	/**
	 * Constructor for specialized TextElements, i.e. IntegerElementImpl.
	 * 
	 * @param name
	 * @param predefinedValue Initial value
	 * @param rows the number of lines or -1 to use default value
	 * @param cols the number of characters per line or -1 to use 100% of the available space
	 * @param isAutoHeightEnabled true: element expands to fit content height, (max 100 lines); false: specified rows used
	 */
	public TextAreaElementImpl(String name, String predefinedValue, int rows, int cols, boolean isAutoHeightEnabled) {
		this(name, rows, cols, isAutoHeightEnabled);
		setValue(predefinedValue);
	}

	/**
	 * Constructor for specialized TextElements, i.e. IntegerElementImpl.
	 * 
	 * @param name
	 * @param rows the number of lines or -1 to use default value
	 * @param cols the number of characters per line or -1 to use 100% of the available space
	 * @param isAutoHeightEnabled true: element expands to fit content height, (max 100 lines); false: specified rows used
	 */
	protected TextAreaElementImpl(String name, int rows, int cols, boolean isAutoHeightEnabled) {
		super(name);
		this.component = new TextAreaElementComponent(this, rows, cols, isAutoHeightEnabled);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormItemImpl#evalFormRequest(org.olat.core.gui.UserRequest)
	 */
	@Override
	public void evalFormRequest(UserRequest ureq) {
		String paramId = String.valueOf(component.getFormDispatchId());
		String value = getRootForm().getRequestParameter(paramId);
		if (value != null) {
			setValue(value);
			// mark associated component dirty, that it gets rerendered
			component.setDirty(true);
		}

	}

	@Override
	protected Component getFormItemComponent() {
		return component;
	}

	@Override
	public void setTranslator(Translator translator) {
		// wrap package translator with fallback form translator
		// hint: do not take this.getClass() but the real class! for package translator creation
		Translator elmTranslator = Util.createPackageTranslator(TextAreaElementImpl.class, translator.getLocale(), translator);
		super.setTranslator(elmTranslator);
	}

}
