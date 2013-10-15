package org.olat.upgrade;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.configuration.Initializable;
import org.olat.core.gui.control.Event;
import org.olat.core.logging.StartupException;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.event.FrameworkStartedEvent;
import org.olat.core.util.event.FrameworkStartupEventChannel;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.xml.XStreamHelper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Description:<br>
 * Class to execute upgrade code during system startup. The idea is to have a place in the code where you can manage your migration code that is necessary because of new
 * code or because of buggy old code that left some dead stuff in the database or the fileSystem.
 * <P>
 * Initial Date: 11.09.2008 <br>
 * 
 * @author guido
 */

public abstract class UpgradeManager extends BasicManager implements Initializable, GenericEventListener {

	static final String INSTALLED_UPGRADES_XML = "installed_upgrades.xml";
	static final String SYSTEM_DIR = "system";

	List<OLATUpgrade> upgrades;
	Map<String, UpgradeHistoryData> upgradesHistories;
	private UpgradesDefinitions upgradesDefinitions;
	protected DriverManagerDataSource dataSource;
	private boolean needsUpgrade = true;
	private boolean autoUpgradeDatabase = true;

	/**
	 * [used by spring]
	 * 
	 * @param dataSource
	 */
	public void setDataSource(final DriverManagerDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public DriverManagerDataSource getDataSource() {
		return dataSource;
	}

	/**
	 * [used by spring]
	 * 
	 * @param upgradesDefinitions
	 */
	public void setUpgradesDefinitions(final UpgradesDefinitions upgradesDefinitions) {
		this.upgradesDefinitions = upgradesDefinitions;
	}

	/**
	 * [used by spring]
	 * 
	 * @param autoUpgradeDatabase
	 */
	public void setAutoUpgradeDatabase(final boolean autoUpgradeDatabase) {
		this.autoUpgradeDatabase = autoUpgradeDatabase;
	}

	/**
	 * Initialize the upgrade manager: get all upgrades from the configuration file and load the upgrade history from the olatdata directory
	 */
	@Override
	public void init() {
		// register for framework starup event
		FrameworkStartupEventChannel.registerForStartupEvent(this);
		// load upgrades using spring framework
		upgrades = upgradesDefinitions.getUpgrades();
		// load history of previous upgrades using xstream
		initUpgradesHistories();
		if (needsUpgrade) {
			if (autoUpgradeDatabase) {
				runAlterDbStatements();
			} else {
				logInfo("Auto upgrade of the database is disabled. Make sure you do it manually by applying the "
						+ "alter*.sql scripts and adding an entry to system/installed_upgrades.xml file.");
			}
			doPreSystemInitUpgrades();

			// post system init task are triggered by an event
			DBFactory.getInstance().commitAndCloseSession();
		}
	}

	/**
	 * Execute alter db sql statements
	 */
	public abstract void runAlterDbStatements();

	/**
	 * Execute the pre system init code of all upgrades in the order as they were configured in the configuration file
	 */
	public abstract void doPreSystemInitUpgrades();

	/**
	 * Execute the post system init code of all upgrades in the order as they were configured in the configuration file
	 */
	public abstract void doPostSystemInitUpgrades();

	/**
	 * @param version identifier of UpgradeHistoryData
	 * @return UpgradeHistoryData of the given version or null if no such history object exists
	 */
	public UpgradeHistoryData getUpgradesHistory(final String version) {
		return upgradesHistories.get(version);
	}

	/**
	 * Persists the UpgradeHistoryData on the file system
	 * 
	 * @param upgradeHistoryData UpgradeHistoryData of the given version
	 * @param version identifier of UpgradeHistoryData
	 */
	public void setUpgradesHistory(final UpgradeHistoryData upgradeHistoryData, final String version) {
		this.upgradesHistories.put(version, upgradeHistoryData);
		final File upgradesDir = new File(WebappHelper.getUserDataRoot(), SYSTEM_DIR);
		upgradesDir.mkdirs(); // create if not exists
		final File upgradesHistoriesFile = new File(upgradesDir, INSTALLED_UPGRADES_XML);
		XStreamHelper.writeObject(upgradesHistoriesFile, this.upgradesHistories);
	}

	/**
	 * Load all persisted UpgradeHistoryData objects from the fileSystem
	 */
	@SuppressWarnings("unchecked")
	protected void initUpgradesHistories() {
		final File upgradesDir = new File(WebappHelper.getUserDataRoot(), SYSTEM_DIR);
		final File upgradesHistoriesFile = new File(upgradesDir, INSTALLED_UPGRADES_XML);
		if (upgradesHistoriesFile.exists()) {
			this.upgradesHistories = (Map<String, UpgradeHistoryData>) XStreamHelper.readObject(upgradesHistoriesFile);
		} else {
			if (this.upgradesHistories == null) {
				this.upgradesHistories = new HashMap<String, UpgradeHistoryData>();
			}
			needsUpgrade = false; // looks like a new install, no upgrade necessary
			logInfo("This looks like a new install or droped data, will not do any upgrades.");
			createUpgradeData();
		}
	}

	/**
	 * create fake upgrade data as this is a new installation
	 */
	private void createUpgradeData() {
		for (final OLATUpgrade upgrade : upgrades) {
			final UpgradeHistoryData uhd = new UpgradeHistoryData();
			uhd.setInstallationComplete(true);
			uhd.setBooleanDataValue(OLATUpgrade.TASK_DP_UPGRADE, true);
			setUpgradesHistory(uhd, upgrade.getVersion());
		}

	}

	/**
	 * On any RuntimeExceptions during init. Abort loading of application. Modules should throw RuntimeExceptions if they can't live with a the given state of
	 * configuration.
	 * 
	 * @param e
	 */
	protected void abort(final Throwable e) {
		if (e instanceof StartupException) {
			final StartupException se = (StartupException) e;
			logWarn("Message: " + se.getLogMsg(), se);
			final Throwable cause = se.getCause();
			logWarn("Cause: " + (cause != null ? cause.getMessage() : "n/a"), se);
		}
		throw new RuntimeException("*** CRITICAL ERROR IN UPGRADE MANAGER. Loading aborted.", e);
	}

	@Override
	public void event(final Event event) {
		if (event instanceof FrameworkStartedEvent) {
			doPostSystemInitUpgrades();
		}
	}

}