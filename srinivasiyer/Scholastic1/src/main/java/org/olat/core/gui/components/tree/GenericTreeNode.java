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

package org.olat.core.gui.components.tree;

import java.io.Serializable;

import org.olat.core.util.nodes.GenericNode;

/**
 * @author Mike Stock
 */
public class GenericTreeNode extends GenericNode implements TreeNode, Serializable {
	private Object userObject;
	private String title = null;
	private String altText = null;
	private String imageURI = null;
	private boolean accessible = true; // can be clicked?
	private boolean selected = false;
	private TreeNode delegate = null; // if this node is clicked, delegate to that
	private String cssClass;
	private String iconCssClass;
	private String iconDecorator1CssClass;
	private String iconDecorator2CssClass;
	private String iconDecorator3CssClass;
	private String iconDecorator4CssClass;

	/**
	 * @param title
	 * @param userObject
	 */
	public GenericTreeNode(String title, Object userObject) {
		this.title = title;
		this.userObject = userObject;
	}

	/**
	 * 
	 */
	public GenericTreeNode() {
		//
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getTitle()
	 */
	@Override
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getAltText()
	 */
	@Override
	public String getAltText() {
		return altText;
	}

	/**
	 * @param altText
	 */
	public void setAltText(String altText) {
		this.altText = altText;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getImageURI()
	 * @deprecated see org.olat.core.gui.components.tree.TreeNode#getImageURI()
	 */
	@Deprecated
	public String getImageURI() {
		return imageURI;
	}

	/**
	 * @deprecated see org.olat.core.gui.components.tree.TreeNode#getImageURI()
	 * @param imageURI
	 */
	@Deprecated
	public void setImageURI(String imageURI) {
		this.imageURI = imageURI;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#isAccessible()
	 */
	@Override
	public boolean isAccessible() {
		return accessible;
	}

	/**
	 * @param accessible
	 */
	public void setAccessible(boolean accessible) {
		this.accessible = accessible;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getUserObject()
	 */
	@Override
	public Object getUserObject() {
		return userObject;
	}

	/**
	 * @param userObject
	 */
	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}

	/**
	 * @return TreeNode
	 */
	@Override
	public TreeNode getDelegate() {
		return delegate;
	}

	/**
	 * Sets the delegate.
	 * 
	 * @param delegate The delegate to set
	 */
	public void setDelegate(TreeNode delegate) {
		this.delegate = delegate;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getCssClass()
	 */
	@Override
	public String getCssClass() {
		return cssClass;
	}

	/**
	 * @param string
	 */
	public void setCssClass(String string) {
		cssClass = string;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getIconCssClass()
	 */
	@Override
	public String getIconCssClass() {
		return iconCssClass;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getIconDecorator1CssClass()
	 */
	@Override
	public String getIconDecorator1CssClass() {
		return iconDecorator1CssClass;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getIconDecorator2CssClass()
	 */
	@Override
	public String getIconDecorator2CssClass() {
		return iconDecorator2CssClass;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getIconDecorator3CssClass()
	 */
	@Override
	public String getIconDecorator3CssClass() {
		return iconDecorator3CssClass;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getIconDecorator4CssClass()
	 */
	@Override
	public String getIconDecorator4CssClass() {
		return iconDecorator4CssClass;
	}

	public void setIconCssClass(String iconCssClass) {
		this.iconCssClass = iconCssClass;
	}

	public void setIconDecorator1CssClass(String iconDecorator1CssClass) {
		this.iconDecorator1CssClass = iconDecorator1CssClass;
	}

	public void setIconDecorator2CssClass(String iconDecorator2CssClass) {
		this.iconDecorator2CssClass = iconDecorator2CssClass;
	}

	public void setIconDecorator3CssClass(String iconDecorator3CssClass) {
		this.iconDecorator3CssClass = iconDecorator3CssClass;
	}

	@Override
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public boolean isSelected() {
		return selected;
	}

}