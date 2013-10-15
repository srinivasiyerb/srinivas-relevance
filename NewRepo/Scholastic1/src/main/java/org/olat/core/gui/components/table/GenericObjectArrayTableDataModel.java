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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.gui.components.table;

import java.util.List;

/**
 * Description:<BR>
 * Generic table data model for Object[]
 * <P>
 * Initial Date: Feb 24, 2005
 * 
 * @author gnaegi
 */
public class GenericObjectArrayTableDataModel extends DefaultTableDataModel {

	private int columnCount;

	/**
	 * @param objectArrays List of Object[] containing whatever data displayable by the table
	 * @param columnCount Number of elements withing the Object[]
	 */
	public GenericObjectArrayTableDataModel(final List objectArrays, final int columnCount) {
		super(objectArrays);
		this.columnCount = columnCount;
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return columnCount;
	}

	/**
	 * 3 columns: first is the title of the message second the (name, firstname) of the creator third lastModifiedDate
	 */
	@Override
	public final Object getValueAt(final int row, final int col) {
		Object[] objectArray = (Object[]) getObject(row);
		return objectArray[col];
	}

	/**
	 * Set a value of a field in the table data model
	 * 
	 * @param o
	 * @param row
	 * @param col
	 */
	public final void setValueAt(final Object o, final int row, final int col) {
		Object[] objectArray = (Object[]) getObject(row);
		objectArray[col] = o;
	}
}
