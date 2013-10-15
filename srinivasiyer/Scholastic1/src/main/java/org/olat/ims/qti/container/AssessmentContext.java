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

package org.olat.ims.qti.container;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.olat.ims.qti.container.qtielements.AssessFeedback;
import org.olat.ims.qti.container.qtielements.Objectives;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.ims.qti.process.QTIHelper;
import org.olat.ims.qti.process.elements.ScoreBooleanEvaluable;

/**
 * contains the sections of the assignment. assumption: each toplevel-section of an assignment means one screen <!ELEMENT assessment (qticomment? , duration? ,
 * qtimetadata* , objectives* , assessmentcontrol* , rubric* , presentation_material? , outcomes_processing* , assessproc_extension? , assessfeedback* ,
 * selection_ordering? , reference? , (sectionref | section)+)> <!ATTLIST assessment %I_Ident; %I_Title; xml:lang CDATA #IMPLIED >
 * 
 * @author Felix Jost
 */
public class AssessmentContext implements Serializable {
	// readonly ref!: the ref to the el_assessment; transient since it we don't
	// want to serialize it (too long) and can reattach it later
	// private transient Element el_assessment;
	private String ident;
	private String title;
	private AssessmentInstance assessInstance;

	private Element el_assessment;
	private Objectives objectives;
	private Switches switches = null;
	private Output output;

	// the sectioncontexts of this assessment
	private List sectionContexts;

	// the current section beeing chosen by the user or forced by the system
	private int currentSectionContextPos;
	private long timeOfStart;
	// server time at the time of the start of the assessment
	private long timeOfStop;
	// server time at the time of the start of the assessment
	private long durationLimit; //
	private float cutvalue = -1.0f;
	private String scoremodel;
	private boolean feedbacktesting;
	private boolean feedbackavailable;

	/**
	 * default constructor needed for persistence
	 */
	public AssessmentContext() {
		//
	}

	/**
	 * 
	 */
	public void init() {
		currentSectionContextPos = -1;
		feedbacktesting = false;
		feedbackavailable = false;
		timeOfStart = -1; // not started yet
		timeOfStop = -1; // not stopped yet
	}

	/**
	 * Method setUp.
	 * 
	 * @param assessInstance
	 */
	public void setUp(final AssessmentInstance assessInstance) {
		this.assessInstance = assessInstance;
		init();

		final Document el_questestinterop = assessInstance.getResolver().getQTIDocument();
		el_assessment = (Element) el_questestinterop.selectSingleNode("questestinterop/assessment");

		ident = el_assessment.attributeValue("ident");
		title = el_assessment.attributeValue("title");
		final Element dur = (Element) el_assessment.selectSingleNode("duration");

		if (dur == null) {
			durationLimit = -1; // no limit
		} else {
			final String sdur = dur.getText();
			durationLimit = QTIHelper.parseISODuration(sdur);
			if (durationLimit == 0) {
				durationLimit = -1; // Assesst Designer fix
			}
		}

		// get objectives
		final Element el_objectives = (Element) el_assessment.selectSingleNode("objectives");
		if (el_objectives != null) {
			objectives = new Objectives(el_objectives);
		}

		// set feedback, hint, and solutions switches
		// <!ENTITY % I_FeedbackSwitch " feedbackswitch (Yes | No ) 'Yes'">
		// <!ENTITY % I_HintSwitch " hintswitch (Yes | No ) 'Yes'">
		// <!ENTITY % I_SolutionSwitch " solutionswitch (Yes | No ) 'Yes'">

		// <!ELEMENT assessment (qticomment? , duration? , qtimetadata* ,
		// objectives* , assessmentcontrol* , rubric* , presentation_material? ,
		// outcomes_processing* , assessproc_extension? , assessfeedback* ,
		// selection_ordering? , reference? , (sectionref | section)+)>
		// <!ELEMENT assessmentcontrol (qticomment?)>
		final Element el_control = (Element) el_assessment.selectSingleNode("assessmentcontrol");
		if (el_control != null) {
			final String feedbackswitch = el_control.attributeValue("feedbackswitch");
			final String hintswitch = el_control.attributeValue("hintswitch");
			final String solutionswitch = el_control.attributeValue("solutionswitch");
			final boolean feedback = (feedbackswitch == null) ? true : feedbackswitch.equals("Yes");
			final boolean hints = (hintswitch == null) ? true : hintswitch.equals("Yes");
			final boolean solutions = (solutionswitch == null) ? true : solutionswitch.equals("Yes");
			switches = new Switches(feedback, hints, solutions);
		}

		// scoring model and outcomes processing
		final Element el_outpro = (Element) el_assessment.selectSingleNode("outcomes_processing");
		if (el_outpro != null) {
			// get the scoring model: we need it later for calculating the score
			// <!ENTITY % I_ScoreModel " scoremodel CDATA #IMPLIED">
			scoremodel = el_outpro.attributeValue("scoremodel");
			// may be null -> then assume SumOfScores

			// set the cutvalue if given (only variable score)
			cutvalue = QTIHelper.getFloatAttribute(el_outpro, "outcomes/decvar[@varname='SCORE']", "cutvalue");
			final List el_oft = el_outpro.selectNodes("outcomes_feedback_test");
			if (el_oft.size() != 0) {
				feedbacktesting = true;
			}
		}

		initSections(el_assessment, switches);
		init();
	}

