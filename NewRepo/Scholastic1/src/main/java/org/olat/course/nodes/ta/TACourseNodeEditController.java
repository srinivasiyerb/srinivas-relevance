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

package org.olat.course.nodes.ta;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.olat.admin.quota.QuotaConstants;
import org.olat.core.commons.modules.bc.FolderEvent;
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
import org.olat.core.gui.render.velocity.VelocityHelper;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailNotificationEditController;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.MailerWithTemplate;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.callbacks.FullAccessWithQuotaCallback;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.condition.Condition;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.NodeEditController;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.TACourseNode;
import org.olat.course.nodes.ms.MSCourseNodeEditController;
import org.olat.course.nodes.ms.MSEditFormController;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.properties.PersistingCoursePropertyManager;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.properties.Property;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

/**
 * Initial Date: 30.08.2004
 * 
 * @author Mike Stock Comment: </pre>
 */

public class TACourseNodeEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	private final OLog log = Tracing.createLoggerFor(this.getClass());

	public static final String PANE_TAB_CONF_SCORING = "pane.tab.conf.scoring";

	public static final String PANE_TAB_CONF_DROPBOX = "pane.tab.conf.dropbox";

	public static final String PANE_TAB_CONF_TASK = "pane.tab.conf.task";

	public static final String PANE_TAB_CONF_MODULES = "pane.tab.conf.modules";

	public static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";

	public static final String PANE_TAB_SOLUTION = "pane.tab.solution";

	private static final String[] paneKeys = { PANE_TAB_SOLUTION, PANE_TAB_CONF_SCORING, PANE_TAB_CONF_DROPBOX, PANE_TAB_CONF_TASK, PANE_TAB_CONF_MODULES,
			PANE_TAB_ACCESSIBILITY };

	private final ICourse course;
	private final TACourseNode node;
	private final ModuleConfiguration config;

	private final VelocityContainer accessabilityVC, solutionVC;
	private final VelocityContainer editModules, editTask, editDropbox, editScoring;
	private TabbedPane myTabbedPane;
	private int taskTabPosition, dropboxTabPosition, scoringTabPosition, solutionTabPosition;
	private final ModulesForm modulesForm;
	private final TaskFormController taskController;
	private final DropboxForm dropboxForm;
	private final MSEditFormController scoringController;
	private FolderRunController frc;
	private final ConditionEditController taskConditionC, dropConditionC, returnboxConditionC, scoringConditionC, solutionConditionC;
	private final boolean hasLogEntries;
	private DialogBoxController dialogBoxController;

	private final Link btfButton;
	private final Link editScoringConfigButton;
	private final Link vfButton;

	private MailNotificationEditController mailCtr;
	private CloseableModalController cmc;
	private List<Identity> identitiesToBeNotified;

	/**
	 * @param ureq
	 * @param wControl
	 * @param course
	 * @param node
	 * @param groupMgr
	 */
	public TACourseNodeEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final TACourseNode node,
			final CourseGroupManager groupMgr, final UserCourseEnvironment euce) {
		super(ureq, wControl);

		this.node = node;
		// o_clusterOk by guido: save to hold reference to course inside editor
		this.course = course;
		this.config = node.getModuleConfiguration();
		final Translator newTranslator = new PackageTranslator(Util.getPackageName(TACourseNodeEditController.class), ureq.getLocale(), new PackageTranslator(
				Util.getPackageName(MSCourseNodeEditController.class), ureq.getLocale()));
		setTranslator(newTranslator);

		accessabilityVC = this.createVelocityContainer("edit");
		// Task precondition
		taskConditionC = new ConditionEditController(ureq, getWindowControl(), groupMgr, node.getConditionTask(), "taskConditionForm",
				AssessmentHelper.getAssessableNodes(course.getEditorTreeModel(), node), euce);
		this.listenTo(taskConditionC);
		if (((Boolean) config.get(TACourseNode.CONF_TASK_ENABLED)).booleanValue()) {
			accessabilityVC.put("taskCondition", taskConditionC.getInitialComponent());
		}

		// DropBox precondition
		dropConditionC = new ConditionEditController(ureq, getWindowControl(), groupMgr, node.getConditionDrop(), "dropConditionForm",
				AssessmentHelper.getAssessableNodes(course.getEditorTreeModel(), node), euce);
		this.listenTo(dropConditionC);
		final Boolean hasDropboxValue = ((Boolean) config.get(TACourseNode.CONF_DROPBOX_ENABLED) != null) ? (Boolean) config.get(TACourseNode.CONF_DROPBOX_ENABLED)
				: false;
		if (hasDropboxValue) {
			accessabilityVC.put("dropCondition", dropConditionC.getInitialComponent());
		}

		// returnbox precondition - use dropbox condition if none defined for rnew Boolean(task.isSelected(0)));boolean returnBoxEnabled = (returnBoxConf !=null) ?
		// ((Boolean) returneturnbox
		final Condition dropboxCondition = node.getConditionDrop();
		Condition returnboxCondition = node.getConditionReturnbox();
		if (dropboxCondition != null && returnboxCondition != null && returnboxCondition.getConditionExpression() == null) {
			// old courses: use ConditionExpression from dropbox if none defined for returnbox
			returnboxCondition = dropboxCondition;
			returnboxCondition.setConditionId(TACourseNode.ACCESS_RETURNBOX);
			node.setConditionReturnbox(returnboxCondition);
		}
		returnboxConditionC = new ConditionEditController(ureq, getWindowControl(), groupMgr, returnboxCondition, "returnboxConditionForm",
				AssessmentHelper.getAssessableNodes(course.getEditorTreeModel(), node), euce);
		this.listenTo(returnboxConditionC);
		final Object returnBoxConf = config.get(TACourseNode.CONF_RETURNBOX_ENABLED);
		// use the dropbox config if none specified for the return box
		final boolean returnBoxEnabled = (returnBoxConf != null) ? ((Boolean) returnBoxConf).booleanValue() : hasDropboxValue;
		if (returnBoxEnabled) {
			accessabilityVC.put("returnboxCondition", returnboxConditionC.getInitialComponent());
		}

		// Scoring precondition
		scoringConditionC = new ConditionEditController(ureq, getWindowControl(), groupMgr, node.getConditionScoring(), "scoringConditionForm",
				AssessmentHelper.getAssessableNodes(course.getEditorTreeModel(), node), euce);
		this.listenTo(scoringConditionC);
		if (((Boolean) config.get(TACourseNode.CONF_SCORING_ENABLED)).booleanValue()) {
			accessabilityVC.put("scoringCondition", scoringConditionC.getInitialComponent());
		}

		// SolutionFolder precondition
		solutionConditionC = new ConditionEditController(ureq, getWindowControl(), groupMgr, node.getConditionSolution(), "solutionConditionForm",
				AssessmentHelper.getAssessableNodes(course.getEditorTreeModel(), node), euce);
		this.listenTo(solutionConditionC);
		if (((Boolean) config.get(TACourseNode.CONF_SOLUTION_ENABLED)).booleanValue()) {
			accessabilityVC.put("solutionCondition", solutionConditionC.getInitialComponent());
		}

		// Modules config
		editModules = this.createVelocityContainer("editModules");
		modulesForm = new ModulesForm(ureq, wControl, config);
		listenTo(modulesForm);
		editModules.put("modulesform", modulesForm.getInitialComponent());

		// Task config
		editTask = this.createVelocityContainer("editTask");
		btfButton = LinkFactory.createButton("taskfolder", editTask, this);

		taskController = new TaskFormController(ureq, wControl, config);
		listenTo(taskController);
		final String taskFolderPath = (String) node.getModuleConfiguration().get(TACourseNode.CONF_TASK_FOLDER_REL_PATH);
		if (taskFolderPath == null) {
			editTask.contextPut("taskfolder", translate("taskfolder.empty"));
		} else {
			editTask.contextPut("taskfolder", taskFolderPath);
		}
		editTask.put("taskform", taskController.getInitialComponent());

		// DropBox config
		editDropbox = this.createVelocityContainer("editDropbox");
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

		// Solution-Tab
		solutionVC = this.createVelocityContainer("editSolutionFolder");
		vfButton = LinkFactory.createButton("link.solutionFolder", solutionVC, this);

	}

	private VFSSecurityCallback getTaskFolderSecCallback(final String relPath) {
		// check if any tasks assigned yet
		final CoursePropertyManager cpm = PersistingCoursePropertyManager.getInstance(course);
		final List assignedProps = cpm.listCourseNodeProperties(node, null, null, TaskController.PROP_ASSIGNED);
		// return new TaskFolderCallback(relPath, (assignedProps.size() > 0));
		return new TaskFolderCallback(relPath, false); // do not look task folder
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (log.isDebug()) {
			log.debug("event source=" + source + " " + event.toString());
		}
		if (source == btfButton) {
			// check if there are already assigned tasks
			final CoursePropertyManager cpm = PersistingCoursePropertyManager.getInstance(course);
			final List assignedProps = cpm.listCourseNodeProperties(node, null, null, TaskController.PROP_ASSIGNED);
			if (assignedProps.size() == 0) {
				// no task assigned
				final String relPath = TACourseNode.getTaskFolderPathRelToFolderRoot(course, node);
				final OlatRootFolderImpl rootFolder = new OlatRootFolderImpl(relPath, null);
				final OlatNamedContainerImpl namedFolder = new OlatNamedContainerImpl(translate("taskfolder"), rootFolder);
				namedFolder.setLocalSecurityCallback(getTaskFolderSecCallback(relPath));
				frc = new FolderRunController(namedFolder, false, ureq, getWindowControl());
				// listenTo(frc);
				frc.addControllerListener(this);
				final CloseableModalController cmc = new CloseableModalController(getWindowControl(), translate("folder.close"), frc.getInitialComponent());
				cmc.activate();
			} else {
				// already assigned task => open dialog with warn
				final String[] args = new String[] { new Integer(assignedProps.size()).toString() };
				dialogBoxController = this.activateOkCancelDialog(ureq, "", getTranslator().translate("taskfolder.overwriting.confirm", args), dialogBoxController);
				final List cs = new ArrayList();
				cs.add(dialogBoxController);
			}
		} else if (source == vfButton) {
			if (log.isDebug()) {
				log.debug("Event for sampleVC");
			}
			// switch to new dialog
			final OlatNamedContainerImpl namedContainer = TACourseNode.getNodeFolderContainer(node, course.getCourseEnvironment());
			Quota quota = QuotaManager.getInstance().getCustomQuota(namedContainer.getRelPath());
			if (quota == null) {
				final Quota defQuota = QuotaManager.getInstance().getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_NODES);
				quota = QuotaManager.getInstance().createQuota(namedContainer.getRelPath(), defQuota.getQuotaKB(), defQuota.getUlLimitKB());
			}
			final VFSSecurityCallback secCallback = new FullAccessWithQuotaCallback(quota);
			namedContainer.setLocalSecurityCallback(secCallback);
			final CloseableModalController cmc = new CloseableModalController(getWindowControl(), translate("close"), new FolderRunController(namedContainer, false,
					ureq, getWindowControl()).getInitialComponent());
			cmc.activate();

			if (log.isDebug()) {
				log.debug("Switch to sample folder dialog : DONE");
			}
			return;
		} else if (source == editScoringConfigButton) {
			scoringController.setDisplayOnly(false);
			editScoring.contextPut("isOverwriting", new Boolean(true));
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest urequest, final Controller source, final Event event) {
		if (source == modulesForm) {
			final boolean onoff = event.getCommand().endsWith("true");
			if (event.getCommand().startsWith("task")) {
				config.set(TACourseNode.CONF_TASK_ENABLED, new Boolean(onoff));
				myTabbedPane.setEnabled(taskTabPosition, onoff);
				if (onoff) {
					accessabilityVC.put("taskCondition", taskConditionC.getInitialComponent());
				} else {
					accessabilityVC.remove(taskConditionC.getInitialComponent());
				}
			} else if (event.getCommand().startsWith("dropbox")) {
				config.set(TACourseNode.CONF_DROPBOX_ENABLED, new Boolean(onoff));
				myTabbedPane.setEnabled(dropboxTabPosition, onoff);
				if (onoff) {
					accessabilityVC.put("dropCondition", dropConditionC.getInitialComponent());
				} else {
					accessabilityVC.remove(dropConditionC.getInitialComponent());
				}
			} else if (event.getCommand().startsWith("returnbox")) {
				config.set(TACourseNode.CONF_RETURNBOX_ENABLED, new Boolean(onoff));
				if (onoff) {
					accessabilityVC.put("returnboxCondition", returnboxConditionC.getInitialComponent());
				} else {
					accessabilityVC.remove(returnboxConditionC.getInitialComponent());
				}
			} else if (event.getCommand().startsWith("scoring")) {
				config.set(TACourseNode.CONF_SCORING_ENABLED, new Boolean(onoff));
				myTabbedPane.setEnabled(scoringTabPosition, onoff);
				if (onoff) {
					accessabilityVC.put("scoringCondition", scoringConditionC.getInitialComponent());
				} else {
					accessabilityVC.remove(scoringConditionC.getInitialComponent());
				}
			} else if (event.getCommand().startsWith("solution")) {
				config.set(TACourseNode.CONF_SOLUTION_ENABLED, new Boolean(onoff));
				myTabbedPane.setEnabled(solutionTabPosition, onoff);
				if (onoff) {
					accessabilityVC.put("solutionCondition", solutionConditionC.getInitialComponent());
				} else {
					accessabilityVC.remove(solutionConditionC.getInitialComponent());
				}
			}

			fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			return;

		} else if (source == taskConditionC) {
			if (event == Event.CHANGED_EVENT) {
				node.setConditionTask(taskConditionC.getCondition());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == dropConditionC) {
			if (event == Event.CHANGED_EVENT) {
				node.setConditionDrop(dropConditionC.getCondition());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == returnboxConditionC) {
			if (event == Event.CHANGED_EVENT) {
				node.setConditionReturnbox(returnboxConditionC.getCondition());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == scoringConditionC) {
			if (event == Event.CHANGED_EVENT) {
				node.setConditionScoring(scoringConditionC.getCondition());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == solutionConditionC) {
			if (event == Event.CHANGED_EVENT) {
				node.setConditionSolution(solutionConditionC.getCondition());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == dialogBoxController) {
			if (DialogBoxUIFactory.isOkEvent(event)) {
				// ok: open task folder
				final String relPath = TACourseNode.getTaskFolderPathRelToFolderRoot(course, node);
				final OlatRootFolderImpl rootFolder = new OlatRootFolderImpl(relPath, null);
				final OlatNamedContainerImpl namedFolder = new OlatNamedContainerImpl(translate("taskfolder"), rootFolder);
				namedFolder.setLocalSecurityCallback(getTaskFolderSecCallback(relPath));
				frc = new FolderRunController(namedFolder, false, urequest, getWindowControl());
				listenTo(frc);
				final CloseableModalController cmc = new CloseableModalController(getWindowControl(), translate("folder.close"), frc.getInitialComponent());
				cmc.activate();
				fireEvent(urequest, Event.CHANGED_EVENT);
			}
		} else if (source == taskController) {
			if (event == Event.CANCELLED_EVENT) {
				return;
			} else if (event == Event.DONE_EVENT) {
				config.set(TACourseNode.CONF_TASK_TYPE, taskController.getTaskType());
				config.set(TACourseNode.CONF_TASK_TEXT, taskController.getOptionalText());
				config.set(TACourseNode.CONF_TASK_SAMPLING_WITH_REPLACEMENT, new Boolean(taskController.getIsSamplingWithReplacement()));
				config.setBooleanEntry(TACourseNode.CONF_TASK_PREVIEW, taskController.isTaskPreviewMode());
				config.setBooleanEntry(TACourseNode.CONF_TASK_DESELECT, taskController.isTaskDeselectMode());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
				return;
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
		} else if (source == dropboxForm) {
			if (event == Event.CANCELLED_EVENT) {
				return;
			} else if (event == Event.DONE_EVENT) {
				config.set(TACourseNode.CONF_DROPBOX_ENABLEMAIL, new Boolean(dropboxForm.mailEnabled()));
				config.set(TACourseNode.CONF_DROPBOX_CONFIRMATION, dropboxForm.getConfirmation());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
				return;
			}
		} else if (source == frc && (event instanceof FolderEvent) && event.getCommand().equals(FolderEvent.DELETE_EVENT)) {
			final String deletedTaskFile = getFileListAsComaSeparated(((FolderEvent) event).getFilename());
			// cancel task assignment
			identitiesToBeNotified = removeAssignedTask(course, deletedTaskFile);
			if (identitiesToBeNotified.size() > 0) {
				// prepare mailTemplate if they are any identities to be notified
				removeAsListenerAndDispose(mailCtr);
				final RepositoryEntry repositoryEntry = RepositoryManager.getInstance().lookupRepositoryEntry(course, true);
				final String courseURL = Settings.getServerContextPathURI() + "/url/RepositoryEntry/" + repositoryEntry.getKey();
				final MailTemplate mailTemplate = this.createTaskDeletedMailTemplate(urequest, course.getCourseTitle(), courseURL, deletedTaskFile);
				mailCtr = new MailNotificationEditController(getWindowControl(), urequest, mailTemplate, true);
				listenTo(mailCtr);
				cmc = new CloseableModalController(getWindowControl(), translate("close"), mailCtr.getInitialComponent());
				listenTo(cmc);
				cmc.activate();
			}
		} else if (source == mailCtr) {
			if (event == Event.DONE_EVENT) {
				cmc.deactivate();
				if (identitiesToBeNotified != null && identitiesToBeNotified.size() > 0) {
					// sent email to all identities that used to have the deleted task assigned
					sendNotificationEmail(urequest, mailCtr.getMailTemplate(), identitiesToBeNotified);
				}
			} else if (event == Event.CANCELLED_EVENT) {
				cmc.deactivate();
			}
		} else {
			log.warn("Can not handle event in TACourseNodeEditController source=" + source + " " + event.toString());
		}
	}

	/**
	 * Strips the html tags from the input string.
	 * 
	 * @param fileListHtml
	 * @return
	 */
	private String getFileListAsComaSeparated(final String fileListHtml) {
		// strip html
		String filesString = "";
		final String[] tokens = fileListHtml.split("<[^<>]+>");
		for (final String token : tokens) {
			if (!token.equals("")) {
				if (filesString.length() > 3) {
					filesString += ", ";
				}
				filesString += token;
			}
		}
		return filesString;
	}

	/**
	 * Create MailTemplate for task deleted action.
	 * 
	 * @param ureq
	 * @param courseName
	 * @param courseLink
	 * @param fileName
	 * @return
	 */
	private MailTemplate createTaskDeletedMailTemplate(final UserRequest ureq, final String courseName, final String courseLink, final String fileName) {
		final String subjectTemplate = courseName + ": " + translate("task.deleted.subject");
		final String bodyTemplate = getTaskDeletedMailBody(ureq, fileName, courseName, courseLink);
		final MailTemplate mailTempl = new MailTemplate(subjectTemplate, bodyTemplate, null) {

			@Override
			public void putVariablesInMailContext(final VelocityContext context, final Identity recipient) {
				// nothing to do
			}
		};
		return mailTempl;
	}

	private String getTaskDeletedMailBody(final UserRequest ureq, final String fileName, final String courseName, final String courseLink) {
		// grab standard text
		final String confirmation = translate("task.deleted.body");

		final Context c = new VelocityContext();
		final Identity identity = ureq.getIdentity();
		c.put("login", identity.getName());
		c.put("first", identity.getUser().getProperty(UserConstants.FIRSTNAME, getLocale()));
		c.put("last", identity.getUser().getProperty(UserConstants.LASTNAME, getLocale()));
		c.put("email", identity.getUser().getProperty(UserConstants.EMAIL, getLocale()));
		c.put("filename", fileName);
		c.put("coursename", courseName);
		c.put("courselink", courseLink);

		return VelocityHelper.getInstance().evaluateVTL(confirmation, c);
	}

	private void sendNotificationEmail(final UserRequest ureq, final MailTemplate mailTemplate, final List<Identity> recipients) {
		// send the notification mail
		if (mailTemplate != null) {
			final MailerWithTemplate mailer = MailerWithTemplate.getInstance();
			final Identity sender = ureq.getIdentity();
			List<Identity> ccIdentities = new ArrayList<Identity>();
			if (mailTemplate.getCpfrom()) {
				ccIdentities.add(sender);
			} else {
				ccIdentities = null;
			}
			final MailerResult mailerResult = mailer.sendMailAsSeparateMails(recipients, ccIdentities, null, mailTemplate, sender);
			MailHelper.printErrorsAndWarnings(mailerResult, getWindowControl(), ureq.getLocale());
		}
	}

	/**
	 * Cancel the task assignment for this task and all Identities.
	 * 
	 * @param course
	 * @param task
	 * @return Returns the Identities list that have had this task assigned.
	 */
	private List<Identity> removeAssignedTask(final ICourse course, final String task) {
		// identities to be notified
		final List<Identity> identityList = new ArrayList<Identity>();
		final CoursePropertyManager cpm = course.getCourseEnvironment().getCoursePropertyManager();
		final List properties = cpm.listCourseNodeProperties(node, null, null, TaskController.PROP_ASSIGNED);
		if (properties != null && properties.size() > 0) {
			for (final Object propetyObj : properties) {
				final Property propety = (Property) propetyObj;
				identityList.add(propety.getIdentity());
				cpm.deleteProperty(propety);
			}
		}
		return identityList;
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	@Override
	public void addTabs(final TabbedPane theTabbedPane) {
		this.myTabbedPane = theTabbedPane;
		myTabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), accessabilityVC);
		myTabbedPane.addTab(translate(PANE_TAB_CONF_MODULES), editModules);
		taskTabPosition = myTabbedPane.addTab(translate(PANE_TAB_CONF_TASK), editTask);
		dropboxTabPosition = myTabbedPane.addTab(translate(PANE_TAB_CONF_DROPBOX), editDropbox);
		scoringTabPosition = myTabbedPane.addTab(translate(PANE_TAB_CONF_SCORING), editScoring);
		solutionTabPosition = myTabbedPane.addTab(translate(PANE_TAB_SOLUTION), solutionVC);

		Boolean bool = (Boolean) config.get(TACourseNode.CONF_TASK_ENABLED);
		myTabbedPane.setEnabled(taskTabPosition, (bool != null) ? bool.booleanValue() : true);
		bool = (Boolean) config.get(TACourseNode.CONF_DROPBOX_ENABLED);
		myTabbedPane.setEnabled(dropboxTabPosition, (bool != null) ? bool.booleanValue() : true);
		bool = (Boolean) config.get(TACourseNode.CONF_SCORING_ENABLED);
		myTabbedPane.setEnabled(scoringTabPosition, (bool != null) ? bool.booleanValue() : true);

		bool = (Boolean) config.get(TACourseNode.CONF_SOLUTION_ENABLED);
		myTabbedPane.setEnabled(solutionTabPosition, (bool != null) ? bool.booleanValue() : true);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// child controllers registered with listenTo() get disposed in BasicController
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

class TaskFolderCallback implements VFSSecurityCallback {

	private final boolean folderLocked;
	private Quota folderQuota = null;

	/**
	 * @param folderLocked
	 */
	public TaskFolderCallback(final String relPath, final boolean folderLocked) {
		this.folderLocked = folderLocked;
		initTaskFolderQuota(relPath);
	}

	private void initTaskFolderQuota(final String relPath) {
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
	 * @see org.olat.core.util.vfs.callbacks.VFSSecurityCallback#canDeleteRevisionsPermanently()
	 */
	@Override
	public boolean canDeleteRevisionsPermanently() {
		return false;
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
}