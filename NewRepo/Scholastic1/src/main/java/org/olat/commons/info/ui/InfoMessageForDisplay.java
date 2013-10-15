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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.commons.info.ui;

import org.olat.core.util.StringHelper;

/**
 * Description:<br>
 * A small wrapper for all informations about an info message.
 * <P>
 * Initial Date: 14 déc. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoMessageForDisplay {

	private final Long key;
	private final String title;
	private final String message;
	private final String infos;
	private final String modifier;

	public InfoMessageForDisplay(final Long key, final String title, final String message, final String infos, final String modifier) {
		this.key = key;
		this.title = title;
		this.infos = infos;
		this.message = message;
		this.modifier = modifier;
	}

	public Long getKey() {
		return key;
	}

	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}

	public String getInfos() {
		return infos;
	}

	public boolean isModified() {
		return StringHelper.containsNonWhitespace(modifier);
	}

	public String getModifier() {
		return modifier;
	}

}
