package org.olat.gui.control;

import org.olat.core.commons.chiefcontrollers.LanguageChooserController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;

public class OlatDmzTopNavController extends BasicController {

	private final VelocityContainer topNavVC;
	private LanguageChooserController languageChooserC;

	public OlatDmzTopNavController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		topNavVC = createVelocityContainer("dmztopnav");

		// choosing language
		languageChooserC = new LanguageChooserController(getWindowControl(), ureq);
		// DOKU:pb:2008-01 listenTo(languageChooserC); not necessary as LanguageChooser sends a MultiUserEvent
		// which is catched by the BaseFullWebappController. This one is then
		// responsible to recreate the GUI with the new Locale
		//
		topNavVC.put("languageChooser", languageChooserC.getInitialComponent());

		putInitialPanel(topNavVC);
	}

	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		//
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// no events yet
	}

	@Override
	protected void doDispose() {
		if (languageChooserC != null) {
			languageChooserC.dispose();
			languageChooserC = null;
		}
	}

}
