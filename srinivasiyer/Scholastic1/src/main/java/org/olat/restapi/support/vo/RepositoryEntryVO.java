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
package org.olat.restapi.support.vo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Description:<br>
 * TODO: srosse Class Description for RepositoryEntryVO
 * <P>
 * Initial Date: 7 apr. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "repositoryEntryVO")
public class RepositoryEntryVO {
	private Long key;
	private String softkey;
	private String resourcename;
	private String displayname;
	private Long resourceableId;
	private String resourceableTypeName;

	public Long getKey() {
		return key;
	}

	public void setKey(final Long key) {
		this.key = key;
	}

	public String getSoftkey() {
		return softkey;
	}

	public void setSoftkey(final String softkey) {
		this.softkey = softkey;
	}

	public String getResourcename() {
		return resourcename;
	}

	public void setResourcename(final String resourcename) {
		this.resourcename = resourcename;
	}

	public String getDisplayname() {
		return displayname;
	}

	public void setDisplayname(final String displayname) {
		this.displayname = displayname;
	}

	public Long getResourceableId() {
		return resourceableId;
	}

	public void setResourceableId(final Long resourceableId) {
		this.resourceableId = resourceableId;
	}

	public String getResourceableTypeName() {
		return resourceableTypeName;
	}

	public void setResourceableTypeName(final String resourceableTypeName) {
		this.resourceableTypeName = resourceableTypeName;
	}

	@Override
	public String toString() {
		return "RepositoryEntryVO[key=" + key + ":name=" + resourcename + ":display=" + displayname + "]";
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) { return true; }
		if (obj instanceof RepositoryEntryVO) {
			final RepositoryEntryVO vo = (RepositoryEntryVO) obj;
			return key != null && key.equals(vo.key);
		}
		return false;
	}
}
