package org.olat.modules.fo.restapi;

import static org.olat.restapi.security.RestSecurityHelper.isAuthor;
import static org.olat.restapi.security.RestSecurityHelper.isAuthorEditor;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.id.Identity;
import org.olat.course.ICourse;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.FOCourseNode;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.Message;
import org.olat.properties.Property;
import org.olat.restapi.repository.course.AbstractCourseNodeWebService;
import org.olat.restapi.repository.course.AbstractCourseNodeWebService.CustomConfigDelegate;
import org.olat.restapi.security.RestSecurityHelper;

/**
 * Description:<br>
 * REST API implementation for forum course node
 * <P>
 * Initial Date: 20.12.2010 <br>
 * 
 * @author skoeber
 */
@Path("repo/courses/{courseId}/elements/forum")
public class ForumCourseNodeWebService extends AbstractCourseNodeWebService {

	/**
	 * This attaches a Forum Element onto a given course. The element will be inserted underneath the supplied parentNodeId.
	 * 
	 * @response.representation.200.qname {http://www.example.com}courseNodeVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The course node metadatas
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSENODEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course or parentNode not found
	 * @param courseId The course resourceable's id
	 * @param parentNodeId The node's id which will be the parent of this single page
	 * @param position The node's position relative to its sibling nodes (optional)
	 * @param shortTitle The node short title
	 * @param longTitle The node long title
	 * @param objectives The node learning objectives
	 * @param visibilityExpertRules The rules to view the node (optional)
	 * @param accessExpertRules The rules to access the node (optional)
	 * @param request The HTTP request
	 * @return The persisted Forum Element (fully populated)
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response attachForumPost(@PathParam("courseId") final Long courseId, @FormParam("parentNodeId") final String parentNodeId,
			@FormParam("position") final Integer position, @FormParam("shortTitle") @DefaultValue("undefined") final String shortTitle,
			@FormParam("longTitle") @DefaultValue("undefined") final String longTitle, @FormParam("objectives") @DefaultValue("undefined") final String objectives,
			@FormParam("visibilityExpertRules") final String visibilityExpertRules, @FormParam("accessExpertRules") final String accessExpertRules,
			@Context final HttpServletRequest request) {
		return attachForum(courseId, parentNodeId, position, shortTitle, longTitle, objectives, visibilityExpertRules, accessExpertRules, request);
	}

	/**
	 * This attaches a Forum Element onto a given course. The element will be inserted underneath the supplied parentNodeId.
	 * 
	 * @response.representation.200.qname {http://www.example.com}courseNodeVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The course node metadatas
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSENODEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course or parentNode not found
	 * @param courseId The course resourceable id
	 * @param parentNodeId The node's id which will be the parent of this single page
	 * @param position The node's position relative to its sibling nodes (optional)
	 * @param shortTitle The node short title
	 * @param longTitle The node long title
	 * @param objectives The node learning objectives
	 * @param visibilityExpertRules The rules to view the node (optional)
	 * @param accessExpertRules The rules to access the node (optional)
	 * @param request The HTTP request
	 * @return The persisted Forum Element (fully populated)
	 */
	@PUT
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response attachForum(@PathParam("courseId") final Long courseId, @QueryParam("parentNodeId") final String parentNodeId,
			@QueryParam("position") final Integer position, @QueryParam("shortTitle") @DefaultValue("undefined") final String shortTitle,
			@QueryParam("longTitle") @DefaultValue("undefined") final String longTitle, @QueryParam("objectives") @DefaultValue("undefined") final String objectives,
			@QueryParam("visibilityExpertRules") final String visibilityExpertRules, @QueryParam("accessExpertRules") final String accessExpertRules,
			@Context final HttpServletRequest request) {
		final ForumCustomConfig config = new ForumCustomConfig();
		return attach(courseId, parentNodeId, "fo", position, shortTitle, longTitle, objectives, visibilityExpertRules, accessExpertRules, config, request);
	}

	/**
	 * Creates a new thread in the forum of the course node
	 * 
	 * @response.representation.200.qname {http://www.example.com}messageVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The root message of the thread
	 * @response.representation.200.example {@link org.olat.modules.fo.restapi.Examples#SAMPLE_MESSAGEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The author, forum or message not found
	 * @param courseId The id of the course.
	 * @param nodeId The id of the course node.
	 * @param title The title for the first post in the thread
	 * @param body The body for the first post in the thread
	 * @param identityName The author identity name (optional)
	 * @param sticky Creates sticky thread.
	 * @param request The HTTP request
	 * @return The new thread
	 */
	@PUT
	@Path("{nodeId}/thread")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response newThreadToForum(@PathParam("courseId") final Long courseId, @PathParam("nodeId") final String nodeId, @QueryParam("title") final String title,
			@QueryParam("body") final String body, @QueryParam("identityName") final String identityName, @QueryParam("sticky") final Boolean isSticky,
			@Context final HttpServletRequest request) {

		return addMessage(courseId, nodeId, null, title, body, identityName, isSticky, request);
	}

