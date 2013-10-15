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
package org.olat.instantMessaging.groupchat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.taskExecutor.TaskExecutorManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.htmlheader.jscss.JSAndCSSComponent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.floatingresizabledialog.FloatingResizableDialogController;
import org.olat.core.gui.control.winmgr.JSCommand;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.AssertException;
import org.olat.core.util.event.EventBus;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.nodes.iq.AssessmentEvent;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.instantMessaging.ClientHelper;
import org.olat.instantMessaging.InstantMessaging;
import org.olat.instantMessaging.InstantMessagingEvent;
import org.olat.instantMessaging.InstantMessagingModule;

/**
 * Description:<br />
 * Handles an group chat in an floating window with all events like receiving messages, sending messages and updating an roster with all joined users There are several
 * options how to display or start the chat as it gets used in different places Initial Date: 13.03.2007 <br />
 * 
 * @author guido
 */
public class InstantMessagingGroupChatController extends BasicController implements GenericEventListener {

	private static final String NICKNAME_PREFIX = "anonym_";
	private String NICKNAME_ANONYMOUS;
	private XMPPConnection connection;
	private MultiUserChat muc;
	private final VelocityContainer errorCompact = createVelocityContainer("errorCompact");
	private final VelocityContainer error = createVelocityContainer("error");
	private final VelocityContainer groupchatVC = createVelocityContainer("groupchat");
	private final VelocityContainer groupChatMsgFieldVC = createVelocityContainer("groupChatMsgField");
	private final VelocityContainer summaryCompactVC = createVelocityContainer("summaryCompact");
	private final VelocityContainer rosterVC = createVelocityContainer("roster");
	private final VelocityContainer summaryVC = createVelocityContainer("summary");
	private Panel groupChatMsgPanel;
	private ToggleAnonymousForm toggleAnonymousForm;
	private SendMessageForm sendMessageForm;
	private String roomJID;
	private StringBuilder messageHistory;
	private Link openGroupChatPanel;
	private Link indicateNewMessage;
	private Link openGroupChatPanelButton;
	private Link refresh;
	private final Panel main;
	private Panel roster;
	private GroupChatJoinTask roomJoinTask;
	private boolean chatWindowOpen = false;
	private final Locale locale;
	private FloatingResizableDialogController floatingResizablePanelCtr;
	private final OLATResourceable ores;
	private final Panel chatWindowHolder;
	private final boolean compact;
	private final String roomName;
	private JSAndCSSComponent jsc;
	private final List<String> rosterList = new ArrayList<String>();
	private final boolean anonymousInChatroom;
	private final boolean lazyCreation;
	private boolean initDone;

	private String myFullName;

	private String jsTweakCmd = "";
	private String jsFocusCmd = "";

	private boolean startup = true;
	private final EventBus singleUserEventCenter;
	private List allChats;
	private final OLATResourceable assessmentEventOres;
	private final OLATResourceable assessmentInstanceOres;
	private boolean isInAssessment;

