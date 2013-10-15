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
package org.olat.repository;

/**
 * Description:<br>
 * This class is used to handle and interpret the status of repository entries.
 * <P>
 * Initial Date: 09.07.2009 <br>
 * 
 * @author bja <bja@bps-system.de>
 */
public class RepositoryEntryStatus {

	public static final int REPOSITORY_STATUS_OPEN = 1;
	public static final int REPOSITORY_STATUS_CLOSED = 2;

	private boolean closed; // 0010

	public RepositoryEntryStatus(final int statusCode) {
		// initialize closed status
		if ((statusCode & REPOSITORY_STATUS_CLOSED) == REPOSITORY_STATUS_CLOSED) {
			setClosed(true);
		} else {
			setClosed(false);
		}
	}

	/**
	 * @param closed
	 */
	private void setClosed(final boolean _closed) {
		this.closed = _closed;
	}

	public boolean isClosed() {
		return this.closed;
	}

}
