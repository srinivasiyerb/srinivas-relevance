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

import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.HrefGenerator;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.logging.AssertException;

/**
 * Description:<br>
 * TODO: bja Class Description for ChecklistMultiSelectColumnDescriptor
 * <P>
 * Initial Date: 11.08.2009 <br>
 * 
 * @author bja <bja@bps-system.de>
 */
public class ChecklistMultiSelectColumnDescriptor implements ColumnDescriptor {

	private Table table;
	private final String headerKey;
	private final int column;

	public ChecklistMultiSelectColumnDescriptor(final String headerKey, final int column) {
		this.headerKey = headerKey;
		this.column = column;
	}

	@Override
	public void renderValue(final StringOutput sb, final int row, final Renderer renderer) {
		// add checkbox
		final int currentPosInModel = table.getSortedRow(row);
		final boolean checked = (Boolean) table.getTableDataModel().getValueAt(currentPosInModel, column);
		if (renderer == null) {
			// special case for table download
			if (checked) {
				sb.append("x");
			}
		} else {
			sb.append("<input type=\"checkbox\" name=\"tb_ms\" value=\"").append(currentPosInModel).append("\"");
			if (checked) {
				sb.append(" checked=\"checked\"");
			}
			sb.append(" disabled=\"disabled\"");
			sb.append(" />");
		}
	}

	@Override
	public int compareTo(final int rowa, final int rowb) {
		final boolean rowaChecked = (Boolean) table.getTableDataModel().getValueAt(rowa, column);
		final boolean rowbChecked = (Boolean) table.getTableDataModel().getValueAt(rowb, column);
		if (rowaChecked && !rowbChecked) {
			return -1;
		} else if (!rowaChecked && rowbChecked) { return 1; }
		return 0;
	}

	@Override
	public boolean equals(final Object object) {
		if (object instanceof ChecklistMultiSelectColumnDescriptor) { return true; }
		return false;
	}

	@Override
	public String getHeaderKey() {
		return this.headerKey;
	}

	@Override
	public boolean translateHeaderKey() {
		return false;
	}

	@Override
	public int getAlignment() {
		return ColumnDescriptor.ALIGNMENT_CENTER;
	}

	@Override
	public String getAction(final int row) {
		// no action
		return null;
	}

	@Override
	public HrefGenerator getHrefGenerator() {
		// no HrefGenerator
		return null;
	}

	@Override
	public String getPopUpWindowAttributes() {
		// no PopuWindow
		return null;
	}

	@Override
	public boolean isPopUpWindowAction() {
		return false;
	}

	@Override
	public boolean isSortingAllowed() {
		return true;
	}

	@Override
	public void modelChanged() {
		// nothing to do here
	}

	@Override
	public void otherColumnDescriptorSorted() {
		// nothing to do here
	}

	@Override
	public void setHrefGenerator(final HrefGenerator h) {
		throw new AssertException("Not allowed to set HrefGenerator on MultiSelectColumn.");
	}

	@Override
	public void setTable(final Table table) {
		this.table = table;
	}

	@Override
	public void sortingAboutToStart() {
		// nothing to do here
	}

	@Override
	public String toString(final int rowid) {
		// return table.getMultiSelectSelectedRows().get(rowid) ? "checked" : "unchecked";
		return "checked";
	}

}