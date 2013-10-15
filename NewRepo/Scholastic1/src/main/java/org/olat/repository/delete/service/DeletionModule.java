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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.olat.basesecurity.BaseSecurity;
import org.olat.core.configuration.AbstractOLATModule;
import org.olat.core.configuration.PersistedProperties;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.util.WebappHelper;

/**
 * TODO:cg Documentation Initial Date: 15.06.2006 <br>
 * 
 * @author Christian Guretzki
 */
public class DeletionModule extends AbstractOLATModule {

	private static final String CONF_ARCHIVE_ROOT_PATH = "archiveRootPath";
	private static final String CONF_DELETE_EMAIL_RESPONSE_TO_USER_NAME = "deleteEmailResponseToUserName";
	private static final String CONF_ADMIN_USER_NAME = "adminUserName";
	private static final String DEFAULT_ADMIN_USERNAME = "administrator";
	private String archiveRootPath;
	private String emailResponseTo;
	private Identity adminUserIdentity;
	private BaseSecurity baseSecurityManager;

	/**
	 * [used by spring]
	 */
	private DeletionModule() {
		//
	}

	/**
	 * [used by spring]
	 * 
	 * @param baseSecurityManager
	 */
	public void setBaseSecurityManager(final BaseSecurity baseSecurityManager) {
		this.baseSecurityManager = baseSecurityManager;
	}

	@Override
	protected void initDefaultProperties() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.olat.core.configuration.OLATModule#init(com.anthonyeden.lib.config.Configuration)
	 */
	@Override
	public void init() {
		archiveRootPath = getStringConfigParameter(CONF_ARCHIVE_ROOT_PATH, System.getProperty("java.io.tmpdir") + File.separator + "olatdata" + File.separator
				+ "deleted_archive", false);
		emailResponseTo = getStringConfigParameter(CONF_DELETE_EMAIL_RESPONSE_TO_USER_NAME, WebappHelper.getMailConfig("mailSupport"), false);

		if (!emailResponseTo.contains("@")) {
			final Identity identity = baseSecurityManager.findIdentityByName(emailResponseTo);
			if (identity != null) {
				emailResponseTo = identity.getUser().getProperty(UserConstants.EMAIL, null);
			} else {
				logWarn("Could not find:  " + CONF_DELETE_EMAIL_RESPONSE_TO_USER_NAME + " with name: " + emailResponseTo, null);
				emailResponseTo = WebappHelper.getMailConfig("mailFrom");
			}

		}

		final String adminUserName = getStringConfigParameter(CONF_ADMIN_USER_NAME, "administrator", false);
		if (adminUserName != null) {
			adminUserIdentity = baseSecurityManager.findIdentityByName(adminUserName);
		} else {
			adminUserIdentity = baseSecurityManager.findIdentityByName(DEFAULT_ADMIN_USERNAME);
		}
		logDebug("archiveRootPath=" + archiveRootPath);
		logDebug("emailResponseTo=" + emailResponseTo);
		logDebug("adminUserIdentity=" + adminUserIdentity);

	}

	/**
	 * @return Returns the archiveRootPath.
	 */
	public String getArchiveRootPath() {
		return archiveRootPath;
	}

	/**
	 * @return Returns the deleteEmailFrom.
	 */
	public String getEmailResponseTo() {
		return emailResponseTo;
	}

	public static String getArchiveDatePath() {
		final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		return formatter.format(new Date());
	}

	public Identity getAdminUserIdentity() {
		return adminUserIdentity;
	}

	@Override
	protected void initFromChangedProperties() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPersistedProperties(final PersistedProperties persistedProperties) {
		this.moduleConfigProperties = persistedProperties;
	}

}
