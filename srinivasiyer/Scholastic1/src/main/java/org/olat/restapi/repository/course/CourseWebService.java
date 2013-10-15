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

import static org.olat.restapi.security.RestSecurityHelper.getIdentity;
import static org.olat.restapi.security.RestSecurityHelper.getUserRequest;
import static org.olat.restapi.security.RestSecurityHelper.isAuthor;
import static org.olat.restapi.security.RestSecurityHelper.isAuthorEditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.admin.securitygroup.gui.IdentitiesAddEvent;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.nodes.INode;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.editor.PublishProcess;
import org.olat.course.editor.StatusDescription;
import org.olat.course.tree.CourseEditorTreeModel;
import org.olat.course.tree.PublishTreeModel;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.restapi.support.ObjectFactory;
import org.olat.restapi.support.vo.CourseConfigVO;
import org.olat.restapi.support.vo.CourseVO;
import org.olat.user.restapi.UserVO;
import org.olat.user.restapi.UserVOFactory;

import com.thoughtworks.xstream.XStream;

/**
 * Description:<br>
 * This web service will handle the functionality related to <code>Course</code> and its contents.
 * <P>
 * Initial Date: 27 apr. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
@Path("repo/courses/{courseId}")
public class CourseWebService {

	private static final OLog log = Tracing.createLoggerFor(CourseWebService.class);
	private static final XStream myXStream = XStreamHelper.createXStreamInstance();

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

	@Path("groups")
	public CourseGroupWebService getCourseGroupWebService(@PathParam("courseId") final Long courseId) {
		final OLATResource ores = getCourseOLATResource(courseId);
		return new CourseGroupWebService(ores);
	}

	/**
	 * Publish the course.
	 * 
	 * @response.representation.200.qname {http://www.example.com}courseVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The metadatas of the created course
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param locale The course locale
	 * @param request The HTTP request
	 * @return It returns the metadatas of the published course.
	 */
	@POST
	@Path("publish")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response publishCourse(@PathParam("courseId") final Long courseId, @QueryParam("locale") final Locale locale, @Context final HttpServletRequest request) {
		if (!isAuthor(request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final UserRequest ureq = getUserRequest(request);
		final ICourse course = loadCourse(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }
		publishCourse(course, ureq.getIdentity(), locale);
		final CourseVO vo = ObjectFactory.get(course);
		return Response.ok(vo).build();
	}

	/**
	 * Publish a course
	 * 
	 * @param course
	 * @param identity
	 * @param locale
	 */
	private void publishCourse(ICourse course, final Identity identity, final Locale locale) {
		try {
			final CourseEditorTreeModel cetm = course.getEditorTreeModel();
			final PublishProcess publishProcess = PublishProcess.getInstance(course, cetm, locale);
			final PublishTreeModel publishTreeModel = publishProcess.getPublishTreeModel();

			final int newAccess = RepositoryEntry.ACC_USERS;
			// access rule -> all users can the see course
			// RepositoryEntry.ACC_OWNERS
			// only owners can the see course
			// RepositoryEntry.ACC_OWNERS_AUTHORS //only owners and authors can the see course
			// RepositoryEntry.ACC_USERS_GUESTS // users and guests can see the course
			publishProcess.changeGeneralAccess(null, newAccess);

			if (publishTreeModel.hasPublishableChanges()) {
				final List<String> nodeToPublish = new ArrayList<String>();
				visitPublishModel(publishTreeModel.getRootNode(), publishTreeModel, nodeToPublish);

				publishProcess.createPublishSetFor(nodeToPublish);
				final StatusDescription[] status = publishProcess.testPublishSet(locale);
				// publish not possible when there are errors
				for (int i = 0; i < status.length; i++) {
					if (status[i].isError()) { return; }
				}
			}

			course = CourseFactory.openCourseEditSession(course.getResourceableId());
			publishProcess.applyPublishSet(identity, locale);
			CourseFactory.closeCourseEditSession(course.getResourceableId(), true);
		} catch (final Throwable e) {
			throw new WebApplicationException(e);
		}
	}

	private void visitPublishModel(final TreeNode node, final PublishTreeModel publishTreeModel, final Collection<String> nodeToPublish) {
		final int numOfChildren = node.getChildCount();
		for (int i = 0; i < numOfChildren; i++) {
			final INode child = node.getChildAt(i);
			if (child instanceof TreeNode) {
				nodeToPublish.add(child.getIdent());
				visitPublishModel((TreeNode) child, publishTreeModel, nodeToPublish);
			}
		}
	}

	/**
	 * Get the metadatas of the course by id
	 * 
	 * @response.representation.200.qname {http://www.example.com}courseVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The metadatas of the created course
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSEVO}
	 * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @return It returns the <code>CourseVO</code> object representing the course.
	 */
	@GET
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response findById(@PathParam("courseId") final Long courseId) {
		final ICourse course = loadCourse(courseId);
		if (course == null) { return Response.serverError().status(Status.NOT_FOUND).build(); }
		final CourseVO vo = ObjectFactory.get(course);
		return Response.ok(vo).build();
	}

	/**
	 * Delete a course by id
	 * 
	 * @response.representation.200.doc The metadatas of the created course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param request The HTTP request
	 * @return It returns the XML representation of the <code>Structure</code> object representing the course.
	 */
	@DELETE
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response deleteCourse(@PathParam("courseId") final Long courseId, @Context final HttpServletRequest request) {
		if (!isAuthor(request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final ICourse course = loadCourse(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }
		// FIXME: this does not remove all data from the database, see repositoryManger
		CourseFactory.deleteCourse(course);
		return Response.ok().build();
	}

	/**
	 * Get the configuration of the course
	 * 
	 * @response.representation.200.qname {http://www.example.com}courseConfigVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The configuration of the course
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSECONFIGVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param request The HTTP request
	 * @return It returns the XML representation of the <code>Structure</code> object representing the course.
	 */
	@GET
	@Path("configuration")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response getConfiguration(@PathParam("courseId") final Long courseId, @Context final HttpServletRequest request) {
		if (!isAuthor(request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final ICourse course = loadCourse(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }
		final CourseConfigVO vo = ObjectFactory.getConfig(course);
		return Response.ok(vo).build();
	}

	/**
	 * Get the runstructure of the course by id
	 * 
	 * @response.representation.200.mediaType application/xml
	 * @response.representation.200.doc The run structure of the course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param httpRequest The HTTP request
	 * @param request The REST request
	 * @return It returns the XML representation of the <code>Structure</code> object representing the course.
	 */
	@GET
	@Path("runstructure")
	@Produces(MediaType.APPLICATION_XML)
	public Response findRunStructureById(@PathParam("courseId") final Long courseId, @Context final HttpServletRequest httpRequest, @Context final Request request) {
		if (!isAuthor(httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final ICourse course = loadCourse(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }
		final VFSItem runStructureItem = course.getCourseBaseContainer().resolve("runstructure.xml");
		final Date lastModified = new Date(runStructureItem.getLastModified());

		final Response.ResponseBuilder response = request.evaluatePreconditions(lastModified);
		if (response == null) { return Response.ok(myXStream.toXML(course.getRunStructure())).build(); }
		return response.build();
	}

	/**
	 * Get the editor tree model of the course by id
	 * 
	 * @response.representation.200.mediaType application/xml
	 * @response.representation.200.doc The editor tree model of the course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param httpRequest The HTTP request
	 * @param request The REST request
	 * @return It returns the XML representation of the <code>Editor model</code> object representing the course.
	 */
	@GET
	@Path("editortreemodel")
	@Produces(MediaType.APPLICATION_XML)
	public Response findEditorTreeModelById(@PathParam("courseId") final Long courseId, @Context final HttpServletRequest httpRequest, @Context final Request request) {
		if (!isAuthor(httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final ICourse course = loadCourse(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }
		final VFSItem editorModelItem = course.getCourseBaseContainer().resolve("editortreemodel.xml");
		final Date lastModified = new Date(editorModelItem.getLastModified());

		final Response.ResponseBuilder response = request.evaluatePreconditions(lastModified);
		if (response == null) { return Response.ok(myXStream.toXML(course.getEditorTreeModel())).build(); }
		return response.build();
	}

	/**
	 * Get all owners and authors of the course
	 * 
	 * @response.representation.200.qname {http://www.example.com}userVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The array of authors
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param httpRequest The HTTP request
	 * @return It returns an array of <code>UserVO</code>
	 */
	@GET
	@Path("authors")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response getAuthors(@PathParam("courseId") final Long courseId, @Context final HttpServletRequest httpRequest) {
		if (!isAuthor(httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final OLATResourceable course = getCourseOLATResource(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course, true);
		final SecurityGroup sg = repositoryEntry.getOwnerGroup();

		final BaseSecurity securityManager = BaseSecurityManager.getInstance();
		final List<Object[]> owners = securityManager.getIdentitiesAndDateOfSecurityGroup(sg);

		int count = 0;
		final UserVO[] authors = new UserVO[owners.size()];
		for (int i = 0; i < owners.size(); i++) {
			final Identity identity = (Identity) owners.get(i)[0];
			authors[count++] = UserVOFactory.get(identity);
		}
		return Response.ok(authors).build();
	}

	/**
	 * Get this specific author and owner of the course
	 * 
	 * @response.representation.200.qname {http://www.example.com}userVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The author
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found or the user is not an onwer or author of the course
	 * @param courseId The course resourceable's id
	 * @param identityKey The user identifier
	 * @param httpRequest The HTTP request
	 * @return It returns an <code>UserVO</code>
	 */
	@GET
	@Path("authors/{identityKey}")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response getAuthor(@PathParam("courseId") final Long courseId, @PathParam("identityKey") final Long identityKey, @Context final HttpServletRequest httpRequest) {
		if (!isAuthor(httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final OLATResourceable course = getCourseOLATResource(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course, true);
		final SecurityGroup sg = repositoryEntry.getOwnerGroup();

		final BaseSecurity securityManager = BaseSecurityManager.getInstance();
		final SecurityGroup authorGroup = securityManager.findSecurityGroupByName(Constants.GROUP_AUTHORS);

		final Identity author = securityManager.loadIdentityByKey(identityKey, false);
		if (securityManager.isIdentityInSecurityGroup(author, sg) && securityManager.isIdentityInSecurityGroup(author, authorGroup)) {
			final UserVO vo = UserVOFactory.get(author);
			return Response.ok(vo).build();
		}
		return Response.ok(author).build();
	}

	/**
	 * Add an owner and author to the course
	 * 
	 * @response.representation.200.doc The user is an author and owner of the course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course or the user not found
	 * @param courseId The course resourceable's id
	 * @param identityKey The user identifier
	 * @param httpRequest The HTTP request
	 * @return It returns 200 if the user is added as owner and author of the course
	 */
	@PUT
	@Path("authors/{identityKey}")
	public Response addAuthor(@PathParam("courseId") final Long courseId, @PathParam("identityKey") final Long identityKey, @Context final HttpServletRequest httpRequest) {
		if (!isAuthor(httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final OLATResourceable course = getCourseOLATResource(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final BaseSecurity securityManager = BaseSecurityManager.getInstance();
		final Identity author = securityManager.loadIdentityByKey(identityKey, false);
		if (author == null) { return Response.serverError().status(Status.NOT_FOUND).build(); }

		final Identity identity = getIdentity(httpRequest);

		final SecurityGroup authorGroup = securityManager.findSecurityGroupByName(Constants.GROUP_AUTHORS);
		final boolean hasBeenAuthor = securityManager.isIdentityInSecurityGroup(author, authorGroup);
		if (!hasBeenAuthor) {
			// not an author already, add this identity to the security group "authors"
			securityManager.addIdentityToSecurityGroup(author, authorGroup);
		}

		// add the author as owner of the course
		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course, true);
		final List<Identity> authors = Collections.singletonList(author);
		final IdentitiesAddEvent identitiesAddedEvent = new IdentitiesAddEvent(authors);
		rm.addOwners(identity, identitiesAddedEvent, repositoryEntry);

		return Response.ok().build();
	}

	/**
	 * Remove an owner and author to the course
	 * 
	 * @response.representation.200.doc The user was successfully removed as owner of the course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course or the user not found
	 * @param courseId The course resourceable's id
	 * @param identityKey The user identifier
	 * @param httpRequest The HTTP request
	 * @return It returns 200 if the user is removed as owner of the course
	 */
	@DELETE
	@Path("authors/{identityKey}")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response removeAuthor(@PathParam("courseId") final Long courseId, @PathParam("identityKey") final Long identityKey,
			@Context final HttpServletRequest httpRequest) {
		if (!isAuthor(httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final OLATResourceable course = getCourseOLATResource(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final BaseSecurity securityManager = BaseSecurityManager.getInstance();
		final Identity author = securityManager.loadIdentityByKey(identityKey, false);
		if (author == null) { return Response.serverError().status(Status.NOT_FOUND).build(); }

		final Identity identity = getIdentity(httpRequest);

		// remove the author as owner of the course
		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course, true);
		final List<Identity> authors = Collections.singletonList(author);
		rm.removeOwners(identity, authors, repositoryEntry);

		return Response.ok().build();
	}

	private OLATResource getCourseOLATResource(final Long courseId) {
		final String typeName = OresHelper.calculateTypeName(CourseModule.class);
		OLATResource ores = OLATResourceManager.getInstance().findResourceable(courseId, typeName);
		if (ores == null && Settings.isJUnitTest()) {
			// hack for the BGContextManagerImpl which load the course
			ores = OLATResourceManager.getInstance().findResourceable(courseId, "junitcourse");
		}
		return ores;
	}

	private ICourse loadCourse(final Long courseId) {
		try {
			final ICourse course = CourseFactory.loadCourse(courseId);
			return course;
		} catch (final Exception ex) {
			log.error("cannot load course with id: " + courseId, ex);
			return null;
		}
	}
}
