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
package org.olat.upgrade;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.hibernate.Hibernate;
import org.hibernate.type.Type;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.admin.user.delete.service.UserFileDeletionManager;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.commons.modules.bc.meta.MetaInfoFileImpl;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.Persistable;
import org.olat.core.id.User;
import org.olat.core.logging.AssertException;
import org.olat.core.util.xml.XMLParser;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.modules.dialog.DialogElement;
import org.olat.modules.dialog.DialogElementsPropertyManager;
import org.olat.modules.dialog.DialogPropertyElements;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;

/**
 * Description:<br>
 * Automatic upgrade code for the OLAT 6.1.1 release Migrate deleted user data, delete user completly (deleted user name can used again).
 * <P>
 * Initial Date: 12.03.2009 <br>
 * 
 * @author Christian Guretzki
 */
public class OLATUpgrade_6_1_1 extends OLATUpgrade {
	private static final String VERSION = "OLAT_6.1.1";
	private static final String TASK_RENAME_DELETED_USER = "Rename deleted username to <timestamp>_bks_<USRENAME>";
	private static final String TASK_REPLACE_AUTHOR_WITH_IDENTITY_KEY = "Replace author-name with identity-key in MetaData-files";
	private static final String TASK_CLEANUP_USERDATA_OF_DELETEDUSER = "Cleanup userdata of deleted user(dropbox, returnboxof task-node, meta data, qti-editor)";
	private static final String TASK_MIGRATE_ALL_DIALOG_ELEMENTS_PROPERTY = "Replace user-name with identity-key in file-dialog properties for all users";

	// migrate only one deleted-user for testing
	private final boolean testMode = false;
	private final UserFileDeletionManager userFileDeletionManager;

	public OLATUpgrade_6_1_1(final UserFileDeletionManager userFileDeletionManager) {
		this.userFileDeletionManager = userFileDeletionManager;
	}

	/**
	 * @see org.olat.upgrade.OLATUpgrade#getVersion()
	 */
	@Override
	public String getVersion() {
		return VERSION;
	}

	/**
	 * @see org.olat.upgrade.OLATUpgrade#doPreSystemInitUpgrade(org.olat.upgrade.UpgradeManager)
	 */
	@Override
	public boolean doPreSystemInitUpgrade(final UpgradeManager upgradeManager) {
		return false;
	}

	/**
	 * @see org.olat.upgrade.OLATUpgrade#doPostSystemInitUpgrade(org.olat.upgrade.UpgradeManager)
	 */
	@Override
	public boolean doPostSystemInitUpgrade(final UpgradeManager upgradeManager) {
		UpgradeHistoryData uhd = upgradeManager.getUpgradesHistory(VERSION);
		if (uhd == null) {
			// has never been called, initialize
			uhd = new UpgradeHistoryData();
		} else {
			if (uhd.isInstallationComplete()) { return false; }
		}
		long startTime = 0;
		if (log.isDebug()) {
			startTime = System.currentTimeMillis();
		}
		renameDeletedUserNames(upgradeManager, uhd);
		if (log.isDebug()) {
			log.debug("OLATUpgrade_6_1_1: renameDeletedUserNames takes " + (System.currentTimeMillis() - startTime) + "ms");
			startTime = System.currentTimeMillis();
		}
		replaceAuthorNameWithIdentityKeyInMetaData(upgradeManager, uhd);
		if (log.isDebug()) {
			log.debug("OLATUpgrade_6_1_1: replaceAuthorNameWithIdentityKeyInMetaData takes " + (System.currentTimeMillis() - startTime) + "ms");
			startTime = System.currentTimeMillis();
		}
		cleanUpDeletedUserData(upgradeManager, uhd);
		if (log.isDebug()) {
			log.debug("OLATUpgrade_6_1_1: cleanUpDeletedUserData takes " + (System.currentTimeMillis() - startTime) + "ms");
			startTime = System.currentTimeMillis();
		}
		migrateAllDialogElementsProperty(upgradeManager, uhd);
		if (log.isDebug()) {
			log.debug("OLATUpgrade_6_1_1: migrateAllDialogElementsProperty takes " + (System.currentTimeMillis() - startTime) + "ms");
			startTime = System.currentTimeMillis();
		}

		// mark upgrade as finished, never run it again
		uhd.setInstallationComplete(true);
		upgradeManager.setUpgradesHistory(uhd, VERSION);
		return true;
	}

