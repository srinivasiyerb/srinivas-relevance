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
package org.olat.search.service.indexer;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.velocity.context.Context;
import org.olat.core.commons.contextHelp.ContextHelpModule;
import org.olat.core.gui.GlobalSettings;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.winmgr.AJAXFlags;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.render.velocity.VelocityRenderDecorator;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;
import org.olat.core.util.resource.OresHelper;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.document.ContextHelpDocument;

/**
 * Description:<br>
 * This indexer indexes the context sensitive help system
 * <P>
 * Initial Date: 05.11.2008 <br>
 * 
 * @author gnaegi
 */
public class ContextHelpIndexer extends AbstractIndexer {
	private static final OLog log = Tracing.createLoggerFor(ContextHelpIndexer.class);

	/**
	 * @see org.olat.search.service.indexer.Indexer#checkAccess(org.olat.core.id.context.ContextEntry, org.olat.core.id.context.BusinessControl,
	 *      org.olat.core.id.Identity, org.olat.core.id.Roles)
	 */
	@Override
	public boolean checkAccess(final ContextEntry contextEntry, final BusinessControl businessControl, final Identity identity, final Roles roles) {
		// context help is visible to everybody, even not-logged in users
		return true;
	}

	/**
	 * @see org.olat.search.service.indexer.Indexer#getSupportedTypeName()
	 */
	@Override
	public String getSupportedTypeName() {
		return OresHelper.calculateTypeName(ContextHelpModule.class);
	}

	/**
	 * @see org.olat.search.service.indexer.AbstractIndexer#doIndex(org.olat.search.service.SearchResourceContext, java.lang.Object,
	 *      org.olat.search.service.indexer.OlatFullIndexer)
	 */
	@Override
	public void doIndex(final SearchResourceContext parentResourceContext, final Object parentObject, final OlatFullIndexer indexWriter) throws IOException,
			InterruptedException {
		if (!ContextHelpModule.isContextHelpEnabled()) {
			// don't index context help when disabled
			return;
		}
		final long startTime = System.currentTimeMillis();
		final Set<String> helpPageIdentifyers = ContextHelpModule.getAllContextHelpPages();
		final Set<String> languages = I18nModule.getEnabledLanguageKeys();
		if (log.isDebug()) {
			log.debug("ContextHelpIndexer helpPageIdentifyers.size::" + helpPageIdentifyers.size() + " and languages.size::" + languages.size());
		}
		// loop over all help pages
		for (final String helpPageIdentifyer : helpPageIdentifyers) {
			final String[] identifyerSplit = helpPageIdentifyer.split(":");
			final String bundleName = identifyerSplit[0];
			final String page = identifyerSplit[1];
			// Translator with default locale. Locale is set to each language in the
			// language iteration below
			final Translator pageTranslator = new PackageTranslator(bundleName, I18nModule.getDefaultLocale());
			// Open velocity page for this help page
			final String pagePath = bundleName.replace('.', '/') + ContextHelpModule.CHELP_DIR + page;
			final VelocityContainer container = new VelocityContainer("contextHelpPageVC", pagePath, pageTranslator, null);
			final Context ctx = container.getContext();
			final GlobalSettings globalSettings = new GlobalSettings() {
				@Override
				public int getFontSize() {
					return 100;
				}

				@Override
				public AJAXFlags getAjaxFlags() {
					return new EmptyAJAXFlags();
				}

				@Override
				public ComponentRenderer getComponentRendererFor(final Component source) {
					return null;
				}

				@Override
				public boolean isIdDivsForced() {
					return false;
				}
			};
			final Renderer renderer = Renderer.getInstance(container, pageTranslator, new EmptyURLBuilder(), null, globalSettings);
			// Add render decorator with helper methods
			final VelocityRenderDecorator vrdec = new VelocityRenderDecorator(renderer, container);
			ctx.put("r", vrdec);
			// Add empty static dir url - only used to not generate error messages
			ctx.put("chelpStaticDirUrl", "");
			// Create document for each language using the velocity context
			for (final String langCode : languages) {
				final Locale locale = I18nManager.getInstance().getLocaleOrNull(langCode);
				final String relPagePath = langCode + "/" + bundleName + "/" + page;
				if (log.isDebug()) {
					log.debug("Indexing help page with path::" + relPagePath);
				}
				final SearchResourceContext searchResourceContext = new SearchResourceContext(parentResourceContext);
				searchResourceContext.setBusinessControlFor(OresHelper.createOLATResourceableType(ContextHelpModule.class.getSimpleName()));// to match the list of
																																			// indexer
				// Create context help document and index now, set translator to current locale
				pageTranslator.setLocale(locale);
				final Document document = ContextHelpDocument.createDocument(searchResourceContext, bundleName, page, pageTranslator, ctx, pagePath);
				indexWriter.addDocument(document);
			}

		}
		final long indexTime = System.currentTimeMillis() - startTime;
		if (log.isDebug()) {
			log.debug("ContextHelpIndexer finished in " + indexTime + " ms");
		}
	}

}

/**
 * Description:<br>
 * Helper flags that work with the context help indexer
 * <P>
 * Initial Date: 05.11.2008 <br>
 * 
 * @author gnaegi
 */
class EmptyAJAXFlags extends AJAXFlags {

	public EmptyAJAXFlags() {
		super(null);
	}

	@Override
	public boolean isIframePostEnabled() {
		return false;
	}
}

/**
 * Description:<br>
 * Helper URL builder for context help indexer
 * <P>
 * Initial Date: 05.11.2008 <br>
 * 
 * @author gnaegi
 */
class EmptyURLBuilder extends URLBuilder {

	public EmptyURLBuilder() {
		super(null, null, null, null);
	}

	@Override
	public void appendTarget(final StringOutput sb) {
		// nothing to do
	}

	@Override
	public void buildJavaScriptBgCommand(final StringOutput buf, final String[] keys, final String[] values, final int mode) {
		// nothing to do
	}

	@Override
	public void buildURI(final StringOutput buf, final String[] keys, final String[] values, final int mode) {
		// nothing to do
	}

	@Override
	public void buildURI(final StringOutput buf, final String[] keys, final String[] values, final String modURI, final int mode) {
		// nothing to do
	}

	@Override
	public void buildURI(final StringOutput buf, final String[] keys, final String[] values, final String modURI) {
		// nothing to do
	}

	@Override
	public void buildURI(final StringOutput buf, final String[] keys, final String[] values) {
		// nothing to do
	}

	@Override
	public URLBuilder createCopyFor(final Component source) {
		return super.createCopyFor(source);
	}

	@Override
	public void setComponentPath(final String componentPath) {
		// nothing to do
	}

}