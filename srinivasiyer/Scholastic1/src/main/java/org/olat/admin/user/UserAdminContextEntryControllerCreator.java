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
package org.olat.admin.user;

import org.olat.admin.site.UserAdminSite;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.ContextEntryControllerCreator;

/**
 * Description:<br>
 * This class offer a way to launch the UserAdminSite
 * <P>
 * Initial Date: 4 sept. 2009 <br>
 * 
 * @author srosse
 */
public class UserAdminContextEntryControllerCreator implements ContextEntryControllerCreator {

	/**
	 * @see org.olat.core.id.context.ContextEntryControllerCreator#createController(org.olat.core.id.context.ContextEntry, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createController(final ContextEntry ce, final UserRequest ureq, final WindowControl wControl) {
		return null;
	}

	/**
	 * @see org.olat.core.id.context.ContextEntryControllerCreator#getSiteClassName(org.olat.core.id.context.ContextEntry)
	 */
	@Override
	public String getSiteClassName(final ContextEntry ce) {
		// opened as site not tab
		return UserAdminSite.class.getName();
	}

	/**
	 * @see org.olat.core.id.context.ContextEntryControllerCreator#getTabName(org.olat.core.id.context.ContextEntry)
	 */
	@Override
	public String getTabName(final ContextEntry ce) {
		return null;
	}

	@Override
	public boolean validateContextEntryAndShowError(final ContextEntry ce, final UserRequest ureq, final WindowControl wControl) {
		return true;
	}
}
