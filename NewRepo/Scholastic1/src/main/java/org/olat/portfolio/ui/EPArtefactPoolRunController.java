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
package org.olat.portfolio.ui;

import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.segmentedview.SegmentViewComponent;
import org.olat.core.gui.components.segmentedview.SegmentViewEvent;
import org.olat.core.gui.components.segmentedview.SegmentViewFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.portfolio.EPSecurityCallback;
import org.olat.portfolio.EPSecurityCallbackImpl;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.EPFilterSettings;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.ui.artefacts.collect.EPAddArtefactController;
import org.olat.portfolio.ui.artefacts.view.EPArtefactChoosenEvent;
import org.olat.portfolio.ui.artefacts.view.EPArtefactDeletedEvent;
import org.olat.portfolio.ui.artefacts.view.EPMultiArtefactsController;
import org.olat.portfolio.ui.artefacts.view.EPMultipleArtefactPreviewController;
import org.olat.portfolio.ui.artefacts.view.EPMultipleArtefactsAsTableController;
import org.olat.portfolio.ui.artefacts.view.EPTagBrowseController;
import org.olat.portfolio.ui.artefacts.view.EPTagBrowseEvent;
import org.olat.portfolio.ui.filter.EPFilterSelectController;
import org.olat.portfolio.ui.filter.PortfolioFilterChangeEvent;
import org.olat.portfolio.ui.filter.PortfolioFilterController;
import org.olat.portfolio.ui.filter.PortfolioFilterEditEvent;

