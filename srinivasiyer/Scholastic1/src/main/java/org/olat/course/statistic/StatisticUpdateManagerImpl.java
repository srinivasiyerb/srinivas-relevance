package org.olat.course.statistic;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.taskExecutor.TaskExecutorManager;
import org.olat.core.gui.control.Event;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.core.util.resource.OresHelper;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;

/**
 * Default implementation for IStatisticUpdateManager
 * <P>
 * Initial Date: 11.02.2010 <br>
 * 
 * @author Stefan
 */
class StatisticUpdateManagerImpl extends BasicManager implements StatisticUpdateManager, GenericEventListener {

	/** the logging object used in this class **/
	static final OLog log_ = Tracing.createLoggerFor(StatisticUpdateManagerImpl.class);

	/** the category used for statistics properties (in the o_properties table) **/
	private static final String STATISTICS_PROPERTIES_CATEGORY = "STATISTICS_PROPERTIES";

	/** the name used for last_updated property (in the o_properties table) **/
	private static final String LAST_UPDATED_PROPERTY_NAME = "LAST_UPDATED";

	/** the event string used to ensure that only one StatisticUpdateManagerImpl is active in a cluster **/
	private static final String STARTUP_EVENT = "startupEvent";

	/** all the IStatisticUpdaters that registered with the StatisticUpdaterManager **/
	final List<IStatisticUpdater> updaters_ = new LinkedList<IStatisticUpdater>();

	private final MultiUserEvent startupEvent_ = new MultiUserEvent(STARTUP_EVENT);

	/** whether or not this manager is enabled - disables itself when there is more than 1 in the cluster **/
	private boolean enabled_ = true;

	boolean updateOngoing_ = false;

	/** spring **/
	public StatisticUpdateManagerImpl(final CoordinatorManager coordinatorManager, final StatisticUpdateConfig config, final String enabled) {
		enabled_ = enabled != null && "enabled".equals(enabled);
		if (!enabled_) {
			log_.info("<init> disabled by configuration");
			return;
		}
		updaters_.addAll(config.getUpdaters());

		// note: not using CoordinatorManager.getInstance().getCoordinator() in this spring-called-constructor
		// as we have a problem in 6.3 where Tracing calls into CoordinatorManager.getInstance().getCoordinator()
		// which in turn causes the coord variable there to be initialized, which in turn
		// initializes the CoreSpringFactory, which in turn creates this bean *BEFORE*
		// the CoordinatorManager.coord was set ... hence we'd get a NullPointerException here
		coordinatorManager.getCoordinator().getEventBus()
				.registerFor(this, null, OresHelper.createOLATResourceableTypeWithoutCheck(StatisticUpdateManagerImpl.class.getName()));
		coordinatorManager.getCoordinator().getEventBus()
				.fireEventToListenersOf(startupEvent_, OresHelper.createOLATResourceableTypeWithoutCheck(StatisticUpdateManagerImpl.class.getName()));
	}

	@Override
	public void addStatisticUpdater(final IStatisticUpdater updater) {
		updaters_.add(updater);
	}

	@Override
	public synchronized boolean isEnabled() {
		return enabled_;
	}

	@Override
	public synchronized boolean updateOngoing() {
		return updateOngoing_;
	}

	@Override
	public boolean updateStatistics(final boolean fullRecalculation, final Runnable finishedCallback) {

		synchronized (this) {
			if (!enabled_) {
				log_.warn("updateStatistics: cannot update statistics, manager is not enabled!", new Exception("updateStatistics"));
				return false;
			}
			if (updateOngoing_) {
				log_.warn("updateStatistics: cannot update statistics since an update is currently ongoing");
				return false;
			}
			updateOngoing_ = true;
		}

		final Runnable r = new Runnable() {

			@Override
			public void run() {
				final long start = System.currentTimeMillis();
				try {
					log_.info("updateStatistics: initialization for update");

					final long nowInMilliseconds = System.currentTimeMillis();
					long lastUpdatedInMilliseconds = getAndUpdateLastUpdated(nowInMilliseconds);
					if (fullRecalculation || (lastUpdatedInMilliseconds == -1)) {
						final Calendar nineteennintyeight = Calendar.getInstance();
						nineteennintyeight.set(1998, 12, 31);
						lastUpdatedInMilliseconds = nineteennintyeight.getTimeInMillis();
					}

					final Date lastUpdatedDate = new Date(lastUpdatedInMilliseconds);
					final Date nowDate = new Date(nowInMilliseconds);

					log_.info("updateStatistics: starting the update");
					DBFactory.getInstance().intermediateCommit();
					for (final Iterator<IStatisticUpdater> it = updaters_.iterator(); it.hasNext();) {
						final IStatisticUpdater statisticUpdater = it.next();
						log_.info("updateStatistics: starting updater " + statisticUpdater);
						statisticUpdater.updateStatistic(fullRecalculation || (lastUpdatedInMilliseconds == -1), lastUpdatedDate, nowDate,
								StatisticUpdateManagerImpl.this);
						log_.info("updateStatistics: done with updater " + statisticUpdater);
						DBFactory.getInstance().intermediateCommit();
					}
				} finally {
					synchronized (StatisticUpdateManagerImpl.this) {
						updateOngoing_ = false;
					}
					final long diff = System.currentTimeMillis() - start;
					log_.info("updateStatistics: total time for updating all statistics was " + diff + " milliseconds");

					if (finishedCallback != null) {
						finishedCallback.run();
					}
				}
			}

		};
		try {
			TaskExecutorManager.getInstance().runTask(r);
			log_.info("updateStatistics: starting the update in its own thread");
			return true;
		} catch (final AssertException ae) {
			log_.info("updateStatistics: Could not start update due to TaskExecutorManager not yet initialized. Will be done next time Cron/User calls!");
			synchronized (StatisticUpdateManagerImpl.this) {
				updateOngoing_ = false;
			}
			return false;
		}

	}

	@Override
	public long getLastUpdated() {
		final PropertyManager pm = PropertyManager.getInstance();
		final Property p = pm.findProperty(null, null, null, STATISTICS_PROPERTIES_CATEGORY, LAST_UPDATED_PROPERTY_NAME);
		if (p == null) {
			return -1;
		} else {
			return p.getLongValue();
		}
	}

	@Override
	public long getAndUpdateLastUpdated(final long lastUpdated) {
		final PropertyManager pm = PropertyManager.getInstance();
		final Property p = pm.findProperty(null, null, null, STATISTICS_PROPERTIES_CATEGORY, LAST_UPDATED_PROPERTY_NAME);
		if (p == null) {
			final Property newp = pm.createPropertyInstance(null, null, null, STATISTICS_PROPERTIES_CATEGORY, LAST_UPDATED_PROPERTY_NAME, null, lastUpdated, null, null);
			pm.saveProperty(newp);
			return -1;
		} else {
			final long result = p.getLongValue();
			p.setLongValue(lastUpdated);
			pm.saveProperty(p);
			return result;
		}
	}

	@Override
	public void event(final Event event) {
		// event from EventBus
		if (event != startupEvent_) {
			// that means some other StatisticUpdateManager is in the cluster
			// not good!
			log_.error("event: CONFIG ERROR: there is more than one StatisticUpdateManager in this Cluster! I'll disable myself.");
			synchronized (this) {
				enabled_ = false;
			}
		}
	}
}
