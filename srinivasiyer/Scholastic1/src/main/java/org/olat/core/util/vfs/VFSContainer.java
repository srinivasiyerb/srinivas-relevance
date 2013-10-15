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

import java.util.List;

import org.olat.core.util.vfs.filters.VFSItemFilter;

/**
 * Description:<br>
 * TODO: Felix Jost Class Description for VFSContainer
 * <P>
 * Initial Date: 23.06.2005 <br>
 * 
 * @author Felix Jost
 */
public interface VFSContainer extends VFSItem {

	/**
	 * @return a list of VFSItem containing
	 */
	public List<VFSItem> getItems();

	/**
	 * @return a list of VFSItem which are accepted by the given filter. If a default filter is set, the default filter will be applied in addition to the given filter in
	 *         this method
	 */
	public List<VFSItem> getItems(VFSItemFilter filter);

	/**
	 * copy either a file or a folder to this folder. e.g. this folder is based at /bla/blu, copying source bli (from /whatever/bli) will add a child bli, so it looks
	 * like /bla/blu/bli later. the operation fails when the source or target do not support canCopyFrom and canCopyTo, resp., or there is already a child with the same
	 * name, or the quota would be exceeded.
	 * 
	 * @param source the source (must exist)
	 * @return the status
	 */
	public VFSStatus copyFrom(VFSItem source);

	/**
	 * Create a new child container (of same type) if possible.
	 * 
	 * @param name
	 * @return VFSItem if successfull, null otherwise.
	 */
	public VFSContainer createChildContainer(String name);

	/**
	 * Create a new leaf (of same type) if possible,
	 * 
	 * @param name
	 * @return VFSItem if successfull, null otherwise.
	 */
	public VFSLeaf createChildLeaf(String name);

	/**
	 * Set a default filter that will be applied to this container getItems method
	 * 
	 * @param defaultFilter
	 */
	public void setDefaultItemFilter(VFSItemFilter defaultFilter);

	/**
	 * @return The default filter for this container or NULL if no filter is set
	 */
	public VFSItemFilter getDefaultItemFilter();

}
