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
package org.olat.course.nodes.iq;

import java.io.File;
import java.util.Locale;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.IQSELFCourseNode;
import org.olat.course.nodes.IQSURVCourseNode;
import org.olat.course.nodes.IQTESTCourseNode;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.ims.qti.QTIResultDetailsController;
import org.olat.ims.qti.export.QTIExportFormatter;
import org.olat.ims.qti.export.QTIExportFormatterCSVType1;
import org.olat.ims.qti.export.QTIExportManager;
import org.olat.ims.qti.fileresource.SurveyFileResource;
import org.olat.ims.qti.fileresource.TestFileResource;
import org.olat.modules.iq.IQSecurityCallback;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

/**
 * Description:<br>
 * TODO: patrickb Class Description for IQControllerCreatorOlat
 * <P>
 * Initial Date: 18.06.2010 <br>
 * 
 * @author patrickb
 */
public class IQControllerCreatorOlat implements IQControllerCreator {

	/**
	 * The iq test edit screen in the course editor.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param course
	 * @param courseNode
	 * @param groupMgr
	 * @param euce
	 * @return
	 */
	@Override
	public TabbableController createIQTestEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final IQTESTCourseNode courseNode,
			final CourseGroupManager groupMgr, final UserCourseEnvironment euce) {
		return new IQEditController(ureq, wControl, course, courseNode, groupMgr, euce);
	}

	/**
	 * The iq test edit screen in the course editor.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param course
	 * @param courseNode
	 * @param groupMgr
	 * @param euce
	 * @return
	 */
	@Override
	public TabbableController createIQSelftestEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course,
			final IQSELFCourseNode courseNode, final CourseGroupManager groupMgr, final UserCourseEnvironment euce) {
		return new IQEditController(ureq, wControl, course, courseNode, groupMgr, euce);
	}

	/**
	 * The iq test edit screen in the course editor.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param course
	 * @param courseNode
	 * @param groupMgr
	 * @param euce
	 * @return
	 */
	@Override
	public TabbableController createIQSurveyEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final IQSURVCourseNode courseNode,
			final CourseGroupManager groupMgr, final UserCourseEnvironment euce) {
		return new IQEditController(ureq, wControl, course, courseNode, groupMgr, euce);
	}

	/**
	 * @param ureq
	 * @param wControl
	 * @param userCourseEnv
	 * @param ne
	 * @param courseNode
	 * @return
	 */
	@Override
	public Controller createIQTestRunController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne,
			final IQTESTCourseNode courseNode) {

		Controller controller = null;

		// Do not allow guests to start tests
		final Roles roles = ureq.getUserSession().getRoles();
		final Translator trans = Util.createPackageTranslator(IQTESTCourseNode.class, ureq.getLocale());
		if (roles.isGuestOnly()) {
			final String title = trans.translate("guestnoaccess.title");
			final String message = trans.translate("guestnoaccess.message");
			controller = MessageUIFactory.createInfoMessage(ureq, wControl, title, message);
		} else {
			final AssessmentManager am = userCourseEnv.getCourseEnvironment().getAssessmentManager();
			final IQSecurityCallback sec = new CourseIQSecurityCallback(courseNode, am, ureq.getIdentity());
			final RepositoryEntry repositoryEntry = ne.getCourseNode().getReferencedRepositoryEntry();
			final OLATResourceable ores = repositoryEntry.getOlatResource();
			final Long resId = ores.getResourceableId();
			final TestFileResource fr = new TestFileResource();
			fr.overrideResourceableId(resId);
			if (!CoordinatorManager.getInstance().getCoordinator().getLocker().isLocked(fr, null)) {
				// QTI1
				controller = new IQRunController(userCourseEnv, courseNode.getModuleConfiguration(), sec, ureq, wControl, courseNode);
			} else {
				final String title = trans.translate("editor.lock.title");
				final String message = trans.translate("editor.lock.message");
				controller = MessageUIFactory.createInfoMessage(ureq, wControl, title, message);
			}
		}

		return controller;
	}

	@Override
	public Controller createIQTestPreviewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv,
			final NodeEvaluation ne, final IQTESTCourseNode courseNode) {
		return new IQPreviewController(ureq, wControl, userCourseEnv, courseNode, ne);
	}

	@Override
	public Controller createIQSelftestRunController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv,
			final NodeEvaluation ne, final IQSELFCourseNode courseNode) {
		final AssessmentManager am = userCourseEnv.getCourseEnvironment().getAssessmentManager();
		final IQSecurityCallback sec = new CourseIQSecurityCallback(courseNode, am, ureq.getIdentity());
		return new IQRunController(userCourseEnv, courseNode.getModuleConfiguration(), sec, ureq, wControl, courseNode);
	}

	@Override
	public Controller createIQSurveyRunController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv,
			final NodeEvaluation ne, final IQSURVCourseNode courseNode) {
		Controller controller = null;

		// Do not allow guests to start questionnaires
		final Roles roles = ureq.getUserSession().getRoles();
		if (roles.isGuestOnly()) {
			final Translator trans = Util.createPackageTranslator(IQSURVCourseNode.class, ureq.getLocale());
			final String title = trans.translate("guestnoaccess.title");
			final String message = trans.translate("guestnoaccess.message");
			controller = MessageUIFactory.createInfoMessage(ureq, wControl, title, message);
		} else {
			final RepositoryEntry repositoryEntry = ne.getCourseNode().getReferencedRepositoryEntry();
			final OLATResourceable ores = repositoryEntry.getOlatResource();
			final Long resId = ores.getResourceableId();
			final SurveyFileResource fr = new SurveyFileResource();
			fr.overrideResourceableId(resId);
			if (!CoordinatorManager.getInstance().getCoordinator().getLocker().isLocked(fr, null)) {
				final AssessmentManager am = userCourseEnv.getCourseEnvironment().getAssessmentManager();
				final IQSecurityCallback sec = new CourseIQSecurityCallback(courseNode, am, ureq.getIdentity());
				controller = new IQRunController(userCourseEnv, courseNode.getModuleConfiguration(), sec, ureq, wControl, courseNode);
			} else {
				final Translator trans = Util.createPackageTranslator(IQSURVCourseNode.class, ureq.getLocale());
				final String title = trans.translate("editor.lock.title");
				final String message = trans.translate("editor.lock.message");
				controller = MessageUIFactory.createInfoMessage(ureq, wControl, title, message);
			}
		}
		return controller;

	}

	@Override
	public Controller createIQTestDetailsEditController(final Long courseResourceableId, final String ident, final Identity identity,
			final RepositoryEntry referencedRepositoryEntry, final String qmdEntryTypeAssess, final UserRequest ureq, final WindowControl wControl) {
		return new QTIResultDetailsController(courseResourceableId, ident, identity, referencedRepositoryEntry, qmdEntryTypeAssess, ureq, wControl);
	}

	@Override
	public boolean archiveIQTestCourseNode(final Locale locale, final String repositorySoftkey, final Long courseResourceableId, final String shortTitle,
			final String ident, final File exportDirectory, final String charset) {
		final QTIExportManager qem = QTIExportManager.getInstance();
		final Long repKey = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(repositorySoftkey, true).getKey();
		final QTIExportFormatter qef = new QTIExportFormatterCSVType1(locale, "\t", "\"", "\\", "\r\n", false);
		final boolean retVal = qem.selectAndExportResults(qef, courseResourceableId, shortTitle, ident, repKey, exportDirectory, charset, ".xls");
		return retVal;
	}

}
