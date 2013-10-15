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

package org.olat.group.right;

import java.util.List;

import org.olat.core.id.Identity;
import org.olat.group.BusinessGroup;
import org.olat.group.context.BGContext;

/**
 * Description:<BR>
 * Interface for a business group right manager. The manager proviedes methods to add, remove and find business group rights from/to business groups.
 * <P>
 * Initial Date: Aug 25, 2004
 * 
 * @author gnaegi
 */
public interface BGRightManager {
	/**
	 * buiness group rights are stored as perminssions. all business group right permission must use this prfix to work properly
	 */
	public static final String BG_RIGHT_PREFIX = "bgr.";

	/**
	 * Add a business group right to a business group
	 * 
	 * @param bgRight
	 * @param rightGroup
	 */
	public abstract void addBGRight(String bgRight, BusinessGroup rightGroup);

	/**
	 * Remove a business group right from a business group
	 * 
	 * @param bgRight
	 * @param rightGroup
	 */
	public abstract void removeBGRight(String bgRight, BusinessGroup rightGroup);

	/**
	 * @param bgRight
	 * @param rightGroup
	 * @return true if a group has this business group right, false otherwhise
	 */
	// public abstract boolean hasBGRight(String bgRight, BusinessGroup
	// rightGroup);
	/**
	 * @param bgRight
	 * @param identity
	 * @param bgContext
	 * @return true if an identity is in a group that has this business group right in the given group context
	 */
	public abstract boolean hasBGRight(String bgRight, Identity identity, BGContext bgContext);

	/**
	 * @param rightGroup
	 * @return a list of all business group rights associated with the given business group
	 */
	public abstract List findBGRights(BusinessGroup rightGroup);
}