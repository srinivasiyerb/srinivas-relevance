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

package org.olat.modules.dialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.olat.core.commons.modules.bc.FileUploadController;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.FolderEvent;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.control.generic.popup.PopupBrowserWindow;
import org.olat.core.gui.control.generic.title.TitleInfo;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.activity.CourseLoggingAction;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.StringHelper;
import org.olat.core.util.notifications.ContextualSubscriptionController;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.PublisherData;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSMediaResource;
import org.olat.core.util.vfs.filters.VFSLeafFilter;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.DialogCourseNode;
import org.olat.course.nodes.dialog.DialogConfigForm;
import org.olat.course.nodes.dialog.DialogNodeForumCallback;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.ForumUIFactory;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description:<br>
 * TODO: guido Class Description for DialogController
 * <P>
 * Initial Date: 03.11.2005 <br>
 * 
 * @author guido
 */
public class DialogElementsController extends BasicController {

	protected static final String ACTION_START_FORUM = "startforum";
	protected static final String ACTION_SHOW_FILE = "showfile";
	protected static final String ACTION_DELETE_ELEMENT = "delete";
	private static final int TABLE_RESULTS_PER_PAGE = 5;

	private final DialogElementsPropertyManager dialogElmsMgr;
	private final VelocityContainer content;
	private TableController tableCtr;
	private final CourseNode courseNode;
	private FileUploadController fileUplCtr;
	private final Panel dialogPanel;
	private final ForumManager forumMgr;
	private DialogElement recentDialogElement, selectedElement;
	private DialogElementsTableModel tableModel;
	private DialogBoxController confirmDeletionCtr;
	private ContextualSubscriptionController csCtr;
	private DialogNodeForumCallback forumCallback;
	private SubscriptionContext subsContext;
	private final CoursePropertyManager coursePropMgr;
	private final boolean isOlatAdmin;
	private final boolean isGuestOnly;
	private final NodeEvaluation nodeEvaluation;
	private final UserCourseEnvironment userCourseEnv;
	private final TableGuiConfiguration tableConf;
	private final Link uploadButton;
	private Controller forumCtr;

	public DialogElementsController(final UserRequest ureq, final WindowControl wControl, final CourseNode courseNode, final UserCourseEnvironment userCourseEnv,
			final NodeEvaluation nodeEvaluation) {
		super(ureq, wControl);
		this.nodeEvaluation = nodeEvaluation;
		this.userCourseEnv = userCourseEnv;
		this.coursePropMgr = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		this.courseNode = courseNode;
		forumMgr = ForumManager.getInstance();
		dialogElmsMgr = DialogElementsPropertyManager.getInstance();

		content = createVelocityContainer("dialog");
		uploadButton = LinkFactory.createButton("dialog.upload.file", content, this);

		isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		isGuestOnly = ureq.getUserSession().getRoles().isGuestOnly();

		forumCallback = new DialogNodeForumCallback(nodeEvaluation, isOlatAdmin, isGuestOnly, subsContext);
		content.contextPut("security", forumCallback);

		if (isGuestOnly) {
			// guests cannot subscribe (OLAT-2019)
			subsContext = null;
		} else {
			subsContext = CourseModule.createSubscriptionContext(userCourseEnv.getCourseEnvironment(), courseNode);
		}

		// if sc is null, then no subscription is desired
		if (subsContext != null) {
			// FIXME:fj: implement subscription callback for group forums
			final String businessPath = wControl.getBusinessControl().getAsString();
			final PublisherData pdata = new PublisherData(OresHelper.calculateTypeName(DialogElement.class), "", businessPath);
			csCtr = new ContextualSubscriptionController(ureq, getWindowControl(), subsContext, pdata);
			listenTo(csCtr);
			content.put("subscription", csCtr.getInitialComponent());
		}
		// configure and display table
		tableConf = new TableGuiConfiguration();
		tableConf.setResultsPerPage(TABLE_RESULTS_PER_PAGE);
		tableConf.setPreferencesOffered(true, "FileDialogElementsTable");
		tableConf.setDownloadOffered(true);
		dialogPanel = putInitialPanel(content);
		showOverviewTable(ureq, forumCallback);
	}

