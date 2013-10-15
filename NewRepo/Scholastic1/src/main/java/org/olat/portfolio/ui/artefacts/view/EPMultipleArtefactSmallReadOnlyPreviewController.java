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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.portfolio.EPArtefactHandler;
import org.olat.portfolio.EPSecurityCallback;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.structel.PortfolioStructure;

/**
 * Description:<br>
 * show minimal set of artefact details in small preview controllers. if an artefact handler provides a special preview, use this instead the generic artefact-view used
 * inside maps.
 * <P>
 * Initial Date: 17.11.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPMultipleArtefactSmallReadOnlyPreviewController extends BasicController implements EPMultiArtefactsController {

	private List<AbstractArtefact> artefacts;
	private final PortfolioModule portfolioModule;
	private ArrayList<Controller> artefactCtrls;
	private final VelocityContainer vC;
	private final PortfolioStructure struct;
	private final EPSecurityCallback secCallback;

	public EPMultipleArtefactSmallReadOnlyPreviewController(final UserRequest ureq, final WindowControl wControl, final List<AbstractArtefact> artefacts,
			final PortfolioStructure struct, final EPSecurityCallback secCallback) {
		super(ureq, wControl);
		this.artefacts = artefacts;
		this.struct = struct;
		this.secCallback = secCallback;
		vC = createVelocityContainer("smallMultiArtefactPreview");
		portfolioModule = (PortfolioModule) CoreSpringFactory.getBean("portfolioModule");

		init(ureq);
		putInitialPanel(vC);
	}

	private void init(final UserRequest ureq) {
		if (artefactCtrls != null) {
			disposeArtefactControllers();
		}
		artefactCtrls = new ArrayList<Controller>();
		final List<List<Panel>> artefactCtrlCompLines = new ArrayList<List<Panel>>();
		List<Panel> artefactCtrlCompLine = new ArrayList<Panel>();
		int i = 1;
		for (final AbstractArtefact artefact : artefacts) {
			final EPArtefactHandler<?> artHandler = portfolioModule.getArtefactHandler(artefact.getResourceableTypeName());
			Controller artCtrl;
			// check for special art-display:
			final boolean special = artHandler.isProvidingSpecialMapViewController();
			if (special) {
				artCtrl = artHandler.getSpecialMapViewController(ureq, getWindowControl(), artefact);
			} else {
				artCtrl = new EPArtefactViewReadOnlyController(ureq, getWindowControl(), artefact, secCallback, struct);
			}
			if (artCtrl != null) {
				artefactCtrls.add(artCtrl);
				final Component artefactCtrlComponent = artCtrl.getInitialComponent();
				listenTo(artCtrl);

				final Panel namedPanel = new Panel("artCtrl" + i);
				namedPanel.setContent(artefactCtrlComponent);

				if (special) {
					if (!artefactCtrlCompLine.isEmpty()) {
						artefactCtrlCompLines.add(artefactCtrlCompLine);
					}
					artefactCtrlCompLines.add(Collections.singletonList(namedPanel));
					artefactCtrlCompLine = new ArrayList<Panel>();
				} else {
					if (artefactCtrlCompLine.size() == 3) {
						if (!artefactCtrlCompLine.isEmpty()) {
							artefactCtrlCompLines.add(artefactCtrlCompLine);
						}
						artefactCtrlCompLine = new ArrayList<Panel>();
					}
					artefactCtrlCompLine.add(namedPanel);
				}
				vC.put("artCtrl" + i, namedPanel);
				if (special) {// need a flag in a lopp for the velociy template
					vC.put("specialartCtrl" + i, artefactCtrlComponent);
				}
				i++;
			}
		}
		if (!artefactCtrlCompLine.isEmpty()) {
			artefactCtrlCompLines.add(artefactCtrlCompLine);
		}

		vC.contextPut("artefactCtrlCompLines", artefactCtrlCompLines);
	}

	private void disposeArtefactControllers() {
		if (artefactCtrls != null) {
			for (Controller artefactCtrl : artefactCtrls) {
				removeAsListenerAndDispose(artefactCtrl);
				artefactCtrl = null;
			}
			artefactCtrls = null;
		}
	}

	/**
	 * @see org.olat.portfolio.ui.artefacts.view.EPMultiArtefactsController#setNewArtefactsList(org.olat.core.gui.UserRequest, java.util.List)
	 */
	@Override
	public void setNewArtefactsList(final UserRequest ureq, final List<AbstractArtefact> artefacts) {
		this.artefacts = artefacts;
		init(ureq);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@SuppressWarnings("unused")
	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		// no events to handle yet
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		super.event(ureq, source, event);
		fireEvent(ureq, event); // pass to others
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		disposeArtefactControllers();
	}

}
