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

package de.bps.onyx.plugin.course.nodes.iq;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.commons.file.filechooser.FileChooseCreateEditController;
import org.olat.commons.file.filechooser.LinkChooseCreateEditController;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsPreviewController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSConstants;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSStatus;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.condition.Condition;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.NodeEditController;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.AbstractAccessableCourseNode;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNodeFactory;
import org.olat.course.nodes.IQSELFCourseNode;
import org.olat.course.nodes.IQSURVCourseNode;
import org.olat.course.nodes.IQTESTCourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseInternalLinkTreeModel;
import org.olat.fileresource.FileResourceManager;
import org.olat.ims.qti.QTIResult;
import org.olat.ims.qti.QTIResultManager;
import org.olat.ims.qti.fileresource.SurveyFileResource;
import org.olat.ims.qti.fileresource.TestFileResource;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.ims.qti.process.QTIHelper;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.iq.IQManager;
import org.olat.modules.iq.IQPreviewSecurityCallback;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.controllers.ReferencableEntriesSearchController;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.resource.OLATResource;

import de.bps.onyx.plugin.OnyxModule;
import de.bps.webservices.clients.onyxreporter.OnyxReporterWebserviceManager;
import de.bps.webservices.clients.onyxreporter.OnyxReporterWebserviceManagerFactory;

/**
 * Description:<BR/>
 * Edit controller for the qti test, selftest and survey course node
 * <P/>
 * Initial Date: Oct 13, 2004
 * 
 * @author Felix Jost
 */
