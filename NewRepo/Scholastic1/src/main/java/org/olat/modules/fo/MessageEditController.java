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
 * Copyright (c) 2009 at frentix GmbH, www.frentix.com
 * <p>
 */
package org.olat.modules.fo;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.FormUIFactory;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.filters.VFSItemExcludePrefixFilter;
import org.olat.user.DisplayPortraitController;

/**
 * Description:<br>
 * Can be used for creating / editing or replying to a thread/post. editmode is set to do internal mode-switch, where needed.
 * <P>
 * Initial Date: 18.06.2009 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, frentix GmbH
 */
public class MessageEditController extends FormBasicController {

	protected static final String EDITMODE_NEWTHREAD = "newthread";
	protected static final String EDITMODE_EDITMSG = "editmsg";
	protected static final String EDITMODE_REPLYMSG = "replymsg";
	private static final String STICKY_SET_IDENTIFIER = "stickyset";
	private static final String CMD_DELETE_ATTACHMENT = "delete.attachment.";
	protected static final Integer MAX_BODY_LENGTH = 4000;

	// see OLAT-4182/OLAT-4219 and OLAT-4259
	// the filtering of .nfs is sort of temporary until we make sure that we no longer reference
	// attached files anywhere at the time of deleting it
	// likely to be resolved after user logs out, caches get cleared - and if not the server
	// restart overnight definitely removes those .nfs files.
	protected static final String[] ATTACHMENT_EXCLUDE_PREFIXES = new String[] { ".nfs", ".CVS", ".DS_Store" };

	private final ForumCallback forumCallback;
	private TextElement msgTitle;
	private RichTextElement msgBody;
	private MultipleSelectionElement stickyCheckBox;
	private String editMode;
	private FileElement fileUpload;

	private Message message, replyMessage = null;
	private DisplayPortraitController portraitCtr;
	private final ForumManager fm;
	private DialogBoxController delAttCtr;
	private OlatRootFolderImpl tempUploadFolder;
	private boolean userIsMsgCreator;
	private boolean msgHasChildren;
	private final VFSItemExcludePrefixFilter exclFilter;

