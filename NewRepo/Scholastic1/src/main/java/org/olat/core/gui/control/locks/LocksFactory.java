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

package org.olat.core.gui.control.locks;

import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;

/**
 * Description:<br>
 * Initial Date: 23.08.2005 <br>
 * 
 * @author Felix
 */
public class LocksFactory {
	private final static LocksFactory INSTANCE = new LocksFactory();

	public static LocksFactory getInstance() {
		return INSTANCE;
	}

	// FIXME:fj: move to wcontrol
	private LocksFactory() {
		// singleton
	}

	public LockInfo createLock(OLATResourceable ores, Identity identity, String lockSubKey) {
		return null;
	}

}
