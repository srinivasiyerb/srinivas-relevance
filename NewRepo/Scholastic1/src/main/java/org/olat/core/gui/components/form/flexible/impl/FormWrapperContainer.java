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

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.components.Container;
import org.olat.core.gui.render.ValidationResult;
import org.olat.core.gui.translator.Translator;

class FormWrapperContainer extends Container {

	// Renderer
	private static ComponentRenderer RENDERER = new FormWrapperContainerRenderer();
	// associated form knows the choosen layout
	private Form form;
	private boolean firstInit;

	/**
	 * @param name
	 * @param translator
	 * @param form
	 */
	public FormWrapperContainer(String name, Translator translator, Form form) {
		super(name, translator);
		this.form = form;
		firstInit = false;
	}

	public String getDispatchFieldId() {
		return form.getDispatchFieldId();
	}

	public String getEventFieldId() {
		return form.getEventFieldId();
	}

	public String getFormName() {
		return form.getFormName();
	}

	/**
	 * @return
	 */
	Container getFormLayout() {
		return form.getFormLayout();
	}

	/**
	 * @return true: form contains multipart elements; false: form does not contain multipart elements
	 */
	boolean isMultipartEnabled() {
		return form.isMultipartEnabled();
	}

	/**
	 * @see org.olat.core.gui.components.Component#doDispatchRequest(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void doDispatchRequest(UserRequest ureq) {
		//
		form.evalFormRequest(ureq);
		//
	}

	/**
	 * @param ureq
	 * @param ok
	 */
	void fireValidation(UserRequest ureq, boolean ok) {
		if (ok) {
			fireEvent(ureq, org.olat.core.gui.components.form.Form.EVNT_VALIDATION_OK);
		} else {
			fireEvent(ureq, org.olat.core.gui.components.form.Form.EVNT_VALIDATION_NOK);
		}
	}

	/**
	 * @see org.olat.core.gui.components.Component#validate(org.olat.core.gui.UserRequest, org.olat.core.gui.render.ValidationResult)
	 */
	@Override
	public void validate(UserRequest ureq, ValidationResult vr) {
		super.validate(ureq, vr);
		if (!firstInit) {
			// initialise dependency rules
			form.evalAllFormDependencyRules(ureq);
			//
			firstInit = true;
		}
	}

	@Override
	public ComponentRenderer getHTMLRendererSingleton() {
		return RENDERER;
	}

	public void fireFormEvent(UserRequest ureq, FormEvent event) {
		fireEvent(ureq, event);
	}

}