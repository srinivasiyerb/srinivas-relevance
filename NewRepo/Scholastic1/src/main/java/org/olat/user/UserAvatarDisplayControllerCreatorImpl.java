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
package org.olat.user;

import org.olat.core.commons.creator.UserAvatarDisplayControllerCreator;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;

/**
 * Description:<br>
 * OLAT implementation for the avatar controller: uses the display controller which offers a link to the users homepage
 * <p>
 * This interface/creator construct is mainly used to decouple olatcore from olat itself. Using this controller creator features from olat can be used from within the
 * core using the spring bean.
 * <P>
 * Initial Date: 01.12.2009 <br>
 * 
 * @author gnaegi
 */
public class UserAvatarDisplayControllerCreatorImpl extends UserAvatarDisplayControllerCreator {

	/**
	 * @see org.olat.core.commons.controllers.user.UserAvatarDisplayControllerCreator#createController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, org.olat.core.id.Identity, boolean, boolean)
	 */
	@Override
	public Controller createController(final UserRequest ureq, final WindowControl wControl, final Identity identity, final boolean useLarge,
			final boolean canLinkToHomePage) {
		return new DisplayPortraitController(ureq, wControl, identity, useLarge, canLinkToHomePage);
	}

}
