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

package org.olat.collaboration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.olat.admin.quota.QuotaConstants;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.model.KalendarConfig;
import org.olat.commons.calendar.ui.CalendarController;
import org.olat.commons.calendar.ui.WeeklyCalendarController;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.title.TitleInfo;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Util;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.coordinate.SyncerExecutor;
import org.olat.core.util.mail.ContactMessage;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.run.calendar.CourseLinkProviderController;
import org.olat.group.BusinessGroup;
import org.olat.group.context.BGContextManagerImpl;
import org.olat.group.ui.BGConfigFlags;
import org.olat.instantMessaging.InstantMessagingModule;
import org.olat.instantMessaging.groupchat.GroupChatManagerController;
import org.olat.modules.co.ContactFormController;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.ForumCallback;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.ForumUIFactory;
import org.olat.modules.fo.archiver.ForumArchiveManager;
import org.olat.modules.fo.archiver.formatters.ForumFormatter;
import org.olat.modules.fo.archiver.formatters.ForumRTFFormatter;
import org.olat.modules.wiki.WikiManager;
import org.olat.modules.wiki.WikiSecurityCallback;
import org.olat.modules.wiki.WikiSecurityCallbackImpl;
import org.olat.modules.wiki.WikiToZipUtils;
import org.olat.portfolio.EPSecurityCallback;
import org.olat.portfolio.EPSecurityCallbackImpl;
import org.olat.portfolio.EPUIFactory;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.structel.PortfolioStructureMap;
import org.olat.properties.NarrowedPropertyManager;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;
import org.olat.resource.OLATResource;
import org.olat.testutils.codepoints.server.Codepoint;

/**
 * Description:<BR>
 * The singleton used for retrieving a collaboration tools suite associated with the supplied OLATResourceable.
 * <P>
 * Description: <br>
 * The CollaborationTools represents a suite of collaborative tools addeable to any OLATResourceable. To get an instance of this suite, one has to use the collaboration
 * tools factory.
 * <p>
 * This collaboration tools class exposes the possibility to retrieve the appropriate controllers for the desired tools. And also provides the means to manage the
 * configuration of the provided tools. Moreover it is already shipped with a controller which can be used to display an administrative view for enabling/disabling such
 * tools for the supplied OLATResourceable.
 * <p>
 * All the future collaborative tools will be found here.
 * 
 * @see org.olat.collaboration.CollaborationToolsFactory
 * @author Felix Jost
 * @author guido
 */
public class CollaborationTools implements Serializable {

	boolean dirty = false;
	private final static String TRUE = "true";
	private final static String FALSE = "false";
	public final static String KEY_FORUM = "forumKey";
	public final static String KEY_PORTFOLIO = "portfolioMapKey";

	/**
	 * <code>PROP_CAT_BG_COLLABTOOLS</code> identifies properties concerning Collaboration Tools
	 */
	public final static String PROP_CAT_BG_COLLABTOOLS = "collabtools";
	/**
	 * constant used to identify the calendar for a BuddyGroup
	 */
	public final static String TOOL_CALENDAR = "hasCalendar";
	/**
	 * constant used to identify the forum for a BuddyGroup
	 */
	public final static String TOOL_FORUM = "hasForum";
	/**
	 * constant used to identify the folder for a BuddyGroup
	 */
	public final static String TOOL_FOLDER = "hasFolder";
	/**
	 * constant used to identify the chat for a BuddyGroup
	 */
	public final static String TOOL_CHAT = "hasChat";
	/**
	 * constant used to identify the contact form for a BuddyGroup
	 */
	public final static String TOOL_CONTACT = "hasContactForm";
	/**
	 * constant used to identify the contact form for a BuddyGroup
	 */
	public final static String TOOL_NEWS = "hasNews";
	/**
	 * constant used to identify the wiki for a BuddyGroup
	 */
	public final static String TOOL_WIKI = "hasWiki";

	/**
	 * constant used to identify the portfolio for a BuddyGroup
	 */
	public final static String TOOL_PORTFOLIO = "hasPortfolio";

	/**
	 * public for group test only, do not use otherwise convenience, helps iterating possible tools, i.e. in jUnit testCase, also for building up a tools choice
	 */
	public static String[] TOOLS;

