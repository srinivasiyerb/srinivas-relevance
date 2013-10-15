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
 * Copyright (c) 1999-2008 at frentix GmbH, Switzerland, http://www.frentix.com
 * <p>
 */
package org.olat.core.commons.modules.glossary;

import java.util.List;

import org.olat.core.commons.modules.glossary.morphService.MorphologicalService;

/**
 * Description:<br>
 * Helps to get configuration settings.
 * <P>
 * Initial Date: 23.12.2008 <br>
 * 
 * @author Roman Haag, frentix GmbH, roman.haag@frentix.com
 */
public class GlossaryModule {

	private static List<MorphologicalService> morphologicalServices;

	/**
	 * @return Returns the flexionServices.
	 */
	public static List<MorphologicalService> getMorphologicalServices() {
		return morphologicalServices;
	}

	/**
	 * used by spring
	 * 
	 * @param morphServices The MorphologicalService to set.
	 */
	public void setMorphologicalServices(List<MorphologicalService> morphServices) {
		morphologicalServices = morphServices;
	}

}
