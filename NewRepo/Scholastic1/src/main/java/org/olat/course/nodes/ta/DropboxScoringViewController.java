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

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.apache.velocity.VelocityContext;
import org.olat.admin.quota.QuotaConstants;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.FolderEvent;
import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.servlets.util.URLEncoder;
import org.olat.core.dispatcher.jumpin.JumpInManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.htmlsite.HtmlStaticPageComponent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.iframe.IFrameDisplayController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.media.FileMediaResource;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.MailerWithTemplate;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.TACourseNode;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.properties.Property;

/**
 * Initial Date: 02.09.2004
 * 
 * @author Mike Stock
 */

public class DropboxScoringViewController extends BasicController {

	private final OLog log = Tracing.createLoggerFor(this.getClass());

	protected CourseNode node;
	protected UserCourseEnvironment userCourseEnv;
	private VelocityContainer myContent;
	private Link taskLaunchButton;
	private Link cancelTaskButton;
	private FolderRunController dropboxFolderRunController, returnboxFolderRunController;
	private String assignedTask;
	private StatusForm statusForm;
	private CloseableModalController cmc;
	private IFrameDisplayController iFrameCtr;
	private DialogBoxController dialogBoxController;

	/**
	 * Scoring view of the dropbox.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param node
	 * @param userCourseEnv
	 */
	public DropboxScoringViewController(final UserRequest ureq, final WindowControl wControl, final CourseNode node, final UserCourseEnvironment userCourseEnv) {
		this(ureq, wControl, node, userCourseEnv, true);
	}

	/**
	 * @param ureq
	 * @param wControl
	 * @param node
	 * @param userCourseEnv
	 * @param doInit When true call init-method in constructor.
	 */
	protected DropboxScoringViewController(final UserRequest ureq, final WindowControl wControl, final CourseNode node, final UserCourseEnvironment userCourseEnv,
			final boolean doInit) {
		super(ureq, wControl);

		this.node = node;
		this.userCourseEnv = userCourseEnv;
		if (doInit) {
			init(ureq);
		}
	}

