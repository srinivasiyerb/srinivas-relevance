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

package org.olat.admin.securitygroup.gui;

import java.util.List;

import org.olat.basesecurity.SecurityGroup;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableMultiSelectEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.Identity;
import org.olat.core.util.mail.MailNotificationEditController;
import org.olat.core.util.mail.MailTemplate;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * Description:<BR>
 * Waiting-list group management controller. Displays the list of users that are in the given waiting group and features an 'move as participant' button to add users to
 * the group. The following events are fired:<br>
 * SingleIdentityChosenEvent Event.CANCELLED_EVENT
 * <P>
 * Initial Date: 16.05.2006
 * 
 * @author Christian Guretzki
 */

public class WaitingGroupController extends GroupController {

	protected static final String COMMAND_MOVE_USER_WAITINGLIST = "move.user.waitinglist";
	private MailTemplate transferUserMailTempl;
	private MailNotificationEditController transferMailCtr;
	private CloseableModalController cmc;
	private List<Identity> toTransfer;

	/**
	 * @param ureq
	 * @param wControl
	 * @param mayModifyMembers
	 * @param keepAtLeastOne
	 * @param enableTablePreferences
	 * @param aSecurityGroup
	 */
	public WaitingGroupController(final UserRequest ureq, final WindowControl wControl, final boolean mayModifyMembers, final boolean keepAtLeastOne,
			final boolean enableTablePreferences, final SecurityGroup waitingListGroup) {
		super(ureq, wControl, mayModifyMembers, keepAtLeastOne, enableTablePreferences, waitingListGroup);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller sourceController, final Event event) {
		if (sourceController == tableCtr) {
			if (event.getCommand().equals(Table.COMMAND_MULTISELECT)) {
				// Multiselect events
				final TableMultiSelectEvent tmse = (TableMultiSelectEvent) event;
				if (tmse.getAction().equals(COMMAND_MOVE_USER_WAITINGLIST)) {
					if (tmse.getSelection().isEmpty()) {
						getWindowControl().setWarning(translate("msg.selectionempty"));
					} else {
						final List<Identity> objects = identitiesTableModel.getIdentities(tmse.getSelection());
						toTransfer = objects;

						removeAsListenerAndDispose(transferMailCtr);
						transferMailCtr = new MailNotificationEditController(getWindowControl(), ureq, transferUserMailTempl, true);
						listenTo(transferMailCtr);

						removeAsListenerAndDispose(cmc);
						cmc = new CloseableModalController(getWindowControl(), translate("close"), transferMailCtr.getInitialComponent());
						listenTo(cmc);

						cmc.activate();
						return; // don't execute super method
					}
				}
			}
		} else if (sourceController == transferMailCtr) {
			if (event == Event.DONE_EVENT) {
				// fetch configured mail template and finish this controller workflow
				final MailTemplate customTransferTemplate = transferMailCtr.getMailTemplate();
				cmc.deactivate();
				final IdentitiesMoveEvent identitiesMoveEvent = new IdentitiesMoveEvent(toTransfer);
				identitiesMoveEvent.setMailTemplate(customTransferTemplate);
				fireEvent(ureq, identitiesMoveEvent);
				final StringBuilder infoMessage = new StringBuilder();
				for (final Identity identity : identitiesMoveEvent.getNotMovedIdentities()) {
					infoMessage.append(translate("msg.alreadyinwaiitinggroup", identity.getName())).append("<br />");
				}
				// report any errors on screen
				if (infoMessage.length() > 0) {
					getWindowControl().setInfo(infoMessage.toString());
				}
				return; // don't execute super method
			} else if (event == Event.CANCELLED_EVENT) {
				cmc.deactivate();
				return; // don't execute super method
			} else {
				throw new RuntimeException("unknown event ::" + event.getCommand());
			}

		}
		// it is no WaitingGroupController event, forward it to super class GroupController
		super.event(ureq, sourceController, event);
	}

	/**
	 * Init WaitingList-table-controller for waitinglist with addional column action=move user to participant-list. Show added-date attribute and sort waiting list per
	 * default by added date.
	 */
	@Override
	protected void initGroupTable(final TableController tableCtr, final UserRequest ureq, final boolean enableTablePreferences, final boolean enableUserSelection) {
		final List<UserPropertyHandler> userPropertyHandlers = UserManager.getInstance().getUserPropertyHandlersFor(usageIdentifyer, isAdministrativeUser);
		// first the login name
		final DefaultColumnDescriptor cd0 = new DefaultColumnDescriptor("table.user.login", 0, COMMAND_VCARD, ureq.getLocale());
		cd0.setIsPopUpWindowAction(true, "height=700, width=900, location=no, menubar=no, resizable=yes, status=no, scrollbars=yes, toolbar=no");
		tableCtr.addColumnDescriptor(cd0);
		// followed by the users fields
		int colId = 0;
		for (int i = 0; i < userPropertyHandlers.size(); i++) {
			final UserPropertyHandler userPropertyHandler = userPropertyHandlers.get(i);
			final boolean visible = UserManager.getInstance().isMandatoryUserProperty(usageIdentifyer, userPropertyHandler);
			tableCtr.addColumnDescriptor(visible, userPropertyHandler.getColumnDescriptor(i + 1, null, ureq.getLocale()));
			if (visible) {
				colId++;
			}
		}

		// in the end
		if (enableTablePreferences) {
			tableCtr.addColumnDescriptor(true, new DefaultColumnDescriptor("table.subject.addeddate", userPropertyHandlers.size() + 1, null, ureq.getLocale()));
			colId++;
			// Sort Waiting-list by addedDate
			tableCtr.setSortColumn(colId, true);
		}

		if (mayModifyMembers) {
			tableCtr.addMultiSelectAction("action.waitinglist.move", COMMAND_MOVE_USER_WAITINGLIST);
			tableCtr.addMultiSelectAction("action.remove", COMMAND_REMOVEUSER);
			tableCtr.setMultiSelect(true);
		} else {
			// neither offer a table delete column nor allow adduser link
		}
	}

	/**
	 * @param addUserMailDefaultTempl Set a template to send mail when adding users to group
	 */
	public void setTransferUserMailTempl(final MailTemplate transferUserMailTempl) {
		this.transferUserMailTempl = transferUserMailTempl;
	}

}
