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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.util.vfs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.core.logging.AssertException;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.core.util.vfs.filters.VFSItemFilter;

/**
 * Description: <br>
 * TODO: Felix Jost Class Description for MultiSource
 * <P>
 * Initial Date: 23.06.2005 <br>
 * 
 * @author Felix Jost
 */
public class MergeSource extends AbstractVirtualContainer {

	private VFSContainer parentContainer;
	private final List mergedContainers;
	private final List mergedContainersChildren;
	private VFSContainer rootWriteContainer;
	private VFSSecurityCallback securityCallback;

	/**
	 * 
	 */
	public MergeSource(VFSContainer parentContainer, String name) {
		super(name);
		this.parentContainer = parentContainer;
		this.mergedContainers = new ArrayList();
		this.mergedContainersChildren = new ArrayList();
	}

	/**
	 * Add container to this merge source. container will show up as its name as a childe of MergeSource.
	 * 
	 * @param container
	 */
	public void addContainer(VFSContainer container) {
		VFSContainer newContainer = container;
		if (isContainerNameTaken(newContainer.getName())) {
			String newName = newContainer.getName() + "_" + CodeHelper.getRAMUniqueID();
			newContainer = new NamedContainerImpl(newName, container);
		}
		// set default filter if container does not already have its own default filter
		if (container.getDefaultItemFilter() != null) {
			container.setDefaultItemFilter(defaultFilter);
			newContainer.setDefaultItemFilter(defaultFilter);
		}
		newContainer.setParentContainer(this);
		mergedContainers.add(newContainer);
	}

	/**
	 * Add all children of this container to the root of this MergeSource.
	 * 
	 * @param container
	 * @param enableWrite If true, writes to the root of this MergeSource are directed to this container.
	 */
	public void addContainersChildren(VFSContainer container, boolean enableWrite) {
		container.setParentContainer(this);
		// set default filter if container does not already have its own default filter
		if (container.getDefaultItemFilter() != null) {
			container.setDefaultItemFilter(defaultFilter);
		}
		// add the container to the list of merged sources
		mergedContainersChildren.add(container);
		if (enableWrite) rootWriteContainer = container;
	}

