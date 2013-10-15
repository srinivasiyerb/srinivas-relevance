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

package org.olat.instantMessaging;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.taskExecutor.TaskExecutorManager;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.LogDelegator;
import org.olat.instantMessaging.groupchat.GroupChatManagerController;
import org.olat.instantMessaging.rosterandchat.ConnectToServerTask;

/**
 * Description: <br />
 * Instant Messaging Client class based on the open source library SMACK (www.igniterealtime.org). It provides connection to the IM-server, sends presence packets,
 * listens to messages and subscription requests. Methods that mention the velocity rendering stuff are uses by template files that render the html-response that for they
 * dont have references in the java source code.
 * <P>
 * 
 * @version Initial Date: 14.10.2004
 * @author Guido Schnider
 */

public class InstantMessagingClient extends LogDelegator {
	// username is olat global unique username
	private final String username;
	// password is auto generated and only used for instant messaging
	private final String password;
	protected XMPPConnection connection = null;
	private long connectionTimestamp = 0;
	private Roster roster = null;
	// null means 'not connected'
	private Presence.Mode presenceMode = null;
	private String statusMsg = " ";
	// the jabber id like username@olat.ch
	private final String jabberServer;
	protected List subscribedUsers = new ArrayList();
	private static final String OLATBUDDIES = "OLAT-Buddies";
	protected boolean collaborationDisabled = false;
	private boolean isConnected;
	private boolean showOfflineBuddies = false;
	private boolean showGroupsInRoster = false;
	private String recentStatusMode = Presence.Mode.available.toString();
	private GroupChatManagerController groupChatManagerCtrl;
	private final String defaultRosterStatus;

	/**
	 * @param username
	 * @param password
	 */
	protected InstantMessagingClient(final String username, final String password) {
		this.username = username;
		this.password = password;
		jabberServer = InstantMessagingModule.getAdapter().getConfig().getServername();

		final Identity ident = BaseSecurityManager.getInstance().findIdentityByName(username);
		final ImPreferences prefs = ImPrefsManager.getInstance().loadOrCreatePropertiesFor(ident);
		this.defaultRosterStatus = prefs.getRosterDefaultStatus();
		reconnect(true);
	}

	/**
	 * if connections fails upon server crash or other incidents, a new Thread gets created and tries to reconnect after a waiting period
	 * 
	 * @param tryImmediately
	 */
	protected void reconnect(final boolean tryImmediately) {
		if (tryImmediately) {
			doConnect();
		}
	}

	/**
	 * connect to the JabberServer
	 */
	protected void doConnect() {
		// Debug enables a java window with all traffic between client
		// and server.
		XMPPConnection.DEBUG_ENABLED = false;
		// check if currently used im client is already connected
		// (reusing clients from previous session)
		connection = new XMPPConnection(InstantMessagingModule.getConnectionConfiguration());

		// connecting to server is done by thread pool

		logDebug("Connecting to server by thread pool with user: " + username);

		TaskExecutorManager.getInstance().runTask(new ConnectToServerTask(this));
		connectionTimestamp = System.currentTimeMillis();

	}

