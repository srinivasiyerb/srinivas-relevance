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

package org.olat.core.commons.modules.bc.commands;

import java.util.Collections;
import java.util.List;

import org.olat.core.commons.modules.bc.FileSelection;
import org.olat.core.commons.modules.bc.FolderEvent;
import org.olat.core.commons.modules.bc.components.FolderComponent;
import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.commons.modules.bc.meta.MetaInfoFactory;
import org.olat.core.commons.modules.bc.meta.MetaInfoHelper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.vfs.OlatRelPathImpl;
import org.olat.core.util.vfs.VFSConstants;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;

public class CmdDelete extends BasicController implements FolderCommand {

	private static int status = FolderCommandStatus.STATUS_SUCCESS;

	private Translator translator;
	private FolderComponent folderComponent;
	private FileSelection fileSelection;

	private DialogBoxController dialogCtr;
	private DialogBoxController lockedFiledCtr;

	protected CmdDelete(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl);
	}

	@Override
	public Controller execute(FolderComponent fc, UserRequest ureq, WindowControl wContr, Translator trans) {
		this.translator = trans;
		this.folderComponent = fc;
		this.fileSelection = new FileSelection(ureq, fc.getCurrentContainerPath());

		VFSContainer currentContainer = folderComponent.getCurrentContainer();
		List<String> lockedFiles = MetaInfoHelper.hasLockedFiles(currentContainer, fileSelection, ureq);
		if (lockedFiles.isEmpty()) {
			String msg = trans.translate("del.confirm") + "<p>" + fileSelection.renderAsHtml() + "</p>";
			// create dialog controller
			dialogCtr = activateYesNoDialog(ureq, trans.translate("del.header"), msg, dialogCtr);
		} else {
			String msg = MetaInfoHelper.renderLockedMessageAsHtml(trans, currentContainer, lockedFiles);
			List<String> buttonLabels = Collections.singletonList(trans.translate("ok"));
			lockedFiledCtr = activateGenericDialog(ureq, trans.translate("lock.title"), msg, buttonLabels, lockedFiledCtr);
		}
		return this;
	}

	@Override
	public int getStatus() {
		return status;
	}

	public FileSelection getFileSelection() {
		return fileSelection;
	}

	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == dialogCtr) {
			if (DialogBoxUIFactory.isYesEvent(event)) {
				// do delete
				VFSContainer currentContainer = folderComponent.getCurrentContainer();
				List<String> files = fileSelection.getFiles();
				if (files.size() == 0) {
					// sometimes, browser sends empty form data...
					getWindowControl().setError(translator.translate("failed"));
					status = FolderCommandStatus.STATUS_FAILED;
					fireEvent(ureq, FOLDERCOMMAND_FINISHED);
				}
				for (String file : files) {
					VFSItem item = currentContainer.resolve(file);
					if (item != null && (item.canDelete() == VFSConstants.YES)) {
						if (item instanceof OlatRelPathImpl) {
							// delete all meta info
							MetaInfo meta = MetaInfoFactory.createMetaInfoFor((OlatRelPathImpl) item);
							if (meta != null) meta.deleteAll();
						}
						// delete the item itself
						item.delete();
					} else {
						getWindowControl().setWarning(translator.translate("del.partial"));
					}
				}

				String confirmationText = fileSelection.renderAsHtml();
				fireEvent(ureq, new FolderEvent(FolderEvent.DELETE_EVENT, confirmationText));
				fireEvent(ureq, FOLDERCOMMAND_FINISHED);
			} else {
				// abort
				status = FolderCommandStatus.STATUS_CANCELED;
				fireEvent(ureq, FOLDERCOMMAND_FINISHED);
			}
		}

	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		// no events to catch
	}

	@Override
	protected void doDispose() {
		// autodisposed by basic controller
	}

	@Override
	public boolean runsModal() {
		// this controller has its own modal dialog box
		return true;
	}

}