	/**
	 * @param ureq
	 * @param wControl
	 * @param ores
	 * @param roomName
	 * @param fixcsspanel if you want the panel rendered somewhere else to solve css issues add it here otherwise null
	 * @param lazyCreation if true the user does not get joined automatically to the chatRoom
	 */
	protected InstantMessagingGroupChatController(final UserRequest ureq, final WindowControl wControl, final OLATResourceable ores, final String roomName,
			final Panel chatWindowHolder, final boolean compact, final boolean anonymousInChatroom, final boolean lazyCreation) {

		super(ureq, wControl);
		this.chatWindowHolder = chatWindowHolder;
		this.compact = compact;
		if (roomName == null) { throw new AssertException("roomName can not be null"); }
		this.roomName = roomName;
		if (ores == null) { throw new AssertException("olat resourcable can not be null"); }
		this.ores = ores;
		this.locale = ureq.getLocale();
		this.anonymousInChatroom = anonymousInChatroom;
		this.lazyCreation = lazyCreation;

		singleUserEventCenter = ureq.getUserSession().getSingleUserEventCenter();
		assessmentEventOres = OresHelper.createOLATResourceableType(AssessmentEvent.class);
		assessmentInstanceOres = OresHelper.createOLATResourceableType(AssessmentInstance.class);

		singleUserEventCenter.registerFor(this, getIdentity(), assessmentEventOres);
		isInAssessment |= singleUserEventCenter.getListeningIdentityCntFor(assessmentInstanceOres) > 0;

		allChats = (List) ureq.getUserSession().getEntry("chats");
		if (allChats == null) {
			allChats = new ArrayList();
			ureq.getUserSession().putEntry("chats", allChats);
		}

		main = new Panel("main");

		if (lazyCreation) { // show only link to join the groupChat

			openGroupChatPanel = LinkFactory.createCustomLink("participantsCount", "cmd.open.client", "", Link.NONTRANSLATED, summaryCompactVC, this);
			openGroupChatPanel.setCustomDisplayText(translate("click.to.join"));
			openGroupChatPanel.setCustomEnabledLinkCSS("b_toolbox_link");
			openGroupChatPanel.setCustomDisabledLinkCSS("b_toolbox_link");
			openGroupChatPanel.setTooltip(translate(isInAssessment ? "chat.not.available.now" : "click.to.join"), false);
			openGroupChatPanel.registerForMousePositionEvent(true);
			openGroupChatPanel.setEnabled(!isInAssessment);
			main.setContent(summaryCompactVC);
			putInitialPanel(main);
		} else {

			// create controller stuff and join chatRoom immediately
			if (init(ureq)) {
				putInitialPanel(main);
			} else {
				// error case
				putInitialPanel(errorCompact);
			}
		}
	}

