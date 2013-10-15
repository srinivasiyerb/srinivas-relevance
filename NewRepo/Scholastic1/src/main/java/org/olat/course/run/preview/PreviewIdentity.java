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

package org.olat.course.run.preview;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.olat.core.id.Identity;
import org.olat.core.id.Persistable;
import org.olat.core.id.Preferences;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.AssertException;

/**
 * Initial Date: 08.02.2005
 * 
 * @author Mike Stock
 */
final class PreviewIdentity implements Identity {

	/**
	 * @see org.olat.core.id.Identity#getName()
	 */
	@Override
	public String getName() {
		return "JaneDoe";
	}

	/**
	 * @see org.olat.core.id.Identity#getUser()
	 */
	@Override
	public User getUser() {
		return new User() {
			Map<String, String> data = new HashMap<String, String>();
			private Map<String, String> envAttrs;
			{
				data.put(UserConstants.FIRSTNAME, "Jane");
				data.put(UserConstants.LASTNAME, "Doe");
				data.put(UserConstants.EMAIL, "jane.doe@testmail.com");
			}

			@Override
			public Long getKey() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			@SuppressWarnings("unused")
			public boolean equalsByPersistableKey(final Persistable persistable) {
				// TODO Auto-generated method stub
				return false;
			}

			public Date getLastModified() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Date getCreationDate() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			@SuppressWarnings("unused")
			public void setProperty(final String propertyName, final String propertyValue) {
				// TODO Auto-generated method stub

			}

			@Override
			@SuppressWarnings("unused")
			public void setPreferences(final Preferences prefs) {
				// TODO Auto-generated method stub

			}

			@Override
			@SuppressWarnings("unused")
			public String getProperty(final String propertyName, final Locale locale) {
				return data.get(propertyName);
			}

			@Override
			public void setIdentityEnvironmentAttributes(final Map<String, String> identEnvAttribs) {
				this.envAttrs = identEnvAttribs;
			}

			@Override
			public String getPropertyOrIdentityEnvAttribute(final String propertyName, final Locale locale) {
				String retVal = null;
				retVal = data.get(propertyName);
				if (retVal == null && this.envAttrs != null) {
					retVal = envAttrs.get(propertyName);
				}
				return retVal;
			}

			@Override
			public Preferences getPreferences() {
				// TODO Auto-generated method stub
				return null;
			}

		};
	}

	/**
	 * @see org.olat.core.commons.persistence.Auditable#getCreationDate()
	 */
	@Override
	public Date getCreationDate() {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.core.commons.persistence.Auditable#getLastModified()
	 */
	public Date getLastModified() {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.core.commons.persistence.Persistable#getKey()
	 */
	@Override
	public Long getKey() {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.core.commons.persistence.Persistable#equalsByPersistableKey(org.olat.core.commons.persistence.Persistable)
	 */
	@Override
	public boolean equalsByPersistableKey(final Persistable persistable) {
		throw new AssertException("unsupported");
	}

	@Override
	public Date getLastLogin() {
		throw new AssertException("unsupported");
	}

	@Override
	public void setLastLogin(final Date loginDate) {
		throw new AssertException("unsupported");
	}

	@Override
	public Integer getStatus() {
		throw new AssertException("unsupported");
	}

	@Override
	public void setStatus(final Integer newStatus) {
		throw new AssertException("unsupported");
	}

	public Date getDeleteEmailDate() {
		throw new AssertException("unsupported");
	}

	public void setDeleteEmailDate(final Date newDeleteEmail) {
		throw new AssertException("unsupported");
	}

	@Override
	public void setName(final String loginName) {

	}

}