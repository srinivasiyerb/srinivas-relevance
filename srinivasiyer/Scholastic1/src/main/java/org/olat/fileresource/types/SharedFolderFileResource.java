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

package org.olat.fileresource.types;

import java.io.File;

import org.olat.modules.sharedfolder.SharedFolderManager;

/**
 * Initial Date: Apr 6, 2004
 * 
 * @author Alexander Schneider
 */
public class SharedFolderFileResource extends FileResource {

	/**
	 * Sharedfolder file resource type.
	 */
	public static final String TYPE_NAME = "FileResource.SHAREDFOLDER";
	/**
	 * name of the folder on the filesystem, not visible for the user
	 */
	public static final String RESOURCE_NAME = "-";

	/**
	 * Standard constructor.
	 */
	public SharedFolderFileResource() {
		super.setTypeName(TYPE_NAME);
	}

	/**
	 * @param f
	 * @return True if is of type.
	 */
	public static boolean validate(final File f) {
		return SharedFolderManager.getInstance().validate(f);
	}
}
