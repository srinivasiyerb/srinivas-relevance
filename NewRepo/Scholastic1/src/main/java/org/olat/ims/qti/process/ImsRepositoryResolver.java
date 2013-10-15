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

package org.olat.ims.qti.process;

import java.io.File;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.FileUtils;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.fileresource.FileResourceManager;
import org.olat.ims.qti.QTIChangeLogMessage;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.resource.OLATResource;

/**
 * Initial Date: 04.08.2003
 * 
 * @author Mike Stock Comment:
 */
public class ImsRepositoryResolver implements Resolver {

	public static final String QTI_FILE = "qti.xml";
	public static final String QTI_FIB_AUTOCOMPLETE_JS_FILE = "media/fibautocompl.js";
	public static final String QTI_FIB_AUTOCOMPLETE_CSS_FILE = "media/fibautocompl.css";
	private File fUnzippedDirRoot;
	private String sUnzippedDirRel;
	private final Document doc = null;

	public ImsRepositoryResolver(final Long repositoryEntryKey) {
		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry entry = rm.lookupRepositoryEntry(repositoryEntryKey);
		if (entry != null) {
			final OLATResource ores = entry.getOlatResource();
			init(ores);
		}
	}

	public ImsRepositoryResolver(final OLATResourceable fileResource) {
		init(fileResource);
	}

	private void init(final OLATResourceable fileResource) {
		final FileResourceManager frm = FileResourceManager.getInstance();
		fUnzippedDirRoot = frm.unzipFileResource(fileResource);
		sUnzippedDirRel = frm.getUnzippedDirRel(fileResource);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.olat.ims.qti.process.Resolver#getObjectBank(java.lang.String)
	 */
	@Override
	public Element getObjectBank(final String ident) {
		// with VFS FIXME:pb:c: remove casts to LocalFileImpl and LocalFolderImpl if no longer needed.
		final VFSContainer vfsUnzippedRoot = new LocalFolderImpl(fUnzippedDirRoot);
		final VFSItem vfsQTI = vfsUnzippedRoot.resolve(ident + ".xml");
		// getDocument(..) ensures that InputStream is closed in every case.
		final Document theDoc = QTIHelper.getDocument((LocalFileImpl) vfsQTI);
		// if doc is null an error loading the document occured (IOException, qti.xml does not exist)
		if (theDoc == null) { return null; }
		final Element objectBank = (Element) theDoc.selectSingleNode("questestinterop/objectbank");
		return objectBank;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.olat.ims.qti.process.Resolver#getQTIDocument()
	 */
	@Override
	public Document getQTIDocument() {
		// with VFS FIXME:pb:c: remove casts to LocalFileImpl and LocalFolderImpl if no longer needed.
		final VFSContainer vfsUnzippedRoot = new LocalFolderImpl(fUnzippedDirRoot);
		final VFSItem vfsQTI = vfsUnzippedRoot.resolve(QTI_FILE);
		// getDocument(..) ensures that InputStream is closed in every case.
		final Document theDoc = QTIHelper.getDocument((LocalFileImpl) vfsQTI);
		// if doc is null an error loading the document occured (IOException, qti.xml does not exist)
		return theDoc;
	}

	/**
	 * reads the files in the ../changelog directory, and generates a <code>QTIChangeLogMessage</code> per file.
	 * 
	 * @return qti changelog messages or an empty array if no changelog exists.
	 * @see QTIChangeLogMessage
	 */
	public QTIChangeLogMessage[] getDocumentChangeLog() {
		final VFSContainer dirRoot = new LocalFolderImpl(fUnzippedDirRoot);
		final VFSContainer dirChangelog = (VFSContainer) dirRoot.resolve("changelog");
		if (dirChangelog == null) {
			// no change log
			return new QTIChangeLogMessage[0];
		}
		final List items = dirChangelog.getItems();
		// PRECONDITION: only changelog files in the changelog directory
		final QTIChangeLogMessage[] log = new QTIChangeLogMessage[items.size()];
		String filName;
		String msg;
		int i = 0;
		final java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss");
		for (final Iterator iter = items.iterator(); iter.hasNext();) {
			final VFSLeaf file = (VFSLeaf) iter.next();
			filName = file.getName();
			final String[] parts = filName.split("\\.");
			msg = FileUtils.load(file.getInputStream(), "utf-8");
			try {
				log[i] = new QTIChangeLogMessage(msg, parts[1].equals("all"), formatter.parse(parts[0]).getTime());
				i++;
			} catch (final ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return log;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.olat.ims.qti.process.Resolver#getSection(java.lang.String)
	 */
	@Override
	public Element getSection(final String ident) {
		final Element el_section = (Element) doc.selectSingleNode("questestinterop/assessment/section[@ident='" + ident + "']");
		return el_section;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.olat.ims.qti.process.Resolver#getItem(java.lang.String)
	 */
	@Override
	public Element getItem(final String ident) {
		// ident of item must be "globally unique"(qti...), unique within a qti
		// document
		final Element el_item = (Element) doc.selectSingleNode("//item[@ident='" + ident + "']");
		return el_item;
	}

	@Override
	public String getStaticsBaseURI() {
		return WebappHelper.getServletContextPath() + "/secstatic/qti/" + sUnzippedDirRel;
	}

	/**
	 * @see org.olat.ims.qti.process.Resolver#hasAutocompleteFiles()
	 */
	@Override
	public boolean hasAutocompleteFiles() {
		final VFSContainer vfsUnzippedRoot = new LocalFolderImpl(fUnzippedDirRoot);
		final VFSItem vfsAutocompleteJsItem = vfsUnzippedRoot.resolve(QTI_FIB_AUTOCOMPLETE_JS_FILE);
		if (vfsAutocompleteJsItem != null) {
			final VFSItem vfsAutocompleteCssItem = vfsUnzippedRoot.resolve(QTI_FIB_AUTOCOMPLETE_CSS_FILE);
			if (vfsAutocompleteCssItem != null) { return true; }
		}
		return false;
	}

}
