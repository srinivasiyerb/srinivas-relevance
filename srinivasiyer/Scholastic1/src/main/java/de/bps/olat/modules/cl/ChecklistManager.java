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
import java.util.Date;
import java.util.List;

import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerExecutor;
import org.olat.core.util.resource.OresHelper;

/**
 * Description:<br>
 * Manager for loading, saving and updating checklists and checkpoints.
 * <P>
 * Initial Date: 30.07.2009 <br>
 * 
 * @author bja <bja@bps-system.de>
 * @author skoeber <skoeber@bps-system.de>
 */
public class ChecklistManager {

	/** singleton */
	private static ChecklistManager INSTANCE = new ChecklistManager();

	private ChecklistManager() {
		// constructor
	}

	public static ChecklistManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Load checklist.
	 * 
	 * @param checklist
	 * @return checklist
	 */
	public Checklist loadChecklist(final Checklist cl) {
		Checklist checklist;
		try {
			// load from db
			checklist = (Checklist) DBFactory.getInstance().loadObject(cl);
		} catch (final Exception e) {
			DBFactory.getInstance().closeSession();
			// in case of error create new object as fallback
			checklist = new Checklist();
		}
		return checklist;
	}

	/**
	 * Load checklist
	 * 
	 * @param key
	 * @return checklist
	 */
	public Checklist loadChecklist(final Long key) {
		Checklist checklist;
		try {
			// load from db
			checklist = (Checklist) DBFactory.getInstance().loadObject(Checklist.class, key);
		} catch (final Exception e) {
			DBFactory.getInstance().closeSession();
			// in case of error create new object as fallback
			checklist = new Checklist();
		}
		return checklist;
	}

	/**
	 * Save new checklist.
	 * 
	 * @param checklist
	 */
	public void saveChecklist(final Checklist cl) {
		cl.setLastModified(new Date());
		DBFactory.getInstance().saveObject(cl);
	}

	/**
	 * Update checklist.
	 * 
	 * @param checklist
	 */
	public void updateChecklist(final Checklist cl) {
		final OLATResourceable ores = OresHelper.createOLATResourceableInstance(Checklist.class, cl.getKey());
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(ores, new SyncerExecutor() {
			@Override
			public void execute() {
				cl.setLastModified(new Date());
				DBFactory.getInstance().updateObject(cl);
			}
		});
	}

	/**
	 * Delete checklist.
	 * 
	 * @param checklist
	 */
	public void deleteChecklist(final Checklist cl) {
		final DB db = DBFactory.getInstance();
		final OLATResourceable ores = OresHelper.createOLATResourceableInstance(Checklist.class, cl.getKey());
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(ores, new SyncerExecutor() {
			@Override
			public void execute() {
				final Checklist checklist = (Checklist) db.loadObject(cl);
				db.deleteObject(checklist);
			}
		});
	}

	/**
	 * Update checkpoint
	 * 
	 * @param checkpoint
	 */
	public void updateCheckpoint(final Checkpoint cp) {
		final OLATResourceable ores = OresHelper.createOLATResourceableInstance(Checkpoint.class, cp.getKey());
		CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(ores, new SyncerExecutor() {
			@Override
			public void execute() {
				cp.setLastModified(new Date());
				DBFactory.getInstance().updateObject(cp);
			}
		});
	}

	/**
	 * Copy checklist without user data and results. Only in RAM, checklist will not be persisted.
	 * 
	 * @param checklist to copy
	 * @return the new checklist
	 */
	public Checklist copyChecklistInRAM(final Checklist cl) {
		final Checklist clCopy = new Checklist();
		clCopy.setTitle(cl.getTitle());
		clCopy.setDescription(cl.getDescription());
		final List<Checkpoint> checkpoints = cl.getCheckpoints();
		final List<Checkpoint> checkpointsCopy = new ArrayList<Checkpoint>();
		for (final Checkpoint cp : checkpoints) {
			final Checkpoint cpCopy = new Checkpoint();
			cpCopy.setChecklist(clCopy);
			cpCopy.setTitle(cp.getTitle());
			cpCopy.setDescription(cp.getDescription());
			cpCopy.setMode(cp.getMode());
			cpCopy.setLastModified(new Date());
			checkpointsCopy.add(cpCopy);
		}
		clCopy.setCheckpoints(checkpointsCopy);

		return clCopy;
	}

	/**
	 * Copy checklist without user data and results and save it.
	 * 
	 * @param checklist to copy
	 * @return the new persisted checklist
	 */
	public Checklist copyChecklist(final Checklist cl) {
		final Checklist clCopy = copyChecklistInRAM(cl);
		saveChecklist(clCopy);

		return clCopy;
	}

}
