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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.core.commons.services.commentAndRating.impl;

import org.olat.core.commons.services.commentAndRating.model.UserCommentsCount;
import org.olat.core.id.OLATResourceable;

/**
 * Description:<br>
 * Allow count on main resource and sub paths
 * <P>
 * Initial Date: 16 nov. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class UserCommentsCountImpl implements UserCommentsCount {

	private Long count;
	private String subPath;
	private OLATResourceable resource;

	public UserCommentsCountImpl() {
		//
	}

	public UserCommentsCountImpl(OLATResourceable resource, String subPath, Long count) {
		this.resource = resource;
		this.subPath = subPath;
		this.count = count;
	}

	@Override
	public OLATResourceable getOlatResource() {
		return resource;
	}

	@Override
	public String getSubPath() {
		return subPath;
	}

	@Override
	public Long getCount() {
		return count;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		return false;
	}

	@Override
	public int hashCode() {
		return (int) (resource.getResourceableId() + resource.getResourceableTypeName().hashCode() + count + (subPath == null ? 643580 : subPath.hashCode()));
	}

	@Override
	public String toString() {
		return resource + "/" + subPath + "(" + count + ")";
	}
}
