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

package org.olat.group.context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.type.Type;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManager;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.group.area.BGArea;
import org.olat.group.area.BGAreaManager;
import org.olat.group.area.BGAreaManagerImpl;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.resource.OLATResource;

/**
 * Description: <BR>
 * Implementation of the business group context manager.
 * <P>
 * Initial Date: Aug 19, 2004 <br>
 * 
 * @author gnaegi
 */
public class BGContextManagerImpl extends BasicManager implements BGContextManager {

	private static BGContextManager INSTANCE;
	static {
		INSTANCE = new BGContextManagerImpl();
	}

	/**
	 * @return singleton instance
	 */
	public static BGContextManager getInstance() {
		return INSTANCE;
	}

	private BGContextManagerImpl() {
		// no public constructor
	}

	/**
	 * @see org.olat.group.context.BGContextManager#createAndPersistBGContext(java.lang.String, java.lang.String, java.lang.String, org.olat.core.id.Identity, boolean)
	 */
	@Override
	public BGContext createAndPersistBGContext(final String name, final String description, final String groupType, final Identity owner, final boolean defaultContext) {
		if (name == null) { throw new AssertException("Business group context name must not be null"); }
		if (groupType == null) { throw new AssertException("Business group groupType name must not be null"); }
		final BaseSecurity securityManager = BaseSecurityManager.getInstance();
		// 1) create administrative owner security group, add owner if available
		final SecurityGroup ownerGroup = securityManager.createAndPersistSecurityGroup();
		if (owner != null) {
			securityManager.addIdentityToSecurityGroup(owner, ownerGroup);
		}
		// 2) create new group context with this security group and save it
		final BGContext bgContext = new BGContextImpl(name, description, ownerGroup, groupType, defaultContext);
		DBFactory.getInstance().saveObject(bgContext);
		// 3) save context owner policy to this context and the owner group
		securityManager.createAndPersistPolicy(ownerGroup, Constants.PERMISSION_ACCESS, bgContext);
		// 4) save groupmanager policy on this group - all members are automatically
		// group managers
		securityManager.createAndPersistPolicy(ownerGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GROUPMANAGER);
		Tracing.logAudit("Created Business Group Context", bgContext.toString(), this.getClass());
		return bgContext;
	}

	/**
	 * @see org.olat.group.context.BGContextManager#copyAndAddBGContextToResource(java.lang.String, org.olat.resource.OLATResource, org.olat.group.context.BGContext)
	 */
	@Override
	public BGContext copyAndAddBGContextToResource(final String contextName, final OLATResource resource, final BGContext originalBgContext) {
		final BGAreaManager areaManager = BGAreaManagerImpl.getInstance();
		final BusinessGroupManager groupManager = BusinessGroupManagerImpl.getInstance();
		if (!originalBgContext.isDefaultContext()) { throw new AssertException("Can only copy default contexts"); }

		// 1. Copy context as default context. Owner group of original context will
		// not be
		// copied since this is a default context
		final BGContext targetContext = createAndAddBGContextToResource(contextName, resource, originalBgContext.getGroupType(), null, true);
		// 2. Copy areas
		final Map areas = areaManager.copyBGAreasOfBGContext(originalBgContext, targetContext);
		// 3. Copy Groups
		// only group configuration will be copied, no group members are copied
		final List origGroups = getGroupsOfBGContext(originalBgContext);
		final Iterator iter = origGroups.iterator();
		while (iter.hasNext()) {
			final BusinessGroup origGroup = (BusinessGroup) iter.next();
			groupManager.copyBusinessGroup(origGroup, origGroup.getName(), origGroup.getDescription(), origGroup.getMinParticipants(), origGroup.getMaxParticipants(),
					targetContext, areas, true, true, true, false, false, true, false);
		}
		return targetContext;
	}

	/**
	 * @see org.olat.group.context.BGContextManager#updateBGContext(org.olat.group.context.BGContext)
	 */
	@Override
	public void updateBGContext(final BGContext bgContext) {
		// 1) update context
		DBFactory.getInstance().updateObject(bgContext);
		// 2) reload course contexts for all courses wher this context is used
		final List resources = findOLATResourcesForBGContext(bgContext);
		for (final Iterator iter = resources.iterator(); iter.hasNext();) {
			final OLATResource resource = (OLATResource) iter.next();
			if (resource.getResourceableTypeName().equals(CourseModule.getCourseTypeName())) {
				final ICourse course = CourseFactory.loadCourse(resource);
				course.getCourseEnvironment().getCourseGroupManager().initGroupContextsList();
			} else if (resource.getResourceableTypeName().equals("junitcourse")) {
				// do nothing when in junit test mode
			} else {
				throw new AssertException("Currently only course resources allowed in resource to context relations.");
			}
		}
	}

