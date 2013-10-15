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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.log4j.Logger;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.control.Disposable;
import org.olat.core.gui.control.Event;
import org.olat.core.id.Identity;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.Tracing;
import org.olat.core.logging.activity.CoreLoggingResourceable;
import org.olat.core.logging.activity.OlatLoggingAction;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.logging.activity.ThreadLocalUserActivityLoggerInstaller;
import org.olat.core.logging.activity.UserActivityLoggerImpl;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.EventBus;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.prefs.Preferences;
import org.olat.core.util.prefs.PreferencesFactory;
import org.olat.core.util.resource.OresHelper;

/**
 * Description: <BR/>
 * the httpsession contains an instance of this class. the UserSession is either authenticated or not; and if it is, then it also contains things like the Identity, the
 * locale etc. of the current user.
 * <P/>
 * 
 * @author Felix Jost
 */
public class UserSession implements HttpSessionBindingListener, GenericEventListener, Serializable {
	private static final String USERSESSIONKEY = UserSession.class.getName();
	public static final OLATResourceable ORES_USERSESSION = OresHelper.createOLATResourceableType(UserSession.class);
	public static final String STORE_KEY_KILLED_EXISTING_SESSION = "killedExistingSession";

	// clusterNOK cache ??
	private static Set<UserSession> authUserSessions = new HashSet<UserSession>(101);
	private static Map<String, Identity> userNameToIdentity = new HashMap<String, Identity>(101);
	private static int sessionTimeoutInSec = 1800;
	private static Set<String> authUsersNamesOtherNodes = new HashSet<String>(101);

	// things to put into that should not be clear when signing on (e.g. remember
	// url for a direct jump)
	private Map nonClearedStore = new HashMap();

	// the environment (identity, locale, ..) of the identity
	private IdentityEnvironment identityEnvironment;
	private SessionInfo sessionInfo = null;
	private Map store = null;
	private boolean authenticated = false;
	private boolean registeredWithBus = false;
	private Preferences guiPreferences;
	private EventBus singleUserSystemBus;

	// brasato:: find a better place?
	private Map<String, Object> sessionServiceInstances;

	private UserSession() {
		// System.out.println("<init> UserSession... hash="+hashCode());
		// Logger.getLogger(getClass().getName()).debug("UserSession<init> START");
		// (new Exception("UserSession<init>")).printStackTrace(System.out);
		init();
		// usersession is listening for SignOnOffEvents from other clusternodes
		CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(this, null, ORES_USERSESSION);
		registeredWithBus = true;
		Logger.getLogger(getClass().getName()).debug("UserSession<init> END");
	}

	/**
	 * 
	 */
	private void init() {
		store = new HashMap(4);
		identityEnvironment = new IdentityEnvironment();
		singleUserSystemBus = CoordinatorManager.getInstance().getCoordinator().createSingleUserInstance();
		authenticated = false;
		sessionInfo = null;
		sessionServiceInstances = new HashMap<String, Object>();
	}

	/**
	 * @param session
	 * @return associated user session
	 */
	public static UserSession getUserSession(HttpSession session) {
		UserSession us;
		synchronized (session) {// o_clusterOK by:fj
			us = (UserSession) session.getAttribute(USERSESSIONKEY);
			if (us == null) {
				us = new UserSession();
				session.setAttribute(USERSESSIONKEY, us); // triggers the
				// valueBoundEvent -> nothing
				// more to do here
			}
		}
		// set a possible changed session timeout interval
		session.setMaxInactiveInterval(UserSession.sessionTimeoutInSec);
		return us;
	}

	/**
	 * @param hreq
	 * @return associated user session
	 */
	public static UserSession getUserSession(HttpServletRequest hreq) {
		// get existing or create new session
		return getUserSession(hreq.getSession(true));
	}

	/**
	 * Return the UserSession of the given request if it is already set or null otherwise
	 * 
	 * @param hreq
	 * @return
	 */
	public static UserSession getUserSessionIfAlreadySet(HttpServletRequest hreq) {
		HttpSession session = hreq.getSession(false);
		if (session == null) { return null; }
		session.setMaxInactiveInterval(UserSession.sessionTimeoutInSec);
		synchronized (session) {// o_clusterOK by:se
			return (UserSession) session.getAttribute(USERSESSIONKEY);
		}
	}

