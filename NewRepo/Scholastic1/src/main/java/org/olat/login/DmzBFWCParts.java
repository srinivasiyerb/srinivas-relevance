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
package org.olat.login;

import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.BaseFullWebappControllerParts;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.gui.control.navigation.SiteInstance;
import org.olat.gui.control.OlatGuestFooterController;

/**
 * Description:<br>
 * TODO: patrickb Class Description for DmzBFWCParts
 * <P>
 * Initial Date: 29.01.2008 <br>
 * 
 * @author patrickb
 */
public class DmzBFWCParts implements BaseFullWebappControllerParts {

	private ControllerCreator contentControllerCreator;
	private boolean showTopNav = true; // default

	/**
	 * @see org.olat.core.commons.fullWebApp.BaseFullWebappControllerParts#createFooterController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createFooterController(final UserRequest ureq, final WindowControl wControl) {
		Controller footerCtr = null;
		// ----------- footer, optional (e.g. for copyright, powered by) ------------------
		if (CoreSpringFactory.containsBean("fullWebApp.DMZFooterControllerCreator")) {
			final ControllerCreator footerCreator = (ControllerCreator) CoreSpringFactory.getBean("fullWebApp.DMZFooterControllerCreator");
			footerCtr = footerCreator.createController(ureq, wControl);
		} else {
			footerCtr = new OlatGuestFooterController(ureq, wControl);
		}
		return footerCtr;
	}

	/**
	 * @see org.olat.core.commons.fullWebApp.BaseFullWebappControllerParts#createHeaderController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createHeaderController(final UserRequest ureq, final WindowControl wControl) {
		Controller headerCtr = null;
		// ----------- header, optional (e.g. for logo, advertising ) ------------------
		if (CoreSpringFactory.containsBean("fullWebApp.DMZHeaderControllerCreator")) {
			final ControllerCreator headerControllerCreator = (ControllerCreator) CoreSpringFactory.getBean("fullWebApp.DMZHeaderControllerCreator");
			headerCtr = headerControllerCreator.createController(ureq, wControl);
		}
		return headerCtr;
	}

	/**
	 * @see org.olat.core.commons.fullWebApp.BaseFullWebappControllerParts#createTopNavController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createTopNavController(final UserRequest ureq, final WindowControl wControl) {
		if (showTopNav) {
			Controller topNavCtr = null;
			if (CoreSpringFactory.containsBean("fullWebApp.DMZTopNavControllerCreator")) {
				final ControllerCreator headerControllerCreator = (ControllerCreator) CoreSpringFactory.getBean("fullWebApp.DMZTopNavControllerCreator");
				topNavCtr = headerControllerCreator.createController(ureq, wControl);
			}
			return topNavCtr;
		} else {
			return null;
		}
	}

	public void setContentControllerCreator(final ControllerCreator contentControllerCreator) {
		this.contentControllerCreator = contentControllerCreator;
	}

	@Override
	public Controller getContentController(final UserRequest ureq, final WindowControl wControl) {
		return contentControllerCreator.createController(ureq, wControl);
	}

	/**
	 * @see org.olat.core.commons.fullWebApp.BaseFullWebappControllerParts#getSiteInstances(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public List<SiteInstance> getSiteInstances(final UserRequest ureq, final WindowControl wControl) {
		return null;
	}

	/**
	 * Enable or disable the dmz top navigation. This is usefull to remove the lang-chooser which causes troubles in the registratoin workflow.
	 * 
	 * @param showTopNav
	 */
	public void showTopNav(final boolean showTopNavController) {
		this.showTopNav = showTopNavController;
	}

}
