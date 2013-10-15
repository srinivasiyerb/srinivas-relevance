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

package org.olat.course.nodes.projectbroker.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.velocity.VelocityContext;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Formatter;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.MailerWithTemplate;
import org.olat.course.nodes.projectbroker.datamodel.Project;

/**
 * @author guretzki
 */

public class ProjectBrokerMailerImpl implements ProjectBrokerMailer {
	private static final String KEY_ENROLLED_EMAIL_TO_PARTICIPANT_SUBJECT = "mail.enrolled.to.participant.subject";
	private static final String KEY_ENROLLED_EMAIL_TO_PARTICIPANT_BODY = "mail.enrolled.to.participant.body";

	private static final String KEY_ENROLLED_EMAIL_TO_MANAGER_SUBJECT = "mail.enrolled.to.manager.subject";
	private static final String KEY_ENROLLED_EMAIL_TO_MANAGER_BODY = "mail.enrolled.to.manager.body";

	private static final String KEY_CANCEL_ENROLLMENT_EMAIL_TO_PARTICIPANT_SUBJECT = "mail.cancel.enrollment.to.participant.subject";
	private static final String KEY_CANCEL_ENROLLMENT_EMAIL_TO_PARTICIPANT_BODY = "mail.cancel.enrollment.to.participant.body";

	private static final String KEY_CANCEL_ENROLLMENT_EMAIL_TO_MANAGER_SUBJECT = "mail.cancel.enrollment.to.manager.subject";
	private static final String KEY_CANCEL_ENROLLMENT_EMAIL_TO_MANAGER_BODY = "mail.cancel.enrollment.to.manager.body";

	private static final String KEY_PROJECT_CHANGED_EMAIL_TO_PARTICIPANT_SUBJECT = "mail.project.changed.to.participant.subject";
	private static final String KEY_PROJECT_CHANGED_EMAIL_TO_PARTICIPANT_BODY = "mail.project.changed.to.participant.body";

	private static final String KEY_PROJECT_DELETED_EMAIL_TO_PARTICIPANT_SUBJECT = "mail.project.deleted.to.participant.subject";
	private static final String KEY_PROJECT_DELETED_EMAIL_TO_PARTICIPANT_BODY = "mail.project.deleted.to.participant.body";

	private static final String KEY_REMOVE_CANDIDATE_EMAIL_SUBJECT = "mail.remove.candidate.subject";
	private static final String KEY_REMOVE_CANDIDATE_EMAIL_BODY = "mail.remove.candidate.body";
	private static final String KEY_ACCEPT_CANDIDATE_EMAIL_SUBJECT = "mail.accept.candidate.subject";
	private static final String KEY_ACCEPT_CANDIDATE_EMAIL_BODY = "mail.accept.candidate.body";
	private static final String KEY_ADD_CANDIDATE_EMAIL_SUBJECT = "mail.add.candidate.subject";
	private static final String KEY_ADD_CANDIDATE_EMAIL_BODY = "mail.add.candidate.body";
	private static final String KEY_ADD_PARTICIPANT_EMAIL_SUBJECT = "mail.add.participant.subject";
	private static final String KEY_ADD_PARTICIPANT_EMAIL_BODY = "mail.add.participant.body";
	private static final String KEY_REMOVE_PARTICIPANT_EMAIL_SUBJECT = "mail.remove.participant.subject";
	private static final String KEY_REMOVE_PARTICIPANT_EMAIL_BODY = "mail.remove.participant.body";

	private final OLog log = Tracing.createLoggerFor(this.getClass());

	// For Enrollment
	@Override
	public MailerResult sendEnrolledEmailToParticipant(final Identity enrolledIdentity, final Project project, final Translator pT) {
		return sendEmail(enrolledIdentity, project, pT.translate(KEY_ENROLLED_EMAIL_TO_PARTICIPANT_SUBJECT), pT.translate(KEY_ENROLLED_EMAIL_TO_PARTICIPANT_BODY),
				pT.getLocale());
	}

	@Override
	public MailerResult sendEnrolledEmailToManager(final Identity enrolledIdentity, final Project project, final Translator pT) {
		return sendEmailToGroup(project.getProjectLeaderGroup(), enrolledIdentity, project, pT.translate(KEY_ENROLLED_EMAIL_TO_MANAGER_SUBJECT),
				pT.translate(KEY_ENROLLED_EMAIL_TO_MANAGER_BODY), pT.getLocale());
	}

