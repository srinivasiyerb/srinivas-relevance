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

package org.olat.modules.co;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.internet.AddressException;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.ContactMessage;
import org.olat.core.util.mail.Emailer;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailLoggingAction;

/**
 * <b>Fires Event: </b>
 * <UL>
 * <LI><b>Event.DONE_EVENT: </B> <BR>
 * email was sent successfully by the underlying Email subsystem</LI>
 * <LI><b>Event.FAILED_EVENT: </B> <BR>
 * email was not sent correct by the underlying Email subsystem <BR>
 * email may be partially sent correct, but some parts failed.</LI>
 * <LI><b>Event.CANCELLED_EVENT: </B> <BR>
 * user interaction, i.e. canceled message creation</LI>
 * </UL>
 * <p>
 * <b>Consumes Events from: </b>
 * <UL>
 * <LI>ContactForm:</LI>
 * <UL>
 * <LI>Form.EVENT_FORM_CANCELLED</LI>
 * <LI>Form.EVENT_VALIDATION_OK</LI>
 * </UL>
 * </UL>
 * <P>
 * <b>Main Purpose: </b> is to provide an easy interface for <i>contact message creation and sending </i> from within different OLAT bulding blocks.
 * <P>
 * <b>Responsabilites: </b> <br>
 * <UL>
 * <LI>supplies a workflow for creating and sending contact messages</LI>
 * <LI>works with the ContactList encapsulating the e-mail addresses in a mailing list.</LI>
 * <LI>contact messages with pre-initialized subject and/or body</LI>
 * </UL>
 * <P>
 * TODO:pb:b refactor ContactFormController and ContactForm to extract a ContactMessageManager, setSubject(..) setRecipients.. etc. should not be in the controller.
 * Refactor to use ContactMessage!
 * 
 * @see org.olat.modules.co.ContactList Initial Date: Jul 19, 2004
 * @author patrick
 */
public class ContactFormController extends BasicController {

	OLog log = Tracing.createLoggerFor(this.getClass());
	//
	private final Identity emailFrom;

	private final ContactForm cntctForm;
	private VelocityContainer vcCreateContactMsg;
	private DialogBoxController noUsersErrorCtr;
	private ArrayList<String> myButtons;
	private final Panel main;

	/**
	 * @param ureq
	 * @param windowControl
	 * @param useDefaultTitle
	 * @param isCanceable
	 * @param isReadonly
	 * @param hasRecipientsEditable
	 * @param cmsg
	 */
	public ContactFormController(final UserRequest ureq, final WindowControl windowControl, final boolean useDefaultTitle, final boolean isCanceable,
			final boolean isReadonly, final boolean hasRecipientsEditable, final ContactMessage cmsg) {
		super(ureq, windowControl);

		// init email form
		this.emailFrom = cmsg.getFrom();

		cntctForm = new ContactForm(ureq, windowControl, emailFrom, isReadonly, isCanceable, hasRecipientsEditable);
		listenTo(cntctForm);

		final List recipList = cmsg.getEmailToContactLists();
		final boolean hasAtLeastOneAddress = hasAtLeastOneAddress(recipList);
		cntctForm.setBody(cmsg.getBodyText());
		cntctForm.setSubject(cmsg.getSubject());

		main = new Panel("contactFormMainPanel");

		// init display component
		init(ureq, useDefaultTitle, hasAtLeastOneAddress, cmsg.getDisabledIdentities());
	}

	private boolean hasAtLeastOneAddress(final List recipList) {
		boolean hasAtLeastOneAddress = false;
		if (recipList != null && recipList.size() > 0) {
			for (final Iterator iter = recipList.iterator(); iter.hasNext();) {
				final ContactList cl = (ContactList) iter.next();
				if (!hasAtLeastOneAddress && cl != null && cl.getEmailsAsStrings().size() > 0) {
					hasAtLeastOneAddress = true;
				}
				if (cl.getEmailsAsStrings().size() > 0) {
					cntctForm.addEmailTo(cl);
				}
			}
		}
		return hasAtLeastOneAddress;
	}

