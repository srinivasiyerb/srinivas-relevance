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
package org.olat.core.gui;

import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.control.winmgr.AJAXFlags;

/**
 * Description:<br>
 * GlobalSettings for -one- user!
 * <P>
 * Initial Date: 24.01.2007 <br>
 * 
 * @author Felix Jost, http://www.goodsolutions.ch
 */
public interface GlobalSettings {

	/**
	 * @return fontsize relative to applied theme
	 */
	public abstract int getFontSize();

	/**
	 * @return Returns the ajaxFlags.
	 */
	public abstract AJAXFlags getAjaxFlags();

	/**
	 * Get the renderer for a specific component
	 * 
	 * @param source
	 * @return
	 */
	public ComponentRenderer getComponentRendererFor(Component source);

	/**
	 * only used by the renderer if in debug mode!
	 * 
	 * @return whether the renderer should force rendering of divs which are normally only used in web 2.0 mode. used by the development controller
	 */
	public boolean isIdDivsForced();

}