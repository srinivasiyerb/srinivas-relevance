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
package de.bps.jobs;

import org.olat.core.logging.Tracing;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import de.bps.onyx.plugin.OnyxResultManager;

/**
 * Description:<br>
 * This class calls the updateOnyxResults in a nightliy job.
 * <P>
 * Initial Date: 20.01.2010 <br>
 * 
 * @author thomasw
 */
public class UpdateOnyxResults extends QuartzJobBean {

	/**
	 * @see org.springframework.scheduling.quartz.QuartzJobBean#executeInternal(org.quartz.JobExecutionContext)
	 */
	@Override
	protected void executeInternal(final JobExecutionContext arg0) throws JobExecutionException {
		try {
			OnyxResultManager.updateOnyxResults();
			Tracing.createLoggerFor(this.getClass()).info("updated onyx results");
		} catch (final Exception e) {
			Tracing.createLoggerFor(this.getClass()).error("error in nightly update of onyx results", e);
			throw new JobExecutionException(e);
		}
	}
}
