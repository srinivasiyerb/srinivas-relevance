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
package org.olat.modules.webFeed;

/**
 * Feed resource security callback.
 * <P>
 * Initial Date: Aug 11, 2009 <br>
 * 
 * @author gwassmann
 */
public class FeedResourceSecurityCallback implements FeedSecurityCallback {

	private final boolean isAdmin, isOwner;

	public FeedResourceSecurityCallback(final boolean isAdmin, final boolean isOwner) {
		this.isAdmin = isAdmin;
		this.isOwner = isOwner;
	}

	/**
	 * @see org.olat.modules.webFeed.FeedSecurityCallback#mayCreateItems()
	 */
	@Override
	public boolean mayCreateItems() {
		return isAdmin || isOwner;
	}

	/**
	 * @see org.olat.modules.webFeed.FeedSecurityCallback#mayDeleteItems()
	 */
	@Override
	public boolean mayDeleteItems() {
		return isAdmin || isOwner;
	}

	/**
	 * @see org.olat.modules.webFeed.FeedSecurityCallback#mayEditItems()
	 */
	@Override
	public boolean mayEditItems() {
		return isAdmin || isOwner;
	}

	/**
	 * @see org.olat.modules.webFeed.FeedSecurityCallback#mayEditMetadata()
	 */
	@Override
	public boolean mayEditMetadata() {
		return isAdmin || isOwner;
	}
}