	/**
	 * @return true if is authenticated
	 */
	public boolean isAuthenticated() {
		return authenticated;
	}

	/**
	 * @param key
	 * @param o
	 */
	public void putEntry(String key, Object o) {
		store.put(key, o);
	}

	/**
	 * @param key
	 * @return entry
	 */
	public Object getEntry(String key) {
		if (key == null) return null;
		if (store.get(key) != null) return store.get(key);
		if (nonClearedStore.get(key) != null) return nonClearedStore.get(key);
		else return null;
	}

	/**
	 * @param key
	 * @return removed entry
	 */
	public Object removeEntry(Object key) {
		return store.remove(key);
	}

	/**
	 * put an entry in the usersession that even survives login/logouts from the users. needed e.g. for a direct jump url, when the url is remembered in the dmz, but used
	 * in auth. since a login occurs, all data from the previous user will be cleared, that is why we introduced this store.
	 * 
	 * @param key
	 * @param o
	 */
	public void putEntryInNonClearedStore(Object key, Object o) {
		nonClearedStore.put(key, o);
	}

	/**
	 * @param key
	 * @return removed entry
	 */
	public Object removeEntryFromNonClearedStore(Object key) {
		return nonClearedStore.remove(key);
	}

	/**
	 * @return Locale
	 */
	public Locale getLocale() {
		Locale locale = identityEnvironment.getLocale();
		return locale;
	}

	/**
	 * @return Identity
	 */
	public Identity getIdentity() {
		return identityEnvironment.getIdentity();
	}

	/**
	 * Sets the locale.
	 * 
	 * @param locale The locale to set
	 */
	public void setLocale(Locale locale) {
		identityEnvironment.setLocale(locale);
	}

	/**
	 * Sets the identity.
	 * 
	 * @param identity The identity to set
	 */
	public void setIdentity(Identity identity) {
		identityEnvironment.setIdentity(identity);
	}

	/**
	 * @return Roles
	 */
	public Roles getRoles() {
		Roles result = identityEnvironment.getRoles();
		if (result == null) {
			Tracing.logWarn("getRoles: null, this=" + this, new RuntimeException("getRoles"), UserSession.class);
		}
		return result;
	}

	/**
	 * Sets the roles.
	 * 
	 * @param roles The roles to set
	 */
	public void setRoles(Roles roles) {
		identityEnvironment.setRoles(roles);
	}

	/**
	 * @see javax.servlet.http.HttpSessionBindingListener#valueBound(javax.servlet.http.HttpSessionBindingEvent)
	 */
	@Override
	public void valueBound(HttpSessionBindingEvent be) {
		if (Tracing.isDebugEnabled(UserSession.class)) {
			Tracing.logDebug("Opened UserSession:" + this.toString(), UserSession.class);
		}
	}

	/**
	 * called when the session is invalidated either by app. server timeout or manual session.invalidate (logout)
	 * 
	 * @see javax.servlet.http.HttpSessionBindingListener#valueUnbound(javax.servlet.http.HttpSessionBindingEvent)
	 */
	@Override
	public void valueUnbound(HttpSessionBindingEvent be) {
		try {
			// the identity can be null if an loginscreen only session gets invalidated
			// (no user was authenticated yet but a tomcat session was created)
			Identity ident = identityEnvironment.getIdentity();
			signOffAndClear();
			if (Tracing.isDebugEnabled(UserSession.class)) {
				Tracing.logDebug("Closed UserSession: identity = " + (ident == null ? "n/a" : ident.getName()), UserSession.class);
			}
			// we do not have a request in the null case (app. server triggered) and user not yet logged in
			// -> in this case we use the special empty activity logger
			if (ident == null) {
				ThreadLocalUserActivityLoggerInstaller.initEmptyUserActivityLogger();
			}
		} catch (Exception e) {
			// safely retrieve
			Identity ident = identityEnvironment.getIdentity();
			Tracing.logError("exception while session was unbound!", e, UserSession.class);
		}
		// called by tomcat's timer thread -> we need to close!! since the next unbound will be called from the same tomcat-thread
		finally {
			// o_clusterNOK: put into managed transaction wrapper
			DBFactory.getInstance(false).commitAndCloseSession();
		}
	}

