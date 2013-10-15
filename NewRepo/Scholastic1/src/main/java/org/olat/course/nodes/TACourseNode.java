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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.ExportUtil;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Util;
import org.olat.core.util.ZipUtil;
import org.olat.course.ICourse;
import org.olat.course.archiver.ScoreAccountingHelper;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionExpression;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.ms.MSEditFormController;
import org.olat.course.nodes.ta.DropboxController;
import org.olat.course.nodes.ta.DropboxScoringViewController;
import org.olat.course.nodes.ta.ReturnboxController;
import org.olat.course.nodes.ta.TACourseNodeEditController;
import org.olat.course.nodes.ta.TACourseNodeRunController;
import org.olat.course.nodes.ta.TaskController;
import org.olat.course.nodes.ta.TaskFormController;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.properties.PersistingCoursePropertyManager;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.properties.Property;
import org.olat.repository.RepositoryEntry;

/**
 * Initial Date: 30.08.2004
 * 
 * @author Mike Stock
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */

public class TACourseNode extends GenericCourseNode implements AssessableCourseNode {
	private static final String PACKAGE_TA = Util.getPackageName(TACourseNodeRunController.class);

	private static final String PACKAGE = Util.getPackageName(TACourseNode.class);

	private static final String TYPE = "ta";

	// NLS support:

	private static final String NLS_GUESTNOACCESS_TITLE = "guestnoaccess.title";
	private static final String NLS_GUESTNOACCESS_MESSAGE = "guestnoaccess.message";
	private static final String NLS_ERROR_MISSINGSCORECONFIG_SHORT = "error.missingscoreconfig.short";
	private static final String NLS_ERROR_MISSING_GROUP_SHORT = "error.missing.group.short";
	private static final String NLS_ERROR_MISSING_GROUP_LONG = "error.missing.group.long";
	private static final String NLS_WARN_NODEDELETE = "warn.nodedelete";
	private static final String NLS_DROPBOX_ERROR_MISSING_GROUP_SHORT = "error.dropbox.missing.group.short";
	private static final String NLS_DROPBOX_ERROR_MISSING_GROUP_LONG = "error.dropbox.missing.group.long";
	private static final String NLS_RETURNBOX_ERROR_MISSING_GROUP_SHORT = "error.returnbox.missing.group.short";
	private static final String NLS_RETURNBOX_ERROR_MISSING_GROUP_LONG = "error.returnbox.missing.group.long";
	private static final String NLS_SOLUTION_ERROR_MISSING_GROUP_SHORT = "error.solution.missing.group.short";
	private static final String NLS_SOLUTION_ERROR_MISSING_GROUP_LONG = "error.solution.missing.group.long";
	private static final String NLS_SCORING_ERROR_MISSING_GROUP_SHORT = "error.scoring.missing.group.short";
	private static final String NLS_SCORING_ERROR_MISSING_GROUP_LONG = "error.scoring.missing.group.long";

	private static final int CURRENT_CONFIG_VERSION = 2;

	/** CONF_TASK_ENABLED configuration parameter key. */
	public static final String CONF_TASK_ENABLED = "task_enabled";
	/** CONF_TASK_TYPE configuration parameter key. */
	public static final String CONF_TASK_TYPE = "task_type";

	/** CONF_TASK_TEXT configuration parameter key. */
	public static final String CONF_TASK_TEXT = "task_text";
	/** CONF_TASK_SAMPLING_WITH_REPLACEMENT configuration parameter key. */
	public static final String CONF_TASK_SAMPLING_WITH_REPLACEMENT = "task_sampling";
	/** CONF_TASK_FOLDER_REL_PATH configuration parameter key. */
	public static final String CONF_TASK_FOLDER_REL_PATH = "task_folder_rel";

	/** CONF_DROPBOX_ENABLED configuration parameter key. */
	public static final String CONF_DROPBOX_ENABLED = "dropbox_enabled";
	/** CONF_DROPBOX_ENABLEMAIL configuration parameter key. */
	public static final String CONF_DROPBOX_ENABLEMAIL = "dropbox_enablemail";
	/** CONF_DROPBOX_CONFIRMATION configuration parameter key. */
	public static final String CONF_DROPBOX_CONFIRMATION = "dropbox_confirmation";

	/** CONF_RETURNBOX_ENABLED configuration parameter key. */
	public static final String CONF_RETURNBOX_ENABLED = "returnbox_enabled";

	/** CONF_SCORING_ENABLED configuration parameter key. */
	public static final String CONF_SCORING_ENABLED = "scoring_enabled";

	/** ACCESS_SCORING configuration parameter key. */
	public static final String ACCESS_SCORING = "scoring";
	/** ACCESS_DROPBOX configuration parameter key. */
	public static final String ACCESS_DROPBOX = "dropbox";
	/** ACCESS_RETURNBOX configuration parameter key. */
	public static final String ACCESS_RETURNBOX = "returnbox";
	/** ACCESS_TASK configuration parameter key. */
	public static final String ACCESS_TASK = "task";
	/** ACCESS_SOLUTION configuration parameter key. */
	public static final String ACCESS_SOLUTION = "solution";

