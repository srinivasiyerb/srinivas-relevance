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
 * Copyright (c) frentix GmbH, www.frentix.com, Switzerland.
 * <p>
 */
package org.olat.course.condition.interpreter;

import java.util.Locale;

import org.olat.core.id.IdentityEnvironment;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * <h3>Description:</h3> The hasLanguage function checks if the current user has the given language configured
 * <p>
 * Initial Date: 01.02.2008 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public class HasLanguageFunction extends AbstractFunction {

	public static String name = "hasLanguage";

	public HasLanguageFunction(final UserCourseEnvironment userCourseEnv) {
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
			return handleException(new ArgumentParseException(ArgumentParseException.NEEDS_FEWER_ARGUMENTS, name, "", "error.fewerargs", "solution.providetwo.attrvalue"));
		} else if (inStack.length < 1) { return handleException(new ArgumentParseException(ArgumentParseException.NEEDS_MORE_ARGUMENTS, name, "", "error.moreargs",
				"solution.providetwo.attrvalue")); }
		/*
		 * argument type check
		 */
		if (!(inStack[0] instanceof String)) { return handleException(new ArgumentParseException(ArgumentParseException.WRONG_ARGUMENT_FORMAT, name, "",
				"error.argtype.attributename", "solution.example.name.infunction")); }
		final String lang = (String) inStack[0];
		Locale locale;

		final CourseEditorEnv cev = getUserCourseEnv().getCourseEditorEnv();
		if (cev != null) {
			locale = cev.getEditorEnvLocale();
		} else {
			final IdentityEnvironment ienv = getUserCourseEnv().getIdentityEnvironment();
			locale = ienv.getLocale();
		}

		// return true for locale="de_CH" and given lang="de"
		return (locale.toString().toLowerCase().startsWith(lang.toLowerCase()) ? ConditionInterpreter.INT_TRUE : ConditionInterpreter.INT_FALSE);
	}

	@Override
	protected Object defaultValue() {
		return ConditionInterpreter.INT_TRUE;
	}

}