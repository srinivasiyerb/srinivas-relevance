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

package org.olat.core.commons.services.search;

/**
 * Search module config.
 * 
 * @author Christian Guretzki
 */
import java.io.File;
import java.util.List;

import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.configuration.AbstractOLATModule;
import org.olat.core.configuration.PersistedProperties;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;

/**
 * Description:<br>
 * Initial Date: 15.06.200g <br>
 * 
 * @author Christian Guretzki
 */
public class SearchModule extends AbstractOLATModule {
	private static final OLog log = Tracing.createLoggerFor(SearchModule.class);

	// Definitions config parameter names in module-config
	public final static String CONF_INDEX_PATH = "indexPath";
	public final static String CONF_TEMP_INDEX_PATH = "tempIndexPath";
	public final static String CONF_TEMP_SPELL_CHECK_PATH = "tempSpellCheckPath";
	public final static String CONF_GENERATE_AT_STARTUP = "generateIndexAtStartup";
	private static final String CONF_RESTART_INTERVAL = "restartInterval";
	private static final String CONF_INDEX_INTERVAL = "indexInterval";
	private static final String CONF_MAX_HITS = "maxHits";
	private static final String CONF_MAX_RESULTS = "maxResults";
	private static final String CONF_NUMBER_INDEX_WRITER = "numberIndexWriter";
	private static final String CONF_FOLDER_POOL_SIZE = "folderPoolSize";
	private static final String CONF_RESTART_WINDOW_START = "restartWindowStart";
	private static final String CONF_RESTART_WINDOW_END = "restartWindowEnd";
	private static final String CONF_UPDATE_INTERVAL = "updateInterval";
	private static final String CONF_DOCUMENTS_PER_INTERVAL = "documentsPerInterval";
	private static final String CONF_RESTART_DAY_OF_WEEK = "restartDayOfWeek";
	private static final String CONF_PPT_FILE_ENABLED = "pptFileEnabled";
	private static final String CONF_EXCEL_FILE_ENABLED = "excelFileEnabled";
	private static final String CONF_PDF_TEXT_BUFFERING = "pdfTextBuffering";
	private static final String CONF_SPELL_CHECK_ENABLED = "spellCheckEnabled";
	private static final String CONF_TEMP_PDF_TEXT_BUF_PATH = "pdfTextBufferPath";
	private static final String CONF_MAX_FILE_SIZE = "maxFileSize";
	private static final String CONF_RAM_BUFFER_SIZE_MB = "ramBufferSizeMb";
	private static final String CONF_USE_COMPOUND_FILE = "useCompoundFile";

	// Default values
	private static final int DEFAULT_RESTART_INTERVAL = 0;
	private static final int DEFAULT_INDEX_INTERVAL = 0;
	private static final int DEFAULT_MAX_HITS = 1000;
	private static final int DEFAULT_MAX_RESULTS = 100;
	private static final int DEFAULT_NUMBER_INDEX_WRITER = 0;
	private static final int DEFAULT_FOLDER_POOL_SIZE = 0;
	private static final int DEFAULT_RESTART_WINDOW_START = 0;
	private static final int DEFAULT_RESTART_WINDOW_END = 24;
	private static final int DEFAULT_UPDATE_INTERVAL = 0;
	private static final int DEFAULT_DOCUMENTS_PER_INTERVAL = 4;
	private static final int DEFAULT_RESTART_DAY_OF_WEEK = 8;
	private static final String DEFAULT_RAM_BUFFER_SIZE_MB = "48";

	private String fullIndexPath;
	private String fullTempIndexPath;
	private String fullTempSpellCheckPath;
	private long indexInterval;
	private long restartInterval;
	private boolean generateAtStartup;
	private int maxHits;
	private int maxResults;
	private List<String> fileBlackList;

	private int numberIndexWriter;
	private int folderPoolSize;
	private int restartWindowStart;
	private int restartWindowEnd;
	private long updateInterval;
	private int documentsPerInterval;
	private int restartDayOfWeek;
	private boolean pptFileEnabled;
	private boolean excelFileEnabled;
	private boolean pdfTextBuffering;
	private boolean isSpellCheckEnabled;
	private String fullTempPdfTextBufferPath;
	private List<String> fileSizeSuffixes;