	private void initSections(final Element assessment, final Switches sw) {
		sectionContexts = new ArrayList(2);

		// <!ELEMENT sectionref (#PCDATA)>
		// <!ATTLIST sectionref %I_LinkRefId; >
		final List sections = assessment.selectNodes("section|sectionref");

		for (final Iterator iter = sections.iterator(); iter.hasNext();) {
			Element el_section = (Element) iter.next();

			// resolve sectionref into the correct sections
			if (el_section.getName().equals("sectionref")) {
				final String linkRefId = el_section.attributeValue("linkrefid");
				el_section = (Element) el_section.selectSingleNode("//section[@ident='" + linkRefId + "']");
				if (el_section == null) { throw new RuntimeException("sectionref with ref '" + linkRefId + "' could not be resolved"); }
			}

			final SectionContext sc = new SectionContext();
			sc.setUp(assessInstance, el_section, sw);
			sectionContexts.add(sc);
		}
	}

	/**
	 * start assessment
	 */
	public void start() {
		// if not started yet, start
		if (timeOfStart == -1) {
			timeOfStart = System.currentTimeMillis();
		}
	}

	/**
	 * stop assessment
	 */
	public void stop() {
		if (timeOfStart != -1 && timeOfStop == -1) {
			timeOfStop = System.currentTimeMillis();
		}
		if (getCurrentSectionContext() != null) {
			getCurrentSectionContext().sectionWasSubmitted();
		}
	}

