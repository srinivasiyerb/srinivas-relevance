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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.id;

import java.util.Date;

/**
 * Initial Date: 24.04.2004
 * 
 * @author Mike Stock
 */
public interface Identity extends CreateInfo, Persistable {

	// status = 1..99 User with this status are visibale (e.g. user search)
	// 100..199 User with this status are invisibale (e.g. user search)
	/** Identity has a permanent olat user account and will be never listen in user-deletion process. */
	public static Integer STATUS_PERMANENT = 1;
	/** Identity has access to olat-system. */
	public static Integer STATUS_ACTIV = 2;
	/** Limit for visible identities, all identities with status < LIMIT will be listed in search etc. */
	public static Integer STATUS_VISIBLE_LIMIT = 100;
	/** Identity can not login and will not be listed (only on login-denied list). */
	public static Integer STATUS_LOGIN_DENIED = 101;
	/** Identity is deleted and has no access to olat-system and is not visible (except administrators). */
	public static Integer STATUS_DELETED = 199;

	/**
	 * @return The username, (login name, nickname..)
	 */
	public abstract String getName();

	/**
	 * @return The user object associated with this identity. The user encapsulates the user data (profile and preferences)
	 */
	public abstract User getUser();

	/**
	 * @return Last date when the user logged in.
	 */
	public Date getLastLogin();

	/**
	 * Set a new last login date for the user.
	 * 
	 * @param loginDate New last-login date.
	 */
	public void setLastLogin(Date loginDate);

	/**
	 * @return Current identity status
	 */
	public Integer getStatus();

	/**
	 * Set new status (aktiv,deleted,permanent) of identity.
	 * 
	 * @param newStatus New status
	 */
	public void setStatus(Integer newStatus);

	public void setName(String name);
}