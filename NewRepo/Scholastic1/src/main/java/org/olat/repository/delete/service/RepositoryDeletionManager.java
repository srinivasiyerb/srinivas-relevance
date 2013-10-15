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

package org.olat.repository.delete.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.SecurityGroup;
import org.olat.commons.lifecycle.LifeCycleManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.Util;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.MailerWithTemplate;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.delete.SelectionController;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.resource.OLATResourceManager;
import org.olat.resource.references.ReferenceManager;
import org.olat.user.UserDataDeletable;

/**
 * Initial Date: Mar 31, 2004
 * 
 * @author Mike Stock Comment:
 */
public class RepositoryDeletionManager extends BasicManager implements UserDataDeletable {

	private static final String REPOSITORY_ARCHIVE_DIR = "archive_deleted_resources";

	private static final String PROPERTY_CATEGORY = "RepositoryDeletion";
	private static final String LAST_USAGE_DURATION_PROPERTY_NAME = "LastUsageDuration";
	private static final int DEFAULT_LAST_USAGE_DURATION = 24;
	private static final String DELETE_EMAIL_DURATION_PROPERTY_NAME = "DeleteEmailDuration";
	private static final int DEFAULT_DELETE_EMAIL_DURATION = 30;

	private static RepositoryDeletionManager INSTANCE;
	private static final String PACKAGE = Util.getPackageName(SelectionController.class);

	public static final String SEND_DELETE_EMAIL_ACTION = "sendDeleteEmail";
	private static final String REPOSITORY_DELETED_ACTION = "respositoryEntryDeleted";
	private final DeletionModule deletionModule;

	/**
	 * [used by spring]
	 * 
	 * @param userDeletionManager
	 */
	private RepositoryDeletionManager(final UserDeletionManager userDeletionManager, final DeletionModule deletionModule) {
		this.deletionModule = deletionModule;
		userDeletionManager.registerDeletableUserData(this);
		INSTANCE = this;
	}

	/**
	 * @return Singleton.
	 */
	public static RepositoryDeletionManager getInstance() {
		return INSTANCE;
	}

	// ////////////////
	// USER_DELETION
	// ////////////////
	/**
	 * Remove identity as owner and initial author. Used in user-deletion. If there is no other owner and/or author, the olat-administrator (defined in olat.properties)
	 * will be added as owner.
	 * 
	 * @see org.olat.user.UserDataDeletable#deleteUserData(org.olat.core.id.Identity)
	 */
	@Override
	public void deleteUserData(final Identity identity, final String newDeletedUserName) {
		// Remove as owner
		List repoEntries = RepositoryManager.getInstance().queryByOwner(identity, new String[] {}/* no type limit */);
		for (final Iterator iter = repoEntries.iterator(); iter.hasNext();) {
			final RepositoryEntry repositoryEntry = (RepositoryEntry) iter.next();

			BaseSecurityManager.getInstance().removeIdentityFromSecurityGroup(identity, repositoryEntry.getOwnerGroup());
			if (BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(repositoryEntry.getOwnerGroup()) == 0) {
				// This group has no owner anymore => add OLAT-Admin as owner
				BaseSecurityManager.getInstance().addIdentityToSecurityGroup(deletionModule.getAdminUserIdentity(), repositoryEntry.getOwnerGroup());
				logInfo("Delete user-data, add Administrator-identity as owner of repositoryEntry=" + repositoryEntry.getDisplayname());
			}
		}
		// Remove as initial author
		repoEntries = RepositoryManager.getInstance().queryByInitialAuthor(identity.getName());
		for (final Iterator iter = repoEntries.iterator(); iter.hasNext();) {
			final RepositoryEntry repositoryEntry = (RepositoryEntry) iter.next();
			repositoryEntry.setInitialAuthor(deletionModule.getAdminUserIdentity().getName());
			logInfo("Delete user-data, add Administrator-identity as initial-author of repositoryEntry=" + repositoryEntry.getDisplayname());
		}
		logDebug("All owner and initial-author entries in repository deleted for identity=" + identity);
	}

