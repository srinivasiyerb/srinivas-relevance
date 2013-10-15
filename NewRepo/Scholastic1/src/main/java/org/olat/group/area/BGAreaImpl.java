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
import org.olat.group.context.BGContext;

/**
 * Description:<BR/>
 * Initial Date: Aug 23, 2004
 * 
 * @author gnaegi
 */
public class BGAreaImpl extends PersistentObject implements BGArea {

	private String name;
	private String description;
	private BGContext groupContext;

	/**
	 * Constructor used for Hibernate instanciation.
	 */
	protected BGAreaImpl() {
		// nothing to do
	}

	BGAreaImpl(final String name, final String description, final BGContext context) {
		setName(name);
		setGroupContext(context);
		setDescription(description);
	}

	/**
	 * @see org.olat.group.area.BGArea#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * @see org.olat.group.area.BGArea#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(final String description) {
		this.description = description;
	}

	/**
	 * @see org.olat.group.area.BGArea#getGroupContext()
	 */
	@Override
	public BGContext getGroupContext() {
		return groupContext;
	}

	/**
	 * @see org.olat.group.area.BGArea#setGroupContext(org.olat.group.context.BGContext)
	 */
	@Override
	public void setGroupContext(final BGContext groupContext) {
		this.groupContext = groupContext;
	}

	/**
	 * @see org.olat.group.area.BGArea#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @see org.olat.group.area.BGArea#setName(java.lang.String)
	 */
	@Override
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "name=" + name + "::" + super.toString();
	}

	/**
	 * @see org.olat.core.gui.ShortName#getShortName()
	 */
	@Override
	public String getShortName() {
		return getName();
	}

	/**
	 * Compares the keys.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		try {
			final BGAreaImpl that = (BGAreaImpl) obj;
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

}
