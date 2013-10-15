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

import org.olat.core.gui.ShortName;
import org.olat.core.id.CreateInfo;
import org.olat.core.id.Persistable;
import org.olat.group.context.BGContext;

/**
 * Description:<BR/>
 * A business group area is used to build a (learning) context for some groups within a group context. A group can be in more than one group areas and an area can have
 * many groups. Scenarios are: learning area 'enrollment' contains group 'class 1' and 'class 2'.
 * <P/>
 * Initial Date: Aug 23, 2004
 * 
 * @author gnaegi
 */
public interface BGArea extends Persistable, CreateInfo, ShortName {
	/** regular expression to check for valid area names */
	// commas are not allowed. name is used in course conditions for weak binding
	public final static String VALID_AREANAME_REGEXP = "^[^,\"]*$";

	/**
	 * @return The group area description
	 */
	public abstract String getDescription();

	/**
	 * @param description the group area description
	 */
	public abstract void setDescription(String description);

	/**
	 * @return The group context
	 */
	public abstract BGContext getGroupContext();

	/**
	 * @param groupContext The group context
	 */
	public abstract void setGroupContext(BGContext groupContext);

	/**
	 * @return The group area name
	 */
	public abstract String getName();

	/**
	 * @param name The group area name
	 */
	public abstract void setName(String name);
}