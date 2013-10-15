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

package org.olat.group;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.StaleObjectStateException;
import org.jfree.util.Log;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.collaboration.CollaborationTools;
import org.olat.collaboration.CollaborationToolsFactory;
import org.olat.commons.lifecycle.LifeCycleManager;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.commons.taskExecutor.TaskExecutorManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.DBRuntimeException;
import org.olat.core.logging.KnownIssueException;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.Tracing;
import org.olat.core.logging.activity.ActionType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.FileUtils;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.coordinate.SyncerExecutor;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.MailerWithTemplate;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.Subscriber;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerManagerFactory;
import org.olat.group.area.BGArea;
import org.olat.group.area.BGAreaManager;
import org.olat.group.area.BGAreaManagerImpl;
import org.olat.group.context.BGContext;
import org.olat.group.context.BGContextManager;
import org.olat.group.context.BGContextManagerImpl;
import org.olat.group.delete.service.GroupDeletionManager;
import org.olat.group.properties.BusinessGroupPropertyManager;
import org.olat.group.right.BGRightManager;
import org.olat.group.right.BGRightManagerImpl;
import org.olat.group.ui.BGConfigFlags;
import org.olat.group.ui.BGMailHelper;
import org.olat.group.ui.edit.BusinessGroupModifiedEvent;
import org.olat.instantMessaging.InstantMessagingModule;
import org.olat.instantMessaging.syncservice.SyncSingleUserTask;
import org.olat.notifications.NotificationsManagerImpl;
import org.olat.repository.RepoJumpInHandlerFactory;
import org.olat.repository.RepositoryEntry;
import org.olat.testutils.codepoints.server.Codepoint;
import org.olat.user.UserDataDeletable;
import org.olat.util.logging.activity.LoggingResourceable;

import com.anthonyeden.lib.config.Configuration;
import com.anthonyeden.lib.config.ConfigurationException;
import com.anthonyeden.lib.config.Dom4jConfiguration;
import com.anthonyeden.lib.config.MutableConfiguration;
import com.anthonyeden.lib.config.XMLConfiguration;

/**
 * Description:<br>
 * Persisting implementation of the business group manager. Persists the data in the database.
 * <P>
 * Initial Date: Jul 28, 2004 <br>
 * 
 * @author patrick
 */
public class BusinessGroupManagerImpl extends BasicManager implements BusinessGroupManager, UserDataDeletable {

	private static BusinessGroupManager INSTANCE;

	private static final String EXPORT_ATTR_NAME = "name";
	private static final String EXPORT_ATTR_MAX_PARTICIPATS = "maxParticipants";
	private static final String EXPORT_ATTR_MIN_PARTICIPATS = "minParticipants";
	private static final String EXPORT_ATTR_WAITING_LIST = "waitingList";
	private static final String EXPORT_ATTR_AUTO_CLOSE_RANKS = "autoCloseRanks";
	private static final String EXPORT_KEY_AREA_RELATION = "AreaRelation";
	private static final String EXPORT_KEY_GROUP = "Group";
	private static final String EXPORT_KEY_GROUP_COLLECTION = "GroupCollection";
	private static final String EXPORT_KEY_AREA = "Area";
	private static final String EXPORT_KEY_AREA_COLLECTION = "AreaCollection";
	private static final String EXPORT_KEY_ROOT = "OLATGroupExport";
	private static final String EXPORT_KEY_DESCRIPTION = "Description";
	private static final String EXPORT_KEY_COLLABTOOLS = "CollabTools";
	private static final String EXPORT_KEY_SHOW_OWNERS = "showOwners";
	private static final String EXPORT_KEY_SHOW_PARTICIPANTS = "showParticipants";
	private static final String EXPORT_KEY_SHOW_WAITING_LIST = "showWaitingList";
	private static final String EXPORT_KEY_CALENDAR_ACCESS = "calendarAccess";
	private static final String EXPORT_KEY_NEWS = "info";

	private final BaseSecurity securityManager;
	private final List<DeletableGroupData> deleteListeners;

	/**
	 * @return singleton instance
	 */
	public static BusinessGroupManager getInstance() {
		return INSTANCE;
	}

	/**
	 * [used by spring]
	 */
	private BusinessGroupManagerImpl(final BaseSecurity securityManager, final UserDeletionManager userDeletionManager) {
		userDeletionManager.registerDeletableUserData(this);
		this.securityManager = securityManager;
		deleteListeners = new ArrayList<DeletableGroupData>();
		INSTANCE = this;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#createAndPersistBusinessGroup(java.lang.String, org.olat.core.id.Identity, java.lang.String, java.lang.String,
	 *      java.lang.Integer, java.lang.Integer, java.lang.Boolean, java.lang.Boolean, org.olat.group.context.BGContext)
	 */
	public BusinessGroup createAndPersistBusinessGroup(final String type, final Identity identity, final String name, final String description,
			final Integer minParticipants, final Integer maxParticipants, final Boolean enableWaitinglist, final Boolean enableAutoCloseRanks,
			final BGContext groupContext) {
		final BusinessGroup grp = BusinessGroupFactory.createAndPersistBusinessGroup(type, identity, name, description, minParticipants, maxParticipants,
				enableWaitinglist, enableAutoCloseRanks, groupContext);
		if (grp != null) {
			Tracing.logAudit("Created Business Group", grp.toString(), this.getClass());
		}
		// else no group created
		return grp;
	}

