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

package org.olat.group;

import java.util.Date;

import org.olat.basesecurity.SecurityGroup;
import org.olat.core.commons.persistence.PersistentObject;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.resource.OresHelper;
import org.olat.group.context.BGContext;

/**
 * Description: <br>
 * POJO designed class <br>
 * Implementation for the Interface BusinessGroup. <br>
 * Initial Date: Jul 27, 2004
 * 
 * @author patrick
 */

public class BusinessGroupImpl extends PersistentObject implements BusinessGroup {
	private String description;
	private String name;
	private String type;
	private Integer minParticipants;
	private Integer maxParticipants;
	private SecurityGroup ownerGroup;
	private SecurityGroup partipiciantGroup;
	private SecurityGroup waitingGroup;
	private Date lastUsage;
	private BGContext groupContext;
	private Boolean waitingListEnabled;
	private Boolean autoCloseRanksEnabled;
	private Date lastModified;

	private static final int TYPE_MAXLENGTH = 15;

	/**
	 * constructs an unitialised BusinessGroup, use setXXX for setting attributes
	 */
	public BusinessGroupImpl() {
		// used by spring
	}

	/**
	 * convenience constructor
	 * 
	 * @param type
	 * @param groupName
	 * @param description
	 * @param ownerGroup
	 * @param partipiciantGroup
	 * @param groupContext
	 */
	public BusinessGroupImpl(final String type, final String groupName, final String description, final SecurityGroup ownerGroup, final SecurityGroup partipiciantGroup,
			final SecurityGroup waitingGroup, final BGContext groupContext) {
		this.setName(groupName);
		this.setDescription(description);
		this.setOwnerGroup(ownerGroup);
		this.setPartipiciantGroup(partipiciantGroup);
		this.setGroupContext(groupContext);
		this.setWaitingGroup(waitingGroup);
		this.setType(type);
		// per default no waiting-list
		final Boolean disabled = new Boolean(false);
		this.setWaitingListEnabled(disabled);
		this.setAutoCloseRanksEnabled(disabled);
		this.setLastUsage(new Date());
		this.setLastModified(new Date());
	}

	/**
	 * @param partipiciantGroupP
	 */
	private void setPartipiciantGroup(final SecurityGroup partipiciantGroupP) {
		this.partipiciantGroup = partipiciantGroupP;
	}

	/**
	 * @param ownerGroupP
	 */
	private void setOwnerGroup(final SecurityGroup ownerGroupP) {
		this.ownerGroup = ownerGroupP;
	}

	/**
	 * @param groupName
	 */
	@Override
	public void setName(final String groupName) {
		this.name = groupName;

	}

	/**
	 * @see org.olat.group.BusinessGroup#getDescription()
	 */
	@Override
	public String getDescription() {
		return this.description;
	}

	/**
	 * @see org.olat.group.BusinessGroup#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(final String descriptionP) {
		this.description = descriptionP;
	}

	/**
	 * @see org.olat.group.BusinessGroup#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * @see org.olat.group.BusinessGroup#getOwnerGroup()
	 */
	@Override
	public SecurityGroup getOwnerGroup() {
		return this.ownerGroup;
	}

	/**
	 * @see org.olat.group.BusinessGroup#getPartipiciantGroup()
	 */
	@Override
	public SecurityGroup getPartipiciantGroup() {
		return this.partipiciantGroup;
	}

	/**
	 * @see org.olat.group.BusinessGroup#getWaitingGroup()
	 */
	@Override
	public SecurityGroup getWaitingGroup() {
		return this.waitingGroup;
	}

	/**
	 * @return Returns the lastUsage.
	 */
	@Override
	public java.util.Date getLastUsage() {
		return this.lastUsage;
	}

	/**
	 * set last usage
	 * 
	 * @param lastUsageP
	 */
	@Override
	public void setLastUsage(final java.util.Date lastUsageP) {
		this.lastUsage = lastUsageP;
	}

