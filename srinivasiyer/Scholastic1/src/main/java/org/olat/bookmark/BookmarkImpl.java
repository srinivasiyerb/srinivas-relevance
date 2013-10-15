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

import org.olat.core.commons.persistence.PersistentObject;
import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;

/**
 * Description:
 * 
 * @author Sabina Jeger
 */
public class BookmarkImpl extends PersistentObject implements Bookmark {

	private static final int DISPLAYRESTYPE_MAXLENGTH = 50;
	private static final int OLATRESTYPE_MAXLENGTH = 50;

	// bookmark fields
	private String displayrestype; // resourceable type to display to user
	private String olatrestype; // olat resourceable type
	private Long olatreskey; // olat resourceable key

	private String title;
	private String description;
	private String detaildata;
	private Identity owner = null;

	/**
	 * Default constructor (needed by hibernate).
	 */
	protected BookmarkImpl() {
		super();
	}

	/**
	 * @param displayrestype
	 * @param olatrestype
	 * @param olatreskey
	 * @param title
	 * @param intref
	 * @param ident
	 */
	BookmarkImpl(final String displayrestype, final String olatrestype, final Long olatreskey, final String title, final String intref, final Identity ident) {
		super();
		this.displayrestype = displayrestype;
		this.olatrestype = olatrestype;
		this.olatreskey = olatreskey;
		this.title = title;
		this.detaildata = intref;
		this.owner = ident;
	}

	/**
	 * @see Bookmark#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * @see org.olat.bookmark.Bookmark#getDetaildata()
	 */
	@Override
	public String getDetaildata() {
		return detaildata;
	}

	/**
	 * @see org.olat.bookmark.Bookmark#getOlatreskey()
	 */
	@Override
	public Long getOlatreskey() {
		return olatreskey;
	}

	/**
	 * @see Bookmark#getOlatrestype()
	 */
	@Override
	public String getOlatrestype() {
		return olatrestype;
	}

	/**
	 * @see org.olat.bookmark.Bookmark#getDisplayrestype()
	 */
	@Override
	public String getDisplayrestype() {
		return displayrestype;
	}

	/**
	 * @see Bookmark#getTitle()
	 */
	@Override
	public String getTitle() {
		return title;
	}

	/**
	 * @see org.olat.bookmark.Bookmark#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(final String string) {
		description = string;
	}

	/**
	 * @see org.olat.bookmark.Bookmark#setDetaildata(java.lang.String)
	 */
	@Override
	public void setDetaildata(final String string) {
		detaildata = string;
	}

	/**
	 * @see org.olat.bookmark.Bookmark#setOlatreskey(java.lang.Long)
	 */
	@Override
	public void setOlatreskey(final Long reskey) {
		olatreskey = reskey;
	}

	/**
	 * @see org.olat.bookmark.Bookmark#setOlatrestype(java.lang.String)
	 */
	@Override
	public void setOlatrestype(final String string) {
		if (string.length() > OLATRESTYPE_MAXLENGTH) { throw new AssertException("olatrestype in o_bookmark too long."); }
		olatrestype = string;
	}

	/**
	 * Set the res type to be displayed in the bookmark broperties.
	 * 
	 * @param string
	 */
	public void setDisplayrestype(final String string) {
		if (string.length() > DISPLAYRESTYPE_MAXLENGTH) { throw new AssertException("displayrestype in o_bookmark too long."); }
		displayrestype = string;
	}

	/**
	 * @see Bookmark#getOwner()
	 */
	@Override
	public Identity getOwner() {
		return owner;
	}

	/**
	 * @see org.olat.bookmark.Bookmark#setOwner(org.olat.core.id.Identity)
	 */
	@Override
	public void setOwner(final Identity ident) {
		owner = ident;
	}

	/**
	 * @see org.olat.bookmark.Bookmark#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(final String string) {
		title = string;
	}

}