	private void showOverviewTable(final UserRequest ureq, final DialogNodeForumCallback callback) {
		removeAsListenerAndDispose(tableCtr);
		tableCtr = new TableController(tableConf, ureq, getWindowControl(), getTranslator());
		final DialogPropertyElements elements = dialogElmsMgr.findDialogElements(coursePropMgr, courseNode);
		List list = new ArrayList();
		tableModel = new DialogElementsTableModel(getTranslator(), callback, courseNode.getModuleConfiguration());
		if (elements != null) {
			list = elements.getDialogPropertyElements();
		}
		for (final Iterator iter = list.iterator(); iter.hasNext();) {
			final DialogElement element = (DialogElement) iter.next();
			final Integer msgCount = forumMgr.countMessagesByForumID(element.getForumKey());
			element.setMessagesCount(msgCount);
			element.setNewMessages(new Integer(msgCount.intValue() - forumMgr.countReadMessagesByUserAndForum(ureq.getIdentity(), element.getForumKey())));
		}
		tableModel.setEntries(list);
		tableModel.addColumnDescriptors(tableCtr);
		tableCtr.setTableDataModel(tableModel);
		tableCtr.modelChanged();
		tableCtr.setSortColumn(3, false);
		listenTo(tableCtr);
		content.put("dialogElementsTable", tableCtr.getInitialComponent());
		dialogPanel.setContent(content);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		DialogElement entry = null;
		// process table events
		if (source == tableCtr) {
			final TableEvent te = (TableEvent) event;
			final String command = te.getActionId();
			final int row = te.getRowId();
			entry = tableModel.getEntryAt(row);
			if (command.equals(ACTION_START_FORUM)) {
				selectedElement = dialogElmsMgr.findDialogElement(coursePropMgr, courseNode, entry.getForumKey());
				if (selectedElement == null) {
					showInfo("element.already.deleted");
					return;
				}
				Forum forum = null;
				forum = forumMgr.loadForum(entry.getForumKey());
				content.contextPut("hasSelectedElement", Boolean.TRUE);
				content.contextPut("selectedElement", selectedElement);

				// display forum either inline or as popup
				final String integration = (String) courseNode.getModuleConfiguration().get(DialogConfigForm.DIALOG_CONFIG_INTEGRATION);

				subsContext = CourseModule.createSubscriptionContext(userCourseEnv.getCourseEnvironment(), courseNode, forum.getKey().toString());
				forumCallback = new DialogNodeForumCallback(nodeEvaluation, isOlatAdmin, isGuestOnly, subsContext);
				content.contextPut("security", forumCallback);

				if (integration.equals(DialogConfigForm.CONFIG_INTEGRATION_VALUE_INLINE)) {
					removeAsListenerAndDispose(forumCtr);
					forumCtr = ForumUIFactory.getStandardForumController(ureq, getWindowControl(), forum, forumCallback);
					listenTo(forumCtr);
					content.contextPut("hasInlineForum", Boolean.TRUE);
					content.put("forum", forumCtr.getInitialComponent());
				} else {
					content.contextPut("hasInlineForum", Boolean.FALSE);
					final TitleInfo titleInfo = new TitleInfo(translate("dialog.selected.element"), selectedElement.getFilename());
					final PopupBrowserWindow pbw = ForumUIFactory.getPopupableForumController(ureq, getWindowControl(), forum, forumCallback, titleInfo);
					pbw.open(ureq);
				}

			} else if (command.equals(ACTION_SHOW_FILE)) {
				doFileDelivery(ureq, entry.getForumKey());
			} else if (command.equals(ACTION_DELETE_ELEMENT)) {
				selectedElement = entry;
				confirmDeletionCtr = activateYesNoDialog(ureq, null, translate("element.delete", entry.getFilename()), confirmDeletionCtr);
				return;
			}
			// process file upload events
		} else if (source == fileUplCtr) {
			// event.
			if (event == Event.DONE_EVENT || event == Event.CANCELLED_EVENT) {
				// reset recent element
				recentDialogElement = null;
				showOverviewTable(ureq, forumCallback);
			} else if (event.getCommand().equals(FolderEvent.UPLOAD_EVENT)) {
				String filename = null;
				try {
					// get size of file
					final OlatRootFolderImpl forumContainer = getForumContainer(recentDialogElement.getForumKey());
					final VFSLeaf vl = (VFSLeaf) forumContainer.getItems().get(0);
					final String fileSize = StringHelper.formatMemory(vl.getSize());

					// new dialog element
					filename = ((FolderEvent) event).getFilename();
					final DialogElement element = new DialogElement();
					element.setAuthor(recentDialogElement.getAuthor());
					element.setDate(new Date());
					element.setFilename(filename);
					element.setForumKey(recentDialogElement.getForumKey());
					element.setFileSize(fileSize);

					// do logging
					// ThreadLocalUserActivityLogger.log(CourseLoggingAction.DIALOG_ELEMENT_FILE_UPLOADED, getClass(), LoggingResourceable.wrapUploadFile(filename));

					// inform subscription manager about new element
					if (subsContext != null) {
						NotificationsManager.getInstance().markPublisherNews(subsContext, ureq.getIdentity());
					}
					// everything when well so save the property
					dialogElmsMgr.addDialogElement(coursePropMgr, courseNode, element);
				} catch (final Exception e) {
					//
					throw new OLATRuntimeException(DialogElementsController.class, "Error while adding new 'file discussion' element with filename: " + filename, e);
				}
			}
		} else if (source == confirmDeletionCtr) {
			if (DialogBoxUIFactory.isYesEvent(event)) {
				final DialogCourseNode node = (DialogCourseNode) courseNode;
				// archive data to personal folder
				node.doArchiveElement(selectedElement, CourseFactory.getOrCreateDataExportDirectory(ureq.getIdentity(), node.getShortTitle()));
				// delete element
				dialogElmsMgr.deleteDialogElement(coursePropMgr, courseNode, selectedElement.getForumKey());
				forumMgr.deleteForum(selectedElement.getForumKey());
				showOverviewTable(ureq, forumCallback);
				content.contextPut("hasSelectedElement", Boolean.FALSE);
				// do logging
				ThreadLocalUserActivityLogger.log(CourseLoggingAction.DIALOG_ELEMENT_FILE_DELETED, getClass(),
						LoggingResourceable.wrapUploadFile(selectedElement.getFilename()));
			}
		}
	}

