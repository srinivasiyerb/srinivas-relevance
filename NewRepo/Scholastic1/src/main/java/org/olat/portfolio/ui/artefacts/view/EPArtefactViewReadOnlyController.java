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
package org.olat.portfolio.ui.artefacts.view;

import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalWindowWrapperController;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.filter.FilterFactory;
import org.olat.portfolio.EPSecurityCallback;
import org.olat.portfolio.EPUIFactory;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.structel.PortfolioStructure;
import org.olat.portfolio.ui.structel.EPStructureChangeEvent;

/**
 * Description:<br>
 * simple artefact read-only controller
 * <P>
 * Initial Date: 17.11.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPArtefactViewReadOnlyController extends BasicController {

	private final VelocityContainer vC;
	private final EPFrontendManager ePFMgr;
	private Link detailsLink;
	private final AbstractArtefact artefact;
	private PortfolioStructure struct;
	private final EPSecurityCallback secCallback;
	private Link unlinkLink;

	protected EPArtefactViewReadOnlyController(final UserRequest ureq, final WindowControl wControl, final AbstractArtefact artefact,
			final EPSecurityCallback secCallback, final PortfolioStructure struct) {
		super(ureq, wControl);
		this.artefact = artefact;
		this.struct = struct;
		this.secCallback = secCallback;
		vC = createVelocityContainer("smallSingleArtefact");
		vC.contextPut("artefact", artefact);
		final Identity artIdent = artefact.getAuthor();
		final String fullName = artIdent.getUser().getProperty(UserConstants.FIRSTNAME, null) + " " + artIdent.getUser().getProperty(UserConstants.LASTNAME, null);

		String description = FilterFactory.getHtmlTagAndDescapingFilter().filter(artefact.getDescription());
		description = Formatter.truncate(description, 50);
		vC.contextPut("description", description);
		vC.contextPut("authorName", fullName);
		if (secCallback.canView()) {
			detailsLink = LinkFactory.createCustomLink("small.details.link", "open", "small.details.link", Link.LINK, vC, this);
		}
		if (secCallback.canRemoveArtefactFromStruct()) {
			unlinkLink = LinkFactory.createCustomLink("unlink.link", "remove", "table.header.unlink", Link.LINK, vC, this);
		}

		ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		final List<String> tags = ePFMgr.getArtefactTags(artefact);
		vC.contextPut("tags", StringHelper.formatAsCSVString(tags));

		putInitialPanel(vC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@SuppressWarnings("unused")
	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == detailsLink && secCallback.canView()) {
			final String title = translate("view.artefact.header");
			final CloseableModalWindowWrapperController artDetails = EPUIFactory.getAndActivatePopupArtefactController(artefact, ureq, getWindowControl(), title);
			listenTo(artDetails);
		} else if (source == unlinkLink) {
			struct = ePFMgr.removeArtefactFromStructure(artefact, struct);
			fireEvent(ureq, new EPStructureChangeEvent(EPStructureChangeEvent.ADDED, struct)); // refresh ui
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing
	}

}
