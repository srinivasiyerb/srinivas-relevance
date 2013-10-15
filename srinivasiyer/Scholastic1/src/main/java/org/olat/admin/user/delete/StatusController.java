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

package org.olat.admin.user.delete;

import java.util.List;

import org.olat.admin.user.UserSearchController;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
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
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.util.Util;
import org.olat.user.UserManager;

/**
 * Controller for tab 'Delete Email Status'.
 * 
 * @author Christian Guretzki
 */
public class StatusController extends BasicController {
	private static final String MY_PACKAGE = Util.getPackageName(StatusController.class);
	private static final String PACKAGE_USER_SEARCH = Util.getPackageName(UserSearchController.class);

	private static final String ACTION_SINGLESELECT_CHOOSE = "ssc";

	private final VelocityContainer myContent;
	private final Panel userDeleteStatusPanel;
	private TableController tableCtr;
	private UserDeleteTableModel tdm;

	private final boolean isAdministrativeUser;
	private final Translator propertyHandlerTranslator;

	/**
	 * @param ureq
	 * @param wControl
	 * @param cancelbutton
	 */
	public StatusController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		final PackageTranslator fallbackTrans = new PackageTranslator(PACKAGE_USER_SEARCH, ureq.getLocale());
		this.setTranslator(new PackageTranslator(MY_PACKAGE, ureq.getLocale(), fallbackTrans));
		// use the PropertyHandlerTranslator as tableCtr translator
		propertyHandlerTranslator = UserManager.getInstance().getPropertyHandlerTranslator(getTranslator());

		myContent = this.createVelocityContainer("deletestatus");

		final Roles roles = ureq.getUserSession().getRoles();
		isAdministrativeUser = (roles.isAuthor() || roles.isGroupManager() || roles.isUserManager() || roles.isOLATAdmin());

		userDeleteStatusPanel = new Panel("userDeleteStatusPanel");
		userDeleteStatusPanel.addListener(this);
		myContent.put("userDeleteStatusPanel", userDeleteStatusPanel);
		myContent.contextPut("header",
				getTranslator().translate("status.delete.email.header", new String[] { Integer.toString(UserDeletionManager.getInstance().getDeleteEmailDuration()) }));

		initializeTableController(ureq);

		this.putInitialPanel(myContent);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == tableCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				final TableEvent te = (TableEvent) event;
				if (te.getActionId().equals(ACTION_SINGLESELECT_CHOOSE)) {
					final int rowid = te.getRowId();
					final Identity foundIdentity = (Identity) tdm.getObject(rowid);
					UserDeletionManager.getInstance().setIdentityAsActiv(foundIdentity);
					updateUserList();
				}
			}
		}
	}

	private void initializeTableController(final UserRequest ureq) {
		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("error.no.user.found"));

		removeAsListenerAndDispose(tableCtr);
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), this.propertyHandlerTranslator);
		listenTo(tableCtr);

		final List l = UserDeletionManager.getInstance().getIdentitiesInDeletionProcess(UserDeletionManager.getInstance().getDeleteEmailDuration());
		tdm = new UserDeleteTableModel(l, ureq.getLocale(), isAdministrativeUser);
		tdm.addColumnDescriptors(tableCtr, null, "table.identity.deleteEmail");
		tableCtr.addColumnDescriptor(new StaticColumnDescriptor(ACTION_SINGLESELECT_CHOOSE, "table.header.action", translate("action.activate")));

		tableCtr.setMultiSelect(false);
		tableCtr.setTableDataModel(tdm);
		userDeleteStatusPanel.setContent(tableCtr.getInitialComponent());
	}

	protected void updateUserList() {
		final List l = UserDeletionManager.getInstance().getIdentitiesInDeletionProcess(UserDeletionManager.getInstance().getDeleteEmailDuration());
		tdm.setObjects(l);
		tableCtr.setTableDataModel(tdm);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

}
