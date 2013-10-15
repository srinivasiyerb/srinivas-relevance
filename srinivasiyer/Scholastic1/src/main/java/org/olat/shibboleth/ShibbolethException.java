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
package org.olat.shibboleth;

/**
 * Description:<br>
 * TODO: Lavinia Dumitrescu Class Description for ShibbolethException
 * <P>
 * Initial Date: 31.10.2007 <br>
 * 
 * @author Lavinia Dumitrescu
 */
public class ShibbolethException extends Exception {

	// error codes
	public static final int GENERAL_SAML_ERROR = 0;
	public static final int UNIQUE_ID_NOT_FOUND = 1;
	public static final int INSUFFICIENT_ATTRIBUTES = 2;

	private final int errorCode;
	private String contactPersonEmail;

	public ShibbolethException(final int errorCode, final String msg) {
		super(msg);
		this.errorCode = errorCode;
	}

	public ShibbolethException(final int errorCode, final Throwable throwable) {
		super(throwable.getMessage());
		this.errorCode = errorCode;
	}

	public ShibbolethException(final int errorCode, final String contactPersonEmail, final Throwable throwable) {
		this(errorCode, throwable);
		this.contactPersonEmail = contactPersonEmail;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getContactPersonEmail() {
		return contactPersonEmail;
	}

	public void setContactPersonEmail(final String contactPersonEmail) {
		this.contactPersonEmail = contactPersonEmail;
	}
}