	// ////////////////////
	// REPOSITORY_DELETION
	// ////////////////////
	public void setLastUsageDuration(final int lastUsageDuration) {
		setProperty(LAST_USAGE_DURATION_PROPERTY_NAME, lastUsageDuration);
	}

	public void setDeleteEmailDuration(final int deleteEmailDuration) {
		setProperty(DELETE_EMAIL_DURATION_PROPERTY_NAME, deleteEmailDuration);
	}

	public int getLastUsageDuration() {
		return getPropertyByName(LAST_USAGE_DURATION_PROPERTY_NAME, DEFAULT_LAST_USAGE_DURATION);
	}

	public int getDeleteEmailDuration() {
		return getPropertyByName(DELETE_EMAIL_DURATION_PROPERTY_NAME, DEFAULT_DELETE_EMAIL_DURATION);
	}

	public String sendDeleteEmailTo(final List selectedRepositoryEntries, final MailTemplate mailTemplate, final boolean isTemplateChanged,
			final String key_email_subject, final String key_email_body, final Identity sender, final Translator pT) {
		final StringBuilder buf = new StringBuilder();
		if (mailTemplate != null) {
			final HashMap identityRepositoryList = collectRepositoryEntriesForIdentities(selectedRepositoryEntries);
			// loop over identity list and send email
			for (final Iterator iterator = identityRepositoryList.keySet().iterator(); iterator.hasNext();) {
				final String result = sendEmailToIdentity((Identity) iterator.next(), identityRepositoryList, mailTemplate, isTemplateChanged, key_email_subject,
						key_email_body, sender, pT);
				if (result != null) {
					buf.append(result).append("\n");
				}
			}
		} else {
			// no template => User decides to sending no delete-email, mark only in lifecycle table 'sendEmail'
			for (final Iterator iter = selectedRepositoryEntries.iterator(); iter.hasNext();) {
				final RepositoryEntry repositoryEntry = (RepositoryEntry) iter.next();
				logAudit("Repository-Deletion: Move in 'Email sent' section without sending email, repositoryEntry=" + repositoryEntry);
				markSendEmailEvent(repositoryEntry);
			}
		}
		return buf.toString();
	}

	/**
	 * Loop over all repository-entries and collect repository-entries with the same owner identites
	 * 
	 * @param repositoryList
	 * @return HashMap with Identity as key elements, List of RepositoryEntry as objects
	 */
	private HashMap collectRepositoryEntriesForIdentities(final List repositoryList) {
		final HashMap identityRepositoryList = new HashMap();
		for (final Iterator iter = repositoryList.iterator(); iter.hasNext();) {
			final RepositoryEntry repositoryEntry = (RepositoryEntry) iter.next();

			// Build owner group, list of identities
			final SecurityGroup ownerGroup = repositoryEntry.getOwnerGroup();
			List<Identity> ownerIdentities;
			if (ownerGroup != null) {
				ownerIdentities = BaseSecurityManager.getInstance().getIdentitiesOfSecurityGroup(ownerGroup);
			} else {
				logInfo("collectRepositoryEntriesForIdentities: ownerGroup is null, add adminUserIdentity as owner repositoryEntry=" + repositoryEntry.getDisplayname()
						+ "  repositoryEntry.key=" + repositoryEntry.getKey());
				// Add admin user
				ownerIdentities = new ArrayList<Identity>();
				ownerIdentities.add(deletionModule.getAdminUserIdentity());
			}
			// Loop over owner to collect all repository-entry for each user
			for (final Iterator<Identity> iterator = ownerIdentities.iterator(); iterator.hasNext();) {
				final Identity identity = iterator.next();
				if (identityRepositoryList.containsKey(identity)) {
					final List repositoriesOfIdentity = (List) identityRepositoryList.get(identity);
					repositoriesOfIdentity.add(repositoryEntry);
				} else {
					final List repositoriesOfIdentity = new ArrayList();
					repositoriesOfIdentity.add(repositoryEntry);
					identityRepositoryList.put(identity, repositoriesOfIdentity);
				}
			}

		}
		return identityRepositoryList;
	}

