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

package org.olat.course.groupsandrights.ui;

import org.olat.core.gui.translator.Translator;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.group.context.BGContext;

/**
 * Description:<BR/>
 * TODO: Class Description for DefaultContextTranslationHelper Initial Date: Mar 10, 2005
 * 
 * @author gnaegi
 */
public class DefaultContextTranslationHelper {

	/**
	 * Returns a nicly translated name for the default context. If it is not a default context, nothing will be done and the contexts name will be returned
	 * 
	 * @param context
	 * @param trans
	 * @return String
	 */
	public static String translateIfDefaultContextName(final BGContext context, final Translator trans) {
		String name = context.getName();
		if (name.indexOf(CourseGroupManager.DEFAULT_NAME_LC_PREFIX) == 0) {
			name = trans.translate("default.context") + " " + name.substring(CourseGroupManager.DEFAULT_NAME_LC_PREFIX.length());
		} else if (name.indexOf(CourseGroupManager.DEFAULT_NAME_RC_PREFIX) == 0) {
			name = trans.translate("default.context") + " " + name.substring(CourseGroupManager.DEFAULT_NAME_RC_PREFIX.length());
		}
		return name;
	}

}
