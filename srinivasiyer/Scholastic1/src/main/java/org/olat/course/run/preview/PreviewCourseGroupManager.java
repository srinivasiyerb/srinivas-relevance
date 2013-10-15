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

import java.io.File;
import java.util.List;

import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.groupsandrights.CourseRights;

/**
 * Initial Date: 08.02.2005
 * 
 * @author Mike Stock
 */
final class PreviewCourseGroupManager extends BasicManager implements CourseGroupManager {

	private final List groups;
	private final List areas;
	private final boolean isCoach, isCourseAdmin;

	/**
	 * @param groups
	 * @param areas
	 * @param isCoach
	 * @param isCourseAdmin
	 */
	public PreviewCourseGroupManager(final List groups, final List areas, final boolean isCoach, final boolean isCourseAdmin) {
		this.groups = groups;
		this.areas = areas;
		this.isCourseAdmin = isCourseAdmin;
		this.isCoach = isCoach;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#initGroupContextsList()
	 */
	@Override
	public void initGroupContextsList() {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#hasRight(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public boolean hasRight(final Identity identity, final String courseRight) {
		if (courseRight.equals(CourseRights.RIGHT_COURSEEDITOR)) { return false; }
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#hasRight(org.olat.core.id.Identity, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean hasRight(final Identity identity, final String courseRight, final String groupContextName) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInLearningGroup(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public boolean isIdentityInLearningGroup(final Identity identity, final String groupName) {
		return groups.contains(groupName);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInLearningGroup(org.olat.core.id.Identity, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isIdentityInLearningGroup(final Identity identity, final String groupName, final String groupContextName) {
		return groups.contains(groupName);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isLearningGroupFull(java.lang.String)
	 */
	@Override
	public boolean isLearningGroupFull(final String groupName) {
		return groups.contains(groupName);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInRightGroup(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public boolean isIdentityInRightGroup(final Identity identity, final String groupName) {
		return groups.contains(groupName);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInRightGroup(org.olat.core.id.Identity, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isIdentityInRightGroup(final Identity identity, final String groupName, final String groupContextName) {
		return groups.contains(groupName);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInLearningArea(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public boolean isIdentityInLearningArea(final Identity identity, final String areaName) {
		return areas.contains(areaName);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInLearningArea(org.olat.core.id.Identity, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isIdentityInLearningArea(final Identity identity, final String areaName, final String groupContextName) {
		return areas.contains(areaName);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInGroupContext(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public boolean isIdentityInGroupContext(final Identity identity, final String groupContextName) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityCourseCoach(org.olat.core.id.Identity)
	 */
	@Override
	public boolean isIdentityCourseCoach(final Identity identity) {
		return isCoach;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityCourseAdministrator(org.olat.core.id.Identity)
	 */
	@Override
	public boolean isIdentityCourseAdministrator(final Identity identity) {
		return isCourseAdmin;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityParticipantInAnyRightGroup(org.olat.core.id.Identity)
	 */
	@Override
	public boolean isIdentityParticipantInAnyRightGroup(final Identity identity) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityParticipantInAnyLearningGroup(org.olat.core.id.Identity)
	 */
	@Override
	public boolean isIdentityParticipantInAnyLearningGroup(final Identity identity) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getLearningGroupContexts()
	 */
	@Override
	public List getLearningGroupContexts() {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getRightGroupContexts()
	 */
	@Override
	public List getRightGroupContexts() {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getAllLearningGroupsFromAllContexts()
	 */
	@Override
	public List getAllLearningGroupsFromAllContexts() {
		return groups;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getLearningGroupsFromAllContexts(java.lang.String)
	 */
	@Override
	public List getLearningGroupsFromAllContexts(final String groupName) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getLearningGroupsInAreaFromAllContexts(java.lang.String)
	 */
	@Override
	public List getLearningGroupsInAreaFromAllContexts(final String areaName) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getParticipatingLearningGroupsFromAllContexts(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public List getParticipatingLearningGroupsFromAllContexts(final Identity identity, final String groupName) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getParticipatingLearningGroupsInAreaFromAllContexts(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public List getParticipatingLearningGroupsInAreaFromAllContexts(final Identity identity, final String araName) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getOwnedLearningGroupsFromAllContexts(org.olat.core.id.Identity)
	 */
	@Override
	public List getOwnedLearningGroupsFromAllContexts(final Identity identity) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getParticipatingLearningGroupsFromAllContexts(org.olat.core.id.Identity)
	 */
	@Override
	public List getParticipatingLearningGroupsFromAllContexts(final Identity identity) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getParticipatingRightGroupsFromAllContexts(org.olat.core.id.Identity)
	 */
	@Override
	public List getParticipatingRightGroupsFromAllContexts(final Identity identity) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getAllRightGroupsFromAllContexts()
	 */
	@Override
	public List getAllRightGroupsFromAllContexts() {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getAllAreasFromAllContexts()
	 */
	@Override
	public List getAllAreasFromAllContexts() {
		return areas;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#deleteCourseGroupmanagement()
	 */
	@Override
	public void deleteCourseGroupmanagement() {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#createCourseGroupmanagement(java.lang.String)
	 */
	@Override
	public void createCourseGroupmanagement(final String courseTitle) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#createCourseGroupmanagementAsCopy(org.olat.course.groupsandrights.CourseGroupManager, java.lang.String)
	 */
	@Override
	public void createCourseGroupmanagementAsCopy(final CourseGroupManager originalCourseGroupManager, final String courseTitle) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getNumberOfMembersFromGroups(java.util.List)
	 */
	@Override
	public List getNumberOfMembersFromGroups(final List groupList) {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getUniqueLearningGroupNamesFromAllContexts()
	 */
	@Override
	public List getUniqueLearningGroupNamesFromAllContexts() {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getUniqueAreaNamesFromAllContexts()
	 */
	@Override
	public List getUniqueAreaNamesFromAllContexts() {
		throw new AssertException("unsupported");
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getLearningAreasOfGroupFromAllContexts(java.lang.String)
	 */
	@Override
	public List getLearningAreasOfGroupFromAllContexts(final String groupName) {
		throw new AssertException("unsupported");
	}

	@Override
	public List getCoachesFromLearningGroup(final String groupName) {
		throw new AssertException("unsupported");
	}

	@Override
	public List getCoachesFromArea(final String areaName) {
		throw new AssertException("unsupported");
	}

	@Override
	public List getParticipantsFromLearningGroup(final String groupName) {
		throw new AssertException("unsupported");
	}

	@Override
	public List getParticipantsFromArea(final String areaName) {
		throw new AssertException("unsupported");
	}

	@Override
	public List getRightGroupsFromAllContexts(final String groupName) {
		throw new AssertException("unsupported");
	}

	@Override
	public void exportCourseRightGroups(final File fExportDirectory) {
		throw new AssertException("unsupported");
	}

	@Override
	public void importCourseRightGroups(final File fImportDirectory) {
		throw new AssertException("unsupported");
	}

	@Override
	public void exportCourseLeaningGroups(final File fExportDirectory) {
		throw new AssertException("unsupported");
	}

	@Override
	public void importCourseLearningGroups(final File fImportDirectory) {
		throw new AssertException("unsupported");
	}

	@Override
	public List getWaitingListGroupsFromAllContexts(final Identity identity) {
		throw new AssertException("unsupported");
	}

}