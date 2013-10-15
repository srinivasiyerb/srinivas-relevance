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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Formatter;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.course.ICourse;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.wiki.WikiEditController;
import org.olat.course.nodes.wiki.WikiRunController;
import org.olat.course.repository.ImportReferencesController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.wiki.WikiManager;
import org.olat.modules.wiki.WikiToZipUtils;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;
import org.olat.repository.RepositoryManager;

/**
 * Description: <br>
 * 
 * @author Felix Jost
 */
public class WikiCourseNode extends AbstractAccessableCourseNode {

	public static final String TYPE = "wiki";
	private Condition preConditionEdit;

	/**
	 * Default constructor for course node of type single page
	 */
	public WikiCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	@Override
	public void updateModuleConfigDefaults(final boolean isNewNode) {
		final ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode) {
			// use defaults for new course building blocks
			config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, false);
			config.setConfigurationVersion(1);
		}
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final UserCourseEnvironment euce) {
		final WikiEditController childTabCntrllr = new WikiEditController(getModuleConfiguration(), ureq, wControl, this, course, euce);
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
		if (ne.isCapabilityAccessible("access")) {
			final WikiRunController wikiController = new WikiRunController(wControl, ureq, this, userCourseEnv.getCourseEnvironment(), ne);
			return new NodeRunConstructionResult(wikiController);
		}
		final Controller controller = MessageUIFactory.createInfoMessage(ureq, wControl, null, this.getNoAccessExplanation());
		return new NodeRunConstructionResult(controller, null, null, null);
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
		final boolean isValid = WikiEditController.isModuleConfigValid(getModuleConfiguration());
		if (!isValid) {
			final String shortKey = "error.noreference.short";
			final String longKey = "error.noreference.long";
			final String[] params = new String[] { this.getShortTitle() };
			final String translPackage = Util.getPackageName(WikiEditController.class);
			sd = new StatusDescription(ValidationStatus.ERROR, shortKey, longKey, params, translPackage);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(WikiEditController.PANE_TAB_WIKICONFIG);
		}
		return sd;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public StatusDescription[] isConfigValid(final CourseEditorEnv cev) {
		oneClickStatusCache = null;
		// only here we know which translator to take for translating condition error messages
		final String translatorStr = Util.getPackageName(WikiEditController.class);
		final List sds = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		// "false" because we do not want to be strict, but just indicate whether
		// the reference still exists or not
		final RepositoryEntry entry = WikiEditController.getWikiReference(getModuleConfiguration(), false);
		return entry;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	@Override
	public boolean needsReferenceToARepositoryEntry() {
		// wiki is a repo entry
		return true;
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#exportNode(java.io.File, org.olat.course.ICourse)
	 */
	@Override
	public void exportNode(final File exportDirectory, final ICourse course) {
		final RepositoryEntry re = WikiEditController.getWikiReference(getModuleConfiguration(), false);
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
			ImportReferencesController.doImport(rie, this, ImportReferencesController.IMPORT_WIKI, true, admin);
			return null;
		} else {
			return new ImportReferencesController(ureq, wControl, this, ImportReferencesController.IMPORT_WIKI, rie);
		}
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#archiveNodeData(java.util.Locale, org.olat.course.ICourse, java.io.File, java.lang.String)
	 */
	@Override
	public boolean archiveNodeData(final Locale locale, final ICourse course, final File exportDirectory, final String charset) {
		final String repoRef = (String) this.getModuleConfiguration().get("reporef");
		final OLATResourceable ores = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(repoRef, true).getOlatResource();

		if (WikiManager.getInstance().getOrLoadWiki(ores).getAllPagesWithContent().size() > 0) {
			// OK, there is somthing to archive
			final VFSContainer exportContainer = new LocalFolderImpl(exportDirectory);
			VFSContainer wikiExportContainer = (VFSContainer) exportContainer.resolve(WikiManager.WIKI_RESOURCE_FOLDER_NAME);
			if (wikiExportContainer == null) {
				wikiExportContainer = exportContainer.createChildContainer(WikiManager.WIKI_RESOURCE_FOLDER_NAME);
			}
			final String exportDirName = getShortTitle() + "_" + Formatter.formatDatetimeFilesystemSave(new Date(System.currentTimeMillis()));
			final VFSContainer destination = wikiExportContainer.createChildContainer(exportDirName);
			if (destination == null) {
				Tracing.logError("archiveNodeData: Could not create destination directory: wikiExportContainer=" + wikiExportContainer + ", exportDirName="
						+ exportDirName, getClass());
			}

			final VFSContainer container = WikiManager.getInstance().getWikiContainer(ores, WikiManager.WIKI_RESOURCE_FOLDER_NAME);
			if (container != null) { // the container could be null if the wiki is an old empty one - so nothing to archive
				final VFSContainer parent = container.getParentContainer();
				final VFSLeaf wikiZip = WikiToZipUtils.getWikiAsZip(parent);
				destination.copyFrom(wikiZip);
			}
			return true;
		}
		// empty wiki, no need to archive
		return false;
	}

	/**
	 * @return
	 */
	public Condition getPreConditionEdit() {
		if (preConditionEdit == null) {
			preConditionEdit = new Condition();
		}
		preConditionEdit.setConditionId("editarticle");
		return preConditionEdit;
	}

	/**
	 * @param preConditionEdit
	 */
	public void setPreConditionEdit(Condition preConditionEdit) {
		if (preConditionEdit == null) {
			preConditionEdit = getPreConditionEdit();
		}
		preConditionEdit.setConditionId("editarticle");
		this.preConditionEdit = preConditionEdit;
	}

	/**
	 * The access condition for wiki is composed of 2 dimensions: readonly (or access) and read&write (or editarticle). <br/>
	 * If the access is readonly, the read&write dimension is no more relevant.<br/>
	 * If the access is not readonly, read&write condition should be evaluated. <br/>
	 * 
	 * @see org.olat.course.nodes.GenericCourseNode#calcAccessAndVisibility(org.olat.course.condition.interpreter.ConditionInterpreter,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	protected void calcAccessAndVisibility(final ConditionInterpreter ci, final NodeEvaluation nodeEval) {
		super.calcAccessAndVisibility(ci, nodeEval);

		final boolean editor = (getPreConditionEdit().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionEdit()));
		nodeEval.putAccessStatus("editarticle", editor);
	}

	@Override
	public void cleanupOnDelete(final ICourse course) {
		// mark the subscription to this node as deleted
		final SubscriptionContext subsContext = WikiManager.createTechnicalSubscriptionContextForCourse(course.getCourseEnvironment(), this);
		NotificationsManager.getInstance().delete(subsContext);

	}

}