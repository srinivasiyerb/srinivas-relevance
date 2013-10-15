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
 * @author Christian Guretzki
 */
public interface FlexiColumnModel {

	public static int ALIGNMENT_LEFT = 1;
	public static int ALIGNMENT_RIGHT = 2;
	public static int ALIGNMENT_CENTER = 3;

	public String getHeaderKey();

	public int getAlignment();

	public void setAlignment(int alignment);

	public void setCellRenderer(FlexiCellRenderer cellRenderer);

	public FlexiCellRenderer getCellRenderer();

}