	/**
	 * Check if the given container is semantically not a child but a container which items have been merged using the addContainersChildren() method.
	 * 
	 * @param container
	 * @return
	 */
	public boolean isContainersChild(VFSContainer container) {
		return mergedContainersChildren.contains(container);
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#getParent()
	 */
	@Override
	public VFSContainer getParentContainer() {
		return parentContainer;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#setParentContainer(org.olat.core.util.vfs.VFSContainer)
	 */
	@Override
	public void setParentContainer(VFSContainer parentContainer) {
		this.parentContainer = parentContainer;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSContainer#getItems()
	 */
	@Override
	public List getItems() {
		return getItems(null);
	}

	/**
	 * @see org.olat.core.util.vfs.VFSContainer#getItems(org.olat.core.util.vfs.filters.VFSItemFilter)
	 */
	@Override
	public List getItems(VFSItemFilter filter) {
		// remember: security callback and parent was already set during add to this MergeSource
		// and refreshed on any setSecurityCallback() so no need to handle the quota of children here.
		List all = new ArrayList();
		if (filter == null && defaultFilter == null) {
			all.addAll(mergedContainers);
		} else {
			// custom filter or default filter is set
			for (Iterator iter = mergedContainers.iterator(); iter.hasNext();) {
				VFSContainer mergedContainer = (VFSContainer) iter.next();
				boolean passedFilter = true;
				// check for default filter
				if (defaultFilter != null && !defaultFilter.accept(mergedContainer)) passedFilter = false;
				// check for custom filter
				if (passedFilter && filter != null && !filter.accept(mergedContainer)) passedFilter = false;
				// only add when both filters passed the test
				if (passedFilter) all.add(mergedContainer);
			}
		}

		for (Iterator iter = mergedContainersChildren.iterator(); iter.hasNext();) {
			VFSContainer container = (VFSContainer) iter.next();
			all.addAll(container.getItems(filter));
		}
		return all;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#canCopyTo()
	 */
	@Override
	public VFSStatus canWrite() {
		if (rootWriteContainer == null) return VFSConstants.NO;
		return rootWriteContainer.canWrite();
	}

	/**
	 * @see org.olat.core.util.vfs.VFSContainer#createChildContainer(java.lang.String)
	 */
	@Override
	public VFSContainer createChildContainer(String name) {
		if (canWrite() != VFSConstants.YES) return null;
		VFSContainer newContainer = rootWriteContainer.createChildContainer(name);
		if (newContainer != null) newContainer.setDefaultItemFilter(defaultFilter);
		return newContainer;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSContainer#createChildLeaf(java.lang.String)
	 */
	@Override
	public VFSLeaf createChildLeaf(String name) {
		if (canWrite() != VFSConstants.YES) return null;
		return rootWriteContainer.createChildLeaf(name);
	}

	/**
	 * @see org.olat.core.util.vfs.VFSContainer#copyFrom(org.olat.core.util.vfs.VFSItem)
	 */
	@Override
	public VFSStatus copyFrom(VFSItem source) {
		if (canWrite() != VFSConstants.YES) throw new AssertException("Cannot create child container in merge source if not writable.");
		return rootWriteContainer.copyFrom(source);
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#resolveFile(java.lang.String)
	 */
	@Override
	public VFSItem resolve(String path) {
		path = VFSManager.sanitizePath(path);
		if (path.equals("/")) return this;

		String childName = VFSManager.extractChild(path);
		for (Iterator iter = mergedContainers.iterator(); iter.hasNext();) {
			VFSContainer container = (VFSContainer) iter.next();
			if (container.getName().equals(childName)) {
				VFSItem vfsItem = container.resolve(path.substring(childName.length() + 1));
				// set default filter on resolved file if it is a container
				if (vfsItem != null && vfsItem instanceof VFSContainer) {
					VFSContainer resolvedContainer = (VFSContainer) vfsItem;
					resolvedContainer.setDefaultItemFilter(defaultFilter);
				}
				return vfsItem;

			}
		}

		for (Iterator iter = mergedContainersChildren.iterator(); iter.hasNext();) {
			VFSContainer container = (VFSContainer) iter.next();
			VFSItem vfsItem = container.resolve(path);
			if (vfsItem != null) {
				// set default filter on resolved file if it is a container
				if (vfsItem instanceof VFSContainer) {
					VFSContainer resolvedContainer = (VFSContainer) vfsItem;
					resolvedContainer.setDefaultItemFilter(defaultFilter);
				}
				return vfsItem;
			}
		}
		return null;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#getLocalSecurityCallback()
	 */
	@Override
	public VFSSecurityCallback getLocalSecurityCallback() {
		return securityCallback;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#setLocalSecurityCallback(org.olat.core.util.vfs.callbacks.VFSSecurityCallback)
	 */
	@Override
	public void setLocalSecurityCallback(VFSSecurityCallback secCallback) {
		securityCallback = secCallback;
	}

	@Override
	public boolean isSame(VFSItem vfsItem) {
		if (rootWriteContainer == null) {
			// Unwriteable merge source (e.g. users private folder), compare on object identity
			return this.equals(vfsItem);
		}
		if (vfsItem instanceof MergeSource) {
			// A writeable merge source, compare on writeable root container
			return rootWriteContainer.equals(((MergeSource) vfsItem).rootWriteContainer);
		}
		return rootWriteContainer.equals(vfsItem);
	}

	public VFSContainer getRootWriteContainer() {
		return rootWriteContainer;
	}

	private boolean isContainerNameTaken(String containerName) {
		for (Iterator iter = mergedContainers.iterator(); iter.hasNext();) {
			VFSContainer mergedContainer = (VFSContainer) iter.next();
			if (mergedContainer.getName().equals(containerName)) return true;

		}
		return false;
	}

}
