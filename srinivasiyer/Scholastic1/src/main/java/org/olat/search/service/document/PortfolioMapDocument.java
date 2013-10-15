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

package org.olat.search.service.document;

import java.util.List;

import org.apache.lucene.document.Document;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.services.search.OlatDocument;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.filter.FilterFactory;
import org.olat.core.util.resource.OresHelper;
import org.olat.portfolio.EPArtefactHandler;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.structel.EPAbstractMap;
import org.olat.portfolio.model.structel.PortfolioStructure;
import org.olat.search.service.SearchResourceContext;

/**
 * Description:<br>
 * Deliver the lucene document made from a portfolio
 * <P>
 * Initial Date: 12 nov. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class PortfolioMapDocument extends OlatDocument {

	private static final OLog log = Tracing.createLoggerFor(PortfolioMapDocument.class);

	private static BaseSecurity securityManager;
	private static EPFrontendManager ePFMgr;
	private static PortfolioModule portfolioModule;

	public PortfolioMapDocument() {
		super();
		securityManager = BaseSecurityManager.getInstance();
		ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		portfolioModule = (PortfolioModule) CoreSpringFactory.getBean("portfolioModule");
	}

	public static Document createDocument(final SearchResourceContext searchResourceContext, final PortfolioStructure map) {
		final PortfolioMapDocument document = new PortfolioMapDocument();
		if (map instanceof EPAbstractMap) {
			final EPAbstractMap abstractMap = (EPAbstractMap) map;
			if (abstractMap.getOwnerGroup() != null) {
				final List<Identity> identities = securityManager.getIdentitiesOfSecurityGroup(abstractMap.getOwnerGroup());
				final StringBuilder authors = new StringBuilder();
				for (final Identity identity : identities) {
					if (authors.length() > 0) {
						authors.append(", ");
					}
					final User user = identity.getUser();
					authors.append(user.getProperty(UserConstants.FIRSTNAME, null)).append(' ').append(user.getProperty(UserConstants.LASTNAME, null));
				}
				document.setAuthor(authors.toString());
			}
			document.setCreatedDate(abstractMap.getCreationDate());
		}

		final Filter filter = FilterFactory.getHtmlTagAndDescapingFilter();

		document.setTitle(map.getTitle());
		document.setDescription(filter.filter(map.getDescription()));
		final StringBuilder sb = new StringBuilder();
		getContent(map, searchResourceContext, sb, filter);
		document.setContent(sb.toString());
		document.setResourceUrl(searchResourceContext.getResourceUrl());
		document.setDocumentType(searchResourceContext.getDocumentType());
		document.setCssIcon("o_ep_icon");
		document.setParentContextType(searchResourceContext.getParentContextType());
		document.setParentContextName(searchResourceContext.getParentContextName());

		if (log.isDebug()) {
			log.debug(document.toString());
		}
		return document.getLuceneDocument();
	}

	private static String getContent(final PortfolioStructure map, final SearchResourceContext resourceContext, final StringBuilder sb, final Filter filter) {
		sb.append(' ').append(map.getTitle());
		if (StringHelper.containsNonWhitespace(map.getDescription())) {
			sb.append(' ').append(filter.filter(map.getDescription()));
		}
		for (final PortfolioStructure child : ePFMgr.loadStructureChildren(map)) {
			getContent(child, resourceContext, sb, filter);
		}
		for (final AbstractArtefact artefact : ePFMgr.getArtefacts(map)) {
			final String reflexion = artefact.getReflexion();
			if (StringHelper.containsNonWhitespace(reflexion)) {
				sb.append(' ').append(filter.filter(reflexion));
			}

			final OLATResourceable ores = OresHelper.createOLATResourceableInstance(AbstractArtefact.class.getSimpleName(), artefact.getKey());
			final EPArtefactHandler<?> handler = portfolioModule.getArtefactHandler(artefact.getResourceableTypeName());

			final SearchResourceContext artefactResourceContext = new SearchResourceContext(resourceContext);
			artefactResourceContext.setBusinessControlFor(ores);
			final OlatDocument doc = handler.getIndexerDocument(artefactResourceContext, artefact, ePFMgr);
			sb.append(' ').append(doc.getContent());
		}
		return sb.toString();
	}
}