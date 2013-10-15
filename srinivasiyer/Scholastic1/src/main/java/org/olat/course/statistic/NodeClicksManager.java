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
package org.olat.course.statistic;

/**
 * Description:<br>
 * TODO: patrickb Class Description for NodeClicksManager
 * <P>
 * Initial Date: 23.12.2009 <br>
 * 
 * @author patrickb
 */
public class NodeClicksManager implements StatisticManager {

	private String initSqlFile = null;

	/**
	 * instance should be injected by Spring only via StatisticActionExtension
	 */
	protected NodeClicksManager() {
		//
	}

	/**
	 * @see org.olat.course.statistic.StatisticManager#getInitSqlFile()
	 */
	@Override
	public String getInitSqlFile() {
		return initSqlFile;
	}

	/**
	 * @see org.olat.course.statistic.StatisticManager#setInitSqlFile(java.lang.String)
	 */
	@Override
	public void setInitSqlFile(final String initSqlFile) {
		this.initSqlFile = initSqlFile;
	}

}
