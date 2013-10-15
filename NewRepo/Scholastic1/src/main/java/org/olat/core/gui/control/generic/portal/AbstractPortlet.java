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

package org.olat.core.gui.control.generic.portal;

import java.util.HashMap;
import java.util.Map;

import org.olat.core.configuration.AbstractConfigOnOff;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;

/**
 * Description:<br>
 * Abstract class that must be implemented by all portlets.
 * <P>
 * Initial Date: 08.07.2005 <br>
 * 
 * @author gnaegi
 */
public abstract class AbstractPortlet extends AbstractConfigOnOff implements Portlet {
	private Map configuration = new HashMap();
	private String name;
	private Translator trans;

	/**
	 * @return The configuration map
	 */
	@Override
	public Map getConfiguration() {
		return this.configuration;
	}

	/**
	 * @param configuration The configuration map
	 */
	@Override
	public void setConfiguration(Map configuration) {
		this.configuration = configuration;
	}

	/**
	 * @see org.olat.core.gui.control.generic.portal.Portlet#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Bean method used by spring to load value from configuration
	 * 
	 * @param name The unique name of this portlet
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @see org.olat.core.gui.control.generic.portal.Portlet#setTranslator(org.olat.core.gui.translator.Translator)
	 */
	@Override
	public void setTranslator(Translator translator) {
		this.trans = translator;
	}

	/**
	 * @see org.olat.core.gui.control.generic.portal.Portlet#getTranslator()
	 */
	@Override
	public Translator getTranslator() {
		return this.trans;
	}

	/**
	 * This must be overriden if there are any tools to be exposed.
	 * 
	 * @see org.olat.core.gui.control.generic.portal.Portlet#getTools()
	 */
	@Override
	public PortletToolController getTools(UserRequest ureq, WindowControl wControl) {
		return null;
	}
}
