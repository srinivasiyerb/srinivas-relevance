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

package org.olat.course;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.configuration.AbstractOLATModule;
import org.olat.core.configuration.PersistedProperties;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.FrameworkStartedEvent;
import org.olat.core.util.event.FrameworkStartupEventChannel;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;

/**
 * Initial Date: 02.09.2005 <br>
 * 
 * @author Mike Stock
 * @author guido
 */
public class CourseModule extends AbstractOLATModule {

	private static boolean courseChatEnabled;
	private static boolean displayParticipantsCount;
	// Repository types
	public static String ORES_TYPE_COURSE = OresHelper.calculateTypeName(CourseModule.class);
	private static OLATResourceable ORESOURCEABLE_TYPE_COURSE = OresHelper.lookupType(CourseModule.class);
	public static final String ORES_COURSE_ASSESSMENT = OresHelper.calculateTypeName(AssessmentManager.class);
	private static String helpCourseSoftkey;
	private static CoordinatorManager coordinatorManager;
	private Map deployedCourses;
	private boolean deployCoursesEnabled;
	private final PropertyManager propertyManager;
	private final CourseFactory courseFactory;
	private Map<String, String> logVisibilities;
	private List<DeployableCourseExport> deployableCourseExports;
	private final RepositoryManager repositoryManager;
	private final OLATResourceManager olatResourceManager;

	/**
	 * [used by spring]
	 */
	private CourseModule(final CoordinatorManager coordinatorManager, final PropertyManager propertyManager, final CourseFactory courseFactory,
			final RepositoryManager repositoryManager, final OLATResourceManager olatResourceManager) {
		CourseModule.coordinatorManager = coordinatorManager;
		this.propertyManager = propertyManager;
		this.courseFactory = courseFactory;
		this.repositoryManager = repositoryManager;
		this.olatResourceManager = olatResourceManager;
		coordinatorManager.getCoordinator().getEventBus().registerFor(this, null, FrameworkStartupEventChannel.getStartupEventChannel());
	}

	/**
	 * Courses are deployed after the startup has completed.
	 */
	@Override
	public void event(final org.olat.core.gui.control.Event event) {
		// do not deploy courses/help course if in JUnit Mode.
		if (Settings.isJUnitTest()) { return; }

		if (event instanceof FrameworkStartedEvent && ((FrameworkStartedEvent) event).isEventOnThisNode()) {
			// Deploy demo courses
			logInfo("Received FrameworkStartedEvent and is on same node, will start deploying demo courses...");
			deployCoursesFromCourseExportFiles();
		}
		// also in startup event processing intermediateCommit
		DBFactory.getInstance(false).intermediateCommit();
	}

	/**
	 * [used by spring]
	 */
	public void setCourseExportFiles(final List<DeployableCourseExport> deployableCourseExports) {
		this.deployableCourseExports = deployableCourseExports;
	}

	/**
	 * [used by spring]
	 */
	public void setLogVisibilityForCourseAuthor(final Map<String, String> logVisibilities) {
		this.logVisibilities = logVisibilities;
	}

	@Override
	protected void initDefaultProperties() {
		courseChatEnabled = getBooleanConfigParameter("enableCourseChat", true);
		deployCoursesEnabled = getBooleanConfigParameter("deployCourseExportsEnabled", true);
		displayParticipantsCount = getBooleanConfigParameter("displayParticipantsCount", true);
	}

	/**
	 * @see org.olat.core.configuration.OLATModule#init(com.anthonyeden.lib.config.Configuration)
	 */
	@Override
	public void init() {
		// skip all the expensive course demo setup and deployment when we are in junit mode.
		if (Settings.isJUnitTest()) { return; }

		logInfo("Initializing the OLAT course system");

		// Cleanup, otherwise this subjects will have problems in normal OLAT
		// operation
		DBFactory.getInstance(false).intermediateCommit();

	}

