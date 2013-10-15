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
package org.olat.repository.controllers;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.SecurityGroup;
import org.olat.catalog.CatalogManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.Form;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormReset;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.WizardController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailNotificationEditController;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.MailerWithTemplate;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.group.ui.BGConfigFlags;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryStatus;
import org.olat.repository.RepositoryManager;

/**
 * Description:<br>
 * A wizard to close a course.
 * <P>
 * Initial Date: 28.10.2008 <br>
 * 
 * @author bja <bja@bps-system.de>
 */
public class WizardCloseCourseController extends WizardController implements WizardCloseResourceController {

	private static final int NUM_STEPS = 3; // 3 steps
	public static final String COURSE_CLOSED = "course.closed";
	private RepositoryEntry repositoryEntry;

	private final VelocityContainer mainVc;
	private final Panel panel;
	private Link nextStep1;
	private MailNotificationEditController mailNotificationCtr;
	private VelocityContainer step1Vc;
	private VelocityContainer sendNotificationVC;
	private CloseRessourceOptionForm formStep2;

	public WizardCloseCourseController(final UserRequest ureq, final WindowControl control, final RepositoryEntry repositoryEntry) {
		super(ureq, control, NUM_STEPS);
		setBasePackage(RepositoryManager.class);
		this.repositoryEntry = repositoryEntry;

		this.mainVc = createVelocityContainer("wizard");
		this.panel = new Panel("panel");
	}

	@Override
	public void startWorkflow() {
		buildStep1();
		mainVc.put("panel", panel);

		this.setWizardTitle(translate("wizard.closecourse.title"));
		this.setNextWizardStep(translate("close.ressource.step1"), mainVc);
	}

	/**
	 * @param ureq
	 */
	private void buildStep1() {
		step1Vc = createVelocityContainer("step1_wizard_close_resource");
		nextStep1 = LinkFactory.createButtonSmall("next", step1Vc, this);
		panel.setContent(step1Vc);
	}

	/**
	 * @param ureq
	 */
	private void buildStep2(final UserRequest ureq) {
		formStep2 = new CloseRessourceOptionForm(ureq, getWindowControl());
		listenTo(formStep2);
		panel.setContent(formStep2.getInitialComponent());
	}

	/**
	 * @param ureq
	 */
	private void buildStep3(final UserRequest ureq) {
		final String courseTitle = "'" + repositoryEntry.getDisplayname() + "'";
		final MailTemplate mailTempl = createMailTemplate(
				translate("wizard.step3.mail.subject", new String[] { courseTitle }),
				translate(
						"wizard.step3.mail.body",
						new String[] {
								courseTitle,
								ureq.getIdentity().getUser().getProperty(UserConstants.FIRSTNAME, null) + " "
										+ ureq.getIdentity().getUser().getProperty(UserConstants.LASTNAME, null) }));
		if (mailNotificationCtr != null) {
			mailNotificationCtr.dispose();
		}
		mailNotificationCtr = new MailNotificationEditController(getWindowControl(), ureq, mailTempl, false);
		mailNotificationCtr.addControllerListener(this);
		sendNotificationVC = createVelocityContainer("sendnotification");
		sendNotificationVC.put("notificationForm", mailNotificationCtr.getInitialComponent());
		panel.setContent(sendNotificationVC);
	}

