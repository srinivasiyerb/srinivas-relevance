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
 * Copyright (c) 2008 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.core.util.notifications;

/**
 * Description:<br>
 * <P>
 * Initial Date: 7 janv. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
public interface NotificationsUpgrade {

	public Publisher ugrade(Publisher publisher);

	/**
	 * Return type of notification-upgrader e.g. Forum, AssessmentManager, FolderModule etc.
	 * 
	 * @return
	 */
	public String getType();

}
