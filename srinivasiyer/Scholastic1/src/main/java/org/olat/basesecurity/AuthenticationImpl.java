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

package org.olat.basesecurity;

import org.olat.core.commons.persistence.PersistentObject;
import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;

/**
 * Description: <br>
 * 
 * @author Felix Jost
 */
public class AuthenticationImpl extends PersistentObject implements Authentication {

	private Identity identity;
	private String provider;
	private String authusername;
	private String credential;

	/**
	 * for hibernate only
	 */
	protected AuthenticationImpl() {
		//
	}

	AuthenticationImpl(final Identity identity, final String provider, final String authusername, final String credential) {
		if (provider.length() > 8) {
			// this implementation allows only 8 characters, as defined in hibernate file
			throw new AssertException("Authentication provider '" + provider + "' to long, only 8 characters supported!");
		}
		this.identity = identity;
		this.provider = provider;
		this.authusername = authusername;
		this.credential = credential;
	}

	/**
	 * @return
	 */
	@Override
	public String getAuthusername() {
		return authusername;
	}

	/**
	 * @return
	 */
	@Override
	public String getProvider() {
		return provider;
	}

	/**
	 * for hibernate only (can never be changed, but set only)
	 * 
	 * @param string
	 */
	@Override
	public void setAuthusername(final String string) {
		authusername = string;
	}

	/**
	 * for hibernate only (can never be changed, but set only)
	 * 
	 * @param string
	 */
	@Override
	public void setProvider(final String string) {
		provider = string;
	}

	/**
	 * for hibernate only
	 * 
	 * @return
	 */
	@Override
	public String getCredential() {
		return credential;
	}

	/**
	 * @param string
	 */
	@Override
	public void setCredential(final String string) {
		credential = string;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "auth: provider:" + provider + " ,authusername:" + authusername + ", hashpwd:" + credential + " ," + super.toString();
	}

	/**
	 * @see org.olat.basesecurity.Authentication#getIdentity()
	 */
	@Override
	public Identity getIdentity() {
		return identity;
	}

	/**
	 * @see org.olat.basesecurity.Authentication#setIdentity(org.olat.core.id.Identity)
	 */
	@Override
	public void setIdentity(final Identity identity) {
		this.identity = identity;
	}
}