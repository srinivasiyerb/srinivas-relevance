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

package org.olat.search.service.indexer.repository;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.ImsCPFileResource;
import org.olat.repository.RepositoryEntry;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.indexer.FolderIndexer;
import org.olat.search.service.indexer.FolderIndexerAccess;
import org.olat.search.service.indexer.Indexer;
import org.olat.search.service.indexer.OlatFullIndexer;

/**
 * Index a repository entry of type IMS-CP.
 * 
 * @author Christian Guretzki
 */
public class ImsCPRepositoryIndexer extends FolderIndexer implements Indexer {
	private static final OLog log = Tracing.createLoggerFor(ImsCPRepositoryIndexer.class);

	// Must correspond with LocalString_xx.properties
	// Do not use '_' because we want to seach for certain documenttype and lucene haev problems with '_'
	public final static String TYPE = "type.repository.entry.imscp";

	public final static String ORES_TYPE_CP = ImsCPFileResource.TYPE_NAME;

	public ImsCPRepositoryIndexer() {
		// Repository types

	}

	/**
	 * 
	 */
	@Override
	public String getSupportedTypeName() {
		return ORES_TYPE_CP;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsDownload()
	 */

	@Override
	public void doIndex(final SearchResourceContext resourceContext, final Object parentObject, final OlatFullIndexer indexWriter) throws IOException,
			InterruptedException {
		final RepositoryEntry repositoryEntry = (RepositoryEntry) parentObject;
		if (log.isDebug()) {
			log.debug("Analyse IMS CP RepositoryEntry...");
		}

		resourceContext.setDocumentType(TYPE);

		if (repositoryEntry == null) { throw new AssertException("no Repository"); }
		final File cpRoot = FileResourceManager.getInstance().unzipFileResource(repositoryEntry.getOlatResource());
		if (cpRoot == null) { throw new AssertException("file of repository entry " + repositoryEntry.getKey() + "was missing"); }

		resourceContext.setParentContextType(TYPE);
		resourceContext.setParentContextName(repositoryEntry.getDisplayname());
		final VFSContainer rootContainer = new LocalFolderImpl(cpRoot);
		doIndexVFSContainer(resourceContext, rootContainer, indexWriter, "", FolderIndexerAccess.FULL_ACCESS);

	}

	/**
	 * Bean setter method used by spring.
	 * 
	 * @param indexerList
	 */
	@Override
	public void setIndexerList(final List indexerList) {}

	@Override
	public boolean checkAccess(final ContextEntry contextEntry, final BusinessControl businessControl, final Identity identity, final Roles roles) {
		return true;
	}

}
