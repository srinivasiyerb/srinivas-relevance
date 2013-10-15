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

package org.olat.admin.user.delete.service;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.olat.admin.user.delete.SelectionController;
import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.IdentityImpl;
import org.olat.basesecurity.SecurityGroup;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.lifecycle.LifeCycleManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerExecutor;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.MailerWithTemplate;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.assessment.EfficiencyStatementManager;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;
import org.olat.repository.delete.service.DeletionModule;
import org.olat.user.UserDataDeletable;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * Manager for user-deletion.
 * 
 * @author Christian Guretzki
 */
public class UserDeletionManager extends BasicManager {

	public static final String DELETED_USER_DELIMITER = "_bkp_";
	/** Default value for last-login duration in month. */
	private static final int DEFAULT_LAST_LOGIN_DURATION = 24;
	/** Default value for delete-email duration in days. */
	private static final int DEFAULT_DELETE_EMAIL_DURATION = 30;
	private static final String LAST_LOGIN_DURATION_PROPERTY_NAME = "LastLoginDuration";
	private static final String DELETE_EMAIL_DURATION_PROPERTY_NAME = "DeleteEmailDuration";
	private static final String PROPERTY_CATEGORY = "UserDeletion";

	private static UserDeletionManager INSTANCE;
	public static final String SEND_DELETE_EMAIL_ACTION = "sendDeleteEmail";
	private static final String USER_ARCHIVE_DIR = "archive_deleted_users";
	private static final String USER_DELETED_ACTION = "userdeleted";
	private static boolean keepUserLoginAfterDeletion;
	private static boolean keepUserEmailAfterDeletion;

	private final Set<UserDataDeletable> userDataDeletableResources;

	// Flag used in user-delete to indicate that all deletable managers are initialized
	private boolean managersInitialized = false;
	private final DeletionModule deletionModule;
	private final CoordinatorManager coordinatorManager;

	/**
	 * [used by spring]
	 */
	private UserDeletionManager(final DeletionModule deletionModule, final CoordinatorManager coordinatorManager) {
		this.deletionModule = deletionModule;
		this.coordinatorManager = coordinatorManager;
		userDataDeletableResources = new HashSet<UserDataDeletable>();
		INSTANCE = this;
	}

	/**
	 * @return Singleton.
	 */
	public static UserDeletionManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Send 'delete'- emails to a list of identities. The delete email is an announcement for the user-deletion.
	 * 
	 * @param selectedIdentities
	 * @return String with warning message (e.g. email-address not valid, could not send email). If there is no warning, the return String is empty ("").
	 */
	public String sendUserDeleteEmailTo(final List<Identity> selectedIdentities, final MailTemplate template, final boolean isTemplateChanged,
			final String keyEmailSubject, final String keyEmailBody, final Identity sender, final Translator pT) {
		final StringBuilder buf = new StringBuilder();
		if (template != null) {
			final MailerWithTemplate mailer = MailerWithTemplate.getInstance();
			template.addToContext("responseTo", deletionModule.getEmailResponseTo());
			for (final Iterator iter = selectedIdentities.iterator(); iter.hasNext();) {
				final Identity identity = (Identity) iter.next();
				if (!isTemplateChanged) {
					// Email template has NOT changed => take translated version of subject and body text
					final Translator identityTranslator = Util.createPackageTranslator(SelectionController.class,
							I18nManager.getInstance().getLocaleOrDefault(identity.getUser().getPreferences().getLanguage()));
					template.setSubjectTemplate(identityTranslator.translate(keyEmailSubject));
					template.setBodyTemplate(identityTranslator.translate(keyEmailBody));
				}
				template.putVariablesInMailContext(template.getContext(), identity);
				logDebug(" Try to send Delete-email to identity=" + identity.getName() + " with email=" + identity.getUser().getProperty(UserConstants.EMAIL, null));
				List<Identity> ccIdentities = new ArrayList<Identity>();
				if (template.getCpfrom()) {
					ccIdentities.add(sender);
				} else {
					ccIdentities = null;
				}
				final MailerResult mailerResult = mailer.sendMailUsingTemplateContext(identity, ccIdentities, null, template, sender);
				if (mailerResult.getReturnCode() != MailerResult.OK) {
					buf.append(pT.translate("email.error.send.failed", new String[] { identity.getUser().getProperty(UserConstants.EMAIL, null), identity.getName() }))
							.append("\n");
				}
				logAudit("User-Deletion: Delete-email send to identity=" + identity.getName() + " with email="
						+ identity.getUser().getProperty(UserConstants.EMAIL, null));
				markSendEmailEvent(identity);
			}
		} else {
			// no template => User decides to sending no delete-email, mark only in lifecycle table 'sendEmail'
			for (final Iterator iter = selectedIdentities.iterator(); iter.hasNext();) {
				final Identity identity = (Identity) iter.next();
				logAudit("User-Deletion: Move in 'Email sent' section without sending email, identity=" + identity.getName());
				markSendEmailEvent(identity);
			}
		}
		return buf.toString();
	}

