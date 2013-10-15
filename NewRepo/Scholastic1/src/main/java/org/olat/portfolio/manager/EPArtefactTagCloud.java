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

package org.olat.portfolio.manager;

import java.util.List;
import java.util.Set;

import org.olat.portfolio.model.artefacts.AbstractArtefact;

/**
 * Description:<br>
 * This a wrapper with the artefacts found and the cloud of tags.
 * <P>
 * Initial Date: 8 déc. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class EPArtefactTagCloud {

	private final List<AbstractArtefact> artefacts;
	private final Set<String> tags;

	public EPArtefactTagCloud(final List<AbstractArtefact> artefacts, final Set<String> tags) {
		this.artefacts = artefacts;
		this.tags = tags;
	}

	public List<AbstractArtefact> getArtefacts() {
		return artefacts;
	}

	public Set<String> getTags() {
		return tags;
	}
}
