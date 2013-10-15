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

package org.olat.bookmark;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.type.Type;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.resource.OresHelper;
import org.olat.user.UserDataDeletable;

/**
 * Description: This bookmark manager persists the bookmarks in the database.
 * 
 * @author Sabina Jeger
 */
public class BookmarkManagerImpl extends BookmarkManager implements UserDataDeletable {
	private static List<BookmarkHandler> bookmarkHandlers = new ArrayList<BookmarkHandler>();
	private static OLog log = Tracing.createLoggerFor(BookmarkManagerImpl.class);

	/**
	 * Do not use this method to access the bookmark manager! Use the BookmakrManager.getInstance() method instead to access the spring loaded bookmark manager
	 */
	public BookmarkManagerImpl(final UserDeletionManager userDeletionManager) {
		userDeletionManager.registerDeletableUserData(this);
		INSTANCE = this;
	}

	/**
	 * @param newBookmark
	 */
	@Override
	public void createAndPersistBookmark(final Bookmark newBookmark) {
		final DB db = DBFactory.getInstance();
		db.saveObject(newBookmark);
		if (log.isDebug()) {
			log.debug("Bookmark has been created: " + newBookmark.getTitle());
		}
		fireBookmarkEvent(newBookmark.getOwner());
	}

	/**
	 * @param identity
	 * @return a List of found bookmarks of given subject
	 */
	@Override
	public List<Bookmark> findBookmarksByIdentity(final Identity identity) {
		final String query = "from org.olat.bookmark.BookmarkImpl as b where b.owner = ?";
		return DBFactory.getInstance().find(query, identity.getKey(), Hibernate.LONG);
	}

	/**
	 * Finds bookmarks of a specific type for an identity
	 * 
	 * @param identity
	 * @param type
	 * @return list of bookmarks for this identity
	 */
	@Override
	public List<Bookmark> findBookmarksByIdentity(final Identity identity, final String type) {
		final String query = "from org.olat.bookmark.BookmarkImpl as b where b.owner = ? and b.displayrestype = ?";
		final List<Bookmark> found = DBFactory.getInstance().find(query, new Object[] { identity.getKey(), type }, new Type[] { Hibernate.LONG, Hibernate.STRING });
		return found;
	}

	/**
	 * @param changedBookmark
	 * @return true if saved successfully
	 */
	@Override
	public void updateBookmark(final Bookmark changedBookmark) {
		DBFactory.getInstance().updateObject(changedBookmark);
		fireBookmarkEvent(changedBookmark.getOwner());
	}

	/**
	 * @param deletableBookmark
	 * @return true if success
	 */
	@Override
	public void deleteBookmark(final Bookmark deletableBookmark) {
		DBFactory.getInstance().deleteObject(deletableBookmark);
		fireBookmarkEvent(deletableBookmark.getOwner());
	}

	/**
	 * calculates the URL for launching a bookmark
	 * 
	 * @param chosenBm
	 * @return resourceablea instance
	 */
	@Override
	public OLATResourceable getLaunchOlatResourceable(final Bookmark chosenBm) {
		final String finalType = chosenBm.getOlatrestype();
		final Long finalKey = chosenBm.getOlatreskey();
		final OLATResourceable res = OresHelper.createOLATResourceableInstance(finalType, finalKey);
		return res;
	}

	/**
	 * Delete all bookmarks pointing to the given resourceable.
	 * 
	 * @param res
	 */
	@Override
	public void deleteAllBookmarksFor(final OLATResourceable res) {
		final String query = "from org.olat.bookmark.BookmarkImpl as b where b.olatrestype = ? and b.olatreskey = ?";
		DBFactory.getInstance().delete(query, new Object[] { res.getResourceableTypeName(), res.getResourceableId() }, new Type[] { Hibernate.STRING, Hibernate.LONG });
		fireBookmarkEvent(null);
	}

