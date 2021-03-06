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

import org.olat.core.logging.AssertException;

/**
 * Base class for all non filtered table-data-model.
 * 
 * @author Christian Guretzki
 */
public abstract class BaseTableDataModelWithoutFilter implements TableDataModel {
	@Override
	public Object getObject(final int row) {
		throw new AssertException("getObject not supported for this tableDataModel");
	}

	@Override
	public void setObjects(final List objects) {
		throw new AssertException("setObjects not supported for this tableDataModel");
	}

	@Override
	public Object createCopyWithEmptyList() {
		throw new AssertException("createCopyWithEmptyList not supported for this tableDataModel");
	}

}