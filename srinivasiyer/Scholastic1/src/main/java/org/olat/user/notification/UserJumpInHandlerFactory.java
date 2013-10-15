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

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.dispatcher.DispatcherAction;
import org.olat.core.dispatcher.jumpin.JumpInHandlerFactory;
import org.olat.core.dispatcher.jumpin.JumpInReceptionist;
import org.olat.core.gui.UserRequest;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;

/**
 * Description:<br>
 * Implement a JumpInHandlerFactory to jump with an Identity to the HomePageDisplayController of this user.
 * <P>
 * Initial Date: 19 august 2009 <br>
 * 
 * @author srosse
 */
public class UserJumpInHandlerFactory implements JumpInHandlerFactory {

	public static final String CONST_EXTLINK = "user/go";
	public static final String CONST_IDENTITY_ID = "idenid";

	@Override
	public JumpInReceptionist createJumpInHandler(final UserRequest ureq) {
		final String identityId = ureq.getParameter(UserJumpInHandlerFactory.CONST_IDENTITY_ID);
		if (identityId == null) { return new UserAdminJumpInReceptionist(ureq.getLocale()); }
		final Long identityKey = Long.parseLong(identityId);
		final Identity identity = BaseSecurityManager.getInstance().loadIdentityByKey(identityKey);
		return new UserJumpInReceptionist(identity, ureq.getLocale());
	}

	/**
	 * Returns a direct jump-in URI for a user home page. e.g. http://olathost.org/olat/auth/user/go?idenid=[IdentityKey]
	 * 
	 * @param identity
	 * @return
	 */
	public static String buildDispatchURI(final Identity identity) {
		return new StringBuilder().append(Settings.getServerContextPathURI()).append(DispatcherAction.PATH_AUTHENTICATED).append(CONST_EXTLINK).append("?")
				.append(CONST_IDENTITY_ID).append("=").append(identity.getKey()).toString();
	}

	/**
	 * Returns a direct jump-in URI for a user home page as guest. e.g. http://olathost.org/olat/auth/user/go?idenid=[IdentityKey]&guest=true
	 * 
	 * @param identity
	 * @return
	 */
	public static String buildDispatchGuestURI(final Identity identity) {
		return new StringBuilder().append(buildDispatchURI(identity)).append("&guest=true").toString();
	}
}
