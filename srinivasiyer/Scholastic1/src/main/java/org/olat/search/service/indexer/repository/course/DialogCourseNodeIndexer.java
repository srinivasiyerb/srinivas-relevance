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

package org.olat.search.service.indexer.repository.course;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Document;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.filters.VFSLeafFilter;
import org.olat.course.ICourse;
import org.olat.course.nodes.CourseNode;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.modules.dialog.DialogElement;
import org.olat.modules.dialog.DialogElementsController;
import org.olat.modules.dialog.DialogElementsPropertyManager;
import org.olat.modules.dialog.DialogPropertyElements;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.Message;
import org.olat.modules.fo.Status;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.SearchServiceFactory;
import org.olat.search.service.document.ForumMessageDocument;
import org.olat.search.service.document.file.DocumentAccessException;
import org.olat.search.service.document.file.DocumentException;
import org.olat.search.service.document.file.DocumentNotImplementedException;
import org.olat.search.service.document.file.FileDocumentFactory;
import org.olat.search.service.indexer.OlatFullIndexer;
import org.olat.search.service.indexer.repository.CourseIndexer;

/**
 * Indexer for dialog course-node.
 * 
 * @author Christian Guretzki
 */
public class DialogCourseNodeIndexer implements CourseNodeIndexer {
	private static final OLog log = Tracing.createLoggerFor(DialogCourseNodeIndexer.class);
	// Must correspond with LocalString_xx.properties
	// Do not use '_' because we want to seach for certain documenttype and lucene haev problems with '_'
	public final static String TYPE_MESSAGE = "type.course.node.dialog.forum.message";
	public final static String TYPE_FILE = "type.course.node.dialog.file";

	private final static String SUPPORTED_TYPE_NAME = "org.olat.course.nodes.DialogCourseNode";

	private final ForumManager forumMgr;

	private final DialogElementsPropertyManager dialogElmsMgr;

	private final CourseIndexer courseNodeIndexer;

	public DialogCourseNodeIndexer() {
		forumMgr = ForumManager.getInstance();
		dialogElmsMgr = DialogElementsPropertyManager.getInstance();
		courseNodeIndexer = new CourseIndexer();
	}

	@Override
	public void doIndex(final SearchResourceContext repositoryResourceContext, final ICourse course, final CourseNode courseNode, final OlatFullIndexer indexWriter)
			throws IOException, InterruptedException {
		final SearchResourceContext courseNodeResourceContext = new SearchResourceContext(repositoryResourceContext);
		courseNodeResourceContext.setBusinessControlFor(courseNode);
		courseNodeResourceContext.setTitle(courseNode.getShortTitle());
		courseNodeResourceContext.setDescription(courseNode.getLongTitle());

		final CoursePropertyManager coursePropMgr = course.getCourseEnvironment().getCoursePropertyManager();
		final DialogPropertyElements elements = dialogElmsMgr.findDialogElements(coursePropMgr, courseNode);
		List<DialogElement> list = new ArrayList<DialogElement>();
		if (elements != null) {
			list = elements.getDialogPropertyElements();
		}
		// loop over all dialog elements
		for (final Iterator<DialogElement> iter = list.iterator(); iter.hasNext();) {
			final DialogElement element = iter.next();
			element.getAuthor();
			element.getDate();
			final Forum forum = forumMgr.loadForum(element.getForumKey());
			// do IndexForum
			doIndexAllMessages(courseNodeResourceContext, forum, indexWriter);
			// do Index File
			doIndexFile(element.getFilename(), element.getForumKey(), courseNodeResourceContext, indexWriter);
		}

		// go further, index my child nodes
		courseNodeIndexer.doIndexCourse(repositoryResourceContext, course, courseNode, indexWriter);
	}

