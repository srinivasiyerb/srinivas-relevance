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

package org.olat.course.nodes.portfolio;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsBackController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.Formatter;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseModule;
import org.olat.course.nodes.PortfolioCourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.portfolio.EPSecurityCallback;
import org.olat.portfolio.EPSecurityCallbackImpl;
import org.olat.portfolio.EPUIFactory;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.structel.EPStructuredMap;
import org.olat.portfolio.model.structel.PortfolioStructureMap;
import org.olat.repository.RepositoryEntry;

/**
 * Description:<br>
 * Assessment details controller.
 * <P>
 * Initial Date: 11 nov. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class PortfolioResultDetailsController extends FormBasicController {
	private final EPFrontendManager ePFMgr;
	private final ModuleConfiguration config;
	private final Identity assessedIdentity;
	private PortfolioStructureMap map;

	private FormLink openMapLink;
	private FormLink changeDeadlineLink;
	private StaticTextElement deadlineEl;
	private DeadlineController deadlineCtr;
	private CloseableCalloutWindowController deadlineCalloutCtr;

	public PortfolioResultDetailsController(final UserRequest ureq, final WindowControl wControl, final PortfolioCourseNode courseNode,
			final UserCourseEnvironment userCourseEnv) {
		super(ureq, wControl);

		this.config = courseNode.getModuleConfiguration();

		ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		assessedIdentity = userCourseEnv.getIdentityEnvironment().getIdentity();

		final RepositoryEntry mapEntry = courseNode.getReferencedRepositoryEntry();
		if (mapEntry != null) {
			final PortfolioStructureMap template = (PortfolioStructureMap) ePFMgr.loadPortfolioStructure(mapEntry.getOlatResource());
			final Long courseResId = userCourseEnv.getCourseEnvironment().getCourseResourceableId();
			final OLATResourceable courseOres = OresHelper.createOLATResourceableInstance(CourseModule.class, courseResId);
			map = ePFMgr.loadPortfolioStructureMap(assessedIdentity, template, courseOres, courseNode.getIdent(), null);
		}

		initForm(ureq);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		if (map == null) {
			uifactory.addStaticTextElement("no.map", "", formLayout);
		} else {
			final Formatter formatter = Formatter.getInstance(getLocale());
			if (map instanceof EPStructuredMap) {
				final EPStructuredMap structuredMap = (EPStructuredMap) map;

				String copyDate = "";
				if (structuredMap.getCopyDate() != null) {
					copyDate = formatter.formatDateAndTime(structuredMap.getCopyDate());
				}
				uifactory.addStaticTextElement("map.copyDate", copyDate, formLayout);

				String returnDate = "";
				if (structuredMap.getReturnDate() != null) {
					returnDate = formatter.formatDateAndTime(structuredMap.getReturnDate());
				}
				uifactory.addStaticTextElement("map.returnDate", returnDate, formLayout);

				String deadLine = "";
				if (structuredMap.getDeadLine() != null) {
					deadLine = formatter.formatDateAndTime(structuredMap.getDeadLine());
				}
				deadlineEl = uifactory.addStaticTextElement("map.deadline", deadLine, formLayout);
				changeDeadlineLink = uifactory.addFormLink("map.deadline.change", formLayout, Link.BUTTON);
			}

			openMapLink = uifactory.addFormLink("open.map", formLayout);
		}
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		//
	}

	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (source == openMapLink) {
			final EPSecurityCallback secCallback = new EPSecurityCallbackImpl(false, true);
			final Controller viewCtr = EPUIFactory.createPortfolioStructureMapController(ureq, getWindowControl(), map, secCallback);
			listenTo(viewCtr);
			final LayoutMain3ColsBackController ctr = new LayoutMain3ColsBackController(ureq, getWindowControl(), null, null, viewCtr.getInitialComponent(), "portfolio"
					+ map.getKey());
			ctr.activate();

		} else if (source == changeDeadlineLink) {
			if (deadlineCalloutCtr == null) {
				popupDeadlineBox(ureq);
			} else {
				// close on second click
				closeDeadlineBox();
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == deadlineCalloutCtr && event == CloseableCalloutWindowController.CLOSE_WINDOW_EVENT) {
			removeAsListenerAndDispose(deadlineCalloutCtr);
			deadlineCalloutCtr = null;
		} else if (source == deadlineCtr) {
			String deadLine = "";
			final EPStructuredMap structuredMap = (EPStructuredMap) map;
			if (structuredMap.getDeadLine() != null) {
				final Formatter formatter = Formatter.getInstance(getLocale());
				deadLine = formatter.formatDateAndTime(structuredMap.getDeadLine());
			}
			deadlineEl.setValue(deadLine);
			closeDeadlineBox();
		}
	}

	/**
	 * @param ureq
	 */
	private void popupDeadlineBox(final UserRequest ureq) {
		final String title = translate("map.deadline.change");
		if (deadlineCtr == null) {
			deadlineCtr = new DeadlineController(ureq, getWindowControl(), (EPStructuredMap) map);
			listenTo(deadlineCtr);
		}
		removeAsListenerAndDispose(deadlineCalloutCtr);
		deadlineCalloutCtr = new CloseableCalloutWindowController(ureq, getWindowControl(), deadlineCtr.getInitialComponent(), changeDeadlineLink, title, true,
				"b_eportfolio_deadline_callout");
		listenTo(deadlineCalloutCtr);
		deadlineCalloutCtr.activate();
	}

	private void closeDeadlineBox() {
		if (deadlineCalloutCtr != null) {
			deadlineCalloutCtr.deactivate();
			removeAsListenerAndDispose(deadlineCalloutCtr);
			deadlineCalloutCtr = null;
		}
	}
}