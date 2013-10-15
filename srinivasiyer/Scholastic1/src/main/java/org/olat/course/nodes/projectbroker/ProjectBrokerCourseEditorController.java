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

import org.olat.admin.quota.QuotaConstants;
import org.olat.admin.securitygroup.gui.GroupController;
import org.olat.admin.securitygroup.gui.IdentitiesAddEvent;
import org.olat.admin.securitygroup.gui.IdentitiesRemoveEvent;
import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.NodeEditController;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.ProjectBrokerCourseNode;
import org.olat.course.nodes.TACourseNode;
import org.olat.course.nodes.ms.MSCourseNodeEditController;
import org.olat.course.nodes.ms.MSEditFormController;
import org.olat.course.nodes.projectbroker.datamodel.ProjectBroker;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerManagerFactory;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerModuleConfiguration;
import org.olat.course.nodes.ta.DropboxForm;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupAddResponse;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.group.ui.BGConfigFlags;
import org.olat.group.ui.BGMailHelper;
import org.olat.modules.ModuleConfiguration;

/**
 * @author guretzki
 */

public class ProjectBrokerCourseEditorController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	// TODO:cg 28.01.2010 no assessment-tool in V1.0
	// public static final String PANE_TAB_CONF_SCORING = "pane.tab.conf.scoring";
	public static final String PANE_TAB_CONF_DROPBOX = "pane.tab.conf.dropbox";
	public static final String PANE_TAB_CONF_MODULES = "pane.tab.conf.modules";
	public static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	private static final String PANE_TAB_OPTIONS = "pane.tab.options";
	private static final String PANE_TAB_ACCOUNT_MANAGEMENT = "pane.tab.accountmanagement";

	private static final String[] paneKeys = { /* PANE_TAB_CONF_SCORING, */PANE_TAB_CONF_DROPBOX, PANE_TAB_CONF_MODULES, PANE_TAB_ACCESSIBILITY };

	private final Long courseId;
	private final ProjectBrokerCourseNode node;
	private final ModuleConfiguration config;
	private final ProjectBrokerModuleConfiguration projectBrokerModuleConfiguration;
	private final BusinessGroup accountManagerGroup;

	private final VelocityContainer accessabilityVC, optionsFormVC, accountManagementFormVC;
	private final VelocityContainer editModules, editDropbox, editScoring;
	private TabbedPane myTabbedPane;
	private int dropboxTabPosition, scoringTabPosition;
	private final ModulesFormController modulesForm;
	private final DropboxForm dropboxForm;
	private final MSEditFormController scoringController;
	private FolderRunController frc;
	// private ConditionEditController dropConditionC, scoringConditionC, returnboxConditionC;
	private final ConditionEditController projectBrokerConditionController;
	private final boolean hasLogEntries;
	private DialogBoxController dialogBoxController;
	private final OptionsFormController optionsForm;
	private GroupController accountManagerGroupController;

	private final Link editScoringConfigButton;

	private final CustomfieldsFormController customfieldsForm;

	private final ProjectEventFormController projectEventForm;

	private CloseableModalController cmc;
	private Long projectBrokerId;

	/**
	 * @param ureq
	 * @param wControl
	 * @param course
	 * @param node
	 * @param groupMgr
	 */
	protected ProjectBrokerCourseEditorController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final ProjectBrokerCourseNode node,
			final CourseGroupManager groupMgr, final UserCourseEnvironment euce) {
		super(ureq, wControl);

		this.node = node;
		// o_clusterOk by guido: save to hold reference to course inside editor
		this.courseId = course.getResourceableId();
		this.config = node.getModuleConfiguration();
		projectBrokerModuleConfiguration = new ProjectBrokerModuleConfiguration(node.getModuleConfiguration());
		final Translator fallbackTranslator = new PackageTranslator(Util.getPackageName(DropboxForm.class), ureq.getLocale(), new PackageTranslator(
				Util.getPackageName(MSCourseNodeEditController.class), ureq.getLocale()));
		final Translator myTranslator = new PackageTranslator(Util.getPackageName(ProjectBrokerCourseEditorController.class), ureq.getLocale(), fallbackTranslator);
		setTranslator(myTranslator);

		// check if a project-broker exists
		final CoursePropertyManager cpm = course.getCourseEnvironment().getCoursePropertyManager();
		projectBrokerId = ProjectBrokerManagerFactory.getProjectBrokerManager().getProjectBrokerId(cpm, node);
		if (projectBrokerId == null) {
			// no project-broker exist => create a new one, happens only once
			final ProjectBroker projectBroker = ProjectBrokerManagerFactory.getProjectBrokerManager().createAndSaveProjectBroker();
			projectBrokerId = projectBroker.getKey();
			ProjectBrokerManagerFactory.getProjectBrokerManager().saveProjectBrokerId(projectBrokerId, cpm, node);
		}

		// Access
		accessabilityVC = this.createVelocityContainer("edit_condition");
		// ProjectBroker precondition
		projectBrokerConditionController = new ConditionEditController(ureq, getWindowControl(), groupMgr, node.getConditionProjectBroker(),
				"projectBrokerConditionForm", AssessmentHelper.getAssessableNodes(course.getEditorTreeModel(), node), euce);
		this.listenTo(projectBrokerConditionController);
		accessabilityVC.put("projectBrokerCondition", projectBrokerConditionController.getInitialComponent());

		// Options with dates and custom-fields
		optionsFormVC = this.createVelocityContainer("optionsForm");
		optionsForm = new OptionsFormController(ureq, wControl, projectBrokerModuleConfiguration, projectBrokerId);
		listenTo(optionsForm);
		optionsFormVC.put("optionsForm", optionsForm.getInitialComponent());
		customfieldsForm = new CustomfieldsFormController(ureq, wControl, projectBrokerModuleConfiguration);
		customfieldsForm.addControllerListener(this);
		optionsFormVC.put("customfieldsForm", customfieldsForm.getInitialComponent());
		projectEventForm = new ProjectEventFormController(ureq, wControl, projectBrokerModuleConfiguration);
		projectEventForm.addControllerListener(this);
		optionsFormVC.put("projectEventForm", projectEventForm.getInitialComponent());

		// Account-Managment
		accountManagementFormVC = this.createVelocityContainer("account_management");
		final String groupName = translate("account.manager.groupname", node.getShortTitle());
		final String groupDescription = translate("account.manager.groupdescription", node.getShortTitle());
		accountManagerGroup = ProjectBrokerManagerFactory.getProjectGroupManager().getAccountManagerGroupFor(cpm, node, course, groupName, groupDescription,
				ureq.getIdentity());
		if (accountManagerGroup != null) {
			accountManagerGroupController = new GroupController(ureq, getWindowControl(), true, false, true, accountManagerGroup.getPartipiciantGroup());
			listenTo(accountManagerGroupController);
			// add mail templates used when adding and removing users
			final MailTemplate ownerAddUserMailTempl = BGMailHelper.createAddParticipantMailTemplate(accountManagerGroup, ureq.getIdentity());
			accountManagerGroupController.setAddUserMailTempl(ownerAddUserMailTempl, false);
			final MailTemplate ownerAremoveUserMailTempl = BGMailHelper.createRemoveParticipantMailTemplate(accountManagerGroup, ureq.getIdentity());
			accountManagerGroupController.setRemoveUserMailTempl(ownerAremoveUserMailTempl, false);
			accountManagementFormVC.put("accountManagementController", accountManagerGroupController.getInitialComponent());
		}

		// Modules config
		editModules = this.createVelocityContainer("editModules");
		modulesForm = new ModulesFormController(ureq, wControl, config);
		listenTo(modulesForm);

		editModules.put("editModules", modulesForm.getInitialComponent());

		// DropBox config (re-used from task-node)
		editDropbox = this.createVelocityContainer("editDropbox");
		editDropbox.setTranslator(myTranslator);
		dropboxForm = new DropboxForm(ureq, wControl, config);
		listenTo(dropboxForm);
		editDropbox.put("dropboxform", dropboxForm.getInitialComponent());

		// Scoring config
		editScoring = this.createVelocityContainer("editScoring");
		editScoringConfigButton = LinkFactory.createButtonSmall("scoring.config.enable.button", editScoring, this);

		scoringController = new MSEditFormController(ureq, wControl, config);
		listenTo(scoringController);
		editScoring.put("scoringController", scoringController.getInitialComponent());

		// if there is already user data available, make for read only
		final UserNodeAuditManager am = course.getCourseEnvironment().getAuditManager();
		hasLogEntries = am.hasUserNodeLogs(node);
		editScoring.contextPut("hasLogEntries", new Boolean(hasLogEntries));
		if (hasLogEntries) {
			scoringController.setDisplayOnly(true);
		}
		// Initialstate
		editScoring.contextPut("isOverwriting", new Boolean(false));

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (getLogger().isDebug()) {
			getLogger().debug("event source=" + source + " " + event.toString());
		}
		if (source == editScoringConfigButton) {
			scoringController.setDisplayOnly(false);
			editScoring.contextPut("isOverwriting", new Boolean(true));
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest urequest, final Controller source, final Event event) {
		if (source == projectBrokerConditionController) {
			if (event == Event.CHANGED_EVENT) {
				node.setConditionProjectBroker(projectBrokerConditionController.getCondition());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == dialogBoxController) {
			if (DialogBoxUIFactory.isOkEvent(event)) {
				// ok: open task folder
				final String relPath = TACourseNode.getTaskFolderPathRelToFolderRoot(CourseFactory.loadCourse(courseId), node);
				final OlatRootFolderImpl rootFolder = new OlatRootFolderImpl(relPath, null);
				final OlatNamedContainerImpl namedFolder = new OlatNamedContainerImpl(translate("taskfolder"), rootFolder);
				namedFolder.setLocalSecurityCallback(new FolderCallback(relPath, false));

				removeAsListenerAndDispose(frc);
				frc = new FolderRunController(namedFolder, false, urequest, getWindowControl());
				listenTo(frc);

				removeAsListenerAndDispose(cmc);
				cmc = new CloseableModalController(getWindowControl(), translate("folder.close"), frc.getInitialComponent());
				listenTo(cmc);

				cmc.activate();
				fireEvent(urequest, Event.CHANGED_EVENT);
			}
		} else if (source == scoringController) {
			if (event == Event.CANCELLED_EVENT) {
				if (hasLogEntries) {
					scoringController.setDisplayOnly(true);
				}
				editScoring.contextPut("isOverwriting", new Boolean(false));
				return;
			} else if (event == Event.DONE_EVENT) {
				scoringController.updateModuleConfiguration(config);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == modulesForm) {
			final boolean onoff = event.getCommand().endsWith("true");
			if (event.getCommand().startsWith("dropbox")) {
				config.set(ProjectBrokerCourseNode.CONF_DROPBOX_ENABLED, onoff);
			} else if (event.getCommand().startsWith("returnbox")) {
				config.set(ProjectBrokerCourseNode.CONF_RETURNBOX_ENABLED, onoff);
			}
			fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			return;
		} else if (source == accountManagerGroupController) {
			final BGConfigFlags flags = BGConfigFlags.createRightGroupDefaultFlags();
			if (event instanceof IdentitiesAddEvent) {
				final IdentitiesAddEvent identitiesAddedEvent = (IdentitiesAddEvent) event;
				final BusinessGroupAddResponse response = BusinessGroupManagerImpl.getInstance().addParticipantsAndFireEvent(urequest.getIdentity(),
						identitiesAddedEvent.getAddIdentities(), accountManagerGroup, flags);
				identitiesAddedEvent.setIdentitiesAddedEvent(response.getAddedIdentities());
				identitiesAddedEvent.setIdentitiesWithoutPermission(response.getIdentitiesWithoutPermission());
				identitiesAddedEvent.setIdentitiesAlreadyInGroup(response.getIdentitiesAlreadyInGroup());
				getLogger().info("Add users as account-managers");
				fireEvent(urequest, Event.CHANGED_EVENT);
			} else if (event instanceof IdentitiesRemoveEvent) {
				BusinessGroupManagerImpl.getInstance().removeParticipantsAndFireEvent(urequest.getIdentity(), ((IdentitiesRemoveEvent) event).getRemovedIdentities(),
						accountManagerGroup, flags);
				getLogger().info("Remove users as account-managers");
				fireEvent(urequest, Event.CHANGED_EVENT);
			}
		} else if (source == optionsForm) {
			if (event == Event.CANCELLED_EVENT) {
				return;
			} else if (event == Event.DONE_EVENT) {
				projectBrokerModuleConfiguration.setNbrParticipantsPerTopic(optionsForm.getNnbrOfAttendees());
				if (projectBrokerModuleConfiguration.isAcceptSelectionManually() && !optionsForm.getSelectionAccept()) {
					// change 'Accept manually' to 'Accept automatically' => enroll all candidates
					ProjectBrokerManagerFactory.getProjectGroupManager().acceptAllCandidates(projectBrokerId, urequest.getIdentity(),
							projectBrokerModuleConfiguration.isAutoSignOut(), optionsForm.getSelectionAccept());
				}
				projectBrokerModuleConfiguration.setAcceptSelectionManaually(optionsForm.getSelectionAccept());
				projectBrokerModuleConfiguration.setSelectionAutoSignOut(optionsForm.getSelectionAutoSignOut());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == customfieldsForm || source == projectEventForm) {
			if (event == Event.CANCELLED_EVENT) {
				return;
			} else if (event == Event.DONE_EVENT) {
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (event == NodeEditController.NODECONFIG_CHANGED_EVENT) {
			getLogger().debug("NODECONFIG_CHANGED_node.shortTitle=" + node.getShortTitle());
			final String groupName = translate("account.manager.groupname", node.getShortTitle());
			final String groupDescription = translate("account.manager.groupdescription", node.getShortTitle());
			ProjectBrokerManagerFactory.getProjectGroupManager().updateAccountManagerGroupName(groupName, groupDescription, accountManagerGroup);
		} else if (source == dropboxForm) {
			if (event == Event.CANCELLED_EVENT) {
				return;
			} else if (event == Event.DONE_EVENT) {
				config.set(ProjectBrokerCourseNode.CONF_DROPBOX_ENABLEMAIL, dropboxForm.mailEnabled());
				config.set(ProjectBrokerCourseNode.CONF_DROPBOX_CONFIRMATION, dropboxForm.getConfirmation());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
				return;
			}
		} else {
			getLogger().warn("Can not handle event in ProjectBrokerCourseEditorController source=" + source + " " + event.toString());
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	@Override
	public void addTabs(final TabbedPane theTabbedPane) {
		this.myTabbedPane = theTabbedPane;
		myTabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), accessabilityVC);
		myTabbedPane.addTab(translate(PANE_TAB_OPTIONS), optionsFormVC);
		myTabbedPane.addTab(translate(PANE_TAB_ACCOUNT_MANAGEMENT), accountManagementFormVC);
		myTabbedPane.addTab(translate(PANE_TAB_CONF_MODULES), editModules);
		dropboxTabPosition = myTabbedPane.addTab(translate(PANE_TAB_CONF_DROPBOX), editDropbox);
		// TODO:cg 28.01.2010 no assessment-tool in V1.0
		// scoringTabPosition = myTabbedPane.addTab(translate(PANE_TAB_CONF_SCORING), editScoring);

		Boolean bool = (Boolean) config.get(ProjectBrokerCourseNode.CONF_DROPBOX_ENABLED);
		myTabbedPane.setEnabled(dropboxTabPosition, (bool != null) ? bool.booleanValue() : true);
		bool = (Boolean) config.get(ProjectBrokerCourseNode.CONF_SCORING_ENABLED);
		// myTabbedPane.setEnabled(scoringTabPosition, (bool != null) ? bool.booleanValue() : true);

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

	@Override
	public String[] getPaneKeys() {
		return paneKeys;
	}

	@Override
	public TabbedPane getTabbedPane() {
		return myTabbedPane;
	}

}

class FolderCallback implements VFSSecurityCallback {

	private final boolean folderLocked;
	private Quota folderQuota = null;

	/**
	 * @param folderLocked
	 */
	public FolderCallback(final String relPath, final boolean folderLocked) {
		this.folderLocked = folderLocked;
		initFolderQuota(relPath);
	}

	private void initFolderQuota(final String relPath) {
		final QuotaManager qm = QuotaManager.getInstance();
		folderQuota = qm.getCustomQuota(relPath);
		if (folderQuota == null) {
			final Quota defQuota = qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_POWER);
			folderQuota = QuotaManager.getInstance().createQuota(relPath, defQuota.getQuotaKB(), defQuota.getUlLimitKB());
		}
	}

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canRead(org.olat.modules.bc.Path)
	 */
	@Override
	public boolean canRead() {
		return true;
	}

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canWrite(org.olat.modules.bc.Path)
	 */
	@Override
	public boolean canWrite() {
		return !folderLocked;
	}

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canDelete(org.olat.modules.bc.Path)
	 */
	@Override
	public boolean canDelete() {
		return !folderLocked;
	}

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canList(org.olat.modules.bc.Path)
	 */
	@Override
	public boolean canList() {
		return true;
	}

	/**
	 * @see org.olat.core.util.vfs.callbacks.VFSSecurityCallback#canCopy()
	 */
	@Override
	public boolean canCopy() {
		return true;
	}

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#getQuotaKB(org.olat.modules.bc.Path)
	 */
	@Override
	public Quota getQuota() {
		return folderQuota;
	}

	/**
	 * @see org.olat.core.util.vfs.callbacks.VFSSecurityCallback#setQuota(org.olat.admin.quota.Quota)
	 */
	@Override
	public void setQuota(final Quota quota) {
		folderQuota = quota;
	}

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#getSubscriptionContext()
	 */
	@Override
	public SubscriptionContext getSubscriptionContext() {
		return null;
	}

	@Override
	public boolean canDeleteRevisionsPermanently() {
		return !folderLocked;
	}
}