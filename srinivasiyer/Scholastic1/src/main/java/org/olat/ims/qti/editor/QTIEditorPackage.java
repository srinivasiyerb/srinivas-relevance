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

package org.olat.ims.qti.editor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.FileUtils;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.NamedContainerImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.xml.XMLParser;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.FileResource;
import org.olat.ims.qti.QTIChangeLogMessage;
import org.olat.ims.qti.editor.beecom.objects.Assessment;
import org.olat.ims.qti.editor.beecom.objects.Metadata;
import org.olat.ims.qti.editor.beecom.objects.QTIDocument;
import org.olat.ims.qti.editor.beecom.objects.Section;
import org.olat.ims.qti.editor.beecom.parser.ParserManager;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.ims.qti.process.ImsRepositoryResolver;
import org.olat.ims.resources.IMSEntityResolver;

/**
 * Initial Date: 27.08.2003
 * 
 * @author Mike Stock
 */
public class QTIEditorPackage {

	public static final String FOLDERNAMEFOR_CHANGELOG = "changelog";
	/*
	 * Files are store in tmp directory as tmp/qtieditor/{login}/{repositoryEntryID}/ extracted from the repositoryEntry
	 */
	private static final String SERIALIZED_QTI_DOCUMENT = "__qti.xstream.xml";
	private static final String CURRENT_HISTORY = "__qti.history.xml";

	private Identity identity = null;
	private FileResource fileResource = null;
	private String packageSubDir = null;
	private File packageDir = null;
	private QTIDocument qtiDocument = null;
	private boolean resumed = false;
	private static OutputFormat outformat;
	private final OLog log = Tracing.createLoggerFor(this.getClass());
	private Translator translator;

	static {
		outformat = OutputFormat.createPrettyPrint();
		outformat.setEncoding("UTF-8");
	}

	/**
	 * @param identity
	 * @param fileResource
	 */
	public QTIEditorPackage(final Identity identity, final FileResource fileResource, final Translator translator) {
		this.identity = identity;
		this.fileResource = fileResource;
		this.translator = translator;
		init();
	}

	/**
	 * Create a new qtipackage.
	 * 
	 * @param title
	 * @param type
	 * @param locale
	 */
	public QTIEditorPackage(final String title, final String type, final Locale locale) {
		// create new qti document
		translator = new PackageTranslator("org.olat.ims.qti", locale);
		qtiDocument = new QTIDocument();
		final Assessment assessment = QTIEditHelper.createAssessment(title, type);
		qtiDocument.setAssessment(assessment);
		final Section section = QTIEditHelper.createSection(translator);
		final List sectionList = new ArrayList();
		sectionList.add(section);
		assessment.setSections(sectionList);
		final List itemList = new ArrayList();
		itemList.add(QTIEditHelper.createSCItem(translator));
		section.setItems(itemList);
		// initialize package
		packageSubDir = CodeHelper.getGlobalForeverUniqueID();
		packageDir = new File(getTmpBaseDir(), packageSubDir);
		packageDir.mkdirs();
		getMediaBaseDir().mkdirs();
		getChangelogBaseDir().mkdirs();
	}

	private QTIEditorPackage() {
		//
	}

	/**
	 * Return the underlying resourceable.
	 * 
	 * @return OLATResourceable
	 */
	public OLATResourceable getRepresentingResourceable() {
		return fileResource;
	}

	private void init() {
		packageSubDir = getPackageSubDir(identity, fileResource);
		packageDir = new File(getTmpBaseDir(), packageSubDir);
		packageDir.mkdirs();
		getMediaBaseDir().mkdirs();
		getChangelogBaseDir().mkdirs();
	}

	/**
	 * Returns the sub directory within the base temp directory for this package.
	 * 
	 * @param i
	 * @param fr
	 * @return Sub directory relative to temporary base directory.
	 */
	private String getPackageSubDir(final Identity i, final FileResource fr) {
		return i.getName() + "/" + fr.getResourceableId();
	}

	/**
	 * Get the temporary root dir where all packages are located.
	 * 
	 * @return The editor's package temp base directory.
	 */
	public static File getTmpBaseDir() {
		return new File(WebappHelper.getUserDataRoot() + "/tmp/qtieditor/");
	}

