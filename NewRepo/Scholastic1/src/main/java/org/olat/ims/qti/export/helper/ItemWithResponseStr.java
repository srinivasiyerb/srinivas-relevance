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

package org.olat.ims.qti.export.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.olat.ims.qti.QTIResult;
import org.olat.ims.qti.QTIResultManager;

/**
 * @author
 */

public class ItemWithResponseStr implements QTIItemObject {
	private boolean isEssay = true;
	private String itemIdent = null;
	private String itemTitle = null;
	private String itemMinValue = null;
	private String itemMaxValue = null;
	private String itemCutValue = null;

	// CELFI#107
	private String quetionText = "";
	// CELFI#107 END

	private final List responseColumnHeaders = new ArrayList(5);
	private final List responseStrIdents = new ArrayList(5);

	// CELFI#107
	private final List responseLabelMaterials = new ArrayList(5);

	// CELFI#107 END

	/**
	 * Constructor for ItemWithResponseLid.
	 * 
	 * @param el_item
	 */
	public ItemWithResponseStr(final Element el_item) {
		// CELFI#107
		this.itemTitle = el_item.attributeValue("title");
		this.itemIdent = el_item.attributeValue("ident");

		final Element decvar = (Element) el_item.selectSingleNode(".//outcomes/decvar");

		if (decvar != null) {
			this.itemMinValue = decvar.attributeValue("minvalue");
			this.itemMaxValue = decvar.attributeValue("maxvalue");
			this.itemCutValue = decvar.attributeValue("cutvalue");
		}

		final List el_presentationElements = el_item.selectNodes(".//presentation//mattext | .//presentation//response_str");

		int i = 1;
		boolean lastWasMattext = false;
		for (final Iterator itPresentations = el_presentationElements.iterator(); itPresentations.hasNext();) {
			final Element el_presentation = (Element) itPresentations.next();
			final String el_qname = el_presentation.getQualifiedName();
			if (el_qname.equalsIgnoreCase("mattext")) {
				this.quetionText += el_presentation.getTextTrim();
				lastWasMattext = true;
			} else {

				responseStrIdents.add(el_presentation.attributeValue("ident"));
				final Element render_fib = el_presentation.element("render_fib");
				if (render_fib != null) {
					isEssay = (render_fib.attributeValue("rows") == null) ? false : true;
					responseColumnHeaders.add((isEssay ? "A" : "B") + i); // A -> Area, B -> Blank

					final Element responseValue = (Element) el_item.selectSingleNode(".//varequal[@respident='" + el_presentation.attributeValue("ident") + "']");
					if (responseValue != null) {
						responseLabelMaterials.add(responseValue.getTextTrim());
						if (lastWasMattext) {
							this.quetionText += " [" + responseValue.getTextTrim() + "] ";
							lastWasMattext = false;
						}
					} else {
						responseLabelMaterials.add("");
					}

				} else {
					responseColumnHeaders.add("unknownType");

					responseLabelMaterials.add("");
				}
				i++;
			}
		}
		// CELFI#107 END
	}

	/**
	 * @see org.olat.ims.qti.export.helper.QTIItemObject#getNumColumnHeaders()
	 */
	@Override
	public int getNumColumnHeaders() {
		return responseColumnHeaders.size();
	}

	/**
	 * @see org.olat.ims.qti.export.helper.QTIItemObject#extractQTIResult(java.util.List)
	 */
	@Override
	public QTIResult extractQTIResult(final List resultSet) {
		for (final Iterator iter = resultSet.iterator(); iter.hasNext();) {
			final QTIResult element = (QTIResult) iter.next();
			if (element.getItemIdent().equals(itemIdent)) {
				resultSet.remove(element);
				return element;
			}
		}
		return null;
	}

	private void addTextAndTabs(final List responseColumns, final String s, final int num) {
		for (int i = 0; i < num; i++) {
			responseColumns.add(s);
		}
	}

	/**
	 * @return itemTitle
	 */
	@Override
	public String getItemTitle() {
		return itemTitle;
	}

	// CELFI#107
	@Override
	public String getQuestionText() {
		return this.quetionText;
	}

	@Override
	public List getResponseColumnHeaders() {
		return responseColumnHeaders;
	}

	/**
	 * @see org.olat.ims.qti.export.helper.QTIItemObject#getResponseColumns(org.olat.ims.qti.QTIResult)
	 */
	@Override
	public List getResponseColumns(final QTIResult qtiresult) {
		final List responseColumns = new ArrayList();
		if (qtiresult == null) {
			// item has not been choosen
			addTextAndTabs(responseColumns, "", getNumColumnHeaders());
		} else {
			final String answer = qtiresult.getAnswer();
			if (answer.length() == 0) {
				addTextAndTabs(responseColumns, ".", getNumColumnHeaders());
			} else {
				final Map answerMap = QTIResultManager.parseResponseStrAnswers(answer);

				for (final Iterator iter = responseStrIdents.iterator(); iter.hasNext();) {
					final String element = (String) iter.next();
					if (answerMap.containsKey(element)) {
						responseColumns.add(answerMap.get(element));
					} else {
						// should not happen
					}
				}
			}
		}

		return responseColumns;
	}

	@Override
	public TYPE getItemType() {
		return isEssay ? TYPE.A : TYPE.B; // A -> Area, B -> Blank
	}

	/**
	 * @see org.olat.ims.qti.export.helper.QTIItemObject#getResponseIdentifier()
	 */
	@Override
	public List getResponseIdentifier() {
		return responseStrIdents;
	}

	@Override
	public List getResponseLabelMaterials() {
		// CELFI#107
		return responseLabelMaterials;
	}

	/**
	 * @see org.olat.ims.qti.export.helper.QTIItemObject#getItemIdent()
	 */
	@Override
	public String getItemIdent() {
		return itemIdent;
	}

	@Override
	public String getItemMinValue() {
		return itemMinValue;
	}

	@Override
	public String getItemMaxValue() {
		return itemMaxValue;
	}

	@Override
	public String getItemCutValue() {
		return itemCutValue;
	}

	@Override
	public boolean hasPositionsOfResponses() {
		return false;
	}

	@Override
	public String getPositionsOfResponses() {
		return null;
	}

}