	/**
	 * @param ureq
	 * @param control
	 * @param forumCallback
	 * @param message may be a new message created by ForumManager.createMessage() which is not yet saved in db
	 * @param quoteMessage may be null if Editor isn't used to reply to a message
	 */
	public MessageEditController(final UserRequest ureq, final WindowControl control, final ForumCallback forumCallback, final Message message, final Message quoteMessage) {
		super(ureq, control, FormBasicController.LAYOUT_VERTICAL);
		this.forumCallback = forumCallback;
		this.message = message;
		this.fm = ForumManager.getInstance();

		tempUploadFolder = new OlatRootFolderImpl(File.separator + "tmp/" + CodeHelper.getGlobalForeverUniqueID() + "/", null);
		// nfs creates .nfs12345 - files during deletion, those shouldn't be displayed / copied after save
		// See OLAT-4182 and OLAT-4219
		exclFilter = new VFSItemExcludePrefixFilter(ATTACHMENT_EXCLUDE_PREFIXES);

		// decide which mode is used
		this.editMode = "";
		if (message.getKey() == null) {
			editMode = EDITMODE_NEWTHREAD;
		} else if (quoteMessage == null && message.getKey() != null) {
			editMode = EDITMODE_EDITMSG;
		} else if (quoteMessage != null) {
			editMode = EDITMODE_REPLYMSG;
			this.replyMessage = message;
			this.message = quoteMessage;
		} else {
			throw new AssertException("EditModus for Forum could not be determined. Error in logic or wrong parameters for this constructor", null);
		}
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		msgTitle = uifactory.addTextElement("msgTitle", "msg.title", 100, message.getTitle(), formLayout);
		msgTitle.setMandatory(true);
		msgTitle.setNotEmptyCheck("error.field.not.empty");
		msgBody = uifactory.addRichTextElementForStringData("msgBody", "msg.body", message.getBody(), 15, -1, false, true, null, null, formLayout, ureq.getUserSession(),
				getWindowControl());
		msgBody.setMandatory(true);
		msgBody.setNotEmptyCheck("error.field.not.empty");
		msgBody.setMaxLength(MAX_BODY_LENGTH);
		msgBody.setNotLongerThanCheck(MAX_BODY_LENGTH, "input.toolong");

		// attachment upload
		uifactory.addStaticTextElement("attachmentTitle", null, translate("attachments"), formLayout);// null -> no label

		setEditPermissions(ureq, message);
		// list existing attachments. init attachment layout now, to place it in
		// right position
		createOrUpdateAttachmentListLayout(formLayout);

		// provide upload field
		if (forumCallback.mayEditMessageAsModerator() || ((userIsMsgCreator) && (msgHasChildren == false))) {
			fileUpload = uifactory.addFileElement("msg.upload", formLayout);
			fileUpload.addActionListener(listener, FormEvent.ONCHANGE);
			fileUpload.setMaxUploadSizeKB((int) FolderConfig.getLimitULKB(), "attachments.too.big",
					new String[] { ((Long) (FolderConfig.getLimitULKB() / 1024)).toString() });
		}

		// show stickyCheckBox only if moderator and message is threadtop
		stickyCheckBox = uifactory.addCheckboxesHorizontal("stickyCheckBox", null, formLayout, new String[] { STICKY_SET_IDENTIFIER },
				new String[] { translate("msg.sticky") }, new String[] { "" });
		final Status msgStatus = Status.getStatus(message.getStatusCode());
		if (msgStatus.isSticky()) {
			stickyCheckBox.select(STICKY_SET_IDENTIFIER, true);
		}
		if (!(forumCallback.mayEditMessageAsModerator() && message.getParent() == null)) {
			stickyCheckBox.setVisible(false);
		}

		// save and cancel buttons
		final FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		formLayout.add(buttonLayout);
		uifactory.addFormSubmitButton("msg.save", buttonLayout);
		uifactory.addFormCancelButton("msg.cancel", buttonLayout, ureq, getWindowControl());

		// show message replying to, if in reply modus
		if (editMode.equals(EDITMODE_REPLYMSG)) {
			final FormLayoutContainer replyMsgLayout = FormLayoutContainer.createCustomFormLayout("replyMsg", getTranslator(),
					Util.getPackageVelocityRoot(this.getClass()) + "/msg-preview.html");
			uifactory.addSpacerElement("spacer1", formLayout, false);
			formLayout.add(replyMsgLayout);
			replyMsgLayout.setLabel("label.replytomsg", new String[] { replyMessage.getTitle() });
			final Identity identity = replyMessage.getCreator();
			replyMsgLayout.contextPut("identity", identity);
			replyMsgLayout.contextPut("messageBody", replyMessage.getBody());
			replyMsgLayout.contextPut("message", replyMessage);
			portraitCtr = new DisplayPortraitController(ureq, getWindowControl(), identity, true, true);
			replyMsgLayout.put("portrait", portraitCtr.getInitialComponent());
		}

	}

	private void setEditPermissions(final UserRequest ureq, final Message msg) {
		// defaults for a new message
		userIsMsgCreator = true;
		msgHasChildren = false;
		// set according to message
		if (msg.getKey() != null) {
			userIsMsgCreator = ureq.getIdentity().getKey().equals(msg.getCreator().getKey());
			msgHasChildren = fm.hasChildren(msg);
		}
	}

	// adds or updates the list of already existing attachments with a delete
	// button for each
	private void createOrUpdateAttachmentListLayout(final FormItemContainer formLayout) {
		final FormUIFactory formUIf = FormUIFactory.getInstance();
		final FormItem attachLayout = formLayout.getFormComponent("attachLayout");

		final List<VFSItem> attachments = new ArrayList<VFSItem>();
		// add already existing attachments:
		if (message.getKey() != null) {
			final OlatRootFolderImpl msgContainer = fm.getMessageContainer(message.getForum().getKey(), message.getKey());
			attachments.addAll(msgContainer.getItems(exclFilter));
		}
		// add files from TempFolder
		attachments.addAll(getTempFolderFileList());

		Collections.sort(attachments, new Comparator<VFSItem>() {
			final Collator c = Collator.getInstance(getLocale());

			@Override
			public int compare(final VFSItem o1, final VFSItem o2) {
				return c.compare((o1).getName(), (o2).getName());
			}
		});

		FormLayoutContainer tmpLayout;
		if (attachLayout == null) {
			tmpLayout = FormLayoutContainer.createCustomFormLayout("attachLayout", getTranslator(), Util.getPackageVelocityRoot(this.getClass())
					+ "/attachments-editview.html");
			formLayout.add(tmpLayout);
		} else {
			tmpLayout = (FormLayoutContainer) attachLayout;
		}
		tmpLayout.contextPut("attachments", attachments);
		tmpLayout.contextPut("myself", this);

		// add delete links for each attachment if user is allowed to see them
		int attNr = 1;
		for (final Iterator<VFSItem> iterator = attachments.iterator(); iterator.hasNext();) {
			final VFSItem tmpFile = iterator.next();
			final FormLink tmpLink = formUIf.addFormLink(CMD_DELETE_ATTACHMENT + attNr, tmpLayout, Link.BUTTON_XSMALL);
			if (!(forumCallback.mayEditMessageAsModerator() || ((userIsMsgCreator) && (msgHasChildren == false)))) {
				tmpLink.setEnabled(false);
				tmpLink.setVisible(false);
			}
			tmpLink.setUserObject(tmpFile);
			tmpLink.setI18nKey("attachments.remove.string");
			attNr++;
		}
	}

