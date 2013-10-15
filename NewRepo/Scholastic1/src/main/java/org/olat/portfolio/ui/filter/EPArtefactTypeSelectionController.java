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
package org.olat.portfolio.ui.filter;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.portfolio.EPArtefactHandler;
import org.olat.portfolio.PortfolioModule;

/**
 * Description:<br>
 * edit artefact type filter
 * <P>
 * Initial Date: 19.07.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPArtefactTypeSelectionController extends FormBasicController {

	private final PortfolioModule portfolioModule;
	private List<String> selectedTypeList;
	private ArrayList<MultipleSelectionElement> typeCmpList;

	public EPArtefactTypeSelectionController(final UserRequest ureq, final WindowControl wControl, final List<String> selectedTypeList) {
		super(ureq, wControl);

		portfolioModule = (PortfolioModule) CoreSpringFactory.getBean("portfolioModule");
		this.selectedTypeList = selectedTypeList;
		initForm(ureq);
	}

	@SuppressWarnings("unused")
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		setFormDescription("filter.type.intro");

		final String[] keys = new String[] { "onoff" };
		final String[] values = new String[] { translate("filter.type.enabled") };

		final List<EPArtefactHandler<?>> handlers = portfolioModule.getAllAvailableArtefactHandlers();
		typeCmpList = new ArrayList<MultipleSelectionElement>();
		for (final EPArtefactHandler<?> handler : handlers) {
			final Translator handlerTrans = handler.getHandlerTranslator(getTranslator());
			this.flc.setTranslator(handlerTrans);
			final String handlerClass = PortfolioFilterController.HANDLER_PREFIX + handler.getClass().getSimpleName() + PortfolioFilterController.HANDLER_TITLE_SUFFIX;
			final MultipleSelectionElement chkBox = uifactory.addCheckboxesHorizontal(handlerClass, formLayout, keys, values, null);
			if (selectedTypeList != null && selectedTypeList.contains(handler.getType())) {
				chkBox.select(keys[0], true);
			}
			chkBox.addActionListener(this, FormEvent.ONCHANGE);
			chkBox.setUserObject(handler.getType());
			typeCmpList.add(chkBox);
		}
		uifactory.addFormSubmitButton("filter.type.submit", formLayout);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formInnerEvent(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.FormItem, org.olat.core.gui.components.form.flexible.impl.FormEvent) fire change events on every click in form and
	 *      update gui
	 */
	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		super.formInnerEvent(ureq, source, event);
		updateSelectedTypeList();
		fireEvent(ureq, Event.CHANGED_EVENT);
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	protected void updateSelectedTypeList() {
		if (selectedTypeList == null) {
			selectedTypeList = new ArrayList<String>();
		}
		for (final MultipleSelectionElement typeCmp : typeCmpList) {
			final String selType = (String) typeCmp.getUserObject();
			if (typeCmp.isSelected(0) && !selectedTypeList.contains(selType)) {
				selectedTypeList.add(selType);
			}
			if (!typeCmp.isSelected(0) && selectedTypeList.contains(selType)) {
				selectedTypeList.remove(selType);
			}
		}
		if (selectedTypeList.size() == 0) {
			selectedTypeList = null;
		}
	}

	@Override
	protected void doDispose() {
		// nothing
	}

}
