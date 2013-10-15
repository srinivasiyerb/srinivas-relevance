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

package org.olat.catalog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.bookmark.BookmarkManager;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.configuration.Initializable;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.controllers.EntryChangedEvent;
import org.olat.user.UserDataDeletable;

/**
 * Description: <br>
 * The CatalogManager is responsible for the persistence of CatalogEntries. Further it provides access methods to retrieve structures of CatalogEntries for a given
 * CatalogEntry, e.g. children, catalog entries which act as roots, delete subcategory structure.
 * <p>
 * Moreover it also has access methods providing all catalog entries referencing a given repository entry.
 * <p>
 * The CatalogManager also provides hooks used by the repository entry manager to signal changes on a repository entry which might have changed. Such changes can invoke
 * the removal from the catalog, e.g. restricting access, deleting a repository entry. Date: 2005/10/14 13:21:42<br>
 * 
 * @author Felix Jost
 */
public class CatalogManager extends BasicManager implements UserDataDeletable, Initializable {
	private static CatalogManager catalogManager;
	/**
	 * Default value for the catalog root <code>CATALOGROOT</code>
	 */
	public static final String CATALOGROOT = "CATALOG ROOT";
	/**
	 * Resource identifyer for catalog entries
	 */
	public static final String CATALOGENTRY = "CatalogEntry";

	/**
	 * [spring]
	 * 
	 * @param userDeletionManager
	 */
	private CatalogManager(final UserDeletionManager userDeletionManager) {
		// singleton
		userDeletionManager.registerDeletableUserData(this);
		catalogManager = this;
	}

	/**
	 * @return Return singleton instance
	 */
	public static CatalogManager getInstance() {
		return catalogManager;
	}

	/**
	 * @return transient catalog entry object
	 */
	public CatalogEntry createCatalogEntry() {
		return new CatalogEntryImpl();
	}

	/**
	 * Children of this CatalogEntry as a list of CatalogEntries
	 * 
	 * @param ce
	 * @return List of catalog entries that are childern entries of given entry
	 */
	public List<CatalogEntry> getChildrenOf(final CatalogEntry ce) {
		final String sqlQuery = "select cei from org.olat.catalog.CatalogEntryImpl as cei " + " where cei.parent = :parent order by cei.name ";
		final DBQuery dbQuery = DBFactory.getInstance().createQuery(sqlQuery);
		dbQuery.setEntity("parent", ce);
		// cache this query
		dbQuery.setCacheable(true);
		return dbQuery.list();
	}

	/**
	 * Returns a list catalog categories
	 * 
	 * @return List of catalog entries of type CatalogEntry.TYPE_NODE
	 */
	public List<CatalogEntry> getAllCatalogNodes() {
		final String sqlQuery = "select cei from org.olat.catalog.CatalogEntryImpl as cei " + " where cei.type= :type ";
		final DBQuery dbQuery = DBFactory.getInstance().createQuery(sqlQuery);
		dbQuery.setInteger("type", CatalogEntry.TYPE_NODE);
		// cache this query
		dbQuery.setCacheable(true);
		return dbQuery.list();
	}

	/**
	 * Checks if the given catalog entry has any child of the given type. The query will be cached.
	 * 
	 * @param ce
	 * @param type CatalogEntry.TYPE_LEAF or CatalogEntry.TYPE_NODE
	 * @return true: entry has at least one child of type node
	 */
	public boolean hasChildEntries(final CatalogEntry ce, final int type) {
		final String sqlQuery = "select count(cei) from org.olat.catalog.CatalogEntryImpl as cei " + " where cei.parent = :parent AND cei.type= :type ";
		final DBQuery dbQuery = DBFactory.getInstance().createQuery(sqlQuery);
		dbQuery.setEntity("parent", ce);
		dbQuery.setInteger("type", type);
		// cache this query
		dbQuery.setCacheable(true);
		final List res = dbQuery.list();
		final Long cntL = (Long) res.get(0);
		return (cntL.longValue() > 0);
	}

