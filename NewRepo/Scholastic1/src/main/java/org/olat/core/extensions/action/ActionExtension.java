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

package org.olat.core.extensions.action;

import java.util.Locale;

import org.olat.core.extensions.ExtensionElement;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;

/**
 * Description:<br>
 * Initial Date: 02.08.2005 <br>
 * 
 * @author Felix
 */
public interface ActionExtension extends ExtensionElement {

	/**
	 * @param loc
	 * @return the description
	 */
	public String getDescription(Locale loc);

	/**
	 * @param loc
	 * @return the text of the html link
	 */
	public String getActionText(Locale loc);

	/**
	 * @param ureq
	 * @param wControl
	 */
	public Controller createController(UserRequest ureq, WindowControl wControl, Object arg);

}