	/**
	 * @see org.olat.group.context.BGContextManager#deleteBGContext(org.olat.group.context.BGContext)
	 */
	public void deleteBGContext(BGContext bgContext) {
		bgContext = (BGContext) DBFactory.getInstance().loadObject(bgContext);
		final BusinessGroupManager bgManager = BusinessGroupManagerImpl.getInstance();
		final BGAreaManager areaManager = BGAreaManagerImpl.getInstance();
		// 1) Delete all groups from group context
		final List groups = getGroupsOfBGContext(bgContext);
		bgManager.deleteBusinessGroups(groups);
		// 2) Delete all group areas
		final List areas = areaManager.findBGAreasOfBGContext(bgContext);
		for (final Iterator iter = areas.iterator(); iter.hasNext();) {
			final BGArea area = (BGArea) iter.next();
			areaManager.deleteBGArea(area);
		}
		// 3) Delete group to resource relations
		final List referencingResources = findOLATResourcesForBGContext(bgContext);
		for (final Iterator iter = referencingResources.iterator(); iter.hasNext();) {
			final OLATResource refRes = (OLATResource) iter.next();
			removeBGContextFromResource(bgContext, refRes);
		}
		// 4) Delete group context
		DBFactory.getInstance().deleteObject(bgContext);
		// 5) Delete security group
		final SecurityGroup owners = bgContext.getOwnerGroup();
		if (owners != null) {
			final BaseSecurity secMgr = BaseSecurityManager.getInstance();
			secMgr.deleteSecurityGroup(owners);
		}
		Tracing.logAudit("Deleted Business Group Context", bgContext.toString(), this.getClass());
	}

	/**
	 * @see org.olat.group.context.BGContextManager#getGroupsOfBGContext(org.olat.group.context.BGContext)
	 */
	public List<BusinessGroup> getGroupsOfBGContext(final BGContext bgContext) {
		final DB db = DBFactory.getInstance();
		DBQuery query;
		if (bgContext == null) {
			final String q = "select bg from org.olat.group.BusinessGroupImpl bg where bg.groupContext is null";
			query = db.createQuery(q);
		} else {
			final String q = "select bg from org.olat.group.BusinessGroupImpl bg where bg.groupContext = :context";
			query = db.createQuery(q);
			query.setEntity("context", bgContext);
		}
		return (List<BusinessGroup>) query.list();
	}

	/**
	 * @see org.olat.group.context.BGContextManager#countGroupsOfBGContext(org.olat.group.context.BGContext)
	 */
	public int countGroupsOfBGContext(final BGContext bgContext) {
		final DB db = DBFactory.getInstance();
		final String q = "select count(bg) from org.olat.group.BusinessGroupImpl bg where bg.groupContext = :context";
		final DBQuery query = db.createQuery(q);
		query.setEntity("context", bgContext);
		return ((Long) query.list().get(0)).intValue();
	}

	/**
	 * @see org.olat.group.context.BGContextManager#countGroupsOfType(java.lang.String)
	 */
	public int countGroupsOfType(final String groupType) {
		final DB db = DBFactory.getInstance();
		final String q = "select count(bg) from org.olat.group.BusinessGroupImpl bg where bg.type = :type";
		final DBQuery query = db.createQuery(q);
		query.setString("type", groupType);
		return ((Long) query.list().get(0)).intValue();
	}

	/**
	 * @see org.olat.group.context.BGContextManager#findGroupOfBGContext(java.lang.String, org.olat.group.context.BGContext)
	 */
	public BusinessGroup findGroupOfBGContext(final String groupName, final BGContext bgContext) {
		final DB db = DBFactory.getInstance();
		final String q = "select bg from org.olat.group.BusinessGroupImpl bg where bg.groupContext = :context and bg.name = :name";
		final DBQuery query = db.createQuery(q);
		query.setEntity("context", bgContext);
		query.setString("name", groupName);
		final List results = query.list();
		if (results.size() == 0) { return null; }
		return (BusinessGroup) results.get(0);
	}

