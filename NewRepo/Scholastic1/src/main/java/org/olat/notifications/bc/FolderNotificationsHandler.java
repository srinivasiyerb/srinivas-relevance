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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.notifications.bc;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.olat.core.commons.modules.bc.FileInfo;
import org.olat.core.commons.modules.bc.FolderManager;
import org.olat.core.commons.modules.bc.meta.MetaInfo;
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
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.notifications.NotificationsUpgradeHelper;
import org.olat.repository.RepositoryManager;

/**
 * Description: <br>
 * create SubscriptionInfo for a folder.
 * <P>
 * Initial Date: 25.10.2004 <br>
 * 
 * @author Felix Jost
 */
public class FolderNotificationsHandler implements NotificationsHandler {
	private static final OLog log = Tracing.createLoggerFor(FolderNotificationsHandler.class);

	/**
	 * 
	 */
	public FolderNotificationsHandler() {
		//
	}

	/**
	 * @see org.olat.notifications.NotificationsHandler#createSubscriptionInfo(org.olat.notifications.Subscriber, java.util.Locale, java.util.Date)
	 */
	@Override
	public SubscriptionInfo createSubscriptionInfo(final Subscriber subscriber, final Locale locale, final Date compareDate) {
		final Publisher p = subscriber.getPublisher();
		final Date latestNews = p.getLatestNewsDate();

		final String businessPath = p.getBusinessPath() + "[path=";

		SubscriptionInfo si;
		// there could be news for me, investigate deeper
		try {
			if (NotificationsManager.getInstance().isPublisherValid(p) && compareDate.before(latestNews)) {
				final String folderRoot = p.getData();
				final List<FileInfo> fInfos = FolderManager.getFileInfos(folderRoot, compareDate);
				final Translator translator = Util.createPackageTranslator(FolderNotificationsHandler.class, locale);

				si = new SubscriptionInfo(getTitleItem(p, translator), null);
				SubscriptionListItem subListItem;
				for (final Iterator<FileInfo> it_infos = fInfos.iterator(); it_infos.hasNext();) {
					final FileInfo fi = it_infos.next();
					String title = fi.getRelPath();
					final MetaInfo metaInfo = fi.getMetaInfo();
					String iconCssClass = null;
					if (metaInfo != null) {
						if (metaInfo.getTitle() != null) {
							title += " (" + metaInfo.getTitle() + ")";
						}
						iconCssClass = metaInfo.getIconCssClass();
					}
					final Identity ident = fi.getAuthor();
					final Date modDate = fi.getLastModified();

					final String desc = translator.translate("notifications.entry", new String[] { title, NotificationHelper.getFormatedName(ident) });
					String urlToSend = null;
					if (p.getBusinessPath() != null) {
						urlToSend = NotificationHelper.getURLFromBusinessPathString(p, businessPath + fi.getRelPath() + "]");
					}
					subListItem = new SubscriptionListItem(desc, urlToSend, modDate, iconCssClass);
					si.addSubscriptionListItem(subListItem);
				}
			} else {
				si = NotificationsManager.getInstance().getNoSubscriptionInfo();
			}
		} catch (final Exception e) {
			log.error("Error creating folder's notifications for subscriber: " + subscriber.getKey(), e);
			checkPublisher(subscriber.getPublisher());
			si = NotificationsManager.getInstance().getNoSubscriptionInfo();
		}
		return si;
	}

	private void checkPublisher(final Publisher p) {
		try {
			if ("BusinessGroup".equals(p.getResName())) {
				final BusinessGroup bg = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(p.getResId(), false);
				if (bg == null) {
					log.info("deactivating publisher with key; " + p.getKey(), null);
					NotificationsManager.getInstance().deactivate(p);
				}
			} else if ("CourseModule".equals(p.getResName())) {
				if (!NotificationsUpgradeHelper.checkCourse(p)) {
					log.info("deactivating publisher with key; " + p.getKey(), null);
					NotificationsManager.getInstance().deactivate(p);
				}
			}
		} catch (final Exception e) {
			log.error("", e);
		}
	}

	private TitleItem getTitleItem(final Publisher p, final Translator translator) {
		String title;
		try {
			final String resName = p.getResName();
			if ("BusinessGroup".equals(resName)) {
				final BusinessGroup bg = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(p.getResId(), false);
				title = translator.translate("notifications.header.group", new String[] { bg.getName() });
			} else if ("CourseModule".equals(resName)) {
				final String displayName = RepositoryManager.getInstance().lookupDisplayNameByOLATResourceableId(p.getResId());
				title = translator.translate("notifications.header.course", new String[] { displayName });
			} else {
				title = translator.translate("notifications.header");
			}
		} catch (final Exception e) {
			log.error("", e);
			checkPublisher(p);
			title = translator.translate("notifications.header");
		}
		return new TitleItem(title, CSSHelper.CSS_CLASS_FILETYPE_FOLDER);
	}

	@Override
	public String createTitleInfo(final Subscriber subscriber, final Locale locale) {
		final Translator translator = Util.createPackageTranslator(FolderNotificationsHandler.class, locale);
		final TitleItem title = getTitleItem(subscriber.getPublisher(), translator);
		return title.getInfoContent("text/plain");
	}

	@Override
	public String getType() {
		return "FolderModule";
	}
}