	/**
	 * called from signOffAndClear() called from event -> MUEvent the real work to do during sign off but without sending the multiuserevent this is used in case the user
	 * logs in to node1 and was logged in on node2 => node2 catches the sign on event and invalidates the user on node2 "silently", e.g. without firing an event.
	 */
	private void signOffAndClearWithout() {
		Tracing.logDebug("signOffAndClearWithout() START", getClass());
		final Identity ident = identityEnvironment.getIdentity();
		// System.out.println("signOffAndClearWithout, ident="+ident+", hash="+this.hashCode()+", identityenv "+identityEnvironment.hashCode());
		// handle safely
		boolean isDebug = Tracing.isDebugEnabled(UserSession.class);
		if (isDebug) {
			Tracing.logDebug("UserSession:::logging off: " + sessionInfo, this.getClass());
		}

		/**
		 * use not RunnableWithException, as exceptionHandlng is inside the run
		 */
		Runnable run = new Runnable() {
			@Override
			public void run() {
				Object obj = null;
				try {

					// do logging
					if (ident != null) {
						ThreadLocalUserActivityLogger.log(OlatLoggingAction.OLAT_LOGOUT, UserSession.class, CoreLoggingResourceable.wrap(ident));
					} else {
						// System.out.println("identity is null!!!!!!!!!!!!!!!!!!!!!");
					}
					// notify all variables in the store (the values) about the disposal
					// if
					// Disposable

					for (Iterator it_storevals = new ArrayList(store.values()).iterator(); it_storevals.hasNext();) {
						obj = it_storevals.next();
						if (obj instanceof Disposable) {
							// synchronous, since triggered by tomcat session timeout or user
							// click and
							// asynchronous, if kicked out by administrator.
							// we assume synchronous
							// !!!!
							// As a reminder, this .dispose() calls dispose on
							// DefaultController which is synchronized.
							// (Windows/WindowManagerImpl/WindowBackOfficeImpl/BaseChiefController/../
							// dispose()
							// !!!! was important for bug OLAT-3390

							((Disposable) obj).dispose();
						}
					}
				} catch (Exception e) {

					String objtostr = "n/a";
					try {
						objtostr = obj.toString();
					} catch (Exception ee) {
						// ignore
					}
					Tracing.logError("exception in signOffAndClear: while disposing object:" + objtostr, e, UserSession.class);
				}
			}
		};

		ThreadLocalUserActivityLoggerInstaller.runWithUserActivityLogger(run, UserActivityLoggerImpl.newLoggerForValueUnbound(this));

		synchronized (authUserSessions) { // o_clusterOK by:fj
			if (authUserSessions.remove(this)) {
				// remove only from identityEnvironment if found in sessions.
				// see also SIDEEFFECT!! line in signOn(..)
				Identity previousSignedOn = identityEnvironment.getIdentity();
				if (previousSignedOn != null) {
					Tracing.logDebug("signOffAndClearWithout() removing from userNameToIdentity: " + previousSignedOn.getName().toLowerCase(), getClass());
					userNameToIdentity.remove(previousSignedOn.getName().toLowerCase());
				}
			} else {
				if (isDebug) {
					Tracing.logInfo("UserSession already removed! for [" + ident + "]", UserSession.class);
				}
			}
		}
		Tracing.logDebug("signOffAndClearWithout() END", getClass());
	}

