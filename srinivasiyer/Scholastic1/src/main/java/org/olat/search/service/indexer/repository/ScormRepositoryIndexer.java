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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.search.service.indexer.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.dom4j.Element;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.ScormCPFileResource;
import org.olat.ims.resources.IMSLoader;
import org.olat.repository.RepositoryEntry;
import org.olat.resource.OLATResource;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.document.file.IMSMetadataDocument;
import org.olat.search.service.indexer.FolderIndexer;
import org.olat.search.service.indexer.FolderIndexerAccess;
import org.olat.search.service.indexer.Indexer;
import org.olat.search.service.indexer.OlatFullIndexer;

/**
 * Description:<br>
 * Index the SCORM package
 * <P>
 * Initial Date: 11 déc. 2009 <br>
 * 
 * @author srosse
 */
public class ScormRepositoryIndexer extends FolderIndexer implements Indexer {
	private static final OLog log = Tracing.createLoggerFor(ScormRepositoryIndexer.class);
	public static Set<String> stopWords = new HashSet<String>();
	static {
		stopWords.add("LOMv1.0");
		stopWords.add("yes");
		stopWords.add("NA");
	}
	public static final List<String> forbiddenExtensions = new ArrayList<String>();
	static {
		forbiddenExtensions.add("LOMv1.0");
		forbiddenExtensions.add(".xsd");
		forbiddenExtensions.add(".js");
	}
	public static final Set<String> forbiddenFiles = new HashSet<String>();
	static {
		forbiddenFiles.add("imsmanifest.xml");
	}

	public final static String TYPE = "type.repository.entry.scorm";
	public final static String ORES_TYPE_SCORM = ScormCPFileResource.TYPE_NAME;

	public ScormRepositoryIndexer() {
		// Repository types
	}

	/**
	 * 
	 */
	@Override
	public String getSupportedTypeName() {
		return ORES_TYPE_SCORM;
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsDownload()
	 */
	@Override
	public void doIndex(final SearchResourceContext resourceContext, final Object parentObject, final OlatFullIndexer indexWriter) throws IOException,
			InterruptedException {
		if (log.isDebug()) {
			log.debug("Index Scorm package...");
		}

		final RepositoryEntry repositoryEntry = (RepositoryEntry) parentObject;
		final OLATResource ores = repositoryEntry.getOlatResource();
		final File cpRoot = FileResourceManager.getInstance().unzipFileResource(ores);

		resourceContext.setDocumentType(TYPE);
		resourceContext.setTitle(repositoryEntry.getDisplayname());
		resourceContext.setDescription(repositoryEntry.getDescription());
		resourceContext.setParentContextType(TYPE);
		resourceContext.setParentContextName(repositoryEntry.getDisplayname());
		doIndex(resourceContext, indexWriter, cpRoot);
	}

	protected void doIndex(final SearchResourceContext resourceContext, final OlatFullIndexer indexWriter, final File cpRoot) throws IOException, InterruptedException {
		final VFSContainer container = new LocalFolderImpl(cpRoot);
		final VFSLeaf fManifest = (VFSLeaf) container.resolve("imsmanifest.xml");
		if (fManifest != null) {
			final Element rootElement = IMSLoader.loadIMSDocument(fManifest).getRootElement();
			final Document manfiestDoc = createManifestDocument(fManifest, rootElement, resourceContext);
			indexWriter.addDocument(manfiestDoc);

			final ScormFileAccess accessRule = new ScormFileAccess();
			doIndexVFSContainer(resourceContext, container, indexWriter, "", accessRule);
		}
	}

	private Document createManifestDocument(final VFSLeaf fManifest, final Element rootElement, final SearchResourceContext resourceContext) {
		final IMSMetadataDocument document = new IMSMetadataDocument();
		document.setResourceUrl(resourceContext.getResourceUrl());
		if (log.isDebug()) {
			log.debug("MM: URL=" + document.getResourceUrl());
		}
		document.setLastChange(new Date(fManifest.getLastModified()));
		document.setDocumentType(resourceContext.getDocumentType());
		if (StringHelper.containsNonWhitespace(resourceContext.getTitle())) {
			document.setTitle(resourceContext.getTitle());
		} else {
			document.setTitle(fManifest.getName());
		}
		document.setParentContextType(resourceContext.getParentContextType());
		document.setParentContextName(resourceContext.getParentContextName());

		final StringBuilder sb = new StringBuilder();
		collectLangString(sb, rootElement);
		document.setContent(sb.toString());
		return document.getLuceneDocument();
	}

	private void collectLangString(final StringBuilder sb, final Element element) {
		if ("langstring".equals(element.getName())) {
			final String content = element.getText();
			if (!stopWords.contains(content)) {
				sb.append(content).append(' ');
			}
		}
		final List children = element.elements();
		for (int i = 0; i < children.size(); i++) {
			final Element child = (Element) children.get(i);
			collectLangString(sb, child);
		}
	}

	@Override
	public boolean checkAccess(final ContextEntry contextEntry, final BusinessControl businessControl, final Identity identity, final Roles roles) {
		return true;
	}

	public class ScormFileAccess implements FolderIndexerAccess {

		@Override
		public boolean allowed(final VFSItem item) {
			final String name = item.getName();
			if (forbiddenFiles.contains(name)) { return false; }

			for (final String forbiddenExtension : forbiddenExtensions) {
				if (name.endsWith(forbiddenExtension)) { return false; }
			}
			return true;
		}
	}
}
