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

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.type.Type;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.resource.OresHelper;
import org.olat.user.UserDataDeletable;

/**
 * Description:
 * 
 * @author Alexander Schneider
 */
public class NoteManager extends BasicManager implements UserDataDeletable {
	private static NoteManager instance;

	/**
	 * [spring]
	 * 
	 * @param userDeletionManager
	 */
	private NoteManager(final UserDeletionManager userDeletionManager) {
		userDeletionManager.registerDeletableUserData(this);
		instance = this;
	}

	/**
	 * @return the singleton
	 */
	public static NoteManager getInstance() {
		return instance;
	}

	/**
	 * @param owner
	 * @param resourceTypeName
	 * @param resourceTypeId
	 * @return a note, either a new one in RAM, or the persisted if found using the params
	 */
	public Note loadNoteOrCreateInRAM(final Identity owner, final String resourceTypeName, final Long resourceTypeId) {
		Note note = findNote(owner, resourceTypeName, resourceTypeId);
		if (note == null) {
			note = createNote(owner, resourceTypeName, resourceTypeId);
		}
		return note;
	}

	/**
	 * @param owner
	 * @param resourceTypeName
	 * @param resourceTypeId
	 * @return the note
	 */
	private Note createNote(final Identity owner, final String resourceTypeName, final Long resourceTypeId) {
		final Note n = new NoteImpl();
		n.setOwner(owner);
		n.setResourceTypeName(resourceTypeName);
		n.setResourceTypeId(resourceTypeId);
		return n;
	}

	/**
	 * @param owner
	 * @param resourceTypeName
	 * @param resourceTypeId
	 * @return the note
	 */
	private Note findNote(final Identity owner, final String resourceTypeName, final Long resourceTypeId) {

		final String query = "from org.olat.note.NoteImpl as n where n.owner = ? and n.resourceTypeName = ? and n.resourceTypeId = ?";
		final List notes = DBFactory.getInstance().find(query, new Object[] { owner.getKey(), resourceTypeName, resourceTypeId },
				new Type[] { Hibernate.LONG, Hibernate.STRING, Hibernate.LONG });

		if (notes == null || notes.size() != 1) {
			return null;
		} else {
			return (Note) notes.get(0);
		}
	}

	/**
	 * @param owner
	 * @return a list of notes belonging to the owner
	 */
	public List<Note> listUserNotes(final Identity owner) {
		final String query = "from org.olat.note.NoteImpl as n inner join fetch n.owner as noteowner where noteowner = :noteowner";
		final DBQuery dbQuery = DBFactory.getInstance().createQuery(query.toString());
		dbQuery.setEntity("noteowner", owner);
		final List<Note> notes = dbQuery.list();
		return notes;
	}

	/**
	 * Deletes a note on the database
	 * 
	 * @param n the note
	 */
	public void deleteNote(Note n) {
		n = (Note) DBFactory.getInstance().loadObject(n);
		DBFactory.getInstance().deleteObject(n);
		fireBookmarkEvent(n.getOwner());
	}

	/**
	 * Save a note
	 * 
	 * @param n
	 */
	public void saveNote(final Note n) {
		n.setLastModified(new Date());
		DBFactory.getInstance().saveObject(n);
		fireBookmarkEvent(n.getOwner());
	}

	/**
	 * Update a note
	 * 
	 * @param n
	 */
	public void updateNote(final Note n) {
		n.setLastModified(new Date());
		DBFactory.getInstance().updateObject(n);
		fireBookmarkEvent(n.getOwner());
	}

	/**
	 * Delete all notes for certain identity.
	 * 
	 * @param identity Delete notes for this identity.
	 */
	@Override
	@SuppressWarnings("unused")
	public void deleteUserData(final Identity identity, final String newDeletedUserName) {
		final List<Note> userNotes = this.listUserNotes(identity);
		for (final Iterator<Note> iter = userNotes.iterator(); iter.hasNext();) {
			this.deleteNote(iter.next());
		}
		if (isLogDebugEnabled()) {
			logDebug("All notes deleted for identity=" + identity, null);
		}
	}

	/**
	 * Fire NoteEvent for a specific user after save/update/delete note.
	 * 
	 * @param identity
	 */
	private void fireBookmarkEvent(final Identity identity) {
		// event this identity
		final NoteEvent noteEvent = new NoteEvent(identity.getName());
		final OLATResourceable eventBusOres = OresHelper.createOLATResourceableInstance(Identity.class, identity.getKey());
		// TODO: LD: use SingleUserEventCenter
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(noteEvent, eventBusOres);
	}
}