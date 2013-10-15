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

package org.olat.course.condition.interpreter;

/**
 * Description:<br>
 * TODO: patrick Class Description for ConditionErrorMessage
 * <P>
 * Initial Date: Jul 8, 2005 <br>
 * 
 * @author patrick
 */
public class ConditionErrorMessage {
	// TODO:pb:a always! use getter and setters!
	public String errorKey = null;
	public String[] errorKeyParams = null;
	public String solutionMsgKey = null;

	public ConditionErrorMessage(final String errorKey, final String solutionMsg, final String[] errorKeyParams) {
		this.errorKey = errorKey;
		this.errorKeyParams = errorKeyParams;
		this.solutionMsgKey = solutionMsg;
	}
}
