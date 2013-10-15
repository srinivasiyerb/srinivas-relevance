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
package org.olat.restapi.i18n;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.olat.core.gui.UserRequest;
import org.olat.core.util.StringHelper;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;
import org.olat.restapi.security.RestSecurityHelper;

/**
 * Description:<br>
 * This handles translations from the i18n module of OLAT.
 * <P>
 * Initial Date: 14 apr. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
@Path("i18n")
public class I18nWebService {

	private static final String VERSION = "1.0";
	private static final String[] EMPTY_ARRAY = new String[0];

	/**
	 * Retrieves the version of the i18n Web Service.
	 * 
	 * @response.representation.200.mediaType text/plain
	 * @response.representation.200.doc The version of this specific Web Service
	 * @response.representation.200.example 1.0
	 * @return
	 */
	@GET
	@Path("version")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getVersion() {
		return Response.ok(VERSION).build();
	}

	/**
	 * Return the translation of the key. If the "locale" parameter is not specified, the method try to use the "locale" of the user and if it hasn't, take the default
	 * locale.
	 * 
	 * @response.representation.200.mediaType text/plain
	 * @response.representation.200.doc The translation of the package + key
	 * @response.representation.200.example OK
	 * @param packageName The name of the package
	 * @param key The key to translate
	 * @param localeKey The locale (optional)
	 * @param request The HTTP request (optional)
	 * @return
	 */
	@GET
	@Path("{package}/{key}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getTranslation(@PathParam("package") final String packageName, @PathParam("key") final String key, @QueryParam("locale") final String localeKey,
			@Context final HttpServletRequest request) {
		final I18nManager i18n = I18nManager.getInstance();

		Locale locale = null;
		if (StringHelper.containsNonWhitespace(localeKey)) {
			locale = i18n.getLocaleOrDefault(localeKey);
		} else {
			final UserRequest ureq = RestSecurityHelper.getUserRequest(request);
			if (ureq != null && ureq.getLocale() != null) {
				locale = ureq.getLocale();
			}
		}

		if (locale == null) {
			locale = I18nModule.getDefaultLocale();
		}

		final boolean overlayEnabled = I18nModule.isOverlayEnabled();
		final String val = i18n.getLocalizedString(packageName, key, EMPTY_ARRAY, locale, overlayEnabled, true);
		return Response.ok(val).build();
	}

}
