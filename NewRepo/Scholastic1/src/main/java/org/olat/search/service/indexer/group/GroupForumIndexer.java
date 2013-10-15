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

package org.olat.search.service.indexer.group;

import java.io.IOException;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.collaboration.CollaborationTools;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.group.BusinessGroup;
import org.olat.group.ui.run.BusinessGroupMainRunController;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.Message;
import org.olat.modules.fo.Status;
import org.olat.properties.NarrowedPropertyManager;
import org.olat.properties.Property;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.document.GroupDocument;
import org.olat.search.service.indexer.ForumIndexer;
import org.olat.search.service.indexer.OlatFullIndexer;

/**
 * Index all group forums.
 * 
 * @author Christian Guretzki
 */
public class GroupForumIndexer extends ForumIndexer {

	// Must correspond with LocalString_xx.properties
	// Do not use '_' because we want to seach for certain documenttype and lucene haev problems with '_'
	public static final String TYPE = "type.group.forum.message";

	public GroupForumIndexer() {
		super();
	}

	/**
	 * Index a forum in a group.
	 * 
	 * @param parentResourceContext
	 * @param businessGroup
	 * @param indexWriter
	 * @throws IOException
	 */
	@Override
	public void doIndex(final SearchResourceContext parentResourceContext, final Object businessObj, final OlatFullIndexer indexWriter) throws IOException,
			InterruptedException {
		if (!(businessObj instanceof BusinessGroup)) { throw new AssertException("businessObj must be BusinessGroup"); }
		final BusinessGroup businessGroup = (BusinessGroup) businessObj;

		final NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(businessGroup);
		final ForumManager fom = ForumManager.getInstance();

		final Property forumKeyProperty = npm.findProperty(null, null, CollaborationTools.PROP_CAT_BG_COLLABTOOLS, CollaborationTools.KEY_FORUM);
		// Check if forum-property exist
		if (forumKeyProperty != null) {
			final Long forumKey = forumKeyProperty.getLongValue();
			final Forum forum = fom.loadForum(forumKey);
			final SearchResourceContext forumSearchResourceContext = new SearchResourceContext(parentResourceContext);
			forumSearchResourceContext.setBusinessControlFor(BusinessGroupMainRunController.ORES_TOOLFORUM);
			forumSearchResourceContext.setDocumentType(TYPE);
			forumSearchResourceContext.setDocumentContext(businessGroup.getKey() + " " + forumKey);
			forumSearchResourceContext.setParentContextType(GroupDocument.TYPE);
			forumSearchResourceContext.setParentContextName(businessGroup.getName());
			doIndexAllMessages(forumSearchResourceContext, forum, indexWriter);
		}
	}

	/**
	 * @see org.olat.search.service.indexer.Indexer#checkAccess(org.olat.core.id.context.ContextEntry, org.olat.core.id.context.BusinessControl,
	 *      org.olat.core.id.Identity, org.olat.core.id.Roles)
	 */
	@Override
	public boolean checkAccess(final ContextEntry contextEntry, final BusinessControl businessControl, final Identity identity, final Roles roles) {
		// TODO:chg: check with collabTools if forum is enabled
		final ContextEntry ce = businessControl.popLauncherContextEntry();
		final Long resourceableId = ce.getOLATResourceable().getResourceableId();
		final Message message = ForumManager.getInstance().loadMessage(resourceableId);
		Message threadtop = message.getThreadtop();
		if (threadtop == null) {
			threadtop = message;
		}
		final boolean isMessageHidden = Status.getStatus(threadtop.getStatusCode()).isHidden();
		// assumes that if is owner then is moderator so it is allowed to see the hidden forum threads
		// TODO: (LD) fix this!!!
		// here it is checked if the identity is owner of the forum tool but it has no way to find out whether is owner of the group that owns the forum tool
		final boolean isOwner = BaseSecurityManager.getInstance().isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_ACCESS,
				contextEntry.getOLATResourceable());
		if (isMessageHidden && !isOwner) { return false; }
		return true;
	}

	@Override
	public String getSupportedTypeName() {
		return BusinessGroupMainRunController.ORES_TOOLFORUM.getResourceableTypeName();
	}

}
