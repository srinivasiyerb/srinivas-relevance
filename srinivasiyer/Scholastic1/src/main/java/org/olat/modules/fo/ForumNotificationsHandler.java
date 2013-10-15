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

package org.olat.modules.fo;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.logging.LogDelegator;
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
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.notifications.NotificationsUpgradeHelper;
import org.olat.repository.RepositoryManager;

/**
 * Initial Date: 25.10.2004 <br>
 * 
 * @author Felix Jost
 */
public class ForumNotificationsHandler extends LogDelegator implements NotificationsHandler {
	private static final OLog log = Tracing.createLoggerFor(ForumNotificationsHandler.class);

	public ForumNotificationsHandler() {
		// nothing to do
	}

	/**
	 * @see org.olat.notifications.NotificationsHandler#createSubscriptionInfo(org.olat.notifications.Subscriber, java.util.Locale, java.util.Date)
	 */
	@Override
	public SubscriptionInfo createSubscriptionInfo(final Subscriber subscriber, final Locale locale, final Date compareDate) {
		try {
			final Publisher p = subscriber.getPublisher();
			final Date latestNews = p.getLatestNewsDate();

			SubscriptionInfo si;
			// there could be news for me, investigate deeper
			if (NotificationsManager.getInstance().isPublisherValid(p) && compareDate.before(latestNews)) {
				String businessControlString = "";
				Long forumKey = Long.valueOf(0);
				try {
					forumKey = Long.parseLong(p.getData());
				} catch (final NumberFormatException e) {
					logError("Could not parse forum key!", e);
					NotificationsManager.getInstance().deactivate(p);
					return NotificationsManager.getInstance().getNoSubscriptionInfo();
				}
				final List<Message> mInfos = ForumManager.getInstance().getNewMessageInfo(forumKey, compareDate);
				final Translator translator = Util.createPackageTranslator(ForumNotificationsHandler.class, locale);

				businessControlString = p.getBusinessPath() + "[Message:";

				si = new SubscriptionInfo(getTitleItem(p, translator), null);
				for (final Message mInfo : mInfos) {
					final String title = mInfo.getTitle();
					final Identity creator = mInfo.getCreator();
					final Identity modifier = mInfo.getModifier();
					final Date modDate = mInfo.getLastModified();

					String name;
					if (modifier != null) {
						name = NotificationHelper.getFormatedName(modifier);
					} else {
						name = NotificationHelper.getFormatedName(creator);
					}
					final String desc = translator.translate("notifications.entry", new String[] { title, name });
					String urlToSend = null;
					if (p.getBusinessPath() != null) {
						urlToSend = NotificationHelper.getURLFromBusinessPathString(p, businessControlString + mInfo.getKey().toString() + "]");
					}

					final SubscriptionListItem subListItem = new SubscriptionListItem(desc, urlToSend, modDate, ForumHelper.CSS_ICON_CLASS_MESSAGE);
					si.addSubscriptionListItem(subListItem);
				}
			} else {
				si = NotificationsManager.getInstance().getNoSubscriptionInfo();
			}
			return si;
		} catch (final Exception e) {
			log.error("Error while creating forum's notifications from publisher with key:" + subscriber.getKey(), e);
			checkPublisher(subscriber.getPublisher());
			return NotificationsManager.getInstance().getNoSubscriptionInfo();
		}
	}

	private void checkPublisher(final Publisher p) {
		try {
			if ("BusinessGroup".equals(p.getResName())) {
				final BusinessGroup bg = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(p.getResId(), false);
				if (bg == null) {
					logInfo("deactivating publisher with key; " + p.getKey(), null);
					NotificationsManager.getInstance().deactivate(p);
				}
			} else if ("CourseModule".equals(p.getResName())) {
				if (!NotificationsUpgradeHelper.checkCourse(p)) {
					logInfo("deactivating publisher with key; " + p.getKey(), null);
					NotificationsManager.getInstance().deactivate(p);
				}
			}
		} catch (final Exception e) {
			logError("", e);
		}
	}

	@Override
	public String createTitleInfo(final Subscriber subscriber, final Locale locale) {
		final Translator translator = Util.createPackageTranslator(ForumNotificationsHandler.class, locale);
		final TitleItem title = getTitleItem(subscriber.getPublisher(), translator);
		return title.getInfoContent("text/plain");
	}

	private TitleItem getTitleItem(final Publisher p, final Translator translator) {
		final Long resId = p.getResId();
		final String type = p.getResName();
		String title;
		try {
			if ("BusinessGroup".equals(type)) {
				final BusinessGroup bg = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(resId, false);
				title = translator.translate("notifications.header.group", new String[] { bg.getName() });
			} else if ("CourseModule".equals(type)) {
				final String displayName = RepositoryManager.getInstance().lookupDisplayNameByOLATResourceableId(resId);
				title = translator.translate("notifications.header.course", new String[] { displayName });
			} else {
				title = translator.translate("notifications.header");
			}
		} catch (final Exception e) {
			log.error("Error while creating assessment notifications for publisher: " + p.getKey(), e);
			checkPublisher(p);
			title = translator.translate("notifications.header");
		}
		return new TitleItem(title, ForumHelper.CSS_ICON_CLASS_FORUM);
	}

	@Override
	public String getType() {
		return "Forum";
	}
}
