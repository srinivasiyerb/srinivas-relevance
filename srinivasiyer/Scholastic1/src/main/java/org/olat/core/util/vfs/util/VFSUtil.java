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

package org.olat.core.util.vfs.util;

import org.olat.core.gui.media.ClasspathMediaResource;
import org.olat.core.util.vfs.StreamedImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;

/**
 * Initial Date: 17.01.2006
 * 
 * @author Felix Jost Description: some utils for the VFS
 */
public class VFSUtil {

	/**
	 * calculates a new vfscontainer and a filename where the container's base is where the file resides.<br>
	 * e.g.<br>
	 * container: /usr/local/olat/courses/123/coursefolder and filename: docs/bla/blu.doc<br>
	 * -> container /usr/local/olat/courses/123/coursefolder/docs/bla and filename: blu.doc
	 * 
	 * @param oldRoot
	 * @param fileName
	 * @return
	 */
	public static ContainerAndFile calculateSubRoot(VFSContainer oldRoot, String fileUri) {
		VFSContainer newC;
		String newFile;
		int sla = fileUri.lastIndexOf('/');
		if (sla != -1) {
			String root = fileUri.substring(0, sla);
			newFile = fileUri.substring(sla + 1);
			newC = (VFSContainer) oldRoot.resolve(root);
		} else {
			newC = oldRoot;
			newFile = fileUri;
		}
		return new ContainerAndFile(newC, newFile);
	}

	/**
	 * Creates a VFS leaf for a file that is somewhere in the classpath.
	 * 
	 * @param clazz The classpath that is used as a base
	 * @param file The filepath relative to the classpath
	 * @return a VFS leaf for the given file
	 */
	public static VFSLeaf createLeafFromClasspath(Class clazz, String file) {
		ClasspathMediaResource cmr = new ClasspathMediaResource(clazz, file);
		StreamedImpl si = new StreamedImpl("vfs-from-classpath", null, cmr.getInputStream());
		return si;
	}

}