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

package org.olat.registration;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.velocity.VelocityContext;
import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.dispatcher.DispatcherAction;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.wizard.WizardInfoController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.Preferences;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Util;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.MailerWithTemplate;
import org.olat.user.UserManager;

/**
 * Description:<br>
 * Controlls the change password workflow.
 * <P>
 * 
 * @author Sabina Jeger
 */
public class PwChangeController extends BasicController {

	private static String SEPARATOR = "____________________________________________________________________\n";
	private final VelocityContainer myContent;

	private final Panel pwarea;
	private WizardInfoController wic;
	private final RegistrationManager rm = RegistrationManager.getInstance();
	private final String pwKey;
	private PwChangeForm pwf;
	private TemporaryKeyImpl tempKey;
	private EmailOrUsernameFormController emailOrUsernameCtr;
	private Link pwchangeHomelink;

	/**
	 * Controller to change a user's password.
	 * 
	 * @param ureq
	 * @param wControl
	 */
	public PwChangeController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		myContent = createVelocityContainer("pwchange");
		wic = new WizardInfoController(ureq, 4);
		myContent.put("pwwizard", wic.getInitialComponent());
		pwarea = new Panel("pwarea");
		myContent.put("pwarea", pwarea);
		pwKey = ureq.getHttpReq().getParameter("key");
		if (pwKey == null || pwKey.equals("")) {
			// no temporarykey is given, we assume step 1
			createEmailForm(ureq, wControl);
			putInitialPanel(myContent);
		} else {
			// we check if given key is a valid temporary key
			tempKey = rm.loadTemporaryKeyByRegistrationKey(pwKey);
			// if key is not valid we redirect to first page
			if (tempKey == null) {
				// error, there should be an entry
				getWindowControl().setError(translate("pwkey.missingentry"));
				createEmailForm(ureq, wControl);
				// load view in layout
				final LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), null, null, myContent, null);
				putInitialPanel(layoutCtr.getInitialComponent());
			} else {
				wic.setCurStep(3);
				pwf = new PwChangeForm(ureq, wControl);
				listenTo(pwf);
				myContent.contextPut("pwdhelp", translate("pwdhelp"));
				myContent.contextPut("text", translate("step3.pw.text"));
				pwarea.setContent(pwf.getInitialComponent());
				// load view in layout
				final LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), null, null, myContent, null);
				putInitialPanel(layoutCtr.getInitialComponent());
			}
		}
	}

	/**
	 * just needed for creating EmailForm
	 */
	private void createEmailForm(final UserRequest ureq, final WindowControl wControl) {
		myContent.contextPut("title", translate("step1.pw.title"));
		myContent.contextPut("text", translate("step1.pw.text"));
		removeAsListenerAndDispose(emailOrUsernameCtr);
		emailOrUsernameCtr = new EmailOrUsernameFormController(ureq, wControl);
		listenTo(emailOrUsernameCtr);
		pwarea.setContent(emailOrUsernameCtr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == pwchangeHomelink) {
			DispatcherAction.redirectToDefaultDispatcher(ureq.getHttpResp());
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == pwf) {
			// pwchange Form was clicked
			if (event == Event.DONE_EVENT) { // form
				// validation was ok
				wic.setCurStep(4);
				myContent.contextPut("pwdhelp", "");
				myContent.contextPut("text", translate("step4.pw.text"));
				pwchangeHomelink = LinkFactory.createLink("pwchange.homelink", myContent, this);
				// pwf.setVisible(false);
				pwarea.setVisible(false);
				final Identity identToChange = UserManager.getInstance().findIdentityByEmail(tempKey.getEmailAddress());
				if (identToChange == null || !pwf.saveFormData(identToChange)) {
					getWindowControl().setError(translate("pwchange.failed"));
				}
				rm.deleteTemporaryKeyWithId(tempKey.getRegistrationKey());
			} else if (event == Event.CANCELLED_EVENT) {
				getWindowControl().setInfo(translate("pwform.cancelled"));
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
		} else if (source == emailOrUsernameCtr) {
			// eMail Form was clicked
			if (event == Event.DONE_EVENT) { // form
				// Email requested for tempkey save the fields somewhere
				String emailOrUsername = emailOrUsernameCtr.getEmailOrUsername();
				emailOrUsername = emailOrUsername.trim();
				String body = null;
				String subject = null;
				// get remote address
				final String ip = ureq.getHttpReq().getRemoteAddr();
				final String today = DateFormat.getDateInstance(DateFormat.LONG, ureq.getLocale()).format(new Date());
				// mailer configuration
				final String serverpath = Settings.getServerContextPathURI();
				final String servername = ureq.getHttpReq().getServerName();
				if (isLogDebugEnabled()) {
					logDebug("this servername is " + servername + " and serverpath is " + serverpath, null);
				}

				// Look for user in "Person" and "user" tables
				Identity identity = null;
				// See if the entered value is a username
				identity = BaseSecurityManager.getInstance().findIdentityByName(emailOrUsername);
				if (identity == null) {
					// Try fallback with email, maybe user used his email address instead
					// only do this, if its really an email, may lead to multiple results else.
					if (MailHelper.isValidEmailAddress(emailOrUsername)) {
						identity = UserManager.getInstance().findIdentityByEmail(emailOrUsername);
					}
				}
				if (identity != null) {
					// check if user has an OLAT provider token, otherwhise a pwd change makes no sense
					final Authentication auth = BaseSecurityManager.getInstance().findAuthentication(identity, BaseSecurityModule.getDefaultAuthProviderIdentifier());
					if (auth == null) {
						getWindowControl().setWarning(translate("password.cantchange"));
						return;
					}
					final Preferences prefs = identity.getUser().getPreferences();
					final Locale locale = I18nManager.getInstance().getLocaleOrDefault(prefs.getLanguage());
					ureq.getUserSession().setLocale(locale);
					myContent.contextPut("locale", locale);
					final Translator userTrans = Util.createPackageTranslator(PwChangeController.class, locale);
					final String emailAdress = identity.getUser().getProperty(UserConstants.EMAIL, locale);
					TemporaryKey tk = rm.loadTemporaryKeyByEmail(emailAdress);
					if (tk == null) {
						tk = rm.createTemporaryKeyByEmail(emailAdress, ip, rm.PW_CHANGE);
					}
					myContent.contextPut("pwKey", tk.getRegistrationKey());
					body = userTrans.translate("pwchange.intro", new String[] { identity.getName() })
							+ userTrans.translate("pwchange.body",
									new String[] { serverpath, tk.getRegistrationKey(), I18nManager.getInstance().getLocaleKey(ureq.getLocale()) }) + SEPARATOR
							+ userTrans.translate("reg.wherefrom", new String[] { serverpath, today, ip });
					subject = userTrans.translate("pwchange.subject");
					final MailTemplate mailTempl = new MailTemplate(subject, body, null) {
						@Override
						public void putVariablesInMailContext(final VelocityContext context, final Identity recipient) {
							// nothing to do
						}
					};

					final MailerResult result = MailerWithTemplate.getInstance().sendMail(identity, null, null, mailTempl, null);
					if (result.getReturnCode() == 0) {
						getWindowControl().setInfo(translate("email.sent"));
						// prepare next step
						wic.setCurStep(2);
						myContent.contextPut("text", translate("step2.pw.text"));
						emailOrUsernameCtr.getInitialComponent().setVisible(false);
					} else {
						getWindowControl().setError(translate("email.notsent"));
					}
				} else {
					// no user exists, this is an error in the pwchange page
					// REVIEW:pb:2009-11-23:gw, setter should not be necessary. -> check the error already in th emailOrUsernameCtr
					emailOrUsernameCtr.setUserNotIdentifiedError();
				}
			} else if (event == Event.CANCELLED_EVENT) {
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		if (wic != null) {
			wic.dispose();
			wic = null;
		}
	}

}