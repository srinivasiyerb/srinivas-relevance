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
package org.olat.core.commons.services.commentAndRating.impl.ui;

import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.event.MultiUserEvent;

/**
 * Description:<br>
 * This event is fired whenever a user comments count changes on a given resource
 * <P>
 * Initial Date: 01.12.2009 <br>
 * 
 * @author gnaegi
 */
public class UserCommentsCountChangedEvent extends MultiUserEvent {
	private int senderIdentifyer;
	private String oresSubPath;

	/**
	 * Constructor
	 * 
	 * @param sender The sender. Will not be part of the event, only used to remember who sent the event using the object hash code.
	 * @param oresSubPath the sub path
	 */
	public UserCommentsCountChangedEvent(GenericEventListener sender, String oresSubPath) {
		super("commentsCountChanged");
		this.senderIdentifyer = sender.hashCode();
		this.oresSubPath = oresSubPath;
	}

	/**
	 * Method to check if youre the sender of the event
	 * 
	 * @param sender
	 * @return
	 */
	public boolean isSentByMyself(GenericEventListener sender) {
		return (senderIdentifyer == sender.hashCode());
	}

	/**
	 * Get the ores subpath or NULL if not defined
	 * 
	 * @return
	 */
	public String getOresSubPath() {
		return oresSubPath;
	}

}
