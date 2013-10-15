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

import java.util.List;
import java.util.Locale;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.StaticColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.change.ChangeManager;
import org.olat.core.util.event.EventBus;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.core.util.resource.OresHelper;

/**
 * Description: Lists all notes of a certain user. The user may choose or delete a note. Works togheter with the NoteController.
 * 
 * @author Alexander Schneider
 */
public class NoteListController extends BasicController implements GenericEventListener {

	private NoteListTableDataModel nLModel;
	private Note chosenN = null;
	private final NoteManager nm = NoteManager.getInstance();

	private final TableController tableC;
	private DialogBoxController deleteDialogCtr;
	private NoteController nc;
	private final EventBus sec;
	private final Identity cOwner;
	private final Locale locale;
	private CloseableModalController cmc;

	/**
	 * @param ureq
	 * @param wControl
	 */
	public NoteListController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		this.cOwner = ureq.getIdentity();
		this.locale = ureq.getLocale();

		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setDownloadOffered(false);
		tableConfig.setTableEmptyMessage(translate("note.nonotes"));
		tableC = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		listenTo(tableC); // autodispose on controller dispose
		tableC.addColumnDescriptor(new DefaultColumnDescriptor("table.note.title", 0, "choose", ureq.getLocale()));
		tableC.addColumnDescriptor(new DefaultColumnDescriptor("table.note.resource", 1, null, ureq.getLocale()));
		tableC.addColumnDescriptor(new StaticColumnDescriptor("delete", "table.header.delete", translate("action.delete")));
		populateNLTable();

		putInitialPanel(tableC.getInitialComponent());

		sec = ureq.getUserSession().getSingleUserEventCenter();
		sec.registerFor(this, ureq.getIdentity(), OresHelper.lookupType(Note.class));
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	@SuppressWarnings("unused")
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// no events to catch
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		// if row has been clicked
		if (source == tableC) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				final TableEvent te = (TableEvent) event;
				final String actionid = te.getActionId();
				final int rowid = te.getRowId();
				this.chosenN = (Note) nLModel.getObject(rowid);
				if (actionid.equals("choose")) {

					removeAsListenerAndDispose(nc);
					nc = new NoteController(ureq, getWindowControl(), chosenN);
					listenTo(nc);

					removeAsListenerAndDispose(cmc);
					cmc = new CloseableModalController(getWindowControl(), translate("close"), nc.getInitialComponent());
					listenTo(cmc);

					cmc.activate();
				} else if (actionid.equals("delete")) {
					deleteDialogCtr = activateYesNoDialog(ureq, null, translate("note.delete.confirmation"), deleteDialogCtr);
				}
			}
		} else if (source == deleteDialogCtr) {
			if (DialogBoxUIFactory.isYesEvent(event)) {
				// delete is ok
				nm.deleteNote(chosenN);
				// fire local event (for the same user)
				// TODO:fj:a make Note (and all persistables) olatresourceables: problem: type is then NoteImpl instead of Note
				final OLATResourceable ores = OresHelper.createOLATResourceableInstance(Note.class, chosenN.getKey());
				ureq.getUserSession().getSingleUserEventCenter().fireEventToListenersOf(new OLATResourceableJustBeforeDeletedEvent(ores), ores);
				showInfo("note.delete.successfull");
				populateNLTable();
				chosenN = null;
			} else {
				// answer was "no"
				chosenN = null;
			}
		} else if (source == nc) {
			if (event == Event.BACK_EVENT) {
				cmc.deactivate();
			}
		}
	}

	private void populateNLTable() {
		final List<Note> l = nm.listUserNotes(cOwner);
		nLModel = new NoteListTableDataModel(l, locale);
		tableC.setTableDataModel(nLModel);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		sec.deregisterFor(this, OresHelper.lookupType(Note.class));
	}

	@Override
	public void event(final Event event) {
		if (ChangeManager.isChangeEvent(event)) {
			populateNLTable();
		}
	}

}