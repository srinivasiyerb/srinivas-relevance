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

package org.olat.search.service.indexer;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.SearchServiceFactory;
import org.olat.search.service.document.file.DocumentAccessException;
import org.olat.search.service.document.file.DocumentException;
import org.olat.search.service.document.file.DocumentNotImplementedException;
import org.olat.search.service.document.file.FileDocumentFactory;

/**
 * Common folder indexer. Index all files form a certain VFS-container as starting point.
 * 
 * @author Christian Guretzki
 */
public abstract class FolderIndexer extends AbstractIndexer {

	protected OLog log = Tracing.createLoggerFor(FolderIndexer.class);

	protected FolderIndexer() {}

	protected void doIndexVFSContainer(final SearchResourceContext parentResourceContext, final VFSContainer container, final OlatFullIndexer indexWriter,
			final String filePath, final FolderIndexerAccess accessRule) throws IOException, InterruptedException {
		if (FolderIndexerWorkerPool.getInstance().isDisabled()) {
			// Do index in single thread mode
			doIndexVFSContainerByMySelf(parentResourceContext, container, indexWriter, filePath, accessRule);
		} else {
			// Start new thread to index folder
			final FolderIndexerWorker runnableFolderIndexer = FolderIndexerWorkerPool.getInstance().getIndexer();
			runnableFolderIndexer.setAccessRule(accessRule);
			runnableFolderIndexer.setParentResourceContext(parentResourceContext);
			runnableFolderIndexer.setContainer(container);
			runnableFolderIndexer.setIndexWriter(indexWriter);
			runnableFolderIndexer.setFilePath(filePath);
			// Start Indexing from this rootContainer in an own thread
			runnableFolderIndexer.start();
		}
	}

	// This index methods will be used in single-thread mode only (FolderIndexerWorkerPool is disabled)
	// ////////////////////////////////////////////////////////////////////////////////////////////////
	private void doIndexVFSContainerByMySelf(final SearchResourceContext parentResourceContext, final VFSContainer container, final OlatFullIndexer indexWriter,
			final String filePath, final FolderIndexerAccess accessRule) throws IOException, InterruptedException {
		// Items: List of VFSContainer & VFSLeaf
		final String myFilePath = filePath;
		for (final VFSItem item : container.getItems()) {
			if (item instanceof VFSContainer) {
				// ok it is a container go further
				if (log.isDebug()) {
					log.debug(item.getName() + " is a VFSContainer => go further ");
				}
				if (accessRule.allowed(item)) {
					doIndexVFSContainerByMySelf(parentResourceContext, (VFSContainer) item, indexWriter, myFilePath + "/" + ((VFSContainer) item).getName(), accessRule);
				}
			} else if (item instanceof VFSLeaf) {
				// ok it is a file => analyse it
				if (log.isDebug()) {
					log.debug(item.getName() + " is a VFSLeaf => analyse file");
				}
				if (accessRule.allowed(item)) {
					doIndexVFSLeafByMySelf(parentResourceContext, (VFSLeaf) item, indexWriter, myFilePath);
				}
			} else {
				log.warn("Unkown element in item-list class=" + item.getClass());
			}
			// TODO:cg/27.10.2010 try to fix Indexer ERROR 'Overdue resource check-out stack trace.' on OLATNG
			DBFactory.getInstance().commitAndCloseSession();
		}
	}

	protected void doIndexVFSLeafByMySelf(final SearchResourceContext leafResourceContext, final VFSLeaf leaf, final OlatFullIndexer indexWriter, final String filePath)
			throws InterruptedException {
		if (log.isDebug()) {
			log.debug("Analyse VFSLeaf=" + leaf.getName());
		}
		try {
			if (SearchServiceFactory.getFileDocumentFactory().isFileSupported(leaf)) {
				String myFilePath = "";
				if (filePath.endsWith("/")) {
					myFilePath = filePath + leaf.getName();
				} else {
					myFilePath = filePath + "/" + leaf.getName();
				}
				leafResourceContext.setFilePath(myFilePath);
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
				log.debug("Documenttype not implemented." + e.getMessage());
			}
		} catch (final DocumentException dex) {
			if (log.isDebug()) {
				log.debug("DocumentException: Can not index leaf=" + leaf.getName() + " exception=" + dex.getMessage());
			}
		} catch (final IOException ioEx) {
			log.warn("IOException: Can not index leaf=" + leaf.getName(), ioEx);
		} catch (final InterruptedException iex) {
			throw new InterruptedException(iex.getMessage());
		} catch (final Exception ex) {
			log.warn("Exception: Can not index leaf=" + leaf.getName(), ex);
		}
	}

	/**
	 * @param leaf
	 * @return Full file-path of leaf without leaf-name
	 */
	protected String getPathFor(final VFSLeaf leaf) {
		String path = "";
		VFSContainer parentContainer = leaf.getParentContainer();
		while (parentContainer.getParentContainer() != null) {
			path = parentContainer.getName() + "/" + path;
			parentContainer = parentContainer.getParentContainer();
		}
		return path;
	}

}
