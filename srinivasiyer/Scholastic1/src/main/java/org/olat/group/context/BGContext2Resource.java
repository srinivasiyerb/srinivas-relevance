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

package org.olat.group.context;

import org.olat.core.commons.persistence.PersistentObject;
import org.olat.resource.OLATResource;

/**
 * Description:<BR/>
 * Lookup object to bind an olat resource to a group context. This is a n-to-n relation
 * <P/>
 * Initial Date: Aug 18, 2004
 * 
 * @author gnaegi
 */
public class BGContext2Resource extends PersistentObject {

	private BGContext groupContext;
	private OLATResource resource;

	/**
	 * Constructor used by hibernate
	 */
	protected BGContext2Resource() {
		// nothing to be declared
	}

	/**
	 * Package scope constructor. Used by the manager to create a new relation between this olat resource and this group context
	 * 
	 * @param resource
	 * @param groupContext
	 */
	BGContext2Resource(final OLATResource resource, final BGContext groupContext) {
		setResource(resource);
		setGroupContext(groupContext);
	}

	/**
	 * @return The olat resource
	 */
	public OLATResource getResource() {
		return resource;
	}

	/**
	 * @param resource The olat resource
	 */
	public void setResource(final OLATResource resource) {
		this.resource = resource;
	}

	/**
	 * @return The group context
	 */
	public BGContext getGroupContext() {
		return groupContext;
	}

	/**
	 * @param groupContext The group context
	 */
	public void setGroupContext(final BGContext groupContext) {
		this.groupContext = groupContext;
	}
}