	/**
	 * check if all given names in context exists.
	 * 
	 * @param names
	 * @param groupContext
	 * @return
	 */
	protected boolean checkIfOneOrMoreNameExistsInContext(final Set names, final BGContext groupContext) {
		return BusinessGroupFactory.checkIfOneOrMoreNameExistsInContext(names, groupContext);
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#findBusinessGroupsOwnedBy(java.lang.String, org.olat.core.id.Identity, org.olat.group.context.BGContext)
	 */
	public List findBusinessGroupsOwnedBy(final String type, final Identity identityP, final BGContext bgContext) {
		// attach group context to session - maybe a proxy...
		String query = "select bgi from " + " org.olat.basesecurity.SecurityGroupMembershipImpl as sgmi," + " org.olat.group.BusinessGroupImpl as bgi"
				+ " where bgi.ownerGroup = sgmi.securityGroup and sgmi.identity = :identId";
		if (bgContext != null) {
			query = query + " and bgi.groupContext = :context";
		}
		if (type != null) {
			query = query + " and bgi.type = :type";
		}

		final DB db = DBFactory.getInstance();
		final DBQuery dbq = db.createQuery(query);
		/*
		 * query.append("select distinct v from" + " org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi," + " org.olat.repository.RepositoryEntry v" + " inner
		 * join fetch v.ownerGroup as secGroup" + " inner join fetch v.olatResource as res where" + " sgmsi.securityGroup = secGroup and sgmsi.identity.key=");
		 */

		dbq.setLong("identId", identityP.getKey().longValue());
		if (bgContext != null) {
			dbq.setEntity("context", bgContext);
		}
		if (type != null) {
			dbq.setString("type", type);
		}

		final List res = dbq.list();
		return res;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#findBusinessGroupsAttendedBy(java.lang.String, org.olat.core.id.Identity, org.olat.group.context.BGContext)
	 */
	public List findBusinessGroupsAttendedBy(final String type, final Identity identityP, final BGContext bgContext) {
		String query = "select bgi from " + "  org.olat.group.BusinessGroupImpl as bgi " + ", org.olat.basesecurity.SecurityGroupMembershipImpl as sgmi"
				+ " where bgi.partipiciantGroup = sgmi.securityGroup";
		query = query + " and sgmi.identity = :identId";
		if (bgContext != null) {
			query = query + " and bgi.groupContext = :context";
		}
		if (type != null) {
			query = query + " and bgi.type = :type";
		}

		final DB db = DBFactory.getInstance();
		final DBQuery dbq = db.createQuery(query);
		dbq.setLong("identId", identityP.getKey().longValue());
		if (bgContext != null) {
			dbq.setEntity("context", bgContext);
		}
		if (type != null) {
			dbq.setString("type", type);
		}

		final List res = dbq.list();
		return res;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#getAllBusinessGroups()
	 */
	public List getAllBusinessGroups() {
		final DBQuery dbq = DBFactory.getInstance().createQuery("select bgi from " + "  org.olat.group.BusinessGroupImpl as bgi ");
		return dbq.list();
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#findBusinessGroupsAttendedBy(java.lang.String, org.olat.core.id.Identity, org.olat.group.context.BGContext)
	 */
	public List findBusinessGroupsWithWaitingListAttendedBy(final String type, final Identity identityP, final BGContext bgContext) {
		String query = "select bgi from " + "  org.olat.group.BusinessGroupImpl as bgi " + ", org.olat.basesecurity.SecurityGroupMembershipImpl as sgmi"
				+ " where bgi.waitingGroup = sgmi.securityGroup and sgmi.identity = :identId";
		if (bgContext != null) {
			query = query + " and bgi.groupContext = :context";
		}
		if (type != null) {
			query = query + " and bgi.type = :type";
		}

		final DB db = DBFactory.getInstance();
		final DBQuery dbq = db.createQuery(query);
		dbq.setLong("identId", identityP.getKey().longValue());
		if (bgContext != null) {
			dbq.setEntity("context", bgContext);
		}
		if (type != null) {
			dbq.setString("type", type);
		}

		final List res = dbq.list();
		return res;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#findBusinessGroup(org.olat.basesecurity.SecurityGroup)
	 */
	@Override
	public BusinessGroup findBusinessGroup(final SecurityGroup secGroup) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select bgi from ").append(BusinessGroupImpl.class.getName()).append(" as bgi where ")
				.append("(bgi.partipiciantGroup=:secGroup or bgi.ownerGroup=:secGroup or bgi.waitingGroup=:secGroup)");

		final DBQuery query = DBFactory.getInstance().createQuery(sb.toString());
		query.setEntity("secGroup", secGroup);
		final List<BusinessGroup> res = query.list();
		if (res.isEmpty()) { return null; }
		return res.get(0);
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#updateBusinessGroup(org.olat.group.BusinessGroup)
	 */
	public void updateBusinessGroup(final BusinessGroup updatedBusinessGroup) {
		updatedBusinessGroup.setLastModified(new Date());
		DBFactory.getInstance().updateObject(updatedBusinessGroup);
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#deleteBusinessGroup(org.olat.group.BusinessGroup)
	 */
	public void deleteBusinessGroup(BusinessGroup businessGroupTodelete) {
		try {
			final OLATResourceableJustBeforeDeletedEvent delEv = new OLATResourceableJustBeforeDeletedEvent(businessGroupTodelete);
			// notify all (currently running) BusinessGroupXXXcontrollers
			// about the deletion which will occur.
			CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(delEv, businessGroupTodelete);

			final String type = businessGroupTodelete.getType();
			// refresh object to avoid stale object exceptions
			businessGroupTodelete = loadBusinessGroup(businessGroupTodelete);
			// 0) Loop over all deletableGroupData
			for (final DeletableGroupData deleteListener : deleteListeners) {
				Log.debug("deleteBusinessGroup: call deleteListener=" + deleteListener);
				deleteListener.deleteGroupDataFor(businessGroupTodelete);
			}
			ProjectBrokerManagerFactory.getProjectBrokerManager().deleteGroupDataFor(businessGroupTodelete);
			// 1) Delete all group properties
			final CollaborationTools ct = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(businessGroupTodelete);
			ct.deleteTools(businessGroupTodelete);// deletes everything concerning properties&collabTools
			// 1.b)delete display member property
			final BusinessGroupPropertyManager bgpm = new BusinessGroupPropertyManager(businessGroupTodelete);
			bgpm.deleteDisplayMembers();
			// 2) Delete the group areas
			if (BusinessGroup.TYPE_LEARNINGROUP.equals(type)) {
				BGAreaManagerImpl.getInstance().deleteBGtoAreaRelations(businessGroupTodelete);
			}
			// 3) Delete the group object itself on the database
			DBFactory.getInstance().deleteObject(businessGroupTodelete);
			// 4) Delete the associated security groups
			if (BusinessGroup.TYPE_BUDDYGROUP.equals(type) || BusinessGroup.TYPE_LEARNINGROUP.equals(type)) {
				final SecurityGroup owners = businessGroupTodelete.getOwnerGroup();
				securityManager.deleteSecurityGroup(owners);
			}
			// in all cases the participant groups
			final SecurityGroup partips = businessGroupTodelete.getPartipiciantGroup();
			securityManager.deleteSecurityGroup(partips);
			// Delete waiting-group when one exists
			if (businessGroupTodelete.getWaitingGroup() != null) {
				securityManager.deleteSecurityGroup(businessGroupTodelete.getWaitingGroup());
			}

			// delete the publisher attached to this group (e.g. the forum and folder
			// publisher)
			NotificationsManagerImpl.getInstance().deletePublishersOf(businessGroupTodelete);

			// delete potential jabber group roster
			if (InstantMessagingModule.isEnabled()) {
				final String groupID = InstantMessagingModule.getAdapter().createChatRoomString(businessGroupTodelete);
				InstantMessagingModule.getAdapter().deleteRosterGroup(groupID);
			}
			Tracing.logAudit("Deleted Business Group", businessGroupTodelete.toString(), this.getClass());
		} catch (final DBRuntimeException dbre) {
			final Throwable th = dbre.getCause();
			if ((th instanceof StaleObjectStateException) && (th.getMessage().startsWith("Row was updated or deleted by another transaction"))) {
				// known issue OLAT-3654
				Tracing.logInfo("Group was deleted by another user in the meantime. Known issue OLAT-3654", this.getClass());
				throw new KnownIssueException("Group was deleted by another user in the meantime", 3654);
			} else {
				throw dbre;
			}
		}
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#deleteBusinessGroupWithMail(org.olat.group.BusinessGroup, org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.translator.Translator, java.util.List)
	 */
	public void deleteBusinessGroupWithMail(final BusinessGroup businessGroupTodelete, final WindowControl wControl, final UserRequest ureq, final Translator trans,
			final List contactLists) {
		Codepoint.codepoint(this.getClass(), "deleteBusinessGroupWithMail");

		// collect data for mail
		final BaseSecurity secMgr = BaseSecurityManager.getInstance();
		final List users = new ArrayList();
		final SecurityGroup ownerGroup = businessGroupTodelete.getOwnerGroup();
		if (ownerGroup != null) {
			final List owner = secMgr.getIdentitiesOfSecurityGroup(ownerGroup);
			users.addAll(owner);
		}
		final SecurityGroup partGroup = businessGroupTodelete.getPartipiciantGroup();
		if (partGroup != null) {
			final List participants = secMgr.getIdentitiesOfSecurityGroup(partGroup);
			users.addAll(participants);
		}
		final SecurityGroup watiGroup = businessGroupTodelete.getWaitingGroup();
		if (watiGroup != null) {
			final List waiting = secMgr.getIdentitiesOfSecurityGroup(watiGroup);
			users.addAll(waiting);
		}
		// now delete the group first
		deleteBusinessGroup(businessGroupTodelete);
		// finally send email
		final MailerWithTemplate mailer = MailerWithTemplate.getInstance();
		final MailTemplate mailTemplate = BGMailHelper.createDeleteGroupMailTemplate(businessGroupTodelete, ureq.getIdentity());
		if (mailTemplate != null) {
			final MailerResult mailerResult = mailer.sendMailAsSeparateMails(users, null, null, mailTemplate, null);
			MailHelper.printErrorsAndWarnings(mailerResult, wControl, ureq.getLocale());
		}

	}

	/**
	 * @see org.olat.group.BusinessGroupManager#deleteBusinessGroups(java.util.List)
	 */
	public void deleteBusinessGroups(final List businessGroups) {
		final Iterator iterator = businessGroups.iterator();
		while (iterator.hasNext()) {
			final BusinessGroup group = (BusinessGroup) iterator.next();
			deleteBusinessGroup(group);
		}
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#isIdentityInBusinessGroup(org.olat.core.id.Identity, java.lang.String, org.olat.group.context.BGContext)
	 */
	public boolean isIdentityInBusinessGroup(final Identity identity, final String groupName, final BGContext groupContext) {
		final DB db = DBFactory.getInstance();
		final StringBuilder q = new StringBuilder();
		q.append(" select count(grp) from").append(" org.olat.group.BusinessGroupImpl as grp,").append(" org.olat.basesecurity.SecurityGroupMembershipImpl as secgmemb")
				.append(" where");
		if (groupContext != null) {
			q.append(" grp.groupContext = :context and");
		}
		q.append(" grp.name = :name").append(" and ((grp.partipiciantGroup = secgmemb.securityGroup").append(" and secgmemb.identity = :id) ")
				.append(" or (grp.ownerGroup = secgmemb.securityGroup").append(" and secgmemb.identity = :id)) ");
		final DBQuery query = db.createQuery(q.toString());
		query.setEntity("id", identity);
		if (groupContext != null) {
			query.setEntity("context", groupContext);
		}
		query.setString("name", groupName);
		query.setCacheable(true);
		final List result = query.list();
		if (result.size() == 0) { return false; }
		return (((Long) result.get(0)).intValue() > 0);
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#isIdentityInBusinessGroup(org.olat.core.id.Identity, org.olat.group.BusinessGroup)
	 */
	public boolean isIdentityInBusinessGroup(final Identity identity, final BusinessGroup businessGroup) {
		final SecurityGroup participants = businessGroup.getPartipiciantGroup();
		final SecurityGroup owners = businessGroup.getOwnerGroup();
		if (participants != null) {
			if (securityManager.isIdentityInSecurityGroup(identity, participants)) { return true; }
		}
		if (owners != null) {
			if (securityManager.isIdentityInSecurityGroup(identity, owners)) { return true; }
		}
		return false;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#loadBusinessGroup(org.olat.group.BusinessGroup)
	 */
	public BusinessGroup loadBusinessGroup(final BusinessGroup currBusinessGroup) {
		return (BusinessGroup) DBFactory.getInstance().loadObject(currBusinessGroup);
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#loadBusinessGroup(java.lang.Long, boolean)
	 */
	public BusinessGroup loadBusinessGroup(final Long groupKey, final boolean strict) {
		if (strict) { return (BusinessGroup) DBFactory.getInstance().loadObject(BusinessGroupImpl.class, groupKey); }
		return (BusinessGroup) DBFactory.getInstance().findObject(BusinessGroupImpl.class, groupKey);
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#copyBusinessGroup(org.olat.group.BusinessGroup, java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer,
	 *      org.olat.group.context.BGContext, java.util.Map, boolean, boolean, boolean, boolean, boolean, boolean)
	 */
	public BusinessGroup copyBusinessGroup(final BusinessGroup sourceBusinessGroup, final String targetName, final String targetDescription, final Integer targetMin,
			final Integer targetMax, final BGContext targetBgContext, final Map areaLookupMap, final boolean copyAreas, final boolean copyCollabToolConfig,
			final boolean copyRights, final boolean copyOwners, final boolean copyParticipants, final boolean copyMemberVisibility, final boolean copyWaitingList) {

		// 1. create group
		final String bgType = sourceBusinessGroup.getType();
		// create group, set waitingListEnabled, enableAutoCloseRanks like source business-group
		final BusinessGroup newGroup = createAndPersistBusinessGroup(bgType, null, targetName, targetDescription, targetMin, targetMax,
				sourceBusinessGroup.getWaitingListEnabled(), sourceBusinessGroup.getAutoCloseRanksEnabled(), targetBgContext);
		// return immediately with null value to indicate an already take groupname
		if (newGroup == null) { return null; }
		// 2. copy tools
		if (copyCollabToolConfig) {
			final CollaborationToolsFactory toolsF = CollaborationToolsFactory.getInstance();
			// get collab tools from original group and the new group
			final CollaborationTools oldTools = toolsF.getOrCreateCollaborationTools(sourceBusinessGroup);
			final CollaborationTools newTools = toolsF.getOrCreateCollaborationTools(newGroup);
			// copy the collab tools settings
			for (int i = 0; i < CollaborationTools.TOOLS.length; i++) {
				final String tool = CollaborationTools.TOOLS[i];
				newTools.setToolEnabled(tool, oldTools.isToolEnabled(tool));
			}
			final String oldNews = oldTools.lookupNews();
			newTools.saveNews(oldNews);
		}
		// 3. copy member visibility
		if (copyMemberVisibility) {
			final BusinessGroupPropertyManager bgpm = new BusinessGroupPropertyManager(newGroup);
			bgpm.copyConfigurationFromGroup(sourceBusinessGroup);
		}
		// 4. copy areas
		if (copyAreas) {
			final BGAreaManager areaManager = BGAreaManagerImpl.getInstance();
			final List areas = areaManager.findBGAreasOfBusinessGroup(sourceBusinessGroup);
			final Iterator iterator = areas.iterator();
			while (iterator.hasNext()) {
				final BGArea area = (BGArea) iterator.next();
				if (areaLookupMap == null) {
					// reference target group to source groups areas
					areaManager.addBGToBGArea(newGroup, area);
				} else {
					// reference target group to mapped group areas
					final BGArea mappedArea = (BGArea) areaLookupMap.get(area);
					areaManager.addBGToBGArea(newGroup, mappedArea);
				}
			}
		}
		// 5. copy owners
		if (copyOwners) {
			final List owners = securityManager.getIdentitiesOfSecurityGroup(sourceBusinessGroup.getOwnerGroup());
			final Iterator iter = owners.iterator();
			while (iter.hasNext()) {
				final Identity identity = (Identity) iter.next();
				securityManager.addIdentityToSecurityGroup(identity, newGroup.getOwnerGroup());
			}
		}
		// 6. copy participants
		if (copyParticipants) {
			final List participants = securityManager.getIdentitiesOfSecurityGroup(sourceBusinessGroup.getPartipiciantGroup());
			final Iterator iter = participants.iterator();
			while (iter.hasNext()) {
				final Identity identity = (Identity) iter.next();
				securityManager.addIdentityToSecurityGroup(identity, newGroup.getPartipiciantGroup());
			}
		}
		// 7. copy rights
		if (copyRights) {
			final BGRightManager rightManager = BGRightManagerImpl.getInstance();
			final List sourceRights = rightManager.findBGRights(sourceBusinessGroup);
			final Iterator iterator = sourceRights.iterator();
			while (iterator.hasNext()) {
				final String sourceRight = (String) iterator.next();
				rightManager.addBGRight(sourceRight, newGroup);
			}
		}
		// 8. copy waiting-lisz
		if (copyWaitingList) {
			final List waitingList = securityManager.getIdentitiesOfSecurityGroup(sourceBusinessGroup.getWaitingGroup());
			final Iterator iter = waitingList.iterator();
			while (iter.hasNext()) {
				final Identity identity = (Identity) iter.next();
				securityManager.addIdentityToSecurityGroup(identity, newGroup.getWaitingGroup());
			}
		}
		return newGroup;

	}

	/**
	 * @see org.olat.group.BusinessGroupManager#addParticipant(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.translator.Translator, org.olat.core.id.Identity, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags,
	 *      org.olat.core.logging.UserActivityLogger, boolean)
	 */
	public void addParticipantAndFireEvent(final Identity ureqIdentity, final Identity identity, final BusinessGroup group, final BGConfigFlags flags,
			final boolean doOnlyPostAddingStuff) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(group);
		if (!doOnlyPostAddingStuff) {
			securityManager.addIdentityToSecurityGroup(identity, group.getPartipiciantGroup());
		}
		// add user to buddies rosters
		addToRoster(ureqIdentity, identity, group, flags);
		// notify currently active users of this business group
		BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_ADDED_EVENT, group, identity);
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_PARTICIPANT_ADDED, getClass(), LoggingResourceable.wrap(identity));
		// send notification mail in your controller!
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#addOwner(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest, org.olat.core.gui.translator.Translator,
	 *      org.olat.core.id.Identity, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger, boolean)
	 */
	public void addOwnerAndFireEvent(final Identity ureqIdentity, final Identity identity, final BusinessGroup group, final BGConfigFlags flags,
			final boolean doOnlyPostAddingStuff) {
		if (!doOnlyPostAddingStuff) {
			securityManager.addIdentityToSecurityGroup(identity, group.getOwnerGroup());
		}
		// add user to buddies rosters
		addToRoster(ureqIdentity, identity, group, flags);
		// notify currently active users of this business group
		BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_ADDED_EVENT, group, identity);
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_OWNER_ADDED, getClass(), LoggingResourceable.wrap(identity));
		// send notification mail in your controller!
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#removeOwner(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.translator.Translator, org.olat.core.id.Identity, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags,
	 *      org.olat.core.logging.UserActivityLogger, boolean)
	 */
	public void removeOwnerAndFireEvent(final Identity ureqIdentity, final Identity identity, final BusinessGroup group, final BGConfigFlags flags,
			final boolean doOnlyPostRemovingStuff) {
		if (!doOnlyPostRemovingStuff) {
			securityManager.removeIdentityFromSecurityGroup(identity, group.getOwnerGroup());
		}
		// remove user from buddies rosters
		removeFromRoster(identity, group, flags);

		// remove subsciptions if user gets removed
		removeSubscriptions(identity, group);

		// notify currently active users of this business group
		if (identity.getKey().equals(ureqIdentity.getKey())) {
			BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.MYSELF_ASOWNER_REMOVED_EVENT, group, identity);
		} else {
			BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_REMOVED_EVENT, group, identity);
		}
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_OWNER_REMOVED, getClass(), LoggingResourceable.wrap(group), LoggingResourceable.wrap(identity));
		// send notification mail in your controller!
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#removeParticipant(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.translator.Translator, org.olat.core.id.Identity, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags,
	 *      org.olat.core.logging.UserActivityLogger, boolean)
	 */
	public void removeParticipantAndFireEvent(final Identity ureqIdentity, final Identity identity, final BusinessGroup group, final BGConfigFlags flags,
			final boolean doOnlyPostRemovingStuff) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(group);
		if (!doOnlyPostRemovingStuff) {
			securityManager.removeIdentityFromSecurityGroup(identity, group.getPartipiciantGroup());
		}
		// remove user from buddies rosters
		removeFromRoster(identity, group, flags);

		// remove subsciptions if user gets removed
		removeSubscriptions(identity, group);

		// notify currently active users of this business group
		BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_REMOVED_EVENT, group, identity);
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_PARTICIPANT_REMOVED, getClass(), LoggingResourceable.wrap(identity), LoggingResourceable.wrap(group));
		// Check if a waiting-list with auto-close-ranks is configurated
		if (group.getWaitingListEnabled().booleanValue() && group.getAutoCloseRanksEnabled().booleanValue()) {
			// even when doOnlyPostRemovingStuff is set to true we really transfer the first Identity here
			transferFirstIdentityFromWaitingToParticipant(ureqIdentity, group, flags);
		}
		// send notification mail in your controller!
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#addParticipant(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.translator.Translator, org.olat.core.id.Identity, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags,
	 *      org.olat.core.logging.UserActivityLogger, boolean)
	 */
	public void addToWaitingListAndFireEvent(final Identity ureqIdentity, final Identity identity, final BusinessGroup group, final boolean doOnlyPostAddingStuff) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(group);
		if (!doOnlyPostAddingStuff) {
			securityManager.addIdentityToSecurityGroup(identity, group.getWaitingGroup());
		}
		// notify currently active users of this business group
		BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_ADDED_EVENT, group, identity);
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_TO_WAITING_LIST_ADDED, getClass(), LoggingResourceable.wrap(identity));
		// send notification mail in your controller!
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#removeFromWaitingListAndFireEvent(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.translator.Translator, org.olat.core.id.Identity, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags,
	 *      org.olat.core.logging.UserActivityLogger, boolean)
	 */
	public void removeFromWaitingListAndFireEvent(final Identity userRequestIdentity, final Identity identity, final BusinessGroup group,
			final boolean doOnlyPostRemovingStuff) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(group);
		if (!doOnlyPostRemovingStuff) {
			securityManager.removeIdentityFromSecurityGroup(identity, group.getWaitingGroup());
		}
		// notify currently active users of this business group
		BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.IDENTITY_REMOVED_EVENT, group, identity);
		// do logging
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_FROM_WAITING_LIST_REMOVED, getClass(), LoggingResourceable.wrap(identity));
		// send notification mail in your controller!
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#exportGroups(org.olat.group.context.BGContext, java.io.File)
	 */
	public void exportGroups(final BGContext context, final File fExportFile) {
		if (context == null) { return; // nothing to do... says Florian.
		}
		final Dom4jConfiguration root = new Dom4jConfiguration(EXPORT_KEY_ROOT);

		// export areas
		final MutableConfiguration confAreas = root.addChild(EXPORT_KEY_AREA_COLLECTION);
		final BGAreaManager am = BGAreaManagerImpl.getInstance();
		final List areas = am.findBGAreasOfBGContext(context);
		for (final Iterator iter = areas.iterator(); iter.hasNext();) {
			final BGArea area = (BGArea) iter.next();
			final MutableConfiguration newArea = confAreas.addChild(EXPORT_KEY_AREA);
			newArea.addAttribute(EXPORT_ATTR_NAME, area.getName());
			newArea.addChild(EXPORT_KEY_DESCRIPTION, area.getDescription());
		}

		// TODO fg: export group rights

		// export groups
		final MutableConfiguration confGroups = root.addChild(EXPORT_KEY_GROUP_COLLECTION);
		final BGContextManager cm = BGContextManagerImpl.getInstance();
		final List groups = cm.getGroupsOfBGContext(context);
		for (final Iterator iter = groups.iterator(); iter.hasNext();) {
			final BusinessGroup group = (BusinessGroup) iter.next();
			exportGroup(fExportFile, confGroups, group);
		}

		saveGroupConfiguration(fExportFile, root);
	}

	public void exportGroup(final BusinessGroup group, final File fExportFile) {
		final Dom4jConfiguration root = new Dom4jConfiguration(EXPORT_KEY_ROOT);
		final MutableConfiguration confGroups = root.addChild(EXPORT_KEY_GROUP_COLLECTION);
		exportGroup(fExportFile, confGroups, group);
		saveGroupConfiguration(fExportFile, root);
	}

	private void exportGroup(final File fExportFile, final MutableConfiguration confGroups, final BusinessGroup group) {
		final MutableConfiguration newGroup = confGroups.addChild(EXPORT_KEY_GROUP);
		newGroup.addAttribute(EXPORT_ATTR_NAME, group.getName());
		if (group.getMinParticipants() != null) {
			newGroup.addAttribute(EXPORT_ATTR_MIN_PARTICIPATS, group.getMinParticipants());
		}
		if (group.getMaxParticipants() != null) {
			newGroup.addAttribute(EXPORT_ATTR_MAX_PARTICIPATS, group.getMaxParticipants());
		}
		if (group.getWaitingListEnabled() != null) {
			newGroup.addAttribute(EXPORT_ATTR_WAITING_LIST, group.getWaitingListEnabled());
		}
		if (group.getAutoCloseRanksEnabled() != null) {
			newGroup.addAttribute(EXPORT_ATTR_AUTO_CLOSE_RANKS, group.getAutoCloseRanksEnabled());
		}
		newGroup.addChild(EXPORT_KEY_DESCRIPTION, group.getDescription());
		// collab tools
		final MutableConfiguration toolsConfig = newGroup.addChild(EXPORT_KEY_COLLABTOOLS);
		final CollaborationTools ct = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(group);
		for (int i = 0; i < CollaborationTools.TOOLS.length; i++) {
			toolsConfig.addAttribute(CollaborationTools.TOOLS[i], ct.isToolEnabled(CollaborationTools.TOOLS[i]) ? "true" : "false");
		}
		final Long calendarAccess = ct.lookupCalendarAccess();
		if (calendarAccess != null) {
			newGroup.addAttribute(EXPORT_KEY_CALENDAR_ACCESS, calendarAccess);
		}
		final String info = ct.lookupNews();
		if (info != null && !info.trim().equals("")) {
			newGroup.addAttribute(EXPORT_KEY_NEWS, info.trim());
		}

		Tracing.logDebug("fExportFile.getParent()=" + fExportFile.getParent(), this.getClass());
		ct.archive(fExportFile.getParent());
		// export membership
		final List bgAreas = BGAreaManagerImpl.getInstance().findBGAreasOfBusinessGroup(group);
		for (final Iterator iterator = bgAreas.iterator(); iterator.hasNext();) {
			final BGArea areaRelation = (BGArea) iterator.next();
			final MutableConfiguration newGroupAreaRel = newGroup.addChild(EXPORT_KEY_AREA_RELATION);
			newGroupAreaRel.setValue(areaRelation.getName());
		}
		// export properties
		final BusinessGroupPropertyManager bgPropertyManager = new BusinessGroupPropertyManager(group);
		final boolean showOwners = bgPropertyManager.showOwners();
		final boolean showParticipants = bgPropertyManager.showPartips();
		final boolean showWaitingList = bgPropertyManager.showWaitingList();

		newGroup.addAttribute(EXPORT_KEY_SHOW_OWNERS, showOwners);
		newGroup.addAttribute(EXPORT_KEY_SHOW_PARTICIPANTS, showParticipants);
		newGroup.addAttribute(EXPORT_KEY_SHOW_WAITING_LIST, showWaitingList);
	}

	private void saveGroupConfiguration(final File fExportFile, final Dom4jConfiguration root) {
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(fExportFile);
			final BufferedOutputStream bos = FileUtils.getBos(fOut);
			root.save(bos);
			bos.flush();
			bos.close();
		} catch (final IOException ioe) {
			throw new OLATRuntimeException("Error writing group configuration during group export.", ioe);
		} catch (final ConfigurationException cfe) {
			throw new OLATRuntimeException("Error writing group configuration during group export.", cfe);
		} finally {
			FileUtils.closeSafely(fOut);
		}
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#importGroups(org.olat.group.context.BGContext, java.io.File)
	 */
	public void importGroups(final BGContext context, final File fGroupExportXML) {
		if (!fGroupExportXML.exists()) { return; }

		Configuration groupConfig = null;
		try {
			groupConfig = new XMLConfiguration(fGroupExportXML);
		} catch (final ConfigurationException ce) {
			throw new OLATRuntimeException("Error importing group config.", ce);
		}
		if (!groupConfig.getName().equals(EXPORT_KEY_ROOT)) { throw new AssertException("Invalid group export file. Root does not match."); }

		// get areas
		final BGAreaManager am = BGAreaManagerImpl.getInstance();
		final Configuration confAreas = groupConfig.getChild(EXPORT_KEY_AREA_COLLECTION);
		if (confAreas != null) {
			final List areas = confAreas.getChildren(EXPORT_KEY_AREA);
			for (final Iterator iter = areas.iterator(); iter.hasNext();) {
				final Configuration area = (Configuration) iter.next();
				final String areaName = area.getAttribute(EXPORT_ATTR_NAME);
				final String areaDesc = area.getChildValue(EXPORT_KEY_DESCRIPTION);
				am.createAndPersistBGAreaIfNotExists(areaName, areaDesc, context);
			}
		}

		// TODO fg: import group rights

		// get groups
		final Configuration confGroups = groupConfig.getChild(EXPORT_KEY_GROUP_COLLECTION);
		if (confGroups != null) {
			final BusinessGroupManager gm = BusinessGroupManagerImpl.getInstance();
			final List groups = confGroups.getChildren(EXPORT_KEY_GROUP);
			for (final Iterator iter = groups.iterator(); iter.hasNext();) {
				// create group
				final Configuration group = (Configuration) iter.next();
				final String groupName = group.getAttribute(EXPORT_ATTR_NAME);
				final String groupDesc = group.getChildValue(EXPORT_KEY_DESCRIPTION);

				// get min/max participants
				Integer groupMinParticipants = null;
				final String sMinParticipants = group.getAttribute(EXPORT_ATTR_MIN_PARTICIPATS);
				if (sMinParticipants != null) {
					groupMinParticipants = new Integer(sMinParticipants);
				}
				Integer groupMaxParticipants = null;
				final String sMaxParticipants = group.getAttribute(EXPORT_ATTR_MAX_PARTICIPATS);
				if (sMaxParticipants != null) {
					groupMaxParticipants = new Integer(sMaxParticipants);
				}

				// waiting list configuration
				final String waitingListConfig = group.getAttribute(EXPORT_ATTR_WAITING_LIST);
				Boolean waitingList = null;
				if (waitingListConfig == null) {
					waitingList = Boolean.FALSE;
				} else {
					waitingList = Boolean.valueOf(waitingListConfig);
				}
				final String enableAutoCloseRanksConfig = group.getAttribute(EXPORT_ATTR_AUTO_CLOSE_RANKS);
				Boolean enableAutoCloseRanks = null;
				if (enableAutoCloseRanksConfig == null) {
					enableAutoCloseRanks = Boolean.FALSE;
				} else {
					enableAutoCloseRanks = Boolean.valueOf(enableAutoCloseRanksConfig);
				}

				final BusinessGroup newGroup = gm.createAndPersistBusinessGroup(context.getGroupType(), null, groupName, groupDesc, groupMinParticipants,
						groupMaxParticipants, waitingList, enableAutoCloseRanks, context);

				// get tools config
				final Configuration toolsConfig = group.getChild(EXPORT_KEY_COLLABTOOLS);
				final CollaborationTools ct = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(newGroup);
				for (int i = 0; i < CollaborationTools.TOOLS.length; i++) {
					final String sTool = toolsConfig.getAttribute(CollaborationTools.TOOLS[i]);
					if (sTool != null) {
						ct.setToolEnabled(CollaborationTools.TOOLS[i], sTool.equals("true") ? true : false);
					}
				}
				if (group.getAttribute(EXPORT_KEY_CALENDAR_ACCESS) != null) {
					final Long calendarAccess = Long.valueOf(group.getAttribute(EXPORT_KEY_CALENDAR_ACCESS));
					ct.saveCalendarAccess(calendarAccess);
				}
				if (group.getAttribute(EXPORT_KEY_NEWS) != null) {
					final String info = group.getAttribute(EXPORT_KEY_NEWS);
					ct.saveNews(info);
				}

				// get memberships
				final List memberships = group.getChildren(EXPORT_KEY_AREA_RELATION);
				for (final Iterator iterator = memberships.iterator(); iterator.hasNext();) {
					final Configuration areaRelation = (Configuration) iterator.next();
					final BGArea area = am.findBGArea(areaRelation.getValue(), context);
					if (area == null) { throw new AssertException("Group-Area-Relationship in export, but area was not created during import."); }
					am.addBGToBGArea(newGroup, area);
				}

				// get properties
				boolean showOwners = true;
				boolean showParticipants = true;
				boolean showWaitingList = true;
				if (group.getAttribute(EXPORT_KEY_SHOW_OWNERS) != null) {
					showOwners = Boolean.valueOf(group.getAttribute(EXPORT_KEY_SHOW_OWNERS));
				}
				if (group.getAttribute(EXPORT_KEY_SHOW_PARTICIPANTS) != null) {
					showParticipants = Boolean.valueOf(group.getAttribute(EXPORT_KEY_SHOW_PARTICIPANTS));
				}
				if (group.getAttribute(EXPORT_KEY_SHOW_WAITING_LIST) != null) {
					showWaitingList = Boolean.valueOf(group.getAttribute(EXPORT_KEY_SHOW_WAITING_LIST));
				}
				final BusinessGroupPropertyManager bgPropertyManager = new BusinessGroupPropertyManager(newGroup);
				bgPropertyManager.updateDisplayMembers(showOwners, showParticipants, showWaitingList);
			}
		}
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#moveIdenitFromWaitingListToParticipant(org.olat.core.id.Identity, org.olat.core.gui.control.WindowControl,
	 *      org.olat.core.gui.UserRequest, org.olat.core.gui.translator.Translator, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags,
	 *      org.olat.core.logging.UserActivityLogger)
	 */
	public BusinessGroupAddResponse moveIdenityFromWaitingListToParticipant(final List<Identity> choosenIdentities, final Identity ureqIdentity,
			final BusinessGroup currBusinessGroup, final BGConfigFlags flags) {
		final BusinessGroupAddResponse response = new BusinessGroupAddResponse();
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup, new SyncerExecutor() {
			public void execute() {
				for (final Identity identity : choosenIdentities) {
					// check if idenity is allready in participant
					if (!securityManager.isIdentityInSecurityGroup(identity, currBusinessGroup.getPartipiciantGroup())) {
						// Idenity is not in participant-list => move idenity from waiting-list to participant-list
						BusinessGroupManagerImpl.this.addParticipantAndFireEvent(ureqIdentity, identity, currBusinessGroup, flags, false);
						BusinessGroupManagerImpl.this.removeFromWaitingListAndFireEvent(ureqIdentity, identity, currBusinessGroup, false);
						response.getAddedIdentities().add(identity);
						// notification mail is handled in controller
					} else {
						response.getIdentitiesAlreadyInGroup().add(identity);
					}
				}
			}
		});
		return response;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#getPositionInWaitingListFor(org.olat.core.id.Identity, org.olat.group.BusinessGroup)
	 */
	public int getPositionInWaitingListFor(final Identity identity, final BusinessGroup businessGroup) {
		// get position in waiting-list
		final List identities = securityManager.getIdentitiesAndDateOfSecurityGroup(businessGroup.getWaitingGroup(), true);
		int pos = 0;
		for (int i = 0; i < identities.size(); i++) {
			final Object[] co = (Object[]) identities.get(i);
			final Identity waitingListIdentity = (Identity) co[0];
			if (waitingListIdentity.getName().equals(identity.getName())) {
				pos = i + 1;// '+1' because list begins with 0
			}
		}
		return pos;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#addOwnersAndFireEvent(org.olat.core.id.Identity, java.util.List, org.olat.group.BusinessGroup,
	 *      org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
	 */
	public BusinessGroupAddResponse addOwnersAndFireEvent(final Identity ureqIdentity, final List<Identity> addIdentities, BusinessGroup currBusinessGroup,
			final BGConfigFlags flags) {
		final BusinessGroupAddResponse response = new BusinessGroupAddResponse();
		for (final Identity identity : addIdentities) {
			currBusinessGroup = loadBusinessGroup(currBusinessGroup); // reload business group
			if (securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GUESTONLY)) {
				response.getIdentitiesWithoutPermission().add(identity);
			}
			// Check if identity is already in group. make a db query in case
			// someone in another workflow already added this user to this group. if
			// found, add user to model
			else if (securityManager.isIdentityInSecurityGroup(identity, currBusinessGroup.getOwnerGroup())) {
				response.getIdentitiesAlreadyInGroup().add(identity);
			} else {
				// identity has permission and is not already in group => add it
				addOwnerAndFireEvent(ureqIdentity, identity, currBusinessGroup, flags, false);
				response.getAddedIdentities().add(identity);
				Tracing.logAudit("added identity '" + identity.getName() + "' to securitygroup with key " + currBusinessGroup.getOwnerGroup().getKey(), this.getClass());
			}
		}
		return response;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#addParticipantsAndFireEvent(org.olat.core.id.Identity, java.util.List, org.olat.group.BusinessGroup,
	 *      org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
	 */
	public BusinessGroupAddResponse addParticipantsAndFireEvent(final Identity ureqIdentity, final List<Identity> addIdentities, final BusinessGroup acurrBusinessGroup,
			final BGConfigFlags flags) {
		final BusinessGroupAddResponse response = new BusinessGroupAddResponse();
		final BusinessGroup currBusinessGroup = loadBusinessGroup(acurrBusinessGroup); // reload business group
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup, new SyncerExecutor() {
			public void execute() {
				for (final Identity identity : addIdentities) {
					if (securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GUESTONLY)) {
						response.getIdentitiesWithoutPermission().add(identity);
					}
					// Check if identity is already in group. make a db query in case
					// someone in another workflow already added this user to this group. if
					// found, add user to model
					else if (securityManager.isIdentityInSecurityGroup(identity, currBusinessGroup.getPartipiciantGroup())) {
						response.getIdentitiesAlreadyInGroup().add(identity);
					} else {
						// identity has permission and is not already in group => add it
						addParticipantAndFireEvent(ureqIdentity, identity, currBusinessGroup, flags, false);
						response.getAddedIdentities().add(identity);
						Tracing.logAudit("added identity '" + identity.getName() + "' to securitygroup with key " + currBusinessGroup.getPartipiciantGroup().getKey(),
								this.getClass());
					}
				}
			}
		});
		return response;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#addToWaitingListAndFireEvent(org.olat.core.id.Identity, java.util.List, org.olat.group.BusinessGroup,
	 *      org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
	 */
	public BusinessGroupAddResponse addToWaitingListAndFireEvent(final Identity ureqIdentity, final List<Identity> addIdentities, final BusinessGroup acurrBusinessGroup,
			final BGConfigFlags flags) {
		final BusinessGroupAddResponse response = new BusinessGroupAddResponse();
		final BusinessGroup currBusinessGroup = loadBusinessGroup(acurrBusinessGroup); // reload business group
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup, new SyncerExecutor() {
			public void execute() {
				for (final Identity identity : addIdentities) {
					if (securityManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GUESTONLY)) {
						response.getIdentitiesWithoutPermission().add(identity);
					}
					// Check if identity is already in group. make a db query in case
					// someone in another workflow already added this user to this group. if
					// found, add user to model
					else if (securityManager.isIdentityInSecurityGroup(identity, currBusinessGroup.getWaitingGroup())) {
						response.getIdentitiesAlreadyInGroup().add(identity);
					} else {
						// identity has permission and is not already in group => add it
						BusinessGroupManagerImpl.this.addToWaitingListAndFireEvent(ureqIdentity, identity, currBusinessGroup, false);
						response.getAddedIdentities().add(identity);
						Tracing.logAudit("added identity '" + identity.getName() + "' to securitygroup with key " + currBusinessGroup.getPartipiciantGroup().getKey(),
								this.getClass());
					}
				}
			}
		});
		return response;
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#removeOwnersAndFireEvent(org.olat.core.id.Identity, java.util.List, org.olat.group.BusinessGroup,
	 *      org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
	 */
	public void removeOwnersAndFireEvent(final Identity ureqIdentity, final List<Identity> identities, final BusinessGroup currBusinessGroup, final BGConfigFlags flags) {
		for (final Identity identity : identities) {
			removeOwnerAndFireEvent(ureqIdentity, identity, currBusinessGroup, flags, false);
			Tracing.logAudit("removed identiy '" + identity.getName() + "' from securitygroup with key " + currBusinessGroup.getOwnerGroup().getKey(), this.getClass());
		}
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#removeParticipantsAndFireEvent(org.olat.core.gui.control.WindowControl, org.olat.core.id.Identity,
	 *      org.olat.core.gui.translator.Translator, java.util.List, org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags,
	 *      org.olat.core.logging.UserActivityLogger)
	 */
	public void removeParticipantsAndFireEvent(final Identity ureqIdentity, final List<Identity> identities, final BusinessGroup currBusinessGroup,
			final BGConfigFlags flags) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup, new SyncerExecutor() {
			public void execute() {
				for (final Identity identity : identities) {
					removeParticipantAndFireEvent(ureqIdentity, identity, currBusinessGroup, flags, false);
					Tracing.logAudit("removed identiy '" + identity.getName() + "' from securitygroup with key " + currBusinessGroup.getPartipiciantGroup().getKey(),
							this.getClass());
				}
			}
		});
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#removeFromWaitingListAndFireEvent(org.olat.core.id.Identity, org.olat.core.gui.translator.Translator, java.util.List,
	 *      org.olat.group.BusinessGroup, org.olat.group.ui.BGConfigFlags, org.olat.core.logging.UserActivityLogger)
	 */
	public void removeFromWaitingListAndFireEvent(final Identity ureqIdentity, final List<Identity> identities, final BusinessGroup currBusinessGroup,
			final BGConfigFlags flags) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup, new SyncerExecutor() {
			public void execute() {
				for (final Identity identity : identities) {
					removeFromWaitingListAndFireEvent(ureqIdentity, identity, currBusinessGroup, false);
					Tracing.logAudit("removed identiy '" + identity.getName() + "' from securitygroup with key " + currBusinessGroup.getOwnerGroup().getKey(),
							this.getClass());
				}
			}
		});
	}

	// ////////////////
	// Private Methods
	// ////////////////
	/**
	 * Get all learning resources associated with the context from the given group and buid a direct jump url to this resources
	 * 
	 * @param group
	 * @return String with direct-jumpin-urls
	 */
	private String getAllLearningResourcesFor(final BusinessGroup group) {
		// g
		final StringBuilder learningResources = new StringBuilder();
		if (group.getGroupContext() != null) {
			final BGContextManager contextManager = BGContextManagerImpl.getInstance();
			final List repoEntries = contextManager.findRepositoryEntriesForBGContext(group.getGroupContext());
			final Iterator iter = repoEntries.iterator();
			while (iter.hasNext()) {
				final RepositoryEntry entry = (RepositoryEntry) iter.next();
				final String title = entry.getDisplayname();
				final String url = RepoJumpInHandlerFactory.buildRepositoryDispatchURI(entry);
				learningResources.append(title);
				learningResources.append("\n");
				learningResources.append(url);
				learningResources.append("\n\n");
			}
		}
		return learningResources.toString();
	}

	private void addToRoster(final Identity ureqIdentity, final Identity identity, final BusinessGroup group, final BGConfigFlags flags) {
		if (flags.isEnabled(BGConfigFlags.BUDDYLIST)) {
			if (InstantMessagingModule.isEnabled()) {
				// evaluate whether to sync or not
				final boolean syncBuddy = InstantMessagingModule.getAdapter().getConfig().isSyncPersonalGroups();
				final boolean isBuddy = group.getType().equals(BusinessGroup.TYPE_BUDDYGROUP);

				final boolean syncLearn = InstantMessagingModule.getAdapter().getConfig().isSyncLearningGroups();
				final boolean isLearn = group.getType().equals(BusinessGroup.TYPE_LEARNINGROUP);

				// only sync when a group is a certain type and this type is configured that you want to sync it
				if ((syncBuddy && isBuddy) || (syncLearn && isLearn)) {
					final String groupID = InstantMessagingModule.getAdapter().createChatRoomString(group);
					final String groupDisplayName = group.getName();
					// course group enrolment is time critial so we move this in an separate thread and catch all failures
					TaskExecutorManager.getInstance().runTask(new SyncSingleUserTask(ureqIdentity, groupID, groupDisplayName, identity));
				}
			}
		}
	}

	private void removeFromRoster(final Identity identity, final BusinessGroup group, final BGConfigFlags flags) {
		if (flags.isEnabled(BGConfigFlags.BUDDYLIST)) {
			if (InstantMessagingModule.isEnabled()) {
				// only remove user from roster if not in other security group
				if (!isIdentityInBusinessGroup(identity, group)) {
					final String groupID = InstantMessagingModule.getAdapter().createChatRoomString(group);
					InstantMessagingModule.getAdapter().removeUserFromFriendsRoster(groupID, identity.getName());
				}
			}
		}
	}

	/**
	 * Transfer first identity of waiting.list (if there is one) to the participant-list. Not thread-safe! Do call this method only from a synchronized block!
	 * 
	 * @param wControl
	 * @param ureq
	 * @param trans
	 * @param identity
	 * @param group
	 * @param flags
	 * @param logger
	 * @param secMgr
	 */
	// o_clusterOK by:cg call this method only from synchronized code-block (removeParticipantAndFireEvent( ).
	private void transferFirstIdentityFromWaitingToParticipant(final Identity ureqIdentity, BusinessGroup group, final BGConfigFlags flags) {
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(group);
		// Check if waiting-list is enabled and auto-rank-up
		if (group.getWaitingListEnabled().booleanValue() && group.getAutoCloseRanksEnabled().booleanValue()) {
			// Check if participant is not full
			final Integer maxSize = group.getMaxParticipants();
			final int waitingPartipiciantSize = securityManager.countIdentitiesOfSecurityGroup(group.getPartipiciantGroup());
			if ((maxSize != null) && (waitingPartipiciantSize < maxSize.intValue())) {
				// ok it has free places => get first idenity from Waitinglist
				final List identities = securityManager.getIdentitiesAndDateOfSecurityGroup(group.getWaitingGroup(), true/* sortedByAddedDate */);
				int i = 0;
				boolean transferNotDone = true;
				while (i < identities.size() && transferNotDone) {
					// It has an identity and transfer from waiting-list to participant-group is not done
					final Object[] co = (Object[]) identities.get(i++);
					final Identity firstWaitingListIdentity = (Identity) co[0];
					// reload group
					group = (BusinessGroup) DBFactory.getInstance().loadObject(group, true);
					// Check if firstWaitingListIdentity is not allready in participant-group
					if (!securityManager.isIdentityInSecurityGroup(firstWaitingListIdentity, group.getPartipiciantGroup())) {
						// move the identity from the waitinglist to the participant group

						final ActionType formerStickyActionType = ThreadLocalUserActivityLogger.getStickyActionType();
						try {
							// OLAT-4955: force add-participant and remove-from-waitinglist logging actions
							// that get triggered in the next two methods to be of ActionType admin
							// This is needed to make sure the targetIdentity ends up in the o_loggingtable
							ThreadLocalUserActivityLogger.setStickyActionType(ActionType.admin);
							this.addParticipantAndFireEvent(ureqIdentity, firstWaitingListIdentity, group, flags, false);
							this.removeFromWaitingListAndFireEvent(ureqIdentity, firstWaitingListIdentity, group, false);
						} finally {
							ThreadLocalUserActivityLogger.setStickyActionType(formerStickyActionType);
						}
						// send a notification mail if available
						final MailTemplate mailTemplate = BGMailHelper.createWaitinglistTransferMailTemplate(group, ureqIdentity);
						if (mailTemplate != null) {
							final MailerWithTemplate mailer = MailerWithTemplate.getInstance();
							final MailerResult mailerResult = mailer.sendMail(firstWaitingListIdentity, null, null, mailTemplate, null);
							// Does not report errors to current screen because this is the identity who triggered the transfer
							Tracing.logWarn("Could not send WaitinglistTransferMail for identity=" + firstWaitingListIdentity.getName(), BusinessGroupManagerImpl.class);
						}
						transferNotDone = false;
					}
				}
			}
		} else {
			Tracing.logWarn("Called method transferFirstIdentityFromWaitingToParticipant but waiting-list or autoCloseRanks is disabled.", BusinessGroupManagerImpl.class);
		}
	}

	/**
	 * Delete all entries as participant, owner and waiting-list for certain identity. If there is no other owner for a group, the olat-administrator (defined in spring
	 * config) will be added as owner.
	 * 
	 * @see org.olat.user.UserDataDeletable#deleteUserData(org.olat.core.id.Identity)
	 */
	public void deleteUserData(final Identity identity, final String newDeletedUserName) {
		// remove as Participant
		final List attendedGroups = findAllBusinessGroupsAttendedBy(identity);
		for (final Iterator iter = attendedGroups.iterator(); iter.hasNext();) {
			securityManager.removeIdentityFromSecurityGroup(identity, ((BusinessGroup) iter.next()).getPartipiciantGroup());
		}
		Tracing.logDebug("Remove partipiciant identity=" + identity + " from " + attendedGroups.size() + " groups", this.getClass());
		// remove from waitinglist
		final List waitingGroups = findBusinessGroupsWithWaitingListAttendedBy(identity);
		for (final Iterator iter = waitingGroups.iterator(); iter.hasNext();) {
			securityManager.removeIdentityFromSecurityGroup(identity, ((BusinessGroup) iter.next()).getWaitingGroup());
		}
		Tracing.logDebug("Remove from waiting-list identity=" + identity + " in " + waitingGroups.size() + " groups", this.getClass());

		// remove as owner
		final List ownerGroups = findAllBusinessGroupsOwnedBy(identity);
		for (final Iterator iter = ownerGroups.iterator(); iter.hasNext();) {
			final BusinessGroup businessGroup = (BusinessGroup) iter.next();
			securityManager.removeIdentityFromSecurityGroup(identity, businessGroup.getOwnerGroup());
			if (businessGroup.getType().equals(BusinessGroup.TYPE_BUDDYGROUP) && securityManager.countIdentitiesOfSecurityGroup(businessGroup.getOwnerGroup()) == 0) {
				// Buddygroup has no owner anymore => add OLAT-Admin as owner
				securityManager.addIdentityToSecurityGroup(UserDeletionManager.getInstance().getAdminIdentity(), businessGroup.getOwnerGroup());
				Tracing.logInfo("Delete user-data, add Administrator-identity as owner of businessGroup=" + businessGroup.getName(), this.getClass());
			}
		}
		Tracing.logDebug("Remove owner identity=" + identity + " from " + ownerGroups.size() + " groups", this.getClass());
		Tracing.logDebug("All entries in groups deleted for identity=" + identity, this.getClass());
	}

	private List findAllBusinessGroupsOwnedBy(final Identity identity) {
		return findBusinessGroupsOwnedBy(null, identity, null);
	}

	private List findAllBusinessGroupsAttendedBy(final Identity identity) {
		return findBusinessGroupsAttendedBy(null, identity, null);
	}

	private List findBusinessGroupsWithWaitingListAttendedBy(final Identity identity) {
		return findBusinessGroupsWithWaitingListAttendedBy(null, identity, null);
	}

	public void archiveGroups(final BGContext context, final File exportFile) {
		BusinessGroupArchiver.getInstance().archiveBGContext(context, exportFile);
	}

	private void removeSubscriptions(final Identity identity, final BusinessGroup group) {
		final NotificationsManager notiMgr = NotificationsManager.getInstance();
		final List<Subscriber> l = notiMgr.getSubscribers(identity);
		for (final Iterator iterator = l.iterator(); iterator.hasNext();) {
			final Subscriber subscriber = (Subscriber) iterator.next();
			final Long resId = subscriber.getPublisher().getResId();
			final Long groupKey = group.getKey();
			if (resId != null && groupKey != null && resId.equals(groupKey)) {
				notiMgr.unsubscribe(subscriber);
			}
		}
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#setLastUsageFor(org.olat.group.BusinessGroup)
	 */
	public void setLastUsageFor(final BusinessGroup currBusinessGroup) {
		// o_clusterOK by:cg
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(currBusinessGroup, new SyncerExecutor() {
			public void execute() {
				// force a reload from db loadObject(..., true) by evicting it from
				// hibernates session
				// cache to catch up on a different thread having commited the update of
				// the launchcounter
				final BusinessGroup reloadedBusinessGroup = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(currBusinessGroup);
				reloadedBusinessGroup.setLastUsage(new Date());
				LifeCycleManager.createInstanceFor(reloadedBusinessGroup).deleteTimestampFor(GroupDeletionManager.SEND_DELETE_EMAIL_ACTION);
				BusinessGroupManagerImpl.getInstance().updateBusinessGroup(reloadedBusinessGroup);
			}
		});
	}

	/**
	 * @see org.olat.group.BusinessGroupManager#createUniqueBusinessGroupsFor(java.util.Set, org.olat.group.context.BGContext, java.lang.String, java.lang.Integer,
	 *      java.lang.Integer, java.lang.Boolean, java.lang.Boolean)
	 */
	public Set<BusinessGroup> createUniqueBusinessGroupsFor(final Set<String> allNames, final BGContext bgContext, final String bgDesc, final Integer bgMin,
			final Integer bgMax, final Boolean enableWaitinglist, final Boolean enableAutoCloseRanks) {
		// o_clusterOK by:cg
		final Set<BusinessGroup> createdGroups = CoordinatorManager.getInstance().getCoordinator().getSyncer()
				.doInSync(bgContext, new SyncerCallback<Set<BusinessGroup>>() {
					public Set<BusinessGroup> execute() {
						if (checkIfOneOrMoreNameExistsInContext(allNames, bgContext)) {
							// set error of non existing name
							return null;
						} else {
							// create bulkgroups only if there is no name which already exists.
							final Set<BusinessGroup> newGroups = new HashSet<BusinessGroup>();
							for (final Iterator<String> iter = allNames.iterator(); iter.hasNext();) {
								final String bgName = iter.next();
								final BusinessGroup newGroup = createAndPersistBusinessGroup(bgContext.getGroupType(), null, bgName, bgDesc, bgMin, bgMax,
										enableWaitinglist, enableAutoCloseRanks, bgContext);
								newGroups.add(newGroup);
							}
							return newGroups;
						}
					}
				});
		return createdGroups;
	}

	public void registerDeletableGroupDataListener(final DeletableGroupData listener) {
		this.deleteListeners.add(listener);
	}

	@Override
	public List<String> getDependingDeletablableListFor(final BusinessGroup currentGroup, final Locale locale) {
		final List<String> deletableList = new ArrayList<String>();
		for (final DeletableGroupData deleteListener : deleteListeners) {
			final DeletableReference deletableReference = deleteListener.checkIfReferenced(currentGroup, locale);
			if (deletableReference.isReferenced()) {
				deletableList.add(deletableReference.getName());
			}
		}
		return deletableList;
	}

}
