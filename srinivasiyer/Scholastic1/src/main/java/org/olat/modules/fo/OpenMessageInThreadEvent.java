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
package org.olat.modules.fo;

import org.olat.core.gui.control.Event;

/**
 * Description:<br>
 * This event trigger the openning of the message in its thread
 * <P>
 * Initial Date: 18 sept. 2009 <br>
 * 
 * @author srosse
 */
public class OpenMessageInThreadEvent extends Event {
	public static final String OPEN_MSG_IN_THREAD = "open_msg_in_thread";

	private final Message message;

	public OpenMessageInThreadEvent(final Message message) {
		super(OPEN_MSG_IN_THREAD);
		this.message = message;
	}

	public Message getMessage() {
		return message;
	}
}
