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

package org.olat.core.util.vfs.callbacks;

import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.Quota;

/**
 * Initial Date: Feb 17, 2004
 * 
 * @author Mike Stock Comment: Implementations of this interface serve the VFS to decide wether read/write/list/delete is allowed for the current item. The callbacks a re
 *         inherited by children in case they do not have a callback on their own.
 */
public interface VFSSecurityCallback {

	/**
	 * @return True if reading is allowed.
	 */
	public boolean canRead();

	/**
	 * @return True if writing is allowed.
	 */
	public boolean canWrite();

	/**
	 * @return True if deleting is allowed.
	 */
	public boolean canDelete();

	/**
	 * @return True if listing is allowed.
	 */
	public boolean canList();

	/**
	 * @return True if copying is allowed.
	 */
	public boolean canCopy();

	/**
	 * @return True if deleting revisions from delete files is allowed
	 */
	public boolean canDeleteRevisionsPermanently();

	/**
	 * @return Return the Quota for this container.
	 */
	public Quota getQuota();

	/**
	 * Set the Quota for this container.
	 * 
	 * @param quota
	 */
	public void setQuota(Quota quota);

	/**
	 * @return the subscriptionContext. If null, then no subscription must be offered.
	 */
	public SubscriptionContext getSubscriptionContext();

}
