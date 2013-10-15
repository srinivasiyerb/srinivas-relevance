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

package org.olat.group.area;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.type.Type;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.coordinate.SyncerExecutor;
import org.olat.group.BusinessGroup;
import org.olat.group.context.BGContext;

/**
 * Description:<BR/>
 * Implementation of the business group area manager
 * <P/>
 * Initial Date: Aug 24, 2004
 * 
 * @author gnaegi
 */
public class BGAreaManagerImpl extends BasicManager implements BGAreaManager {

	private static BGAreaManager INSTANCE;
	static {
		INSTANCE = new BGAreaManagerImpl();
	}

	/**
	 * @return singleton instance
	 */
	public static BGAreaManager getInstance() {
		return INSTANCE;
	}

	private BGAreaManagerImpl() {
		// no public constructor
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#createAndPersistBGAreaIfNotExists(java.lang.String, java.lang.String, org.olat.group.context.BGContext)
	 */
	// o_clusterOK by:cg synchronized on groupContext's olatresourceable
	@Override
	public BGArea createAndPersistBGAreaIfNotExists(final String areaName, final String description, final BGContext groupContext) {
		final BGArea createdBGArea = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(groupContext, new SyncerCallback<BGArea>() {
			@Override
			public BGArea execute() {
				final BGArea area = findBGArea(areaName, groupContext);
				if (area == null) { return createAndPersistBGArea(areaName, description, groupContext); }
				return null;
			}
		});
		return createdBGArea;
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#copyBGAreasOfBGContext(org.olat.group.context.BGContext, org.olat.group.context.BGContext)
	 */
	// o_clusterOK by:cg ; must be synchronized too ? => not 100% sure,
	@Override
	public Map copyBGAreasOfBGContext(final BGContext origBgContext, final BGContext targetBgContext) {
		final List origAreas = findBGAreasOfBGContext(origBgContext);
		final Map areas = new HashMap();
		final Iterator iterator = origAreas.iterator();
		while (iterator.hasNext()) {
			final BGArea origArea = (BGArea) iterator.next();
			final BGArea targetArea = createAndPersistBGArea(origArea.getName(), origArea.getDescription(), targetBgContext);
			areas.put(origArea, targetArea);
		}
		return areas;
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#findBGArea(java.lang.String, org.olat.group.context.BGContext)
	 */
	@Override
	public BGArea findBGArea(final String areaName, final BGContext groupContext) {
		final DB db = DBFactory.getInstance();
		final String q = "select area from org.olat.group.area.BGAreaImpl area " + " where area.name = :areaName" + " and area.groupContext = :context";
		final DBQuery query = db.createQuery(q);
		query.setString("areaName", areaName);
		query.setEntity("context", groupContext);
		final List areas = query.list();
		if (areas.size() == 0) {
			return null;
		} else if (areas.size() > 1) { throw new OLATRuntimeException(BGAreaManagerImpl.class, "findBGArea(" + areaName
				+ ") returned more than one row for BGContext with key " + groupContext.getKey(), null); }
		return (BGAreaImpl) areas.get(0);
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#updateBGArea(org.olat.group.area.BGArea)
	 */
	// o_clusterOK by:cg synchronized
	@Override
	public BGArea updateBGArea(final BGArea area) {
		// look if an area with such a name does already exist in this context
		final BGContext groupContext = area.getGroupContext();

		final BGArea updatedBGArea = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(groupContext, new SyncerCallback<BGArea>() {
			@Override
			public BGArea execute() {
				final BGArea douplicate = findBGArea(area.getName(), groupContext);
				if (douplicate == null) {
					// does not exist, so just update it
					DBFactory.getInstance().updateObject(area);
					return area;
				} else if (douplicate.getKey().equals(area.getKey())) {
					// name already exists, found the same object (name didn't change)
					// need to copy description (that has changed) and update the object.
					// if we updated area at this place we would get a hibernate exception
					douplicate.setDescription(area.getDescription());
					DBFactory.getInstance().updateObject(douplicate);
					return douplicate;
				}
				return null; // nothing updated
			}
		});
		return updatedBGArea;
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#deleteBGArea(org.olat.group.area.BGArea)
	 */
	// o_clusterOK by:cg must be synchronized too
	@Override
	public void deleteBGArea(final BGArea area) {
		final BGContext groupContext = area.getGroupContext();
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(groupContext, new SyncerExecutor() {
			@Override
			public void execute() {
				final BGArea reloadArea = findBGArea(area.getName(), groupContext);
				if (reloadArea != null) {
					// 1) delete all area - group relations
					deleteBGtoAreaRelations(reloadArea);
					// 2) delete area itself
					DBFactory.getInstance().deleteObject(reloadArea);
					Tracing.logAudit("Deleted Business Group Area", reloadArea.toString(), this.getClass());
				} else {
					Tracing.logAudit("Business Group Area was already deleted", area.toString(), this.getClass());
				}
			}
		});
	}

	/**
	 * Deletes all business group to area relations from the given business group
	 * 
	 * @param group
	 */
	@Override
	public void deleteBGtoAreaRelations(final BusinessGroup group) {
		final String q = " from org.olat.group.area.BGtoAreaRelationImpl as bgarel where bgarel.businessGroup = ?";
		DBFactory.getInstance().delete(q, new Object[] { group.getKey() }, new Type[] { Hibernate.LONG });
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#addBGToBGArea(org.olat.group.BusinessGroup, org.olat.group.area.BGArea)
	 */
	@Override
	public void addBGToBGArea(final BusinessGroup group, final BGArea area) {
		final BGtoAreaRelation bgAreaRel = new BGtoAreaRelationImpl(area, group);
		DBFactory.getInstance().saveObject(bgAreaRel);
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#removeBGFromArea(org.olat.group.BusinessGroup, org.olat.group.area.BGArea)
	 */
	@Override
	public void removeBGFromArea(final BusinessGroup group, final BGArea area) {
		removeBGFromArea(group.getKey(), area.getKey());
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#findBusinessGroupsOfArea(org.olat.group.area.BGArea)
	 */
	@Override
	public List findBusinessGroupsOfArea(final BGArea area) {
		final String q = " select grp from org.olat.group.BusinessGroupImpl as grp," + " org.olat.group.area.BGtoAreaRelationImpl as bgarel"
				+ " where bgarel.businessGroup = grp" + " and bgarel.groupArea = :area";
		final DBQuery query = DBFactory.getInstance().createQuery(q);
		query.setEntity("area", area);
		final List result = query.list();
		return result;
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#findBusinessGroupsOfAreaAttendedBy(org.olat.core.id.Identity, java.lang.String, org.olat.group.context.BGContext)
	 */
	@Override
	public List findBusinessGroupsOfAreaAttendedBy(final Identity identity, final String areaName, final BGContext context) {
		final String query = "select bgi from " + "  org.olat.group.BusinessGroupImpl as bgi " + ", org.olat.basesecurity.SecurityGroupMembershipImpl as sgmi"
				+ ", org.olat.group.area.BGtoAreaRelationImpl as bgarel" + ", org.olat.group.area.BGAreaImpl as area" + " where area.name = :name "
				+ " and bgarel.businessGroup = bgi" + " and bgarel.groupArea = area" + " and bgi.partipiciantGroup = sgmi.securityGroup"
				+ " and sgmi.identity = :identId" + " and bgi.groupContext = :context";
		final DBQuery dbq = DBFactory.getInstance().createQuery(query);
		dbq.setEntity("identId", identity);
		dbq.setString("name", areaName);
		dbq.setEntity("context", context);
		final List result = dbq.list();
		return result;
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#findBGAreasOfBusinessGroup(org.olat.group.BusinessGroup)
	 */
	@Override
	public List findBGAreasOfBusinessGroup(final BusinessGroup group) {
		final String q = " select area from org.olat.group.area.BGAreaImpl as area," + " org.olat.group.area.BGtoAreaRelationImpl as bgarel "
				+ " where bgarel.groupArea = area" + " and bgarel.businessGroup = :group";
		final DBQuery query = DBFactory.getInstance().createQuery(q);
		query.setEntity("group", group);
		final List result = query.list();
		return result;
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#countBGAreasOfBGContext(org.olat.group.context.BGContext)
	 */
	@Override
	public int countBGAreasOfBGContext(final BGContext groupContext) {
		final String q = " select count(area) from org.olat.group.area.BGAreaImpl area where area.groupContext = :context";
		final DBQuery query = DBFactory.getInstance().createQuery(q);
		query.setEntity("context", groupContext);
		return ((Long) query.list().get(0)).intValue();
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#findBGAreasOfBGContext(org.olat.group.context.BGContext)
	 */
	@Override
	public List findBGAreasOfBGContext(final BGContext groupContext) {
		final String q = " select area from org.olat.group.area.BGAreaImpl area where area.groupContext = :context ";
		final DBQuery query = DBFactory.getInstance().createQuery(q);
		query.setEntity("context", groupContext);
		return query.list();
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#isIdentityInBGArea(org.olat.core.id.Identity, java.lang.String, org.olat.group.context.BGContext)
	 */
	@Override
	public boolean isIdentityInBGArea(final Identity identity, final String areaName, final BGContext groupContext) {
		final String q = " select count(grp) from" + " org.olat.group.BusinessGroupImpl as grp," + " org.olat.group.area.BGAreaImpl as area,"
				+ " org.olat.group.area.BGtoAreaRelationImpl bgarel," + " org.olat.basesecurity.SecurityGroupMembershipImpl as secgmemb" + " where area.name = :name"
				+ " and bgarel.groupArea = area" + " and bgarel.businessGroup = grp" + " and grp.groupContext = :context"
				+ " and ((grp.partipiciantGroup = secgmemb.securityGroup and secgmemb.identity = :id) "
				+ " or (grp.ownerGroup = secgmemb.securityGroup and secgmemb.identity = :id)) ";
		final DBQuery query = DBFactory.getInstance().createQuery(q);
		query.setEntity("id", identity);
		query.setEntity("context", groupContext);
		query.setString("name", areaName);
		query.setCacheable(true);
		final List result = query.list();
		if (result.size() == 0) { return false; }
		return (((Long) result.get(0)).intValue() > 0);
	}

	/**
	 * @see org.olat.group.area.BGAreaManager#reloadArea(org.olat.group.area.BGArea)
	 */
	@Override
	public BGArea reloadArea(final BGArea area) {
		return (BGArea) DBFactory.getInstance().loadObject(area);
	}

	@Override
	public boolean checkIfOneOrMoreNameExistsInContext(final Set<String> allNames, final BGContext bgContext) {
		final String q = " select area from org.olat.group.area.BGAreaImpl area " + "where area.groupContext = :context " + "AND area.name in (:names) ";
		final DBQuery query = DBFactory.getInstance().createQuery(q);
		query.setEntity("context", bgContext);
		query.setParameterList("names", allNames);

		final List result = query.list();
		if (result.size() == 0) { return false; }
		return true;

	}

	/**
	 * Creates an area object and persists the object in the database
	 * 
	 * @param areaName The visible area name
	 * @param description The area description
	 * @param groupContext The group context of this area
	 * @return The new area
	 */
	private BGArea createAndPersistBGArea(final String areaName, final String description, final BGContext groupContext) {
		final BGArea area = new BGAreaImpl(areaName, description, groupContext);
		DBFactory.getInstance().saveObject(area);
		if (area != null) {
			Tracing.logAudit("Created Business Group Area", area.toString(), this.getClass());
		}
		// else no area created, name douplicate
		return area;
	}

	/**
	 * Remove a business group from a business group area. If no such relation exists, the mehthod does nothing.
	 * 
	 * @param businessGroupKey
	 * @param bgAreaKey
	 */
	private void removeBGFromArea(final Long businessGroupKey, final Long bgAreaKey) {
		final String q = " from org.olat.group.area.BGtoAreaRelationImpl as bgarel where bgarel.groupArea.key = ? and bgarel.businessGroup = ?";
		DBFactory.getInstance().delete(q, new Object[] { bgAreaKey, businessGroupKey }, new Type[] { Hibernate.LONG, Hibernate.LONG });
	}

	/**
	 * Deletes all business group to area relations from the given business group area
	 * 
	 * @param area
	 */
	private void deleteBGtoAreaRelations(final BGArea area) {
		final String q = " from org.olat.group.area.BGtoAreaRelationImpl as bgarel where bgarel.groupArea = ?";
		DBFactory.getInstance().delete(q, new Object[] { area.getKey() }, new Type[] { Hibernate.LONG });
	}

}