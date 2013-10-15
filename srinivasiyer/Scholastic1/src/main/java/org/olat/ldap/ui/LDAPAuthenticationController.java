package org.olat.ldap.ui;

import java.util.Locale;
import java.util.Map;

import javax.naming.directory.Attributes;

import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.basesecurity.AuthHelper;
import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.dispatcher.DispatcherAction;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.util.Encoder;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;
import org.olat.ldap.LDAPError;
import org.olat.ldap.LDAPLoginManager;
import org.olat.ldap.LDAPLoginModule;
import org.olat.login.LoginModule;
import org.olat.login.OLATAuthenticationController;
import org.olat.login.auth.AuthenticationController;
import org.olat.login.auth.OLATAuthentcationForm;
import org.olat.registration.DisclaimerController;
import org.olat.registration.PwChangeController;
import org.olat.registration.RegistrationManager;
import org.olat.user.UserModule;

public class LDAPAuthenticationController extends AuthenticationController {
	public static final String PROVIDER_LDAP = "LDAP";

	private final VelocityContainer loginComp;
	private Link pwLink;
	private Link anoLink;
	private Controller subController;
	private final OLATAuthentcationForm loginForm;
	private DisclaimerController disclaimerCtr;
	private Identity authenticatedIdentity;
	private String provider = null;

	private CloseableModalController cmc;

	public LDAPAuthenticationController(final UserRequest ureq, final WindowControl control) {
		// use fallback translator to login and registration package
		super(ureq, control, Util.createPackageTranslator(LoginModule.class, ureq.getLocale(), Util.createPackageTranslator(RegistrationManager.class, ureq.getLocale())));

		loginComp = createVelocityContainer("ldaplogin");

		if (UserModule.isPwdchangeallowed() && LDAPLoginModule.isPropagatePasswordChangedOnLdapServer()) {
			pwLink = LinkFactory.createLink("menu.pw", loginComp, this);
			pwLink.setCustomEnabledLinkCSS("o_login_pwd");
		}
		if (LoginModule.isGuestLoginLinksEnabled()) {
			anoLink = LinkFactory.createLink("menu.guest", loginComp, this);
			anoLink.setCustomEnabledLinkCSS("o_login_guests");
		}

		// Use the standard OLAT login form but with our LDAP translator
		loginForm = new OLATAuthentcationForm(ureq, control, getTranslator());
		listenTo(loginForm);

		loginComp.put("ldapForm", loginForm.getInitialComponent());

		putInitialPanel(loginComp);
	}