	/** CONF_SOLUTION_ENABLED configuration parameter key. */
	public static final String CONF_SOLUTION_ENABLED = "solution_enabled";

	/** Solution folder-name in the file-system. */
	public static final String SOLUTION_FOLDER_NAME = "solutions";

	/** CONF_TASK_PREVIEW configuration parameter key used for task-form. */
	public static final String CONF_TASK_PREVIEW = "task_preview";

	/** CONF_TASK_DESELECT configuration parameter key used for task-form. */
	public static final String CONF_TASK_DESELECT = "task_deselect";

	private Condition conditionTask, conditionDrop, conditionReturnbox, conditionScoring, conditionSolution;

	private transient CourseGroupManager groupMgr;

	private static final OLog log = Tracing.createLoggerFor(TACourseNode.class);

	/**
	 * Default constructor.
	 */
	public TACourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		final TACourseNodeEditController childTabCntrllr = new TACourseNodeEditController(ureq, wControl, course, this, course.getCourseEnvironment()
				.getCourseGroupManager(), euce);
		final CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		groupMgr = course.getCourseEnvironment().getCourseGroupManager();
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, groupMgr, euce, childTabCntrllr);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv,
			final NodeEvaluation ne, final String nodecmd) {
		updateModuleConfigDefaults(false);
		Controller controller;
		// Do not allow guests to access tasks
		final Roles roles = ureq.getUserSession().getRoles();
		if (roles.isGuestOnly()) {
			final Translator trans = new PackageTranslator(PACKAGE, ureq.getLocale());
			final String title = trans.translate(NLS_GUESTNOACCESS_TITLE);
			final String message = trans.translate(NLS_GUESTNOACCESS_MESSAGE);
			controller = MessageUIFactory.createInfoMessage(ureq, wControl, title, message);
		} else {
			controller = new TACourseNodeRunController(ureq, wControl, userCourseEnv, ne, false);
		}
		final Controller ctrl = TitledWrapperHelper.getWrapper(ureq, wControl, controller, this, "o_ta_icon");
		return new NodeRunConstructionResult(ctrl);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPreviewController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPreviewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		return new TACourseNodeRunController(ureq, wControl, userCourseEnv, ne, true);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	@Override
	public boolean needsReferenceToARepositoryEntry() {
		return false;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	public StatusDescription isConfigValid() {
		/*
		 * first check the one click cache
		 */
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		boolean isValid = true;
		final Boolean hasScoring = (Boolean) getModuleConfiguration().get(CONF_SCORING_ENABLED);
		if (hasScoring.booleanValue()) {
			if (!MSEditFormController.isConfigValid(getModuleConfiguration())) {
				isValid = false;
			}
		}
		StatusDescription sd = StatusDescription.NOERROR;
		if (!isValid) {
			// FIXME: refine statusdescriptions by moving the statusdescription
			final String shortKey = NLS_ERROR_MISSINGSCORECONFIG_SHORT;
			final String longKey = NLS_ERROR_MISSINGSCORECONFIG_SHORT;
			final String[] params = new String[] { this.getShortTitle() };
			final String translPackage = Util.getPackageName(MSEditFormController.class);
			sd = new StatusDescription(StatusDescription.ERROR, shortKey, longKey, params, translPackage);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(TACourseNodeEditController.PANE_TAB_CONF_SCORING);
		}
		// Check if any group exist make sense only with dropbox, scoring or solution
		Boolean hasDropbox = (Boolean) getModuleConfiguration().get(CONF_DROPBOX_ENABLED);
		if (hasDropbox == null) {
			hasDropbox = new Boolean(false);
		}
		Boolean hasReturnbox = (Boolean) getModuleConfiguration().get(CONF_RETURNBOX_ENABLED);
		if (hasReturnbox == null) {
			hasReturnbox = hasDropbox;
		}
		Boolean hasSolution = (Boolean) getModuleConfiguration().get(CONF_SOLUTION_ENABLED);
		if (hasSolution == null) {
			hasSolution = new Boolean(false);
		}

		if (hasScoring.booleanValue() || hasDropbox.booleanValue() || hasSolution.booleanValue() || hasReturnbox.booleanValue()) {
			// check if any group exit for this course
			if ((groupMgr != null) && (groupMgr.getAllLearningGroupsFromAllContexts().size() == 0)) {
				final String[] params = new String[] { this.getShortTitle() };
				final String translPackage = Util.getPackageName(TaskFormController.class);
				sd = new StatusDescription(StatusDescription.WARNING, NLS_ERROR_MISSING_GROUP_SHORT, NLS_ERROR_MISSING_GROUP_LONG, params, translPackage);
				sd.setDescriptionForUnit(getIdent());
				// set which pane is affected by error
				sd.setActivateableViewIdentifier(TACourseNodeEditController.PANE_TAB_ACCESSIBILITY);
			} else if (hasDropbox.booleanValue() && (conditionDrop.getEasyModeGroupAccess() == null || conditionDrop.getEasyModeGroupAccess().equals(""))
					&& (conditionDrop.getEasyModeGroupAreaAccess() == null || conditionDrop.getEasyModeGroupAreaAccess().equals(""))) {
				final String[] params = new String[] { this.getShortTitle() };
				final String translPackage = Util.getPackageName(TaskFormController.class);
				sd = new StatusDescription(StatusDescription.WARNING, NLS_DROPBOX_ERROR_MISSING_GROUP_SHORT, NLS_DROPBOX_ERROR_MISSING_GROUP_LONG, params, translPackage);
				sd.setDescriptionForUnit(getIdent());
				// set which pane is affected by error
				sd.setActivateableViewIdentifier(TACourseNodeEditController.PANE_TAB_ACCESSIBILITY);
			} else if (hasReturnbox.booleanValue() && (conditionReturnbox.getEasyModeGroupAccess() == null || conditionReturnbox.getEasyModeGroupAccess().equals(""))
					&& (conditionReturnbox.getEasyModeGroupAreaAccess() == null || conditionReturnbox.getEasyModeGroupAreaAccess().equals(""))) {
				// show NLS_RETURNBOX_ERROR_MISSING_GROUP error only if the dropCondition is also null, else use same group as for the dropbox
				if (conditionDrop.getEasyModeGroupAccess() == null /* || conditionDrop.getEasyModeGroupAccess().equals("") */) {
					final String[] params = new String[] { this.getShortTitle() };
					final String translPackage = Util.getPackageName(TaskFormController.class);
					sd = new StatusDescription(StatusDescription.WARNING, NLS_RETURNBOX_ERROR_MISSING_GROUP_SHORT, NLS_RETURNBOX_ERROR_MISSING_GROUP_LONG, params,
							translPackage);
					sd.setDescriptionForUnit(getIdent());
					// set which pane is affected by error
					sd.setActivateableViewIdentifier(TACourseNodeEditController.PANE_TAB_ACCESSIBILITY);
				}
			} else if (hasScoring.booleanValue() && (conditionScoring.getEasyModeGroupAccess() == null || conditionScoring.getEasyModeGroupAccess().equals(""))
					&& (conditionScoring.getEasyModeGroupAreaAccess() == null || conditionScoring.getEasyModeGroupAreaAccess().equals(""))) {
				final String[] params = new String[] { this.getShortTitle() };
				final String translPackage = Util.getPackageName(TaskFormController.class);
				sd = new StatusDescription(StatusDescription.WARNING, NLS_SCORING_ERROR_MISSING_GROUP_SHORT, NLS_SCORING_ERROR_MISSING_GROUP_LONG, params, translPackage);
				sd.setDescriptionForUnit(getIdent());
				// set which pane is affected by error
				sd.setActivateableViewIdentifier(TACourseNodeEditController.PANE_TAB_ACCESSIBILITY);
			} else if (hasSolution.booleanValue() && (conditionSolution.getEasyModeGroupAccess() == null || conditionSolution.getEasyModeGroupAccess().equals(""))
					&& (conditionSolution.getEasyModeGroupAreaAccess() == null || conditionSolution.getEasyModeGroupAreaAccess().equals(""))) {
				final String[] params = new String[] { this.getShortTitle() };
				final String translPackage = Util.getPackageName(TaskFormController.class);
				sd = new StatusDescription(StatusDescription.WARNING, NLS_SOLUTION_ERROR_MISSING_GROUP_SHORT, NLS_SOLUTION_ERROR_MISSING_GROUP_LONG, params,
						translPackage);
				sd.setDescriptionForUnit(getIdent());
				// set which pane is affected by error
				sd.setActivateableViewIdentifier(TACourseNodeEditController.PANE_TAB_ACCESSIBILITY);
			}
		}
		return sd;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public StatusDescription[] isConfigValid(final CourseEditorEnv cev) {
		oneClickStatusCache = null;
		// only here we know which translator to take for translating condition
		// error messages
		final String translatorStr = Util.getPackageName(TACourseNodeEditController.class);
		// check if group-manager is already initialized
		if (groupMgr == null) {
			groupMgr = cev.getCourseGroupManager();
		}
		final List sds = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
	}

	@Override
	protected void calcAccessAndVisibility(final ConditionInterpreter ci, final NodeEvaluation nodeEval) {
		if (ci == null) { throw new OLATRuntimeException("no condition interpreter <" + getIdent() + " " + getShortName() + ">", new IllegalArgumentException()); }
		if (nodeEval == null) { throw new OLATRuntimeException("node Evaluationt is null!! for <" + getIdent() + " " + getShortName() + ">",
				new IllegalArgumentException()); }
		// evaluate the preconditions
		final boolean task = (getConditionTask().getConditionExpression() == null ? true : ci.evaluateCondition(conditionTask));
		nodeEval.putAccessStatus(ACCESS_TASK, task);
		final boolean dropbox = (getConditionDrop().getConditionExpression() == null ? true : ci.evaluateCondition(conditionDrop));
		nodeEval.putAccessStatus(ACCESS_DROPBOX, dropbox);
		final boolean returnbox = (getConditionReturnbox().getConditionExpression() == null ? true : ci.evaluateCondition(conditionReturnbox));
		nodeEval.putAccessStatus(ACCESS_RETURNBOX, returnbox);
		final boolean scoring = (getConditionScoring().getConditionExpression() == null ? true : ci.evaluateCondition(conditionScoring));
		nodeEval.putAccessStatus(ACCESS_SCORING, scoring);
		final boolean solution = (getConditionSolution().getConditionExpression() == null ? true : ci.evaluateCondition(conditionSolution));
		nodeEval.putAccessStatus(ACCESS_SOLUTION, solution);

		final boolean visible = (getPreConditionVisibility().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionVisibility()));
		nodeEval.setVisible(visible);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#informOnDelete(org.olat.core.gui.UserRequest, org.olat.course.ICourse)
	 */
	@Override
	public String informOnDelete(final Locale locale, final ICourse course) {
		final Translator trans = new PackageTranslator(PACKAGE_TA, locale);
		final CoursePropertyManager cpm = PersistingCoursePropertyManager.getInstance(course);
		final List list = cpm.listCourseNodeProperties(this, null, null, null);
		if (list.size() != 0) { return trans.translate("warn.nodedelete"); // properties exist
		}
		final File fTaskFolder = new File(FolderConfig.getCanonicalRoot() + TACourseNode.getTaskFolderPathRelToFolderRoot(course, this));
		if (fTaskFolder.exists() && fTaskFolder.list().length > 0) { return trans.translate(NLS_WARN_NODEDELETE); // task folder contains files
		}
		return null; // no data yet.
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#cleanupOnDelete(org.olat.course.ICourse)
	 */
	@Override
	public void cleanupOnDelete(final ICourse course) {
		final CoursePropertyManager pm = course.getCourseEnvironment().getCoursePropertyManager();
		// Delete all properties...
		pm.deleteNodeProperties(this, null);
		final File fTaskFolder = new File(FolderConfig.getCanonicalRoot() + TACourseNode.getTaskFolderPathRelToFolderRoot(course, this));
		if (fTaskFolder.exists()) {
			FileUtils.deleteDirsAndFiles(fTaskFolder, true, true);
		}
		final File fDropBox = new File(FolderConfig.getCanonicalRoot() + DropboxController.getDropboxPathRelToFolderRoot(course.getCourseEnvironment(), this));
		if (fDropBox.exists()) {
			FileUtils.deleteDirsAndFiles(fDropBox, true, true);
		}
	}

	/**
	 * @return dropbox condition
	 */
	public Condition getConditionDrop() {
		if (conditionDrop == null) {
			conditionDrop = new Condition();
		}
		conditionDrop.setConditionId("drop");
		return conditionDrop;
	}

	/**
	 * @return Returnbox condition
	 */
	public Condition getConditionReturnbox() {
		if (conditionReturnbox == null) {
			conditionReturnbox = new Condition();
		}
		conditionReturnbox.setConditionId(ACCESS_RETURNBOX);
		return conditionReturnbox;
	}

	/**
	 * @return scoring condition
	 */
	public Condition getConditionScoring() {
		if (conditionScoring == null) {
			conditionScoring = new Condition();
		}
		conditionScoring.setConditionId("scoring");
		return conditionScoring;
	}

	/**
	 * @return task condition
	 */
	public Condition getConditionTask() {
		if (conditionTask == null) {
			conditionTask = new Condition();
		}
		conditionTask.setConditionId("task");
		return conditionTask;
	}

	/**
	 * @return scoring condition
	 */
	public Condition getConditionSolution() {
		if (conditionSolution == null) {
			conditionSolution = new Condition();
		}
		conditionSolution.setConditionId("solution");
		return conditionSolution;
	}

	/**
	 * @param conditionDrop
	 */
	public void setConditionDrop(Condition conditionDrop) {
		if (conditionDrop == null) {
			conditionDrop = getConditionDrop();
		}
		conditionDrop.setConditionId("drop");
		this.conditionDrop = conditionDrop;
	}

	/**
	 * @param condition
	 */
	public void setConditionReturnbox(Condition condition) {
		if (condition == null) {
			condition = getConditionReturnbox();
		}
		condition.setConditionId(ACCESS_RETURNBOX);
		this.conditionReturnbox = condition;
	}

	/**
	 * @param conditionScoring
	 */
	public void setConditionScoring(Condition conditionScoring) {
		if (conditionScoring == null) {
			conditionScoring = getConditionScoring();
		}
		conditionScoring.setConditionId("scoring");
		this.conditionScoring = conditionScoring;
	}

	/**
	 * @param conditionTask
	 */
	public void setConditionTask(Condition conditionTask) {
		if (conditionTask == null) {
			conditionTask = getConditionTask();
		}
		conditionTask.setConditionId("task");
		this.conditionTask = conditionTask;
	}

	/**
	 * @param conditionScoring
	 */
	public void setConditionSolution(Condition conditionSolution) {
		if (conditionSolution == null) {
			conditionSolution = getConditionSolution();
		}
		conditionSolution.setConditionId("solution");
		this.conditionSolution = conditionSolution;
	}

	// //////////// assessable interface implementation

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserScoreEvaluation(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public ScoreEvaluation getUserScoreEvaluation(final UserCourseEnvironment userCourseEnvironment) {
		// read score from properties
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		Boolean passed = null;
		Float score = null;
		// only db lookup if configured, else return null
		if (hasPassedConfigured()) {
			passed = am.getNodePassed(this, mySelf);
		}
		if (hasScoreConfigured()) {
			score = am.getNodeScore(this, mySelf);
		}

		final ScoreEvaluation se = new ScoreEvaluation(score, passed);
		return se;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasCommentConfigured()
	 */
	public boolean hasCommentConfigured() {
		final ModuleConfiguration config = getModuleConfiguration();
		final Boolean comment = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_COMMENT_FIELD);
		if (comment == null) { return false; }
		return comment.booleanValue();
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasPassedConfigured()
	 */
	public boolean hasPassedConfigured() {
		final ModuleConfiguration config = getModuleConfiguration();
		final Boolean passed = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD);
		if (passed == null) { return false; }
		return passed.booleanValue();
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasScoreConfigured()
	 */
	public boolean hasScoreConfigured() {
		final ModuleConfiguration config = getModuleConfiguration();
		final Boolean score = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD);
		if (score == null) { return false; }
		return score.booleanValue();
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasStatusConfigured()
	 */
	public boolean hasStatusConfigured() {
		return true; // Task Course node has always a status-field
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getMaxScoreConfiguration()
	 */
	public Float getMaxScoreConfiguration() {
		if (!hasScoreConfigured()) { throw new OLATRuntimeException(TACourseNode.class, "getMaxScore not defined when hasScore set to false", null); }
		final ModuleConfiguration config = getModuleConfiguration();
		final Float max = (Float) config.get(MSCourseNode.CONFIG_KEY_SCORE_MAX);
		return max;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getMinScoreConfiguration()
	 */
	public Float getMinScoreConfiguration() {
		if (!hasScoreConfigured()) { throw new OLATRuntimeException(TACourseNode.class, "getMinScore not defined when hasScore set to false", null); }
		final ModuleConfiguration config = getModuleConfiguration();
		final Float min = (Float) config.get(MSCourseNode.CONFIG_KEY_SCORE_MIN);
		return min;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getCutValueConfiguration()
	 */
	public Float getCutValueConfiguration() {
		if (!hasPassedConfigured()) { throw new OLATRuntimeException(TACourseNode.class, "getCutValue not defined when hasPassed set to false", null); }
		final ModuleConfiguration config = getModuleConfiguration();
		final Float cut = (Float) config.get(MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE);
		return cut;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserCoachComment(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public String getUserCoachComment(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final String coachCommentValue = am.getNodeCoachComment(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return coachCommentValue;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserUserComment(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public String getUserUserComment(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final String userCommentValue = am.getNodeComment(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return userCommentValue;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserLog(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public String getUserLog(final UserCourseEnvironment userCourseEnvironment) {
		final UserNodeAuditManager am = userCourseEnvironment.getCourseEnvironment().getAuditManager();
		final String logValue = am.getUserNodeLog(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return logValue;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#isEditableConfigured()
	 */
	public boolean isEditableConfigured() {
		// always true
		return true;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserCoachComment(java.lang.String, org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public void updateUserCoachComment(final String coachComment, final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		if (coachComment != null) {
			am.saveNodeCoachComment(this, mySelf, coachComment);
		}
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserScoreEvaluation(org.olat.course.run.scoring.ScoreEvaluation,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.core.id.Identity)
	 */
	public void updateUserScoreEvaluation(final ScoreEvaluation scoreEvaluation, final UserCourseEnvironment userCourseEnvironment, final Identity coachingIdentity,
			final boolean incrementAttempts) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		am.saveScoreEvaluation(this, coachingIdentity, mySelf, new ScoreEvaluation(scoreEvaluation.getScore(), scoreEvaluation.getPassed()), userCourseEnvironment,
				incrementAttempts);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserUserComment(java.lang.String, org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.core.id.Identity)
	 */
	public void updateUserUserComment(final String userComment, final UserCourseEnvironment userCourseEnvironment, final Identity coachingIdentity) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		if (userComment != null) {
			am.saveNodeComment(this, coachingIdentity, mySelf, userComment);
		}
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserAttempts(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public Integer getUserAttempts(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		final Integer userAttemptsValue = am.getNodeAttempts(this, mySelf);
		return userAttemptsValue;

	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasAttemptsConfigured()
	 */
	public boolean hasAttemptsConfigured() {
		return true;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserAttempts(java.lang.Integer, org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.core.id.Identity)
	 */
	public void updateUserAttempts(final Integer userAttempts, final UserCourseEnvironment userCourseEnvironment, final Identity coachingIdentity) {
		if (userAttempts != null) {
			final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
			final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
			am.saveNodeAttempts(this, coachingIdentity, mySelf, userAttempts);
		}
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#incrementUserAttempts(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public void incrementUserAttempts(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		am.incrementNodeAttempts(this, mySelf, userCourseEnvironment);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public Controller getDetailsEditController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnvironment) {
		// prepare file component
		return new DropboxScoringViewController(ureq, wControl, this, userCourseEnvironment);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsListView(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public String getDetailsListView(final UserCourseEnvironment userCourseEnvironment) {
		final Identity identity = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		final CoursePropertyManager propMgr = userCourseEnvironment.getCourseEnvironment().getCoursePropertyManager();
		final List samples = propMgr.findCourseNodeProperties(this, identity, null, TaskController.PROP_ASSIGNED);
		if (samples.size() == 0) { return null; // no sample assigned yet
		}
		return ((Property) samples.get(0)).getStringValue();
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsListViewHeaderKey()
	 */
	public String getDetailsListViewHeaderKey() {
		return "table.header.details.ta";
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasDetails()
	 */
	public boolean hasDetails() {
		final ModuleConfiguration modConfig = getModuleConfiguration();
		Boolean hasTask = (Boolean) modConfig.get(TACourseNode.CONF_TASK_ENABLED);
		if (hasTask == null) {
			hasTask = Boolean.FALSE;
		}
		Boolean hasDropbox = (Boolean) modConfig.get(TACourseNode.CONF_DROPBOX_ENABLED);
		if (hasDropbox == null) {
			hasDropbox = Boolean.FALSE;
		}
		Boolean hasReturnbox = (Boolean) modConfig.get(TACourseNode.CONF_RETURNBOX_ENABLED);
		if (hasReturnbox == null) {
			hasReturnbox = hasDropbox;
		}

		return (hasTask.booleanValue() || hasDropbox.booleanValue() || hasReturnbox.booleanValue());
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#exportNode(java.io.File, org.olat.course.ICourse)
	 */
	@Override
	public void exportNode(final File fExportDirectory, final ICourse course) {
		// export only this taskfolder's tasks
		final File fTaskFolder = new File(FolderConfig.getCanonicalRoot() + TACourseNode.getTaskFolderPathRelToFolderRoot(course, this));
		final File fNodeExportDir = new File(fExportDirectory, this.getIdent());
		fNodeExportDir.mkdirs();
		FileUtils.copyDirContentsToDir(fTaskFolder, fNodeExportDir, false, "export task course node");
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#importNode(java.io.File, org.olat.course.ICourse, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller importNode(final File importDirectory, final ICourse course, final boolean unattendedImport, final UserRequest ureq, final WindowControl wControl) {
		final File fNodeImportDir = new File(importDirectory, this.getIdent());
		final File fTaskfolderDir = new File(FolderConfig.getCanonicalRoot() + TACourseNode.getTaskFolderPathRelToFolderRoot(course, this));
		FileUtils.copyDirContentsToDir(fNodeImportDir, fTaskfolderDir, false, "import task course node");
		return null;
	}

	/**
	 * archives the dropbox of this task course node to the user's personal folder under private/archive/[coursename]/dropboxes/[nodeIdent].zip
	 * 
	 * @param locale
	 * @param course
	 * @param fArchiveDirectory
	 * @param charset
	 */
	@Override
	public boolean archiveNodeData(final Locale locale, final ICourse course, final File fArchiveDirectory, final String charset) {
		boolean dataFound = false;
		final String dropboxPath = FolderConfig.getCanonicalRoot() + DropboxController.getDropboxPathRelToFolderRoot(course.getCourseEnvironment(), this);
		final File dropboxDir = new File(dropboxPath);
		final String solutionsPath = FolderConfig.getCanonicalRoot() + TACourseNode.getFoldernodesPathRelToFolderBase(course.getCourseEnvironment()) + "/"
				+ this.getIdent();
		final File solutionDir = new File(solutionsPath);
		final String returnboxPath = FolderConfig.getCanonicalRoot() + ReturnboxController.getReturnboxPathRelToFolderRoot(course.getCourseEnvironment(), this);
		final File returnboxDir = new File(returnboxPath);
		final Boolean hasTask = (Boolean) getModuleConfiguration().get(TACourseNode.CONF_TASK_ENABLED);

		if (dropboxDir.exists() || solutionDir.exists() || returnboxDir.exists() || hasTask.booleanValue()) {
			// Create Temp Dir for zipping
			String tmpDirPath = FolderConfig.getCanonicalTmpDir() + course.getCourseEnvironment().getCourseBaseContainer().getRelPath();
			File tmpDir = new File(tmpDirPath);

			if (!tmpDir.exists()) {
				tmpDir.mkdirs();
			}
			// we need a unique dir name
			final File newDir = FileUtils.createTempDir("tmp", "", tmpDir);
			if (newDir != null) {
				tmpDir = newDir;
				try {
					tmpDirPath = tmpDir.getCanonicalPath();
				} catch (final IOException e) {
					log.warn("tmpDir.getCanonicalPath() throws IOException!");
				}
			}

			// prepare writing course results overview table
			final List users = ScoreAccountingHelper.loadUsers(course.getCourseEnvironment());
			final List nodes = new ArrayList();
			nodes.add(this);
			final String s = ScoreAccountingHelper.createCourseResultsOverviewTable(users, nodes, course, locale);

			final String courseTitle = course.getCourseTitle();
			final String fileName = ExportUtil.createFileNameWithTimeStamp(courseTitle, "xls");

			// write course results overview table to filesystem
			ExportUtil.writeContentToFile(fileName, s, tmpDir, charset);

			// prepare zipping the node directory and the course results overview table
			final Set fileList = new HashSet();
			// move xls file to tmp dir
			fileList.add(fileName);
			// copy solutions to tmp dir
			if (solutionDir.exists()) {
				FileUtils.copyDirContentsToDir(solutionDir, new File(tmpDirPath + "/solutions"), false, "archive task course node solutions");
				fileList.add("solutions");
			}
			// copy dropboxes to tmp dir
			if (dropboxDir.exists()) {
				FileUtils.copyDirContentsToDir(dropboxDir, new File(tmpDirPath + "/dropboxes"), false, "archive task course node dropboxes");
				fileList.add("dropboxes");
				// dropboxes exists, so there is something to archive
				dataFound = true;
			}
			// copy only the choosen task to user taskfolder, loop over all users
			final String taskfolderPath = FolderConfig.getCanonicalRoot() + TACourseNode.getTaskFolderPathRelToFolderRoot(course.getCourseEnvironment(), this);
			boolean taskFolderExist = false;
			for (final Iterator iter = users.iterator(); iter.hasNext();) {
				final Identity identity = (Identity) iter.next();
				// check if user already chose a task
				final String assignedTask = TaskController.getAssignedTask(identity, course.getCourseEnvironment(), this);
				if (assignedTask != null) {
					// copy choosen task to user folder
					final String tmpUserTaskDirPath = tmpDirPath + "/taskfolders/" + identity.getName();
					FileUtils.copyFileToDir(taskfolderPath + "/" + assignedTask, tmpUserTaskDirPath);
					taskFolderExist = true;
				}
			}
			if (taskFolderExist) {
				fileList.add("taskfolders");
			}

			// copy returnboxes to tmp dir
			if (returnboxDir.exists()) {
				FileUtils.copyDirContentsToDir(returnboxDir, new File(tmpDirPath + "/returnboxes"), false, "archive task course node returnboxes");
				fileList.add("returnboxes");
				// returnboxes exists, so there is something to archive
				dataFound |= true;
			}

			if (dataFound) {
				final String zipName = ExportUtil.createFileNameWithTimeStamp(this.getIdent(), "zip");

				final java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss_SSS");
				final String exportDirName = "task_" + this.getShortName() + "_" + formatter.format(new Date(System.currentTimeMillis()));
				final File fDropBoxArchiveDir = new File(fArchiveDirectory, exportDirName);
				if (!fDropBoxArchiveDir.exists()) {
					fDropBoxArchiveDir.mkdir();
				}
				final File archiveDir = new File(fDropBoxArchiveDir, zipName);

				// zip
				dataFound &= ZipUtil.zip(fileList, tmpDir, archiveDir, true);
				// Delete all temp files
				FileUtils.deleteDirsAndFiles(tmpDir, true, true);
			}
		}
		return dataFound;
	}

	/**
	 * Get the the place where all task folders are stored. Path relative to the folder root.
	 * 
	 * @param courseEnv
	 * @return the task folders path relative to the folder root.
	 */
	public static String getTaskFoldersPathRelToFolderRoot(final CourseEnvironment courseEnv) {
		return courseEnv.getCourseBaseContainer().getRelPath() + "/taskfolders";
	}

	/**
	 * Get the task folder path relative to the folder root for a specific node.
	 * 
	 * @param courseEnv
	 * @param cNode
	 * @return the task folder path relative to the folder root.
	 */
	public static String getTaskFolderPathRelToFolderRoot(final CourseEnvironment courseEnv, final CourseNode cNode) {
		return getTaskFoldersPathRelToFolderRoot(courseEnv) + "/" + cNode.getIdent();
	}

	/**
	 * Get the task folder path relative to the folder root for a specific node.
	 * 
	 * @param course
	 * @param cNode
	 * @return the task folder path relative to the folder root.
	 */
	public static String getTaskFolderPathRelToFolderRoot(final ICourse course, final CourseNode cNode) {
		return getTaskFolderPathRelToFolderRoot(course.getCourseEnvironment(), cNode);
	}

	/**
	 * Get the the place where all dropboxes are stored. Path relative to the folder root.
	 * 
	 * @param courseEnv
	 * @return the dropboxes path relative to the folder root.
	 */
	public static String getDropBoxesPathRelToFolderRoot(final CourseEnvironment courseEnv) {
		return courseEnv.getCourseBaseContainer().getRelPath() + "/dropboxes";
	}

	/**
	 * Get the dropbox path relative to the folder root for a specific node.
	 * 
	 * @param courseEnv
	 * @param cNode
	 * @return the dropbox path relative to the folder root.
	 */
	public static String getDropBoxPathRelToFolderRoot(final CourseEnvironment courseEnv, final CourseNode cNode) {
		return getDropBoxesPathRelToFolderRoot(courseEnv) + "/" + cNode.getIdent();
	}

	/**
	 * Get the dropbox path relative to the folder root for a specific node.
	 * 
	 * @param course
	 * @param cNode
	 * @return the dropbox path relative to the folder root.
	 */
	public static String getDropBoxPathRelToFolderRoot(final ICourse course, final CourseNode cNode) {
		return getDropBoxPathRelToFolderRoot(course.getCourseEnvironment(), cNode);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#getConditionExpressions()
	 */
	@Override
	public List getConditionExpressions() {
		ArrayList retVal;
		final List parentsConditions = super.getConditionExpressions();
		if (parentsConditions.size() > 0) {
			retVal = new ArrayList(parentsConditions);
		} else {
			retVal = new ArrayList();
		}
		//
		String coS = getConditionDrop().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			final ConditionExpression ce = new ConditionExpression(getConditionDrop().getConditionId());
			ce.setExpressionString(getConditionDrop().getConditionExpression());
			retVal.add(ce);
		}
		coS = getConditionReturnbox().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			final ConditionExpression ce = new ConditionExpression(getConditionReturnbox().getConditionId());
			ce.setExpressionString(getConditionReturnbox().getConditionExpression());
			retVal.add(ce);
		} else if (coS == null && getConditionDrop().getConditionExpression() != null && !getConditionDrop().getConditionExpression().equals("")) {
			// old courses that had dropbox but no returnbox: use for returnbox the conditionExpression from dropbox
			final ConditionExpression ce = new ConditionExpression(getConditionReturnbox().getConditionId());
			ce.setExpressionString(getConditionDrop().getConditionExpression());
			retVal.add(ce);
		}
		coS = getConditionScoring().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			final ConditionExpression ce = new ConditionExpression(getConditionScoring().getConditionId());
			ce.setExpressionString(getConditionScoring().getConditionExpression());
			retVal.add(ce);
		}
		coS = getConditionTask().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			final ConditionExpression ce = new ConditionExpression(getConditionTask().getConditionId());
			ce.setExpressionString(getConditionTask().getConditionExpression());
			retVal.add(ce);
		}
		//
		return retVal;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	public static OlatNamedContainerImpl getNodeFolderContainer(final TACourseNode node, final CourseEnvironment courseEnvironment) {
		final String path = getFoldernodePathRelToFolderBase(courseEnvironment, node);
		final OlatRootFolderImpl rootFolder = new OlatRootFolderImpl(path, null);
		final OlatNamedContainerImpl namedFolder = new OlatNamedContainerImpl(TACourseNode.SOLUTION_FOLDER_NAME, rootFolder);
		return namedFolder;
	}

	/**
	 * @param courseEnv
	 * @param node
	 * @return the relative folder base path for this folder node
	 */
	private static String getFoldernodePathRelToFolderBase(final CourseEnvironment courseEnvironment, final TACourseNode node) {
		return getFoldernodesPathRelToFolderBase(courseEnvironment) + "/" + node.getIdent();
	}

	/**
	 * @param courseEnv
	 * @return the relative folder base path for folder nodes
	 */
	public static String getFoldernodesPathRelToFolderBase(final CourseEnvironment courseEnv) {
		return courseEnv.getCourseBaseContainer().getRelPath() + "/" + TACourseNode.SOLUTION_FOLDER_NAME;
	}

	/**
	 * Init config parameter with default values for a new course node.
	 */
	@Override
	public void updateModuleConfigDefaults(final boolean isNewNode) {
		final ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode) {
			// use defaults for new course building blocks
			// task defaults
			config.set(CONF_TASK_ENABLED, Boolean.TRUE);
			config.set(CONF_TASK_TYPE, TaskController.TYPE_MANUAL);
			config.set(CONF_TASK_TEXT, "");
			config.set(CONF_TASK_SAMPLING_WITH_REPLACEMENT, Boolean.TRUE);
			// dropbox defaults
			config.set(CONF_DROPBOX_ENABLED, Boolean.TRUE);
			config.set(CONF_RETURNBOX_ENABLED, Boolean.TRUE);
			config.set(CONF_DROPBOX_ENABLEMAIL, Boolean.FALSE);
			config.set(CONF_DROPBOX_CONFIRMATION, "");
			// scoring defaults
			config.set(CONF_SCORING_ENABLED, Boolean.TRUE);
			// New config parameter version 2
			config.setBooleanEntry(CONF_TASK_PREVIEW, false);
			// solution defaults
			config.set(CONF_SOLUTION_ENABLED, Boolean.TRUE);
			MSCourseNode.initDefaultConfig(config);
			config.setConfigurationVersion(CURRENT_CONFIG_VERSION);
		} else {
			int version = config.getConfigurationVersion();
			if (version < CURRENT_CONFIG_VERSION) {
				// Loaded config is older than current config version => migrate
				if (version == 1) {
					// migrate V1 => V2
					config.setBooleanEntry(CONF_TASK_PREVIEW, false);
					// solution defaults
					config.set(CONF_SOLUTION_ENABLED, Boolean.FALSE);
					MSCourseNode.initDefaultConfig(config);
					version = 2;
				}
				config.setConfigurationVersion(CURRENT_CONFIG_VERSION);
			}
		}
	}

}