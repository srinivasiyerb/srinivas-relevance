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
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.Structure;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.config.CourseConfig;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.properties.CoursePropertyManager;

/**
 * Initial Date: May 5, 2004
 * 
 * @author Felix Jost<br>
 *         Comment: contains the LearningGroupManager, the SysEnvironment, and the Properties for the run of a course NOT FOR one user, but shared for all using this
 *         course
 */
public interface CourseEnvironment {

	/**
	 * Get the current time in milli seconds
	 * 
	 * @return The current time in millis
	 */
	public long getCurrentTimeMillis();

	/**
	 * if true then all ConditionInterpreter functions should return true/1 immediately without e.g. accessing the database. We use this as validating of the syntax
	 * before saving it, so that there are only grammatically correct expressions which are saved and therefore are evaluated in the run mode later.
	 * 
	 * @return true if no op mode
	 */
	public boolean isNoOpMode();

	/**
	 * Get the course group management environment
	 * 
	 * @return The course group management
	 */
	public CourseGroupManager getCourseGroupManager();

	/**
	 * Get the resourceable id of this course. This is the repository entry id.
	 * 
	 * @return The course resourceable id
	 */
	public Long getCourseResourceableId();

	/**
	 * Get a course property manager for this course
	 * 
	 * @return The course property manager
	 */
	public CoursePropertyManager getCoursePropertyManager();

	/**
	 * Get the assessment manager for this course
	 * 
	 * @return The course assessment manager
	 */
	public AssessmentManager getAssessmentManager();

	/**
	 * Get the audit manager for this course
	 * 
	 * @return The course audit manager
	 */
	public UserNodeAuditManager getAuditManager();

	/**
	 * Get the course folder path. This is the same as the path you would get from course.getCourseFolderPath()
	 * 
	 * @return The course folder path for this course
	 */
	public VFSContainer getCourseFolderContainer();

	/**
	 * Return the course base path.
	 * 
	 * @return the course base path
	 */
	public OlatRootFolderImpl getCourseBaseContainer();

	/**
	 * Return the course run structure
	 * 
	 * @return the runstructure
	 */
	public Structure getRunStructure();

	/**
	 * @return the courseTitle
	 */
	public String getCourseTitle();

	/**
	 * @return course config of this course
	 */
	public CourseConfig getCourseConfig();

}