	// For cancel enrollment
	@Override
	public MailerResult sendCancelEnrollmentEmailToParticipant(final Identity enrolledIdentity, final Project project, final Translator pT) {
		return sendEmail(enrolledIdentity, project, pT.translate(KEY_CANCEL_ENROLLMENT_EMAIL_TO_PARTICIPANT_SUBJECT),
				pT.translate(KEY_CANCEL_ENROLLMENT_EMAIL_TO_PARTICIPANT_BODY), pT.getLocale());
	}

	@Override
	public MailerResult sendCancelEnrollmentEmailToManager(final Identity enrolledIdentity, final Project project, final Translator pT) {
		return sendEmailToGroup(project.getProjectLeaderGroup(), enrolledIdentity, project, pT.translate(KEY_CANCEL_ENROLLMENT_EMAIL_TO_MANAGER_SUBJECT),
				pT.translate(KEY_CANCEL_ENROLLMENT_EMAIL_TO_MANAGER_BODY), pT.getLocale());
	}

	// Project change
	@Override
	public MailerResult sendProjectChangedEmailToParticipants(final Identity changer, final Project project, final Translator pT) {
		return sendEmailProjectChanged(project.getProjectParticipantGroup(), changer, project, pT.translate(KEY_PROJECT_CHANGED_EMAIL_TO_PARTICIPANT_SUBJECT),
				pT.translate(KEY_PROJECT_CHANGED_EMAIL_TO_PARTICIPANT_BODY), pT.getLocale());
	}

	@Override
	public MailerResult sendProjectDeletedEmailToParticipants(final Identity changer, final Project project, final Translator pT) {
		return sendEmailProjectChanged(project.getProjectParticipantGroup(), changer, project, pT.translate(KEY_PROJECT_DELETED_EMAIL_TO_PARTICIPANT_SUBJECT),
				pT.translate(KEY_PROJECT_DELETED_EMAIL_TO_PARTICIPANT_BODY), pT.getLocale());
	}

	@Override
	public MailTemplate createRemoveAsCandiadateMailTemplate(final Project project, final Identity projectManager, final Translator pT) {
		return createProjectChangeMailTemplate(project, projectManager, pT.translate(KEY_REMOVE_CANDIDATE_EMAIL_SUBJECT), pT.translate(KEY_REMOVE_CANDIDATE_EMAIL_BODY),
				pT.getLocale());
	}

	@Override
	public MailTemplate createAcceptCandiadateMailTemplate(final Project project, final Identity projectManager, final Translator pT) {
		return createProjectChangeMailTemplate(project, projectManager, pT.translate(KEY_ACCEPT_CANDIDATE_EMAIL_SUBJECT), pT.translate(KEY_ACCEPT_CANDIDATE_EMAIL_BODY),
				pT.getLocale());
	}

	@Override
	public MailTemplate createAddCandidateMailTemplate(final Project project, final Identity projectManager, final Translator pT) {
		return createProjectChangeMailTemplate(project, projectManager, pT.translate(KEY_ADD_CANDIDATE_EMAIL_SUBJECT), pT.translate(KEY_ADD_CANDIDATE_EMAIL_BODY),
				pT.getLocale());
	}

	@Override
	public MailTemplate createAddParticipantMailTemplate(final Project project, final Identity projectManager, final Translator pT) {
		return createProjectChangeMailTemplate(project, projectManager, pT.translate(KEY_ADD_PARTICIPANT_EMAIL_SUBJECT), pT.translate(KEY_ADD_PARTICIPANT_EMAIL_BODY),
				pT.getLocale());
	}

	@Override
	public MailTemplate createRemoveParticipantMailTemplate(final Project project, final Identity projectManager, final Translator pT) {
		return createProjectChangeMailTemplate(project, projectManager, pT.translate(KEY_REMOVE_PARTICIPANT_EMAIL_SUBJECT),
				pT.translate(KEY_REMOVE_PARTICIPANT_EMAIL_BODY), pT.getLocale());
	}

	// ////////////////
	// Private Methods
	// ////////////////
	private MailerResult sendEmail(final Identity enrolledIdentity, final Project project, final String subject, final String body, final Locale locale) {
		final MailTemplate enrolledMailTemplate = this.createMailTemplate(project, enrolledIdentity, subject, body, locale);
		// TODO: cg/12.01.2010 in der Methode sendMailUsingTemplateContext wurden die Variablen nicht ersetzt (Fehler oder falsch angewendet?)
		// als Workaround wurde die Methode sendMailAsSeparateMails verwendet
		final List<Identity> enrolledIdentityList = new ArrayList<Identity>();
		enrolledIdentityList.add(enrolledIdentity);
		final MailerResult mailerResult = MailerWithTemplate.getInstance().sendMailAsSeparateMails(enrolledIdentityList, null, null, enrolledMailTemplate, null);
		log.audit("ProjectBroker: sendEmail to identity.name=" + enrolledIdentity.getName() + " , mailerResult.returnCode=" + mailerResult.getReturnCode());
		return mailerResult;
	}

