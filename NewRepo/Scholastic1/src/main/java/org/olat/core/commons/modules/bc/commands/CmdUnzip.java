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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.olat.core.commons.modules.bc.FileSelection;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.components.FolderComponent;
import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.commons.modules.bc.meta.MetaInfoHelper;
import org.olat.core.commons.modules.bc.meta.tagged.MetaTagged;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.VFSConstants;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;

public class CmdUnzip extends BasicController implements FolderCommand {

	private int status = FolderCommandStatus.STATUS_SUCCESS;

	private Translator translator;
	private DialogBoxController lockedFiledCtr;

	public CmdUnzip(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl);
	}

	@Override
	public Controller execute(FolderComponent folderComponent, UserRequest ureq, WindowControl wContr, Translator trans) {
		this.translator = trans;
		FileSelection selection = new FileSelection(ureq, folderComponent.getCurrentContainerPath());
		VFSContainer currentContainer = folderComponent.getCurrentContainer();
		if (!(currentContainer.canWrite() == VFSConstants.YES)) throw new AssertException("Cannot unzip to folder. Writing denied.");

		// check if command is executed on a file containing invalid filenames or paths - checks if the resulting folder has a valid name
		if (selection.getInvalidFileNames().size() > 0) {
			status = FolderCommandStatus.STATUS_INVALID_NAME;
			return null;
		}

		List<String> lockedFiles = new ArrayList<String>();
		for (String sItem : selection.getFiles()) {
			VFSItem vfsItem = currentContainer.resolve(sItem);
			if (vfsItem instanceof VFSLeaf) {
				boolean isAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
				lockedFiles.addAll(checkLockedFiles((VFSLeaf) vfsItem, currentContainer, ureq.getIdentity(), isAdmin));
			}
		}

		if (!lockedFiles.isEmpty()) {
			String msg = MetaInfoHelper.renderLockedMessageAsHtml(trans, null, lockedFiles);
			List<String> buttonLabels = Collections.singletonList(trans.translate("ok"));
			lockedFiledCtr = activateGenericDialog(ureq, trans.translate("lock.title"), msg, buttonLabels, lockedFiledCtr);
			return null;
		}

		boolean fileNotExist = false;
		for (String sItem : selection.getFiles()) {
			VFSItem vfsItem = currentContainer.resolve(sItem);
			if (vfsItem != null && (vfsItem instanceof VFSLeaf)) {
				if (!doUnzip((VFSLeaf) vfsItem, currentContainer, ureq, wContr)) {
					status = FolderCommandStatus.STATUS_FAILED;
					break;
				}
			} else {
				fileNotExist = true;
				break;
			}
		}

		if (fileNotExist) {
			status = FolderCommandStatus.STATUS_FAILED;
			getWindowControl().setError(translator.translate("FileDoesNotExist"));
		}

		return null;
	}

	private List<String> checkLockedFiles(VFSLeaf vfsItem, VFSContainer currentContainer, Identity identity, boolean isAdmin) {
		String name = vfsItem.getName();
		if (!name.toLowerCase().endsWith(".zip")) { return Collections.emptyList(); }

		boolean versioning = FolderConfig.versionsEnabled(currentContainer);
		if (!versioning) {
			// this command don't overwrite existing folders
			return Collections.emptyList();
		}

		String sZipContainer = name.substring(0, name.length() - 4);
		VFSItem zipContainer = currentContainer.resolve(sZipContainer);
		if (zipContainer == null) {
			return Collections.emptyList();
		} else if (zipContainer instanceof VFSContainer) {
			return ZipUtil.checkLockedFileBeforeUnzip(vfsItem, (VFSContainer) zipContainer, identity, isAdmin);
		} else {
			// replace a file with a folder ???
			return Collections.emptyList();
		}
	}

	private boolean doUnzip(VFSLeaf vfsItem, VFSContainer currentContainer, UserRequest ureq, WindowControl wControl) {
		String name = vfsItem.getName();
		if (!name.toLowerCase().endsWith(".zip")) {
			wControl.setError(translator.translate("FileUnzipFailed", new String[] { vfsItem.getName() }));
			return false;
		}

		// we make a new folder with the same name as the zip file
		String sZipContainer = name.substring(0, name.length() - 4);

		boolean versioning = FolderConfig.versionsEnabled(currentContainer);

		VFSContainer zipContainer = currentContainer.createChildContainer(sZipContainer);
		if (zipContainer == null) {
			if (versioning) {
				zipContainer = (VFSContainer) currentContainer.resolve(sZipContainer);
			} else {
				// folder already exists... issue warning
				wControl.setError(translator.translate("unzip.alreadyexists", new String[] { sZipContainer }));
				return false;
			}
		} else if (zipContainer instanceof MetaTagged) {
			MetaInfo info = ((MetaTagged) zipContainer).getMetaInfo();
			if (info != null && ureq.getIdentity() != null) {
				info.setAuthor(ureq.getIdentity().getName());
				info.write();
			}
		}

		if (!ZipUtil.unzip(vfsItem, zipContainer, ureq.getIdentity(), versioning)) {
			// operation failed - rollback
			zipContainer.delete();
			wControl.setError(translator.translate("failed"));
			return false;
		} else {
			// check quota
			long quotaLeftKB = VFSManager.getQuotaLeftKB(currentContainer);
			if (quotaLeftKB != Quota.UNLIMITED && quotaLeftKB < 0) {
				// quota exceeded - rollback
				zipContainer.delete();
				wControl.setError(translator.translate("QuotaExceeded"));
				return false;
			}
		}
		return true;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public boolean runsModal() {
		return false;
	}

	@Override
	protected void doDispose() {
		// autodisposed by BasicController
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		// no events to catch
	}

	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		// no events to catch
	}

}