	private long maxFileSize;
	private List<Long> repositoryBlackList;

	private double ramBufferSizeMB;
	private boolean useCompoundFile;

	/**
	 * [used by spring]
	 */
	private SearchModule() {}

	/**
	 * [used by spring]
	 * 
	 * @param fileSizeSuffixes
	 */
	public void setFileSizeSuffixes(List<String> fileSizeSuffixes) {
		this.fileSizeSuffixes = fileSizeSuffixes;
	}

	/**
	 * [used by spring]
	 * 
	 * @param fileBlackList
	 */
	public void setFileBlackList(List<String> fileBlackList) {
		this.fileBlackList = fileBlackList;
	}

	/**
	 * [used by spring]
	 * 
	 * @param fileBlackList
	 */
	public void setRepositoryBlackList(List<Long> repositoryBlackList) {
		this.repositoryBlackList = repositoryBlackList;
	}

	/**
	 * Read config-parameter from configuration and store this locally.
	 */
	@Override
	public void init() {
		log.debug("init start...");
		String indexPath = getStringConfigParameter(CONF_INDEX_PATH, "/tmp", false);
		log.debug("init indexPath=" + indexPath);
		String tempIndexPath = getStringConfigParameter(CONF_TEMP_INDEX_PATH, "/tmp", false);
		String tempSpellCheckPath = getStringConfigParameter(CONF_TEMP_SPELL_CHECK_PATH, "/tmp", false);
		String tempPdfTextBufferPath = getStringConfigParameter(CONF_TEMP_PDF_TEXT_BUF_PATH, "/tmp", false);
		fullIndexPath = FolderConfig.getCanonicalTmpDir() + File.separator + indexPath;
		fullTempIndexPath = FolderConfig.getCanonicalTmpDir() + File.separator + tempIndexPath;
		fullTempSpellCheckPath = FolderConfig.getCanonicalTmpDir() + File.separator + tempSpellCheckPath;
		fullTempPdfTextBufferPath = FolderConfig.getCanonicalTmpDir() + File.separator + tempPdfTextBufferPath;

		generateAtStartup = getBooleanConfigParameter(CONF_GENERATE_AT_STARTUP, true);
		restartInterval = getIntConfigParameter(CONF_RESTART_INTERVAL, DEFAULT_RESTART_INTERVAL);
		indexInterval = getIntConfigParameter(CONF_INDEX_INTERVAL, DEFAULT_INDEX_INTERVAL);
		maxHits = getIntConfigParameter(CONF_MAX_HITS, DEFAULT_MAX_HITS);
		maxResults = getIntConfigParameter(CONF_MAX_RESULTS, DEFAULT_MAX_RESULTS);
		numberIndexWriter = getIntConfigParameter(CONF_NUMBER_INDEX_WRITER, DEFAULT_NUMBER_INDEX_WRITER);
		folderPoolSize = getIntConfigParameter(CONF_FOLDER_POOL_SIZE, DEFAULT_FOLDER_POOL_SIZE);
		restartWindowStart = getIntConfigParameter(CONF_RESTART_WINDOW_START, DEFAULT_RESTART_WINDOW_START);
		restartWindowEnd = getIntConfigParameter(CONF_RESTART_WINDOW_END, DEFAULT_RESTART_WINDOW_END);
		updateInterval = getIntConfigParameter(CONF_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL);
		documentsPerInterval = getIntConfigParameter(CONF_DOCUMENTS_PER_INTERVAL, DEFAULT_DOCUMENTS_PER_INTERVAL);
		restartDayOfWeek = getIntConfigParameter(CONF_RESTART_DAY_OF_WEEK, DEFAULT_RESTART_DAY_OF_WEEK);
		pptFileEnabled = getBooleanConfigParameter(CONF_PPT_FILE_ENABLED, true);
		excelFileEnabled = getBooleanConfigParameter(CONF_EXCEL_FILE_ENABLED, true);
		pdfTextBuffering = getBooleanConfigParameter(CONF_PDF_TEXT_BUFFERING, true);
		isSpellCheckEnabled = getBooleanConfigParameter(CONF_SPELL_CHECK_ENABLED, true);
		maxFileSize = Integer.parseInt(getStringConfigParameter(CONF_MAX_FILE_SIZE, "0", false));
		ramBufferSizeMB = Double.parseDouble(getStringConfigParameter(CONF_RAM_BUFFER_SIZE_MB, DEFAULT_RAM_BUFFER_SIZE_MB, false));
		useCompoundFile = getBooleanConfigParameter(CONF_USE_COMPOUND_FILE, false);
	}

