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

package org.olat.course.run.environment;

import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.logging.AssertException;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.PersistingCourseImpl;
import org.olat.course.Structure;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.assessment.NewCachePersistingAssessmentManager;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.auditing.UserNodeAuditManagerImpl;
import org.olat.course.config.CourseConfig;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.groupsandrights.PersistingCourseGroupManager;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.properties.PersistingCoursePropertyManager;

/**
 * Initial Date: 09.03.2004
 * 
 * @author Felix Jost
 */
public class CourseEnvironmentImpl implements CourseEnvironment {

	private final PersistingCourseImpl course;
	private final CourseGroupManager cgm;
	private final CoursePropertyManager propertyManager;
	private final AssessmentManager assessmentManager;
	private UserNodeAuditManager auditManager;

	/**
	 * Constructor for the course environment
	 * 
	 * @param course The course
	 */
	public CourseEnvironmentImpl(final PersistingCourseImpl course) {
		this.course = course;
		this.propertyManager = PersistingCoursePropertyManager.getInstance(course);
		this.cgm = PersistingCourseGroupManager.getInstance(course);
		this.assessmentManager = NewCachePersistingAssessmentManager.getInstance(course);
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getCurrentTimeMillis()
	 */
	@Override
	public long getCurrentTimeMillis() {
		return System.currentTimeMillis();
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#isNoOpMode()
	 */
	@Override
	public boolean isNoOpMode() {
		return false;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getCourseGroupManager()
	 */
	@Override
	public CourseGroupManager getCourseGroupManager() {
		return cgm;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getCourseResourceableId()
	 */
	@Override
	public Long getCourseResourceableId() {
		return course.getResourceableId();
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getCoursePropertyManager()
	 */
	@Override
	public CoursePropertyManager getCoursePropertyManager() {
		return propertyManager;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getAssessmentManager()
	 */
	@Override
	public AssessmentManager getAssessmentManager() {
		return this.assessmentManager;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getAuditManager()
	 */
	@Override
	public UserNodeAuditManager getAuditManager() {
		/**
		 * staring audit manager due to early caused problem with fresh course imports (demo courses!) on startup
		 */
		if (this.auditManager == null) {
			this.auditManager = new UserNodeAuditManagerImpl(course);
		}
		return this.auditManager;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getRunStructure()
	 */
	@Override
	public Structure getRunStructure() {
		final Structure runStructure = course.getRunStructure();
		if (runStructure == null) { throw new AssertException("asked for runstructure, but icourse's runstructure is still null"); }
		return runStructure;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getCourseTitle()
	 */
	@Override
	public String getCourseTitle() {
		return course.getCourseTitle();
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getCourseConfig()
	 */
	@Override
	public CourseConfig getCourseConfig() {
		return course.getCourseConfig();
	}

	@Override
	public VFSContainer getCourseFolderContainer() {
		return course.getCourseFolderContainer();
	}

	@Override
	public OlatRootFolderImpl getCourseBaseContainer() {
		return course.getCourseBaseContainer();
	}

}