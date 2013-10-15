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

package org.olat.admin.quota;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
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
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;

/**
 * Description:<br>
 * is the controller for
 * 
 * @author Felix Jost
 */
public class QuotaController extends BasicController {

	private final VelocityContainer myContent;
	private final QuotaTableModel quotaTableModel;

	private GenericQuotaEditController quotaEditCtr;
	private final Panel main;
	private final TableController tableCtr;
	private final Link addQuotaButton;

	/**
	 * @param ureq
	 * @param wControl
	 */
	public QuotaController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		final BaseSecurity mgr = BaseSecurityManager.getInstance();
		if (!mgr.isIdentityPermittedOnResourceable(ureq.getIdentity(), Constants.PERMISSION_ACCESS, OresHelper.lookupType(this.getClass()))) { throw new OLATSecurityException(
				"Insufficient permissions to access QuotaController"); }

		main = new Panel("quotamain");
		myContent = createVelocityContainer("index");
		addQuotaButton = LinkFactory.createButton("qf.new", myContent, this);

		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		listenTo(tableCtr);

		quotaTableModel = new QuotaTableModel();
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.path", 0, null, getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.quota", 1, null, getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.limit", 2, null, getLocale()));
		tableCtr.addColumnDescriptor(new StaticColumnDescriptor("qf.edit", "table.action", translate("edit")));
		tableCtr.addColumnDescriptor(new StaticColumnDescriptor("qf.del", "table.action", translate("delete")));
		tableCtr.setTableDataModel(quotaTableModel);

		myContent.put("quotatable", tableCtr.getInitialComponent());
		main.setContent(myContent);

		putInitialPanel(main);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == addQuotaButton) {
			// clean up old controller first
			if (quotaEditCtr != null) {
				removeAsListenerAndDispose(quotaEditCtr);
			}
			// start edit workflow in dedicated quota edit controller
			removeAsListenerAndDispose(quotaEditCtr);
			quotaEditCtr = new GenericQuotaEditController(ureq, getWindowControl(), null);
			listenTo(quotaEditCtr);
			main.setContent(quotaEditCtr.getInitialComponent());
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == quotaEditCtr) {
			if (event == Event.CHANGED_EVENT) {
				quotaTableModel.refresh();
				tableCtr.setTableDataModel(quotaTableModel);
			}
			// else cancel event. in any case set content to list
			main.setContent(myContent);
		}

		if (source == tableCtr && event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
			final TableEvent te = (TableEvent) event;
			final Quota q = quotaTableModel.getRowData(te.getRowId());
			if (te.getActionId().equals("qf.edit")) {
				// clean up old controller first
				// start edit workflow in dedicated quota edit controller
				removeAsListenerAndDispose(quotaEditCtr);
				quotaEditCtr = new GenericQuotaEditController(ureq, getWindowControl(), q);
				listenTo(quotaEditCtr);
				main.setContent(quotaEditCtr.getInitialComponent());

			} else if (te.getActionId().equals("qf.del")) {
				// try to delete quota
				final boolean deleted = QuotaManager.getInstance().deleteCustomQuota(q);
				if (deleted) {
					quotaTableModel.refresh();
					tableCtr.setTableDataModel(quotaTableModel);
					showInfo("qf.deleted", q.getPath());
				} else {
					// default quotas can not be qf.cannot.del.default")deleted
					showError("qf.cannot.del.default");
				}
			}
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