	protected void init(final UserRequest ureq) {
		myContent = createVelocityContainer("dropboxscoring");
		taskLaunchButton = LinkFactory.createButton("task.launch", myContent, this);
		cancelTaskButton = LinkFactory.createButton("task.cancel", myContent, this);
		putInitialPanel(myContent);

		final ModuleConfiguration modConfig = node.getModuleConfiguration();
		final Boolean bValue = (Boolean) modConfig.get(TACourseNode.CONF_TASK_ENABLED);
		myContent.contextPut("hasTask", (bValue != null) ? bValue : new Boolean(false));
		final Boolean hasDropbox = (Boolean) modConfig.get(TACourseNode.CONF_DROPBOX_ENABLED); // configured value
		final Boolean hasDropboxValue = (hasDropbox != null) ? hasDropbox : new Boolean(true);
		myContent.contextPut("hasDropbox", hasDropboxValue);

		final Boolean hasReturnbox = (Boolean) modConfig.get(TACourseNode.CONF_RETURNBOX_ENABLED);
		myContent.contextPut("hasReturnbox", (hasReturnbox != null) ? hasReturnbox : hasDropboxValue);

		// dropbox display
		final String assesseeName = userCourseEnv.getIdentityEnvironment().getIdentity().getName();
		final OlatRootFolderImpl rootDropbox = new OlatRootFolderImpl(getDropboxFilePath(assesseeName), null);
		rootDropbox.setLocalSecurityCallback(getDropboxVfsSecurityCallback());
		final OlatNamedContainerImpl namedDropbox = new OlatNamedContainerImpl(getDropboxRootFolderName(assesseeName), rootDropbox);
		namedDropbox.setLocalSecurityCallback(getDropboxVfsSecurityCallback());

		dropboxFolderRunController = new FolderRunController(namedDropbox, false, ureq, getWindowControl());
		listenTo(dropboxFolderRunController);

		myContent.put("dropbox", dropboxFolderRunController.getInitialComponent());

		// returnbox display
		final OlatRootFolderImpl rootReturnbox = new OlatRootFolderImpl(getReturnboxFilePath(assesseeName), null);
		rootReturnbox.setLocalSecurityCallback(getReturnboxVfsSecurityCallback(rootReturnbox.getRelPath(), userCourseEnv, node)); //
		final OlatNamedContainerImpl namedReturnbox = new OlatNamedContainerImpl(getReturnboxRootFolderName(assesseeName), rootReturnbox);
		namedReturnbox.setLocalSecurityCallback(getReturnboxVfsSecurityCallback(rootReturnbox.getRelPath(), userCourseEnv, node));

		returnboxFolderRunController = new FolderRunController(namedReturnbox, false, ureq, getWindowControl());
		listenTo(returnboxFolderRunController);

		myContent.put("returnbox", returnboxFolderRunController.getInitialComponent());

		// insert Status Pull-Down Menu depending on user role == author
		final boolean isAuthor = ureq.getUserSession().getRoles().isAuthor();
		final boolean isTutor = userCourseEnv.getCourseEnvironment().getCourseGroupManager().isIdentityCourseCoach(ureq.getIdentity());
		if (((AssessableCourseNode) node).hasStatusConfigured() && (isAuthor || isTutor)) {
			myContent.contextPut("hasStatusPullDown", Boolean.TRUE);
			statusForm = new StatusForm(ureq, getWindowControl());
			listenTo(statusForm);

			// get identity not from request (this would be an author)
			StatusManager.getInstance().loadStatusFormData(statusForm, node, userCourseEnv);
			myContent.put("statusForm", statusForm.getInitialComponent());
		}

		assignedTask = TaskController.getAssignedTask(userCourseEnv.getIdentityEnvironment().getIdentity(), userCourseEnv.getCourseEnvironment(), node);
		if (assignedTask != null) {
			myContent.contextPut("assignedtask", assignedTask);
			if (!(assignedTask.toLowerCase().endsWith(".html") || assignedTask.toLowerCase().endsWith(".htm") || assignedTask.toLowerCase().endsWith(".txt"))) {
				taskLaunchButton.setTarget("_blank");
			}
		}
	}

	protected VFSSecurityCallback getDropboxVfsSecurityCallback() {
		return new ReadOnlyAndDeleteCallback();
	}

	protected VFSSecurityCallback getReturnboxVfsSecurityCallback(final String returnboxRelPath, final UserCourseEnvironment userCourseEnv2, final CourseNode node2) {
		return new ReturnboxFullAccessCallback(returnboxRelPath, userCourseEnv2, node2);
	}

	protected String getDropboxRootFolderName(final String assesseeName) {
		return assesseeName;
	}

