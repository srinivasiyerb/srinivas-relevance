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

package org.olat.core.gui.components.htmlsite;

import org.olat.core.gui.control.Event;

/**
 * Initial Date: 03.02.2005 <br>
 * 
 * @author Felix Jost
 */
public class OlatCmdEvent extends Event {
	private boolean accepted;
	private final String subcommand;

	/** command identifier for a jumpt to a course node **/
	public static final String GOTONODE_CMD = "gotonode";

	/**
	 * @param command
	 * @param subcommand
	 */
	public OlatCmdEvent(String command, String subcommand) {
		super(command);
		this.subcommand = subcommand;
	}

	/**
	 * @return whether the receiver of this event accepts the olat cmd and is responsible for further dispatching
	 */
	public boolean isAccepted() {
		return accepted;
	}

	/**
	 * 
	 */
	public void accept() {
		this.accepted = true;
	}

	/**
	 * @return String
	 */
	public String getSubcommand() {
		return subcommand;
	}
}
