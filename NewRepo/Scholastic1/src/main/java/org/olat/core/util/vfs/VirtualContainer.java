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
 * Copyright (c) 1999-2007 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.util.vfs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.core.util.vfs.filters.VFSItemFilter;

public class VirtualContainer extends AbstractVirtualContainer {

	private List children;
	private VFSSecurityCallback secCallback = null;
	private VFSContainer parentContainer;

	public VirtualContainer(String name) {
		super(name);
		children = new ArrayList();
	}

	/**
	 * Add a VFSItem to this CirtualContainer.
	 * 
	 * @param vfsItem
	 */
	public void addItem(VFSItem vfsItem) {
		children.add(vfsItem);
	}

	@Override
	public List getItems() {
		return getItems(null);
	}

	@Override
	public List getItems(VFSItemFilter filter) {
		if (filter == null) return children;
		else {
			List filtered = new ArrayList();
			for (Iterator iter = children.iterator(); iter.hasNext();) {
				VFSItem vfsItem = (VFSItem) iter.next();
				if (filter.accept(vfsItem)) filtered.add(vfsItem);
			}
			return filtered;
		}
	}

	@Override
	public VFSStatus canWrite() {
		return VFSConstants.NO;
	}

	@Override
	public VFSSecurityCallback getLocalSecurityCallback() {
		return secCallback;
	}

	@Override
	public VFSContainer getParentContainer() {
		return parentContainer;
	}

	@Override
	public boolean isSame(VFSItem vfsItem) {
		return (this == vfsItem);
	}

	@Override
	public VFSItem resolve(String path) {
		return VFSManager.resolveFile(this, path);
	}

	@Override
	public void setLocalSecurityCallback(VFSSecurityCallback secCallback) {
		this.secCallback = secCallback;
	}

	@Override
	public void setParentContainer(VFSContainer parentContainer) {
		this.parentContainer = parentContainer;
	}

}