	// TODO:RH:forum try to use a generic way to get fileIcon for VFSItem
	public String renderFileIconCssClass(final String filename) {
		final String filetype = filename.substring(filename.lastIndexOf(".") + 1);
		if (filetype == null) { return "b_filetype_file"; // default
		}
		return "b_filetype_" + filetype;
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
		removeTempUploadedFiles();
		if (portraitCtr != null) {
			portraitCtr.dispose();
			portraitCtr = null;
		}
		if (delAttCtr != null) {
			delAttCtr.dispose();
			delAttCtr = null;
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(final UserRequest ureq) {
		// if msg exist -> persist uploads directly to final dest
		if (message.getKey() != null) {
			persistTempUploadedFiles(message);
		}
		// prevent modifying an old object!
		if (getLastEditModus().equals(EDITMODE_EDITMSG)) {
			message = fm.loadMessage(message.getKey());
		}
		// set values from form to message
		saveValuesToMessage(message);
		fireEvent(ureq, Event.DONE_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formCancelled(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formCancelled(final UserRequest ureq) {
		// remove uploaded files if editing is canceled
		removeTempUploadedFiles();
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formInnerEvent(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.FormItem, org.olat.core.gui.components.form.flexible.impl.FormEvent)
	 */
	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, @SuppressWarnings("unused") final FormEvent event) {
		if (source == fileUpload) {
			if (fileUpload.isUploadSuccess()) {
				final String fileName = fileUpload.getUploadFileName();
				if (fileUpload.getUploadSize() / 1024 < fileUpload.getMaxUploadSizeKB()) {

					// checking tmp-folder and msg-container for filename
					boolean fileExists = false;
					if (getTempFolderFileList().contains(fileName)) {
						fileExists = true;
					}
					if (message.getKey() != null) {
						final OlatRootFolderImpl msgContainer = fm.getMessageContainer(message.getForum().getKey(), message.getKey());
						if (msgContainer.resolve(fileName) != null) {
							fileExists = true;
						}
					}

					if (fileExists) {
						fileUpload.setErrorKey("attachments.error.file.exists", null);
						fileUpload.getUploadFile().delete();
						fileUpload.showError(true);
					} else {
						// files got stored in an extra tempFolder, to use the same
						// fileUploader multiple times
						fileUpload.moveUploadFileTo(tempUploadFolder.getBasefile());
						fileUpload.showError(false);
						fileUpload.reset();

						createOrUpdateAttachmentListLayout(this.flc);
						showInfo("attachments.upload.successful", fileName);
					}
				} else {
					fileUpload.setErrorKey("attachments.too.big", new String[] { ((Integer) (fileUpload.getMaxUploadSizeKB() / 1024)).toString() });
					fileUpload.getUploadFile().delete();
					fileUpload.showError(true);
				}
			}
		} else if (source instanceof FormLink) {
			final FormLink activeLink = (FormLink) source;
			// attachment delete button may have been pressed
			final Object userObj = activeLink.getUserObject();
			if (userObj != null) {
				setEditPermissions(ureq, message);
				if (userObj instanceof VFSLeaf) {
					final VFSLeaf file = (VFSLeaf) userObj;
					if (forumCallback.mayEditMessageAsModerator() || ((userIsMsgCreator) && (msgHasChildren == false))) {
						delAttCtr = activateYesNoDialog(ureq, null, translate("reallydeleteAtt"), delAttCtr);
						delAttCtr.setUserObject(file);
					} else {
						if ((userIsMsgCreator) && (msgHasChildren == true)) {
							// user is author of the current message but it has already at
							// least one child
							showWarning("may.not.delete.att.as.author");
						} else {
							// user isn't author of the current message
							showInfo("may.not.delete.att");
						}
					}
				}
			}
		}
	}

	private List<VFSItem> getTempFolderFileList() {
		if (tempUploadFolder == null) {
			tempUploadFolder = new OlatRootFolderImpl(File.separator + "tmp/" + CodeHelper.getGlobalForeverUniqueID() + "/", null);
		}
		return tempUploadFolder.getItems(exclFilter);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		super.event(ureq, source, event);
		if (source == delAttCtr) {
			if (DialogBoxUIFactory.isYesEvent(event)) { // ok to really delete this
														// attachment
				final DialogBoxController dbCtr = (DialogBoxController) source;
				final Object userObj = dbCtr.getUserObject();
				if (userObj instanceof VFSLeaf) {
					final VFSLeaf file = (VFSLeaf) userObj;
					file.delete();
					showInfo("delete.att.ok");
					createOrUpdateAttachmentListLayout(this.flc);
				}
			}
		}
	}

	private void saveValuesToMessage(final Message tmpMessage) {
		tmpMessage.setTitle(msgTitle.getValue());
		String newBody = msgBody.getValue();
		// strip 1 empty line from beginning and end.
		if (newBody.startsWith(ForumController.TINYMCE_EMPTYLINE_CODE)) {
			newBody = newBody.substring(newBody.indexOf(ForumController.TINYMCE_EMPTYLINE_CODE) + ForumController.TINYMCE_EMPTYLINE_CODE.length());
		}
		if (newBody.endsWith(ForumController.TINYMCE_EMPTYLINE_CODE)) {
			newBody = newBody.substring(0, newBody.lastIndexOf(ForumController.TINYMCE_EMPTYLINE_CODE));
		}
		newBody = newBody.trim();
		tmpMessage.setBody(newBody);
		final Status msgStatus = Status.getStatus(tmpMessage.getStatusCode());
		final boolean isSticky = stickyCheckBox.getSelectedKeys().contains(STICKY_SET_IDENTIFIER);
		msgStatus.setSticky(isSticky);
		tmpMessage.setStatusCode(Status.getStatusCode(msgStatus));
	}

	/**
	 * Used to get the message edited right before. the new values got saved to it locally by formOK()
	 * 
	 * @return the edited message
	 */
	public Message getMessageBackAfterEdit() {
		if (!StringHelper.containsNonWhitespace(message.getTitle()) && message == null) {
			throw new AssertException("Getting back the edited message failed! You first have to edit one and intialize properly!");
		} else {
			return message;
		}
	}

	/**
	 * gives back the mode in which the editor was (create/edit/reply)
	 * 
	 * @return editMode which can be matched against static Strings from this class
	 */
	public String getLastEditModus() {
		return editMode;
	}

	/**
	 * - used locally if in edit mode where the msg-key is known - called from ForumController after creating a thread or a reply to copy temp files to msg-folder
	 * 
	 * @param tmpMessage
	 */
	public void persistTempUploadedFiles(final Message tmpMessage) {
		if (tmpMessage == null) { throw new AssertException("Message may not be null to persist temp files"); }
		final OlatRootFolderImpl msgContainer = fm.getMessageContainer(message.getForum().getKey(), message.getKey());
		if (msgContainer != null) {
			final List<VFSItem> tmpFList = getTempFolderFileList();
			for (final VFSItem file : tmpFList) {
				final VFSLeaf leaf = (VFSLeaf) file;
				try {
					FileUtils.bcopy(leaf.getInputStream(), msgContainer.createChildLeaf(leaf.getName()).getOutputStream(false), "forumSaveUploadedFile");
				} catch (final IOException e) {
					removeTempUploadedFiles();
					throw new RuntimeException("I/O error saving uploaded file:" + msgContainer.getBasefile().getAbsolutePath() + File.separator + leaf.getName());
				}
			}
		}
		removeTempUploadedFiles();
	}

	private void removeTempUploadedFiles() {
		if (tempUploadFolder != null) {
			tempUploadFolder.delete();
			tempUploadFolder = null;
		}
	}

}
