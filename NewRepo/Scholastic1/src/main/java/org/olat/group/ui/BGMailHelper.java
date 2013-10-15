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
 * Description:<br>
 * The MailTemplate holds a mail subject/body template and the according methods to populate the velocity contexts with the user values
 * <P>
 * Usage:<br>
 * Helper to create various mail templates used in the groupmanagement when adding and removing users.
 * <p>
 * Initial Date: 23.11.2006 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH<br>
 *         http://www.frentix.com
 */

package org.olat.group.ui;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.velocity.VelocityContext;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Util;
import org.olat.core.util.filter.FilterFactory;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.mail.MailTemplate;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManager;
import org.olat.group.context.BGContextManager;
import org.olat.group.context.BGContextManagerImpl;
import org.olat.repository.RepoJumpInHandlerFactory;
import org.olat.repository.RepositoryEntry;

public class BGMailHelper {

	/**
	 * The mail templated when adding users to a group. The method chooses automatically the right translator for the given group type to customize the template text
	 * 
	 * @param group
	 * @param actor
	 * @return the generated MailTemplate
	 */
	public static MailTemplate createAddParticipantMailTemplate(final BusinessGroup group, final Identity actor) {
		final String subjectKey = "notification.mail.added.subject";
		final String bodyKey = "notification.mail.added.body";
		return createMailTemplate(group, actor, subjectKey, bodyKey);
	}

	/**
	 * The mail templated when removing users from a group. The method chooses automatically the right translator for the given group type to customize the template text
	 * 
	 * @param group
	 * @param actor
	 * @return the generated MailTemplate
	 */
	public static MailTemplate createRemoveParticipantMailTemplate(final BusinessGroup group, final Identity actor) {
		final String subjectKey = "notification.mail.removed.subject";
		final String bodyKey = "notification.mail.removed.body";
		return createMailTemplate(group, actor, subjectKey, bodyKey);
	}

	/**
	 * The mail templated when deleting a whole group. The method chooses automatically the right translator for the given group type to customize the template text
	 * 
	 * @param group
	 * @param actor
	 * @return the generated MailTemplate
	 */
	public static MailTemplate createDeleteGroupMailTemplate(final BusinessGroup group, final Identity actor) {
		final String subjectKey = "notification.mail.deleted.subject";
		final String bodyKey = "notification.mail.deleted.body";
		return createMailTemplate(group, actor, subjectKey, bodyKey);
	}

	/**
	 * The mail templated when a user added himself to a group. The method chooses automatically the right translator for the given group type to customize the template
	 * text
	 * 
	 * @param group
	 * @param actor
	 * @return the generated MailTemplate
	 */
	public static MailTemplate createAddMyselfMailTemplate(final BusinessGroup group, final Identity actor) {
		final String subjectKey = "notification.mail.added.self.subject";
		final String bodyKey = "notification.mail.added.self.body";
		return createMailTemplate(group, actor, subjectKey, bodyKey);
	}

	/**
	 * The mail templated when a user removed himself from a group. The method chooses automatically the right translator for the given group type to customize the
	 * template text
	 * 
	 * @param group
	 * @param actor
	 * @return the generated MailTemplate
	 */
	public static MailTemplate createRemoveMyselfMailTemplate(final BusinessGroup group, final Identity actor) {
		final String subjectKey = "notification.mail.removed.self.subject";
		final String bodyKey = "notification.mail.removed.self.body";
		return createMailTemplate(group, actor, subjectKey, bodyKey);
	}

	/**
	 * The mail templated when adding users to a waitinglist. The method chooses automatically the right translator for the given group type to customize the template
	 * text
	 * 
	 * @param group
	 * @param actor
	 * @return the generated MailTemplate
	 */
	public static MailTemplate createAddWaitinglistMailTemplate(final BusinessGroup group, final Identity actor) {
		final String subjectKey = "notification.mail.waitingList.added.subject";
		final String bodyKey = "notification.mail.waitingList.added.body";
		return createMailTemplate(group, actor, subjectKey, bodyKey);
	}