	private void markSendEmailEvent(Identity identity) {
		identity = (Identity) DBFactory.getInstance().loadObject(identity);
		LifeCycleManager.createInstanceFor(identity).markTimestampFor(SEND_DELETE_EMAIL_ACTION);
		DBFactory.getInstance().updateObject(identity);
	}

	/**
	 * Return list of identities which have last-login older than 'lastLoginDuration' parameter. This user are ready to start with user-deletion process.
	 * 
	 * @param lastLoginDuration last-login duration in month
	 * @return List of Identity objects
	 */
	public List getDeletableIdentities(final int lastLoginDuration) {
		final Calendar lastLoginLimit = Calendar.getInstance();
		lastLoginLimit.add(Calendar.MONTH, -lastLoginDuration);
		logDebug("lastLoginLimit=" + lastLoginLimit);
		// 1. get all 'active' identities with lastlogin > x
		String queryStr = "from org.olat.core.id.Identity as ident where ident.status=" + Identity.STATUS_ACTIV
				+ " and (ident.lastLogin = null or ident.lastLogin < :lastLogin)";
		DBQuery dbq = DBFactory.getInstance().createQuery(queryStr);
		dbq.setDate("lastLogin", lastLoginLimit.getTime());
		final List identities = dbq.list();
		// 2. get all 'active' identities in deletion process
		queryStr = "select ident from org.olat.core.id.Identity as ident" + " , org.olat.commons.lifecycle.LifeCycleEntry as le" + " where ident.key = le.persistentRef "
				+ " and le.persistentTypeName ='" + IdentityImpl.class.getName() + "'" + " and le.action ='" + SEND_DELETE_EMAIL_ACTION + "' ";
		dbq = DBFactory.getInstance().createQuery(queryStr);
		final List identitiesInProcess = dbq.list();
		// 3. Remove all identities in deletion-process from all inactive-identities
		identities.removeAll(identitiesInProcess);
		return identities;
	}

	/**
	 * Return list of identities which are in user-deletion-process. user-deletion-process means delete-announcement.email send, duration of waiting for response is not
	 * expired.
	 * 
	 * @param deleteEmailDuration Duration of user-deletion-process in days
	 * @return List of Identity objects
	 */
	public List getIdentitiesInDeletionProcess(final int deleteEmailDuration) {
		final Calendar deleteEmailLimit = Calendar.getInstance();
		deleteEmailLimit.add(Calendar.DAY_OF_MONTH, -(deleteEmailDuration - 1));
		logDebug("deleteEmailLimit=" + deleteEmailLimit);
		final String queryStr = "select ident from org.olat.core.id.Identity as ident" + " , org.olat.commons.lifecycle.LifeCycleEntry as le"
				+ " where ident.key = le.persistentRef " + " and ident.status = " + Identity.STATUS_ACTIV + " and le.persistentTypeName ='"
				+ IdentityImpl.class.getName() + "'" + " and le.action ='" + SEND_DELETE_EMAIL_ACTION + "' and le.lcTimestamp >= :deleteEmailDate ";
		final DBQuery dbq = DBFactory.getInstance().createQuery(queryStr);
		dbq.setDate("deleteEmailDate", deleteEmailLimit.getTime());
		return dbq.list();
	}

