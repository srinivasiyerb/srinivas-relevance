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

package org.olat.repository.controllers;

import org.olat.catalog.CatalogEntry;
import org.olat.catalog.ui.CatalogController;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.components.tree.MenuTree;
import org.olat.core.gui.components.tree.TreeEvent;
import org.olat.core.gui.components.tree.TreeModel;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.MainLayoutBasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.Activateable;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.control.generic.portal.PortletFactory;
import org.olat.core.gui.control.generic.tool.ToolController;
import org.olat.core.gui.control.generic.tool.ToolFactory;
import org.olat.core.gui.control.generic.wizard.WizardController;
import org.olat.core.gui.control.state.ControllerState;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.core.util.tree.TreeHelper;
import org.olat.course.CourseModule;
import org.olat.fileresource.types.BlogFileResource;
import org.olat.fileresource.types.GlossaryResource;
import org.olat.fileresource.types.ImsCPFileResource;
import org.olat.fileresource.types.PodcastFileResource;
import org.olat.fileresource.types.ScormCPFileResource;
import org.olat.fileresource.types.SharedFolderFileResource;
import org.olat.fileresource.types.WikiResource;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.ims.qti.fileresource.SurveyFileResource;
import org.olat.ims.qti.fileresource.TestFileResource;
import org.olat.portfolio.EPTemplateMapResource;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryTableModel;
import org.olat.repository.delete.TabbedPaneController;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;

import de.bps.olat.repository.controllers.WizardAddOwnersController;

/**
 * Description: <br>
 * Activatable:<br>
 * Supported commands are: "search.home": root entry point "search.catalog": catalog view "search.generic": search form "search.my": list of owned resources
 * "search.REPOTYPE": list of all resources of given type, where REPOTYPE is something like course, test, survey, cp, scorm, sharedfolder
 * 
 * @date Initial Date: Oct 21, 2004 <br>
 * @author Felix Jost
 */
public class RepositoryMainController extends MainLayoutBasicController implements Activateable {

	OLog log = Tracing.createLoggerFor(this.getClass());
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(RepositoryManager.class);

	public static final String JUMPFROMEXTERN = "jumpfromextern";
	public static final String JUMPFROMCOURSE = "jumpfromcourse";
	static final String ACTION_NEW_CREATECOURSE = "cco";
	static final String ACTION_NEW_CREATETEST = "cte";
	static final String ACTION_NEW_CREATESURVEY = "csu";
	static final String ACTION_NEW_CREATESHAREDFOLDER = "csf";
	static final String ACTION_NEW_CREATECP = "ccp";
	static final String ACTION_NEW_WIKI = "wiki";
	static final String ACTION_NEW_PODCAST = "podcast";
	static final String ACTION_NEW_BLOG = "blog";
	static final String ACTION_NEW_GLOSSARY = "glossary";
	static final String ACTION_NEW_PORTFOLIO = "portfolio";
	private static final String ACTION_DELETE_RESOURCE = "deleteresource";
	private static final String ACTION_ADD_OWNERS = "addowners";

	private final Panel mainPanel;
	private final VelocityContainer main;
	private final LayoutMain3ColsController columnsLayoutCtr;
	private ToolController mainToolC;
	private final MenuTree menuTree;

	private String myEntriesNodeId;

	private RepositoryAddController addController;
	private final RepositorySearchController searchController;
	private final RepositoryDetailsController detailsController;
	private DialogBoxController launchEditorDialog;
	private boolean isAuthor;
	private CatalogController catalogCntrllr;
	// REVIEW:pb:concept for jumping between activateables, instead of hardcoding
	// each dependency
	// REVIEW:pb:like jumpfromcourse, backtocatalog, etc.
	private boolean backtocatalog = false;
	private TabbedPaneController deleteTabPaneCtr;
	private CloseableModalController cmc;
	private String lastUserObject = null; // to detect double click on catalog
											// menu item
	private WizardController wc;
	private RepositoryAddChooseStepsController chooseStepsController;
	private Controller creationWizardController;

	/**
	 * The check for author rights is executed on construction time and then cached during the objects lifetime.
	 * 
	 * @param ureq
	 * @param wControl
	 */
	public RepositoryMainController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		if (log.isDebug()) {
			log.debug("Constructing ReposityMainController for user::" + ureq.getIdentity());
		}

		// use i18n from RepositoryManager level
		setTranslator(Util.createPackageTranslator(RepositoryManager.class, ureq.getLocale()));

