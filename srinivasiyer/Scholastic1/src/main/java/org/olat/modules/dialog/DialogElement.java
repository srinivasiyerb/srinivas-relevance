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

package org.olat.modules.dialog;

import java.util.Date;

import org.hibernate.ObjectNotFoundException;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;

/**
 * Description:<br>
 * TODO: guido Class Description for DialogElementsTableEntry
 * <P>
 * Initial Date: 08.11.2005 <br>
 * 
 * @author guido
 */
public class DialogElement {

	// table entries have to be objects, basic typs are not allowed
	private String filename;
	private String author;
	private String fileSize;
	private Date date;
	private transient Integer messagesCount;
	private transient Integer newMessages;
	private Long forumKey;

	public DialogElement() {
		// empty construvtor
	}

	// getters must be public for velocity access
	public DialogElement(final Long forumKey) {
		this.forumKey = forumKey;
	}

	/**
	 * get the full filename
	 * 
	 * @return
	 */
	public String getFilename() {
		return filename;
	}

	public String getAuthor() {
		try {
			// try to handle as identity id
			final Identity identity = BaseSecurityManager.getInstance().loadIdentityByKey(Long.valueOf(author));
			if (identity == null) { return author; }
			return identity.getName();
		} catch (final NumberFormatException nEx) {
			return author;
		} catch (final ObjectNotFoundException oEx) {
			DBFactory.getInstanceForClosing().rollbackAndCloseSession();
			return author;
		} catch (final Throwable th) {
			DBFactory.getInstanceForClosing().rollbackAndCloseSession();
			return author;
		}
	}

	public String getFileSize() {
		return fileSize;
	}

	public void setAuthor(final String author) {
		final Identity identity = BaseSecurityManager.getInstance().findIdentityByName(author);
		this.author = identity.getKey().toString();
	}

	public void setFilename(final String filename) {
		this.filename = filename;
	}

	public void setFileSize(final String fileSize) {
		this.fileSize = fileSize;
	}

	public Long getForumKey() {
		return forumKey;
	}

	public void setForumKey(final Long forumKey) {
		this.forumKey = forumKey;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(final Date date) {
		this.date = date;
	}

	public Integer getMessagesCount() {
		return messagesCount;
	}

	protected void setMessagesCount(final Integer messagesCount) {
		this.messagesCount = messagesCount;
	}

	public Integer getNewMessages() {
		return newMessages;
	}

	protected void setNewMessages(final Integer newMessages) {
		this.newMessages = newMessages;
	}

	public void setAuthorIdentityId(final String identityId) {
		this.author = identityId;
	}

}
