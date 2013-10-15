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

package org.olat.core.logging.activity;

import java.lang.reflect.Field;

/**
 * LoggingActions around Course - such as course browsing, but also course editor
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
public class CourseLoggingAction extends BaseLoggingAction {

	public static final ILoggingAction COURSE_BROWSE_GOTO_NODE = new CourseLoggingAction(ActionType.statistic, CrudAction.retrieve, ActionVerb.open,
			ActionObject.gotonode);

	public static final ILoggingAction NODE_SINGLEPAGE_GET_FILE = new CourseLoggingAction(ActionType.statistic, CrudAction.retrieve, ActionVerb.open,
			ActionObject.spgetfile);

	public static final ILoggingAction CP_GET_FILE = new CourseLoggingAction(ActionType.statistic, CrudAction.retrieve, ActionVerb.open, ActionObject.cpgetfile)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node, StringResourceableType.cpNode).or()
					.addMandatory(OlatResourceableType.cp, StringResourceableType.cpNode));

	public static final ILoggingAction ST_GOTO_NODE = new CourseLoggingAction(ActionType.statistic, CrudAction.retrieve, ActionVerb.open, ActionObject.gotonode)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node, StringResourceableType.nodeId)
					.
					// the following has been seen here:
					/*
					 * 2010-02-26 00:03:36,515 [TP-Processor754] WARN UserActivityLoggerImpl - OLAT::WARN ^%^ N2-W40796 ^%^ org.olat.core.logging.activity ^%^ vjetel ^%^
					 * 85.2.180.155 ^%^ https://www.olat.uzh.ch/olat/auth/1%3A1%3A0%3A0%3A0/ ^%^ Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; GTB6.4) ^%^
					 * LoggingAction reported an inconsistency: action=org.olat.core.logging.activity.CourseLoggingAction@79888465, fieldId=null, expected: [Mandatory are
					 * course,node,nodeId. ], actual: [LoggingResourceables: 5, LoggingResourceInfo[type=CourseModule,rtype=genRepoEntry,id=80997996474040,name=Krier:
					 * Einführung in die Literaturwissenschaft FS 2010], LoggingResourceInfo[type=st,rtype=node,id=81053185492848,name=Sitzung II],
					 * LoggingResourceInfo[type=BusinessGroup,rtype=businessGroup,id=1975255068,name=Krier Einführungsseminar Lit.wiss.],
					 * LoggingResourceInfo[type=CourseModule,rtype=course,id=80997996474040,name=Krier: Einführung in die Literaturwissenschaft FS 2010],
					 * LoggingResourceInfo[type=nodeId,rtype=nodeId,id=81053185557462,name=//Lotman_Zeichen.pdf]] ^%^ >>>stack of 1.cause::java.lang.Exception: OLAT-4653
					 * -> at org.olat.core.logging.activity.UserActivityLoggerImpl.log(UserActivityLoggerImpl.java:578) at
					 * org.olat.core.logging.activity.ThreadLocalUserActivityLogger.log(ThreadLocalUserActivityLogger.java:137) at
					 * org.olat.course.nodes.st.STCourseNodeRunController.event(STCourseNodeRunController.java:192) at
					 * org.olat.core.gui.control.DefaultController$1.run(DefaultController.java:247) at
					 * org.olat.core.logging.activity.ThreadLocalUserActivityLoggerInstaller.runWithUserActivityLogger(ThreadLocalUserActivityLoggerInstaller.java:84) at
					 * org.olat.core.gui.control.DefaultController.dispatchEvent(DefaultController.java:244) at
					 * org.olat.core.gui.control.DefaultController.fireEvent(DefaultController.java:182) at
					 * org.olat.course.nodes.st.PeekViewWrapperController.event(PeekViewWrapperController.java:115) at
					 * org.olat.core.gui.control.DefaultController$1.run(DefaultController.java:247) at
					 * org.olat.core.logging.activity.ThreadLocalUserActivityLoggerInstaller.runWithUserActivityLogger(ThreadLocalUserActivityLoggerInstaller.java:84) at
					 * org.olat.core.gui.control.DefaultController.dispatchEvent(DefaultController.java:244)
					 */
					or()
					.addMandatory(OlatResourceableType.genRepoEntry, OlatResourceableType.node, OlatResourceableType.businessGroup, OlatResourceableType.course,
							StringResourceableType.nodeId));

	public static final ILoggingAction FILE_UPLOADED = new CourseLoggingAction(ActionType.statistic, CrudAction.create, ActionVerb.add, ActionObject.file)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node, StringResourceableType.uploadFile));

	public static final ILoggingAction DIALOG_ELEMENT_FILE_UPLOADED = new CourseLoggingAction(ActionType.admin, CrudAction.create, ActionVerb.add, ActionObject.file)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node, StringResourceableType.uploadFile));

	public static final ILoggingAction DIALOG_ELEMENT_FILE_DELETED = new CourseLoggingAction(ActionType.admin, CrudAction.update, ActionVerb.remove, ActionObject.node)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node, StringResourceableType.uploadFile));

	public static final ILoggingAction DIALOG_ELEMENT_FILE_DOWNLOADED = new CourseLoggingAction(ActionType.admin, CrudAction.retrieve, ActionVerb.view, ActionObject.file)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node, StringResourceableType.uploadFile));

	public static final ILoggingAction CHECKLIST_ELEMENT_CHECKPOINT_UPDATED = new CourseLoggingAction(ActionType.statistic, CrudAction.update, ActionVerb.edit,
			ActionObject.checkpoint).setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node,
			StringResourceableType.checklist, StringResourceableType.checkpoint));

	public static final ILoggingAction COURSE_ENTERING = new CourseLoggingAction(ActionType.statistic, CrudAction.retrieve, ActionVerb.launch, ActionObject.course)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course));

	public static final ILoggingAction COURSE_LEAVING = new CourseLoggingAction(ActionType.statistic, CrudAction.exit, ActionVerb.exit, ActionObject.course)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course).or()
					.addMandatory(OlatResourceableType.course, OlatResourceableType.genRepoEntry).addOptional(OlatResourceableType.businessGroup));

	public static final ILoggingAction COURSE_NAVIGATION_NODE_ACCESS = new CourseLoggingAction(ActionType.statistic, CrudAction.retrieve, ActionVerb.launch,
			ActionObject.node).setTypeList(
	// the first one represents the normal course navigation
			new ResourceableTypeList()
					.addMandatory(OlatResourceableType.course, OlatResourceableType.node)
					.addOptional(OlatResourceableType.businessGroup)
					.
					// the second one is navigating in a CP
					or()
					.addMandatory(OlatResourceableType.course, OlatResourceableType.node, StringResourceableType.cpNode)
					.addOptional(OlatResourceableType.businessGroup)
					.
					// the third one is the wiki in a course case
					or().addMandatory(OlatResourceableType.course, OlatResourceableType.node, OlatResourceableType.wiki)
					.addOptional(OlatResourceableType.businessGroup)
					.
					// the fourth one is navigating in a forum
					or().addMandatory(OlatResourceableType.course, OlatResourceableType.node, OlatResourceableType.forum)
					.addOptional(OlatResourceableType.businessGroup)
					.
					// OLAT-4653 & LogAnalyzer Pattern 386: LoggingAction reported an inconsistency
					or().addMandatory(OlatResourceableType.course, OlatResourceableType.node, OlatResourceableType.genRepoEntry)
					.addOptional(OlatResourceableType.businessGroup));
	public static final ILoggingAction COURSE_NAVIGATION_NODE_NO_ACCESS = new CourseLoggingAction(ActionType.statistic, CrudAction.retrieve, ActionVerb.denied,
			ActionObject.node).setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node));

	public static final ILoggingAction COURSE_EDITOR_OPEN = new CourseLoggingAction(ActionType.admin, CrudAction.retrieve, ActionVerb.launch, ActionObject.editor)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course));

	public static final ILoggingAction COURSE_EDITOR_CLOSE = new CourseLoggingAction(ActionType.admin, CrudAction.retrieve, ActionVerb.exit, ActionObject.editor)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course));

	public static final ILoggingAction COURSE_EDITOR_NODE_MOVED = new CourseLoggingAction(ActionType.admin, CrudAction.update, ActionVerb.move, ActionObject.node)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node));
	public static final ILoggingAction COURSE_EDITOR_NODE_COPIED = new CourseLoggingAction(ActionType.admin, CrudAction.update, ActionVerb.copy, ActionObject.node)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node));
	public static final ILoggingAction COURSE_EDITOR_NODE_DELETED = new CourseLoggingAction(ActionType.admin, CrudAction.delete, ActionVerb.remove, ActionObject.node)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node));
	public static final ILoggingAction COURSE_EDITOR_NODE_EDITED = new CourseLoggingAction(ActionType.admin, CrudAction.update, ActionVerb.edit, ActionObject.node)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node));
	public static final ILoggingAction COURSE_EDITOR_NODE_CREATED = new CourseLoggingAction(ActionType.admin, CrudAction.create, ActionVerb.add, ActionObject.node)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node));
	public static final ILoggingAction COURSE_EDITOR_NODE_RESTORED = new CourseLoggingAction(ActionType.admin, CrudAction.update, ActionVerb.add, ActionObject.node)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node));

	public static final ILoggingAction COURSE_EDITOR_PUBLISHED = new CourseLoggingAction(ActionType.admin, CrudAction.update, ActionVerb.edit, ActionObject.publisher)
			.setTypeList(new ResourceableTypeList().addMandatory(OlatResourceableType.course, OlatResourceableType.node));

	/**
	 * This static constructor's only use is to set the javaFieldIdForDebug on all of the LoggingActions defined in this class.
	 * <p>
	 * This is used to simplify debugging - as it allows to issue (technical) log statements where the name of the LoggingAction Field is written.
	 */
	static {
		Field[] fields = CourseLoggingAction.class.getDeclaredFields();
		if (fields != null) {
			for (int i = 0; i < fields.length; i++) {
				Field field = fields[i];
				if (field.getType() == CourseLoggingAction.class) {
					try {
						CourseLoggingAction aLoggingAction = (CourseLoggingAction) field.get(null);
						aLoggingAction.setJavaFieldIdForDebug(field.getName());
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
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
	CourseLoggingAction(ActionType resourceActionType, CrudAction action, ActionVerb actionVerb, ActionObject actionObject) {
		super(resourceActionType, action, actionVerb, actionObject.name());
	}

}
