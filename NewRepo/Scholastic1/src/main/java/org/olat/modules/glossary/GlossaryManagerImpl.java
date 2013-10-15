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
 * Copyright (c) 1999-2008 at frentix GmbH, Switzerland, http://www.frentix.com
 * <p>
 */
package org.olat.modules.glossary;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Document;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.modules.glossary.GlossaryItemManager;
import org.olat.core.commons.services.search.OlatDocument;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.media.CleanupAfterDeliveryFileMediaResource;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.filter.FilterFactory;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.config.CourseConfig;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.GlossaryResource;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;
import org.olat.repository.RepositoryManager;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceImpl;
import org.olat.resource.OLATResourceManager;
import org.olat.resource.references.ReferenceImpl;
import org.olat.resource.references.ReferenceManager;
import org.olat.search.service.SearchResourceContext;

/**
 * Description:<br>
 * Manager to create, delete etc. glossary learning resources. The OLAT glossary functionality is based on the core framework glossary / textmarker functions.
 * <P>
 * <P>
 * Initial Date: 15.01.2009 <br>
 * 
 * @author Roman Haag, frentix GmbH, roman.haag@frentix.com
 */
public class GlossaryManagerImpl extends GlossaryManager {
	private static final String EXPORT_FOLDER_NAME = "glossary";
	private final ReferenceManager referenceManager;

	/**
	 * [used by spring]
	 */
	private GlossaryManagerImpl(final ReferenceManager referenceManager) {
		INSTANCE = this;
		this.referenceManager = referenceManager;
	}

	/**
	 * Returns the internal glossary folder.
	 * 
	 * @param res
	 * @return
	 */
	@Override
	public OlatRootFolderImpl getGlossaryRootFolder(final OLATResourceable res) {
		final OlatRootFolderImpl resRoot = FileResourceManager.getInstance().getFileResourceRootImpl(res);
		if (resRoot == null) { return null; }
		VFSItem glossaryRoot = resRoot.resolve(INTERNAL_FOLDER_NAME);
		if (glossaryRoot == null) {
			// Glossary has been imported but not yet renamed to the internal folder.
			// This is ugly but no other way to do since the add resource callback
			// somehow does not provide this hook?
			final VFSItem unzipped = resRoot.resolve(FileResourceManager.ZIPDIR);
			if (unzipped == null) {
				// Should not happen, but since we have no unzipped folder we better
				// continue with an empty glossary than crashing
				resRoot.createChildContainer(INTERNAL_FOLDER_NAME);
			} else {
				// We do not use the unzipped folder anymore, this was only for import.
				// We rename it to the internal glossary folder and use it from now on
				unzipped.rename(INTERNAL_FOLDER_NAME);
			}
			glossaryRoot = resRoot.resolve(INTERNAL_FOLDER_NAME);
		}
		return (OlatRootFolderImpl) glossaryRoot;
	}

	/**
	 * Creates a lucene index document for this glossary
	 * 
	 * @param repositoryEntry
	 * @param searchResourceContext
	 * @return Document the index document
	 */
	@Override
	public Document getIndexerDocument(final RepositoryEntry repositoryEntry, final SearchResourceContext searchResourceContext) {
		final GlossaryItemManager gIMgr = GlossaryItemManager.getInstance();
		final VFSContainer glossaryFolder = getGlossaryRootFolder(repositoryEntry.getOlatResource());
		final VFSLeaf glossaryFile = gIMgr.getGlossaryFile(glossaryFolder);
		if (glossaryFile == null) { return null; }
		String glossaryContent = gIMgr.getGlossaryContent(glossaryFolder);
		// strip all html tags
		final Filter htmlTagsFilter = FilterFactory.getHtmlTagsFilter();
		glossaryContent = htmlTagsFilter.filter(glossaryContent);

		// create standard olat index document with this data
		final OlatDocument glossaryDocument = new OlatDocument();
		if (repositoryEntry.getInitialAuthor() != null) {
			glossaryDocument.setAuthor(repositoryEntry.getInitialAuthor());
		}
		if (repositoryEntry.getDisplayname() != null) {
			glossaryDocument.setTitle(repositoryEntry.getDisplayname());
		}
		if (repositoryEntry.getDescription() != null) {
			glossaryDocument.setDescription(htmlTagsFilter.filter(repositoryEntry.getDescription()));
		}
		glossaryDocument.setContent(glossaryContent);
		glossaryDocument.setCreatedDate(repositoryEntry.getCreationDate());
		glossaryDocument.setLastChange(new Date(glossaryFile.getLastModified()));
		glossaryDocument.setResourceUrl(searchResourceContext.getResourceUrl());
		glossaryDocument.setDocumentType(searchResourceContext.getDocumentType());
		glossaryDocument.setCssIcon("o_FileResource-GLOSSARY_icon");
		return glossaryDocument.getLuceneDocument();
	}

