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
import org.olat.core.commons.services.search.OlatDocument;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.filter.FilterFactory;
import org.olat.core.util.i18n.I18nModule;
import org.olat.search.service.SearchResourceContext;
import org.olat.user.HomePageConfig;
import org.olat.user.HomePageConfigManager;
import org.olat.user.HomePageConfigManagerImpl;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * <h3>Description:</h3>
 * <p>
 * The IdentityDocument creates a search engine view for a certain identity
 * <p>
 * Initial Date: 21.08.2009 <br>
 * 
 * @author gnaegi, gnaegi@frentix.com, www.frentix.com
 */
public class IdentityDocument extends OlatDocument {
	private static final OLog log = Tracing.createLoggerFor(IdentityDocument.class);

	/**
	 * Factory method to create a new IdentityDocument
	 * 
	 * @param searchResourceContext
	 * @param wikiPage
	 * @return
	 */
	public static Document createDocument(final SearchResourceContext searchResourceContext, final Identity identity) {

		final UserManager userMgr = UserManager.getInstance();
		final User user = identity.getUser();

		final HomePageConfigManager homepageMgr = HomePageConfigManagerImpl.getInstance();
		final HomePageConfig publishConfig = homepageMgr.loadConfigFor(identity.getName());

		final IdentityDocument identityDocument = new IdentityDocument();
		identityDocument.setTitle(identity.getName());
		identityDocument.setCreatedDate(user.getCreationDate());

		// loop through all user properties and collect the content string and the last modified
		final List<UserPropertyHandler> userPropertyHanders = userMgr.getUserPropertyHandlersFor(IdentityDocument.class.getName(), false);
		final StringBuilder content = new StringBuilder();
		for (final UserPropertyHandler userPropertyHandler : userPropertyHanders) {
			final String propertyName = userPropertyHandler.getName();
			// only index fields the user has published!
			if (publishConfig.isEnabled(propertyName)) {
				final String value = user.getProperty(propertyName, I18nModule.getDefaultLocale());
				if (value != null) {
					content.append(value).append(" ");
				}
			}
		}
		// user text
		String text = publishConfig.getTextAboutMe();
		if (StringHelper.containsNonWhitespace(text)) {
			text = FilterFactory.getHtmlTagsFilter().filter(text);
			content.append(text).append(' ');
		}
		// finally use the properties as the content for this identity
		if (content.length() > 0) {
			identityDocument.setContent(content.toString());
		}

		identityDocument.setResourceUrl(searchResourceContext.getResourceUrl());
		identityDocument.setDocumentType(searchResourceContext.getParentContextType());
		identityDocument.setCssIcon(CSSHelper.CSS_CLASS_USER);

		if (log.isDebug()) {
			log.debug(identityDocument.toString());
		}
		return identityDocument.getLuceneDocument();
	}

}
