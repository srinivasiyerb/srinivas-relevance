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

import java.util.List;

import org.olat.core.gui.components.table.BaseTableDataModelWithoutFilter;
import org.olat.core.gui.components.table.TableDataModel;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;

/**
 * Initial Date: Mar 30, 2004
 * 
 * @author Mike Stock
 */
public class QuotaTableModel extends BaseTableDataModelWithoutFilter implements TableDataModel {

	private List quotaList;

	/**
	 * 
	 */
	public QuotaTableModel() {
		refresh();
	}

	/**
	 * 
	 */
	public void refresh() {
		final QuotaManager qm = QuotaManager.getInstance();
		quotaList = qm.listCustomQuotasKB();
	}

	/**
	 * @param row
	 * @return Quota.
	 */
	public Quota getRowData(final int row) {
		return (Quota) quotaList.get(row);
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return 4;
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getRowCount()
	 */
	@Override
	public int getRowCount() {
		return quotaList.size();
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(final int row, final int col) {
		final Quota q = (Quota) quotaList.get(row);
		switch (col) {
			case 0:
				return q.getPath();
			case 1:
				return q.getQuotaKB();
			case 2:
				return q.getUlLimitKB();
			case 3:
				return "Choose";
			default:
				return "error";
		}
	}

}