	/**
	 * @param useDefaultTitle
	 * @param hasAtLeastOneAddress
	 */
	private void init(final UserRequest ureq, final boolean useDefaultTitle, final boolean hasAtLeastOneAddress, final List<Identity> disabledIdentities) {
		if (hasAtLeastOneAddress) {

			vcCreateContactMsg = createVelocityContainer("c_contactMsg");
			vcCreateContactMsg.put("cntctForm", cntctForm.getInitialComponent());
			main.setContent(vcCreateContactMsg);
			putInitialPanel(main);

		} else {
			final Controller mCtr = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, translate("error.msg.send.no.rcps"));
			listenTo(mCtr);// to be disposed as this controller gets disposed
			putInitialPanel(mCtr.getInitialComponent());
		}
		if (!hasAtLeastOneAddress | disabledIdentities.size() > 0) {
			// show error that message can not be sent
			myButtons = new ArrayList<String>();
			myButtons.add(translate("back"));
			String title = "";
			String message = "";
			if (disabledIdentities.size() > 0) {
				title = MailHelper.getTitleForFailedUsersError(ureq.getLocale());
				message = MailHelper.getMessageForFailedUsersError(ureq.getLocale(), disabledIdentities);
			} else {
				title = translate("error.title.nousers");
				message = translate("error.msg.nousers");
			}
			noUsersErrorCtr = activateGenericDialog(ureq, title, message, myButtons, noUsersErrorCtr);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == noUsersErrorCtr) {
			if (event.equals(Event.CANCELLED_EVENT)) {
				// user has clicked the close button in the top-right corner
				fireEvent(ureq, Event.CANCELLED_EVENT);
			} else {
				// user has clicked the cancel button
				final int pos = DialogBoxUIFactory.getButtonPos(event);
				if (pos == 0) {
					// cancel button has been pressed, fire event to parent
					fireEvent(ureq, Event.CANCELLED_EVENT);
				}
			}
		} else if (source == cntctForm) {
			if (event == Event.DONE_EVENT) {
				final boolean useInstitutionalEmail = false;
				final Emailer emailer = new Emailer(emailFrom, useInstitutionalEmail);
				//
				boolean success = false;
				try {
					final List<File> attachments = cntctForm.getAttachments();
					success = emailer.sendEmail(cntctForm.getEmailToContactLists(), cntctForm.getSubject(), cntctForm.getBody(), attachments);
					if (cntctForm.isTcpFrom()) {
						success = emailer.sendEmailCC(cntctForm.getEmailFrom(), cntctForm.getSubject(), cntctForm.getBody(), attachments);
					}
				} catch (final AddressException e) {
					// error in recipient email address(es)
					handleAddressException(success);
					// no return here, depending on boolean success there are
					// events to fire
				} catch (final SendFailedException e) {
					// error in sending message
					// CAUSE: sender email address invalid
					if (handleSendFailedException(e)) {
						// exception handling says that although the message could not be
						// send we should proceed and finish this workflow with a failed event
						fireEvent(ureq, Event.FAILED_EVENT);
						return;
					} else {
						fireEvent(ureq, Event.FAILED_EVENT);
						return;
					}
				} catch (final MessagingException e) {
					// error in message-subject || .-body
					handleMessagingException();
					// fireEvent(ureq, Event.FAILED_EVENT);
					return;
				}
				cntctForm.setDisplayOnly(true);
				if (success) {
					showInfo("msg.send.ok");
					// do logging
					ThreadLocalUserActivityLogger.log(MailLoggingAction.MAIL_SENT, getClass());
					fireEvent(ureq, Event.DONE_EVENT);
				} else {
					showInfo("error.msg.send.nok");
					fireEvent(ureq, Event.FAILED_EVENT);
				}
			} else if (event == Event.CANCELLED_EVENT) {
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
		}
	}

	/**
	 * handles events from Components <BR>
	 * i.e. ContactForm and c_contactMsg.html <br>
	 * creates an InfoMessage in the WindowController on error. <br>
	 * <b>Fires: </b>
	 * <UL>
	 * <LI><b>Event.DONE_EVENT: </B> <BR>
	 * email was sent successfully by the underlying Email subsystem</LI>
	 * <LI><b>Event.FAILED_EVENT: </B> <BR>
	 * email was not sent correct by the underlying Email subsystem <BR>
	 * email may be partially sent correct, but some parts failed.</LI>
	 * <LI><b>Event.CANCELLED_EVENT: </B> <BR>
	 * user interaction, i.e. canceled message creation</LI>
	 * </UL>
	 * <p>
	 * 
	 * @param ureq
	 * @param source
	 * @param event
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	/**
	 */
	private void handleMessagingException() {
		String infoMessage = translate("error.msg.send.nok");
		infoMessage += "<br />";
		infoMessage += translate("error.msg.content.nok");
		this.getWindowControl().setError(infoMessage);
	}

	/**
	 * @param success
	 */
	private void handleAddressException(final boolean success) {
		final StringBuilder errorMessage = new StringBuilder();
		if (success) {
			errorMessage.append(translate("error.msg.send.partially.nok"));
			errorMessage.append("<br />");
			errorMessage.append(translate("error.msg.send.invalid.rcps"));
		} else {
			errorMessage.append(translate("error.msg.send.nok"));
			errorMessage.append("<br />");
			errorMessage.append(translate("error.msg.send.553"));
		}
		this.getWindowControl().setError(errorMessage.toString());
	}

