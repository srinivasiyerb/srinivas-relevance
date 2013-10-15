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

package org.olat.course.assessment;

import java.util.List;
import java.util.Map;

import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.core.gui.translator.Translator;

/**
 * Initial Date: Jun 23, 2004
 * 
 * @author gnaegi Comment: Use the IndentedNodeRenderer to render the node element!
 */

public class NodeTableDataModel extends DefaultTableDataModel {
	private final Translator trans;

	/**
	 * Constructor for the node table
	 * 
	 * @param objects List maps containting the node data using the keys defined in AssessmentHelper
	 * @param trans The table model translator any node select link
	 */
	public NodeTableDataModel(final List objects, final Translator trans) {
		super(objects);
		this.trans = trans;
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		// node, select
		return 2;
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(final int row, final int col) {
		final Map nodeData = (Map) getObject(row);
		switch (col) {
			case 0:
				// rendered using the indentedNodeRenderer
				return nodeData;
			case 1:
				// selection command
				final Boolean courseNodeEditable = (Boolean) nodeData.get(AssessmentHelper.KEY_SELECTABLE);
				if (courseNodeEditable.booleanValue()) {
					return trans.translate("select");
				} else {
					return null;
				}
			default:
				return "error";
		}
	}

}
