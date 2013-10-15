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
import java.util.List;
import java.util.Locale;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.id.Identity;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.cp.CPEditController;
import org.olat.course.nodes.scorm.ScormEditController;
import org.olat.course.nodes.scorm.ScormRunController;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.repository.ImportReferencesController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.scorm.assessment.ScormResultDetailsController;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;

/**
 * Description:<br>
 * 
 * @author Felix Jost
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class ScormCourseNode extends AbstractAccessableCourseNode implements AssessableCourseNode {
	private static final String TYPE = "scorm";
	private static final int CURRENT_CONFIG_VERSION = 3;

	/**
	 * Constructor for a course building block of the type IMS CP learning content
	 */
	public ScormCourseNode() {
		super(TYPE);
		// init default values
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		final ScormEditController childTabCntrllr = new ScormEditController(this, ureq, wControl, course, euce);
		final CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, course.getCourseEnvironment().getCourseGroupManager(), euce,
				childTabCntrllr);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv,
			final NodeEvaluation ne, final String nodecmd) {
		updateModuleConfigDefaults(false);
		final ScormRunController cprunC = new ScormRunController(getModuleConfiguration(), ureq, userCourseEnv, wControl, this, false);
		final Controller ctrl = TitledWrapperHelper.getWrapper(ureq, wControl, cprunC, this, "o_scorm_icon");
		// no inline-in-olat-menu integration possible: no display configuration option
		return new NodeRunConstructionResult(ctrl);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPreviewController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPreviewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		updateModuleConfigDefaults(false);
		final ScormRunController cprunC = new ScormRunController(getModuleConfiguration(), ureq, userCourseEnv, wControl, this, true);
		return new NodeRunConstructionResult(cprunC).getRunController();
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	@Override
	public StatusDescription isConfigValid() {
		/*
		 * first check the one click cache
		 */
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		StatusDescription sd = StatusDescription.NOERROR;
		final boolean isValid = ScormEditController.isModuleConfigValid(getModuleConfiguration());
		if (!isValid) {
			final String shortKey = "error.noreference.short";
			final String longKey = "error.noreference.long";
			final String[] params = new String[] { this.getShortTitle() };
			final String translPackage = Util.getPackageName(ScormEditController.class);
			sd = new StatusDescription(ValidationStatus.ERROR, shortKey, longKey, params, translPackage);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(ScormEditController.PANE_TAB_CPCONFIG);
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
		final String translatorStr = Util.getPackageName(ScormEditController.class);
		final List<StatusDescription> sds = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		// ",false" because we do not want to be strict, but just indicate whether
		// the reference still exists or not
		final RepositoryEntry entry = CPEditController.getCPReference(getModuleConfiguration(), false);
		return entry;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	@Override
	public boolean needsReferenceToARepositoryEntry() {
		return true;
	}

	/**
	 * Update the module configuration to have all mandatory configuration flags set to usefull default values
	 * 
	 * @param isNewNode true: an initial configuration is set; false: upgrading from previous node configuration version, set default to maintain previous behaviour
	 */
	@Override
	public void updateModuleConfigDefaults(final boolean isNewNode) {
		final ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode) {
			// use defaults for new course building blocks
			config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, Boolean.TRUE.booleanValue());
			config.setBooleanEntry(NodeEditController.CONFIG_COMPONENT_MENU, Boolean.TRUE.booleanValue());
			config.setBooleanEntry(ScormEditController.CONFIG_SHOWNAVBUTTONS, Boolean.TRUE.booleanValue());
			config.set(ScormEditController.CONFIG_HEIGHT, ScormEditController.CONFIG_HEIGHT_AUTO);
			config.setConfigurationVersion(1);
		} else {
			int version = config.getConfigurationVersion();
			if (version < CURRENT_CONFIG_VERSION) {
				// Loaded config is older than current config version => migrate
				if (version == 1) {
					version = 2;
					// remove old config from previous versions
					config.remove(NodeEditController.CONFIG_INTEGRATION);
					// add new parameter 'shownavbuttons' and 'height'
					config.setBooleanEntry(ScormEditController.CONFIG_SHOWNAVBUTTONS, Boolean.TRUE.booleanValue());
					config.set(ScormEditController.CONFIG_HEIGHT, ScormEditController.CONFIG_HEIGHT_AUTO);
				}

				if (version == 2) {
					version = 3;
					config.set(NodeEditController.CONFIG_CONTENT_ENCODING, NodeEditController.CONFIG_CONTENT_ENCODING_AUTO);
					config.set(NodeEditController.CONFIG_JS_ENCODING, NodeEditController.CONFIG_JS_ENCODING_AUTO);
				}
				// version is now set to current version
				config.setConfigurationVersion(CURRENT_CONFIG_VERSION);
			}
		}
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#exportNode(java.io.File, org.olat.course.ICourse)
	 */
	@Override
	public void exportNode(final File exportDirectory, final ICourse course) {
		final RepositoryEntry re = CPEditController.getCPReference(getModuleConfiguration(), false);
		if (re == null) { return; }
		final File fExportDirectory = new File(exportDirectory, getIdent());
		fExportDirectory.mkdirs();
		final RepositoryEntryImportExport reie = new RepositoryEntryImportExport(re, fExportDirectory);
		reie.exportDoExport();
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#importNode(java.io.File, org.olat.course.ICourse, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller importNode(final File importDirectory, final ICourse course, final boolean unattendedImport, final UserRequest ureq, final WindowControl wControl) {
		final File importSubdir = new File(importDirectory, getIdent());
		final RepositoryEntryImportExport rie = new RepositoryEntryImportExport(importSubdir);
		if (!rie.anyExportedPropertiesAvailable()) { return null; }

		// do import referenced repository entries
		if (unattendedImport) {
			final Identity admin = BaseSecurityManager.getInstance().findIdentityByName("administrator");
			ImportReferencesController.doImport(rie, this, ImportReferencesController.IMPORT_SCORM, true, admin);
			return null;
		} else {
			return new ImportReferencesController(ureq, wControl, this, ImportReferencesController.IMPORT_SCORM, rie);
		}
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createInstanceForCopy()
	 */
	@Override
	public CourseNode createInstanceForCopy() {
		final CourseNode copyInstance = super.createInstanceForCopy();
		CPEditController.removeCPReference(copyInstance.getModuleConfiguration());
		return copyInstance;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserScoreEvaluation(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public ScoreEvaluation getUserScoreEvaluation(final UserCourseEnvironment userCourseEnvironment) {
		// read score from properties save score, passed and attempts information
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		final Boolean passed = am.getNodePassed(this, mySelf);
		final Float score = am.getNodeScore(this, mySelf);
		final ScoreEvaluation se = new ScoreEvaluation(score, passed);
		return se;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getCutValueConfiguration()
	 */
	@Override
	public Float getCutValueConfiguration() {
		final ModuleConfiguration config = this.getModuleConfiguration();
		final int cutValue = config.getIntegerSafe(ScormEditController.CONFIG_CUTVALUE, 0);
		return new Float(new Integer(cutValue).floatValue());
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getMaxScoreConfiguration()
	 */
	@Override
	public Float getMaxScoreConfiguration() {
		// According to SCORM Standard, SCORE is between 0 and 100.
		return new Float(100);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getMinScoreConfiguration()
	 */
	@Override
	public Float getMinScoreConfiguration() {
		// According to SCORM Standard, SCORE is between 0 and 100.
		return new Float(0);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasCommentConfigured()
	 */
	@Override
	public boolean hasCommentConfigured() {
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasPassedConfigured()
	 */
	@Override
	public boolean hasPassedConfigured() {
		return getModuleConfiguration().getBooleanSafe(ScormEditController.CONFIG_ISASSESSABLE, false);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasScoreConfigured()
	 */
	@Override
	public boolean hasScoreConfigured() {
		return getModuleConfiguration().getBooleanSafe(ScormEditController.CONFIG_ISASSESSABLE, false);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasStatusConfigured()
	 */
	@Override
	public boolean hasStatusConfigured() {
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#isEditableConfigured()
	 */
	@Override
	public boolean isEditableConfigured() {
		return getModuleConfiguration().getBooleanSafe(ScormEditController.CONFIG_ISASSESSABLE, false);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserCoachComment(java.lang.String, org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public void updateUserCoachComment(final String coachComment, final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		if (coachComment != null) {
			am.saveNodeCoachComment(this, userCourseEnvironment.getIdentityEnvironment().getIdentity(), coachComment);
		}
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserScoreEvaluation(org.olat.course.run.scoring.ScoreEvaluation,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.core.id.Identity)
	 */
	@Override
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
	@Override
	public void updateUserUserComment(final String userComment, final UserCourseEnvironment userCourseEnvironment, final Identity coachingIdentity) {
		if (userComment != null) {
			final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
			final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
			am.saveNodeComment(this, coachingIdentity, mySelf, userComment);
		}
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserCoachComment(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getUserCoachComment(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		final String coachCommentValue = am.getNodeCoachComment(this, mySelf);
		return coachCommentValue;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserUserComment(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getUserUserComment(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		final String userCommentValue = am.getNodeComment(this, mySelf);
		return userCommentValue;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserLog(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getUserLog(final UserCourseEnvironment userCourseEnvironment) {
		final UserNodeAuditManager am = userCourseEnvironment.getCourseEnvironment().getAuditManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		final String logValue = am.getUserNodeLog(this, mySelf);
		return logValue;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserAttempts(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public Integer getUserAttempts(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		final Integer userAttemptsValue = am.getNodeAttempts(this, mySelf);
		return userAttemptsValue;

	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasAttemptsConfigured()
	 */
	@Override
	public boolean hasAttemptsConfigured() {
		return getModuleConfiguration().getBooleanSafe(ScormEditController.CONFIG_ISASSESSABLE, false);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserAttempts(java.lang.Integer, org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.core.id.Identity)
	 */
	@Override
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
	@Override
	public void incrementUserAttempts(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		am.incrementNodeAttempts(this, mySelf, userCourseEnvironment);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public Controller getDetailsEditController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnvironment) {
		return new ScormResultDetailsController(ureq, wControl, this, userCourseEnvironment);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsListView(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getDetailsListView(final UserCourseEnvironment userCourseEnvironment) {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsListViewHeaderKey()
	 */
	@Override
	public String getDetailsListViewHeaderKey() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasDetails()
	 */
	@Override
	public boolean hasDetails() {
		return getModuleConfiguration().getBooleanSafe(ScormEditController.CONFIG_ISASSESSABLE, false);
	}

	// //////////////////////////// fix it

	/**
	 * @see org.olat.course.nodes.CourseNode#informOnDelete(org.olat.core.gui.UserRequest, org.olat.course.ICourse)
	 */
	@Override
	public String informOnDelete(final Locale locale, final ICourse course) {
		// checking for data is too complex - we would have to work through all users
		// of the system since data is stored under users name instead of the repo entry
		// with many users on the system it could take quite some time and the user
		// is waiting in a workflow
		// FIXME gs
		// see comment on cleanupOnDelete
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#cleanupOnDelete(org.olat.course.ICourse)
	 */
	@Override
	public void cleanupOnDelete(final ICourse course) {
		final CoursePropertyManager pm = course.getCourseEnvironment().getCoursePropertyManager();
		// 1) Delete all properties: score, passed, log, comment, coach_comment,
		// attempts
		pm.deleteNodeProperties(this, null);
		// 2) Delete all user files for this scorm node
		// FIXME gs
		// it is problematic that the data is stored using username/courseid-scormid/
		// much better would be /courseid-scormid/username/
		// I would consider refatoring this and setting up an upgrade task that moves the
		// folders accordingly
	}

	/**
	 * Override default implementation
	 * 
	 * @see org.olat.course.nodes.CourseNode#archiveNodeData(java.util.Locale, org.olat.course.ICourse, java.io.File)
	 */
	@Override
	public boolean archiveNodeData(final Locale locale, final ICourse course, final File exportDirectory, final String charset) {
		return super.archiveNodeData(locale, course, exportDirectory, charset);
		// copy all user directories containing the xml files into the export dir

		// FIXME gs
		// we would have to work through all users
		// of the system since data is stored under users name instead of the repo entry
		// with many users on the system it could take quite some time and the user
		// is waiting in a workflow
		// see comment on cleanupOnDelete
	}

}