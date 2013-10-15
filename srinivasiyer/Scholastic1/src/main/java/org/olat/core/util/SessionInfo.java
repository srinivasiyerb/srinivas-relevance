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

import javax.servlet.http.HttpSession;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;

/**
 * Initial Date: 08.08.2003
 * 
 * @author Mike Stock
 */
public class SessionInfo {

	private String login;
	private HttpSession session;
	private String firstname;
	private String lastname;
	private String fromIP;
	private String fromFQN;
	private String authProvider;
	private String userAgent;
	private String webMode;
	private boolean isWebDAV;
	private boolean isREST;
	private boolean secure;
	private long timestmp = -1;
	private long creationTime = -1;
	private static final String FORMATTED = "login: [%s] first: [%s] last: [%s] fromIP: [%s] fromFQN: [%s] authProvider: [%s] webdav: [%s] REST: [%s] secure: [%s] webMode: [%s] duration: [%d]s";

	/**
	 * @param login
	 * @param session
	 */
	public SessionInfo(String login, HttpSession session) {
		setLogin(login);
		setSession(session);
		secure = false;
		creationTime = session.getCreationTime();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format(FORMATTED, login, firstname, lastname, fromIP, fromFQN, authProvider, isWebDAV, isREST, isSecure(), getWebMode(),
				getSessionDuration() / 1000);
	}

	/**
	 * @param sec
	 */
	public void setSecure(boolean sec) {
		secure = sec;
	}

	/**
	 * @return Wether the session is SSL enabled
	 */
	public boolean isSecure() {
		return secure;
	}

	/**
	 * @return firstname of logged-in user
	 */
	public String getFirstname() {
		return firstname;
	}

	/**
	 * @return login of logged-in user
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * @return last name of logged-in user
	 */
	public String getLastname() {
		return lastname;
	}

	/**
	 * @return the associated session
	 */
	public HttpSession getSession() {
		return session;
	}

	/**
	 * @param string
	 */
	public void setFirstname(String string) {
		firstname = string;
	}

	/**
	 * @param string
	 */
	private void setLogin(String string) {
		login = string;
	}

	/**
	 * @param string
	 */
	public void setLastname(String string) {
		lastname = string;
	}

	/**
	 * @param session
	 */
	private void setSession(HttpSession session) {
		this.session = session;
	}

	/**
	 * @return the fully qualified domain name of this user
	 */
	public String getFromFQN() {
		return fromFQN;
	}

	/**
	 * @return the ip of this user
	 */
	public String getFromIP() {
		return fromIP;
	}

	/**
	 * @param string
	 */
	public void setFromFQN(String string) {
		fromFQN = string;
	}

	/**
	 * @param string
	 */
	public void setFromIP(String string) {
		fromIP = string;
	}

	/**
	 * @return the authentication provider used to authenticate this user
	 */
	public String getAuthProvider() {
		return authProvider;
	}

	/**
	 * @param authProvider
	 */
	public void setAuthProvider(String authProvider) {
		this.authProvider = authProvider;
	}

	/**
	 * @return true if session is from WebDAV
	 */
	public boolean isWebDAV() {
		return isWebDAV;
	}

	/**
	 * @param isWebDav
	 */
	public void setWebDAV(boolean isWebDav) {
		this.isWebDAV = isWebDav;
	}

	/**
	 * @return true if session is from the REST API
	 */
	public boolean isREST() {
		return isREST;
	}

	/**
	 * @param isREST
	 */
	public void setREST(boolean isREST) {
		this.isREST = isREST;
	}

	/**
	 * Get the user agent.
	 * 
	 * @return user agent
	 */
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * Set the user agent.
	 * 
	 * @param userAgent
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * record business relevant click time. The HTTP Sessions last access time can differ from business click time because of the AJAX polling feature.
	 * http://www.devx.com/Java/Article/28685 shows that System.nanoTime() is fast.
	 * 
	 * @param timestmp
	 */
	public void setLastClickTime() {
		this.timestmp = System.currentTimeMillis();
	}

	/**
	 * last time a business relevant click was made
	 * 
	 * @return timestamp in nanoseconds
	 */
	public long getLastClickTime() {
		return this.timestmp;
	}

	public long getSessionDuration() {
		return timestmp - creationTime;
	}

	/**
	 * @return The web delivery mode as readable string
	 */
	public String getWebMode() {
		return webMode;
	}

	/**
	 * @param ureq The user request for which the web mode should be calculated or null for normal web 1.0 mode (e.g. in web dav)
	 */
	public void setWebModeFromUreq(UserRequest ureq) {
		String deliveryMode = "web 1.0"; // default, e.g. when connecting with webdav
		if (ureq != null) {
			// calculate ajax delivery mode
			if (Windows.getWindows(ureq).getWindowManager().isAjaxEnabled()) deliveryMode = "web 2.0";
			if (Windows.getWindows(ureq).getWindowManager().isForScreenReader()) deliveryMode = "web 2.a";
		}
		this.webMode = deliveryMode;
	}
}
