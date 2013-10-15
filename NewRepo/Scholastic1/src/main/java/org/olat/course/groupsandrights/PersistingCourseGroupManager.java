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

package org.olat.course.groupsandrights;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManager;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.group.area.BGArea;
import org.olat.group.area.BGAreaManager;
import org.olat.group.area.BGAreaManagerImpl;
import org.olat.group.context.BGContext;
import org.olat.group.context.BGContextManager;
import org.olat.group.context.BGContextManagerImpl;
import org.olat.group.right.BGRightManager;
import org.olat.group.right.BGRightManagerImpl;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;

/**
 * Description:<BR/>
 * Implementation of the CourseGroupManager that persists its data on the database
 * <P/>
 * Initial Date: Aug 25, 2004
 * 
 * @author gnaegi
 */
public class PersistingCourseGroupManager extends BasicManager implements CourseGroupManager {

	private static final String LEARNINGGROUPEXPORT_XML = "learninggroupexport.xml";
	private static final String RIGHTGROUPEXPORT_XML = "rightgroupexport.xml";
	private static final String LEARNINGGROUPARCHIVE_XLS = "learninggroup_archiv.xls";
	private static final String RIGHTGROUPARCHIVE_XLS = "rightgroup_archiv.xls";

	private final OLATResource courseResource;
	private List learningGroupContexts;
	private List rightGroupContexts;

	private PersistingCourseGroupManager(final OLATResourceable course) {
		this.courseResource = OLATResourceManager.getInstance().findOrPersistResourceable(course);
		initGroupContextsList();
	}