	/**
	 * Deletes the guest users from 6.0 release, the guest users are now created using other user names using an automated naming schema
	 * 
	 * @param upgradeManager
	 * @param uhd
	 */
	private void renameDeletedUserNames(final UpgradeManager upgradeManager, final UpgradeHistoryData uhd) {
		if (!uhd.getBooleanDataValue(TASK_RENAME_DELETED_USER)) {
			log.audit("+---------------------------------------------------------------+");
			log.audit("+... Rename deleted username to <TIMESTAMP>_bks_<USRENAME>   ...+");
			log.audit("+---------------------------------------------------------------+");
			int counter = 0;
			// keep email only -> change login-name
			if (!UserDeletionManager.isKeepUserLoginAfterDeletion()) {
				// loop over all deleted-users
				final BaseSecurity secMgr = BaseSecurityManager.getInstance();
				final List<Identity> identitiesList = secMgr.getIdentitiesByPowerSearch(null, null, true, null, null, null, null, null, null, null,
						Identity.STATUS_DELETED);
				for (final Iterator<Identity> iterator = identitiesList.iterator(); iterator.hasNext();) {
					final Identity identity = iterator.next();
					if (!identity.getName().contains(UserDeletionManager.DELETED_USER_DELIMITER)) {
						if (!(testMode && (counter > 0))) {
							final String oldName = identity.getName();
							final String newName = UserDeletionManager.getInstance().getBackupStringWithDate(oldName);
							identity.setName(newName);
							DBFactory.getInstance().updateObject(identity);
							log.audit("Rename deleted username from '" + oldName + "' to '" + newName + "'");
							counter++;
						} else {
							log.info("TEST-MODE: Do not rename username '" + identity.getName() + "'");
						}
					}
					if (counter % 10 == 0) {
						DBFactory.getInstance().intermediateCommit();
					}
				}
			}
			// Final commit of all identity changes
			DBFactory.getInstance().intermediateCommit();
			log.audit("Rename " + counter + " deleted username to '<TIMESTAMP>_bks_<USRENAME>'");
			uhd.setBooleanDataValue(TASK_RENAME_DELETED_USER, true);
			upgradeManager.setUpgradesHistory(uhd, VERSION);
		}
	}

	private void replaceAuthorNameWithIdentityKeyInMetaData(final UpgradeManager upgradeManager, final UpgradeHistoryData uhd) {
		if (!uhd.getBooleanDataValue(TASK_REPLACE_AUTHOR_WITH_IDENTITY_KEY)) {
			log.audit("+-----------------------------------------------------------------------------+");
			log.audit("+... Replace author-name with identity-key in MetaData-files for all users ...+");
			log.audit("+-----------------------------------------------------------------------------+");
			final String metaRootDirPath = FolderConfig.getCanonicalMetaRoot();
			final File metaRootDir = new File(metaRootDirPath);
			if (log.isDebug()) {
				log.debug("replaceAuthorNameWithIdentityKeyInMetaData: metaRootDir=" + metaRootDir.getAbsolutePath());
			}
			final long counter = replaceAuthorNameWithIdentityKeyInAllMetaData(metaRootDir, 0 /* subFolderNbr */, 0);
			log.info("replaceAuthorNameWithIdentityKeyInMetaData: replace #" + counter + " user-names in meta files");
			uhd.setBooleanDataValue(TASK_REPLACE_AUTHOR_WITH_IDENTITY_KEY, true);
			upgradeManager.setUpgradesHistory(uhd, VERSION);
		}
	}

