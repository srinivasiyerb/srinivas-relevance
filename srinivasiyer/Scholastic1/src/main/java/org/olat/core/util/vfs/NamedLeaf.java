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
 * Copyright (c) 2007 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.core.util.vfs;

import java.io.InputStream;
import java.io.OutputStream;

import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;

/**
 * <h3>Description:</h3> The named leaf takes an existing VFSLeaf and wraps it with another name. This is handy to display items using another name than the real
 * filesystem name. Most methods are delegated to the original VFSLeaf
 * <p>
 * Initial Date: 30.05.2007 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public class NamedLeaf implements VFSLeaf {
	protected final String name;
	protected final VFSLeaf delegate;

	/**
	 * Constructor
	 * 
	 * @param name Name under which the leaf should appear
	 * @param delegate The delegate leaf
	 */
	public NamedLeaf(String name, VFSLeaf delegate) {
		this.name = name;
		this.delegate = delegate;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSLeaf#getInputStream()
	 */
	@Override
	public InputStream getInputStream() {
		return delegate.getInputStream();
	}

	/**
	 * @see org.olat.core.util.vfs.VFSLeaf#getOutputStream(boolean)
	 */
	@Override
	public OutputStream getOutputStream(boolean append) {
		return delegate.getOutputStream(append);
	}

	/**
	 * @see org.olat.core.util.vfs.VFSLeaf#getSize()
	 */
	@Override
	public long getSize() {
		return delegate.getSize();
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#canCopy()
	 */
	@Override
	public VFSStatus canCopy() {
		return delegate.canCopy();
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#canDelete()
	 */
	@Override
	public VFSStatus canDelete() {
		return delegate.canDelete();
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#canRename()
	 */
	@Override
	public VFSStatus canRename() {
		// renaming is not supported
		return VFSConstants.NO;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#canWrite()
	 */
	@Override
	public VFSStatus canWrite() {
		return delegate.canWrite();
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#delete()
	 */
	@Override
	public VFSStatus delete() {
		return delegate.canDelete();
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#getLastModified()
	 */
	@Override
	public long getLastModified() {
		return delegate.getLastModified();
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#getLocalSecurityCallback()
	 */
	@Override
	public VFSSecurityCallback getLocalSecurityCallback() {
		return delegate.getLocalSecurityCallback();
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#getName()
	 */
	@Override
	public String getName() {
		// use the name of the wrapper
		return this.name;
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#getParentContainer()
	 */
	@Override
	public VFSContainer getParentContainer() {
		return delegate.getParentContainer();
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#isSame(org.olat.core.util.vfs.VFSItem)
	 */
	@Override
	public boolean isSame(VFSItem vfsItem) {
		// test on delegate item and not on wrapper
		return delegate.isSame(vfsItem);
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#rename(java.lang.String)
	 */
	@Override
	public VFSStatus rename(String newname) {
		throw new RuntimeException("unsupported");
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#resolve(java.lang.String)
	 */
	@Override
	public VFSItem resolve(String path) {
		return delegate.resolve(delegate.getName());
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#setLocalSecurityCallback(org.olat.core.util.vfs.callbacks.VFSSecurityCallback)
	 */
	@Override
	public void setLocalSecurityCallback(VFSSecurityCallback secCallback) {
		delegate.setLocalSecurityCallback(secCallback);
	}

	/**
	 * @see org.olat.core.util.vfs.VFSItem#setParentContainer(org.olat.core.util.vfs.VFSContainer)
	 */
	@Override
	public void setParentContainer(VFSContainer parentContainer) {
		delegate.setParentContainer(parentContainer);
	}

}
