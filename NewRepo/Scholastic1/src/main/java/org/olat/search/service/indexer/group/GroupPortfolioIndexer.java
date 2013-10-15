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

package org.olat.search.service.indexer.group;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.olat.collaboration.CollaborationTools;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.group.BusinessGroup;
import org.olat.group.ui.run.BusinessGroupMainRunController;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.structel.EPDefaultMap;
import org.olat.portfolio.model.structel.ElementType;
import org.olat.portfolio.model.structel.PortfolioStructure;
import org.olat.properties.NarrowedPropertyManager;
import org.olat.properties.Property;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.document.GroupDocument;
import org.olat.search.service.document.PortfolioMapDocument;
import org.olat.search.service.indexer.AbstractIndexer;
import org.olat.search.service.indexer.OlatFullIndexer;

/**
 * Description:<br>
 * Index the portfolio map in the groups
 * <P>
 * Initial Date: 17 nov. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class GroupPortfolioIndexer extends AbstractIndexer {

	public static final String TYPE = "type.group." + EPDefaultMap.class.getSimpleName();
	public static final String ORES_TYPE = EPDefaultMap.class.getSimpleName();

	private PortfolioModule portfolioModule;
	private EPFrontendManager frontendManager;

	/**
	 * [used by Spring]
	 * 
	 * @param portfolioModule
	 */
	public void setPortfolioModule(final PortfolioModule portfolioModule) {
		this.portfolioModule = portfolioModule;
	}

	/**
	 * [used by Spring]
	 * 
	 * @param frontendManager
	 */
	public void setFrontendManager(final EPFrontendManager frontendManager) {
		this.frontendManager = frontendManager;
	}

	protected String getDocumentType() {
		return TYPE;
	}

	protected ElementType getElementType() {
		return ElementType.DEFAULT_MAP;
	}

	@Override
	public String getSupportedTypeName() {
		return ORES_TYPE;
	}

	@Override
	public void doIndex(final SearchResourceContext parentResourceContext, final Object businessObj, final OlatFullIndexer indexerWriter) throws IOException,
			InterruptedException {
		if (!portfolioModule.isEnabled()) { return; }
		if (!(businessObj instanceof BusinessGroup)) { throw new AssertException("businessObj must be BusinessGroup"); }

		final BusinessGroup businessGroup = (BusinessGroup) businessObj;
		final NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(businessGroup);
		final Property mapKeyProperty = npm.findProperty(null, null, CollaborationTools.PROP_CAT_BG_COLLABTOOLS, CollaborationTools.KEY_PORTFOLIO);
		// Check if portfolio map property exist
		if (mapKeyProperty != null) {
			final Long mapKey = mapKeyProperty.getLongValue();
			final PortfolioStructure map = frontendManager.loadPortfolioStructureByKey(mapKey);
			final SearchResourceContext resourceContext = new SearchResourceContext(parentResourceContext);
			resourceContext.setBusinessControlFor(BusinessGroupMainRunController.ORES_TOOLPORTFOLIO);
			resourceContext.setDocumentType(TYPE);
			resourceContext.setDocumentContext(businessGroup.getKey() + " " + mapKey);
			resourceContext.setParentContextType(GroupDocument.TYPE);
			resourceContext.setParentContextName(businessGroup.getName());
			final Document document = PortfolioMapDocument.createDocument(resourceContext, map);
			indexerWriter.addDocument(document);
		}
	}

	@Override
	public boolean checkAccess(final ContextEntry contextEntry, final BusinessControl businessControl, final Identity identity, final Roles roles) {
		return true;
	}
}