	/**
	 * Only owners have write access to the calendar.
	 */
	public static final int CALENDAR_ACCESS_OWNERS = 0;
	/**
	 * Owners and members have write access to the calendar.
	 */
	public static final int CALENDAR_ACCESS_ALL = 1;

	/**
	 * cache for Boolean Objects representing the State
	 */
	private final static String KEY_NEWS = "news";
	private final static String KEY_CALENDAR_ACCESS = "cal";

	// o_clusterOK by guido
	Hashtable<String, Boolean> cacheToolStates;
	final OLATResourceable ores;

	OLog log = Tracing.createLoggerFor(this.getClass());
	private transient CoordinatorManager coordinatorManager;

	/**
	 * package local constructor only
	 * 
	 * @param ores
	 */
	CollaborationTools(final CoordinatorManager coordinatorManager, final OLATResourceable ores) {
		this.coordinatorManager = coordinatorManager;
		this.ores = ores;
		cacheToolStates = new Hashtable<String, Boolean>();
		if (InstantMessagingModule.isEnabled()) {
			TOOLS = new String[] { TOOL_NEWS, TOOL_CONTACT, TOOL_CALENDAR, TOOL_FOLDER, TOOL_FORUM, TOOL_CHAT, TOOL_WIKI, TOOL_PORTFOLIO };
		} else {
			TOOLS = new String[] { TOOL_NEWS, TOOL_CONTACT, TOOL_CALENDAR, TOOL_FOLDER, TOOL_FORUM, TOOL_WIKI, TOOL_PORTFOLIO };
		}
	}

	/**
	 * @param ureq
	 * @return a news controller
	 */
	public Controller createNewsController(final UserRequest ureq, final WindowControl wControl) {
		final String news = lookupNews();
		return new SimpleNewsController(ureq, wControl, news);
	}

	/**
	 * TODO: rename to getForumController and save instance?
	 * 
	 * @param ureq
	 * @param wControl
	 * @param isAdmin
	 * @param subsContext the subscriptioncontext if subscriptions to this forum should be possible
	 * @return a forum controller
	 */
	public Controller createForumController(final UserRequest ureq, final WindowControl wControl, final boolean isAdmin, final boolean isGuestOnly,
			final SubscriptionContext subsContext) {
		Codepoint.codepoint(CollaborationTools.class, "createForumController-init");
		final boolean isAdm = isAdmin;
		final boolean isGuest = isGuestOnly;
		final ForumManager fom = ForumManager.getInstance();
		final NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);

		// TODO: is there a nicer solution without setting an instance variable
		// final List<Forum> forumHolder = new ArrayList<Forum>();

		Codepoint.codepoint(CollaborationTools.class, "pre_sync_enter");

