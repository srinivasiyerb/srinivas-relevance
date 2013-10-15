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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.core.commons.editor.htmleditor;

import java.io.InputStream;
import java.util.Date;

import org.olat.core.commons.controllers.linkchooser.CustomLinkTreeModel;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.richText.RichTextConfiguration;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.util.Encoder;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Formatter;
import org.olat.core.util.SimpleHtmlParser;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.version.Versionable;

/**
 * Description:<br>
 * The HTMLEditorController provides a full-fledged WYSIWYG HTML editor with support for media and link browsing based on a VFS item. The editor will keep any header
 * information such as references to CSS or JS files, but those will not be active while editing the file.
 * <p>
 * Keep in mind that this editor might be destructive when editing files that have been created with an external, more powerful editor.
 * <p>
 * Use the WYSIWYGFactory to create an instance.
 * <P>
 * Initial Date: 08.05.2009 <br>
 * 
 * @author gnaegi
 */
public class HTMLEditorController extends FormBasicController {
	// HTML constants
	static final String DOCTYPE = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n";
	static final String OPEN_HTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n";
	static final String OPEN_HEAD = "<head>";
	static final String CLOSE_HEAD = "</head>";
	static final String OPEN_TITLE = "<title>";
	static final String CLOSE_TITLE = "</title>";
	static final String EMTPY_TITLE = OPEN_TITLE + CLOSE_TITLE;
	static final String CLOSE_HTML = "\n<html>";
	static final String CLOSE_BODY_HTML = "</body></html>";
	static final String CLOSE_HEAD_OPEN_BODY = "</head><body>";
	// Editor version metadata to check if file has already been edited with this editor
	static final String GENERATOR = "olat-tinymce-";
	static final String GENERATOR_VERSION = "3";
	static final String GENERATOR_META = "<meta name=\"generator\" content=\"" + GENERATOR + GENERATOR_VERSION + "\" />\n";
	// Default char set for new files is UTF-8
	static final String UTF_8 = "utf-8";
	static final String UTF8CHARSET = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n";

	private String preface; // null if no head, otherwise head is kept in memory
	private String body; // Content of body tag
	private String charSet = UTF_8; // default for first parse attempt

	private String fileName, fileRelPath;
	private LockResult lock;

	private RichTextElement htmlElement;
	private VFSContainer baseContainer;
	private VFSLeaf fileLeaf;
	private FormLink cancel, save, saveClose;
	private CustomLinkTreeModel customLinkTreeModel;

	private VelocityContainer metadataVC;
	private boolean newFile = true;
	private boolean editorCheckEnabled = true; // default

