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
package org.olat.core.commons.modules.glossary;

import java.util.List;

import org.olat.core.commons.modules.glossary.morphService.MorphologicalService;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.VFSContainer;

/**
 * Description:<br>
 * Holds a tabbed pane in a modal window and fires event to glossaryMainController
 * 
 * @author Roman Haag, frentix GmbH, roman.haag@frentix.com
 */
public class GlossaryItemEditorController extends BasicController implements Activateable {

	private TabbedPane glossEditTabP;
	private GlossaryDefinitionController defCtrl;
	private VelocityContainer editorVC;
	private GlossaryTermAndSynonymController itmCtrl;
	private GlossaryFlexionController flexCtrl;

	/**
	 * @param ureq
	 * @param control
	 * @param glossaryItemList
	 * @param glossaryItem to be null, if a new Item should be generated and added to List
	 */
	protected GlossaryItemEditorController(UserRequest ureq, WindowControl control, VFSContainer glossaryFolder, List<GlossaryItem> glossaryItemList,
			GlossaryItem glossaryItem) {
		super(ureq, control);
		editorVC = createVelocityContainer("editor");

		boolean addNewItem = false;
		if (glossaryItem == null) {
			addNewItem = true;
			glossaryItem = new GlossaryItem("", "");
			glossaryItemList.add(glossaryItem);
		}
		editorVC.contextPut("addNewItem", addNewItem);

		glossEditTabP = new TabbedPane("tp", ureq.getLocale());

		itmCtrl = new GlossaryTermAndSynonymController(ureq, control, glossaryItem, glossaryFolder);
		listenTo(itmCtrl);
		glossEditTabP.addTab(translate("term.and.synonyms.title"), itmCtrl.getInitialComponent());

		List<MorphologicalService> morphServices = GlossaryModule.getMorphologicalServices();
		if (morphServices != null && morphServices.size() != 0) {
			flexCtrl = new GlossaryFlexionController(ureq, control, glossaryItem, glossaryFolder);
			listenTo(flexCtrl);
			glossEditTabP.addTab(translate("flexions.title"), flexCtrl.getInitialComponent());
		}

		defCtrl = new GlossaryDefinitionController(ureq, control, glossaryItem, glossaryFolder);
		listenTo(defCtrl);
		glossEditTabP.addTab(translate("definition.title"), defCtrl.getInitialComponent());

		// enable/disable other tabs, if no term is yet set
		enableDisableTermDependentTabs(StringHelper.containsNonWhitespace(glossaryItem.getGlossTerm()));

		glossEditTabP.addListener(this);
		editorVC.put("glossEditTabP", glossEditTabP);
		putInitialPanel(editorVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing to dispose, everything gets cleared because of earlier registering with "listenTo"
	}

	private void enableDisableTermDependentTabs(boolean enDis) {
		glossEditTabP.setEnabled(1, enDis);
		glossEditTabP.setEnabled(2, enDis);
		glossEditTabP.setDirty(true);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		// none
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == itmCtrl) {
			if (event.getCommand().equals("termOK")) {
				enableDisableTermDependentTabs(true);
			} else if (event.getCommand().equals("termNOK")) {
				enableDisableTermDependentTabs(false);
			}
		}
	}

	@Override
	public void activate(UserRequest ureq, String viewIdentifier) {
		// nothing
	}

}
