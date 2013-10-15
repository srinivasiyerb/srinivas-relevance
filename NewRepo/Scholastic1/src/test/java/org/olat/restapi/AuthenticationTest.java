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

package org.olat.restapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;
import org.olat.test.OlatJerseyTestCase;

/**
 * Description:<br>
 * Test the authentication service
 * <P>
 * Initial Date: 14 apr. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
public class AuthenticationTest extends OlatJerseyTestCase {

	public AuthenticationTest() {
		super();
	}

	@Test
	public void testSessionCookieLogin() throws HttpException, IOException {
		final URI uri = UriBuilder.fromUri(getContextURI()).path("auth").path("administrator").queryParam("password", "olat").build();
		final GetMethod method = createGet(uri, MediaType.TEXT_PLAIN, true);
		final HttpClient c = getHttpClient();
		final int code = c.executeMethod(method);
		assertEquals(200, code);
		final String response = method.getResponseBodyAsString();
		assertTrue(response.startsWith("<hello"));
		assertTrue(response.endsWith("Hello administrator</hello>"));
		final Cookie[] cookies = c.getState().getCookies();
		assertNotNull(cookies);
		assertTrue(cookies.length > 0);
	}

	@Test
	public void testWrongPassword() throws HttpException, IOException {
		final URI uri = UriBuilder.fromUri(getContextURI()).path("auth").path("administrator").queryParam("password", "blabla").build();
		final GetMethod method = createGet(uri, MediaType.TEXT_PLAIN, true);
		final HttpClient c = getHttpClient();
		final int code = c.executeMethod(method);
		assertEquals(401, code);
	}

	@Test
	public void testUnkownUser() throws HttpException, IOException {
		final URI uri = UriBuilder.fromUri(getContextURI()).path("auth").path("treuitr").queryParam("password", "blabla").build();
		final GetMethod method = createGet(uri, MediaType.TEXT_PLAIN, true);
		final HttpClient c = getHttpClient();
		final int code = c.executeMethod(method);
		assertEquals(401, code);
	}
}
