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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.course.nodes;

import java.io.File;
import java.util.List;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.portfolio.PortfolioCourseNodeConfiguration;
import org.olat.course.nodes.portfolio.PortfolioCourseNodeEditController;
import org.olat.course.nodes.portfolio.PortfolioCourseNodeRunController;
import org.olat.course.nodes.portfolio.PortfolioResultDetailsController;
import org.olat.course.repository.ImportPortfolioReferencesController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.portfolio.manager.EPStructureManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;
import org.olat.repository.RepositoryManager;

/**
 * Description:<br>
 * course node of type portfolio.
 * <P>
 * Initial Date: 6 oct. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class PortfolioCourseNode extends AbstractAccessableCourseNode implements AssessableCourseNode {

	private static final int CURRENT_CONFIG_VERSION = 2;

	public static final String EDIT_CONDITION_ID = "editportfolio";

	private static final String PACKAGE_EP = Util.getPackageName(PortfolioCourseNodeRunController.class);
	private static final String TYPE = "ep";

	private Condition preConditionEdit;

	public PortfolioCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	@Override
	public void updateModuleConfigDefaults(final boolean isNewNode) {
		final ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode) {
			MSCourseNode.initDefaultConfig(config);
			config.setConfigurationVersion(CURRENT_CONFIG_VERSION);
		}
		if (config.getConfigurationVersion() < 2) {
			if (config.get(PortfolioCourseNodeConfiguration.REPO_SOFT_KEY) == null) {
				final Object mapKey = config.get(PortfolioCourseNodeConfiguration.MAP_KEY);
				if (mapKey instanceof Long) {
					final EPStructureManager eSTMgr = (EPStructureManager) CoreSpringFactory.getBean("epStructureManager");
					final RepositoryEntry re = eSTMgr.loadPortfolioRepositoryEntryByMapKey((Long) mapKey);
					config.set(PortfolioCourseNodeConfiguration.REPO_SOFT_KEY, re.getSoftkey());
				}
			}
			config.setConfigurationVersion(2);
		}
	}

	@Override
	public TabbableController createEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final UserCourseEnvironment euce) {
		final PortfolioCourseNodeEditController childTabCntrllr = new PortfolioCourseNodeEditController(ureq, wControl, course, this, getModuleConfiguration(), euce);
		updateModuleConfigDefaults(false);
		final CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, course.getCourseEnvironment().getCourseGroupManager(), euce,
				childTabCntrllr);
	}

	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv,
			final NodeEvaluation ne, final String nodecmd) {
		updateModuleConfigDefaults(false);
		final PortfolioCourseNodeRunController portfolioCtrl = new PortfolioCourseNodeRunController(ureq, wControl, userCourseEnv, ne, this);
		final Controller ctrl = TitledWrapperHelper.getWrapper(ureq, wControl, portfolioCtrl, this, "o_ep_icon");
		return new NodeRunConstructionResult(ctrl);
	}

	/**
	 * Default set the write privileges to coaches and admin only
	 * 
	 * @return
	 */
	public Condition getPreConditionEdit() {
		if (preConditionEdit == null) {
			preConditionEdit = new Condition();
			preConditionEdit.setEasyModeCoachesAndAdmins(true);
			preConditionEdit.setConditionExpression(preConditionEdit.getConditionFromEasyModeConfiguration());
			preConditionEdit.setExpertMode(false);
		}
		preConditionEdit.setConditionId(EDIT_CONDITION_ID);
		return preConditionEdit;
	}

	/**
	 * @param preConditionEdit
	 */
	public void setPreConditionEdit(Condition preConditionEdit) {
		if (preConditionEdit == null) {
			preConditionEdit = getPreConditionEdit();
		}
		preConditionEdit.setConditionId(EDIT_CONDITION_ID);
		this.preConditionEdit = preConditionEdit;
	}

	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		final Object repoSoftkey = getModuleConfiguration().get(PortfolioCourseNodeConfiguration.REPO_SOFT_KEY);
		if (repoSoftkey instanceof String) {
			final RepositoryManager rm = RepositoryManager.getInstance();
			final RepositoryEntry entry = rm.lookupRepositoryEntryBySoftkey((String) repoSoftkey, false);
			if (entry != null) { return entry; }
		}
		final Long mapKey = (Long) getModuleConfiguration().get(PortfolioCourseNodeConfiguration.MAP_KEY);
		if (mapKey != null) {
			final EPStructureManager eSTMgr = (EPStructureManager) CoreSpringFactory.getBean("epStructureManager");
			final RepositoryEntry re = eSTMgr.loadPortfolioRepositoryEntryByMapKey(mapKey);
			return re;
		}
		return null;
	}

	@Override
	public boolean needsReferenceToARepositoryEntry() {
		return true;
	}

	@Override
	public StatusDescription isConfigValid() {
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		StatusDescription sd = StatusDescription.NOERROR;
		final boolean isValid = PortfolioCourseNodeEditController.isModuleConfigValid(getModuleConfiguration());
		if (!isValid) {
			final String shortKey = "error.noreference.short";
			final String longKey = "error.noreference.long";
			final String[] params = new String[] { getShortTitle() };
			sd = new StatusDescription(ValidationStatus.ERROR, shortKey, longKey, params, PACKAGE_EP);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(PortfolioCourseNodeEditController.PANE_TAB_CONFIG);
		}
		return sd;
	}

	@Override
	public StatusDescription[] isConfigValid(final CourseEditorEnv cev) {
		oneClickStatusCache = null;
		final List<StatusDescription> statusDescs = isConfigValidWithTranslator(cev, PACKAGE_EP, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(statusDescs);
		return oneClickStatusCache;
	}

	@Override
	protected void calcAccessAndVisibility(final ConditionInterpreter ci, final NodeEvaluation nodeEval) {
		// nodeEval.setVisible(true);
		super.calcAccessAndVisibility(ci, nodeEval);

		// evaluate the preconditions
		final boolean editor = (getPreConditionEdit().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionEdit()));
		nodeEval.putAccessStatus(EDIT_CONDITION_ID, editor);
	}

	@Override
	public Float getMaxScoreConfiguration() {
		if (!hasScoreConfigured()) { throw new OLATRuntimeException(PortfolioCourseNode.class, "getMaxScore not defined when hasScore set to false", null); }
		final ModuleConfiguration config = getModuleConfiguration();
		final Float max = (Float) config.get(MSCourseNode.CONFIG_KEY_SCORE_MAX);
		return max;
	}

	@Override
	public Float getMinScoreConfiguration() {
		if (!hasScoreConfigured()) { throw new OLATRuntimeException(PortfolioCourseNode.class, "getMinScore not defined when hasScore set to false", null); }
		final ModuleConfiguration config = getModuleConfiguration();
		final Float min = (Float) config.get(MSCourseNode.CONFIG_KEY_SCORE_MIN);
		return min;
	}

	@Override
	public Float getCutValueConfiguration() {
		if (!hasPassedConfigured()) { throw new OLATRuntimeException(PortfolioCourseNode.class, "getCutValue not defined when hasPassed set to false", null); }
		final ModuleConfiguration config = getModuleConfiguration();
		final Float cut = (Float) config.get(MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE);
		return cut;
	}

	@Override
	public boolean hasScoreConfigured() {
		final ModuleConfiguration config = getModuleConfiguration();
		final Boolean score = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD);
		return (score == null) ? false : score.booleanValue();
	}

	@Override
	public boolean hasPassedConfigured() {
		final ModuleConfiguration config = getModuleConfiguration();
		final Boolean passed = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD);
		return (passed == null) ? false : passed.booleanValue();
	}

	@Override
	public boolean hasCommentConfigured() {
		final ModuleConfiguration config = getModuleConfiguration();
		final Boolean comment = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_COMMENT_FIELD);
		return (comment == null) ? false : comment.booleanValue();
	}

	@Override
	public boolean hasAttemptsConfigured() {
		return true;
	}

	@Override
	public boolean hasDetails() {
		return true;
	}

	@Override
	public boolean isEditableConfigured() {
		return true;
	}

	@Override
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

	@Override
	public String getUserUserComment(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final String userCommentValue = am.getNodeComment(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return userCommentValue;
	}

	@Override
	public String getUserCoachComment(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final String coachCommentValue = am.getNodeCoachComment(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return coachCommentValue;
	}

	@Override
	public String getUserLog(final UserCourseEnvironment userCourseEnvironment) {
		final UserNodeAuditManager am = userCourseEnvironment.getCourseEnvironment().getAuditManager();
		final String logValue = am.getUserNodeLog(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return logValue;
	}

	@Override
	public Integer getUserAttempts(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		final Integer userAttemptsValue = am.getNodeAttempts(this, mySelf);
		return userAttemptsValue;
	}

	@Override
	public String getDetailsListView(final UserCourseEnvironment userCourseEnvironment) {
		return null;
	}

	@Override
	public String getDetailsListViewHeaderKey() {
		return "table.header.details.ta";
	}

	@Override
	public Controller getDetailsEditController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnvironment) {
		return new PortfolioResultDetailsController(ureq, wControl, this, userCourseEnvironment);
	}

	@Override
	public void updateUserScoreEvaluation(final ScoreEvaluation scoreEvaluation, final UserCourseEnvironment userCourseEnvironment, final Identity coachingIdentity,
			final boolean incrementAttempts) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		am.saveScoreEvaluation(this, coachingIdentity, mySelf, new ScoreEvaluation(scoreEvaluation.getScore(), scoreEvaluation.getPassed()), userCourseEnvironment,
				incrementAttempts);
	}

	@Override
	public void updateUserUserComment(final String userComment, final UserCourseEnvironment userCourseEnvironment, final Identity coachingIdentity) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		if (userComment != null) {
			am.saveNodeComment(this, coachingIdentity, mySelf, userComment);
		}
	}

	@Override
	public void incrementUserAttempts(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		am.incrementNodeAttempts(this, mySelf, userCourseEnvironment);
	}

	@Override
	public void updateUserAttempts(final Integer userAttempts, final UserCourseEnvironment userCourseEnvironment, final Identity coachingIdentity) {
		if (userAttempts != null) {
			final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
			final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
			am.saveNodeAttempts(this, coachingIdentity, mySelf, userAttempts);
		}
	}

	@Override
	public void updateUserCoachComment(final String coachComment, final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		if (coachComment != null) {
			am.saveNodeCoachComment(this, mySelf, coachComment);
		}
	}

	@Override
	public boolean hasStatusConfigured() {
		return true;
	}

	@Override
	public void exportNode(final File exportDirectory, final ICourse course) {
		final RepositoryEntry re = getReferencedRepositoryEntry();
		if (re == null) { return; }

		final File fExportDirectory = new File(exportDirectory, getIdent());
		fExportDirectory.mkdirs();
		final RepositoryEntryImportExport reie = new RepositoryEntryImportExport(re, fExportDirectory);
		reie.exportDoExport();
	}

	@Override
	public Controller importNode(final File importDirectory, final ICourse course, final boolean unattendedImport, final UserRequest ureq, final WindowControl wControl) {
		final File importSubdir = new File(importDirectory, getIdent());
		final RepositoryEntryImportExport rie = new RepositoryEntryImportExport(importSubdir);
		if (!rie.anyExportedPropertiesAvailable()) { return null; }

		// do import referenced repository entries
		if (unattendedImport) {
			final Identity admin = BaseSecurityManager.getInstance().findIdentityByName("administrator");
			ImportPortfolioReferencesController.doImport(rie, this, true, admin);
			return null;
		} else {
			return new ImportPortfolioReferencesController(ureq, wControl, this, rie);
		}
	}
}