	/**
	 * change jabber status. Example: sendPresencePacket(Presence.Type.AVAILABLE, "at meeting...", 1, Presence.Mode.AWAY);
	 * 
	 * @param type
	 * @param status
	 * @param priority
	 * @param mode
	 */
	public void sendPresence(final Presence.Type type, String status, final int priority, final Presence.Mode mode) {
		// get rid of "&" because they break xml packages!
		if (status != null) {
			status = status.replaceAll("&", "&amp;");
		}
		if (connection == null || !connection.isConnected()) { return; }
		if (collaborationDisabled) { return; }

		setStatus(mode);
		final Presence presence = new Presence(type);
		if (status == null) {
			if (mode == Presence.Mode.available) {
				status = InstantMessagingConstants.PRESENCE_MODE_AVAILABLE;
			} else if (mode == Presence.Mode.away) {
				status = InstantMessagingConstants.PRESENCE_MODE_AWAY;
			} else if (mode == Presence.Mode.chat) {
				status = InstantMessagingConstants.PRESENCE_MODE_CHAT;
			} else if (mode == Presence.Mode.dnd) {
				status = InstantMessagingConstants.PRESENCE_MODE_DND;
			} else if (mode == Presence.Mode.xa) {
				status = InstantMessagingConstants.PRESENCE_MODE_XAWAY;
			}
			presence.setStatus(status);
		} else {
			presence.setStatus(status);
		}
		setStatusMsg(presence.getStatus());
		// setting prio when type == unavailable causes error on IM server
		if (presence.getType() == Presence.Type.available) {
			presence.setPriority(priority);
		}
		if (mode != null) {
			presence.setMode(mode);
		}
		try {
			connection.sendPacket(presence);
		} catch (final RuntimeException ex) {
			logWarn("Error while trying to send Instant Messaging packet for user: " + username + " .Errormessage: ", ex);
		}
	}

	/**
	 * send a presence packet "available" with a certain mode e.g. "away" to all buddies
	 * 
	 * @param mode
	 */
	public void sendPresenceAvailable(final Presence.Mode mode) {
		sendPresence(Presence.Type.available, null, 0, mode);
	}

	/**
	 * send a presence packet "unavailable" to all buddies
	 */
	public void sendPresenceUnavailable() {
		sendPresence(Presence.Type.unavailable, null, 0, null);
	}

	/**
	 * By adding this method (right now added to the constructor) we do have auto subscription. All subscribe packets get automatically answered by a subscribed packet.
	 */
	public void addSubscriptionListener() {
		final PacketFilter filter = new PacketTypeFilter(Presence.class);
		connection.createPacketCollector(filter);
		final PacketListener myListener = new PacketListener() {
			@Override
			public void processPacket(final Packet packet) {
				final Presence presence = (Presence) packet;
				if (presence.getType() == Presence.Type.subscribe) {
					Presence response = new Presence(Presence.Type.subscribe);
					response.setTo(presence.getFrom());
					// System.out.println("subscribed to: "+presence.getFrom());
					connection.sendPacket(response);
					// ask also for subscription
					if (!subscribedUsers.contains(presence.getFrom())) {
						response = null;
						response = new Presence(Presence.Type.subscribe);
						response.setTo(presence.getFrom());
						connection.sendPacket(response);
						// update the roster with the new user
						final RosterPacket rosterPacket = new RosterPacket();
						rosterPacket.setType(IQ.Type.SET);
						final RosterPacket.Item item = new RosterPacket.Item(presence.getFrom(), parseName(presence.getFrom()));
						item.addGroupName(OLATBUDDIES);
						item.setItemType(RosterPacket.ItemType.both);
						// item.setItemStatus(RosterPacket.ItemStatus.fromString());
						rosterPacket.addRosterItem(item);
						connection.sendPacket(rosterPacket);
					}
				}
				if (presence.getType() == Presence.Type.subscribe) {
					subscribedUsers.add(presence.getFrom());
				}
			}
		};
		connection.addPacketListener(myListener, filter);
	}

	/**
	 * For unsubscription we have to create a packet like: <iq type="set" id="ab7ba" > <query xmlns="jabber:iq:roster"> <item subscription="remove" jid="guido@localhost"
	 * /> </query> </iq>
	 * 
	 * @param uname a valid username
	 */
	protected void removeSubscription(final String uname) {
		final RosterPacket rosterPacket = new RosterPacket();
		rosterPacket.setType(IQ.Type.SET);
		final RosterPacket.Item item = new RosterPacket.Item(uname + "@" + jabberServer, uname);
		item.setItemType(RosterPacket.ItemType.remove);
		rosterPacket.addRosterItem(item);
		try {
			connection.sendPacket(rosterPacket);
		} catch (final RuntimeException e) {
			logWarn("Error while trying to send Instant Messaging packet.", e);
		}
	}

