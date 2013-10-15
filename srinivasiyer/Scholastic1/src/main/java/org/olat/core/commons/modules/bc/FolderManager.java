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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.commons.modules.bc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.commons.modules.bc.meta.MetaInfoFactory;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.helpers.Settings;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.vfs.OlatRelPathImpl;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;

/**
 * Initial Date: 18.12.2002
 * 
 * @author Mike Stock
 */
public class FolderManager extends BasicManager {

	/**
	 * Get this path as a full WebDAV link
	 * 
	 * @return Full link representation.
	 */
	public static String getWebDAVLink() {
		return Settings.getServerContextPathURI() + "/webdav";
	}

	/**
	 * @param bcBase
	 * @param newerThan
	 * @return a List of FileInfo
	 */
	public static List<FileInfo> getFileInfos(final String olatRelPath, Date newerThan) {

		final List<FileInfo> fileInfos = new ArrayList<FileInfo>();
		final long newerThanLong = newerThan.getTime();
		OlatRootFolderImpl rootFolder = new OlatRootFolderImpl(olatRelPath, null);
		getFileInfosRecursively(rootFolder, fileInfos, newerThanLong, olatRelPath.length());
		return fileInfos;
	}

	private static void getFileInfosRecursively(OlatRelPathImpl relPath, List<FileInfo> fileInfos, long newerThan, int basePathlen) {
		if (relPath instanceof VFSLeaf) {
			// is a file
			long lastModified = ((VFSLeaf) relPath).getLastModified();
			if (lastModified > newerThan) {
				MetaInfo meta = MetaInfoFactory.createMetaInfoFor(relPath);
				String bcrootPath = relPath.getRelPath();
				String bcRelPath = bcrootPath.substring(basePathlen);
				fileInfos.add(new FileInfo(bcRelPath, meta, new Date(lastModified)));
			}
		} else {
			// is a folder
			OlatRootFolderImpl container = (OlatRootFolderImpl) relPath;
			for (VFSItem item : container.getItems()) {
				getFileInfosRecursively((OlatRelPathImpl) item, fileInfos, newerThan, basePathlen);
			}
		}
	}

}
