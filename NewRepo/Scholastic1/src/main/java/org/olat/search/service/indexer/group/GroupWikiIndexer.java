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

package org.olat.search.service.indexer.group;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.olat.collaboration.CollaborationTools;
import org.olat.collaboration.CollaborationToolsFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.group.BusinessGroup;
import org.olat.group.ui.run.BusinessGroupMainRunController;
import org.olat.modules.wiki.Wiki;
import org.olat.modules.wiki.WikiManager;
import org.olat.modules.wiki.WikiPage;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.document.WikiPageDocument;
import org.olat.search.service.indexer.FolderIndexer;
import org.olat.search.service.indexer.OlatFullIndexer;

/**
 * Index all group folders.
 * 
 * @author Christian Guretzki
 */
public class GroupWikiIndexer extends FolderIndexer {
	private static final OLog log = Tracing.createLoggerFor(GroupWikiIndexer.class);
	// Must correspond with LocalString_xx.properties
	// Do not use '_' because we want to seach for certain documenttype and lucene haev problems with '_'
	public static final String TYPE = "type.group.wiki";

	public GroupWikiIndexer() {
		//
	}

	@Override
	public void doIndex(final SearchResourceContext parentResourceContext, final Object businessObj, final OlatFullIndexer indexWriter) throws IOException,
			InterruptedException {
		if (!(businessObj instanceof BusinessGroup)) { throw new AssertException("businessObj must be BusinessGroup"); }
		final BusinessGroup businessGroup = (BusinessGroup) businessObj;

		// Index Group Wiki
		if (log.isDebug()) {
			log.debug("Analyse Wiki for Group=" + businessGroup);
		}
		final CollaborationTools collabTools = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(businessGroup);
		if (collabTools.isToolEnabled(CollaborationTools.TOOL_WIKI)) {
			try {
				final Wiki wiki = WikiManager.getInstance().getOrLoadWiki(businessGroup);
				// loop over all wiki pages
				final List<WikiPage> wikiPageList = wiki.getAllPagesWithContent();
				for (final WikiPage wikiPage : wikiPageList) {
					final SearchResourceContext wikiResourceContext = new SearchResourceContext(parentResourceContext);
					wikiResourceContext.setBusinessControlFor(BusinessGroupMainRunController.ORES_TOOLWIKI);
					wikiResourceContext.setDocumentType(TYPE);
					wikiResourceContext.setDocumentContext(businessGroup.getKey() + " ");
					wikiResourceContext.setFilePath(wikiPage.getPageName());

					final Document document = WikiPageDocument.createDocument(wikiResourceContext, wikiPage);
					indexWriter.addDocument(document);
				}
			} catch (final NullPointerException nex) {
				log.warn("NullPointerException in GroupWikiIndexer.doIndex.", nex);
			}
		} else {
			if (log.isDebug()) {
				log.debug("Group=" + businessGroup + " has no Wiki.");
			}
		}

	}

	@Override
	public boolean checkAccess(final ContextEntry contextEntry, final BusinessControl businessControl, final Identity identity, final Roles roles) {
		// TODO:chg: check with collabTools if folder is enabled
		return true;
	}

	@Override
	public String getSupportedTypeName() {
		return BusinessGroupMainRunController.ORES_TOOLWIKI.getResourceableTypeName();
	}

}