	private void cleanUpDeletedUserData(final UpgradeManager upgradeManager, final UpgradeHistoryData uhd) {
		if (!uhd.getBooleanDataValue(TASK_CLEANUP_USERDATA_OF_DELETEDUSER)) {
			log.audit("+---------------------------------------------------------------------------------------------------------------+");
			log.audit("+... Cleanup userdata of deleted user (dropbox, returnbox of task-node, .meta/homes/<USER> data, qti-editor) ...+");
			log.audit("+---------------------------------------------------------------------------------------------------------------+");
			int counter = 0;
			int counterNotCleanup = 0;
			final BaseSecurity secMgr = BaseSecurityManager.getInstance();
			final List<Identity> identitiesList = secMgr.getIdentitiesByPowerSearch(null, null, true, null, null, null, null, null, null, null, Identity.STATUS_DELETED);
			if (log.isDebug()) {
				log.debug("cleanUpDeletedUserData: found #" + identitiesList.size() + " deleted users");
			}
			// loop over all deleted-users
			for (final Iterator<Identity> iterator = identitiesList.iterator(); iterator.hasNext();) {
				final Identity identity = iterator.next();
				if (log.isDebug()) {
					log.debug("cleanUpDeletedUserData: process identity=" + identity);
				}
				if (!(testMode && (counter > 0))) {
					if (!UserDeletionManager.isKeepUserLoginAfterDeletion()) {
						// deleted username are like <TIMESTAMP>_bks_<USRENAME>
						// check if username is already in use again
						final String orginalUserName = extractOrginalUserName(identity.getName());
						if (log.isDebug()) {
							log.debug("cleanUpDeletedUserData: process orginalUserName=" + orginalUserName);
						}
						final Identity foundIdentity = secMgr.findIdentityByName(orginalUserName);
						if (foundIdentity == null) {
							final MigrationIdentity migrationIdentity = new MigrationIdentity(orginalUserName);
							userFileDeletionManager.deleteUserData(migrationIdentity, migrationIdentity.getName());
							log.audit("Cleanup userdata of deleted user '" + identity.getName() + "' orginalUserName=" + orginalUserName);
							counter++;
						} else {
							log.audit("Could NOT cleanup userdata of deleted user '" + identity.getName() + "' because '" + orginalUserName + "' is already in use");
							counterNotCleanup++;
						}
					} else {
						// deleted username are not renamed
						userFileDeletionManager.deleteUserData(identity, identity.getName());
						log.audit("KeepUserLoginAfterDeletion-Mode : Cleanup userdata of deleted user '" + identity.getName() + "'");
					}
				} else {
					log.info("TEST-MODE: Do not cleanup username '" + identity.getName() + "'");
				}
				DBFactory.getInstance(false).commitAndCloseSession();
				if (log.isDebug()) {
					log.debug("cleanUpDeletedUserData: DONE migration of identity=" + identity);
				}
			}
			log.audit("Cleanup userdata of " + counter + " users");
			if (counterNotCleanup > 0) {
				log.audit("Could not cleanup userdata of " + counterNotCleanup + " users because the username is already in use");
			}
			uhd.setBooleanDataValue(TASK_CLEANUP_USERDATA_OF_DELETEDUSER, true);
			upgradeManager.setUpgradesHistory(uhd, VERSION);
		}
	}

	private void migrateAllDialogElementsProperty(final UpgradeManager upgradeManager, final UpgradeHistoryData uhd) {
		if (!uhd.getBooleanDataValue(TASK_MIGRATE_ALL_DIALOG_ELEMENTS_PROPERTY)) {
			log.audit("+-----------------------------------------------------------------------------------+");
			log.audit("+... Replace user-name with identity-key in file-dialog properties for all users ...+");
			log.audit("+-----------------------------------------------------------------------------------+");

			migrateAllDialogElementsProperty();

			uhd.setBooleanDataValue(TASK_REPLACE_AUTHOR_WITH_IDENTITY_KEY, true);
			upgradeManager.setUpgradesHistory(uhd, VERSION);
		}
	}

	/**
	 * @param name deleted-username format like <TIMESTAMP>_bks_<USRENAME>
	 * @return
	 */
	private String extractOrginalUserName(final String name) {
		if (name.indexOf(UserDeletionManager.DELETED_USER_DELIMITER) == -1) { throw new AssertException("Deleted user-name '" + name + "' without delimiter '"
				+ UserDeletionManager.DELETED_USER_DELIMITER + "' "); }
		final String userName = name.substring(name.indexOf(UserDeletionManager.DELETED_USER_DELIMITER) + UserDeletionManager.DELETED_USER_DELIMITER.length(),
				name.length());
		return userName;
	}

