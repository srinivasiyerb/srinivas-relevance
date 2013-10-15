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
package org.olat.modules.webFeed.search.indexer;

import java.io.IOException;

import org.olat.core.commons.services.search.OlatDocument;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.filter.FilterFactory;
import org.olat.modules.webFeed.dispatching.Path;
import org.olat.modules.webFeed.managers.FeedManager;
import org.olat.modules.webFeed.models.Feed;
import org.olat.modules.webFeed.models.Item;
import org.olat.modules.webFeed.search.document.FeedItemDocument;
import org.olat.repository.RepositoryEntry;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.indexer.Indexer;
import org.olat.search.service.indexer.OlatFullIndexer;

/**
 * The feed repository entry indexer
 * <P>
 * Initial Date: Aug 18, 2009 <br>
 * 
 * @author gwassmann
 */
public abstract class FeedRepositoryIndexer implements Indexer {

	private static final OLog log = Tracing.createLoggerFor(FeedRepositoryIndexer.class);

	/**
	 * @see org.olat.search.service.indexer.Indexer#checkAccess(org.olat.core.id.context.ContextEntry, org.olat.core.id.context.BusinessControl,
	 *      org.olat.core.id.Identity, org.olat.core.id.Roles)
	 */
	@Override
	public boolean checkAccess(final ContextEntry contextEntry, final BusinessControl businessControl, final Identity identity, final Roles roles) {
		return true;
	}

	/**
	 * @see org.olat.search.service.indexer.Indexer#doIndex(org.olat.search.service.SearchResourceContext, java.lang.Object,
	 *      org.olat.search.service.indexer.OlatFullIndexer)
	 */
	@Override
	public void doIndex(final SearchResourceContext searchResourceContext, final Object parentObject, final OlatFullIndexer indexer) throws IOException,
			InterruptedException {
		final RepositoryEntry repositoryEntry = (RepositoryEntry) parentObject;
		// used for log messages
		String repoEntryName = "*name not available*";
		try {
			repoEntryName = repositoryEntry.getDisplayname();
			if (log.isDebug()) {
				log.info("Indexing: " + repoEntryName);
			}
			final Feed feed = FeedManager.getInstance().getFeed(repositoryEntry.getOlatResource());

			// Set the document type, e.g. type.repository.entry.FileResource.BLOG
			searchResourceContext.setDocumentType(getDocumentType());
			searchResourceContext.setParentContextType(getDocumentType());
			searchResourceContext.setParentContextName(repoEntryName);

			// Make sure images are displayed properly
			// TODO:GW It's only working for public resources, because base url is
			// personal. -> fix
			final String mapperBaseURL = Path.getFeedBaseUri(feed, null, null, null);
			final Filter mediaUrlFilter = FilterFactory.getBaseURLToMediaRelativeURLFilter(mapperBaseURL);

			// Only index items. Feed itself is indexed by RepositoryEntryIndexer.
			log.info("PublishedItems size=" + feed.getPublishedItems().size());
			for (final Item item : feed.getPublishedItems()) {
				final OlatDocument itemDoc = new FeedItemDocument(item, searchResourceContext, mediaUrlFilter);
				indexer.addDocument(itemDoc.getLuceneDocument());
			}
		} catch (final NullPointerException e) {
			log.error("Error indexing feed:" + repoEntryName, e);
		}

	}

	/**
	 * @see org.olat.search.service.indexer.Indexer#getSupportedTypeName()
	 */
	@Override
	public abstract String getSupportedTypeName();

	/**
	 * @return The I18n key representing the document type
	 */
	protected abstract String getDocumentType();
}
