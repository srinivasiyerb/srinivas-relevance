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
package org.olat.user.restapi;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.olat.restapi.support.vo.LinkVO;

/**
 * Description:<br>
 * TODO: srosse Class Description for UserVO
 * <P>
 * Initial Date: 7 apr. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "userVO")
public class UserVO {

	private Long key;
	private String login;
	private String password;
	private String firstName;
	private String lastName;
	private String email;

	@XmlElementWrapper(name = "properties")
	@XmlElement(name = "property")
	private List<UserPropertyVO> properties = new ArrayList<UserPropertyVO>();

	@XmlElement(name = "link", nillable = true)
	private List<LinkVO> link = new ArrayList<LinkVO>();

	public UserVO() {
		// make JAXB happy
	}

	public Long getKey() {
		return key;
	}

	public void setKey(final Long key) {
		this.key = key;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(final String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public List<UserPropertyVO> getProperties() {
		return properties;
	}

	public void setProperties(final List<UserPropertyVO> properties) {
		this.properties = properties;
	}

	public void putProperty(final String name, final String value) {
		properties.add(new UserPropertyVO(name, value));
	}

	public String getProperty(final String name) {
		for (final UserPropertyVO entry : properties) {
			if (entry.getName().equals(name)) { return entry.getValue(); }
		}
		return null;
	}

	public List<LinkVO> getLink() {
		return link;
	}

	public void setLink(final List<LinkVO> link) {
		this.link = link;
	}

	@Override
	public String toString() {
		return "UserVO[key=" + key + ":name=" + login + "]";
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) { return true; }
		if (obj instanceof UserVO) {
			final UserVO vo = (UserVO) obj;
			return key != null && key.equals(vo.key);
		}
		return false;
	}
}