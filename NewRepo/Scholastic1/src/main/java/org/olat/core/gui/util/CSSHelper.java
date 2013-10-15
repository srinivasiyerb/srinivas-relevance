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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.core.gui.util;

/**
 * Description:<br>
 * Helper to create css classes
 * <P>
 * Initial Date: 08.01.2010 <br>
 * 
 * @author gnaegi
 */
public class CSSHelper {
	// Filetype icons
	private static final String CSS_CLASS_FILETYPE_FILE_PREFIX = "b_filetype_";
	public static final String CSS_CLASS_FILETYPE_FILE = CSS_CLASS_FILETYPE_FILE_PREFIX + "file";
	public static final String CSS_CLASS_FILETYPE_FOLDER = CSS_CLASS_FILETYPE_FILE_PREFIX + "folder";
	// Standard icons
	public static final String CSS_CLASS_USER = "b_user_icon";
	public static final String CSS_CLASS_GROUP = "b_group_icon";
	// Message icons
	public static final String CSS_CLASS_ERROR = "b_error_icon";
	public static final String CSS_CLASS_WARN = "b_warn_icon";
	public static final String CSS_CLASS_INFO = "b_info_icon";
	public static final String CSS_CLASS_NEW = "b_new_icon";

	/**
	 * Get the icon css class for a file based on the file ending (e.g. hello.pdf)
	 * 
	 * @param fileName
	 * @return
	 */
	public static String createFiletypeIconCssClassFor(String fileName) {
		StringBuilder cssClass;
		// fallback to standard file icon in case the next class does not exist
		cssClass = new StringBuilder(CSS_CLASS_FILETYPE_FILE);
		int typePos = fileName.lastIndexOf(".");
		if (typePos > 0) {
			cssClass.append(' ').append(CSS_CLASS_FILETYPE_FILE_PREFIX).append(fileName.substring(typePos + 1).toLowerCase());
		}
		return cssClass.toString();
	}

}
