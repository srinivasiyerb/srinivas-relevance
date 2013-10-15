/**
 * OLAT - Online Learning and Training<br />
 * http://www.olat.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br />
 * you may not use this file except in compliance with the License.<br />
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,<br />
 * software distributed under the License is distributed on an "AS IS" BASIS, <br />
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br />
 * See the License for the specific language governing permissions and <br />
 * limitations under the License.
 * <p>
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br />
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.instantMessaging.rosterandchat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.jivesoftware.smack.packet.Presence;
import org.olat.core.commons.scheduler.JobWithDB;
import org.olat.core.util.SessionInfo;
import org.olat.core.util.UserSession;
import org.olat.instantMessaging.ClientManager;
import org.olat.instantMessaging.InstantMessagingClient;
import org.olat.instantMessaging.InstantMessagingModule;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Description:<br />
 * changes the IM presence to away if a user has not clicked in the olat gui for a certain time
 * <P>
 * Initial Date: 18.05.2007 <br />
 * 
 * @author guido
 */
public class ChangePresenceJob extends JobWithDB {

	/**
	 * TODO:gs:a make this properties writable by jmx or by setting it via admin gui that we can kick out users to free resources if needed
	 */
	private long idleWaitTime;
	private static long autoLogOutCutTime;
	private static boolean initializedAutoLogOutCutTime = false;

	@Override
	public void executeWithDB(final JobExecutionContext arg0) throws JobExecutionException {
		try {
			final Long timeNow = System.currentTimeMillis();

			final List<UserSession> authUserSessions = new ArrayList<UserSession>(UserSession.getAuthenticatedUserSessions());

			for (final Iterator<UserSession> iter = authUserSessions.iterator(); iter.hasNext();) {
				final UserSession session = iter.next();
				long lastAccessTime = 0;
				String username = null;
				InstantMessagingClient client = null;
				boolean isWebDav = false;
				try {
					lastAccessTime = session.getSessionInfo().getLastClickTime();
					username = session.getIdentity().getName();
					isWebDav = session.getSessionInfo().isWebDAV();
				} catch (final Exception e) {
					log.info("Tried to get LastAccessTime from session that became in the meantime invalid", e.toString());
				}
				if (!isWebDav) { // leave webdav sessions untouched

					if (InstantMessagingModule.isEnabled()) {
						// avoid reconnection of dead or duplicate sessions
						final ClientManager mgr = InstantMessagingModule.getAdapter().getClientManager();
						if (username != null && mgr.hasActiveInstantMessagingClient(username)) {
							client = mgr.getInstantMessagingClient(username);
						}
						if (log.isDebug()) {
							if (client != null) {
								log.debug("Fetched im client via mangager. Connections status is - connected=" + client.isConnected() + " for user: "
										+ client.getUsername());
							} else {
								log.debug("Could not fetch IM client for user: " + username);
							}
						}
					}

					if (session != null) {
						if ((timeNow - lastAccessTime) > autoLogOutCutTime) {
							try {
								// brasato:::: since tomcat doesn't do this in its normal
								// session invalidation process,
								// (since polling), we must do it manually.
								// But: it does not belong here (must work also without IM, but
								// with polling)
								// brasato:: alternative: instead of a job, generate a timer
								// which rechecks if clicked within 5 mins ??

								// TODO: that presence change stuff should also be moved to the
								// IMManager...
								// invalidation triggers dispose of controller chain and closes
								// IM
								// and removes IM client
								try {
									final SessionInfo sessionInfo = session.getSessionInfo();
									if (sessionInfo != null) {
										final HttpSession session2 = sessionInfo.getSession();
										if (session2 != null) {
											session2.invalidate();
										}
									}
								} catch (final IllegalStateException ise) {
									// ignore since session already closed by user, see javadoc:
									// "    Throws: java.lang.IllegalStateException - if this method is called on an already invalidated session"
								}
								log.audit("Automatically logged out idle user: " + username);
							} catch (final Exception e) {
								log.warn("Error while automatically logging out user: " + username, e);
							}
						} else if ((timeNow - lastAccessTime) > idleWaitTime) {
							/**
							 * user is back on track send presence message available to inform
							 */
							if (InstantMessagingModule.isEnabled()) {
								if ((client != null && client.isConnected())
										&& (client.getPresenceMode() == Presence.Mode.available || client.getPresenceMode() == Presence.Mode.chat)) {
									client.sendPresenceAutoStatusIdle();
									// inform the GUI
									InstantMessagingModule.getAdapter().getClientManager().sendPresenceEvent(Presence.Type.available, username);
									if (log.isDebug()) {
										log.debug("change presence for user " + client.getUsername() + " to away.");
									}
								}
							}
						} else {
							if (InstantMessagingModule.isEnabled()) {
								/**
								 * uses makes a brake last access was more than five minutes ago so set instant messaging presence to away
								 */
								if ((client != null && client.isConnected())
										&& (client.getPresenceMode() == Presence.Mode.away || client.getPresenceMode() == Presence.Mode.xa)) {
									client.sendPresence(Presence.Type.available, null, 0, Presence.Mode.valueOf(client.getRecentPresenceStatusMode()));
									// inform the GUI
									InstantMessagingModule.getAdapter().getClientManager().sendPresenceEvent(Presence.Type.available, username);
									if (log.isDebug()) {
										log.debug("change presence for user " + client.getUsername() + " back to recent presence.");
									}
								}
								// check if we can switch user back to available mode
							}
						}
					}
				}
			}
			authUserSessions.clear();
		} catch (final Throwable th) {
			// Presence Job can not fail - as it is responsible for kicking out people.
			log.error("PReSeNCe JoB FaiLuRe!! continue!!!", th);
		}
	}

	public long getAutoLogOutCutTime() {
		return autoLogOutCutTime;
	}

	/**
	 * @@org.springframework.jmx.export.metadata.ManagedAttribute (description="Set the time a user gets automatically logged out even if he still has an active session")
	 * @param autoLogOutCutTime
	 */
	public void setInitialAutoLogOutCutTime(final long initialAutoLogOutCutTime) {
		// Initialize static value only once because setInitialAutoLogOutCutTime is called at each ChangePresenceJob-run
		if (!initializedAutoLogOutCutTime) {
			autoLogOutCutTime = initialAutoLogOutCutTime;
			// init GlobalSessionTimeout used by tomcat for session-timeout
			final int sessionTimeoutSeconds = Math.round((ChangePresenceJob.getAutoLogOutCutTimeValue() / 1000));
			UserSession.setGlobalSessionTimeout(sessionTimeoutSeconds);
			initializedAutoLogOutCutTime = true;
		}
	}

	/**
	 * @@org.springframework.jmx.export.metadata.ManagedAttribute (description="Set the time a users instant messaging status gets automatically switched into idle
	 *                                                            status")
	 * @param idleWaitTime
	 */
	public void setIdleWaitTime(final long idleWaitTime) {
		this.idleWaitTime = idleWaitTime;
	}

	/**
	 * Static method to set autoLogOutCutTime used by AdminModule. Set non-static methot
	 * 
	 * @param newValue
	 */
	public static void setAutoLogOutCutTimeValue(final long newValue) {
		autoLogOutCutTime = newValue;
	}

	public static long getAutoLogOutCutTimeValue() {
		return autoLogOutCutTime;
	}

}
