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

package org.olat.course.statistic;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.internet.AddressException;

import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.ExportUtil;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.mail.Emailer;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.course.statistic.export.ICourseLogExporter;

/**
 * Description:<br>
 * The Manager exports the course log files and statistic file as zip archive.
 * <P>
 * Initial Date: 19.11.2009 <br>
 * 
 * @author bja
 */
public class ExportManager extends BasicManager {

	/** the logging object used in this class **/
	private static final OLog log_ = Tracing.createLoggerFor(ExportManager.class);

	/** ExportManager is a singleton, configured by spring **/
	private static ExportManager INSTANCE;

	/**
	 * filename used to store courseauthor's activities (personalized)
	 */
	private static final String FILENAME_ADMIN_LOG = "course_admin_log.csv";
	/**
	 * filename used to store all user's activities (personalized) in the course only visible for OLAT-admins
	 */
	private static final String FILENAME_USER_LOG = "course_user_log.csv";
	/**
	 * filename used to store all user's activities (anonymized) in the course
	 */
	private static final String FILENAME_STATISTIC_LOG = "course_statistic_log.csv";
	/**
	 * filename course statistic
	 */
	private static final String FILENAME_COURSE_STATISTIC = "course_statistic.csv";
	/**
	 * zip filename substring (archive log files)
	 */
	public static final String COURSE_LOG_FILES = "CourseLogFiles";
	/**
	 * zip filename substring (statistic)
	 */
	public static final String COURSE_STATISTIC = "CourseStatistic";

	/** injected via spring **/
	private ICourseLogExporter courseLogExporter;

	/** created via spring **/
	private ExportManager() {
		INSTANCE = this;
	}

	/** injected via spring **/
	public void setCourseLogExporter(final ICourseLogExporter courseLogExporter) {
		this.courseLogExporter = courseLogExporter;
	}

	/**
	 * @return Singleton.
	 */
	public static final ExportManager getInstance() {
		if (INSTANCE == null) { throw new IllegalStateException("ExportManager bean not created via spring. Configuration error!"); }
		return INSTANCE;
	}

	/**
	 * Archives the course log files
	 * 
	 * @param oresID
	 * @param exportDir
	 * @param begin
	 * @param end
	 * @param adminLog
	 * @param userLog
	 * @param statisticLog
	 * @param charset
	 * @param locale
	 * @param email
	 */
	public void archiveCourseLogFiles(final Long oresID, final String exportDir, final Date begin, final Date end, final boolean adminLog, final boolean userLog,
			final boolean statisticLog, final String charset, final Locale locale, final String email) {

		final String zipName = ExportUtil.createFileNameWithTimeStamp(ExportManager.COURSE_LOG_FILES, "zip");
		final Date date = new Date();
		final String tmpDirName = oresID + "-" + date.getTime();
		final VFSContainer tmpDirVFSContainer = new OlatRootFolderImpl(new File(FolderConfig.getRelativeTmpDir(), tmpDirName).getPath(), null);
		final File tmpDir = new File(new File(FolderConfig.getCanonicalRoot(), FolderConfig.getRelativeTmpDir()), tmpDirName);

		final List<VFSItem> logFiles = new ArrayList<VFSItem>();
		if (adminLog) {
			logFiles.add(createLogFile(oresID, begin, end, charset, tmpDirVFSContainer, tmpDir, FILENAME_ADMIN_LOG, true, false));
		}
		if (userLog) {
			logFiles.add(createLogFile(oresID, begin, end, charset, tmpDirVFSContainer, tmpDir, FILENAME_USER_LOG, false, false));
		}
		if (statisticLog) {
			logFiles.add(createLogFile(oresID, begin, end, charset, tmpDirVFSContainer, tmpDir, FILENAME_STATISTIC_LOG, false, true));
		}

		saveFile(exportDir, zipName, tmpDirVFSContainer, logFiles, email, "email.archive", locale);
	}

	/**
	 * Create the actual log file.
	 * <p>
	 * Note: vfsContainer and dir must point to the very same directory. This is necessary to allow converting of the resulting file into a VFSLeaf (which is required by
	 * the zip util class) and to have the absolute path name of the vfsContainer for the sql export (there's no getter on the VFSContainer for the absolute path - this
	 * is core reason why). This 'hack' is not very nice but we'll live with it for the moment.
	 * <p>
	 * 
	 * @param oresID
	 * @param begin
	 * @param end
	 * @param charset
	 * @param vfsContainer
	 * @param dir
	 * @param filename
	 * @param resourceAdminAction
	 * @param anonymize
	 * @return
	 */
	private VFSItem createLogFile(final Long oresID, final Date begin, final Date end, final String charset, final VFSContainer vfsContainer, final File dir,
			final String filename, final boolean resourceAdminAction, final boolean anonymize) {

		final File outFile = new File(dir, filename);
		// trigger the course log exporter - it will store the file to outFile
		log_.info("createLogFile: start exporting course log file " + outFile.getAbsolutePath());
		courseLogExporter.exportCourseLog(outFile, charset, oresID, begin, end, resourceAdminAction, anonymize);
		log_.info("createLogFile: finished exporting course log file " + outFile.getAbsolutePath());
		final VFSItem logFile = vfsContainer.resolve(filename);
		if (logFile == null) {
			log_.warn("createLogFile: could not resolve " + filename);
		}
		return logFile;
	}

