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
package org.olat.course.nodes.feed;

import org.olat.course.run.userview.NodeEvaluation;
import org.olat.modules.webFeed.FeedSecurityCallback;

/**
 * Feed node security callback based on course node access conditions and user status (admin, guest)
 * <P>
 * Initial Date: Aug 10, 2009 <br>
 * 
 * @author gwassmann
 */
public class FeedNodeSecurityCallback implements FeedSecurityCallback {

	private final NodeEvaluation ne;
	private final boolean isOlatAdmin;
	private final boolean isGuestOnly;

	public FeedNodeSecurityCallback(final NodeEvaluation ne, final boolean isOlatAdmin, final boolean isGuestOnly) {
		this.ne = ne;
		this.isOlatAdmin = isOlatAdmin;
		this.isGuestOnly = isGuestOnly;
	}

	/**
	 * @see org.olat.modules.webFeed.FeedSecurityCallback#mayEditMetadata()
	 */
	@Override
	public boolean mayEditMetadata() {
		if (isGuestOnly) { return false; }
		return ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.webFeed.FeedSecurityCallback#mayCreateItems()
	 */
	@Override
	public boolean mayCreateItems() {
		if (isGuestOnly) { return false; }
		return ne.isCapabilityAccessible("poster") || ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.webFeed.FeedSecurityCallback#mayDeleteItems()
	 */
	@Override
	public boolean mayDeleteItems() {
		if (isGuestOnly) { return false; }
		return ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.webFeed.FeedSecurityCallback#mayEditItems()
	 */
	@Override
	public boolean mayEditItems() {
		if (isGuestOnly) { return false; }
		return ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}
}
