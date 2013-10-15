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

package org.olat.modules.iq;

import org.olat.core.gui.control.Event;

/**
 * Initial Date: May 27, 2004
 * 
 * @author gnaegi
 */
public class IQSubmittedEvent extends Event {
	private float score = 0;
	private boolean passed = false;
	private long assessmentID;

	/**
	 * constructor for a finished survey event
	 */
	public IQSubmittedEvent() {
		super("iqfinished");
	}

	/**
	 * Constructor for a finished test or selftest event
	 * 
	 * @param score
	 * @param passed
	 */
	public IQSubmittedEvent(final float score, final boolean passed, final long assessmentID) {
		super("iqfinished");
		this.score = score;
		this.passed = passed;
		this.assessmentID = assessmentID;
	}

	/**
	 * @return Returns the passed.
	 */
	public boolean isPassed() {
		return passed;
	}

	/**
	 * @return Returns the score.
	 */
	public float getScore() {
		return score;
	}

	/**
	 * @return Returns the (last) assessmentID.
	 */
	public long getAssessmentID() {
		return assessmentID;
	}
}