public class IQEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	public final String PANE_TAB_IQCONFIG_XXX;
	public static final String PANE_TAB_IQCONFIG_SURV = "pane.tab.iqconfig.surv";
	public static final String PANE_TAB_IQCONFIG_SELF = "pane.tab.iqconfig.self";
	public static final String PANE_TAB_IQCONFIG_TEST = "pane.tab.iqconfig.test";
	public static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	private static final String VC_CHOSENTEST = "chosentest";
	private static final String ACTION_CORRECT = "correcttest";
	/** configuration key: repository sof key reference to qti file */
	public static final String CONFIG_KEY_REPOSITORY_SOFTKEY = "repoSoftkey";
	/** configuration key: the disclaimer text */
	public static final String CONFIG_KEY_DISCLAIMER = "disc";
	/** configuration key: enable menu switch */
	public static final String CONFIG_KEY_ENABLEMENU = "enableMenu";
	/** configuration key: display menu switch */
	public static final String CONFIG_KEY_DISPLAYMENU = "displayMenu";
	/** configuration key: all questions, section titles only */
	public static final String CONFIG_KEY_RENDERMENUOPTION = "renderMenu";
	/** configuration key: enable score progress switch */
	public static final String CONFIG_KEY_SCOREPROGRESS = "displayScoreProgress";
	/** configuration key: enable cancel switch */
	public static final String CONFIG_KEY_ENABLECANCEL = "enableCancel";
	/** configuration key: enable suspend switch */
	public static final String CONFIG_KEY_ENABLESUSPEND = "enableSuspend";
	/** configuration key: enable question progress switch */
	public static final String CONFIG_KEY_QUESTIONPROGRESS = "displayQuestionProgss";
	/** configuration key: enable question progress switch */
	public static final String CONFIG_KEY_QUESTIONTITLE = "displayQuestionTitle";
	/** configuration key: question sequence: item or selection */
	public static final String CONFIG_KEY_SEQUENCE = "sequence";
	/** configuration key: mode */
	public static final String CONFIG_KEY_TYPE = "mode";
	/** configuration key: show summary: compact or detailed */
	public static final String CONFIG_KEY_SUMMARY = "summary";
	/** configuration key: max attempts */
	public static final String CONFIG_KEY_ATTEMPTS = "attempts";
	/** configuration key: minimal score */
	public static final String CONFIG_KEY_MINSCORE = "minscore";
	/** configuration key: maximal score */
	public static final String CONFIG_KEY_MAXSCORE = "maxscore";
	/** configuration key: cut value (socre > cut = passed) */
	public static final String CONFIG_KEY_CUTVALUE = "cutvalue";
	/** configuration key for the filename */
	public static final String CONFIG_KEY_FILE = "file";
	/** configuration key: should relative links like ../otherfolder/my.css be allowed? **/
	public static final String CONFIG_KEY_ALLOW_RELATIVE_LINKS = "allowRelativeLinks";
	/** configuration key: enable 'show score infos' on start page */
	public static final String CONFIG_KEY_ENABLESCOREINFO = "enableScoreInfo";

	public static final String CONFIG_KEY_DATE_DEPENDENT_RESULTS = "dateDependentResults";
	public static final String CONFIG_KEY_RESULTS_START_DATE = "resultsStartDate";
	public static final String CONFIG_KEY_RESULTS_END_DATE = "resultsEndDate";
	public static final String CONFIG_KEY_RESULT_ON_FINISH = "showResultsOnFinish";
	public static final String CONFIG_KEY_RESULT_ON_HOME_PAGE = "showResultsOnHomePage";

	public static final String CONFIG_KEY_TEMPLATE = "templateid";
	public static final String CONFIG_KEY_TYPE_QTI = "qtitype";
	public static final String CONFIG_VALUE_QTI2 = "qti2";
	public static final Object CONFIG_VALUE_QTI1 = "qti1";
	public static final String CONFIG_KEY_IS_SURVEY = "issurv";

	private final OLog log = Tracing.createLoggerFor(IQEditController.class);
	private final String[] paneKeys;

	private final ModuleConfiguration moduleConfiguration;
	private Panel main;
	private VelocityContainer myContent;

	private IQEditForm modConfigForm;
	private ReferencableEntriesSearchController searchController;
	private final ICourse course;
	private ConditionEditController accessibilityCondContr;
	private final AbstractAccessableCourseNode courseNode;
	private final String type;
	private final UserCourseEnvironment euce;
	private TabbedPane myTabbedPane;
	private FileChooseCreateEditController fccecontr;
	private Controller correctQTIcontroller;
	private Boolean allowRelativeLinks;
	private Link previewLink;
	private Link chooseTestButton;
	private Link changeTestButton;
	private IQEditReplaceWizard replaceWizard;
	private List<Identity> learners;
	private LayoutMain3ColsPreviewController previewLayoutCtr;
	private CloseableModalController cmc;
	private Link editTestButton;

	/**
	 * Constructor for the IMS QTI edit controller for a test course node
	 * 
	 * @param ureq The user request
	 * @param wControl The window controller
	 * @param course The course
	 * @param courseNode The test course node
	 * @param groupMgr
	 * @param euce
	 */
	public IQEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final IQTESTCourseNode courseNode,
			final CourseGroupManager groupMgr, final UserCourseEnvironment euce) {
		super(ureq, wControl);

		final Translator translator = Util.createPackageTranslator(org.olat.course.nodes.iq.IQEditController.class, getTranslator().getLocale(), getTranslator());
		setTranslator(translator);

		this.moduleConfiguration = courseNode.getModuleConfiguration();
		// o_clusterOk by guido: save to hold reference to course inside editor
		this.course = course;
		this.courseNode = courseNode;
		this.euce = euce;
		this.type = AssessmentInstance.QMD_ENTRY_TYPE_ASSESS;
		this.PANE_TAB_IQCONFIG_XXX = PANE_TAB_IQCONFIG_TEST;
		paneKeys = new String[] { PANE_TAB_IQCONFIG_XXX, PANE_TAB_ACCESSIBILITY };
		// put some default values
		if (moduleConfiguration.get(CONFIG_KEY_ENABLECANCEL) == null) {
			moduleConfiguration.set(CONFIG_KEY_ENABLECANCEL, Boolean.FALSE);
		}
		if (moduleConfiguration.get(CONFIG_KEY_ENABLESUSPEND) == null) {
			moduleConfiguration.set(CONFIG_KEY_ENABLESUSPEND, Boolean.FALSE);
		}
		if (moduleConfiguration.get(CONFIG_KEY_RENDERMENUOPTION) == null) {
			moduleConfiguration.set(CONFIG_KEY_RENDERMENUOPTION, Boolean.FALSE);
		}

		init(ureq, groupMgr, wControl);
		myContent.contextPut("repEntryTitle", translate("choosenfile.test"));
		myContent.contextPut("type", this.type);
	}

	/**
	 * Constructor for the IMS QTI edit controller for a self-test course node
	 * 
	 * @param ureq The user request
	 * @param wControl The window controller
	 * @param course The course
	 * @param courseNode The self course node
	 * @param groupMgr
	 * @param euce
	 */
	public IQEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final IQSELFCourseNode courseNode,
			final CourseGroupManager groupMgr, final UserCourseEnvironment euce) {
		super(ureq, wControl);

		final Translator translator = Util.createPackageTranslator(org.olat.course.nodes.iq.IQEditController.class, getTranslator().getLocale(), getTranslator());
		setTranslator(translator);

		this.moduleConfiguration = courseNode.getModuleConfiguration();
		this.course = course;
		this.courseNode = courseNode;
		this.euce = euce;
		this.type = AssessmentInstance.QMD_ENTRY_TYPE_SELF;
		this.PANE_TAB_IQCONFIG_XXX = PANE_TAB_IQCONFIG_SELF;
		paneKeys = new String[] { PANE_TAB_IQCONFIG_XXX, PANE_TAB_ACCESSIBILITY };
		// put some default values
		if (moduleConfiguration.get(CONFIG_KEY_ENABLECANCEL) == null) {
			moduleConfiguration.set(CONFIG_KEY_ENABLECANCEL, Boolean.TRUE);
		}
		if (moduleConfiguration.get(CONFIG_KEY_ENABLESUSPEND) == null) {
			moduleConfiguration.set(CONFIG_KEY_ENABLESUSPEND, Boolean.TRUE);
		}

		init(ureq, groupMgr, wControl);
		myContent.contextPut("repEntryTitle", translate("choosenfile.self"));
		myContent.contextPut("type", this.type);
	}

	/**
	 * Constructor for the IMS QTI edit controller for a survey course node
	 * 
	 * @param ureq The user request
	 * @param wControl The window controller
	 * @param course The course
	 * @param courseNode The survey course node
	 * @param groupMgr
	 * @param euce
	 */
	public IQEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final IQSURVCourseNode courseNode,
			final CourseGroupManager groupMgr, final UserCourseEnvironment euce) {
		super(ureq, wControl);

		final Translator translator = Util.createPackageTranslator(org.olat.course.nodes.iq.IQEditController.class, getTranslator().getLocale(), getTranslator());
		setTranslator(translator);

		this.moduleConfiguration = courseNode.getModuleConfiguration();
		this.course = course;
		this.courseNode = courseNode;
		this.euce = euce;
		this.type = AssessmentInstance.QMD_ENTRY_TYPE_SURVEY;
		this.PANE_TAB_IQCONFIG_XXX = PANE_TAB_IQCONFIG_SURV;
		paneKeys = new String[] { PANE_TAB_IQCONFIG_XXX, PANE_TAB_ACCESSIBILITY };

		// put some default values
		if (moduleConfiguration.get(CONFIG_KEY_SCOREPROGRESS) == null) {
			moduleConfiguration.set(CONFIG_KEY_SCOREPROGRESS, Boolean.FALSE);
		}
		if (moduleConfiguration.getBooleanEntry(CONFIG_KEY_ALLOW_RELATIVE_LINKS) == null) {
			moduleConfiguration.setBooleanEntry(CONFIG_KEY_ALLOW_RELATIVE_LINKS, false);
		}

		init(ureq, groupMgr, wControl);
		myContent.contextPut("repEntryTitle", translate("choosenfile.surv"));
		myContent.contextPut("type", this.type);
		chooseTestButton.setCustomDisplayText(translate("command.createSurvey"));
	}

	private void init(final UserRequest ureq, final CourseGroupManager groupMgr, final WindowControl wControl) {
		main = new Panel("iqeditpanel");

		myContent = this.createVelocityContainer("edit");
		chooseTestButton = LinkFactory.createButtonSmall("command.chooseRepFile", myContent, this);
		changeTestButton = LinkFactory.createButtonSmall("command.changeRepFile", myContent, this);

		if (this.type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY)) {
			moduleConfiguration.set(CONFIG_KEY_IS_SURVEY, Boolean.TRUE);
		}

		// fetch repository entry
		RepositoryEntry re = null;
		final String repoSoftkey = (String) moduleConfiguration.get(CONFIG_KEY_REPOSITORY_SOFTKEY);
		if (repoSoftkey != null) {
			re = getIQReference(moduleConfiguration, false);
		}
		myContent.contextPut(VC_CHOSENTEST, re == null ? translate("no.file.chosen") : re.getDisplayname());

		// onyx tests are not editable
		// editTestButton = LinkFactory.createButtonSmall("command.editRepFile", myContent, this);

		if (re != null) {
			if (isEditable(ureq.getIdentity(), re)) {
				editTestButton = LinkFactory.createButtonSmall("command.editRepFile", myContent, this);
			}
			myContent.contextPut("dontRenderRepositoryButton", new Boolean(true));
			// Put values to velocity container

			final boolean isOnyx = OnyxModule.isOnyxTest(re.getOlatResource());
			myContent.contextPut("isOnyx", new Boolean(isOnyx));
			if (isOnyx) {
				myContent.contextPut("onyxDisplayName", re.getDisplayname());
				myContent.contextPut("showOutcomes", new Boolean(true));
				Map<String, String> outcomes = new HashMap<String, String>();
				try {
					final OnyxReporterWebserviceManager onyxReporter = OnyxReporterWebserviceManagerFactory.getInstance().fabricate("OnyxReporterWebserviceClient");
					if (onyxReporter != null) {
						outcomes = onyxReporter.getOutcomes((AssessableCourseNode) courseNode);
					} else {
						throw new UnsupportedOperationException("could not connect to onyx reporter");
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
				myContent.contextPut("outcomes", outcomes);
			} else {
				myContent.contextPut("showOutcomes", Boolean.FALSE);
				myContent.contextPut(CONFIG_KEY_MINSCORE, moduleConfiguration.get(CONFIG_KEY_MINSCORE));
				myContent.contextPut(CONFIG_KEY_MAXSCORE, moduleConfiguration.get(CONFIG_KEY_MAXSCORE));
				myContent.contextPut(CONFIG_KEY_CUTVALUE, moduleConfiguration.get(CONFIG_KEY_CUTVALUE));
			}

			previewLink = LinkFactory.createCustomLink("command.preview", "command.preview", re.getDisplayname(), Link.NONTRANSLATED, myContent, this);
			previewLink.setCustomEnabledLinkCSS("b_preview");
			previewLink.setTitle(getTranslator().translate("command.preview"));
		}

		// construct the config form after check for onyx
		modConfigForm = new IQEditForm(ureq, wControl, moduleConfiguration);
		modConfigForm.addControllerListener(this);
		myContent.put("iqeditform", modConfigForm.getInitialComponent());

		final String disclaimer = (String) moduleConfiguration.get(CONFIG_KEY_DISCLAIMER);
		// allowRelativeLinks = courseNode.getModuleConfiguration().getBooleanEntry(CONFIG_KEY_ALLOW_RELATIVE_LINKS);

		final String legend = translate("fieldset.chosecreateeditfile");
		final String target = "qti";
		allowRelativeLinks = moduleConfiguration.getBooleanEntry(CONFIG_KEY_ALLOW_RELATIVE_LINKS);
		if (allowRelativeLinks == null) {
			allowRelativeLinks = Boolean.FALSE;
		}
		fccecontr = new LinkChooseCreateEditController(ureq, wControl, disclaimer, allowRelativeLinks, course.getCourseFolderContainer(), target, legend,
				new CourseInternalLinkTreeModel(course.getEditorTreeModel()));
		this.listenTo(fccecontr);

		final Component fcContent = fccecontr.getInitialComponent();
		myContent.put("filechoosecreateedit", fcContent);

		final Condition accessCondition = courseNode.getPreConditionAccess();
		accessibilityCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, accessCondition, "accessabilityConditionForm",
				AssessmentHelper.getAssessableNodes(course.getEditorTreeModel(), courseNode), euce);
		this.listenTo(accessibilityCondContr);

		main.setContent(myContent);
		// not needed for tabbledController: setInitialComponent(main);
	}

	/**
	 * @param identity
	 * @param repository entry
	 * @return
	 */
	private boolean isEditable(final Identity identity, final RepositoryEntry re) {

		final boolean isOnyx = OnyxModule.isOnyxTest(re.getOlatResource());
		if (isOnyx) { return false; }

		return (BaseSecurityManager.getInstance().isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_ADMIN)
				|| RepositoryManager.getInstance().isOwnerOfRepositoryEntry(identity, re) || RepositoryManager.getInstance().isInstitutionalRessourceManagerFor(re,
				identity));
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == myContent) {
			if (event.getCommand().equals(ACTION_CORRECT)) {
				/*
				 * FIXME:pb:remove this elseif code, as the use case "correct" is started from details view only - check if the test is in use at the moment - check if
				 * already results exist - check if test is referenced from other courses
				 */
				final String repoSoftKey = (String) moduleConfiguration.get(CONFIG_KEY_REPOSITORY_SOFTKEY);
				final RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(repoSoftKey, false);
				if (re == null) {
					// not found
				} else {
					final RepositoryHandler typeToEdit = RepositoryHandlerFactory.getInstance().getRepositoryHandler(re);
					final OLATResourceable ores = re.getOlatResource();
					correctQTIcontroller = typeToEdit.createEditorController(ores, ureq, this.getWindowControl());
					this.getWindowControl().pushToMainArea(correctQTIcontroller.getInitialComponent());
					this.listenTo(correctQTIcontroller);
				}
			}
		} else if (source == previewLink) {
			// handle preview
			if (previewLayoutCtr != null) {
				previewLayoutCtr.dispose();
			}
			final Controller previewController = IQManager.getInstance().createIQDisplayController(moduleConfiguration, new IQPreviewSecurityCallback(), ureq,
					getWindowControl(), course.getResourceableId().longValue(), courseNode.getIdent());
			previewLayoutCtr = new LayoutMain3ColsPreviewController(ureq, getWindowControl(), null, null, previewController.getInitialComponent(), null);
			previewLayoutCtr.addDisposableChildController(previewController);
			previewLayoutCtr.activate();

		} else if (source == chooseTestButton) {// initiate search controller
			if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY)) {
				searchController = new ReferencableEntriesSearchController(getWindowControl(), ureq, SurveyFileResource.TYPE_NAME, translate("command.chooseSurvey"));

				// plus [ Inhalt Ticket#: 2008112610000246 ]
			} else if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SELF)) { // test and selftest use same repository resource type
				searchController = new ReferencableEntriesSearchController(getWindowControl(), ureq, TestFileResource.TYPE_NAME, translate("command.chooseTest"));
			} else {
				final String[] types = new String[] { TestFileResource.TYPE_NAME };
				searchController = new ReferencableEntriesSearchController(getWindowControl(), ureq, types, translate("command.chooseTest"));
			}

			this.listenTo(searchController);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), searchController.getInitialComponent());
			cmc.activate();
		} else if (source == changeTestButton) {// change associated test
			if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SELF)) {// selftest
				final String[] types = new String[] { TestFileResource.TYPE_NAME };
				searchController = new ReferencableEntriesSearchController(getWindowControl(), ureq, types, translate("command.chooseTest"));
				cmc = new CloseableModalController(getWindowControl(), translate("close"), searchController.getInitialComponent());
				this.listenTo(searchController);
			} else if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS) | type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY)) {// test, survey
				String[] types;
				if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS)) {// test
					types = new String[] { TestFileResource.TYPE_NAME };
				} else {// survey
					types = new String[] { SurveyFileResource.TYPE_NAME };
				}
				final RepositoryEntry re = courseNode.getReferencedRepositoryEntry();
				// look if there are PASSED entries in changelog
				// if yes create archive of results and all users can be notified about the changed test configuration
				final String repositorySoftKey = (String) courseNode.getModuleConfiguration().get(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
				final Long repKey = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(repositorySoftKey, true).getKey();

				if (moduleConfiguration.get(CONFIG_KEY_TYPE_QTI) == null) {
					updateQtiType(re);
				}

				int onyxSuccess = 0;
				if (moduleConfiguration.get(CONFIG_KEY_TYPE_QTI) != null && moduleConfiguration.get(CONFIG_KEY_TYPE_QTI).equals(CONFIG_VALUE_QTI2)) {
					if (courseNode.getClass().equals(IQSURVCourseNode.class)) {
						final File surveyDir = new File(course.getCourseEnvironment().getCourseBaseContainer().getBasefile() + File.separator + courseNode.getIdent()
								+ File.separator);
						if (surveyDir != null && surveyDir.exists() && surveyDir.listFiles().length > 0) {
							onyxSuccess = surveyDir.listFiles().length;
						}
					} else {
						final List<QTIResult> results = QTIResultManager.getInstance().selectResults(course.getResourceableId(), courseNode.getIdent(), repKey, 1);
						if (results != null && results.size() > 0) {
							onyxSuccess = results.size();
						}
					}
				}
				if (moduleConfiguration.get(CONFIG_KEY_TYPE_QTI) != null && moduleConfiguration.get(CONFIG_KEY_TYPE_QTI).equals(CONFIG_VALUE_QTI2) && onyxSuccess > 0) {
					replaceWizard = new IQEditReplaceWizard(ureq, getWindowControl(), course, courseNode, types, learners, null, onyxSuccess, true);
					replaceWizard.addControllerListener(this);
					cmc = new CloseableModalController(getWindowControl(), translate("close"), replaceWizard.getInitialComponent());
				} else {

					final List<QTIResult> results = QTIResultManager.getInstance().selectResults(course.getResourceableId(), courseNode.getIdent(), repKey, 1);
					// test was passed from an user
					final boolean passed = (results != null && results.size() > 0) ? true : false;
					// test was started and not passed
					// it exists partly results for this test
					final List<Identity> identitiesWithQtiSerEntry = IQManager.getInstance().getIdentitiesWithQtiSerEntry(course.getResourceableId(),
							courseNode.getIdent());
					if (passed || identitiesWithQtiSerEntry.size() > 0) {
						learners = new ArrayList<Identity>();
						for (final QTIResult result : results) {
							final Identity identity = result.getResultSet().getIdentity();
							if (identity != null && !learners.contains(identity)) {
								learners.add(identity);
							}
						}
						// add identities with qti.ser entry
						for (final Identity identity : identitiesWithQtiSerEntry) {
							if (!learners.contains(identity)) {
								learners.add(identity);
							}
						}
						replaceWizard = new IQEditReplaceWizard(ureq, getWindowControl(), course, courseNode, types, learners, results, identitiesWithQtiSerEntry.size());
						replaceWizard.addControllerListener(this);
						cmc = new CloseableModalController(getWindowControl(), translate("close"), replaceWizard.getInitialComponent());
					} else {
						searchController = new ReferencableEntriesSearchController(getWindowControl(), ureq, types, translate("command.chooseTest"));
						this.listenTo(searchController);
						cmc = new CloseableModalController(getWindowControl(), translate("close"), searchController.getInitialComponent());
					}
				}
			}
			cmc.activate();
		} else if (source == editTestButton) {
			CourseNodeFactory.getInstance().launchReferencedRepoEntryEditor(ureq, courseNode);
		}
	}

	/**
	 * This method updates the QTI Type in the editortreemodel.
	 * 
	 * @param re Needed to check if this Test is of QTI Type 2.1
	 */
	private void updateQtiType(final RepositoryEntry re) {
		final boolean isOnyx = OnyxModule.isOnyxTest(re.getOlatResource());
		if (isOnyx) {
			moduleConfiguration.set(CONFIG_KEY_TYPE_QTI, CONFIG_VALUE_QTI2);
		} else {
			moduleConfiguration.set(CONFIG_KEY_TYPE_QTI, CONFIG_VALUE_QTI1);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(final UserRequest urequest, final Controller source, final Event event) {
		if (source == modConfigForm) { // config form action
			if (event == Event.CANCELLED_EVENT) {
				return;
			} else if (event == Event.DONE_EVENT) {

				String qtiType = null;
				if (moduleConfiguration.get(IQEditController.CONFIG_KEY_TYPE_QTI) != null) {
					qtiType = (String) moduleConfiguration.get(IQEditController.CONFIG_KEY_TYPE_QTI);
				}
				final Boolean isOnyx = (qtiType != null && qtiType.equals(IQEditController.CONFIG_VALUE_QTI2)) ? true : false;
				if (isOnyx) {
					moduleConfiguration.set(CONFIG_KEY_TEMPLATE, modConfigForm.getTemplate());
					if (!(this.type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY))) {
						moduleConfiguration.set(CONFIG_KEY_ATTEMPTS, modConfigForm.getAttempts());
						moduleConfiguration.set(CONFIG_KEY_CUTVALUE, modConfigForm.getCutValue() != null ? Float.parseFloat("" + modConfigForm.getCutValue()) : null);
					}
					moduleConfiguration.set(CONFIG_KEY_DATE_DEPENDENT_RESULTS, new Boolean(modConfigForm.isShowResultsDateDependent()));
					moduleConfiguration.set(CONFIG_KEY_RESULTS_START_DATE, modConfigForm.getShowResultsStartDate());
					moduleConfiguration.set(CONFIG_KEY_RESULTS_END_DATE, modConfigForm.getShowResultsEndDate());
					moduleConfiguration.set(CONFIG_KEY_RESULT_ON_HOME_PAGE, modConfigForm.isShowResultsOnHomePage());
				} else {

					moduleConfiguration.set(CONFIG_KEY_ENABLEMENU, new Boolean(modConfigForm.isEnableMenu()));
					if (modConfigForm.isEnableMenu()) {
						// user selected sequence
						moduleConfiguration.set(CONFIG_KEY_DISPLAYMENU, Boolean.TRUE); // always display menu
						moduleConfiguration.set(CONFIG_KEY_QUESTIONPROGRESS, Boolean.FALSE); // never display question progress
						moduleConfiguration.set(CONFIG_KEY_RENDERMENUOPTION, Boolean.FALSE);// render sections and questions
					} else {
						// seqauential sequence, take displayMenu and questionProgress from configured values
						moduleConfiguration.set(CONFIG_KEY_DISPLAYMENU, new Boolean(modConfigForm.isDisplayMenu()));
						moduleConfiguration.set(CONFIG_KEY_QUESTIONPROGRESS, new Boolean(modConfigForm.isDisplayQuestionProgress()));
						moduleConfiguration.set(CONFIG_KEY_RENDERMENUOPTION, modConfigForm.isMenuRenderSectionsOnly());
					}
					moduleConfiguration.set(CONFIG_KEY_SEQUENCE, modConfigForm.getSequence());
					moduleConfiguration.set(CONFIG_KEY_ENABLECANCEL, new Boolean(modConfigForm.isEnableCancel()));
					moduleConfiguration.set(CONFIG_KEY_ENABLESUSPEND, new Boolean(modConfigForm.isEnableSuspend()));
					moduleConfiguration.set(CONFIG_KEY_QUESTIONTITLE, new Boolean(modConfigForm.isDisplayQuestionTitle()));
					// Only tests and selftests have summaries and score progress
					if (!type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY)) {
						moduleConfiguration.set(CONFIG_KEY_SUMMARY, modConfigForm.getSummary());
						moduleConfiguration.set(CONFIG_KEY_SCOREPROGRESS, new Boolean(modConfigForm.isDisplayScoreProgress()));
						moduleConfiguration.set(CONFIG_KEY_ENABLESCOREINFO, new Boolean(modConfigForm.isEnableScoreInfo()));
						moduleConfiguration.set(CONFIG_KEY_DATE_DEPENDENT_RESULTS, new Boolean(modConfigForm.isShowResultsDateDependent()));
						moduleConfiguration.set(CONFIG_KEY_RESULTS_START_DATE, modConfigForm.getShowResultsStartDate());
						moduleConfiguration.set(CONFIG_KEY_RESULTS_END_DATE, modConfigForm.getShowResultsEndDate());
						moduleConfiguration.set(CONFIG_KEY_RESULT_ON_FINISH, modConfigForm.isShowResultsAfterFinishTest());
						moduleConfiguration.set(CONFIG_KEY_RESULT_ON_HOME_PAGE, modConfigForm.isShowResultsOnHomePage());
					}
					// Only tests have a limitation on number of attempts
					if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS)) {
						moduleConfiguration.set(CONFIG_KEY_ATTEMPTS, modConfigForm.getAttempts());
					}
				}
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
				return;
			}
		} else if (source.equals(searchController)) {
			if (event == ReferencableEntriesSearchController.EVENT_REPOSITORY_ENTRY_SELECTED) {
				// repository search controller done
				cmc.deactivate();
				final RepositoryEntry re = searchController.getSelectedEntry();
				doIQReference(urequest, re);

				// after importing a test, refresh iqeditform
				if (this.type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY)) {
					moduleConfiguration.set(CONFIG_KEY_IS_SURVEY, Boolean.TRUE);
				}
				modConfigForm = new IQEditForm(urequest, getWindowControl(), moduleConfiguration);
				modConfigForm.addControllerListener(this);
				if (!this.type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY) && OnyxModule.isOnyxTest(re.getOlatResource())) {
					if (moduleConfiguration.get(CONFIG_KEY_ATTEMPTS) == null) {
						moduleConfiguration.set(CONFIG_KEY_ATTEMPTS, modConfigForm.getAttempts());
					}
				}
				myContent.put("iqeditform", modConfigForm.getInitialComponent());

			}
		} else if (source == accessibilityCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = accessibilityCondContr.getCondition();
				courseNode.setPreConditionAccess(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == fccecontr) {
			if (event == FileChooseCreateEditController.FILE_CHANGED_EVENT) {
				final String chosenFile = fccecontr.getChosenFile();
				if (chosenFile != null) {
					moduleConfiguration.set(CONFIG_KEY_DISCLAIMER, fccecontr.getChosenFile());
				} else {
					moduleConfiguration.remove(CONFIG_KEY_DISCLAIMER);
				}
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			} else if (event == FileChooseCreateEditController.ALLOW_RELATIVE_LINKS_CHANGED_EVENT) {
				allowRelativeLinks = fccecontr.getAllowRelativeLinks();
				courseNode.getModuleConfiguration().setBooleanEntry(CONFIG_KEY_ALLOW_RELATIVE_LINKS, allowRelativeLinks.booleanValue());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == correctQTIcontroller) {
			if (event == Event.DONE_EVENT) {
				// getWindowControl().pop();
			}
		} else if (source == replaceWizard) {
			if (event == Event.CANCELLED_EVENT) {
				cmc.deactivate();
			} else if (event == Event.DONE_EVENT) {
				cmc.deactivate();
				final String repositorySoftKey = (String) moduleConfiguration.get(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
				final Long repKey = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(repositorySoftKey, true).getKey();
				QTIResultManager.getInstance().deleteAllResults(course.getResourceableId(), courseNode.getIdent(), repKey);
				removeIQReference(moduleConfiguration);
				final VFSStatus isDeleted = IQManager.getInstance().removeQtiSerFiles(course.getResourceableId(), courseNode.getIdent());
				if (!isDeleted.equals(VFSConstants.YES)) {
					// couldn't removed qtiser files
					log.warn("Couldn't removed course node folder! Course resourceable id: " + course.getResourceableId() + ", Course node ident: "
							+ courseNode.getIdent());
				}
				doIQReference(urequest, replaceWizard.getSelectedRepositoryEntry());
			}
		}
	}

	private void doIQReference(final UserRequest urequest, final RepositoryEntry re) {
		// repository search controller done
		if (re != null) {
			if (CoordinatorManager.getInstance().getCoordinator().getLocker().isLocked(re.getOlatResource(), null)) {
				this.showError("error.entry.locked");
			} else {

				if (editTestButton == null) {
					editTestButton = LinkFactory.createButtonSmall("command.editRepFile", myContent, this);
				}
				if (isEditable(urequest.getIdentity(), re)) {
					myContent.put(editTestButton);
				} else {
					myContent.remove(editTestButton);
				}

				setIQReference(re, moduleConfiguration);
				previewLink = LinkFactory.createCustomLink("command.preview", "command.preview", re.getDisplayname(), Link.NONTRANSLATED, myContent, this);
				previewLink.setCustomEnabledLinkCSS("b_preview");
				previewLink.setTitle(getTranslator().translate("command.preview"));
				myContent.contextPut("dontRenderRepositoryButton", new Boolean(true));

				final boolean isOnyx = OnyxModule.isOnyxTest(re.getOlatResource());
				myContent.contextPut("isOnyx", new Boolean(isOnyx));
				if (isOnyx) {
					myContent.contextPut("onyxDisplayName", re.getDisplayname());
					moduleConfiguration.set(CONFIG_KEY_TYPE_QTI, CONFIG_VALUE_QTI2);
				} else {
					moduleConfiguration.set(CONFIG_KEY_TYPE_QTI, CONFIG_VALUE_QTI1);
					// If of type test, get min, max, cut - put in module config and push
					// to velocity
					if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS)) {
						updateModuleConfigFromQTIFile(re.getOlatResource());
						// Put values to velocity container
						myContent.contextPut(CONFIG_KEY_MINSCORE, moduleConfiguration.get(CONFIG_KEY_MINSCORE));
						myContent.contextPut(CONFIG_KEY_MAXSCORE, moduleConfiguration.get(CONFIG_KEY_MAXSCORE));
						myContent.contextPut(CONFIG_KEY_CUTVALUE, moduleConfiguration.get(CONFIG_KEY_CUTVALUE));
					}
					if (isEditable(urequest.getIdentity(), re)) {
						editTestButton = LinkFactory.createButtonSmall("command.editRepFile", myContent, this);
					}

				}
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableDefaultController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	public void addTabs(final TabbedPane tabbedPane) {
		myTabbedPane = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), accessibilityCondContr.getWrappedDefaultAccessConditionVC(translate("condition.accessibility.title")));
		// PANE_TAB_IQCONFIG_XXX is set during construction time
		tabbedPane.addTab(translate(PANE_TAB_IQCONFIG_XXX), main);
	}

	/**
	 * Ge the qti file soft key repository reference
	 * 
	 * @param config
	 * @param strict
	 * @return RepositoryEntry
	 */
	public static RepositoryEntry getIQReference(final ModuleConfiguration config, final boolean strict) {
		if (config == null) {
			if (strict) {
				throw new AssertException("missing config in IQ");
			} else {
				return null;
			}
		}
		final String repoSoftkey = (String) config.get(CONFIG_KEY_REPOSITORY_SOFTKEY);
		if (repoSoftkey == null) {
			if (strict) {
				throw new AssertException("invalid config when being asked for references");
			} else {
				return null;
			}
		}
		final RepositoryManager rm = RepositoryManager.getInstance();
		return rm.lookupRepositoryEntryBySoftkey(repoSoftkey, strict);
	}

	/**
	 * Set the referenced repository entry.
	 * 
	 * @param re
	 * @param moduleConfiguration
	 */
	public static void setIQReference(final RepositoryEntry re, final ModuleConfiguration moduleConfiguration) {
		moduleConfiguration.set(CONFIG_KEY_REPOSITORY_SOFTKEY, re.getSoftkey());
	}

	/**
	 * Remove the reference to the repository entry.
	 * 
	 * @param moduleConfiguration
	 */
	public static void removeIQReference(final ModuleConfiguration moduleConfiguration) {
		moduleConfiguration.remove(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
	}

	/**
	 * Update the module configuration from the qti file: read min/max/cut values
	 * 
	 * @param res
	 */
	public void updateModuleConfigFromQTIFile(final OLATResource res) {
		final FileResourceManager frm = FileResourceManager.getInstance();
		final File unzippedRoot = frm.unzipFileResource(res);
		// with VFS FIXME:pb:c: remove casts to LocalFileImpl and LocalFolderImpl if no longer needed.
		final VFSContainer vfsUnzippedRoot = new LocalFolderImpl(unzippedRoot);
		final VFSItem vfsQTI = vfsUnzippedRoot.resolve("qti.xml");
		if (vfsQTI == null) { throw new AssertException("qti file did not exist even it should be guaranteed by repositor check-in "
				+ ((LocalFileImpl) vfsQTI).getBasefile().getAbsolutePath()); }
		// ensures that InputStream is closed in every case.
		final Document doc = QTIHelper.getDocument((LocalFileImpl) vfsQTI);
		if (doc == null) {
			// error reading qti file (existence check was made before)
			throw new AssertException("qti file could not be read " + ((LocalFileImpl) vfsQTI).getBasefile().getAbsolutePath());
		}
		// Extract min, max and cut value
		Float minValue = null, maxValue = null, cutValue = null;
		final Element decvar = (Element) doc.selectSingleNode("questestinterop/assessment/outcomes_processing/outcomes/decvar");
		if (decvar != null) {
			final Attribute minval = decvar.attribute("minvalue");
			if (minval != null) {
				final String mv = minval.getValue();
				try {
					minValue = new Float(Float.parseFloat(mv));
				} catch (final NumberFormatException e1) {
					// if not correct in qti file -> ignore
				}
			}
			final Attribute maxval = decvar.attribute("maxvalue");
			if (maxval != null) {
				final String mv = maxval.getValue();
				try {
					maxValue = new Float(Float.parseFloat(mv));
				} catch (final NumberFormatException e1) {
					// if not correct in qti file -> ignore
				}
			}
			final Attribute cutval = decvar.attribute("cutvalue");
			if (cutval != null) {
				final String cv = cutval.getValue();
				try {
					cutValue = new Float(Float.parseFloat(cv));
				} catch (final NumberFormatException e1) {
					// if not correct in qti file -> ignore
				}
			}
		}
		// Put values to module configuration
		moduleConfiguration.set(CONFIG_KEY_MINSCORE, minValue);
		moduleConfiguration.set(CONFIG_KEY_MAXSCORE, maxValue);
		moduleConfiguration.set(CONFIG_KEY_CUTVALUE, cutValue);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		// child controllers registered with listenTo() get disposed in BasicController
		if (previewLayoutCtr != null) {
			previewLayoutCtr.dispose();
			previewLayoutCtr = null;
		}
	}

	public String[] getPaneKeys() {
		return paneKeys;
	}

	public TabbedPane getTabbedPane() {
		return myTabbedPane;
	}

}