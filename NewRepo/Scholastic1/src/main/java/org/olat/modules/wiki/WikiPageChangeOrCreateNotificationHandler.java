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

package org.olat.modules.wiki;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.LogDelegator;
import org.olat.core.util.Util;
import org.olat.core.util.notifications.NotificationHelper;
import org.olat.core.util.notifications.NotificationsHandler;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.Publisher;
import org.olat.core.util.notifications.Subscriber;
import org.olat.core.util.notifications.SubscriptionInfo;
import org.olat.core.util.notifications.items.SubscriptionListItem;
import org.olat.core.util.notifications.items.TitleItem;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.WikiCourseNode;
import org.olat.course.nodes.wiki.WikiEditController;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.fileresource.types.WikiResource;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.modules.ModuleConfiguration;
import org.olat.notifications.NotificationsUpgradeHelper;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

/**
 * Description:<br>
 * To inform users whether a page has been recently changed or created user can subscribe a wiki a this class evaluates whether something new is available or not.
 * <P>
 * Initial Date: Jun 26, 2006 <br>
 * 
 * @author guido
 */
public class WikiPageChangeOrCreateNotificationHandler extends LogDelegator implements NotificationsHandler {

	private static final String CSS_CLASS_WIKI_PAGE_CHANGED_ICON = "o_edit_icon";
	protected String businessControlString;

	public WikiPageChangeOrCreateNotificationHandler() {
		//
	}

	/**
	 * @see org.olat.notifications.NotificationsHandler#createSubscriptionInfo(org.olat.notifications.Subscriber, java.util.Locale, java.util.Date)
	 */
	@Override
	public SubscriptionInfo createSubscriptionInfo(final Subscriber subscriber, final Locale locale, final Date compareDate) {
		final Publisher p = subscriber.getPublisher();

		final Date latestNews = p.getLatestNewsDate();
		Long resId = p.getResId();
		SubscriptionInfo si;
		// there could be news for me, investigate deeper
		logDebug("compareDate=" + compareDate + " ; latestNews=" + latestNews, null);
		try {
			if (NotificationsManager.getInstance().isPublisherValid(p) && compareDate.before(latestNews)) {
				OLATResourceable ores = null;
				if (p.getResName().equals(CourseModule.getCourseTypeName())) {
					// resId = CourseResourceableId p.getSubidentifier() = wikiCourseNode.getIdent()
					final ICourse course = CourseFactory.loadCourse(resId);
					final CourseEnvironment cenv = course.getCourseEnvironment();
					final CourseNode courseNode = cenv.getRunStructure().getNode(p.getSubidentifier());
					if (courseNode == null) {
						// OLAT-3356 because removing wikicoursenodes was not propagated to
						// disable subcriptions, we may end up here with a NULL wikicoursenode
						// Best we can do here -> return noSubsInfo and clean up
						NotificationsManager.getInstance().deactivate(p);
						// return nothing available
						return NotificationsManager.getInstance().getNoSubscriptionInfo();
					}
					final ModuleConfiguration config = ((WikiCourseNode) courseNode).getModuleConfiguration();
					final RepositoryEntry re = WikiEditController.getWikiRepoReference(config, true);
					resId = re.getOlatResource().getResourceableId();
					logDebug("resId=" + resId, null);
					ores = OresHelper.createOLATResourceableInstance(WikiResource.TYPE_NAME, resId);
					businessControlString = p.getBusinessPath() + "[path=";
				} else {
					// resName = 'BusinessGroup' or 'FileResource.WIKI'
					logDebug("p.getResName()=" + p.getResName(), null);
					ores = OresHelper.createOLATResourceableInstance(p.getResName(), resId);
					businessControlString = p.getBusinessPath() + "[path=";
				}

				final Wiki wiki = WikiManager.getInstance().getOrLoadWiki(ores);
				final List<WikiPage> pages = wiki.getPagesByDate();
				final Translator translator = Util.createPackageTranslator(WikiPageChangeOrCreateNotificationHandler.class, locale);

				final TitleItem title = getTitleItem(p, translator);
				si = new SubscriptionInfo(title, null);
				SubscriptionListItem subListItem;
				for (final Iterator<WikiPage> it = pages.listIterator(); it.hasNext();) {
					final WikiPage element = it.next();

					// do only show entries newer then the ones already seen
					final Date modDate = new Date(element.getModificationTime());
					logDebug("modDate=" + modDate + " ; compareDate=" + compareDate, null);
					if (modDate.after(compareDate)) {
						if ((element.getPageName().startsWith("O_") || element.getPageName().startsWith(WikiPage.WIKI_MENU_PAGE)) && (element.getModifyAuthor() <= 0)) {
							// theses pages are created sometimes automatically. Check if this is the case
							continue;
						}

						// build Businesscontrol-Path
						final String bControlString = businessControlString + element.getPageName() + "]";
						String urlToSend = null;
						if (p.getBusinessPath() != null) {
							urlToSend = NotificationHelper.getURLFromBusinessPathString(p, bControlString);
						}

						// string[] gets filled into translation key by adding {0...n} to
						// the string
						final Identity ident = BaseSecurityManager.getInstance().loadIdentityByKey(Long.valueOf(element.getModifyAuthor()));
						final String desc = translator
								.translate("notifications.entry", new String[] { element.getPageName(), NotificationHelper.getFormatedName(ident) });
						subListItem = new SubscriptionListItem(desc, urlToSend, modDate, CSS_CLASS_WIKI_PAGE_CHANGED_ICON);
						si.addSubscriptionListItem(subListItem);
					} else {
						// there are no more new pages so we stop here
						break;
					}
				}
			} else {
				// no news
				si = NotificationsManager.getInstance().getNoSubscriptionInfo();
			}
		} catch (final Exception e) {
			logError("Error creating wiki's notifications for subscriber: " + subscriber.getKey(), e);
			checkPublisher(p);
			si = NotificationsManager.getInstance().getNoSubscriptionInfo();
		}
		return si;
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
			} else {
				if (!NotificationsUpgradeHelper.checkOLATResourceable(p)) {
					logInfo("deactivating publisher with key; " + p.getKey(), null);
					NotificationsManager.getInstance().deactivate(p);
				}
			}
		} catch (final Exception e) {
			logError("", e);
		}
	}

	private TitleItem getTitleItem(final Publisher p, final Translator translator) {
		final Long resId = p.getResId();
		final String type = p.getResName();
		String title;
		if ("BusinessGroup".equals(type)) {
			final BusinessGroup bg = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(resId, false);
			title = translator.translate("notifications.header.group", new String[] { bg.getName() });
		} else if (CourseModule.getCourseTypeName().equals(type)) {
			final String displayName = RepositoryManager.getInstance().lookupDisplayNameByOLATResourceableId(resId);
			title = translator.translate("notifications.header.course", new String[] { displayName });
		} else {
			title = translator.translate("notifications.header");
		}

		return new TitleItem(title, Wiki.CSS_CLASS_WIKI_ICON);
	}

	@Override
	public String createTitleInfo(final Subscriber subscriber, final Locale locale) {
		try {
			final Translator translator = Util.createPackageTranslator(WikiPageChangeOrCreateNotificationHandler.class, locale);
			final TitleItem title = getTitleItem(subscriber.getPublisher(), translator);
			return title.getInfoContent("text/plain");
		} catch (final Exception e) {
			logError("Error while creating assessment notifications for subscriber: " + subscriber.getKey(), e);
			checkPublisher(subscriber.getPublisher());
			return "-";
		}
	}

	@Override
	public String getType() {
		return "WikiPage";
	}
}
