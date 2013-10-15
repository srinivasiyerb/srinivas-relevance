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

package org.olat.course.assessment.portfolio;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.course.assessment.EfficiencyStatement;
import org.olat.course.assessment.EfficiencyStatementController;
import org.olat.portfolio.EPAbstractHandler;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.search.service.SearchResourceContext;

import com.thoughtworks.xstream.XStream;

/**
 * Description:<br>
 * EvidenceArtefactHandler
 * <P>
 * Initial Date: 7 oct. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class EfficiencyStatementArtefactHandler extends EPAbstractHandler<EfficiencyStatementArtefact> {

	private final XStream myXStream = XStreamHelper.createXStreamInstance();

	@Override
	public String getType() {
		return EfficiencyStatementArtefact.ARTEFACT_TYPE;
	}

	@Override
	public EfficiencyStatementArtefact createArtefact() {
		return new EfficiencyStatementArtefact();
	}

	/**
	 * @see org.olat.portfolio.EPAbstractHandler#prefillArtefactAccordingToSource(org.olat.portfolio.model.artefacts.AbstractArtefact, java.lang.Object)
	 */
	@Override
	public void prefillArtefactAccordingToSource(final AbstractArtefact artefact, final Object source) {
		super.prefillArtefactAccordingToSource(artefact, source);

		if (source instanceof EfficiencyStatement) {
			final EfficiencyStatement statement = (EfficiencyStatement) source;
			if (artefact.getTitle() == null) {
				artefact.setTitle(statement.getCourseTitle());
			}
			final String efficiencyStatementX = myXStream.toXML(statement);
			artefact.setSource(statement.getCourseTitle());
			artefact.setFulltextContent(efficiencyStatementX);
			artefact.setSignature(90);
		}
	}

	@Override
	public Controller createDetailsController(final UserRequest ureq, final WindowControl wControl, final AbstractArtefact artefact, final boolean readOnlyMode) {
		final EPFrontendManager ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		final String statementXml = ePFMgr.getArtefactFullTextContent(artefact);
		final EfficiencyStatement statement = (EfficiencyStatement) myXStream.fromXML(statementXml);
		return new EfficiencyStatementController(wControl, ureq, statement);
	}

	@Override
	protected void getContent(final AbstractArtefact artefact, final StringBuilder sb, final SearchResourceContext context, final EPFrontendManager ePFManager) {
		final String statementXml = ePFManager.getArtefactFullTextContent(artefact);
		if (!StringHelper.containsNonWhitespace(statementXml)) { return; }

		try {
			final EfficiencyStatement statement = (EfficiencyStatement) myXStream.fromXML(statementXml);
			sb.append(statement.getCourseTitle()).append(' ');
			sb.append(statement.getDisplayableUserInfo()).append(' ');
		} catch (final Exception ex) {
			log.error("Error while parsing " + artefact, ex);
		}
	}
}
