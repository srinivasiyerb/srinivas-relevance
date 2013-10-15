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

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocumentType;

/**
 * @author rkulow
 */
public class QTIDocument {

	private QTIObject qticomment = null; // occurs 0 or 1 time
	private QTIObject objectbank = null; // occurs 0 or 1 time
	private Assessment assessment = null; // occurs 0 or 1 time
	private boolean survey = false;

	private static String DOCUMENT_ROOT = "questestinterop";
	private static String DOCUMENT_DTD = "ims_qtiasiv1p2p1.dtd";

	/**
	 * Get the structure as an XML Document.
	 * 
	 * @return XML Document.
	 */
	public Document getDocument() {
		final Document tmp = DocumentHelper.createDocument();
		final DocumentType type = new DOMDocumentType();
		type.setElementName(DOCUMENT_ROOT);
		type.setSystemID(DOCUMENT_DTD);

		tmp.setDocType(type);
		final Element questestinterop = tmp.addElement(DOCUMENT_ROOT);
		this.assessment.addToElement(questestinterop);
		return tmp;
	}

	/**
	 * @return True if this assessment is a survey.
	 */
	public boolean isSurvey() {
		return survey;
	}

	/**
	 * @param survey
	 */
	public void setSurvey(final boolean survey) {
		this.survey = survey;
	}

	/**
	 * Returns the assesment.
	 * 
	 * @return Assesment
	 */
	public Assessment getAssessment() {
		return assessment;
	}

	/**
	 * Returns the objectbank.
	 * 
	 * @return QTIObject
	 */
	public QTIObject getObjectbank() {
		return objectbank;
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
	 * Sets the assesment.
	 * 
	 * @param assessment The assesment to set
	 */
	public void setAssessment(final Assessment assessment) {
		this.assessment = assessment;
	}

	/**
	 * Sets the objectbank.
	 * 
	 * @param objectbank The objectbank to set
	 */
	public void setObjectbank(final QTIObject objectbank) {
		this.objectbank = objectbank;
	}

	/**
	 * Sets the qticomment.
	 * 
	 * @param qticomment The qticomment to set
	 */
	public void setQticomment(final QTIObject qticomment) {
		this.qticomment = qticomment;
	}

}
