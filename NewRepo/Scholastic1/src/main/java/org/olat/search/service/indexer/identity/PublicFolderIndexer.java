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
package org.olat.search.service.indexer.identity;

import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.resource.OresHelper;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.indexer.FolderIndexer;
import org.olat.search.service.indexer.FolderIndexerAccess;
import org.olat.search.service.indexer.OlatFullIndexer;

/**
 * <h3>Description:</h3>
 * <p>
 * The identity indexer indexes the users public folder
 * <p>
 * Initial Date: 21.08.2009 <br>
 * 
 * @author gnaegi, gnaegi@frentix.com, www.frentix.com
 */
public class PublicFolderIndexer extends FolderIndexer {
	private static final OLog log = Tracing.createLoggerFor(PublicFolderIndexer.class);
	public static final String TYPE = "type.identity.publicfolder";
	public static final OLATResourceable BUSINESS_CONTROL_TYPE = OresHelper.createOLATResourceableTypeWithoutCheck(FolderRunController.class.getSimpleName());

	/**
	 * @see org.olat.search.service.indexer.Indexer#getSupportedTypeName()
	 */
	@Override
	public String getSupportedTypeName() {
		return Identity.class.getSimpleName();
	}

	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsDownload()
	 */

	@Override
	public void doIndex(final SearchResourceContext parentResourceContext, final Object parentObject, final OlatFullIndexer indexWriter) {

		try {
			// get public folder for user
			final Identity identity = (Identity) parentObject;
			final OlatRootFolderImpl rootContainer = new OlatRootFolderImpl(FolderConfig.getUserHome(identity.getName()) + "/public", null);
			if (!rootContainer.getBasefile().exists()) { return; }
			// build new resource context
			final SearchResourceContext searchResourceContext = new SearchResourceContext(parentResourceContext);
			searchResourceContext.setParentContextName(identity.getName());
			searchResourceContext.setBusinessControlFor(BUSINESS_CONTROL_TYPE);
			searchResourceContext.setDocumentType(TYPE);
			// now index the folder
			doIndexVFSContainer(searchResourceContext, rootContainer, indexWriter, "", FolderIndexerAccess.FULL_ACCESS);
		} catch (final Exception ex) {
			log.warn("Exception while indexing public folder of identity::" + parentObject.toString() + ". Skipping this user, try next one.", ex);
		}
		if (log.isDebug()) {
			log.debug("PublicFolder finished for user::" + parentObject.toString());
		}

	}

	/**
	 * @see org.olat.search.service.indexer.Indexer#checkAccess(org.olat.core.id.context.ContextEntry, org.olat.core.id.context.BusinessControl,
	 *      org.olat.core.id.Identity, org.olat.core.id.Roles)
	 */
	@Override
	public boolean checkAccess(final ContextEntry contextEntry, final BusinessControl businessControl, final Identity identity, final Roles roles) {
		return true;
	}
}
