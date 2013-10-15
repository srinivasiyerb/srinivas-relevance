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

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.ICourse;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.cp.CPEditController;
import org.olat.course.nodes.cp.CPRunController;
import org.olat.course.repository.ImportReferencesController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;

/**
 * Description:<br>
 * 
 * @author Felix Jost
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class CPCourseNode extends AbstractAccessableCourseNode {
	private static final String TYPE = "cp";

	/**
	 * Constructor for a course building block of the type IMS CP learning content
	 */
	public CPCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		final CPEditController childTabCntrllr = new CPEditController(this, ureq, wControl, course, euce);
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
		NodeRunConstructionResult ncr;
		updateModuleConfigDefaults(false);
		final OLATResourceable ores = OresHelper.createOLATResourceableInstance(ICourse.class, userCourseEnv.getCourseEnvironment().getCourseResourceableId());
		final CPRunController cprunC = new CPRunController(getModuleConfiguration(), ureq, userCourseEnv, wControl, this, nodecmd, ores);
		ncr = cprunC.createNodeRunConstructionResult(ureq);
		return ncr;
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPreviewController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPreviewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		return createNodeRunConstructionResult(ureq, wControl, userCourseEnv, ne, null).getRunController();
	}

	@Override
	protected String getDefaultTitleOption() {
		return CourseNode.DISPLAY_OPTS_CONTENT;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	@Override
	public StatusDescription isConfigValid() {/*
											 * first check the one click cache
											 */
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		StatusDescription sd = StatusDescription.NOERROR;
		final boolean isValid = CPEditController.isModuleConfigValid(getModuleConfiguration());
		if (!isValid) {
			// FIXME: refine statusdescriptions
			final String shortKey = "error.noreference.short";
			final String longKey = "error.noreference.long";
			final String[] params = new String[] { this.getShortTitle() };
			final String translPackage = Util.getPackageName(CPEditController.class);
			sd = new StatusDescription(ValidationStatus.ERROR, shortKey, longKey, params, translPackage);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(CPEditController.PANE_TAB_CPCONFIG);
		}
		return sd;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public StatusDescription[] isConfigValid(final CourseEditorEnv cev) {
		// only here we know which translator to take for translating condition
		// error messages
		final String translatorStr = Util.getPackageName(ConditionEditController.class);
		final List statusDescs = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		return StatusDescriptionHelper.sort(statusDescs);
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
			config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, Boolean.FALSE.booleanValue());
			config.setBooleanEntry(NodeEditController.CONFIG_COMPONENT_MENU, Boolean.TRUE.booleanValue());
			config.setConfigurationVersion(2);
		} else {
			config.remove(NodeEditController.CONFIG_INTEGRATION);
			if (config.getConfigurationVersion() < 2) {
				// update new configuration options using default values for existing
				// nodes
				config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, Boolean.TRUE.booleanValue());
				final Boolean componentMenu = config.getBooleanEntry(NodeEditController.CONFIG_COMPONENT_MENU);
				if (componentMenu == null) {
					config.setBooleanEntry(NodeEditController.CONFIG_COMPONENT_MENU, Boolean.TRUE.booleanValue());
				}
				config.setConfigurationVersion(2);
			}

			if (config.getConfigurationVersion() < 3) {
				config.set(NodeEditController.CONFIG_CONTENT_ENCODING, NodeEditController.CONFIG_CONTENT_ENCODING_AUTO);
				config.set(NodeEditController.CONFIG_JS_ENCODING, NodeEditController.CONFIG_JS_ENCODING_AUTO);
				config.setConfigurationVersion(3);
			}
			// else node is up-to-date - nothing to do
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
			ImportReferencesController.doImport(rie, this, ImportReferencesController.IMPORT_CP, true, admin);
			return null;
		} else {
			return new ImportReferencesController(ureq, wControl, this, ImportReferencesController.IMPORT_CP, rie);
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

}