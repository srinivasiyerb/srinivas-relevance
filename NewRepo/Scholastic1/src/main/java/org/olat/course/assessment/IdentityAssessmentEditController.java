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
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.Structure;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * Description:<BR>
 * This controller provides the functionality to view a users course assessment and to edit all editable fields. It uses the IdentityAssessmentOverviewController to
 * generate the users assessment overview page and the AssessmentEditController to manipulate the assessment data of a specific course node. <BR>
 * This controller fires a Event.CANCELLED_EVENT when the view is finished without changing any assessment data, otherwhise the Event.CHANGED_EVENT is fired.
 * <P>
 * Initial Date: Oct 28, 2004
 * 
 * @author gnaegi
 */
public class IdentityAssessmentEditController extends BasicController {

	private final boolean mayEdit;
	private VelocityContainer identityOverviewVC;
	private final Panel main;
	private AssessmentEditController assessmentEditCtr;
	private IdentityAssessmentOverviewController assessmentOverviewCtr;
	private final UserCourseEnvironment assessedUserCourseEnvironment;
	private Link backLink;
	private final OLATResourceable ores;

	/**
	 * Constructor for the identity assessment overview controller
	 * 
	 * @param wControl The window control
	 * @param ureq The user request
	 * @param assessedUserCourseEnvironment The assessed identitys user course environment
	 * @param course
	 * @param mayEdit true: user may edit the assessment, false: readonly view (user view)
	 */
	public IdentityAssessmentEditController(final WindowControl wControl, final UserRequest ureq, final UserCourseEnvironment assessedUserCourseEnvironment,
			final OLATResourceable ores, final boolean mayEdit) {

		super(ureq, wControl);
		this.mayEdit = mayEdit;
		this.main = new Panel("main");
		this.assessedUserCourseEnvironment = assessedUserCourseEnvironment;
		this.ores = ores;
		doIdentityAssessmentOverview(ureq, true);
		putInitialPanel(main);

		final BusinessControl bc = getWindowControl().getBusinessControl();
		final ContextEntry ce = bc.popLauncherContextEntry();
		if (ce != null) {
			final OLATResourceable oresNode = ce.getOLATResourceable();
			if (OresHelper.isOfType(oresNode, CourseNode.class)) {
				final Long courseNodeId = oresNode.getResourceableId();
				final Structure runStructure = assessedUserCourseEnvironment.getCourseEnvironment().getRunStructure();
				final CourseNode courseNode = runStructure.getNode(courseNodeId.toString());
				if (courseNode instanceof AssessableCourseNode) {
					doEditNodeAssessment(ureq, (AssessableCourseNode) courseNode);
				}
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == backLink) {
			fireEvent(ureq, Event.CANCELLED_EVENT);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == assessmentOverviewCtr) {
			if (event.equals(IdentityAssessmentOverviewController.EVENT_NODE_SELECTED)) {
				final AssessableCourseNode courseNode = assessmentOverviewCtr.getSelectedCourseNode();
				doEditNodeAssessment(ureq, courseNode);
			}
		} else if (source == assessmentEditCtr) {
			if (event.equals(Event.CANCELLED_EVENT)) {
				doIdentityAssessmentOverview(ureq, false);
			} else if (event.equals(Event.CHANGED_EVENT)) {
				doIdentityAssessmentOverview(ureq, true);
			}
		}
	}

	private void doIdentityAssessmentOverview(final UserRequest ureq, final boolean initTable) {
		if (identityOverviewVC == null) {
			identityOverviewVC = createVelocityContainer("identityoverview");
			backLink = LinkFactory.createLinkBack(identityOverviewVC, this);

			final Identity assessedIdentity = assessedUserCourseEnvironment.getIdentityEnvironment().getIdentity();
			identityOverviewVC.contextPut("user", assessedIdentity.getUser());
		}
		if (initTable) {
			assessmentOverviewCtr = new IdentityAssessmentOverviewController(ureq, getWindowControl(), assessedUserCourseEnvironment, mayEdit, false, true);
			listenTo(assessmentOverviewCtr);
			identityOverviewVC.put("assessmentOverviewTable", assessmentOverviewCtr.getInitialComponent());
		}
		main.setContent(identityOverviewVC);
	}

	private void doEditNodeAssessment(final UserRequest ureq, final AssessableCourseNode courseNode) {
		if (mayEdit) {
			final ICourse course = CourseFactory.loadCourse(ores);
			final AssessedIdentityWrapper assessedIdentityWrapper = AssessmentHelper.wrapIdentity(assessedUserCourseEnvironment, courseNode);
			assessmentEditCtr = new AssessmentEditController(ureq, getWindowControl(), course, courseNode, assessedIdentityWrapper);
			listenTo(assessmentEditCtr);
			main.setContent(assessmentEditCtr.getInitialComponent());
		} else {
			throw new OLATSecurityException("doEditNodeAssessment() called but controller configured with mayEdit=false");
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// auto dispose by basic controller
	}

}
