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

package org.olat.search.service;

import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.olat.core.commons.services.search.QueryException;
import org.olat.core.commons.services.search.SearchModule;
import org.olat.core.commons.services.search.SearchResults;
import org.olat.core.commons.services.search.SearchService;
import org.olat.core.commons.services.search.SearchServiceStatus;
import org.olat.core.commons.services.search.ServiceNotAvailableException;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;

/**
 * @author Christian Guretzki
 */
public class SearchServiceDisabled implements SearchService {
	private static final OLog log = Tracing.createLoggerFor(SearchServiceDisabled.class);

	/**
	 * [used by spring]
	 */
	private SearchServiceDisabled() {
		log.info("SearchService Disabled");
	}

	@Override
	public void addToIndex(final Document document) {}

	@Override
	public void startIndexing() {}

	@Override
	public void stopIndexing() {}

	@Override
	public void deleteFromIndex(final Document document) {}

	@Override
	public void init() {}

	@Override
	public SearchServiceStatus getStatus() {
		return null;
	}

	@Override
	public void setIndexInterval(final long indexInterval) {}

	@Override
	public long getIndexInterval() {
		return 0;
	}

	/**
	 * @return Resturn search module configuration.
	 */
	@Override
	public SearchModule getSearchModuleConfig() {
		return null;
	}

	/**
	 * @see org.olat.search.service.SearchService#spellCheck(java.lang.String)
	 */
	@Override
	public Set<String> spellCheck(final String query) throws ServiceNotAvailableException {
		log.error("call spellCheck on disabled search service");
		throw new ServiceNotAvailableException("call spellCheck on disabled search service");
	}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		return false;
	}

	@Override
	public long getQueryCount() {
		return 0;
	}

	@Override
	public SearchResults doSearch(final String queryString, final List<String> condQueries, final Identity identity, final Roles roles, final int firstResult,
			final int maxResults, final boolean doHighlighting) throws ServiceNotAvailableException, ParseException, QueryException {
		log.error("call doSearch on disabled search service");
		throw new ServiceNotAvailableException("call doSearch on disabled search service");
	}

}
