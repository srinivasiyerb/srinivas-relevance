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

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.components.form.flexible.impl.FormBaseComponentImpl;
import org.olat.core.gui.control.JSAndCSSAdder;
import org.olat.core.gui.render.ValidationResult;
import org.olat.core.gui.translator.Translator;

/**
 * Description:<br>
 * TODO: patrickb Class Description for JSDateChooserComponent
 * <P>
 * Initial Date: 19.01.2007 <br>
 * 
 * @author patrickb
 */
class JSDateChooserComponent extends FormBaseComponentImpl {

	/**
	 * @see org.olat.core.gui.components.Component#getDispatchID() without this: the events are sent from the text-component, but the id will be the one from the
	 *      datechooser-span so the component cant be found while dispatching. and therefore eventhandling won't work. by using the id from the text-component, it should
	 *      work as expected. See OLAT-4735.
	 */
	@Override
	public long getDispatchID() {
		return (element.getTextElementComponent().getDispatchID());
	}

	private final static ComponentRenderer RENDERER = new JSDateChooserRenderer();
	private JSDateChooser element;

	public JSDateChooserComponent(JSDateChooser element) {
		super(element.getName());
		this.element = element;
	}

	/**
	 * @see org.olat.core.gui.components.Component#getHTMLRendererSingleton()
	 */
	@Override
	public ComponentRenderer getHTMLRendererSingleton() {
		return RENDERER;
	}

	public TextElementComponent getTextElementComponent() {
		return element.getTextElementComponent();
	}

	public String getDateChooserDateFormat() {
		return element.getDateChooserDateFormat();
	}

	public boolean isDateChooserTimeEnabled() {
		return element.isDateChooserTimeEnabled();
	}

	public Translator getElementTranslator() {
		return element.getTranslator();
	}

	public String getExampleDateString() {
		return element.getExampleDateString();
	}

	/**
	 * @see org.olat.core.gui.components.Component#validate(org.olat.core.gui.UserRequest, org.olat.core.gui.render.ValidationResult)
	 */
	@Override
	public void validate(UserRequest ureq, ValidationResult vr) {
		super.validate(ureq, vr);
		JSAndCSSAdder jsa = vr.getJsAndCSSAdder();
		// FIXME:FG:THEME: calendar.css files for themes
		jsa.addRequiredCSSFile(org.olat.core.gui.components.form.Form.class, "css/jscalendar.css", false);
		jsa.addRequiredJsFile(org.olat.core.gui.components.form.Form.class, "js/jscalendar/calendar.js");
		jsa.addRequiredJsFile(org.olat.core.gui.components.form.Form.class, "js/jscalendar/olatcalendartranslator.js");
		jsa.addRequiredJsFile(org.olat.core.gui.components.form.Form.class, "js/jscalendar/calendar-setup.js");
	}

}
