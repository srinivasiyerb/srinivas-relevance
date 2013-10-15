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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.core.gui.components.form.flexible.impl.elements;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.elements.SpacerElement;
import org.olat.core.gui.components.form.flexible.impl.FormItemImpl;

/**
 * Implements an HTML horizontal bar (&lt;HR&gt;) element.
 * 
 * @author twuersch
 */
public class SpacerElementImpl extends FormItemImpl implements SpacerElement {
	private String spacerCssClass;

	public SpacerElementImpl(String name) {
		super(name);
		this.component = new SpacerElementComponent(this);
	}

	private SpacerElementComponent component;

	@Override
	public void evalFormRequest(UserRequest ureq) {
		// No need to do that for this element.

	}

	@Override
	protected Component getFormItemComponent() {
		return component;
	}

	@Override
	public void reset() {
		// No need to do that for this element.
	}

	@Override
	protected void rootFormAvailable() {
		// Not available for this element.

	}

	@Override
	public void validate(List validationResults) {
		// No need to do that for this element.
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.elements.SpacerElement#setSpacerCssClass(java.lang.String)
	 */
	@Override
	public void setSpacerCssClass(String spacerCssClass) {
		this.spacerCssClass = spacerCssClass;
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.elements.SpacerElement#getSpacerCssClass()
	 */
	@Override
	public String getSpacerCssClass() {
		return spacerCssClass;
	}

}
