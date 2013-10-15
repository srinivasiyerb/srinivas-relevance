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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.notifications.restapi.vo;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.olat.core.util.notifications.items.SubscriptionListItem;

/**
 * <h3>Description:</h3>
 * <p>
 * Initial Date: 25 aug. 2010 <br>
 * 
 * @author srosse, srosse@frentix.com, www.frentix.com
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SubscriptionListItemVO {

	private String link;
	private Date date;
	private String description;
	private String iconCssClass;

	public SubscriptionListItemVO() {
		// make JAXB happy
	}

	public SubscriptionListItemVO(final SubscriptionListItem item) {
		link = item.getLink();
		date = item.getDate();
		description = item.getDescription();
		iconCssClass = item.getIconCssClass();
	}

	public String getLink() {
		return link;
	}

	public void setLink(final String link) {
		this.link = link;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(final Date date) {
		this.date = date;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getIconCssClass() {
		return iconCssClass;
	}

	public void setIconCssClass(final String iconCssClass) {
		this.iconCssClass = iconCssClass;
	}
}