	private String sendEmailToIdentity(final Identity identity, final HashMap identityRepositoryList, final MailTemplate template, final boolean isTemplateChanged,
			final String keyEmailSubject, final String keyEmailBody, final Identity sender, final Translator pT) {
		final MailerWithTemplate mailer = MailerWithTemplate.getInstance();
		template.addToContext("responseTo", deletionModule.getEmailResponseTo());
		if (!isTemplateChanged) {
			// Email template has NOT changed => take translated version of subject and body text
			final Translator identityTranslator = new PackageTranslator(PACKAGE, I18nManager.getInstance().getLocaleOrDefault(
					identity.getUser().getPreferences().getLanguage()));
			template.setSubjectTemplate(identityTranslator.translate(keyEmailSubject));
			template.setBodyTemplate(identityTranslator.translate(keyEmailBody));
		}
		// loop over all repositoriesOfIdentity to build email message
		final StringBuilder buf = new StringBuilder();
		for (final Iterator repoIterator = ((List) identityRepositoryList.get(identity)).iterator(); repoIterator.hasNext();) {
			final RepositoryEntry repositoryEntry = (RepositoryEntry) repoIterator.next();
			buf.append("\n  ").append(repositoryEntry.getDisplayname()).append(" / ").append(trimDescription(repositoryEntry.getDescription(), 60));
		}
		template.addToContext("repositoryList", buf.toString());
		template.putVariablesInMailContext(template.getContext(), identity);
		logDebug(" Try to send Delete-email to identity=" + identity.getName() + " with email=" + identity.getUser().getProperty(UserConstants.EMAIL, null));
		List<Identity> ccIdentities = new ArrayList<Identity>();
		if (template.getCpfrom()) {
			ccIdentities.add(sender);
		} else {
			ccIdentities = null;
		}
		final MailerResult mailerResult = mailer.sendMailUsingTemplateContext(identity, ccIdentities, null, template, sender);
		if (mailerResult.getReturnCode() == MailerResult.OK) {
			// Email sended ok => set deleteEmailDate
			for (final Iterator repoIterator = ((List) identityRepositoryList.get(identity)).iterator(); repoIterator.hasNext();) {
				final RepositoryEntry repositoryEntry = (RepositoryEntry) repoIterator.next();
				logAudit("Repository-Deletion: Delete-email for repositoryEntry=" + repositoryEntry + "send to identity=" + identity.getName());
				markSendEmailEvent(repositoryEntry);
			}
			return null; // Send ok => return null
		} else {
			return pT.translate("email.error.send.failed", new String[] { identity.getUser().getProperty(UserConstants.EMAIL, null), identity.getName() });
		}
	}

	private void markSendEmailEvent(RepositoryEntry repositoryEntry) {
		repositoryEntry = (RepositoryEntry) DBFactory.getInstance().loadObject(repositoryEntry);
		LifeCycleManager.createInstanceFor(repositoryEntry).markTimestampFor(SEND_DELETE_EMAIL_ACTION);
		DBFactory.getInstance().updateObject(repositoryEntry);
	}

	private String trimDescription(final String description, final int maxlength) {
		if (description.length() > (maxlength)) { return description.substring(0, maxlength - 3) + "..."; }
		return description;
	}

	public List getDeletableReprositoryEntries(final int lastLoginDuration) {
		final Calendar lastUsageLimit = Calendar.getInstance();
		lastUsageLimit.add(Calendar.MONTH, -lastLoginDuration);
		logDebug("lastLoginLimit=" + lastUsageLimit);

		// 1. get all ReprositoryEntries with lastusage > x
		String query = "select re from org.olat.repository.RepositoryEntry as re " + " where (re.lastUsage = null or re.lastUsage < :lastUsage)"
				+ " and re.olatResource != null ";
		DBQuery dbq = DBFactory.getInstance().createQuery(query);
		dbq.setDate("lastUsage", lastUsageLimit.getTime());
		final List reprositoryEntries = dbq.list();
		// 2. get all ReprositoryEntries in deletion-process (email send)
		query = "select re from org.olat.repository.RepositoryEntry as re" + " , org.olat.commons.lifecycle.LifeCycleEntry as le" + " where re.key = le.persistentRef "
				+ " and re.olatResource != null " + " and le.persistentTypeName ='" + RepositoryEntry.class.getName() + "'" + " and le.action ='"
				+ SEND_DELETE_EMAIL_ACTION + "' ";
		dbq = DBFactory.getInstance().createQuery(query);
		final List groupsInProcess = dbq.list();
		// 3. Remove all ReprositoryEntries in deletion-process from all unused-ReprositoryEntries
		reprositoryEntries.removeAll(groupsInProcess);
		return filterRepositoryWithReferences(reprositoryEntries);
	}

