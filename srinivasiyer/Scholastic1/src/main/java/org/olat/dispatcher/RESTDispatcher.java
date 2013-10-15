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
package org.olat.dispatcher;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.olat.basesecurity.AuthHelper;
import org.olat.core.CoreSpringFactory;
import org.olat.core.dispatcher.Dispatcher;
import org.olat.core.dispatcher.DispatcherAction;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Window;
import org.olat.core.gui.control.ChiefController;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.id.Identity;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.UserSession;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;
import org.olat.login.LoginModule;
import org.olat.restapi.security.RestSecurityBean;
import org.olat.restapi.security.RestSecurityHelper;

/**
 * Description:<br>
 * Entry point for Resource URL's which are a replacement for the jumpIn / Go Repo style URL's. The assumption is, that the URL here set up from a list of
 * BusinessControls containing a (type/resource)name and an (type/resource)id of type long.</br> e.g.
 * [RepoyEntry:12323123][CourseNode:2341231456][message:123123][blablup:555555] which is mapped to</br>
 * /RepoyEntry/12323123/CourseNode/2341231456/message/123123/blablup/555555/</p> This dispatcher does the reverse mapping and creation of a list of BusinessControls which
 * can be used to activate/spawn the Controller. The same mechanism is used for lucene search engine and the activation of search results.
 * <p>
 * This dispatcher supports also a simple single sign-on-mechanism (SS). If an URL contains the parameter X-OLAT-TOKEN, the RestSecurityBean will be used to look up the
 * associated user. You can use the REST API to create such a X-OLAT-TOKEN or replace the RestSecurityBean with your own implementation that creates the tokens. Please
 * refere to the REST API documentation on how to create the X-OLAT-TOKEN <br />
 * Example: [RepoyEntry:12323123][CourseNode:2341231456][message:123123][blablup:555555]?X-OLAT-TOKEN=xyz
 * <P>
 * TODO:pb:2009-06-02: (1) Check for Authenticated Session, otherwise send over login page (2) UZHDisparcher has a security check for use of SSL -> introduce also here or
 * maybe bring the check into webapphelper.
 * <P>
 * Initial Date: 24.04.2009 <br>
 * 
 * @author patrickb
 */
public class RESTDispatcher implements Dispatcher {
	private static final OLog log = Tracing.createLoggerFor(RESTDispatcher.class);

