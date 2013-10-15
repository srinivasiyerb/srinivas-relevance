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

import org.olat.core.id.Identity;
import org.olat.core.id.IdentityManager;
import org.olat.core.manager.BasicManager;

/**
 * Description:<br>
 * TODO: patrickb Class Description for IdentityManagerImpl
 * <P>
 * Initial Date: 23.08.2007 <br>
 * 
 * @author patrickb
 */
public class IdentityManagerImpl extends BasicManager implements IdentityManager {

	BaseSecurity im = null;

	/**
	 * for spring
	 */
	private IdentityManagerImpl(final BaseSecurity im) {
		this.im = im;
	}

	/**
	 * @see org.olat.core.id.IdentityManager#findIdentityByName(java.lang.String)
	 */
	@Override
	public Identity findIdentityByName(final String userName) {
		return im.findIdentityByName(userName);
	}

}
