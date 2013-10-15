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
package org.olat.core.commons.fullWebApp.popup;

import org.olat.core.commons.fullWebApp.BaseFullWebappControllerParts;
import org.olat.core.gui.control.creator.ControllerCreator;

/**
 * Description:<br>
 * Brasato defines several different layouts for pop-up windows. Within application all pop-up windows which are not using the default - have to be layouted with the
 * BaseFullWebappPopupLayoutFactory.
 * <P>
 * Initial Date: 31.07.2007 <br>
 * 
 * @author patrickb
 * @since Release 6.0.0
 */
public interface BaseFullWebappPopupLayout extends ControllerCreator {

	/**
	 * the screen parts static tabs, footer, header, logo, topnav and also the content controller creator are defined inside the controller parts.
	 * 
	 * @see BaseFullWebappControllerParts
	 * @see org.olat.core.commons.fullWebApp.BaseFullWebappController
	 * @see org.olat.core.gui.control.creator.ControllerCreator
	 * @return
	 */
	BaseFullWebappControllerParts getFullWebappParts();

}
