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

package org.olat.course.condition.interpreter;

import org.olat.core.id.Identity;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * Description:<BR/>
 * Condition function inRightGroup()
 * <P/>
 * Initial Date: Sep 15, 2004
 * 
 * @author gnaegi
 */
public class InRightGroupFunction extends AbstractFunction {

	public static final String name = "inRightGroup";

	/**
	 * @param userCourseEnv
	 */
	public InRightGroupFunction(final UserCourseEnvironment userCourseEnv) {
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
		if (inStack.length > 1) {
			return handleException(new ArgumentParseException(ArgumentParseException.NEEDS_FEWER_ARGUMENTS, name, "", "error.fewerargs", "solution.provideone.groupname"));
		} else if (inStack.length < 1) { return handleException(new ArgumentParseException(ArgumentParseException.NEEDS_MORE_ARGUMENTS, name, "", "error.moreargs",
				"solution.provideone.groupname")); }
		/*
		 * argument type check
		 */
		if (!(inStack[0] instanceof String)) { return handleException(new ArgumentParseException(ArgumentParseException.WRONG_ARGUMENT_FORMAT, name, "",
				"error.argtype.groupnameexpected", "solution.example.name.infunction")); }
		final String groupName = (String) inStack[0];
		/*
		 * check reference integrity
		 */
		final CourseEditorEnv cev = getUserCourseEnv().getCourseEditorEnv();
		if (cev != null) {
			if (!cev.existsRightGroup(groupName)) { return handleException(new ArgumentParseException(ArgumentParseException.REFERENCE_NOT_FOUND, name, groupName,
					"error.notfound.name", "solution.checkgroupmanagement")); }
			// remember the reference to the node id for this condtion
			cev.addSoftReference("groupId", groupName);
			// return a valid value to continue with condition evaluation test
			return defaultValue();
		}

		/*
		 * the real function evaluation which is used during run time
		 */
		final Identity ident = getUserCourseEnv().getIdentityEnvironment().getIdentity();

		final CourseGroupManager cgm = getUserCourseEnv().getCourseEnvironment().getCourseGroupManager();

		return cgm.isIdentityInRightGroup(ident, groupName) ? ConditionInterpreter.INT_TRUE : ConditionInterpreter.INT_FALSE;
	}

	@Override
	protected Object defaultValue() {
		return ConditionInterpreter.INT_TRUE;
	}

}
