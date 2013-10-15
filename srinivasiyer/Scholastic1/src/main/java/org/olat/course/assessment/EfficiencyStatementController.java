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

package org.olat.course.assessment;

import java.util.List;

import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.MainLayoutBasicController;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.id.Roles;
import org.olat.core.util.StringHelper;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * Description:<br>
 * Displays the users efficiency statement
 * <P>
 * Initial Date: 11.08.2005 <br>
 * 
 * @author gnaegi
 */
public class EfficiencyStatementController extends MainLayoutBasicController {

	private VelocityContainer userDataVC;
	private static final String usageIdentifyer = EfficiencyStatementController.class.getCanonicalName();

	/**
	 * Constructor
	 * 
	 * @param wControl
	 * @param ureq
	 * @param courseId
	 */
	public EfficiencyStatementController(final WindowControl wControl, final UserRequest ureq, final Long courseRepoEntryKey) {
		this(wControl, ureq, EfficiencyStatementManager.getInstance().getUserEfficiencyStatement(courseRepoEntryKey, ureq.getIdentity()));
	}

	public EfficiencyStatementController(final WindowControl wControl, final UserRequest ureq, final EfficiencyStatement efficiencyStatement) {
		super(ureq, wControl);

		// either the efficiency statement or the error message, that no data is available goes to the content area
		Component content = null;

		if (efficiencyStatement != null) {
			// extract efficiency statement data
			// fallback translation for user properties
			setTranslator(UserManager.getInstance().getPropertyHandlerTranslator(getTranslator()));
			userDataVC = createVelocityContainer("efficiencystatement");
			userDataVC.contextPut("courseTitle", efficiencyStatement.getCourseTitle() + " (" + efficiencyStatement.getCourseRepoEntryKey().toString() + ")");
			userDataVC.contextPut("user", ureq.getIdentity().getUser());
			userDataVC.contextPut("username", ureq.getIdentity().getName());
			userDataVC.contextPut("date", StringHelper.formatLocaleDateTime(efficiencyStatement.getLastUpdated(), ureq.getLocale()));

			final Roles roles = ureq.getUserSession().getRoles();
			final boolean isAdministrativeUser = (roles.isAuthor() || roles.isGroupManager() || roles.isUserManager() || roles.isOLATAdmin());
			final List<UserPropertyHandler> userPropertyHandlers = UserManager.getInstance().getUserPropertyHandlersFor(usageIdentifyer, isAdministrativeUser);
			userDataVC.contextPut("userPropertyHandlers", userPropertyHandlers);

			final Controller identityAssessmentCtr = new IdentityAssessmentOverviewController(ureq, wControl, efficiencyStatement.getAssessmentNodes());
			listenTo(identityAssessmentCtr);// dispose it when this one is disposed
			userDataVC.put("assessmentOverviewTable", identityAssessmentCtr.getInitialComponent());

			content = userDataVC;
		} else {
			// message, that no data is available. This may happen in the case the "open efficiency" link is available, while in the meantime an author
			// disabled the efficiency statement.
			final String text = translate("efficiencystatement.nodata");
			final Controller messageCtr = MessageUIFactory.createErrorMessage(ureq, wControl, null, text);
			listenTo(messageCtr);// gets disposed as this controller gets disposed.
			content = messageCtr.getInitialComponent();
		}
		// Content goes to a 3 cols layout without left and right column
		final LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), null, null, content, null);
		listenTo(layoutCtr);
		putInitialPanel(layoutCtr.getInitialComponent());

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	@SuppressWarnings("unused")
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// nothing to catch
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

}