	/**
	 * Index a file of dialog-module.
	 * 
	 * @param filename
	 * @param forumKey
	 * @param leafResourceContext
	 * @param indexWriter
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void doIndexFile(final String filename, final Long forumKey, final SearchResourceContext leafResourceContext, final OlatFullIndexer indexWriter)
			throws IOException, InterruptedException {
		final OlatRootFolderImpl forumContainer = DialogElementsController.getForumContainer(forumKey);
		final VFSLeaf leaf = (VFSLeaf) forumContainer.getItems(new VFSLeafFilter()).get(0);
		if (log.isDebug()) {
			log.debug("Analyse VFSLeaf=" + leaf.getName());
		}
		try {
			if (SearchServiceFactory.getFileDocumentFactory().isFileSupported(leaf)) {
				leafResourceContext.setFilePath(filename);
				leafResourceContext.setDocumentType(TYPE_FILE);
				final Document document = FileDocumentFactory.createDocument(leafResourceContext, leaf);
				indexWriter.addDocument(document);
			} else {
				if (log.isDebug()) {
					log.debug("Documenttype not supported. file=" + leaf.getName());
				}
			}
		} catch (final DocumentAccessException e) {
			if (log.isDebug()) {
				log.debug("Can not access document." + e.getMessage());
			}
		} catch (final DocumentNotImplementedException e) {
			if (log.isDebug()) {
				log.debug("Documenttype not implemented.");
			}
		} catch (final DocumentException dex) {
			if (log.isDebug()) {
				log.debug("DocumentException: Can not index leaf=" + leaf.getName());
			}
		} catch (final IOException ioEx) {
			log.warn("IOException: Can not index leaf=" + leaf.getName(), ioEx);
		} catch (final InterruptedException iex) {
			throw new InterruptedException(iex.getMessage());
		} catch (final Exception ex) {
			log.warn("Exception: Can not index leaf=" + leaf.getName(), ex);
		}
	}

	private void doIndexAllMessages(final SearchResourceContext parentResourceContext, final Forum forum, final OlatFullIndexer indexWriter) throws IOException,
			InterruptedException {
		// loop over all messages of a forum
		final List<Message> messages = forumMgr.getMessagesByForum(forum);
		for (final Message message : messages) {
			final SearchResourceContext searchResourceContext = new SearchResourceContext(parentResourceContext);
			searchResourceContext.setBusinessControlFor(message);
			searchResourceContext.setDocumentType(TYPE_MESSAGE);
			searchResourceContext.setDocumentContext(parentResourceContext.getDocumentContext() + " " + forum.getKey());
			final Document document = ForumMessageDocument.createDocument(searchResourceContext, message);
			indexWriter.addDocument(document);
		}
	}

	@Override
	public String getSupportedTypeName() {
		return SUPPORTED_TYPE_NAME;
	}

	@Override
	public boolean checkAccess(final ContextEntry contextEntry, final BusinessControl businessControl, final Identity identity, final Roles roles) {
		final ContextEntry ce = businessControl.popLauncherContextEntry();
		final OLATResourceable ores = ce.getOLATResourceable();
		if (log.isDebug()) {
			log.debug("OLATResourceable=" + ores);
		}
		if ((ores != null) && (ores.getResourceableTypeName().startsWith("path="))) {
			// => it is a file element, typeName format: 'path=/test1/test2/readme.txt'
			return true;
		} else if ((ores != null) && ores.getResourceableTypeName().equals(OresHelper.calculateTypeName(Message.class))) {
			// it is message => check message access
			final Long resourceableId = ores.getResourceableId();
			final Message message = ForumManager.getInstance().loadMessage(resourceableId);
			Message threadtop = message.getThreadtop();
			if (threadtop == null) {
				threadtop = message;
			}
			final boolean isMessageHidden = Status.getStatus(threadtop.getStatusCode()).isHidden();
			// assumes that if is owner then is moderator so it is allowed to see the hidden forum threads
			// TODO: (LD) fix this!!! - the contextEntry is not the right context for this check
			final boolean isOwner = BaseSecurityManager.getInstance().isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_ACCESS,
					contextEntry.getOLATResourceable());
			if (isMessageHidden && !isOwner) { return false; }
			return true;
		} else {
			log.warn("In DialogCourseNode unkown OLATResourceable=" + ores);
			return false;
		}
	}

}