	private PersistingCourseGroupManager(final OLATResource courseResource) {
		this.courseResource = courseResource;
		initGroupContextsList();
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#initGroupContextsList()
	 */
	@Override
	public void initGroupContextsList() {
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		learningGroupContexts = contextManager.findBGContextsForResource(courseResource, BusinessGroup.TYPE_LEARNINGROUP, true, true);
		rightGroupContexts = contextManager.findBGContextsForResource(courseResource, BusinessGroup.TYPE_RIGHTGROUP, true, true);
	}

	/**
	 * @param course The current course
	 * @return A course group manager that uses persisted data
	 */
	public static PersistingCourseGroupManager getInstance(final OLATResourceable course) {
		return new PersistingCourseGroupManager(course);
	}

	/**
	 * @param courseResource The current course resource
	 * @return A course group manager that uses persisted data
	 */
	public static PersistingCourseGroupManager getInstance(final OLATResource courseResource) {
		return new PersistingCourseGroupManager(courseResource);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#hasRight(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public boolean hasRight(final Identity identity, final String courseRight) {
		return hasRight(identity, courseRight, null);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#hasRight(org.olat.core.id.Identity, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean hasRight(final Identity identity, final String courseRight, final String groupContextName) {
		final BGRightManager rightManager = BGRightManagerImpl.getInstance();
		final Iterator iter = rightGroupContexts.iterator();
		while (iter.hasNext()) {
			final BGContext context = (BGContext) iter.next();
			if (groupContextName == null || context.getName().equals(groupContextName)) {
				final boolean hasRight = rightManager.hasBGRight(courseRight, identity, context);
				if (hasRight) { return true; // finished
				}
			}
		}
		return false;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInLearningGroup(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public boolean isIdentityInLearningGroup(final Identity identity, final String groupName) {
		return isIdentityInLearningGroup(identity, groupName, null);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInLearningGroup(org.olat.core.id.Identity, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isIdentityInLearningGroup(final Identity identity, final String groupName, final String groupContextName) {
		return isIdentityInGroup(identity, groupName, groupContextName, this.learningGroupContexts);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isLearningGroupFull(java.lang.String)
	 */
	@Override
	public boolean isLearningGroupFull(final String groupName) {
		final OLog logger = Tracing.createLoggerFor(getClass());
		final List<BusinessGroup> groups = getLearningGroupsFromAllContexts(groupName);

		if (groups == null) {
			logger.warn("no groups available");
			return false;
		} else {
			boolean isLearningGroupFull = false;
			for (final BusinessGroup businessGroup : groups) {
				// if group null
				if (businessGroup == null) {
					logger.warn("group is null");
					return false;
				}
				// has group participants
				final BaseSecurity secMgr = BaseSecurityManager.getInstance();
				final List<Identity> members = secMgr.getIdentitiesOfSecurityGroup(businessGroup.getPartipiciantGroup());
				if (members == null) {
					logger.warn("group members are null");
					return false;
				}
				// has group no maximum of participants
				if (businessGroup.getMaxParticipants() == null) {
					logger.warn("group.getMaxParticipants() is null");
					return false;
				}
				// is the set of members greater equals than the maximum of participants
				if (members.size() >= businessGroup.getMaxParticipants().intValue()) {
					isLearningGroupFull = true;
				} else {
					return false;
				}
			}
			return isLearningGroupFull;
		}
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInRightGroup(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public boolean isIdentityInRightGroup(final Identity identity, final String groupName) {
		return isIdentityInRightGroup(identity, groupName, null);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInRightGroup(org.olat.core.id.Identity, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isIdentityInRightGroup(final Identity identity, final String groupName, final String groupContextName) {
		return isIdentityInGroup(identity, groupName, groupContextName, this.rightGroupContexts);
	}

	/**
	 * Internal method to check if an identity is in a group
	 * 
	 * @param identity
	 * @param groupName the group name. must not be null
	 * @param groupContextName context name to restrict to a certain context or null if in any context
	 * @param contextList list of contexts that should be searched
	 * @return true if in group, false otherwhise
	 */
	private boolean isIdentityInGroup(final Identity identity, final String groupName, final String groupContextName, final List contextList) {
		final BusinessGroupManager groupManager = BusinessGroupManagerImpl.getInstance();
		final Iterator iter = contextList.iterator();
		while (iter.hasNext()) {
			final BGContext context = (BGContext) iter.next();
			if (groupContextName == null || context.getName().equals(groupContextName)) {
				final boolean inGroup = groupManager.isIdentityInBusinessGroup(identity, groupName, context);
				if (inGroup) { return true; // finished
				}
			}
		}
		return false;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInLearningArea(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public boolean isIdentityInLearningArea(final Identity identity, final String areaName) {
		return isIdentityInLearningArea(identity, areaName, null);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInLearningArea(org.olat.core.id.Identity, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isIdentityInLearningArea(final Identity identity, final String areaName, final String groupContextName) {
		final BGAreaManager areaManager = BGAreaManagerImpl.getInstance();
		final Iterator iter = learningGroupContexts.iterator();
		while (iter.hasNext()) {
			final BGContext context = (BGContext) iter.next();
			if (groupContextName == null || context.getName().equals(groupContextName)) {
				final boolean inArea = areaManager.isIdentityInBGArea(identity, areaName, context);
				if (inArea) { return true; // finished
				}
			}
		}
		return false;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityInGroupContext(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public boolean isIdentityInGroupContext(final Identity identity, final String groupContextName) {
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		Iterator iter = learningGroupContexts.iterator();
		while (iter.hasNext()) {
			final BGContext context = (BGContext) iter.next();
			if (groupContextName == null || context.getName().equals(groupContextName)) {
				final boolean inContext = contextManager.isIdentityInBGContext(identity, context, true, true);
				if (inContext) { return true; // finished
				}
			}
		}
		iter = rightGroupContexts.iterator();
		while (iter.hasNext()) {
			final BGContext context = (BGContext) iter.next();
			if (groupContextName == null || context.getName().equals(groupContextName)) {
				final boolean inContext = contextManager.isIdentityInBGContext(identity, context, true, true);
				if (inContext) { return true; // finished
				}
			}
		}
		return false;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getLearningGroupContexts()
	 */
	@Override
	public List getLearningGroupContexts() {
		return learningGroupContexts;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getRightGroupContexts()
	 */
	@Override
	public List getRightGroupContexts() {
		return rightGroupContexts;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getRightGroupsFromAllContexts(java.lang.String)
	 */
	@Override
	public List getRightGroupsFromAllContexts(final String groupName) {
		final List groups = new ArrayList();
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		final Iterator iterator = rightGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final BusinessGroup group = contextManager.findGroupOfBGContext(groupName, bgContext);
			if (group != null) {
				groups.add(group);
			}
		}
		return groups;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getAllLearningGroupsFromAllContexts()
	 */
	@Override
	public List getAllLearningGroupsFromAllContexts() {
		final List allGroups = new ArrayList();
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		final Iterator iterator = learningGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final List contextGroups = contextManager.getGroupsOfBGContext(bgContext);
			allGroups.addAll(contextGroups);
		}
		return allGroups;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getLearningGroupsFromAllContexts(java.lang.String)
	 */
	@Override
	public List<BusinessGroup> getLearningGroupsFromAllContexts(final String groupName) {
		final List<BusinessGroup> groups = new ArrayList<BusinessGroup>();
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		final Iterator iterator = learningGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final BusinessGroup group = contextManager.findGroupOfBGContext(groupName, bgContext);
			if (group != null) {
				groups.add(group);
			}
		}
		return groups;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getAllAreasFromAllContexts()
	 */
	@Override
	public List getAllAreasFromAllContexts() {
		final List allAreas = new ArrayList();
		final BGAreaManager areaManager = BGAreaManagerImpl.getInstance();
		final Iterator iterator = learningGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final List contextAreas = areaManager.findBGAreasOfBGContext(bgContext);
			allAreas.addAll(contextAreas);
		}
		return allAreas;

	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getLearningGroupsInAreaFromAllContexts(java.lang.String)
	 */
	@Override
	public List getLearningGroupsInAreaFromAllContexts(final String areaName) {
		final List groups = new ArrayList();
		final BGAreaManager areaManager = BGAreaManagerImpl.getInstance();
		final Iterator iterator = learningGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final BGArea area = areaManager.findBGArea(areaName, bgContext);
			if (area != null) {
				final List areaGroups = areaManager.findBusinessGroupsOfArea(area);
				groups.addAll(areaGroups);
			}
		}
		return groups;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getLearningAreasOfGroupFromAllContexts(java.lang.String)
	 */
	@Override
	public List getLearningAreasOfGroupFromAllContexts(final String groupName) {
		final List areas = new ArrayList();
		final BGAreaManager areaManager = BGAreaManagerImpl.getInstance();
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		final Iterator iterator = learningGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final BusinessGroup group = contextManager.findGroupOfBGContext(groupName, bgContext);
			if (group != null) {
				final List groupAreas = areaManager.findBGAreasOfBusinessGroup(group);
				areas.addAll(groupAreas);
			}
		}
		return areas;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getParticipatingLearningGroupsFromAllContexts(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public List getParticipatingLearningGroupsFromAllContexts(final Identity identity, final String groupName) {
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		final List groups = new ArrayList();
		final Iterator iter = learningGroupContexts.iterator();
		while (iter.hasNext()) {
			final BGContext context = (BGContext) iter.next();
			final BusinessGroup group = contextManager.findGroupAttendedBy(identity, groupName, context);
			if (group != null) {
				groups.add(group);
			}
		}
		return groups;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getParticipatingLearningGroupsInAreaFromAllContexts(org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public List getParticipatingLearningGroupsInAreaFromAllContexts(final Identity identity, final String areaName) {
		final BGAreaManager areaManager = BGAreaManagerImpl.getInstance();
		final List groups = new ArrayList();
		final Iterator iter = learningGroupContexts.iterator();
		while (iter.hasNext()) {
			final BGContext context = (BGContext) iter.next();
			final List contextGroups = areaManager.findBusinessGroupsOfAreaAttendedBy(identity, areaName, context);
			groups.addAll(contextGroups);
		}
		return groups;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getAllRightGroupsFromAllContexts()
	 */
	@Override
	public List getAllRightGroupsFromAllContexts() {
		final List allGroups = new ArrayList();
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		final Iterator iterator = rightGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final List contextGroups = contextManager.getGroupsOfBGContext(bgContext);
			allGroups.addAll(contextGroups);
		}
		return allGroups;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getOwnedLearningGroupsFromAllContexts(org.olat.core.id.Identity)
	 */
	@Override
	public List getOwnedLearningGroupsFromAllContexts(final Identity identity) {
		final List allGroups = new ArrayList();
		final BusinessGroupManager groupManager = BusinessGroupManagerImpl.getInstance();
		final Iterator iterator = learningGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final List contextGroups = groupManager.findBusinessGroupsOwnedBy(bgContext.getGroupType(), identity, bgContext);
			allGroups.addAll(contextGroups);
		}
		return allGroups;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getParticipatingLearningGroupsFromAllContexts(org.olat.core.id.Identity)
	 */
	@Override
	public List getParticipatingLearningGroupsFromAllContexts(final Identity identity) {
		final List allGroups = new ArrayList();
		final BusinessGroupManager groupManager = BusinessGroupManagerImpl.getInstance();
		final Iterator iterator = learningGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final List contextGroups = groupManager.findBusinessGroupsAttendedBy(bgContext.getGroupType(), identity, bgContext);
			allGroups.addAll(contextGroups);
		}
		return allGroups;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getParticipatingRightGroupsFromAllContexts(org.olat.core.id.Identity)
	 */
	@Override
	public List getParticipatingRightGroupsFromAllContexts(final Identity identity) {
		final List allGroups = new ArrayList();
		final BusinessGroupManager groupManager = BusinessGroupManagerImpl.getInstance();
		final Iterator iterator = rightGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final List contextGroups = groupManager.findBusinessGroupsAttendedBy(bgContext.getGroupType(), identity, bgContext);
			allGroups.addAll(contextGroups);
		}
		return allGroups;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityCourseCoach(org.olat.core.id.Identity)
	 */
	@Override
	public boolean isIdentityCourseCoach(final Identity identity) {
		final BaseSecurity secManager = BaseSecurityManager.getInstance();
		final Iterator iterator = learningGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final boolean isCoach = secManager.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_COACH, bgContext);
			if (isCoach) { return true; }
		}
		return false;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityCourseAdministrator(org.olat.core.id.Identity)
	 */
	@Override
	public boolean isIdentityCourseAdministrator(final Identity identity) {
		// not really a group management method, for your convenience we have a
		// shortcut here...
		final BaseSecurity secMgr = BaseSecurityManager.getInstance();
		return secMgr.isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_ADMIN, courseResource);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityParticipantInAnyLearningGroup(org.olat.core.id.Identity)
	 */
	@Override
	public boolean isIdentityParticipantInAnyLearningGroup(final Identity identity) {
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		final Iterator iterator = learningGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			if (contextManager.isIdentityInBGContext(identity, bgContext, false, true)) { return true; }
		}
		return false;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#isIdentityParticipantInAnyRightGroup(org.olat.core.id.Identity)
	 */
	@Override
	public boolean isIdentityParticipantInAnyRightGroup(final Identity identity) {
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		final Iterator iterator = rightGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			if (contextManager.isIdentityInBGContext(identity, bgContext, false, true)) { return true; }
		}
		return false;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#createCourseGroupmanagement(java.lang.String)
	 */
	@Override
	public void createCourseGroupmanagement(final String courseTitle) {
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		// 1. context for learning groups
		if (this.learningGroupContexts.size() == 0) {
			final String learningGroupContextName = CourseGroupManager.DEFAULT_NAME_LC_PREFIX + courseTitle;
			contextManager.createAndAddBGContextToResource(learningGroupContextName, courseResource, BusinessGroup.TYPE_LEARNINGROUP, null, true);
			// no need to add it to list of contexts, already done by createAndAddBGContextToResource

		}
		// 2. context for right groups
		if (this.rightGroupContexts.size() == 0) {
			final String rightGroupContextName = CourseGroupManager.DEFAULT_NAME_RC_PREFIX + courseTitle;
			contextManager.createAndAddBGContextToResource(rightGroupContextName, courseResource, BusinessGroup.TYPE_RIGHTGROUP, null, true);
			// no need to add it to list of contexts, already done by createAndAddBGContextToResource
		}
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#createCourseGroupmanagementAsCopy(org.olat.course.groupsandrights.CourseGroupManager, java.lang.String)
	 */
	@Override
	public void createCourseGroupmanagementAsCopy(final CourseGroupManager originalCourseGroupManager, final String courseTitle) {

		// wrap as transatcion: do everything or nothing

		// 1. do copy learning group contexts
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		final List origLgC = originalCourseGroupManager.getLearningGroupContexts();
		Iterator iter = origLgC.iterator();
		while (iter.hasNext()) {
			final BGContext origContext = (BGContext) iter.next();
			if (origContext.isDefaultContext()) {
				// we found default context, copy this one
				final String learningGroupContextName = CourseGroupManager.DEFAULT_NAME_LC_PREFIX + courseTitle;
				contextManager.copyAndAddBGContextToResource(learningGroupContextName, this.courseResource, origContext);
				// no need to add it to list of contexts, already done by copyAndAddBGContextToResource
			} else {
				// not a course default context but an associated context - copy only
				// reference
				contextManager.addBGContextToResource(origContext, courseResource);
				// no need to add it to list of contexts, already done by addBGContextToResource
			}
		}
		// 2. do copy right group contexts
		final List origRgC = originalCourseGroupManager.getRightGroupContexts();
		iter = origRgC.iterator();
		while (iter.hasNext()) {
			final BGContext origContext = (BGContext) iter.next();
			if (origContext.isDefaultContext()) {
				// we found default context, copy this one
				final String rightGroupContextName = CourseGroupManager.DEFAULT_NAME_RC_PREFIX + courseTitle;
				contextManager.copyAndAddBGContextToResource(rightGroupContextName, this.courseResource, origContext);
				// no need to add it to list of contexts, already done by copyAndAddBGContextToResource
			} else {
				// not a course default context but an associated context - copy only
				// reference
				contextManager.addBGContextToResource(origContext, courseResource);
				// no need to add it to list of contexts, already done by addBGContextToResource
			}
		}
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#deleteCourseGroupmanagement()
	 */
	@Override
	public void deleteCourseGroupmanagement() {
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		// contextManager.removeAllBGContextsFromResource(courseResource);

		final List allContexts = contextManager.findBGContextsForResource(courseResource, true, true);
		final Iterator iter = allContexts.iterator();
		while (iter.hasNext()) {
			final BGContext context = (BGContext) iter.next();
			if (context.isDefaultContext()) {
				contextManager.deleteBGContext(context);
			} else {
				// not a default context, only unlink from this course
				contextManager.removeBGContextFromResource(context, courseResource);
			}
		}
		Tracing.logAudit("Deleting course groupmanagement for " + courseResource.toString(), this.getClass());
	}

	/**
	 * @param groups List of business groups
	 * @return list of Integers that contain the number of participants for each group
	 */
	@Override
	public List getNumberOfMembersFromGroups(final List groups) {
		final BaseSecurity securityManager = BaseSecurityManager.getInstance();
		final List members = new ArrayList();
		final Iterator iterator = groups.iterator();
		while (iterator.hasNext()) {
			final BusinessGroup group = (BusinessGroup) iterator.next();
			final int numbMembers = securityManager.countIdentitiesOfSecurityGroup(group.getPartipiciantGroup());
			members.add(new Integer(numbMembers));
		}
		return members;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getUniqueAreaNamesFromAllContexts()
	 */
	@Override
	public List getUniqueAreaNamesFromAllContexts() {
		final List areas = getAllAreasFromAllContexts();
		final List areaNames = new ArrayList();

		final Iterator iter = areas.iterator();
		while (iter.hasNext()) {
			final BGArea area = (BGArea) iter.next();
			final String areaName = area.getName();
			if (!areaNames.contains(areaName)) {
				areaNames.add(areaName.trim());
			}
		}

		Collections.sort(areaNames);

		return areaNames;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getUniqueLearningGroupNamesFromAllContexts()
	 */
	@Override
	public List getUniqueLearningGroupNamesFromAllContexts() {
		final List groups = getAllLearningGroupsFromAllContexts();
		final List groupNames = new ArrayList();

		final Iterator iter = groups.iterator();
		while (iter.hasNext()) {
			final BusinessGroup group = (BusinessGroup) iter.next();
			final String groupName = group.getName();
			if (!groupNames.contains(groupName)) {
				groupNames.add(groupName.trim());
			}
		}

		Collections.sort(groupNames);

		return groupNames;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#exportCourseLeaningGroups(java.io.File)
	 */
	@Override
	public void exportCourseLeaningGroups(final File fExportDirectory) {
		final BGContext context = findDefaultLearningContext();
		final File fExportFile = new File(fExportDirectory, LEARNINGGROUPEXPORT_XML);
		BusinessGroupManagerImpl.getInstance().exportGroups(context, fExportFile);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#importCourseLearningGroups(java.io.File)
	 */
	@Override
	public void importCourseLearningGroups(final File fImportDirectory) {
		final File fGroupExportXML = new File(fImportDirectory, LEARNINGGROUPEXPORT_XML);
		final BGContext context = findDefaultLearningContext();
		if (context == null) { throw new AssertException(
				"Unable to find default context for imported course. Should have been created before calling importCourseLearningGroups()"); }
		BusinessGroupManagerImpl.getInstance().importGroups(context, fGroupExportXML);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#exportCourseRightGroups(java.io.File)
	 */
	@Override
	public void exportCourseRightGroups(final File fExportDirectory) {
		final BGContext context = findDefaultRightsContext();
		final File fExportFile = new File(fExportDirectory, RIGHTGROUPEXPORT_XML);
		BusinessGroupManagerImpl.getInstance().exportGroups(context, fExportFile);
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#importCourseRightGroups(java.io.File)
	 */
	@Override
	public void importCourseRightGroups(final File fImportDirectory) {
		final File fGroupExportXML = new File(fImportDirectory, RIGHTGROUPEXPORT_XML);
		final BGContext context = findDefaultRightsContext();
		if (context == null) { throw new AssertException(
				"Unable to find default context for imported course. Should have been created before calling importCourseLearningGroups()"); }
		BusinessGroupManagerImpl.getInstance().importGroups(context, fGroupExportXML);
	}

	private BGContext findDefaultLearningContext() {
		final List contexts = getLearningGroupContexts();
		BGContext context = null;
		for (final Iterator iter = contexts.iterator(); iter.hasNext();) {
			context = (BGContext) iter.next();
			if (context.isDefaultContext()) {
				break;
			}
		}
		return context;
	}

	private BGContext findDefaultRightsContext() {
		final List contexts = getRightGroupContexts();
		BGContext context = null;
		for (final Iterator iter = contexts.iterator(); iter.hasNext();) {
			context = (BGContext) iter.next();
			if (context.isDefaultContext()) {
				break;
			}
		}
		return context;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getCoachesFromLearningGroups(String)
	 */
	@Override
	public List getCoachesFromLearningGroup(final String groupName) {
		final BaseSecurity secManager = BaseSecurityManager.getInstance();
		final List retVal = new ArrayList();
		List bgs = null;
		if (groupName != null) {
			// filtered by name
			bgs = getLearningGroupsFromAllContexts(groupName);
		} else {
			// no filter
			bgs = getAllLearningGroupsFromAllContexts();
		}
		for (int i = 0; i < bgs.size(); i++) {
			// iterates over all business group in the course, fetching the identities
			// of the business groups OWNER
			final BusinessGroup elm = (BusinessGroup) bgs.get(i);
			retVal.addAll(secManager.getIdentitiesOfSecurityGroup(elm.getOwnerGroup()));
		}
		return retVal;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getParticipantsFromLearningGroups(String)
	 */
	@Override
	public List getParticipantsFromLearningGroup(final String groupName) {
		final BaseSecurity secManager = BaseSecurityManager.getInstance();
		final List retVal = new ArrayList();
		List bgs = null;
		if (groupName != null) {
			// filtered by name
			bgs = getLearningGroupsFromAllContexts(groupName);
		} else {
			// no filter
			bgs = getAllLearningGroupsFromAllContexts();
		}
		for (int i = 0; i < bgs.size(); i++) {
			// iterates over all business group in the course, fetching the identities
			// of the business groups PARTICIPANTS
			final BusinessGroup elm = (BusinessGroup) bgs.get(i);
			retVal.addAll(secManager.getIdentitiesOfSecurityGroup(elm.getPartipiciantGroup()));
		}
		return retVal;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getCoachesFromArea(java.lang.String)
	 */
	@Override
	public List getCoachesFromArea(final String areaName) {
		final BaseSecurity secManager = BaseSecurityManager.getInstance();
		final List retVal = new ArrayList();
		List bgs = null;
		if (areaName != null) {
			bgs = getLearningGroupsInAreaFromAllContexts(areaName);
		} else {
			bgs = getAllLearningGroupsFromAllContexts();
		}
		for (int i = 0; i < bgs.size(); i++) {
			// iterates over all business group in the course's area, fetching the
			// OWNER identities
			final BusinessGroup elm = (BusinessGroup) bgs.get(i);
			retVal.addAll(secManager.getIdentitiesOfSecurityGroup(elm.getOwnerGroup()));
		}
		return retVal;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getParticipantsFromArea(java.lang.String)
	 */
	@Override
	public List getParticipantsFromArea(final String areaName) {
		final BaseSecurity secManager = BaseSecurityManager.getInstance();
		final List retVal = new ArrayList();
		List bgs = null;
		if (areaName != null) {
			bgs = getLearningGroupsInAreaFromAllContexts(areaName);
		} else {
			bgs = getAllLearningGroupsFromAllContexts();
		}
		for (int i = 0; i < bgs.size(); i++) {
			// iterates over all business group in the course's area, fetching the
			// PARTIPICIANT identities
			final BusinessGroup elm = (BusinessGroup) bgs.get(i);
			retVal.addAll(secManager.getIdentitiesOfSecurityGroup(elm.getPartipiciantGroup()));
		}
		return retVal;
	}

	/**
	 * @see org.olat.course.groupsandrights.CourseGroupManager#getWaitingListGroupsFromAllContexts(org.olat.core.id.Identity)
	 */
	@Override
	public List getWaitingListGroupsFromAllContexts(final Identity identity) {
		final List allGroups = new ArrayList();
		final BusinessGroupManager groupManager = BusinessGroupManagerImpl.getInstance();
		final Iterator iterator = learningGroupContexts.iterator();
		while (iterator.hasNext()) {
			final BGContext bgContext = (BGContext) iterator.next();
			final List contextGroups = groupManager.findBusinessGroupsWithWaitingListAttendedBy(bgContext.getGroupType(), identity, bgContext);
			allGroups.addAll(contextGroups);
		}
		return allGroups;
	}

	/**
	 * Archive all learning-group-contexts and right-group-contexts.
	 * 
	 * @param exportDirectory
	 */
	public void archiveCourseGroups(final File exportDirectory) {
		archiveAllContextFor(getLearningGroupContexts(), LEARNINGGROUPARCHIVE_XLS, exportDirectory);
		archiveAllContextFor(getRightGroupContexts(), RIGHTGROUPARCHIVE_XLS, exportDirectory);
	}

	/**
	 * Archive a list of context. Archive the default context in a xls file with prefix 'default_' e.g. default_learninggroupexport.xml. Archive all other context in xls
	 * files with prefix 'context_<CONTEXTCOUNTER>_' e.g. context_2_learninggroupexport.xml
	 * 
	 * @param contextList List of BGContext
	 * @param fileName E.g. learninggroupexport.xml
	 * @param exportDirectory Archive files will be created in this dir.
	 */
	private void archiveAllContextFor(final List contextList, final String fileName, final File exportDirectory) {
		int contextCounter = 1;
		for (final Iterator iter = contextList.iterator(); iter.hasNext();) {
			final BGContext context = (BGContext) iter.next();
			if (context.isDefaultContext()) {
				BusinessGroupManagerImpl.getInstance().archiveGroups(context, new File(exportDirectory, "default_" + fileName));
			} else {
				BusinessGroupManagerImpl.getInstance().archiveGroups(context, new File(exportDirectory, "context_" + contextCounter + "_" + fileName));
				contextCounter++;
			}
		}

	}

}