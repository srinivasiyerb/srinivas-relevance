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

package org.olat.course.nodes.projectbroker;

import java.io.File;

import org.olat.admin.quota.QuotaConstants;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.callbacks.ReadOnlyCallback;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.ta.DropboxController;
import org.olat.course.nodes.ta.DropboxScoringViewController;
import org.olat.course.nodes.ta.ReturnboxController;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * @author Christian Guretzki
 */

public class ProjectBrokerDropboxScoringViewController extends DropboxScoringViewController {

	private final Project project;

	/**
	 * Scoring view of the dropbox.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param node
	 * @param userCourseEnv
	 */
	public ProjectBrokerDropboxScoringViewController(final Project project, final UserRequest ureq, final WindowControl wControl, final CourseNode node,
			final UserCourseEnvironment userCourseEnv) {
		super(ureq, wControl, node, userCourseEnv, false);
		this.project = project;
		this.setVelocityRoot(Util.getPackageVelocityRoot(DropboxScoringViewController.class));
		final Translator fallbackTranslator = new PackageTranslator(Util.getPackageName(this.getClass()), ureq.getLocale());
		final Translator myTranslator = new PackageTranslator(Util.getPackageName(DropboxScoringViewController.class), ureq.getLocale(), fallbackTranslator);
		this.setTranslator(myTranslator);
		init(ureq);
	}

	@Override
	protected String getDropboxFilePath(final String assesseeName) {
		return DropboxController.getDropboxPathRelToFolderRoot(userCourseEnv.getCourseEnvironment(), node) + File.separator + project.getKey();
	}

	@Override
	protected String getReturnboxFilePath(final String assesseeName) {
		return ReturnboxController.getReturnboxPathRelToFolderRoot(userCourseEnv.getCourseEnvironment(), node) + File.separator + project.getKey();
	}

	@Override
	protected String getDropboxRootFolderName(final String assesseeName) {
		return translate("scoring.dropbox.rootfolder.name");
	}

	@Override
	protected String getReturnboxRootFolderName(final String assesseeName) {
		return translate("scoring.returnbox.rootfolder.name");
	}

	@Override
	protected VFSSecurityCallback getDropboxVfsSecurityCallback() {
		return new ReadOnlyCallback();
	}

	@Override
	protected VFSSecurityCallback getReturnboxVfsSecurityCallback(final String returnboxRelPath, final UserCourseEnvironment userCourseEnv2, final CourseNode node2) {
		return new ReturnboxFullAccessCallback(returnboxRelPath, userCourseEnv2, node2);
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
		return false;
	}

	/**
	 * @see org.olat.core.util.vfs.callbacks.VFSSecurityCallback#canCopy()
	 */
	@Override
	public boolean canCopy() {
		return false;
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