	protected String getReturnboxRootFolderName(final String assesseeName) {
		return assesseeName;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == taskLaunchButton) {
			final File fTaskfolder = new File(FolderConfig.getCanonicalRoot() + TACourseNode.getTaskFolderPathRelToFolderRoot(userCourseEnv.getCourseEnvironment(), node));
			if (assignedTask.toLowerCase().endsWith(".html") || assignedTask.toLowerCase().endsWith(".htm") || assignedTask.toLowerCase().endsWith(".txt")) {

				if (getWindowControl().getWindowBackOffice().getWindowManager().isForScreenReader()) {
					// render content for screenreaders always inline
					final HtmlStaticPageComponent cpc = new HtmlStaticPageComponent("cpc", new LocalFolderImpl(fTaskfolder));
					cpc.setCurrentURI(assignedTask);

					removeAsListenerAndDispose(cmc);
					cmc = new CloseableModalController(getWindowControl(), translate("close"), cpc);
					listenTo(cmc);

					cmc.activate();
				} else {

					// render content for other users always in iframe
					removeAsListenerAndDispose(iFrameCtr);
					iFrameCtr = new IFrameDisplayController(ureq, getWindowControl(), fTaskfolder);
					listenTo(iFrameCtr);

					iFrameCtr.setCurrentURI(assignedTask);

					removeAsListenerAndDispose(cmc);
					cmc = new CloseableModalController(getWindowControl(), translate("close"), iFrameCtr.getInitialComponent());
					listenTo(cmc);

					cmc.activate();
				}

			} else {
				ureq.getDispatchResult().setResultingMediaResource(new FileMediaResource(new File(fTaskfolder, assignedTask)));
			}
		} else if (source == cancelTaskButton) {
			// confirm cancel task assignment
			dialogBoxController = this.activateYesNoDialog(ureq, "", translate("task.cancel.reassign"), dialogBoxController);
		}

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == dropboxFolderRunController) {
			if (event instanceof FolderEvent) {
				final FolderEvent folderEvent = (FolderEvent) event;
				if (folderEvent.getCommand().equals(FolderEvent.DELETE_EVENT)) {
					final UserNodeAuditManager am = userCourseEnv.getCourseEnvironment().getAuditManager();
					// log entry for this file
					final Identity coach = ureq.getIdentity();
					final Identity student = userCourseEnv.getIdentityEnvironment().getIdentity();
					am.appendToUserNodeLog(node, coach, student, "FILE DELETED: " + folderEvent.getFilename());
				}
			}
		} else if (source == returnboxFolderRunController) {
			if (event instanceof FolderEvent) {
				final FolderEvent folderEvent = (FolderEvent) event;
				if (folderEvent.getCommand().equals(FolderEvent.UPLOAD_EVENT) || folderEvent.getCommand().equals(FolderEvent.NEW_FILE_EVENT)) {
					final UserNodeAuditManager am = userCourseEnv.getCourseEnvironment().getAuditManager();
					// log entry for this file
					final Identity coach = ureq.getIdentity();
					final Identity student = userCourseEnv.getIdentityEnvironment().getIdentity();
					am.appendToUserNodeLog(node, coach, student, "FILE UPLOADED: " + folderEvent.getFilename());
					final String toMail = student.getUser().getProperty(UserConstants.EMAIL, ureq.getLocale());
					final Locale locale = I18nManager.getInstance().getLocaleOrDefault(student.getUser().getPreferences().getLanguage());
					final String nodeUrl = new URLEncoder().encode("[" + OresHelper.calculateTypeName(CourseNode.class) + ":" + node.getIdent() + "]");
					final String link = JumpInManager.getJumpInUri(this.getWindowControl().getBusinessControl()) + nodeUrl;
					log.debug("DEBUG : Returnbox notification email with link=" + link);
					final MailTemplate mailTempl = new MailTemplate(translate("returnbox.email.subject"), translate("returnbox.email.body", new String[] {
							userCourseEnv.getCourseEnvironment().getCourseTitle(), node.getShortTitle(), folderEvent.getFilename(), link }), null) {

						@Override
						public void putVariablesInMailContext(final VelocityContext context, final Identity recipient) {
							// nothing to do
						}
					};

					final MailerResult result = MailerWithTemplate.getInstance().sendMail(student, null, null, mailTempl, null);

					if (result.getReturnCode() > 0) {
						am.appendToUserNodeLog(node, coach, student, "MAIL SEND FAILED TO:" + toMail + "; MailReturnCode: " + result.getReturnCode());
						log.warn("Could not send email 'returnbox notification' to " + student + "with email=" + toMail);
					} else {
						log.info("Send email 'returnbox notification' to " + student + "with email=" + toMail);
					}
				}
			}
		} else if (source == statusForm) {
			if (event == Event.DONE_EVENT) {
				// get identity not from request (this would be an author)
				StatusManager.getInstance().saveStatusFormData(statusForm, node, userCourseEnv);
			}
		} else if (source == dialogBoxController) {
			if (DialogBoxUIFactory.isYesEvent(event) && assignedTask != null) {
				// cancel task assignment, and show "no task assigned to user"
				removeAssignedTask(userCourseEnv, userCourseEnv.getIdentityEnvironment().getIdentity(), assignedTask);
				// update UI
				myContent.contextPut("assignedtask", null);
			}
		}
	}

	/**
	 * Cancel the task assignment.
	 * 
	 * @param identity
	 * @param task
	 */
	private void removeAssignedTask(final UserCourseEnvironment courseEnv, final Identity identity, final String task) {
		final CoursePropertyManager cpm = courseEnv.getCourseEnvironment().getCoursePropertyManager();
		List properties = cpm.findCourseNodeProperties(node, identity, null, TaskController.PROP_ASSIGNED);
		if (properties != null && properties.size() > 0) {
			final Property propety = (Property) properties.get(0);
			cpm.deleteProperty(propety);
			assignedTask = null;
		}
		// removed sampled
		properties = cpm.findCourseNodeProperties(node, null, null, TaskController.PROP_SAMPLED);
		if (properties != null && properties.size() > 0) {
			final Property propety = (Property) properties.get(0);
			cpm.deleteProperty(propety);
		}
	}

	protected String getDropboxFilePath(final String assesseeName) {
		return DropboxController.getDropboxPathRelToFolderRoot(userCourseEnv.getCourseEnvironment(), node) + "/" + assesseeName;
	}

	protected String getReturnboxFilePath(final String assesseeName) {
		return ReturnboxController.getReturnboxPathRelToFolderRoot(userCourseEnv.getCourseEnvironment(), node) + "/" + assesseeName;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}
}

