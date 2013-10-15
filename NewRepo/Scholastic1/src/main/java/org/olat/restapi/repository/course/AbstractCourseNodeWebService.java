package org.olat.restapi.repository.course;

import static org.olat.restapi.security.RestSecurityHelper.getIdentity;
import static org.olat.restapi.security.RestSecurityHelper.isAuthor;
import static org.olat.restapi.security.RestSecurityHelper.isAuthorEditor;
import static org.olat.restapi.support.ObjectFactory.get;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.condition.Condition;
import org.olat.course.nodes.AbstractAccessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.CourseNodeConfiguration;
import org.olat.course.nodes.CourseNodeFactory;
import org.olat.course.tree.CourseEditorTreeNode;
import org.olat.modules.ModuleConfiguration;
import org.olat.restapi.support.vo.CourseNodeVO;

public abstract class AbstractCourseNodeWebService {

	private static final OLog log = Tracing.createLoggerFor(AbstractCourseNodeWebService.class);

	private static final String CONDITION_ID_ACCESS = "accessability";
	private static final String CONDITION_ID_VISIBILITY = "visibility";

	protected ICourse loadCourse(final Long courseId) {
		try {
			final ICourse course = CourseFactory.loadCourse(courseId);
			return course;
		} catch (final Exception ex) {
			log.error("cannot load course with id: " + courseId, ex);
			return null;
		}
	}

	private CourseEditSession openEditSession(ICourse course, final Identity identity) {
		final LockResult lock = CoordinatorManager.getInstance().getCoordinator().getLocker().acquireLock(course, identity, CourseFactory.COURSE_EDITOR_LOCK);
		if (lock.isSuccess()) {
			course = CourseFactory.openCourseEditSession(course.getResourceableId());
		}
		return new CourseEditSession(course, lock);
	}

