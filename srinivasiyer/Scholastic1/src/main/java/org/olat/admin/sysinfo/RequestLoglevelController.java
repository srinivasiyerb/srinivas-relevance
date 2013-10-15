package org.olat.admin.sysinfo;

import org.apache.log4j.Logger;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.util.threadlog.RequestBasedLogLevelManager;
import org.olat.core.util.threadlog.UserBasedLogLevelManager;

/**
 * A rather simply kept controller used for the requestloglevel feature which was added to debug special cases with 'slow requests'.
 * <p>
 * It allows to mark particular requests (based on ip address or username - dont overlap those two though!) with a specific loglevel and even an appender.
 * <p>
 * That way you can have all requests from say user 'administrator' logged with log level DEBUG and sent to appender 'DebugLog' (which is a standard log4j appender and
 * can therefore for example be writing to a different file than the rest of the log events).
 * <P>
 * Initial Date: 13.09.2010 <br>
 * 
 * @author Stefan
 */
public class RequestLoglevelController extends BasicController implements Controller {

	private RequestLoglevelForm form;

	private final RequestBasedLogLevelManager requestBasedLogLevelManager;

	private final UserBasedLogLevelManager userBasedLogLevelManager;

	protected RequestLoglevelController(final UserRequest ureq, final WindowControl control) {
		super(ureq, control);

		requestBasedLogLevelManager = RequestBasedLogLevelManager.getInstance();
		userBasedLogLevelManager = UserBasedLogLevelManager.getInstance();

		if (requestBasedLogLevelManager == null && userBasedLogLevelManager == null) {
			final VelocityContainer requestlogleveldisabled = createVelocityContainer("requestlogleveldisabled");
			putInitialPanel(requestlogleveldisabled);
		} else {
			form = new RequestLoglevelForm("requestloglevelform", getTranslator());
			form.addListener(this);
			putInitialPanel(form);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == form) {
			final String[] usernames = form.getUsernamesAndLevels();
			final String[] ips = form.getIpsAndLevels();

			if (requestBasedLogLevelManager != null) {
				requestBasedLogLevelManager.reset();

				requestBasedLogLevelManager.storeIpsAndLevels(form.getRawIpsAndLevels());

				for (int i = 0; i < ips.length; i++) {
					final String ip = ips[i];
					if (ip != null && ip.length() > 0 && ip.contains("=")) {
						try {
							requestBasedLogLevelManager.setLogLevelAndAppender(ip);
						} catch (final Exception e) {
							Logger.getLogger(getClass()).warn("Couldnt set loglevel for remote address: " + ip, e);
						}
					}
				}
			}

			if (userBasedLogLevelManager != null) {
				userBasedLogLevelManager.storeUsernameAndLevels(form.getRawUsernames());

				userBasedLogLevelManager.reset();
				for (int i = 0; i < usernames.length; i++) {
					final String username = usernames[i];
					if (username != null && username.length() > 0 && username.contains("=")) {
						try {
							userBasedLogLevelManager.setLogLevelAndAppender(username);
						} catch (final Exception e) {
							Logger.getLogger(getClass()).warn("Couldnt set loglevel for username: " + username, e);
						}
					}
				}
			}

		}

	}

}