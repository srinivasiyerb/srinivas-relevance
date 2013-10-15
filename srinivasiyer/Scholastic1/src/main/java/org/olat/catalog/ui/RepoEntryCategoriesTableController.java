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
 * Copyright (c) 2008 frentix GmbH, Switzerland<br>
 * http://www.frentix.com,
 * <p>
 */
package org.olat.catalog.ui;

import java.util.List;

import org.olat.catalog.CatalogEntry;
import org.olat.catalog.CatalogManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.site.RepositorySite;

/**
 * Description:<br>
 * This controller displays a list of catalog categories where the given OLATResource is attached as a catalog leaf
 * <P>
 * Initial Date: 30.05.2008 <br>
 * 
 * @author gnaegi
 */
public class RepoEntryCategoriesTableController extends BasicController {
	private final TableController tableCtr;
	private final RepositoryEntry repoEntry;

	/**
	 * Constructor for a categories table controller for a given repository entry
	 * 
	 * @param ureq
	 * @param wControl
	 * @param repoEntry
	 * @param canManageRepoEntry true: user has administrative rights for the given repo entry
	 */
	public RepoEntryCategoriesTableController(final UserRequest ureq, final WindowControl wControl, final RepositoryEntry repoEntry, final boolean canManageRepoEntry) {
		super(ureq, wControl);
		this.repoEntry = repoEntry;

		// table configuration: use plain vanilla table - no header etc.
		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setColumnMovingOffered(false);
		tableConfig.setCustomCssClass("o_catalog_categories_table");
		tableConfig.setDisplayRowCount(false);
		tableConfig.setDisplayTableHeader(false);
		tableConfig.setDownloadOffered(false);
		tableConfig.setMultiSelect(false);
		tableConfig.setPageingEnabled(false);
		tableConfig.setTableEmptyMessage(translate("repo.nocategories"));

		// build categoris list table model
		final List<CatalogEntry> catalogEntries = CatalogManager.getInstance().getCatalogCategoriesFor(repoEntry);
		final CategoriesListModel categoriesListModel = new CategoriesListModel(catalogEntries, getLocale());
		// create table
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		listenTo(tableCtr);
		// add column descriptors to table
		categoriesListModel.addColumnDescriptors(tableCtr, canManageRepoEntry, getTranslator());
		// add table model
		tableCtr.setTableDataModel(categoriesListModel);

		putInitialPanel(tableCtr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		// no component events to dispatch
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == tableCtr) { // process table actions
			final TableEvent te = (TableEvent) event;
			final String action = te.getActionId();
			CategoriesListModel categoriesListModel = (CategoriesListModel) tableCtr.getTableDataModel();
			final CatalogEntry selectedCategoryLevel = categoriesListModel.getCatalogEntry(te.getRowId());

			if (action.equals(CategoriesListModel.ACTION_GOTO)) {
				// select repo site and activate catalog entry in catalog
				final DTabs dts = (DTabs) getWindowControl().getWindowBackOffice().getWindow().getAttribute("DTabs");
				dts.activateStatic(ureq, RepositorySite.class.getName(), "search.catalog:" + selectedCategoryLevel.getKey());

			} else if (action.equals(CategoriesListModel.ACTION_DELETE)) {
				// remove selected entry from the data model
				final CatalogManager cm = CatalogManager.getInstance();
				final List<CatalogEntry> children = cm.getChildrenOf(selectedCategoryLevel);
				// find all child element of this level that reference our repo entry
				for (final CatalogEntry child : children) {
					final RepositoryEntry childRepoEntry = child.getRepositoryEntry();
					if (childRepoEntry != null && childRepoEntry.equalsByPersistableKey(repoEntry)) {
						// remove from catalog
						cm.deleteCatalogEntry(child);
					}
				}
				// The catalog entry must have been deleted in the meantime by someone
				// else. In this case we just reload the table data model
				final List<CatalogEntry> catalogEntries = CatalogManager.getInstance().getCatalogCategoriesFor(repoEntry);
				categoriesListModel = new CategoriesListModel(catalogEntries, getLocale());
				tableCtr.setTableDataModel(categoriesListModel);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// table controler auto disposed by basic controller
	}

}
