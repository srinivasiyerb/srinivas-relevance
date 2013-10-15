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

package org.olat.admin.user;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.util.resource.OresHelper;
import org.olat.login.auth.OLATAuthManager;

/**
 * Initial Date: Jul 29, 2003
 * 
 * @author Felix Jost, Florian Gnaegi
 * 
 *         <pre>
 * Comment:  
 * Subworkflow that presents a form to change the OLAT local password of the given user.
 * 
 * </pre>
 */
public class UserChangePasswordController extends BasicController {

	private final ChangeUserPasswordForm chPwdForm;
	private final SendTokenToUserForm tokenForm;
	private final VelocityContainer mainVC;
	private final Identity user;

	/**
	 * @param ureq
	 * @param wControl
	 * @param changeableUser
	 */
	public UserChangePasswordController(final UserRequest ureq, final WindowControl wControl, final Identity changeableUser) {
		super(ureq, wControl);

		final BaseSecurity mgr = BaseSecurityManager.getInstance();
		if (!mgr.isIdentityPermittedOnResourceable(ureq.getIdentity(), Constants.PERMISSION_ACCESS, OresHelper.lookupType(this.getClass()))) { throw new OLATSecurityException(
				"Insufficient permissions to access UserChangePasswordController"); }

		user = changeableUser;
		chPwdForm = new ChangeUserPasswordForm(ureq, wControl, user);
		listenTo(chPwdForm);
		tokenForm = new SendTokenToUserForm(ureq, wControl, user);
		listenTo(tokenForm);

		mainVC = createVelocityContainer("pwd");
		mainVC.put("chPwdForm", chPwdForm.getInitialComponent());
		mainVC.put("tokenForm", tokenForm.getInitialComponent());

		putInitialPanel(mainVC);
	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == chPwdForm && event.equals(Event.DONE_EVENT)) {
			if (OLATAuthManager.changePassword(ureq.getIdentity(), user, chPwdForm.getNewPassword())) {
				showInfo("changeuserpwd.successful");
				logAudit("user password changed successfully of " + user.getName(), this.getClass().getName());
			} else {
				showError("changeuserpwd.failed");
			}
			fireEvent(ureq, Event.DONE_EVENT);
		}
	}

	@Override
	protected void doDispose() {
		//
	}
}