	/**
	 * deliver the selected file and show in a popup
	 * 
	 * @param ureq
	 * @param command
	 */
	private void doFileDelivery(final UserRequest ureq, final Long forumKey) {
		final OlatRootFolderImpl forumContainer = getForumContainer(forumKey);
		final VFSLeaf vl = (VFSLeaf) forumContainer.getItems(new VFSLeafFilter()).get(0);

		// ureq.getDispatchResult().setResultingMediaResource(new FileDialogMediaResource(vl));
		ureq.getDispatchResult().setResultingMediaResource(new VFSMediaResource(vl));
		// do logging
		ThreadLocalUserActivityLogger.log(CourseLoggingAction.DIALOG_ELEMENT_FILE_DOWNLOADED, getClass(), LoggingResourceable.wrapBCFile(vl.getName()));
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// process my content events
		if (source == content) {
			final String command = event.getCommand();
			if (command.equals(ACTION_SHOW_FILE)) {
				doFileDelivery(ureq, selectedElement.getForumKey());
			}
		} else if (source == uploadButton) {
			final Forum forum = forumMgr.addAForum();
			final OlatRootFolderImpl forumContainer = getForumContainer(forum.getKey());

			removeAsListenerAndDispose(fileUplCtr);
			fileUplCtr = new FileUploadController(getWindowControl(), forumContainer, ureq, (int) FolderConfig.getLimitULKB(), Quota.UNLIMITED, null, false);
			listenTo(fileUplCtr);

			recentDialogElement = new DialogElement();
			recentDialogElement.setForumKey(forum.getKey());
			recentDialogElement.setAuthor(ureq.getIdentity().getName());
			dialogPanel.setContent(fileUplCtr.getInitialComponent());
		}

	}

	/**
	 * to save content
	 * 
	 * @param forumKey
	 * @return
	 */
	public static OlatRootFolderImpl getForumContainer(final Long forumKey) {
		final StringBuilder sb = new StringBuilder();
		sb.append("/forum/");
		sb.append(forumKey);
		sb.append("/");
		final String pathToForumDir = sb.toString();
		final OlatRootFolderImpl forumContainer = new OlatRootFolderImpl(pathToForumDir, null);
		final File baseFile = forumContainer.getBasefile();
		baseFile.mkdirs();
		return forumContainer;
	}

	public static String getFileSize(final Long forumKey) {
		final OlatRootFolderImpl forumContainer = getForumContainer(forumKey);
		final VFSLeaf vl = (VFSLeaf) forumContainer.getItems().get(0);
		return StringHelper.formatMemory(vl.getSize());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

}
