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

package org.olat.course.nodes.ta;

import org.olat.core.id.Identity;
import org.olat.core.manager.BasicManager;
import org.olat.course.nodes.CourseNode;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.properties.Property;

/**
 * Store the status of a task. The task status will be stored in the DB property table.
 * <P>
 * Initial Date: 24.04.2006 <br>
 * 
 * @author guretzki
 */
public class StatusManager extends BasicManager {
	private static StatusManager statusManager = new StatusManager();

	// Name used to save user STATUS-FORM data in the properties table
	public static final String PROPERTY_KEY_STATUS = "TASK_STATUS";

	/**
	 * @return Return singleton instance
	 */
	public static StatusManager getInstance() {
		return statusManager;
	}

	private StatusManager() {
		// singleton
	}

	/**
	 * Initializes the user profile with the data from the database
	 * 
	 * @param ident The current Subject
	 */
	public void loadStatusFormData(final StatusForm statusForm, final CourseNode node, final UserCourseEnvironment userCourseEnv) {
		final Identity identity = userCourseEnv.getIdentityEnvironment().getIdentity();
		final CoursePropertyManager cpm = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		Property statusProperty;

		statusProperty = cpm.findCourseNodeProperty(node, identity, null, PROPERTY_KEY_STATUS);
		if (statusProperty == null) {
			// found no status property => init DEFAULT value
			statusForm.setSelectedStatus(StatusForm.STATUS_VALUE_INITIAL);
		} else {
			final String value = statusProperty.getStringValue();
			statusForm.setSelectedStatus(value);
		}

	}

	public void saveStatusFormData(final StatusForm statusForm, final CourseNode node, final UserCourseEnvironment userCourseEnv) {
		final Identity identity = userCourseEnv.getIdentityEnvironment().getIdentity();
		final String selectedKey = statusForm.getSelectedStatus();

		final CoursePropertyManager cpm = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		Property statusProperty;

		statusProperty = cpm.findCourseNodeProperty(node, identity, null, PROPERTY_KEY_STATUS);
		if (statusProperty == null) {
			statusProperty = cpm.createCourseNodePropertyInstance(node, identity, null, PROPERTY_KEY_STATUS, null, null, selectedKey, null);
			cpm.saveProperty(statusProperty);
		} else {
			statusProperty.setStringValue(selectedKey);
			cpm.updateProperty(statusProperty);
		}
	}

}
