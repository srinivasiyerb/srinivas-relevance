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

package org.olat.search.service.document;

import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.olat.core.commons.services.search.OlatDocument;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.filter.FilterFactory;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.indexer.repository.course.ProjectBrokerCourseNodeIndexer;

/**
 * Lucene document mapper.
 * 
 * @author Christian Guretzki
 */
public class ProjectBrokerProjectDocument extends OlatDocument {
	private static final OLog log = Tracing.createLoggerFor(ProjectBrokerProjectDocument.class);

	// Must correspond with LocalString_xx.properties
	// Do not use '_' because we want to search for certain documenttype and lucene have problems with '_'

	public ProjectBrokerProjectDocument() {
		super();
	}

	public static Document createDocument(final SearchResourceContext searchResourceContext, final Project project) {
		final ProjectBrokerProjectDocument projectDocument = new ProjectBrokerProjectDocument();

		projectDocument.setTitle(project.getTitle());
		final String projectDescription = FilterFactory.getHtmlTagsFilter().filter(project.getDescription());
		projectDocument.setContent(projectDescription);
		final StringBuilder projectLeaderString = new StringBuilder();
		for (final Iterator<Identity> iterator = project.getProjectLeaders().iterator(); iterator.hasNext();) {
			projectLeaderString.append(iterator.next().getName());
			projectLeaderString.append(" ");
		}
		projectDocument.setAuthor(projectLeaderString.toString());
		projectDocument.setCreatedDate(project.getCreationDate());
		projectDocument.setResourceUrl(searchResourceContext.getResourceUrl());
		projectDocument.setDocumentType(ProjectBrokerCourseNodeIndexer.TYPE);
		projectDocument.setCssIcon("o_projectbroker_icon");
		projectDocument.setParentContextType(searchResourceContext.getParentContextType());
		projectDocument.setParentContextName(searchResourceContext.getParentContextName());

		if (log.isDebug()) {
			log.debug(projectDocument.toString());
		}
		return projectDocument.getLuceneDocument();
	}
}
