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

package org.olat.admin.user;

import java.util.List;

import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.core.gui.components.table.StaticColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.Identity;

/**
 * Initial Date: Aug 27, 2004
 * 
 * @author Mike Stock
 */
public class UserAuthenticationsEditorController extends BasicController {
	private final TableController tableCtr;
	private AuthenticationsTableDataModel authTableModel;
	private DialogBoxController confirmationDialog;
	private final Identity changeableIdentity;

	/**
	 * @param ureq
	 * @param wControl
	 * @param changeableIdentity
	 */
	public UserAuthenticationsEditorController(final UserRequest ureq, final WindowControl wControl, final Identity changeableIdentity) {
		super(ureq, wControl);

		this.changeableIdentity = changeableIdentity;

		// init main view container as initial component
		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.auth.provider", 0, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.auth.login", 1, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.auth.credential", 2, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new StaticColumnDescriptor("delete", "table.header.action", translate("delete")));
		authTableModel = new AuthenticationsTableDataModel(BaseSecurityManager.getInstance().getAuthentications(changeableIdentity));
		tableCtr.setTableDataModel(authTableModel);
		listenTo(tableCtr);

		putInitialPanel(tableCtr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// no events to catch
	}

	/**
	 * Rebuild the authentications table data model
	 */
	public void rebuildAuthenticationsTableDataModel() {
		authTableModel = new AuthenticationsTableDataModel(BaseSecurityManager.getInstance().getAuthentications(changeableIdentity));
		tableCtr.setTableDataModel(authTableModel);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == confirmationDialog) {
			if (DialogBoxUIFactory.isYesEvent(event)) {
				final Authentication auth = (Authentication) confirmationDialog.getUserObject();
				BaseSecurityManager.getInstance().deleteAuthentication(auth);
				getWindowControl().setInfo(getTranslator().translate("authedit.delete.success", new String[] { auth.getProvider(), changeableIdentity.getName() }));
				authTableModel.setObjects(BaseSecurityManager.getInstance().getAuthentications(changeableIdentity));
				tableCtr.modelChanged();
			}
		} else if (source == tableCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				final TableEvent te = (TableEvent) event;
				final String actionid = te.getActionId();
				if (actionid.equals("delete")) {
					final int rowid = te.getRowId();
					final Authentication auth = (Authentication) authTableModel.getObject(rowid);
					confirmationDialog = activateYesNoDialog(ureq, null,
							getTranslator().translate("authedit.delete.confirm", new String[] { auth.getProvider(), changeableIdentity.getName() }), confirmationDialog);
					confirmationDialog.setUserObject(auth);
					return;
				}
			}
		}

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// DialogBoxController and TableController get disposed by BasicController
	}

	/**
	 * 
	 */
	class AuthenticationsTableDataModel extends DefaultTableDataModel {

		/**
		 * @param objects
		 */
		public AuthenticationsTableDataModel(final List objects) {
			super(objects);
		}

		/**
		 * @see org.olat.core.gui.components.table.TableDataModel#getValueAt(int, int)
		 */
		@Override
		public final Object getValueAt(final int row, final int col) {
			final Authentication auth = (Authentication) getObject(row);
			switch (col) {
				case 0:
					return auth.getProvider();
				case 1:
					return auth.getAuthusername();
				case 2:
					return auth.getCredential();
				default:
					return "error";
			}
		}

		/**
		 * @see org.olat.core.gui.components.table.TableDataModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {
			return 3;
		}

	}

}
