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
 * Copyright (c) 1999-2008 at Multimedia- & E-Learning Services (MELS),<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.core.gui.components.form.flexible.impl.elements;

import java.util.Locale;

import org.olat.core.gui.components.form.ValidationError;

/**
 * Description:<br>
 * This interface is needed, to call required isValidValue() depending on sub-type
 * <P>
 * Initial Date: 07.03.2008 <br>
 * 
 * @author rhaag
 */
public interface ItemValidatorProvider {

	public boolean isValidValue(String value, ValidationError validationError, Locale locale);
}
