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

package org.olat.portal.macartney;

import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.portal.AbstractPortlet;
import org.olat.core.gui.control.generic.portal.Portlet;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.util.Util;

/**
 * Description:<br>
 * Displays the the macartney of the day comic strip
 * <P>
 * Initial Date: 24.08.2005 <br>
 * 
 * @author gnaegi
 */
public class MacartneyPortlet extends AbstractPortlet {
	private Controller runCtr;

	/**
	 * @see org.olat.gui.control.generic.portal.AbstractPortlet#createInstance(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest, java.util.Map)
	 */
	@Override
	public Portlet createInstance(final WindowControl wControl, final UserRequest ureq, final Map configuration) {
		final Portlet p = new MacartneyPortlet();
		p.setName(this.getName());
		p.setConfiguration(configuration);
		p.setTranslator(new PackageTranslator(Util.getPackageName(MacartneyPortlet.class), ureq.getLocale()));
		return p;
	}

	/**
	 * @see org.olat.gui.control.generic.portal.Portlet#getTitle()
	 */
	@Override
	public String getTitle() {
		return getTranslator().translate("macartney.title");
	}

	/**
	 * @see org.olat.gui.control.generic.portal.Portlet#getDescription()
	 */
	@Override
	public String getDescription() {
		return getTranslator().translate("macartney.description");
	}

	/**
	 * @see org.olat.gui.control.generic.portal.Portlet#getInitialRunComponent(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest)
	 */
	@Override
	public Component getInitialRunComponent(final WindowControl wControl, final UserRequest ureq) {
		if (this.runCtr != null) {
			runCtr.dispose();
		}
		this.runCtr = new MacartneyPortletRunController(ureq, wControl, getConfiguration());
		return this.runCtr.getInitialComponent();
	}

	/**
	 * @see org.olat.core.gui.control.Disposable#dispose(boolean)
	 */
	@Override
	public void dispose() {
		disposeRunComponent();
	}

	/**
	 * @see org.olat.gui.control.generic.portal.Portlet#getCssClass()
	 */
	@Override
	public String getCssClass() {
		return "o_portlet_macartney";
	}

	/**
	 * @see org.olat.gui.control.generic.portal.Portlet#disposeRunComponent(boolean)
	 */
	@Override
	public void disposeRunComponent() {
		if (this.runCtr != null) {
			this.runCtr.dispose();
			this.runCtr = null;
		}
	}

}
