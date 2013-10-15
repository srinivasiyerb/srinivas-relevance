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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.core.util;

import java.util.logging.Level;

/**
 * Description:<br>
 * TODO: patrickb Class Description for ValidationStatusImpl
 * <P>
 * Initial Date: 04.12.2006 <br>
 * 
 * @author patrickb
 */
public class ValidationStatusImpl implements ValidationStatus {

	private Level level = ValidationStatus.NOERROR;
	private ValidationAction action;
	public static ValidationStatus NOERROR = new ValidationStatusImpl();

	private ValidationStatusImpl() {
		//
	}

	public ValidationStatusImpl(Level level) {
		this(level, null);
	}

	public ValidationStatusImpl(Level level, ValidationAction action) {
		this.level = level;
		this.action = action;
	}

	/**
	 * @see org.olat.core.util.ValidationStatus#isError()
	 */
	@Override
	public boolean isError() {
		return ValidationStatus.ERROR.equals(level);
	}

	/**
	 * @see org.olat.core.util.ValidationStatus#isInfo()
	 */
	@Override
	public boolean isInfo() {
		return ValidationStatus.INFO.equals(level);
	}

	/**
	 * @see org.olat.core.util.ValidationStatus#isWarning()
	 */
	@Override
	public boolean isWarning() {
		return ValidationStatus.WARNING.equals(level);
	}

	@Override
	public Level getLevel() {
		return level;
	}

	@Override
	public ValidationAction getAction() {
		return action;
	}
}
