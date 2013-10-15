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

package org.olat.course.run.userview;

import java.util.HashMap;
import java.util.Map;

import org.olat.core.gui.components.tree.TreeNode;
import org.olat.course.nodes.CourseNode;

/**
 * Initial Date: Feb 6, 2004
 * 
 * @author Felix Jost
 */
public class TreeEvaluation {
	private final Map courseToTree = new HashMap(); // keys: coursenodes; values: treenodes

	public void cacheCourseToTreeNode(final CourseNode cn, final TreeNode tn) {
		courseToTree.put(cn, tn);
	}

	/**
	 * used to quickly know if a given coursenode is still in the generated treemodel, and if yes, to fetch the treenode so we can reselect the user's selection even
	 * though we just built a new treemodel (which clears the selection of course)
	 * 
	 * @param cn to courseNode to find
	 * @return null if the coursenode has no corresponding treenode in the newly built treemodel anymore (e.g. when the precondition evaluation changed so that the
	 *         coursenode became invisible), or the TreeNode otherwise.
	 */
	public TreeNode getCorrespondingTreeNode(final CourseNode cn) {
		return (TreeNode) courseToTree.get(cn);
	}

}
