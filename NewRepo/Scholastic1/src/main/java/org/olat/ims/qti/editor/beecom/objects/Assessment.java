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

package org.olat.ims.qti.editor.beecom.objects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.olat.core.util.CodeHelper;
import org.olat.ims.qti.editor.QTIEditHelper;

/**
 * @author rkulow
 */
public class Assessment implements QTIObject {

	// Assesment Attributes
	private String ident = ""; // required, max 256 chars
	private String title = ""; // optional, max 256 chars
	private String xml_lang = null; // optional, max 32 chars

	// Elements
	private QTIObject qticomment = null; // occurs 0 ore 1 time
	private QTIObject duration = null; // occurs 0 ore 1 time
	private String objectives = null; // occurs 0 to many, 1st if available is processed
	private List rubrics = new ArrayList(); // occurs 0 to many
	private List assessmentcontrols = new ArrayList();// occurs 0 to many
	private QTIObject presentation_material = null; // occurs 0 to 1 time
	private OutcomesProcessing outcomes_processing = null; // ?
	private Metadata metadata = new Metadata(); // occurs 0 to 1 time
	private List assessfeedbacks = new ArrayList(); // occurs 0 to many
	private QTIObject selection_ordering = null; // ?
	private QTIObject reference = null; // occurs 0 to 1 time
	private List sections = new ArrayList(); // occurs 0 to 1 time ( sections and section_references)
	private List items = new ArrayList();
	private boolean inheritControls = false;

	public Assessment() {
		setIdent(String.valueOf(CodeHelper.getRAMUniqueID()));
		setTitle("New QTI Document");
		getAssessmentcontrols().add(new Control());
	}

	@Override
	public void addToElement(final Element root) {

		final Element assessment = root.addElement("assessment");
		assessment.addAttribute("ident", ident);
		assessment.addAttribute("title", title);

		// DURATION
		final QTIObject obj_duration = this.getDuration();
		if (obj_duration != null) {
			obj_duration.addToElement(assessment);
		}

		// METADATA
		final QTIObject meta = this.getMetadata();
		if (meta != null) {
			meta.addToElement(assessment);
		}

		// OBJECTIVES
		QTIEditHelper.addObjectives(assessment, objectives);

		// CONTROL
		if (isInheritControls()) {
			for (final Iterator i = this.assessmentcontrols.iterator(); i.hasNext();) {
				final QTIObject obj = (QTIObject) i.next();
				obj.addToElement(assessment);
			}
		}

		// OUTCOMES PROCESSING
		final QTIObject obj_outcomes_processing = this.getOutcomes_processing();
		if (obj_outcomes_processing != null) {
			obj_outcomes_processing.addToElement(assessment);
		}

		// SECTIONS
		for (final Iterator i = this.sections.iterator(); i.hasNext();) {
			final QTIObject obj = (QTIObject) i.next();
			if (obj != null) {
				obj.addToElement(assessment);
			}
		}

		// ITEMS
		for (final Iterator i = this.items.iterator(); i.hasNext();) {
			final QTIObject obj = (QTIObject) i.next();
			if (obj != null) {
				obj.addToElement(assessment);
			}
		}
	}

	/**
	 * Returns the ident.
	 * 
	 * @return String
	 */
	public String getIdent() {
		return ident;
	}

	/**
	 * Returns the title.
	 * 
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the ident.
	 * 
	 * @param ident The ident to set
	 */
	public void setIdent(final String ident) {
		this.ident = ident;
	}

	/**
	 * Sets the title.
	 * 
	 * @param title The title to set
	 */
	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * Returns the sections.
	 * 
	 * @return List
	 */
	public List getSections() {
		return sections;
	}

	/**
	 * Sets the sections.
	 * 
	 * @param sections The sections to set
	 */
	public void setSections(final List sections) {
		this.sections = sections;
	}

	/**
	 * Returns the assessmentcontrols.
	 * 
	 * @return Switches
	 */
	public List getAssessmentcontrols() {
		return assessmentcontrols;
	}

	/**
	 * Sets the assessmentcontrols.
	 * 
	 * @param assessmentcontrols The assessmentcontrols to set
	 */
	public void setAssessmentcontrols(final List assessmentcontrols) {
		this.assessmentcontrols = assessmentcontrols;
	}

	/**
	 * Returns the assessfeedbacks.
	 * 
	 * @return List
	 */
	public List getAssessfeedbacks() {
		return assessfeedbacks;
	}

	/**
	 * Returns the assessproc_extension.
	 * 
	 * @return QTIObject
	 */
	public Metadata getMetadata() {
		return metadata;
	}

