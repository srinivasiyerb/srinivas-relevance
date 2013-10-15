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

import org.olat.core.commons.scheduler.JobWithDB;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.search.service.SearchServiceFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Description:<br>
 * job that starts search indexing process.
 * <P>
 * Initial Date: 09.09.2008 <br>
 * 
 * @author Christian Guretzki
 */
public class SearchIndexingJob extends JobWithDB {
	private final OLog log = Tracing.createLoggerFor(SearchIndexingJob.class);

	/**
	 * @see org.olat.core.commons.scheduler.JobWithDB#executeWithDB(org.quartz.JobExecutionContext)
	 */
	@Override
	public void executeWithDB(final JobExecutionContext arg0) throws JobExecutionException {
		log.info("Search indexer started via cronjob.");
		SearchServiceFactory.getService().startIndexing();
	}

}
