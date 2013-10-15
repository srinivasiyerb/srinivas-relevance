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

package org.olat.course.nodes.ta;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.modules.bc.FileInfo;
import org.olat.core.commons.modules.bc.FolderManager;
import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.logging.LogDelegator;
import org.olat.core.logging.OLog;
import org.olat.core.util.Util;
import org.olat.core.util.notifications.ContextualSubscriptionController;
import org.olat.core.util.notifications.NotificationHelper;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.Publisher;
import org.olat.core.util.notifications.PublisherData;
import org.olat.core.util.notifications.Subscriber;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.notifications.SubscriptionInfo;
import org.olat.core.util.notifications.items.SubscriptionListItem;
import org.olat.core.util.notifications.items.TitleItem;
import org.olat.core.util.resource.OresHelper;
import org.olat.notifications.NotificationsUpgradeHelper;
import org.olat.repository.RepositoryManager;

/**
 * Basic abstract notification-handler for all task-notification-handler.
 * 
 * @author guretzki
 */
public abstract class AbstractTaskNotificationHandler extends LogDelegator {

	/**
	 * @see org.olat.notifications.NotificationsHandler#createSubscriptionInfo(org.olat.notifications.Subscriber, java.util.Locale, java.util.Date)
	 */
	public SubscriptionInfo createSubscriptionInfo(final Subscriber subscriber, final Locale locale, final Date compareDate) {
		final Publisher p = subscriber.getPublisher();
		final Date latestNews = p.getLatestNewsDate();

		SubscriptionInfo si;

		// there could be news for me, investigate deeper
		try {
			if (NotificationsManager.getInstance().isPublisherValid(p) && compareDate.before(latestNews)) {
				final String folderRoot = p.getData();
				if (isLogDebugEnabled()) {
					logDebug("folderRoot=", folderRoot);
				}
				final List<FileInfo> fInfos = FolderManager.getFileInfos(folderRoot, compareDate);
				final Translator translator = Util.createPackageTranslator(AbstractTaskNotificationHandler.class, locale);
				final String displayName = RepositoryManager.getInstance().lookupDisplayNameByOLATResourceableId(p.getResId());
				if (displayName == null) {
					if (!checkPublisher(p)) { return NotificationsManager.getInstance().getNoSubscriptionInfo(); }
				}

				si = new SubscriptionInfo(new TitleItem(translator.translate(getNotificationHeaderKey(), new String[] { displayName }), getCssClassIcon()), null);
				SubscriptionListItem subListItem;
				for (final Iterator<FileInfo> it_infos = fInfos.iterator(); it_infos.hasNext();) {
					final FileInfo fi = it_infos.next();
					final MetaInfo metaInfo = fi.getMetaInfo();
					final String filePath = fi.getRelPath();
					if (isLogDebugEnabled()) {
						logDebug("filePath=", filePath);
					}
					final String fullUserName = getUserNameFromFilePath(filePath);

					final Date modDate = fi.getLastModified();
					final String desc = translator.translate(getNotificationEntryKey(), new String[] { filePath, fullUserName });
					final String urlToSend = NotificationHelper.getURLFromBusinessPathString(p, p.getBusinessPath());

					String iconCssClass = null;
					if (metaInfo != null) {
						iconCssClass = metaInfo.getIconCssClass();
					}
					subListItem = new SubscriptionListItem(desc, urlToSend, modDate, iconCssClass);
					si.addSubscriptionListItem(subListItem);
				}
			} else {
				si = NotificationsManager.getInstance().getNoSubscriptionInfo();
			}
		} catch (final Exception e) {
			getLogger().error("Error creating task notifications for subscriber: " + subscriber.getKey(), e);
			checkPublisher(p);
			si = NotificationsManager.getInstance().getNoSubscriptionInfo();
		}
		return si;
	}

	/**
	 * Extract username form file-path and return the firstname and lastname.
	 * 
	 * @param filePath E.g. '/username/abgabe.txt'
	 * @return 'firstname lastname'
	 */
	protected String getUserNameFromFilePath(final String filePath) {
		// remove first '/'
		try {
			final String path = filePath.substring(1);
			if (path.indexOf("/") != -1) {
				final String userName = path.substring(0, path.indexOf("/"));
				final Identity identity = BaseSecurityManager.getInstance().findIdentityByName(userName);
				final String fullName = NotificationHelper.getFormatedName(identity);
				return fullName;
			} else {
				return "";
			}
		} catch (final Exception e) {
			logWarn("Can not extract user from path=" + filePath, null);
			return "";
		}
	}

	public AbstractTaskNotificationHandler() {
		super();
	}

	private boolean checkPublisher(final Publisher p) {
		try {
			if ("CourseModule".equals(p.getResName())) {
				if (!NotificationsUpgradeHelper.checkCourse(p)) {
					getLogger().info("deactivating publisher with key; " + p.getKey(), null);
					NotificationsManager.getInstance().deactivate(p);
					return false;
				}
			}
		} catch (final Exception e) {
			getLogger().error("Could not check Publisher", e);
		}
		return true;
	}

	public String createTitleInfo(final Subscriber subscriber, final Locale locale) {
		try {
			final Translator translator = Util.createPackageTranslator(AbstractTaskNotificationHandler.class, locale);
			final Long resId = subscriber.getPublisher().getResId();
			final String displayName = RepositoryManager.getInstance().lookupDisplayNameByOLATResourceableId(resId);
			return translator.translate(getNotificationHeaderKey(), new String[] { displayName });
		} catch (final Exception e) {
			getLogger().error("Error while creating task notifications for subscriber: " + subscriber.getKey(), e);
			checkPublisher(subscriber.getPublisher());
			return "-";
		}
	}

	public static ContextualSubscriptionController createContextualSubscriptionController(final UserRequest ureq, final WindowControl wControl, final String folderPath,
			final SubscriptionContext subsContext, final Class callerClass) {
		final String businessPath = wControl.getBusinessControl().getAsString();
		final PublisherData pdata = new PublisherData(OresHelper.calculateTypeName(callerClass), folderPath, businessPath);
		final ContextualSubscriptionController contextualSubscriptionCtr = new ContextualSubscriptionController(ureq, wControl, subsContext, pdata);
		return contextualSubscriptionCtr;
	}

	// Abstract methods
	// //////////////////
	abstract protected String getCssClassIcon();

	abstract protected String getNotificationHeaderKey();

	abstract protected String getNotificationEntryKey();

	@Override
	abstract protected OLog getLogger();
}