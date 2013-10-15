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
import org.olat.ims.qti.editor.QTIEditHelper;

/**
 * @author rkulow
 */
public class Item implements QTIObject {

	// Item Attributes
	private String ident = null; // required, max 256 chars
	private String title = null; // optional, max 256 chars
	private String label = null; // optional, max 256 chars
	private int maxattempts = 0; // optional, max 2 chars

	// Elements
	private QTIObject qticomment = null; // occurs 0 ore 1 time
	private Duration duration = null; // occurs 0 ore 1 time
	private QTIObject itemmetadata = null; // occurs 0 to 1
	private List itempreconditions = new ArrayList(); // occurs 0 to many
	private List itempostconditions = new ArrayList(); // occurs 0 to many
	private String objectives = null; // occurs 0 to many, 1st processed
	private List itemcontrols = new ArrayList(); // occurs 0 to many
	private List itemrubrics = new ArrayList(); // occurs 0 to many
	private List rubrics = new ArrayList(); // occurs 0 to many
	private List itemfeedbacks = new ArrayList(); // occurs 0 to many
	private Question question = null; // occurs once
	private QTIXMLWrapper rawXML = null;

	/**
	 * @see org.olat.ims.qti.editor.beecom.QTIObject#addToElement(org.dom4j.Element)
	 */
	@Override
	public void addToElement(final Element root) {

		// check for raw xml
		if (rawXML != null) {
			rawXML.addToElement(root);
			return;
		}

		// Item
		final Element item = root.addElement("item");
		item.addAttribute("ident", this.ident);
		item.addAttribute("title", this.title);
		if (maxattempts > 0) {
			item.addAttribute("maxattempts", "" + maxattempts);
		}

		// DURATION
		final QTIObject obj_duration = this.getDuration();
		if (obj_duration != null) {
			obj_duration.addToElement(item);
		}

		// OBJECTIVES
		QTIEditHelper.addObjectives(item, objectives);

		// ITEMCONTROL
		for (final Iterator i = this.itemcontrols.iterator(); i.hasNext();) {
			((QTIObject) i.next()).addToElement(item);
		}

		// QUESTION
		if (question != null) {
			((QTIObject) question).addToElement(item);
		}

		// FEEDBACK
		for (final Iterator i = this.itemfeedbacks.iterator(); i.hasNext();) {
			final QTIObject obj = (QTIObject) i.next();
			if (obj != null) {
				obj.addToElement(item);
			}
		}
	}

	/**
	 * Search an item for material with a given ID
	 * 
	 * @param matId
	 * @return
	 */
	public Material findMaterial(final String matId) {
		Material mat = null;
		// look at question
		mat = getQuestion().getQuestion();
		if (mat.getId().equals(matId)) { return mat; }
		// look at responses
		for (final Iterator iter = getQuestion().getResponses().iterator(); iter.hasNext();) {
			final Response element = (Response) iter.next();
			mat = element.getContent();
			if (mat.getId().equals(matId)) { return mat; }
		}
		return null;
	}

	public boolean isAlient() {
		return (rawXML != null);
	}

	/**
	 * Returns the duration.
	 * 
	 * @return QTIObject
	 */
	public Duration getDuration() {
		return duration;
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
	 * Returns the itemcontrols.
	 * 
	 * @return List
	 */
	public List getItemcontrols() {
		return itemcontrols;
	}

	/**
	 * Returns the itemfeedbacks.
	 * 
	 * @return List
	 */
	public List getItemfeedbacks() {
		return itemfeedbacks;
	}

	/**
	 * Returns the itemmetadata.
	 * 
	 * @return QTIObject
	 */
	public QTIObject getItemmetadata() {
		return itemmetadata;
	}

	/**
	 * Returns the itempostconditions.
	 * 
	 * @return List
	 */
	public List getItempostconditions() {
		return itempostconditions;
	}

	/**
	 * Returns the itempreconditions.
	 * 
	 * @return List
	 */
	public List getItempreconditions() {
		return itempreconditions;
	}

	/**
	 * Returns the itemrubrics.
	 * 
	 * @return List
	 */
	public List getItemrubrics() {
		return itemrubrics;
	}

	/**
	 * Returns the label.
	 * 
	 * @return String
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Returns the maxattempts.
	 * 
	 * @return String
	 */
	public int getMaxattempts() {
		return maxattempts;
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
	 * Returns the qticomment.
	 * 
	 * @return QTIObject
	 */
	public QTIObject getQticomment() {
		return qticomment;
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
	 * Returns the title.
	 * 
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the duration.
	 * 
	 * @param duration The duration to set
	 */
	public void setDuration(final Duration duration) {
		this.duration = duration;
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
	 * Sets the itemcontrols.
	 * 
	 * @param itemcontrols The itemcontrols to set
	 */
	public void setItemcontrols(final List itemcontrols) {
		this.itemcontrols = itemcontrols;
	}

	/**
	 * Sets the itemfeedbacks.
	 * 
	 * @param itemfeedbacks The itemfeedbacks to set
	 */
	public void setItemfeedbacks(final List itemfeedbacks) {
		this.itemfeedbacks = itemfeedbacks;
	}

	/**
	 * Sets the itemmetadata.
	 * 
	 * @param itemmetadata The itemmetadata to set
	 */
	public void setItemmetadata(final QTIObject itemmetadata) {
		this.itemmetadata = itemmetadata;
	}

	/**
	 * Sets the itempostconditions.
	 * 
	 * @param itempostconditions The itempostconditions to set
	 */
	public void setItempostconditions(final List itempostconditions) {
		this.itempostconditions = itempostconditions;
	}

	/**
	 * Sets the itempreconditions.
	 * 
	 * @param itempreconditions The itempreconditions to set
	 */
	public void setItempreconditions(final List itempreconditions) {
		this.itempreconditions = itempreconditions;
	}

	/**
	 * Sets the itemrubrics.
	 * 
	 * @param itemrubrics The itemrubrics to set
	 */
	public void setItemrubrics(final List itemrubrics) {
		this.itemrubrics = itemrubrics;
	}

	/**
	 * Sets the label.
	 * 
	 * @param label The label to set
	 */
	public void setLabel(final String label) {
		this.label = label;
	}

	/**
	 * Sets the maxattempts.
	 * 
	 * @param maxattempts The maxattempts to set
	 */
	public void setMaxattempts(final int maxattempts) {
		this.maxattempts = maxattempts;
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
	 * Sets the qticomment.
	 * 
	 * @param qticomment The qticomment to set
	 */
	public void setQticomment(final QTIObject qticomment) {
		this.qticomment = qticomment;
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
	 * Sets the title.
	 * 
	 * @param title The title to set
	 */
	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * @return
	 */
	public Question getQuestion() {
		return question;
	}

	/**
	 * @param object
	 */
	public void setQuestion(final Question object) {
		question = object;
	}

	/**
	 * @return
	 */
	public QTIXMLWrapper getRawXML() {
		return rawXML;
	}

	/**
	 * @param wrapper
	 */
	public void setRawXML(final QTIXMLWrapper wrapper) {
		rawXML = wrapper;
	}

}