	/**
	 * Creates a new forum message in the forum of the course node
	 * 
	 * @response.representation.200.qname {http://www.example.com}messageVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The root message of the thread
	 * @response.representation.200.example {@link org.olat.modules.fo.restapi.Examples#SAMPLE_MESSAGEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The author, forum or message not found
	 * @param courseId The id of the course.
	 * @param nodeId The id of the course node.
	 * @param parentMessageId The id of the parent message.
	 * @param title The title for the first post in the thread
	 * @param body The body for the first post in the thread
	 * @param identityName The author identity name (optional)
	 * @param request The HTTP request
	 * @return The new thread
	 */
	@PUT
	@Path("{nodeId}/message")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response newMessageToForum(@PathParam("courseId") final Long courseId, @PathParam("nodeId") final String nodeId,
			@QueryParam("parentMessageId") final Long parentMessageId, @QueryParam("title") final String title, @QueryParam("body") final String body,
			@QueryParam("identityName") final String identityName, @Context final HttpServletRequest request) {

		if (parentMessageId == null || parentMessageId == 0L) { return Response.serverError().status(Status.NOT_FOUND).build(); }

		return addMessage(courseId, nodeId, parentMessageId, title, body, identityName, false, request);
	}

	/**
	 * Internal helper method to add a message to a forum.
	 * 
	 * @param courseId
	 * @param nodeId
	 * @param parentMessageId can be null (will lead to new thread)
	 * @param title
	 * @param body
	 * @param identityName
	 * @param isSticky only necessary when adding new thread
	 * @param request
	 * @return
	 */
	private Response addMessage(final Long courseId, final String nodeId, final Long parentMessageId, final String title, final String body, final String identityName,
			final Boolean isSticky, final HttpServletRequest request) {
		if (!isAuthor(request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final BaseSecurity securityManager = BaseSecurityManager.getInstance();

		Identity identity;
		if (identityName != null) {
			identity = securityManager.findIdentityByName(identityName);
		} else {
			identity = RestSecurityHelper.getIdentity(request);
		}

		if (identity == null) { return Response.serverError().status(Status.NOT_FOUND).build(); }

		// load forum
		final ICourse course = loadCourse(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final CourseNode courseNode = getParentNode(course, nodeId);
		if (courseNode == null) { return Response.serverError().status(Status.NOT_FOUND).build(); }

		final CoursePropertyManager cpm = course.getCourseEnvironment().getCoursePropertyManager();
		final Property forumKeyProp = cpm.findCourseNodeProperty(courseNode, null, null, FOCourseNode.FORUM_KEY);
		Forum forum = null;
		final ForumManager fom = ForumManager.getInstance();
		if (forumKeyProp != null) {
			// Forum does already exist, load forum with key from properties
			final Long forumKey = forumKeyProp.getLongValue();
			forum = fom.loadForum(forumKey);
		}

		if (forum == null) { return Response.serverError().status(Status.NOT_FOUND).build(); }

		MessageVO vo;

		if (parentMessageId == null || parentMessageId == 0L) {
			// creating the thread (a message without a parent message)
			final Message newThread = fom.createMessage();
			if (isSticky != null && isSticky.booleanValue()) {
				// set sticky
				final org.olat.modules.fo.Status status = new org.olat.modules.fo.Status();
				status.setSticky(true);
				newThread.setStatusCode(org.olat.modules.fo.Status.getStatusCode(status));
			}
			newThread.setTitle(title);
			newThread.setBody(body);
			// open a new thread
			fom.addTopMessage(identity, forum, newThread);

			vo = new MessageVO(newThread);
		} else {
			// adding response message (a message with a parent message)
			final Message threadMessage = fom.loadMessage(parentMessageId);

			if (threadMessage == null) { return Response.serverError().status(Status.NOT_FOUND).build(); }
			// create new message
			final Message message = fom.createMessage();
			message.setTitle(title);
			message.setBody(body);

			fom.replyToMessage(message, identity, threadMessage);

			vo = new MessageVO(message);
		}

		return Response.ok(vo).build();
	}

}

class ForumCustomConfig implements CustomConfigDelegate {

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void configure(final ICourse course, final CourseNode newNode, final ModuleConfiguration moduleConfig) {
		// create the forum
		final ForumManager fom = ForumManager.getInstance();
		final CoursePropertyManager cpm = course.getCourseEnvironment().getCoursePropertyManager();
		final Forum forum = fom.addAForum();
		final Long forumKey = forum.getKey();
		final Property forumKeyProperty = cpm.createCourseNodePropertyInstance(newNode, null, null, FOCourseNode.FORUM_KEY, null, forumKey, null, null);
		cpm.saveProperty(forumKeyProperty);
	}
}
