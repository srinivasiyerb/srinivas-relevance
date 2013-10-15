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

import java.util.Iterator;
import java.util.List;

import org.olat.admin.user.UserSearchController;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.table.StaticColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.table.TableMultiSelectEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.util.Util;
import org.olat.user.UserManager;

/**
 * Controller for 'Ready-to-delete' tab.
 * 
 * @author Christian Guretzki
 */
public class ReadyToDeleteController extends BasicController {
	private static final String MY_PACKAGE = Util.getPackageName(ReadyToDeleteController.class);
	private static final String PACKAGE_USER_SEARCH = Util.getPackageName(UserSearchController.class);

	private static final String ACTION_SINGLESELECT_CHOOSE = "ssc";
	private static final String ACTION_MULTISELECT_CHOOSE = "msc";

	private final VelocityContainer myContent;
	private final Panel readyToDeletePanel;
	private TableController tableCtr;
	private UserDeleteTableModel tdm;
	private List readyToDeleteIdentities;
	private DialogBoxController deleteConfirmController;
	private final boolean isAdministrativeUser;
	private final Translator propertyHandlerTranslator;

	/**
	 * @param ureq
	 * @param wControl
	 * @param cancelbutton
	 */
	public ReadyToDeleteController(final UserRequest ureq, final WindowControl wControl) {

		super(ureq, wControl);

		final PackageTranslator fallbackTrans = new PackageTranslator(PACKAGE_USER_SEARCH, ureq.getLocale());
		this.setTranslator(new PackageTranslator(MY_PACKAGE, ureq.getLocale(), fallbackTrans));
		// use the PropertyHandlerTranslator as tableCtr translator
		propertyHandlerTranslator = UserManager.getInstance().getPropertyHandlerTranslator(getTranslator());

		myContent = createVelocityContainer("panel");
		readyToDeletePanel = new Panel("readyToDeletePanel");
		readyToDeletePanel.addListener(this);
		myContent.put("panel", readyToDeletePanel);

		final Roles roles = ureq.getUserSession().getRoles();
		isAdministrativeUser = (roles.isAuthor() || roles.isGroupManager() || roles.isUserManager() || roles.isOLATAdmin());

		initializeTableController(ureq);
		initializeContent();
		putInitialPanel(myContent);
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

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
			} else if (event.getCommand().equals(Table.COMMAND_MULTISELECT)) {
				final TableMultiSelectEvent tmse = (TableMultiSelectEvent) event;
				if (tmse.getAction().equals(ACTION_MULTISELECT_CHOOSE)) {
					handleDeleteButtonEvent(ureq, tmse);
				}
			}
		} else if (source == deleteConfirmController) {
			if (DialogBoxUIFactory.isOkEvent(event)) {
				deleteIdentities(readyToDeleteIdentities);
				showInfo("deleted.feedback.msg");
				initializeContent();
			}
		}
	}

	private void handleDeleteButtonEvent(final UserRequest ureq, final TableMultiSelectEvent tmse) {
		if (tdm.getObjects(tmse.getSelection()).size() != 0) {
			if (!UserDeletionManager.getInstance().isReadyToDelete()) {
				showInfo("info.is.not.ready.to.delete");
				return;
			}
			readyToDeleteIdentities = tdm.getObjects(tmse.getSelection());
			deleteConfirmController = activateOkCancelDialog(ureq, null, translate("readyToDelete.delete.confirm", getUserlistAsString(readyToDeleteIdentities)),
					deleteConfirmController);
			return;
		} else {
			showWarning("nothing.selected.msg");
		}
	}

	/**
	 * Build form a list of identities a string representation.
	 * 
	 * @param readyToDeleteIdentities2
	 * @return String with login-name, comma separated
	 */
	private String getUserlistAsString(final List readyToDeleteIdentities2) {
		final StringBuilder strb = new StringBuilder();
		for (final Iterator iter = readyToDeleteIdentities2.iterator(); iter.hasNext();) {
			strb.append(((Identity) iter.next()).getName());
			if (iter.hasNext()) {
				strb.append(",");
			}
		}
		return strb.toString();
	}

	private void initializeTableController(final UserRequest ureq) {
		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("error.no.user.found"));
		tableConfig.setShowAllLinkEnabled(false);

		removeAsListenerAndDispose(tableCtr);
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), this.propertyHandlerTranslator);
		listenTo(tableCtr);

		final List l = UserDeletionManager.getInstance().getIdentitiesInDeletionProcess(UserDeletionManager.getInstance().getDeleteEmailDuration());
		tdm = new UserDeleteTableModel(l, ureq.getLocale(), isAdministrativeUser);
		tdm.addColumnDescriptors(tableCtr, null, "table.identity.deleteEmail");
		tableCtr.addColumnDescriptor(new StaticColumnDescriptor(ACTION_SINGLESELECT_CHOOSE, "table.header.action", translate("action.activate")));
		tableCtr.addMultiSelectAction("action.ready.to.delete", ACTION_MULTISELECT_CHOOSE);
		tableCtr.setMultiSelect(true);
		tableCtr.setTableDataModel(tdm);
	}

	private void initializeContent() {
		updateUserList();
		final VelocityContainer readyToDeleteContent = createVelocityContainer("readyToDelete");
		readyToDeleteContent.put("readyToDelete", tableCtr.getInitialComponent());
		readyToDeleteContent.contextPut("header", translate("ready.to.delete.header", Integer.toString(UserDeletionManager.getInstance().getDeleteEmailDuration())));
		readyToDeletePanel.setContent(readyToDeleteContent);
	}

	protected void updateUserList() {
		final List l = UserDeletionManager.getInstance().getIdentitiesReadyToDelete(UserDeletionManager.getInstance().getDeleteEmailDuration());
		tdm.setObjects(l);
		tableCtr.setTableDataModel(tdm);
	}

	private void deleteIdentities(final List objects) {
		for (final Iterator iter = objects.iterator(); iter.hasNext();) {
			UserDeletionManager.getInstance().deleteIdentity((Identity) iter.next());
			DBFactory.getInstance().intermediateCommit();
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

}
