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
import java.util.List;

import org.apache.lucene.document.Document;
import org.olat.commons.info.manager.InfoMessageManager;
import org.olat.commons.info.model.InfoMessage;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.ICourse;
import org.olat.course.nodes.CourseNode;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.document.InfoMessageDocument;
import org.olat.search.service.indexer.AbstractIndexer;
import org.olat.search.service.indexer.OlatFullIndexer;
import org.olat.search.service.indexer.repository.CourseIndexer;

/**
 * Description:<br>
 * <P>
 * Initial Date: 29 juil. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoCourseNodeIndexer extends AbstractIndexer implements CourseNodeIndexer {
	private static final OLog log = Tracing.createLoggerFor(InfoCourseNodeIndexer.class);
	// Must correspond with LocalString_xx.properties
	// Do not use '_' because we want to seach for certain documenttype and lucene haev problems with '_'
	public final static String TYPE = "type.course.node.info.message";

	private final static String SUPPORTED_TYPE_NAME = "org.olat.course.nodes.InfoCourseNode";

	private final CourseIndexer courseNodeIndexer;
	private InfoMessageManager infoMessageManager;

	public InfoCourseNodeIndexer() {
		courseNodeIndexer = new CourseIndexer();
	}

	/**
	 * [used by Spring]
	 * 
	 * @param infoMessageManager
	 */
	public void setInfoMessageManager(final InfoMessageManager infoMessageManager) {
		this.infoMessageManager = infoMessageManager;
	}

	@Override
	public void doIndex(final SearchResourceContext repositoryResourceContext, final ICourse course, final CourseNode courseNode, final OlatFullIndexer indexWriter) {
		try {
			final SearchResourceContext courseNodeResourceContext = new SearchResourceContext(repositoryResourceContext);
			courseNodeResourceContext.setBusinessControlFor(courseNode);
			courseNodeResourceContext.setTitle(courseNode.getShortTitle());
			courseNodeResourceContext.setDescription(courseNode.getLongTitle());
			doIndexInfos(courseNodeResourceContext, course, courseNode, indexWriter);
			// go further, index my child nodes
			courseNodeIndexer.doIndexCourse(repositoryResourceContext, course, courseNode, indexWriter);
		} catch (final Exception ex) {
			log.error("Exception indexing courseNode=" + courseNode, ex);
		} catch (final Error err) {
			log.error("Error indexing courseNode=" + courseNode, err);
		}
	}

	@Override
	public String getSupportedTypeName() {
		return SUPPORTED_TYPE_NAME;
	}

	@Override
	public boolean checkAccess(final ContextEntry contextEntry, final BusinessControl businessControl, final Identity identity, final Roles roles) {
		return true;
	}

	private void doIndexInfos(final SearchResourceContext parentResourceContext, final ICourse course, final CourseNode courseNode, final OlatFullIndexer indexWriter)
			throws IOException, InterruptedException {
		final List<InfoMessage> messages = infoMessageManager.loadInfoMessageByResource(course, courseNode.getIdent(), null, null, null, 0, -1);
		for (final InfoMessage message : messages) {
			final SearchResourceContext searchResourceContext = new SearchResourceContext(parentResourceContext);
			final OLATResourceable ores = OresHelper.createOLATResourceableInstance(InfoMessage.class, message.getKey());
			searchResourceContext.setBusinessControlFor(ores);
			searchResourceContext.setDocumentContext(parentResourceContext.getDocumentContext() + " " + message.getKey());
			final Document document = InfoMessageDocument.createDocument(searchResourceContext, message);
			indexWriter.addDocument(document);
		}
	}
}
