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

import org.olat.core.util.vfs.VFSContainer;

/**
 * Description:<br>
 * TODO: HP_Besitzer Class Description for ContainerAndFile
 * <P>
 * Initial Date: 15.03.2006 <br>
 * 
 * @author HP_Besitzer
 */
public class ContainerAndFile {
	private VFSContainer container;
	private String fileName;

	/**
	 * @param container
	 * @param fileName
	 */
	public ContainerAndFile(VFSContainer container, String fileName) {
		this.container = container;
		this.fileName = fileName;
	}

	/**
	 * @return Returns the container.
	 */
	public VFSContainer getContainer() {
		return container;
	}

	/**
	 * @return Returns the fileName.
	 */
	public String getFileName() {
		return fileName;
	}

}
