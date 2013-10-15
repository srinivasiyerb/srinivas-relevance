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

package org.olat.group.area;

import org.olat.core.commons.persistence.PersistentObject;
import org.olat.group.BusinessGroup;

/**
 * Description:<BR/>
 * Implementation of the business group to business group area relation
 * <P/>
 * Initial Date: Aug 23, 2004
 * 
 * @author gnaegi
 */
public class BGtoAreaRelationImpl extends PersistentObject implements BGtoAreaRelation {
	private BGArea groupArea;
	private BusinessGroup businessGroup;

	/**
	 * package local
	 */
	protected BGtoAreaRelationImpl() {
		// for hibernate
	}

	BGtoAreaRelationImpl(final BGArea groupArea, final BusinessGroup businessGroup) {
		setBusinessGroup(businessGroup);
		setGroupArea(groupArea);
	}

	/**
	 * @see org.olat.group.area.BGtoAreaRelation#getBusinessGroup()
	 */
	@Override
	public BusinessGroup getBusinessGroup() {
		return businessGroup;
	}

	/**
	 * @see org.olat.group.area.BGtoAreaRelation#setBusinessGroup(org.olat.group.BusinessGroup)
	 */
	@Override
	public void setBusinessGroup(final BusinessGroup businessGroup) {
		this.businessGroup = businessGroup;
	}

	/**
	 * @see org.olat.group.area.BGtoAreaRelation#getGroupArea()
	 */
	@Override
	public BGArea getGroupArea() {
		return groupArea;
	}

	/**
	 * @see org.olat.group.area.BGtoAreaRelation#setGroupArea(org.olat.group.area.BGArea)
	 */
	@Override
	public void setGroupArea(final BGArea groupArea) {
		this.groupArea = groupArea;
	}
}
