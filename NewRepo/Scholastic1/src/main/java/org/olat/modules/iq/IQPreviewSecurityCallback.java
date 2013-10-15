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

import org.olat.ims.qti.process.AssessmentInstance;

/**
 * Initial Date: Mar 4, 2004
 * 
 * @author Mike Stock
 */
public class IQPreviewSecurityCallback implements IQSecurityCallback {

	public IQPreviewSecurityCallback() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.modules.iq.IQSecurityCallback#isAllowed(org.olat.ims.qti.process.AssessmentInstance)
	 */
	@Override
	public boolean isAllowed(final AssessmentInstance ai) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.modules.iq.IQSecurityCallback#attemptsLeft(org.olat.ims.qti.process.AssessmentInstance)
	 */
	@Override
	public int attemptsLeft(final AssessmentInstance ai) {
		return 99;
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.modules.iq.IQSecurityCallback#isPreview()
	 */
	@Override
	public boolean isPreview() {
		return true;
	}

}
