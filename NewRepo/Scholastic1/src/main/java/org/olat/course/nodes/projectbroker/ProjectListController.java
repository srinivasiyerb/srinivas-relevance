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

package org.olat.course.nodes.projectbroker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.core.commons.fullWebApp.popup.BaseFullWebappPopupLayoutFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.table.BooleanColumnDescriptor;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.CustomRenderColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.course.nodes.projectbroker.datamodel.CustomField;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.projectbroker.datamodel.ProjectBroker;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerManagerFactory;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerModuleConfiguration;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.group.BusinessGroup;
import org.olat.user.UserInfoMainController;

/**
 * @author guretzki
 */

public class ProjectListController extends BasicController implements GenericEventListener {

	// List commands
	private static final String TABLE_ACTION_SHOW_DETAIL = "cmd.show.detail";
	private static final String TABLE_ACTION_ACCOUNT_MANAGER = "cmd.account.manager";
	private static final String TABLE_ACTION_SELECT = "cmd.select";
	private static final String TABLE_ACTION_CANCEL_SELECT = "cmd.cancel.select";

	private final VelocityContainer contentVC;
	private final Panel mainPanel;
	private ProjectListTableModel projectListTableModel;
	private TableController tableController;
	private Controller projectController;

	private Link createNewProjectButton;

	private final Long courseId;
	private final UserCourseEnvironment userCourseEnv;
	private final NodeEvaluation nodeEvaluation;

	private final ProjectBrokerModuleConfiguration moduleConfig;
	private Long projectBrokerId;
	private int numberOfCustomFieldInTable = 0;
	private int numberOfEventInTable = 0;
	private int nbrSelectedProjects;
	private boolean isParticipantInAnyProject;

