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
package org.olat.modules.fo.portfolio;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.download.DownloadComponent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.filters.VFSItemExcludePrefixFilter;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;

/**
 * Description:<br>
 * Show the specific part of the ForumArtefact
 * <P>
 * Initial Date: 11 oct. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class ForumArtefactDetailsController extends BasicController {

	private final VelocityContainer vC;
	protected static final String[] ATTACHMENT_EXCLUDE_PREFIXES = new String[] { ".nfs", ".CVS", ".DS_Store" }; // see: MessageEditController.ATTACHMENT_EXCLUDE_PREFIXES

	public ForumArtefactDetailsController(final UserRequest ureq, final WindowControl wControl, final AbstractArtefact artefact) {
		super(ureq, wControl);
		final ForumArtefact fArtefact = (ForumArtefact) artefact;
		vC = createVelocityContainer("messageDetails");
		final EPFrontendManager ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		vC.contextPut("text", ePFMgr.getArtefactFullTextContent(fArtefact));
		final VFSContainer artContainer = ePFMgr.getArtefactContainer(artefact);
		if (artContainer != null && artContainer.getItems().size() != 0) {
			final List<VFSItem> attachments = new ArrayList<VFSItem>(artContainer.getItems(new VFSItemExcludePrefixFilter(ATTACHMENT_EXCLUDE_PREFIXES)));
			int i = 1; // vc-shift!
			for (final VFSItem vfsItem : attachments) {
				final VFSLeaf file = (VFSLeaf) vfsItem;
				// DownloadComponent downlC = new DownloadComponent("download"+i, file);
				final DownloadComponent downlC = new DownloadComponent("download" + i, file, file.getName() + " (" + String.valueOf(file.getSize() / 1024) + " KB)",
						null, CSSHelper.createFiletypeIconCssClassFor(file.getName()));
				vC.put("download" + i, downlC);
				i++;
			}
			vC.contextPut("attachments", attachments);
			vC.contextPut("hasAttachments", true);
		}

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
}
