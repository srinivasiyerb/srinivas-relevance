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

package org.olat.resource;

import java.util.List;

import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseModule;

/**
 * A <b>SecurityResourceManager</b> is
 * 
 * @author Andreas Ch. Kapp
 */
public class OLATResourceManager extends BasicManager {

	private static OLATResourceManager INSTANCE;
	private DB dbInstance;

	/**
	 * @return Singleton
	 */
	public static OLATResourceManager getInstance() {
		return INSTANCE;
	}

	/**
	 * [used by spring]
	 */
	private OLATResourceManager() {
		INSTANCE = this;
	}

	/**
	 * Creates a new OLATResource instance (but does not persist the instance)
	 * 
	 * @param resource
	 * @return OLATResource
	 */
	public OLATResource createOLATResourceInstance(final OLATResourceable resource) {
		return new OLATResourceImpl(resource);
	}

	/**
	 * Creates a new OLATResource instance (but does not persist the instance)
	 * 
	 * @param typeName
	 * @return OLATResource
	 */
	public OLATResource createOLATResourceInstance(final String typeName) {
		final Long id = new Long(CodeHelper.getForeverUniqueID());
		return new OLATResourceImpl(id, typeName);
	}

	/**
	 * Creates a new OLATResource instance (but does not persist the instance)
	 * 
	 * @param aClass
	 * @return OLATResource
	 */
	public OLATResource createOLATResourceInstance(final Class aClass) {
		final String typeName = OresHelper.calculateTypeName(aClass);
		return createOLATResourceInstance(typeName);
	}

	/**
	 * Saves a resource.
	 * 
	 * @param resource
	 * @return True upon success.
	 */
	public void saveOLATResource(final OLATResource resource) {
		if (resource.getResourceableTypeName().length() > 50) { throw new AssertException("OlatResource: type length may not exceed 50 chars"); }
		dbInstance.saveObject(resource);
	}

	/**
	 * Delete an existing resource.
	 * 
	 * @param resource
	 * @return True upon success.
	 */
	public void deleteOLATResource(final OLATResource resource) {
		dbInstance.deleteObject(resource);
	}

	/**
	 * @param resourceable
	 * @return true if resourceable was found and deleted, false if it was not found.
	 */
	public void deleteOLATResourceable(final OLATResourceable resourceable) {
		final OLATResource ores = findResourceable(resourceable);
		if (ores == null) { return; }
		deleteOLATResource(ores);
	}

	/**
	 * Find the OLATResource for the resourceable. If not found, a new OLATResource is created and returned.
	 * 
	 * @param resourceable
	 * @return an OLATResource representing the resourceable.
	 */
	public OLATResource findOrPersistResourceable(final OLATResourceable resourceable) {
		if (resourceable.getResourceableTypeName() == null) { throw new AssertException("typename of olatresourceable can not be null"); }
		// First try to find resourceable without synchronization
		OLATResource ores = findResourceable(resourceable);
		if (ores != null) { return ores; }
		// Second there exists no resourcable => try to find and create(if no exists) in a synchronized block
		// o_clusterOK by:cg
		ores = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(resourceable, new SyncerCallback<OLATResource>() {
			@Override
			public OLATResource execute() {
				logDebug("start synchronized-block in findOrPersistResourceable");
				OLATResource oresSync = findResourceable(resourceable);
				// if not found, persist it.
				if (oresSync == null) {
					if (CourseModule.ORES_TYPE_COURSE.equals(resourceable.getResourceableTypeName())) {
						logInfo("OLATResourceManager - createOLATResourceInstance if not found: " + resourceable.getResourceableTypeName() + " "
								+ resourceable.getResourceableId());
					}
					oresSync = createOLATResourceInstance(resourceable);
					saveOLATResource(oresSync);
				}
				return oresSync;
			}
		});
		return ores;
	}

	/**
	 * Find a resourceanle
	 * 
	 * @param resourceable
	 * @return OLATResource object or null if not found.
	 */
	public OLATResource findResourceable(final OLATResourceable resourceable) {
		final String type = resourceable.getResourceableTypeName();
		if (type == null) { throw new AssertException("typename of olatresourceable must not be null"); }
		final Long id = resourceable.getResourceableId();

		return doQueryResourceable(id, type);
	}

	/**
	 * Find a resourceable
	 * 
	 * @param resourceableId
	 * @return OLATResource object or null if not found.
	 */
	public OLATResource findResourceable(final Long resourceableId, final String resourceableTypeName) {
		return doQueryResourceable(resourceableId, resourceableTypeName);
	}

	private OLATResource doQueryResourceable(Long resourceableId, final String type) {
		if (resourceableId == null) {
			resourceableId = OLATResourceImpl.NULLVALUE;
		}

		final String s = new String("from org.olat.resource.OLATResourceImpl ori where ori.resName = :resname and ori.resId = :resid");
		DBQuery query = null;
		query = dbInstance.createQuery(s);
		query.setString("resname", type);
		query.setLong("resid", resourceableId.longValue());
		query.setCacheable(true);

		final List resources = query.list();
		// if not found, it is an empty list
		if (resources.size() == 0) { return null; }
		return (OLATResource) resources.get(0);
	}

	/**
	 * @param db
	 */
	public void setDbInstance(final DB db) {
		this.dbInstance = db;
	}

}
