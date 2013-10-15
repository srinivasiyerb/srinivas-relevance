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

package org.olat.admin.quota;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;

/**
 * Description:<BR>
 * Generic editor controller for quotas. Can be constructed from a quota or a folder path. When finished the controller fires the following events:<BR>
 * Event.CANCELLED_EVENT Event.CHANGED_EVENT
 * <P>
 * Initial Date: Dec 22, 2004
 * 
 * @author gnaegi
 */
public class GenericQuotaEditController extends BasicController {

	private VelocityContainer myContent;
	private QuotaForm quotaForm;
	private final boolean modalMode;

	private Quota currentQuota;
	private Link addQuotaButton;
	private Link delQuotaButton;
	private Link cancelButton;

	/**
	 * Constructor for the generic quota edit controller used to change a quota anywhere in the system not using the generic quota management. Instead of using a quota
	 * the constructor takes the folder path for which the quota will be changed.
	 * <p>
	 * To create an instance of this controller, use QuotaManager's factory method
	 * 
	 * @param ureq
	 * @param wControl
	 * @param quotaPath The path for which the quota should be edited
	 * @param modalMode true: window will push to fullscreen and pop itself when finished. false: normal controller mode, get initial component using
	 *            getInitialComponent()
	 */
	GenericQuotaEditController(final UserRequest ureq, final WindowControl wControl, final String relPath, final boolean modalMode) {
		super(ureq, wControl);
		this.modalMode = modalMode;

		// check if quota foqf.cannot.del.defaultr this path already exists
		final QuotaManager qm = QuotaManager.getInstance();
		this.currentQuota = qm.getCustomQuota(relPath);
		// init velocity context
		initMyContent(ureq);
		if (currentQuota == null) {
			this.currentQuota = QuotaManager.getInstance().createQuota(relPath, null, null);
			myContent.contextPut("editQuota", Boolean.FALSE);
		} else {
			initQuotaForm(ureq, currentQuota);
		}
		putInitialPanel(myContent);
	}

	/**
	 * Constructor for the generic quota edit controller used when an existing quota should be edited, as done in the admin quotamanagement
	 * 
	 * @param ureq
	 * @param wControl
	 * @param quota The existing quota or null. If null, a new quota is generated
	 */
	public GenericQuotaEditController(final UserRequest ureq, final WindowControl wControl, final Quota quota) {
		super(ureq, wControl);
		this.modalMode = false;

		initMyContent(ureq);

		// start with neq quota if quota is empty
		if (quota == null) {
			this.currentQuota = QuotaManager.getInstance().createQuota(null, null, null);
			myContent.contextPut("isEmptyQuota", true);
		} else {
			this.currentQuota = quota;
		}
		initQuotaForm(ureq, currentQuota);

		putInitialPanel(myContent);
	}

	private void initMyContent(final UserRequest ureq) {
		final BaseSecurity mgr = BaseSecurityManager.getInstance();
		if (!mgr.isIdentityPermittedOnResourceable(ureq.getIdentity(), Constants.PERMISSION_ACCESS, OresHelper.lookupType(this.getClass()))) { throw new OLATSecurityException(
				"Insufficient permissions to access QuotaController"); }

		myContent = createVelocityContainer("edit");
		myContent.contextPut("modalMode", Boolean.valueOf(modalMode));
		addQuotaButton = LinkFactory.createButtonSmall("qf.new", myContent, this);
		delQuotaButton = LinkFactory.createButtonSmall("qf.del", myContent, this);
		cancelButton = LinkFactory.createButtonSmall("cancel", myContent, this);

		final QuotaManager qm = QuotaManager.getInstance();
		// TODO loop over QuotaManager.getDefaultQuotaIdentifyers instead
		myContent.contextPut("users", qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_USERS));
		myContent.contextPut("powerusers", qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_POWER));
		myContent.contextPut("groups", qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_GROUPS));
		myContent.contextPut("repository", qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_REPO));
		myContent.contextPut("coursefolder", qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_COURSE));
		myContent.contextPut("nodefolder", qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_NODES));
	}

	private void initQuotaForm(final UserRequest ureq, final Quota quota) {
		if (quotaForm != null) {
			removeAsListenerAndDispose(quotaForm);
		}
		quotaForm = new QuotaForm(ureq, getWindowControl(), quota);
		listenTo(quotaForm);
		myContent.put("quotaform", quotaForm.getInitialComponent());
		myContent.contextPut("editQuota", Boolean.TRUE);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		initQuotaForm(ureq, currentQuota);
		if (source == delQuotaButton) {
			final boolean deleted = QuotaManager.getInstance().deleteCustomQuota(currentQuota);
			if (deleted) {
				myContent.remove(quotaForm.getInitialComponent());
				myContent.contextPut("editQuota", Boolean.FALSE);
				showInfo("qf.deleted", currentQuota.getPath());
				fireEvent(ureq, Event.CHANGED_EVENT);
			} else {
				showError("qf.cannot.del.default");
			}
		} else if (source == cancelButton) {
			fireEvent(ureq, Event.CANCELLED_EVENT);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == quotaForm) {
			if (event == Event.DONE_EVENT) {
				final QuotaManager qm = QuotaManager.getInstance();
				currentQuota = QuotaManager.getInstance().createQuota(quotaForm.getPath(), new Long(quotaForm.getQuotaKB()), new Long(quotaForm.getULLimit()));
				qm.setCustomQuotaKB(currentQuota);
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
		}
	}

	/**
	 * @return Quota the edited quota
	 */
	public Quota getQuota() {
		if (currentQuota == null) { throw new AssertException("getQuota called but currentQuota is null"); }
		return currentQuota;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}
}
