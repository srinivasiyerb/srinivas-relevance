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

package org.olat.course.assessment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.olat.core.gui.ShortName;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.table.BooleanColumnDescriptor;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.CustomRenderColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.logging.AssertException;
import org.olat.course.Structure;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * Description:<BR>
 * This controller provides an overview to the users course assessment. Two constructors are available, one for the students read-only view and one for the
 * coach/course-admins assessment tool. In the second case a node can be selected which results in a EVENT_NODE_SELECTED event. <BR>
 * Use the IdentityAssessmentEditController to edit the users assessment data instead of this one.
 * <P>
 * Initial Date: Oct 28, 2004
 * 
 * @author gnaegi
 */
public class IdentityAssessmentOverviewController extends BasicController {

	private static final String CMD_SELECT_NODE = "cmd.select.node";
	/** Event fired when a node has been selected, meaning when a row in the table has been selected **/
	public static final Event EVENT_NODE_SELECTED = new Event("event.node.selected");

	private final Panel main = new Panel("assessmentOverviewPanel");
	private final Structure runStructure;
	private final boolean nodesSelectable;
	private boolean discardEmptyNodes;
	private final boolean allowTableFiltering;
	private NodeAssessmentTableDataModel nodesTableModel;
	private TableController tableFilterCtr;

	private final UserCourseEnvironment userCourseEnvironment;
	private AssessableCourseNode selectedCourseNode;
	private List<ShortName> nodesoverviewTableFilters;
	private ShortName discardEmptyNodesFilter;
	private ShortName showAllNodesFilter;
	private ShortName currentTableFilter;
	private List<Map<String, Object>> preloadedNodesList;
	private final boolean loadNodesFromCourse;