	/**
	 * @param identity
	 * @param res
	 * @return true if resourceable is bookmarked
	 */
	@Override
	public boolean isResourceableBookmarked(final Identity identity, final OLATResourceable res) {
		final String query = "from org.olat.bookmark.BookmarkImpl as b where b.olatrestype = ? and b.olatreskey = ? and b.owner.key = ?";

		final List results = DBFactory.getInstance().find(query, new Object[] { res.getResourceableTypeName(), res.getResourceableId(), identity.getKey() },
				new Type[] { Hibernate.STRING, Hibernate.LONG, Hibernate.LONG });
		return results.size() != 0;
	}

	/**
	 * Delete all bookmarks for certain identity.
	 * 
	 * @see org.olat.user.UserDataDeletable#deleteUserData(org.olat.core.id.Identity)
	 */
	@Override
	public void deleteUserData(final Identity identity, final String newDeletedUserName) {
		final List bookmarks = findBookmarksByIdentity(identity);
		for (final Iterator iter = bookmarks.iterator(); iter.hasNext();) {
			deleteBookmark((Bookmark) iter.next());
		}
		log.debug("All bookmarks deleted for identity=" + identity);
		// no need to fire BookmarkEvent - the user was deleted
	}

	/**
	 * Launch the given bookmark
	 * 
	 * @param bookmark
	 * @param ureq
	 * @param wControl
	 * @return TRUE: launch successful; FALSE: no launcher found (unknonwn bookmark type)
	 */
	@Override
	public boolean launchBookmark(final Bookmark bookmark, final UserRequest ureq, final WindowControl wControl) {
		for (final BookmarkHandler handler : bookmarkHandlers) {
			final boolean success = handler.tryToLaunch(bookmark, ureq, wControl);
			if (log.isDebug()) {
				log.debug("Tried to launch bookmark::" + bookmark + " with handler::" + handler + " with result::" + success);
			}
			if (success) { return true; }
		}
		// no handler found that could launch the given bookmark
		log.warn("Could not find a launcher for bookmark::" + bookmark + " with displayType::" + bookmark.getDisplayrestype());
		return false;
	}

	/**
	 * Create a fully qualified URL that can be used to launch this bookmark e.g. from a browser bookmark or an RSS feed document
	 * 
	 * @param bookmark
	 * @return URL or NULL if not successful
	 */
	@Override
	public String createJumpInURL(final Bookmark bookmark) {
		for (final BookmarkHandler handler : bookmarkHandlers) {
			// try it with this handler, will return an URL or null if it was the wrong handler
			final String url = handler.createJumpInURL(bookmark);
			if (log.isDebug()) {
				log.debug("Tried to create jump in URL for bookmark::" + bookmark + " with handler::" + handler + " with result::" + url);
			}
			if (url != null) { return url; }
		}
		// no handler found that could launch the given bookmark
		log.warn("Could not create a jump in URL for bookmark::" + bookmark + " with displayType::" + bookmark.getDisplayrestype());
		return null;
	}

	/**
	 * Spring setter method
	 * 
	 * @param newBookmarkHanlders
	 */
	@Override
	public void setBookmarkHandlers(final List<BookmarkHandler> newBookmarkHanlders) {
		bookmarkHandlers = newBookmarkHanlders;
	}

	/**
	 * Fire MultiUserEvent - BookmarkEvent - after add/modify/delete bookmark.
	 * <p>
	 * If the input identity not null the event is intended only for one user, else for all users.
	 * 
	 * @param bookmark
	 */
	private void fireBookmarkEvent(final Identity identity) {
		// event for all users
		BookmarkEvent bookmarkEvent = new BookmarkEvent();
		OLATResourceable eventBusOres = OresHelper.createOLATResourceableType(Identity.class);
		if (identity != null) {
			// event for the specified user
			bookmarkEvent = new BookmarkEvent(identity.getName());
			eventBusOres = OresHelper.createOLATResourceableInstance(Identity.class, identity.getKey());
		}
		// TODO: LD: use this: //UserSession.getSingleUserEventCenter().fireEventToListenersOf(bookmarkEvent, eventBusOres);
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(bookmarkEvent, eventBusOres);
	}

}