	/**
	 * Factory method to create a file based HTML editor instance that uses locking to prevent two people editing the same file.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param baseContainer the baseContainer (below that folder all images can be chosen)
	 * @param relFilePath the file e.g. "index.html"
	 * @param userActivityLogger the userActivity Logger if used
	 * @param customLinkTreeModel Model for internal-link tree e.g. course-node tree with link information
	 * @param editorCheckEnabled true: check if file has been created with another tool and warn user about potential data loss; false: ignore other authoring tools
	 * @return Controller with internal-link selector
	 */
	protected HTMLEditorController(UserRequest ureq, WindowControl wControl, VFSContainer baseContainer, String relFilePath, CustomLinkTreeModel customLinkTreeModel,
			boolean editorCheckEnabled) {
		super(ureq, wControl, "htmleditor");
		// set some basic variables
		this.baseContainer = baseContainer;
		this.fileRelPath = relFilePath;
		this.customLinkTreeModel = customLinkTreeModel;
		this.editorCheckEnabled = editorCheckEnabled;
		// make sure the filename doesn't start with a slash
		this.fileName = ((relFilePath.charAt(0) == '/') ? relFilePath.substring(1) : relFilePath);
		this.fileLeaf = (VFSLeaf) baseContainer.resolve(fileName);
		if (fileLeaf == null) throw new AssertException("file::" + getFileDebuggingPath(baseContainer, relFilePath) + " does not exist!");
		// check if someone else is already editing the file
		if (fileLeaf instanceof LocalFileImpl) {
			// Cast to LocalFile necessary because the VFSItem is missing some
			// ID mechanism that identifies an item within the system
			OLATResourceable lockResourceable = OresHelper.createOLATResourceableTypeWithoutCheck(fileLeaf.toString());
			// OLAT-5066: the use of "fileName" gives users the (false) impression that the file they wish to access
			// is already locked by someone else. Since the lock token must be smaller than 50 characters we us an
			// MD5 hash of the absolute file path which will always be 32 characters long and virtually unique.
			String lockToken = Encoder.encrypt(getFileDebuggingPath(baseContainer, relFilePath));
			this.lock = CoordinatorManager.getInstance().getCoordinator().getLocker().acquireLock(lockResourceable, ureq.getIdentity(), lockToken);
			VelocityContainer vc = (VelocityContainer) flc.getComponent();
			if (!lock.isSuccess()) {
				vc.contextPut("locked", Boolean.TRUE);
				vc.contextPut("lockOwner", lock.getOwner().getName());
				return;
			} else {
				vc.contextPut("locked", Boolean.FALSE);
			}
		}
		// Parse the content of the page
		this.body = parsePage(fileLeaf);
		// load form now
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
		releaseLock();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(UserRequest ureq) {
		// form does not have button, form ok is triggered when user presses
		// command-save or uses the save icon in the toolbar
		doSaveData();
		// override dirtyness of form layout container to prevent redrawing of editor
		this.flc.setDirty(false);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		super.formInnerEvent(ureq, source, event);
		if (source == htmlElement) {
			// nothing to catch
		} else if (source == save && lock != null) {
			doSaveData();
		} else if (source == saveClose && lock != null) {
			doSaveData();
			fireEvent(ureq, Event.DONE_EVENT);
			releaseLock();
		} else if (source == cancel) {
			fireEvent(ureq, Event.CANCELLED_EVENT);
			releaseLock();
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		htmlElement = uifactory.addRichTextElementForFileData("rtfElement", null, body, -1, -1, false, baseContainer, fileName, customLinkTreeModel, formLayout,
				ureq.getUserSession(), getWindowControl());
		//
		// Add resize handler
		RichTextConfiguration editorConfiguration = htmlElement.getEditorConfiguration();
		editorConfiguration.addOnInitCallbackFunction("b_resizetofit_htmleditor");
		editorConfiguration.setNonQuotedConfigValue(RichTextConfiguration.HEIGHT, "b_initialEditorHeight()");
		//
		// The buttons
		save = uifactory.addFormLink("savebuttontext", formLayout, Link.BUTTON);
		save.addActionListener(this, FormEvent.ONCLICK);
		cancel = uifactory.addFormLink("cancel", formLayout, Link.BUTTON);
		cancel.addActionListener(this, FormEvent.ONCLICK);
		saveClose = uifactory.addFormLink("saveandclosebuttontext", formLayout, Link.BUTTON);
		saveClose.addActionListener(this, FormEvent.ONCLICK);
		//
		// Add some file metadata
		VelocityContainer vc = (VelocityContainer) formLayout.getComponent();
		metadataVC = createVelocityContainer("metadata");
		vc.put("metadata", metadataVC);
		long lm = fileLeaf.getLastModified();
		metadataVC.contextPut("lastModified", Formatter.getInstance(ureq.getLocale()).formatDateAndTime(new Date(lm)));
		metadataVC.contextPut("charSet", charSet);
		metadataVC.contextPut("fileName", fileName);
	}

	/**
	 * Optional configuration option to display the save button below the HTML editor form. This will not disable the save button in the tinyMCE bar (if available).
	 * Default: true
	 * 
	 * @param buttonEnabled true: show save button; false: hide save button
	 */
	public void setSaveButtonEnabled(boolean buttonEnabled) {
		save.setVisible(buttonEnabled);
	}

	/**
	 * Optional configuration option to display the save-and-close button below the HTML editor form. This will not disable the save button in the tinyMCE bar (if
	 * available). Default: true
	 * 
	 * @param buttonEnabled true: show save-and-close button; false: hide save-and-close button
	 */
	public void setSaveCloseButtonEnabled(boolean buttonEnabled) {
		saveClose.setVisible(buttonEnabled);
	}

	/**
	 * Optional configuration option to display the cancel button below the HTML editor form. This will not disable the cancel button in the tinyMCE bar (if available).
	 * Default: true
	 * 
	 * @param buttonEnabled true: show cancel button; false: hide cancel button
	 */
	public void setCancelButtonEnabled(boolean buttonEnabled) {
		cancel.setVisible(buttonEnabled);
	}

	/**
	 * Optional configuration to show the file name, file encoding and last modified date in a header bar. Default: true
	 * 
	 * @param metadataEnabled true: show metadata; false: hide metadata
	 */
	public void setShowMetadataEnabled(boolean metadataEnabled) {
		VelocityContainer vc = (VelocityContainer) this.flc.getComponent();
		if (metadataEnabled) {
			vc.put("metadata", metadataVC);
		} else {
			vc.remove(metadataVC);
		}
	}

	/**
	 * Get the rich text config object. This can be used to fine-tune the editor features, e.g. to enable additional buttons or to remove available buttons
	 * 
	 * @return
	 */
	public RichTextConfiguration getRichTextConfiguration() {
		return htmlElement.getEditorConfiguration();
	}

	/**
	 * Internal helper to parse the page content
	 * 
	 * @param vfsLeaf
	 * @return String containing the page body
	 */
	private String parsePage(VFSLeaf vfsLeaf) {
		// Load data with given encoding
		InputStream is = vfsLeaf.getInputStream();
		if (is == null) { throw new AssertException("Could not open input stream for file::" + getFileDebuggingPath(this.baseContainer, this.fileRelPath)); }
		this.charSet = SimpleHtmlParser.extractHTMLCharset(vfsLeaf);
		String leafData = FileUtils.load(is, charSet);
		if (leafData == null || leafData.length() == 0) {
			leafData = "";
		}
		int generatorPos = leafData.indexOf(GENERATOR);
		SimpleHtmlParser parser = new SimpleHtmlParser(leafData);
		StringBuilder sb = new StringBuilder();
		if (parser.getHtmlDocType() != null) sb.append(parser.getHtmlDocType());
		if (parser.getXhtmlNamespaces() != null) {
			sb.append(parser.getXhtmlNamespaces());
		} else {
			sb.append(CLOSE_HTML);
		}
		sb.append(OPEN_HEAD);
		// include generator so foreign editor warning only appears once
		if (generatorPos == -1) sb.append(GENERATOR_META);
		if (parser.getHtmlHead() != null) sb.append(parser.getHtmlHead());
		sb.append(CLOSE_HEAD);
		sb.append(parser.getBodyTag());
		preface = sb.toString();

		// warn if the file has no generator metadata and is not empty
		if (generatorPos == -1 && leafData.length() > 0) {
			if (editorCheckEnabled) showWarning("warn.foreigneditor");
			// else ignore warning but try to keep header anyway
		} else if (leafData.length() == 0) {
			// set new one when file created with this editor
			preface = null;
		}
		// now get the body part
		return parser.getHtmlContent();
	}

	/**
	 * Event implementation for savedata
	 * 
	 * @param ureq
	 */
	private void doSaveData() {
		// No XSS checks, are done in the HTML editor - users can upload illegal
		// stuff, JS needs to be enabled for users
		String content = htmlElement.getRawValue();
		// If preface was null -> append own head and save it in utf-8. Preface
		// is the header that was in the file when we opened the file
		StringBuffer fileContent = new StringBuffer();
		if (preface == null) {
			fileContent.append(DOCTYPE).append(OPEN_HTML).append(OPEN_HEAD);
			fileContent.append(GENERATOR_META).append(UTF8CHARSET);
			// In new documents, create empty title to be W3C conform. Title
			// is mandatory element in meta element.
			fileContent.append(EMTPY_TITLE);
			fileContent.append(CLOSE_HEAD_OPEN_BODY);
			fileContent.append(content);
			fileContent.append(CLOSE_BODY_HTML);
			charSet = UTF_8; // use utf-8 by default for new files
		} else {
			// existing preface, just reinsert so we don't lose stuff the user put
			// in there
			fileContent.append(preface).append(content).append(CLOSE_BODY_HTML);
		}

		// save the file
		if (fileLeaf instanceof Versionable && ((Versionable) fileLeaf).getVersions().isVersioned()) {
			InputStream inStream = FileUtils.getInputStream(fileContent.toString(), charSet);
			((Versionable) fileLeaf).getVersions().addVersion(getIdentity(), "", inStream);
		} else {
			FileUtils.save(fileLeaf.getOutputStream(false), fileContent.toString(), charSet);
		}

		// Update last modified date in view
		long lm = fileLeaf.getLastModified();
		metadataVC.contextPut("lastModified", Formatter.getInstance(getLocale()).formatDateAndTime(new Date(lm)));
		// Set new content as default value in element
		htmlElement.setNewOriginalValue(content);
	}

	/**
	 * Helper method to get a meaningfull debugging filename from the vfs container and the file path
	 * 
	 * @param root
	 * @param relPath
	 * @return
	 */
	private String getFileDebuggingPath(VFSContainer root, String relPath) {
		String path = relPath;
		VFSContainer dir = root;
		while (dir != null) {
			path = "/" + dir.getName() + path;
			dir = dir.getParentContainer();
		}
		return path;
	}

	/**
	 * Releases the lock for this page if set
	 */
	private void releaseLock() {
		if (lock != null) {
			CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lock);
			lock = null;
		}
	}

	public boolean isNewFile() {
		return newFile;
	}

	public void setNewFile(boolean newFile) {
		this.newFile = newFile;
	}
}