	private List filterRepositoryWithReferences(final List repositoryList) {
		logDebug("filterRepositoryWithReferences repositoryList.size=" + repositoryList.size());
		final List filteredList = new ArrayList();
		int loopCounter = 0;
		for (final Iterator iter = repositoryList.iterator(); iter.hasNext();) {
			final RepositoryEntry repositoryEntry = (RepositoryEntry) iter.next();
			logDebug("filterRepositoryWithReferences repositoryEntry=" + repositoryEntry);
			logDebug("filterRepositoryWithReferences repositoryEntry.getOlatResource()=" + repositoryEntry.getOlatResource());
			if (OLATResourceManager.getInstance().findResourceable(repositoryEntry.getOlatResource()) != null) {
				if (ReferenceManager.getInstance().getReferencesTo(repositoryEntry.getOlatResource()).size() == 0) {
					filteredList.add(repositoryEntry);
					logDebug("filterRepositoryWithReferences add to filteredList repositoryEntry=" + repositoryEntry);
				} else {
					// repositoryEntry has references, can not be deleted
					logDebug("filterRepositoryWithReferences Do NOT add to filteredList repositoryEntry=" + repositoryEntry);
					if (LifeCycleManager.createInstanceFor(repositoryEntry).lookupLifeCycleEntry(SEND_DELETE_EMAIL_ACTION) != null) {
						LifeCycleManager.createInstanceFor(repositoryEntry).deleteTimestampFor(SEND_DELETE_EMAIL_ACTION);
						logInfo("filterRepositoryWithReferences: found repositoryEntry with references, remove from deletion-process repositoryEntry=" + repositoryEntry);
					}
				}
			} else {
				logError("filterRepositoryWithReferences, could NOT found Resourceable for repositoryEntry=" + repositoryEntry, null);
			}
			if (loopCounter++ % 100 == 0) {
				DBFactory.getInstance().intermediateCommit();
			}
		}
		logDebug("filterRepositoryWithReferences filteredList.size=" + filteredList.size());
		return filteredList;
	}

	public List getReprositoryEntriesInDeletionProcess(final int deleteEmailDuration) {
		final Calendar deleteEmailLimit = Calendar.getInstance();
		deleteEmailLimit.add(Calendar.DAY_OF_MONTH, -(deleteEmailDuration - 1));
		logDebug("deleteEmailLimit=" + deleteEmailLimit);
		final String queryStr = "select re from org.olat.repository.RepositoryEntry as re" + " , org.olat.commons.lifecycle.LifeCycleEntry as le"
				+ " where re.key = le.persistentRef " + " and re.olatResource != null " + " and le.persistentTypeName ='" + RepositoryEntry.class.getName() + "'"
				+ " and le.action ='" + SEND_DELETE_EMAIL_ACTION + "' and le.lcTimestamp >= :deleteEmailDate ";
		final DBQuery dbq = DBFactory.getInstance().createQuery(queryStr);
		dbq.setDate("deleteEmailDate", deleteEmailLimit.getTime());
		return filterRepositoryWithReferences(dbq.list());
	}

