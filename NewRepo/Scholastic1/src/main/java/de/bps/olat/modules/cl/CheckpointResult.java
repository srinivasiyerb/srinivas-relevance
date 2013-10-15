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
import java.util.Date;

import org.olat.core.commons.persistence.PersistentObject;
import org.olat.core.id.ModifiedInfo;

/**
 * Description:<br>
 * CheckpointResults object, stored in "o_checkpoint_results" via Hibernate
 * <P>
 * Initial Date: 23.07.2009 <br>
 * 
 * @author bja <bja@bps-system.de>
 */
public class CheckpointResult extends PersistentObject implements ModifiedInfo, Serializable {

	private Date lastModified;
	private boolean result;
	private Long identityId;
	private Checkpoint checkpoint;

	public CheckpointResult() {
		//
	}

	public CheckpointResult(final Checkpoint checkpoint, final Long identityId, final boolean result) {
		this.checkpoint = checkpoint;
		this.identityId = identityId;
		this.result = result;
		this.lastModified = new Date();
	}

	public Checkpoint getCheckpoint() {
		return checkpoint;
	}

	public void setCheckpoint(final Checkpoint checkpoint) {
		this.checkpoint = checkpoint;
	}

	/**
	 * @return Returns the lastModified.
	 */
	@Override
	public Date getLastModified() {
		return lastModified;
	}

	/**
	 * @return Returns the result.
	 */
	public boolean getResult() {
		return result;
	}

	/**
	 * @return Returns the identityId.
	 */
	public Long getIdentityId() {
		return identityId;
	}

	/**
	 * @param lastModified The lastModified to set.
	 */
	@Override
	public void setLastModified(final Date lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * @param result The result to set.
	 */
	public void setResult(final boolean result) {
		this.result = result;
	}

	/**
	 * @param identityId The identityId to set.
	 */
	public void setIdentityId(final Long identityId) {
		this.identityId = identityId;
	}

}