class ReadOnlyAndDeleteCallback implements VFSSecurityCallback {

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canList(org.olat.modules.bc.Path)
	 */
	@Override
	public boolean canList() {
		return true;
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
		return false;
	}

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canDelete(org.olat.modules.bc.Path)
	 */
	@Override
	public boolean canDelete() {
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
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canDeleteRevisionsPermanently()
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
		return null;
	}

	/**
	 * @see org.olat.core.util.vfs.callbacks.VFSSecurityCallback#setQuota(org.olat.admin.quota.Quota)
	 */
	@Override
	public void setQuota(final Quota quota) {}

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#getSubscriptionContext()
	 */
	@Override
	public SubscriptionContext getSubscriptionContext() {
		return null;
	}
}

class ReturnboxFullAccessCallback implements VFSSecurityCallback {

	private Quota quota;
	private final UserCourseEnvironment userCourseEnv;
	private final CourseNode courseNode;

	public ReturnboxFullAccessCallback(final String relPath, final UserCourseEnvironment userCourseEnv, final CourseNode courseNode) {
		this.userCourseEnv = userCourseEnv;
		this.courseNode = courseNode;
		final QuotaManager qm = QuotaManager.getInstance();
		quota = qm.getCustomQuota(relPath);
		if (quota == null) { // if no custom quota set, use the default quotas...
			final Quota defQuota = qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_POWER);
			quota = QuotaManager.getInstance().createQuota(relPath, defQuota.getQuotaKB(), defQuota.getUlLimitKB());
		}
	}

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canList(org.olat.modules.bc.Path)
	 */
	@Override
	public boolean canList() {
		return true;
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
		return true;
	}

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canDelete(org.olat.modules.bc.Path)
	 */
	@Override
	public boolean canDelete() {
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
		return quota;
	}

	/**
	 * @see org.olat.core.util.vfs.callbacks.VFSSecurityCallback#setQuota(org.olat.admin.quota.Quota)
	 */
	@Override
	public void setQuota(final Quota quota) {
		this.quota = quota;
	}

	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#getSubscriptionContext()
	 */
	@Override
	public SubscriptionContext getSubscriptionContext() {
		return null;
	}
}
