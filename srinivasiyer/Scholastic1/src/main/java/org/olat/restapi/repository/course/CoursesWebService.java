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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.restapi.repository.course;

import static org.olat.restapi.security.RestSecurityHelper.getRoles;
import static org.olat.restapi.security.RestSecurityHelper.getUserRequest;
import static org.olat.restapi.security.RestSecurityHelper.isAuthor;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.gui.UserRequest;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.config.CourseConfig;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.CourseNode;
import org.olat.course.tree.CourseEditorTreeNode;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.restapi.support.ObjectFactory;
import org.olat.restapi.support.vo.CourseConfigVO;
import org.olat.restapi.support.vo.CourseVO;

/**
 * Description:<br>
 * This web service handles the courses.
 * <P>
 * Initial Date: 27 apr. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
@Path("repo/courses")
public class CoursesWebService {

	private static final OLog log = Tracing.createLoggerFor(CoursesWebService.class);

	private static final String VERSION = "1.0";

	/**
	 * The version of the Course Web Service
	 * 
	 * @response.representation.200.mediaType text/plain
	 * @response.representation.200.doc The version of this specific Web Service
	 * @response.representation.200.example 1.0
	 * @return
	 */
	@GET
	@Path("version")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getVersion() {
		return Response.ok(VERSION).build();
	}

	/**
	 * Get all courses viewable by the authenticated user
	 * 
	 * @response.representation.200.qname {http://www.example.com}courseVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc List of visible courses
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSEVOes}
	 * @param request The HTTP request
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response getCourseList(@Context final HttpServletRequest request) {
		final Roles roles = getRoles(request);

		final List<String> courseType = new ArrayList<String>();
		courseType.add(CourseModule.getCourseTypeName());
		final List<RepositoryEntry> repoEntries = RepositoryManager.getInstance().genericANDQueryWithRolesRestriction("*", "*", "*", courseType, roles, "");

		final List<CourseVO> voList = new ArrayList<CourseVO>();
		for (final RepositoryEntry repoEntry : repoEntries) {
			try {
				final ICourse course = CourseFactory.loadCourse(repoEntry.getOlatResource().getResourceableId());
				voList.add(ObjectFactory.get(course));
			} catch (final Exception e) {
				log.error("Cannot load the course with this repository entry: " + repoEntry);
			}
		}

		final CourseVO[] vos = new CourseVO[voList.size()];
		voList.toArray(vos);
		return Response.ok(vos).build();
	}

	/**
	 * Creates an empty course
	 * 
	 * @response.representation.200.qname {http://www.example.com}courseVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The metadatas of the created course
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @param shortTitle The short title
	 * @param title The title
	 * @param sharedFolderSoftKey The repository entry key of a shared folder (optional)
	 * @param request The HTTP request
	 * @return It returns the id of the newly created Course
	 */
	@PUT
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response createEmptyCourse(@QueryParam("shortTitle") final String shortTitle, @QueryParam("title") final String title,
			@QueryParam("sharedFolderSoftKey") final String sharedFolderSoftKey, @Context final HttpServletRequest request) {
		if (!isAuthor(request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }
		final CourseConfigVO configVO = new CourseConfigVO();
		configVO.setSharedFolderSoftKey(sharedFolderSoftKey);

		final UserRequest ureq = getUserRequest(request);
		final ICourse course = createEmptyCourse(ureq.getIdentity(), shortTitle, title, configVO);
		final CourseVO vo = ObjectFactory.get(course);
		return Response.ok(vo).build();
	}

	/**
	 * Create an empty course with some defaults settings
	 * 
	 * @param identity
	 * @param name
	 * @param longTitle
	 * @param courseConfigVO
	 * @return
	 */
	public static ICourse createEmptyCourse(final Identity identity, final String name, final String longTitle, final CourseConfigVO courseConfigVO) {
		final String shortTitle = name;
		final String learningObjectives = name + " (Example of creating a new course)";

		try {
			// create the course resource
			final OLATResourceable oresable = OLATResourceManager.getInstance().createOLATResourceInstance(CourseModule.class);

			// create a repository entry
			final RepositoryEntry addedEntry = RepositoryManager.getInstance().createRepositoryEntryInstance(identity.getName());
			addedEntry.setCanDownload(false);
			addedEntry.setCanLaunch(true);
			addedEntry.setDisplayname(shortTitle);
			addedEntry.setResourcename("-");
			// Do set access for owner at the end, because unfinished course should be
			// invisible
			// addedEntry.setAccess(RepositoryEntry.ACC_OWNERS);
			addedEntry.setAccess(0);// Access for nobody

			// Set the resource on the repository entry and save the entry.
			final RepositoryManager rm = RepositoryManager.getInstance();
			// bind resource and repository entry
			final OLATResource ores = OLATResourceManager.getInstance().findOrPersistResourceable(oresable);
			addedEntry.setOlatResource(ores);

			// create an empty course
			ICourse course = CourseFactory.createEmptyCourse(oresable, shortTitle, longTitle, learningObjectives);
			// initialize course group management
			final CourseGroupManager cgm = course.getCourseEnvironment().getCourseGroupManager();
			cgm.createCourseGroupmanagement(course.getResourceableId().toString());

			// create security group
			final BaseSecurity securityManager = BaseSecurityManager.getInstance();
			final SecurityGroup newGroup = securityManager.createAndPersistSecurityGroup();
			// member of this group may modify member's membership
			securityManager.createAndPersistPolicy(newGroup, Constants.PERMISSION_ACCESS, newGroup);
			// members of this group are always authors also
			securityManager.createAndPersistPolicy(newGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);

			securityManager.addIdentityToSecurityGroup(identity, newGroup);
			addedEntry.setOwnerGroup(newGroup);
			// Do set access for owner at the end, because unfinished course should be
			// invisible
			addedEntry.setAccess(RepositoryEntry.ACC_OWNERS);

			rm.saveRepositoryEntry(addedEntry);

			securityManager.createAndPersistPolicy(addedEntry.getOwnerGroup(), Constants.PERMISSION_ADMIN, addedEntry.getOlatResource());

			// set root node title
			course = CourseFactory.openCourseEditSession(addedEntry.getOlatResource().getResourceableId());
			final String displayName = addedEntry.getDisplayname();
			course.getRunStructure().getRootNode().setShortTitle(Formatter.truncate(displayName, 25));
			course.getRunStructure().getRootNode().setLongTitle(displayName);

			final CourseNode rootNode = ((CourseEditorTreeNode) course.getEditorTreeModel().getRootNode()).getCourseNode();
			rootNode.setShortTitle(Formatter.truncate(displayName, 25));
			rootNode.setLongTitle(displayName);

			if (courseConfigVO != null) {
				final CourseConfig courseConfig = course.getCourseEnvironment().getCourseConfig();
				if (StringHelper.containsNonWhitespace(courseConfigVO.getSharedFolderSoftKey())) {
					courseConfig.setSharedFolderSoftkey(courseConfigVO.getSharedFolderSoftKey());
				}
			}
			RepositoryManager.getInstance().updateRepositoryEntry(addedEntry);

			CourseFactory.saveCourse(course.getResourceableId());
			CourseFactory.closeCourseEditSession(course.getResourceableId(), true);
			course = CourseFactory.loadCourse(oresable.getResourceableId());
			return course;
		} catch (final Exception e) {
			throw new WebApplicationException(e);
		}
	}
}