	/**
	 * @see org.olat.group.context.BGContextManager#findGroupAttendedBy(org.olat.core.id.Identity, java.lang.String, org.olat.group.context.BGContext)
	 */
	public BusinessGroup findGroupAttendedBy(final Identity identity, final String groupName, final BGContext bgContext) {
		final String query = "select bgi from " + "  org.olat.group.BusinessGroupImpl as bgi " + ", org.olat.basesecurity.SecurityGroupMembershipImpl as sgmi"
				+ " where bgi.name = :name " + " and bgi.partipiciantGroup =  sgmi.securityGroup" + " and sgmi.identity = :identId" + " and bgi.groupContext = :context";
		final DB db = DBFactory.getInstance();
		final DBQuery dbq = db.createQuery(query);
		dbq.setEntity("identId", identity);
		dbq.setString("name", groupName);
		dbq.setEntity("context", bgContext);
		final List res = dbq.list();
		if (res.size() == 0) {
			return null;
		} else if (res.size() > 1) { throw new AssertException("more than one result row found for (identity, groupname, context) (" + identity.getName() + ", "
				+ groupName + ", " + bgContext.getName()); }
		return (BusinessGroup) res.get(0);
	}

	/**
	 * @see org.olat.group.context.BGContextManager#getBGOwnersOfBGContext(org.olat.group.context.BGContext)
	 */
	public List getBGOwnersOfBGContext(final BGContext bgContext) {
		final DB db = DBFactory.getInstance();
		final String q = "select distinct id from org.olat.basesecurity.IdentityImpl as id inner join fetch id.user as iuser"
				+ ", org.olat.basesecurity.SecurityGroupMembershipImpl sgm" + ", org.olat.group.BusinessGroupImpl bg" + " where bg.groupContext = :context"
				+ " and bg.ownerGroup = sgm.securityGroup" + " and sgm.identity = id";
		final DBQuery query = db.createQuery(q);
		query.setEntity("context", bgContext);
		return query.list();
	}

	/**
	 * @see org.olat.group.context.BGContextManager#countBGOwnersOfBGContext(org.olat.group.context.BGContext)
	 */
	public int countBGOwnersOfBGContext(final BGContext bgContext) {
		final DB db = DBFactory.getInstance();
		final String q = "select count(distinct id) from org.olat.basesecurity.IdentityImpl id" + ", org.olat.basesecurity.SecurityGroupMembershipImpl sgm"
				+ ", org.olat.group.BusinessGroupImpl bg" + " where bg.groupContext = :context" + " and bg.ownerGroup = sgm.securityGroup" + " and sgm.identity = id";
		final DBQuery query = db.createQuery(q);
		query.setEntity("context", bgContext);
		final List resultList = query.list();

		int result = 0;
		// if no join/group by matches, result list size is 0 and count undefined ->
		// result is 0
		if (resultList.size() > 0) {
			final Object obj = resultList.get(0);
			if (obj == null) { return 0; }
			result = ((Long) obj).intValue();
		}
		return result;
	}

	/**
	 * @see org.olat.group.context.BGContextManager#getBGParticipantsOfBGContext(org.olat.group.context.BGContext)
	 */
	public List getBGParticipantsOfBGContext(final BGContext bgContext) {
		final DB db = DBFactory.getInstance();
		final String q = "select distinct id from org.olat.basesecurity.IdentityImpl as id inner join fetch id.user as iuser"
				+ ", org.olat.basesecurity.SecurityGroupMembershipImpl sgm" + ", org.olat.group.BusinessGroupImpl bg" + " where bg.groupContext = :context"
				+ " and bg.partipiciantGroup = sgm.securityGroup" + " and sgm.identity = id";
		final DBQuery query = db.createQuery(q);
		query.setEntity("context", bgContext);
		return query.list();
	}

	/**
	 * @see org.olat.group.context.BGContextManager#countBGParticipantsOfBGContext(org.olat.group.context.BGContext)
	 */
	public int countBGParticipantsOfBGContext(final BGContext bgContext) {
		final DB db = DBFactory.getInstance();
		final String q = "select count(distinct id) from org.olat.basesecurity.IdentityImpl id" + ", org.olat.basesecurity.SecurityGroupMembershipImpl sgm"
				+ ", org.olat.group.BusinessGroupImpl bg" + " where bg.groupContext = :context" + " and bg.partipiciantGroup = sgm.securityGroup"
				+ " and sgm.identity = id";
		final DBQuery query = db.createQuery(q);
		query.setEntity("context", bgContext);
		final List resultList = query.list();
		int result = 0;
		// if no join/group by matches, result list size is 0 and count undefined ->
		// result is 0
		if (resultList.size() > 0) {
			final Object obj = resultList.get(0);
			if (obj == null) { return 0; }
			result = ((Long) obj).intValue();
		}
		return result;
	}

