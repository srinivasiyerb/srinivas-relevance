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

package org.olat.course.condition.interpreter.score;

import org.olat.course.assessment.EfficiencyStatement;
import org.olat.course.assessment.EfficiencyStatementManager;
import org.olat.course.condition.interpreter.AbstractFunction;
import org.olat.course.condition.interpreter.ArgumentParseException;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * Description:<br>
 * Get the passed of a node in using the efficiency statement from another course
 * <P>
 * Initial Date: 11.08.2005 <br>
 * 
 * @author gnaegi
 */
public class GetPassedWithCourseIdFunction extends AbstractFunction {
	public static final String name = "getPassedWithCourseId";

	/**
	 * Default constructor to use the current date
	 * 
	 * @param userCourseEnv
	 */
	public GetPassedWithCourseIdFunction(final UserCourseEnvironment userCourseEnv) {
		super(userCourseEnv);
	}

	/**
	 * @see com.neemsoft.jmep.FunctionCB#call(java.lang.Object[])
	 */
	@Override
	public Object call(final Object[] inStack) {
		/*
		 * argument check
		 */
		if (inStack.length > 2) {
			return handleException(new ArgumentParseException(ArgumentParseException.NEEDS_FEWER_ARGUMENTS, name, "", "error.fewerargs",
					"solution.provideone.nodereference"));
		} else if (inStack.length < 2) { return handleException(new ArgumentParseException(ArgumentParseException.NEEDS_MORE_ARGUMENTS, name, "", "error.moreargs",
				"solution.provideone.nodereference")); }
		/*
		 * argument type check
		 */
		Long courseRepoEntryKey;
		try {
			courseRepoEntryKey = Long.decode((String) inStack[0]);
		} catch (final NumberFormatException nfe) {
			return handleException(new ArgumentParseException(ArgumentParseException.WRONG_ARGUMENT_FORMAT, name, "", "error.argtype.coursnodeidexpeted",
					"solution.example.node.infunction"));
		}

		if (!(inStack[1] instanceof String)) { return handleException(new ArgumentParseException(ArgumentParseException.WRONG_ARGUMENT_FORMAT, name, "",
				"error.argtype.coursnodeidexpeted", "solution.example.node.infunction")); }
		final String childId = (String) inStack[1];
		/*
		 * no integrity check can be done - other course might not exist anymore
		 */
		final CourseEditorEnv cev = getUserCourseEnv().getCourseEditorEnv();
		if (cev != null) { return defaultValue(); }

		/*
		 * the real function evaluation which is used during run time
		 */
		final EfficiencyStatementManager esm = EfficiencyStatementManager.getInstance();
		final EfficiencyStatement es = esm.getUserEfficiencyStatement(courseRepoEntryKey, getUserCourseEnv().getIdentityEnvironment().getIdentity());
		if (es == null) { return defaultValue(); }
		final Boolean passed = esm.getPassed(childId, es);
		if (passed == null) { return defaultValue(); }
		// finally check existing value
		return (passed.booleanValue() ? ConditionInterpreter.INT_TRUE : ConditionInterpreter.INT_FALSE);

	}

	/**
	 * @see org.olat.course.condition.interpreter.AbstractFunction#defaultValue()
	 */
	@Override
	protected Object defaultValue() {
		return ConditionInterpreter.INT_FALSE;
	}

}