	/**
	 * @param ureq
	 */
	private boolean init(final UserRequest ureq) {

		NICKNAME_ANONYMOUS = NICKNAME_PREFIX + (int) Math.rint(Math.random() * getIdentity().getKey());
		connection = InstantMessagingModule.getAdapter().getClientManager().getInstantMessagingClient(getIdentity().getName()).getConnection();

		roomJID = InstantMessagingModule.getAdapter().createChatRoomJID(ores);
		groupChatMsgPanel = new Panel("groupchat");
		roster = new Panel("roster");
		roster.setContent(rosterVC);

		messageHistory = new StringBuilder();
		groupchatVC.put("groupChatMessages", groupChatMsgPanel);

		if (compact) {

			openGroupChatPanel = LinkFactory.createCustomLink("participantsCount", "cmd.open.client", "", Link.NONTRANSLATED, summaryCompactVC, this);
			openGroupChatPanel.setCustomDisplayText(translate("click.to.join"));
			openGroupChatPanel.setTooltip(translate("course.chat.click.to.join"), false);
			openGroupChatPanel.setCustomEnabledLinkCSS("b_toolbox_link");
			openGroupChatPanel.setCustomDisabledLinkCSS("b_toolbox_link");
			openGroupChatPanel.registerForMousePositionEvent(true);
			openGroupChatPanel.setEnabled(!isInAssessment);
			main.setContent(summaryCompactVC);
		} else {
			openGroupChatPanelButton = LinkFactory.createButton("openChat", summaryVC, this);
			openGroupChatPanelButton.registerForMousePositionEvent(true);
			openGroupChatPanelButton.setEnabled(!isInAssessment);
			summaryVC.put("roster", roster);
			main.setContent(summaryVC);
		}

		groupChatMsgPanel.setContent(groupChatMsgFieldVC);
		groupChatMsgFieldVC.contextPut("id", this.hashCode());

		// create form for username toggle
		toggleAnonymousForm = new ToggleAnonymousForm(ureq, getWindowControl());
		listenTo(toggleAnonymousForm);

		// toggle form only if logged in anonymous
		if (anonymousInChatroom) {
			groupchatVC.put("toggleSwitch", toggleAnonymousForm.getInitialComponent());
		}

		// create form for msg sending
		sendMessageForm = new SendMessageForm(ureq, getWindowControl());
		listenTo(sendMessageForm);
		groupchatVC.put("sendMessageForm", sendMessageForm.getInitialComponent());

		refresh = LinkFactory.createCustomLink("refresh", "cmd.refresh", "", Link.NONTRANSLATED, groupchatVC, this);
		refresh.setCustomEnabledLinkCSS("b_small_icon o_instantmessaging_refresh_icon");
		refresh.setTitle(getTranslator().translate("im.refresh"));

		final Link refreshList = LinkFactory.createButtonXSmall("im.refresh", summaryVC, this);

		jsc = new JSAndCSSComponent("intervall2", this.getClass(), null, null, false, null, InstantMessagingModule.getIDLE_POLLTIME());
		groupchatVC.put("checkfordirtycomponents", jsc);

		groupChatMsgFieldVC.contextPut("groupChatMessages", "");

		final boolean ajaxOn = getWindowControl().getWindowBackOffice().getWindowManager().isAjaxEnabled();
		groupchatVC.contextPut("isAjaxMode", Boolean.valueOf(ajaxOn));
		summaryVC.contextPut("isAjaxMode", Boolean.valueOf(ajaxOn));

		setChatStartable(false);

		if (connection != null && connection.isConnected()) {
			try {
				muc = new MultiUserChat(connection, roomJID);
				if (anonymousInChatroom) {
					roomJoinTask = new GroupChatJoinTask(ores, muc, connection, roomJID, NICKNAME_ANONYMOUS, sanitizeRoomName(roomName), this);
				} else {
					roomJoinTask = new GroupChatJoinTask(ores, muc, connection, roomJID, getIdentity().getName(), sanitizeRoomName(roomName), this);
					rosterVC.setDirty(true);
				}

				TaskExecutorManager.getInstance().runTask(roomJoinTask);
			} catch (final IllegalStateException e) {
				logWarn("Error while trying to create group chat room for user" + getIdentity().getName() + " and course resource: +ores", e);
			}

		} else {
			return false;
		}

		myFullName = getFullUserName(getIdentity().getName());

		initDone = true;
		return true;
	}

