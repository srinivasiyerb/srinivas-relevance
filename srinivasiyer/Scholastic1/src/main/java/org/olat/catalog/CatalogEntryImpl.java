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

package org.olat.catalog;

import org.olat.basesecurity.SecurityGroup;
import org.olat.core.commons.persistence.PersistentObject;
import org.olat.core.logging.AssertException;
import org.olat.repository.RepositoryEntry;

/**
 * Description: <br>
 * Implementation of CatalogEntry
 * 
 * @see org.olat.catalog.CatalogEntry
 * @author Felix Jost
 */
public class CatalogEntryImpl extends PersistentObject implements CatalogEntry {
	private String name;
	private String description;
	private String externalURL;
	private RepositoryEntry repositoryEntry;
	private CatalogEntry parent;

	private SecurityGroup ownerGroup;
	private int type;

	protected CatalogEntryImpl() {
		// for hibernate
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(final String description) {
		this.description = description;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#setName(java.lang.String)
	 */
	@Override
	public void setName(final String name) {
		if (name.length() > 100) { throw new AssertException("CatalogEntry: Name is limited to 100 characters."); }
		this.name = name;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#getRepositoryEntry()
	 */
	@Override
	public RepositoryEntry getRepositoryEntry() {
		return repositoryEntry;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#setRepositoryEntry(org.olat.repository.RepositoryEntry)
	 */
	@Override
	public void setRepositoryEntry(final RepositoryEntry repositoryEntry) {
		this.repositoryEntry = repositoryEntry;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#getOwnerGroup()
	 */
	@Override
	public SecurityGroup getOwnerGroup() {
		return ownerGroup;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#setOwnerGroup(org.olat.basesecurity.SecurityGroup)
	 */
	@Override
	public void setOwnerGroup(final SecurityGroup ownerGroup) {
		this.ownerGroup = ownerGroup;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#getType()
	 */
	@Override
	public int getType() {
		return type;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#setType(int)
	 */
	@Override
	public void setType(final int type) {
		this.type = type;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#getExternalURL()
	 */
	@Override
	public String getExternalURL() {
		return externalURL;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#setExternalURL(java.lang.String)
	 */
	@Override
	public void setExternalURL(final String externalURL) {
		this.externalURL = externalURL;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#getParent()
	 */
	@Override
	public CatalogEntry getParent() {
		return parent;
	}

	/**
	 * @see org.olat.catalog.CatalogEntry#setParent(org.olat.catalog.CatalogEntry)
	 */
	@Override
	public void setParent(final CatalogEntry parent) {
		this.parent = parent;
	}

	/**
	 * @see org.olat.core.commons.persistence.PersistentObject#toString()
	 */
	@Override
	public String toString() {
		return "cat:" + getName() + "=" + super.toString();
	}

	/**
	 * @see org.olat.core.id.OLATResourceablegetResourceableTypeName()
	 */
	@Override
	public String getResourceableTypeName() {
		return this.getClass().getName();
	}

	/**
	 * @see org.olat.core.id.OLATResourceablegetResourceableId()
	 */
	@Override
	public Long getResourceableId() {
		final Long key = getKey();
		if (key == null) { throw new AssertException("no key yet!"); }
		return key;
	}
}