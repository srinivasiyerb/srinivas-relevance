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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;

/**
 * Used in multi-threaded mode. Reads lucene documents from document-queue in main-indexer and add this documents to index-writer. The index-writer works with his own
 * 'part-[ID]' index-directory. Initial Date: 10.10.2006 <br>
 * 
 * @author guretzki
 */
public class IndexWriterWorker implements Runnable {

	private static OLog log = Tracing.createLoggerFor(IndexWriterWorker.class);

	private Thread indexerWriterThread;
	private final int id;
	private final OlatFullIndexer fullIndexer;
	private IndexWriter indexWriter;
	private boolean running = true;
	private boolean finish = false;
	private boolean closed = false;

	/**
	 * @param id Unique index ID. Is used to generate unique directory name.
	 * @param tempIndexPath Absolute directory-path where the temporary index can be generated.
	 * @param fullIndexer Reference to full-index
	 */
	public IndexWriterWorker(final int id, final File tempIndexDir, final OlatFullIndexer fullIndexer) {
		this.id = id;
		this.fullIndexer = fullIndexer;
		try {
			final File indexPartFile = new File(tempIndexDir, "part" + id);
			final Directory indexPartDirectory = FSDirectory.open(indexPartFile);
			indexWriter = new IndexWriter(indexPartDirectory, new StandardAnalyzer(Version.LUCENE_CURRENT), true, IndexWriter.MaxFieldLength.UNLIMITED);
			indexWriter.deleteAll();
		} catch (final IOException e) {
			log.warn("Can not create IndexWriter");
		}
	}

	/**
	 * Create and start a new index-writer thread.
	 */
	public void start() {
		indexerWriterThread = new Thread(this, "indexWriter-" + id);
		indexerWriterThread.setPriority(Thread.MIN_PRIORITY);
		indexerWriterThread.setDaemon(true);
		indexerWriterThread.start();
	}

	public boolean isClosed() {
		return closed;
	}

	/**
	 * Set finish flag. The index-writer will be closed when the document-queue is empty.
	 */
	public void finishIndexing() {
		finish = true;
	}

	/**
	 * Check if document-queue of main-indexer has elements. Get document from queue and add it to index-writer.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		final List<Document> documentQueue = fullIndexer.getDocumentQueue();
		while (running) {
			try {
				while (!finish && documentQueue.isEmpty()) {
					// nothing to do => sleep
					Thread.sleep(200);
				}
				Document document = null;
				synchronized (documentQueue) { // o_clusterOK by:fj, when only one indexwriter runs in the whole cluster
					if (!documentQueue.isEmpty()) {
						document = documentQueue.remove(0);
					}
				}
				if (document != null) {
					indexWriter.addDocument(document);
					if (log.isDebug()) {
						log.debug("documentQueue.remove size=" + documentQueue.size());
						log.debug("IndexWriter docCount=" + indexWriter.maxDoc());
					}
				}
			} catch (final InterruptedException ex) {
				log.warn("InterruptedException in run");
			} catch (final Exception ex) {
				log.warn("Exception in run", ex);
			}
			if (finish && documentQueue.isEmpty()) {
				running = false;
			}
			if (fullIndexer.isInterupted()) {
				running = false;
			}
		}
		try {
			indexWriter.close();
			closed = true;
			if (log.isDebug()) {
				log.debug("IndexWriter " + id + " closed");
			}
		} catch (final IOException e) {
			log.warn("Can not close IndexWriter", e);
		}
		log.info("IndexWriter " + id + " end of run.");
	}

	/**
	 * @return Lucene Directory object of index-writer.
	 */
	public Directory getIndexDir() {
		return indexWriter.getDirectory();
	}

	/**
	 * @return Return number of added documents.
	 */
	public int getDocCount() {
		return indexWriter.maxDoc();
	}

}
