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
package org.olat.notifications;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable;
import org.olat.core.id.Identity;
import org.olat.core.util.StringHelper;
import org.olat.core.util.notifications.NotificationHelper;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.Subscriber;
import org.olat.core.util.notifications.SubscriptionInfo;
import org.olat.core.util.notifications.SubscriptionItem;

/**
 * Description:<br>
 * This controller shows the list of the news generated by the users subscriptions. The news interval can be changed by the user by setting an appropriate date. To manage
 * the users subscription the NotificationSubscriptionController can be used.
 * <P>
 * Initial Date: 22.12.2009 <br>
 * 
 * @author gnaegi
 */
class NotificationNewsController extends BasicController implements Activateable {
	private final VelocityContainer newsVC;
	private Date compareDate;
	private String newsType;
	private final Identity subscriberIdentity;
	private final DateChooserController dateChooserCtr;
	private final Link emailLink;
	private Map<Subscriber, SubscriptionInfo> subsInfoMap;

	/**
	 * Constructor
	 * 
	 * @param subscriberIdentity The identity which news are displayed
	 * @param ureq
	 * @param wControl
	 * @param newsSinceDate The lower date boundary to collect the news or NULL to use the user defined notification interval
	 */
	NotificationNewsController(final Identity subscriberIdentity, final UserRequest ureq, final WindowControl wControl, final Date newsSinceDate) {
		super(ureq, wControl);
		this.subscriberIdentity = subscriberIdentity;
		if (newsSinceDate == null) {
			final NotificationsManager man = NotificationsManager.getInstance();
			compareDate = man.getCompareDateFromInterval(man.getUserIntervalOrDefault(ureq.getIdentity()));
		} else {
			compareDate = newsSinceDate;
		}
		// Main view is a velocity container
		newsVC = createVelocityContainer("notificationsNews");
		// Fetch data from DB and update datamodel and reuse subscribers
		final List<Subscriber> subs = updateNewsDataModel();
		// Add date and type chooser
		dateChooserCtr = new DateChooserController(ureq, getWindowControl(), new Date());
		dateChooserCtr.setSubscribers(subs);
		listenTo(dateChooserCtr);
		newsVC.put("dateChosserCtr", dateChooserCtr.getInitialComponent());
		// Add email link
		emailLink = LinkFactory.createButton("emailLink", newsVC, this);
		//
		putInitialPanel(newsVC);
	}

	/**
	 * Update the new data model and refresh the GUI
	 */
	List<Subscriber> updateNewsDataModel() {
		final NotificationsManager man = NotificationsManager.getInstance();
		final List<Subscriber> subs = man.getSubscribers(subscriberIdentity);
		if (StringHelper.containsNonWhitespace(newsType)) {
			for (final Iterator<Subscriber> it = subs.iterator(); it.hasNext();) {
				if (!newsType.equals(it.next().getPublisher().getType())) {
					it.remove();
				}
			}
		}

		newsVC.contextPut("subs", subs);
		subsInfoMap = NotificationHelper.getSubscriptionMap(getIdentity(), getLocale(), true, compareDate);
		final NotificationSubscriptionAndNewsFormatter subsFormatter = new NotificationSubscriptionAndNewsFormatter(compareDate, getTranslator(), subsInfoMap);
		newsVC.contextPut("subsFormatter", subsFormatter);
		return subs;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == dateChooserCtr) {
			if (event == Event.CHANGED_EVENT) {
				compareDate = dateChooserCtr.getChoosenDate();
				newsType = dateChooserCtr.getType();
				updateNewsDataModel();
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == emailLink) {
			// send email to user with the currently visible date
			final NotificationsManager man = NotificationsManager.getInstance();
			final List<SubscriptionItem> infoList = new ArrayList<SubscriptionItem>();
			final List<Subscriber> subsList = new ArrayList<Subscriber>();
			for (final Subscriber subscriber : subsInfoMap.keySet()) {
				subsList.add(subscriber);
				final SubscriptionItem item = man.createSubscriptionItem(subscriber, getLocale(), SubscriptionInfo.MIME_PLAIN, SubscriptionInfo.MIME_PLAIN, compareDate);
				if (item != null) {
					infoList.add(item);
				}
			}
			if (man.sendMailToUserAndUpdateSubscriber(subscriberIdentity, infoList, getTranslator(), subsList)) {
				showInfo("email.ok");
			} else {
				showError("email.nok");
			}
		}
	}

	/**
	 * Can set type and date of the controller: identifier in the ofrm of [type=Type:0][date=yyyymmdd:0]
	 * 
	 * @see org.olat.core.gui.control.generic.dtabs.Activateable#activate(org.olat.core.gui.UserRequest, java.lang.String)
	 */
	@Override
	public void activate(final UserRequest ureq, final String viewIdentifier) {
		if (viewIdentifier == null) { return; }

		boolean changed = false;
		for (final StringTokenizer tokenizer = new StringTokenizer(viewIdentifier, "[]"); tokenizer.hasMoreTokens();) {
			final String token = tokenizer.nextToken();
			if (token.startsWith("type=")) {
				newsType = extractValue("type=", token);
				dateChooserCtr.setType(newsType);
				changed = true;
			} else if (token.startsWith("date=")) {
				try {
					final String date = extractValue("date=", token);
					final DateFormat format = new SimpleDateFormat("yyyyMMdd");
					compareDate = format.parse(date);
					dateChooserCtr.setDate(compareDate);
					changed = true;
				} catch (final ParseException e) {
					e.printStackTrace();
				}
			}
		}

		if (changed) {
			updateNewsDataModel();
		}
	}

	private String extractValue(final String str, final String identifier) {
		if (identifier.startsWith(str)) {
			final int sepIndex = identifier.indexOf(':');
			final int lastIndex = (sepIndex > 0 ? sepIndex : identifier.length());
			final String value = identifier.substring(str.length(), lastIndex);
			return value;
		}
		return null;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// child controllers disposed by basic controller
	}
}
