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

package org.olat.core.gui.render.velocity;

import org.olat.core.configuration.Initializable;
import org.olat.core.helpers.Settings;

/**
 * Initial Date: Apr 29, 2004
 * 
 * @author Mike Stock Comment:
 */
public class VelocityModule implements Initializable {

	private static final String DEFAULT_ENCODING = "UTF-8";

	private static String inputEncoding = DEFAULT_ENCODING;
	private static String outputEncoding = DEFAULT_ENCODING;

	/**
	 * [spring]
	 */
	private VelocityModule() {
		// called by spring
	}

	/**
	 * @return Returns the cachePages.
	 */
	public static boolean isCachePages() {
		// caching only for productive mode
		return !Settings.isDebuging();
	}

	/**
	 * @return Returns the inputEncoding.
	 */
	public static String getInputEncoding() {
		return inputEncoding;
	}

	/**
	 * @return Returns the outputEncoding.
	 */
	public static String getOutputEncoding() {
		return outputEncoding;
	}

	@Override
	public void init() {
		VelocityHelper.getInstance();
	}

}