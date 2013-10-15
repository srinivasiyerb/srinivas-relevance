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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.olat.basesecurity.BaseSecurityManager;
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
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.util.event.EventBus;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.core.util.resource.OresHelper;
import org.olat.instantMessaging.ClientHelper;
import org.olat.instantMessaging.InstantMessaging;
import org.olat.instantMessaging.InstantMessagingEvent;
import org.olat.instantMessaging.InstantMessagingModule;
import org.olat.instantMessaging.groupchat.SendMessageForm;

/**
 * Description:<br />
 * Controller for a single Chat in a floating window
 * <P>
 * Initial Date: 13.07.2007 <br />
 * 
 * @author guido
 */
public class ChatController extends BasicController implements GenericEventListener {

	private final SendMessageForm sendMessageForm;
	private final VelocityContainer chatContent = createVelocityContainer("chat");
	private final VelocityContainer chatMsgFieldContent = createVelocityContainer("chatMsgField");
	private final Panel chatMsgFieldPanel;
	private Chat chatManager;
	private final String chatPartnerJid;
	private final String username;
	private final StringBuilder messageHistory = new StringBuilder();
	private final Locale locale;
	private final FloatingResizableDialogController chatPanelCtr;
	private final String userJid;
	private final JSAndCSSComponent jsc;
	private final Link refresh;

	private String jsTweakCmd = "";
	private String jsFocusCmd = "";

	private List allChats;
	private final EventBus singleUserEventCenter;

	public ChatController(final UserRequest ureq, final WindowControl wControl, final String chatPartnerJid, final int offsetX, final int offsetY,
			final Message initialMessage) {
		super(ureq, wControl);
		this.chatPartnerJid = chatPartnerJid;
		this.locale = ureq.getLocale();
		this.username = getIdentity().getName();

		this.singleUserEventCenter = ureq.getUserSession().getSingleUserEventCenter();
		allChats = (List) ureq.getUserSession().getEntry("chats");
		if (allChats == null) {
			allChats = new ArrayList();
			ureq.getUserSession().putEntry("chats", allChats);
		}
		allChats.add(Integer.toString(hashCode()));
		singleUserEventCenter.fireEventToListenersOf(new MultiUserEvent("ChatWindowOpened"), OresHelper.createOLATResourceableType(InstantMessaging.class));

		// this.userJid = IMNameHelper.getIMUsernameByOlatUsername(getIdentity().getName())+"@"+InstantMessagingModule.getConferenceServer();
		this.userJid = InstantMessagingModule.getAdapter().getUserJid(this.username);
		final boolean ajaxOn = getWindowControl().getWindowBackOffice().getWindowManager().isAjaxEnabled();
		chatContent.contextPut("isAjaxMode", Boolean.valueOf(ajaxOn));

		// checks with the given intervall if dirty components are available to rerender
		jsc = new JSAndCSSComponent("intervall", this.getClass(), null, null, false, null, InstantMessagingModule.getCHAT_POLLTIME());
		chatContent.put("updatecontrol", jsc);

		InstantMessagingModule.getAdapter().getClientManager().registerEventListener(username, this, false);
		sendMessageForm = new SendMessageForm(ureq, getWindowControl());
		listenTo(sendMessageForm);
		sendMessageForm.resetTextField();

		chatMsgFieldPanel = new Panel("chatMsgField");
		chatMsgFieldPanel.setContent(chatMsgFieldContent);

		final String chatPartnerUsername = InstantMessagingModule.getAdapter().getUsernameFromJid(chatPartnerJid);

		chatPanelCtr = new FloatingResizableDialogController(ureq, getWindowControl(), chatContent, getTranslator().translate("im.chat.with") + ": "
				+ getFullUserName(chatPartnerUsername), 450, 300, offsetX, offsetY, null, null, true, false, true, String.valueOf(this.hashCode()));
		listenTo(chatPanelCtr);

		final String pn = chatPanelCtr.getPanelName();
		chatContent.contextPut("panelName", pn);

		// due to limitations in flexi form, we have to tweak focus handling manually
		jsTweakCmd = "<script>Ext.onReady(function(){try{tweak_" + pn + "();}catch(e){}});</script>";
		jsFocusCmd = "<script>Ext.onReady(function(){try{focus_" + pn + "();}catch(e){}});</script>";

		if (username.equals(chatPartnerUsername)) {
			chatMsgFieldContent.contextPut("chatMessages", getTranslator().translate("chat.with.yourself"));
		} else {
			if (initialMessage == null) {
				chatMsgFieldContent.contextPut("chatMessages", messageHistory.toString());
			} else {
				appendToMessageHistory(initialMessage);
				chatMsgFieldContent.contextPut("chatMessages", messageHistory.toString());
			}
		}

		chatMsgFieldContent.contextPut("id", this.hashCode());
		chatContent.put("chatMsgFieldPanel", chatMsgFieldPanel);

		chatContent.put("sendMessageForm", sendMessageForm.getInitialComponent());

		refresh = LinkFactory.createCustomLink("refresh", "cmd.refresh", "", Link.NONTRANSLATED, chatContent, this);
		refresh.setCustomEnabledLinkCSS("b_small_icoureq.getUserSession().getSingleUserEventCenter().n sendMessageFormo_instantmessaging_refresh_icon");
		refresh.setTitle("im.refresh");

		putInitialPanel(chatPanelCtr.getInitialComponent());
	}