	/**
	 * 
	 */
	public void eval() {
		if (assessInstance.isSurvey()) { return; }
		final int sccnt = getSectionContextCount();
		for (int i = 0; i < sccnt; i++) {
			final SectionContext sc = getSectionContext(i);
			sc.eval();
		}
		if (feedbacktesting) {
			calcFeedBack();
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "<br /><br />assessment:" + sectionContexts.toString() + "=" + super.toString();
	}

	/**
	 * Method getIdent.
	 * 
	 * @return String
	 */
	public String getIdent() {
		return ident;
	}

	/**
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return
	 */
	public SectionContext getCurrentSectionContext() {
		if (currentSectionContextPos == -1) { return null; }
		final SectionContext sc = (SectionContext) sectionContexts.get(currentSectionContextPos);
		return sc;
	}

	/**
	 * Sets the currentSectionPos.
	 * 
	 * @param currentSectionPos The currentSectionPos to set
	 */
	public void setCurrentSectionPos(final int currentSectionPos) {
		if (currentSectionPos >= sectionContexts.size()) { throw new RuntimeException("error"); }
		this.currentSectionContextPos = currentSectionPos;
	}

	/**
	 * Method getSectionContextCount.
	 * 
	 * @return int
	 */
	public int getSectionContextCount() {
		return sectionContexts.size();
	}

	/**
	 * Return the total items in all sections of the assessment.
	 * 
	 * @return Total number of items
	 */
	public int getItemContextCount() {
		int count = 0;
		final int sccnt = getSectionContextCount();

		for (int i = 0; i < sccnt; i++) {
			final SectionContext sc = getSectionContext(i);
			count += sc.getItemContextCount();
		}
		return count;
	}

	/**
	 * Get the position of the current item within the assessment.
	 * 
	 * @return position of the current item within the assessment.
	 */
	public int getItemPosWithinAssessment() {
		if (currentSectionContextPos == -1) { return 1; // first question
		}
		int currentPos = 1;
		for (int i = 0; i < getCurrentSectionContextPos(); i++) {
			// count all items in previous section
			currentPos += getSectionContext(i).getItemContextCount();
		}
		final SectionContext curSectionContext = getCurrentSectionContext();
		if (curSectionContext.getCurrentItemContextPos() != -1) {
			// this is a section page, just add 1 item to the current pos
			currentPos += curSectionContext.getCurrentItemContextPos();
		}
		return currentPos;
	}

	/**
	 * Get the position of the first item within the assessment.
	 * 
	 * @return position of the first item within the assessment.
	 */
	public int getFirstItemPosWithinSection() {
		if (currentSectionContextPos == -1) { return 1; // first question
		}
		int currentPos = 1;
		for (int i = 0; i < getCurrentSectionContextPos(); i++) {
			// count all items in previous section
			currentPos += getSectionContext(i).getItemContextCount();
		}
		return currentPos;
	}

	/**
	 * Get the position of the last item of the current section within the assessment.
	 * 
	 * @return position of the last item of the current section within the assessment.
	 */
	public int getLastItemPosWithinSection() {
		int currentPos = 0;
		for (int i = 0; getCurrentSectionContextPos() > -1 && i < getCurrentSectionContextPos(); i++) {
			// count all items in previous section
			currentPos += getSectionContext(i).getItemContextCount();
		}
		final SectionContext curSectionContext = getCurrentSectionContext();
		if (getCurrentSectionContextPos() > -1) {
			currentPos += getSectionContext(getCurrentSectionContextPos()).getItemContextCount();
		}

		return currentPos;
	}

	/**
	 * Method setCurrentSectionContextPos.
	 * 
	 * @param i
	 */
	public void setCurrentSectionContextPos(final int i) {
		currentSectionContextPos = i;
	}

	/**
	 * Returns the currentSectionContextPos.
	 * 
	 * @return int
	 */
	public int getCurrentSectionContextPos() {
		return currentSectionContextPos;
	}

	/**
	 * checks whether the user may still submit answers
	 * 
	 * @return
	 */
	public boolean isOpen() {
		// not started yet or no timelimit or within timelimit
		return (timeOfStart == -1) || (durationLimit == -1) || (System.currentTimeMillis() < (timeOfStart + durationLimit));
	}

	/**
	 * @return
	 */
	public boolean isStarted() {
		return (timeOfStart != -1);
	}

	/**
	 * @param pos
	 * @return
	 */
	public SectionContext getSectionContext(final int pos) {
		return (SectionContext) sectionContexts.get(pos);
	}

	/**
	 * @return long
	 */
	public long getDurationLimit() {
		return durationLimit;
	}

	/**
	 * Return the time to completion for this assessment
	 * 
	 * @return long Millis to completion
	 */
	public long getDuration() {
		if (timeOfStart == -1 | timeOfStop == -1) { return 0; }
		return timeOfStop - timeOfStart;
	}

	/**
	 * Get the maximum score for this assessment. (Sum of maxscore of all items)
	 * 
	 * @return
	 */
	public float getMaxScore() {
		float count = 0.0f;
		for (final Iterator iter = sectionContexts.iterator(); iter.hasNext();) {
			final SectionContext sc = (SectionContext) iter.next();
			final float maxScore = sc.getMaxScore();
			if (maxScore == -1) {
				return -1;
			} else {
				count += maxScore;
			}
		}
		return count;
	}

	/**
	 * @return
	 */
	public float getScore() {
		if (scoremodel == null || scoremodel.equalsIgnoreCase("SumOfScores")) { // sumofScores

			float count = 0;
			for (final Iterator iter = sectionContexts.iterator(); iter.hasNext();) {
				final SectionContext sc = (SectionContext) iter.next();
				count += sc.getScore();
			}
			return count;
		} else if (scoremodel.equalsIgnoreCase("NumberCorrect")) {
			float tmpscore = 0.0f;
			// calculate correct number of sections: an section is correct if its
			// correct items reach the section's cutvalue
			for (final Iterator iter = sectionContexts.iterator(); iter.hasNext();) {
				final SectionContext sc = (SectionContext) iter.next();
				final float sscore = sc.getScore();
				if (sscore >= cutvalue) {
					tmpscore++; // count items correct
				}
			}
			return tmpscore;
		} else {
			throw new RuntimeException("scoring algorithm " + scoremodel + " not supported");
		}

	}

	/**
	 * @return
	 */
	public boolean isPassed() {
		final float score = getScore();
		return (score >= cutvalue);
	}

	/**
	 * @return
	 */
	public int getItemsPresentedCount() {
		int count = 0;
		for (final Iterator iter = sectionContexts.iterator(); iter.hasNext();) {
			final SectionContext sc = (SectionContext) iter.next();
			count += sc.getItemsPresentedCount();
		}
		return count;
	}

	/**
	 * @return
	 */
	public int getItemsAnsweredCount() {
		int count = 0;
		for (final Iterator iter = sectionContexts.iterator(); iter.hasNext();) {
			final SectionContext sc = (SectionContext) iter.next();
			count += sc.getItemsAnsweredCount();
		}
		return count;
	}

	/**
	 * @return
	 */
	public int getItemsAttemptedCount() {
		int count = 0;
		for (final Iterator iter = sectionContexts.iterator(); iter.hasNext();) {
			final SectionContext sc = (SectionContext) iter.next();
			count += sc.getItemsAttemptedCount();
		}
		return count;
	}

	/**
	 * Method calcFeedBack.
	 */
	private void calcFeedBack() {
		if (feedbacktesting) {
			final List el_ofts = el_assessment.selectNodes("outcomes_processing/outcomes_feedback_test");
			feedbackavailable = false;
			for (final Iterator it_oft = el_ofts.iterator(); it_oft.hasNext();) {
				final Element el_oft = (Element) it_oft.next();
				// <!ELEMENT outcomes_feedback_test (test_variable , displayfeedback+)>
				final Element el_testvar = (Element) el_oft.selectSingleNode("test_variable");
				// must exist: dtd
				// <!ELEMENT test_variable (variable_test | and_test | or_test |
				// not_test)>
				final Element el_varandornot = (Element) el_testvar.selectSingleNode("variable_test|and_test|or_test|not_test");
				final String elname = el_varandornot.getName();
				final ScoreBooleanEvaluable sbe = QTIHelper.getSectionBooleanEvaluableInstance(elname);
				final float totalscore = getScore();
				final boolean fulfilled = sbe.eval(el_varandornot, totalscore);
				if (fulfilled) {
					// get feedback
					final Element el_displayfeedback = (Element) el_oft.selectSingleNode("displayfeedback");
					final String linkRefId = el_displayfeedback.attributeValue("linkrefid");
					// must exist (dtd)
					// ignore feedbacktype, since we section or assess feedback only
					// accepts material, no hints or solutions
					final Element el_resolved = (Element) el_assessment.selectSingleNode(".//assessfeedback[@ident='" + linkRefId + "']");
					getOutput().setEl_response(new AssessFeedback(el_resolved));
					// give the whole assessmentfeedback to render
					feedbackavailable = true;
				}
			}
		}
	}

	/**
	 * @return Output
	 */
	public Output getOutput() {
		if (output == null) {
			output = new Output();
		}
		return output;
	}

	/**
	 * @return
	 */
	public Switches getSwitches() {
		return switches;
	}

	/**
	 * @param switches
	 */
	public void setSwitches(final Switches switches) {
		this.switches = switches;
	}

	/**
	 * @return
	 */
	public boolean isFeedbackavailable() {
		return feedbackavailable;
	}

	/**
	 * @param b
	 */
	public void setFeedbackavailable(final boolean b) {
		feedbackavailable = b;
	}

	/**
	 * @return float
	 */
	public float getCutvalue() {
		return cutvalue;
	}

	/**
	 * @return
	 */
	public long getTimeOfStart() {
		return timeOfStart;
	}

	/**
	 * @return
	 */
	public long getTimeOfStop() {
		return timeOfStop;
	}

	public Objectives getObjectives() {
		return objectives;
	}
}