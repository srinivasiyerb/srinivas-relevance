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

import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.tree.CourseEditorTreeModel;

/**
 * Description:<BR/>
 * Interface of the OLAT course. The course has a course environment and a run structure and some other fields.
 * <p>
 * Initial Date: 2004/10/11 13:55:48
 * 
 * @author Felix Jost
 */
public interface ICourse extends OLATResourceable {

	/**
	 * Name of folder within course root directory where nodes export their data.
	 */
	public static final String EXPORTED_DATA_FOLDERNAME = "export";

	/**
	 * property name for the initial launch date will be set only the first time the users launch the course.
	 */
	public static final String PROPERTY_INITIAL_LAUNCH_DATE = "initialCourseLaunchDate";
	/**
	 * property name for the recent launch date will be changed every time the user start the course.
	 */
	public static final String PROPERTY_RECENT_LAUNCH_DATE = "recentCourseLaunchDate";

	/**
	 * @return The course run structure
	 */
	public Structure getRunStructure();

	/**
	 * @return The course editor tree model for this course
	 */
	public CourseEditorTreeModel getEditorTreeModel();

	/**
	 * Export course to file system.
	 * 
	 * @param exportDirecotry The directory to export files to.
	 */
	public void exportToFilesystem(File exportDirecotry);

	/**
	 * Return the container to files for this course. (E.g. "/course/123/")
	 * 
	 * @return the container to files for this course
	 */
	public OlatRootFolderImpl getCourseBaseContainer();

	/**
	 * Return the container to the coursefolder of this course. (E.g. "COURSEBASEPATH/coursefolder/")
	 * 
	 * @return the container to the coursefolder of this course
	 */
	public VFSContainer getCourseFolderContainer();

	/**
	 * @return The course title. This is the display name of the course repository entry or the short title of the course run structure root node if the repository entry
	 *         has not been created yet
	 */
	public String getCourseTitle();

	/**
	 * @return The course environment of this course
	 */
	public CourseEnvironment getCourseEnvironment();

	/**
	 * @return true: if the structure has assessable nodes or structure course nodes (subtype of assessable node), which 'hasPassedConfigured' or 'hasScoreConfigured' is
	 *         true
	 */
	public boolean hasAssessableNodes();

}
