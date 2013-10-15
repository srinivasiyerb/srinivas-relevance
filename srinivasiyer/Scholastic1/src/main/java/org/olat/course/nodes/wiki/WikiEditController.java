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

package org.olat.course.nodes.wiki;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
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
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.condition.Condition;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.NodeEditController;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.WikiCourseNode;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseEditorTreeModel;
import org.olat.fileresource.types.WikiResource;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.wiki.WikiMainController;
import org.olat.modules.wiki.WikiManager;
import org.olat.modules.wiki.WikiSecurityCallback;
import org.olat.modules.wiki.WikiSecurityCallbackImpl;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.controllers.ReferencableEntriesSearchController;
import org.olat.repository.controllers.RepositoryDetailsController;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;

/**
 * Description: <BR/>
 * Edit controller for single page course nodes
 * <P/>
 * Initial Date: Oct 12, 2004
 * 
 * @author Felix Jost
 */
public class WikiEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {
	public static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	public static final String PANE_TAB_WIKICONFIG = "pane.tab.wikiconfig";
	public static final String PANE_TAB_WIKIDISPLAYCONFIG = "pane.tab.wikidisplayconfig";

	private static final String[] paneKeys = { PANE_TAB_WIKICONFIG, PANE_TAB_ACCESSIBILITY };

	private static final String CHOSEN_ENTRY = "chosen_entry";
	private static final String CONFIG_KEY_REPOSITORY_SOFTKEY = "reporef";

	private final ModuleConfiguration moduleConfiguration;
	private final WikiCourseNode wikiCourseNode;
	private final ConditionEditController accessCondContr;
	private TabbedPane tabs;
	private final Panel main;
	private final VelocityContainer content;
	private ReferencableEntriesSearchController searchController;
	private WikiMainController wikiCtr;
	private CloseableModalController cmcWikiCtr;
	private CloseableModalController cmcSearchController;
	private Link previewLink;
	private final Link chooseButton;
	private final Link changeButton;
	private Link editLink;
	private final VelocityContainer editAccessVc;
	private final ConditionEditController editCondContr;
	private final ICourse course;

	/**
	 * Constructor for wiki page editor controller
	 * 
	 * @param config The node module configuration
	 * @param ureq The user request
	 * @param wikiCourseNode The current wiki page course node
	 * @param course
	 */
	public WikiEditController(final ModuleConfiguration config, final UserRequest ureq, final WindowControl wControl, final WikiCourseNode wikiCourseNode,
			final ICourse course, final UserCourseEnvironment euce) {
		super(ureq, wControl);
		this.moduleConfiguration = config;
		this.wikiCourseNode = wikiCourseNode;
		// o_clusterOk by guido: save to hold reference to course inside editor
		this.course = course;

		main = new Panel("wikimain");

		content = this.createVelocityContainer("edit");
		/* previewButton = LinkFactory.createButtonSmall("command.preview", content, this); */
		chooseButton = LinkFactory.createButtonSmall("command.create", content, this);
		changeButton = LinkFactory.createButtonSmall("command.change", content, this);

		editAccessVc = this.createVelocityContainer("edit_access");
		final CourseGroupManager groupMgr = course.getCourseEnvironment().getCourseGroupManager();
		final CourseEditorTreeModel editorModel = course.getEditorTreeModel();
		// Accessibility precondition
		final Condition accessCondition = wikiCourseNode.getPreConditionAccess();
		accessCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, accessCondition, "accessConditionForm", AssessmentHelper.getAssessableNodes(
				editorModel, wikiCourseNode), euce);
		this.listenTo(accessCondContr);
		editAccessVc.put("readerCondition", accessCondContr.getInitialComponent());

