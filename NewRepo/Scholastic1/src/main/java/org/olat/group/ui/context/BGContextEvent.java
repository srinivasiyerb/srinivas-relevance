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

package org.olat.group.ui.context;

import org.olat.core.util.event.MultiUserEvent;
import org.olat.group.context.BGContext;

/**
 * Description:<BR>
 * Multi user event fired by the business manage and edit controllers
 * <P>
 * Initial Date: Jan 31, 2005
 * 
 * @author gnaegi
 */
public class BGContextEvent extends MultiUserEvent {

	/** the context is deleted * */
	public static final String CONTEXT_DELETED = "contextdeleted";
	/** a new resource has been added * */
	public static final String RESOURCE_ADDED = "resourceadded";
	/** a resource has been removed * */
	public static final String RESOURCE_REMOVED = "resourceremoved";

	private final Long bgContextKey;

	/**
	 * @param command User public final stings here
	 * @param bgContext
	 */
	public BGContextEvent(final String command, final BGContext bgContext) {
		super(command);
		this.bgContextKey = bgContext.getKey();
	}

	/**
	 * @return the key of the BGContext
	 */
	public Long getBgContextKey() {
		return bgContextKey;
	}
}
