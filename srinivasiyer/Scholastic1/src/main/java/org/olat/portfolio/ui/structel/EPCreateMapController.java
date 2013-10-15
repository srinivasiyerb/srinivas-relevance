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

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.portfolio.EPLoggingAction;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.structel.PortfolioStructureMap;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description:<br>
 * Small controller to create a new default map and to fire event afterwards
 * <P>
 * Initial Date: 03.08.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPCreateMapController extends FormBasicController {

	private TextElement titleEl;
	private RichTextElement descEl;
	private final EPFrontendManager ePFMgr;

	public EPCreateMapController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@SuppressWarnings("unused")
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		final String title = "";
		titleEl = uifactory.addTextElement("map.title", "map.title", 512, title, formLayout);
		titleEl.setNotEmptyCheck("map.title.not.empty");
		titleEl.setMandatory(true);

		final String description = "";
		descEl = uifactory.addRichTextElementForStringDataMinimalistic("map.description", "map.description", description, 7, -1, false, formLayout,
				ureq.getUserSession(), getWindowControl());
		descEl.setNotLongerThanCheck(2047, "map.description.too.long");
		descEl.setExtDelay(true);

		uifactory.addSpacerElement("spacer", formLayout, true);
		uifactory.addFormSubmitButton("save.and.open.map", formLayout);
	}

	/**
	 * Set the fields to empty
	 */
	public void reset() {
		titleEl.setValue("");
		descEl.setValue("");
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(final UserRequest ureq) {
		final String mapTitle = titleEl.getValue();
		final String mapDesc = descEl.getValue();

		PortfolioStructureMap resMap = ePFMgr.createAndPersistPortfolioDefaultMap(getIdentity(), mapTitle, mapDesc);
		// add a page, as each map should have at least one per default!
		final String title = translate("new.page.title");
		final String description = translate("new.page.desc");
		ePFMgr.createAndPersistPortfolioPage(resMap, title, description);
		resMap = (PortfolioStructureMap) ePFMgr.loadPortfolioStructureByKey(resMap.getKey()); // refresh to get all elements with db-keys
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapPortfolioOres(resMap));
		ThreadLocalUserActivityLogger.log(EPLoggingAction.EPORTFOLIO_MAP_CREATED, getClass());
		fireEvent(ureq, new EPMapCreatedEvent(resMap));
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing
	}

}
