package org.olat.admin.user.bulkChange;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.FormUIFactory;
import org.olat.core.gui.components.form.flexible.elements.SelectionElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.login.auth.OLATAuthManager;
import org.olat.registration.RegistrationManager;
import org.olat.user.UserManager;

/**
 * Description:<br>
 * This is an extension for the admin site for changing password for a user list (adding respectively OLAT authentication for the ones that doesn't have one).
 * <P>
 * Initial Date: 25.05.2010 <br>
 * 
 * @author Lavinia Dumitrescu
 */
public class UserBulkChangePasswordController extends BasicController {

	private static final OLog log = Tracing.createLoggerFor(UserBulkChangePasswordController.class);
	private final ChangePasswordForm changePasswordForm;

	public UserBulkChangePasswordController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		final Panel main = new Panel("changePsw");
		final VelocityContainer mainVC = createVelocityContainer("index");

		changePasswordForm = new ChangePasswordForm(ureq, wControl);
		this.listenTo(changePasswordForm);
		mainVC.put("form", changePasswordForm.getInitialComponent());

		main.setContent(mainVC);
		putInitialPanel(main);
	}

	@Override
	protected void doDispose() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (event == Event.DONE_EVENT) {
			final String[] usernames = changePasswordForm.getUsernames();
			final String password = changePasswordForm.getPassword();
			final boolean autodisc = changePasswordForm.getDisclaimerAccept();
			final boolean langGerman = changePasswordForm.getLangGerman();

			final BaseSecurity identityManager = BaseSecurityManager.getInstance();

			int c = 0;

			for (final String username : usernames) {
				if (username.length() == 0) {
					continue;
				}

				try {
					final Identity identity = identityManager.findIdentityByName(username);
					if (identity != null) {
						if (password != null && password.trim().length() > 0) {
							OLATAuthManager.changePassword(ureq.getIdentity(), identity, password);
							log.info("changePassword for username: " + username);
						}
						if (autodisc) {
							RegistrationManager.getInstance().setHasConfirmedDislaimer(identity);
							log.info("Disclaimer accepted for username: " + username);
						}
						if (langGerman) {
							identity.getUser().getPreferences().setLanguage("de");
							UserManager.getInstance().updateUserFromIdentity(identity);
							log.info("Set language German for username: " + username);
						}

						c++;

					} else {
						log.warn("could find user with username: " + username);
					}
				} catch (final Exception e) {
					e.printStackTrace();
					log.error("Failed to change password/settings for username: " + username, e);
				}
			}

			// notify done
			getWindowControl().setInfo(translate("bulk.psw.done", "" + c));

			// TODO: clear the form
			// changePasswordForm.clearForm(); //???
		}
	}

	/**
	 * ChangePasswordForm.
	 * <P>
	 * Initial Date: 08.06.2010 <br>
	 * 
	 * @author Lavinia Dumitrescu
	 */
	private class ChangePasswordForm extends FormBasicController {

		private TextElement olatPasswordAuthentication;
		private TextElement userListTextArea;
		private SelectionElement acceptDisclaimer;
		private SelectionElement langGerman;
		private FormSubmit submitButton;

		public ChangePasswordForm(final UserRequest ureq, final WindowControl wControl) {
			super(ureq, wControl);

			initForm(ureq);
		}

		@Override
		protected void formOK(final UserRequest ureq) {
			fireEvent(ureq, Event.DONE_EVENT);
		}

		@Override
		protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {

			userListTextArea = FormUIFactory.getInstance().addTextAreaElement("bulk.psw.users", 10, 2, null, formLayout);
			olatPasswordAuthentication = FormUIFactory.getInstance().addTextElement("pswtextfield", "bulk.psw.newpsw", 255, "", formLayout);
			acceptDisclaimer = FormUIFactory.getInstance().addCheckboxesVertical("bulk.auto.disc", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);
			langGerman = FormUIFactory.getInstance().addCheckboxesVertical("bulk.lang.german", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);

			final FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("button_layout", getTranslator());
			formLayout.add(buttonLayout);
			submitButton = FormUIFactory.getInstance().addFormSubmitButton("bulk.psw.submit", buttonLayout);

			acceptDisclaimer.select("xx", true);
			langGerman.select("xx", true);
		}

		private String[] getUsernames() {
			final String[] retVal = userListTextArea.getValue().split("\r\n");
			return retVal;
		}

		private String getPassword() {
			return olatPasswordAuthentication.getValue();
		}

		private boolean getDisclaimerAccept() {
			return acceptDisclaimer.isSelected(0);
		}

		private boolean getLangGerman() {
			return langGerman.isSelected(0);
		}

		@Override
		protected void doDispose() {
			if (olatPasswordAuthentication != null) {
				olatPasswordAuthentication.setValue("");
			}
			if (userListTextArea != null) {
				userListTextArea.setValue("");
			}
		}

	}

}
