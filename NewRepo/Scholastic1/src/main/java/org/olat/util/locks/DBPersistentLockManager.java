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
package org.olat.util.locks;

import org.hibernate.Hibernate;
import org.hibernate.type.Type;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.coordinate.LockEntry;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.coordinate.LockResultImpl;
import org.olat.core.util.coordinate.PersistentLockManager;
import org.olat.core.util.resource.OresHelper;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;
import org.olat.user.UserDataDeletable;

/**
 * Description:<br>
 * TODO: patrickb Class Description for DBPersistentLockManager
 * <P>
 * Initial Date: 21.06.2006 <br>
 * 
 * @author patrickb
 */
public class DBPersistentLockManager extends BasicManager implements PersistentLockManager, UserDataDeletable {

	private static final String CATEGORY_PERSISTENTLOCK = "o_lock";

	/**
	 * [used by spring]
	 * 
	 * @param userDeletionManager
	 */
	private DBPersistentLockManager(final UserDeletionManager userDeletionManager) {
		userDeletionManager.registerDeletableUserData(this);
	}

	/**
	 * @see org.olat.core.util.locks.PersistentLockManager#aquirePersistentLock(org.olat.core.id.OLATResourceable, org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public LockResult aquirePersistentLock(final OLATResourceable ores, final Identity ident, final String locksubkey) {
		// synchronisation is solved in the LockManager
		LockResult lres;
		final PropertyManager pm = PropertyManager.getInstance();
		final String derivedLockString = OresHelper.createStringRepresenting(ores, locksubkey);
		long aqTime;
		Identity lockOwner;
		boolean success;
		Property p;

		p = pm.findProperty(null, null, null, CATEGORY_PERSISTENTLOCK, derivedLockString);
		if (p == null) {
			// no persistent lock acquired yet
			// save a property: cat = o_lock, key = derivedLockString, Longvalue = key
			// of identity acquiring the lock
			final Property newp = pm.createPropertyInstance(null, null, null, CATEGORY_PERSISTENTLOCK, derivedLockString, null, ident.getKey(), null, null);
			pm.saveProperty(newp);
			aqTime = System.currentTimeMillis();
			lockOwner = ident;
			success = true;
		} else {
			// already acquired, but check on reaquiring
			aqTime = p.getLastModified().getTime();
			final Long lockOwnerKey = p.getLongValue();
			if (ident.getKey().equals(lockOwnerKey)) {
				// reaquire ok
				success = true;
			} else {
				// already locked by an other person
				success = false;
			}
			// FIXME:fj:c find a better way to retrieve information about the
			// lock-holder
			lockOwner = BaseSecurityManager.getInstance().loadIdentityByKey(lockOwnerKey);
		}

		final LockEntry le = new LockEntry(derivedLockString, aqTime, lockOwner);
		lres = new LockResultImpl(success, le);
		return lres;

	}

	/**
	 * @see org.olat.core.util.locks.PersistentLockManager#releasePersistentLock(org.olat.core.util.locks.LockEntry)
	 */
	@Override
	public void releasePersistentLock(final LockResult le) {
		// synchronisation is solved in the LockManager
		final String derivedLockString = ((LockResultImpl) le).getLockEntry().getKey();
		final PropertyManager pm = PropertyManager.getInstance();
		final Property p = pm.findProperty(null, null, null, CATEGORY_PERSISTENTLOCK, derivedLockString);
		if (p == null) { throw new AssertException("could not release lock: no lock in db, " + derivedLockString); }
		final Identity ident = le.getOwner();
		final Long ownerKey = p.getLongValue();
		if (!ownerKey.equals(ident.getKey())) { throw new AssertException("user " + ident.getName() + " cannot release lock belonging to user with key " + ownerKey
				+ " on resourcestring " + derivedLockString); }
		pm.deleteProperty(p);
	}

	/**
	 * Delete all persisting-locks for certain identity.
	 * 
	 * @see org.olat.user.UserDataDeletable#deleteUserData(org.olat.core.id.Identity)
	 */
	@Override
	public void deleteUserData(final Identity identity, final String newDeletedUserName) {
		final String query = "from v in class org.olat.properties.Property where v.category = ? and v.longValue = ?";
		DBFactory.getInstance().delete(query, new Object[] { CATEGORY_PERSISTENTLOCK, identity.getKey() }, new Type[] { Hibernate.STRING, Hibernate.LONG });
		Tracing.logDebug("All db-persisting-locks deleted for identity=" + identity, this.getClass());
	}

}
