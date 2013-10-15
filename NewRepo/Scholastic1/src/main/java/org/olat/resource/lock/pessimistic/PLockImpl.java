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

package org.olat.resource.lock.pessimistic;

import org.olat.core.commons.persistence.PersistentObject;

/**
 * A <b>OLATResourceImpl</b> is
 * 
 * @author Andreas
 */
public class PLockImpl extends PersistentObject implements PLock {

	private String asset;

	/**
	 * Constructor needed for Hibernate.
	 */
	PLockImpl() {
		// singleton
	}

	PLockImpl(final String asset) {
		this.asset = asset;
	}

	public String getAsset() {
		return asset;
	}

	/**
	 * [for hibernate]
	 * 
	 * @param asset
	 */
	void setAsset(final String asset) {
		this.asset = asset;
	}

}
