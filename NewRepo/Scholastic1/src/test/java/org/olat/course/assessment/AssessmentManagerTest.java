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

package org.olat.course.assessment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;

/**
 * Description:<br>
 * Assessment test class.
 * <P>
 * Initial Date: 26.08.2008 <br>
 * 
 * @author Lavinia Dumitrescu
 */
public class AssessmentManagerTest extends OlatTestCase {

	private static Logger log = Logger.getLogger(AssessmentManagerTest.class.getName());

	private AssessmentManager assessmentManager;
	private ICourse course;
	private AssessableCourseNode assessableCourseNode;
	private Identity tutor;
	private Identity student;
	private final Float score = new Float(10);
	private final Boolean passed = Boolean.TRUE;

	@Before
	public void setUp() throws Exception {
		try {
			log.info("setUp start ------------------------");

			tutor = JunitTestHelper.createAndPersistIdentityAsUser("junit_tutor");
			student = JunitTestHelper.createAndPersistIdentityAsUser("junit_student");

			// import "Demo course" into the bcroot_junittest
			final RepositoryEntry repositoryEntry = JunitTestHelper.deployDemoCourse();
			final Long resourceableId = repositoryEntry.getOlatResource().getResourceableId();
			System.out.println("Demo course imported - resourceableId: " + resourceableId);

			final DB db = DBFactory.getInstance();
			course = CourseFactory.loadCourse(resourceableId);
			DBFactory.getInstance().closeSession();

			course.getCourseEnvironment().getCourseConfig().setEfficencyStatementIsEnabled(true);
			course = CourseFactory.loadCourse(resourceableId);

			log.info("setUp done ------------------------");
		} catch (final RuntimeException e) {
			log.error("Exception in setUp(): " + e);
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() throws Exception {
		// TODO: Does not cleanup Demo-course because other Test which use Demo-Course too, will be have failures
		// remove demo course on file system
		// CourseFactory.deleteCourse(course);
		try {
			DBFactory.getInstance().closeSession();
		} catch (final Exception e) {
			log.error("tearDown failed: ", e);
		}
	}

	/**
	 * Tests the AssessmentManager methods.
	 */
	@Test
	public void testSaveScoreEvaluation() {
		System.out.println("Start testSaveScoreEvaluation");

		assertNotNull(course);
		// find an assessableCourseNode
		final List<CourseNode> assessableNodeList = AssessmentHelper.getAssessableNodes(course.getEditorTreeModel(), null);
		final Iterator<CourseNode> nodesIterator = assessableNodeList.iterator();
		boolean testNodeFound = false;
		while (nodesIterator.hasNext()) {
			final CourseNode currentNode = nodesIterator.next();
			if (currentNode instanceof AssessableCourseNode) {
				if (currentNode.getType().equalsIgnoreCase("iqtest")) {
					System.out.println("Yes, we found a test node! - currentNode.getType(): " + currentNode.getType());
					assessableCourseNode = (AssessableCourseNode) currentNode;
					testNodeFound = true;
					break;
				}
			}
		}
		assertTrue("found no test-node of type 'iqtest' (hint: add one to DemoCourse) ", testNodeFound);

		assessmentManager = course.getCourseEnvironment().getAssessmentManager();

		final Long assessmentID = new Long("123456");
		Integer attempts = 1;
		final String coachComment = "SomeUselessCoachComment";
		final String userComment = "UselessUserComment";

		// store ScoreEvaluation for the assessableCourseNode and student
		final ScoreEvaluation scoreEvaluation = new ScoreEvaluation(score, passed, assessmentID);

		final IdentityEnvironment ienv = new IdentityEnvironment();
		ienv.setIdentity(student);
		final UserCourseEnvironment userCourseEnv = new UserCourseEnvironmentImpl(ienv, course.getCourseEnvironment());
		final boolean incrementAttempts = true;
		// assessableCourseNode.updateUserScoreEvaluation(scoreEvaluation, userCourseEnv, tutor, incrementAttempts); //alternative
		assessmentManager.saveScoreEvaluation(assessableCourseNode, tutor, student, scoreEvaluation, userCourseEnv, incrementAttempts);
		DBFactory.getInstance().closeSession();
		// the attempts mut have been incremented
		// assertEquals(attempts, assessableCourseNode.getUserAttempts(userCourseEnv)); //alternative
		assertEquals(attempts, assessmentManager.getNodeAttempts(assessableCourseNode, student));

		assessmentManager.saveNodeCoachComment(assessableCourseNode, student, coachComment);
		// assessableCourseNode.updateUserCoachComment(coachComment, userCourseEnv); //alternative

		assessmentManager.saveNodeComment(assessableCourseNode, tutor, student, userComment);
		// assessableCourseNode.updateUserUserComment(userComment, userCourseEnv, tutor); //alternative

		attempts++;
		assessmentManager.saveNodeAttempts(assessableCourseNode, tutor, student, attempts);
		assertEquals(attempts, assessmentManager.getNodeAttempts(assessableCourseNode, student));
		// assessableCourseNode.updateUserAttempts(attempts, userCourseEnv, tutor); //alternative

		assertEquals(score, assessmentManager.getNodeScore(assessableCourseNode, student));
		assertEquals(passed, assessmentManager.getNodePassed(assessableCourseNode, student));
		assertEquals(assessmentID, assessmentManager.getAssessmentID(assessableCourseNode, student));

		assertEquals(coachComment, assessmentManager.getNodeCoachComment(assessableCourseNode, student));
		assertEquals(userComment, assessmentManager.getNodeComment(assessableCourseNode, student));

		System.out.println("Finish testing AssessmentManager read/write methods");

		checkEfficiencyStatementManager();
		assertNotNull("no course at the end of test", course);
		try {
			course = CourseFactory.loadCourse(course.getResourceableId());
		} catch (final Exception ex) {
			fail("Could not load course at the end of test Exception=" + ex);
		}
		assertNotNull("no course after loadCourse", course);
	}

	/**
	 * This assumes that the student identity has scoreEvaluation information stored into the o_property for at least one test node into the "Demo course". It tests the
	 * EfficiencyStatementManager methods.
	 */
	private void checkEfficiencyStatementManager() {
		System.out.println("Start testUpdateEfficiencyStatement");

		final List<Identity> identitiyList = new ArrayList<Identity>();
		identitiyList.add(student);

		final boolean checkForExistingProperty = false;

		final Long courseResId = course.getCourseEnvironment().getCourseResourceableId();
		final RepositoryEntry courseRepositoryEntry = RepositoryManager.getInstance().lookupRepositoryEntry(
				OresHelper.createOLATResourceableInstance(CourseModule.class, courseResId), false);
		assertNotNull(courseRepositoryEntry);
		// check the stored EfficiencyStatement
		EfficiencyStatement efficiencyStatement = checkEfficiencyStatement(courseRepositoryEntry);

		final EfficiencyStatementManager efficiencyStatementManager = EfficiencyStatementManager.getInstance();
		// force the storing of the efficiencyStatement - this is usually done only at Learnresource/modify properties/Efficiency statement (ON)
		efficiencyStatementManager.updateEfficiencyStatements(course, identitiyList, checkForExistingProperty);
		DBFactory.getInstance().closeSession();

		// archive the efficiencyStatement into a temporary dir
		try {
			final File archiveDir = File.createTempFile("junit", "output");
			if (archiveDir.exists()) {
				archiveDir.delete();
				if (archiveDir.mkdir()) {
					efficiencyStatementManager.archiveUserData(student, archiveDir);
					System.out.println("Archived EfficiencyStatement path: " + archiveDir.getAbsolutePath());
				}
			}
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// delete the efficiencyStatements for the current course
		efficiencyStatementManager.deleteEfficiencyStatementsFromCourse(courseRepositoryEntry.getKey());
		DBFactory.getInstance().closeSession();
		efficiencyStatement = efficiencyStatementManager.getUserEfficiencyStatement(courseRepositoryEntry.getKey(), student);
		DBFactory.getInstance().closeSession();
		assertNull(efficiencyStatement);

		// updateUserEfficiencyStatement of the student identity
		final IdentityEnvironment ienv = new IdentityEnvironment();
		ienv.setIdentity(student);
		final UserCourseEnvironment userCourseEnv = new UserCourseEnvironmentImpl(ienv, course.getCourseEnvironment());
		efficiencyStatementManager.updateUserEfficiencyStatement(userCourseEnv);
		DBFactory.getInstance().closeSession();
		// check again the stored EfficiencyStatement
		efficiencyStatement = checkEfficiencyStatement(courseRepositoryEntry);

		// delete the efficiencyStatement of the student
		efficiencyStatementManager.deleteUserData(student, "deleted_" + student.getName());
		DBFactory.getInstance().closeSession();
		efficiencyStatement = efficiencyStatementManager.getUserEfficiencyStatement(courseRepositoryEntry.getKey(), student);
		DBFactory.getInstance().closeSession();
		assertNull(efficiencyStatement);
	}

	/**
	 * Asserts that the stored efficiencyStatement is not null and it contains the correct score/passed info.
	 * 
	 * @param courseRepositoryEntry
	 * @return
	 */
	private EfficiencyStatement checkEfficiencyStatement(final RepositoryEntry courseRepositoryEntry) {
		final EfficiencyStatementManager efficiencyStatementManager = EfficiencyStatementManager.getInstance();
		// check the stored EfficiencyStatement
		final EfficiencyStatement efficiencyStatement = efficiencyStatementManager.getUserEfficiencyStatement(courseRepositoryEntry.getKey(), student);
		assertNotNull(efficiencyStatement);
		final List<Map<String, Object>> assessmentNodes = efficiencyStatement.getAssessmentNodes();
		final Iterator<Map<String, Object>> listIterator = assessmentNodes.iterator();
		while (listIterator.hasNext()) {
			final Map<String, Object> assessmentMap = listIterator.next();
			if (assessmentMap.get(AssessmentHelper.KEY_IDENTIFYER).equals(assessableCourseNode.getIdent())) {
				final String scoreString = (String) assessmentMap.get(AssessmentHelper.KEY_SCORE);
				System.out.println("scoreString: " + scoreString);
				assertEquals(score, new Float(scoreString));
			}
		}
		final Double scoreDouble = efficiencyStatementManager.getScore(assessableCourseNode.getIdent(), efficiencyStatement);
		System.out.println("scoreDouble: " + scoreDouble);
		assertEquals(new Double(score), efficiencyStatementManager.getScore(assessableCourseNode.getIdent(), efficiencyStatement));
		assertEquals(passed, efficiencyStatementManager.getPassed(assessableCourseNode.getIdent(), efficiencyStatement));
		return efficiencyStatement;
	}

}
