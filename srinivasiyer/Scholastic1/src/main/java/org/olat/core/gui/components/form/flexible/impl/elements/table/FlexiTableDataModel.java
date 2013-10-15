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

package org.olat.core.gui.components.form.flexible.impl.elements.table;

/**
 * Interface for table data model including column models.
 * 
 * @author Christian Guretzki
 */
public interface FlexiTableDataModel {

	/**
	 * @return Number of table rows.
	 */
	public int getRowCount();

	/**
	 * Return Object for certain table cell.
	 * 
	 * @param row Row number [0...row]
	 * @param col column number [0...column]
	 * @return Object for certain table cell
	 */
	public Object getValueAt(int row, int col);

	/**
	 * Return table-column-model (all columns) for this table-data-model.
	 * 
	 * @return table-column-model
	 */
	public FlexiTableColumnModel getTableColumnModel();

	/**
	 * Set table-column-model (all columns) for this table-data-model.
	 */
	public void setTableColumnModel(FlexiTableColumnModel tableColumnModel);

}