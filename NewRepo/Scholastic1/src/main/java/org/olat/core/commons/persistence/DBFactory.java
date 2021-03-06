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

package org.olat.core.commons.persistence;

public class DBFactory {

	/**
	 * !!IMPORTANT!! Get the DB instance. The DB Session is lazy initialized. Make sure you don't save the instance in a class variable as you do not have a guarantee
	 * that a Hibernate session is already initialized.
	 * 
	 * @return the DB instance.
	 */
	public static DB getInstance() {
		return DBImpl.getInstance(true);
	}

	public static DB getInstance(boolean initialize) {
		return DBImpl.getInstance(initialize);
	}

	public static DB getInstanceForClosing() {
		return DBImpl.getInstanceForClosing();
	}

}