	/**
	 * Return list of identities which are ready-to-delete in user-deletion-process. (delete-announcement.email send, duration of waiting for response is expired).
	 * 
	 * @param deleteEmailDuration Duration of user-deletion-process in days
	 * @return List of Identity objects
	 */
	public List getIdentitiesReadyToDelete(final int deleteEmailDuration) {
		final Calendar deleteEmailLimit = Calendar.getInstance();
		deleteEmailLimit.add(Calendar.DAY_OF_MONTH, -(deleteEmailDuration - 1));
		logDebug("deleteEmailLimit=" + deleteEmailLimit);
		final String queryStr = "select ident from org.olat.core.id.Identity as ident" + " , org.olat.commons.lifecycle.LifeCycleEntry as le"
				+ " where ident.key = le.persistentRef " + " and ident.status = " + Identity.STATUS_ACTIV + " and le.persistentTypeName ='"
				+ IdentityImpl.class.getName() + "'" + " and le.action ='" + SEND_DELETE_EMAIL_ACTION + "' and le.lcTimestamp < :deleteEmailDate ";
		final DBQuery dbq = DBFactory.getInstance().createQuery(queryStr);
		dbq.setDate("deleteEmailDate", deleteEmailLimit.getTime());
		return dbq.list();
	}

	/**
	 * @return true when user can be deleted (non deletion-process is still running)
	 */
	public boolean isReadyToDelete() {
		return UserFileDeletionManager.isReadyToDelete();
	}

