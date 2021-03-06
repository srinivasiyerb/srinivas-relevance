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

package org.olat.ims.qti.navigator;

import java.io.Serializable;

import org.olat.ims.qti.QTIConstants;
import org.olat.ims.qti.container.AssessmentContext;
import org.olat.ims.qti.container.ItemContext;
import org.olat.ims.qti.container.ItemsInput;
import org.olat.ims.qti.container.SectionContext;
import org.olat.ims.qti.process.AssessmentInstance;

/**
 * Navigator used for the case: <br>
 * 1. Navigation (via Menu) visible and enabled, one section per page <br>
 * 
 * @author Felix Jost
 */
public class MenuSectionNavigator extends DefaultNavigator implements Navigator, Serializable {

	/**
	 * @param assessmentContext
	 */
	public MenuSectionNavigator(final AssessmentInstance assessmentInstance) {
		super(assessmentInstance);
	}

	@Override
	public void startAssessment() {
		getInfo().setStatus(QTIConstants.ASSESSMENT_RUNNING);
		getInfo().setMessage(QTIConstants.MESSAGE_ASSESSMENT_INFODEMANDED);
		getInfo().setRenderItems(false);
		getAssessmentInstance().start();
		getAssessmentInstance().persist();
	}

	/**
	 * @see org.olat.qti.process.Navigator#submitItems(org.olat.qti.container.ItemsInput)
	 */
	@Override
	public void submitItems(final ItemsInput curitsinp) {
		clearInfo();
		// the whole section is submitted
		final int st = submitMultipleItems(curitsinp);
		getAssessmentContext().getCurrentSectionContext().sectionWasSubmitted();
		if (st != QTIConstants.SECTION_SUBMITTED) {
			// we could not submit the section (out of time is the only reason), display a error msg above the next section or assessment-finished-text
			getInfo().setError(st);
			getInfo().setRenderItems(false);
		} else {
			// eval the whole assessment here each time (so after a submitted item, one sees overall progress)
			// getAssessmentContext().eval();
			getInfo().setMessage(QTIConstants.MESSAGE_SECTION_SUBMITTED);
			getInfo().setRenderItems(false);
		}
		getAssessmentInstance().persist();
	}

	/**
	 * @see org.olat.qti.process.Navigator#goToItem(int, int)
	 */
	@Override
	public void goToItem(final int sectionPos, final int itemPos) {
		throw new RuntimeException("can only go to sections");
	}

	/**
	 * @see org.olat.qti.process.Navigator#goToSection(int)
	 */
	@Override
	public void goToSection(final int sectionPos) {
		clearInfo();
		final AssessmentContext ac = getAssessmentContext();
		SectionContext sc = ac.getSectionContext(sectionPos);
		// check if section still open
		if (!ac.isOpen()) {
			getInfo().setError(QTIConstants.ERROR_ASSESSMENT_OUTOFTIME);
			getInfo().setRenderItems(false);
		} else if (!sc.isOpen()) {
			getInfo().setError(QTIConstants.ERROR_SECTION_OUTOFTIME);
			getInfo().setRenderItems(false);
		} else {
			getInfo().setStatus(QTIConstants.ASSESSMENT_RUNNING);
			getInfo().setMessage(QTIConstants.MESSAGE_SECTION_INFODEMANDED); // show section info (title and description)
			getInfo().setRenderItems(true);
			ac.setCurrentSectionContextPos(sectionPos);
			sc = ac.getCurrentSectionContext();
			startSection(sc);
		}
		getAssessmentInstance().persist();
	}

	private void startSection(final SectionContext sc) {
		sc.start();
		for (int i = 0; i < sc.getItemContextCount(); i++) {
			final ItemContext itc = sc.getItemContext(i);
			itc.start();
		}
	}

}
