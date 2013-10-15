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
package org.olat.instantMessaging.groupchat;

import java.util.HashMap;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.OLATResourceable;

/**
 * Description:<br>
 * GroupChatManagerController manages creation and deletion of groupChatControllers and provides
 * <P>
 * Initial Date: 15.05.2008 <br>
 * 
 * @author guido
 */
public class GroupChatManagerController extends BasicController {

	protected VelocityContainer container = createVelocityContainer("groupchats");
	protected HashMap<OLATResourceable, InstantMessagingGroupChatController> chats = new HashMap<OLATResourceable, InstantMessagingGroupChatController>(2);
	protected boolean lazyCreateChat = false;

	public GroupChatManagerController(final UserRequest ureq, final WindowControl control) {
		super(ureq, control);
		container.contextPut("chats", chats);
		putInitialPanel(container);
	}

	/**
	 * [spring]
	 * 
	 * @param ureq
	 * @param control
	 * @param fallBackTranslator
	 */
	private GroupChatManagerController(final UserRequest ureq, final WindowControl control, final Translator fallBackTranslator) {
		super(ureq, control, fallBackTranslator);
	}

	/**
	 * @param ureq
	 * @param windowControl
	 * @param ores
	 * @param roomName
	 * @param compact
	 * @param anonymousInChatroom
	 */
	public void createGroupChat(final UserRequest ureq, final WindowControl windowControl, final OLATResourceable ores, final String roomName, final boolean compact,
			final boolean anonymousInChatroom) {

		final Panel p = new Panel("groupchatholder");
		final InstantMessagingGroupChatController groupchat = new InstantMessagingGroupChatController(ureq, windowControl, ores, roomName, p, compact,
				anonymousInChatroom, !ores.getResourceableTypeName().equals("BusinessGroup"));

		container.put(ores.getResourceableId().toString(), p);
		chats.put(ores, groupchat);
	}

	public Controller getGroupChatController(final OLATResourceable ores) {
		return chats.get(ores);
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	public Component getGroupChatContainer() {
		return container;
	}

	public void removeChat(final OLATResourceable ores) {
		final InstantMessagingGroupChatController chatCtr = chats.remove(ores);
		if (chatCtr != null) {
			chatCtr.dispose();
		}

		final Component c = container.getComponent(ores.getResourceableId().toString());
		container.remove(c);
	}

	/**
	 * @return true if the users do not join the chatRoom automatically
	 */
	public boolean isLazyCreateChat() {
		return lazyCreateChat;
	}

}