		final Forum forum = coordinatorManager.getCoordinator().getSyncer().doInSync(ores, new SyncerCallback<Forum>() {
			@Override
			public Forum execute() {

				Codepoint.codepoint(CollaborationTools.class, "sync_enter");

				// was: synchronized (CollaborationTools.class) {
				Forum aforum;
				Long forumKey;
				Property forumKeyProperty = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_FORUM);
				if (forumKeyProperty == null) {
					// First call of forum, create new forum and save
					aforum = fom.addAForum();
					forumKey = aforum.getKey();
					if (log.isDebug()) {
						log.debug("created new forum in collab tools: foid::" + forumKey.longValue() + " for ores::" + ores.getResourceableTypeName() + "/"
								+ ores.getResourceableId());
					}
					forumKeyProperty = npm.createPropertyInstance(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_FORUM, null, forumKey, null, null);
					npm.saveProperty(forumKeyProperty);
				} else {
					// Forum does already exist, load forum with key from properties
					forumKey = forumKeyProperty.getLongValue();
					aforum = fom.loadForum(forumKey);
					if (aforum == null) { throw new AssertException("Unable to load forum with key " + forumKey.longValue() + " for ores "
							+ ores.getResourceableTypeName() + " with key " + ores.getResourceableId()); }
					if (log.isDebug()) {
						log.debug("loading forum in collab tools from properties: foid::" + forumKey.longValue() + " for ores::" + ores.getResourceableTypeName() + "/"
								+ ores.getResourceableId());
					}
				}
				Codepoint.codepoint(CollaborationTools.class, "sync_exit");
				return aforum;
			}
		});

		final Translator trans = Util.createPackageTranslator(this.getClass(), ureq.getLocale());
		final TitleInfo titleInfo = new TitleInfo(null, trans.translate("collabtools.named.hasForum"));
		titleInfo.setSeparatorEnabled(true);
		final Controller forumController = ForumUIFactory.getTitledForumController(ureq, wControl, forum, new ForumCallback() {

			@Override
			public boolean mayOpenNewThread() {
				return true;
			}

			@Override
			public boolean mayReplyMessage() {
				return true;
			}

			@Override
			public boolean mayEditMessageAsModerator() {
				return isAdm;
			}

			@Override
			public boolean mayDeleteMessageAsModerator() {
				return isAdm;
			}

			@Override
			public boolean mayArchiveForum() {
				return !isGuest;
			}

			@Override
			public boolean mayFilterForUser() {
				return isAdm;
			}

			@Override
			public SubscriptionContext getSubscriptionContext() {
				return subsContext;
			}
		}, titleInfo);
		return forumController;
	}

	public String getFolderRelPath() {
		return "/cts/folders/" + ores.getResourceableTypeName() + "/" + ores.getResourceableId();
	}

	/**
	 * Creates a folder run controller with all rights enabled for everybody
	 * 
	 * @param ureq
	 * @param wControl
	 * @param subsContext
	 * @return Copnfigured FolderRunController
	 */
	public FolderRunController createFolderController(final UserRequest ureq, final WindowControl wControl, final SubscriptionContext subsContext) {
		// do not use a global translator since in the fututre a collaborationtools
		// may be shared among users
		final Translator trans = Util.createPackageTranslator(this.getClass(), ureq.getLocale());
		final String relPath = getFolderRelPath();
		final OlatRootFolderImpl rootContainer = new OlatRootFolderImpl(relPath, null);
		final OlatNamedContainerImpl namedContainer = new OlatNamedContainerImpl(trans.translate("folder"), rootContainer);
		namedContainer.setLocalSecurityCallback(new CollabSecCallback(relPath, subsContext));
		final FolderRunController frc = new FolderRunController(namedContainer, true, true, ureq, wControl);
		return frc;
	}

	/**
	 * Creates a calendar controller
	 * 
	 * @param ureq
	 * @param wControl
	 * @param resourceableId
	 * @return Configured WeeklyCalendarController
	 */
	public CalendarController createCalendarController(final UserRequest ureq, final WindowControl wControl, final BusinessGroup businessGroup, final boolean isAdmin) {
		// do not use a global translator since in the fututre a collaborationtools
		// may be shared among users
		final List<KalendarRenderWrapper> calendars = new ArrayList<KalendarRenderWrapper>();
		// get the calendar
		final CalendarManager calManager = CalendarManagerFactory.getInstance().getCalendarManager();
		final KalendarRenderWrapper calRenderWrapper = calManager.getGroupCalendar(businessGroup);
		final boolean isOwner = BaseSecurityManager.getInstance().isIdentityInSecurityGroup(ureq.getIdentity(), businessGroup.getOwnerGroup());
		if (!(isAdmin || isOwner)) {
			// check if participants have read/write access
			int iCalAccess = CollaborationTools.CALENDAR_ACCESS_OWNERS;
			final Long lCalAccess = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(businessGroup).lookupCalendarAccess();
			if (lCalAccess != null) {
				iCalAccess = lCalAccess.intValue();
			}
			if (iCalAccess == CollaborationTools.CALENDAR_ACCESS_ALL) {
				calRenderWrapper.setAccess(KalendarRenderWrapper.ACCESS_READ_WRITE);
			} else {
				calRenderWrapper.setAccess(KalendarRenderWrapper.ACCESS_READ_ONLY);
			}
		} else {
			calRenderWrapper.setAccess(KalendarRenderWrapper.ACCESS_READ_WRITE);
		}
		final KalendarConfig config = calManager.findKalendarConfigForIdentity(calRenderWrapper.getKalendar(), ureq);
		if (config != null) {
			calRenderWrapper.getKalendarConfig().setCss(config.getCss());
			calRenderWrapper.getKalendarConfig().setVis(config.isVis());
		}
		calRenderWrapper.getKalendarConfig().setResId(businessGroup.getKey());
		if (businessGroup.getType().equals(BusinessGroup.TYPE_LEARNINGROUP)) {
			// add linking
			final List<OLATResource> resources = BGContextManagerImpl.getInstance().findOLATResourcesForBGContext(businessGroup.getGroupContext());
			for (final Iterator<OLATResource> iter = resources.iterator(); iter.hasNext();) {
				final OLATResource resource = iter.next();
				if (resource.getResourceableTypeName().equals(CourseModule.getCourseTypeName())) {
					final ICourse course = CourseFactory.loadCourse(resource);
					final CourseLinkProviderController clp = new CourseLinkProviderController(course, ureq, wControl);
					calRenderWrapper.setLinkProvider(clp);
					// for the time being only internal learning groups are supported, therefore we only get
					// the first course reference.
					break;
				}
			}
		}
		calendars.add(calRenderWrapper);

		final WeeklyCalendarController calendarController = new WeeklyCalendarController(ureq, wControl, calendars, WeeklyCalendarController.CALLER_COLLAB, true);

		return calendarController;
	}

	/**
	 * @param ureq
	 * @param wControl
	 * @return a contact form controller
	 */
	public ContactFormController createContactFormController(final UserRequest ureq, final WindowControl wControl, final ContactMessage cmsg) {
		final ContactFormController cfc = new ContactFormController(ureq, wControl, true, true, false, false, cmsg);
		return cfc;
	}

	/**
	 * @param ureq
	 * @param wControl
	 * @param chatName
	 * @return Controller
	 */
	public Controller createChatController(final UserRequest ureq, final WindowControl wControl, final BusinessGroup grp) {
		if (InstantMessagingModule.isEnabled()) {
			GroupChatManagerController ccmc;
			ccmc = InstantMessagingModule.getAdapter().getGroupChatManagerController(ureq);
			ccmc.createGroupChat(ureq, wControl, ores, grp.getName(), false, false);
			return ccmc.getGroupChatController(ores);

		} else {
			throw new AssertException("cannot create a chat controller when instant messasging is disabled in the configuration");
		}
	}

	/**
	 * return an controller for the wiki tool
	 * 
	 * @param ureq
	 * @param wControl
	 * @return
	 */
	public Controller createWikiController(final UserRequest ureq, final WindowControl wControl) {
		// Check for jumping to certain wiki page
		final BusinessControl bc = wControl.getBusinessControl();
		final ContextEntry ce = bc.popLauncherContextEntry();

		final SubscriptionContext subContext = new SubscriptionContext(ores, WikiManager.WIKI_RESOURCE_FOLDER_NAME);
		final boolean isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		final boolean isGuestOnly = ureq.getUserSession().getRoles().isGuestOnly();
		final boolean isResourceOwner = BaseSecurityManager.getInstance().isIdentityPermittedOnResourceable(ureq.getIdentity(), Constants.PERMISSION_ACCESS, ores);
		final WikiSecurityCallback callback = new WikiSecurityCallbackImpl(null, isOlatAdmin, isGuestOnly, true, isResourceOwner, subContext);
		if (ce != null) { // jump to a certain context
			final OLATResourceable ceOres = ce.getOLATResourceable();
			final String typeName = ceOres.getResourceableTypeName();
			String page = typeName.substring("page=".length());
			if (page != null && page.endsWith(":0")) {
				page = page.substring(0, page.length() - 2);
			}
			return WikiManager.getInstance().createWikiMainController(ureq, wControl, ores, callback, page);
		} else {
			return WikiManager.getInstance().createWikiMainController(ureq, wControl, ores, callback, null);
		}
	}

	/**
	 * return an controller for the wiki tool
	 * 
	 * @param ureq
	 * @param wControl
	 * @return
	 */
	public Controller createPortfolioController(final UserRequest ureq, final WindowControl wControl, final BusinessGroup group) {
		final EPFrontendManager ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		final NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);

		final PortfolioStructureMap map = coordinatorManager.getCoordinator().getSyncer().doInSync(ores, new SyncerCallback<PortfolioStructureMap>() {
			@Override
			public PortfolioStructureMap execute() {
				PortfolioStructureMap aMap;
				Long mapKey;
				Property mapKeyProperty = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_PORTFOLIO);
				if (mapKeyProperty == null) {
					// First call of forum, create new forum and save
					aMap = ePFMgr.createAndPersistPortfolioDefaultMap(group, group.getName(), group.getDescription());
					mapKey = aMap.getKey();
					if (log.isDebug()) {
						log.debug("created new portfolio map in collab tools: foid::" + mapKey + " for ores::" + ores.getResourceableTypeName() + "/"
								+ ores.getResourceableId());
					}
					mapKeyProperty = npm.createPropertyInstance(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_PORTFOLIO, null, mapKey, null, null);
					npm.saveProperty(mapKeyProperty);
				} else {
					// Forum does already exist, load forum with key from properties
					mapKey = mapKeyProperty.getLongValue();
					aMap = (PortfolioStructureMap) ePFMgr.loadPortfolioStructureByKey(mapKey);
					if (aMap == null) { throw new AssertException("Unable to load portfolio map with key " + mapKey + " for ores " + ores.getResourceableTypeName()
							+ " with key " + ores.getResourceableId()); }
					if (log.isDebug()) {
						log.debug("loading portfolio map in collab tools from properties: foid::" + mapKey + " for ores::" + ores.getResourceableTypeName() + "/"
								+ ores.getResourceableId());
					}
				}
				return aMap;
			}
		});

		final EPSecurityCallback secCallback = new EPSecurityCallbackImpl(true, true);
		return EPUIFactory.createMapViewController(ureq, wControl, map, secCallback);
	}

	/**
	 * @param toolToChange
	 * @param enable
	 */
	public void setToolEnabled(final String toolToChange, final boolean enable) {
		createOrUpdateProperty(toolToChange, enable);
	}

	/**
	 * reads from the internal cache. <b>Precondition </b> cache must be filled at CollaborationTools creation time.
	 * 
	 * @param enabledTool
	 * @return boolean
	 */
	public boolean isToolEnabled(final String enabledTool) {
		// o_clusterOK as whole object gets invalidated if tool is added or deleted
		if (!cacheToolStates.containsKey(enabledTool)) {
			// not in cache yet, read property first (see getPropertyOf(..))
			getPropertyOf(enabledTool);
		}
		// POSTCONDITION: cacheToolStates.get(enabledTool) != null
		final Boolean cachedValue = cacheToolStates.get(enabledTool);
		return cachedValue.booleanValue();
	}

	/**
	 * delete all CollaborationTools stuff from the database, which is related to the calling OLATResourceable.
	 */
	public void deleteTools(final BusinessGroup businessGroupTodelete) {
		final NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		/*
		 * delete the forum, if existing
		 */
		final ForumManager fom = ForumManager.getInstance();
		final Property forumKeyProperty = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_FORUM);
		if (forumKeyProperty != null) {
			// if there was a forum, delete it
			final Long forumKey = forumKeyProperty.getLongValue();
			if (forumKey == null) { throw new AssertException("property had no longValue, prop:" + forumKeyProperty); }
			fom.deleteForum(forumKey);
		}
		/*
		 * delete the folder, if existing
		 */
		final OlatRootFolderImpl vfsContainer = new OlatRootFolderImpl(getFolderRelPath(), null);
		final File fFolderRoot = vfsContainer.getBasefile();
		if (fFolderRoot.exists()) {
			FileUtils.deleteDirsAndFiles(fFolderRoot, true, true);
		}

		/*
		 * delete the wiki if existing
		 */
		final VFSContainer rootContainer = WikiManager.getInstance().getWikiRootContainer(ores);
		if (rootContainer != null) {
			rootContainer.delete();
		}

		/*
		 * Delete calendar if exists
		 */
		if (businessGroupTodelete != null) {
			final CalendarManager calManager = CalendarManagerFactory.getInstance().getCalendarManager();
			calManager.deleteGroupCalendar(businessGroupTodelete);
		}

		/*
		 * delete chatRoom
		 */
		// no cleanup needed, automatically done when last user exits the room
		/*
		 * delete all Properties defining enabled/disabled CollabTool XY and the news content
		 */
		npm.deleteProperties(null, null, PROP_CAT_BG_COLLABTOOLS, null);

		/*
		 * and last but not least the cache is reseted
		 */
		cacheToolStates.clear();
		this.dirty = true;
	}

	/**
	 * creates the property if non-existing, or updates the existing property to the supplied values. Real changes are made persistent immediately.
	 * 
	 * @param selectedTool
	 * @param toolValue
	 */
	private void createOrUpdateProperty(final String selectedTool, final boolean toolValue) {

		final Boolean cv = cacheToolStates.get(selectedTool);
		if (cv != null && cv.booleanValue() == toolValue) { return; // nice, cache saved a needless update
		}

		// handle Boolean Values via String Field in Property DB Table
		final String toolValueStr = toolValue ? TRUE : FALSE;
		final PropertyManager pm = PropertyManager.getInstance();
		//
		coordinatorManager.getCoordinator().getSyncer().doInSync(ores, new SyncerExecutor() {
			@Override
			public void execute() {
				// was: synchronized (CollaborationTools.class) {
				Property property = getPropertyOf(selectedTool);
				if (property == null) {
					// not existing -> create it
					property = pm.createPropertyInstance(null, null, ores, PROP_CAT_BG_COLLABTOOLS, selectedTool, null, null, toolValueStr, null);
				} else {
					// if existing -> update to desired value
					property.setStringValue(toolValueStr);
				}
				// property becomes persistent
				pm.saveProperty(property);
			}
		});
		this.dirty = true;
		cacheToolStates.put(selectedTool, Boolean.valueOf(toolValue));
	}

	Property getPropertyOf(final String selectedTool) {
		final PropertyManager pm = PropertyManager.getInstance();
		final Property property = pm.findProperty(null, null, ores, PROP_CAT_BG_COLLABTOOLS, selectedTool);
		Boolean res;
		if (property == null) { // meaning false
			res = Boolean.FALSE;
		} else {
			final String val = property.getStringValue();
			res = val.equals(TRUE) ? Boolean.TRUE : Boolean.FALSE;
		}
		cacheToolStates.put(selectedTool, res);
		return property;
	}

	/**
	 * create the Collaboration Tools Suite. This Controller handles the enabling/disabling of Collab Tools.
	 * 
	 * @param ureq
	 * @return a collaboration tools settings controller
	 */
	public CollaborationToolsSettingsController createCollaborationToolsSettingsController(final UserRequest ureq, final WindowControl wControl, final BGConfigFlags flags) {
		return new CollaborationToolsSettingsController(ureq, wControl, ores, flags);
	}

	/**
	 * @return the news; if there is no news yet: return null;
	 */
	public String lookupNews() {
		final NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		final Property property = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_NEWS);
		if (property == null) { // no entry
			return null;
		}
		// read the text value of the existing property
		final String text = property.getTextValue();
		return text;
	}

	/**
	 * @param news
	 */
	public void saveNews(final String news) {
		final NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		final Property property = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_NEWS);
		if (property == null) { // create a new one
			final Property nP = npm.createPropertyInstance(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_NEWS, null, null, null, news);
			npm.saveProperty(nP);
		} else { // modify the existing one
			property.setTextValue(news);
			npm.updateProperty(property);
		}
	}

	public Long lookupCalendarAccess() {
		final NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		final Property property = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_CALENDAR_ACCESS);
		if (property == null) { // no entry
			return null;
		}
		// read the long value of the existing property
		return property.getLongValue();
	}

	public void saveCalendarAccess(final Long calendarAccess) {
		final NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		final Property property = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_CALENDAR_ACCESS);
		if (property == null) { // create a new one
			final Property nP = npm.createPropertyInstance(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_CALENDAR_ACCESS, null, calendarAccess, null, null);
			npm.saveProperty(nP);
		} else { // modify the existing one
			property.setLongValue(calendarAccess);
			npm.updateProperty(property);
		}
	}

	public class CollabSecCallback implements VFSSecurityCallback {

		private Quota folderQuota = null;
		private final SubscriptionContext subsContext;

		public CollabSecCallback(final String relPath, final SubscriptionContext subsContext) {
			this.subsContext = subsContext;
			initFolderQuota(relPath);
		}

		private void initFolderQuota(final String relPath) {
			final QuotaManager qm = QuotaManager.getInstance();
			folderQuota = qm.getCustomQuota(relPath);
			if (folderQuota == null) {
				final Quota defQuota = qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_GROUPS);
				folderQuota = QuotaManager.getInstance().createQuota(relPath, defQuota.getQuotaKB(), defQuota.getUlLimitKB());
			}
		}

		@Override
		public boolean canRead() {
			return true;
		}

		@Override
		public boolean canWrite() {
			return true;
		}

		@Override
		public boolean canDelete() {
			return true;
		}

		@Override
		public boolean canList() {
			return true;
		}

		@Override
		public boolean canCopy() {
			return true;
		}

		@Override
		public boolean canDeleteRevisionsPermanently() {
			return true;
		}

		@Override
		public Quota getQuota() {
			return folderQuota;
		}

		@Override
		public void setQuota(final Quota quota) {
			this.folderQuota = quota;
		}

		@Override
		public SubscriptionContext getSubscriptionContext() {
			return subsContext;
		}
	}

	/**
	 * It is assumed that this is only called by an administrator (e.g. at deleteGroup)
	 * 
	 * @param archivFilePath
	 */
	public void archive(final String archivFilePath) {
		if (isToolEnabled(CollaborationTools.TOOL_FORUM)) {
			archiveForum(this.ores, archivFilePath);
		}
		if (isToolEnabled(CollaborationTools.TOOL_WIKI)) {
			archiveWiki(this.ores, archivFilePath);
		}
		if (isToolEnabled(CollaborationTools.TOOL_FOLDER)) {
			archiveFolder(this.ores, archivFilePath);
		}
	}

	private void archiveForum(final OLATResourceable ores, final String archivFilePath) {
		final ForumManager fom = ForumManager.getInstance();
		final Property forumKeyProperty = NarrowedPropertyManager.getInstance(ores).findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_FORUM);
		if (forumKeyProperty != null) {
			final VFSContainer archiveContainer = new LocalFolderImpl(new File(archivFilePath));
			final String archiveForumName = "del_forum_" + forumKeyProperty.getLongValue();
			final VFSContainer archiveForumContainer = archiveContainer.createChildContainer(archiveForumName);
			final ForumFormatter ff = new ForumRTFFormatter(archiveForumContainer, false);
			ForumArchiveManager.getInstance().applyFormatter(ff, forumKeyProperty.getLongValue(), null);
		}
	}

	private void archiveWiki(final OLATResourceable ores, final String archivFilePath) {
		final VFSContainer wikiContainer = WikiManager.getInstance().getWikiRootContainer(ores);
		final VFSLeaf wikiZip = WikiToZipUtils.getWikiAsZip(wikiContainer);
		final String exportFileName = "del_wiki_" + ores.getResourceableId() + ".zip";
		final File archiveDir = new File(archivFilePath);
		if (!archiveDir.exists()) {
			archiveDir.mkdir();
		}
		final String fullFilePath = archivFilePath + File.separator + exportFileName;

		try {
			FileUtils.bcopy(wikiZip.getInputStream(), new File(fullFilePath), "archive wiki");
		} catch (final FileNotFoundException e) {
			log.warn("Can not archive wiki repoEntry=" + ores.getResourceableId());
		} catch (final IOException ioe) {
			log.warn("Can not archive wiki repoEntry=" + ores.getResourceableId());
		}
	}

	private void archiveFolder(final OLATResourceable ores, final String archiveFilePath) {
		final OlatRootFolderImpl folderContainer = new OlatRootFolderImpl(getFolderRelPath(), null);
		final File fFolderRoot = folderContainer.getBasefile();
		if (fFolderRoot.exists()) {
			final String zipFileName = "del_folder_" + ores.getResourceableId() + ".zip";
			final String fullZipFilePath = archiveFilePath + File.separator + zipFileName;
			ZipUtil.zipAll(fFolderRoot, new File(fullZipFilePath), true);
		}
	}

	/**
	 * whole object gets cached, if tool gets added or deleted the object becomes dirty and will be removed from cache.
	 * 
	 * @return
	 */
	protected boolean isDirty() {
		return dirty;
	}
}