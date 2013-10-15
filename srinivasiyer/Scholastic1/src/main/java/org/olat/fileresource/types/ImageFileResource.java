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

/**
 * Initial Date: Apr 6, 2004
 * 
 * @author Mike Stock
 */
public class ImageFileResource extends FileResource {

	/**
	 * Image file resource type identifier.
	 */
	public static final String TYPE_NAME = "FileResource.IMAGE";

	/**
	 * Standard constructor.
	 */
	public ImageFileResource() {
		super.setTypeName(TYPE_NAME);
	}

	/**
	 * @param f
	 * @return True if is of type.
	 */
	public static boolean validate(final File f) {
		final String filename_ = f.getName().toLowerCase();
		return filename_.endsWith(".jpg") || filename_.endsWith(".jpeg") || filename_.endsWith(".gif") || filename_.endsWith(".tiff") || filename_.endsWith(".img")
				|| filename_.endsWith(".bmp") || filename_.endsWith(".pbm") || filename_.endsWith(".ico") || filename_.endsWith(".pict") || filename_.endsWith(".png");
	}
}
