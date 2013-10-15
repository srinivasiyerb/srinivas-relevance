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
package org.olat.note;

import org.olat.core.util.event.MultiUserEvent;

/**
 * Description:<br>
 * Any save/update/delete note event.
 * <P>
 * Initial Date: 26.09.2008 <br>
 * 
 * @author Lavinia Dumitrescu
 */
public class NoteEvent extends MultiUserEvent {

	private final String username;

	/**
	 * @param username
	 */
	public NoteEvent(final String username) {
		super("note_event");
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
}
