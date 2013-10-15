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

package org.olat.modules.dialog;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.olat.basesecurity.BaseSecurityManager;
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
import org.olat.notifications.NotificationsUpgradeHelper;
import org.olat.repository.RepositoryManager;

/**
 * Description:<br>
 * Notification handler for course node dialog. Subscribers get informed about new uploaded file in the dialog table.
 * <P>
 * Initial Date: 23.11.2005 <br>
 * 
 * @author guido
 */
public class FileUploadNotificationHandler implements NotificationsHandler {
	private static final OLog log = Tracing.createLoggerFor(FileUploadNotificationHandler.class);
	private static final String CSSS_CLASS_UPLOAD_ICON = "o_dialog_icon";

	public FileUploadNotificationHandler() {
		//
	}

	/**
	 * @see org.olat.notifications.NotificationsHandler#createSubscriptionInfo(org.olat.notifications.Subscriber, java.util.Locale, java.util.Date)
	 */
	@Override
	public SubscriptionInfo createSubscriptionInfo(final Subscriber subscriber, final Locale locale, final Date compareDate) {
		final Publisher p = subscriber.getPublisher();
		final Date latestNews = p.getLatestNewsDate();

		SubscriptionInfo si;
		// there could be news for me, investigate deeper
		try {
			if (NotificationsManager.getInstance().isPublisherValid(p) && compareDate.before(latestNews)) {
				final String displayname = RepositoryManager.getInstance().lookupDisplayNameByOLATResourceableId(p.getResId());
				if (displayname == null) {
					if (!checkPublisher(subscriber.getPublisher())) { return NotificationsManager.getInstance().getNoSubscriptionInfo(); }
				}
				final DialogElementsPropertyManager mgr = DialogElementsPropertyManager.getInstance();
				final DialogPropertyElements elements = mgr.findDialogElements(p.getResId(), p.getSubidentifier());
				final List<DialogElement> dialogElements = elements.getDialogPropertyElements();
				final Translator translator = Util.createPackageTranslator(FileUploadNotificationHandler.class, locale);

				si = new SubscriptionInfo(new TitleItem(translator.translate("notifications.header", new String[] { displayname }), CSSS_CLASS_UPLOAD_ICON), null);
				SubscriptionListItem subListItem;
				for (final DialogElement element : dialogElements) {
					// do only show entries newer then the ones already seen
					if (element.getDate().after(compareDate)) {
						final String filename = element.getFilename();
						final String creator = element.getAuthor();
						final Identity ident = BaseSecurityManager.getInstance().findIdentityByName(creator);
						final Date modDate = element.getDate();

						final String desc = translator.translate("notifications.entry", new String[] { filename, NotificationHelper.getFormatedName(ident) });
						final String urlToSend = NotificationHelper.getURLFromBusinessPathString(p, p.getBusinessPath());
						final String cssClass = CSSHelper.createFiletypeIconCssClassFor(filename);

						subListItem = new SubscriptionListItem(desc, urlToSend, modDate, cssClass);
						si.addSubscriptionListItem(subListItem);
					}
				}
			} else {
				si = NotificationsManager.getInstance().getNoSubscriptionInfo();
			}
		} catch (final Exception e) {
			log.error("Error creating file upload's notifications for subscriber: " + subscriber.getKey(), e);
			si = NotificationsManager.getInstance().getNoSubscriptionInfo();
		}
		return si;
	}

	@Override
	public String createTitleInfo(final Subscriber subscriber, final Locale locale) {
		try {
			final Translator translator = Util.createPackageTranslator(FileUploadNotificationHandler.class, locale);
			final String displayname = RepositoryManager.getInstance().lookupDisplayNameByOLATResourceableId(subscriber.getPublisher().getResId());
			if (displayname == null) {
				checkPublisher(subscriber.getPublisher());
			}
			return translator.translate("notifications.header", new String[] { displayname });
		} catch (final Exception e) {
			log.error("Error while creating assessment notifications for subscriber: " + subscriber.getKey(), e);
			checkPublisher(subscriber.getPublisher());
			return "-";
		}
	}

	private boolean checkPublisher(final Publisher p) {
		try {
			if ("CourseModule".equals(p.getResName())) {
				if (!NotificationsUpgradeHelper.checkCourse(p)) {
					log.info("deactivating publisher with key; " + p.getKey(), null);
					NotificationsManager.getInstance().deactivate(p);
					return false;
				}
			}
		} catch (final Exception e) {
			log.error("", e);
		}
		return true;
	}

	@Override
	public String getType() {
		return "DialogElement";
	}
}
