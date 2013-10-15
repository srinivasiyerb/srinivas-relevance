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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionExpression;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.bc.BCCourseNodeEditController;
import org.olat.course.nodes.bc.BCCourseNodeRunController;
import org.olat.course.nodes.bc.BCPeekviewController;
import org.olat.course.nodes.bc.BCPreviewController;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.repository.RepositoryEntry;

/**
 * Description:<br>
 * 
 * @author Felix Jost
 */
public class BCCourseNode extends GenericCourseNode {
	private static final String PACKAGE_BC = Util.getPackageName(BCCourseNodeRunController.class);
	private static final String TYPE = "bc";
	/**
	 * Condition.getCondition() == null means no precondition, always accessible
	 */
	private Condition preConditionUploaders, preConditionDownloaders;

	/**
	 * Constructor for a course building block of type briefcase (folder)
	 */
	public BCCourseNode() {
		super(TYPE);
		preConditionUploaders = getPreConditionUploaders();
		preConditionUploaders.setEasyModeCoachesAndAdmins(true);
		preConditionUploaders.setConditionExpression(preConditionUploaders.getConditionFromEasyModeConfiguration());
		preConditionUploaders.setExpertMode(false);

	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final UserCourseEnvironment euce) {
		final BCCourseNodeEditController childTabCntrllr = new BCCourseNodeEditController(this, course, ureq, wControl, euce);
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
		final BCCourseNodeRunController bcCtrl = new BCCourseNodeRunController(ne, userCourseEnv.getCourseEnvironment(), ureq, wControl);
		if (StringHelper.containsNonWhitespace(nodecmd)) {
			bcCtrl.activate(ureq, nodecmd);
		}
		final Controller titledCtrl = TitledWrapperHelper.getWrapper(ureq, wControl, bcCtrl, this, "o_bc_icon");
		return new NodeRunConstructionResult(titledCtrl);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPeekViewRunController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPeekViewRunController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		if (ne.isAtLeastOneAccessible()) {
			// Create a folder peekview controller that shows the latest two entries
			final String path = getFoldernodePathRelToFolderBase(userCourseEnv.getCourseEnvironment(), ne.getCourseNode());
			final OlatRootFolderImpl rootFolder = new OlatRootFolderImpl(path, null);
			final Controller peekViewController = new BCPeekviewController(ureq, wControl, rootFolder, ne.getCourseNode().getIdent(), 4);
			return peekViewController;
		} else {
			// use standard peekview
			return super.createPeekViewRunController(ureq, wControl, userCourseEnv, ne);
		}
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPreviewController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPreviewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		return new BCPreviewController(ureq, wControl, this, userCourseEnv.getCourseEnvironment(), ne);
	}

	/**
	 * @param courseEnv
	 * @param node
	 * @return the relative folder base path for this folder node
	 */
	public static String getFoldernodePathRelToFolderBase(final CourseEnvironment courseEnv, final CourseNode node) {
		return getFoldernodesPathRelToFolderBase(courseEnv) + "/" + node.getIdent();
	}

	/**
	 * @param courseEnv
	 * @return the relative folder base path for folder nodes
	 */
	public static String getFoldernodesPathRelToFolderBase(final CourseEnvironment courseEnv) {
		return courseEnv.getCourseBaseContainer().getRelPath() + "/foldernodes";
	}

	/**
	 * Get a named container of a node with the node title as its name.
	 * 
	 * @param node
	 * @param courseEnv
	 * @return
	 */
	public static OlatNamedContainerImpl getNodeFolderContainer(final BCCourseNode node, final CourseEnvironment courseEnv) {
		final String path = getFoldernodePathRelToFolderBase(courseEnv, node);
		final OlatRootFolderImpl rootFolder = new OlatRootFolderImpl(path, null);
		final OlatNamedContainerImpl namedFolder = new OlatNamedContainerImpl(node.getShortTitle(), rootFolder);
		return namedFolder;
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#exportNode(java.io.File, org.olat.course.ICourse)
	 */
	@Override
	public void exportNode(final File exportDirectory, final ICourse course) {
		// this is the node folder, a folder with the node's ID, so we can just copy
		// the contents over to the export folder
		final File fFolderNodeData = new File(FolderConfig.getCanonicalRoot() + getFoldernodePathRelToFolderBase(course.getCourseEnvironment(), this));
		final File fNodeExportDir = new File(exportDirectory, this.getIdent());
		fNodeExportDir.mkdirs();
		FileUtils.copyDirContentsToDir(fFolderNodeData, fNodeExportDir, false, "export course node");
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#importNode(java.io.File, org.olat.course.ICourse, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller importNode(final File importDirectory, final ICourse course, final boolean unattendedImport, final UserRequest ureq, final WindowControl wControl) {
		// the export has copies the files under the node's ID
		final File fFolderNodeData = new File(importDirectory, this.getIdent());
		// the whole folder can be moved back to the root direcotry of foldernodes
		// of this course
		final File fFolderNodeDir = new File(FolderConfig.getCanonicalRoot() + getFoldernodePathRelToFolderBase(course.getCourseEnvironment(), this));
		fFolderNodeDir.mkdirs();
		FileUtils.copyDirContentsToDir(fFolderNodeData, fFolderNodeDir, true, "import course node");
		return null;
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#calcAccessAndVisibility(org.olat.course.condition.interpreter.ConditionInterpreter,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	protected void calcAccessAndVisibility(final ConditionInterpreter ci, final NodeEvaluation nodeEval) {

		final boolean uploadability = (getPreConditionUploaders().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionUploaders()));
		nodeEval.putAccessStatus("upload", uploadability);
		final boolean downloadability = (getPreConditionDownloaders().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionDownloaders()));
		nodeEval.putAccessStatus("download", downloadability);