	protected Response attach(final Long courseId, final String parentNodeId, final String type, final Integer position, final String shortTitle, final String longTitle,
			final String objectives, final String visibilityExpertRules, final String accessExpertRules, final CustomConfigDelegate config,
			final HttpServletRequest request) {
		if (!isAuthor(request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		if (config != null && !config.isValid()) { return Response.serverError().status(Status.NOT_ACCEPTABLE).build(); }

		final ICourse course = loadCourse(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }
		final CourseNode parentNode = getParentNode(course, parentNodeId);
		if (parentNode == null) { return Response.serverError().status(Status.NOT_FOUND).build(); }

		CourseEditSession editSession = null;
		try {
			editSession = openEditSession(course, getIdentity(request));
			if (!editSession.canEdit()) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

			final CourseNodeVO newNode = createCourseNode(type, shortTitle, longTitle, objectives, visibilityExpertRules, accessExpertRules, config, editSession,
					parentNode, position);
			return Response.ok(newNode).build();
		} catch (final Exception ex) {
			log.error("Error while adding an enrolment building block", ex);
			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			saveAndCloseCourse(editSession);
		}
	}

	protected Response attachNodeConfig(final Long courseId, final String nodeId, final FullConfigDelegate config, final HttpServletRequest request) {
		if (!isAuthor(request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		if (config == null || !config.isValid()) { return Response.serverError().status(Status.CONFLICT).build(); }

		final ICourse course = loadCourse(courseId);
		if (course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final CourseNode courseNode = getParentNode(course, nodeId);
		if (courseNode == null) { return Response.serverError().status(Status.NOT_FOUND).build(); }

		if (!config.isApplicable(course, courseNode)) { return Response.serverError().status(Status.NOT_ACCEPTABLE).build(); }

		CourseEditSession editSession = null;
		try {
			editSession = openEditSession(course, getIdentity(request));
			if (!editSession.canEdit()) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }
			final ModuleConfiguration moduleConfig = courseNode.getModuleConfiguration();
			config.configure(course, courseNode, moduleConfig);

			return Response.ok().build();
		} catch (final Exception ex) {
			log.error("Error while adding an enrolment building block", ex);
			return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			saveAndCloseCourse(editSession);
		}
	}

	private CourseNodeVO createCourseNode(final String type, final String shortTitle, final String longTitle, final String learningObjectives,
			final String visibilityExpertRules, final String accessExpertRules, final CustomConfigDelegate delegateConfig, final CourseEditSession editSession,
			final CourseNode parentNode, final Integer position) {

		final CourseNodeConfiguration newNodeConfig = CourseNodeFactory.getInstance().getCourseNodeConfiguration(type);
		final CourseNode insertedNode = newNodeConfig.getInstance();
		insertedNode.setShortTitle(shortTitle);
		insertedNode.setLongTitle(longTitle);
		insertedNode.setLearningObjectives(learningObjectives);
		insertedNode.setNoAccessExplanation("You don't have access");

		if (StringHelper.containsNonWhitespace(visibilityExpertRules)) {
			final Condition cond = this.createExpertCondition(CONDITION_ID_VISIBILITY, visibilityExpertRules);
			insertedNode.setPreConditionVisibility(cond);
		}

		if (StringHelper.containsNonWhitespace(accessExpertRules) && insertedNode instanceof AbstractAccessableCourseNode) {
			final Condition cond = createExpertCondition(CONDITION_ID_ACCESS, accessExpertRules);
			((AbstractAccessableCourseNode) insertedNode).setPreConditionAccess(cond);
		}

		final ICourse course = editSession.getCourse();
		if (delegateConfig != null) {
			final ModuleConfiguration moduleConfig = insertedNode.getModuleConfiguration();
			delegateConfig.configure(course, insertedNode, moduleConfig);
		}

		if (position == null || position.intValue() < 0) {
			course.getEditorTreeModel().addCourseNode(insertedNode, parentNode);
		} else {
			course.getEditorTreeModel().insertCourseNodeAt(insertedNode, parentNode, position);
		}

		final CourseEditorTreeNode editorNode = course.getEditorTreeModel().getCourseEditorNodeContaining(insertedNode);
		final CourseNodeVO vo = get(insertedNode);
		vo.setParentId(editorNode.getParent() == null ? null : editorNode.getParent().getIdent());
		return vo;
	}

	protected Condition createExpertCondition(final String conditionId, final String expertRules) {
		final Condition cond = new Condition();
		cond.setConditionExpression(expertRules);
		cond.setExpertMode(true);
		cond.setConditionId(conditionId);
		return cond;
	}

	protected CourseNode getParentNode(final ICourse course, final String parentNodeId) {
		if (parentNodeId == null) {
			return course.getRunStructure().getRootNode();
		} else {
			return course.getEditorTreeModel().getCourseNode(parentNodeId);
		}
	}

	private void saveAndCloseCourse(final CourseEditSession editSession) {
		if (editSession == null || !editSession.canEdit()) { return; }

		CourseFactory.saveCourseEditorTreeModel(editSession.getCourseId());
		CourseFactory.fireModifyCourseEvent(editSession.getCourseId());// close the edit session too
		CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(editSession.getLock());
	}

	public interface CustomConfigDelegate {

		public boolean isValid();

		public void configure(ICourse course, CourseNode newNode, ModuleConfiguration moduleConfig);
	}

	public interface FullConfigDelegate extends CustomConfigDelegate {
		public boolean isApplicable(ICourse course, CourseNode courseNode);
	}

	private class CourseEditSession {
		private final ICourse course;
		private final LockResult entry;

		public CourseEditSession(final ICourse course, final LockResult entry) {
			this.course = course;
			this.entry = entry;
		}

		public Long getCourseId() {
			return course.getResourceableId();
		}

		public ICourse getCourse() {
			return course;
		}

		public LockResult getLock() {
			return entry;
		}

		public boolean canEdit() {
			return course != null && entry.isSuccess();
		}
	}
}