	@Override
	public void changeLocale(final Locale newLocale) {
		setLocale(newLocale, true);
	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == pwLink) {
			// double-check if allowed first
			if (!UserModule.isPwdchangeallowed() || !LDAPLoginModule.isPropagatePasswordChangedOnLdapServer()) { throw new OLATSecurityException(
					"chose password to be changed, but disallowed by config"); }

			removeAsListenerAndDispose(subController);
			subController = new PwChangeController(ureq, getWindowControl());
			listenTo(subController);

			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), subController.getInitialComponent());
			listenTo(cmc);

			cmc.activate();

		} else if (source == anoLink) {
			if (AuthHelper.doAnonymousLogin(ureq, ureq.getLocale()) == AuthHelper.LOGIN_OK) {
				return;
			} else {
				showError("login.error", WebappHelper.getMailConfig("mailSupport"));
			}
		}
	}

	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {

		final LDAPError ldapError = new LDAPError();

		if (source == loginForm && event == Event.DONE_EVENT) {

			final String login = loginForm.getLogin();
			final String pass = loginForm.getPass();

			authenticatedIdentity = authenticate(login, pass, ldapError);

			if (authenticatedIdentity != null) {
				provider = LDAPAuthenticationController.PROVIDER_LDAP;
			} else {
				// try fallback to OLAT provider if configured
				if (LDAPLoginModule.isCacheLDAPPwdAsOLATPwdOnLogin()) {
					authenticatedIdentity = OLATAuthenticationController.authenticate(login, pass);
				}
				if (authenticatedIdentity != null) {
					provider = BaseSecurityModule.getDefaultAuthProviderIdentifier();
				}
			}
			// Still not found? register for hacking attempts
			if (authenticatedIdentity == null) {
				if (LoginModule.registerFailedLoginAttempt(login)) {
					logAudit("Too many failed login attempts for " + login + ". Login blocked.", null);
					showError("login.blocked", LoginModule.getAttackPreventionTimeoutMin().toString());
					return;
				} else {
					showError("login.error", ldapError.get());
					return;
				}
			}

			LoginModule.clearFailedLoginAttempts(login);

			// Check if disclaimer has been accepted
			if (RegistrationManager.getInstance().needsToConfirmDisclaimer(authenticatedIdentity)) {
				// accept disclaimer first

				removeAsListenerAndDispose(disclaimerCtr);
				disclaimerCtr = new DisclaimerController(ureq, getWindowControl());
				listenTo(disclaimerCtr);

				removeAsListenerAndDispose(cmc);
				cmc = new CloseableModalController(getWindowControl(), translate("close"), disclaimerCtr.getInitialComponent());
				listenTo(cmc);

				cmc.activate();

			} else {
				// disclaimer acceptance not required
				doLoginAndRegister(authenticatedIdentity, ureq, provider);
			}
		}

		if (source == subController) {
			if (event == Event.CANCELLED_EVENT || event == Event.DONE_EVENT) {
				cmc.deactivate();
			}
		} else if (source == disclaimerCtr) {
			cmc.deactivate();
			if (event == Event.DONE_EVENT) {
				// User accepted disclaimer, do login now
				RegistrationManager.getInstance().setHasConfirmedDislaimer(authenticatedIdentity);
				doLoginAndRegister(authenticatedIdentity, ureq, provider);
			} else if (event == Event.CANCELLED_EVENT) {
				// User did not accept, workflow ends here
				showWarning("disclaimer.form.cancelled");
			}
		} else if (source == cmc) {
			// User did close disclaimer window, workflow ends here
			showWarning("disclaimer.form.cancelled");
		}
	}

	public static Identity authenticate(final String username, final String pwd, final LDAPError ldapError) {

		final LDAPLoginManager ldapManager = (LDAPLoginManager) CoreSpringFactory.getBean(LDAPLoginManager.class);
		final Attributes attrs = ldapManager.bindUser(username, pwd, ldapError);

		if (ldapError.isEmpty() && attrs != null) {
			Identity identity = ldapManager.findIdentyByLdapAuthentication(username, ldapError);
			if (!ldapError.isEmpty()) { return null; }
			if (identity == null) {
				// User authenticated but not yet existing - create as new OLAT user
				ldapManager.createAndPersistUser(attrs);
				identity = ldapManager.findIdentyByLdapAuthentication(username, ldapError);
			} else {
				// User does already exist - just sync attributes
				final Map<String, String> olatProToSync = ldapManager.prepareUserPropertyForSync(attrs, identity);
				if (olatProToSync != null) {
					ldapManager.syncUser(olatProToSync, identity);
				}
			}
			// Add or update an OLAT authentication token for this user if configured in the module
			if (identity != null && LDAPLoginModule.isCacheLDAPPwdAsOLATPwdOnLogin()) {
				final BaseSecurity secMgr = BaseSecurityManager.getInstance();
				final Authentication auth = secMgr.findAuthentication(identity, BaseSecurityModule.getDefaultAuthProviderIdentifier());
				if (auth == null) {
					// Reuse exising authentication token
					secMgr.createAndPersistAuthentication(identity, BaseSecurityModule.getDefaultAuthProviderIdentifier(), username, Encoder.encrypt(pwd));
				} else {
					// Create new authenticaten token
					auth.setCredential(Encoder.encrypt(pwd));
					DBFactory.getInstance().updateObject(auth);
				}
			}
			return identity;
		}
		return null;
	}

	/**
	 * Internal helper to perform the real login code and do all necessary steps to register the user session
	 * 
	 * @param authenticatedIdentity
	 * @param ureq
	 * @param myProvider The provider that identified the user
	 */
	private void doLoginAndRegister(final Identity authenticatedIdentity, final UserRequest ureq, final String myProvider) {
		if (provider.equals(PROVIDER_LDAP)) {
			// prepare redirects to home etc, set status
			final int loginStatus = AuthHelper.doLogin(authenticatedIdentity, myProvider, ureq);
			if (loginStatus == AuthHelper.LOGIN_OK) {
				// update last login date and register active user
				UserDeletionManager.getInstance().setIdentityAsActiv(authenticatedIdentity);
			} else if (loginStatus == AuthHelper.LOGIN_NOTAVAILABLE) {
				DispatcherAction.redirectToServiceNotAvailable(ureq.getHttpResp());
			} else {
				getWindowControl().setError(translate("login.error", WebappHelper.getMailConfig("mailSupport")));
			}
		} else if (provider.equals(BaseSecurityModule.getDefaultAuthProviderIdentifier())) {
			// delegate login process to OLAT authentication controller
			authenticated(ureq, authenticatedIdentity);
		} else {
			throw new OLATRuntimeException("Unknown login provider::" + myProvider, null);
		}
	}

	@Override
	protected void doDispose() {
		//
	}
}