	private MailerResult sendEmailToGroup(final SecurityGroup group, final Identity enrolledIdentity, final Project project, final String subject, final String body,
			final Locale locale) {
		final MailTemplate enrolledMailTemplate = this.createMailTemplate(project, enrolledIdentity, subject, body, locale);
		// loop over all project manger
		final List<Identity> projectManagerList = BaseSecurityManager.getInstance().getIdentitiesOfSecurityGroup(group);
		final StringBuilder identityNames = new StringBuilder();
		for (final Identity identity : projectManagerList) {
			if (identityNames.length() > 0) {
				identityNames.append(",");
			}
			identityNames.append(identity.getName());
		}
		final MailerResult mailerResult = MailerWithTemplate.getInstance().sendMailAsSeparateMails(projectManagerList, null, null, enrolledMailTemplate, null);
		log.audit("ProjectBroker: sendEmailToGroup: identities=" + identityNames.toString() + " , mailerResult.returnCode=" + mailerResult.getReturnCode());
		return mailerResult;
	}

	private MailerResult sendEmailProjectChanged(final SecurityGroup group, final Identity changer, final Project project, final String subject, final String body,
			final Locale locale) {
		final MailTemplate enrolledMailTemplate = this.createProjectChangeMailTemplate(project, changer, subject, body, locale);
		// loop over all project manger
		final List<Identity> projectManagerList = BaseSecurityManager.getInstance().getIdentitiesOfSecurityGroup(group);
		final StringBuilder identityNames = new StringBuilder();
		for (final Identity identity : projectManagerList) {
			if (identityNames.length() > 0) {
				identityNames.append(",");
			}
			identityNames.append(identity.getName());
		}
		final MailerResult mailerResult = MailerWithTemplate.getInstance().sendMailAsSeparateMails(projectManagerList, null, null, enrolledMailTemplate, null);
		log.audit("ProjectBroker: sendEmailToGroup: identities=" + identityNames.toString() + " , mailerResult.returnCode=" + mailerResult.getReturnCode());
		return mailerResult;
	}

	/**
	 * Create default template which fill in context 'firstname' , 'lastname' and 'username'.
	 * 
	 * @param subject
	 * @param body
	 * @return
	 */
	private MailTemplate createMailTemplate(final Project project, final Identity enrolledIdentity, final String subject, final String body, final Locale locale) {
		final String projectTitle = project.getTitle();
		final String currentDate = Formatter.getInstance(locale).formatDateAndTime(new Date());
		final String firstNameEnrolledIdentity = enrolledIdentity.getUser().getProperty(UserConstants.FIRSTNAME, null);
		final String lastnameEnrolledIdentity = enrolledIdentity.getUser().getProperty(UserConstants.LASTNAME, null);
		final String usernameEnrolledIdentity = enrolledIdentity.getName();

		return new MailTemplate(subject, body, null) {
			@Override
			public void putVariablesInMailContext(final VelocityContext context, final Identity identity) {
				context.put("enrolled_identity_firstname", firstNameEnrolledIdentity);
				context.put("enrolled_identity_lastname", lastnameEnrolledIdentity);
				context.put("enrolled_identity_username", usernameEnrolledIdentity);
				// Put variables from greater context
				context.put("projectTitle", projectTitle);
				context.put("currentDate", currentDate);
			}
		};
	}

	/**
	 * Create default template which fill in context 'firstname' , 'lastname' and 'username'.
	 * 
	 * @param subject
	 * @param body
	 * @return
	 */
	private MailTemplate createProjectChangeMailTemplate(final Project project, final Identity changer, final String subject, final String body, final Locale locale) {
		final String projectTitle = project.getTitle();
		final String currentDate = Formatter.getInstance(locale).formatDateAndTime(new Date());
		final String firstnameProjectManager = changer.getUser().getProperty(UserConstants.FIRSTNAME, null);
		final String lastnameProjectManager = changer.getUser().getProperty(UserConstants.LASTNAME, null);
		final String usernameProjectManager = changer.getName();

		return new MailTemplate(subject, body, null) {
			@Override
			public void putVariablesInMailContext(final VelocityContext context, final Identity identity) {
				// Put variables from greater context
				context.put("projectTitle", projectTitle);
				context.put("currentDate", currentDate);
				context.put("firstnameProjectManager", firstnameProjectManager);
				context.put("lastnameProjectManager", lastnameProjectManager);
				context.put("usernameProjectManager", usernameProjectManager);
			}
		};
	}

}