	/**
	 * Delete all user-data in registered deleteable resources.
	 * 
	 * @param identity
	 * @return true
	 */
	public void deleteIdentity(Identity identity) {
		logInfo("Start deleteIdentity for identity=" + identity);

		final String newName = getBackupStringWithDate(identity.getName());

		// TODO: chg: Workaround: instances each manager which implements UaserDataDeletable interface
		// Each manager register themself as deletable
		// Should be better with new config concept

		// FIXME: it would be better to call the mangers over a common interface which would not need to have references to all mangers here
		if (!managersInitialized) {
			// HomePageConfigManagerImpl.getInstance();
			// DisplayPortraitManager.getInstance();
			// NoteManager.getInstance();
			// PropertyManager.getInstance();
			// BookmarkManager.getInstance();
			// NotificationsManager.getInstance();
			// PersonalFolderManager.getInstance();
			// IQManager.getInstance();
			// QTIResultManager.getInstance();
			// BusinessGroupManagerImpl.getInstance();
			// RepositoryDeletionManager.getInstance();
			// CatalogManager.getInstance();
			CalendarManagerFactory.getInstance(); // the only one that left for refactoring
			// EfficiencyStatementManager.getInstance();
			// UserFileDeletionManager.getInstance();
			managersInitialized = true;
		}

		logInfo("Start EfficiencyStatementManager.archiveUserData for identity=" + identity);
		EfficiencyStatementManager.getInstance().archiveUserData(identity, getArchivFilePath(identity));

		logInfo("Start Deleting user=" + identity);
		for (final Iterator<UserDataDeletable> iter = userDataDeletableResources.iterator(); iter.hasNext();) {
			final UserDataDeletable element = iter.next();
			logInfo("UserDataDeletable-Loop element=" + element);
			element.deleteUserData(identity, newName);
		}
		logInfo("deleteUserProperties user=" + identity.getUser());
		UserManager.getInstance().deleteUserProperties(identity.getUser());
		// Delete all authentications for certain identity
		final List authentications = BaseSecurityManager.getInstance().getAuthentications(identity);
		for (final Iterator iter = authentications.iterator(); iter.hasNext();) {
			final Authentication auth = (Authentication) iter.next();
			logInfo("deleteAuthentication auth=" + auth);
			BaseSecurityManager.getInstance().deleteAuthentication(auth);
			logDebug("Delete auth=" + auth + "  of identity=" + identity);
		}

		// remove identity from its security groups
		final BaseSecurity secMgr = BaseSecurityManager.getInstance();
		final List<SecurityGroup> securityGroups = BaseSecurityManager.getInstance().getSecurityGroupsForIdentity(identity);
		for (final SecurityGroup secGroup : securityGroups) {
			secMgr.removeIdentityFromSecurityGroup(identity, secGroup);
			logInfo("Removing user=" + identity + " from security group=" + secGroup.toString());
		}

		// can be used, if there is once the possibility to delete identities without db-constraints...
		// if neither email nor login should be kept, REALLY DELETE Identity
		/*
		 * if (!keepUserEmailAfterDeletion & !keepUserLoginAfterDeletion){ identity = (Identity)DBFactory.getInstance().loadObject(identity);
		 * DBFactory.getInstance().deleteObject(identity.getUser()); DBFactory.getInstance().deleteObject(identity); } else {
		 */
		identity = (Identity) DBFactory.getInstance().loadObject(identity);
		// keep login-name only -> change email
		if (!keepUserEmailAfterDeletion) {
			final List<UserPropertyHandler> userPropertyHandlers = UserManager.getInstance().getUserPropertyHandlersFor("org.olat.admin.user.UsermanagerUserSearchForm",
					true);
			final User persistedUser = identity.getUser();
			String actualProperty;
			for (final UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
				actualProperty = userPropertyHandler.getName();
				if (actualProperty.equals(UserConstants.EMAIL)) {
					final String oldEmail = userPropertyHandler.getUserProperty(persistedUser, null);
					String newEmail = "";
					if (StringHelper.containsNonWhitespace(oldEmail)) {
						newEmail = getBackupStringWithDate(oldEmail);
					}
					logInfo("Update user-property user=" + persistedUser);
					userPropertyHandler.setUserProperty(persistedUser, newEmail);
				}
			}
		}

		// keep email only -> change login-name
		if (!keepUserLoginAfterDeletion) {
			identity.setName(newName);
		}

		// keep everything, change identity.status to deleted
		logInfo("Change stater identity=" + identity);
		identity.setStatus(Identity.STATUS_DELETED);
		DBFactory.getInstance().updateObject(identity);
		LifeCycleManager.createInstanceFor(identity).deleteTimestampFor(SEND_DELETE_EMAIL_ACTION);
		LifeCycleManager.createInstanceFor(identity).markTimestampFor(USER_DELETED_ACTION, createLifeCycleLogDataFor(identity));
		// }

		// TODO: chg: ev. logAudit at another place
		logAudit("User-Deletion: Delete all userdata for identity=" + identity);
	}

	public String getBackupStringWithDate(final String original) {
		final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
		final String dateStamp = dateFormat.format(new Date());
		return dateStamp + DELETED_USER_DELIMITER + original;
	}

	private String createLifeCycleLogDataFor(final Identity identity) {
		final StringBuilder buf = new StringBuilder();
		buf.append("<identity>");
		buf.append("<username>").append(identity.getName()).append("</username>");
		buf.append("<lastname>").append(identity.getName()).append("</lastname>");
		buf.append("<firstname>").append(identity.getName()).append("</firstname>");
		buf.append("<email>").append(identity.getName()).append("</email>");
		buf.append("</identity>");
		return buf.toString();
	}

