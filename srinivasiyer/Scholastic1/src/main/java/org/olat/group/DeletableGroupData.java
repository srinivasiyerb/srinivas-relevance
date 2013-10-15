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

import java.util.Locale;

/**
 * @author ChristianGuretzki
 */

public interface DeletableGroupData {

	/**
	 * Check if a group is used by deletable-group data element. E.g. projectbroker has references to certain groups
	 * 
	 * @param group Check reference for this group
	 * @param locale locale can be used to translate element-type. E.g. project-broker to Themenboerse
	 * @return DeletableReference object, when the group is used, deleteable.Reference.isReferenced return true
	 */
	public DeletableReference checkIfReferenced(BusinessGroup group, Locale locale);

	/**
	 * Delete data for element which are be used by certain group.
	 * 
	 * @param group Delete data for this group
	 * @return true: data deleted , false: no data deleted
	 */
	public boolean deleteGroupDataFor(BusinessGroup group);

}