	private void deployCoursesFromCourseExportFiles() {
		logInfo("Deploying course exports.");
		for (final DeployableCourseExport export : deployableCourseExports) {
			if (0 < export.getAccess() && export.getAccess() < 5) {
				if (deployCoursesEnabled || export.isHelpCourse()) {
					RepositoryEntry re = null;
					try {
						re = deployCourse(export, export.getAccess());
					} catch (final Exception e) {
						logWarn("Skipping deployment of course::" + export.getIdentifier(), e);
					}
					DBFactory.getInstance().intermediateCommit();
					if (re != null && export.isHelpCourse()) {
						helpCourseSoftkey = re.getSoftkey();
						logInfo("Assigned help course softkey: " + re.getSoftkey());
					}
					continue;
				}
			} else {
				logInfo("Skipping deployment of course::" + export.getIdentifier() + " ; access attribute must be 1,2,3 or 4 but values is::" + export.getAccess());
			}
			logInfo("Skipping deployment of course::" + export.getIdentifier());
		}
		if (!deployCoursesEnabled) {
			logInfo("Skipping deployment of demo course exports. To deploy course exports, please enable in the configuration file. Help course will always be deployed!");
		}
	}

	private RepositoryEntry deployCourse(final DeployableCourseExport export, final int access) {
		// let's see if we previously deployed demo courses...

		RepositoryEntry re = (RepositoryEntry) getDeployedCourses().get(export.getIdentifier());
		if (re != null) {
			logInfo("Course '" + export.getIdentifier() + "' has been previousely deployed. Skipping.");
			return re;
		}

		final File file = export.getDeployableCourseZipFile();
		if (file != null && file.exists()) {
			logInfo("deploying Course: " + file.getName());
			if (!file.exists()) {
				// do not throw exception as users may upload bad file
				logWarn("Cannot deploy course from file: " + file.getAbsolutePath(), null);
				return null;
			}
			re = CourseFactory.deployCourseFromZIP(file, access);
			if (re != null) {
				markAsDeployed(export, re, false);
			}
			return re;
		}
		return null;
	}

	/**
	 * Mark a course as deployed. Remember the key of the repository entry it was deployed.
	 * 
	 * @param courseExportPath
	 * @param re
	 */
	private void markAsDeployed(final DeployableCourseExport export, final RepositoryEntry re, final boolean update) {
		final Property prop = propertyManager.createPropertyInstance(null, null, null, "_o3_", "deployedCourses", export.getVersion(), re.getKey(),
				export.getIdentifier(), null);
		propertyManager.saveProperty(prop);
		deployedCourses.put(export.getIdentifier(), re);
	}

	/**
	 * Get the Map of deployed courses. Map contains repo entries by path keys.
	 * 
	 * @return
	 */
	private Map getDeployedCourses() {
		if (deployedCourses != null) { return deployedCourses; }
		final List props = propertyManager.findProperties(null, null, null, "_o3_", "deployedCourses");
		deployedCourses = new HashMap(props.size());
		for (final Iterator iter = props.iterator(); iter.hasNext();) {
			final Property prop = (Property) iter.next();
			final Long repoKey = prop.getLongValue();
			RepositoryEntry re = null;
			re = RepositoryManager.getInstance().lookupRepositoryEntry(repoKey);
			if (re != null) {
				// props with floatValue null are old entries - delete them.
				if (prop.getFloatValue() == null) {
					// those are courses deployed with the old mechanism, delete them and redeploy
					logInfo("This course was already deployed but has old property values. Deleting it and redeploy course: " + prop.getStringValue());
					deleteCourseAndProperty(prop, re);
					re = null; // do not add to deployed courses
				} else {
					// check if latest version if course is installed
					for (final DeployableCourseExport export : deployableCourseExports) {
						if (export.getIdentifier().equals(prop.getStringValue()) && export.getVersion() > prop.getFloatValue()) {
							// we have a newer version - delete the old course
							logInfo("There is a new version for this course available. Deleting it and redeploy course: " + prop.getStringValue());
							deleteCourseAndProperty(prop, re);
							re = null; // do not add to deployed courses
							break;
						}
					}
				}
			}
			if (re != null) {
				deployedCourses.put(prop.getStringValue(), re);
			}
		}
		return deployedCourses;
	}

