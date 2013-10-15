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

import java.util.List;

import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.vfs.VFSItem;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.Message;
import org.olat.portfolio.EPAbstractHandler;
import org.olat.portfolio.model.artefacts.AbstractArtefact;

/**
 * Description:<br>
 * The ArtefactHandler for Forums
 * <P>
 * Initial Date: 11.06.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class ForumArtefactHandler extends EPAbstractHandler<ForumArtefact> {

	/**
	 * @see org.olat.portfolio.EPAbstractHandler#prefillArtefactAccordingToSource(org.olat.portfolio.model.artefacts.AbstractArtefact, java.lang.Object)
	 */
	@Override
	public void prefillArtefactAccordingToSource(final AbstractArtefact artefact, final Object source) {
		super.prefillArtefactAccordingToSource(artefact, source);
		if (source instanceof OLATResourceable) {
			final OLATResourceable ores = (OLATResourceable) source;
			final ForumManager fMgr = ForumManager.getInstance();
			final Message fm = fMgr.loadMessage(ores.getResourceableId());
			final String thread = fm.getThreadtop() != null ? fm.getThreadtop().getTitle() + " - " : "";
			artefact.setTitle(thread + fm.getTitle());

			final OlatRootFolderImpl msgContainer = fMgr.getMessageContainer(fm.getForum().getKey(), fm.getKey());
			if (msgContainer != null) {
				final List<VFSItem> foAttach = msgContainer.getItems();
				if (foAttach.size() != 0) {
					artefact.setFileSourceContainer(msgContainer);
				}
			}

			artefact.setSignature(70);
			artefact.setFulltextContent(fm.getBody());
		}
	}

	@Override
	public ForumArtefact createArtefact() {
		final ForumArtefact artefact = new ForumArtefact();
		return artefact;
	}

	@Override
	public String getType() {
		return ForumArtefact.FORUM_ARTEFACT_TYPE;
	}

	@Override
	public Controller createDetailsController(final UserRequest ureq, final WindowControl wControl, final AbstractArtefact artefact, final boolean readOnlyMode) {
		return new ForumArtefactDetailsController(ureq, wControl, artefact);
	}
}