	/**
	 * Filters all catalog entries of type leaf that are owned by the given user
	 * 
	 * @param identity
	 * @param catalogEntries List of catalog entries to be filtered
	 * @return List of catalog entries
	 */
	public List filterOwnedLeafs(final Identity identity, final List catalogEntries) {
		final List ownedEntries = new ArrayList();
		final BaseSecurity securityManager = BaseSecurityManager.getInstance();

		final Iterator iter = catalogEntries.iterator();
		while (iter.hasNext()) {
			final CatalogEntry cate = (CatalogEntry) iter.next();
			if (cate.getType() == CatalogEntry.TYPE_LEAF) {
				final RepositoryEntry repe = cate.getRepositoryEntry();
				final SecurityGroup secGroup = repe.getOwnerGroup();
				if (securityManager.isIdentityInSecurityGroup(identity, secGroup)) {
					ownedEntries.add(cate);
				}
			}
		}
		return ownedEntries;
	}

	/**
	 * Reload the given catalog entry from db or from hibernate second level cache
	 * 
	 * @param catalogEntry
	 * @return reloaded catalog entry
	 */
	public CatalogEntry loadCatalogEntry(final CatalogEntry catalogEntry) {
		return (CatalogEntry) DBFactory.getInstance().loadObject(catalogEntry);
	}

	/**
	 * Load the catalog entry by the given ID
	 * 
	 * @param catEntryId
	 * @return
	 */
	public CatalogEntry loadCatalogEntry(final Long catEntryId) {
		return (CatalogEntry) DBFactory.getInstance().loadObject(CatalogEntryImpl.class, catEntryId);
	}

	/**
	 * persist catalog entry
	 * 
	 * @param ce
	 */
	public void saveCatalogEntry(final CatalogEntry ce) {
		DBFactory.getInstance().saveObject(ce);
	}

	/**
	 * update catalog entry on db
	 * 
	 * @param ce
	 */
	public void updateCatalogEntry(final CatalogEntry ce) {
		DBFactory.getInstance().updateObject(ce);
	}

	public List entriesWithOwnerFrom(final Identity owner, final CatalogEntry ce) {
		final List retVal = new ArrayList();
		/*
		 * 
		 */
		findEntriesOf(owner, ce, retVal);
		return retVal;
	}

	private void findEntriesOf(final Identity owner, final CatalogEntry root, final List entries) {
		/*
		 * check if node is owned by identity
		 */
		final BaseSecurity secMgr = BaseSecurityManager.getInstance();
		final SecurityGroup owners = root.getOwnerGroup();
		if (owners != null && secMgr.isIdentityInSecurityGroup(owner, owners)) {
			entries.add(root);
		}
		/*
		 * check subtree, by visit children first strategy
		 */
		final List children = getChildrenOf(root);
		final Iterator iter = children.iterator();
		while (iter.hasNext()) {
			final CatalogEntry nextCe = (CatalogEntry) iter.next();
			findEntriesOf(owner, nextCe, entries);
		}
	}

	/**
	 * delete a catalog entry and a potentially referenced substructure from db. Be aware of how to use this deletion, as all the referenced substructure is deleted.
	 * 
	 * @param ce
	 */
	public void deleteCatalogEntry(final CatalogEntry ce) {
		getLogger().debug("deleteCatalogEntry start... ce=" + ce);
		final BaseSecurity securityManager = BaseSecurityManager.getInstance();
		if (ce.getType() == CatalogEntry.TYPE_LEAF) {
			// delete catalog entry, then delete owner group
			final SecurityGroup owner = ce.getOwnerGroup();
			DBFactory.getInstance().deleteObject(ce);
			if (owner != null) {
				getLogger().debug("deleteCatalogEntry case_1: delete owner-group=" + owner);
				securityManager.deleteSecurityGroup(owner);
			}
		} else {
			final List secGroupsToBeDeleted = new ArrayList();
			// FIXME pb: the transaction must also include the deletion of the security
			// groups. Why not using this method as a recursion and seperating the
			// deletion of the ce and the groups by collecting the groups? IMHO there
			// are not less db queries. This way the code is much less clear, e.g. the method
			// deleteCatalogSubtree does not really delete the subtree, it leaves the
			// security groups behind. I would preferre to have one delete method that
			// deletes its children first by calling itself on the children and then deletes
			// itself ant its security group. The nested transaction that occures is actually
			// not a problem, the DB object can handel this.
			deleteCatalogSubtree(ce, secGroupsToBeDeleted);
			// after deleting all entries, delete all secGroups corresponding
			for (final Iterator iter = secGroupsToBeDeleted.iterator(); iter.hasNext();) {
				final SecurityGroup grp = (SecurityGroup) iter.next();
				getLogger().debug("deleteCatalogEntry case_2: delete groups of deleteCatalogSubtree grp=" + grp);
				securityManager.deleteSecurityGroup(grp);
			}
		}
		getLogger().debug("deleteCatalogEntry END");
	}

