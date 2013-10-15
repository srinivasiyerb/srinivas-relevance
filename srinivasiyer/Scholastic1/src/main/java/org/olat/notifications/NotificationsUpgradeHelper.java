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

package org.olat.notifications;

import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.notifications.Publisher;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseModule;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

/**
 * Description:<br>
 * <P>
 * Initial Date: 7 jan. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
public class NotificationsUpgradeHelper {
	static OLog log = Tracing.createLoggerFor(NotificationsUpgradeHelper.class);

	public static boolean checkOLATResourceable(final Publisher publisher) {
		final Long resId = publisher.getResId();
		try {
			final OLATResourceable ores = OresHelper.createOLATResourceableInstance(publisher.getResName(), resId);
			if (ores == null) { return false; }
			final RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(ores, true);
			if (re == null) { return false; }
			return true;
		} catch (final Exception e) {
			return false;
		}
	}

	public static boolean checkCourse(final Publisher publisher) {
		final Long resId = publisher.getResId();
		try {
			final OLATResourceable ores = OresHelper.createOLATResourceableInstance(CourseModule.class, resId);
			if (ores == null) { return false; }
			final RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(ores, true);
			if (re == null) { return false; }
			return true;
		} catch (final Exception e) {
			return false;
		}
	}

	public static String getCourseNodePath(final Publisher publisher) {
		String businessPath = null;
		try {
			final RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(
					OresHelper.createOLATResourceableInstance(CourseModule.class, publisher.getResId()), true);
			businessPath = "[RepositoryEntry:" + re.getKey() + "][CourseNode:" + publisher.getSubidentifier() + "]";
		} catch (final Exception e) {
			// if something went wrong...
			log.warn("error while processing resid: " + publisher.getResId(), e);
		}
		return businessPath;
	}
}
