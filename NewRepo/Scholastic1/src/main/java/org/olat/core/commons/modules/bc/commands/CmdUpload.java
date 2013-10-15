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

import org.olat.core.commons.modules.bc.FileUploadController;
import org.olat.core.commons.modules.bc.FolderEvent;
import org.olat.core.commons.modules.bc.components.FolderComponent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.impl.elements.FileElementImpl;
import org.olat.core.gui.components.progressbar.ProgressBar;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.AssertException;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.VFSConstants;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;

/**
 * Description:<br>
 * File Upload command class
 * <P>
 * Initial Date: 09.06.2006 <br>
 * 
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class CmdUpload extends BasicController implements FolderCommand {
	public static final Event FOLDERCOMMAND_CANCELED = new Event("fc_canceled");

	private int status = FolderCommandStatus.STATUS_SUCCESS;

	private VelocityContainer mainVC;
	private VFSContainer currentContainer, inheritingContainer;
	private VFSSecurityCallback secCallback;

	private FolderComponent folderComponent;
	private ProgressBar ubar;
	private String uploadFileName;
	private VFSLeaf vfsNewFile;
	private long quotaKB;
	private int uploadLimitKB;
	private boolean overwritten = false;
	private FileUploadController fileUploadCtr;
	private boolean cancelResetsForm;
	private boolean showMetadata = false;
	private boolean showCancel = true; // default is to show cancel button

	public CmdUpload(UserRequest ureq, WindowControl wControl, boolean showMetadata, boolean showCancel) {
		super(ureq, wControl, Util.createPackageTranslator(FileElementImpl.class, ureq.getLocale()));
		this.showMetadata = showMetadata;
		this.showCancel = showCancel;
	}

	protected CmdUpload(UserRequest ureq, WindowControl wControl, boolean showMetadata) {
		super(ureq, wControl);
		this.showMetadata = showMetadata;
	}

	@Override
	public Controller execute(FolderComponent fc, UserRequest ureq, WindowControl windowControl, Translator trans) {
		return execute(fc, ureq, windowControl, trans, false);
	}

	public Controller execute(FolderComponent fc, UserRequest ureq, WindowControl windowControl, Translator trans, boolean cancelResetsForm) {
		this.folderComponent = fc;
		this.cancelResetsForm = cancelResetsForm;

		setTranslator(trans);
		currentContainer = folderComponent.getCurrentContainer();
		if (currentContainer.canWrite() != VFSConstants.YES) throw new AssertException("Cannot write to selected folder.");
		// mainVC is the main view

		mainVC = createVelocityContainer("upload");
		// Add progress bar
		ubar = new ProgressBar("ubar");
		ubar.setWidth(200);
		ubar.setUnitLabel("MB");
		mainVC.put(ubar.getComponentName(), ubar);

		// Calculate quota and limits
		long actualUsage = 0;
		quotaKB = Quota.UNLIMITED;
		uploadLimitKB = Quota.UNLIMITED;

		inheritingContainer = VFSManager.findInheritingSecurityCallbackContainer(currentContainer);
		if (inheritingContainer != null) {
			secCallback = inheritingContainer.getLocalSecurityCallback();
			actualUsage = VFSManager.getUsageKB(inheritingContainer);
			ubar.setActual(actualUsage / 1024);
			if (inheritingContainer.getLocalSecurityCallback().getQuota() != null) {
				quotaKB = secCallback.getQuota().getQuotaKB().longValue();
				uploadLimitKB = (int) secCallback.getQuota().getUlLimitKB().longValue();
			}
		}
		// set wether we have a quota on this folder
		if (quotaKB == Quota.UNLIMITED) ubar.setIsNoMax(true);
		else ubar.setMax(quotaKB / 1024);
		// set default ulLimit if none is defined...
		if (uploadLimitKB == Quota.UNLIMITED) uploadLimitKB = (int) QuotaManager.getInstance().getDefaultQuotaDependingOnRole(ureq.getIdentity()).getUlLimitKB()
				.longValue();

		// Add file upload form
		int remainingQuotaKB = (int) quotaKB - (int) actualUsage;
		if (quotaKB == Quota.UNLIMITED) remainingQuotaKB = (int) quotaKB;
		else if (quotaKB - actualUsage < 0) remainingQuotaKB = 0;
		else remainingQuotaKB = (int) quotaKB - (int) actualUsage;
		removeAsListenerAndDispose(fileUploadCtr);

		// if folder full show error msg
		if (remainingQuotaKB == 0) {
			String supportAddr = WebappHelper.getMailConfig("mailSupport");
			String msg = translate("QuotaExceededSupport", new String[] { supportAddr });
			getWindowControl().setError(msg);
			return null;
		} else {
			fileUploadCtr = new FileUploadController(getWindowControl(), currentContainer, ureq, uploadLimitKB, remainingQuotaKB, null, true, showMetadata, true,
					showCancel);
			listenTo(fileUploadCtr);
			mainVC.put("fileUploadCtr", fileUploadCtr.getInitialComponent());
			mainVC.contextPut("showFieldset", Boolean.TRUE);

			putInitialPanel(mainVC);
		}
		return this;
	}

	public void refreshActualFolderUsage() {
		long actualUsage = 0;
		quotaKB = Quota.UNLIMITED;
		uploadLimitKB = Quota.UNLIMITED;

		inheritingContainer = VFSManager.findInheritingSecurityCallbackContainer(currentContainer);
		if (inheritingContainer != null) {
			secCallback = inheritingContainer.getLocalSecurityCallback();
			actualUsage = VFSManager.getUsageKB(inheritingContainer);
			quotaKB = secCallback.getQuota().getQuotaKB().longValue();
			uploadLimitKB = (int) secCallback.getQuota().getUlLimitKB().longValue();
			ubar.setActual(actualUsage / 1024);
			fileUploadCtr.setMaxUploadSizeKB(uploadLimitKB);
		}
	}

	/**
	 * Call this to remove the fieldset
	 */
	public void hideFieldset() {
		if (mainVC == null) { throw new AssertException("Programming error - execute must be called before calling hideFieldset()"); }
		mainVC.contextPut("showFieldset", Boolean.FALSE);
		if (fileUploadCtr != null) {
			fileUploadCtr.hideTitleAndFieldset();
		}
	}

	/**
	 * @return
	 */
	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		// no events to catch
	}

	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == fileUploadCtr) {
			// catch upload event
			if (event instanceof FolderEvent && event.getCommand().equals(FolderEvent.UPLOAD_EVENT)) {
				FolderEvent folderEvent = (FolderEvent) event;
				// Get file from temp folder location
				uploadFileName = folderEvent.getFilename();
				vfsNewFile = (VFSLeaf) currentContainer.resolve(uploadFileName);
				overwritten = fileUploadCtr.isExistingFileOverwritten();
				if (vfsNewFile != null) {
					notifyFinished(ureq);
				} else {
					showError("file.element.error.general");
				}
			} else if (event.equals(Event.CANCELLED_EVENT)) {
				if (cancelResetsForm) {
					fileUploadCtr.reset();
				} else {
					status = FolderCommandStatus.STATUS_CANCELED;
					fireEvent(ureq, FOLDERCOMMAND_FINISHED);
				}
			}
		}
	}

	private void notifyFinished(UserRequest ureq) {
		// After upload, notify the subscribers
		if (secCallback != null) {
			SubscriptionContext subsContext = secCallback.getSubscriptionContext();
			if (subsContext != null) {
				NotificationsManager.getInstance().markPublisherNews(subsContext, ureq.getIdentity());
			}
		}
		// Notify everybody
		fireEvent(ureq, FOLDERCOMMAND_FINISHED);
	}

	/**
	 * Get the filename of the uploaded file or NULL if nothing uploaded
	 * 
	 * @return
	 */
	public String getFileName() {
		return this.uploadFileName;
	}

	public Boolean fileWasOverwritten() {
		return this.overwritten;
	}

	@Override
	protected void doDispose() {
		// nothing to dispose
	}

	@Override
	public boolean runsModal() {
		return false;
	}

}
