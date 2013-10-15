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

package org.olat.user;

import java.io.File;

import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.FileUtils;
import org.olat.core.util.xml.XStreamHelper;

/**
 * Description: <br>
 * TODO: alex Class Description for HomePageConfigManagerImpl
 * <P>
 * Initial Date: Jun 3, 2005 <br>
 * 
 * @author Alexander Schneider
 */
public class HomePageConfigManagerImpl extends BasicManager implements HomePageConfigManager {
	// TODO already in basic manager
	private static OLog log = Tracing.createLoggerFor(HomePageConfigManagerImpl.class);
	private static HomePageConfigManagerImpl singleton;

	/**
	 * [spring]
	 * 
	 * @param userDeletionManager
	 */
	private HomePageConfigManagerImpl(final UserDeletionManager userDeletionManager) {
		userDeletionManager.registerDeletableUserData(this);
		singleton = this;
	}

	/**
	 * Singleton pattern
	 * 
	 * @return instance
	 */
	public static HomePageConfigManager getInstance() {
		return singleton;
	}

	/**
	 * @param userName
	 * @return homePageConfig
	 */
	@Override
	public HomePageConfig loadConfigFor(final String userName) {
		HomePageConfig retVal = null;
		File configFile = getConfigFile(userName);
		if (!configFile.exists()) {
			// config file does not exist! create one, init the defaults, save it.
			retVal = loadAndSaveDefaults(userName);
		} else {
			// file exists, load it with XStream, resolve version
			try {
				final Object tmp = XStreamHelper.readObject(configFile);
				if (tmp instanceof HomePageConfig) {
					retVal = (HomePageConfig) tmp;
					retVal.resolveVersionIssues();
					if (!retVal.hasResourceableId()) {
						retVal.setResourceableId(new Long(CodeHelper.getForeverUniqueID()));
					}
					configFile = null;
					saveConfigTo(userName, retVal);
				}
			} catch (final Exception e) {
				log.error("Error while loading homepage config from path::" + configFile.getAbsolutePath() + ", fallback to default configuration", e);
				if (configFile.exists()) {
					configFile.delete();
				}
				retVal = loadAndSaveDefaults(userName);
				// show message to user
			}
		}
		return retVal;
	}

	/**
	 * Private helper to load and create a default homepage configuration
	 * 
	 * @param userName
	 * @return
	 */
	private HomePageConfig loadAndSaveDefaults(final String userName) {
		HomePageConfig retVal;
		retVal = new HomePageConfig();
		retVal.initDefaults();
		retVal.setResourceableId(new Long(CodeHelper.getForeverUniqueID()));
		saveConfigTo(userName, retVal);
		return retVal;
	}

	/**
	 * @param userName
	 * @param homePageConfig
	 */
	@Override
	public void saveConfigTo(final String userName, final HomePageConfig homePageConfig) {
		homePageConfig.setUserName(userName);
		final File configFile = getConfigFile(userName);
		XStreamHelper.writeObject(configFile, homePageConfig);
	}

	/**
	 * the configuration is saved in the user home
	 * 
	 * @param userName
	 * @return the configuration file
	 */
	static File getConfigFile(final String userName) {
		final File userHomePage = getUserHomePageDir(userName);

		/*
		 * String pathHome = FolderConfig.getCanonicalRoot() + FolderConfig.getUserHome(userName); File userHome = new File(pathHome); if (!userHome.exists())
		 * userHome.mkdir();
		 */
		final File homePageConfigFile = new File(userHomePage, HomePageConfigManager.HOMEPAGECONFIG_XML);
		return homePageConfigFile;
	}

	private static File getUserHomePageDir(final String userName) {
		final String pathHomePage = FolderConfig.getCanonicalRoot() + FolderConfig.getUserHomePage(userName);
		final File userHomePage = new File(pathHomePage);
		userHomePage.mkdirs();
		return userHomePage;
	}

	/**
	 * Delete home-page config-file of a certain user.
	 * 
	 * @see org.olat.user.UserDataDeletable#deleteUserData(org.olat.core.id.Identity)
	 */
	@Override
	public void deleteUserData(final Identity identity, final String newDeletedUserName) {
		getConfigFile(identity.getName()).delete();
		FileUtils.deleteDirsAndFiles(getUserHomePageDir(identity.getName()), true, true);
		Tracing.logDebug("Homepage-config file and homepage-dir deleted for identity=" + identity, this.getClass());
	}

}