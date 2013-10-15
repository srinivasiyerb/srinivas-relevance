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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.core.gui.control.generic.breadcrumb;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;

/**
 * <h3>Description:</h3> This abstract class implements the CrumbController interface for the FormBasicController. Since Java does not support multiple inheritance, this
 * is the only way to do it.
 * <p>
 * Initial Date: 10.09.2008 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */

public abstract class CrumbFormBasicController extends FormBasicController implements CrumbController {
	private BreadCrumbController breadCrumbCtr;
	private CrumbController childCrumbCtr;

	/************************************
	 * Constructors from form basic controller
	 ************************************/
	protected CrumbFormBasicController(UserRequest ureq, WindowControl control) {
		super(ureq, control);
	}

	protected CrumbFormBasicController(UserRequest ureq, WindowControl control, String pageName) {
		super(ureq, control, pageName);
	}

	public CrumbFormBasicController(UserRequest ureq, WindowControl control, String pageName, Translator fallbackTranslator) {
		super(ureq, control, pageName, fallbackTranslator);
	}

	protected CrumbFormBasicController(UserRequest ureq, WindowControl control, int layout) {
		super(ureq, control, layout);
	}

	protected CrumbFormBasicController(UserRequest ureq, WindowControl wControl, int layout, String customLayoutPageName, Form externalMainForm) {
		super(ureq, wControl, layout, customLayoutPageName, externalMainForm);
	}

	/********************************
	 * Methods from crumb controller
	 ********************************/

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.generic.breadcrumb.CrumbController#getChildCrumbController()
	 */
	@Override
	public CrumbController getChildCrumbController() {
		return childCrumbCtr;
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.generic.breadcrumb.CrumbController#setBreadCrumbController(org.olat.core.gui.control.generic.breadcrumb.BreadCrumbController)
	 */
	@Override
	public void setBreadCrumbController(BreadCrumbController breadCrumbCtr) {
		this.breadCrumbCtr = breadCrumbCtr;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.olat.core.gui.control.generic.breadcrumb.CrumbController#activateAndListenToChildCrumbController(org.olat.core.gui.control.generic.breadcrumb.CrumbController)
	 */
	@Override
	public void activateAndListenToChildCrumbController(CrumbController childCrumbController) {
		// remove old one
		deactivateAndDisposeChildCrumbController();
		childCrumbCtr = childCrumbController;
		listenTo(childCrumbCtr); // auto cleanup
		childCrumbCtr.setBreadCrumbController(this.breadCrumbCtr);
		breadCrumbCtr.putToBreadCrumbStack(childCrumbCtr);
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.generic.breadcrumb.CrumbController#deactivateAndDisposeChildCrumbController()
	 */
	@Override
	public void deactivateAndDisposeChildCrumbController() {
		if (childCrumbCtr != null) {
			childCrumbCtr.deactivateAndDisposeChildCrumbController();
			removeAsListenerAndDispose(childCrumbCtr);
			childCrumbCtr = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.generic.breadcrumb.CrumbController#removeFromBreadCrumbPathAndDispose()
	 */
	@Override
	public void removeFromBreadCrumbPathAndDispose() {
		this.breadCrumbCtr.removeFromBreadCrumb(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.core.gui.control.generic.breadcrumb.CrumbController#resetCrumbTexts()
	 */
	@Override
	public void resetCrumbTexts() {
		this.breadCrumbCtr.resetCrumbTexts();
	}

}