	@Override
	public void execute(final HttpServletRequest request, final HttpServletResponse response, final String uriPrefix) {
		//
		// create a ContextEntries String which can be used to create a BusinessControl -> move to
		//
		final String origUri = request.getRequestURI();
		String restPart = origUri.substring(uriPrefix.length());
		try {
			restPart = URLDecoder.decode(restPart, "UTF8");
		} catch (final UnsupportedEncodingException e) {
			log.error("Unsupported encoding", e);
		}

		final String[] split = restPart.split("/");
		if (split.length % 2 != 0) {
			// assert(split.length % 2 == 0);
			// The URL is not a valid business path
			DispatcherAction.sendBadRequest(origUri, response);
			log.warn("URL is not valid: " + restPart);
			return;
		}
		String businessPath = "";
		for (int i = 0; i < split.length; i = i + 2) {
			String key = split[i];
			if (key != null && key.startsWith("path=")) {
				key = key.replace("~~", "/");
			}
			final String value = split[i + 1];
			businessPath += "[" + key + ":" + value + "]";
		}
		if (log.isDebug()) {
			log.debug("REQUEST URI: " + origUri);
			log.debug("REQUEST PREFIX " + restPart);
			log.debug("calc buspath " + businessPath);
		}

		// check if the businesspath is valid
		try {
			final BusinessControl bc = BusinessControlFactory.getInstance().createFromString(businessPath);
			if (!bc.hasContextEntry()) {
				// The URL is not a valid business path
				DispatcherAction.sendBadRequest(origUri, response);
				return;
			}
		} catch (final Exception e) {
			DispatcherAction.sendBadRequest(origUri, response);
			log.warn("Error with business path: " + origUri, e);
			return;
		}

		//
		// create the olat ureq and get an associated main window to spawn the "tab"
		//
		final UserSession usess = UserSession.getUserSession(request);
		UserRequest ureq = null;
		try {
			// upon creation URL is checked for
			ureq = new UserRequest(uriPrefix, request, response);
		} catch (final NumberFormatException nfe) {
			// MODE could not be decoded
			// typically if robots with wrong urls hit the system
			// or user have bookmarks
			// or authors copy-pasted links to the content.
			// showing redscreens for non valid URL is wrong instead
			// a 404 message must be shown -> e.g. robots correct their links.
			if (log.isDebug()) {
				log.debug("Bad Request " + request.getPathInfo());
			}
			DispatcherAction.sendBadRequest(request.getPathInfo(), response);
			return;
		}
		// XX:GUIInterna.setLoadPerformanceMode(ureq);

		// Do auto-authenticate if url contains a X-OLAT-TOKEN Single-Sign-On REST-Token
		final String xOlatToken = ureq.getParameter(RestSecurityHelper.SEC_TOKEN);
		if (xOlatToken != null) {
			// Lookup identity that is associated with this token
			final RestSecurityBean securityBean = (RestSecurityBean) CoreSpringFactory.getBean(RestSecurityBean.class);
			final Identity restIdentity = securityBean.getIdentity(xOlatToken);
			//
			if (log.isDebug()) {
				if (restIdentity == null) {
					log.debug("Found SSO token " + RestSecurityHelper.SEC_TOKEN + " in url, but token is not bound to an identity");
				} else {
					log.debug("Found SSO token " + RestSecurityHelper.SEC_TOKEN + " in url which is bound to identity::" + restIdentity.getName());
				}
			}
			//
			if (restIdentity != null) {
				// Test if the current OLAT session does already belong to this user.
				// The session could be an old session from another user or it could
				// belong to this user but miss the window object because so far it was
				// a head-less REST session. REST sessions initially have a small
				// timeout, however OLAT does set the standard session timeout on each
				// UserSession.getSession() request. This means, the normal session
				// timeout is set in the redirect request that will happen immediately
				// after the REST dispatcher finishes. No need to change it here.
				if (!usess.isAuthenticated() || !restIdentity.equalsByPersistableKey(usess.getIdentity())) {
					// Re-authenticate user session for this user and start a fresh
					// standard OLAT session
					AuthHelper.doLogin(restIdentity, RestSecurityHelper.SEC_TOKEN, ureq);
				} else if (Windows.getWindows(usess).getAttribute("AUTHCHIEFCONTROLLER") == null) {
					// Session is already available, but no main window (Head-less REST
					// session). Only create the base chief controller and the window
					AuthHelper.createAuthHome(ureq);
				}
			}
		}

		final boolean auth = usess.isAuthenticated();
		if (auth) {
			usess.putEntryInNonClearedStore(AuthenticatedDispatcher.AUTHDISPATCHER_BUSINESSPATH, businessPath);

			final String url = getRedirectToURL(usess);
			DispatcherAction.redirectTo(response, url);
		} else {
			// prepare for redirect
			usess.putEntryInNonClearedStore(AuthenticatedDispatcher.AUTHDISPATCHER_BUSINESSPATH, businessPath);
			final String invitationAccess = ureq.getParameter(AuthenticatedDispatcher.INVITATION);

			if (invitationAccess != null && LoginModule.isInvitationEnabled()) {
				// try to log in as anonymous
				// use the language from the lang paramter if available, otherwhise use the system default locale
				final Locale guestLoc = getLang(ureq);
				final int loginStatus = AuthHelper.doInvitationLogin(invitationAccess, ureq, guestLoc);
				if (loginStatus == AuthHelper.LOGIN_OK) {
					// logged in as invited user, continue
					final String url = getRedirectToURL(usess);
					DispatcherAction.redirectTo(response, url);
				} else if (loginStatus == AuthHelper.LOGIN_NOTAVAILABLE) {
					DispatcherAction.redirectToServiceNotAvailable(response);
				} else {
					// error, redirect to login screen
					DispatcherAction.redirectToDefaultDispatcher(response);
				}
			} else {
				final String guestAccess = ureq.getParameter(AuthenticatedDispatcher.GUEST);
				if (guestAccess == null || !LoginModule.isGuestLoginLinksEnabled()) {
					DispatcherAction.redirectToDefaultDispatcher(response);
					return;
				} else if (guestAccess.equals(AuthenticatedDispatcher.TRUE)) {
					// try to log in as anonymous
					// use the language from the lang paramter if available, otherwhise use the system default locale
					final Locale guestLoc = getLang(ureq);
					final int loginStatus = AuthHelper.doAnonymousLogin(ureq, guestLoc);
					if (loginStatus == AuthHelper.LOGIN_OK) {
						// logged in as anonymous user, continue
						final String url = getRedirectToURL(usess);
						DispatcherAction.redirectTo(response, url);
					} else if (loginStatus == AuthHelper.LOGIN_NOTAVAILABLE) {
						DispatcherAction.redirectToServiceNotAvailable(response);
					} else {
						// error, redirect to login screen
						DispatcherAction.redirectToDefaultDispatcher(response);
					}
				}
			}
		}
	}

	private Locale getLang(final UserRequest ureq) {
		// try to log in as anonymous
		// use the language from the lang paramter if available, otherwhise use the system default locale
		final String guestLang = ureq.getParameter("lang");
		Locale guestLoc;
		if (guestLang == null) {
			guestLoc = I18nModule.getDefaultLocale();
		} else {
			guestLoc = I18nManager.getInstance().getLocaleOrDefault(guestLang);
		}
		return guestLoc;
	}

	private String getRedirectToURL(final UserSession usess) {
		final ChiefController cc = (ChiefController) Windows.getWindows(usess).getAttribute("AUTHCHIEFCONTROLLER");
		final Window w = cc.getWindow();

		final URLBuilder ubu = new URLBuilder("", w.getInstanceId(), String.valueOf(w.getTimestamp()), null);
		final StringOutput sout = new StringOutput(30);
		ubu.buildURI(sout, null, null);

		return WebappHelper.getServletContextPath() + DispatcherAction.PATH_AUTHENTICATED + sout.toString();
	}
}
