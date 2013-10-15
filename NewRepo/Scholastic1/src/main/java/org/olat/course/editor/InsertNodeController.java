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

package org.olat.course.editor;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.tree.SelectionTree;
import org.olat.core.gui.components.tree.TreeEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.id.OLATResourceable;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.CourseNodeConfiguration;
import org.olat.course.nodes.CourseNodeFactory;
import org.olat.course.tree.InsertTreeModel;
import org.olat.course.tree.TreePosition;

/**
 * Description:<br>
 * TODO: guido Class Description for InsertNodeController
 */
public class InsertNodeController extends BasicController {

	private final String type;
	private CourseNode insertedNode;

	private final SelectionTree insertTree;
	private final InsertTreeModel insertModel;
	private final OLATResourceable ores;

	public InsertNodeController(final UserRequest ureq, final WindowControl wControl, final OLATResourceable ores, final String type) {
		super(ureq, wControl);
		this.ores = ores;
		this.type = type;

		insertTree = new SelectionTree("insert_new_node_selection", getTranslator());
		insertTree.setFormButtonKey("insertAtSelectedTreepos");
		insertTree.addListener(this);
		final ICourse course = CourseFactory.getCourseEditSession(ores.getResourceableId());
		insertModel = new InsertTreeModel(course.getEditorTreeModel());
		insertTree.setTreeModel(insertModel);
		final VelocityContainer insertVC = createVelocityContainer("insertNode");
		insertVC.put("selection", insertTree);

		if (insertModel.totalNodeCount() > CourseModule.getCourseNodeLimit()) {
			final String msg = getTranslator().translate("warning.containsXXXormore.nodes",
					new String[] { String.valueOf(insertModel.totalNodeCount()), String.valueOf(CourseModule.getCourseNodeLimit() + 1) });
			final Controller tmp = MessageUIFactory.createWarnMessage(ureq, wControl, null, msg);
			listenTo(tmp);
			insertVC.put("nodelimitexceededwarning", tmp.getInitialComponent());
		}

		this.putInitialPanel(insertVC);
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == insertTree) {
			final TreeEvent te = (TreeEvent) event;
			if (te.getCommand().equals(TreeEvent.COMMAND_TREENODE_CLICKED)) {
				final ICourse course = CourseFactory.getCourseEditSession(ores.getResourceableId());
				// user chose a position to insert a new node
				final String nodeId = te.getNodeId();
				final TreePosition tp = insertModel.getTreePosition(nodeId);
				final int pos = tp.getChildpos();
				final CourseNodeConfiguration newNodeConfig = CourseNodeFactory.getInstance().getCourseNodeConfiguration(type);
				insertedNode = newNodeConfig.getInstance();

				// Set some default values
				final String title = new String(newNodeConfig.getLinkText(ureq.getLocale()));
				insertedNode.setShortTitle(title);
				final String longTitle = new String(translate("longtitle.default") + " " + title);
				insertedNode.setLongTitle(longTitle);
				insertedNode.setNoAccessExplanation(translate("form.noAccessExplanation.default"));

				// Insert it now
				final CourseNode selectedNode = insertModel.getCourseNode(tp.getParentTreeNode());
				course.getEditorTreeModel().insertCourseNodeAt(insertedNode, selectedNode, pos);
				CourseFactory.saveCourseEditorTreeModel(course.getResourceableId());

				fireEvent(ureq, Event.DONE_EVENT);
			} else {
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
		}
	}

	@Override
	protected void doDispose() {
		// nothing to dispose
	}

	public CourseNode getInsertedNode() {
		return insertedNode;
	}

}
