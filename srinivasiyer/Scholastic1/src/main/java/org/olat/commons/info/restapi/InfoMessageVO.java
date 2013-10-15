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
package org.olat.commons.info.restapi;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.olat.commons.info.model.InfoMessage;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "infoMessageVO")
public class InfoMessageVO {

	private Long key;
	private Date creationDate;
	private String title;
	private String message;

	private Long resId;
	private String resName;
	private String resSubPath;
	private String businessPath;

	private Long authorKey;

	public InfoMessageVO() {
		// make JAXB happy
	}

	public InfoMessageVO(final InfoMessage info) {
		key = info.getKey();
		creationDate = info.getCreationDate();
		title = info.getTitle();
		message = info.getMessage();
		resId = info.getResId();
		resName = info.getResName();
		resSubPath = info.getResSubPath();
		businessPath = info.getBusinessPath();
		authorKey = info.getAuthor().getKey();
	}

	public Long getKey() {
		return key;
	}

	public void setKey(final Long key) {
		this.key = key;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(final Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public Long getResId() {
		return resId;
	}

	public void setResId(final Long resId) {
		this.resId = resId;
	}

	public String getResName() {
		return resName;
	}

	public void setResName(final String resName) {
		this.resName = resName;
	}

	public String getResSubPath() {
		return resSubPath;
	}

	public void setResSubPath(final String resSubPath) {
		this.resSubPath = resSubPath;
	}

	public String getBusinessPath() {
		return businessPath;
	}

	public void setBusinessPath(final String businessPath) {
		this.businessPath = businessPath;
	}

	public Long getAuthorKey() {
		return authorKey;
	}

	public void setAuthorKey(final Long authorKey) {
		this.authorKey = authorKey;
	}
}
