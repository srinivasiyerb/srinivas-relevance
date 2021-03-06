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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.core.commons.controllers.filechooser;

import org.olat.core.gui.control.Event;
import org.olat.core.util.vfs.VFSItem;

/**
 * <h3>Description:</h3> The file choosen event signal that a user has selected a file. Get the selected vfs item using the getter method. Use the FileChooserUIFactory to
 * get a relative file path from the event if necessary
 * <p>
 * Initial Date: 13.06.2008 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */

public class FileChoosenEvent extends Event {
	public static final String COMMAND = "fileSelected";
	private VFSItem selectedItem;

	/**
	 * Constructor to create a file choosen event
	 * 
	 * @param selectedItem The choosen item
	 */
	FileChoosenEvent(VFSItem selectedItem) {
		super(COMMAND);
		this.selectedItem = selectedItem;
	}

	/**
	 * Get the vfs item that was selected by the user
	 * 
	 * @return
	 */
	VFSItem getSelectedItem() {
		return this.selectedItem;
	}

}
