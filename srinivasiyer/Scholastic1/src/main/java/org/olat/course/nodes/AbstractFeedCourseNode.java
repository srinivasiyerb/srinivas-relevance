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
import java.util.Date;
import java.util.Locale;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.Formatter;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.course.ICourse;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.repository.ImportReferencesController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.webFeed.managers.FeedManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;
import org.olat.repository.RepositoryManager;

/**
 * The podcast course node.
 * <P>
 * Initial Date: Mar 30, 2009 <br>
 * 
 * @author gwassmann
 */
public abstract class AbstractFeedCourseNode extends GenericCourseNode {
	public static final String CONFIG_KEY_REPOSITORY_SOFTKEY = "reporef";
	protected ModuleConfiguration config;
	protected Condition preConditionReader, preConditionPoster, preConditionModerator;

	/**
	 * @param type
	 */
	public AbstractFeedCourseNode(final String type) {
		super(type);
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#updateModuleConfigDefaults(boolean)
	 */
	@Override
	public void updateModuleConfigDefaults(final boolean isNewNode) {
		this.config = getModuleConfiguration();
		if (isNewNode) {
			// No startpage
			config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, false);
			config.setConfigurationVersion(1);
			// restrict moderator access to course admins and owners
			preConditionModerator = getPreConditionModerator();
			preConditionModerator.setEasyModeCoachesAndAdmins(true);
			preConditionModerator.setConditionExpression(preConditionModerator.getConditionFromEasyModeConfiguration());
			preConditionModerator.setExpertMode(false);
			// restrict poster access to course admins and owners
			preConditionPoster = getPreConditionPoster();
			preConditionPoster.setEasyModeCoachesAndAdmins(true);
			preConditionPoster.setConditionExpression(preConditionPoster.getConditionFromEasyModeConfiguration());
			preConditionPoster.setExpertMode(false);
		}
	}

