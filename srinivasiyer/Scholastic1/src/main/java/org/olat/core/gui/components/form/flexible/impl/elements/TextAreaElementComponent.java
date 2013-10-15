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
import org.olat.core.gui.components.form.flexible.impl.FormBaseComponentImpl;

/**
 * Description:<br>
 * Initial Date: 31.01.08 <br>
 * 
 * @author rhaag
 */
class TextAreaElementComponent extends FormBaseComponentImpl {
	private ComponentRenderer RENDERER = new TextAreaElementRenderer();
	private TextAreaElementImpl element;
	private int cols;
	private int rows;
	private boolean autoHeightEnabled = false;

	/**
	 * Constructor for a text area element
	 * 
	 * @param element
	 * @param rows the number of lines or -1 to use default value
	 * @param cols the number of characters per line or -1 to use 100% of the available space
	 * @param isAutoHeightEnabled true: element expands to fit content height, (max 100 lines); false: specified rows used
	 */
	public TextAreaElementComponent(TextAreaElementImpl element, int rows, int cols, boolean isAutoHeightEnabled) {
		super(element.getName());
		this.element = element;
		setCols(cols);
		setRows(rows);
		this.autoHeightEnabled = isAutoHeightEnabled;
	}

	TextAreaElementImpl getTextAreaElementImpl() {
		return element;
	}

	/**
	 * @see org.olat.core.gui.components.Component#getHTMLRendererSingleton()
	 */
	@Override
	public ComponentRenderer getHTMLRendererSingleton() {
		return RENDERER;
	}

	public int getCols() {
		return cols;
	}

	public void setCols(int cols) {
		this.cols = cols;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public boolean isAutoHeightEnabled() {
		return autoHeightEnabled;
	}

}
