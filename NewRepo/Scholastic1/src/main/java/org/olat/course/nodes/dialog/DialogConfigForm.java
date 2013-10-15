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

package org.olat.course.nodes.dialog;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.logging.AssertException;
import org.olat.modules.ModuleConfiguration;

/**
 * Description:<br>
 * TODO: guido Class Description for DialogConfigForm
 * <P>
 * Initial Date: 08.11.2005 <br>
 * 
 * @author guido
 */
public class DialogConfigForm extends FormBasicController {

	public static final String DIALOG_CONFIG_INTEGRATION = "dialog_integration";
	/** Integration configuration value: integrate it into course showing the course menu **/
	public static final String CONFIG_INTEGRATION_VALUE_INLINE = "inline";
	/** Integration configuration value: integrate it using a modal dialog withoud course menu **/
	public static final String CONFIG_INTEGRATION_VALUE_MODAL = "modal";
	/** Integration configuration value: integrate it as a pop up window **/
	public static final String CONFIG_INTEGRATION_VALUE_POPUP = "popup";

	private SelectionElement select;

	private final ModuleConfiguration config;

	/**
	 * @param name
	 * @param config the ModuleConfiguration
	 * @param translator
	 */
	public DialogConfigForm(final UserRequest ureq, final WindowControl wControl, final ModuleConfiguration config) {
		super(ureq, wControl);

		if (config == null) { throw new AssertException("module configuration is null!"); }

		this.config = config;

		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.Form#validate(org.olat.core.gui.UserRequest)
	 */
	public boolean validate() {
		return true;
	}

	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (config == null) { throw new AssertException("Try to do updateConfiguration() but module configuration is null"); }
		config.set(DialogConfigForm.DIALOG_CONFIG_INTEGRATION, select.isSelected(0) ? CONFIG_INTEGRATION_VALUE_POPUP : CONFIG_INTEGRATION_VALUE_INLINE);
		config.setConfigurationVersion(1);
		fireEvent(ureq, Event.CHANGED_EVENT);
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		//
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {

		if (config == null) { throw new AssertException("module configuration is null!"); }

		select = uifactory.addCheckboxesVertical("forumAsPopup", "selection.forumAsPopup.label", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);

		final String selectConfig = (String) config.get(DialogConfigForm.DIALOG_CONFIG_INTEGRATION);
		select.select("xx", selectConfig == CONFIG_INTEGRATION_VALUE_POPUP);
		select.addActionListener(this, FormEvent.ONCLICK);
	}

	@Override
	protected void doDispose() {
		//
	}

}