	/**
	 * @return Returns the jid.
	 */
	public String getJid() {
		final String jid = getChatUsername() + "@" + jabberServer;
		return jid;
	}

	/**
	 * @return Returns the status.
	 */
	protected String getStatus() {
		if (presenceMode != null) { return presenceMode.toString(); }
		return Presence.Type.unavailable.toString();
	}

	public Presence.Mode getPresenceMode() {
		return presenceMode;
	}

	/**
	 * The status to set.
	 * 
	 * @param status
	 */
	protected void setStatus(final Presence.Mode status) {
		this.presenceMode = status;
	}

	/**
	 * @return Returns the roster.
	 */
	public Roster getRoster() {
		return roster;
	}

	/**
	 * @param roster
	 */
	public void setRoster(final Roster roster) {
		this.roster = roster;
	}

	/**
	 * Close the connection to the server
	 */
	public void closeConnection(final boolean closeSynchronously) {
		// Set isConnected to false first since connection.close triggers an
		// XMPPConnListener.connectionClosed() event which would result in
		// in a cyclic call of this close method.
		isConnected = false;
		final XMPPConnection connectionToClose = connection;
		final Runnable connectionCloseRunnable = new Runnable() {
			@Override
			public void run() {
				try {
					if (connectionToClose != null && connectionToClose.isConnected()) {
						connectionToClose.disconnect();
					}
				} catch (final RuntimeException e) {
					logWarn("Error while trying to close instant messaging connection", e);
				}
			}
		};
		if (closeSynchronously) {
			connectionCloseRunnable.run();
		} else {
			TaskExecutorManager.getInstance().runTask(connectionCloseRunnable);
		}
	}

	/**
	 * Ask an other online user to subscribe to their roster
	 * 
	 * @param uname
	 */
	protected void subscribeToUser(final String uname) {
		final Presence presence = new Presence(Presence.Type.subscribe);
		presence.setTo(uname + "@" + jabberServer);
		try {
			connection.sendPacket(presence);
		} catch (final RuntimeException e) {
			logWarn("Error while trying to send Instant Messaging packet.", e);
		}

	}

	/**
	 * Sends a subscription request to the username answers are handled by the method
	 * 
	 * @see org.olat.instantMessaging.InstantMessagingClient#addSubscriptionListener()
	 * @param uname
	 * @param groupname
	 */
	protected void subscribeToUser(final String uname, final String groupname) {
		final Presence presence = new Presence(Presence.Type.subscribe);
		presence.setTo(uname + "@" + jabberServer);
		try {
			connection.sendPacket(presence);

			final RosterPacket rosterPacket = new RosterPacket();
			rosterPacket.setType(IQ.Type.SET);
			final RosterPacket.Item item = new RosterPacket.Item(uname + "@" + jabberServer, uname);
			item.addGroupName(groupname);
			item.setItemType(RosterPacket.ItemType.both);
			rosterPacket.addRosterItem(item);
			connection.sendPacket(rosterPacket);
		} catch (final RuntimeException e) {
			logWarn("Error while trying to send Instant Messaging packet.", e);
		}
	}

	/**
	 * @param xmppAddress jabber jid like guido@swissjabber.org
	 * @return returns just the name "guido" without the rest
	 */
	protected String parseName(final String xmppAddress) {
		if (xmppAddress == null) { return null; }
		final int atIndex = xmppAddress.indexOf("@");
		if (atIndex <= 0) { return ""; }
		return xmppAddress.substring(0, atIndex);
	}

	/**
	 * @param xmppAddressWithRessource like guido@swissjabber.org/office
	 * @return treurns the jid without the ressource like guido@swissjabber.org
	 */
	protected String parseJid(final String xmppAddressWithRessource) {
		if (xmppAddressWithRessource == null) { return null; }
		final int atIndex = xmppAddressWithRessource.indexOf("/");
		if (atIndex <= 0) {
			// if no "/" is found we pass back the full adress
			return xmppAddressWithRessource;
		}
		return xmppAddressWithRessource.substring(0, atIndex);
	}