	/**
	 * The mail templated when removing users from a waiting list. The method chooses automatically the right translator for the given group type to customize the
	 * template text
	 * 
	 * @param group
	 * @param actor
	 * @return the generated MailTemplate
	 */
	public static MailTemplate createRemoveWaitinglistMailTemplate(final BusinessGroup group, final Identity actor) {
		final String subjectKey = "notification.mail.waitingList.removed.subject";
		final String bodyKey = "notification.mail.waitingList.removed.body";
		return createMailTemplate(group, actor, subjectKey, bodyKey);
	}

	/**
	 * The mail templated when automatically transferring users from the waitinglist to the participants list adding users to a waitinglist. The method chooses
	 * automatically the right translator for the given group type to customize the template text
	 * 
	 * @param group
	 * @param actor
	 * @return the generated MailTemplate
	 */
	public static MailTemplate createWaitinglistTransferMailTemplate(final BusinessGroup group, final Identity actor) {
		final String subjectKey = "notification.mail.waitingList.transfer.subject";
		final String bodyKey = "notification.mail.waitingList.transfer.body";
		return createMailTemplate(group, actor, subjectKey, bodyKey);
	}

	/**
	 * Internal helper - does all the magic
	 * 
	 * @param group
	 * @param actor
	 * @param subjectKey
	 * @param bodyKey
	 * @return
	 */
	private static MailTemplate createMailTemplate(final BusinessGroup group, final Identity actor, final String subjectKey, final String bodyKey) {
		// build learning resources as list of url as string
		final StringBuilder learningResources = new StringBuilder();
		if (group.getGroupContext() != null) {
			final BGContextManager contextManager = BGContextManagerImpl.getInstance();
			final List repoEntries = contextManager.findRepositoryEntriesForBGContext(group.getGroupContext());
			final Iterator iter = repoEntries.iterator();
			while (iter.hasNext()) {
				final RepositoryEntry entry = (RepositoryEntry) iter.next();
				final String title = entry.getDisplayname();
				final String url = RepoJumpInHandlerFactory.buildRepositoryDispatchURI(entry);
				learningResources.append(title);
				learningResources.append(" (");
				learningResources.append(url);
				learningResources.append(")\n");
			}
		}
		final String courselist = learningResources.toString();
		// get group name and description
		final String groupname = group.getName();
		final String groupdescription = FilterFactory.getHtmlTagAndDescapingFilter().filter(group.getDescription());
		// get some data about the actor and fetch the translated subject / body via
		// i18n module
		final String[] bodyArgs = new String[] { actor.getUser().getProperty(UserConstants.FIRSTNAME, null), actor.getUser().getProperty(UserConstants.LASTNAME, null),
				actor.getUser().getProperty(UserConstants.EMAIL, null), actor.getName() };
		final Locale locale = I18nManager.getInstance().getLocaleOrDefault(actor.getUser().getPreferences().getLanguage());
		final Translator trans = BGTranslatorFactory.createBGPackageTranslator(Util.getPackageName(BusinessGroupManager.class), group.getType(), locale);
		String subject = trans.translate(subjectKey);
		String body = trans.translate(bodyKey, bodyArgs);

		subject = subject.replaceAll("\\$groupname", groupname == null ? "" : groupname);
		body = body.replaceAll("\\$groupname", groupname == null ? "" : groupname);
		body = body.replaceAll("\\$groupdescription", groupdescription == null ? "" : groupdescription);
		body = body.replaceAll("\\$courselist", courselist == null ? "" : courselist);

		// create a mail template which all these data
		final MailTemplate mailTempl = new MailTemplate(subject, body, null) {
			@Override
			public void putVariablesInMailContext(final VelocityContext context, final Identity identity) {
				// Put user variables into velocity context
				final User user = identity.getUser();
				context.put("firstname", user.getProperty(UserConstants.FIRSTNAME, null));
				context.put("lastname", user.getProperty(UserConstants.LASTNAME, null));
				context.put("login", identity.getName());
				// Put variables from greater context
				context.put("groupname", groupname);
				context.put("groupdescription", groupdescription);
				context.put("courselist", courselist);
			}
		};
		return mailTempl;
	}
}
