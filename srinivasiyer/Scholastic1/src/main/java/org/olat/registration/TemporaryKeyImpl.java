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

package org.olat.registration;

import java.util.Date;

import org.olat.core.id.CreateInfo;

/**
 * Description:
 * 
 * @author Sabina Jeger
 */
public class TemporaryKeyImpl implements CreateInfo, TemporaryKey {

	private Long key = null;
	private String emailAddress = null;
	private String ipAddress = null;
	private Date creationDate = new Date();
	private Date lastModified = null;
	private String registrationKey = null;
	private String regAction = null;
	private boolean mailSent = false;
	private int version;

	/**
	 * 
	 */
	protected TemporaryKeyImpl() {
		super();
	}

	/**
	 * Temporary key database object.
	 * 
	 * @param emailaddress
	 * @param ipaddress
	 * @param registrationKey
	 * @param action
	 */
	public TemporaryKeyImpl(final String emailaddress, final String ipaddress, final String registrationKey, final String action) {
		this.emailAddress = emailaddress;
		this.ipAddress = ipaddress;
		this.registrationKey = registrationKey;
		this.regAction = action;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#getEmailAddress()
	 */
	@Override
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#setEmailAddress(java.lang.String)
	 */
	@Override
	public void setEmailAddress(final String string) {
		emailAddress = string;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#getIpAddress()
	 */
	@Override
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#setIpAddress(java.lang.String)
	 */
	@Override
	public void setIpAddress(final String string) {
		ipAddress = string;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#getCreationDate()
	 */
	@Override
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#getLastModified()
	 */
	public Date getLastModified() {
		return lastModified;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#getRegistrationKey()
	 */
	@Override
	public String getRegistrationKey() {
		return registrationKey;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#setRegistrationKey(java.lang.String)
	 */
	@Override
	public void setRegistrationKey(final String string) {
		registrationKey = string;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#isMailSent()
	 */
	@Override
	public boolean isMailSent() {
		return mailSent;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#setMailSent(boolean)
	 */
	@Override
	public void setMailSent(final boolean b) {
		mailSent = b;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#getKey()
	 */
	@Override
	public Long getKey() {
		return key;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#setKey(java.lang.Long)
	 */
	@Override
	public void setKey(final Long long1) {
		key = long1;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#setCreationDate(java.util.Date)
	 */
	@Override
	public void setCreationDate(final Date date) {
		creationDate = date;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#setLastModified(java.util.Date)
	 */
	public void setLastModified(final Date date) {
		lastModified = date;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#getRegAction()
	 */
	@Override
	public String getRegAction() {
		return regAction;
	}

	/**
	 * @see org.olat.registration.TemporaryKey#setRegAction(java.lang.String)
	 */
	@Override
	public void setRegAction(final String string) {
		regAction = string;
	}

	@Override
	public int getVersion() {
		return this.version;
	}

	@Override
	public void setVersion(final int version) {
		this.version = version;
	}
}