	/**
	 * Return the media base URL for delivering media of this package.
	 * 
	 * @return Complete media base URL.
	 */
	public String getMediaBaseURL() {
		return WebappHelper.getServletContextPath() + "/secstatic/qtieditor/" + packageSubDir;
	}

	/**
	 * Returns the package's media directory.
	 * 
	 * @return the media directory
	 */
	public File getMediaBaseDir() {
		return new File(packageDir, "/media");
	}

	public VFSContainer getBaseDir() {
		final NamedContainerImpl namedBaseDir = new NamedContainerImpl(translator.translate("qti.basedir.displayname"), new LocalFolderImpl(packageDir));
		return namedBaseDir;
	}

	/**
	 * Returns the package's change log directory
	 * 
	 * @return change log directory
	 */
	public File getChangelogBaseDir() {
		return new File(packageDir, "/" + FOLDERNAMEFOR_CHANGELOG);
	}

	/**
	 * Unzip package into temporary directory.
	 * 
	 * @return true if successfull, false otherwise
	 */
	private boolean unzipPackage() {
		final FileResourceManager frm = FileResourceManager.getInstance();
		final File fPackageZIP = frm.getFileResource(fileResource);
		return ZipUtil.unzip(fPackageZIP, packageDir);
	}

	/**
	 * @return Reutrns the QTIDocument structure
	 */
	public QTIDocument getQTIDocument() {
		if (qtiDocument == null) {
			if (hasSerializedQTIDocument()) {
				qtiDocument = loadSerializedQTIDocument();
				resumed = true;
			} else {
				unzipPackage();
				final Document doc = loadQTIDocument();
				if (doc != null) {
					final ParserManager parser = new ParserManager();
					qtiDocument = (QTIDocument) parser.parse(doc);
					// grab assessment type
					final Metadata meta = qtiDocument.getAssessment().getMetadata();
					final String assessType = meta.getField(AssessmentInstance.QMD_LABEL_TYPE);
					if (assessType != null) {
						qtiDocument.setSurvey(assessType.equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY));
					}
					resumed = false;
				} else {
					qtiDocument = null;
				}
			}
		}
		return qtiDocument;
	}

	/**
	 * @return True upon success, false otherwise.
	 */
	public boolean savePackageToRepository() {
		final FileResourceManager frm = FileResourceManager.getInstance();
		final File tmpZipFile = new File(FolderConfig.getCanonicalTmpDir() + "/" + CodeHelper.getRAMUniqueID() + ".zip");
		// first save complete ZIP package to repository
		if (!savePackageTo(tmpZipFile)) { return false; }
		// move file from temp to repository root and rename
		final File fRepositoryZip = frm.getFileResource(fileResource);
		if (!FileUtils.moveFileToDir(tmpZipFile, frm.getFileResourceRoot(fileResource))) {
			tmpZipFile.delete();
			return false;
		}
		fRepositoryZip.delete();
		new File(frm.getFileResourceRoot(fileResource), tmpZipFile.getName()).renameTo(fRepositoryZip);
		// delete old unzip content. If the repository entry gets called in the meantime,
		// the package will get unzipped again.
		tmpZipFile.delete();
		frm.deleteUnzipContent(fileResource);
		// to be prepared for the next start, unzip right now.
		return (frm.unzipFileResource(fileResource) != null);
	}

	/**
	 * save the change log in the changelog folder, must be called before savePackageToRepository.
	 * 
	 * @param changelog
	 */
	public void commitChangelog(final QTIChangeLogMessage clm) {
		final Date tmp = new Date(clm.getTimestmp());
		final java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss");
		String filname = formatter.format(tmp);
		filname += clm.isPublic() ? ".all" : ".group";
		filname += ".txt";
		final File changelogFile = new File(getChangelogBaseDir(), filname);
		FileUtils.save(changelogFile, clm.getLogMessage(), "utf-8");
	}

	/**
	 * Package the package to the given file.
	 * 
	 * @param fOut
	 * @return True upon success.
	 */
	public boolean savePackageTo(final File fOut) {
		saveQTIDocument(qtiDocument.getDocument());
		final Set files = new HashSet(3);
		files.add(ImsRepositoryResolver.QTI_FILE);
		files.add("media");
		files.add("changelog");
		return ZipUtil.zip(files, packageDir, fOut, false);
	}

	/**
	 * Remove the media files specified in the input set (removable contains filenames)
	 * 
	 * @param removable
	 */
	public void removeMediaFiles(final Set<String> removable) {
		final LocalFolderImpl mediaFolder = new LocalFolderImpl(new File(packageDir, "media"));
		final List<VFSItem> allMedia = mediaFolder.getItems();
		QTIEditHelper.removeUnusedMedia(removable, allMedia);
	}

	/**
	 * Saves a serialized versionof the underlying QTIDocument.
	 */
	public void serializeQTIDocument() {
		XStreamHelper.writeObject(new File(packageDir, SERIALIZED_QTI_DOCUMENT), qtiDocument);
	}

	private boolean hasSerializedQTIDocument() {
		return new File(packageDir, SERIALIZED_QTI_DOCUMENT).exists();
	}

	private QTIDocument loadSerializedQTIDocument() {
		return (QTIDocument) XStreamHelper.readObject(new File(packageDir, SERIALIZED_QTI_DOCUMENT));
	}

	/**
	 * save a temporary file with the change history
	 * 
	 * @param history
	 */
	public void serializeChangelog(final Map history) {
		XStreamHelper.writeObject(new File(packageDir, CURRENT_HISTORY), history);
	}

	/**
	 * check if a serialized change log exists
	 * 
	 * @return
	 */
	public boolean hasSerializedChangelog() {
		return new File(packageDir, CURRENT_HISTORY).exists();
	}

	/**
	 * resume the change log from the temporary file
	 * 
	 * @return
	 */
	public Map loadChangelog() {
		return (Map) XStreamHelper.readObject(new File(packageDir, CURRENT_HISTORY));
	}

	/**
	 * Load a document from file.
	 * 
	 * @return the loaded document or null if loading failed
	 */
	private Document loadQTIDocument() {
		File fIn = null;
		FileInputStream in = null;
		BufferedInputStream bis = null;
		Document doc = null;
		try {
			fIn = new File(packageDir, ImsRepositoryResolver.QTI_FILE);
			in = new FileInputStream(fIn);
			bis = new BufferedInputStream(in);
			final XMLParser xmlParser = new XMLParser(new IMSEntityResolver());
			doc = xmlParser.parse(bis, true);
		} catch (final Exception e) {
			log.warn("Exception when parsing input QTI input stream for " + fIn != null ? fIn.getAbsolutePath() : "qti.xml", e);
			return null;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (bis != null) {
					bis.close();
				}
			} catch (final Exception e) {
				throw new OLATRuntimeException(this.getClass(), "Could not close input file stream ", e);
			}
		}
		return doc;
	}

	/**
	 * SaveQTIDocument in temporary folder.
	 * 
	 * @param doc
	 * @return true: save was successful, false otherwhise
	 */
	private boolean saveQTIDocument(final Document doc) {
		File fOut = null;
		OutputStream out = null;
		try {
			fOut = new File(packageDir, ImsRepositoryResolver.QTI_FILE);
			out = new FileOutputStream(fOut);
			final XMLWriter writer = new XMLWriter(out, outformat);
			writer.write(doc);
			writer.close();
		} catch (final Exception e) {
			throw new OLATRuntimeException(this.getClass(), "Exception when saving QTI document to " + fOut != null ? fOut.getAbsolutePath() : "qti.xml", e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (final IOException e1) {
					throw new OLATRuntimeException(this.getClass(), "Could not close output file stream ", e1);
				}
			}
		}
		return true;
	}

	/**
	 * Cleanup any temporary directory for this qti file only.
	 */
	public void cleanupTmpPackageDir() {
		FileUtils.deleteDirsAndFiles(packageDir, true, true);
	}

	/**
	 * @return True if package has been resumed.
	 */
	public boolean isResumed() {
		return resumed;
	}

	/**
	 * @param b
	 */
	public void setResumed(final boolean b) {
		resumed = b;
	}

}