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

import java.util.HashMap;

import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * Initial Date: 08.02.2005
 * 
 * @author Mike Stock
 */
final class PreviewAssessmentManager extends BasicManager implements AssessmentManager {
	private final HashMap nodeScores = new HashMap();
	private final HashMap nodePassed = new HashMap();
	private final HashMap nodeAttempts = new HashMap();
	private final HashMap nodeAssessmentID = new HashMap();

	/**
	 * @see org.olat.course.assessment.AssessmentManager#saveNodeScore(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity, org.olat.core.id.Identity,
	 *      java.lang.Float)
	 */
	private void saveNodeScore(final CourseNode courseNode, final Identity identity, final Identity assessedIdentity, final Float score) {
		nodeScores.put(courseNode.getIdent(), score);
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#saveNodeAttempts(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity, org.olat.core.id.Identity,
	 *      java.lang.Integer)
	 */
	@Override
	public void saveNodeAttempts(final CourseNode courseNode, final Identity identity, final Identity assessedIdentity, final Integer attempts) {
		nodeAttempts.put(courseNode.getIdent(), attempts);
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#saveNodeComment(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity, org.olat.core.id.Identity,
	 *      java.lang.String)
	 */
	@Override
	public void saveNodeComment(final CourseNode courseNode, final Identity identity, final Identity assessedIdentity, final String comment) {
		throw new AssertException("Not implemented for preview.");
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#saveNodeCoachComment(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity, java.lang.String)
	 */
	@Override
	public void saveNodeCoachComment(final CourseNode courseNode, final Identity assessedIdentity, final String comment) {
		throw new AssertException("Not implemented for preview.");
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#saveNodePassed(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity, org.olat.core.id.Identity,
	 *      java.lang.Boolean)
	 */
	private void saveNodePassed(final CourseNode courseNode, final Identity identity, final Identity assessedIdentity, final Boolean passed) {
		nodePassed.put(courseNode.getIdent(), passed);
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#incrementNodeAttempts(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity)
	 */
	@Override
	public void incrementNodeAttempts(final CourseNode courseNode, final Identity identity, final UserCourseEnvironment userCourseEnvironment) {
		Integer attempts = (Integer) nodeAttempts.get(courseNode.getIdent());
		if (attempts == null) {
			attempts = new Integer(0);
		}
		int iAttempts = attempts.intValue();
		iAttempts++;
		nodeAttempts.put(courseNode.getIdent(), new Integer(iAttempts));
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#incrementNodeAttemptsInBackground(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity,
	 *      org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public void incrementNodeAttemptsInBackground(final CourseNode courseNode, final Identity identity, final UserCourseEnvironment userCourseEnvironment) {
		incrementNodeAttempts(courseNode, identity, userCourseEnvironment);
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#getNodeScore(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity)
	 */
	@Override
	public Float getNodeScore(final CourseNode courseNode, final Identity identity) {
		return (Float) nodeScores.get(courseNode.getIdent());
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#getNodeComment(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity)
	 */
	@Override
	public String getNodeComment(final CourseNode courseNode, final Identity identity) {
		return "This is a preview"; // default comment for preview
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#getNodeCoachComment(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity)
	 */
	@Override
	public String getNodeCoachComment(final CourseNode courseNode, final Identity identity) {
		return "This is a preview"; // default comment for preview
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#getNodePassed(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity)
	 */
	@Override
	public Boolean getNodePassed(final CourseNode courseNode, final Identity identity) {
		return (Boolean) nodePassed.get(courseNode.getIdent());
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#getNodeAttempts(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity)
	 */
	@Override
	public Integer getNodeAttempts(final CourseNode courseNode, final Identity identity) {
		final Integer attempts = (Integer) nodeAttempts.get(courseNode.getIdent());
		return (attempts == null ? new Integer(0) : attempts);
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#registerForAssessmentChangeEvents(org.olat.core.util.event.GenericEventListener, org.olat.core.id.Identity)
	 */
	@Override
	public void registerForAssessmentChangeEvents(final GenericEventListener gel, final Identity identity) {
		throw new AssertException("Not implemented for preview.");
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#deregisterFromAssessmentChangeEvents(org.olat.core.util.event.GenericEventListener)
	 */
	@Override
	public void deregisterFromAssessmentChangeEvents(final GenericEventListener gel) {
		throw new AssertException("Not implemented for preview.");
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#preloadCache()
	 */
	@Override
	public void preloadCache() {
		throw new AssertException("Not implemented for preview.");
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#preloadCache(org.olat.core.id.Identity)
	 */
	@Override
	public void preloadCache(final Identity identity) {
		throw new AssertException("Not implemented for preview.");
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#saveAssessmentID(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity, java.lang.String)
	 */
	private void saveAssessmentID(final CourseNode courseNode, final Identity assessedIdentity, final Long assessmentID) {
		nodeAssessmentID.put(courseNode.getIdent(), assessmentID);
	}

	/**
	 * @param courseNode
	 * @param identity
	 * @return
	 */
	@Override
	public Long getAssessmentID(final CourseNode courseNode, final Identity identity) {
		return (Long) nodeAssessmentID.get(courseNode.getIdent());
	}

	/**
	 * @see org.olat.course.assessment.AssessmentManager#saveScoreEvaluation(org.olat.course.nodes.CourseNode, org.olat.core.id.Identity, org.olat.core.id.Identity,
	 *      org.olat.course.run.scoring.ScoreEvaluation)
	 */
	@Override
	public void saveScoreEvaluation(final CourseNode courseNode, final Identity identity, final Identity assessedIdentity, final ScoreEvaluation scoreEvaluation,
			final UserCourseEnvironment userCourseEnvironment, final boolean incrementUserAttempts) {

		saveNodeScore(courseNode, identity, assessedIdentity, scoreEvaluation.getScore());
		saveNodePassed(courseNode, identity, assessedIdentity, scoreEvaluation.getPassed());
		saveAssessmentID(courseNode, assessedIdentity, scoreEvaluation.getAssessmentID());
		if (incrementUserAttempts) {
			incrementNodeAttempts(courseNode, identity, userCourseEnvironment);
		}
	}

	@Override
	public OLATResourceable createOLATResourceableForLocking(final Identity assessedIdentity) {
		throw new AssertException("Not implemented for preview.");
	}

}