	/**
	 * recursively delete the structure starting from the catalog entry.
	 * 
	 * @param ce
	 */
	private void deleteCatalogSubtree(CatalogEntry ce, final List secGroupsToBeDeleted) {
		final DB db = DBFactory.getInstance();

		final List children = getChildrenOf(ce);
		final Iterator iter = children.iterator();
		while (iter.hasNext()) {
			final CatalogEntry nextCe = (CatalogEntry) iter.next();
			deleteCatalogSubtree(nextCe, secGroupsToBeDeleted);
		}
		ce = (CatalogEntry) db.loadObject(ce);
		// mark owner group for deletion.
		final SecurityGroup owner = ce.getOwnerGroup();
		if (owner != null) {
			secGroupsToBeDeleted.add(owner);
		}
		// delete user bookmarks
		final OLATResourceable ores = createOLATResouceableFor(ce);
		BookmarkManager.getInstance().deleteAllBookmarksFor(ores);
		// delete catalog entry itself
		db.deleteObject(ce);
	}

	/**
	 * find all catalog entries referencing the supplied Repository Entry.
	 * 
	 * @param repoEntry
	 * @return List of catalog entries
	 */
	public List getCatalogEntriesReferencing(final RepositoryEntry repoEntry) {
		final String sqlQuery = "select cei from " + " org.olat.catalog.CatalogEntryImpl as cei " + " ,org.olat.repository.RepositoryEntry as re "
				+ " where cei.repositoryEntry = re AND re.key= :reKey ";
		final DBQuery dbQuery = DBFactory.getInstance().createQuery(sqlQuery);
		dbQuery.setCacheable(true);
		dbQuery.setLong("reKey", repoEntry.getKey().longValue());
		final List resSet = dbQuery.list();
		return resSet;
	}

	/**
	 * find all catalog categorie that the given repository entry is a child of
	 * 
	 * @param repoEntry
	 * @return List of catalog entries
	 */
	public List getCatalogCategoriesFor(final RepositoryEntry repoEntry) {
		final String sqlQuery = "select distinct parent from org.olat.catalog.CatalogEntryImpl as parent " + ", org.olat.catalog.CatalogEntryImpl as cei "
				+ ", org.olat.repository.RepositoryEntry as re " + " where cei.repositoryEntry = re " + " and re.key= :reKey " + " and cei.parent = parent ";
		final DBQuery dbQuery = DBFactory.getInstance().createQuery(sqlQuery);
		dbQuery.setCacheable(true);
		dbQuery.setLong("reKey", repoEntry.getKey().longValue());
		final List resSet = dbQuery.list();
		return resSet;
	}

	/**
	 * find catalog entries by supplied name
	 * 
	 * @param name
	 * @return List of catalog entries
	 */
	public List getCatalogEntriesByName(final String name) {
		final String sqlQuery = "select cei from org.olat.catalog.CatalogEntryImpl as cei where cei.name = :name";
		final DBQuery dbQuery = DBFactory.getInstance().createQuery(sqlQuery);
		dbQuery.setString("name", name);
		dbQuery.setCacheable(true);
		return dbQuery.list();

	}

	/**
	 * Find catalog entries for certain identity
	 * 
	 * @param name
	 * @return List of catalog entries
	 */
	public List getCatalogEntriesOwnedBy(final Identity identity) {
		final String sqlQuery = "select cei from org.olat.catalog.CatalogEntryImpl as cei inner join fetch cei.ownerGroup, "
				+ " org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi" + " where " + " cei.ownerGroup = sgmsi.securityGroup and" + " sgmsi.identity = :identity";
		final DBQuery dbQuery = DBFactory.getInstance().createQuery(sqlQuery);
		dbQuery.setEntity("identity", identity);
		dbQuery.setCacheable(true);
		return dbQuery.list();

	}