	private void deleteCourseAndProperty(final Property prop, final RepositoryEntry re) {
		try {
			propertyManager.deleteProperty(prop);
			repositoryManager.deleteRepositoryEntryAndBasesecurity(re);
			CourseFactory.deleteCourse(re.getOlatResource());
			final OLATResource ores = olatResourceManager.findResourceable(re.getOlatResource());
			olatResourceManager.deleteOLATResource(ores);
		} catch (final Exception e) {
			logWarn("Could not delete course and property of demo course with name: " + prop.getStringValue(), e);
		}
	}

	/**
	 * @return true if the course author can see/download/modify the admin log
	 */
	public boolean isAdminLogVisibleForMigrationOnly() {
		return logVisibilities.get("AdminLog").equals("VISIBLE");
	}

	/**
	 * @return true if the course author can see/download/modify the user log
	 */
	public boolean isUserLogVisibleForMigrationOnly() {
		return logVisibilities.get("UserLog").equals("VISIBLE");
	}

	/**
	 * @return true if the course author can see/download/modify the statistic log
	 */
	public boolean isStatisticLogVisibleForMigrationOnly() {
		return logVisibilities.get("StatisticLog").equals("VISIBLE");
	}

	/**
	 * @return The filename of the zipped help course
	 */
	public static String getHelpCourseSoftKey() {
		return helpCourseSoftkey;
	}

	/**
	 * @return type name
	 */
	public static String getCourseTypeName() {
		return ORES_TYPE_COURSE;
	}

	/**
	 * @param ce
	 * @param cn
	 * @return the generated SubscriptionContext
	 */
	public static SubscriptionContext createSubscriptionContext(final CourseEnvironment ce, final CourseNode cn) {
		final SubscriptionContext sc = new SubscriptionContext(getCourseTypeName(), ce.getCourseResourceableId(), cn.getIdent());
		return sc;
	}

	/**
	 * @param ce
	 * @param cn
	 * @return a subscriptioncontext with no translations for the user, but only to be able to cleanup/obtain
	 */
	public static SubscriptionContext createTechnicalSubscriptionContext(final CourseEnvironment ce, final CourseNode cn) {
		final SubscriptionContext sc = new SubscriptionContext(getCourseTypeName(), ce.getCourseResourceableId(), cn.getIdent());
		return sc;
	}

	/**
	 * Creates subscription context which points to an element e.g. that is a sub element of a node (subsubId). E.g. inside the course node dialog elements where a course
	 * node can have several forums.
	 * 
	 * @param ce
	 * @param cn
	 * @param subsubId
	 * @return
	 */
	public static SubscriptionContext createSubscriptionContext(final CourseEnvironment ce, final CourseNode cn, final String subsubId) {
		final SubscriptionContext sc = new SubscriptionContext(getCourseTypeName(), ce.getCourseResourceableId(), cn.getIdent() + ":" + subsubId);
		return sc;
	}

	/**
	 * whether course chat is enabled or not - depends on Instant Messaging enabled! you should check first for IM Enabled @see
	 * {@link org.olat.instantMessaging.InstantMessagingModule}
	 * 
	 * @return
	 */
	public static boolean isCourseChatEnabled() {
		return courseChatEnabled;
	}

	public static void registerForCourseType(final GenericEventListener gel, final Identity identity) {
		coordinatorManager.getCoordinator().getEventBus().registerFor(gel, identity, ORESOURCEABLE_TYPE_COURSE);
	}

	public static void deregisterForCourseType(final GenericEventListener gel) {
		coordinatorManager.getCoordinator().getEventBus().deregisterFor(gel, ORESOURCEABLE_TYPE_COURSE);
	}

	/**
	 * max number of course nodes
	 * 
	 * @return
	 */
	public static int getCourseNodeLimit() {
		return 499;
	}

	@Override
	protected void initFromChangedProperties() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPersistedProperties(final PersistedProperties persistedProperties) {
		this.moduleConfigProperties = persistedProperties;
	}

	public static boolean displayParticipantsCount() {
		return CourseModule.displayParticipantsCount;
	}
}