		final boolean visible = (getPreConditionVisibility().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionVisibility()));
		nodeEval.setVisible(visible);
	}

	/**
	 * @return Returns the preConditionDownloaders.
	 */
	public Condition getPreConditionDownloaders() {
		if (preConditionDownloaders == null) {
			preConditionDownloaders = new Condition();
		}
		preConditionDownloaders.setConditionId("downloaders");
		return preConditionDownloaders;
	}

	/**
	 * @param preConditionDownloaders The preConditionDownloaders to set.
	 */
	public void setPreConditionDownloaders(Condition preConditionDownloaders) {
		if (preConditionDownloaders == null) {
			preConditionDownloaders = getPreConditionDownloaders();
		}
		this.preConditionDownloaders = preConditionDownloaders;
		preConditionDownloaders.setConditionId("downloaders");
	}

	/**
	 * @return Returns the preConditionUploaders.
	 */
	public Condition getPreConditionUploaders() {
		if (preConditionUploaders == null) {
			preConditionUploaders = new Condition();
		}
		preConditionUploaders.setConditionId("uploaders");
		return preConditionUploaders;
	}

	/**
	 * @param preConditionUploaders The preConditionUploaders to set.
	 */
	public void setPreConditionUploaders(Condition preConditionUploaders) {
		if (preConditionUploaders == null) {
			preConditionUploaders = getPreConditionUploaders();
		}
		preConditionUploaders.setConditionId("uploaders");
		this.preConditionUploaders = preConditionUploaders;
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

		return StatusDescription.NOERROR;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public StatusDescription[] isConfigValid(final CourseEditorEnv cev) {
		// only here we know which translator to take for translating condition error messages
		oneClickStatusCache = null;
		final String translatorStr = Util.getPackageName(BCCourseNodeEditController.class);
		final List statusDescs = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(statusDescs);
		return oneClickStatusCache;
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
	 * @see org.olat.course.nodes.CourseNode#informOnDelete(org.olat.core.gui.UserRequest, org.olat.course.ICourse)
	 */
	@Override
	public String informOnDelete(final Locale locale, final ICourse course) {
		return new PackageTranslator(PACKAGE_BC, locale).translate("warn.folderdelete");
	}

	/**
	 * Delete the folder if node is deleted.
	 * 
	 * @see org.olat.course.nodes.CourseNode#cleanupOnDelete(org.olat.course.ICourse)
	 */
	@Override
	public void cleanupOnDelete(final ICourse course) {
		// mark the subscription to this node as deleted
		final SubscriptionContext folderSubContext = CourseModule.createTechnicalSubscriptionContext(course.getCourseEnvironment(), this);
		NotificationsManager.getInstance().delete(folderSubContext);
		// delete filesystem
		final File fFolderRoot = new File(FolderConfig.getCanonicalRoot() + getFoldernodePathRelToFolderBase(course.getCourseEnvironment(), this));
		if (fFolderRoot.exists()) {
			FileUtils.deleteDirsAndFiles(fFolderRoot, true, true);
		}
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
		String coS = getPreConditionDownloaders().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			final ConditionExpression ce = new ConditionExpression(getPreConditionDownloaders().getConditionId());
			ce.setExpressionString(getPreConditionDownloaders().getConditionExpression());
			retVal.add(ce);
		}
		//
		coS = getPreConditionUploaders().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			final ConditionExpression ce = new ConditionExpression(getPreConditionUploaders().getConditionId());
			ce.setExpressionString(getPreConditionUploaders().getConditionExpression());
			retVal.add(ce);
		}
		//
		return retVal;
	}

}
