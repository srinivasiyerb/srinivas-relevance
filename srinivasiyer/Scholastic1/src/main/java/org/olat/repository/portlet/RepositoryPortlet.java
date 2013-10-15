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
 * Copyright (c) 2009 by frentix GmbH, www.frentix.com
 * <p>
 */

package org.olat.repository.portlet;

import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.portal.AbstractPortlet;
import org.olat.core.gui.control.generic.portal.Portlet;
import org.olat.core.gui.control.generic.portal.PortletToolController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;

/**
 * Description:<br>
 * Displays the list of courses from this user
 * <P>
 * Initial Date: 08.07.2005 <br>
 * 
 * @author gnaegi
 * @author Roman Haag, roman.haag@frentix.com, www.frentix.com
 */
public class RepositoryPortlet extends AbstractPortlet {
	private RepositoryPortletRunController runCtr;
	private static final String CONFIG_KEY_ROLE = "role";
	private static final String CONFIG_KEY_ROLE_STUDENT = "student";
	private static final String CONFIG_KEY_ROLE_TEACHER = "teacher";

	/**
	 * @see org.olat.gui.control.generic.portal.AbstractPortlet#createInstance(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest, java.util.Map)
	 */
	@Override
	public Portlet createInstance(final WindowControl wControl, final UserRequest ureq, final Map configuration) {
		final Translator translator = Util.createPackageTranslator(RepositoryPortlet.class, ureq.getLocale());
		final Portlet p = new RepositoryPortlet();
		p.setName(this.getName());
		p.setConfiguration(configuration);
		p.setTranslator(translator);
		return p;
	}

	/**
	 * @see org.olat.gui.control.generic.portal.Portlet#getTitle()
	 */
	@Override
	public String getTitle() {
		if (CONFIG_KEY_ROLE_STUDENT.equals(getConfiguration().get(CONFIG_KEY_ROLE))) {
			return getTranslator().translate("repositoryPortlet.student.title");
		} else {
			return getTranslator().translate("repositoryPortlet.teacher.title");
		}
	}

	/**
	 * @see org.olat.gui.control.generic.portal.Portlet#getDescription()
	 */
	@Override
	public String getDescription() {
		if (CONFIG_KEY_ROLE_STUDENT.equals(getConfiguration().get(CONFIG_KEY_ROLE))) {
			return getTranslator().translate("repositoryPortlet.student.description");
		} else {
			return getTranslator().translate("repositoryPortlet.teacher.description");
		}
	}

	/**
	 * @see org.olat.gui.control.generic.portal.Portlet#getInitialRunComponent(org.olat.core.gui.control.WindowControl, org.olat.core.gui.UserRequest)
	 */
	@Override
	public Component getInitialRunComponent(final WindowControl wControl, final UserRequest ureq) {
		if (this.runCtr != null) {
			runCtr.dispose();
		}
		final boolean studentView = CONFIG_KEY_ROLE_STUDENT.equals(getConfiguration().get(CONFIG_KEY_ROLE));
		this.runCtr = new RepositoryPortletRunController(wControl, ureq, getTranslator(), this.getName(), studentView);
		return this.runCtr.getInitialComponent();
	}

	/**
	 * @see org.olat.core.gui.control.Disposable#dispose(boolean)
	 */
	@Override
	public void dispose() {
		disposeRunComponent();
	}

	/**
	 * @see org.olat.gui.control.generic.portal.Portlet#getCssClass()
	 */
	@Override
	public String getCssClass() {
		if (CONFIG_KEY_ROLE_STUDENT.equals(getConfiguration().get(CONFIG_KEY_ROLE))) {
			return "o_portlet_repository_student";
		} else {
			return "o_portlet_repository_teacher";
		}
	}

	/**
	 * @see org.olat.gui.control.generic.portal.Portlet#disposeRunComponent(boolean)
	 */
	@Override
	public void disposeRunComponent() {
		if (this.runCtr != null) {
			this.runCtr.dispose();
			this.runCtr = null;
		}
	}

	@Override
	public PortletToolController getTools(final UserRequest ureq, final WindowControl wControl) {
		// portlet was not yet visible
		if (runCtr == null) {
			final boolean studentView = CONFIG_KEY_ROLE_STUDENT.equals(getConfiguration().get(CONFIG_KEY_ROLE));
			this.runCtr = new RepositoryPortletRunController(wControl, ureq, getTranslator(), this.getName(), studentView);
		}
		return runCtr.createSortingTool(ureq, wControl);
	}
}