	/**
	 * When doing a test, collaboration by IM should be disabled
	 * 
	 * @param reason The reason why this user is not allowd to chat e.g. doing test By setting the third param to 10 all messages should be send to this client even if
	 *            other clients are up. So we have full control over them.
	 */
	protected void disableCollaboration(final String reason) {
		recentStatusMode = this.getStatus();
		sendPresence(Presence.Type.unavailable, reason, 10, Presence.Mode.dnd);
		// trigger an event on the im controller, to update the status icon
		InstantMessagingModule.getAdapter().getClientManager().sendPresenceEvent(Presence.Type.available, username);
		collaborationDisabled = true;
	}

	/**
	 * enable collaboration
	 */
	protected void enableCollaboration() {
		collaborationDisabled = false;

		if (recentStatusMode.equals(Presence.Type.unavailable.toString())) {
			sendPresence(Presence.Type.unavailable, null, 0, null);
			InstantMessagingModule.getAdapter().getClientManager().sendPresenceEvent(Presence.Type.unavailable, username);
		} else {
			// Set back the priority to normal level.
			sendPresence(Presence.Type.available, null, 0, Presence.Mode.valueOf(recentStatusMode));
			// trigger an event on the im controller, to update the status icon
			InstantMessagingModule.getAdapter().getClientManager().sendPresenceEvent(Presence.Type.available, username);
		}
	}

	public boolean isChatDisabled() {
		return collaborationDisabled;
	}

	public String getRecentPresenceStatusMode() {
		return recentStatusMode;
	}

