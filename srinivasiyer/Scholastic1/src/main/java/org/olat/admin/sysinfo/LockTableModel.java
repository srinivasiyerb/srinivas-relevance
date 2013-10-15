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

import java.util.Date;
import java.util.List;

import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Formatter;
import org.olat.core.util.coordinate.LockEntry;

/**
 * @author Christian Guretzki
 */

public class LockTableModel extends DefaultTableDataModel {

	/**
	 * @param list of locks
	 */
	public LockTableModel(final List<LockEntry> locks) {
		super(locks);
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return 4;
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(final int row, final int col) {
		final LockEntry lock = (LockEntry) getObject(row);
		switch (col) {
			case 0:
				return lock.getKey();
			case 1:
				return lock.getOwner().getName() + ", " + lock.getOwner().getUser().getProperty(UserConstants.FIRSTNAME, null) + " "
						+ lock.getOwner().getUser().getProperty(UserConstants.LASTNAME, null);
			case 2:
				return Formatter.formatDatetime(new Date(lock.getLockAquiredTime()));
			default:
				return "Error";
		}
	}

}
