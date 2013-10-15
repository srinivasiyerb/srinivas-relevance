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
package org.olat.instantMessaging.syncservice;

import org.olat.core.id.Identity;
import org.olat.instantMessaging.InstantMessagingModule;

/**
 * Description:<br>
 * Asynchronously synchronizes a single user with the instant messaging server group administration. This has to be done out of scope of the current thread to not disturb
 * the enrolment mechanism which is very time critical when hundred of people try to enrol to a certain group within a short time.
 * <P>
 * Initial Date: 30.04.2008 <br>
 * 
 * @author guido
 */
public class SyncSingleUserTask implements Runnable {

	private final Identity groupowner;
	private final String groupId;
	private final String groupDisplayName;
	private final Identity userToAdd;

	/**
	 * @param groupowner
	 * @param groupId
	 * @param groupDisplayName
	 * @param userToAdd
	 */
	public SyncSingleUserTask(final Identity groupowner, final String groupId, final String groupDisplayName, final Identity userToAdd) {
		super();
		this.groupowner = groupowner;
		this.groupId = groupId;
		this.groupDisplayName = groupDisplayName;
		this.userToAdd = userToAdd;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		InstantMessagingModule.getAdapter().addUserToFriendsRoster(groupowner.getName(), groupId, groupDisplayName, userToAdd.getName());
	}

}
