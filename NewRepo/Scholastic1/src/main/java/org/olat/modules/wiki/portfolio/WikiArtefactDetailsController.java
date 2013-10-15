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
package org.olat.modules.wiki.portfolio;

import java.util.Locale;

import org.jamwiki.DataHandler;
import org.jamwiki.model.Topic;
import org.jamwiki.model.WikiFile;
import org.jamwiki.parser.AbstractParser;
import org.jamwiki.parser.ParserDocument;
import org.jamwiki.parser.ParserInput;
import org.jamwiki.parser.jflex.JFlexParser;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.filter.FilterFactory;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;

/**
 * Description:<br>
 * Show the specific part of the WikiArtefact
 * <P>
 * Initial Date: 11 oct. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class WikiArtefactDetailsController extends BasicController {

	private static final OLog log = Tracing.createLoggerFor(WikiArtefactDetailsController.class);

	private final VelocityContainer vC;

	public WikiArtefactDetailsController(final UserRequest ureq, final WindowControl wControl, final AbstractArtefact artefact) {
		super(ureq, wControl);
		final WikiArtefact fArtefact = (WikiArtefact) artefact;
		vC = createVelocityContainer("details");
		final EPFrontendManager ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		final String wikiText = getContent(ePFMgr.getArtefactFullTextContent(fArtefact));
		vC.contextPut("text", wikiText);
		putInitialPanel(vC);
	}

	@Override
	@SuppressWarnings("unused")
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	@Override
	protected void doDispose() {
		//
	}

	private static String getContent(final String content) {
		try {
			final ParserInput input = new ParserInput();
			input.setWikiUser(null);
			input.setAllowSectionEdit(false);
			input.setDepth(2);
			input.setContext("");
			input.setLocale(Locale.ENGLISH);
			input.setTopicName("dummy");
			input.setUserIpAddress("0.0.0.0");
			input.setDataHandler(new DummyDataHandler());
			input.setVirtualWiki("/olat");

			final AbstractParser parser = new JFlexParser(input);
			final ParserDocument parsedDoc = parser.parseHTML(content);
			final String parsedContent = parsedDoc.getContent();
			final String filteredContent = FilterFactory.getHtmlTagAndDescapingFilter().filter(parsedContent);
			return filteredContent;
		} catch (final Exception e) {
			e.printStackTrace();
			log.error("", e);
			return content;
		}
	}

	public static class DummyDataHandler implements DataHandler {

		@Override
		@SuppressWarnings("unused")
		public boolean exists(final String virtualWiki, final String topic) {
			return true;
		}

		@Override
		@SuppressWarnings("unused")
		public Topic lookupTopic(final String virtualWiki, final String topicName, final boolean deleteOK, final Object transactionObject) throws Exception {
			return null;
		}

		@Override
		@SuppressWarnings("unused")
		public WikiFile lookupWikiFile(final String virtualWiki, final String topicName) throws Exception {
			return null;
		}
	}

}
