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
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.logging.DBRuntimeException;
import org.olat.core.logging.KnownIssueException;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.iq.IQEditController;
import org.olat.course.nodes.iq.IQRunController;
import org.olat.course.nodes.iq.IQUIFactory;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.repository.ImportReferencesController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.ims.qti.QTIResultManager;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.modules.ModuleConfiguration;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;
import org.olat.repository.RepositoryManager;

/**
 * Initial Date: Feb 9, 2004
 * 
 * @author Mike Stock Comment:
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class IQTESTCourseNode extends AbstractAccessableCourseNode implements AssessableCourseNode {
	private static final String TYPE = "iqtest";

	private static final int CURRENT_CONFIG_VERSION = 2;

	public IQTESTCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		final TabbableController childTabCntrllr = IQUIFactory.createIQTestEditController(ureq, wControl, course, this, course.getCourseEnvironment()
				.getCourseGroupManager(), euce);
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
		final Controller controller = IQUIFactory.createIQTestRunController(ureq, wControl, userCourseEnv, ne, this);
		final Controller ctrl = TitledWrapperHelper.getWrapper(ureq, wControl, controller, this, "o_iqtest_icon");
		return new NodeRunConstructionResult(ctrl);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createPreviewController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPreviewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		return IQUIFactory.createIQTestPreviewController(ureq, wControl, userCourseEnv, ne, this);
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

		boolean isValid = getModuleConfiguration().get(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY) != null;
		if (isValid) {
			/*
			 * COnfiugre an IQxxx BB with a repo entry, do not publish this BB, mark IQxxx as deleted, remove repo entry, undelete BB IQxxx and bang you enter this if.
			 */
			final Object repoEntry = IQEditController.getIQReference(getModuleConfiguration(), false);
			if (repoEntry == null) {
				isValid = false;
				IQEditController.removeIQReference(getModuleConfiguration());
				// FIXME:ms: may be show a refined error message, that the former referenced repo entry is meanwhile deleted.
			}
		}
		StatusDescription sd = StatusDescription.NOERROR;
		if (!isValid) {
			// FIXME: refine statusdescriptions
			final String shortKey = "error.test.undefined.short";
			final String longKey = "error.test.undefined.long";
			final String[] params = new String[] { this.getShortTitle() };
			final String translPackage = Util.getPackageName(IQEditController.class);
			sd = new StatusDescription(ValidationStatus.ERROR, shortKey, longKey, params, translPackage);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(IQEditController.PANE_TAB_IQCONFIG_TEST);
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
		final String translatorStr = Util.getPackageName(IQEditController.class);
		final List sds = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
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
		final Long assessmentID = am.getAssessmentID(this, mySelf);
		final ScoreEvaluation se = new ScoreEvaluation(score, passed, assessmentID);
		return se;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getCutValueConfiguration()
	 */
	@Override
	public Float getCutValueConfiguration() {
		final ModuleConfiguration config = this.getModuleConfiguration();
		return (Float) config.get(IQEditController.CONFIG_KEY_CUTVALUE);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getMaxScoreConfiguration()
	 */
	@Override
	public Float getMaxScoreConfiguration() {
		final ModuleConfiguration config = this.getModuleConfiguration();
		return (Float) config.get(IQEditController.CONFIG_KEY_MAXSCORE);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getMinScoreConfiguration()
	 */
	@Override
	public Float getMinScoreConfiguration() {
		final ModuleConfiguration config = this.getModuleConfiguration();
		return (Float) config.get(IQEditController.CONFIG_KEY_MINSCORE);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasCommentConfigured()
	 */
	@Override
	public boolean hasCommentConfigured() {
		// coach should be able to add comments here, visible to users
		return true;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasPassedConfigured()
	 */
	@Override
	public boolean hasPassedConfigured() {
		return true;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasScoreConfigured()
	 */
	@Override
	public boolean hasScoreConfigured() {
		return true;
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
		// test scoring fields can be edited manually
		return true;
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
		try {
			am.saveScoreEvaluation(this, coachingIdentity, mySelf, scoreEvaluation, userCourseEnvironment, incrementAttempts);
		} catch (final DBRuntimeException ex) {
			throw new KnownIssueException("DBRuntimeException - Row was updated or deleted...", 3570);
		}
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
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		// ",false" because we do not want to be strict, but just indicate whether
		// the reference still exists or not
		final RepositoryEntry re = IQEditController.getIQReference(getModuleConfiguration(), false);
		return re;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	@Override
	public boolean needsReferenceToARepositoryEntry() {
		return true;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#informOnDelete(org.olat.core.gui.UserRequest, org.olat.course.ICourse)
	 */
	@Override
	public String informOnDelete(final Locale locale, final ICourse course) {
		// Check if there are qtiresults for this test
		final String repositorySoftKey = (String) getModuleConfiguration().get(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
		final Long repKey = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(repositorySoftKey, true).getKey();
		if (QTIResultManager.getInstance().hasResultSets(course.getResourceableId(), this.getIdent(), repKey)) {
			final Translator trans = Util.createPackageTranslator(IQRunController.class, locale);
			return trans.translate("info.nodedelete");
		}
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
		// 2) Delete all qtiresults for this node
		final String repositorySoftKey = (String) getModuleConfiguration().get(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
		final Long repKey = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(repositorySoftKey, true).getKey();
		QTIResultManager.getInstance().deleteAllResults(course.getResourceableId(), this.getIdent(), repKey);
	}

	/**
	 * Override default implementation
	 * 
	 * @see org.olat.course.nodes.CourseNode#archiveNodeData(java.util.Locale, org.olat.course.ICourse, java.io.File)
	 */
	@Override
	public boolean archiveNodeData(final Locale locale, final ICourse course, final File exportDirectory, final String charset) {
		super.archiveNodeData(locale, course, exportDirectory, charset);
		final String repositorySoftKey = (String) getModuleConfiguration().get(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
		final Long courseResourceableId = course.getResourceableId();
		final String shortTitle = this.getShortTitle();
		final String ident = this.getIdent();
		return IQUIFactory.archive(locale, repositorySoftKey, courseResourceableId, shortTitle, ident, exportDirectory, charset);

	}

	/**
	 * @see org.olat.course.nodes.CourseNode#exportNode(java.io.File, org.olat.course.ICourse)
	 */
	@Override
	public void exportNode(final File exportDirectory, final ICourse course) {
		final String repositorySoftKey = (String) getModuleConfiguration().get(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
		if (repositorySoftKey == null) { return; // nothing to export
		}
		// self healing
		final RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(repositorySoftKey, false);
		if (re == null) {
			// nothing to export, but correct the module configuration
			IQEditController.removeIQReference(getModuleConfiguration());
			return;
		}
		final File fExportDirectory = new File(exportDirectory, getIdent());
		fExportDirectory.mkdirs();
		final RepositoryEntryImportExport reie = new RepositoryEntryImportExport(re, fExportDirectory);
		reie.exportDoExport();
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#importNode(java.io.File, org.olat.course.ICourse, org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller importNode(final File importDirectory, final ICourse course, final boolean unattendedImport, final UserRequest ureq, final WindowControl wControl) {
		final File importSubdir = new File(importDirectory, getIdent());
		final RepositoryEntryImportExport rie = new RepositoryEntryImportExport(importSubdir);
		if (!rie.anyExportedPropertiesAvailable()) { return null; }

		// do import referenced repository entries
		if (unattendedImport) {
			final Identity admin = BaseSecurityManager.getInstance().findIdentityByName("administrator");
			ImportReferencesController.doImport(rie, this, ImportReferencesController.IMPORT_TEST, true, admin);
			return null;
		} else {
			return new ImportReferencesController(ureq, wControl, this, ImportReferencesController.IMPORT_TEST, rie);
		}
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
		return true;
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
		return IQUIFactory.createIQTestDetailsEditController(userCourseEnvironment.getCourseEnvironment().getCourseResourceableId(), this.getIdent(),
				userCourseEnvironment.getIdentityEnvironment().getIdentity(), this.getReferencedRepositoryEntry(), AssessmentInstance.QMD_ENTRY_TYPE_ASSESS, ureq,
				wControl);
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
		return true;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createInstanceForCopy()
	 */
	@Override
	public CourseNode createInstanceForCopy() {
		final CourseNode copyInstance = super.createInstanceForCopy();
		IQEditController.removeIQReference(copyInstance.getModuleConfiguration());
		return copyInstance;
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
			// add default module configuration
			config.set(IQEditController.CONFIG_KEY_ENABLEMENU, new Boolean(true));
			config.set(IQEditController.CONFIG_KEY_SEQUENCE, AssessmentInstance.QMD_ENTRY_SEQUENCE_ITEM);
			config.set(IQEditController.CONFIG_KEY_TYPE, AssessmentInstance.QMD_ENTRY_TYPE_ASSESS);
			config.set(IQEditController.CONFIG_KEY_SUMMARY, AssessmentInstance.QMD_ENTRY_SUMMARY_COMPACT);
			config.set(IQEditController.CONFIG_KEY_ENABLESCOREINFO, new Boolean(true));
		} else {
			int version = config.getConfigurationVersion();
			if (version < CURRENT_CONFIG_VERSION) {
				// Loaded config is older than current config version => migrate
				if (version == 1) {
					// migrate V1 => V2, new parameter 'enableScoreInfo'
					version = 2;
					config.set(IQEditController.CONFIG_KEY_ENABLESCOREINFO, new Boolean(true));
				}
				config.setConfigurationVersion(CURRENT_CONFIG_VERSION);
			}
		}
	}

}