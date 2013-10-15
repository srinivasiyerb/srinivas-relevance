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

package org.olat.commons.servlets;

import java.util.HashMap;

import org.olat.commons.servlets.pathhandlers.PathHandler;
import org.olat.core.configuration.Initializable;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;

/**
 * Initial Date: 13.11.2002
 * 
 * @author Mike Stock Comment:
 * @deprecated Please use GlobalMapperRegistry if you need to provide an url for e.g. static resources which are shared by all users
 */
@Deprecated
public class StaticsModule implements Initializable {
	OLog log = Tracing.createLoggerFor(StaticsModule.class);
	private static HashMap handlers = new HashMap(5);

	/**
	 * 
	 */
	private StaticsModule() {
		//
	}

	/**
	 * @see org.olat.core.configuration.OLATModule#init(com.anthonyeden.lib.config.Configuration)
	 */
	@Override
	public void init() {
		// Initialize handlers

		try {
			Class pathHandler = Class.forName("org.olat.commons.servlets.pathhandlers.FilePathHandler");
			PathHandler o = (PathHandler) pathHandler.newInstance();
			o.init("static");
			handlers.put("raw", o);

			pathHandler = Class.forName("org.olat.commons.servlets.pathhandlers.ContextHelpFilePathHandler");
			o = (PathHandler) pathHandler.newInstance();
			o.init("static/help");
			handlers.put("help", o);

			pathHandler = Class.forName("org.olat.ims.qti.QTIStaticsHandler");
			o = (PathHandler) pathHandler.newInstance();
			o.init("");
			handlers.put("qti", o);

			pathHandler = Class.forName("org.olat.ims.qti.editor.QTIEditorStaticsHandler");
			o = (PathHandler) pathHandler.newInstance();
			o.init("");
			handlers.put("qtieditor", o);
		} catch (final Exception e) {
			log.error("error while creating class by name", e);
		}
	}

	/**
	 * @param name
	 * @return A path handler handling requests for the given name identifier.
	 */
	public static PathHandler getInstance(final String name) {
		return (PathHandler) handlers.get(name);
	}

}