	/**
	 * Constructor for the identity assessment overview controller to be used in the assessment tool or in the users course overview page
	 * 
	 * @param ureq The user request
	 * @param wControl
	 * @param userCourseEnvironment The assessed identitys user course environment
	 * @param nodesSelectable configuration switch: true: user may select the nodes, e.g. to edit the nodes result, false: readonly view (user view)
	 * @param discardEmptyNodes filtering default value: true: do not show nodes that have no value. false: show all assessable nodes
	 * @param allowTableFiltering configuration switch: true: allow user to filter table all nodes/only nodes with data
	 */
	public IdentityAssessmentOverviewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnvironment,
			final boolean nodesSelectable, final boolean discardEmptyNodes, final boolean allowTableFiltering) {
		super(ureq, wControl);
		this.runStructure = userCourseEnvironment.getCourseEnvironment().getRunStructure();
		this.nodesSelectable = nodesSelectable;
		this.discardEmptyNodes = discardEmptyNodes;
		this.allowTableFiltering = allowTableFiltering;
		this.userCourseEnvironment = userCourseEnvironment;
		this.loadNodesFromCourse = true;

		if (this.allowTableFiltering) {
			initNodesoverviewTableFilters();
		}

		doIdentityAssessmentOverview(ureq);
		putInitialPanel(main);
	}

	/**
	 * Internal constructor used by the efficiency statement: uses a precompiled list of node data information instead of fetching everything from the database for each
	 * node
	 * 
	 * @param ureq
	 * @param wControl
	 * @param assessmentCourseNodes List of maps containing the node assessment data using the AssessmentManager keys
	 */
	protected IdentityAssessmentOverviewController(final UserRequest ureq, final WindowControl wControl, final List<Map<String, Object>> assessmentCourseNodes) {
		super(ureq, wControl);
		this.runStructure = null;
		this.nodesSelectable = false;
		this.discardEmptyNodes = true;
		this.allowTableFiltering = false;
		this.userCourseEnvironment = null;
		this.loadNodesFromCourse = false;
		this.preloadedNodesList = assessmentCourseNodes;

		doIdentityAssessmentOverview(ureq);
		putInitialPanel(main);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	@SuppressWarnings("unused")
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// no events to catch
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == tableFilterCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				final TableEvent te = (TableEvent) event;
				final String actionid = te.getActionId();
				if (actionid.equals(CMD_SELECT_NODE)) {
					final int rowid = te.getRowId();
					final Map<String, Object> nodeData = (Map<String, Object>) nodesTableModel.getObject(rowid);
					final CourseNode node = runStructure.getNode((String) nodeData.get(AssessmentHelper.KEY_IDENTIFYER));
					this.selectedCourseNode = (AssessableCourseNode) node;
					// cast should be save, only assessable nodes are selectable
					fireEvent(ureq, EVENT_NODE_SELECTED);
				}
			} else if (event.equals(TableController.EVENT_FILTER_SELECTED)) {
				this.currentTableFilter = tableFilterCtr.getActiveFilter();
				if (this.currentTableFilter.equals(this.discardEmptyNodesFilter)) {
					this.discardEmptyNodes = true;
				} else if (this.currentTableFilter.equals(this.showAllNodesFilter)) {
					this.discardEmptyNodes = false;
				}
				doIdentityAssessmentOverview(ureq);
			}
		}
	}

	private void doIdentityAssessmentOverview(final UserRequest ureq) {
		List<Map<String, Object>> nodesTableList;
		if (loadNodesFromCourse) {
			// get list of course node and user data and populate table data model
			final CourseNode rootNode = runStructure.getRootNode();
			nodesTableList = AssessmentHelper.addAssessableNodeAndDataToList(0, rootNode, userCourseEnvironment, this.discardEmptyNodes, false);
		} else {
			// use list from efficiency statement
			nodesTableList = preloadedNodesList;
		}
		// only populate data model if data available
		if (nodesTableList == null) {
			final String text = translate("nodesoverview.emptylist");
			final Controller messageCtr = MessageUIFactory.createSimpleMessage(ureq, getWindowControl(), text);
			main.setContent(messageCtr.getInitialComponent());
		} else {

			final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
			tableConfig.setDownloadOffered(false);
			tableConfig.setColumnMovingOffered(false);
			tableConfig.setSortingEnabled(false);
			tableConfig.setDisplayTableHeader(true);
			tableConfig.setDisplayRowCount(false);
			tableConfig.setPageingEnabled(false);
			tableConfig.setTableEmptyMessage(translate("nodesoverview.emptylist"));

			removeAsListenerAndDispose(tableFilterCtr);
			if (allowTableFiltering) {
				tableFilterCtr = new TableController(tableConfig, ureq, getWindowControl(), this.nodesoverviewTableFilters, this.currentTableFilter,
						translate("nodesoverview.filter.title"), null, getTranslator());
			} else {
				tableFilterCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
			}
			listenTo(tableFilterCtr);

			// table columns
			tableFilterCtr.addColumnDescriptor(new CustomRenderColumnDescriptor("table.header.node", 0, null, ureq.getLocale(), ColumnDescriptor.ALIGNMENT_LEFT,
					new IndentedNodeRenderer()));
			tableFilterCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.details", 1, null, ureq.getLocale()));
			tableFilterCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.attempts", 2, null, ureq.getLocale(), ColumnDescriptor.ALIGNMENT_RIGHT));
			tableFilterCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.score", 3, null, ureq.getLocale(), ColumnDescriptor.ALIGNMENT_RIGHT));
			tableFilterCtr.addColumnDescriptor(new BooleanColumnDescriptor("table.header.passed", 4, translate("passed.true"), translate("passed.false")));
			// node selection only available if configured
			if (nodesSelectable) {
				tableFilterCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.action.select", 5, CMD_SELECT_NODE, ureq.getLocale()));
			}
			nodesTableModel = new NodeAssessmentTableDataModel(nodesTableList, getTranslator(), nodesSelectable);
			tableFilterCtr.setTableDataModel(nodesTableModel);

			main.setContent(tableFilterCtr.getInitialComponent());
		}
	}

	private void initNodesoverviewTableFilters() {
		// create filter for only nodes with values
		this.discardEmptyNodesFilter = new ShortName() {
			/**
			 * @see org.olat.core.gui.ShortName#getShortName()
			 */
			@Override
			public String getShortName() {
				return translate("nodesoverview.filter.discardEmptyNodes");
			}
		};
		// create filter for all nodes, even with no values
		this.showAllNodesFilter = new ShortName() {
			/**
			 * @see org.olat.core.gui.ShortName#getShortName()
			 */
			@Override
			public String getShortName() {
				return translate("nodesoverview.filter.showEmptyNodes");
			}
		};
		// add this two filter to the filters list
		this.nodesoverviewTableFilters = new ArrayList<ShortName>();
		this.nodesoverviewTableFilters.add(discardEmptyNodesFilter);
		this.nodesoverviewTableFilters.add(showAllNodesFilter);
		// set the current table filter according to configuration
		if (this.discardEmptyNodes) {
			this.currentTableFilter = this.discardEmptyNodesFilter;
		} else {
			this.currentTableFilter = this.showAllNodesFilter;
		}
	}

	/**
	 * Returns the selected assessable course node. Call this method after getting the EVENT_NODE_SELECTED to get the selected node
	 * 
	 * @return AssessableCourseNode
	 */
	public AssessableCourseNode getSelectedCourseNode() {
		if (selectedCourseNode == null) { throw new AssertException(
				"Selected course node was null. Maybe getSelectedCourseNode called prior to EVENT_NODE_SELECTED has been fired?"); }
		return selectedCourseNode;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

}
