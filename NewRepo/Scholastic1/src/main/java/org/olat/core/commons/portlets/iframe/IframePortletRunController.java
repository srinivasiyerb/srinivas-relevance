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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.commons.portlets.iframe;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.olat.core.commons.editor.htmleditor.WysiwygFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;

/**
 * Description:<br>
 * Run controller of the iframe portlet.
 * <P>
 * Initial Date: 11.07.2005 <br>
 * 
 * @author gnaegi
 */
public class IframePortletRunController extends BasicController {

	private OLog log = Tracing.createLoggerFor(this.getClass());

	private Panel panel;
	private VelocityContainer iframeVC;
	private Link editLink;
	private Controller editorCtr;
	private String fileName;
	private VFSContainer rootDir;
	private CloseableModalController cmc;

	protected IframePortletRunController(UserRequest ureq, WindowControl wControl, Map configuration) {
		super(ureq, wControl);

		this.iframeVC = this.createVelocityContainer("iframePortlet");

		// uri is mandatory
		String uri = (String) configuration.get("uri");
		/*
		 * temp fix for wrong config
		 */
		if (uri != null && uri.startsWith("$")) {
			uri = null;
			iframeVC.contextPut("ENABLED", Boolean.FALSE);
		} else {
			iframeVC.contextPut("ENABLED", Boolean.TRUE);
		}
		if (uri == null) log.warn("Missing argument 'uri' in iframeportlet configuration");
		this.iframeVC.contextPut("uri", uri);

		// height of iframe is mandatory
		String height = (String) configuration.get("height");
		if (height == null) log.warn("Missing argument 'height' in iframeportlet configuration");
		this.iframeVC.contextPut("height", height);

		// target attribute of iframe, should be unique on page
		String id = (String) configuration.get("id");
		this.iframeVC.contextPut("name", id);

		// edit Link only for administrators
		if (ureq.getUserSession().getRoles().isOLATAdmin()) {
			String editFilePath = (String) configuration.get("editFilePath");
			boolean editLinkEnabled = false;
			if (StringHelper.containsNonWhitespace(editFilePath)) {
				editLinkEnabled = initEditButton(ureq, editFilePath);
			} else {
				// ignore missing argument
				// editLinkEnabled false in this case
				log.warn("Missing argument 'editFilePath' in iframeportlet configuration");
			}
			editLink = LinkFactory.createButtonXSmall("edit", iframeVC, this);
			editLink.setEnabled(editLinkEnabled);// edit link always there, but disabled if something went wrong
		}

		panel = this.putInitialPanel(this.iframeVC);
	}

	/**
	 * Helper to build the editor button and initialize some variables needed by the editor
	 * 
	 * @param ureq
	 * @param editFilePath
	 */
	public boolean initEditButton(UserRequest ureq, String editFilePath) {
		if (editFilePath == null) {
			log.warn("initEditButton: editFilePath was null");
			return false;
		}
		File editFile = new File(editFilePath);
		File parent = editFile.getParentFile();
		if (parent == null) {
			log.warn("initEditButton: no parent folder for " + editFilePath);
			return false;
		}
		if (!editFile.getParentFile().exists()) {
			log.warn("editFilePath is wrong, not even parent dir exists::" + editFile.getParentFile().getAbsolutePath());
			return false;
		}
		if (!editFile.canWrite()) {
			log.warn("Can not write to file::" + editFile.getAbsolutePath());
			return false;
		}

		/**
		 * postcondition: editFilePath exists and we have permission to write.
		 */

		if (!editFile.exists()) {
			try {
				editFile.createNewFile();
			} catch (IOException e) {
				log.error("Cannot create file::" + editFile.getAbsolutePath());
				return false;
			}
		}
		// now as we have a writable file we initialize the path to this file
		// and the edit button
		fileName = editFile.getName();
		rootDir = new LocalFolderImpl(editFile.getParentFile());

		return true;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == editLink) {
			// start up editor controller as modal dialog
			if (editorCtr != null) editorCtr.dispose();
			editorCtr = WysiwygFactory.createWysiwygController(ureq, getWindowControl(), rootDir, fileName, true);
			this.listenTo(editorCtr);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), editorCtr.getInitialComponent());
			cmc.activate();
		}
	}

	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == editorCtr) {
			if (event == Event.CANCELLED_EVENT || event == Event.DONE_EVENT) {
				cmc.deactivate();
				// clean up memory
				editorCtr.dispose();
				editorCtr = null;
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// editorCtr is registerd with listenTo and gets disposed in BasicController
	}

}