		// main component layed out in panel
		// use veloctiy pages from RepositoryManager level
		main = new VelocityContainer("vc_index", RepositoryManager.class, "index", getTranslator(), this);

		isAuthor = ureq.getUserSession().getRoles().isAuthor();
		// if the user has the role InstitutionalResourceManager (and has the same
		// university like the author)
		// then set isAuthor to true
		isAuthor = isAuthor | (ureq.getUserSession().getRoles().isInstitutionalResourceManager());
		main.contextPut("isAuthor", Boolean.valueOf(isAuthor));

		mainPanel = new Panel("repopanel");
		mainPanel.setContent(main);

		searchController = new RepositorySearchController(translate("details.header"), ureq, getWindowControl(), false, true);
		listenTo(searchController);
		main.put("searchcomp", searchController.getInitialComponent());

		detailsController = new RepositoryDetailsController(ureq, getWindowControl());
		listenTo(detailsController);
		detailsController.addControllerListener(searchController); // to catch
		// delete
		// events

		createRepoToolController(isAuthor, ureq.getUserSession().getRoles().isOLATAdmin());

		menuTree = new MenuTree("repoTree");
		menuTree.setTreeModel(buildTreeModel(isAuthor));
		menuTree.setSelectedNodeId(menuTree.getTreeModel().getRootNode().getIdent());
		setState("search.home");
		menuTree.addListener(this);

