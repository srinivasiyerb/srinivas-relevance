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

package org.olat.shibboleth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.basesecurity.AuthHelper;
import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.dispatcher.Dispatcher;
import org.olat.core.dispatcher.DispatcherAction;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.ChiefController;
import org.olat.core.gui.exception.MsgFactory;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.media.RedirectMediaResource;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.i18n.I18nModule;
import org.olat.shibboleth.util.ShibbolethAttribute;
import org.olat.shibboleth.util.ShibbolethHelper;

/**
 * Initial Date: 17.07.2004
 * 
 * @author Mike Stock
 */
public class ShibbolethDispatcher implements Dispatcher {

	/** Provider identifier */
	public static final String PROVIDER_SHIB = "Shib";
	/** Identifies requests for the ShibbolethDispatcher */
	public static final String PATH_SHIBBOLETH = "/shib/";

	private static final String PACKAGE = Util.getPackageName(ShibbolethDispatcher.class);
	private Translator translator;

	/**
	 * Main method called by DIspatcherAction. This processess all shibboleth requests.
	 * 
	 * @param req
	 * @param resp
	 * @param uriPrefix
	 */
	@Override
	public void execute(final HttpServletRequest req, final HttpServletResponse resp, final String uriPrefix) {
		if (translator == null) {
			translator = new PackageTranslator(PACKAGE, I18nModule.getDefaultLocale());
		}
		String uri = req.getRequestURI();

		if (!ShibbolethModule.isEnableShibbolethLogins()) { throw new OLATSecurityException("Got shibboleth request but shibboleth is not enabled: " + uri); }
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			throw new AssertException("UTF-8 encoding not supported!!!!");
		}
		uri = uri.substring(uriPrefix.length()); // guaranteed to exist by DispatcherAction

		final Map<String, String> attributesMap = getShibbolethAttributesFromRequest(req);
		final String uniqueID = getUniqueIdentifierFromRequest(req, resp, attributesMap);
		if (uniqueID == null) { return; }

		UserRequest ureq = null;
		try {
			// upon creation URL is checked for
			ureq = new UserRequest(uriPrefix, req, resp);
		} catch (final NumberFormatException nfe) {
			// MODE could not be decoded
			// typically if robots with wrong urls hit the system
			// or user have bookmarks
			// or authors copy-pasted links to the content.
			// showing redscreens for non valid URL is wrong instead
			// a 404 message must be shown -> e.g. robots correct their links.
			if (Tracing.isDebugEnabled(ShibbolethDispatcher.class)) {
				Tracing.logDebug("Bad Request " + req.getPathInfo(), this.getClass());
			}
			DispatcherAction.sendBadRequest(req.getPathInfo(), resp);
			return;
		}

		final Authentication auth = BaseSecurityManager.getInstance().findAuthenticationByAuthusername(uniqueID, PROVIDER_SHIB);
		if (auth == null) { // no matching authentication...
			ShibbolethRegistrationController.putShibAttributes(req, attributesMap);
			ShibbolethRegistrationController.putShibUniqueID(req, uniqueID);
			redirectToShibbolethRegistration(resp);
			return;
		}
		final int loginStatus = AuthHelper.doLogin(auth.getIdentity(), ShibbolethDispatcher.PROVIDER_SHIB, ureq);
		if (loginStatus != AuthHelper.LOGIN_OK) {
			if (loginStatus == AuthHelper.LOGIN_NOTAVAILABLE) {
				DispatcherAction.redirectToServiceNotAvailable(resp);
			}
			DispatcherAction.redirectToDefaultDispatcher(resp); // error, redirect to login screen
			return;
		}

		// successfull login
		UserDeletionManager.getInstance().setIdentityAsActiv(ureq.getIdentity());
		ureq.getUserSession().getIdentityEnvironment().addAttributes(ShibbolethModule.getAttributeTranslator().translateAttributesMap(attributesMap));
		final MediaResource mr = ureq.getDispatchResult().getResultingMediaResource();
		if (!(mr instanceof RedirectMediaResource)) {
			DispatcherAction.redirectToDefaultDispatcher(resp); // error, redirect to login screen
			return;
		}