	// ////////////////////////////////////////////////////////
	// Replace author-name in MetaData files with identity-id.
	// ////////////////////////////////////////////////////////
	private long replaceAuthorNameWithIdentityKeyInAllMetaData(final File aFile, final int deepness, long fileCounter) {
		final int MAX_RECURSIV_DEEPNESS = 100;
		if (log.isDebug()) {
			log.debug("replaceAuthorNameWithIdentityKeyInAllMetaData: process file=" + aFile.getAbsolutePath());
		}
		if (deepness > MAX_RECURSIV_DEEPNESS) {
			log.error("replaceAuthorNameWithIdentityKeyInAllMetaData: Reach max_recursiv_deepness, metaDir=" + aFile);
			return 0;
		}
		if (aFile.isDirectory()) {
			final File[] subFiles = aFile.listFiles();
			for (int j = 0; j < subFiles.length; j++) {
				fileCounter += replaceAuthorNameWithIdentityKeyInAllMetaData(subFiles[j], deepness + 1, fileCounter);

			}
		} else {
			// it is File
			if (aFile.getName().endsWith(".xml")) {
				try {
					final MetaInfoFileImpl metaInfoFile = new MetaInfoFileImpl(aFile);
					if (metaInfoFile.parseXMLdom(aFile)) {
						// read author direct as XML value and not via getAuthor method because getAuthor is based on Identityreturns '-'
						final String username = getAuthorFromXmlMetaData(aFile);
						Identity identity = BaseSecurityManager.getInstance().findIdentityByName(username);
						if (identity == null) {
							// Could not found identity => try to find as deleted username <TIMESTAMP>_bkp_<USERNAME>
							identity = findDeletedIdentityByName(username);
							if (identity != null) {
								metaInfoFile.setAuthor(identity.getName());
							} else {
								// // Could not found identity as deleted-identity too, warn only when username is not empty
								if (username.trim().length() > 0) {
									log.warn("Could not found identity with username=" + username + " file=" + aFile.getAbsolutePath());
								}
							}
						} else {
							// Set author again to force replacement of username with identity-key
							metaInfoFile.setAuthor(username);
						}
						if (!testMode) {
							metaInfoFile.write();
							if (log.isDebug()) {
								log.debug("replaceAuthorNameWithIdentityKeyInAllMetaData setAuthor=" + username + " in meta file=" + aFile.getAbsolutePath());
							}
						} else {
							log.info("replaceAuthorNameWithIdentityKeyInAllMetaData: TEST-MODE !!! DO NOT WRITE setAuthor=" + username + " in meta file="
									+ aFile.getAbsolutePath());
						}
						fileCounter++;
					}
				} catch (final Exception dex) {
					log.warn("Could not read meta file=" + aFile.getAbsolutePath() + " , DocumentException=", dex);
				}
			} else {
				log.warn("replaceAuthorNameWithIdentityKeyInAllMetaData: found non meta file=" + aFile.getAbsolutePath());
			}
		}
		if (fileCounter % 10 == 0) {
			DBFactory.getInstance().intermediateCommit();
		}
		return fileCounter;
	}

	/**
	 * Must be in sync with MetaInfo class.
	 * 
	 * @param fMeta
	 * @return
	 */
	public String getAuthorFromXmlMetaData(final File fMeta) {
		if (fMeta == null) { return null; }
		FileInputStream in;
		try {
			in = new FileInputStream(fMeta);
		} catch (final FileNotFoundException e) {
			return null;
		}
		final XMLParser xmlp = new XMLParser();
		final Document doc = xmlp.parse(in, false);
		if (doc == null) { return null; }

		// extract data from XML
		final Element root = doc.getRootElement();
		final Element authorElement = root.element("author");
		if (authorElement != null) { return authorElement.getText(); }
		return null;
	}

