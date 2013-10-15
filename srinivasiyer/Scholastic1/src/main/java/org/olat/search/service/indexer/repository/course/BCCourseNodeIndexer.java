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

import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.Tracing;
import org.olat.course.ICourse;
import org.olat.course.nodes.BCCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.indexer.FolderIndexer;
import org.olat.search.service.indexer.FolderIndexerAccess;
import org.olat.search.service.indexer.OlatFullIndexer;
import org.olat.search.service.indexer.repository.CourseIndexer;

/**
 * Indexer for (BC) brief case course-node.
 * 
 * @author Christian Guretzki
 */
public class BCCourseNodeIndexer extends FolderIndexer implements CourseNodeIndexer {

	// Must correspond with LocalString_xx.properties
	// Do not use '_' because we want to seach for certain documenttype and lucene haev problems with '_'
	public static final String TYPE = "type.course.node.bc";

	private final static String SUPPORTED_TYPE_NAME = "org.olat.course.nodes.BCCourseNode";

	private final CourseIndexer courseNodeIndexer;

	public BCCourseNodeIndexer() {
		courseNodeIndexer = new CourseIndexer();
	}

	@Override
	public void doIndex(final SearchResourceContext repositoryResourceContext, final ICourse course, final CourseNode courseNode, final OlatFullIndexer indexWriter)
			throws IOException, InterruptedException {
		if (Tracing.isDebugEnabled(BCCourseNodeIndexer.class)) {
			Tracing.logDebug("Index Briefcase...", BCCourseNodeIndexer.class);
		}

		final SearchResourceContext courseNodeResourceContext = new SearchResourceContext(repositoryResourceContext);
		courseNodeResourceContext.setBusinessControlFor(courseNode);
		courseNodeResourceContext.setDocumentType(TYPE);
		courseNodeResourceContext.setTitle(courseNode.getShortTitle());
		courseNodeResourceContext.setDescription(courseNode.getLongTitle());

		final OlatNamedContainerImpl namedContainer = BCCourseNode.getNodeFolderContainer((BCCourseNode) courseNode, course.getCourseEnvironment());
		doIndexVFSContainer(courseNodeResourceContext, namedContainer, indexWriter, "", FolderIndexerAccess.FULL_ACCESS);
		// go further, index my child nodes
		courseNodeIndexer.doIndexCourse(repositoryResourceContext, course, courseNode, indexWriter);
	}

	@Override
	public String getSupportedTypeName() {
		return SUPPORTED_TYPE_NAME;
	}

	@Override
	public boolean checkAccess(final ContextEntry contextEntry, final BusinessControl businessControl, final Identity identity, final Roles roles) {
		return true;
	}

}
