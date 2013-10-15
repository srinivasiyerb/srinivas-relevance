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
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.repository.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.olat.core.dispatcher.DispatcherAction;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.UserConstants;
import org.olat.core.util.Util;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryTableModel;
import org.olat.repository.SearchForm;

/**
 * Description: This workflow is used to search for repository entries. The workflow has two steps: 1) a search form and 2) the results list. After calling the
 * constructor nothing happens. The views must be initialized manually. Using some public methods the desired view can be triggered. The workflow can be limited to a
 * specific repository type. Onother option is to set the set the enableSearchforAllReferencalbeInSearchForm In this case, the search workflow can be used to find
 * repository entires that can be referenced by the user. Some doSearch... methods allow the presentation of lists without using the search form at all.
 * 
 * @author Felix Jost
 */
public class RepositorySearchController extends BasicController {

	private static final String PACKAGE = Util.getPackageName(RepositoryManager.class);
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(RepositoryManager.class);

	protected VelocityContainer vc;
	protected Translator translator;
	protected RepositoryTableModel repoTableModel;
	protected SearchForm searchForm;
	protected TableController tableCtr;

	private Link backLink, cancelButton;
	private RepositoryEntry selectedEntry = null;
	private boolean enableSearchforAllReferencalbeInSearchForm = false;
	private Link loginLink;

	/**
	 * A generic search controller.
	 * 
	 * @param selectButtonLabel
	 * @param ureq
	 * @param myWControl
	 * @param withCancel
	 * @param enableDirectLaunch
	 */
	public RepositorySearchController(final String selectButtonLabel, final UserRequest ureq, final WindowControl myWControl, final boolean withCancel,
			final boolean enableDirectLaunch) {
		super(ureq, myWControl);
		init(selectButtonLabel, ureq, withCancel, enableDirectLaunch, new String[] {});
	}

	/**
	 * A generic search controller.
	 * 
	 * @param selectButtonLabel
	 * @param ureq
	 * @param myWControl
	 * @param withCancel
	 * @param enableDirectLaunch
	 * @param limitType
	 */
	public RepositorySearchController(final String selectButtonLabel, final UserRequest ureq, final WindowControl myWControl, final boolean withCancel,
			final boolean enableDirectLaunch, final String limitType) {
		this(selectButtonLabel, ureq, myWControl, withCancel, enableDirectLaunch, new String[] { limitType });
	}

	public RepositorySearchController(final String selectButtonLabel, final UserRequest ureq, final WindowControl myWControl, final boolean withCancel,
			final boolean enableDirectLaunch, final String[] limitTypes) {
		super(ureq, myWControl);
		init(selectButtonLabel, ureq, withCancel, enableDirectLaunch, limitTypes);
	}

	/**
	 * @param myWControl
	 */
	public RepositorySearchController(final UserRequest ureq, final WindowControl myWControl) {
		super(ureq, myWControl);
	}