	/**
	 * clean room name from ampersands
	 * 
	 * @param room
	 * @return
	 */
	private String sanitizeRoomName(final String room) {
		if (room.contains("&")) { return room.replaceAll("&", "&amp;"); }
		return room;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {

		if (muc != null && muc.isJoined() && connection.isConnected()) {
			try {

				muc.leave();
				final PacketListener msgListener = roomJoinTask.getMessageListener();
				if (msgListener != null) {
					muc.removeMessageListener(msgListener);
				}
				final PacketListener pListener = roomJoinTask.getParticipationsListener();
				if (pListener != null) {
					muc.removeParticipantListener(pListener);
				}
				muc = null;

			} catch (final Exception e) {
				logWarn("Error while leaving multiuserchat:", e);
			}

		}

		if (chatWindowOpen) {
			getWindowControl().getWindowBackOffice().sendCommandTo(getCloseCommand());
			allChats.remove(this);
			singleUserEventCenter.fireEventToListenersOf(new MultiUserEvent("ChatWindowClosed"), OresHelper.createOLATResourceableType(InstantMessaging.class));
		}
	}

	public JSCommand getCloseCommand() {
		final String w = floatingResizablePanelCtr.getPanelName();
		final StringBuilder sb = new StringBuilder();
		sb.append("try{");
		sb.append("Ext.getCmp('").append(w).append("').purgeListeners();");
		sb.append("Ext.getCmp('").append(w).append("').close();");
		sb.append("Ext.getCmp('").append(w).append("').distroy();");
		sb.append("}catch(e){}");
		return new JSCommand(sb.toString());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {

		if (lazyCreation && !initDone) {
			init(ureq);
		}

		// offer refresh button for non ajax mode
		final boolean ajaxOn = getWindowControl().getWindowBackOffice().getWindowManager().isAjaxEnabled();
		groupchatVC.contextPut("isAjaxMode", Boolean.valueOf(ajaxOn));
		summaryVC.contextPut("isAjaxMode", Boolean.valueOf(ajaxOn));

		if ((muc != null && muc.isJoined()) || lazyCreation) {
			if (source == openGroupChatPanel || source == openGroupChatPanelButton || source == indicateNewMessage) {
				int x = 0;
				int y = 0;
				if (source == openGroupChatPanel) {
					x = openGroupChatPanel.getOffsetX() - 450;
					y = openGroupChatPanel.getOffsetY() + 30;
					if (x == -450 && y == 30) {
						x = 300;
						y = 300;
					} // selenium does not send xy coordinates -> set panel somewhere visible
				} else if (source == openGroupChatPanelButton) {
					x = openGroupChatPanelButton.getOffsetX();
					y = openGroupChatPanelButton.getOffsetY();
				} else if (source == indicateNewMessage) {
					x = indicateNewMessage.getOffsetX() - 550;
					y = indicateNewMessage.getOffsetY();
				}

				if (!chatWindowOpen) {
					setChatStartable(false);
					removeAsListenerAndDispose(floatingResizablePanelCtr);
					floatingResizablePanelCtr = new FloatingResizableDialogController(ureq, getWindowControl(), groupchatVC, translate("course.groupchat") + " "
							+ roomNameShort(roomName), 550, 300, x, y, roster, translate("groupchat.roster"), true, false, true, "chat_" + ores.getResourceableId()
							+ "_win");
					listenTo(floatingResizablePanelCtr);

					groupchatVC.contextPut("panelName", floatingResizablePanelCtr.getPanelName());

					final String pn = floatingResizablePanelCtr.getPanelName();
					jsTweakCmd = "<script>Ext.onReady(function(){try{tweak_" + pn + "();}catch(e){}});</script>";
					jsFocusCmd = "<script>Ext.onReady(function(){try{focus_" + pn + "();}catch(e){}});</script>";

					jsc.setRefreshIntervall(InstantMessagingModule.getCHAT_POLLTIME());

					if (chatWindowHolder != null) {
						chatWindowHolder.setContent(floatingResizablePanelCtr.getInitialComponent());
					} else {
						main.setContent(floatingResizablePanelCtr.getInitialComponent());
					}
				}

				if (muc != null && muc.isJoined()) {
					muc.changeAvailabilityStatus("chatOpen", Presence.Mode.available);
				} else {
					addMeToRosterList(anonymousInChatroom ? NICKNAME_ANONYMOUS : getIdentity().getName());
				}

				chatWindowOpen = true;

				allChats.add(Integer.toString(hashCode()));
				singleUserEventCenter.fireEventToListenersOf(new MultiUserEvent("ChatWindowOpened"), OresHelper.createOLATResourceableType(InstantMessaging.class));

				groupchatVC.contextPut("title", roomNameShort(roomName));
				if (indicateNewMessage != null) {
					indicateNewMessage.setCustomEnabledLinkCSS("");
				}
			}
		} else {
			if (compact) {
				main.setContent(errorCompact);
			} else {
				main.setContent(error);
			}
		}
		update();
	}

	private String roomNameShort(final String roomNameLong) {
		if (roomNameLong.length() > 30) { return roomNameLong.substring(0, 29) + "..."; }
		return roomNameLong;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {

		if (source == sendMessageForm) {
			if (sendMessageForm.getMessage().trim().length() == 0) {
				// nothing happens on entering empty message
				// need refresh for focus

				synchronized (messageHistory) {
					groupChatMsgFieldVC.contextPut("groupChatMessages", messageHistory.toString() + jsTweakCmd + jsFocusCmd);
				}

				return;
			}
		}

		// TODO:gs:a wrap also muc, to catch all exceptions which can occur
		if (muc != null && muc.isJoined()) {

			try {
				if (source == toggleAnonymousForm) {
					try {
						removeMeFromRosterList();
						if (toggleAnonymousForm.isAnonymous()) {
							muc.changeNickname(NICKNAME_ANONYMOUS);
							addMeToRosterList(NICKNAME_ANONYMOUS);
						} else {
							muc.changeNickname(getIdentity().getName());
							addMeToRosterList(myFullName);
						}
					} catch (final XMPPException e) {
						logWarn("Could not change nickname for user: " + getIdentity().getName() + " in course chat: " + ores, e);
						appendToMsgHistory(createMessage("chatroom", getTranslator().translate("msg.send.error")));
					} catch (final Exception e) {
						logWarn("Could not change nickname for user: " + getIdentity().getName() + " in course chat: " + ores, e);
						appendToMsgHistory(createMessage("chatroom", translate("msg.send.error")));
					}

				} else if (source == sendMessageForm) {
					logDebug("sending msg: +" + sendMessageForm.getMessage() + "+ to chatroom: " + roomJID, null);
					try {
						muc.sendMessage(sendMessageForm.getMessage());
						sendMessageForm.resetTextField();

					} catch (final XMPPException e) {
						logWarn("Could not send IM message for user: " + getIdentity().getName() + " in course chat: " + ores, e);
						appendToMsgHistory(createMessage("chatroom", translate("msg.send.error")));
					}
				}

			} catch (final IllegalStateException e) {
				// this happens when the server is going down while the user had already joind a room and tries to send a msg
				logWarn("Could not send IM message for user: " + getIdentity().getName() + " in course chat: " + ores, e);
				appendToMsgHistory(createMessage("chatroom", translate("msg.send.error")));
			}

			groupChatMsgFieldVC.setDirty(true);// for non ajax mode
			rosterVC.setDirty(true); // for non ajax mode

		} else {
			final Message msg = createMessage("chatroom", translate("coursechat.not.available"));
			appendToMsgHistory(msg);
			main.setContent(errorCompact);
		}

		if (source == floatingResizablePanelCtr) {

			if (event.getCommand().equals("done")) {
				close();
			}
		}
		update();
	}

	public void close() {

		chatWindowOpen = false;
		setChatStartable(true);

		allChats.remove(Integer.toString(hashCode()));
		singleUserEventCenter.fireEventToListenersOf(new MultiUserEvent("ChatWindowClosed"), OresHelper.createOLATResourceableType(InstantMessaging.class));

		removeMeFromRosterList();

		jsc.setRefreshIntervall(InstantMessagingModule.getIDLE_POLLTIME());

		if (muc != null && connection.isConnected() && muc.isJoined()) {
			muc.changeAvailabilityStatus("chatClosed", Presence.Mode.available);
		}

		if (chatWindowHolder != null) {
			chatWindowHolder.setContent(null);
		} else {
			if (compact) {
				main.setContent(summaryCompactVC);
			} else {
				main.setContent(summaryVC);
			}

			if (indicateNewMessage != null) {
				summaryCompactVC.remove(indicateNewMessage);
				indicateNewMessage = null;
			}
		}
	}

	public boolean isChatWindowOpen() {
		return chatWindowOpen;
	}

	private Message createMessage(final String from, final String msgBody) {
		final Message msg = new Message();
		msg.setBody(msgBody);
		msg.setFrom(from);
		msg.setProperty("receiveTime", new Long(new Date().getTime()));
		return msg;
	}

	private void setChatStartable(final boolean onoff) {
		if (openGroupChatPanelButton != null) {
			openGroupChatPanelButton.setEnabled(!isInAssessment && onoff);
		}
		if (openGroupChatPanel != null) {
			openGroupChatPanel.setEnabled(!isInAssessment && onoff);
		}
	}

	public void event(final Event event) {

		if (event instanceof AssessmentEvent) {
			if (((AssessmentEvent) event).getEventType().equals(AssessmentEvent.TYPE.STARTED)) {
				isInAssessment = true;
			} else if (((AssessmentEvent) event).getEventType().equals(AssessmentEvent.TYPE.STOPPED)) {
				isInAssessment = singleUserEventCenter.getListeningIdentityCntFor(assessmentInstanceOres) > 0;
			}
			summaryVC.contextPut("isInAssessment", isInAssessment);
			setChatStartable(!startup && !isInAssessment);
			if (openGroupChatPanel != null) {
				openGroupChatPanel.setEnabled(!isInAssessment);
				if (isInAssessment) {
					openGroupChatPanel.setTooltip(translate("chat.not.available.now"), false);
				} else {
					openGroupChatPanel.setTooltip(translate("course.chat.click.to.join"), false);
				}
			}
			return;
		}

		if (event.getCommand().equals("ready")) {
			startup = false;
			setChatStartable(!isInAssessment);
			update();
			return;
		}

		setChatStartable(!isInAssessment && !chatWindowOpen && !startup);

		if (muc != null) {

			if (startup) {
				startup = false;
				if (!chatWindowOpen) {
					muc.changeAvailabilityStatus("chatClosed", Presence.Mode.away);
				}
			}

			removeMeFromRosterList();

			if (anonymousInChatroom && toggleAnonymousForm.isAnonymous()) {
				addMeToRosterList(NICKNAME_ANONYMOUS);
			} else {
				if (chatWindowOpen) {
					addMeToRosterList(myFullName);
				}
			}
		}

		if (event instanceof InstantMessagingEvent) {

			final InstantMessagingEvent imEvent = (InstantMessagingEvent) event;

			if (imEvent.getCommand().equals("groupchat")) {

				final Message msg = (Message) imEvent.getPacket();
				msg.setProperty("receiveTime", new Long(new Date().getTime()));

				logDebug("incoming msg for groupchat: " + msg.getType(), null);

				if ((msg.getType() == Message.Type.groupchat) && msg.getBody() != null) {

					final String uname = extractUsername(msg.getFrom());

					if (!uname.equals("chatroom")) {
						if (!chatWindowOpen) {
							indicateNewMessage = LinkFactory.createCustomLink("indicateNewMsg", "cmd.open.client", "&nbsp;", Link.NONTRANSLATED, summaryCompactVC, this);
							indicateNewMessage.registerForMousePositionEvent(true);
							indicateNewMessage.setCustomEnabledLinkCSS("b_small_icon o_instantmessaging_new_msg_icon");
							indicateNewMessage.setTooltip(getTranslator().translate("groupchat.new.msg"), true);
						}

						appendToMsgHistory(msg);

					}
				}
			} else if (imEvent.getCommand().equals("participant")) {

				final Presence presence = (Presence) imEvent.getPacket();
				logDebug("incoming presence change for groupchat: " + presence.getFrom() + " : " + presence.getType(), null);

				if (presence.getFrom() != null) {

					try {
						prepareRosterList(presence);
						// have to close DB connection because presence events
						// are not from "normal clicks" ...
						DBFactory.getInstance(false).commitAndCloseSession();

					} catch (final Exception e) {
						DBFactory.getInstance(false).rollbackAndCloseSession();
					}
				}
			}
		}
		update();
	}

	private void appendToMsgHistory(final Message msg) {

		final String uname = extractUsername(msg.getFrom());
		if (uname.equals("chatroom")) {
			// not displaying system messages in olat
			return;
		}

		final StringBuilder sb = new StringBuilder();
		sb.append("<div><span style=\"color:" + colorizeUserName(uname) + "\">[");
		sb.append(ClientHelper.getSendDate(msg, locale));
		sb.append("] ");
		sb.append(uname);
		sb.append(": </span>");
		sb.append(msg.getBody().replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
		sb.append("</div>");

		synchronized (messageHistory) {
			StringBuilder f;
			messageHistory.append(sb);
			f = new StringBuilder(messageHistory);
			f.append(jsTweakCmd);
			if (uname.equals(getIdentity().getName()) || uname.equals(NICKNAME_ANONYMOUS)) {
				f.append(jsFocusCmd);
			}
			groupChatMsgFieldVC.contextPut("groupChatMessages", f.toString());
		}
	}

	private String getFullUserName(final String username) {
		final Identity ident = BaseSecurityManager.getInstance().findIdentityByName(username);
		if (ident != null) {
			final StringBuilder sb = new StringBuilder();
			sb.append(ident.getUser().getProperty(UserConstants.FIRSTNAME, locale)).append(" ");
			sb.append(ident.getUser().getProperty(UserConstants.LASTNAME, locale)).append(" ");
			sb.append("(").append(ident.getName()).append(")");
			return sb.toString();
		}
		return username;
	}

	private void prepareRosterList(final Presence p) {

		synchronized (rosterList) {

			final String s = p.getStatus();
			final String t = p.getType().name();
			final String n = getFullUserName(extractUsername(p.getFrom()));

			if ("chatEcho".equals(s)) {
				if (!rosterList.contains(n)) {
					rosterList.add(n);
				}
			} else if ("chatOpen".equals(s)) {
				if (!rosterList.contains(n)) {
					rosterList.add(n);
				}
				if (chatWindowOpen) {
					muc.changeAvailabilityStatus("chatEcho", Presence.Mode.available);
				}
			} else if ("chatClosed".equals(s)) {
				if (chatWindowOpen) {
					muc.changeAvailabilityStatus("chatEcho", Presence.Mode.available);
				}
				rosterList.remove(n);
			} else if (t.equals("available")) {
				if (!rosterList.contains(n)) {
					rosterList.add(n);
				}
			} else if (t.equals("unavailable")) {
				rosterList.remove(n);
			}

			if (!chatWindowOpen) {
				rosterList.remove(NICKNAME_ANONYMOUS);
			}
		}

	}

	private void removeMeFromRosterList() {

		synchronized (rosterList) {
			rosterList.remove(NICKNAME_ANONYMOUS);
			rosterList.remove(myFullName);
		}
	}

	private void addMeToRosterList(final String fname) {

		synchronized (rosterList) {

			rosterList.remove(NICKNAME_ANONYMOUS);

			if (!rosterList.contains(fname)) {
				rosterList.add(fname);
			}
		}
	}

	private String colorizeUserName(String from) {
		// append name to lengt 6
		if (from.startsWith(NICKNAME_PREFIX)) {
			from = new StringBuilder(from).reverse().toString();
		}

		if (from.length() < 6) {
			while (from.length() < 6) {
				from = from + "9";
			}
		}
		// get hex form the first 6 chars (only numbers)
		final StringBuilder sb = new StringBuilder();
		sb.append("#");
		for (int j = 0; j < 6; j++) {
			final int z = from.charAt(j) % 9;
			switch (z) {
				case 8:
					sb.append("A");
					break;// make more darker colors
				case 9:
					sb.append("B");
					break;
				default:
					sb.append(z);
			}
		}
		return sb.toString();
	}

	private String extractUsername(final String from) {
		if (from != null) {
			if (from.contains("/")) { return from.substring(from.lastIndexOf("/") + 1, from.length()); }
		}
		return "chatroom";
	}

	private void update() {

		synchronized (rosterList) {
			final Integer c = rosterList.size();
			Collections.sort(rosterList);
			rosterVC.contextPut("rosterList", rosterList);

			if (openGroupChatPanel != null) {
				openGroupChatPanel.setCustomDisplayText(translate("participants.in.chat", new String[] { c.toString() }));
			}
		}
	}

}