		final RedirectMediaResource rmr = (RedirectMediaResource) mr;
		rmr.prepare(resp);

	}

	private String getUniqueIdentifierFromRequest(final HttpServletRequest req, final HttpServletResponse resp, final Map<String, String> attributesMap) {
		final String uniqueID = attributesMap.get(ShibbolethModule.getDefaultUIDAttribute());
		if (uniqueID == null) {
			handleException(
					new ShibbolethException(
							ShibbolethException.UNIQUE_ID_NOT_FOUND,
							"Unable to get unique identifier for subject. Make sure you are listed in the metadata.xml file and your resources your are trying to access are available and your are allowed to see them. (Resourceregistry). "),
					req, resp, translator);
			return null;
		} else if (!checkAttributes(attributesMap)) {
			handleException(new ShibbolethException(ShibbolethException.INSUFFICIENT_ATTRIBUTES, "Insufficient shibboleth attributes!"), req, resp, translator);
			return null;
		}
		return uniqueID;
	}

	private Map<String, String> getShibbolethAttributesFromRequest(final HttpServletRequest req) {
		final Set<String> translateableAttributes = ShibbolethModule.getAttributeTranslator().getTranslateableAttributes();
		final Map<String, String> attributesMap = new HashMap<String, String>();
		final Enumeration headerEnum = req.getHeaderNames();
		while (headerEnum.hasMoreElements()) {
			final String attribute = (String) headerEnum.nextElement();
			final String attributeValue = req.getHeader(attribute);

			final ShibbolethAttribute shibbolethAttribute = ShibbolethAttribute.createFromUserRequestValue(attribute, attributeValue);

			final boolean validAndTranslateableAttribute = shibbolethAttribute.isValid() && translateableAttributes.contains(shibbolethAttribute.getName());
			if (validAndTranslateableAttribute) {
				attributesMap.put(shibbolethAttribute.getName(), shibbolethAttribute.getValueString());
			}
		}

		if (Tracing.isDebugEnabled(ShibbolethDispatcher.class)) {
			Tracing.logDebug("Shib attribute Map: \n\n" + attributesMap.toString() + "\n\n", ShibbolethDispatcher.class);
		}

		return attributesMap;
	}

	/**
	 * Check if all required attributes are here.
	 * 
	 * @param attributesMap
	 * @return true if all required attributes are present, false otherwise.
	 */
	private boolean checkAttributes(final Map<String, String> attributesMap) {
		if (attributesMap.keySet().size() == 1) { return false; }
		final String lastname = attributesMap.get(ShibbolethModule.getLastName());
		final String firstname = attributesMap.get(ShibbolethModule.getFirstName());
		final String email = ShibbolethHelper.getFirstValueOf(ShibbolethModule.getEMail(), attributesMap);
		final String institutionalEMail = ShibbolethHelper.getFirstValueOf(ShibbolethModule.getInstitutionalEMail(), attributesMap);
		final String institutionalName = attributesMap.get(ShibbolethModule.getInstitutionalName());
		// String institutionalUserIdentifier = userMapping.getInstitutionalUserIdentifier();
		if (lastname != null && !lastname.equals("") && firstname != null && !firstname.equals("") && email != null && !email.equals("") && institutionalEMail != null
				&& !institutionalEMail.equals("") && institutionalName != null && !institutionalName.equals("")) { return true; }
		return false;
	}

	private final void redirectToShibbolethRegistration(final HttpServletResponse response) {
		try {
			response.sendRedirect(WebappHelper.getServletContextPath() + DispatcherAction.getPathDefault() + ShibbolethModule.PATH_REGISTER_SHIBBOLETH + "/");
		} catch (final IOException e) {
			Tracing.logError("Redirect failed: url=" + WebappHelper.getServletContextPath() + DispatcherAction.getPathDefault(), e, ShibbolethDispatcher.class);
		}
	}

	/**
	 * It first tries to catch the frequent SAMLExceptions and to ask the user to login again. It basically lets the user to login again without getting a RedScreen if
	 * one of the most frequent shibboleth error occurs. Else a RedScreen is the last option.
	 * 
	 * @param e
	 * @param req
	 * @param resp
	 */
	private void handleException(final Throwable e, final HttpServletRequest req, final HttpServletResponse resp, final Translator translator) {
		final UserRequest ureq = new UserRequest(ShibbolethDispatcher.PATH_SHIBBOLETH, req, resp);
		if (e instanceof ShibbolethException) {
			String userMsg = "";
			final int errorCode = ((ShibbolethException) e).getErrorCode();
			switch (errorCode) {
				case ShibbolethException.GENERAL_SAML_ERROR:
					userMsg = translator.translate("error.shibboleth.generic");
					break;
				case ShibbolethException.UNIQUE_ID_NOT_FOUND:
					userMsg = translator.translate("error.unqueid.notfound");
					break;
				case ShibbolethException.INSUFFICIENT_ATTRIBUTES:
					userMsg = translator.translate("error.insufficieant.attributes");
					break;
				default:
					userMsg = translator.translate("error.shibboleth.generic");
					break;
			}
			showMessage(ureq, "org.opensaml.SAMLException: " + e.getMessage(), e, userMsg, ((ShibbolethException) e).getContactPersonEmail());
			return;
		} else {
			try {
				final ChiefController msgcc = MsgFactory.createMessageChiefController(ureq,
						new OLATRuntimeException("Error processing Shibboleth request: " + e.getMessage(), e), false);
				msgcc.getWindow().dispatchRequest(ureq, true);
			} catch (final Throwable t) {
				Tracing.logError("We're fucked up....", t, ShibbolethDispatcher.class);
			}
		}
	}

	/**
	 * @param ureq
	 * @param exceptionLogMessage will be recorded into the log file
	 * @param cause
	 * @param userMessage gets shown to the user
	 * @param supportEmail if any available, else null
	 */
	private void showMessage(final UserRequest ureq, final String exceptionLogMessage, final Throwable cause, final String userMessage, final String supportEmail) {
		final ChiefController msgcc = MessageWindowController.createMessageChiefController(ureq, new OLATRuntimeException(exceptionLogMessage, cause), userMessage,
				supportEmail);
		msgcc.getWindow().dispatchRequest(ureq, true);
	}

}