	@Override
	protected void doDispose() {
		allChats.remove(Integer.toString(hashCode()));
		InstantMessagingModule.getAdapter().getClientManager().deregisterControllerListener(username, this);
	}

	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {

		if (source == sendMessageForm && sendMessageForm.getMessage().trim().length() == 0) {
			// ignore empty manObjectessage entry and refocus on entry field
			chatMsgFieldContent.contextPut("chatMessages", messageHistory.toString() + jsFocusCmd);
			return;
		}

		if (source == sendMessageForm) {
			if (chatManager == null) {
				chatManager = InstantMessagingModule.getAdapter().getClientManager().createChat(username, chatPartnerJid, this);
			}
			try {
				chatManager.sendMessage(sendMessageForm.getMessage());
				appendToMessageHistory(createInstantMessage(sendMessageForm.getMessage(), userJid));
			} catch (final XMPPException e) {
				logWarn("Could not send instant message from" + username + " to: " + chatPartnerJid, e);
			}
			sendMessageForm.resetTextField();
		} else if (source == chatPanelCtr) {
			// user closed panel by close icon
			fireEvent(ureq, new Event(chatPartnerJid));
			allChats.remove(Integer.toString(hashCode()));
			jsc.setRefreshIntervall(InstantMessagingModule.getIDLE_POLLTIME());
			singleUserEventCenter.fireEventToListenersOf(new MultiUserEvent("ChatWindowClosed"), OresHelper.createOLATResourceableType(InstantMessaging.class));
		}
	}

	/**
	 * gets called if either a new message or a presence change from one of the buddies happens
	 * 
	 * @see org.olat.core.util.event.GenericEventListener#event(org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final Event event) {
		final InstantMessagingEvent imEvent = (InstantMessagingEvent) event;
		if (imEvent.getCommand().equals("chatmessage")) {
			// chat mode. user started chat himself
			final Message msg = (Message) imEvent.getPacket();
			logDebug("incoming message for user: " + msg.getTo() + "  - body: " + msg.getBody(), null);
			if ((msg.getType() == Message.Type.chat) && msg.getBody() != null) {
				if (!isMessageFromMe(msg)) {
					appendToMessageHistory(msg);
				}
			}
		}
	}

	/**
	 * @param body - any text
	 * @param from must be a valid jid
	 * @return
	 */
	private Message createInstantMessage(final String body, final String from) {
		final Message message = new Message();
		message.setBody(body);
		message.setFrom(from);
		message.setProperty("receiveTime", new Long(new Date().getTime()));
		return message;
	}

	private String getFullUserName(final String userName) {
		final Identity ident = BaseSecurityManager.getInstance().findIdentityByName(userName);
		if (ident != null) {
			final StringBuilder sb = new StringBuilder();
			sb.append(ident.getUser().getProperty(UserConstants.FIRSTNAME, locale));
			sb.append(" ");
			sb.append(ident.getUser().getProperty(UserConstants.LASTNAME, locale));
			sb.append(" (");
			sb.append(ident.getName());
			sb.append(")");
			if (sb.length() > 35) { return sb.substring(0, 35) + "..."; }
			return sb.toString();
		}
		return "";
	}

	private boolean isMessageFromMe(final Message m) {
		return InstantMessagingModule.getAdapter().getUsernameFromJid(m.getFrom()).equals(getIdentity().getName());
	}

	private void appendToMessageHistory(final Message message) {

		final String uname = InstantMessagingModule.getAdapter().getUsernameFromJid(message.getFrom());

		final String m = message.getBody().replaceAll("<br/>\n", "\r\n");

		final StringBuilder sb = new StringBuilder();

		sb.append("<div><span style=\"color:");
		sb.append(colorize(message.getFrom()));
		sb.append("\">[");
		sb.append(ClientHelper.getSendDate(message, locale));
		sb.append("] ");
		sb.append(uname);
		sb.append(": </span>");
		sb.append(prepareMsgBody(m.replaceAll("<", "&lt;").replaceAll(">", "&gt;")).replaceAll("\r\n", "<br/>\n"));
		sb.append("</div>");

		synchronized (messageHistory) {
			messageHistory.append(sb);
		}

		final StringBuilder fh = new StringBuilder(messageHistory);
		fh.append(jsTweakCmd);
		if (uname.equals(getIdentity().getName())) {
			fh.append(jsFocusCmd);
		}

		chatMsgFieldContent.contextPut("chatMessages", fh.toString());
		chatMsgFieldContent.contextPut("id", this.hashCode());
	}

	private String prepareMsgBody(String body) {

		final List<String> done = new ArrayList<String>();

		final Matcher m = Pattern.compile("((mailto\\:|(news|(ht|f)tp(s?))\\://){1}\\S+)").matcher(body);

		while (m.find()) {
			final String l = m.group();
			if (!done.contains(l)) {
				body = body.replaceFirst(l, "<a href=\"" + l + "\" target=\"_blank\">" + l + "</a>");
			}
			done.add(l);
		}

		return body;
	}

	private String colorize(final String from) {
		if (InstantMessagingModule.getAdapter().getUsernameFromJid(from).equals(username)) {
			return "blue";
		} else if (from.equals("info@localhost")) {
			return "green";
		} else {
			return "red";
		}
	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	protected void setChatManager(final Chat chatManager) {
		this.chatManager = chatManager;
	}

	protected Chat getChatManager() {
		return chatManager;
	}

	protected String getMessageHistory() {
		synchronized (messageHistory) {
			return messageHistory.toString();
		}
	}

	protected void setMessageHistory(final String m) {
		synchronized (messageHistory) {
			messageHistory.insert(0, m);
		}

		chatMsgFieldContent.contextPut("chatMessages", messageHistory.toString());
	}
}