	/**
	 * Returns the objectives.
	 * 
	 * @return List
	 */
	public String getObjectives() {
		return objectives;
	}

	/**
	 * Returns the outcomes_processing.
	 * 
	 * @return OutcomesProcessing
	 */
	public OutcomesProcessing getOutcomes_processing() {
		return outcomes_processing;
	}

	/**
	 * Returns the presentation_material.
	 * 
	 * @return QTIObject
	 */
	public QTIObject getPresentation_material() {
		return presentation_material;
	}

	/**
	 * Returns the qticomment.
	 * 
	 * @return QTIObject
	 */
	public QTIObject getQticomment() {
		return qticomment;
	}

	/**
	 * Returns the reference.
	 * 
	 * @return QTIObject
	 */
	public QTIObject getReference() {
		return reference;
	}

	/**
	 * Returns the rubrics.
	 * 
	 * @return List
	 */
	public List getRubrics() {
		return rubrics;
	}

	/**
	 * Returns the selection_ordering.
	 * 
	 * @return QTIObject
	 */
	public QTIObject getSelection_ordering() {
		return selection_ordering;
	}

	/**
	 * Returns the xml_lang.
	 * 
	 * @return String
	 */
	public String getXml_lang() {
		return xml_lang;
	}

	/**
	 * Sets the assessfeedbacks.
	 * 
	 * @param assessfeedbacks The assessfeedbacks to set
	 */
	public void setAssessfeedbacks(final List assessfeedbacks) {
		this.assessfeedbacks = assessfeedbacks;
	}

	/**
	 * Sets the assessproc_extension.
	 * 
	 * @param assessproc_extension The assessproc_extension to set
	 */
	public void setMetadata(final Metadata metadata) {
		this.metadata = metadata;
	}

	/**
	 * Sets the objectives.
	 * 
	 * @param objectives The objectives to set
	 */
	public void setObjectives(final String objectives) {
		this.objectives = objectives;
	}

	/**
	 * Sets the outcomes_processing.
	 * 
	 * @param outcomes_processing The outcomes_processing to set
	 */
	public void setOutcomes_processing(final OutcomesProcessing outcomes_processing) {
		this.outcomes_processing = outcomes_processing;
	}

	/**
	 * Sets the presentation_material.
	 * 
	 * @param presentation_material The presentation_material to set
	 */
	public void setPresentation_material(final QTIObject presentation_material) {
		this.presentation_material = presentation_material;
	}

	/**
	 * Sets the qticomment.
	 * 
	 * @param qticomment The qticomment to set
	 */
	public void setQticomment(final QTIObject qticomment) {
		this.qticomment = qticomment;
	}

	/**
	 * Sets the reference.
	 * 
	 * @param reference The reference to set
	 */
	public void setReference(final QTIObject reference) {
		this.reference = reference;
	}

	/**
	 * Sets the rubrics.
	 * 
	 * @param rubrics The rubrics to set
	 */
	public void setRubrics(final List rubrics) {
		this.rubrics = rubrics;
	}

	/**
	 * Sets the selection_ordering.
	 * 
	 * @param selection_ordering The selection_ordering to set
	 */
	public void setSelection_ordering(final QTIObject selection_ordering) {
		this.selection_ordering = selection_ordering;
	}

	/**
	 * Sets the xml_lang.
	 * 
	 * @param xml_lang The xml_lang to set
	 */
	public void setXml_lang(final String xml_lang) {
		this.xml_lang = xml_lang;
	}

	/**
	 * Returns the items.
	 * 
	 * @return List
	 */
	public List getItems() {
		return items;
	}

	/**
	 * Sets the items.
	 * 
	 * @param items The items to set
	 */
	public void setItems(final List items) {
		this.items = items;
	}

	/**
	 * @return
	 */
	public boolean isInheritControls() {
		return inheritControls;
	}

	/**
	 * @param b
	 */
	public void setInheritControls(final boolean b) {
		inheritControls = b;
	}

	/**
	 * Sets the duration.
	 * 
	 * @param duration The duration to set
	 */
	public void setDuration(final QTIObject duration) {
		this.duration = duration;
	}

	/**
	 * Returns the duration.
	 * 
	 * @return QTIObject
	 */
	public QTIObject getDuration() {
		return duration;
	}

	/**
	 * Checks if this assessment contains any questions of type 'essay'
	 * 
	 * @return
	 */
	public boolean containsEssayQuestions() {
		for (final Iterator i = this.sections.iterator(); i.hasNext();) {
			final Section section = (Section) i.next();
			if (section != null && section.containsEssayQuestions()) { return true; }
		}
		return false;
	}
}
