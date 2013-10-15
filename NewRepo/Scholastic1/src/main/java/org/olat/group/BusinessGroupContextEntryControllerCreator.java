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
package org.olat.group;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.ContextEntryControllerCreator;
import org.olat.group.ui.BGControllerFactory;

/**
 * <h3>Description:</h3>
 * <p>
 * This class can create run controllers for business groups for a given context entry
 * <p>
 * Initial Date: 19.08.2009 <br>
 * 
 * @author gnaegi, gnaegi@frentix.com, www.frentix.com
 */
public class BusinessGroupContextEntryControllerCreator implements ContextEntryControllerCreator {

	/**
	 * @see org.olat.core.id.context.ContextEntryControllerCreator#createController(org.olat.core.id.context.ContextEntry, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createController(final ContextEntry ce, final UserRequest ureq, final WindowControl wControl) {
		final OLATResourceable ores = ce.getOLATResourceable();

		final Long gKey = ores.getResourceableId();
		final BusinessGroupManager bman = BusinessGroupManagerImpl.getInstance();
		final BusinessGroup bgroup = bman.loadBusinessGroup(gKey, true);
		Controller ctrl = null;
		final boolean isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		// check if allowed to start (must be member or admin)
		if (isOlatAdmin || bman.isIdentityInBusinessGroup(ureq.getIdentity(), bgroup)) {
			// only olatadmins or admins of this group can administer this group
			ctrl = BGControllerFactory.getInstance().createRunControllerFor(ureq, wControl, bgroup, isOlatAdmin, null);
		}
		return ctrl;
	}

	/**
	 * @see org.olat.core.id.context.ContextEntryControllerCreator#getTabName(org.olat.core.id.context.ContextEntry)
	 */
	@Override
	public String getTabName(final ContextEntry ce) {
		final OLATResourceable ores = ce.getOLATResourceable();
		final Long gKey = ores.getResourceableId();
		final BusinessGroupManager bman = BusinessGroupManagerImpl.getInstance();
		final BusinessGroup bgroup = bman.loadBusinessGroup(gKey, true);
		return bgroup.getName();
	}

	/**
	 * @see org.olat.core.id.context.ContextEntryControllerCreator#getSiteClassName(org.olat.core.id.context.ContextEntry)
	 */
	@Override
	public String getSiteClassName(final ContextEntry ce) {
		return null;
	}

	@Override
	public boolean validateContextEntryAndShowError(final ContextEntry ce, final UserRequest ureq, final WindowControl wControl) {
		final OLATResourceable ores = ce.getOLATResourceable();
		final Long gKey = ores.getResourceableId();
		final BusinessGroupManager bman = BusinessGroupManagerImpl.getInstance();
		final BusinessGroup bgroup = bman.loadBusinessGroup(gKey, false);
		return bgroup != null;
	}

}