	/**
	 * @param ureq
	 * @param wControl
	 * @param userCourseEnv
	 * @param ne
	 * @param previewMode
	 */
	protected ProjectListController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne,
			final boolean previewMode) {
		super(ureq, wControl);
		this.userCourseEnv = userCourseEnv;
		this.nodeEvaluation = ne;
		courseId = userCourseEnv.getCourseEnvironment().getCourseResourceableId();
		moduleConfig = new ProjectBrokerModuleConfiguration(ne.getCourseNode().getModuleConfiguration());

		contentVC = createVelocityContainer("project_list");
		// set header info with project-broker run mode [accept.automatically.limited , accept.manually.limited etc.]
		String infoProjectBrokerRunMode = "";
		if (moduleConfig.isAcceptSelectionManually() && moduleConfig.isAutoSignOut()) {
			infoProjectBrokerRunMode = translate("info.projectbroker.runmode.accept.manually.auto.sign.out", Integer.toString(moduleConfig.getNbrParticipantsPerTopic()));
		} else if (moduleConfig.isAcceptSelectionManually()) {
			if (moduleConfig.getNbrParticipantsPerTopic() == ProjectBrokerModuleConfiguration.NBR_PARTICIPANTS_UNLIMITED) {
				infoProjectBrokerRunMode = translate("info.projectbroker.runmode.accept.manually.unlimited");
			} else {
				infoProjectBrokerRunMode = translate("info.projectbroker.runmode.accept.manually.limited", Integer.toString(moduleConfig.getNbrParticipantsPerTopic()));
			}
		} else {
			if (moduleConfig.getNbrParticipantsPerTopic() == ProjectBrokerModuleConfiguration.NBR_PARTICIPANTS_UNLIMITED) {
				infoProjectBrokerRunMode = translate("info.projectbroker.runmode.accept.automatically.unlimited");
			} else {
				infoProjectBrokerRunMode = translate("info.projectbroker.runmode.accept.automatically.limited",
						Integer.toString(moduleConfig.getNbrParticipantsPerTopic()));
			}
		}
		contentVC.contextPut("infoProjectBrokerRunMode", infoProjectBrokerRunMode);
		mainPanel = new Panel("projectlist_panel");
		final CoursePropertyManager cpm = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		if ((ProjectBrokerManagerFactory.getProjectGroupManager().isAccountManager(ureq.getIdentity(), cpm, ne.getCourseNode()) && !previewMode)
				|| userCourseEnv.getCourseEnvironment().getCourseGroupManager().isIdentityCourseAdministrator(ureq.getIdentity())
				|| ureq.getUserSession().getRoles().isOLATAdmin()) {
			contentVC.contextPut("isAccountManager", true);
			createNewProjectButton = LinkFactory.createButtonSmall("create.new.project.button", contentVC, this);
		} else {
			contentVC.contextPut("isAccountManager", false);
		}
		// push title and learning objectives, only visible on intro page
		contentVC.contextPut("menuTitle", ne.getCourseNode().getShortTitle());
		contentVC.contextPut("displayTitle", ne.getCourseNode().getLongTitle());

		projectBrokerId = ProjectBrokerManagerFactory.getProjectBrokerManager().getProjectBrokerId(cpm, ne.getCourseNode());
		if (projectBrokerId == null) {
			// no project-broker exist => create a new one, happens only once
			final ProjectBroker projectBroker = ProjectBrokerManagerFactory.getProjectBrokerManager().createAndSaveProjectBroker();
			projectBrokerId = projectBroker.getKey();
			ProjectBrokerManagerFactory.getProjectBrokerManager().saveProjectBrokerId(projectBrokerId, cpm, ne.getCourseNode());
			getLogger().info("no project-broker exist => create a new one projectBrokerId=" + projectBrokerId);
		}

		tableController = this.createTableController(ureq, wControl);

		final OLATResourceable projectBroker = ProjectBrokerManagerFactory.getProjectBrokerManager().getProjectBroker(projectBrokerId);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(this, ureq.getIdentity(), projectBroker);
		updateProjectListModelOf(tableController, ureq.getIdentity());
		contentVC.put("projectList", tableController.getInitialComponent());
		mainPanel.setContent(contentVC);

		// jump to either the forum or the folder if the business-launch-path says so.
		final BusinessControl bc = getWindowControl().getBusinessControl();
		final ContextEntry ce = bc.popLauncherContextEntry();
		if (ce != null) { // a context path is left for me
			if (isLogDebugEnabled()) {
				logDebug("businesscontrol (for further jumps) would be: ", bc.toString());
			}
			final OLATResourceable ores = ce.getOLATResourceable();
			if (isLogDebugEnabled()) {
				logDebug("OLATResourceable= ", ores.toString());
			}
			final Long resId = ores.getResourceableId();
			if (resId.longValue() != 0) {
				if (isLogDebugEnabled()) {
					logDebug("projectId=", ores.getResourceableId().toString());
				}

				final Project currentProject = ProjectBrokerManagerFactory.getProjectBrokerManager().getProject(ores.getResourceableId());
				if (currentProject != null) {
					activateProjectController(currentProject, ureq);
				} else {
					// message not found, do nothing. Load normal start screen
					logDebug("Invalid projectId=", ores.getResourceableId().toString());
				}
			} else {
				// FIXME:chg: Should not happen, occurs when course-node are called
				if (isLogDebugEnabled()) {
					logDebug("Invalid projectId=", ores.getResourceableId().toString());
				}
			}
		}

		putInitialPanel(mainPanel);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == createNewProjectButton) {
			String projectTitle = translate("new.project.title");
			int i = 1;
			while (ProjectBrokerManagerFactory.getProjectBrokerManager().existProjectName(projectBrokerId, projectTitle)) {
				projectTitle = translate("new.project.title") + i++;
			}
			final String projectGroupName = translate("project.member.groupname", projectTitle);
			final String projectGroupDescription = translate("project.member.groupdescription", projectTitle);
			final BusinessGroup projectGroup = ProjectBrokerManagerFactory.getProjectGroupManager().createProjectGroupFor(projectBrokerId, ureq.getIdentity(),
					projectGroupName, projectGroupDescription, courseId);
			final Project project = ProjectBrokerManagerFactory.getProjectBrokerManager().createAndSaveProjectFor(projectTitle, projectTitle, projectBrokerId,
					projectGroup);
			ProjectBrokerManagerFactory.getProjectGroupManager().sendGroupChangeEvent(project, courseId, ureq.getIdentity());
			getLogger().debug("Created a new project=" + project);
			projectController = new ProjectController(ureq, this.getWindowControl(), userCourseEnv, nodeEvaluation, project, true, moduleConfig);
			projectController.addControllerListener(this);
			mainPanel.pushContent(projectController.getInitialComponent());
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest urequest, final Controller source, final Event event) {
		if ((source == tableController) && (event instanceof TableEvent)) {
			handleTableEvent(urequest, (TableEvent) event);
		} else if ((source == projectController) && (event == Event.BACK_EVENT)) {
			mainPanel.popContent();
			updateProjectListModelOf(tableController, urequest.getIdentity());
		} else if ((source == projectController) && (event instanceof CancelNewProjectEvent)) {
			final CancelNewProjectEvent cancelEvent = (CancelNewProjectEvent) event;
			getLogger().info("event form cancelled => delete project");
			ProjectBrokerManagerFactory.getProjectBrokerManager().deleteProject(cancelEvent.getProject(), true, userCourseEnv.getCourseEnvironment(),
					nodeEvaluation.getCourseNode());
			mainPanel.popContent();
			updateProjectListModelOf(tableController, urequest.getIdentity());
		}
	}

	private void handleTableEvent(final UserRequest urequest, final TableEvent te) {
		final Project currentProject = (Project) tableController.getTableDataModel().getObject(te.getRowId());
		if (ProjectBrokerManagerFactory.getProjectBrokerManager().existsProject(currentProject.getKey())) {
			handleTableEventForProject(urequest, te, currentProject);
		} else {
			this.showInfo("info.project.nolonger.exist", currentProject.getTitle());
			updateProjectListModelOf(tableController, urequest.getIdentity());
		}
	}

	private void handleTableEventForProject(final UserRequest urequest, final TableEvent te, final Project currentProject) {
		if (te.getActionId().equals(TABLE_ACTION_SHOW_DETAIL)) {
			activateProjectController(currentProject, urequest);
		} else if (te.getActionId().equals(TABLE_ACTION_ACCOUNT_MANAGER)) {
			activateUserController(currentProject, urequest);
		} else if (te.getActionId().equals(TABLE_ACTION_SELECT)) {
			handleEnrollAction(urequest, currentProject);
		} else if (te.getActionId().equals(TABLE_ACTION_CANCEL_SELECT)) {
			handleCancelEnrollmentAction(urequest, currentProject);
		} else {
			getLogger().warn("Controller-event-handling: Unkown event=" + te);
		}
		fireEvent(urequest, te);
	}

	private void handleCancelEnrollmentAction(final UserRequest urequest, final Project currentProject) {
		getLogger().debug("start cancelProjectEnrollmentOf identity=" + urequest.getIdentity() + " to project=" + currentProject);
		final boolean cancelledEnrollmend = ProjectBrokerManagerFactory.getProjectBrokerManager().cancelProjectEnrollmentOf(urequest.getIdentity(), currentProject,
				moduleConfig);
		if (cancelledEnrollmend) {
			ProjectBrokerManagerFactory.getProjectBrokerEmailer().sendCancelEnrollmentEmailToParticipant(urequest.getIdentity(), currentProject, this.getTranslator());
			if (currentProject.isMailNotificationEnabled()) {
				ProjectBrokerManagerFactory.getProjectBrokerEmailer().sendCancelEnrollmentEmailToManager(urequest.getIdentity(), currentProject, this.getTranslator());
			}
			ProjectBrokerManagerFactory.getProjectGroupManager().sendGroupChangeEvent(currentProject, courseId, urequest.getIdentity());
		} else {
			showInfo("info.msg.could.not.cancel.enrollment");
		}
		updateProjectListModelOf(tableController, urequest.getIdentity());
	}

	private void handleEnrollAction(final UserRequest urequest, final Project currentProject) {
		getLogger().debug("start enrollProjectParticipant identity=" + urequest.getIdentity() + " to project=" + currentProject);
		final boolean enrolled = ProjectBrokerManagerFactory.getProjectBrokerManager().enrollProjectParticipant(urequest.getIdentity(), currentProject, moduleConfig,
				nbrSelectedProjects, isParticipantInAnyProject);
		if (enrolled) {
			ProjectBrokerManagerFactory.getProjectBrokerEmailer().sendEnrolledEmailToParticipant(urequest.getIdentity(), currentProject, this.getTranslator());
			if (currentProject.isMailNotificationEnabled()) {
				ProjectBrokerManagerFactory.getProjectBrokerEmailer().sendEnrolledEmailToManager(urequest.getIdentity(), currentProject, this.getTranslator());
			}
			ProjectBrokerManagerFactory.getProjectGroupManager().sendGroupChangeEvent(currentProject, courseId, urequest.getIdentity());
		} else {
			showInfo("info.msg.could.not.enroll");
		}
		updateProjectListModelOf(tableController, urequest.getIdentity());
	}

	private void updateProjectListModelOf(final TableController tableController, final Identity identity) {
		final List<Project> projects = new ArrayList<Project>(ProjectBrokerManagerFactory.getProjectBrokerManager().getProjectListBy(projectBrokerId));
		nbrSelectedProjects = ProjectBrokerManagerFactory.getProjectBrokerManager().getNbrSelectedProjects(identity, projects);
		isParticipantInAnyProject = ProjectBrokerManagerFactory.getProjectBrokerManager().isParticipantInAnyProject(identity, projects);
		projectListTableModel = new ProjectListTableModel(projects, identity, getTranslator(), moduleConfig, numberOfCustomFieldInTable, numberOfEventInTable,
				nbrSelectedProjects, isParticipantInAnyProject);
		tableController.setTableDataModel(projectListTableModel);

	}

	private void activateUserController(final Project projectAt, final UserRequest urequest) {
		if (projectAt.getProjectLeaders().isEmpty()) {
			this.showInfo("show.info.no.project.leader");
		} else if (projectAt.getProjectLeaders().size() > 0) {
			// Open visiting card in new popup
			final ControllerCreator ctrlCreator = new ControllerCreator() {
				@Override
				public Controller createController(final UserRequest lureq, final WindowControl lwControl) {
					return new UserInfoMainController(lureq, lwControl, projectAt.getProjectLeaders().get(0));
				}
			};
			// wrap the content controller into a full header layout
			final ControllerCreator layoutCtrlr = BaseFullWebappPopupLayoutFactory.createAuthMinimalPopupLayout(urequest, ctrlCreator);
			this.openInNewBrowserWindow(urequest, layoutCtrlr);
		}
	}

	private void activateProjectController(final Project project, final UserRequest urequest) {
		removeAsListenerAndDispose(projectController);
		projectController = new ProjectController(urequest, this.getWindowControl(), userCourseEnv, nodeEvaluation, project, false, moduleConfig);
		listenTo(projectController);
		mainPanel.pushContent(projectController.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

	private TableController createTableController(final UserRequest ureq, final WindowControl wControl) {
		numberOfCustomFieldInTable = 0;
		numberOfEventInTable = 0;
		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("projectlist.no.projects"));
		tableConfig.setPreferencesOffered(true, "projectbrokerList");
		tableConfig.setShowAllLinkEnabled(false);// Do not allow show all because many entries takes too long to render

		removeAsListenerAndDispose(tableController);
		tableController = new TableController(tableConfig, ureq, wControl, this.getTranslator(), true);
		listenTo(tableController);

		int dataColumn = 0;
		tableController.addColumnDescriptor(new DefaultColumnDescriptor("projectlist.tableheader.title", dataColumn++, TABLE_ACTION_SHOW_DETAIL, ureq.getLocale()));
		final DefaultColumnDescriptor projectManagerDescriptor = new DefaultColumnDescriptor("projectlist.tableheader.account.manager", dataColumn++,
				TABLE_ACTION_ACCOUNT_MANAGER, ureq.getLocale());
		projectManagerDescriptor.setIsPopUpWindowAction(true, "height=600, width=900, location=no, menubar=no, resizable=yes, status=no, scrollbars=yes, toolbar=no");
		tableController.addColumnDescriptor(projectManagerDescriptor);
		// Custom-Fields
		final List<CustomField> customFieldList = moduleConfig.getCustomFields();
		for (final Iterator iterator = customFieldList.iterator(); iterator.hasNext();) {
			final CustomField customField = (CustomField) iterator.next();
			if (customField.isTableViewEnabled()) {
				numberOfCustomFieldInTable++;
				final DefaultColumnDescriptor columnDescriptor = new DefaultColumnDescriptor(customField.getName(), dataColumn++, null, ureq.getLocale());
				columnDescriptor.setTranslateHeaderKey(false);
				tableController.addColumnDescriptor(columnDescriptor);
			}
		}
		// Project Events
		for (final Project.EventType eventType : Project.EventType.values()) {
			if (moduleConfig.isProjectEventEnabled(eventType) && moduleConfig.isProjectEventTableViewEnabled(eventType)) {
				numberOfEventInTable++;
				tableController.addColumnDescriptor(new CustomRenderColumnDescriptor("projectlist.tableheader.event." + eventType.getI18nKey(), dataColumn++, null, ureq
						.getLocale(), ColumnDescriptor.ALIGNMENT_LEFT, new ProjectEventColumnRenderer()));
			}
		}

		tableController.addColumnDescriptor(new CustomRenderColumnDescriptor("projectlist.tableheader.state", dataColumn++, null, ureq.getLocale(),
				ColumnDescriptor.ALIGNMENT_LEFT, new ProjectStateColumnRenderer()));
		tableController.addColumnDescriptor(new DefaultColumnDescriptor("projectlist.tableheader.numbers", dataColumn++, null, ureq.getLocale()));
		tableController.addColumnDescriptor(new BooleanColumnDescriptor("projectlist.tableheader.select", dataColumn++, TABLE_ACTION_SELECT,
				translate("table.action.select"), "-"));
		tableController.addColumnDescriptor(new BooleanColumnDescriptor("projectlist.tableheader.cancel.select", dataColumn++, TABLE_ACTION_CANCEL_SELECT,
				translate("projectlist.tableheader.cancel.select"), "-"));

		return tableController;

	}

	/**
	 * Is called when a project is deleted via group-management (ProjectBrokerManager.deleteGroupDataFor(BusinessGroup group) , DeletableGroupData-interface)
	 * 
	 * @see org.olat.core.util.event.GenericEventListener#event(org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final Event event) {
		updateProjectListModelOf(tableController, getIdentity());
	}

}