/**
 * Presents an overview of all artefacts of an user. Initial Date: 11.06.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPArtefactPoolRunController extends BasicController {

	private VelocityContainer vC;
	private final EPFrontendManager ePFMgr;
	private EPFilterSettings filterSettings = new EPFilterSettings();
	private Controller addArtefactCtrl;
	private final boolean artefactChooseMode;
	private SegmentViewComponent segmentView;
	private Link artefactsLink;
	private Link browseLink;
	private Link searchLink;
	private Controller filterSelectCtrl;
	private Filter previousFilterMode;
	private EPViewModeController viewModeCtrl;
	private EPMultiArtefactsController artCtrl;
	private String previousViewMode;
	private List<AbstractArtefact> previousArtefactsList;

	public EPArtefactPoolRunController(final UserRequest ureq, final WindowControl wControl) {
		this(ureq, wControl, false);
	}

	public EPArtefactPoolRunController(final UserRequest ureq, final WindowControl wControl, final boolean artefactChooseMode) {
		super(ureq, wControl);
		this.artefactChooseMode = artefactChooseMode;
		Component viewComp = new Panel("empty");
		final Component filterPanel = new Panel("filter");
		final PortfolioModule portfolioModule = (PortfolioModule) CoreSpringFactory.getBean("portfolioModule");
		ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		if (portfolioModule.isEnabled()) {
			init(ureq);
			viewComp = vC;
			vC.put("filterPanel", filterPanel);

			if (filterSettings.isFilterEmpty()) {
				initTPAllView(ureq);
			} else {
				initTPFilterView(ureq);
			}
		}

		initViewModeController(ureq);

		putInitialPanel(viewComp);
	}

	/**
	 * create the velocity for the artefact-pool with a tabbed pane / segmented view this doesn't initialize anything, the panels first are empty!
	 * 
	 * @param ureq
	 */
	private void init(final UserRequest ureq) {
		vC = createVelocityContainer("artefactsmain");
		vC.contextPut("artefactChooseMode", artefactChooseMode);

		segmentView = SegmentViewFactory.createSegmentView("segments", vC, this);
		artefactsLink = LinkFactory.createLink("viewTab.all", vC, this);
		segmentView.addSegment(artefactsLink, true);

		browseLink = LinkFactory.createLink("viewTab.browse", vC, this);
		segmentView.addSegment(browseLink, false);

		searchLink = LinkFactory.createLink("viewTab.search", vC, this);
		segmentView.addSegment(searchLink, false);

		addArtefactCtrl = new EPAddArtefactController(ureq, getWindowControl());
		listenTo(addArtefactCtrl);
		vC.put("addArtefactCtrl", addArtefactCtrl.getInitialComponent());
	}

	/**
	 * switch between filter selection (drop down only) and the full filter-view and put this to the filter-panel
	 * 
	 * @param ureq
	 * @param readOnlyMode
	 */
	private void initFilterPanel(final UserRequest ureq, final Filter filterMode) {
		if (filterSelectCtrl == null || previousFilterMode != filterMode) {
			removeAsListenerAndDispose(filterSelectCtrl);
			switch (filterMode) {
				case read_only:
					filterSelectCtrl = new EPFilterSelectController(ureq, getWindowControl(), filterSettings.getFilterId());
					break;
				case tags:
					filterSelectCtrl = new EPTagBrowseController(ureq, getWindowControl());
					break;
				case extended:
					filterSelectCtrl = new PortfolioFilterController(ureq, getWindowControl(), filterSettings);
					break;
			}

			previousFilterMode = filterMode;
			listenTo(filterSelectCtrl);
			vC.put("filterPanel", filterSelectCtrl.getInitialComponent());
		}
	}

	private void setSegmentContent(final Controller ctrl) {
		vC.put("segmentContent", ctrl.getInitialComponent());
	}

	private void initTPAllView(final UserRequest ureq) {
		filterSettings = new EPFilterSettings();
		final List<AbstractArtefact> artefacts = ePFMgr.getArtefactPoolForUser(getIdentity());
		initMultiArtefactCtrl(ureq, artefacts);
		initFilterPanel(ureq, Filter.read_only);
		setSegmentContent(artCtrl);
	}

	private void initMultiArtefactCtrl(final UserRequest ureq, final List<AbstractArtefact> artefacts) {
		// decide how to present artefacts depending on users settings
		final String userPrefsMode = ePFMgr.getUsersPreferedArtefactViewMode(getIdentity(), EPViewModeController.VIEWMODE_CONTEXT_ARTEFACTPOOL);
		if (previousViewMode != null && !previousViewMode.equals(userPrefsMode)) {
			removeAsListenerAndDispose(artCtrl);
		}
		if (userPrefsMode != null && userPrefsMode.equals(EPViewModeController.VIEWMODE_TABLE)) {
			final EPSecurityCallback secCallback = new EPSecurityCallbackImpl(true, true);
			artCtrl = new EPMultipleArtefactsAsTableController(ureq, getWindowControl(), artefacts, null, artefactChooseMode, secCallback);
		} else {
			artCtrl = new EPMultipleArtefactPreviewController(ureq, getWindowControl(), artefacts, artefactChooseMode);
		}
		previousViewMode = userPrefsMode;
		listenTo(artCtrl);
		previousArtefactsList = artefacts;
	}

	private void initTPFilterView(final UserRequest ureq) {
		final List<AbstractArtefact> filteredArtefacts = ePFMgr.filterArtefactsByFilterSettings(filterSettings, getIdentity(), ureq.getUserSession().getRoles());
		initMultiArtefactCtrl(ureq, filteredArtefacts);
		initFilterPanel(ureq, Filter.extended);
		setSegmentContent(artCtrl);
	}

	private void initTPBrowseView(final UserRequest ureq) {
		final List<AbstractArtefact> artefacts = ePFMgr.getArtefactPoolForUser(getIdentity());
		initMultiArtefactCtrl(ureq, artefacts);
		initFilterPanel(ureq, Filter.tags);
		setSegmentContent(artCtrl);
	}

	private void initViewModeController(final UserRequest ureq) {
		viewModeCtrl = new EPViewModeController(ureq, getWindowControl(), EPViewModeController.VIEWMODE_CONTEXT_ARTEFACTPOOL);
		listenTo(viewModeCtrl);
		vC.put("viewMode", viewModeCtrl.getInitialComponent());
	}

	@Override
	protected void doDispose() {
		// ctrls disposed by basicCtrl due to listenTo()'s
	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == segmentView) {
			if (event instanceof SegmentViewEvent) {
				final SegmentViewEvent sve = (SegmentViewEvent) event;
				final String segmentCName = sve.getComponentName();
				final Component clickedLink = vC.getComponent(segmentCName);
				if (clickedLink == artefactsLink) {
					initTPAllView(ureq);
				} else if (clickedLink == browseLink) {
					initTPBrowseView(ureq);
				} else if (clickedLink == searchLink) {
					initTPFilterView(ureq);
				}
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		super.event(ureq, source, event);
		if (source == addArtefactCtrl) {
			// some artefacts were added, refresh view
			if (event.equals(Event.DONE_EVENT)) {
				initTPAllView(ureq);
			}
		} else if (event instanceof EPArtefactChoosenEvent) {
			// an artefact was choosen, pass through the event until top
			fireEvent(ureq, event);
		} else if (source == filterSelectCtrl) {
			if (event instanceof PortfolioFilterChangeEvent) {
				final PortfolioFilterChangeEvent pFEvent = (PortfolioFilterChangeEvent) event;
				filterSettings = pFEvent.getFilterList();
			} else if (event instanceof PortfolioFilterEditEvent) {
				final PortfolioFilterEditEvent editEvent = (PortfolioFilterEditEvent) event;
				filterSettings = editEvent.getFilterList();
			}
			if (source instanceof EPFilterSelectController) {
				if (event == Event.CHANGED_EVENT) {
					initTPFilterView(ureq);
				} else if (event instanceof PortfolioFilterChangeEvent) {
					// preset search was selected, apply it, but stay within first segment
					initTPFilterView(ureq);
					initFilterPanel(ureq, Filter.read_only);
				} else if (event instanceof PortfolioFilterEditEvent) {
					initTPFilterView(ureq);
					initFilterPanel(ureq, Filter.extended);
					segmentView.select(searchLink);
				}
			} else if (source instanceof EPTagBrowseController) {
				if (event instanceof EPTagBrowseEvent) {
					final EPTagBrowseEvent found = (EPTagBrowseEvent) event;
					initMultiArtefactCtrl(ureq, found.getArtefacts());
					setSegmentContent(artCtrl);
				}
			} else if (source instanceof PortfolioFilterController) {
				if (event instanceof PortfolioFilterChangeEvent) {
					initTPFilterView(ureq);
				}
			}
		} else if (source == viewModeCtrl && event.getCommand().equals(EPViewModeController.VIEWMODE_CHANGED_EVENT_CMD)) {
			initMultiArtefactCtrl(ureq, previousArtefactsList);
			setSegmentContent(artCtrl);
		} else if (event instanceof EPArtefactDeletedEvent) {
			final EPArtefactDeletedEvent epDelEv = (EPArtefactDeletedEvent) event;
			previousArtefactsList.remove(epDelEv.getArtefact());
			initMultiArtefactCtrl(ureq, previousArtefactsList);
			setSegmentContent(artCtrl);
		}
	}

	private enum Filter {
		read_only, tags, extended
	}
}