	/**
	 * Re-activate an identity, lastLogin = now, reset deleteemaildate = null.
	 * 
	 * @param identity
	 */
	public void setIdentityAsActiv(final Identity anIdentity) {
		coordinatorManager.getCoordinator().getSyncer()
				.doInSync(OresHelper.createOLATResourceableInstance(anIdentity.getClass(), anIdentity.getKey()), new SyncerExecutor() {
					@Override
					public void execute() {
						// o_clusterOK by:fj : must be fast
						final Identity identity = (Identity) DBFactory.getInstance().loadObject(anIdentity, true);
						if (isLogDebugEnabled()) {
							logDebug("setIdentityAsActiv beginSingleTransaction identity=" + identity);
						}
						identity.setLastLogin(new Date());
						final LifeCycleManager lifeCycleManagerForIdenitiy = LifeCycleManager.createInstanceFor(identity);
						if (lifeCycleManagerForIdenitiy.lookupLifeCycleEntry(SEND_DELETE_EMAIL_ACTION) != null) {
							logAudit("User-Deletion: Remove from delete-list identity=" + identity);
							lifeCycleManagerForIdenitiy.deleteTimestampFor(SEND_DELETE_EMAIL_ACTION);
						}
						if (isLogDebugEnabled()) {
							logDebug("setIdentityAsActiv updateObject identity=" + identity);
						}
						DBFactory.getInstance().updateObject(identity);
						if (isLogDebugEnabled()) {
							logDebug("setIdentityAsActiv committed identity=" + identity);
						}
					}
				});
	}

	/**
	 * @return Return duration in days for waiting for reaction on delete-email.
	 */
	public int getDeleteEmailDuration() {
		return getPropertyByName(DELETE_EMAIL_DURATION_PROPERTY_NAME, DEFAULT_DELETE_EMAIL_DURATION);
	}

	/**
	 * @return Return last-login duration in month for user on delete-selection list.
	 */
	public int getLastLoginDuration() {
		return getPropertyByName(LAST_LOGIN_DURATION_PROPERTY_NAME, DEFAULT_LAST_LOGIN_DURATION);
	}

	private int getPropertyByName(final String name, final int defaultValue) {
		final List properties = PropertyManager.getInstance().findProperties(null, null, null, PROPERTY_CATEGORY, name);
		if (properties.size() == 0) {
			return defaultValue;
		} else {
			return ((Property) properties.get(0)).getLongValue().intValue();
		}
	}

	public void setLastLoginDuration(final int lastLoginDuration) {
		setProperty(LAST_LOGIN_DURATION_PROPERTY_NAME, lastLoginDuration);
	}

	public void setDeleteEmailDuration(final int deleteEmailDuration) {
		setProperty(DELETE_EMAIL_DURATION_PROPERTY_NAME, deleteEmailDuration);
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

	/**
	 * Return in spring config defined administrator identity.
	 * 
	 * @return
	 */
	public Identity getAdminIdentity() {
		return deletionModule.getAdminUserIdentity();
	}

	public void registerDeletableUserData(final UserDataDeletable deletableUserDataResource) {
		userDataDeletableResources.add(deletableUserDataResource);
	}

	private File getArchivFilePath(final Identity identity) {
		final String archiveFilePath = deletionModule.getArchiveRootPath() + File.separator + USER_ARCHIVE_DIR + File.separator + DeletionModule.getArchiveDatePath()
				+ File.separator + "del_identity_" + identity.getName();
		final File archiveIdentityRootDir = new File(archiveFilePath);
		if (!archiveIdentityRootDir.exists()) {
			archiveIdentityRootDir.mkdirs();
		}
		return archiveIdentityRootDir;
	}

	/**
	 * Setter method used by spring
	 * 
	 * @param keepUserLoginAfterDeletion The keepUserLoginAfterDeletion to set.
	 */
	public void setKeepUserLoginAfterDeletion(final boolean keepUserLoginAfterDeletion) {
		UserDeletionManager.keepUserLoginAfterDeletion = keepUserLoginAfterDeletion;
	}

	/**
	 * Setter method used by spring
	 * 
	 * @param keepUserEmailAfterDeletion The keepUserEmailAfterDeletion to set.
	 */
	public void setKeepUserEmailAfterDeletion(final boolean keepUserEmailAfterDeletion) {
		UserDeletionManager.keepUserEmailAfterDeletion = keepUserEmailAfterDeletion;
	}

	public static boolean isKeepUserLoginAfterDeletion() {
		return keepUserLoginAfterDeletion;
	}

}
