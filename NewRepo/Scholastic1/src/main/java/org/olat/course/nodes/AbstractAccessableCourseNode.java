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

package org.olat.course.nodes;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.course.ICourse;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionExpression;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * Initial Date: May 28, 2004
 * 
 * @author gnaegi<br>
 *         Comment: Use this abstract course node if you implement a node that has only one accessability condition: access the node. See the CPCourse node for an example
 *         implementation.
 */
public abstract class AbstractAccessableCourseNode extends GenericCourseNode {

	private Condition preConditionAccess;

	/**
	 * Constructor, only used by implementing course nodes
	 * 
	 * @param type The course node type
	 */
	protected AbstractAccessableCourseNode(final String type) {
		super(type);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	abstract public TabbableController createEditController(UserRequest ureq, WindowControl wControl, ICourse course, UserCourseEnvironment euce);

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	abstract public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnv,
			NodeEvaluation ne, String nodecmd);

	/**
	 * Returns the generic access precondition
	 * 
	 * @return Condition
	 */
	@Override
	public Condition getPreConditionAccess() {
		if (preConditionAccess == null) {
			preConditionAccess = new Condition();
		}
		preConditionAccess.setConditionId("accessability");
		return preConditionAccess;
	}

	/**
	 * Sets the generic access precondition.
	 * 
	 * @param precondition_accessor The precondition_accessor to set
	 */
	public void setPreConditionAccess(Condition precondition_accessor) {
		if (precondition_accessor == null) {
			precondition_accessor = getPreConditionAccess();
		}
		precondition_accessor.setConditionId("accessability");
		this.preConditionAccess = precondition_accessor;
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#calcAccessAndVisibility(org.olat.course.condition.interpreter.ConditionInterpreter,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	protected void calcAccessAndVisibility(final ConditionInterpreter ci, final NodeEvaluation nodeEval) {
		// for this node: only one role: accessing the node
		final boolean accessible = (getPreConditionAccess().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionAccess()));
		nodeEval.putAccessStatus("access", accessible);
		final boolean visible = (getPreConditionVisibility().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionVisibility()));
		nodeEval.setVisible(visible);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createInstanceForCopy()
	 */
	@Override
	public CourseNode createInstanceForCopy() {
		final CourseNode copyInstance = super.createInstanceForCopy();
		setPreConditionAccess(null);
		return copyInstance;
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#getConditionExpression()
	 */
	@Override
	public List<ConditionExpression> getConditionExpressions() {
		ArrayList<ConditionExpression> retVal;
		final List<ConditionExpression> parentsConditions = super.getConditionExpressions();
		if (parentsConditions.size() > 0) {
			retVal = new ArrayList<ConditionExpression>(parentsConditions);
		} else {
			retVal = new ArrayList<ConditionExpression>();
		}
		//
		final String coS = getPreConditionAccess().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			final ConditionExpression ce = new ConditionExpression(getPreConditionAccess().getConditionId());
			ce.setExpressionString(getPreConditionAccess().getConditionExpression());
			retVal.add(ce);
		}
		//
		return retVal;
	}

}