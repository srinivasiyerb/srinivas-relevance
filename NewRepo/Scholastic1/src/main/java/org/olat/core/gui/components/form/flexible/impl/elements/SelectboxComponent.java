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
package org.olat.core.gui.components.form.flexible.impl.elements;

import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.components.form.flexible.elements.SelectionElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBaseComponentImpl;
import org.olat.core.gui.translator.Translator;

/**
 * Description:<br>
 * TODO: patrickb Class Description for SingleSelectionSelectboxComponent
 * <P>
 * Initial Date: 02.01.2007 <br>
 * 
 * @author patrickb
 */
class SelectboxComponent extends FormBaseComponentImpl {

	private static final ComponentRenderer RENDERER = new SelectboxRenderer();
	private SelectionElement selectionWrapper;
	private String[] values;
	private String[] options;
	private String[] cssClasses;

	public SelectboxComponent(String name, Translator translator, SelectionElement selectionWrapper, String[] options, String[] values, String[] cssClasses) {
		super(name, translator);
		this.selectionWrapper = selectionWrapper;
		this.options = options;
		this.values = values;
		this.cssClasses = cssClasses;
	}

	/**
	 * @see org.olat.core.gui.components.Component#getHTMLRendererSingleton()
	 */
	@Override
	public ComponentRenderer getHTMLRendererSingleton() {
		return RENDERER;
	}

	public String getGroupingName() {
		return getComponentName();
	}

	public String[] getOptions() {
		return options;
	}

	public String[] getValues() {
		return values;
	}

	public String[] getCssClasses() {
		return cssClasses;
	}

	public boolean isSelected(int i) {
		return selectionWrapper.isSelected(i);
	}

	/**
	 * wheter this select box allows multiple values to be selected or not
	 * 
	 * @param isMultiSelect
	 */
	public boolean isMultiSelect() {
		return selectionWrapper.isMultiselect();
	}

	public Form getRootForm() {
		return selectionWrapper.getRootForm();
	}

	public int getAction() {
		return selectionWrapper.getAction();
	}

	public String getSelectionElementFormDisId() {
		return selectionWrapper.getFormDispatchId();
	}

}
