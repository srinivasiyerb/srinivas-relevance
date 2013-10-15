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
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.core.util.i18n.ui;

import java.io.File;
import java.util.Set;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.media.NamedFileMediaResource;
import org.olat.core.helpers.Settings;
import org.olat.core.util.ArrayHelper;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;

/**
 * Description:<br>
 * This form allows the user to export languages to a jar file. The language file can then be imported to another instance. <h3>Events fired by this controller</h3>
 * <ul>
 * <li>Event.CANCELLED_EVENT</li>
 * <li>Event.DONE_EVENT</li>
 * </ul>
 * <P>
 * Initial Date: 05.12.2008 <br>
 * 
 * @author gnaegi
 */
class I18nConfigSubExportLangController extends FormBasicController {
	private MultipleSelectionElement exportLangSelection;
	private FormLink cancelButton;
	private FormSubmit submitButton;

	/**
	 * Constructor for the export-language workflow
	 * 
	 * @param ureq
	 * @param control
	 */
	public I18nConfigSubExportLangController(UserRequest ureq, WindowControl control) {
		super(ureq, control, LAYOUT_VERTICAL);
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(FormItemContainer formLayout, @SuppressWarnings("unused") Controller listener, @SuppressWarnings("unused") UserRequest ureq) {
		I18nManager i18nMgr = I18nManager.getInstance();
		// A title, displayed in fieldset
		setFormTitle("configuration.management.package.export.title");
		setFormDescription("configuration.management.package.export.description");
		//
		// Add languages checkboxes
		Set<String> availableKeysUnsorted = I18nModule.getAvailableLanguageKeys();
		String[] availableKeys = ArrayHelper.toArray(availableKeysUnsorted);
		String[] availableValues = new String[availableKeys.length];
		for (int i = 0; i < availableKeys.length; i++) {
			String key = availableKeys[i];
			String explLang = i18nMgr.getLanguageInEnglish(key, false);
			String all = explLang;
			if (explLang != null && !explLang.equals(key)) all += " (" + key + ")";
			availableValues[i] = all;
		}
		ArrayHelper.sort(availableKeys, availableValues, false, true, false);
		String[] availableLangCssClasses = i18nMgr.createLanguageFlagsCssClasses(availableKeys, "b_with_small_icon_left");
		exportLangSelection = uifactory.addCheckboxesVertical("configuration.exportLangSelection", null, formLayout, availableKeys, availableValues,
				availableLangCssClasses, 2);
		exportLangSelection.addActionListener(this, FormEvent.ONCLICK);
		// Add cancel and submit in button group layout
		FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonGroupLayout", getTranslator());
		formLayout.add(buttonGroupLayout);
		cancelButton = uifactory.addFormLink("cancel", buttonGroupLayout, Link.BUTTON);
		submitButton = uifactory.addFormSubmitButton("configuration.management.package.export", buttonGroupLayout);
		submitButton.setEnabled(false); // enable as soon as something is checked
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(UserRequest ureq) {
		Set<String> toExport = exportLangSelection.getSelectedKeys();
		logDebug("Following languages selected for export::" + toExport.toString(), null);
		if (toExport.size() == 0) {
			// should not happen since button disabled
			return;
		}
		String tmpFileName = CodeHelper.getGlobalForeverUniqueID();
		// crate new temp file
		File exportFile = I18nManager.getInstance().createLanguageJarFile(toExport, tmpFileName);
		if (exportFile != null) {
			String fileName = "language_export_" + Settings.getApplicationName() + "_" + Settings.getVersion() + ".jar";
			// Create a temporary media resource that gets deleted from the
			// file system automatically after delivery
			NamedFileMediaResource mediaResource = new NamedFileMediaResource(exportFile, fileName, "language download for olatcore webapp framework", true);
			logDebug("Exporting tmp file::" + exportFile.getAbsolutePath() + " as fileName::" + fileName, null);

			// TODO open in new window (click invalid)
			ureq.getDispatchResult().setResultingMediaResource(mediaResource);
			fireEvent(ureq, Event.DONE_EVENT);
		}
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		super.formInnerEvent(ureq, source, event);
		if (source == cancelButton) {
			fireEvent(ureq, Event.CANCELLED_EVENT);

		} else if (source == exportLangSelection) {
			if (exportLangSelection.getSelectedKeys().size() == 0) {
				submitButton.setEnabled(false);
			} else {
				submitButton.setEnabled(true);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing to dispose
	}

}