		// wiki read / write preconditions
		final Condition editCondition = wikiCourseNode.getPreConditionEdit();
		editCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, editCondition, "editConditionForm", AssessmentHelper.getAssessableNodes(
				editorModel, wikiCourseNode), euce);
		this.listenTo(editCondContr);
		editAccessVc.put("editCondition", editCondContr.getInitialComponent());

		if (config.get(CONFIG_KEY_REPOSITORY_SOFTKEY) != null) {
			// fetch repository entry to display the repository entry title of the
			// chosen wiki
			final RepositoryEntry re = getWikiRepoReference(config, false);
			if (re == null) { // we cannot display the entrie's name, because the
				// repository entry had been deleted between the time
				// when it was chosen here, and now
				this.showError("error.repoentrymissing");
				content.contextPut("showPreviewLink", Boolean.FALSE);
				content.contextPut(CHOSEN_ENTRY, translate("no.entry.chosen"));
			} else {
				// no securitycheck on wiki, editable by everybody
				editLink = LinkFactory.createButtonSmall("edit", content, this);
				content.contextPut("showPreviewLink", Boolean.TRUE);
				previewLink = LinkFactory.createCustomLink("command.preview", "command.preview", re.getDisplayname(), Link.NONTRANSLATED, content, this);
				previewLink.setCustomEnabledLinkCSS("b_preview");
				previewLink.setTitle(getTranslator().translate("command.preview"));
			}
		} else {
			// no valid config yet
			content.contextPut("showPreviewLink", Boolean.FALSE);
			content.contextPut(CHOSEN_ENTRY, translate("no.entry.chosen"));
		}

		main.setContent(content);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == previewLink) {
			// Preview as modal dialogue only if the config is valid
			final RepositoryEntry re = getWikiRepoReference(moduleConfiguration, false);
			if (re == null) { // we cannot preview it, because the repository entry
				// had been deleted between the time when it was
				// chosen here, and now
				this.showError("error.repoentrymissing");
			} else {
				// File cpRoot =
				// FileResourceManager.getInstance().unzipFileResource(re.getOlatResource());
				final Identity ident = ureq.getIdentity();
				final boolean isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
				final boolean isResourceOwner = RepositoryManager.getInstance().isOwnerOfRepositoryEntry(ident, re);
				final CourseEnvironment cenv = course.getCourseEnvironment();
				final SubscriptionContext subsContext = WikiManager.createTechnicalSubscriptionContextForCourse(cenv, wikiCourseNode);
				final WikiSecurityCallback callback = new WikiSecurityCallbackImpl(null, isOlatAdmin, false, false, isResourceOwner, subsContext);
				wikiCtr = WikiManager.getInstance().createWikiMainController(ureq, getWindowControl(), re.getOlatResource(), callback, null);
				cmcWikiCtr = new CloseableModalController(getWindowControl(), translate("command.close"), wikiCtr.getInitialComponent());
				this.listenTo(cmcWikiCtr);
				cmcWikiCtr.insertHeaderCss();
				cmcWikiCtr.activate();
			}
		} else if (source == chooseButton || source == changeButton) {
			searchController = new ReferencableEntriesSearchController(getWindowControl(), ureq, WikiResource.TYPE_NAME, translate("command.choose"));
			this.listenTo(searchController);
			cmcSearchController = new CloseableModalController(getWindowControl(), translate("close"), searchController.getInitialComponent(), true,
					translate("command.create"));
			cmcSearchController.activate();
		} else if (source == editLink) {
			final RepositoryEntry repositoryEntry = wikiCourseNode.getReferencedRepositoryEntry();
			if (repositoryEntry == null) {
				// do nothing
				return;
			}
			final RepositoryHandler typeToEdit = RepositoryHandlerFactory.getInstance().getRepositoryHandler(repositoryEntry);
			// Open editor in new tab
			final OLATResourceable ores = repositoryEntry.getOlatResource();
			final DTabs dts = (DTabs) Windows.getWindows(ureq).getWindow(ureq).getAttribute("DTabs");
			DTab dt = dts.getDTab(ores);
			if (dt == null) {
				// does not yet exist -> create and add
				dt = dts.createDTab(ores, repositoryEntry.getDisplayname());
				if (dt == null) {
					// null means DTabs are full -> warning is shown
					return;
				}
				// user activity logger is set by course factory
				final Controller editorController = typeToEdit.createLaunchController(ores, null, ureq, dt.getWindowControl());
				if (editorController == null) {
					// editor could not be created -> warning is shown
					return;
				}
				dt.setController(editorController);
				dts.addDTab(dt);
			}
			dts.activate(ureq, dt, RepositoryDetailsController.ACTIVATE_EDITOR);
		}

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest urequest, final Controller source, final Event event) {
		if (source == searchController) {
			cmcSearchController.deactivate();
			// repository search controller done
			if (event == ReferencableEntriesSearchController.EVENT_REPOSITORY_ENTRY_SELECTED) {
				final RepositoryEntry re = searchController.getSelectedEntry();
				if (re != null) {
					setWikiRepoReference(re, moduleConfiguration);
					content.contextPut("showPreviewLink", Boolean.TRUE);
					previewLink = LinkFactory.createCustomLink("command.preview", "command.preview", re.getDisplayname(), Link.NONTRANSLATED, content, this);
					previewLink.setCustomEnabledLinkCSS("b_preview");
					previewLink.setTitle(getTranslator().translate("command.preview"));
					// no securitycheck on wiki, editable by everybody
					editLink = LinkFactory.createButtonSmall("edit", content, this);
					// fire event so the updated config is saved by the
					// editormaincontroller
					fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
				}
			} // else cancelled repo search
		} else if (source == accessCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = accessCondContr.getCondition();
				wikiCourseNode.setPreConditionAccess(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == editCondContr) {
			if (event == Event.CHANGED_EVENT) {
				final Condition cond = editCondContr.getCondition();
				wikiCourseNode.setPreConditionEdit(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == cmcWikiCtr) {
			if (event == CloseableModalController.CLOSE_MODAL_EVENT) {
				cmcWikiCtr.dispose();
				wikiCtr.dispose();
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	@Override
	public void addTabs(final TabbedPane tabbedPane) {
		tabs = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), editAccessVc);
		tabbedPane.addTab(translate(PANE_TAB_WIKICONFIG), main);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// child controllers registered with listenTo() get disposed in BasicController
		if (wikiCtr != null) {
			wikiCtr.dispose();
			wikiCtr = null;
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController#getPaneKeys()
	 */
	@Override
	public String[] getPaneKeys() {
		return paneKeys;
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController#getTabbedPane()
	 */
	@Override
	public TabbedPane getTabbedPane() {
		return tabs;
	}

	/**
	 * @param config the moduleConfig
	 * @param strict an assertion exception is thrown if no entry is found when strict is set to true, otherwise, null is returned
	 * @return the repositoryentry or null if not in strict mode and no entry found
	 * @throws AssertException when in strict mode and no entry is found
	 */
	public static RepositoryEntry getWikiRepoReference(final ModuleConfiguration config, final boolean strict) {
		if (config == null) { throw new AssertException("missing config in wiki course node"); }
		final String repoSoftkey = (String) config.get(WikiEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
		if (repoSoftkey == null) { throw new AssertException("invalid config when being asked for references"); }
		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry entry = rm.lookupRepositoryEntryBySoftkey(repoSoftkey, strict);
		// entry can be null only if !strict
		return entry;
	}

	/**
	 * set an repository reference to an wiki course node
	 * 
	 * @param re
	 * @param moduleConfiguration
	 */
	public static void setWikiRepoReference(final RepositoryEntry re, final ModuleConfiguration moduleConfiguration) {
		moduleConfiguration.set(CONFIG_KEY_REPOSITORY_SOFTKEY, re.getSoftkey());
	}

	/**
	 * @param moduleConfiguration
	 * @return boolean
	 */
	public static boolean isModuleConfigValid(final ModuleConfiguration moduleConfiguration) {
		return (moduleConfiguration.get(CONFIG_KEY_REPOSITORY_SOFTKEY) != null);
	}

	/**
	 * @param config the moduleConfig
	 * @param strict an assertion exception is thrown if no entry is found when strict is set to true, otherwise, null is returned
	 * @return the repositoryentry or null if not in strict mode and no entry found
	 * @throws AssertException when in strict mode and no entry is found
	 */
	public static RepositoryEntry getWikiReference(final ModuleConfiguration config, final boolean strict) {
		if (config == null) {
			if (strict) {
				throw new AssertException("missing config in Wiki");
			} else {
				return null;
			}
		}
		final String repoSoftkey = (String) config.get(WikiEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
		if (repoSoftkey == null) {
			if (strict) {
				throw new AssertException("invalid config when being asked for references");
			} else {
				return null;
			}
		}
		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry entry = rm.lookupRepositoryEntryBySoftkey(repoSoftkey, strict);
		// entry can be null only if !strict
		return entry;
	}

	/**
	 * remove ref to wiki from the config
	 * 
	 * @param moduleConfig
	 */
	public static void removeWikiReference(final ModuleConfiguration moduleConfig) {
		moduleConfig.remove(WikiEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
	}

}
