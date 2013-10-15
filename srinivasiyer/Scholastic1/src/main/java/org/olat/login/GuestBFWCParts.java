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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.BaseFullWebappControllerParts;
import org.olat.core.extensions.ExtManager;
import org.olat.core.extensions.Extension;
import org.olat.core.extensions.sitescreator.SitesCreator;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.navigation.SiteDefinition;
import org.olat.core.gui.control.navigation.SiteDefinitions;
import org.olat.core.gui.control.navigation.SiteInstance;
import org.olat.gui.control.OlatGuestFooterController;
import org.olat.gui.control.OlatGuestTopNavController;

/**
 * Description:<br>
 * TODO: patrickb Class Description for GuestBFWCParts
 * <P>
 * Initial Date: 30.01.2008 <br>
 * 
 * @author patrickb
 */
public class GuestBFWCParts implements BaseFullWebappControllerParts {

	/**
	 * @see org.olat.core.commons.fullWebApp.BaseFullWebappControllerParts#createFooterController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createFooterController(final UserRequest ureq, final WindowControl wControl) {
		Controller footerCtr = null;
		// ----------- footer, optional (e.g. for copyright, powered by) ------------------
		if (CoreSpringFactory.containsBean("fullWebApp.GuestFooterControllerCreator")) {
			final ControllerCreator footerCreator = (ControllerCreator) CoreSpringFactory.getBean("fullWebApp.GuestFooterControllerCreator");
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
		if (CoreSpringFactory.containsBean("fullWebApp.GuestHeaderControllerCreator")) {
			final ControllerCreator headerControllerCreator = (ControllerCreator) CoreSpringFactory.getBean("fullWebApp.GuestHeaderControllerCreator");
			headerCtr = headerControllerCreator.createController(ureq, wControl);
		}
		return headerCtr;
	}

	/**
	 * @see org.olat.core.commons.fullWebApp.BaseFullWebappControllerParts#createTopNavController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createTopNavController(final UserRequest ureq, final WindowControl wControl) {
		return new OlatGuestTopNavController(ureq, wControl);
	}

	@Override
	public Controller getContentController(final UserRequest ureq, final WindowControl wControl) {
		return null;
	}

	/**
	 * @see org.olat.core.commons.fullWebApp.BaseFullWebappControllerParts#getSiteInstances(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public List<SiteInstance> getSiteInstances(final UserRequest ureq, final WindowControl wControl) {
		final List<SiteInstance> sites = new ArrayList<SiteInstance>();
		final SiteDefinitions sitedefs = (SiteDefinitions) CoreSpringFactory.getBean("olatsites");
		final List<SiteDefinition> sitedeflist = sitedefs.getSiteDefList();

		for (final Iterator<SiteDefinition> it_sites = sitedeflist.iterator(); it_sites.hasNext();) {
			final SiteDefinition sitedef = it_sites.next();
			final SiteInstance site = sitedef.createSite(ureq, wControl);
			if (site != null) {
				// site == null means that site is not visible to the current user
				sites.add(site);
			}
		}

		// let all extensions add sitedefinitions
		final ExtManager extm = ExtManager.getInstance();
		final Class extensionPointSites = DTabs.class;
		final int cnt = extm.getExtensionCnt();
		for (int i = 0; i < cnt; i++) {
			final Extension anExt = extm.getExtension(i);
			// check for sites
			final SitesCreator sc = (SitesCreator) anExt.getExtensionFor(extensionPointSites.getName());
			if (sc != null) {
				final List extsitedefs = sc.createSiteDefinitions();
				for (final Iterator it_extsites = extsitedefs.iterator(); it_extsites.hasNext();) {
					final SiteDefinition sdef = (SiteDefinition) it_extsites.next();
					final SiteInstance si = sdef.createSite(ureq, wControl);
					sites.add(si);
					extm.inform(extensionPointSites, anExt, "added sitedefinition " + sdef.getClass().getName());
				}
			}
		}

		return sites;
	}

}
