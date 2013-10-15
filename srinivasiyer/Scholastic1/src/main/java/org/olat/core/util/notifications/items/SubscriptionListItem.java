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
 * Copyright (c) since 2004 at frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.core.util.notifications.items;

import java.util.Date;
import java.util.Locale;

import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.notifications.SubscriptionInfo;

/**
 * Description:<br>
 * represents a news-item in SubscriptionInfo
 * <P>
 * Initial Date: 07.12.2009 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, frentix GmbH
 */
public class SubscriptionListItem {

	private final String link;
	private final Date date;
	private final String description;
	private final String descriptionTooltip;
	private final String iconCssClass;

	public SubscriptionListItem(String desc, String url, Date dateInfo, String iconCssClass) {
		this(desc, null, url, dateInfo, iconCssClass);
	}

	public SubscriptionListItem(String desc, String tooltip, String url, Date dateInfo, String iconCssClass) {
		this.description = desc;
		this.descriptionTooltip = tooltip;
		this.link = url;
		this.date = dateInfo;
		this.iconCssClass = iconCssClass;
	}

	public String getLink() {
		return link;
	}

	public Date getDate() {
		return date;
	}

	public String getDescription() {
		return description;
	}

	public String getIconCssClass() {
		return iconCssClass;
	}

	public String getDescriptionTooltip() {
		return descriptionTooltip;
	}

	/**
	 * compose list item representation depending on mimeType
	 * 
	 * @param mimeType
	 * @param locale
	 * @return formated list-item
	 */
	public String getContent(String mimeType, Locale locale) {
		if (mimeType.equals(SubscriptionInfo.MIME_HTML)) {
			return getHTMLContent(locale);
		} else {
			return getPlaintextContent(locale);
		}
	}

	private String getPlaintextContent(Locale locale) {
		Translator trans = Util.createPackageTranslator(SubscriptionInfo.class, locale);
		Formatter form = Formatter.getInstance(locale);
		StringBuilder sb = new StringBuilder();
		String datePart = trans.translate("subscription.listitem.dateprefix", new String[] { form.formatDateAndTime(date) });
		sb.append("- ");
		sb.append(description.trim());
		sb.append(" ").append(datePart.trim());
		if (StringHelper.containsNonWhitespace(link)) sb.append("\n").append("  ").append(link);
		return sb.toString();
	}

	private String getHTMLContent(Locale locale) {
		StringBuilder sb = new StringBuilder();
		Translator trans = Util.createPackageTranslator(SubscriptionInfo.class, locale);
		Formatter form = Formatter.getInstance(locale);
		String datePart = trans.translate("subscription.listitem.dateprefix", new String[] { form.formatDateAndTime(date) });
		if (iconCssClass != null) {
			sb.append("<li class=\"b_with_small_icon_left ");
			sb.append(iconCssClass);
			sb.append("\">");
		} else {
			sb.append("<li>");
		}
		if (StringHelper.containsNonWhitespace(link)) {
			sb.append("<a href=\"");
			sb.append(link);
			sb.append("\">");
		}
		sb.append(description.trim());
		if (StringHelper.containsNonWhitespace(link)) sb.append("</a>");
		sb.append(" ").append(datePart.trim());
		sb.append("</li>");
		return sb.toString();
	}

}
