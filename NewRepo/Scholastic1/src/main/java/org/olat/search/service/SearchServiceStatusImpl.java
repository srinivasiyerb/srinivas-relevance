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

import java.util.Date;

import org.olat.core.commons.services.search.SearchService;
import org.olat.search.service.indexer.FullIndexerStatus;
import org.olat.search.service.indexer.Index;

/**
 * Collection of search-service states. Includes state of full-index, index and search.
 * 
 * @author Christian Guretzki
 */
public class SearchServiceStatusImpl implements org.olat.core.commons.services.search.SearchServiceStatus {

	FullIndexerStatus fullIndexerStatus;

	private final boolean indexExists;
	private final long queryCount;
	private final Date indexCreationDate;

	/**
	 * Creates a new search-service status. The values will be read in constructur. To get a status for an other moment, you must create a new SearchServiceStatus.
	 * 
	 * @param indexer Reference to index-component.
	 * @param search Reference to search-component.
	 */
	public SearchServiceStatusImpl(final Index indexer, final SearchService search) {
		fullIndexerStatus = indexer.getFullIndexStatus();
		indexExists = indexer.existIndex();
		indexCreationDate = indexer.getCreationDate();
		queryCount = search.getQueryCount();
	}

	@Override
	public String getStatus() {
		return fullIndexerStatus.getStatus();
	}

	/**
	 * @return Status of full-indexer.
	 */
	public FullIndexerStatus getFullIndexerStatus() {
		return fullIndexerStatus;
	}

	/**
	 * @return TRUE when an search index exists.
	 */
	public boolean getIndexExists() {
		return indexExists;
	}

	/**
	 * @return Date when search index is created.
	 */
	public Date getIndexCreationDate() {
		return indexCreationDate;
	}

	/**
	 * @return Number of handled search queries since last restart.
	 */
	public long getQueryCount() {
		return queryCount;
	}

}
