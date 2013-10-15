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
package org.olat.portfolio.ui.artefacts.view.details;

import javax.servlet.http.HttpServletRequest;

import org.olat.core.CoreSpringFactory;
import org.olat.core.dispatcher.mapper.Mapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.util.Formatter;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.filter.FilterFactory;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSMediaResource;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.ui.artefacts.collect.EPCreateTextArtefactStepForm00;

/**
 * Description:<br>
 * Show the specific part of the EPTextArtefact
 * <P>
 * Initial Date: 11 oct. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class TextArtefactDetailsController extends BasicController {

	private final VelocityContainer vC;
	private final boolean readOnlyMode;
	private Link editBtn;
	private CloseableCalloutWindowController calloutCtrl;
	private EPCreateTextArtefactStepForm00 textEditCtrl;
	private final AbstractArtefact artefact;
	private final EPFrontendManager ePFMgr;

	public TextArtefactDetailsController(final UserRequest ureq, final WindowControl wControl, final AbstractArtefact artefact, final boolean readOnlyMode) {
		super(ureq, wControl);
		this.readOnlyMode = readOnlyMode;
		this.artefact = artefact;
		ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		vC = createVelocityContainer("textDetails");
		init();

		putInitialPanel(vC);
	}

	private void init() {
		final String artFulltextContent = ePFMgr.getArtefactFullTextContent(artefact);
		if (!readOnlyMode) {
			// prepare an edit link
			String fulltext = FilterFactory.getHtmlTagAndDescapingFilter().filter(artFulltextContent);
			fulltext = Formatter.truncate(fulltext, 50);
			editBtn = LinkFactory.createCustomLink("text.edit.link", "edit", fulltext, Link.NONTRANSLATED, vC, this);
			editBtn.setCustomEnabledLinkCSS("b_inline_editable b_ep_nolink");
		} else {
			// register a mapper to deliver uploaded media files
			final VFSContainer artefactFolder = ePFMgr.getArtefactContainer(artefact);
			final String mapperBase = registerMapper(new Mapper() {
				@SuppressWarnings("unused")
				@Override
				public MediaResource handle(final String relPath, final HttpServletRequest request) {
					final VFSItem currentItem = artefactFolder.resolve(relPath);
					final VFSMediaResource vmr = new VFSMediaResource((VFSLeaf) currentItem);
					return vmr;
				}
			});
			final Filter urlFilter = FilterFactory.getBaseURLToMediaRelativeURLFilter(mapperBase);
			final String wrappedText = urlFilter.filter(artFulltextContent);
			vC.contextPut("text", wrappedText);
		}
	}

	@Override
	@SuppressWarnings("unused")
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == editBtn) {
			popupEditorCallout(ureq);
		}
	}

	@SuppressWarnings("unused")
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == calloutCtrl && event.equals(CloseableCalloutWindowController.CLOSE_WINDOW_EVENT)) {
			removeAsListenerAndDispose(calloutCtrl);
			calloutCtrl = null;
		} else if (source == textEditCtrl && event == Event.DONE_EVENT) {
			// close callout, refresh artefact-details
			calloutCtrl.deactivate();
			removeAsListenerAndDispose(calloutCtrl);
			init();
		}
	}

	private void popupEditorCallout(final UserRequest ureq) {
		removeAsListenerAndDispose(textEditCtrl);
		textEditCtrl = new EPCreateTextArtefactStepForm00(ureq, getWindowControl(), artefact);
		listenTo(textEditCtrl);
		instantiateCalloutController(ureq, textEditCtrl.getInitialComponent(), editBtn);
	}

	private void instantiateCalloutController(final UserRequest ureq, final Component content, final Link button) {
		removeAsListenerAndDispose(calloutCtrl);
		final String title = translate("textartefact.edit.title");
		calloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(), content, button, title, true, null);
		listenTo(calloutCtrl);
		calloutCtrl.activate();
	}

	@Override
	protected void doDispose() {
		// nothing
	}
}
