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

package org.olat.ims.qti.export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.CustomRenderColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.OLATResourceable;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.assessment.AssessmentUIFactory;
import org.olat.course.assessment.IndentedNodeRenderer;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.IQSELFCourseNode;
import org.olat.course.nodes.IQSURVCourseNode;
import org.olat.course.nodes.IQTESTCourseNode;

/**
 * @author schneider Comment: Archives all QTI results from a specific QTI node in the personal folder of the current user.
 */
public class CourseQTIArchiveController extends BasicController {

	private static final String CMD_SELECT_NODE = "cmd.select.node";

	private final VelocityContainer introVC;

	private TableController nodeListCtr;

	private Controller qawc;
	private CloseableModalController cmc;
	private final OLATResourceable ores;

	private final List nodesTableObjectArrayList;
	private final Link startExportDummyButton;
	private final Link startExportButton;

	/**
	 * Constructor for the assessment tool controller.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param course
	 */
	public CourseQTIArchiveController(final UserRequest ureq, final WindowControl wControl, final OLATResourceable ores) {

		super(ureq, wControl);

		this.ores = ores;

		introVC = this.createVelocityContainer("intro");
		startExportDummyButton = LinkFactory.createButtonSmall("command.start.exportwizard.dummy", introVC, this);
		startExportButton = LinkFactory.createButtonSmall("command.start.exportwizard", introVC, this);

		nodesTableObjectArrayList = doNodeChoose(ureq);

		if (nodesTableObjectArrayList == null) {
			introVC.contextPut("hasQTINodes", Boolean.FALSE);
		} else {
			introVC.contextPut("hasQTINodes", Boolean.TRUE);
		}

		putInitialPanel(introVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		final ICourse course = CourseFactory.loadCourse(ores);
		if (source == startExportButton) {
			qawc = AssessmentUIFactory.createQTIArchiveWizardController(false, ureq, nodesTableObjectArrayList, course, getWindowControl());
		} else if (source == startExportDummyButton) {
			qawc = AssessmentUIFactory.createQTIArchiveWizardController(true, ureq, nodesTableObjectArrayList, course, getWindowControl());
		}
		listenTo(qawc);
		cmc = new CloseableModalController(getWindowControl(), translate("close"), qawc.getInitialComponent());
		cmc.activate();
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == qawc) {
			if (event == Event.DONE_EVENT || event == Event.CANCELLED_EVENT) {
				cmc.deactivate();
			}
		}
	}

	/**
	 * @param ureq
	 * @return
	 */
	private List doNodeChoose(final UserRequest ureq) {
		// table configuraton
		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("nodesoverview.nonodes"));
		tableConfig.setDownloadOffered(false);
		tableConfig.setColumnMovingOffered(false);
		tableConfig.setSortingEnabled(false);
		tableConfig.setDisplayTableHeader(true);
		tableConfig.setDisplayRowCount(false);
		tableConfig.setPageingEnabled(false);

		nodeListCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		listenTo(nodeListCtr);

		// table columns
		nodeListCtr.addColumnDescriptor(new CustomRenderColumnDescriptor("table.header.node", 0, null, ureq.getLocale(), ColumnDescriptor.ALIGNMENT_LEFT,
				new IndentedNodeRenderer()));
		nodeListCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.action.select", 1, CMD_SELECT_NODE, ureq.getLocale()));

		// get list of course node data and populate table data model
		final ICourse course = CourseFactory.loadCourse(ores);
		final CourseNode rootNode = course.getRunStructure().getRootNode();
		final List objectArrayList = addQTINodesAndParentsToList(0, rootNode);

		return objectArrayList;
	}

	/**
	 * Recursive method that adds tasks nodes and all its parents to a list
	 * 
	 * @param recursionLevel
	 * @param courseNode
	 * @return A list of Object[indent, courseNode, selectable]
	 */
	@SuppressWarnings("unchecked")
	private List addQTINodesAndParentsToList(final int recursionLevel, final CourseNode courseNode) {
		// 1) Get list of children data using recursion of this method
		final List childrenData = new ArrayList();
		for (int i = 0; i < courseNode.getChildCount(); i++) {
			final CourseNode child = (CourseNode) courseNode.getChildAt(i);
			final List childData = addQTINodesAndParentsToList((recursionLevel + 1), child);
			if (childData != null) {
				childrenData.addAll(childData);
			}
		}

		if (childrenData.size() > 0 || courseNode instanceof IQTESTCourseNode || courseNode instanceof IQSELFCourseNode || courseNode instanceof IQSURVCourseNode) {
			// Store node data in hash map. This hash map serves as data model for
			// the tasks overview table. Leave user data empty since not used in
			// this table. (use only node data)
			final Map nodeData = new HashMap();
			// indent
			nodeData.put(AssessmentHelper.KEY_INDENT, new Integer(recursionLevel));
			// course node data
			nodeData.put(AssessmentHelper.KEY_TYPE, courseNode.getType());
			nodeData.put(AssessmentHelper.KEY_TITLE_SHORT, courseNode.getShortTitle());
			nodeData.put(AssessmentHelper.KEY_TITLE_LONG, courseNode.getLongTitle());
			nodeData.put(AssessmentHelper.KEY_IDENTIFYER, courseNode.getIdent());

			if (courseNode instanceof IQTESTCourseNode || courseNode instanceof IQSELFCourseNode || courseNode instanceof IQSURVCourseNode) {
				nodeData.put(AssessmentHelper.KEY_SELECTABLE, Boolean.TRUE);
			} else {
				nodeData.put(AssessmentHelper.KEY_SELECTABLE, Boolean.FALSE);
			}

			final List nodeAndChildren = new ArrayList();
			nodeAndChildren.add(nodeData);

			nodeAndChildren.addAll(childrenData);
			return nodeAndChildren;
		}
		return null;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}
}
