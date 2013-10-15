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

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.elements.FormToggle;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Event;

/**
 * Description:<br>
 * Extending the formlink to be used as a toggle-switch supporting on/off.
 * <P>
 * Initial Date: 21.07.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, frentix GmbH
 */
public class FormToggleImpl extends FormLinkImpl implements FormToggle {

	private String activeCSS = "b_toggle b_small b_on";
	private String inactiveCSS = "b_toggle b_small";
	private boolean isOn = false;

	public FormToggleImpl(String name, String cmd, String i18n) {
		super(name, cmd, i18n, 0);
		setCustomEnabledLinkCSS(inactiveCSS);
	}

	public FormToggleImpl(String name, String cmd, String toggleText, int nontranslated) {
		super(name, cmd, toggleText, nontranslated);
		setCustomEnabledLinkCSS(inactiveCSS);
	}

	@Override
	public void dispatchFormRequest(UserRequest ureq) {
		toggle();
		getRootForm().fireFormEvent(ureq, new FormEvent(Event.DONE_EVENT, this, FormEvent.ONCLICK));
	}

	@Override
	public void toggle() {
		if (isOn()) {
			toggleOff();
		} else {
			toggleOn();
		}
	}

	@Override
	public boolean isOn() {
		return isOn;
	}

	@Override
	public void toggleOn() {
		isOn = true;
		setCustomEnabledLinkCSS(activeCSS);
	}

	@Override
	public void toggleOff() {
		isOn = false;
		setCustomEnabledLinkCSS(inactiveCSS);
	}

	@Override
	public void setToggledOnCSS(String toggledOnCSS) {
		activeCSS = toggledOnCSS;
	}

	@Override
	public void setToggledOffCSS(String toggledOffCSS) {
		inactiveCSS = toggledOffCSS;
	}

}
