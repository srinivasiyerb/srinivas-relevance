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

import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.core.commons.modules.bc.BriefcaseWebDAVProvider;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.id.Identity;
import org.olat.core.logging.Tracing;

/**
 * Manager for the personal-folder of a user.
 */
public class PersonalFolderManager extends BriefcaseWebDAVProvider implements UserDataDeletable {

	private static PersonalFolderManager instance;

	/**
	 * [spring only]
	 * 
	 * @param userDeletionManager
	 */
	private PersonalFolderManager(final UserDeletionManager userDeletionManager) {
		userDeletionManager.registerDeletableUserData(this);
		instance = this;
	}

	/**
	 * @return Instance of a UserManager
	 */
	public static PersonalFolderManager getInstance() {
		return instance;
	}

	/**
	 * Delete personal-folder homes/<username> (private & public) of an user.
	 */
	@Override
	public void deleteUserData(final Identity identity, final String newDeletedUserName) {
		new OlatRootFolderImpl(getRootPathFor(identity), null).delete();
		Tracing.logDebug("Personal-folder deleted for identity=" + identity, this.getClass());
	}

}
