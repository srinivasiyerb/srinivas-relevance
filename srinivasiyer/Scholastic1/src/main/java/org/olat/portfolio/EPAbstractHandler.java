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
package org.olat.portfolio;

import java.util.Date;

import org.olat.core.commons.services.search.OlatDocument;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.filter.FilterFactory;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.search.service.SearchResourceContext;

/**
 * Description:<br>
 * reason to have this abstract between interface and concrete implementation is to swap out common code here.
 * <P>
 * Initial Date: 11.06.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public abstract class EPAbstractHandler<U extends AbstractArtefact> implements EPArtefactHandler<U> {

	private boolean enabled = true;

	protected static final OLog log = Tracing.createLoggerFor(EPAbstractHandler.class);

	public EPAbstractHandler() {
		//
	}

	/**
	 * @see org.olat.portfolio.EPArtefactHandler#prefillArtefactAccordingToSource(org.olat.portfolio.model.artefacts.AbstractArtefact, java.lang.Object)
	 */
	@Override
	public void prefillArtefactAccordingToSource(final AbstractArtefact artefact, final Object source) {
		if (source instanceof OLATResourceable) {
			final OLATResourceable ores = (OLATResourceable) source;
			artefact.setSource(ores.getResourceableTypeName());
		}
		artefact.setCollectionDate(new Date());
	}

	/**
	 * @see org.olat.portfolio.EPArtefactHandler#getType()
	 */
	@Override
	public abstract String getType();

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @see org.olat.portfolio.EPArtefactHandler#getHandlerTranslator(org.olat.core.gui.translator.Translator)
	 */
	@Override
	public PackageTranslator getHandlerTranslator(final Translator fallBackTrans) {
		final PackageTranslator pT = new PackageTranslator(this.getClass().getPackage().getName(), fallBackTrans.getLocale(), fallBackTrans);
		return pT;
	}

	@Override
	public Controller createDetailsController(final UserRequest ureq, final WindowControl wControl, final AbstractArtefact artefact, final boolean readOnlyMode) {
		return null;
	}

	@Override
	public abstract U createArtefact();

	/**
	 * @return Returns the providesSpecialMapViewController.
	 */
	@Override
	public boolean isProvidingSpecialMapViewController() {
		return false;
	}

	@SuppressWarnings("unused")
	@Override
	public Controller getSpecialMapViewController(final UserRequest ureq, final WindowControl wControl, final AbstractArtefact artefact) {
		return null;
	}

	@Override
	public OlatDocument getIndexerDocument(final SearchResourceContext searchResourceContext, final AbstractArtefact artefact, final EPFrontendManager ePFManager) {

		final OlatDocument document = new OlatDocument();

		final Identity author = artefact.getAuthor();
		if (author != null && author.getUser() != null) {
			final User user = author.getUser();
			document.setAuthor(user.getProperty(UserConstants.FIRSTNAME, null) + " " + user.getProperty(UserConstants.LASTNAME, null));
		}

		final Filter filter = FilterFactory.getHtmlTagAndDescapingFilter();

		document.setCreatedDate(artefact.getCreationDate());
		document.setTitle(filter.filter(artefact.getTitle()));
		document.setDescription(filter.filter(artefact.getDescription()));
		document.setResourceUrl(searchResourceContext.getResourceUrl());
		document.setDocumentType(searchResourceContext.getDocumentType());
		document.setCssIcon(artefact.getIcon());
		document.setParentContextType(searchResourceContext.getParentContextType());
		document.setParentContextName(searchResourceContext.getParentContextName());

		final StringBuilder sb = new StringBuilder();
		if (artefact.getReflexion() != null) {
			sb.append(artefact.getReflexion()).append(' ');
		}
		getContent(artefact, sb, searchResourceContext, ePFManager);
		document.setContent(sb.toString());
		return document;
	}

	@SuppressWarnings("unused")
	protected void getContent(final AbstractArtefact artefact, final StringBuilder sb, final SearchResourceContext context, final EPFrontendManager ePFManager) {
		final String content = ePFManager.getArtefactFullTextContent(artefact);
		if (content != null) {
			sb.append(content);
		}
	}
}
