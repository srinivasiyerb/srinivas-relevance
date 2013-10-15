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
package org.olat.core.commons.chiefcontrollers;

import java.util.Locale;
import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.ArrayHelper;
import org.olat.core.util.StringHelper;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.resource.OresHelper;

/**
 * Description:<br>
 * TODO: patrickb Class Description for LanguageChooserController
 * <P>
 * Initial Date: 25.01.2007 <br>
 * 
 * @author patrickb
 */
public class LanguageChooserController extends FormBasicController {

	private SingleSelection langs;

	String curlang;

	public LanguageChooserController(WindowControl wControl, UserRequest ureq) {
		super(ureq, wControl, "langchooser");
		// init variables
		curlang = ureq.getLocale().toString();
		initForm(this.flc, this, ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDefaultController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	@SuppressWarnings("unused")
	protected void formOK(UserRequest ureq) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDefaultController#formInnerEvent(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.FormItem, org.olat.core.gui.components.form.flexible.FormEvent)
	 */
	@Override
	@SuppressWarnings("unused")
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		Locale loc = I18nManager.getInstance().getLocaleOrDefault(getSelectedLanguage());
		MultiUserEvent mue = new LanguageChangedEvent(loc, ureq);
		ureq.getUserSession().setLocale(loc);
		ureq.getUserSession().putEntry("negotiated-locale", loc);
		I18nManager.updateLocaleInfoToThread(ureq.getUserSession());
		OLATResourceable wrappedLocale = OresHelper.createOLATResourceableType(Locale.class);
		ureq.getUserSession().getSingleUserEventCenter().fireEventToListenersOf(mue, wrappedLocale);
		// Update in velocity for flag
		this.flc.contextPut("languageCode", loc.toString());
	}

	/**
	 * selected language
	 * 
	 * @return
	 */
	public String getSelectedLanguage() {
		return langs.getSelectedKey();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDefaultController#initFormElements(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller)
	 */
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, final UserRequest ureq) {

		// SingleSelectionImpl creates following $r.render("..") names in velocity
		// languages_LABEL -> access label of singleselection
		// languages_ERROR -> access error of singleselection
		// languages_EXAMPLE -> access example of singleselection
		// languages_SELBOX -> render whole selection as selectionbox
		// radiobuttons are accessed by appending the key, for example by
		// languages_yes languages_no
		//
		Map<String, String> languages = I18nManager.getInstance().getEnabledLanguagesTranslated();
		String[] langKeys = StringHelper.getMapKeysAsStringArray(languages);
		String[] langValues = StringHelper.getMapValuesAsStringArray(languages);
		ArrayHelper.sort(langKeys, langValues, false, true, false);
		// Build css classes for reference languages
		String[] langCssClasses = I18nManager.getInstance().createLanguageFlagsCssClasses(langKeys, "b_with_small_icon_left");
		langs = uifactory.addDropdownSingleselect("select.language", formLayout, langKeys, langValues, langCssClasses);
		langs.addActionListener(this, FormEvent.ONCHANGE);
		langs.select(curlang, true);
		// Add to velocity for flag
		this.flc.contextPut("languageCode", curlang.toString());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		langs = null;
	}

}