	/**
	 * @see org.olat.course.nodes.AbstractAccessableCourseNode#createEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.ICourse, org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public abstract TabbableController createEditController(UserRequest ureq, WindowControl wControl, ICourse course, UserCourseEnvironment euce);

	/**
	 * @see org.olat.course.nodes.AbstractAccessableCourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation, java.lang.String)
	 */
	@Override
	public abstract NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl control, UserCourseEnvironment userCourseEnv,
			NodeEvaluation ne, String nodecmd);

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#isConfigValid(org.olat.course.editor.CourseEditorEnv)
	 */
	@Override
	public abstract StatusDescription[] isConfigValid(CourseEditorEnv cev);

	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		this.config = getModuleConfiguration();
		final String repoSoftkey = (String) config.get(CONFIG_KEY_REPOSITORY_SOFTKEY);
		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry entry = rm.lookupRepositoryEntryBySoftkey(repoSoftkey, false);
		return entry;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	@Override
	public abstract StatusDescription isConfigValid();

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	@Override
	public boolean needsReferenceToARepositoryEntry() {
		return true;
	}

	/**
	 * @return Returns the preConditionModerator.
	 */
	public Condition getPreConditionModerator() {
		if (preConditionModerator == null) {
			preConditionModerator = new Condition();
		}
		preConditionModerator.setConditionId("moderator");
		return preConditionModerator;
	}

	/**
	 * @param preConditionModerator The preConditionModerator to set.
	 */
	public void setPreConditionModerator(Condition preConditionModerator) {
		if (preConditionModerator == null) {
			preConditionModerator = getPreConditionModerator();
		}
		preConditionModerator.setConditionId("moderator");
		this.preConditionModerator = preConditionModerator;
	}

	/**
	 * @return Returns the preConditionPoster.
	 */
	public Condition getPreConditionPoster() {
		if (preConditionPoster == null) {
			preConditionPoster = new Condition();
		}
		preConditionPoster.setConditionId("poster");
		return preConditionPoster;
	}

	/**
	 * @param preConditionPoster The preConditionPoster to set.
	 */
	public void setPreConditionPoster(Condition preConditionPoster) {
		if (preConditionPoster == null) {
			preConditionPoster = getPreConditionPoster();
		}
		preConditionPoster.setConditionId("poster");
		this.preConditionPoster = preConditionPoster;
	}

	/**
	 * @return Returns the preConditionReader.
	 */
	public Condition getPreConditionReader() {
		if (preConditionReader == null) {
			preConditionReader = new Condition();
		}
		preConditionReader.setConditionId("reader");
		return preConditionReader;
	}

	/**
	 * @param preConditionReader The preConditionReader to set.
	 */
	public void setPreConditionReader(Condition preConditionReader) {
		if (preConditionReader == null) {
			preConditionReader = getPreConditionReader();
		}
		preConditionReader.setConditionId("reader");
		this.preConditionReader = preConditionReader;
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#calcAccessAndVisibility(org.olat.course.condition.interpreter.ConditionInterpreter,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	protected void calcAccessAndVisibility(final ConditionInterpreter ci, final NodeEvaluation nodeEval) {
		// evaluate the preconditions
		final boolean reader = (getPreConditionReader().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionReader()));
		nodeEval.putAccessStatus("reader", reader);
		final boolean poster = (getPreConditionPoster().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionPoster()));
		nodeEval.putAccessStatus("poster", poster);
		final boolean moderator = (getPreConditionModerator().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionModerator()));
		nodeEval.putAccessStatus("moderator", moderator);

		final boolean visible = (getPreConditionVisibility().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionVisibility()));
		nodeEval.setVisible(visible);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#exportNode(java.io.File, org.olat.course.ICourse)
	 */
	@Override
	public void exportNode(final File exportDirectory, final ICourse course) {
		final RepositoryEntry re = getReferencedRepositoryEntry();
		if (re == null) { return; }
		// build current export ZIP for feed learning resource
		FeedManager.getInstance().getFeedArchive(re.getOlatResource());
		// trigger resource file export
		final File fExportDirectory = new File(exportDirectory, getIdent());
		fExportDirectory.mkdirs();
		final RepositoryEntryImportExport reie = new RepositoryEntryImportExport(re, fExportDirectory);
		reie.exportDoExport();
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#importNode(java.io.File, org.olat.course.ICourse, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	public Controller importNode(final File importDirectory, final ICourse course, final boolean unattendedImport, final UserRequest ureq, final WindowControl wControl,
			final int importType) {
		final File importSubdir = new File(importDirectory, getIdent());
		final RepositoryEntryImportExport rie = new RepositoryEntryImportExport(importSubdir);
		if (!rie.anyExportedPropertiesAvailable()) { return null; }

		// do import referenced repository entries
		if (unattendedImport) {
			final Identity admin = BaseSecurityManager.getInstance().findIdentityByName("administrator");
			ImportReferencesController.doImport(rie, this, importType, true, admin);
			return null;
		} else {
			return new ImportReferencesController(ureq, wControl, this, importType, rie);
		}
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#archiveNodeData(java.util.Locale, org.olat.course.ICourse, java.io.File, java.lang.String)
	 */
	public void archiveNodeData(final Locale locale, final ICourse course, final File exportDirectory, final String charset, final String type) {
		final VFSContainer exportContainer = new LocalFolderImpl(exportDirectory);
		VFSContainer exportDir = (VFSContainer) exportContainer.resolve(type);
		if (exportDir == null) {
			exportDir = exportContainer.createChildContainer(type);
		}
		final String exportDirName = getShortTitle() + "_" + Formatter.formatDatetimeFilesystemSave(new Date(System.currentTimeMillis()));
		final VFSContainer destination = exportDir.createChildContainer(exportDirName);
		final String repoRef = (String) getModuleConfiguration().get(CONFIG_KEY_REPOSITORY_SOFTKEY);
		if (repoRef != null) {
			final OLATResourceable ores = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(repoRef, true).getOlatResource();

			final VFSContainer container = FeedManager.getInstance().getFeedContainer(ores);
			if (container != null) {
				final VFSLeaf archive = FeedManager.getInstance().getFeedArchive(ores);
				destination.copyFrom(archive);
			}
			// FIXME:FG:6.3 Archive user comments as soon as implemented.
		}
	}
}
