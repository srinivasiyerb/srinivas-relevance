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

package org.olat.core.dispatcher.jumpin;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.OLATResourceable;

/**
 * Description:<br>
 * Initial Date: 23.02.2005 <br>
 * 
 * @author Felix Jost
 */
public interface JumpInReceptionist {
	/**
	 * @return
	 */
	public String getTitle();

	/**
	 * @return
	 */
	public OLATResourceable getOLATResourceable();

	/**
	 * @param ureq
	 * @param wControl
	 * @return never null!, but the contained controller may be null if the resource could not be started
	 */
	public JumpInResult createJumpInResult(UserRequest ureq, WindowControl wControl);

	/**
	 * @param ureq
	 * @return
	 */
	public String extractActiveViewId(UserRequest ureq);

}
