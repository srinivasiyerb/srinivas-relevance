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
package org.olat.core.gui.control.generic.folder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.commons.modules.bc.meta.MetaInfoFactory;
import org.olat.core.commons.modules.bc.meta.tagged.MetaTagged;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.OlatRelPathImpl;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.filters.VFSItemFilter;

/**
 * This TreeModel is intended for OlatRootFolderImpl and OlatRootFileImpl. Instances of these classes have usually a MetaInfo object attached, whose title string is used
 * to set the tree node's title (filename if title is empty).<br/>
 * No CSS classes are added.
 * <P>
 * Initial Date: Jul 9, 2009 <br>
 * 
 * @author gwassmann
 */
public class OlatRootFolderTreeModel extends GenericTreeModel {
	/**
	 * used during deserialization to verify that the sender and receiver of a serialized object have loaded classes for that object that are compatible with respect to
	 * serialization
	 * 
	 * @see http://java.sun.com/j2se/1.5.0/docs/api/java/io/Serializable.html
	 */
	static final long serialVersionUID = 1L;

	private VFSItemFilter filter;
	private Comparator<MetaTagged> comparator;

	public OlatRootFolderTreeModel(OlatRootFolderImpl root) {
		setRootNode(createNode(root));
		makeChildren(getRootNode(), root);
	}

	public OlatRootFolderTreeModel(OlatRootFolderImpl root, VFSItemFilter filter) {
		this.filter = filter;
		setRootNode(createNode(root));
		makeChildren(getRootNode(), root);
	}

	public OlatRootFolderTreeModel(OlatRootFolderImpl root, VFSItemFilter filter, Comparator<MetaTagged> comparator) {
		this.filter = filter;
		this.comparator = comparator;
		setRootNode(createNode(root));
		makeChildren(getRootNode(), root);
	}

	/**
	 * Add children to the node
	 * 
	 * @param node
	 * @param root
	 */
	private void makeChildren(GenericTreeNode node, OlatRootFolderImpl root) {
		List<MetaTagged> children = castToMetaTaggables(root.getItems(filter));
		if (comparator != null) {
			Collections.sort(children, comparator);
		}
		for (OlatRelPathImpl child : castToRelPathItems(children)) {
			// create a node for each child and add it
			GenericTreeNode childNode = createNode(child);
			node.addChild(childNode);
			if (child instanceof OlatRootFolderImpl) {
				// add the child's children recursively
				makeChildren(childNode, (OlatRootFolderImpl) child);
			}
		}
	}

	/**
	 * Cast the list of VFSItems to a list of OlatRelPathImpl instances.
	 * 
	 * @param items
	 * @return The OlatRelPathImpl list
	 */
	private List<OlatRelPathImpl> castToRelPathItems(List<MetaTagged> items) {
		List<OlatRelPathImpl> relPathItems = new ArrayList<OlatRelPathImpl>(items.size());
		for (MetaTagged item : items) {
			if (item instanceof OlatRelPathImpl) {
				relPathItems.add((OlatRelPathImpl) item);
			}
		}
		return relPathItems;
	}

	/**
	 * Cast the list of VFSItems to a list of OlatRelPathImpl instances.
	 * 
	 * @param items
	 * @return The OlatRelPathImpl list
	 */
	private List<MetaTagged> castToMetaTaggables(List<VFSItem> items) {
		List<MetaTagged> relPathItems = new ArrayList<MetaTagged>(items.size());
		for (VFSItem item : items) {
			if (item instanceof MetaTagged) {
				relPathItems.add((MetaTagged) item);
			}
		}
		return relPathItems;
	}

	/**
	 * Create a node out of a relative path vfs item. The user object is set to the relative path.
	 * 
	 * @param item
	 */
	private GenericTreeNode createNode(OlatRelPathImpl item) {
		GenericTreeNode node = new GenericTreeNode();
		MetaInfo meta = MetaInfoFactory.createMetaInfoFor(item);
		if (meta != null) {
			String title = meta.getTitle();
			if (StringHelper.containsNonWhitespace(title)) {
				node.setTitle(title);
			} else {
				node.setTitle(meta.getName());
			}
		} else {
			// TODO:GW log warning that
			// "metadate couldn't be loaded for folder relpath: " +
			// folder.getRelPath();
		}
		node.setUserObject(item.getRelPath());
		return node;
	}

	/**
	 * @see org.olat.core.gui.components.tree.GenericTreeModel#getRootNode()
	 */
	@Override
	public GenericTreeNode getRootNode() {
		return (GenericTreeNode) super.getRootNode();
	}

}
