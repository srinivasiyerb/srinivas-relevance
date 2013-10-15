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
import org.olat.ims.qti.container.Output;
import org.olat.ims.qti.container.SectionContext;
import org.olat.ims.qti.process.AssessmentInstance;

/**
 * Navigator used for the cases: <br>
 * 1. Navigation (via Menu) not visible, one section per page <br>
 * 2. Navigation visible but disabled, one section per page <br>
 * 
 * @author Felix Jost
 */
public class SequentialSectionNavigator extends DefaultNavigator implements Navigator, Serializable {

	/**
	 * 
	 */
	public void Navigator() {
		//
	}

	/**
	 * @param assessmentInstance
	 */
	public SequentialSectionNavigator(final AssessmentInstance assessmentInstance) {
		super(assessmentInstance);
	}

	@Override
	public void startAssessment() {
		getAssessmentContext().setCurrentSectionContextPos(0);
		getAssessmentInstance().start();
		startSection(getAssessmentContext().getCurrentSectionContext());
		getInfo().setStatus(QTIConstants.ASSESSMENT_RUNNING);
		if (!getAssessmentContext().isOpen()) {
			getInfo().setError(QTIConstants.ERROR_ASSESSMENT_OUTOFTIME);
			getInfo().setRenderItems(false);
		} else {
			getInfo().setMessage(QTIConstants.MESSAGE_ASSESSMENT_INFODEMANDED); // show test title and description first
			getInfo().setRenderItems(false); // do not show items as first step
		}
		getAssessmentInstance().persist();
	}

	/**
	 * @see org.olat.qti.process.Navigator#submitItem(org.olat.qti.process.ItemsInput)
	 */
	@Override
	public void submitItems(final ItemsInput curitsinp) {
		clearInfo();
		final int st = submitMultipleItems(curitsinp);
		final SectionContext sc = getAssessmentContext().getCurrentSectionContext();
		if (st != QTIConstants.SECTION_SUBMITTED) {
			// we could not submit the section (out of time is the only reason),
			// display a error msg above the next section or assessment-finished-text
			getInfo().setError(st);
			getInfo().setRenderItems(true);
		} else { // section was successfully submitted
			sc.sectionWasSubmitted(); // increase times answered of section
			sc.eval(); // calculate any section feedback
			if (sc.isFeedbackavailable()) {
				final Output outp = sc.getOutput();
				getInfo().setCurrentOutput(outp);
				getInfo().setFeedback(true);
			}
			getInfo().setMessage(QTIConstants.MESSAGE_SECTION_SUBMITTED);
			getInfo().setRenderItems(true);
		}

		// find next section
		final AssessmentContext ac = getAssessmentContext();
		int secPos = ac.getCurrentSectionContextPos();
		final int secPosMax = ac.getSectionContextCount() - 1;
		if (!ac.isOpen()) {
			getInfo().setError(QTIConstants.ERROR_ASSESSMENT_OUTOFTIME);
			getInfo().setRenderItems(false);
			submitAssessment();
		} else if (secPos == secPosMax) {
			submitAssessment();
		} else {
			while (secPos < secPosMax) { // there are still further section(s)
				secPos++;
				if (ac.getSectionContext(secPos).getItemContextCount() != 0) {
					break;
				}
			}

			if (secPos == secPosMax && ac.getSectionContext(secPos).getItemContextCount() == 0) {
				// reached last section but section is empty -> finish assessment
				submitAssessment();
			} else {
				ac.setCurrentSectionContextPos(secPos);
				startSection(ac.getCurrentSectionContext());
			}
		}
		getAssessmentInstance().persist();
	}

	/**
	 * @see org.olat.qti.process.Navigator#goToItem(int, int)
	 */
	@Override
	public void goToItem(final int sectionPos, final int itemPos) {
		throw new RuntimeException("not allowed to go to sectionPos " + sectionPos + ", itempos " + itemPos);
	}

	/**
	 * @see org.olat.qti.process.Navigator#goToSection(int)
	 */
	@Override
	public void goToSection(final int sectionPos) {
		final AssessmentContext ac = getAssessmentContext();
		ac.setCurrentSectionContextPos(sectionPos);
		getInfo().setMessage(QTIConstants.MESSAGE_SECTION_INFODEMANDED); // show the section title and description
		getInfo().setRenderItems(true); // show the items as well
	}

	private void startSection(final SectionContext sc) {
		sc.start();
		for (int i = 0; i < sc.getItemContextCount(); i++) {
			final ItemContext itc = sc.getItemContext(i);
			itc.start();
		}
	}

}