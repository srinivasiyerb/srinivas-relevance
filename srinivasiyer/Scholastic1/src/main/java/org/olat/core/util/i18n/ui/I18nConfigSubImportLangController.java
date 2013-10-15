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

package org.olat.core.util.i18n.ui;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.ArrayHelper;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;

/**
 * <h3>Description:</h3> This form allows the user to import languages from a jar file. <h3>Events fired by this controller</h3>
 * <ul>
 * <li>Event.CANCELLED_EVENT</li>
 * <li>Event.DONE_EVENT</li>
 * </ul>
 * <p>
 * Initial Date: 08.12.2008 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */

class I18nConfigSubImportLangController extends FormBasicController {
	private FileElement importFile;
	private FormLink cancelButton;
	private MultipleSelectionElement importKeys;

	public I18nConfigSubImportLangController(UserRequest ureq, WindowControl control) {
		super(ureq, control, LAYOUT_VERTICAL);
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, @SuppressWarnings("unused") Controller listener, @SuppressWarnings("unused") UserRequest ureq) {
		// A title, displayed in fieldset
		setFormTitle("configuration.management.package.import.title");
		if (I18nModule.isTransToolEnabled()) {
			setFormDescription("configuration.management.package.import.description.transserver");
		} else {
			setFormDescription("configuration.management.package.import.description");
		}
		//
		// The import file upload
		importFile = uifactory.addFileElement("configuration.management.package.import.file", formLayout);
		importFile.setLabel("configuration.management.package.import.file", null);
		importFile.setMandatory(true, "configuration.management.package.import.file.error.mandatory");
		// Limit to jar files and set upload limit to 50 MB
		importFile.setMaxUploadSizeKB(50000, "configuration.management.package.import.file.error.size", null);
		Set<String> mimeTypes = new HashSet<String>();
		mimeTypes.add("application/java-archive");
		mimeTypes.add("application/x-jar");
		mimeTypes.add("application/x-java-jar");
		importFile.limitToMimeType(mimeTypes, "configuration.management.package.import.file.error.type", null);
		importFile.addActionListener(this, FormEvent.ONCHANGE); // trigger auto-upload
		//
		// Add checkboxes for the found languages - hide so far
		String[] langKeys = new String[] {};
		importKeys = uifactory.addCheckboxesVertical("configuration.management.package.import.select", this.flc, langKeys, langKeys, null, 1);
		importKeys.setVisible(false);
		//
		// Add cancel and submit in button group layout
		FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonGroupLayout", getTranslator());
		formLayout.add(buttonGroupLayout);
		cancelButton = uifactory.addFormLink("cancel", buttonGroupLayout, Link.BUTTON);
		uifactory.addFormSubmitButton("configuration.management.package.import", buttonGroupLayout);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		if (importKeys.isVisible() && importKeys.getSelectedKeys().size() > 0) {
			Set<String> importLangKeys = importKeys.getSelectedKeys();
			Set<String> alreadyInstalledLangs = new HashSet<String>();
			for (String langKey : importLangKeys) {
				if (I18nModule.getAvailableLanguageKeys().contains(langKey)) {
					alreadyInstalledLangs.add(langKey);
				}
			}
			if (I18nModule.isTransToolEnabled()) {
				// In translation mode importing will copy the language package
				// over an existing language or create a new language
				File tmpJar = importFile.getUploadFile();
				I18nManager.getInstance().copyLanguagesFromJar(tmpJar, importLangKeys);
				logAudit("Uploaded languages from jar::" + importFile.getUploadFileName(), null);
				showInfo("configuration.management.package.import.success", importLangKeys.toString());

			} else {
				// In language adaption mode: import is copied to user managed i18n package space in olatdata
				if (alreadyInstalledLangs.size() == importLangKeys.size()) {
					showError("configuration.management.package.import.failure.installed");
					return;
				}
				// Ok, contains at least one language, copy to lang pack dir
				importFile.moveUploadFileTo(I18nModule.LANG_PACKS_DIRECTORY);
				logAudit("Uploaded language pack::" + importFile.getUploadFileName(), null);

				if (alreadyInstalledLangs.size() > 0) {
					getWindowControl().setWarning(
							getTranslator().translate("configuration.management.package.import.success.with.existing",
									new String[] { importLangKeys.toString(), alreadyInstalledLangs.toString() }));
				} else {
					showInfo("configuration.management.package.import.success", importLangKeys.toString());
				}
			}
			// Reset i18n system
			I18nModule.reInitializeAndFlushCache();
			fireEvent(ureq, Event.DONE_EVENT);
		}
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		super.formInnerEvent(ureq, source, event);
		if (source == cancelButton) {
			fireEvent(ureq, Event.CANCELLED_EVENT);

		} else if (source == importFile) {
			if (importFile.isUploadSuccess()) {
				File tmpJar = importFile.getUploadFile();
				Set<String> importLangKeys = I18nManager.getInstance().sarchForAvailableLanguagesInJarFile(tmpJar, true);
				if (importLangKeys.size() == 0) {
					showError("configuration.management.package.import.failure.empty");
					return;
				}
				//
				// enable language key selection
				String[] langKeys = ArrayHelper.toArray(importLangKeys);
				importKeys.setKeysAndValues(langKeys, langKeys, null);
				importKeys.selectAll();
				importKeys.setVisible(true);
				// In language adaption mode the import is done as a package - can't deselect anything
				importKeys.setEnabled(false);
			}
		}
	}

	@Override
	protected void doDispose() {
		// nothing to dispose
	}

}
