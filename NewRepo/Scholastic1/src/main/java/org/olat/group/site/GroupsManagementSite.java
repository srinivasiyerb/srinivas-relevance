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

package org.olat.group.site;

import java.util.Locale;

import org.olat.ControllerFactory;
import org.olat.core.commons.chiefcontrollers.BaseChiefController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.control.navigation.DefaultNavElement;
import org.olat.core.gui.control.navigation.NavElement;
import org.olat.core.gui.control.navigation.SiteInstance;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.Util;
import org.olat.core.util.resource.OresHelper;
import org.olat.group.ui.context.BGContextManagementController;

/**
 * Description:<br>
 * <P>
 * Initial Date: 19.07.2005 <br>
 * 
 * @author Felix Jost
 */
public class GroupsManagementSite implements SiteInstance {
	private static final OLATResourceable ORES_GROUPSMANAGEMENT = OresHelper.lookupType(BGContextManagementController.class);

	// refer to the definitions in org.olat
	private static final String PACKAGE = Util.getPackageName(BaseChiefController.class);

	private final NavElement origNavElem;
	private NavElement curNavElem;

	/**
	 * 
	 */
	public GroupsManagementSite(final Locale loc) {
		final Translator trans = new PackageTranslator(PACKAGE, loc);
		origNavElem = new DefaultNavElement(trans.translate("topnav.gm"), trans.translate("topnav.gm.alt"), "o_site_groupsmanagement");
		curNavElem = new DefaultNavElement(origNavElem);
	}

	/**
	 * @see org.olat.navigation.SiteInstance#getNavElement()
	 */
	@Override
	public NavElement getNavElement() {
		return curNavElem;
	}

	/**
	 * @see org.olat.navigation.SiteInstance#createController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public MainLayoutController createController(final UserRequest ureq, final WindowControl wControl) {
		final MainLayoutController c = ControllerFactory.createLaunchController(ORES_GROUPSMANAGEMENT, null, ureq, wControl, true);
		return c;
	}

	/**
	 * @see org.olat.navigation.SiteInstance#isKeepState()
	 */
	@Override
	public boolean isKeepState() {
		return true;
	}

	@Override
	public void reset() {
		curNavElem = new DefaultNavElement(origNavElem);
	}

}
