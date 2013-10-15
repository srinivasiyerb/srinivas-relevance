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
package org.olat.portfolio;

import org.olat.NewControllerFactory;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.ContextEntryControllerCreator;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.structel.PortfolioStructureMap;

/**
 * Description:<br>
 * load my maps menu-entry. config here instead of xml allows en-/disabling at runtime
 * <P>
 * Initial Date: 03.08.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
@SuppressWarnings("unused")
public class EPMapOnInvitationExtension {

	public EPMapOnInvitationExtension() {

		NewControllerFactory.getInstance().addContextEntryControllerCreator("MapInvitation", new ContextEntryControllerCreator() {

			@Override
			public Controller createController(final ContextEntry ce, final UserRequest ureq, final WindowControl wControl) {
				final Long mapKey = ce.getOLATResourceable().getResourceableId();
				final EPFrontendManager ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
				final PortfolioStructureMap map = (PortfolioStructureMap) ePFMgr.loadPortfolioStructureByKey(mapKey);
				final EPSecurityCallback secCallback = new EPSecurityCallbackImpl(false, true);
				final Controller epCtr = EPUIFactory.createMapViewController(ureq, wControl, map, secCallback);

				final LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(ureq, wControl, null, null, epCtr.getInitialComponent(), null);
				layoutCtr.addDisposableChildController(epCtr);
				return layoutCtr;
			}

			@Override
			public String getTabName(final ContextEntry ce) {
				final Long mapKey = ce.getOLATResourceable().getResourceableId();
				final EPFrontendManager ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
				final PortfolioStructureMap map = (PortfolioStructureMap) ePFMgr.loadPortfolioStructureByKey(mapKey);
				return map.getTitle();
			}

			@Override
			public String getSiteClassName(final ContextEntry ce) {
				return null;
			}

			@Override
			public boolean validateContextEntryAndShowError(final ContextEntry ce, final UserRequest ureq, final WindowControl wControl) {
				return true;
			}
		});
	}
}
