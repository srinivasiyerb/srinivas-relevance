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

package org.olat.ims.qti.process.elements.section;

import java.util.List;

import org.dom4j.Element;
import org.olat.ims.qti.process.QTIHelper;
import org.olat.ims.qti.process.elements.ExpressionBuilder;

/**
 * @author Felix Jost
 */
public class QTI_not_selection implements ExpressionBuilder {

	/**
	 * Constructor for QTI_and_selection.
	 */
	public QTI_not_selection() {
		super();
	}

	/**
	 * <!ELEMENT and_selection (selection_metadata | and_selection | or_selection | not_selection)+>
	 * 
	 * @see org.olat.qti.process.elements.ExpressionBuilder#buildXPathExpression(org.dom4j.Element, java.lang.StringBuilder)
	 */
	@Override
	public void buildXPathExpression(final Element selectionElement, final StringBuilder expr, final boolean not_switch, final boolean use_switch) {
		// assert: use_switch always true
		if (!use_switch) { throw new RuntimeException("error in not_selection; use_switch was switched off"); }
		final List elems = selectionElement.elements();
		final Element child = (Element) elems.get(0);
		final String name = child.getName();
		final ExpressionBuilder eb = QTIHelper.getExpressionBuilder(name);
		eb.buildXPathExpression(child, expr, !not_switch, true);
	}

}
