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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.olat.core.commons.persistence.PersistentObject;
import org.olat.core.id.ModifiedInfo;

/**
 * Description:<br>
 * Checklist object, stored in "o_checklist" via Hibernate
 * <P>
 * Initial Date: 23.07.2009 <br>
 * 
 * @author bja <bja@bps-system.de>
 */
public class Checklist extends PersistentObject implements ModifiedInfo, Serializable {

	private String title;
	private String description;
	private Date lastMofified;
	private List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();

	public Checklist() {
		this.title = "";
		this.description = "";
	}

	public Checklist(final String title, final String description, final List<Checkpoint> checkpoints) {
		this.title = title;
		this.description = description;
		this.checkpoints = checkpoints;
		this.lastMofified = new Date();
	}

	/**
	 * @return Returns the title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return Returns the checkpoints.
	 */
	public List<Checkpoint> getCheckpoints() {
		return checkpoints;
	}

	/**
	 * @return Returns the checkpoints sorted.
	 */
	public List<Checkpoint> getCheckpointsSorted(final Comparator comparator) {
		Collections.sort(checkpoints, comparator);
		return checkpoints;
	}

	/**
	 * @param title The title to set.
	 */
	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * @param description The description to set.
	 */
	public void setDescription(final String description) {
		this.description = description;
	}

	/**
	 * @param checkpoints The checkpoints to set.
	 */
	public void setCheckpoints(final List<Checkpoint> checkpoints) {
		this.checkpoints = checkpoints;
	}

	/**
	 * @see org.olat.core.id.ModifiedInfo#getLastModified()
	 */
	@Override
	public Date getLastModified() {
		return this.lastMofified;
	}

	/**
	 * @see org.olat.core.id.ModifiedInfo#setLastModified(java.util.Date)
	 */
	@Override
	public void setLastModified(final Date date) {
		this.lastMofified = date;
	}

	/**
	 * Add checkpoint to this checklist
	 * 
	 * @param index
	 * @param checkpoint
	 */
	public void addCheckpoint(final int index, final Checkpoint checkpoint) {
		this.checkpoints.add(index, checkpoint);
	}

	/**
	 * Remove checkpoint from this checklist
	 * 
	 * @param checkpoint
	 */
	public void removeCheckpoint(final Checkpoint checkpoint) {
		this.checkpoints.remove(checkpoint);
	}

	/**
	 * @return <code>true</code> or <code>false</code>
	 */
	public boolean hasCheckpoints() {
		return (this.checkpoints != null) && (this.checkpoints.size() > 0);
	}

	/**
	 * Filter out unvisible checkpoints.
	 * 
	 * @return List with all visible checkpoints
	 */
	public List<Checkpoint> getVisibleCheckpoints() {
		final List<Checkpoint> visibleCheckpoints = new ArrayList<Checkpoint>();
		for (final Checkpoint checkpoint : getCheckpoints()) {
			if (!checkpoint.getMode().equals(CheckpointMode.MODE_HIDDEN)) {
				visibleCheckpoints.add(checkpoint);
			}
		}
		return visibleCheckpoints;
	}
}