	private void saveFile(final String targetDir, final String fileName, final VFSContainer tmpDir, final List<VFSItem> files, final String email,
			final String emailI18nSubkey, final Locale locale) {
		final File file = new File(targetDir, fileName);
		final VFSLeaf exportFile = new LocalFileImpl(file);
		if (!ZipUtil.zip(files, exportFile, true)) {
			// cleanup zip file
			exportFile.delete();
		} else {
			// success
			sendEMail(email, locale, emailI18nSubkey);
		}
		removeTemporaryFolder(tmpDir);
	}

	private void sendEMail(final String email, Locale locale, final String emailI18nSubkey) {
		if (email == null || email.length() == 0) { return; }
		if (locale == null) {
			locale = I18nManager.getInstance().getCurrentThreadLocale();
		}
		final Translator translator = new PackageTranslator(this.getClass().getPackage().getName(), locale);

		final Emailer emailer = new Emailer(locale);
		final String subject = translator.translate(emailI18nSubkey + ".subject");
		final String body = translator.translate(emailI18nSubkey + ".body");
		try {
			emailer.sendEmail(email, subject, body);
		} catch (final AddressException e) {
			log_.error("Error sending information email to user that file was saved successfully.", e);
		} catch (final SendFailedException e) {
			log_.error("Error sending information email to user that file was saved successfully.", e);
		} catch (final MessagingException e) {
			log_.error("Error sending information email to user that file was saved successfully.", e);
		}
	}

	/*
	 * private VFSItem createCourseStatisticFile(Long resourceableId, VFSContainer tmpDir, Date begin, Date end, List<ExtendedCondition> conditions, String groupByKey,
	 * boolean hasANDConnection, String charset, Locale locale) { VFSLeaf statisticFile = tmpDir.createChildLeaf(FILENAME_COURSE_STATISTIC); if(statisticFile == null) {
	 * statisticFile = (VFSLeaf) tmpDir.resolve(FILENAME_COURSE_STATISTIC); } // Translator Translator translator = new
	 * PackageTranslator(this.getClass().getPackage().getName(), locale); IStatistic simpleCourseStatistic = new SimpleCourseStatistic(begin, end, conditions, groupByKey,
	 * hasANDConnection, translator, resourceableId); FileUtils.save(statisticFile.getOutputStream(true), simpleCourseStatistic.getResult(), charset); return
	 * statisticFile; } public void createCourseStatistic(Long resourceableId, String targetDir, Date begin, Date end, List<ExtendedCondition> conditions, String
	 * groupByKey, boolean hasANDConnection, String charset, Locale locale, String email) { String zipName =
	 * ExportUtil.createFileNameWithTimeStamp(ExportManager.COURSE_STATISTIC, "zip"); Date date = new Date(); String tmpDirName = resourceableId + "-" + date.getTime();
	 * VFSContainer tmpDir = new OlatRootFolderImpl(FolderConfig.getRelativeTmpDir() + File.separator + tmpDirName, null); List<VFSItem> statisticFiles = new
	 * ArrayList<VFSItem>(); statisticFiles.add(createCourseStatisticFile(resourceableId, tmpDir, begin, end, conditions, groupByKey, hasANDConnection, charset, locale));
	 * saveFile(targetDir, zipName, tmpDir, statisticFiles, email, "email.statistic", locale); }
	 */

	public File getLatestCourseStatisticFile(final String targetDir) {
		final File courseStatisticsDir = new File(targetDir);
		final File[] exportedCourseStatisticZipFiles = courseStatisticsDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return name.startsWith(ExportManager.COURSE_STATISTIC) && name.endsWith(".zip");
			}

		});
		if (exportedCourseStatisticZipFiles == null || exportedCourseStatisticZipFiles.length == 0) { return null; }

		if (exportedCourseStatisticZipFiles.length == 1) { return exportedCourseStatisticZipFiles[0]; }

		// we have more than one - return the newest
		File newestFile = exportedCourseStatisticZipFiles[0];
		for (int i = 0; i < exportedCourseStatisticZipFiles.length; i++) {
			final File file = exportedCourseStatisticZipFiles[i];
			if (file.lastModified() > newestFile.lastModified()) {
				newestFile = file;
			}
		}

		return newestFile;
	}

	/**
	 * remove temporary folder
	 * 
	 * @param tmpDir
	 */
	private void removeTemporaryFolder(final VFSContainer tmpDir) {
		tmpDir.delete();
	}

}