	/**
	 * Create default template which fill in context 'firstname' , 'lastname' and 'username'.
	 * 
	 * @param subject
	 * @param body
	 * @return
	 */
	private MailTemplate createMailTemplate(final String subject, final String body) {
		return new MailTemplate(subject, body, null) {
			@Override
			public void putVariablesInMailContext(final VelocityContext context, final Identity identity) {
				// nothing to do
			}
		};
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		super.event(ureq, source, event);
		// forward to step 2
		if (source == nextStep1) {
			buildStep2(ureq);
			this.setNextWizardStep(translate("close.ressource.step2"), mainVc);
		}
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == mailNotificationCtr && event == Event.DONE_EVENT) {
			if (mailNotificationCtr.getMailTemplate() != null) {
				List<Identity> ownerList = new ArrayList<Identity>();
				// owners
				final BaseSecurity securityManager = BaseSecurityManager.getInstance();
				final SecurityGroup owners = repositoryEntry.getOwnerGroup();
				if (securityManager.isIdentityInSecurityGroup(ureq.getIdentity(), owners)) {
					ownerList = securityManager.getIdentitiesOfSecurityGroup(owners);
				}
				List<Identity> ccIdentities = new ArrayList<Identity>();
				if (mailNotificationCtr.getMailTemplate().getCpfrom()) {
					ccIdentities.add(ureq.getIdentity());
				} else {
					ccIdentities = null;
				}
				final MailerResult mailerResult = MailerWithTemplate.getInstance().sendMailAsSeparateMails(ownerList, ccIdentities, null,
						mailNotificationCtr.getMailTemplate(), ureq.getIdentity());
				final StringBuilder errorMessage = new StringBuilder();
				final StringBuilder warningMessage = new StringBuilder();
				MailHelper.appendErrorsAndWarnings(mailerResult, errorMessage, warningMessage, ureq.getLocale());
				if (warningMessage.length() > 0) {
					getWindowControl().setWarning(warningMessage.toString());
				}
				if (errorMessage.length() > 0) {
					getWindowControl().setError(errorMessage.toString());
				}
				ownerList.clear();
			}
			repositoryEntry = (RepositoryEntry) DBFactory.getInstance().loadObject(repositoryEntry);
			// set ICourse property
			setCourseProperty();
			// clean catalog
			if ((formStep2 != null) && (formStep2.isCheckboxCleanCatalog())) {
				doCleanCatalog();
			}
			// clean groups
			if ((formStep2 != null) && (formStep2.isCheckboxCleanGroups())) {
				doCleanGroups(ureq.getIdentity());
			}

			if (mailNotificationCtr != null) {
				mailNotificationCtr.dispose();
				mailNotificationCtr = null;
			}
			fireEvent(ureq, Event.DONE_EVENT);
		} else if (source == mailNotificationCtr && event == Event.CANCELLED_EVENT) {
			mailNotificationCtr.dispose();
			mailNotificationCtr = null;
			fireEvent(ureq, Event.CANCELLED_EVENT);
		}
		// forward to step 3
		if (source == formStep2) {
			if (event.equals(Event.DONE_EVENT)) {
				buildStep3(ureq);
				this.setNextWizardStep(translate("close.ressource.step3"), mainVc);
			} else if (event.equals(Event.BACK_EVENT)) {
				buildStep1();
				this.setBackWizardStep(translate("close.ressource.step1"), mainVc);
			}

		}
	}

	/**
	 * do unsubscribe all group members from this course
	 */
	private void doCleanGroups(final Identity identity) {
		final ICourse course = CourseFactory.loadCourse(repositoryEntry.getOlatResource());
		if (course != null) {
			final BaseSecurity securityManager = BaseSecurityManager.getInstance();
			SecurityGroup secGroupOwner = null;
			SecurityGroup secGroupPartipiciant = null;
			SecurityGroup secGroupWaiting = null;
			// LearningGroups
			List allGroups = course.getCourseEnvironment().getCourseGroupManager().getAllLearningGroupsFromAllContexts();
			final BGConfigFlags flagsLearning = BGConfigFlags.createLearningGroupDefaultFlags();
			for (final Object bGroup : allGroups) {
				secGroupOwner = ((BusinessGroup) bGroup).getOwnerGroup();
				secGroupPartipiciant = ((BusinessGroup) bGroup).getPartipiciantGroup();
				BusinessGroupManagerImpl.getInstance().removeOwnersAndFireEvent(identity, securityManager.getIdentitiesOfSecurityGroup(secGroupOwner),
						((BusinessGroup) bGroup), flagsLearning);
				BusinessGroupManagerImpl.getInstance().removeParticipantsAndFireEvent(identity, securityManager.getIdentitiesOfSecurityGroup(secGroupPartipiciant),
						((BusinessGroup) bGroup), flagsLearning);
				if (((BusinessGroup) bGroup).getWaitingListEnabled()) {
					secGroupWaiting = ((BusinessGroup) bGroup).getWaitingGroup();
					BusinessGroupManagerImpl.getInstance().removeFromWaitingListAndFireEvent(identity, securityManager.getIdentitiesOfSecurityGroup(secGroupWaiting),
							((BusinessGroup) bGroup), flagsLearning);
				}
			}
			// RightGroups
			allGroups.clear();
			allGroups = course.getCourseEnvironment().getCourseGroupManager().getAllRightGroupsFromAllContexts();
			final BGConfigFlags flagsRightgroup = BGConfigFlags.createRightGroupDefaultFlags();
			for (final Object bGroup : allGroups) {
				secGroupPartipiciant = ((BusinessGroup) bGroup).getPartipiciantGroup();
				BusinessGroupManagerImpl.getInstance().removeParticipantsAndFireEvent(identity, securityManager.getIdentitiesOfSecurityGroup(secGroupPartipiciant),
						((BusinessGroup) bGroup), flagsRightgroup);
			}
		}
	}

	/**
	 * clean all references from catalog
	 */
	private void doCleanCatalog() {
		CatalogManager.getInstance().resourceableDeleted(repositoryEntry);
	}

	/**
	 * Set ICourse property COURSE_CLOSED
	 */
	private void setCourseProperty() {
		repositoryEntry.setStatusCode(RepositoryEntryStatus.REPOSITORY_STATUS_CLOSED);
		DBFactory.getInstance().updateObject(repositoryEntry);
	}
}

