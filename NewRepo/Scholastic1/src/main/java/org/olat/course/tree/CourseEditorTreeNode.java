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

package org.olat.course.tree;

import java.util.List;

import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.util.nodes.INode;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.CourseNodeFactory;

/**
 * Description:<br>
 * 
 * @author Felix Jost
 */
public class CourseEditorTreeNode extends GenericTreeNode {

	/*
	 * The course node... Important: coursenode's structure is not updated!
	 */
	private final CourseNode cn;

	/*
	 * Status flags
	 */
	private boolean dirty;
	private boolean deleted;
	private boolean newnode;

	/**
	 * @param cn
	 */
	public CourseEditorTreeNode(final CourseNode cn) {
		this.cn = cn;
		setIdent(cn.getIdent());

		dirty = false;
		deleted = false;
		newnode = false;
	}

	/**
	 * @param cetn
	 */
	public CourseEditorTreeNode(final CourseEditorTreeNode cetn) {
		cn = cetn.cn;
		setIdent(cetn.getIdent());

		dirty = cetn.dirty;
		deleted = cetn.deleted;
		newnode = cetn.newnode;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getTitle()
	 */
	@Override
	public String getTitle() {
		return cn.getShortTitle();
	}

	/**
	 * @see org.olat.core.gui.components.tree.GenericTreeNode#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(final String title) {
		throw new UnsupportedOperationException("title is given by associated coursenode's shorttitle");
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getAltText()
	 */
	@Override
	public String getAltText() {
		return cn.getLongTitle() + " (id:" + getIdent() + ")";
	}

	/**
	 * @see org.olat.core.gui.components.tree.GenericTreeNode#setAltText(java.lang.String)
	 */
	@Override
	public void setAltText(final String altText) {
		throw new UnsupportedOperationException("alttext is given by associated coursenode's longtitle");
	}

	/**
	 * @see org.olat.core.gui.components.tree.GenericTreeNode#setImageURI(java.lang.String)
	 */
	@Override
	public void setImageURI(final String imageURI) {
		throw new UnsupportedOperationException("imageuri is calculated by associated coursenode's type");
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getCssClass()
	 */
	@Override
	public String getCssClass() {
		if (deleted) { return "b_deleted"; }
		return null;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getIconCssClass()
	 */
	@Override
	public String getIconCssClass() {
		return CourseNodeFactory.getInstance().getCourseNodeConfiguration(cn.getType()).getIconCSSClass();
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getIconDecorator1CssClass()
	 */
	@Override
	public String getIconDecorator1CssClass() {
		// no decoration top-left
		return null;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getIconDecorator2CssClass()
	 */
	@Override
	public String getIconDecorator2CssClass() {
		// top-right

		final StatusDescription sd = cn.isConfigValid();
		// this one is deleted
		if (deleted) { return "o_middel"; }
		// errors!
		if (sd.isError()) { return "o_miderr"; }
		// ready for publish
		if (!sd.isError() && dirty) { return "o_midpub"; }
		// instead of >>if(hasPublishableChanges()) return "o_midpub";<< because
		return null;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getIconDecorator3CssClass()
	 */
	@Override
	public String getIconDecorator3CssClass() {
		// do not show errors if marked for deletion
		if (deleted) { return null; }
		//
		final StatusDescription sd = cn.isConfigValid();
		// warnings only
		if (sd.isWarning()) { return "o_midwarn"; }
		return null;
	}

	/**
	 * @see org.olat.core.gui.components.tree.TreeNode#getIconDecorator4CssClass()
	 */
	@Override
	public String getIconDecorator4CssClass() {
		// do not show errors if marked for deletion
		if (deleted) { return null; }
		//
		final List conditions = cn.getConditionExpressions();
		if (conditions.size() > 0) { return "o_midlock"; }
		return null;
	}

	/**
	 * @param pos
	 * @return the CourseEditorTreeNode which is the child at position pos
	 */
	public CourseEditorTreeNode getCourseEditorTreeNodeChildAt(final int pos) {
		return (CourseEditorTreeNode) getChildAt(pos);
	}

	/**
	 * @return the attached course node
	 */
	public CourseNode getCourseNode() {
		return cn;
	}

	/**
	 * 
	 */
	public void moveUpInChildlist() {
		final CourseEditorTreeNode parentNode = (CourseEditorTreeNode) getParent();
		if (parentNode == null) { return; }
		final int pos = getPosition();
		if (pos == 0) { return; // upmost children cannot be moved up further
		}
		removeFromParent();
		parentNode.insert(this, pos - 1);
		setDirty(true);
	}

	/**
	 * 
	 */
	public void moveDownInChildlist() {
		final CourseEditorTreeNode parentNode = (CourseEditorTreeNode) getParent();
		if (parentNode == null) { return; }
		final int pos = getPosition();
		if (pos == parentNode.getChildCount() - 1) { return; // latest children
		}
		// cannot be moved down
		// further
		removeFromParent();
		parentNode.insert(this, pos + 1);
		setDirty(true);
	}

	/**
	 * @return true if dirty
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * @param b
	 */
	public void setDirty(final boolean b) {
		dirty = b;
	}

	/**
	 * @return true if deleted
	 */
	public boolean isDeleted() {
		return deleted;
	}

	/**
	 * @param b
	 */
	public void setDeleted(final boolean b) {
		deleted = b;
	}

	/**
	 * @return true if new
	 */
	public boolean isNewnode() {
		return newnode;
	}

	/**
	 * @param b
	 */
	public void setNewnode(final boolean b) {
		newnode = b;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "editorId: " + getIdent() + ", " + cn.toString();
	}

	/**
	 * @return true if this editornode has publishable changes
	 */
	public boolean hasPublishableChanges() {
		boolean configIsValid = !cn.isConfigValid().isError();
		if (configIsValid && !isDeleted() && getParent() != null) {
			/*
			 * if my config is valid I have to check if all parents up to the root are also valid. But only if I am not deleted and I am not the root.
			 */
			configIsValid = isAllParentsConfigValid((CourseEditorTreeNode) getParent());
		}
		final boolean hasDelta = (isDirty() || isNewnode());
		return (configIsValid && hasDelta) || isDeleted();
	}

	/**
	 * recursively ask all nodes towards the root if their config is valid. This is a helper method to decide if a course node has publishable changes.<br>
	 * If one of the course nodes along the path has a config error, all the childrens publishable changes are no longer publishable. A more relaxed version would fail
	 * only if the config error happens in a new course node.
	 * 
	 * @param child
	 * @return <code>true</code> if all course node configurations are valid, <code>false</code> if at least one course node has a config error.
	 */
	private boolean isAllParentsConfigValid(final CourseEditorTreeNode child) {
		final INode parent = child.getParent();
		if (parent == null) {
			return !child.getCourseNode().isConfigValid().isError();
		} else {
			final boolean myConfigIsValid = !child.getCourseNode().isConfigValid().isError();
			if (myConfigIsValid) {
				return isAllParentsConfigValid((CourseEditorTreeNode) parent);
			} else {
				return false;
			}
		}
	}

}