	/**
	 * @return Absolute file path for the full-index.
	 */
	public String getFullIndexPath() {
		return fullIndexPath;
	}

	/**
	 * @return Absolute file path for the temporally index used to generate new an index.
	 */
	public String getFullTempIndexPath() {
		return fullTempIndexPath;
	}

	/**
	 * @return Absolute file path for the temporally spell-check index used to specll check search queries.
	 */
	public String getSpellCheckDictionaryPath() {
		return fullTempSpellCheckPath;
	}

	/**
	 * @return TRUE: Generate a full-index after system start.
	 */
	public boolean getGenerateAtStartup() {
		return generateAtStartup;
	}

	/**
	 * @return Time in millisecond between restart generation of a full-index.
	 */
	public long getRestartInterval() {
		return restartInterval;
	}

	/**
	 * @return Sleep time in millisecond between indexing documents.
	 */
	public long getIndexInterval() {
		return indexInterval;
	}

	/**
	 * @return Number of maximal hits before filtering of results for a certain search-query.
	 */
	public int getMaxHits() {
		return maxHits;
	}

	/**
	 * @return Number of maximal displayed results for a certain search-query.
	 */
	public int getMaxResults() {
		return maxResults;
	}

	/**
	 * @return Space seperated list of non indexed files.
	 */
	public List<String> getFileBlackList() {
		return fileBlackList;
	}

	/**
	 * @return Number of IndexWriterWorker in Multithreaded mode.
	 */
	public int getNumberIndexWriter() {
		return numberIndexWriter;
	}

	/**
	 * @return Number of FolderIndexWorker in Multithreaded mode.
	 */
	public int getFolderPoolSize() {
		return folderPoolSize;
	}

	/**
	 * @return Start hour for restart-window.
	 */
	public int getRestartWindowStart() {
		return restartWindowStart;
	}

	/**
	 * @return End hour for restart-window.
	 */
	public int getRestartWindowEnd() {
		return restartWindowEnd;
	}

	/**
	 * @return Time in millisecond between running updater.
	 */
	public long getUpdateInterval() {
		return updateInterval;
	}

	/**
	 * @return Number of indexed documents before sleeping during indexing.
	 */
	public int getDocumentsPerInterval() {
		return documentsPerInterval;
	}

	/**
	 * @return Restart only at this da of the week.
	 */
	public int getRestartDayOfWeek() {
		return restartDayOfWeek;
	}

	/**
	 * @return TRUE: index Power-Point-files.
	 */
	public boolean getPptFileEnabled() {
		return pptFileEnabled;
	}

	/**
	 * @return TRUE: index Excel-files.
	 */
	public boolean getExcelFileEnabled() {
		return excelFileEnabled;
	}

	/**
	 * @return TRUE: store a temporary text file with content of extracted PDF text.
	 */
	public boolean getPdfTextBuffering() {
		return pdfTextBuffering;
	}

	/**
	 * @return TRUE: Spell-checker is enabled.
	 */
	public boolean getSpellCheckEnabled() {
		return isSpellCheckEnabled;
	}

	public String getPdfTextBufferPath() {
		return fullTempPdfTextBufferPath;
	}

	public List<String> getFileSizeSuffixes() {
		return fileSizeSuffixes;
	}

	public long getMaxFileSize() {
		return maxFileSize;
	}

	public List<Long> getRepositoryBlackList() {
		return repositoryBlackList;
	}

	@Override
	protected void initDefaultProperties() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void initFromChangedProperties() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPersistedProperties(PersistedProperties persistedProperties) {
		this.moduleConfigProperties = persistedProperties;
	}

	public double getRAMBufferSizeMB() {
		return ramBufferSizeMB;
	}

	public boolean getUseCompoundFile() {
		return useCompoundFile;
	}

}
