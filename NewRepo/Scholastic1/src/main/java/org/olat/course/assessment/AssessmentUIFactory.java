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

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.Activateable;
import org.olat.core.id.OLATResourceable;
import org.olat.course.ICourse;

/**
 * Description:<br>
 * TODO: patrickb Class Description for AssessmentUIFactory
 * <P>
 * Initial Date: 29.06.2010 <br>
 * 
 * @author patrickb
 */
public class AssessmentUIFactory {

	private static AssessmentControllerCreator controllerCreator = null;

	AssessmentUIFactory(final AssessmentControllerCreator specificControllerCreator) {
		AssessmentUIFactory.controllerCreator = specificControllerCreator;
	}

	public static Activateable createAssessmentMainController(final UserRequest ureq, final WindowControl wControl, final OLATResourceable ores,
			final IAssessmentCallback assessmentCallback) {
		return controllerCreator.createAssessmentMainController(ureq, wControl, ores, assessmentCallback);
	}

	public static Controller createQTIArchiveWizardController(final boolean dummyMode, final UserRequest ureq, final List nodesTableObjectArrayList,
			final ICourse course, final WindowControl windowControl) {
		return controllerCreator.createQTIArchiveWizardController(dummyMode, ureq, nodesTableObjectArrayList, course, windowControl);
	}

	/**
	 * @param ureq
	 * @param wControl
	 * @param course
	 * @return null if no contextualSubscriptionController is available
	 */
	public static Controller createContextualSubscriptionController(final UserRequest ureq, final WindowControl wControl, final ICourse course) {
		return controllerCreator.createContextualSubscriptionController(ureq, wControl, course);
	}

}
