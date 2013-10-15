package org.olat.course.nodes.tu;

import java.net.MalformedURLException;
import java.net.URL;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.modules.ModuleConfiguration;

public class TUStartController extends BasicController {

	private final VelocityContainer runVC;

	public TUStartController(final UserRequest ureq, final WindowControl wControl, final ModuleConfiguration config) {
		super(ureq, wControl);

		runVC = createVelocityContainer("run");

		URL url = null;
		try {
			url = new URL((String) config.get(TUConfigForm.CONFIGKEY_PROTO), (String) config.get(TUConfigForm.CONFIGKEY_HOST),
					((Integer) config.get(TUConfigForm.CONFIGKEY_PORT)).intValue(), (String) config.get(TUConfigForm.CONFIGKEY_URI));
		} catch (final MalformedURLException e) {
			// this should not happen since the url was already validated in edit mode
			runVC.contextPut("url", "");
		}
		if (url != null) {
			final StringBuilder sb = new StringBuilder(128);
			sb.append(url.toString());
			// since the url only includes the path, but not the query (?...), append it here, if any
			final String query = (String) config.get(TUConfigForm.CONFIGKEY_QUERY);
			if (query != null) {
				sb.append("?");
				sb.append(query);
			}
			runVC.contextPut("url", sb.toString());
		}

		putInitialPanel(runVC);
	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	@Override
	protected void doDispose() {
		//
	}
}