	/**
	 * Used by Velocity renderer
	 * 
	 * @return a String representing the online buddies out of the number of total buddies
	 */
	public String buddyCountOnline() {
		int onlineBuddyEntries = connection.getRoster().getEntryCount();
		final int allBuddies = onlineBuddyEntries;
		for (final Iterator l = connection.getRoster().getEntries().iterator(); l.hasNext();) {
			final RosterEntry entry = (RosterEntry) l.next();
			final Presence presence = connection.getRoster().getPresence(entry.getUser());
			if (presence.getType() == Presence.Type.unavailable) {
				onlineBuddyEntries--;
			}
		}
		// final string looks like e.g. "(3/5)"
		final StringBuilder sb = new StringBuilder(10);
		sb.append("(");
		sb.append(onlineBuddyEntries);
		sb.append("/");
		sb.append(allBuddies);
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Used by Velocity renderer
	 * 
	 * @param groupname
	 * @return a String representing the online buddies out of the number of total buddies for a single group like (3/5)
	 */
	protected String buddyCountOnlineForGroup(final String groupname) {
		final RosterGroup rosterGroup = connection.getRoster().getGroup(groupname);
		int buddyEntries = rosterGroup.getEntryCount();
		final int allBuddies = buddyEntries;
		for (final Iterator I = rosterGroup.getEntries().iterator(); I.hasNext();) {
			final RosterEntry entry = (RosterEntry) I.next();
			final Presence presence = connection.getRoster().getPresence(entry.getUser());
			if (presence.getType() == Presence.Type.unavailable) {
				buddyEntries--;
			}
		}
		// final string looks like e.g. "(3/5)"
		final StringBuilder sb = new StringBuilder(10);
		sb.append("(");
		sb.append(buddyEntries);
		sb.append("/");
		sb.append(allBuddies);
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Used by Velocity renderer
	 * 
	 * @param jid
	 * @return get a presence for a specific user
	 */
	protected String getUserPresence(final String jid) {
		final Presence presence = connection.getRoster().getPresence(jid);
		String imageName = "offline"; // default
		// mode == null is equals available!!
		if (presence.getMode() == null && presence.getType() == Presence.Type.available) {
			imageName = Presence.Mode.available.toString();
		}
		if (presence.getMode() != null) {
			imageName = presence.getMode().toString();
		}
		return imageName;
	}

	/**
	 * @return Returns the password.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * @return Returns the username.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * @return
	 */
	public String getFullName() {
		final Identity identity = BaseSecurityManager.getInstance().findIdentityByName(username);
		return identity != null ? identity.getUser().getProperty(UserConstants.FIRSTNAME, null) + " " + identity.getUser().getProperty(UserConstants.LASTNAME, null) : "";
	}

	/**
	 * @return
	 */
	public String getEmail() {
		final Identity identity = BaseSecurityManager.getInstance().findIdentityByName(username);
		return identity != null ? identity.getUser().getProperty(UserConstants.EMAIL, null) : "";
	}

	/**
	 * in case multiple olat instances use one si, InstantMessagingClient.class);ngle jabber server, username needs to be distinguished by instance id
	 * 
	 * @return returns the chat-username used on jabber server
	 */
	public String getChatUsername() {
		return InstantMessagingModule.getAdapter().getIMUsername(username);
	}

	/**
	 * @return Returns true when user is connected to server
	 */
	public boolean isConnected() {
		return isConnected;
	}

	public void setIsConnected(final boolean isConnected) {
		this.isConnected = isConnected;
	}

	/**
	 * TODO:gs used by velocity, change connected client list to client helper object
	 * 
	 * @return Returns the statusMsg.
	 */
	protected String getStatusMsg() {
		return statusMsg;
	}

	/**
	 * Free text to add more details to a specific status like status "away" and msg "eating lunch until 2pm..."
	 * 
	 * @param statusMsg The statusMsg to set.
	 */
	protected void setStatusMsg(String statusMsg) {
		if (statusMsg == null) {
			statusMsg = "";
		}
		this.statusMsg = statusMsg;
	}

	/**
	 * @return Returns the showOfflineBuddies.
	 */
	protected boolean getShowOfflineBuddies() {
		return showOfflineBuddies;
	}

	/**
	 * @param showOfflineBuddies The showOfflineBuddies to set.
	 */
	protected void setShowOfflineBuddies(final boolean showOfflineBuddies) {
		this.showOfflineBuddies = showOfflineBuddies;
	}

	/**
	 * TODO:gs used by velocity, change connected client list to client helper object
	 * 
	 * @return minutes the user is online
	 */
	protected String getOnlineTime() {
		final long diff = System.currentTimeMillis() - connectionTimestamp;
		return new Integer((int) diff / 1000 / 60).toString();
	}

	/**
	 * @return Returns the showGroupsInRoster.
	 */
	protected boolean isShowGroupsInRoster() {
		return showGroupsInRoster;
	}

	/**
	 * @param showGroupsInRoster The showGroupsInRoster to set.
	 */
	protected void setShowGroupsInRoster(final boolean showGroupsInRoster) {
		this.showGroupsInRoster = showGroupsInRoster;
	}

	public XMPPConnection getConnection() {
		return connection;
	}

	public String getServerName() {
		return jabberServer;
	}

	public void sendPresenceAutoStatusIdle() {
		recentStatusMode = getPresenceMode().toString();
		// TODO:gs:a translate
		sendPresence(Presence.Type.available, "auto away", 0, Presence.Mode.away);

	}

	/**
	 * set the per user instance of the group chat manager
	 * 
	 * @param groupChatManagerController
	 */
	public void setGroupChatManager(final GroupChatManagerController groupChatManagerCtrl) {
		this.groupChatManagerCtrl = groupChatManagerCtrl;

	}

	/**
	 * access the per user instance of the group chat manager
	 * 
	 * @return
	 */
	public GroupChatManagerController getGroupChatManagerController() {
		return this.groupChatManagerCtrl;
	}

	/**
	 * @return status from IM preferences or default wich is "available"
	 */
	public String getDefaultRosterStatus() {
		return defaultRosterStatus;
	}

}