	/**
	 * @see org.olat.group.context.BGContextManager#isIdentityInBGContext(org.olat.core.id.Identity, org.olat.group.context.BGContext, boolean, boolean)
	 */
	public boolean isIdentityInBGContext(final Identity identity, final BGContext bgContext, final boolean asOwner, final boolean asParticipant) {
		final DB db = DBFactory.getInstance();
		final StringBuilder q = new StringBuilder();

		q.append(" select count(grp) from" + " org.olat.group.BusinessGroupImpl as grp,"
				+ " org.olat.basesecurity.SecurityGroupMembershipImpl as secgmemb where grp.groupContext = :context" + " and ");
		// restricting where clause for participants
		final String partRestr = "(grp.partipiciantGroup = secgmemb.securityGroup and secgmemb.identity = :id) ";
		// restricting where clause for owners
		final String ownRestr = "(grp.ownerGroup = secgmemb.securityGroup and secgmemb.identity = :id)";

		if (asParticipant && asOwner) {
			q.append("(").append(partRestr).append(" or ").append(ownRestr).append(")");
		} else if (asParticipant && !asOwner) {
			q.append(partRestr);
		} else if (!asParticipant && asOwner) {
			q.append(ownRestr);
		} else {
			throw new AssertException("illegal arguments: at leas one of asOwner or asParticipant must be true");
		}

		final DBQuery query = db.createQuery(q.toString());
		query.setEntity("id", identity);
		query.setEntity("context", bgContext);
		query.setCacheable(true);
		final List result = query.list();

		if (result.size() == 0) { return false; }
		return (((Long) result.get(0)).intValue() > 0);
	}

	/**
	 * @see org.olat.group.context.BGContextManager#createAndAddBGContextToResource(java.lang.String, org.olat.resource.OLATResource, java.lang.String,
	 *      org.olat.core.id.Identity, boolean)
	 */
	public BGContext createAndAddBGContextToResource(final String contextName, final OLATResource resource, final String groupType, final Identity initialOwner,
			final boolean defaultContext) {
		final BGContextManager cm = BGContextManagerImpl.getInstance();
		final BGContext context = cm.createAndPersistBGContext(contextName, null, groupType, initialOwner, defaultContext);
		addBGContextToResource(context, resource);
		return context;
	}

	/**
	 * @see org.olat.group.context.BGContextManager#addBGContextToResource(org.olat.group.context.BGContext, org.olat.resource.OLATResource)
	 */
	public void addBGContextToResource(final BGContext bgContext, final OLATResource resource) {
		final BGContext2Resource courseBgContext = new BGContext2Resource(resource, bgContext);
		DBFactory.getInstance().saveObject(courseBgContext);
		// update course context list in this course resource
		if (resource.getResourceableTypeName().equals(CourseModule.getCourseTypeName())) {
			final ICourse course = CourseFactory.loadCourse(resource);
			course.getCourseEnvironment().getCourseGroupManager().initGroupContextsList();
		} else if (resource.getResourceableTypeName().equals("junitcourse")) {
			// do nothing when in junit test mode
		} else {
			throw new AssertException("Currently only course resources allowed in resource to context relations.");
		}
		Tracing.logAudit("Added Business Group Context to OLATResource " + resource.toString(), bgContext.toString(), this.getClass());
	}

	/**
	 * @see org.olat.group.context.BGContextManager#findBGContextsForResource(org.olat.resource.OLATResource, boolean, boolean)
	 */
	public List findBGContextsForResource(final OLATResource resource, final boolean defaultContexts, final boolean nonDefaultContexts) {
		return findBGContextsForResource(resource, null, defaultContexts, nonDefaultContexts);
	}

	/**
	 * @see org.olat.group.context.BGContextManager#findBGContextsForResource(org.olat.resource.OLATResource, java.lang.String, boolean, boolean)
	 */
	public List findBGContextsForResource(final OLATResource resource, final String groupType, final boolean defaultContexts, final boolean nonDefaultContexts) {
		final DB db = DBFactory.getInstance();
		final StringBuilder q = new StringBuilder();
		q.append(" select context from org.olat.group.context.BGContextImpl as context,");
		q.append(" org.olat.group.context.BGContext2Resource as bgcr");
		q.append(" where bgcr.resource = :resource");
		q.append(" and bgcr.groupContext = context");
		if (groupType != null) {
			q.append(" and context.groupType = :gtype");
		}

		final boolean checkDefault = defaultContexts != nonDefaultContexts;
		if (checkDefault) {
			q.append(" and context.defaultContext = :isDefault");
		}
		final DBQuery query = db.createQuery(q.toString());
		query.setEntity("resource", resource);
		if (groupType != null) {
			query.setString("gtype", groupType);
		}
		if (checkDefault) {
			query.setBoolean("isDefault", defaultContexts ? true : false);
		}
		return query.list();
	}

