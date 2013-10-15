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
 * Copyright (c) 2008 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.user.notification;

import java.util.Locale;

import org.olat.core.dispatcher.jumpin.JumpInReceptionist;
import org.olat.core.dispatcher.jumpin.JumpInResult;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.UserConstants;
import org.olat.user.UserInfoMainController;

/**
 * Description:<br>
 * Implement a JumpInReceptionist to jump with an Identity to the HomePageDisplayController of this user.
 * <P>
 * Initial Date: 19 august 2009 <br>
 * 
 * @author srosse
 */
public class UserJumpInReceptionist implements JumpInReceptionist {

	private final Locale locale;
	private final Identity identity;

	public UserJumpInReceptionist(final Identity identity, final Locale locale) {
		this.locale = locale;
		this.identity = identity;
	}

	/**
	 * Return the last name of the user
	 */
	@Override
	public String getTitle() {
		return identity.getUser().getProperty(UserConstants.LASTNAME, locale);
	}

	@Override
	public OLATResourceable getOLATResourceable() {
		return new IdentityResourceable(identity.getKey());
	}

	/**
	 * Return the UserInfoMainController packed in a JumpInResult
	 */
	@Override
	public JumpInResult createJumpInResult(final UserRequest ureq, final WindowControl wControl) {
		final Controller homePageController = new UserInfoMainController(ureq, wControl, identity);
		return new JumpInResult(homePageController, null);
	}

	@Override
	public String extractActiveViewId(final UserRequest ureq) {
		final String id = ureq.getParameter(UserJumpInHandlerFactory.CONST_IDENTITY_ID);
		return id;
	}

	public class IdentityResourceable implements OLATResourceable {
		private final Long resourceableId;

		public IdentityResourceable(final Long resourceableId) {
			this.resourceableId = resourceableId;
		}

		@Override
		public Long getResourceableId() {
			return resourceableId;
		}

		@Override
		public String getResourceableTypeName() {
			return "Identity";
		}
	}
}
