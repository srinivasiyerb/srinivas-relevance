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
 * Copyright (c) 2009 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.modules.scorm.archiver;

/**
 * Description:<br>
 * Hold a single objective of the sco's datamodel
 * <P>
 * Initial Date: 17 august 2009 <br>
 * 
 * @author srosse
 */
public class ScoObjective {
	private String id;
	private String scoreMin;
	private String scoreMax;
	private String scoreRaw;
	private int position;

	public ScoObjective(final int position) {
		this.position = position;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(final int position) {
		this.position = position;
	}

	public String getScoreMin() {
		return scoreMin;
	}

	public void setScoreMin(final String scoreMin) {
		this.scoreMin = scoreMin;
	}

	public String getScoreMax() {
		return scoreMax;
	}

	public void setScoreMax(final String scoreMax) {
		this.scoreMax = scoreMax;
	}

	public String getScoreRaw() {
		return scoreRaw;
	}

	public void setScoreRaw(final String scoreRaw) {
		this.scoreRaw = scoreRaw;
	}
}
