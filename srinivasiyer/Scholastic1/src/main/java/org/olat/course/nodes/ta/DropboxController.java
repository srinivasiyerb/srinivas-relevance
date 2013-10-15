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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.olat.admin.quota.QuotaConstants;
import org.olat.commons.file.filechooser.FileChooserController;
import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.commons.modules.bc.meta.tagged.MetaTagged;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.render.velocity.VelocityHelper;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Formatter;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.MailerWithTemplate;
import org.olat.core.util.notifications.ContextualSubscriptionController;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.callbacks.FullAccessWithQuotaCallback;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.TACourseNode;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;

/**
 * Initial Date: 02.09.2004
 * 
 * @author Mike Stock
 */

public class DropboxController extends BasicController {

	public static String DROPBOX_DIR_NAME = "dropboxes";
	// config

	protected ModuleConfiguration config;
	protected CourseNode node;
	protected UserCourseEnvironment userCourseEnv;
	private VelocityContainer myContent;
	private FileChooserController fileChooserController;
	private SubscriptionContext subsContext;
	private ContextualSubscriptionController contextualSubscriptionCtr;
	private Link ulButton;
	private CloseableModalController cmc;

	// Constructor for ProjectBrokerDropboxController
	protected DropboxController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		this.setBasePackage(DropboxController.class);
	}

	/**
	 * Implements a dropbox.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param config
	 * @param node
	 * @param userCourseEnv
	 * @param previewMode
	 */
	public DropboxController(final UserRequest ureq, final WindowControl wControl, final ModuleConfiguration config, final CourseNode node,
			final UserCourseEnvironment userCourseEnv, final boolean previewMode) {
		super(ureq, wControl);
		this.setBasePackage(DropboxController.class);
		this.config = config;
		this.node = node;
		this.userCourseEnv = userCourseEnv;
		final boolean isCourseAdmin = userCourseEnv.getCourseEnvironment().getCourseGroupManager().isIdentityCourseAdministrator(ureq.getIdentity());
		final boolean isCourseCoach = userCourseEnv.getCourseEnvironment().getCourseGroupManager().isIdentityCourseCoach(ureq.getIdentity());
		final boolean hasNotification = (isCourseAdmin || isCourseCoach);
		init(ureq, wControl, previewMode, hasNotification);
	}

	protected void init(final UserRequest ureq, final WindowControl wControl, final boolean previewMode, final boolean hasNotification) {
		myContent = createVelocityContainer("dropbox");

		ulButton = LinkFactory.createButton("dropbox.upload", myContent, this);

		if (!previewMode) {
			final VFSContainer fDropbox = getDropBox(ureq.getIdentity());
			final int numFiles = fDropbox.getItems().size();
			if (numFiles > 0) {
				myContent.contextPut("numfiles", new String[] { Integer.toString(numFiles) });
			}

		} else {
			myContent.contextPut("numfiles", "0");
		}
		myContent.contextPut("previewMode", previewMode ? Boolean.TRUE : Boolean.FALSE);

		// notification
		if (hasNotification && !previewMode) {
			// offer subscription, but not to guests
			subsContext = DropboxFileUploadNotificationHandler.getSubscriptionContext(userCourseEnv, node);
			if (subsContext != null) {
				contextualSubscriptionCtr = AbstractTaskNotificationHandler.createContextualSubscriptionController(ureq, wControl,
						getDropboxPathRelToFolderRoot(userCourseEnv.getCourseEnvironment(), node), subsContext, DropboxController.class);
				myContent.put("subscription", contextualSubscriptionCtr.getInitialComponent());
				myContent.contextPut("hasNotification", Boolean.TRUE);
			}
		} else {
			myContent.contextPut("hasNotification", Boolean.FALSE);
		}

		putInitialPanel(myContent);
	}

	/**
	 * Dropbox path relative to folder root.
	 * 
	 * @param courseEnv
	 * @param cNode
	 * @return Dropbox path relative to folder root.
	 */
	public static String getDropboxPathRelToFolderRoot(final CourseEnvironment courseEnv, final CourseNode cNode) {
		return courseEnv.getCourseBaseContainer().getRelPath() + File.separator + DROPBOX_DIR_NAME + File.separator + cNode.getIdent();
	}

	/**
	 * Get the dropbox of an identity.
	 * 
	 * @param identity
	 * @return Dropbox of an identity
	 */
	protected VFSContainer getDropBox(final Identity identity) {
		final OlatRootFolderImpl dropBox = new OlatRootFolderImpl(getRelativeDropBoxFilePath(identity), null);
		if (!dropBox.getBasefile().exists()) {
			dropBox.getBasefile().mkdirs();
		}
		return dropBox;
	}

	protected String getRelativeDropBoxFilePath(final Identity identity) {
		return getDropboxPathRelToFolderRoot(userCourseEnv.getCourseEnvironment(), node) + File.separator + identity.getName();
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == ulButton) {

			removeAsListenerAndDispose(fileChooserController);
			fileChooserController = new FileChooserController(ureq, getWindowControl(), getUploadLimit(ureq), true);
			listenTo(fileChooserController);

			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), fileChooserController.getInitialComponent(), true, "Upload");
			listenTo(cmc);

			cmc.activate();
		}
	}

	/**
	 * Get upload limit for dropbox of a certain user. The upload can be limited by available-folder space, max folder size or configurated upload-limit.
	 * 
	 * @param ureq
	 * @return max upload limit in KB
	 */
	private int getUploadLimit(final UserRequest ureq) {
		final String dropboxPath = getRelativeDropBoxFilePath(ureq.getIdentity());
		Quota dropboxQuota = QuotaManager.getInstance().getCustomQuota(dropboxPath);
		if (dropboxQuota == null) {
			dropboxQuota = QuotaManager.getInstance().getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_NODES);
		}
		final OlatRootFolderImpl rootFolder = new OlatRootFolderImpl(getRelativeDropBoxFilePath(ureq.getIdentity()), null);
		final VFSContainer dropboxContainer = new OlatNamedContainerImpl(ureq.getIdentity().getName(), rootFolder);
		final FullAccessWithQuotaCallback secCallback = new FullAccessWithQuotaCallback(dropboxQuota);
		rootFolder.setLocalSecurityCallback(secCallback);
		final int ulLimit = QuotaManager.getInstance().getUploadLimitKB(dropboxQuota.getQuotaKB(), dropboxQuota.getUlLimitKB(), dropboxContainer);
		return ulLimit;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == fileChooserController) {
			cmc.deactivate();
			if (event.equals(Event.DONE_EVENT)) {
				VFSLeaf fIn;
				boolean success = false;
				final VFSContainer fDropbox = getDropBox(ureq.getIdentity());
				if (fileChooserController.isFileFromFolder()) {
					fIn = fileChooserController.getFileSelection();
				} else {
					fIn = fileChooserController.getUploadedVFSFile();
				}

				VFSLeaf fOut;
				if (fDropbox.resolve(fIn.getName()) != null) {
					// FIXME ms: check if dropbox quota is exceeded -> with customers abkl�ren
					fOut = fDropbox.createChildLeaf(getNewUniqueName(fIn.getName()));
				} else {
					fOut = fDropbox.createChildLeaf(fIn.getName());
				}

				final InputStream in = fIn.getInputStream();
				final OutputStream out = new BufferedOutputStream(fOut.getOutputStream(false));
				success = FileUtils.copy(in, out);
				FileUtils.closeSafely(in);
				FileUtils.closeSafely(out);

				if (fOut instanceof MetaTagged) {
					final MetaInfo info = ((MetaTagged) fOut).getMetaInfo();
					if (info != null) {
						info.setAuthor(ureq.getIdentity().getName());
						info.write();
					}
				}

				if (success) {
					final int numFiles = fDropbox.getItems().size();
					myContent.contextPut("numfiles", new String[] { Integer.toString(numFiles) });
					// assemble confirmation
					final String confirmation = getConfirmation(ureq, fIn.getName());
					// send email if necessary
					Boolean sendEmail = (Boolean) config.get(TACourseNode.CONF_DROPBOX_ENABLEMAIL);
					if (sendEmail == null) {
						sendEmail = Boolean.FALSE;
					}
					boolean sendMailError = false;
					if (sendEmail.booleanValue()) {
						final MailTemplate mailTempl = new MailTemplate(translate("conf.mail.subject"), confirmation, null) {

							@Override
							public void putVariablesInMailContext(final VelocityContext context, final Identity recipient) {
								// nothing to do
							}
						};

						final MailerResult result = MailerWithTemplate.getInstance().sendMail(ureq.getIdentity(), null, null, mailTempl, null);
						if (result.getFailedIdentites().size() > 0) {
							List<Identity> disabledIdentities = new ArrayList<Identity>();
							disabledIdentities = result.getFailedIdentites();
							// show error that message can not be sent
							final ArrayList<String> myButtons = new ArrayList<String>();
							myButtons.add(translate("back"));
							final String title = MailHelper.getTitleForFailedUsersError(ureq.getLocale());
							String message = MailHelper.getMessageForFailedUsersError(ureq.getLocale(), disabledIdentities);
							// add dropbox specific error message
							message += "\n<br />" + translate("conf.mail.error");
							// FIXME:FG:6.2: fix problem in info message, not here
							message += "\n<br />\n<br />" + confirmation.replace("\n", "&#10;").replace("\r", "&#10;").replace("\u2028", "&#10;");
							DialogBoxController noUsersErrorCtr = null;
							noUsersErrorCtr = activateGenericDialog(ureq, title, message, myButtons, noUsersErrorCtr);
							sendMailError = true;
						} else if (result.getReturnCode() > 0) {
							// show error that message can not be sent
							final ArrayList<String> myButtons = new ArrayList<String>();
							myButtons.add(translate("back"));
							DialogBoxController noUsersErrorCtr = null;
							String message = translate("conf.mail.error");
							// FIXME:FG:6.2: fix problem in info message, not here
							message += "\n<br />\n<br />" + confirmation.replace("\n", "&#10;").replace("\r", "&#10;").replace("\u2028", "&#10;");
							noUsersErrorCtr = activateGenericDialog(ureq, translate("error.header"), message, myButtons, noUsersErrorCtr);
							sendMailError = true;
						}
					}

					subsContext = DropboxFileUploadNotificationHandler.getSubscriptionContext(userCourseEnv, node);
					// inform subscription manager about new element
					if (subsContext != null) {
						NotificationsManager.getInstance().markPublisherNews(subsContext, ureq.getIdentity());
					}
					// configuration is already translated, don't use showInfo(i18nKey)!
					// FIXME:FG:6.2: fix problem in info message, not here
					if (!sendMailError) {
						getWindowControl().setInfo(confirmation.replace("\n", "&#10;").replace("\r", "&#10;").replace("\u2028", "&#10;"));
					}
					return;
				} else {
					showInfo("dropbox.upload.failed");
				}
			}
		}
	}

	private String getNewUniqueName(final String name) {
		String body = null;
		String ext = null;
		final int dot = name.lastIndexOf(".");
		if (dot != -1) {
			body = name.substring(0, dot);
			ext = name.substring(dot);
		} else {
			body = name;
			ext = "";
		}
		final String tStamp = new SimpleDateFormat("yyMMdd-HHmmss").format(new Date());
		return body + "." + tStamp + ext;
	}

	private String getConfirmation(final UserRequest ureq, final String filename) {
		// grab standard text
		final String confirmation = translate("conf.stdtext");
		config.set(TACourseNode.CONF_DROPBOX_CONFIRMATION, confirmation);

		final Context c = new VelocityContext();
		final Identity identity = ureq.getIdentity();
		c.put("login", identity.getName());
		c.put("first", identity.getUser().getProperty(UserConstants.FIRSTNAME, getLocale()));
		c.put("last", identity.getUser().getProperty(UserConstants.LASTNAME, getLocale()));
		c.put("email", identity.getUser().getProperty(UserConstants.EMAIL, getLocale()));
		c.put("filename", filename);
		final Date now = new Date();
		final Formatter f = Formatter.getInstance(ureq.getLocale());
		c.put("date", f.formatDate(now));
		c.put("time", f.formatTime(now));

		// update attempts counter for this user: one file - one attempts
		final AssessableCourseNode acn = (AssessableCourseNode) node;
		acn.incrementUserAttempts(userCourseEnv);

		// log entry for this file
		final UserNodeAuditManager am = userCourseEnv.getCourseEnvironment().getAuditManager();
		am.appendToUserNodeLog(node, identity, identity, "FILE UPLOADED: " + filename);

		return VelocityHelper.getInstance().evaluateVTL(confirmation, c);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// DialogBoxController gets disposed by BasicController
		if (fileChooserController != null) {
			fileChooserController.dispose();
			fileChooserController = null;
		}
		if (contextualSubscriptionCtr != null) {
			contextualSubscriptionCtr.dispose();
			contextualSubscriptionCtr = null;
		}
	}
}
