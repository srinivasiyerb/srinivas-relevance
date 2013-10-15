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

import org.olat.core.gui.components.table.TableDataModel;

/**
 * @author Christian Guretzki
 */
public class FlexiTableDataModelFactory {

	/**
	 * Factory method to create a FlexiTableDataModel based on an non-flexi table-data-model.
	 * 
	 * @param tableModel
	 * @param tableColumnModel
	 * @return
	 */
	public static FlexiTableDataModel createFlexiTableDataModel(TableDataModel tableModel, FlexiTableColumnModel tableColumnModel) {
		FlexiTableDataModelImpl flexiTableDataModel = new FlexiTableDataModelImpl(tableModel, tableColumnModel);
		return flexiTableDataModel;
	}

	/**
	 * Factory method to create an empty FlexiTableColumnModel. You must add FlexiColumnModel for each table column.
	 * 
	 * @return
	 */
	public static FlexiTableColumnModel createFlexiTableColumnModel() {
		return new FlexiTableColumnModelImpl();
	}

}