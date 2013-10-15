/**
 * BPS Bildungsportal Sachsen GmbH<br>
 * Bahnhofstrasse 6<br>
 * 09111 Chemnitz<br>
 * Germany<br>
 * Copyright (c) 2005-2009 by BPS Bildungsportal Sachsen GmbH<br>
 * http://www.bps-system.de<br>
 * All rights reserved.
 */
package de.bps.olat.modules.cl;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;

public class ChecklistManageTableDataModel extends DefaultTableDataModel {

	private static int COLUMN_COUNT;
	private static int ROW_COUNT;

	private final Checklist checklist;
	private final List<Identity> participants;
	private final List entries;

	@SuppressWarnings("unchecked")
	public ChecklistManageTableDataModel(final Checklist checklist, final List<Identity> participants) {
		super(participants);
		this.checklist = checklist;
		this.participants = participants;

		COLUMN_COUNT = checklist.getCheckpoints().size() + 2;
		ROW_COUNT = participants.size();

		this.entries = new ArrayList(ROW_COUNT);
		for (final Identity identity : participants) {
			final List row = new ArrayList(COLUMN_COUNT);
			// name
			row.add(identity.getUser().getProperty(UserConstants.FIRSTNAME, getLocale()) + " " + identity.getUser().getProperty(UserConstants.LASTNAME, getLocale()));
			// checkpoints value
			for (final Checkpoint checkpoint : this.checklist.getCheckpointsSorted(ChecklistUIFactory.comparatorTitleAsc)) {
				row.add(checkpoint.getSelectionFor(identity));
			}
			// action
			row.add(true);
			// add to columns
			entries.add(row);
		}
	}

	@Override
	public int getColumnCount() {
		// name, 1-n checkpoints, action
		return COLUMN_COUNT;
	}

	@Override
	public int getRowCount() {
		return ROW_COUNT;
	}

	@Override
	public Object getValueAt(final int row, final int col) {
		final List entry = (List) entries.get(row);
		return entry.get(col);
	}

	public Identity getParticipantAt(final int row) {
		return participants.get(row);
	}
}
