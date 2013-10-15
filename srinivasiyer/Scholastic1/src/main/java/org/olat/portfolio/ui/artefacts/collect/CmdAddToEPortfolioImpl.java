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

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.commands.CmdAddToEPortfolio;
import org.olat.core.commons.modules.bc.commands.FolderCommandHelper;
import org.olat.core.commons.modules.bc.commands.FolderCommandStatus;
import org.olat.core.commons.modules.bc.components.FolderComponent;
import org.olat.core.commons.modules.bc.components.ListRenderer;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.VFSItem;
import org.olat.portfolio.EPArtefactHandler;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.artefacts.FileArtefact;

/**
 * Description:<br>
 * wrapper for the old folder-architecture to handle clicks on ePortfolio-add in folder
 * <P>
 * Initial Date: 03.09.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class CmdAddToEPortfolioImpl extends BasicController implements CmdAddToEPortfolio {

	private int status;
	private VFSItem currentItem;
	private final PortfolioModule portfolioModule;
	private Controller collectStepsCtrl;

	public CmdAddToEPortfolioImpl(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		portfolioModule = (PortfolioModule) CoreSpringFactory.getBean("portfolioModule");
	}

	/**
	 * might return NULL!, if item clicked was removed meanwhile or if portfolio is disabled or if only the folder-artefact-handler is disabled.
	 * 
	 * @see org.olat.core.commons.modules.bc.commands.FolderCommand#execute(org.olat.core.commons.modules.bc.components.FolderComponent, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, org.olat.core.gui.translator.Translator)
	 */
	@Override
	public Controller execute(final FolderComponent folderComponent, final UserRequest ureq, final WindowControl wControl, final Translator translator) {
		final String pos = ureq.getParameter(ListRenderer.PARAM_EPORT);
		if (!StringHelper.containsNonWhitespace(pos)) {
			// somehow parameter did not make it to us
			status = FolderCommandStatus.STATUS_FAILED;
			getWindowControl().setError(translator.translate("failed"));
			return null;
		}

		status = FolderCommandHelper.sanityCheck(wControl, folderComponent);
		if (status == FolderCommandStatus.STATUS_SUCCESS) {
			currentItem = folderComponent.getCurrentContainerChildren().get(Integer.parseInt(pos));
			status = FolderCommandHelper.sanityCheck2(wControl, folderComponent, ureq, currentItem);
		}
		if (status == FolderCommandStatus.STATUS_FAILED) { return null; }

		final EPArtefactHandler<?> artHandler = portfolioModule.getArtefactHandler(FileArtefact.FILE_ARTEFACT_TYPE);
		final AbstractArtefact artefact = artHandler.createArtefact();
		artHandler.prefillArtefactAccordingToSource(artefact, currentItem);
		artefact.setAuthor(getIdentity());

		collectStepsCtrl = new ArtefactWizzardStepsController(ureq, wControl, artefact, currentItem.getParentContainer());

		return collectStepsCtrl;
	}

	/**
	 * @see org.olat.core.commons.modules.bc.commands.FolderCommand#getStatus()
	 */
	@Override
	public int getStatus() {
		return status;
	}

	/**
	 * @see org.olat.core.commons.modules.bc.commands.FolderCommand#runsModal()
	 */
	@Override
	public boolean runsModal() {
		return true;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@SuppressWarnings("unused")
	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		// none
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		if (collectStepsCtrl != null) {
			collectStepsCtrl.dispose();
			collectStepsCtrl = null;
		}
	}

}
