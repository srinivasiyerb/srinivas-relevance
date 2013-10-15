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
 * Copyright (c) 1999-2008 at frentix GmbH, Switzerland, http://www.frentix.com
 * <p>
 */
package org.olat.modules.glossary;

import java.util.Properties;

import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.modules.glossary.GlossaryItemManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.resource.OLATResource;

/**
 * Description:<br>
 * allows to set register/index on/off for repository typ glossary
 * <P>
 * Initial Date: 20.01.2009 <br>
 * 
 * @author Roman Haag, frentix GmbH, roman.haag@frentix.com
 */
public class GlossaryRegisterSettingsController extends FormBasicController {

	private final OLATResource olatresource;
	private MultipleSelectionElement regOnOff;
	private final OlatRootFolderImpl glossaryFolder;

	public GlossaryRegisterSettingsController(final UserRequest ureq, final WindowControl control, final OLATResource resource) {
		super(ureq, control);
		this.olatresource = resource;
		glossaryFolder = GlossaryManager.getInstance().getGlossaryRootFolder(olatresource);

		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formInnerEvent(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.FormItem, org.olat.core.gui.components.form.flexible.impl.FormEvent)
	 */
	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (source == regOnOff) {
			final boolean regOnChecked = regOnOff.isSelected(0);
			final GlossaryItemManager gIM = GlossaryItemManager.getInstance();
			final Properties glossProps = gIM.getGlossaryConfig(glossaryFolder);
			glossProps.put(GlossaryItemManager.REGISTER_ONOFF, String.valueOf(regOnChecked));
			gIM.setGlossaryConfig(glossaryFolder, glossProps);
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(final UserRequest ureq) {
		// saved in innerEvent
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		setFormTitle("register.title");
		setFormDescription("register.intro");
		final String[] regKeys = { "true" };
		final String[] regValues = { "" };
		final String[] regCSS = new String[1];

		regOnOff = uifactory.addCheckboxesHorizontal("register.onoff", formLayout, regKeys, regValues, regCSS);
		regOnOff.addActionListener(listener, FormEvent.ONCLICK);

		final Properties glossProps = GlossaryItemManager.getInstance().getGlossaryConfig(glossaryFolder);
		final String configuredStatus = glossProps.getProperty(GlossaryItemManager.REGISTER_ONOFF);
		if (configuredStatus != null) {
			regOnOff.select(configuredStatus, true);
		}
	}

}
