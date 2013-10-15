package org.olat.test.guidemo;

import org.olat.admin.user.UserSearchController;
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
import org.olat.core.gui.control.floatingresizabledialog.FloatingResizableDialogController;
import org.olat.core.gui.dev.controller.SourceViewController;

public class GuiDemoFloatingPanelController extends BasicController {

	private final VelocityContainer panelVc = createVelocityContainer("panel");
	private final VelocityContainer openerVc = createVelocityContainer("opener");
	private final VelocityContainer localContent = createVelocityContainer("localContent");
	private final Panel panel = new Panel("panel");
	private final Link open;
	private final Link open2;
	private Link contentLink;
	private FloatingResizableDialogController dialog;

	public GuiDemoFloatingPanelController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		panel.setContent(openerVc);
		open = LinkFactory.createLink("open", openerVc, this);
		open2 = LinkFactory.createLink("open2", openerVc, this);

		// add source view control
		final Controller sourceview = new SourceViewController(ureq, wControl, this.getClass(), openerVc);
		openerVc.put("sourceview", sourceview.getInitialComponent());

		putInitialPanel(panel);
	}

	@Override
	protected void doDispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == open) {
			final UserSearchController userSearch = new UserSearchController(ureq, getWindowControl(), true);
			dialog = new FloatingResizableDialogController(ureq, getWindowControl(), userSearch.getInitialComponent(), "Your title", 350, 350, 400, 200, null, "", true,
					false);
			dialog.addControllerListener(this);
			panelVc.put("panel", dialog.getInitialComponent());
			panel.setContent(panelVc);
		} else if (source == open2) {
			dialog = new FloatingResizableDialogController(ureq, getWindowControl(), localContent, "Your title", 350, 350, 400, 200,
					createVelocityContainer("localContent2"), "", true, false);
			dialog.addControllerListener(this);
			panelVc.put("panel", dialog.getInitialComponent());
			contentLink = LinkFactory.createLink("link4", localContent, this);
			panel.setContent(panelVc);
		} else if (source == contentLink) {
			getWindowControl().setInfo("Congratulations! You won a trip to Lorem Ipsum.");
		}

	}

	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == dialog) {
			if (event == Event.DONE_EVENT) {
				panel.setContent(openerVc);
			}
		}
	}

}
