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
package org.olat.portfolio.ui.artefacts.collect;

import java.util.Date;

import org.olat.core.CoreSpringFactory;
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
import org.olat.core.gui.control.generic.wizard.Step;
import org.olat.core.gui.control.generic.wizard.StepRunnerCallback;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.modules.webFeed.portfolio.EPCreateLiveBlogArtefactStep00;
import org.olat.modules.webFeed.portfolio.LiveBlogArtefact;
import org.olat.portfolio.EPArtefactHandler;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.artefacts.EPTextArtefact;
import org.olat.portfolio.model.artefacts.FileArtefact;

/**
 * Description:<br>
 * overlay controller to hold some links for different kind of adding artefacts. - triggers further workflows to add artefact fires an Done-Event when an artefact was
 * added
 * <P>
 * Initial Date: 26.07.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPAddArtefactController extends BasicController {

	private Link uploadBtn;
	private Link liveBlogBtn;
	private Link importBtn; // not yet available, for v2 when import/export exists
	private final EPFrontendManager ePFMgr;
	private VelocityContainer addPage = null;
	private Link textBtn;
	private final Link addBtn;
	private StepsMainRunController collectStepsCtrl;
	private final PortfolioModule portfolioModule;
	private VFSContainer vfsTemp;
	private final VelocityContainer addLinkVC;
	private CloseableCalloutWindowController calloutCtr;

	public EPAddArtefactController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		portfolioModule = (PortfolioModule) CoreSpringFactory.getBean("portfolioModule");
		addLinkVC = createVelocityContainer("addLink");
		addBtn = LinkFactory.createButton("add.artefact", addLinkVC, this);
		putInitialPanel(addLinkVC);
	}

	private void initAddPageVC() {
		addPage = createVelocityContainer("addpanel");
		final EPArtefactHandler<?> textHandler = portfolioModule.getArtefactHandler(EPTextArtefact.TEXT_ARTEFACT_TYPE);
		if (textHandler != null && textHandler.isEnabled()) {
			textBtn = LinkFactory.createLink("add.text.artefact", addPage, this);
		}
		final EPArtefactHandler<?> fileHandler = portfolioModule.getArtefactHandler(FileArtefact.FILE_ARTEFACT_TYPE);
		if (fileHandler != null && fileHandler.isEnabled()) {
			uploadBtn = LinkFactory.createLink("add.artefact.upload", addPage, this);
		}
		final EPArtefactHandler<?> liveblogHandler = portfolioModule.getArtefactHandler(LiveBlogArtefact.TYPE);
		if (liveblogHandler != null && liveblogHandler.isEnabled()) {
			liveBlogBtn = LinkFactory.createLink("add.artefact.liveblog", addPage, this);
			liveBlogBtn.setCustomDisplayText(translate("add.artefact.blog"));
		}

		importBtn = LinkFactory.createLink("add.artefact.import", addPage, this); // not yet available, for v2 when import/export exists
	}

	private void initAddLinkPopup(final UserRequest ureq) {
		if (addPage == null) {
			initAddPageVC();
		}
		final String title = translate("add.artefact");

		removeAsListenerAndDispose(calloutCtr);
		calloutCtr = new CloseableCalloutWindowController(ureq, getWindowControl(), addPage, addBtn, title, true, null);
		listenTo(calloutCtr);
		calloutCtr.activate();
	}

	private void closeAddLinkPopup() {
		if (calloutCtr != null) {
			calloutCtr.deactivate();
			removeAsListenerAndDispose(calloutCtr);
			calloutCtr = null;
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Component source, @SuppressWarnings("unused") final Event event) {
		if (source == addBtn) {
			if (calloutCtr == null) {
				initAddLinkPopup(ureq);
			} else {
				closeAddLinkPopup();
			}
		} else {
			// close on all clicked links in the popup
			closeAddLinkPopup();
			if (source == textBtn) {
				prepareNewTextArtefactWizzard(ureq);
			} else if (source == uploadBtn) {
				prepareFileArtefactWizzard(ureq);
			} else if (source == liveBlogBtn) {
				prepareNewLiveBlogArtefactWizzard(ureq);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == collectStepsCtrl && event == Event.CANCELLED_EVENT) {
			if (vfsTemp != null) {
				vfsTemp.delete();
				vfsTemp = null;
			}
			getWindowControl().pop();
			removeAsListenerAndDispose(collectStepsCtrl);
		}
		if (source == collectStepsCtrl && event == Event.CHANGED_EVENT) {
			getWindowControl().pop();
			removeAsListenerAndDispose(collectStepsCtrl);
			showInfo("collect.success.text.artefact");
			fireEvent(ureq, Event.DONE_EVENT);
		}
		if (source == calloutCtr && event == CloseableCalloutWindowController.CLOSE_WINDOW_EVENT) {
			removeAsListenerAndDispose(calloutCtr);
			calloutCtr = null;
		}
	}

	/**
	 * prepare a new text artefact and open with wizzard initialized with a special first step for text-artefacts
	 * 
	 * @param ureq
	 */
	private void prepareNewTextArtefactWizzard(final UserRequest ureq) {
		final EPArtefactHandler<?> artHandler = portfolioModule.getArtefactHandler(EPTextArtefact.TEXT_ARTEFACT_TYPE);
		final AbstractArtefact artefact1 = artHandler.createArtefact();
		artefact1.setAuthor(getIdentity());
		artefact1.setSource(translate("text.artefact.source.info"));
		artefact1.setCollectionDate(new Date());
		artefact1.setSignature(-20);

		vfsTemp = ePFMgr.getArtefactsTempContainer(getIdentity());
		final Step start = new EPCreateTextArtefactStep00(ureq, artefact1, vfsTemp);
		final StepRunnerCallback finish = new EPArtefactWizzardStepCallback(vfsTemp);
		collectStepsCtrl = new StepsMainRunController(ureq, getWindowControl(), start, finish, null, translate("create.text.artefact.wizzard.title"));
		listenTo(collectStepsCtrl);
		getWindowControl().pushAsModalDialog(collectStepsCtrl.getInitialComponent());
	}

	/**
	 * prepare a file artefact and open with wizzard initialized with a special first step for file-artefacts
	 * 
	 * @param ureq
	 */
	private void prepareFileArtefactWizzard(final UserRequest ureq) {
		final EPArtefactHandler<?> artHandler = portfolioModule.getArtefactHandler(FileArtefact.FILE_ARTEFACT_TYPE);
		final AbstractArtefact artefact1 = artHandler.createArtefact();
		artefact1.setAuthor(getIdentity());
		artefact1.setSource(translate("file.artefact.source.info"));
		artefact1.setCollectionDate(new Date());
		artefact1.setSignature(-30);

		vfsTemp = ePFMgr.getArtefactsTempContainer(getIdentity());
		final Step start = new EPCreateFileArtefactStep00(ureq, artefact1, vfsTemp);
		final StepRunnerCallback finish = new EPArtefactWizzardStepCallback(vfsTemp);
		collectStepsCtrl = new StepsMainRunController(ureq, getWindowControl(), start, finish, null, translate("create.file.artefact.wizzard.title"));
		listenTo(collectStepsCtrl);
		getWindowControl().pushAsModalDialog(collectStepsCtrl.getInitialComponent());
	}

	private void prepareNewLiveBlogArtefactWizzard(final UserRequest ureq) {
		final EPArtefactHandler<?> artHandler = portfolioModule.getArtefactHandler(LiveBlogArtefact.TYPE);
		final AbstractArtefact artefact1 = artHandler.createArtefact();
		artefact1.setAuthor(getIdentity());
		artefact1.setCollectionDate(new Date());
		artefact1.setSignature(60); // preset as signed by 60%

		final Step start = new EPCreateLiveBlogArtefactStep00(ureq, artefact1);
		final StepRunnerCallback finish = new EPArtefactWizzardStepCallback(); // no vfsTemp!, blog doesn't need a directory
		collectStepsCtrl = new StepsMainRunController(ureq, getWindowControl(), start, finish, null, translate("create.blog.artefact.wizzard.title"));
		listenTo(collectStepsCtrl);
		getWindowControl().pushAsModalDialog(collectStepsCtrl.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		if (vfsTemp != null) {
			vfsTemp.delete();
		}
	}

}