	/**
	 * called to make sure the current authenticated user (if there is one at all) is cleared and signed off. This method is firing the SignOnOffEvent Multiuserevent.
	 */
	public synchronized void signOffAndClear() { // o_clusterOK by:fj
		Tracing.logDebug("signOffAndClear() START", getClass());
		//
		signOffAndClearWithout();
		// handle safely
		try {
			if (isAuthenticated()) {
				Identity identity = identityEnvironment.getIdentity();
				Tracing.logAudit("Logged off: " + sessionInfo, this.getClass());
				CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new SignOnOffEvent(identity, false), ORES_USERSESSION);
				Tracing.logDebug("signOffAndClear() deregistering usersession from eventbus, id=" + sessionInfo, getClass());
				CoordinatorManager.getInstance().getCoordinator().getEventBus().deregisterFor(this, ORES_USERSESSION);
				registeredWithBus = false;
			}
		} catch (Exception e) {
			Tracing.logError("exception in signOffAndClear: while sending signonoffevent!", e, UserSession.class);
		}
		// clear all instance variables, set authenticated to false
		init();
		Tracing.logDebug("signOffAndClear() END", getClass());
	}

	/**
	 * prior to calling this method, all instance vars must be set.
	 */
	public synchronized void signOn() {
		// ^^^^^^^^^^^^ Added synchronized to be symmetric with sign off and to
		// fix a possible dead-lock see also OLAT-3390
		Tracing.logDebug("signOn() START", getClass());
		if (isAuthenticated()) throw new AssertException("sign on: already signed on!");
		Identity identity = identityEnvironment.getIdentity();
		if (identity == null) throw new AssertException("identity is null in identityEnvironment!");
		if (sessionInfo == null) throw new AssertException("sessionInfo was null for identity " + identity);
		// String login = identity.getName();
		authenticated = true;

		if (sessionInfo.isWebDAV()) {
			// load user prefs
			guiPreferences = PreferencesFactory.getInstance().getPreferencesFor(identity, identityEnvironment.getRoles().isGuestOnly());

			synchronized (authUserSessions) { // o_clusterOK by:se
				// we're only adding this webdav session to the authUserSessions - not to the userNameToIdentity.
				// userNameToIdentity is only needed for IM which can't do anything with a webdav session
				authUserSessions.add(this);
			}
			Tracing.logAudit("Logged on [via webdav]: " + sessionInfo.toString(), this.getClass());
			return;
		}

		Tracing.logDebug("signOn() authUsersNamesOtherNodes.contains " + identity.getName() + ": " + authUsersNamesOtherNodes.contains(identity.getName()), getClass());

		UserSession invalidatedSession = null;
		synchronized (authUserSessions) { // o_clusterOK by:fj
			// check if allready a session exist for this user
			if ((userNameToIdentity.containsKey(identity.getName().toLowerCase()) || authUsersNamesOtherNodes.contains(identity.getName())) && !sessionInfo.isWebDAV()
					&& !this.getRoles().isGuestOnly()) {
				Tracing.logInfo("Loggin-process II: User has already a session => signOffAndClear existing session", this.getClass());

				invalidatedSession = getUserSessionFor(identity.getName().toLowerCase());
				// remove session to be invalidated
				// SIDEEFFECT!! to signOffAndClear
				// if invalidatedSession is removed from authUserSessions
				// signOffAndClear does not remove the identity.getName().toLowerCase() from the userNameToIdentity
				//
				authUserSessions.remove(invalidatedSession);
			}
			authUserSessions.add(this);
			// user can choose upercase letters in identity name, but this has no effect on the
			// database queries, the login form or the IM account. IM works only with lowercase
			// characters -> map stores values as such
			Tracing.logDebug("signOn() adding to userNameToIdentity: " + identity.getName().toLowerCase(), getClass());
			userNameToIdentity.put(identity.getName().toLowerCase(), identity);
		}
		// load user prefs
		guiPreferences = PreferencesFactory.getInstance().getPreferencesFor(identity, identityEnvironment.getRoles().isGuestOnly());

		if (!registeredWithBus) {
			// OLAT-3706
			CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(this, null, ORES_USERSESSION);
		}

		Tracing.logAudit("Logged on: " + sessionInfo.toString(), this.getClass());
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new SignOnOffEvent(identity, true), ORES_USERSESSION);

		// THE FOLLOWING CHECK MUST BE PLACED HERE NOT TO PRODUCE A DEAD-LOCK WITH SIGNOFFANDCLEAR
		// check if a session from any browser was invalidated (IE has a cookie set per Browserinstance!!)
		if (invalidatedSession != null || authUsersNamesOtherNodes.contains(identity.getName())) {
			// put flag killed-existing-session into session-store to show info-message 'only one session for each user' on user-home screen
			this.putEntry(STORE_KEY_KILLED_EXISTING_SESSION, Boolean.TRUE);
			Tracing.logDebug("signOn() removing from authUsersNamesOtherNodes: " + identity.getName(), getClass());
			authUsersNamesOtherNodes.remove(identity.getName());
			// OLAT-3381 & OLAT-3382
			if (invalidatedSession != null) invalidatedSession.signOffAndClear();
		}

		Tracing.logDebug("signOn() END", getClass());
	}

	/**
	 * Lookup non-webdav UserSession for username.
	 * 
	 * @param userName
	 * @return user-session or null when no session was founded.
	 */
	private UserSession getUserSessionFor(String userName) {
		// do not call from somewhere else then signOffAndClear!!
		Set authUserSessionsCopy = new HashSet(authUserSessions);
		for (Iterator iterator = authUserSessionsCopy.iterator(); iterator.hasNext();) {
			UserSession userSession = (UserSession) iterator.next();
			if (userName.equalsIgnoreCase(userSession.getIdentity().getName()) && userSession.getSessionInfo() != null && !userSession.getSessionInfo().isWebDAV()) { return userSession; }
		}
		return null;
	}

	/**
	 * @param userName
	 * @return the identity or null if no user with userName is currently logged on
	 */
	public static Identity getSignedOnIdentity(String userName) {
		synchronized (authUserSessions) { // o_clusterOK by:fj
			return userNameToIdentity.get(userName.toLowerCase());
		}
	}

	/**
	 * @return set of authenticated active user sessions
	 */
	public static Set getAuthenticatedUserSessions() {
		Set copy;
		synchronized (authUserSessions) { // o_clusterOK by:fj
			copy = new HashSet(authUserSessions);
		}
		return copy;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Session of " + identityEnvironment + ", " + super.toString();
	}

	/**
	 * @return identity environment
	 */
	public IdentityEnvironment getIdentityEnvironment() {
		return identityEnvironment;
	}

	/**
	 * may be null
	 * <p>
	 * 
	 * @return session info object
	 */
	public SessionInfo getSessionInfo() {
		return sessionInfo;
	}

	/**
	 * @param sessionInfo
	 */
	public void setSessionInfo(SessionInfo sessionInfo) {
		this.sessionInfo = sessionInfo;
	}

	/**
	 * @return Returns the userSessionsCnt.
	 */
	public static int getUserSessionsCnt() {
		// clusterNOK ?? return only number of locale sessions ?
		return authUserSessions.size();
	}

	/**
	 * @return Returns the guiPreferences.
	 */
	public Preferences getGuiPreferences() {
		return guiPreferences;
	}

	/**
	 * This is the olatsystembus to broadcast event amongst controllers of a single user only (the one whom this usersession belongs to)
	 * 
	 * @return the olatsystembus for the local user
	 */
	public EventBus getSingleUserEventCenter() {
		return singleUserSystemBus;
	}

	/**
	 * only for SignOffEvents - Usersession keeps book about usernames - WindowManager responsible to dispose controller chain
	 * 
	 * @see org.olat.core.util.event.GenericEventListener#event(org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(Event event) {
		Tracing.logDebug("event() START", getClass());
		SignOnOffEvent se = (SignOnOffEvent) event;
		Tracing.logDebug("event() is SignOnOffEvent. isSignOn=" + se.isSignOn(), getClass());
		if (!se.isEventOnThisNode()) {
			// - signOnOff from other node
			// - Single OLAT Instance is never passing by here.
			if (se.isSignOn()) {
				// it is a logged on event
				// -> remember other nodes logged usernames
				Tracing.logDebug("event() adding to authUsersNamesOtherNodes: " + se.getIdentityName(), getClass());
				authUsersNamesOtherNodes.add(se.getIdentityName());
				if (sessionInfo != null && se.getIdentityName().equals(sessionInfo.getLogin()) && !sessionInfo.isWebDAV() && !this.getRoles().isGuestOnly()) {
					// if this listening UserSession instance is from the same user
					// and it is not a WebDAV Session, and it is not GuestSession
					// => log user off on this node
					this.signOffAndClearWithout();
					Tracing.logDebug("event() deregistering usersession from eventbus, id=" + se.getIdentityName(), getClass());
					CoordinatorManager.getInstance().getCoordinator().getEventBus().deregisterFor(this, ORES_USERSESSION);
					registeredWithBus = false;
					init();
				}
			} else {
				// it is logged off event
				// -> remove from other nodes logged on list.
				Tracing.logDebug("event() removing from authUsersNamesOtherNodes: " + se.getIdentityName(), getClass());
				authUsersNamesOtherNodes.remove(se.getIdentityName());
			}
		}
		Tracing.logDebug("event() END", getClass());
	}

	/**
	 * Invalidate all sessions except admin-sessions.
	 * 
	 * @return Number of invalidated sessions.
	 */
	public static int invalidateAllSessions() {
		Tracing.logDebug("invalidateAllSessions() START", UserSession.class);
		int invalidateCounter = 0;
		Tracing.logAudit("All sessions were invalidated by an administrator", UserSession.class);
		// clusterNOK ?? invalidate only locale sessions ?
		Set iterCopy = new HashSet(authUserSessions);
		for (Iterator iterator = iterCopy.iterator(); iterator.hasNext();) {
			UserSession userSession = (UserSession) iterator.next();
			Roles userRoles = userSession != null ? userSession.getRoles() : null;
			if (userRoles != null && !userRoles.isOLATAdmin()) {
				// do not logout administrators
				try {
					userSession.signOffAndClear();
					invalidateCounter++;
				} catch (Exception ex) {
					// Session already signed off => do nothing and continues
				}
			}
		}
		Tracing.logDebug("invalidateAllSessions() END", UserSession.class);
		return invalidateCounter;
	}

	/**
	 * Invalidate a given number of oldest (last-click-time) sessions except admin-sessions.
	 * 
	 * @param nbrSessions number of sessions whisch will be invalidated
	 * @return Number of invalidated sessions.
	 */
	public static int invalidateOldestSessions(int nbrSessions) {
		int invalidateCounter = 0;
		// 1. Copy authUserSessions in sorted TreeMap
		// This is the Comparator that will be used to sort the TreeSet:
		Comparator sessionComparator = new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				Long long1 = new Long(((UserSession) o1).getSessionInfo().getLastClickTime());
				Long long2 = new Long(((UserSession) o2).getSessionInfo().getLastClickTime());
				return long1.compareTo(long2);
			}
		};
		// clusterNOK ?? invalidate only locale sessions ?
		TreeSet sortedSet = new TreeSet(sessionComparator);
		sortedSet.addAll(authUserSessions);
		int i = 0;
		for (Iterator iterator = sortedSet.iterator(); iterator.hasNext() && i++ < nbrSessions;) {
			try {
				UserSession userSession = (UserSession) iterator.next();
				if (!userSession.getRoles().isOLATAdmin() && !userSession.getSessionInfo().isWebDAV()) {
					userSession.signOffAndClear();
					invalidateCounter++;
				}
			} catch (Throwable th) {
				Tracing.logWarn("Error signOffAndClear ", th, UserSession.class);
			}
		}
		return invalidateCounter;
	}

	/**
	 * set session timeout on http session -
	 * 
	 * @param sessionTimeoutInSec
	 */
	public static void setGlobalSessionTimeout(int sessionTimeoutInSec) {
		UserSession.sessionTimeoutInSec = sessionTimeoutInSec;
		Set<UserSession> sessionSnapShot = new HashSet<UserSession>(authUserSessions);
		for (UserSession session : sessionSnapShot) {
			try {
				SessionInfo sessionInfo2 = session.getSessionInfo();
				if (sessionInfo2 != null) {
					sessionInfo2.getSession().setMaxInactiveInterval(sessionTimeoutInSec);
				}
			} catch (Throwable th) {
				Tracing.logError("error setting sesssionTimeout", th, UserSession.class);
			}
		}
	}

}