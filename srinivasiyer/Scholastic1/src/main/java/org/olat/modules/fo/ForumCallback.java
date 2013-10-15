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

package org.olat.modules.fo;

import org.olat.core.util.notifications.SubscriptionContext;

/**
 * @author schneider
 */
public interface ForumCallback {

	/**
	 * @return if allowed to open a new forum thread
	 */
	boolean mayOpenNewThread();

	/**
	 * @return if the current user may reply to messages
	 */
	boolean mayReplyMessage();

	/**
	 * @return if allowed to moderate
	 */
	boolean mayEditMessageAsModerator();

	/**
	 * @return if allowed to delete non-owned messages
	 */
	boolean mayDeleteMessageAsModerator();

	/**
	 * @return true if allowed to archive the whole forum or threads of it
	 */
	boolean mayArchiveForum();

	/**
	 * @return true if allowed to filter the whole forum
	 */
	boolean mayFilterForUser();

	/**
	 * @return the subscriptionContext. if null, then no subscription must be offered
	 */
	SubscriptionContext getSubscriptionContext();

}