	/**
	 * add a catalog entry to the specified parent
	 * 
	 * @param parent
	 * @param newEntry
	 */
	public void addCatalogEntry(final CatalogEntry parent, final CatalogEntry newEntry) {
		getLogger().debug("addCatalogEntry parent=" + parent);
		newEntry.setParent(parent);
		getLogger().debug("addCatalogEntry newEntry=" + newEntry);
		getLogger().debug("addCatalogEntry newEntry.getOwnerGroup()=" + newEntry.getOwnerGroup());
		saveCatalogEntry(newEntry);
	}

	/**
	 * Find all CatalogEntries which can act as catalog roots. Frankly speaking only one is found up to now, but for later stages one can think of getting more such
	 * roots. An empty list indicates an error.
	 * 
	 * @return List of catalog entries
	 */
	public List getRootCatalogEntries() {
		final String sqlQuery = "select cei from org.olat.catalog.CatalogEntryImpl as cei where cei.parent is null";
		final DBQuery dbQuery = DBFactory.getInstance().createQuery(sqlQuery);
		dbQuery.setCacheable(true);
		return dbQuery.list();
	}

	/**
	 * init called on module start-up
	 */
	@Override
	public void init() {
		final List roots = getRootCatalogEntries();
		if (roots.isEmpty()) { // not initialized yet
			// TODO inject via spring
			final BaseSecurity secMgr = BaseSecurityManager.getInstance();
			/*
			 * copy a snapshot of olatAdmins into catalogAdmins do not put secMgr.findSecurityGroupByName(Constants.GROUP_ADMIN) directly into a CatalogEntry!!
			 */
			final SecurityGroup olatAdmins = secMgr.findSecurityGroupByName(Constants.GROUP_ADMIN);
			final List olatAdminIdents = secMgr.getIdentitiesOfSecurityGroup(olatAdmins);
			final SecurityGroup catalogAdmins = secMgr.createAndPersistSecurityGroup();
			for (int i = 0; i < olatAdminIdents.size(); i++) {
				secMgr.addIdentityToSecurityGroup((Identity) olatAdminIdents.get(i), catalogAdmins);
			}
			/*
			 * start with something called CATALOGROOT, you can rename it to whatever name you like later as OLATAdmin
			 */
			// parent == null -> no parent -> I am a root node.
			saveCatEntry(CATALOGROOT, null, CatalogEntry.TYPE_NODE, catalogAdmins, null, null);
			DBFactory.getInstance(false).intermediateCommit();
		}
	}

	private CatalogEntry saveCatEntry(final String name, final String desc, final int type, final SecurityGroup ownerGroup, final RepositoryEntry repoEntry,
			final CatalogEntry parent) {
		final CatalogEntry ce = createCatalogEntry();
		ce.setName(name);
		ce.setDescription(desc);
		ce.setOwnerGroup(ownerGroup);
		ce.setRepositoryEntry(repoEntry);
		ce.setParent(parent);
		ce.setType(type);
		saveCatalogEntry(ce);
		return ce;
	}

	/**
	 * Move the given catalog entry to the new parent
	 * 
	 * @param toBeMovedEntry
	 * @param newParentEntry return true: success; false: failure
	 */
	public boolean moveCatalogEntry(CatalogEntry toBeMovedEntry, CatalogEntry newParentEntry) {
		final CatalogManager cm = CatalogManager.getInstance();
		// reload current item to prevent stale object modification
		toBeMovedEntry = cm.loadCatalogEntry(toBeMovedEntry);
		newParentEntry = cm.loadCatalogEntry(newParentEntry);
		// check that the new parent is not a leaf
		if (newParentEntry.getType() == CatalogEntry.TYPE_LEAF) { return false; }
		// check that the new parent is not a child of the to be moved entry
		CatalogEntry tempEntry = newParentEntry;
		while (tempEntry != null) {
			if (tempEntry.getKey().equals(toBeMovedEntry.getKey())) {
				// ups, the new parent is within the to be moved entry - abort
				return false;
			}
			tempEntry = tempEntry.getParent();
		}
		// set new parent and save
		toBeMovedEntry.setParent(newParentEntry);
		cm.updateCatalogEntry(toBeMovedEntry);
		return true;
	}

