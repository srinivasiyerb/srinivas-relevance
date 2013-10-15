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
package org.olat.restapi.security;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.core.gui.UserRequest;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.util.i18n.I18nModule;
import org.olat.course.ICourse;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.groupsandrights.CourseRights;
import org.olat.dispatcher.LocaleNegotiator;

/**
 * Description:<br>
 * TODO: srosse Class Description for RestSecurityHelper
 * <P>
 * Initial Date: 7 apr. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
public class RestSecurityHelper {

	public static final String SUB_CONTEXT = "/restapibeta";
	public static final String SEC_TOKEN = "X-OLAT-TOKEN";
	public static final String SEC_USER_REQUEST = "olat-user-request";

	public static UserRequest getUserRequest(final HttpServletRequest request) {
		return (UserRequest) request.getAttribute(SEC_USER_REQUEST);
	}

	public static Identity getIdentity(final HttpServletRequest request) {
		final UserRequest ureq = (UserRequest) request.getAttribute(SEC_USER_REQUEST);
		if (ureq == null) { return null; }
		return ureq.getIdentity();
	}

	public static boolean isUserManager(final HttpServletRequest request) {
		try {
			final Roles roles = getRoles(request);
			return (roles.isUserManager() || roles.isOLATAdmin());
		} catch (final Exception e) {
			return false;
		}
	}

	public static boolean isGroupManager(final HttpServletRequest request) {
		try {
			final Roles roles = getRoles(request);
			return (roles.isGroupManager() || roles.isOLATAdmin());
		} catch (final Exception e) {
			return false;
		}
	}

	public static boolean isAuthor(final HttpServletRequest request) {
		try {
			final Roles roles = getRoles(request);
			return (roles.isAuthor() || roles.isOLATAdmin());
		} catch (final Exception e) {
			return false;
		}
	}

	public static boolean isAuthorEditor(final ICourse course, final HttpServletRequest request) {
		try {
			final Roles roles = getRoles(request);
			if (roles.isOLATAdmin()) { return true; }
			if (roles.isAuthor()) {
				final UserRequest ureq = getUserRequest(request);
				final Identity identity = ureq.getIdentity();
				final CourseGroupManager cgm = course.getCourseEnvironment().getCourseGroupManager();
				return cgm.isIdentityCourseAdministrator(identity) || cgm.hasRight(identity, CourseRights.RIGHT_COURSEEDITOR);
			}
			return false;
		} catch (final Exception e) {
			return false;
		}
	}

	public static boolean isAuthorEditor(final OLATResourceable resourceable, final HttpServletRequest request) {
		try {
			final Roles roles = getRoles(request);
			if (roles.isOLATAdmin()) { return true; }
			if (roles.isAuthor()) {
				final UserRequest ureq = getUserRequest(request);
				final Identity identity = ureq.getIdentity();
				final BaseSecurity secMgr = BaseSecurityManager.getInstance();
				return secMgr.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_ADMIN, resourceable);
			}
			return false;
		} catch (final Exception e) {
			return false;
		}
	}

	public static boolean isAuthorGrpManager(final ICourse course, final HttpServletRequest request) {
		try {
			final Roles roles = getRoles(request);
			if (roles.isOLATAdmin()) { return true; }
			if (roles.isAuthor()) {
				final UserRequest ureq = getUserRequest(request);
				final Identity identity = ureq.getIdentity();
				final CourseGroupManager cgm = course.getCourseEnvironment().getCourseGroupManager();
				final boolean editor = cgm.hasRight(identity, CourseRights.RIGHT_GROUPMANAGEMENT);
				return editor;
			}
			return false;
		} catch (final Exception e) {
			return false;
		}
	}

	public static boolean isAdmin(final HttpServletRequest request) {
		try {
			final Roles roles = getRoles(request);
			return roles.isOLATAdmin();
		} catch (final Exception e) {
			return false;
		}
	}

	public static Roles getRoles(final HttpServletRequest request) {
		final UserRequest ureq = (UserRequest) request.getAttribute(SEC_USER_REQUEST);
		if (ureq == null || ureq.getUserSession() == null || ureq.getUserSession().getRoles() == null) {
			// guest roles
			return new Roles(false, false, false, false, true, false, false);
		}
		return ureq.getUserSession().getRoles();
	}

	public static Locale getLocale(final HttpServletRequest request) {
		if (request == null) { return I18nModule.getDefaultLocale(); }
		final UserRequest ureq = (UserRequest) request.getAttribute(SEC_USER_REQUEST);
		if (ureq == null) { return I18nModule.getDefaultLocale(); }
		return LocaleNegotiator.getPreferedLocale(ureq);
	}
}
