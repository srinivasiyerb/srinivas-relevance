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

package org.olat.admin.policy;

import java.util.List;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Policy;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.table.BaseTableDataModelWithoutFilter;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableDataModel;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.Identity;
import org.olat.resource.OLATResource;

/**
 * Description:<br>
 * TODO: Class Description for PolicyController
 */
public class PolicyController extends BasicController {

	private final TableController tableCtr;

	/**
	 * caller of this constructor must make sure only olat admins come here
	 * 
	 * @param ureq
	 * @param wControl
	 * @param identity
	 */
	public PolicyController(final UserRequest ureq, final WindowControl wControl, final Identity identity) {
		super(ureq, wControl);

		final List entries = BaseSecurityManager.getInstance().getPoliciesOfIdentity(identity);
		final TableDataModel tdm = new PolicyTableDataModel(entries);

		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();

		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		listenTo(tableCtr);

		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.secgroup", 0, null, ureq.getLocale(), ColumnDescriptor.ALIGNMENT_RIGHT));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.policyimpl", 1, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.resourceimplkey", 2, null, ureq.getLocale(), ColumnDescriptor.ALIGNMENT_RIGHT));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.resourceimpltype", 3, null, ureq.getLocale()));
		tableCtr.setTableDataModel(tdm);

		putInitialPanel(tableCtr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// no component events to listen to
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		// no component events to listen to
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

}

class PolicyTableDataModel extends BaseTableDataModelWithoutFilter {
	private final List entries;

	protected PolicyTableDataModel(final List entries) {
		this.entries = entries;
	}

	@Override
	public int getColumnCount() {
		// group key, permission, resource
		return 4;
	}

	@Override
	public int getRowCount() {
		return entries.size();
	}

	@Override
	public final Object getValueAt(final int row, final int col) {
		final Object[] o = (Object[]) entries.get(row);
		final Object co = o[(col == 3 ? 2 : col)];
		switch (col) {
			case 0: // secgr
				final SecurityGroup sg = (SecurityGroup) co;
				return sg.getKey(); // "key:"+sg.getKey()+" ("+sg.getCreationDate()+")";
			case 1: // permission
				final Policy po = (Policy) co;
				final String perm = po.getPermission();
				return perm;
			case 2:
				final OLATResource or = (OLATResource) co;
				return or.getResourceableId();
			case 3:
				final OLATResource ore = (OLATResource) co;
				return ore.getResourceableTypeName();
		}
		return co.toString();
	}
}