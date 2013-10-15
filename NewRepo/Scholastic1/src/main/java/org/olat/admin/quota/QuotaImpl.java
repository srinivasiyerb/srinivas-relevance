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

package org.olat.admin.quota;

import org.olat.core.util.vfs.Quota;

/**
 * Initial Date: Mar 30, 2004
 * 
 * @author Mike Stock
 */
public class QuotaImpl implements Quota {

	private final String path;
	private final Long quotaKB;
	private final Long ulLimitKB;

	QuotaImpl(final String path, final Long quotaKB, final Long ulLimitKB) {
		this.path = path != null ? path : "";
		this.quotaKB = quotaKB;
		this.ulLimitKB = ulLimitKB;
	}

	/**
	 * @return The path
	 */
	@Override
	public String getPath() {
		return path;
	}

	/**
	 * @return Quota in KB
	 */
	@Override
	public Long getQuotaKB() {
		return quotaKB;
	}

	/**
	 * @return Upload Limit in KB.
	 */
	@Override
	public Long getUlLimitKB() {
		return ulLimitKB;
	}

}