	/**
	 * @param repositoryEntry
	 */
	public void resourceableDeleted(final RepositoryEntry repositoryEntry) {
		// if a repository entry gets deleted, the referencing Catalog Entries gets
		// retired to
		getLogger().debug("sourceableDeleted start... repositoryEntry=" + repositoryEntry);
		final List references = getCatalogEntriesReferencing(repositoryEntry);
		if (references != null && !references.isEmpty()) {
			for (int i = 0; i < references.size(); i++) {
				deleteCatalogEntry((CatalogEntry) references.get(i));
			}
		}
	}

	/**
	 * Remove identity as owner of catalog-entry. If there is no other owner, the olat-administrator (define in spring config) will be added as owner.
	 * 
	 * @see org.olat.user.UserDataDeletable#deleteUserData(org.olat.core.id.Identity)
	 */
	@Override
	public void deleteUserData(final Identity identity, final String newDeletedUserName) {
		// Remove as owner
		final List catalogEntries = getCatalogEntriesOwnedBy(identity);
		for (final Iterator iter = catalogEntries.iterator(); iter.hasNext();) {
			final CatalogEntry catalogEntry = (CatalogEntry) iter.next();

			BaseSecurityManager.getInstance().removeIdentityFromSecurityGroup(identity, catalogEntry.getOwnerGroup());
			if (BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(catalogEntry.getOwnerGroup()) == 0) {
				// This group has no owner anymore => add OLAT-Admin as owner
				BaseSecurityManager.getInstance().addIdentityToSecurityGroup(UserDeletionManager.getInstance().getAdminIdentity(), catalogEntry.getOwnerGroup());
				Tracing.logInfo("Delete user-data, add Administrator-identity as owner of catalogEntry=" + catalogEntry.getName(), this.getClass());
			}
		}
		Tracing.logDebug("All owner entries in catalog deleted for identity=" + identity, this.getClass());
	}

	/**
	 * checks if the given catalog entry is within one of the given catalog categories
	 * 
	 * @param toBeCheckedEntry
	 * @param entriesList
	 * @return
	 */
	public boolean isEntryWithinCategory(final CatalogEntry toBeCheckedEntry, final List<CatalogEntry> entriesList) {
		CatalogEntry tempEntry = toBeCheckedEntry;
		while (tempEntry != null) {
			if (PersistenceHelper.listContainsObjectByKey(entriesList, tempEntry)) { return true; }
			tempEntry = tempEntry.getParent();
		}
		return false;
	}

	/**
	 * Create a volatile OLATResourceable for a given catalog entry that can be used to create a bookmark to this catalog entry
	 * 
	 * @param currentCatalogEntry
	 * @return
	 */
	public OLATResourceable createOLATResouceableFor(final CatalogEntry currentCatalogEntry) {
		if (currentCatalogEntry == null) { return null; }
		return new OLATResourceable() {
			@Override
			public Long getResourceableId() {
				return new Long(currentCatalogEntry.getKey());
			}

			@Override
			public String getResourceableTypeName() {
				return CATALOGENTRY;
			}
		};
	}

	/**
	 * @param repositoryEntry
	 */
	public void updateReferencedRepositoryEntry(final RepositoryEntry repositoryEntry) {
		final RepositoryEntry reloaded = RepositoryManager.getInstance().lookupRepositoryEntry(repositoryEntry.getKey());
		reloaded.setDisplayname(repositoryEntry.getDisplayname());
		reloaded.setDescription(repositoryEntry.getDescription());
		RepositoryManager.getInstance().updateRepositoryEntry(reloaded);
		// inform anybody interested about this change
		final MultiUserEvent modifiedEvent = new EntryChangedEvent(reloaded, EntryChangedEvent.MODIFIED_DESCRIPTION);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(modifiedEvent, reloaded);
	}

}
