package org.olat.shibboleth;

import java.util.Locale;

import org.olat.basesecurity.AuthHelper;
import org.olat.core.dispatcher.DispatcherAction;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;
import org.olat.login.LoginModule;
import org.olat.login.auth.AuthenticationController;

/**
 * Description:<br>
 * Simple ShibbolethAuthenticationController. It just has a link for redirecting the requests to the /shib/.
 * <P>
 * Initial Date: 08.07.2009 <br>
 * 
 * @author Lavinia Dumitrescu
 */
public class DefaultShibbolethAuthenticationController extends AuthenticationController {

	private final VelocityContainer loginComp;
	private final Link shibLink;
	private Link guestLink;
	private final Panel mainPanel;

	/**
	 * @param ureq
	 * @param wControl
	 */
	public DefaultShibbolethAuthenticationController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		// extends authControll which is a BasicController, so we have to set the
		// Base new to resolve our velocity pages
		setBasePackage(this.getClass());
		// Manually set translator that uses a fallback translator to the login module
		// Can't use constructor with fallback translator because it gets overriden by setBasePackage call above
		setTranslator(Util.createPackageTranslator(this.getClass(), ureq.getLocale(), Util.createPackageTranslator(LoginModule.class, ureq.getLocale())));

		if (!ShibbolethModule.isEnableShibbolethLogins()) { throw new OLATSecurityException("Shibboleth is not enabled."); }

		loginComp = createVelocityContainer("default_shibbolethlogin");
		shibLink = LinkFactory.createLink("shib.redirect", loginComp, this);

		if (LoginModule.isGuestLoginLinksEnabled()) {
			guestLink = LinkFactory.createLink("menu.guest", loginComp, this);
			guestLink.setCustomEnabledLinkCSS("o_login_guests");
		}

		mainPanel = putInitialPanel(loginComp);
	}

	@Override
	public void changeLocale(final Locale newLocale) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doDispose() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == shibLink) {
			DispatcherAction.redirectTo(ureq.getHttpResp(), WebappHelper.getServletContextPath() + "/shib/");
		} else if (source == guestLink) {
			final int loginStatus = AuthHelper.doAnonymousLogin(ureq, ureq.getLocale());
			if (loginStatus == AuthHelper.LOGIN_OK) {
				return;
			} else if (loginStatus == AuthHelper.LOGIN_NOTAVAILABLE) {
				// getWindowControl().setError(translate("login.notavailable", OLATContext.getSupportaddress()));
				DispatcherAction.redirectToServiceNotAvailable(ureq.getHttpResp());
			} else {
				getWindowControl().setError(translate("login.error", WebappHelper.getMailConfig("mailSupport")));
			}
		}
	}

}
