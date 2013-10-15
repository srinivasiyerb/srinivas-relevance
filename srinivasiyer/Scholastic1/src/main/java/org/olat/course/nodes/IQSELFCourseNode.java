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
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.id.Identity;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.iq.IQEditController;
import org.olat.course.nodes.iq.IQRunController;
import org.olat.course.nodes.iq.IQUIFactory;
import org.olat.course.repository.ImportReferencesController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.ims.qti.QTIResultManager;
import org.olat.ims.qti.QTIResultSet;
import org.olat.ims.qti.export.QTIExportFormatter;
import org.olat.ims.qti.export.QTIExportFormatterCSVType2;
import org.olat.ims.qti.export.QTIExportManager;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.iq.IQManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;
import org.olat.repository.RepositoryManager;

/**
 * Initial Date: Feb 9, 2004
 * 
 * @author Mike Stock
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class IQSELFCourseNode extends AbstractAccessableCourseNode implements SelfAssessableCourseNode {

	private static final String PACKAGE_IQ = Util.getPackageName(IQRunController.class);
	private static final String TYPE = "iqself";

	/**
	 * Constructor to create a course node of type IMS QTI.
	 */
	public IQSELFCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final UserCourseEnvironment euce) {

		final TabbableController childTabCntrllr = IQUIFactory.createIQSelftestEditController(ureq, wControl, course, this, course.getCourseEnvironment()
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

		final Controller runController = IQUIFactory.createIQSelftestRunController(ureq, wControl, userCourseEnv, ne, this);
		final Controller ctrl = TitledWrapperHelper.getWrapper(ureq, wControl, runController, this, "o_iqself_icon");

		return new NodeRunConstructionResult(ctrl);
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
				// FIXME:ms: may be show a refined error message, that the former
				// referenced repo entry is meanwhile deleted.
			}
		}
		StatusDescription sd = StatusDescription.NOERROR;
		if (!isValid) {
			final String shortKey = "error.self.undefined.short";
			final String longKey = "error.self.undefined.long";
			final String[] params = new String[] { this.getShortTitle() };
			final String translPackage = Util.getPackageName(IQEditController.class);
			sd = new StatusDescription(ValidationStatus.ERROR, shortKey, longKey, params, translPackage);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(IQEditController.PANE_TAB_IQCONFIG_SELF);
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
		// Check if there are qtiresults for this selftest
		final String repositorySoftKey = (String) getModuleConfiguration().get(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
		final Long repKey = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(repositorySoftKey, true).getKey();
		if (QTIResultManager.getInstance().hasResultSets(course.getResourceableId(), this.getIdent(), repKey)) { return new PackageTranslator(PACKAGE_IQ, locale)
				.translate("info.nodedelete"); }
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#cleanupOnDelete(org.olat.course.ICourse)
	 */
	@Override
	public void cleanupOnDelete(final ICourse course) {
		// Delete all qtiresults for this node. No properties used on this node
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

		final QTIExportManager qem = QTIExportManager.getInstance();
		final String repositorySoftKey = (String) getModuleConfiguration().get(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
		final Long repKey = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(repositorySoftKey, true).getKey();

		final QTIExportFormatter qef = new QTIExportFormatterCSVType2(locale, null, "\t", "\"", "\\", "\r\n", false);
		return qem.selectAndExportResults(qef, course.getResourceableId(), this.getShortTitle(), this.getIdent(), repKey, exportDirectory, charset, ".xls");

	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#exportNode(java.io.File, org.olat.course.ICourse)
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
			ImportReferencesController.doImport(rie, this, ImportReferencesController.IMPORT_TEST, true, admin);
			return null;
		} else {
			return new ImportReferencesController(ureq, wControl, this, ImportReferencesController.IMPORT_TEST, rie);
		}
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
			config.set(IQEditController.CONFIG_KEY_TYPE, AssessmentInstance.QMD_ENTRY_TYPE_SELF);
			config.set(IQEditController.CONFIG_KEY_SUMMARY, AssessmentInstance.QMD_ENTRY_SUMMARY_DETAILED);
		}
	}

	/**
	 * @see org.olat.course.nodes.SelfAssessableCourseNode#getUserScoreEvaluation(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public ScoreEvaluation getUserScoreEvaluation(final UserCourseEnvironment userCourseEnv) {
		final Identity identity = userCourseEnv.getIdentityEnvironment().getIdentity();
		final long olatResourceId = userCourseEnv.getCourseEnvironment().getCourseResourceableId().longValue();
		final QTIResultSet qTIResultSet = IQManager.getInstance().getLastResultSet(identity, olatResourceId, this.getIdent());
		if (qTIResultSet != null) {
			final ScoreEvaluation scoreEvaluation = new ScoreEvaluation(new Float(qTIResultSet.getScore()), new Boolean(qTIResultSet.getIsPassed()), new Long(
					qTIResultSet.getAssessmentID()));
			return scoreEvaluation;
		}
		return null;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#incrementUserAttempts(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public void incrementUserAttempts(final UserCourseEnvironment userCourseEnvironment) {
		final AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		final Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		am.incrementNodeAttempts(this, mySelf, userCourseEnvironment);
	}

}
