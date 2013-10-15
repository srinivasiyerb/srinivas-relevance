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

package org.olat.commons.rss;

import org.apache.commons.lang.RandomStringUtils;
import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;

/**
 * Description:<BR>
 * Constants and helper methods for the OLAT RSS feeds
 * <P>
 * Initial Date: Jan 12, 2005
 * 
 * @author gnaegi
 */
public class RSSUtil {

	/** Authentication provider name for RSS authentication **/
	public static final String RSS_AUTH_PROVIDER = "RSS-OLAT";
	/** Key under which the users rss token is beeing kept in the http session **/
	public static final String RSS_AUTH_TOKEN_KEY = "rsstoken";
	/** path prefix for personal rss feed **/
	public static final String RSS_PREFIX_PERSONAL = "/personal/";
	/** path prefix for public rss feed **/
	public static final String RSS_PREFIX_PUBLIC = "/public/";

	/** OLAT server URI **/
	public static final String URI_SERVER;
	/** Personal rss channel URI prefix **/
	public static final String URI_PERSONAL_CHANNEL;
	/** Public rss channel URI prefix **/
	public static final String URI_PUBLIC_CHANNEL;
	static {
		URI_SERVER = Settings.getServerContextPathURI() + "/";
		URI_PERSONAL_CHANNEL = URI_SERVER + "rss" + RSS_PREFIX_PERSONAL;
		URI_PUBLIC_CHANNEL = URI_SERVER + "rss" + RSS_PREFIX_PUBLIC;
	}

	/**
	 * Puts the users rss token into the httpsession. If no token is available one is generated and peristed in the database
	 * 
	 * @param ureq
	 * @return String the token
	 */
	public static String putPersonalRssTokenInSession(final UserRequest ureq) {
		final Identity identity = ureq.getIdentity();
		String token = null;
		final BaseSecurity secManager = BaseSecurityManager.getInstance();
		Authentication auth = secManager.findAuthentication(identity, RSSUtil.RSS_AUTH_PROVIDER);
		if (auth == null) {
			// no token found - create one
			token = RandomStringUtils.randomAlphanumeric(6);
			auth = secManager.createAndPersistAuthentication(identity, RSSUtil.RSS_AUTH_PROVIDER, identity.getName(), token);
		} else {
			token = auth.getCredential();
		}
		ureq.getUserSession().putEntry(RSSUtil.RSS_AUTH_TOKEN_KEY, token);
		return token;
	}

	/**
	 * Calculates the absolute URL to the users personal rss feed
	 * 
	 * @param ureq
	 * @return String
	 */
	public static String getPersonalRssLink(final UserRequest ureq) {
		final String token = (String) ureq.getUserSession().getEntry(RSSUtil.RSS_AUTH_TOKEN_KEY);
		return (getPersonalRssLink(ureq.getIdentity(), token));
	}

	/**
	 * Calculates the absolute URL to the users personal rss feed
	 * 
	 * @param identity
	 * @param token
	 * @return String
	 */
	public static String getPersonalRssLink(final Identity identity, final String token) {
		final String link = RSSUtil.URI_PERSONAL_CHANNEL + identity.getName() + "/" + token + "/" + "olat.rss";
		return link;
	}
}
