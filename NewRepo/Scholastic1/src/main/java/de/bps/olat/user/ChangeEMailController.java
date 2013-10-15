/**
 * BPS Bildungsportal Sachsen GmbH<br>
 * Bahnhofstrasse 6<br>
 * 09111 Chemnitz<br>
 * Germany<br>
 * Copyright (c) 2005-2008 by BPS Bildungsportal Sachsen GmbH<br>
 * http://www.bps-system.de<br>
 * All rights reserved.
 */
package de.bps.olat.user;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.olat.core.dispatcher.DispatcherAction;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.registration.RegistrationManager;
import org.olat.registration.TemporaryKeyImpl;
import org.olat.user.ProfileAndHomePageEditController;
import org.olat.user.UserManager;

import com.thoughtworks.xstream.XStream;

/**
 * This controller do change the email of a user after he has clicked the appropriate activation-link.
 * <P>
 * Initial Date: 27.04.2009 <br>
 * 
 * @author bja
 */
public class ChangeEMailController extends DefaultController {

	protected static final String PACKAGE = ProfileAndHomePageEditController.class.getPackage().getName();
	protected static final String CHANGE_EMAIL_ENTRY = "change.email.login";

	public static final int TIME_OUT = 3;

	protected Translator pT;
	protected String emKey;
	protected TemporaryKeyImpl tempKey;
	protected RegistrationManager rm = RegistrationManager.getInstance();

	protected UserRequest userRequest;

	/**
	 * executed after click the link in email
	 * 
	 * @param ureq
	 * @param wControl
	 */
	public ChangeEMailController(final UserRequest ureq, final WindowControl wControl) {
		super(wControl);
		this.userRequest = ureq;
		pT = new PackageTranslator(PACKAGE, userRequest.getLocale());
		pT = UserManager.getInstance().getPropertyHandlerTranslator(pT);
		emKey = userRequest.getHttpReq().getParameter("key");
		if ((emKey == null) && (userRequest.getUserSession().getEntry(CHANGE_EMAIL_ENTRY) != null)) {
			emKey = userRequest.getIdentity().getUser().getProperty("emchangeKey", null);
		}
		if (emKey != null) {
			// key exist
			// we check if given key is a valid temporary key
			tempKey = rm.loadTemporaryKeyByRegistrationKey(emKey);
		}
		if (emKey != null) {
			// if key is not valid we redirect to first page
			if (tempKey == null) {
				// registration key not available
				userRequest.getUserSession().putEntryInNonClearedStore("error.change.email", pT.translate("error.change.email"));
				DispatcherAction.redirectToDefaultDispatcher(userRequest.getHttpResp());
				return;
			} else {
				if (!isLinkTimeUp()) {
					try {
						if ((userRequest.getUserSession().getEntry(CHANGE_EMAIL_ENTRY) == null)
								|| (!userRequest.getUserSession().getEntry(CHANGE_EMAIL_ENTRY).equals(CHANGE_EMAIL_ENTRY))) {
							userRequest.getUserSession().putEntryInNonClearedStore(CHANGE_EMAIL_ENTRY, CHANGE_EMAIL_ENTRY);
							DispatcherAction.redirectToDefaultDispatcher(userRequest.getHttpResp());
							return;
						} else {
							if (userRequest.getIdentity() == null) {
								DispatcherAction.redirectToDefaultDispatcher(userRequest.getHttpResp());
								return;
							}
						}
					} catch (final ClassCastException e) {
						DispatcherAction.redirectToDefaultDispatcher(userRequest.getHttpResp());
						return;
					}
				} else {
					// link time is up
					userRequest.getUserSession().putEntryInNonClearedStore("error.change.email.time", pT.translate("error.change.email.time"));
					final XStream xml = new XStream();
					final HashMap<String, String> mails = (HashMap<String, String>) xml.fromXML(tempKey.getEmailAddress());
					final Identity ident = UserManager.getInstance().findIdentityByEmail(mails.get("currentEMail"));
					if (ident != null) {
						// remove keys
						ident.getUser().setProperty("emchangeKey", null);
					}
					// delete registration key
					rm.deleteTemporaryKeyWithId(tempKey.getRegistrationKey());
					DispatcherAction.redirectToDefaultDispatcher(userRequest.getHttpResp());
					return;
				}
			}
		}
	}

	/**
	 * check if the link time up
	 * 
	 * @return
	 */
	public boolean isLinkTimeUp() {
		final Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DAY_OF_WEEK, TIME_OUT * -1);

		if (tempKey == null) {
			// the database entry was deleted
			return true;
		}

		if (!tempKey.getCreationDate().after(cal.getTime())) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * delete registration key, 'change.email.login' entry and set the userproperty emchangeKey to null
	 */
	public void deleteRegistrationKey() {
		final User user = userRequest.getIdentity().getUser();
		// remove keys
		user.setProperty("emchangeKey", null);
		userRequest.getUserSession().removeEntryFromNonClearedStore(CHANGE_EMAIL_ENTRY);
		userRequest.getUserSession().removeEntryFromNonClearedStore("error.change.email.time");
		// delete registration key
		if (tempKey != null) {
			rm.deleteTemporaryKeyWithId(tempKey.getRegistrationKey());
		}
	}

	@Override
	protected void doDispose() {
		// nothing to do
	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		// nothing to do
	}
}
