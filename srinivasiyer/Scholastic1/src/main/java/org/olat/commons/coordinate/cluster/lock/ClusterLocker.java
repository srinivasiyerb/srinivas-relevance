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
package org.olat.commons.coordinate.cluster.lock;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.control.Event;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.DBRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.SignOnOffEvent;
import org.olat.core.util.UserSession;
import org.olat.core.util.coordinate.LockEntry;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.coordinate.LockResultImpl;
import org.olat.core.util.coordinate.Locker;
import org.olat.core.util.coordinate.PersistentLockManager;
import org.olat.core.util.coordinate.Syncer;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.event.EventBus;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.resource.OresHelper;
import org.olat.resource.lock.pessimistic.PessimisticLockManager;

/**
 * Description:<br>
 * the cluster implementation for the Locker. It uses a database table oc_lock to perform the locking.
 * <P>
 * Initial Date: 21.09.2007 <br>
 * 
 * @author Felix Jost, http://www.goodsolutions.ch
 */
// Must be abstract because Spring configuration of method 'getPersistentLockManager' :
// to avoid circular reference method lookup is used for dependecy injection of persistent lock manager
public abstract class ClusterLocker implements Locker, GenericEventListener {
	protected OLog log = Tracing.createLoggerFor(this.getClass());
	PersistentLockManager plm = null;

	private Syncer syncer;
	private EventBus eventBus;
	private final ClusterLockManager clusterLockManager;

	/**
	 * [used by spring]
	 */
	protected ClusterLocker(final ClusterLockManager clusterLockManager) {
		this.clusterLockManager = clusterLockManager;
	}

	/**
	 * [used by spring]
	 */
	public void init() {
		// called by spring.
		// register for sign-off event in order to release all locks for that user
		eventBus.registerFor(this, null, OresHelper.createOLATResourceableType(UserSession.class));
	}

	// cluster:::::: on init of olat system, clear all locks?? but only the one from node in question?

	@Override
	public LockResult acquireLock(final OLATResourceable ores, final Identity requestor, final String locksubkey) {
		final String asset = OresHelper.createStringRepresenting(ores, locksubkey);

		final LockResult res = syncer.doInSync(ores, new SyncerCallback<LockResult>() {
			@Override
			public LockResult execute() {
				LockResultImpl lres;
				LockImpl li = clusterLockManager.findLock(asset);
				if (li == null) { // fine, we can lock it
					li = clusterLockManager.createLockImpl(asset, requestor);
					clusterLockManager.saveLock(li);
					final LockEntry le = new LockEntry(li.getAsset(), li.getCreationDate().getTime(), li.getOwner());
					lres = new LockResultImpl(true, le);
				} else {
					// already locked by a user.
					// if that user is us, we can reacquire it
					final LockEntry le = new LockEntry(li.getAsset(), li.getCreationDate().getTime(), li.getOwner());
					if (requestor.getName().equals(li.getOwner().getName())) {
						// that's us -> success (asset, owner is the same, and we leave creationdate to when the lock was originally acquired, not when it was reacquired.
						lres = new LockResultImpl(true, le);
					} else {
						lres = new LockResultImpl(false, le);
					}
				}
				return lres;
			}
		});

		return res;
	}