class CloseRessourceOptionForm extends FormBasicController {

	private FormSubmit submit;
	private MultipleSelectionElement checkboxClean;
	private FormReset back;
	private final Translator translator;
	private static final String PACKAGE = RepositoryManager.class.getPackage().getName();

	public CloseRessourceOptionForm(final UserRequest ureq, final WindowControl control) {
		super(ureq, control);
		this.translator = new PackageTranslator(PACKAGE, ureq.getLocale());
		initForm(ureq);
	}

	public boolean isCheckboxCleanCatalog() {
		return checkboxClean.isSelected(0);
	}

	public boolean isCheckboxCleanGroups() {
		return checkboxClean.isSelected(1);
	}

	@Override
	protected void doDispose() {
		// nothing to do
	}

	@Override
	@SuppressWarnings("unused")
	protected void formOK(final UserRequest ureq) {
		// nothing to do
	}

	@Override
	@SuppressWarnings("unused")
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (event.getCommand().equals(Form.EVNT_VALIDATION_OK.getCommand())) {
			fireEvent(ureq, Event.DONE_EVENT);
		} else if (event.getCommand().equals(FormEvent.RESET.getCommand())) {
			fireEvent(ureq, Event.BACK_EVENT);
		}
	}

	@Override
	@SuppressWarnings("unused")
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {

		final String[] keys = new String[] { "form.clean.catalog", "form.clean.groups" };
		final String[] values = new String[] { translator.translate("form.clean.catalog"), translator.translate("form.clean.groups") };
		checkboxClean = uifactory.addCheckboxesVertical("form.clean.catalog", null, formLayout, keys, values, null, 1);

		submit = new FormSubmit("next", "next");
		back = new FormReset("back", "back");
		final FormLayoutContainer horizontalL = FormLayoutContainer.createHorizontalFormLayout("horiz", getTranslator());
		formLayout.add(horizontalL);
		horizontalL.add(back);
		horizontalL.add(submit);

	}

}
