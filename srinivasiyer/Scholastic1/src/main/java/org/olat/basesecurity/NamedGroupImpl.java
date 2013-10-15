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

package org.olat.basesecurity;

import org.olat.core.commons.persistence.PersistentObject;

/**
 * Description: <br>
 * TODO: Class Description
 * 
 * @author Felix Jost
 */
public class NamedGroupImpl extends PersistentObject implements NamedGroup {
	private String groupName;
	private SecurityGroup securityGroup;

	/**
	 * for hibernate only
	 */
	protected NamedGroupImpl() {
		//
	}

	NamedGroupImpl(final String groupName, final SecurityGroup securityGroup) {
		this.groupName = groupName;
		this.securityGroup = securityGroup;
	}

	/**
	 * @return String
	 */
	@Override
	public String getGroupName() {
		return groupName;
	}

	/**
	 * @return SecurityGroup
	 */
	@Override
	public SecurityGroup getSecurityGroup() {
		return securityGroup;
	}

	/**
	 * for hibernate only
	 * 
	 * @param groupName
	 */
	private void setGroupName(final String groupName) {
		this.groupName = groupName;
	}

	/**
	 * for hibernate only
	 * 
	 * @param securityGroup
	 */
	private void setSecurityGroup(final SecurityGroup securityGroup) {
		this.securityGroup = securityGroup;
	}

	/**
	 * @see org.olat.core.commons.persistence.PersistentObject#toString()
	 */
	@Override
	public String toString() {
		return "groupname:" + groupName + ", " + super.toString();
	}

}