	public List getReprositoryEntriesReadyToDelete(final int deleteEmailDuration) {
		final Calendar deleteEmailLimit = Calendar.getInstance();
		deleteEmailLimit.add(Calendar.DAY_OF_MONTH, -(deleteEmailDuration - 1));
		logDebug("deleteEmailLimit=" + deleteEmailLimit);
		final String queryStr = "select re from org.olat.repository.RepositoryEntry as re" + " , org.olat.commons.lifecycle.LifeCycleEntry as le"
				+ " where re.key = le.persistentRef " + " and re.olatResource != null " + " and le.persistentTypeName ='" + RepositoryEntry.class.getName() + "'"
				+ " and le.action ='" + SEND_DELETE_EMAIL_ACTION + "' and le.lcTimestamp < :deleteEmailDate ";
		final DBQuery dbq = DBFactory.getInstance().createQuery(queryStr);
		dbq.setDate("deleteEmailDate", deleteEmailLimit.getTime());
		return filterRepositoryWithReferences(dbq.list());
	}

	public void deleteRepositoryEntries(final UserRequest ureq, final WindowControl wControl, final List repositoryEntryList) {
		for (final Iterator iter = repositoryEntryList.iterator(); iter.hasNext();) {
			final RepositoryEntry repositoryEntry = (RepositoryEntry) iter.next();
			final RepositoryHandler repositoryHandler = RepositoryHandlerFactory.getInstance().getRepositoryHandler(repositoryEntry);
			final File archiveDir = new File(getArchivFilePath());
			if (!archiveDir.exists()) {
				archiveDir.mkdirs();
			}
			final String archiveFileName = repositoryHandler.archive(ureq.getIdentity(), getArchivFilePath(), repositoryEntry);
			logAudit("Repository-Deletion: archived repositoryEntry=" + repositoryEntry + " , archive-file-name=" + archiveFileName);
			RepositoryManager.getInstance().deleteRepositoryEntryWithAllData(ureq, wControl, repositoryEntry);
			LifeCycleManager.createInstanceFor(repositoryEntry).deleteTimestampFor(SEND_DELETE_EMAIL_ACTION);
			LifeCycleManager.createInstanceFor(repositoryEntry).markTimestampFor(REPOSITORY_DELETED_ACTION, createLifeCycleLogDataFor(repositoryEntry));
			logAudit("Repository-Deletion: deleted repositoryEntry=" + repositoryEntry);
			DBFactory.getInstance().intermediateCommit();
		}
	}

	private String createLifeCycleLogDataFor(final RepositoryEntry repositoryEntry) {
		final StringBuilder buf = new StringBuilder();
		buf.append("<repositoryentry>");
		buf.append("<name>").append(repositoryEntry.getDisplayname()).append("</name>");
		buf.append("<description>").append(trimDescription(repositoryEntry.getDescription(), 60)).append("</description>");
		buf.append("<resid>").append(repositoryEntry.getOlatResource().getResourceableId()).append("</resid>");
		buf.append("<initialauthor>").append(repositoryEntry.getInitialAuthor()).append("</initialauthor>");
		buf.append("</repositoryentry>");
		return buf.toString();
	}

	private String getArchivFilePath() {
		return deletionModule.getArchiveRootPath() + File.separator + REPOSITORY_ARCHIVE_DIR + File.separator + DeletionModule.getArchiveDatePath();
	}

	// ////////////////
	// Private Methods
	// ////////////////
	private int getPropertyByName(final String name, final int defaultValue) {
		final List properties = PropertyManager.getInstance().findProperties(null, null, null, PROPERTY_CATEGORY, name);
		if (properties.size() == 0) {
			return defaultValue;
		} else {
			return ((Property) properties.get(0)).getLongValue().intValue();
		}
	}

	private void setProperty(final String propertyName, final int value) {
		final List properties = PropertyManager.getInstance().findProperties(null, null, null, PROPERTY_CATEGORY, propertyName);
		Property property = null;
		if (properties.size() == 0) {
			property = PropertyManager.getInstance().createPropertyInstance(null, null, null, PROPERTY_CATEGORY, propertyName, null, new Long(value), null, null);
		} else {
			property = (Property) properties.get(0);
			property.setLongValue(new Long(value));
		}
		PropertyManager.getInstance().saveProperty(property);
	}

}