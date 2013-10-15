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

package org.olat.ims.qti.container.qtielements;

import org.dom4j.Element;
import org.olat.core.util.Formatter;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.filter.FilterFactory;

/**
 * Initial Date: 24.11.2004
 * 
 * @author Mike Stock
 */
public class Mattext extends GenericQTIElement {

	/**
	 * Comment for <code>xmlClass</code>
	 */
	public static final String xmlClass = "mattext";

	private final String texttype;
	private final String charset;
	private final String content;

	/**
	 * @param el_matemtext
	 */
	public Mattext(final Element el_matemtext) {
		super(el_matemtext);
		texttype = el_matemtext.attributeValue("texttype");
		charset = el_matemtext.attributeValue("charset");
		content = el_matemtext.getText();
	}

	/**
	 * @return charset
	 */
	public String getCharset() {
		return charset;
	}

	/**
	 * @return content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * @return texttype
	 */
	public String getTexttype() {
		return texttype;
	}

	/**
	 * @see org.olat.ims.qti.container.qtielements.QTIElement#render(StringBuilder, RenderInstructions)
	 */
	@Override
	public void render(final StringBuilder buffer, final RenderInstructions ri) {
		buffer.append("<span class=\"o_qti_item_mattext\">");
		// Add static media base URI to render media elements inline
		final Filter urlFilter = FilterFactory.getBaseURLToMediaRelativeURLFilter((String) ri.get(RenderInstructions.KEY_STATICS_PATH));
		String withBaseUrl = urlFilter.filter(content);
		// Add latex fomulas formatter
		withBaseUrl = Formatter.formatLatexFormulas(withBaseUrl);
		//
		buffer.append(withBaseUrl);
		buffer.append("</span>");
	}
}
