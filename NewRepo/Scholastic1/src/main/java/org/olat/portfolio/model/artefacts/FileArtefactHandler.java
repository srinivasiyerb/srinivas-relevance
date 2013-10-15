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

package org.olat.portfolio.model.artefacts;

import org.apache.lucene.document.Document;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.commons.modules.bc.meta.MetaInfoFactory;
import org.olat.core.commons.modules.bc.vfs.OlatRootFileImpl;
import org.olat.core.commons.services.search.AbstractOlatDocument;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.OlatRelPathImpl;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.portfolio.EPAbstractHandler;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.ui.artefacts.view.details.FileArtefactDetailsController;
import org.olat.repository.RepositoryManager;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.document.file.FileDocumentFactory;

/**
 * Description:<br>
 * Artefacthandler for collected or uploaded files
 * <P>
 * Initial Date: 25 jun. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, www.frentix.com
 */
public class FileArtefactHandler extends EPAbstractHandler<FileArtefact> {

	/**
	 * @see org.olat.portfolio.EPAbstractHandler#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);

		// en-/disable ePortfolio collecting link in folder component
		// needs to stay here in olat3-context, as olatcore's folder-comp. doesn't
		// know about ePortfolio itself!
		FolderConfig.setEPortfolioAddEnabled(enabled);
	}

	@Override
	public FileArtefact createArtefact() {
		final FileArtefact artefact = new FileArtefact();
		return artefact;
	}

	/**
	 * @see org.olat.portfolio.EPAbstractHandler#prefillArtefactAccordingToSource(org.olat.portfolio.model.artefacts.AbstractArtefact, java.lang.Object)
	 */
	@Override
	public void prefillArtefactAccordingToSource(final AbstractArtefact artefact, final Object source) {
		super.prefillArtefactAccordingToSource(artefact, source);
		if (source instanceof VFSItem) {
			final VFSItem fileSource = (VFSItem) source;
			((FileArtefact) artefact).setFilename(fileSource.getName());
			final MetaInfo meta = MetaInfoFactory.createMetaInfoFor((OlatRelPathImpl) fileSource);
			if (StringHelper.containsNonWhitespace(meta.getTitle())) {
				artefact.setTitle(meta.getTitle());
			} else {
				artefact.setTitle(fileSource.getName());
			}
			if (StringHelper.containsNonWhitespace(meta.getComment())) {
				artefact.setDescription(meta.getComment());
			}
			artefact.setSignature(60);

			final String path = ((OlatRootFileImpl) fileSource).getRelPath();
			final String[] pathElements = path.split("/");

			String finalBusinessPath = null;
			String sourceInfo = null;
			// used to rebuild businessPath and source for a file:
			if (pathElements[1].equals("homes") && pathElements[2].equals(meta.getAuthor())) {
				// from users briefcase
				String lastParts = "/";
				for (int i = 4; i < (pathElements.length - 1); i++) {
					lastParts = lastParts + pathElements[i] + "/";
				}
				sourceInfo = "Home -> " + pathElements[3] + " -> " + lastParts + fileSource.getName();
			} else if (pathElements[3].equals("BusinessGroup")) {
				// out of a businessgroup
				String lastParts = "/";
				for (int i = 5; i < (pathElements.length - 1); i++) {
					lastParts = lastParts + pathElements[i] + "/";
				}
				final BusinessGroup bGroup = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(new Long(pathElements[4]), false);
				if (bGroup != null) {
					sourceInfo = bGroup.getName() + " -> " + lastParts + " -> " + fileSource.getName();
				}
				finalBusinessPath = "[BusinessGroup:" + pathElements[4] + "][toolfolder:0][path=" + lastParts + fileSource.getName() + ":0]";
			} else if (pathElements[4].equals("coursefolder")) {
				// the course folder
				sourceInfo = RepositoryManager.getInstance().lookupDisplayNameByOLATResourceableId(new Long(pathElements[2])) + " -> " + fileSource.getName();

			} else if (pathElements[1].equals("course") && pathElements[3].equals("foldernodes")) {
				// folders inside a course
				sourceInfo = RepositoryManager.getInstance().lookupDisplayNameByOLATResourceableId(new Long(pathElements[2])) + " -> " + pathElements[4] + " -> "
						+ fileSource.getName();
				finalBusinessPath = "[RepositoryEntry:" + pathElements[2] + "][CourseNode:" + pathElements[4] + "]";
			}

			if (sourceInfo == null) {
				// unknown source, keep full path
				sourceInfo = VFSManager.getRealPath(fileSource.getParentContainer()) + "/" + fileSource.getName();
			}

			artefact.setBusinessPath(finalBusinessPath);
			artefact.setSource(sourceInfo);
		}

	}

	/**
	 * @see org.olat.portfolio.EPAbstractHandler#createDetailsController(org.olat.core.gui.UserRequest, org.olat.portfolio.model.artefacts.AbstractArtefact)
	 */
	@Override
	public Controller createDetailsController(final UserRequest ureq, final WindowControl wControl, final AbstractArtefact artefact, final boolean readOnlyMode) {
		return new FileArtefactDetailsController(ureq, wControl, artefact, readOnlyMode);
	}

	@Override
	public String getType() {
		return FileArtefact.FILE_ARTEFACT_TYPE;
	}

	@Override
	protected void getContent(final AbstractArtefact artefact, final StringBuilder sb, final SearchResourceContext context, final EPFrontendManager ePFManager) {
		final FileArtefact fileArtefact = (FileArtefact) artefact;
		final String filename = fileArtefact.getFilename();

		final VFSItem file = ePFManager.getArtefactContainer(artefact).resolve(filename);
		if (file != null && file instanceof VFSLeaf) {
			try {
				final Document doc = FileDocumentFactory.createDocument(context, (VFSLeaf) file);
				final String content = doc.get(AbstractOlatDocument.CONTENT_FIELD_NAME);
				sb.append(content);
			} catch (final Exception e) {
				log.error("", e);
			}
		}
	}
}
