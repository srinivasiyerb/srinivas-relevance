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
 * Copyright (c) 1999-2009 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.course.assessment;

import java.lang.reflect.Field;

import org.olat.core.logging.activity.ActionObject;
import org.olat.core.logging.activity.ActionType;
import org.olat.core.logging.activity.ActionVerb;
import org.olat.core.logging.activity.BaseLoggingAction;
import org.olat.core.logging.activity.CrudAction;
import org.olat.core.logging.activity.ILoggingAction;
import org.olat.core.logging.activity.OlatResourceableType;
import org.olat.core.logging.activity.ResourceableTypeList;
import org.olat.core.logging.activity.StringResourceableType;

/**
 * LoggingActions used by Assessment
 * <P>
 * PLEASE BE CAREFUL WHEN EDITING IN HERE.
 * <p>
 * Especially when modifying the ResourceableTypeList - which is a exercise where we try to predict/document/define which ResourceableTypes will later on - at runtime -
 * be available to the IUserActivityLogger.log() method.
 * <p>
 * The names of the LoggingAction should be self-describing.
 * <p>
 * Initial Date: 20.10.2009 <br>
 * 
 * @author Stefan
 */
/* package protected */class AssessmentLoggingAction extends BaseLoggingAction {

	// note that these assessment logging actions can both be
	// triggered by a user (when running a test) or a tutor (when assessing a user).
	// the type is set to statistic here to cover the normal user case
	// and the AssessmentMainController (plus any other admin related controller) sets
	// the stickyactiontype to admin
	public static final ILoggingAction ASSESSMENT_ATTEMPTS_UPDATED = new AssessmentLoggingAction(ActionType.statistic, CrudAction.update, ActionVerb.edit,
			ActionObject.testattempts).setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node,
			StringResourceableType.qtiAttempts, StringResourceableType.targetIdentity));
	public static final ILoggingAction ASSESSMENT_SCORE_UPDATED = new AssessmentLoggingAction(ActionType.statistic, CrudAction.update, ActionVerb.edit,
			ActionObject.testscore).setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node,
			StringResourceableType.qtiScore, StringResourceableType.targetIdentity));
	public static final ILoggingAction ASSESSMENT_PASSED_UPDATED = new AssessmentLoggingAction(ActionType.statistic, CrudAction.update, ActionVerb.edit,
			ActionObject.testsuccess).setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node,
			StringResourceableType.qtiPassed, StringResourceableType.targetIdentity));

	// note that comments can only be set via a tutor, hence the following two are strict admin actions
	public static final ILoggingAction ASSESSMENT_USERCOMMENT_UPDATED = new AssessmentLoggingAction(ActionType.admin, CrudAction.update, ActionVerb.edit,
			ActionObject.testcomment).setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node,
			StringResourceableType.qtiUserComment, StringResourceableType.targetIdentity));
	public static final ILoggingAction ASSESSMENT_COACHCOMMENT_UPDATED = new AssessmentLoggingAction(ActionType.admin, CrudAction.update, ActionVerb.edit,
			ActionObject.testcomment).setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node,
			StringResourceableType.qtiCoachComment, StringResourceableType.targetIdentity));

	/**
	 * This static constructor's only use is to set the javaFieldIdForDebug on all of the LoggingActions defined in this class.
	 * <p>
	 * This is used to simplify debugging - as it allows to issue (technical) log statements where the name of the LoggingAction Field is written.
	 */
	static {
		final Field[] fields = AssessmentLoggingAction.class.getDeclaredFields();
		if (fields != null) {
			for (int i = 0; i < fields.length; i++) {
				final Field field = fields[i];
				if (field.getType() == AssessmentLoggingAction.class) {
					try {
						final AssessmentLoggingAction aLoggingAction = (AssessmentLoggingAction) field.get(null);
						aLoggingAction.setJavaFieldIdForDebug(field.getName());
					} catch (final IllegalArgumentException e) {
						e.printStackTrace();
					} catch (final IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Simple wrapper calling super<init>
	 * 
	 * @see BaseLoggingAction#BaseLoggingAction(ActionType, CrudAction, ActionVerb, String)
	 */
	AssessmentLoggingAction(final ActionType resourceActionType, final CrudAction action, final ActionVerb actionVerb, final ActionObject actionObject) {
		super(resourceActionType, action, actionVerb, actionObject.name());
	}

}