		final Component toolComp = (mainToolC == null ? null : mainToolC.getInitialComponent());
		columnsLayoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), menuTree, toolComp, mainPanel, "repomain");
		columnsLayoutCtr.addCssClassToMain("o_repository");

		listenTo(columnsLayoutCtr);

		if (isAuthor || ureq.getUserSession().getRoles().isOLATAdmin()) {
			activate(ureq, "search.my");
		} else {
			activate(ureq, "search.catalog");
		}

		putInitialPanel(columnsLayoutCtr.getInitialComponent());
	}

	/**
	 */
	private void createRepoToolController(final boolean isAuthor, final boolean bIsAdmin) {
		if (isAuthor) {
			mainToolC = ToolFactory.createToolController(getWindowControl());
			listenTo(mainToolC);
			// CP, SCORM, Wiki, Podcast, Blog, Test, Questionnaire, Glossary, other formats

			mainToolC.addHeader(translate("tools.add.header"));
			mainToolC.addLink(RepositoryAddController.ACTION_ADD_COURSE, translate("tools.add.course"), RepositoryAddController.ACTION_ADD_COURSE, "o_toolbox_course");
			mainToolC.addLink(RepositoryAddController.ACTION_ADD_CP, translate("tools.add.cp"), RepositoryAddController.ACTION_ADD_CP, "o_toolbox_content");
			mainToolC.addLink(RepositoryAddController.ACTION_ADD_SCORM, translate("tools.add.scorm"), RepositoryAddController.ACTION_ADD_SCORM, "o_toolbox_scorm");
			mainToolC.addLink(RepositoryAddController.ACTION_ADD_WIKI, translate("tools.add.wiki"), RepositoryAddController.ACTION_ADD_WIKI, "o_toolbox_wiki");
			mainToolC
					.addLink(RepositoryAddController.ACTION_ADD_PODCAST, translate("tools.add.podcast"), RepositoryAddController.ACTION_ADD_PODCAST, "o_toolbox_podcast");
			mainToolC.addLink(RepositoryAddController.ACTION_ADD_BLOG, translate("tools.add.blog"), RepositoryAddController.ACTION_ADD_BLOG, "o_toolbox_blog");
			mainToolC.addLink(RepositoryAddController.ACTION_ADD_TEST, translate("tools.add.test"), RepositoryAddController.ACTION_ADD_TEST, "o_toolbox_test");
			mainToolC.addLink(RepositoryAddController.ACTION_ADD_SURVEY, translate("tools.add.survey"), RepositoryAddController.ACTION_ADD_SURVEY,
					"o_toolbox_questionnaire");
			mainToolC.addLink(RepositoryAddController.ACTION_ADD_GLOSSARY, translate("tools.add.glossary"), RepositoryAddController.ACTION_ADD_GLOSSARY,
					"o_toolbox_glossary");
			mainToolC.addLink(RepositoryAddController.ACTION_ADD_DOC, translate("tools.add.webdoc"), RepositoryAddController.ACTION_ADD_DOC, "b_toolbox_doc");

			mainToolC.addHeader(translate("tools.new.header"));
			mainToolC.addLink(ACTION_NEW_CREATECOURSE, translate("tools.new.createcourse"), ACTION_NEW_CREATECOURSE, "o_toolbox_course");
			mainToolC.addLink(ACTION_NEW_CREATECP, translate("tools.new.createcp"), ACTION_NEW_CREATECP, "o_toolbox_content");
			mainToolC.addLink(ACTION_NEW_WIKI, translate("tools.new.wiki"), ACTION_NEW_WIKI, "o_toolbox_wiki");
			mainToolC.addLink(ACTION_NEW_PODCAST, translate("tools.new.podcast"), ACTION_NEW_PODCAST, "o_toolbox_podcast");
			mainToolC.addLink(ACTION_NEW_BLOG, translate("tools.new.blog"), ACTION_NEW_BLOG, "o_toolbox_blog");
			mainToolC.addLink(ACTION_NEW_PORTFOLIO, translate("tools.new.portfolio"), ACTION_NEW_PORTFOLIO, "o_toolbox_portfolio");
			mainToolC.addLink(ACTION_NEW_CREATETEST, translate("tools.new.createtest"), ACTION_NEW_CREATETEST, "o_toolbox_test");
			mainToolC.addLink(ACTION_NEW_CREATESURVEY, translate("tools.new.createsurvey"), ACTION_NEW_CREATESURVEY, "o_toolbox_questionnaire");
			mainToolC.addLink(ACTION_NEW_CREATESHAREDFOLDER, translate("tools.new.createsharedfolder"), ACTION_NEW_CREATESHAREDFOLDER, "o_toolbox_sharedfolder");
			mainToolC.addLink(ACTION_NEW_GLOSSARY, translate("tools.new.glossary"), ACTION_NEW_GLOSSARY, "o_toolbox_glossary");
			if (bIsAdmin || isAuthor) {
				mainToolC.addHeader(translate("tools.administration.header"));
				if (bIsAdmin) {
					mainToolC.addLink(ACTION_DELETE_RESOURCE, translate("tools.delete.resource"));
				}
				mainToolC.addLink(ACTION_ADD_OWNERS, translate("tools.add.owners"));
			}
		} else {
			mainToolC = null;
		}
	}

	private TreeModel buildTreeModel(final boolean bIsAuthor) {
		final GenericTreeModel gtm = new GenericTreeModel();
		final GenericTreeNode rootNode = new GenericTreeNode(translate("search.home"), "search.home");
		gtm.setRootNode(rootNode);

		// TODO:catalog not yet finished :
		rootNode.addChild(new GenericTreeNode(translate("search.catalog"), "search.catalog"));

		// check if repository portlet is configured in olat_portals.xml
		final boolean repoPortletOn = PortletFactory.containsPortlet("RepositoryPortletStudent");
		// add default searches
		rootNode.addChild(new GenericTreeNode(translate("search.generic"), "search.generic"));
		if (bIsAuthor) {
			final GenericTreeNode myEntriesTn = new GenericTreeNode(translate("search.my"), "search.my");
			myEntriesNodeId = myEntriesTn.getIdent();
			rootNode.addChild(myEntriesTn);
		}
		// add repository search also used by portlets
		if (repoPortletOn) {
			rootNode.addChild(new GenericTreeNode(translate("search.mycourses.student"), "search.mycourses.student"));
			// for authors or users with group rights also show the teacher portlet
			if (bIsAuthor || BusinessGroupManagerImpl.getInstance().findBusinessGroupsAttendedBy(BusinessGroup.TYPE_RIGHTGROUP, getIdentity(), null).size() > 0) {
				rootNode.addChild(new GenericTreeNode(translate("search.mycourses.teacher"), "search.mycourses.teacher"));
			}
		}
		rootNode.addChild(new GenericTreeNode(translate("search.course"), "search.course"));
		if (bIsAuthor) {
			// cp, scorm, wiki, podcast, portfolie, test, questionn, resource folder, glossary
			rootNode.addChild(new GenericTreeNode(translate("search.cp"), "search.cp"));
			rootNode.addChild(new GenericTreeNode(translate("search.scorm"), "search.scorm"));
			rootNode.addChild(new GenericTreeNode(translate("search.wiki"), "search.wiki"));
			rootNode.addChild(new GenericTreeNode(translate("search.podcast"), "search.podcast"));
			rootNode.addChild(new GenericTreeNode(translate("search.blog"), "search.blog"));
			rootNode.addChild(new GenericTreeNode(translate("search.portfolio"), "search.portfolio"));
			rootNode.addChild(new GenericTreeNode(translate("search.test"), "search.test"));
			rootNode.addChild(new GenericTreeNode(translate("search.survey"), "search.survey"));
			rootNode.addChild(new GenericTreeNode(translate("search.sharedfolder"), "search.sharedfolder"));
			rootNode.addChild(new GenericTreeNode(translate("search.glossary"), "search.glossary"));
		}

		return gtm;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		final long start = System.currentTimeMillis(); // TODO: add check if debug logging
		if (source == menuTree) {
			if (event.getCommand().equals(MenuTree.COMMAND_TREENODE_CLICKED)) {
				final Component toolComp = (mainToolC == null ? null : mainToolC.getInitialComponent());
				columnsLayoutCtr.setCol2(toolComp);
				mainPanel.setContent(main);
				final TreeEvent te = (TreeEvent) event;
				final TreeNode clickedNode = menuTree.getTreeModel().getNodeById(te.getNodeId());
				final Object userObject = clickedNode.getUserObject();
				final long duration1 = System.currentTimeMillis() - start;
				log.info("Repo-Perf: duration1=" + duration1);
				activateContent(ureq, userObject, null);
			}
		}
		final long duration = System.currentTimeMillis() - start;
		log.info("Repo-Perf: event duration=" + duration);
	}

	@Override
	protected void adjustState(final ControllerState cstate, final UserRequest ureq) {
		final String cmd = cstate.getSerializedState();
		// sub view identifyers are attached to user object with ":"
		final String[] cmdArray = cmd.split(":");
		activateContent(ureq, cmdArray[0], (cmdArray.length > 1 ? cmdArray[1] : null));
		// adjust the menu
		final TreeNode rootNode = this.menuTree.getTreeModel().getRootNode();
		final TreeNode activatedNode = TreeHelper.findNodeByUserObject(cmdArray[0], rootNode);
		this.menuTree.setSelectedNode(activatedNode);
	}

	/**
	 * Activate the content in the content area based on a user object representing the identifyer of the content
	 * 
	 * @param ureq
	 * @param uObj
	 * @param subViewIdentifyer optional view identifyer for a sub controller
	 */
	private void activateContent(final UserRequest ureq, final Object userObject, final String subViewIdentifyer) {
		log.info("activateContent userObject=" + userObject);
		if (userObject.equals("search.home")) { // the
			// home
			main.setPage(VELOCITY_ROOT + "/index.html");
			mainPanel.setContent(main);
		} else if (userObject.equals("search.generic")) { // new
			// search
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.displaySearchForm();
			searchController.enableBackToSearchFormLink(true);
		} else if (userObject.equals("search.catalog")) {
			// enter catalog browsing
			activateCatalogController(ureq, subViewIdentifyer);
			mainPanel.setContent(catalogCntrllr.getInitialComponent());
		} else if (userObject.equals("search.my")) { // search
			// own resources
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByOwner(ureq.getIdentity());
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.course")) { // search
			// courses
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByTypeLimitAccess(CourseModule.getCourseTypeName(), ureq);
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.test")) { // search
			// tests
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByTypeLimitAccess(TestFileResource.TYPE_NAME, ureq);
			searchController.enableBackToSearchFormLink(false);

		} else if (userObject.equals("search.survey")) { // search
			// surveys
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByTypeLimitAccess(SurveyFileResource.TYPE_NAME, ureq);
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.cp")) { // search
			// cp
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByTypeLimitAccess(ImsCPFileResource.TYPE_NAME, ureq);
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.scorm")) { // search
			// scorm
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByTypeLimitAccess(ScormCPFileResource.TYPE_NAME, ureq);
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.sharedfolder")) { // search
			// shared folder
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByTypeLimitAccess(SharedFolderFileResource.TYPE_NAME, ureq);
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.wiki")) { // search
			// wiki
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByTypeLimitAccess(WikiResource.TYPE_NAME, ureq);
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.podcast")) { // search
			// podcast
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByTypeLimitAccess(PodcastFileResource.TYPE_NAME, ureq);
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.blog")) { // search
			// blog
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByTypeLimitAccess(BlogFileResource.TYPE_NAME, ureq);
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.glossary")) { // search
			// glossary
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByTypeLimitAccess(GlossaryResource.TYPE_NAME, ureq);
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.portfolio")) { // search
			// portfolio template maps
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchByTypeLimitAccess(EPTemplateMapResource.TYPE_NAME, ureq);
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.mycourses.student")) {
			// my courses as student
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchMyCoursesStudent(ureq);
			searchController.enableBackToSearchFormLink(false);
		} else if (userObject.equals("search.mycourses.teacher")) {
			// my courses as student
			main.setPage(VELOCITY_ROOT + "/index2.html");
			mainPanel.setContent(main);
			searchController.doSearchMyCoursesTeacher(ureq);
			searchController.enableBackToSearchFormLink(false);
		}
		// encode sub view identifyer into state, attach separated by ":"
		lastUserObject = userObject.toString();
		final String state = lastUserObject + (subViewIdentifyer != null ? ":" + subViewIdentifyer : null);
		setState(state);
		removeAsListenerAndDispose(deleteTabPaneCtr);
	}

	private void activateCatalogController(final UserRequest ureq, final String nodeId) {
		// create new catalog controller with given node if none exists
		// create also new catalog controller when the user clicked twice on the
		// catalog link in the menu
		if (catalogCntrllr == null || lastUserObject.equals("search.catalog")) {
			removeAsListenerAndDispose(catalogCntrllr);
			catalogCntrllr = new CatalogController(ureq, getWindowControl(), nodeId);
			listenTo(catalogCntrllr);
		} else {
			// just activate the existing catalog
			if (nodeId != null) {
				catalogCntrllr.activate(ureq, nodeId);
			}
		}
		// set correct tool controller
		final ToolController ccToolCtr = catalogCntrllr.createCatalogToolController();
		final Component toolComp = (ccToolCtr == null ? null : ccToolCtr.getInitialComponent());
		columnsLayoutCtr.setCol2(toolComp);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest urequest, final Controller source, final Event event) {
		if (source == chooseStepsController) {
			if (event.equals(RepositoryAddChooseStepsController.CREATION_WIZARD)) {
				cmc.deactivate();
				final RepositoryEntry addedEntry = chooseStepsController.getCourseRepositoryEntry();
				final RepositoryHandler handler = RepositoryHandlerFactory.getInstance().getRepositoryHandler(addedEntry);
				creationWizardController = handler.createWizardController(addedEntry, urequest, getWindowControl());
				listenTo(creationWizardController);
				getWindowControl().pushAsModalDialog(creationWizardController.getInitialComponent());
			} else if (event.equals(RepositoryAddChooseStepsController.DETAILS_VIEW)) {
				// show details view
				cmc.deactivate();
			} else if (event.equals(RepositoryAddChooseStepsController.COURSE_EDIT)) {
				// show course editor
				cmc.deactivate();
				final ToolController toolC = detailsController.setEntry(chooseStepsController.getCourseRepositoryEntry(), urequest, false);
				final Component toolComp = (toolC == null ? null : toolC.getInitialComponent());
				columnsLayoutCtr.setCol2(toolComp);
				mainPanel.setContent(detailsController.getInitialComponent());
				detailsController.doEdit(urequest);
			} else if (event.equals(Event.CANCELLED_EVENT)) {
				cmc.deactivate();
			}
			removeAsListenerAndDispose(chooseStepsController);
		} else if (source == creationWizardController) {
			if (event.equals(Event.CHANGED_EVENT)) {
				getWindowControl().pop();
				removeAsListenerAndDispose(creationWizardController);
				detailsController.setEntry(chooseStepsController.getCourseRepositoryEntry(), urequest, false);
				detailsController.doLaunch(urequest);
			} else if (event.equals(Event.CANCELLED_EVENT)) {
				// delete entry when the wizard was cancelled
				detailsController.deleteRepositoryEntry(urequest, getWindowControl(), (RepositoryEntry) chooseStepsController.getCourseRepositoryEntry());
				getWindowControl().pop();
				removeAsListenerAndDispose(creationWizardController);
				mainPanel.setContent(main);
			}
		} else if (source == addController) { // process add controller finished()
			if (event.equals(Event.DONE_EVENT)) {
				// default to myEntries search
				cmc.deactivate();
				final ToolController toolC = detailsController.setEntry(addController.getAddedEntry(), urequest, false);
				final Component toolComp = (toolC == null ? null : toolC.getInitialComponent());
				mainPanel.setContent(detailsController.getInitialComponent());
				columnsLayoutCtr.setCol2(toolComp);
				menuTree.setSelectedNodeId(myEntriesNodeId);

				final RepositoryHandler handler = RepositoryHandlerFactory.getInstance().getRepositoryHandler(addController.getAddedEntry());
				// ask if we should start the editor/wizard right now.
				if (handler.supportsEdit(addController.getAddedEntry())) {
					// check if wizard is supported and if the repo entry is new (not imported)
					final boolean isNew = addController.getActionProcess().equals(RepositoryAddController.PROCESS_NEW);
					if (handler.supportsWizard(addController.getAddedEntry()) && isNew) {
						removeAsListenerAndDispose(chooseStepsController);
						chooseStepsController = new RepositoryAddChooseStepsController(urequest, getWindowControl(), addController.getAddedEntry());
						listenTo(chooseStepsController);
						cmc = new CloseableModalController(getWindowControl(), translate("close"), chooseStepsController.getInitialComponent());
						cmc.activate();
					} else {
						removeAsListenerAndDispose(launchEditorDialog); // cleanup old instance
						launchEditorDialog = activateYesNoDialog(urequest, null, translate("add.launchedit.msg"), launchEditorDialog);
						launchEditorDialog.setUserObject(addController.getAddedEntry());
					}
					return;
				}
			} else if (event.equals(Event.CANCELLED_EVENT)) {
				cmc.deactivate();
			} else if (event.equals(Event.FAILED_EVENT)) {
				getWindowControl().setError(translate("add.failed"));
				mainPanel.setContent(main);
			}
		} else if (source == searchController) {
			final RepositoryEntry selectedEntry = RepositoryManager.getInstance().lookupRepositoryEntry(searchController.getSelectedEntry().getKey());
			if (selectedEntry == null) {
				showWarning("warn.entry.meantimedeleted");
				main.setPage(VELOCITY_ROOT + "/index.html");
				return;
			}
			if (event.getCommand().equals(RepositoryTableModel.TABLE_ACTION_SELECT_ENTRY)) {
				// entry has been selected to be launched in the
				// searchController
				final ToolController toolC = detailsController.setEntry(selectedEntry, urequest, false);
				if (selectedEntry.getCanLaunch()) {
					detailsController.doLaunch(urequest);
				} else if (selectedEntry.getCanDownload()) {
					detailsController.doDownload(urequest);
				} else { // offer details view
					final Component toolComp = (toolC == null ? null : toolC.getInitialComponent());
					columnsLayoutCtr.setCol2(toolComp);
					mainPanel.setContent(detailsController.getInitialComponent());
				}
			} else if (event.getCommand().equals(RepositoryTableModel.TABLE_ACTION_SELECT_LINK)) {
				// entry has been selected in the searchController
				final ToolController toolC = detailsController.setEntry(selectedEntry, urequest, false);
				final Component toolComp = (toolC == null ? null : toolC.getInitialComponent());
				columnsLayoutCtr.setCol2(toolComp);
				mainPanel.setContent(detailsController.getInitialComponent());
			}
		} else if (source == detailsController) { // back from details
			if (event.equals(Event.DONE_EVENT)) {
				if (backtocatalog) {
					backtocatalog = false;
					final ToolController toolC = catalogCntrllr.createCatalogToolController();
					final Component toolComp = (toolC == null ? null : toolC.getInitialComponent());
					columnsLayoutCtr.setCol2(toolComp);
					mainPanel.setContent(catalogCntrllr.getInitialComponent());

				} else {
					final Component toolComp = (mainToolC == null ? null : mainToolC.getInitialComponent());
					columnsLayoutCtr.setCol2(toolComp);
					mainPanel.setContent(main);
				}
			} else if (event instanceof EntryChangedEvent) {
				final Component toolComp = (mainToolC == null ? null : mainToolC.getInitialComponent());
				columnsLayoutCtr.setCol2(toolComp);
				mainPanel.setContent(main);
			} else if (event.equals(Event.CHANGED_EVENT)) {
				final ToolController toolC = detailsController.getDetailsToolController();
				final Component toolComp = (toolC == null ? null : toolC.getInitialComponent());
				columnsLayoutCtr.setCol2(toolComp);
			}
		} else if (source == mainToolC) {
			// handles the tools events for the Repository Actions
			handleToolEvents(urequest, event);
		} else if (source == launchEditorDialog) {
			if (DialogBoxUIFactory.isYesEvent(event)) {
				// do
				// launch
				// editor
				final ToolController toolC = detailsController.setEntry((RepositoryEntry) launchEditorDialog.getUserObject(), urequest, false);
				final Component toolComp = (toolC == null ? null : toolC.getInitialComponent());
				columnsLayoutCtr.setCol2(toolComp);
				mainPanel.setContent(detailsController.getInitialComponent());
				detailsController.doEdit(urequest);
			} else {
				// no, don't launch editor }
			}
		}
		/*
		 * from the catalog controller its sending change events if the tools available have changed, e.g. no longer localTreeAdmin
		 */
		else if (source == catalogCntrllr) {
			if (event == Event.CHANGED_EVENT) {
				final ToolController toolC = catalogCntrllr.createCatalogToolController();
				final Component toolComp = (toolC == null ? null : toolC.getInitialComponent());
				columnsLayoutCtr.setCol2(toolComp);
			} else if (event instanceof EntryChangedEvent) {
				// REVIEW:pb:signal from the catalog to show Detail!
				// REVIEW:pb:the first event is also a change event, signalling a change
				// in the tools
				// REVIEW:pb:the EntryChangedEvent is misused to signal "show details"
				final EntryChangedEvent entryChangedEvent = (EntryChangedEvent) event;
				final RepositoryEntry e = RepositoryManager.getInstance().lookupRepositoryEntry(entryChangedEvent.getChangedEntryKey());
				if (e != null) {
					backtocatalog = true;
					final ToolController toolC = detailsController.setEntry(e, urequest, false);
					final Component toolComp = (toolC == null ? null : toolC.getInitialComponent());
					columnsLayoutCtr.setCol2(toolComp);
					mainPanel.setContent(detailsController.getInitialComponent());
				}
			}

		} else if (source == wc) {
			if (event == Event.CANCELLED_EVENT) {
				wc.dispose();
				cmc.deactivate();
			}
			if (event == Event.DONE_EVENT) {
				wc.dispose();
				cmc.deactivate();
			}
		} else if (source == cmc) { // user closes the overlay and not the cancel button -> clean up repo entry
			if (addController != null) {
				addController.doDispose();// does the clean up
			}
			fireEvent(urequest, Event.CANCELLED_EVENT);
		}
	}

	/**
	 * @param urequest
	 * @param event
	 */
	private void handleToolEvents(final UserRequest urequest, final Event event) {
		/*
		 * Repository Tools
		 */
		if (event.getCommand().startsWith(RepositoryAddController.ACTION_ADD_PREFIX)) {
			removeAsListenerAndDispose(addController);
			addController = new RepositoryAddController(urequest, getWindowControl(), event.getCommand());
			listenTo(addController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), addController.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		} else if (event.getCommand().equals(ACTION_NEW_CREATECOURSE)) {
			removeAsListenerAndDispose(addController);
			addController = new RepositoryAddController(urequest, getWindowControl(), RepositoryAddController.ACTION_NEW_COURSE);
			listenTo(addController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), addController.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		} else if (event.getCommand().equals(ACTION_NEW_CREATETEST)) {
			removeAsListenerAndDispose(addController);
			addController = new RepositoryAddController(urequest, getWindowControl(), RepositoryAddController.ACTION_NEW_TEST);
			listenTo(addController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), addController.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		} else if (event.getCommand().equals(ACTION_NEW_CREATESURVEY)) {
			removeAsListenerAndDispose(addController);
			addController = new RepositoryAddController(urequest, getWindowControl(), RepositoryAddController.ACTION_NEW_SURVEY);
			listenTo(addController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), addController.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		} else if (event.getCommand().equals(ACTION_NEW_CREATESHAREDFOLDER)) {
			removeAsListenerAndDispose(addController);
			addController = new RepositoryAddController(urequest, getWindowControl(), RepositoryAddController.ACTION_NEW_SHAREDFOLDER);
			listenTo(addController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), addController.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		} else if (event.getCommand().equals(ACTION_NEW_WIKI)) {
			removeAsListenerAndDispose(addController);
			addController = new RepositoryAddController(urequest, getWindowControl(), RepositoryAddController.ACTION_NEW_WIKI);
			listenTo(addController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), addController.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		} else if (event.getCommand().equals(ACTION_NEW_PODCAST)) {
			removeAsListenerAndDispose(addController);
			addController = new RepositoryAddController(urequest, getWindowControl(), RepositoryAddController.ACTION_NEW_PODCAST);
			listenTo(addController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), addController.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		} else if (event.getCommand().equals(ACTION_NEW_BLOG)) {
			removeAsListenerAndDispose(addController);
			addController = new RepositoryAddController(urequest, getWindowControl(), RepositoryAddController.ACTION_NEW_BLOG);
			listenTo(addController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), addController.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		} else if (event.getCommand().equals(ACTION_NEW_GLOSSARY)) {
			removeAsListenerAndDispose(addController);
			addController = new RepositoryAddController(urequest, getWindowControl(), RepositoryAddController.ACTION_NEW_GLOSSARY);
			listenTo(addController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), addController.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		} else if (event.getCommand().equals(ACTION_NEW_PORTFOLIO)) {
			removeAsListenerAndDispose(addController);
			addController = new RepositoryAddController(urequest, getWindowControl(), RepositoryAddController.ACTION_NEW_PORTFOLIO);
			listenTo(addController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), addController.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		} else if (event.getCommand().equals(ACTION_DELETE_RESOURCE)) {
			removeAsListenerAndDispose(deleteTabPaneCtr);
			deleteTabPaneCtr = new TabbedPaneController(urequest, getWindowControl());
			listenTo(deleteTabPaneCtr);
			mainPanel.setContent(deleteTabPaneCtr.getInitialComponent());
		} else if (event.getCommand().equals(ACTION_NEW_CREATECP)) {
			removeAsListenerAndDispose(addController);
			addController = new RepositoryAddController(urequest, getWindowControl(), RepositoryAddController.ACTION_NEW_CP);
			listenTo(addController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), addController.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		} else if (event.getCommand().equals(ACTION_ADD_OWNERS)) {
			removeAsListenerAndDispose(wc);
			wc = new WizardAddOwnersController(urequest, getWindowControl());
			listenTo(wc);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), wc.getInitialComponent());
			listenTo(cmc);
			cmc.activate();
			return;
		}
	}

	@Override
	protected void doDispose() {
		//
	}

	/**
	 * @see org.olat.core.gui.control.generic.dtabs.Activateable#activate(org.olat.core.gui.UserRequest, java.lang.String)
	 */
	public void activate(final UserRequest ureq, String viewIdentifier) {
		// REVIEW:pb: activate is now also used for course details activation
		// REVIEW:pb:concept for jumping between activateables, instead of
		// hardcoding each dependency
		// REVIEW:pb:like jumpfromcourse, backtocatalog, etc.
		if (viewIdentifier.startsWith(JUMPFROMEXTERN)) {
			viewIdentifier = viewIdentifier.replaceFirst(JUMPFROMEXTERN, "").trim();
			boolean jumpfromcourse = false;

			if (viewIdentifier.startsWith(JUMPFROMCOURSE)) {
				viewIdentifier = viewIdentifier.replaceFirst(JUMPFROMCOURSE, "").trim();
				jumpfromcourse = true;
			}

			Long key = new Long(0);
			try {
				key = Long.valueOf(viewIdentifier);
			} catch (final NumberFormatException e) {
				throw new AssertException(e.getMessage());
			}
			final RepositoryEntry selectedEntry = RepositoryManager.getInstance().lookupRepositoryEntry(key);
			if (selectedEntry != null) {
				final ToolController toolC = detailsController.setEntry(selectedEntry, ureq, jumpfromcourse);
				final Component toolComp = (toolC == null ? null : toolC.getInitialComponent());
				columnsLayoutCtr.setCol2(toolComp);
				mainPanel.setContent(detailsController.getInitialComponent());
			}
		} else if (viewIdentifier.startsWith(CatalogEntry.class.getSimpleName())) {
			final String catId = viewIdentifier.substring(viewIdentifier.indexOf(':') + 1);
			final TreeNode rootNode = menuTree.getTreeModel().getRootNode();
			final TreeNode activatedNode = TreeHelper.findNodeByUserObject("search.catalog", rootNode);
			if (activatedNode != null) {
				menuTree.setSelectedNodeId(activatedNode.getIdent());
				activateContent(ureq, "search.catalog", catId);
			}
		} else {
			// find the menu node that has the user object that represents the
			// viewIdentifyer
			// sub view identifyers are separated with ":" characters
			final String[] parsedViewIdentifyers = viewIdentifier.split(":");

			final TreeNode rootNode = this.menuTree.getTreeModel().getRootNode();
			final TreeNode activatedNode = TreeHelper.findNodeByUserObject(parsedViewIdentifyers[0], rootNode);
			if (activatedNode != null) {
				this.menuTree.setSelectedNodeId(activatedNode.getIdent());
				activateContent(ureq, parsedViewIdentifyers[0], (parsedViewIdentifyers.length > 1 ? parsedViewIdentifyers[1] : null));
			} else {
				// not found, activate the root node
				this.menuTree.setSelectedNodeId(rootNode.getIdent());
				activateContent(ureq, parsedViewIdentifyers[0], null);
			}
		}
	}
}