	private void init(final String selectButtonLabel, final UserRequest ureq, final boolean withCancel, final boolean enableDirectLaunch, final String[] limitTypes) {
		translator = new PackageTranslator(PACKAGE, ureq.getLocale());
		final Roles roles = ureq.getUserSession().getRoles();

		vc = new VelocityContainer("reposearch", VELOCITY_ROOT + "/search.html", translator, this);

		removeAsListenerAndDispose(searchForm);
		searchForm = new SearchForm(ureq, getWindowControl(), withCancel, roles.isOLATAdmin(), limitTypes);
		listenTo(searchForm);

		searchForm.setVisible(false);
		vc.put("searchform", searchForm.getInitialComponent());

		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		if (selectButtonLabel != null) {
			tableConfig.setPreferencesOffered(true, "repositorySearchResult");
		}

		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), translator, true);
		listenTo(tableCtr);

		repoTableModel = new RepositoryTableModel(translator);
		repoTableModel.addColumnDescriptors(tableCtr, selectButtonLabel, enableDirectLaunch);
		tableCtr.setTableDataModel(repoTableModel);
		tableCtr.setSortColumn(1, true);
		vc.put("repotable", tableCtr.getInitialComponent());

		vc.contextPut("isAuthor", Boolean.valueOf(roles.isAuthor()));
		vc.contextPut("withCancel", new Boolean(withCancel));
		enableBackToSearchFormLink(false); // default, must be enabled explicitly
		enableSearchforAllReferencalbeInSearchForm(false); // default
		putInitialPanel(vc);
	}

	/**
	 * @param enableBack true: back link is shown, back goes to search form; false; no back link
	 */
	public void enableBackToSearchFormLink(final boolean enableBack) {
		vc.contextPut("withBack", new Boolean(enableBack));
	}

	/**
	 * @param enable true: searches done by the search form will find all resources that are referencable by the current user; false: searches done by the search form
	 *            will find all resources that have at least BAR setting in the BARG configuration list
	 */
	public void enableSearchforAllReferencalbeInSearchForm(final boolean enable) {
		enableSearchforAllReferencalbeInSearchForm = enable;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == cancelButton) {
			fireEvent(ureq, Event.CANCELLED_EVENT);
			return;
		} else if (source == backLink) {
			displaySearchForm();
			return;
		} else if (source == loginLink) {
			DispatcherAction.redirectToDefaultDispatcher(ureq.getHttpResp());
		}
	}

	/**
	 * Implementation normal search: find repo entries that are public using the values from the form
	 * 
	 * @param ureq
	 */
	private void doSearch(final UserRequest ureq) {
		final RepositoryManager rm = RepositoryManager.getInstance();
		final Set s = searchForm.getRestrictedTypes();
		final List restrictedTypes = (s == null) ? null : new ArrayList(s);
		final List entries = rm.genericANDQueryWithRolesRestriction(searchForm.getDisplayName(), searchForm.getAuthor(), searchForm.getDescription(), restrictedTypes,
				ureq.getUserSession().getRoles(), ureq.getIdentity().getUser().getProperty(UserConstants.INSTITUTIONALNAME, null));
		repoTableModel.setObjects(entries);
		tableCtr.modelChanged();
		displaySearchResults(ureq);
	}

	/**
	 * Implementation of referencable search: find repo entries that are owned by the uer or set to referencable and have at lease BA settings
	 * 
	 * @param ureq
	 */
	private void doSearchAllReferencables(final UserRequest ureq) {
		final RepositoryManager rm = RepositoryManager.getInstance();
		final Set s = searchForm.getRestrictedTypes();
		final List restrictedTypes = (s == null) ? null : new ArrayList(s);
		final Roles roles = ureq.getUserSession().getRoles();
		final Identity ident = ureq.getIdentity();
		final String name = searchForm.getDisplayName();
		final String author = searchForm.getAuthor();
		final String desc = searchForm.getDescription();

		final List entries = rm.queryReferencableResourcesLimitType(ident, roles, restrictedTypes, name, author, desc);
		repoTableModel.setObjects(entries);
		tableCtr.modelChanged();
		displaySearchResults(ureq);
	}

	/**
	 * Do search for all resources that the user can reference either because he is the owner of the resource or because he has author rights and the resource is set to
	 * at least BA in the BARG settings and the resource has the flat 'canReference' set to true.
	 * 
	 * @param owner The current identity
	 * @param limitType The search limitation a specific type
	 * @param roles The users roles
	 */
	public void doSearchForReferencableResourcesLimitType(final Identity owner, final String limitType, final Roles roles) {
		doSearchForReferencableResourcesLimitType(owner, limitType.equals("") ? null : new String[] { limitType }, roles);
	}

	public void doSearchForReferencableResourcesLimitType(final Identity owner, final String[] limitTypes, final Roles roles) {
		final RepositoryManager rm = RepositoryManager.getInstance();
		List<String> restrictedTypes = new ArrayList<String>();
		if (limitTypes == null) {
			restrictedTypes = null;
		} else {
			restrictedTypes.addAll(Arrays.asList(limitTypes));
		}
		final List entries = rm.queryReferencableResourcesLimitType(owner, roles, restrictedTypes, null, null, null);

		repoTableModel.setObjects(entries);
		tableCtr.modelChanged();
		displaySearchResults(null);
	}

	/**
	 * Do search for all resources of a given type where identity is owner.
	 * 
	 * @param owner
	 * @param limitType
	 */
	public void doSearchByOwnerLimitType(final Identity owner, final String limitType) {
		doSearchByOwnerLimitType(owner, new String[] { limitType });
	}

	public void doSearchByOwnerLimitType(final Identity owner, final String[] limitTypes) {
		final RepositoryManager rm = RepositoryManager.getInstance();
		final List entries = rm.queryByOwner(owner, limitTypes);
		repoTableModel.setObjects(entries);
		tableCtr.modelChanged();
		displaySearchResults(null);
	}

	/**
	 * Do search for all resources of a given type where identity is owner.
	 * 
	 * @param owner
	 * @param access
	 */
	public void doSearchByOwnerLimitAccess(final Identity owner, final int access) {
		final RepositoryManager rm = RepositoryManager.getInstance();
		final List entries = rm.queryByOwnerLimitAccess(owner, access);

		repoTableModel.setObjects(entries);
		tableCtr.modelChanged();
		displaySearchResults(null);
	}

	/**
	 * Search for all resources where identity is owner.
	 * 
	 * @param owner
	 */
	public void doSearchByOwner(final Identity owner) {
		doSearchByOwnerLimitType(owner, new String[] {});
	}

	/**
	 * Package private. Used by repository main controller to execute predefined searches.
	 * 
	 * @param type
	 * @param ureq
	 */
	void doSearchByTypeLimitAccess(final String type, final UserRequest ureq) {
		final RepositoryManager rm = RepositoryManager.getInstance();
		final List entries = rm.queryByTypeLimitAccess(type, ureq);
		repoTableModel.setObjects(entries);
		tableCtr.modelChanged();
		displaySearchResults(ureq);
	}

	private void doSearchById(final Long id) {
		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry entry = rm.lookupRepositoryEntry(id);
		final List<RepositoryEntry> entries = new ArrayList<RepositoryEntry>(1);
		if (entry != null) {
			entries.add(entry);
		}
		repoTableModel.setObjects(entries);
		tableCtr.modelChanged();
		displaySearchResults(null);
	}

	protected void doSearchMyCoursesStudent(final UserRequest ureq) {
		final RepositoryManager rm = RepositoryManager.getInstance();
		final List<RepositoryEntry> entries = rm.getLearningResourcesAsStudent(ureq.getIdentity());
		repoTableModel.setObjects(entries);
		tableCtr.modelChanged();
		displaySearchResults(ureq);
	}

	protected void doSearchMyCoursesTeacher(final UserRequest ureq) {
		final RepositoryManager rm = RepositoryManager.getInstance();
		final List<RepositoryEntry> entries = rm.getLearningResourcesAsTeacher(ureq.getIdentity());
		repoTableModel.setObjects(entries);
		tableCtr.modelChanged();
		displaySearchResults(ureq);
	}

	/**
	 * @return Returns the selectedEntry.
	 */
	public RepositoryEntry getSelectedEntry() {
		return selectedEntry;
	}

	/**
	 * Will reset the controller to display the search form again.
	 */
	public void displaySearchForm() {
		searchForm.setVisible(true);
		vc.setPage(VELOCITY_ROOT + "/search.html");
	}

	/**
	 * Present the search results page.
	 */
	public void displaySearchResults(final UserRequest ureq) {
		searchForm.setVisible(false);
		if (repoTableModel.getRowCount() == 0) {
			vc.contextPut("hasResults", Boolean.FALSE);
		} else {
			vc.contextPut("hasResults", Boolean.TRUE);
		}
		backLink = LinkFactory.createLinkBack(vc, this);
		vc.setPage(VELOCITY_ROOT + "/results.html");
		// REVIEW:pb why can ureq be null here?
		vc.contextPut("isGuest", (ureq != null) ? new Boolean(ureq.getUserSession().getRoles().isGuestOnly()) : Boolean.FALSE);
		loginLink = LinkFactory.createLink("repo.login", vc, this);
		cancelButton = LinkFactory.createButton("cancel", vc, this);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest urequest, final Controller source, final Event event) {
		if (source == tableCtr) { // process table actions
			final TableEvent te = (TableEvent) event;
			selectedEntry = (RepositoryEntry) tableCtr.getTableDataModel().getObject(te.getRowId());
			if (te.getActionId().equals(RepositoryTableModel.TABLE_ACTION_SELECT_ENTRY)) {
				fireEvent(urequest, new Event(RepositoryTableModel.TABLE_ACTION_SELECT_ENTRY));
				return;
			} else if (te.getActionId().equals(RepositoryTableModel.TABLE_ACTION_SELECT_LINK)) {
				fireEvent(urequest, new Event(RepositoryTableModel.TABLE_ACTION_SELECT_LINK));
				return;
			}
		} else if (event instanceof EntryChangedEvent) { // remove deleted entry
			final EntryChangedEvent ecv = (EntryChangedEvent) event;
			if (ecv.getChange() == EntryChangedEvent.DELETED) {
				final List<RepositoryEntry> newEntries = new ArrayList<RepositoryEntry>();
				for (int i = 0; i < repoTableModel.getRowCount(); i++) {
					final RepositoryEntry foo = (RepositoryEntry) repoTableModel.getObject(i);
					if (!foo.getKey().equals(ecv.getChangedEntryKey())) {
						newEntries.add(foo);
					}
				}
				repoTableModel.setObjects(newEntries);
				tableCtr.modelChanged();
			} else if (ecv.getChange() == EntryChangedEvent.ADDED) {
				doSearchByOwner(urequest.getIdentity());
			}
		} else if (source == searchForm) { // process search form events
			if (event == Event.DONE_EVENT) {
				if (searchForm.hasId()) {
					doSearchById(searchForm.getId());
				} else if (enableSearchforAllReferencalbeInSearchForm) {
					doSearchAllReferencables(urequest);
				} else {
					doSearch(urequest);
				}
				return;
			} else if (event == Event.CANCELLED_EVENT) {
				fireEvent(urequest, Event.CANCELLED_EVENT);
				return;
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}
}