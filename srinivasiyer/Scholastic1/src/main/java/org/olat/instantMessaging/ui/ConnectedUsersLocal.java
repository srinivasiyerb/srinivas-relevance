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
package org.olat.instantMessaging.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.UserSession;
import org.olat.core.util.cache.n.CacheWrapper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.instantMessaging.ClientHelper;
import org.olat.instantMessaging.ImPreferences;
import org.olat.instantMessaging.ImPrefsManager;
import org.olat.instantMessaging.InstantMessagingModule;
import org.olat.instantMessaging.syncservice.InstantMessagingSessionItems;

/**
 * Description:<br>
 * TODO: guido Class Description for ConnectedUsersLocal
 * <P>
 * Initial Date: 06.08.2008 <br>
 * 
 * @author guido
 */
public class ConnectedUsersLocal implements InstantMessagingSessionItems {

	OLog log = Tracing.createLoggerFor(this.getClass());
	private CacheWrapper sessionItemsCache;
	private final ImPrefsManager imPrefsManager;

	public ConnectedUsersLocal(final ImPrefsManager imPrefsManager) {
		this.imPrefsManager = imPrefsManager;
	}

	/**
	 * @see org.olat.instantMessaging.AllOnlineUsers#getConnectedUsers()
	 */
	@Override
	public List<ConnectedUsersListEntry> getConnectedUsers(final Identity currentUser) {
		String username;
		if (currentUser != null) {
			username = currentUser.getName();
		} else {
			username = "";
		}
		/**
		 * create a cache for the entries as looping over a few hundred entries need too much time. Every node has its own cache and therefore no need to inform each
		 * other o_clusterOK by guido
		 */
		if (sessionItemsCache == null) {
			synchronized (this) {
				sessionItemsCache = CoordinatorManager.getInstance().getCoordinator().getCacher().getOrCreateCache(this.getClass(), "items");
			}
		}

		final List<ConnectedUsersListEntry> entries = new ArrayList<ConnectedUsersListEntry>();
		final Map<String, Long> lastActivity = new HashMap<String, Long>();
		final Set<String> usernames = InstantMessagingModule.getAdapter().getUsernamesFromConnectedUsers();
		final List<UserSession> authSessions = new ArrayList(UserSession.getAuthenticatedUserSessions());

		for (final Iterator<UserSession> iter = authSessions.iterator(); iter.hasNext();) {
			final UserSession userSession = iter.next();
			long lastAccTime = 0;
			try {
				lastAccTime = userSession.getSessionInfo().getSession().getLastAccessedTime();
				lastActivity.put(userSession.getIdentity().getName(), Long.valueOf(lastAccTime));
			} catch (final RuntimeException e) {
				// getAuthenticatedUserSessions delivers sessions that are sometimes already invalid.
				log.warn("Tried to get LastAccessTime from session that became in the meantime invalid", null);
			}

		}

		for (final Iterator<String> iter = usernames.iterator(); iter.hasNext();) {
			final String olatusername = iter.next();

			ConnectedUsersListEntry entry = (ConnectedUsersListEntry) sessionItemsCache.get(olatusername);
			if (entry != null && !olatusername.equals(username)) {
				entries.add(entry);
				if (log.isDebug()) {
					log.debug("loading item from cache: " + olatusername);
				}

			} else {
				// item not in cache
				Identity identity = UserSession.getSignedOnIdentity(olatusername);
				if (identity != null) {
					identity = (Identity) DBFactory.getInstance().loadObject(identity);
					try {
						final ImPreferences imPrefs = imPrefsManager.loadOrCreatePropertiesFor(identity);
						if ((imPrefs != null)) {
							final ClientHelper clientHelper = new ClientHelper(olatusername, null, null, null);
							entry = new ConnectedUsersListEntry(olatusername, identity.getUser().getPreferences().getLanguage());
							entry.setName(identity.getUser().getProperty(UserConstants.LASTNAME, null));
							entry.setPrename(identity.getUser().getProperty(UserConstants.FIRSTNAME, null));
							entry.setShowAwarenessMessage(imPrefs.isAwarenessVisible());
							entry.setShowOnlineTime(imPrefs.isOnlineTimeVisible());
							entry.setAwarenessMessage(clientHelper.getStatusMsg());
							entry.setInstantMessagingStatus(clientHelper.getStatus());
							entry.setLastActivity(lastActivity.get(olatusername));
							entry.setOnlineTime(clientHelper.getOnlineTime());
							entry.setJabberId(clientHelper.getJid());
							entry.setVisibleToOthers(imPrefs.isVisibleToOthers());
							entries.add(entry);

							// put in cache
							sessionItemsCache.put(olatusername, entry);
						}
					} catch (final AssertException ex) {
						log.warn("Can not load IM-Prefs for identity=" + identity, ex);
					}
				}
			}
		}// end loop
		return entries;
	}

}
