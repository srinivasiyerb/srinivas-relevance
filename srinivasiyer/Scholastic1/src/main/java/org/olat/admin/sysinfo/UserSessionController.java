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

package org.olat.admin.sysinfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.Window;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.StaticColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.util.Formatter;
import org.olat.core.util.SessionInfo;
import org.olat.core.util.UserSession;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockEntry;

/**
 * Initial Date: 01.09.2004
 * 
 * @author Mike Stock
 */

public class UserSessionController extends BasicController {

	private final VelocityContainer myContent;
	private final TableController tableCtr;
	private final Formatter f;
	private UserSessionTableModel usessTableModel;
	private DialogBoxController dialogController;
	private int selRow;
	private final Link backLink;
	private final Link sessKillButton;

	private final Panel myPanel;
	/**
	 * Timeframe in minutes is needed to calculate the last klicks from users in OLAT.
	 */
	private static final int LAST_KLICK_TIMEFRAME = 5;
	private static final long DIFF = 1000 * 60 * LAST_KLICK_TIMEFRAME; // milliseconds of klick difference

	/**
	 * Controlls user session in admin view.
	 * 
	 * @param ureq
	 * @param wControl
	 */
	public UserSessionController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		f = Formatter.getInstance(ureq.getLocale());

		myContent = createVelocityContainer("sessions");

		backLink = LinkFactory.createLinkBack(myContent, this);
		sessKillButton = LinkFactory.createButton("sess.kill", myContent, this);

		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("sess.last", 0, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("sess.first", 1, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("sess.identity", 2, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("sess.authprovider", 3, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("sess.fqdn", 4, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("sess.access", 5, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("sess.duration", 6, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("sess.mode", 7, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new StaticColumnDescriptor("sess.details", "table.action", translate("sess.details")));
		listenTo(tableCtr);
		reset();
		myContent.put("sessiontable", tableCtr.getInitialComponent());
		myPanel = putInitialPanel(myContent);
	}

	/**
	 * Re-initialize this controller. Fetches sessions again.
	 */
	public void reset() {
		final List<UserSession> authUserSessions = new ArrayList<UserSession>(UserSession.getAuthenticatedUserSessions());
		usessTableModel = new UserSessionTableModel(authUserSessions, getTranslator());
		tableCtr.setTableDataModel(usessTableModel);
		// view number of user - lastKlick <= LAST_KLICK_TIMEFRAME min
		final long now = System.currentTimeMillis();
		int counter = 0;
		for (final UserSession usess : authUserSessions) {

			final long lastklick = usess.getSessionInfo().getLastClickTime();
			if ((now - lastklick) <= DIFF) {
				counter++;
			}
		}
		myContent.contextPut("scount", String.valueOf(counter));
		myContent.contextPut("minutes", String.valueOf(LAST_KLICK_TIMEFRAME));
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == backLink) {
			myPanel.popContent();
			reset();
		} else if (source == sessKillButton) {
			dialogController = activateYesNoDialog(ureq, null, translate("sess.kill.sure"), dialogController);
			return;
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == dialogController) {
			if (DialogBoxUIFactory.isYesEvent(event)) {
				final UserSession usess = (UserSession) usessTableModel.getObject(selRow);
				final SessionInfo sessInfo = usess.getSessionInfo();
				if (usess.isAuthenticated()) {
					final HttpSession session = sessInfo.getSession();
					if (session != null) {
						try {
							session.invalidate();
						} catch (final IllegalStateException ise) {
							// thrown when session already invalidated. fine. ignore.
						}
					}
					showInfo("sess.kill.done", sessInfo.getLogin());
				}
				reset();
			}
		} else if (source == tableCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				final TableEvent te = (TableEvent) event;
				selRow = te.getRowId();
				// session info (we only have authenticated sessions here
				final UserSession usess = (UserSession) usessTableModel.getObject(selRow);
				// if (!usess.isAuthenticated()) throw new AssertException("usersession was not authenticated!?");

				final VelocityContainer sesDetails = createVelocityContainer("sessionDetails");
				sesDetails.contextPut("us", usess);
				final SessionInfo sessInfo = usess.getSessionInfo();
				sesDetails.contextPut("si", sessInfo);
				final boolean isAuth = usess.isAuthenticated();
				sesDetails.contextPut("isauth", isAuth ? "yes" : "-- NOT AUTHENTICATED!");

				long creatTime = -1;
				long lastAccessTime = -1;

				boolean success = false;
				if (isAuth) {
					try {
						final HttpSession se = sessInfo.getSession();
						creatTime = se.getCreationTime();
						lastAccessTime = se.getLastAccessedTime();
						success = true;
					} catch (final Exception ise) {
						// nothing to do
					}
				}

				if (success) {
					sesDetails.contextPut("created", f.formatDateAndTime(new Date(creatTime)));
					sesDetails.contextPut("lastaccess", f.formatDateAndTime(new Date(lastAccessTime)));
				} else {
					sesDetails.contextPut("created", " -- this session has been invalidated --");
					sesDetails.contextPut("lastaccess", " -- this session has been invalidated --");
				}

				if (success) {
					// lock information
					final String username = sessInfo.getLogin();
					final ArrayList lockList = new ArrayList();
					final List<LockEntry> locks = CoordinatorManager.getInstance().getCoordinator().getLocker().adminOnlyGetLockEntries();
					final Formatter f = Formatter.getInstance(ureq.getLocale());
					for (final LockEntry entry : locks) {
						if (entry.getOwner().getName().equals(username)) {
							lockList.add(entry.getKey() + " " + f.formatDateAndTime(new Date(entry.getLockAquiredTime())));
						}
					}
					sesDetails.contextPut("locklist", lockList);

					// user environment
					sesDetails.contextPut("env", usess.getIdentityEnvironment());

					// GUI statistics
					final Windows ws = Windows.getWindows(usess);
					final StringBuilder sb = new StringBuilder();
					for (final Iterator iterator = ws.getWindowIterator(); iterator.hasNext();) {
						final Window window = (Window) iterator.next();
						sb.append("- Window ").append(window.getDispatchID()).append(" dispatch info: ").append(window.getLatestDispatchComponentInfo()).append("<br />");
					}
					sb.append("<br />");
					sesDetails.contextPut("guistats", sb.toString());
				}
				sesDetails.put("backLink", backLink);
				sesDetails.put("sess.kill", sessKillButton);

				myPanel.pushContent(sesDetails);
			}
		}
	}

	@Override
	protected void doDispose() {
		// DialogBoxController and TableController get disposed by BasicController
	}
}