	/**
	 * receives all sign on / sign off events so it can release locks of users which have or are logged off
	 * 
	 * @see org.olat.core.util.event.GenericEventListener#event(org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final Event event) {
		final SignOnOffEvent se = (SignOnOffEvent) event;
		if (!se.isSignOn() && se.isEventOnThisNode()) {
			// it is a "logout" event - we are only interested in logout events
			// and it is from our VM => only release all locks from within one VM
			final String identName = se.getIdentityName();
			// release all locks held by the identity that has just logged out.
			// (assuming one user has only one session (logged in with one browser only): otherwise (as in singlevm, too)
			// since the lock is reentrant, a lock could be freed while a session still is in a locked workflow (2x lock and then once freed)
			try {
				clusterLockManager.releaseAllLocksFor(identName);
			} catch (final DBRuntimeException dbEx) {
				log.warn("releaseAllLocksFor failed, close session and try it again for identName=" + identName);
				// TODO: 2010-04-23 Transactions [eglis]: OLAT-4318: this rollback has possibly unwanted
				// side effects, as it rolls back any changes with this transaction during this
				// event handling. Nicer would be to be done in the outmost-possible place, e.g. dofire()
				DBFactory.getInstance().rollbackAndCloseSession();
				// try again with new db-session
				log.info("try again to release all locks for identName=" + identName);
				clusterLockManager.releaseAllLocksFor(identName);
				log.info("Done, released all locks for identName=" + identName);
			}
		}
	}

	@Override
	public boolean isLocked(final OLATResourceable ores, final String locksubkey) {
		final String asset = OresHelper.createStringRepresenting(ores, locksubkey);
		final LockImpl li = clusterLockManager.findLock(asset);
		return (li != null);
	}

	@Override
	public void releaseLock(final LockResult lockResult) {
		// if the lock has not been acquired, then nothing is to be released -
		// return silently to make cleaning up easier
		if (!lockResult.isSuccess()) { return; }
		releaseLockEntry(((LockResultImpl) lockResult).getLockEntry());
	}

	/**
	 * for admin purposes only. Release a lockentry directly. Use 'releaseLock' as method to release a lock.
	 * 
	 * @param lock release this lockentry
	 */
	@Override
	public void releaseLockEntry(final LockEntry lockEntry) {
		final String asset = lockEntry.getKey();
		final Identity releaseRequestor = lockEntry.getOwner();

		// cluster:: change to useage with syncer, but we don't have the olatresourceable yet
		PessimisticLockManager.getInstance().findOrPersistPLock(asset);

		final LockImpl li = clusterLockManager.findLock(asset);
		if (li == null) {
			// do nothing - since this lock may have been one that was cleared when restarting the vm
		} else {
			// check that entry was previously locked by the same user that now wants to release the lock.
			final Identity ownwer = li.getOwner();
			if (releaseRequestor.getName().equals(ownwer.getName())) {
				// delete the lock
				clusterLockManager.deleteLock(li);
			} else {
				throw new AssertException("cannot release lock since the requestor of the release (" + releaseRequestor.getName() + ") is not the owner ("
						+ ownwer.getName() + ") of the lock (" + asset + ")");
			}
		}
	}

	@Override
	public List<LockEntry> adminOnlyGetLockEntries() {
		final List<LockImpl> li = clusterLockManager.getAllLocks();
		final List<LockEntry> res = new ArrayList<LockEntry>(li.size());
		for (final LockImpl impl : li) {
			res.add(new LockEntry(impl.getAsset(), impl.getCreationDate().getTime(), impl.getOwner()));
		}
		return res;
	}

	@Override
	public LockResult aquirePersistentLock(final OLATResourceable ores, final Identity ident, final String locksubkey) {
		final LockResult res = syncer.doInSync(ores, new SyncerCallback<LockResult>() {
			@Override
			public LockResult execute() {
				final LockResult ares = getPersistentLockManager().aquirePersistentLock(ores, ident, locksubkey);
				return ares;
			}
		});
		return res;
	}

	@Override
	public void releasePersistentLock(final LockResult lockResult) {
		// cluster_ok: since a certain LockResult can only be from one user/session that previously acquired the lock
		// if the lock has not been acquired, do nothing
		if (!lockResult.isSuccess()) { return; }

		// delegate to the concrete implementation
		getPersistentLockManager().releasePersistentLock(lockResult);
	}

	/**
	 * [used by spring]
	 * 
	 * @param syncer
	 */
	public void setSyncer(final Syncer syncer) {
		this.syncer = syncer;
	}

	/**
	 * [used by spring]
	 */
	public void setEventBus(final EventBus eventBus) {
		this.eventBus = eventBus;
	}

}
