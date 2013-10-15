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

package org.olat.core.gui.media;

import java.io.File;

/**
 * Description:<BR/>
 * <p>
 * The mimed file media resource explicitly uses the given mime type instead of the mime type compiled from the file name.
 * <p>
 * Initial Date: Mar 7, 2005
 * 
 * @author Felix Jost
 */
public class MimedFileMediaResource extends FileMediaResource {

	private final String mimeType;

	/**
	 * @param file
	 * @param mimeType
	 * @param deliverAsAttachment true: deliver as attachment; false: deliver inline
	 */
	public MimedFileMediaResource(File file, String mimeType, boolean deliverAsAttachment) {
		super(file, deliverAsAttachment);
		this.mimeType = mimeType;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getContentType()
	 */
	@Override
	public String getContentType() {
		return mimeType;
	}
}
