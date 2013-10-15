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
 * Copyright (c) 2005-2006 by JGS goodsolutions GmbH, Switzerland<br>
 * http://www.goodsolutions.ch All rights reserved.
 * <p>
 */
package org.olat.core.id.context;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;

/**
 * Description:<br>
 * <P>
 * Initial Date: 23.06.2006 <br>
 * 
 * @author Felix Jost
 */
public interface ContextEntryControllerCreator {

	/**
	 * Factory method to create the run controller for this contex.
	 * <p>
	 * Just create the correct controller given the contextentry. Everthing else already done. (no need to advance pos in stack or such)
	 * <p>
	 * If context opens a site instead of creating a new dtab, the method can return NULL
	 * <p>
	 * The controller delivered by the factory method must take care of its entire business path.
	 * 
	 * @param ce
	 * @param ureq
	 * @param wControl
	 * @return the controller or NULL if the context is an existing site
	 */
	public Controller createController(ContextEntry ce, UserRequest ureq, WindowControl wControl);

	/**
	 * The name of the dynamic tab if such a tab should be created or NULL if opened as Site
	 * 
	 * @param ce
	 * @return Return tab name for certain context entry or NULL if the target is the opeing of an existing site
	 */
	public String getTabName(ContextEntry ce);

	/**
	 * The class name of the site that must be activated or NULL if opened as dTab
	 * 
	 * @param ce
	 * @return Return the class name that is used to activate an existing site or NULL if the target is a new dtab
	 */
	public String getSiteClassName(ContextEntry ce);

	/**
	 * @param ce
	 * @param ureq
	 * @param wControl
	 * @return true, if this contextentry can be launched
	 */
	public boolean validateContextEntryAndShowError(ContextEntry ce, UserRequest ureq, WindowControl wControl);
}
