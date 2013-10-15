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

package org.olat.note;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.olat.ControllerFactory;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.fullWebApp.popup.BaseFullWebappPopupLayoutFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.generic.popup.PopupBrowserWindow;
import org.olat.core.gui.control.generic.portal.AbstractPortletRunController;
import org.olat.core.gui.control.generic.portal.PortletDefaultTableDataModel;
import org.olat.core.gui.control.generic.portal.PortletEntry;
import org.olat.core.gui.control.generic.portal.PortletToolSortingControllerImpl;
import org.olat.core.gui.control.generic.portal.SortingCriteria;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.resource.OresHelper;
import org.olat.home.site.HomeSite;

/**
 * Description:<br>
 * Run view controller for the groups list portlet
 * <P>
 * Initial Date: 11.07.2005 <br>
 * 
 * @author gnaegi
 */
public class NotesPortletRunController extends AbstractPortletRunController implements GenericEventListener {

	private static final String CMD_LAUNCH = "cmd.launch";

	private final TableController tableCtr;
	private NoteSortingTableDataModel notesListModel;
	private final VelocityContainer notesVC;
	private final Identity cOwner;
	private final Link showAllLink;
	private final OLATResourceable eventBusThisIdentityOres;

	/**
	 * Constructor
	 * 
	 * @param ureq
	 * @param component
	 */
	public NotesPortletRunController(final WindowControl wControl, final UserRequest ureq, final Translator trans, final String portletName) {
		super(wControl, ureq, trans, portletName);
		this.cOwner = ureq.getIdentity();

		sortingTermsList.add(SortingCriteria.ALPHABETICAL_SORTING);
		sortingTermsList.add(SortingCriteria.DATE_SORTING);

		this.notesVC = this.createVelocityContainer("notesPortlet");
		showAllLink = LinkFactory.createLink("notesPortlet.showAll", notesVC, this);

		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(trans.translate("notesPortlet.nonotes"));
		tableConfig.setDisplayTableHeader(false);
		tableConfig.setCustomCssClass("b_portlet_table");
		tableConfig.setDisplayRowCount(false);
		tableConfig.setPageingEnabled(false);
		tableConfig.setDownloadOffered(false);
		// disable the default sorting for this table
		tableConfig.setSortingEnabled(false);
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), trans);
		listenTo(tableCtr);

		// dummy header key, won't be used since setDisplayTableHeader is set to false
		final DefaultColumnDescriptor cd0 = new DefaultColumnDescriptor("notesPortlet.bgname", 0, CMD_LAUNCH, trans.getLocale());
		cd0.setIsPopUpWindowAction(true, "height=550, width=750, location=no, menubar=no, resizable=yes, status=no, scrollbars=yes, toolbar=no");
		tableCtr.addColumnDescriptor(cd0);
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("notesPortlet.type", 1, null, trans.getLocale(), ColumnDescriptor.ALIGNMENT_RIGHT));

		this.sortingCriteria = getPersistentSortingConfiguration(ureq);
		reloadModel(sortingCriteria);
		this.notesVC.put("table", tableCtr.getInitialComponent());

		putInitialPanel(notesVC);

		// register for events targeted at this Identity - TODO: LD: use SingleUserEventCenter
		eventBusThisIdentityOres = OresHelper.createOLATResourceableInstance(Identity.class, identity.getKey());
		CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(this, ureq.getIdentity(), eventBusThisIdentityOres);
	}

	/**
	 * @param ureq
	 * @return
	 */
	private List<PortletEntry> getAllPortletEntries() {
		final NoteManager nm = NoteManager.getInstance();
		final List<Note> noteList = nm.listUserNotes(cOwner);
		return convertNoteToPortletEntryList(noteList);
	}

	/**
	 * @param items
	 * @return
	 */
	private List<PortletEntry> convertNoteToPortletEntryList(final List<Note> items) {
		final List<PortletEntry> convertedList = new ArrayList<PortletEntry>();
		final Iterator<Note> listIterator = items.iterator();
		while (listIterator.hasNext()) {
			convertedList.add(new NotePortletEntry(listIterator.next()));
		}
		return convertedList;
	}

	/**
	 * @see org.olat.core.gui.control.generic.portal.AbstractPortletRunController#reloadModel(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.generic.portal.SortingCriteria)
	 */
	@Override
	protected void reloadModel(final SortingCriteria sortingCriteria) {
		if (sortingCriteria.getSortingType() == SortingCriteria.AUTO_SORTING) {
			final NoteManager nm = NoteManager.getInstance();
			List<Note> noteList = nm.listUserNotes(cOwner);

			noteList = getSortedList(noteList, sortingCriteria);

			final List<PortletEntry> entries = convertNoteToPortletEntryList(noteList);
			notesListModel = new NoteSortingTableDataModel(entries, locale);
			tableCtr.setTableDataModel(notesListModel);
		} else {
			reloadModel(this.getPersistentManuallySortedItems());
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.portal.AbstractPortletRunController#reloadModel(org.olat.core.gui.UserRequest, java.util.List)
	 */
	@Override
	protected void reloadModel(final List<PortletEntry> sortedItems) {
		notesListModel = new NoteSortingTableDataModel(sortedItems, locale);
		tableCtr.setTableDataModel(notesListModel);
	}

	/**
	 * Listen to NoteEvents for this identity.
	 * 
	 * @see org.olat.core.util.event.GenericEventListener#event(org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final Event event) {
		if (event instanceof NoteEvent) {
			if (((NoteEvent) event).getUsername().equals(identity.getName())) {
				reloadModel(sortingCriteria);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == showAllLink) {
			// activate homes tab in top navigation and active notes menu item
			final DTabs dts = (DTabs) Windows.getWindows(ureq).getWindow(ureq).getAttribute("DTabs");
			// was brasato:: getWindowControl().getDTabs().activateStatic(ureq, HomeSite.class.getName(), "note");
			dts.activateStatic(ureq, HomeSite.class.getName(), "note");
		}
	}

	/**
	 * @see org.olat.core.gui.control.ControllerEventListener#dispatchEvent(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller,
	 *      org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		super.event(ureq, source, event);
		if (source == tableCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				final TableEvent te = (TableEvent) event;
				final String actionid = te.getActionId();
				if (actionid.equals(CMD_LAUNCH)) {
					final int rowid = te.getRowId();
					final NotePortletEntry pe = (NotePortletEntry) notesListModel.getObject(rowid);
					final Note note = pe.getValue();
					// will not be disposed on course run dispose, popus up as new browserwindow
					final ControllerCreator ctrlCreator = new ControllerCreator() {
						@Override
						public Controller createController(final UserRequest lureq, final WindowControl lwControl) {
							final Controller nc = new NoteController(lureq, lwControl, note);
							// use on column layout
							final LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(lureq, lwControl, null, null, nc.getInitialComponent(), null);
							layoutCtr.addDisposableChildController(nc); // dispose content on layout dispose
							return layoutCtr;
						}
					};
					// wrap the content controller into a full header layout
					final ControllerCreator layoutCtrlr = BaseFullWebappPopupLayoutFactory.createAuthMinimalPopupLayout(ureq, ctrlCreator);
					// open in new browser window
					final PopupBrowserWindow pbw = getWindowControl().getWindowBackOffice().getWindowManager().createNewPopupBrowserWindowFor(ureq, layoutCtrlr);
					pbw.open(ureq);
					//
				}
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		CoordinatorManager.getInstance().getCoordinator().getEventBus().deregisterFor(this, eventBusThisIdentityOres);
		super.doDispose();
	}

	/**
	 * Retrieves the persistent sortingCriteria and the persistent manually sorted, if any, creates the table model for the manual sorting, and instantiates the
	 * PortletToolSortingControllerImpl.
	 * 
	 * @param ureq
	 * @param wControl
	 * @return a PortletToolSortingControllerImpl instance.
	 */
	protected PortletToolSortingControllerImpl createSortingTool(final UserRequest ureq, final WindowControl wControl) {
		if (portletToolsController == null) {

			final List<PortletEntry> entries = getAllPortletEntries();
			final PortletDefaultTableDataModel tableDataModel = new NoteManualSortingTableDataModel(entries);
			final List<PortletEntry> sortedItems = getPersistentManuallySortedItems();

			portletToolsController = new PortletToolSortingControllerImpl(ureq, wControl, getTranslator(), sortingCriteria, tableDataModel, sortedItems);
			portletToolsController.setConfigManualSorting(true);
			portletToolsController.setConfigAutoSorting(true);
			portletToolsController.addControllerListener(this);
		}
		return portletToolsController;
	}

	/**
	 * @param ureq
	 * @return
	 */
	private List<PortletEntry> getPersistentManuallySortedItems() {
		final List<PortletEntry> entries = getAllPortletEntries();
		return this.getPersistentManuallySortedItems(entries);
	}

	/**
	 * @see org.olat.core.gui.control.generic.portal.AbstractPortletRunController#getComparator(org.olat.core.gui.control.generic.portal.SortingCriteria)
	 */
	@Override
	protected Comparator getComparator(final SortingCriteria sortingCriteria) {
		return new Comparator() {
			@Override
			public int compare(final Object o1, final Object o2) {
				final Note note1 = (Note) o1;
				final Note note2 = (Note) o2;
				int comparisonResult = 0;
				if (sortingCriteria.getSortingTerm() == SortingCriteria.ALPHABETICAL_SORTING) {
					comparisonResult = collator.compare(StringEscapeUtils.escapeHtml(note1.getNoteTitle()).toString(), StringEscapeUtils.escapeHtml(note2.getNoteTitle())
							.toString());
				} else if (sortingCriteria.getSortingTerm() == SortingCriteria.DATE_SORTING) {
					comparisonResult = note1.getLastModified().compareTo(note2.getLastModified());
				}
				if (!sortingCriteria.isAscending()) {
					// if not isAscending return (-comparisonResult)
					return -comparisonResult;
				}
				return comparisonResult;
			}
		};
	}

	/**
	 * PortletDefaultTableDataModel implementation for the current portlet.
	 * <P>
	 * Initial Date: 18.12.2007 <br>
	 * 
	 * @author Lavinia Dumitrescu
	 */
	private class NoteSortingTableDataModel extends PortletDefaultTableDataModel {
		private final Locale locale;

		public NoteSortingTableDataModel(final List<PortletEntry> objects, final Locale locale) {
			super(objects, 2);
			this.locale = locale;
		}

		/**
		 * @see org.olat.core.gui.components.table.TableDataModel#getValueAt(int, int)
		 */
		@Override
		public final Object getValueAt(final int row, final int col) {
			final Note note = (Note) getObject(row).getValue();
			switch (col) {
				case 0:
					return StringEscapeUtils.escapeHtml(note.getNoteTitle()).toString();
				case 1:
					final String resType = note.getResourceTypeName();
					return (resType == null ? "n/a" : ControllerFactory.translateResourceableTypeName(resType, locale));
				default:
					return "error";
			}
		}
	}

	/**
	 * Different from the above model only in the second column value.
	 * <P>
	 * Initial Date: 18.12.2007 <br>
	 * 
	 * @author Lavinia Dumitrescu
	 */
	private class NoteManualSortingTableDataModel extends PortletDefaultTableDataModel {
		/**
		 * @param objects
		 * @param locale
		 */
		public NoteManualSortingTableDataModel(final List<PortletEntry> objects) {
			super(objects, 2);
		}

		/**
		 * @see org.olat.core.gui.components.table.TableDataModel#getValueAt(int, int)
		 */
		@Override
		public final Object getValueAt(final int row, final int col) {
			final Note note = (Note) getObject(row).getValue();
			switch (col) {
				case 0:
					return StringEscapeUtils.escapeHtml(note.getNoteTitle()).toString();
				case 1:
					final Date lastUpdate = note.getLastModified();
					return lastUpdate;
					// return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getTranslator().getLocale()).format(lastUpdate);
				default:
					return "error";
			}
		}
	}

	/**
	 * PortletEntry impl for Note values.
	 * <P>
	 * Initial Date: 10.12.2007 <br>
	 * 
	 * @author Lavinia Dumitrescu
	 */
	class NotePortletEntry implements PortletEntry {
		private final Note value;
		private final Long key;

		public NotePortletEntry(final Note note) {
			value = note;
			key = note.getKey();
		}

		@Override
		public Long getKey() {
			return key;
		}

		@Override
		public Note getValue() {
			return value;
		}
	}

}
