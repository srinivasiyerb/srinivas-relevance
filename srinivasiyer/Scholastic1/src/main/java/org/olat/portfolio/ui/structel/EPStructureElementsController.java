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
package org.olat.portfolio.ui.structel;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.portfolio.EPSecurityCallback;
import org.olat.portfolio.EPUIFactory;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.restriction.CollectRestriction;
import org.olat.portfolio.model.structel.PortfolioStructure;
import org.olat.portfolio.ui.artefacts.view.EPMultiArtefactsController;
import org.olat.portfolio.ui.structel.edit.EPCollectRestrictionResultController;

/**
 * Description:<br>
 * displays child structure elements on page or on a map
 * <P>
 * Initial Date: 24.08.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPStructureElementsController extends BasicController {

	private final List<PortfolioStructure> structElements;
	List<Controller> tableCtrls;
	List<Controller> addBtnCtrls;
	private final EPSecurityCallback secCallback;
	private final EPFrontendManager ePFMgr;
	private final boolean parentMapClosed;
	private int maxStructAmount;

	private final VelocityContainer flc;

	public EPStructureElementsController(final UserRequest ureq, final WindowControl wControl, final List<PortfolioStructure> structElements,
			final EPSecurityCallback secCallback, final boolean parentMapClosed) {
		super(ureq, wControl);

		this.structElements = structElements;
		this.secCallback = secCallback;
		this.parentMapClosed = parentMapClosed;
		this.maxStructAmount = 1;

		ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");

		flc = createVelocityContainer("structElements");
		initForm(ureq);
		putInitialPanel(flc);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	protected void initForm(final UserRequest ureq) {
		flc.contextPut("structElements", structElements);
		tableCtrls = new ArrayList<Controller>();
		addBtnCtrls = new ArrayList<Controller>();

		int i = 1;
		removeComponents();
		for (final PortfolioStructure portStruct : structElements) {

			if (secCallback.isRestrictionsEnabled()) {
				final List<CollectRestriction> restrictions = portStruct.getCollectRestrictions();
				if (!restrictions.isEmpty()) {
					final boolean check = ePFMgr.checkCollectRestriction(portStruct);
					final EPCollectRestrictionResultController resultCtrl = new EPCollectRestrictionResultController(ureq, getWindowControl(), portStruct);
					resultCtrl.setMessage(ureq, portStruct.getCollectRestrictions(), check);
					flc.put("checkResults" + i, resultCtrl.getInitialComponent());
					listenTo(resultCtrl);
				}
			}

			// get artefacts for this structure
			final List<AbstractArtefact> artefacts = ePFMgr.getArtefacts(portStruct);
			if (artefacts.size() != 0) {
				final EPMultiArtefactsController artefactCtrl = EPUIFactory.getConfigDependentArtefactsControllerForStructure(ureq, getWindowControl(), artefacts,
						portStruct, secCallback);
				flc.put("artefacts" + i, artefactCtrl.getInitialComponent());
				listenTo(artefactCtrl);
				tableCtrls.add(artefactCtrl);
			}

			if (!parentMapClosed && secCallback.canAddArtefact()) {
				// get an addElement-button for each structure
				final EPAddElementsController addButton = new EPAddElementsController(ureq, getWindowControl(), portStruct);
				listenTo(addButton);
				addButton.setShowLink(EPAddElementsController.ADD_ARTEFACT);
				flc.put("addButton" + i, addButton.getInitialComponent());
				addBtnCtrls.add(addButton);
			}
			i++;
		}
		if (i != maxStructAmount) {
			maxStructAmount = i;
		}
	}

	// remove components which were put before to be able to update flc by initForm
	private void removeComponents() {
		for (int j = 1; j < maxStructAmount; j++) {
			flc.remove(flc.getComponent("artefacts" + j));
			flc.remove(flc.getComponent("addButton" + j));
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		super.event(ureq, source, event);
		if (event instanceof EPStructureChangeEvent && event.getCommand().equals(EPStructureChangeEvent.ADDED)) {
			// update the elements
			final EPStructureChangeEvent changeEvent = (EPStructureChangeEvent) event;
			final PortfolioStructure changedEl = changeEvent.getPortfolioStructure();
			if (changedEl != null) {
				int index = 0;
				for (final PortfolioStructure strucEl : structElements) {
					if (changedEl.getKey().equals(strucEl.getKey())) {
						structElements.set(index, changedEl);
						break;
					}
					index++;
				}
			}

			// something changed
			initForm(ureq);
		}
	}

	@SuppressWarnings("unused")
	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// dispose all in table-ctrls, button-ctrls
		final List<Controller> allCtrls = new ArrayList<Controller>();
		allCtrls.addAll(addBtnCtrls);
		allCtrls.addAll(tableCtrls);
		for (final Controller ctrl : allCtrls) {
			removeAsListenerAndDispose(ctrl);
		}
		addBtnCtrls = null;
		tableCtrls = null;
	}

}
