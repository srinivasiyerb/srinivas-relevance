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

import java.util.Date;

import org.olat.core.commons.persistence.PersistentObject;
import org.olat.resource.OLATResource;

/**
 * @author Felix Jost
 */
public class PolicyImpl extends PersistentObject implements Policy {
	private OLATResource olatResource;
	private SecurityGroup securityGroup;
	private String permission;
	private Date from;
	private Date to;

	/**
	 * package local
	 */
	protected PolicyImpl() {
		//
	}

	/**
	 * @return OLATResource
	 */
	@Override
	public OLATResource getOlatResource() {
		return olatResource;
	}

	/**
	 * @return SecurityGroup
	 */
	@Override
	public SecurityGroup getSecurityGroup() {
		return securityGroup;
	}

	/**
	 * for hibernate only and Manager Sets the olatResource.
	 * 
	 * @param olatResource The olatResource to set
	 */
	void setOlatResource(final OLATResource olatResource) {
		this.olatResource = olatResource;
	}

	/**
	 * for hibernate only and Manager Sets the securityGroup.
	 * 
	 * @param securityGroup The securityGroup to set
	 */
	void setSecurityGroup(final SecurityGroup securityGroup) {
		this.securityGroup = securityGroup;
	}

	/**
	 * @return String
	 */
	@Override
	public String getPermission() {
		return permission;
	}

	/**
	 * Sets the permission.
	 * 
	 * @param permission The permission to set
	 */
	public void setPermission(final String permission) {
		this.permission = permission;
	}

	/**
	 * @return Date from which the policy apply
	 */
	@Override
	public Date getFrom() {
		return from;
	}

	/**
	 * @param from Date from which the policy apply
	 */
	public void setFrom(final Date from) {
		this.from = from;
	}

	/**
	 * @return Limit from which the policy apply
	 */
	@Override
	public Date getTo() {
		return to;
	}

	/**
	 * @param to Limit to which the policy apply
	 */
	public void setTo(final Date to) {
		this.to = to;
	}

	/**
	 * @see org.olat.core.commons.persistence.PersistentObject#toString()
	 */
	@Override
	public String toString() {
		return "secgroupKey:" + securityGroup.getKey() + ", perm:" + permission + ", oresource: " + olatResource.getResourceableTypeName() + ":"
				+ olatResource.getResourceableId() + " (key:" + olatResource.getKey() + "), " + super.toString();
	}

}