	/**
	 * @see org.olat.group.context.BGContextManager#findBGContextsForIdentity(org.olat.core.id.Identity, boolean, boolean)
	 */
	public List findBGContextsForIdentity(final Identity identity, final boolean defaultContexts, final boolean nonDefaultContexts) {
		final DB db = DBFactory.getInstance();
		final StringBuilder q = new StringBuilder();
		q.append(" select context from org.olat.group.context.BGContextImpl as context,");
		q.append(" org.olat.basesecurity.SecurityGroupMembershipImpl as secgmemb");
		q.append(" where context.ownerGroup = secgmemb.securityGroup");
		q.append(" and secgmemb.identity = :identity");

		final boolean checkDefault = defaultContexts != nonDefaultContexts;
		if (checkDefault) {
			q.append(" and context.defaultContext = :isDefault");
		}
		final DBQuery query = db.createQuery(q.toString());
		query.setEntity("identity", identity);
		if (checkDefault) {
			query.setBoolean("isDefault", defaultContexts ? true : false);
		}

		return query.list();
	}

	/**
	 * @see org.olat.group.context.BGContextManager#findOLATResourcesForBGContext(org.olat.group.context.BGContext)
	 */
	public List findOLATResourcesForBGContext(final BGContext bgContext) {
		final DB db = DBFactory.getInstance();
		final String q = " select bgcr.resource from org.olat.group.context.BGContext2Resource as bgcr where bgcr.groupContext = :context";
		final DBQuery query = db.createQuery(q);
		query.setEntity("context", bgContext);
		return query.list();
	}

	/**
	 * @see org.olat.group.context.BGContextManager#findRepositoryEntriesForBGContext(org.olat.group.context.BGContext)
	 */
	public List findRepositoryEntriesForBGContext(final BGContext bgContext) {
		final List resources = findOLATResourcesForBGContext(bgContext);
		final List entries = new ArrayList();
		for (final Iterator iter = resources.iterator(); iter.hasNext();) {
			final OLATResource resource = (OLATResource) iter.next();
			final RepositoryEntry entry = RepositoryManager.getInstance().lookupRepositoryEntry(resource, false);
			if (entry == null) {
				throw new AssertException("No repository entry found for olat resource with TYPE::" + resource.getResourceableTypeName() + " ID::"
						+ resource.getResourceableId());
			} else {
				entries.add(entry);
			}
		}
		return entries;
	}

	/**
	 * @see org.olat.group.context.BGContextManager#removeBGContextFromResource(org.olat.group.context.BGContext, org.olat.resource.OLATResource)
	 */
	public void removeBGContextFromResource(final BGContext bgContext, final OLATResource resource) {
		// 1) delete references for this resource
		final String q = " from org.olat.group.context.BGContext2Resource as bgcr where bgcr.groupContext = ? and bgcr.resource = ?";
		DBFactory.getInstance().delete(q, new Object[] { bgContext.getKey(), resource.getKey() }, new Type[] { Hibernate.LONG, Hibernate.LONG });
		// 2) update course context list in this course resource
		if (resource.getResourceableTypeName().equals(CourseModule.getCourseTypeName())) {
			final ICourse course = CourseFactory.loadCourse(resource);
			course.getCourseEnvironment().getCourseGroupManager().initGroupContextsList();
		} else if (resource.getResourceableTypeName().equals("junitcourse")) {
			// do nothing when in junit test mode
		} else {
			throw new AssertException("Currently only course resources allowed in resource to context relations.");
		}

		Tracing.logAudit("Removed Business Group Context from OLATResource " + resource.toString(), bgContext.toString(), this.getClass());
	}

	/**
	 * @see org.olat.group.context.BGContextManager#loadBGContext(org.olat.group.context.BGContext)
	 */
	public BGContext loadBGContext(final BGContext bgContext) {
		return (BGContext) DBFactory.getInstance().loadObject(bgContext);
	}

}