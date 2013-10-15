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
 * Copyright (c) 2008 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.admin.layout;

import java.io.File;
import java.io.FilenameFilter;

import org.olat.admin.SystemAdminMainController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.helpers.Settings;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;

/**
 * <h3>Description:</h3> Admin workflow to configure the application layout
 * <p>
 * Initial Date: 31.03.2008 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public class LayoutAdminController extends FormBasicController {

	private SingleSelection themeSelection;

	public LayoutAdminController(final UserRequest ureq, final WindowControl wControl) {
		// use admin package fallback translator to display warn message about not
		// saving the data (see comment in formInnerEvent method)
		super(ureq, wControl, "layoutadmin", Util.createPackageTranslator(SystemAdminMainController.class, ureq.getLocale()));
		initForm(this.flc, this, ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		// wrapper container that generates standard layout for the form elements
		final FormItemContainer themeAdminFormContainer = FormLayoutContainer.createDefaultFormLayout("themeAdminFormContainer", getTranslator());
		formLayout.add(themeAdminFormContainer);

		final String[] keys = getThemes();
		final String enabledTheme = Settings.getGuiThemeIdentifyer();
		themeSelection = uifactory.addDropdownSingleselect("themeSelection", "form.theme", themeAdminFormContainer, keys, keys, null);
		themeSelection.select(enabledTheme, true);
		themeSelection.addActionListener(listener, FormEvent.ONCHANGE);
	}

	private String[] getThemes() {
		// get all themes from disc
		final String staticAbsPath = WebappHelper.getContextRoot() + "/static/themes";
		final File themesDir = new File(staticAbsPath);
		if (!themesDir.exists()) {
			logWarn("Themes dir not found: " + staticAbsPath, null);
		}
		final File[] themes = themesDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				// remove files - only accept dirs
				if (!new File(dir, name).isDirectory()) { return false; }
				// remove unwanted meta-dirs
				if (name.equalsIgnoreCase("CVS")) {
					return false;
				} else if (name.equalsIgnoreCase(".DS_Store")) {
					return false;
				} else {
					return true;
				}
			}
		});

		final String[] themesStr = new String[themes.length];
		for (int i = 0; i < themes.length; i++) {
			final File theme = themes[i];
			themesStr[i] = theme.getName();
		}
		return themesStr;
	}

	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		// set new theme in Settings
		final String newThemeIdentifyer = themeSelection.getSelectedKey();
		Settings.setGuiThemeIdentifyerGlobally(newThemeIdentifyer);
		// use new theme in current window
		getWindowControl().getWindowBackOffice().getWindow().getGuiTheme().init(newThemeIdentifyer);
		getWindowControl().getWindowBackOffice().getWindow().setDirty(true);
		//
		logAudit("GUI theme changed", newThemeIdentifyer);
		fireEvent(ureq, Event.CHANGED_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// nothing to clean up
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		// saving already done in formInnerEvent method - no submit button
	}

}