	/**
	 * handles the sendFailedException
	 * <p>
	 * generates an infoMessage
	 * 
	 * @param e
	 * @throws OLATRuntimeException return boolean true: handling was successful, exception can be ignored; false: handling was not successful, refuse to proceed.
	 */
	private boolean handleSendFailedException(final SendFailedException e) {
		// get wrapped excpetion
		final MessagingException me = (MessagingException) e.getNextException();
		if (me instanceof AuthenticationFailedException) {
			// catch this one separately, this kind of exception has no message
			// as the other below
			final StringBuilder infoMessage = new StringBuilder();
			infoMessage.append(translate("error.msg.send.nok"));
			infoMessage.append("<br />");
			infoMessage.append(translate("error.msg.smtp.authentication.failed"));
			this.getWindowControl().setInfo(infoMessage.toString());
			log.warn("Mail message could not be sent: ", e);
			// message could not be sent, however let user proceed with his action
			return true;
		}
		final String message = me.getMessage();
		if (message.startsWith("553")) {
			// javax.mail.MessagingException: 553 5.5.4 <invalid>... Domain name
			// required for sender address invalid@id.unizh.ch
			// javax.mail.MessagingException: 553 5.1.8 <invalid@invalid.>...
			// Domain of sender address invalid@invalid does not exist
			// ...
			final StringBuilder infoMessage = new StringBuilder();
			infoMessage.append(translate("error.msg.send.553"));
			showInfo(infoMessage.toString());

		} else if (message.startsWith("Invalid Addresses")) {
			// javax.mail.SendFailedException: Sending failed;
			// nested exception is:
			// class javax.mail.SendFailedException: Invalid Addresses;
			// nested exception is:
			// class javax.mail.SendFailedException: 550 5.1.1 <dfgh>... User
			// unknownhandleSendFailedException
			final StringBuilder infoMessage = new StringBuilder();
			infoMessage.append(translate("error.msg.send.nok"));
			infoMessage.append("<br />");
			infoMessage.append(translate("error.msg.send.invalid.rcps"));
			infoMessage.append(addressesArr2HtmlOList(e.getInvalidAddresses()));
			this.getWindowControl().setInfo(infoMessage.toString());
		} else if (message.startsWith("503 5.0.0")) {
			// message:503 5.0.0 Need RCPT (recipient) ,javax.mail.MessagingException
			final StringBuilder infoMessage = new StringBuilder();
			infoMessage.append(translate("error.msg.send.nok"));
			infoMessage.append("<br />");
			infoMessage.append(translate("error.msg.send.no.rcps"));
			this.getWindowControl().setInfo(infoMessage.toString());
		} else if (message.startsWith("Unknown SMTP host")) {
			final StringBuilder infoMessage = new StringBuilder();
			infoMessage.append(translate("error.msg.send.nok"));
			infoMessage.append("<br />");
			infoMessage.append(translate("error.msg.unknown.smtp", WebappHelper.getMailConfig("mailFrom")));
			this.getWindowControl().setInfo(infoMessage.toString());
			log.warn("Mail message could not be sent: ", e);
			// message could not be sent, however let user proceed with his action
			return true;
		} else if (message.startsWith("Could not connect to SMTP host")) {
			// could not connect to smtp host, no connection or connection timeout
			final StringBuilder infoMessage = new StringBuilder();
			infoMessage.append(translate("error.msg.send.nok"));
			infoMessage.append("<br />");
			infoMessage.append(translate("error.msg.notconnectto.smtp", WebappHelper.getMailConfig("mailhost")));
			this.getWindowControl().setInfo(infoMessage.toString());
			log.warn(null, e);
			// message could not be sent, however let user proceed with his action
			return true;
		} else {
			throw new OLATRuntimeException(ContactFormController.class, "" + cntctForm.getEmailTo(), e.getNextException());
		}
		// message could not be sent, return false
		return false;
	}

	/**
	 * converts an Address[] to an HTML ordered list
	 * 
	 * @param invalidAdr Address[] with invalid addresses
	 * @return StringBuilder
	 */
	private StringBuilder addressesArr2HtmlOList(final Address[] invalidAdr) {
		final StringBuilder iAddressesSB = new StringBuilder();
		if (invalidAdr != null && invalidAdr.length > 0) {
			iAddressesSB.append("<ol>");
			for (int i = 0; i < invalidAdr.length; i++) {
				iAddressesSB.append("<li>");
				iAddressesSB.append(invalidAdr[i].toString());
				iAddressesSB.append("</li>");
			}
			iAddressesSB.append("</ol>");
		}
		return iAddressesSB;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

}