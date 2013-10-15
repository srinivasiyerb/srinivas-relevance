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

package org.olat.course.run.preview;

import java.util.Date;

import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.Structure;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.config.CourseConfig;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.run.environment.CourseEnvironment;

/**
 * Initial Date: 08.02.2005
 * 
 * @author Mike Stock
 */
final class PreviewCourseEnvironment implements CourseEnvironment {
	private final String title;
	private final Structure runStructure;
	private final OlatRootFolderImpl courseBaseContainer;
	private final VFSContainer courseFolderContainer;
	private final CoursePropertyManager cpm;
	private final CourseGroupManager cgm;
	private final UserNodeAuditManager auditman;
	private final AssessmentManager am;
	private final long simulatedDateTime;
	private final Long resourceablId;

	PreviewCourseEnvironment(final String title, final Structure runStructure, final Date simulatedDateTime, final VFSContainer courseFolderContainer,
			final OlatRootFolderImpl courseBaseContainer, final Long courseResourceableID, final CoursePropertyManager cpm, final CourseGroupManager cgm,
			final UserNodeAuditManager auditman, final AssessmentManager am) {
		super();
		this.title = title;
		this.simulatedDateTime = simulatedDateTime.getTime();
		this.courseFolderContainer = courseFolderContainer;
		this.courseBaseContainer = courseBaseContainer;
		this.runStructure = runStructure;
		this.cpm = cpm;
		this.cgm = cgm;
		this.auditman = auditman;
		this.am = am;
		this.resourceablId = courseResourceableID;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getCurrentTimeMillis()
	 */
	@Override
	public long getCurrentTimeMillis() {
		return simulatedDateTime;
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
		// since OLAT 6.0.x: needed for SinglePage and hence for STCourseNode
		// introduced dependancy through iFrame refactoring of SinglePage and CP
		return resourceablId;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getCoursePropertyManager()
	 */
	@Override
	public CoursePropertyManager getCoursePropertyManager() {
		return cpm;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getAssessmentManager()
	 */
	@Override
	public AssessmentManager getAssessmentManager() {
		return am;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getAuditManager()
	 */
	@Override
	public UserNodeAuditManager getAuditManager() {
		return auditman;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getRunStructure()
	 */
	@Override
	public Structure getRunStructure() {
		return runStructure;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getCourseTitle()
	 */
	@Override
	public String getCourseTitle() {
		return title;
	}

	@Override
	public CourseConfig getCourseConfig() {
		throw new UnsupportedOperationException("never to be called in preview mode");
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#setCourseConfig()
	 */
	public void setCourseConfig(final CourseConfig cc) {
		throw new UnsupportedOperationException("never to be called in preview mode");
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getCourseFolderContainer()
	 */
	@Override
	public VFSContainer getCourseFolderContainer() {
		return courseFolderContainer;
	}

	/**
	 * @see org.olat.course.run.environment.CourseEnvironment#getCourseBaseContainer()
	 */
	@Override
	public OlatRootFolderImpl getCourseBaseContainer() {
		return courseBaseContainer;
	}

}