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

import org.olat.core.id.Identity;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * Initial Date: Jun 23, 2004
 * 
 * @author gnaegi Comment: Wrapper class that contains an identity and the associated score, attempts and oder variables that should be displayed in the user list table.
 */
public class AssessedIdentityWrapper {
	private final UserCourseEnvironment userCourseEnvironment;
	private final Integer nodeAttempts;
	private final String detailsListView;

	/**
	 * Constructor for this identity wrapper object. Wraps an identity with its score and passed values
	 * 
	 * @param userCourseEnvironment the users course environment
	 * @param nodeAttempts the users node attempts for the current node
	 * @param detailsListView the users details for this node
	 */
	public AssessedIdentityWrapper(final UserCourseEnvironment userCourseEnvironment, final Integer nodeAttempts, final String detailsListView) {
		super();
		this.userCourseEnvironment = userCourseEnvironment;
		this.nodeAttempts = nodeAttempts;
		this.detailsListView = detailsListView;
	}

	/**
	 * @return Returns the userCourseEnvironment.
	 */
	public UserCourseEnvironment getUserCourseEnvironment() {
		return userCourseEnvironment;
	}

	/**
	 * Shortcut to get the identity from the course environment
	 * 
	 * @return the identity
	 */
	public Identity getIdentity() {
		return userCourseEnvironment.getIdentityEnvironment().getIdentity();
	}

	/**
	 * @return the users details for the current node
	 */
	public String getDetailsListView() {
		return detailsListView;
	}

	/**
	 * @return the users node attempts for the current node
	 */
	public Integer getNodeAttempts() {
		return nodeAttempts;
	}
}
