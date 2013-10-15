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
 * Copyright (c) 2009 frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.collaboration;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;

/**
 * Provides a controller for entering an information text for group members using FlexiForms.
 * 
 * @author twuersch
 */
public class NewsFormController extends FormBasicController {

	/**
	 * The rich text element for the information text.
	 */
	private RichTextElement newsInputElement;

	/**
	 * The information text.
	 */
	private final String news;

	/**
	 * Initializes this controller.
	 * 
	 * @param ureq The user request.
	 * @param wControl The window control.
	 * @param news The information text.
	 */
	public NewsFormController(final UserRequest ureq, final WindowControl wControl, final String news) {
		super(ureq, wControl);
		this.news = news;
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// Nothing to dispose.
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formNOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formNOK(final UserRequest ureq) {
		fireEvent(ureq, Event.FAILED_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		setFormTitle("news.content");
		this.newsInputElement = uifactory.addRichTextElementForStringData("news.content", "news.content", this.news, 10, -1, false, false, null, null, formLayout,
				ureq.getUserSession(), getWindowControl());
		this.newsInputElement.setMandatory(true);

		// Create submit button
		uifactory.addFormSubmitButton("submit", formLayout);
	}

	/**
	 * Returns the information text.
	 * 
	 * @return The information text.
	 */
	public String getNewsValue() {
		return this.newsInputElement.getValue();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#validateFormLogic(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected boolean validateFormLogic(final UserRequest ureq) {
		boolean newsOk = true;
		// This field is mandatory, so check whether there's something in it.
		final String newsText = newsInputElement.getValue();
		if (!StringHelper.containsNonWhitespace(newsText) || newsText.length() > 4000) {
			newsOk = false;
			if (newsText.length() > 4000) {
				newsInputElement.setErrorKey("input.toolong", new String[] { "4000" });
			} else {
				newsInputElement.setErrorKey("form.legende.mandatory", new String[] {});
			}
		} else {
			newsInputElement.clearError();
		}

		return newsOk && super.validateFormLogic(ureq);
	}
}