	/**
	 * @see org.olat.group.BusinessGroup#getType()
	 */
	@Override
	public String getType() {
		return this.type;// BusinessGroupImpl.class.getName();
	}

	/**
	 * @param type2
	 */
	private void setType(final String type2) {
		if (type2.length() > TYPE_MAXLENGTH) { throw new AssertException("businessgrouptype in o_bg_business too long."); }
		this.type = type2;
	}

	/**
	 * @see org.olat.group.BusinessGroup#getDisplayableType(java.util.Locale)
	 */

	/**
	 * @see org.olat.core.id.OLATResourceablegetResourceableTypeName()
	 */
	@Override
	public String getResourceableTypeName() {
		return OresHelper.calculateTypeName(BusinessGroup.class);
	}

	/**
	 * @see org.olat.core.id.OLATResourceablegetResourceableId()
	 */
	@Override
	public Long getResourceableId() {
		return getKey();
	}

	/**
	 * @see org.olat.group.BusinessGroup#getGroupContext()
	 */
	@Override
	public BGContext getGroupContext() {
		return this.groupContext;
	}

	/**
	 * @see org.olat.group.BusinessGroup#setGroupContext(org.olat.group.context.BGContext)
	 */
	@Override
	public void setGroupContext(final BGContext groupContext) {
		this.groupContext = groupContext;
	}

	/**
	 * @see org.olat.group.BusinessGroup#getMaxParticipants()
	 */
	@Override
	public Integer getMaxParticipants() {
		return maxParticipants;
	}

	/**
	 * @see org.olat.group.BusinessGroup#setMaxParticipants(java.lang.Integer)
	 */
	@Override
	public void setMaxParticipants(final Integer maxParticipants) {
		final boolean maxParticipantsChanged = getMaxParticipants() != null && !getMaxParticipants().equals(maxParticipants);
		final int oldMaxParticipants = getMaxParticipants() != null ? getMaxParticipants() : 0;
		this.maxParticipants = maxParticipants;
		if (maxParticipantsChanged) {
			Tracing.logAudit("Max participants value changed for group " + this + " was " + oldMaxParticipants + " changed to " + maxParticipants,
					BusinessGroupImpl.class);
		}
	}

	/**
	 * @see org.olat.group.BusinessGroup#getMinParticipants()
	 */
	@Override
	public Integer getMinParticipants() {
		return minParticipants;
	}

	/**
	 * @see org.olat.group.BusinessGroup#setMinParticipants(java.lang.Integer)
	 */
	@Override
	public void setMinParticipants(final Integer minParticipants) {
		this.minParticipants = minParticipants;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "name=" + name + "::" + "type=" + type + "::" + super.toString();
	}

	@Override
	public void setWaitingGroup(final SecurityGroup waitingGroup) {
		this.waitingGroup = waitingGroup;
	}

	@Override
	public Boolean getAutoCloseRanksEnabled() {
		return autoCloseRanksEnabled;
	}

	@Override
	public void setAutoCloseRanksEnabled(final Boolean autoCloseRanksEnabled) {
		this.autoCloseRanksEnabled = autoCloseRanksEnabled;
	}

	@Override
	public Boolean getWaitingListEnabled() {
		return waitingListEnabled;
	}

	@Override
	public void setWaitingListEnabled(final Boolean waitingListEnabled) {
		this.waitingListEnabled = waitingListEnabled;
	}

	/**
	 * Compares the keys.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		try {
			final BusinessGroupImpl that = (BusinessGroupImpl) obj;
			if (this.getKey().equals(that.getKey())) { return true; }
		} catch (final Exception ex) {
			// nothing to do
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (this.getKey() != null) { return getKey().intValue(); }
		return 0;
	}

	/**
	 * @see org.olat.core.id.ModifiedInfo#getLastModified()
	 */
	@Override
	public Date getLastModified() {
		return lastModified;
	}

	/**
	 * @see org.olat.core.id.ModifiedInfo#setLastModified(java.util.Date)
	 */
	@Override
	public void setLastModified(final Date date) {
		this.lastModified = date;
	}

}