	/**
	 * Exports the glossary resource to the given export directory
	 * 
	 * @param glossarySoftkey
	 * @param exportedDataDir
	 * @return
	 */
	@Override
	public boolean exportGlossary(final String glossarySoftkey, final File exportedDataDir) {
		final RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(glossarySoftkey, false);
		if (re == null) { return false; }
		final File fExportBaseDirectory = new File(exportedDataDir, EXPORT_FOLDER_NAME);
		if (!fExportBaseDirectory.mkdir()) { return false; }
		// export properties
		final RepositoryEntryImportExport reImportExport = new RepositoryEntryImportExport(re, fExportBaseDirectory);
		return reImportExport.exportDoExport();
	}

	/**
	 * Export the glossary as a media resource. The resource name is set to the resources display name
	 * 
	 * @param res
	 * @return
	 */
	@Override
	public MediaResource getAsMediaResource(final OLATResourceable res) {
		final RepositoryEntry repoEntry = RepositoryManager.getInstance().lookupRepositoryEntry(res, false);
		final String exportFileName = repoEntry.getDisplayname() + ".zip";
		final File fExportZIP = new File(FolderConfig.getCanonicalTmpDir() + "/" + exportFileName);
		final VFSContainer glossaryRoot = getGlossaryRootFolder(res);
		ZipUtil.zip(glossaryRoot.getItems(), new LocalFileImpl(fExportZIP), false);
		return new CleanupAfterDeliveryFileMediaResource(fExportZIP);
	}

	/**
	 * Creates a glossary resource and creates the necessary folders on disk. The glossary will be placed in the resources _unizipped dir to make import / export easier
	 * 
	 * @return
	 */
	@Override
	public GlossaryResource createGlossary() {
		final GlossaryResource resource = new GlossaryResource();
		final VFSContainer rootContainer = FileResourceManager.getInstance().getFileResourceRootImpl(resource);
		if (rootContainer == null) { return null; }
		if (rootContainer.createChildContainer(INTERNAL_FOLDER_NAME) == null) { return null; }
		final OLATResourceManager rm = OLATResourceManager.getInstance();
		final OLATResource ores = rm.createOLATResourceInstance(resource);
		rm.saveOLATResource(ores);
		return resource;
	}

	/**
	 * The import export data container used for course import
	 * 
	 * @param importDataDir
	 * @return
	 */
	@Override
	public RepositoryEntryImportExport getRepositoryImportExport(final File importDataDir) {
		final File fImportBaseDirectory = new File(importDataDir, EXPORT_FOLDER_NAME);
		return new RepositoryEntryImportExport(fImportBaseDirectory);
	}

	// TODO:RH:gloss change courseconfig, to keep more than 1 single glossary as a list
	/**
	 * @param res glossary to be deleted
	 */
	@Override
	public void deleteGlossary(final OLATResourceable res) {
		// first remove all references
		final List repoRefs = referenceManager.getReferencesTo(res);
		for (final Iterator iter = repoRefs.iterator(); iter.hasNext();) {
			final ReferenceImpl ref = (ReferenceImpl) iter.next();
			if (ref.getUserdata().equals(GLOSSARY_REPO_REF_IDENTIFYER)) {
				// remove the reference from the course configuration
				// TODO:RH:improvement: this should use a callback method or send a general delete
				// event so that the course can take care of this rather than having it
				// here hardcoded
				final OLATResourceImpl courseResource = ref.getSource();
				// ICourse course = CourseFactory.loadCourse(courseResource);
				final ICourse course = CourseFactory.openCourseEditSession(courseResource.getResourceableId());
				final CourseConfig cc = course.getCourseEnvironment().getCourseConfig();
				cc.setGlossarySoftKey(null);
				CourseFactory.setCourseConfig(course.getResourceableId(), cc);
				CourseFactory.closeCourseEditSession(course.getResourceableId(), true);
				// remove reference from the references table
				referenceManager.delete(ref);
			}
		}
		// now remove the resource itself
		FileResourceManager.getInstance().deleteFileResource(res);
	}

	/**
	 * Creates a copy of a glossary
	 * 
	 * @param res
	 * @param ureq
	 * @return the copy
	 */
	@Override
	public OLATResourceable createCopy(final OLATResourceable res, final UserRequest ureq) {
		final FileResourceManager frm = FileResourceManager.getInstance();
		final OLATResourceable copy = frm.createCopy(res, INTERNAL_FOLDER_NAME);
		return copy;
	}

	@Override
	public String archive(final String archivFilePath, final RepositoryEntry repoEntry) {
		final String exportFileName = "del_glossar_" + repoEntry.getOlatResource().getResourceableId() + ".zip";
		final String fullFilePath = archivFilePath + File.separator + exportFileName;
		final File fExportZIP = new File(fullFilePath);
		final VFSContainer glossaryRoot = getGlossaryRootFolder(repoEntry.getOlatResource());
		ZipUtil.zip(glossaryRoot.getItems(), new LocalFileImpl(fExportZIP), true);
		return fullFilePath;
	}

}