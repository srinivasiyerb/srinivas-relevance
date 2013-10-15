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

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.services.search.ui.SearchController;
import org.olat.core.commons.services.search.ui.SearchServiceUIFactory;
import org.olat.core.commons.services.search.ui.SearchServiceUIFactory.DisplayOption;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalWindowWrapperController;
import org.olat.core.gui.control.generic.dtabs.Activateable;
import org.olat.core.id.Identity;
import org.olat.core.util.StringHelper;
import org.olat.portfolio.EPTemplateMapResource;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.structel.EPDefaultMap;
import org.olat.portfolio.model.structel.EPStructuredMap;
import org.olat.portfolio.model.structel.PortfolioStructureMap;
import org.olat.portfolio.ui.structel.EPCreateMapController;
import org.olat.portfolio.ui.structel.EPMapCreatedEvent;
import org.olat.portfolio.ui.structel.EPMapEvent;
import org.olat.portfolio.ui.structel.EPMultipleMapController;
import org.olat.portfolio.ui.structel.EPStructureEvent;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.controllers.ReferencableEntriesSearchController;

/**
 * Description:<br>
 * Shows all Maps of a user.
 * <P>
 * Initial Date: 11.06.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPMapRunController extends BasicController implements Activateable {

	private VelocityContainer vC;
	private Link createMapLink;
	private Link createMapFromTemplateLink;
	private EPCreateMapController createMapCtrl;
	private CloseableModalWindowWrapperController createMapBox;
	private ReferencableEntriesSearchController searchTemplateCtrl;
	private EPMultipleMapController multiMapCtrl;
	private SearchController searchController;

	private final boolean create;
	private final Identity choosenOwner;
	private final EPMapRunViewOption option;
	private final EPFrontendManager ePFMgr;
	private Link createMapCalloutLink;
	private CloseableCalloutWindowController mapCreateCalloutCtrl;

	/**
	 * @param ureq
	 * @param wControl
	 * @param create Can user create new maps in this context
	 * @param option Select the view option from the maps
	 * @param choosenOwner Limit the list to maps from one specific owner
	 * @param types
	 */
	public EPMapRunController(final UserRequest ureq, final WindowControl wControl, final boolean create, final EPMapRunViewOption option, final Identity choosenOwner) {
		super(ureq, wControl);
		this.create = create;
		this.option = option;
		this.choosenOwner = choosenOwner;
		ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");

		Component viewComp = new Panel("empty");
		final PortfolioModule portfolioModule = (PortfolioModule) CoreSpringFactory.getBean("portfolioModule");
		if (portfolioModule.isEnabled()) {
			init(ureq);
			viewComp = vC;
		}

		putInitialPanel(viewComp);
	}

	private void init(final UserRequest ureq) {
		vC = createVelocityContainer("mymapsmain");
		if (create) {
			createMapLink = LinkFactory.createButton("create.map", vC, this);
		}

		String documentType = null;
		switch (option) {
			case MY_DEFAULTS_MAPS:
				documentType = "type.d*." + EPDefaultMap.class.getSimpleName();
				break;
			case MY_EXERCISES_MAPS:
				documentType = "type.*." + EPStructuredMap.class.getSimpleName();
				break;
		}

		if (documentType != null) {
			final SearchServiceUIFactory searchServiceUIFactory = (SearchServiceUIFactory) CoreSpringFactory.getBean(SearchServiceUIFactory.class);
			searchController = searchServiceUIFactory.createInputController(ureq, getWindowControl(), DisplayOption.STANDARD, null);
			listenTo(searchController);
			vC.put("search_input", searchController.getInitialComponent());

			searchController.setDocumentType(documentType);
			searchController.setResourceContextEnable(true);
			searchController.setResourceUrl(null);
		}

		initTitle();
		removeAsListenerAndDispose(multiMapCtrl);
		multiMapCtrl = new EPMultipleMapController(ureq, getWindowControl(), option, choosenOwner);
		listenTo(multiMapCtrl);
		vC.put("mapCtrl", multiMapCtrl.getInitialComponent());
	}

	private void initTitle() {
		String titleKey;
		String descriptionKey;
		switch (option) {
			case OTHER_MAPS:
				titleKey = "othermap.title";
				descriptionKey = "othermap.intro";
				break;
			case OTHERS_MAPS:
				titleKey = "othermaps.title";
				descriptionKey = "othermaps.intro";
				break;
			case MY_EXERCISES_MAPS:
				titleKey = "mystructuredmaps.title";
				descriptionKey = "mystructuredmaps.intro";
				break;
			default:// MY_DEFAULTS_MAPS:
				titleKey = "mymaps.title";
				descriptionKey = "mymaps.intro";
				break;
		}

		vC.contextPut("title", titleKey);
		vC.contextPut("description", descriptionKey);
	}

	@Override
	public void activate(final UserRequest ureq, final String viewIdentifier) {
		if (!StringHelper.containsNonWhitespace(viewIdentifier)) { return; }

		final int index = viewIdentifier.indexOf("[map:");
		if (index >= 0) {
			multiMapCtrl.activate(ureq, viewIdentifier);
		} else {
			try {
				Long.parseLong(viewIdentifier);
			} catch (final Exception e) {
				logError("could not convert viewIdentifier to mapID", e);
			} finally {
				multiMapCtrl.activate(ureq, viewIdentifier);
			}
		}

	}

	@SuppressWarnings("unused")
	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == createMapLink) {
			// if only normal maps can be created, show popup immediately, else present selection in callout
			if (option.equals(EPMapRunViewOption.MY_DEFAULTS_MAPS)) {
				final VelocityContainer mapCreateVC = createVelocityContainer("createMapCallout");
				createMapCalloutLink = LinkFactory.createLink("create.map.default", mapCreateVC, this);
				createMapFromTemplateLink = LinkFactory.createLink("create.map.fromTemplate", mapCreateVC, this);
				final String title = translate("create.map");

				removeAsListenerAndDispose(mapCreateCalloutCtrl);
				mapCreateCalloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(), mapCreateVC, createMapLink, title, true, null);
				listenTo(mapCreateCalloutCtrl);
				mapCreateCalloutCtrl.activate();
			} else {
				popUpCreateMapBox(ureq);
			}
		} else if (source == createMapFromTemplateLink) {
			closeCreateMapCallout();
			popUpCreateMapFromTemplateBox(ureq);
		} else if (source == createMapCalloutLink) {
			closeCreateMapCallout();
			popUpCreateMapBox(ureq);
		}
	}

	private void closeCreateMapCallout() {
		if (mapCreateCalloutCtrl != null) {
			mapCreateCalloutCtrl.deactivate();
			removeAsListenerAndDispose(mapCreateCalloutCtrl);
			mapCreateCalloutCtrl = null;
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		super.event(ureq, source, event);
		if (source == createMapBox) {
			popDownCreateMapBox();
		} else if (source == createMapCtrl) {
			if (event instanceof EPMapCreatedEvent) {
				final PortfolioStructureMap newMap = ((EPMapCreatedEvent) event).getPortfolioStructureMap();
				multiMapCtrl.activateMap(ureq, newMap);
			}
			createMapBox.deactivate();
			popDownCreateMapBox();
		} else if (source == searchTemplateCtrl) {
			if (event == ReferencableEntriesSearchController.EVENT_REPOSITORY_ENTRY_SELECTED) {
				final RepositoryEntry repoEntry = searchTemplateCtrl.getSelectedEntry();
				final PortfolioStructureMap newMap = createMapFromTemplate(repoEntry);
				multiMapCtrl.activateMap(ureq, newMap);
			}
			createMapBox.deactivate();
			popDownCreateMapBox();
		} else if (source == multiMapCtrl) {
			if (event instanceof EPMapEvent) {
				final String cmd = event.getCommand();
				if (EPStructureEvent.SELECT.equals(cmd)) {
					if (createMapLink != null) {
						createMapLink.setVisible(false);
					}
				} else if (EPStructureEvent.CLOSE.equals(cmd)) {
					if (createMapLink != null) {
						createMapLink.setVisible(true);
					}
				}
			}
		} else if (source == mapCreateCalloutCtrl && event == CloseableCalloutWindowController.CLOSE_WINDOW_EVENT) {
			removeAsListenerAndDispose(mapCreateCalloutCtrl);
			mapCreateCalloutCtrl = null;
		}
	}

	private void popDownCreateMapBox() {
		removeAsListenerAndDispose(createMapCtrl);
		createMapCtrl = null;
		createMapBox.dispose();
		createMapBox = null;
	}

	/**
	 * @param ureq
	 */
	private void popUpCreateMapBox(final UserRequest ureq) {
		final String title = translate("create.map");
		createMapCtrl = new EPCreateMapController(ureq, getWindowControl());
		listenTo(createMapCtrl);
		createMapBox = new CloseableModalWindowWrapperController(ureq, getWindowControl(), title, createMapCtrl.getInitialComponent(), "addMapBox");
		listenTo(createMapBox);
		createMapBox.setInitialWindowSize(750, 300);
		createMapBox.activate();
	}

	private void popUpCreateMapFromTemplateBox(final UserRequest ureq) {
		final String title = translate("create.map");
		final String commandLabel = translate("create.map.selectTemplate");
		removeAsListenerAndDispose(searchTemplateCtrl);
		searchTemplateCtrl = new ReferencableEntriesSearchController(getWindowControl(), ureq, new String[] { EPTemplateMapResource.TYPE_NAME }, commandLabel, false,
				false, false);
		listenTo(searchTemplateCtrl);
		createMapBox = new CloseableModalWindowWrapperController(ureq, getWindowControl(), title, searchTemplateCtrl.getInitialComponent(), "addMapFromTemplateBox");
		listenTo(createMapBox);
		createMapBox.setInitialWindowSize(800, 600);
		createMapBox.activate();
	}

	private PortfolioStructureMap createMapFromTemplate(final RepositoryEntry repoEntry) {
		final PortfolioStructureMap template = (PortfolioStructureMap) ePFMgr.loadPortfolioStructure(repoEntry.getOlatResource());
		final PortfolioStructureMap copy = ePFMgr.createAndPersistPortfolioDefaultMap(getIdentity(), template.getTitle(), template.getDescription());
		ePFMgr.copyStructureRecursively(template, copy, true);
		return copy;
	}

	@Override
	protected void doDispose() {
		if (createMapBox != null) {
			createMapBox.dispose();
			createMapBox = null;
		}
	}
}
