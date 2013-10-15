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
 * Copyright (c) 2008 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.user.notification;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.olat.core.gui.translator.Translator;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.core.util.notifications.NotificationHelper;
import org.olat.core.util.notifications.NotificationsHandler;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.Publisher;
import org.olat.core.util.notifications.Subscriber;
import org.olat.core.util.notifications.SubscriptionInfo;
import org.olat.core.util.notifications.items.SubscriptionListItem;
import org.olat.core.util.notifications.items.TitleItem;

/**
 * Description:<br>
 * This is an implementation of the NotificationsHandler for newly created users.
 * <P>
 * Initial Date: 18 august 2009 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
public class NewUsersNotificationHandler implements NotificationsHandler {
	private static final OLog log = Tracing.createLoggerFor(NewUsersNotificationHandler.class);

	private List<Identity> identities;

	@Override
	public SubscriptionInfo createSubscriptionInfo(final Subscriber subscriber, final Locale locale, final Date compareDate) {
		final Publisher p = subscriber.getPublisher();
		final Date latestNews = p.getLatestNewsDate();

		SubscriptionInfo si;
		Translator translator = Util.createPackageTranslator(this.getClass(), locale);
		// there could be news for me, investigate deeper
		try {
			if (NotificationsManager.getInstance().isPublisherValid(p) && compareDate.before(latestNews)) {
				identities = UsersSubscriptionManager.getInstance().getNewIdentityCreated(compareDate);
				if (identities.isEmpty()) {
					si = NotificationsManager.getInstance().getNoSubscriptionInfo();
				} else {
					translator = Util.createPackageTranslator(this.getClass(), locale);
					si = new SubscriptionInfo(new TitleItem(getItemTitle(translator), CSSHelper.CSS_CLASS_GROUP), null);
					SubscriptionListItem subListItem;
					for (final Identity newUser : identities) {
						final String desc = translator.translate("notifications.entry", new String[] { NotificationHelper.getFormatedName(newUser) });
						final String businessPath = "[Identity:" + newUser.getKey() + "]";
						final String urlToSend = NotificationHelper.getURLFromBusinessPathString(p, businessPath);
						final Date modDate = newUser.getCreationDate();
						subListItem = new SubscriptionListItem(desc, urlToSend, modDate, CSSHelper.CSS_CLASS_USER);
						si.addSubscriptionListItem(subListItem);
					}
				}
			} else {
				si = NotificationsManager.getInstance().getNoSubscriptionInfo();
			}
		} catch (final Exception e) {
			log.error("Error creating new identity's notifications for subscriber: " + subscriber.getKey(), e);
			si = NotificationsManager.getInstance().getNoSubscriptionInfo();
		}
		return si;
	}

	private String getItemTitle(final Translator translator) {
		final String numOfNewUsers = Integer.toString(identities.size());
		if (identities.size() > 1) { return translator.translate("notifications.title", new String[] { numOfNewUsers }); }
		return translator.translate("notifications.titleOne");
	}

	@Override
	public String createTitleInfo(final Subscriber subscriber, final Locale locale) {
		final Translator translator = Util.createPackageTranslator(this.getClass(), locale);
		return translator.translate("notifications.table.title");
	}

	@Override
	public String getType() {
		return "User";
	}
}
