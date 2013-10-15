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
package org.olat.modules.webFeed.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.modules.webFeed.FeedViewHelper;
import org.olat.modules.webFeed.models.Feed;

/**
 * Controller for displaying the feed-url to the students. When the element is clicked, a warning occurs that the url is private.
 * <P>
 * Initial Date: May 20, 2009 <br>
 * 
 * @author gwassmann
 */
public class DisplayFeedUrlController extends FormBasicController {
	private final Feed feed;
	private final FeedViewHelper helper;
	private TextElement feedUrl;

	boolean userHasBeenNotifiedOfConfidentialityOfUrl = false;

	/**
	 * Constructor
	 * 
	 * @param ureq
	 * @param control
	 * @param feed
	 */
	public DisplayFeedUrlController(final UserRequest ureq, final WindowControl control, final Feed feed, final FeedViewHelper helper, final Translator translator) {
		super(ureq, control, FormBasicController.LAYOUT_VERTICAL);
		this.feed = feed;
		this.helper = helper;
		setTranslator(translator);
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing to do
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formInnerEvent(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.FormItem, org.olat.core.gui.components.form.flexible.impl.FormEvent)
	 */
	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (source == feedUrl && event.wasTriggerdBy(FormEvent.ONCLICK)) {
			if (feed.isInternal() && !userHasBeenNotifiedOfConfidentialityOfUrl) {
				showWarning("feed.url.is.personal.warning");
				userHasBeenNotifiedOfConfidentialityOfUrl = true;
			}
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(final UserRequest ureq) {
		// this is actually not a proper form. don't do anything.
	}

	/**
	 * @see org.olat.modules.webFeed.ui.podcast.FeedFormController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	@SuppressWarnings("unused")
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		feedUrl = uifactory.addTextAreaElement("feedUrl", "feed.url.label", 5000, 1, 1, true, helper.getFeedUrl(), this.flc);
		// no editing. selecting allowed only
		feedUrl.setEnabled(false);
		feedUrl.addActionListener(this, FormEvent.ONCLICK);
	}

	/**
	 * Sets the URL to display.
	 * 
	 * @param url
	 */
	public void setUrl(final String url) {
		feedUrl.setValue(url);
	}
}
