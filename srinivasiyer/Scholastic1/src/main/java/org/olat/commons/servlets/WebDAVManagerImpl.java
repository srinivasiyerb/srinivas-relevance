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

package org.olat.commons.servlets;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.servlets.SecureWebdavServlet;
import org.olat.core.servlets.WebDAVManager;
import org.olat.core.util.SessionInfo;
import org.olat.core.util.UserSession;
import org.olat.core.util.cache.n.CacheWrapper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.login.auth.WebDAVAuthManager;

import com.oreilly.servlet.Base64Decoder;

/**
 * Initial Date: 16.04.2003
 * 
 * @author Mike Stock
 * @author guido Comment:
 */
public class WebDAVManagerImpl extends WebDAVManager {
	private static boolean enabled = true;

	private static final String BASIC_AUTH_REALM = "OLAT WebDAV Access";
	private final CoordinatorManager coordinatorManager;

	private CacheWrapper timedSessionCache;

	/**
	 * [spring]
	 */
	private WebDAVManagerImpl(final CoordinatorManager coordinatorManager) {
		this.coordinatorManager = coordinatorManager;
		INSTANCE = this;
	}

	/**
	 * @see org.olat.commons.servlets.WebDAVManager#handleAuthentication(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected boolean handleAuthentication(final HttpServletRequest req, final HttpServletResponse resp) {
		final UserSession usess = handleBasicAuthentication(req, resp);
		if (usess == null) { return false; }

		// register usersession in REQUEST, not session !!
		// see SecureWebDAVServlet.setAuthor() and checkQuota()
		req.setAttribute(SecureWebdavServlet.REQUEST_USERSESSION_KEY, usess);
		return true;
	}

	/**
	 * @see org.olat.commons.servlets.WebDAVManager#getUserSession(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected UserSession getUserSession(final HttpServletRequest req) {
		return (UserSession) req.getAttribute(SecureWebdavServlet.REQUEST_USERSESSION_KEY);
	}

	private UserSession handleBasicAuthentication(final HttpServletRequest request, final HttpServletResponse response) {

		if (timedSessionCache == null) {
			synchronized (this) {
				timedSessionCache = coordinatorManager.getCoordinator().getCacher().getOrCreateCache(this.getClass(), "webdav");
			}
		}

		// Get the Authorization header, if one was supplied
		final String authHeader = request.getHeader("Authorization");
		if (authHeader != null) {
			// fetch user session from a previous authentication
			UserSession usess = (UserSession) timedSessionCache.get(authHeader);
			if (usess != null && usess.isAuthenticated()) { return usess; }

			final StringTokenizer st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				final String basic = st.nextToken();

				// We only handle HTTP Basic authentication
				if (basic.equalsIgnoreCase("Basic")) {
					final String credentials = st.nextToken();

					// This example uses sun.misc.* classes.
					// You will need to provide your own
					// if you are not comfortable with that.
					final String userPass = Base64Decoder.decode(credentials);

					// The decoded string is in the form
					// "userID:password".
					final int p = userPass.indexOf(":");
					if (p != -1) {
						final String userID = userPass.substring(0, p);
						final String password = userPass.substring(p + 1);

						// Validate user ID and password
						// and set valid true if valid.
						// In this example, we simply check
						// that neither field is blank
						final Identity identity = WebDAVAuthManager.authenticate(userID, password);
						if (identity != null) {
							usess = UserSession.getUserSession(request);
							usess.signOffAndClear();
							usess.setIdentity(identity);
							UserDeletionManager.getInstance().setIdentityAsActiv(identity);
							// set the roles (admin, author, guest)
							final Roles roles = BaseSecurityManager.getInstance().getRoles(identity);
							usess.setRoles(roles);
							// set authprovider
							// usess.getIdentityEnvironment().setAuthProvider(OLATAuthenticationController.PROVIDER_OLAT);

							// set session info
							final SessionInfo sinfo = new SessionInfo(identity.getName(), request.getSession());
							final User usr = identity.getUser();
							sinfo.setFirstname(usr.getProperty(UserConstants.FIRSTNAME, null));
							sinfo.setLastname(usr.getProperty(UserConstants.LASTNAME, null));
							sinfo.setFromIP(request.getRemoteAddr());
							sinfo.setFromFQN(request.getRemoteAddr());
							try {
								final InetAddress[] iaddr = InetAddress.getAllByName(request.getRemoteAddr());
								if (iaddr.length > 0) {
									sinfo.setFromFQN(iaddr[0].getHostName());
								}
							} catch (final UnknownHostException e) {
								// ok, already set IP as FQDN
							}
							sinfo.setAuthProvider(BaseSecurityModule.getDefaultAuthProviderIdentifier());
							sinfo.setUserAgent(request.getHeader("User-Agent"));
							sinfo.setSecure(request.isSecure());
							sinfo.setWebDAV(true);
							sinfo.setWebModeFromUreq(null);
							// set session info for this session
							usess.setSessionInfo(sinfo);
							//
							usess.signOn();
							timedSessionCache.put(authHeader, usess);
							return usess;
						}
					}
				}
			}
		}

		// If the user was not validated or the browser does not know about the realm yet, fail with a
		// 401 status code (UNAUTHORIZED) and
		// pass back a WWW-Authenticate header for
		// this servlet.
		//
		// Note that this is the normal situation the
		// first time you access the page. The client
		// web browser will prompt for userID and password
		// and cache them so that it doesn't have to
		// prompt you again.

		response.setHeader("WWW-Authenticate", "Basic realm=\"" + BASIC_AUTH_REALM + "\"");
		response.setStatus(401);
		return null;
	}

	/**
	 * @see org.olat.core.servlets.WebDAVManager#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Spring setter method to enable/disable the webDAV module
	 * 
	 * @param enabled
	 */
	public void setEnabled(final boolean enabled) {
		WebDAVManagerImpl.enabled = enabled;
	}

}