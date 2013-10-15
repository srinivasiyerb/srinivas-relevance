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
 * Copyright (c) 2007 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.core.gui.control.generic.ajax.tree;

import org.olat.core.gui.control.Event;

/**
 * <h3>Description:</h3> Event fired when a tree node has been clicked.
 * <p>
 * Initial Date: 30.05.2008 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public class TreeNodeClickedEvent extends Event {
	private String nodeId;

	/**
	 * Constructor
	 * 
	 * @param nodeId ID of the clicked tree node
	 */
	public TreeNodeClickedEvent(String nodeId) {
		super("move");
		this.nodeId = nodeId;
	}

	/**
	 * @return ID of the clicked tree node
	 */
	public String getNodeId() {
		return nodeId;
	}

}