	// ///////////////////////////////////////////////////////////////
	// Replace author-name in DialogElement entries with identity-id.
	// ///////////////////////////////////////////////////////////////
	public void migrateAllDialogElementsProperty() {
		int counter = 0;
		int counterDialogElement = 0;
		int counterSetAuthor = 0;
		final List properties = findAllProperty();
		// loop over all property
		for (final Iterator iterator = properties.iterator(); iterator.hasNext();) {
			final Property prop = (Property) iterator.next();
			final DialogPropertyElements dialogPropertyElements = (DialogPropertyElements) XStreamHelper.fromXML(prop.getTextValue());
			// loop over all elements
			final List list = dialogPropertyElements.getDialogPropertyElements();
			for (final Iterator iterator2 = list.iterator(); iterator2.hasNext();) {
				final DialogElement dialogElement = (DialogElement) iterator2.next();
				counterDialogElement++;
				try {
					final String author = dialogElement.getAuthor();
					Identity identity = BaseSecurityManager.getInstance().findIdentityByName(author);
					if (identity == null) {
						// Did not found username => try to find as deleted username <TIMESTAMP>_bks_<USERNAME>
						identity = findDeletedIdentityByName(author);
						if (identity != null) {
							log.audit("migrateAllDialogElementsProperty setIdentityId for author=" + author + " with IdentityId=" + identity.getKey());
							dialogElement.setAuthorIdentityId(identity.getKey().toString());
							counter++;
						} else {
							log.warn("migrateAllDialogElementsProperty: Could not found username=" + author);
						}
					} else {
						dialogElement.setAuthor(author);
						counterSetAuthor++;
					}
					if (counterDialogElement % 10 == 0) {
						DBFactory.getInstance().intermediateCommit();
					}
				} catch (final Throwable th) {
					log.warn("migrateAllDialogElementsProperty: Exception in loop over dialog-elements, exception should be handled in DialogElement, " + dialogElement,
							th);
					DBFactory.getInstanceForClosing().rollbackAndCloseSession();
				}
			}
			final String dialogElementsAsXML = XStreamHelper.toXML(dialogPropertyElements);
			prop.setTextValue(dialogElementsAsXML);
			if (!testMode) {
				PropertyManager.getInstance().updateProperty(prop);
			} else {
				log.warn("migrateAllDialogElementsProperty: TEST-MODE !!! DO NOT updateProperty");
			}
		}
		log.info("migrateAllDialogElementsProperty: replace #" + counter + " deleted user-names, call setAuthor #" + counterSetAuthor + " for existing user-names  in #"
				+ counterDialogElement + " DialogElements");
	}

	private List findAllProperty() {
		final PropertyManager propMrg = PropertyManager.getInstance();
		final List elements = propMrg.listProperties(null, null, "CourseModule", null, null, DialogElementsPropertyManager.PROPERTY_NAME);
		return elements;
	}

	public Identity findDeletedIdentityByName(String identityName) {
		if (identityName == null) { throw new AssertException("findIdentitybyName: name was null"); }
		identityName = "%" + UserDeletionManager.DELETED_USER_DELIMITER + identityName;
		final List identities = DBFactory.getInstance().find(
				"select ident from org.olat.basesecurity.IdentityImpl as ident where ident.name like ? and ident.status = ?",
				new Object[] { identityName, Identity.STATUS_DELETED }, new Type[] { Hibernate.STRING, Hibernate.INTEGER });
		final int size = identities.size();
		if (size == 0) { return null; }
		if (size != 1) { throw new AssertException("non unique name in identites: " + identityName); }
		final Identity identity = (Identity) identities.get(0);
		return identity;
	}

}

class MigrationIdentity implements Identity {
	private final String name;

	public MigrationIdentity(final String name) {
		this.name = name;
	}

	@Override
	public Date getLastLogin() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Integer getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User getUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLastLogin(final Date loginDate) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setName(final String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStatus(final Integer newStatus) {
		// TODO Auto-generated method stub

	}

	@Override
	public Date getCreationDate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean equalsByPersistableKey(final Persistable persistable) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Long getKey() {
		// TODO Auto-generated method stub
		return null;
	}

}