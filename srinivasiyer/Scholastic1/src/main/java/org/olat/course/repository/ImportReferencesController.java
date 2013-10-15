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

package org.olat.course.repository;

import java.io.File;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.cp.CPEditController;
import org.olat.course.nodes.feed.FeedNodeEditController;
import org.olat.course.nodes.iq.IQEditController;
import org.olat.course.nodes.scorm.ScormEditController;
import org.olat.course.nodes.wiki.WikiEditController;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.AddingResourceException;
import org.olat.fileresource.types.BlogFileResource;
import org.olat.fileresource.types.FileResource;
import org.olat.fileresource.types.ImsCPFileResource;
import org.olat.fileresource.types.PodcastFileResource;
import org.olat.fileresource.types.ScormCPFileResource;
import org.olat.fileresource.types.WikiResource;
import org.olat.ims.qti.fileresource.SurveyFileResource;
import org.olat.ims.qti.fileresource.TestFileResource;
import org.olat.repository.DetailsReadOnlyForm;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryTableModel;
import org.olat.repository.controllers.RepositorySearchController;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;

/**
 * Initial Date: 19.05.2005
 * 
 * @author Mike Stock
 */
public class ImportReferencesController extends BasicController {

	/**
	 * Configures controller for importing CP references.
	 */
	public static final int IMPORT_CP = 0;
	/**
	 * Configures controller for importing test references.
	 */
	public static final int IMPORT_TEST = 1;
	/**
	 * Configures controller for importing survey references.
	 */
	public static final int IMPORT_SURVEY = 2;
	/**
	 * Configures controller for importing SCORM references.
	 */
	public static final int IMPORT_SCORM = 3;
	/**
	 * Configures controller for importing Wiki references.
	 */
	public static final int IMPORT_WIKI = 4;
	/**
	 * Configures controller for importing Wiki references.
	 */
	public static final int IMPORT_BLOG = 5;
	/**
	 * Configures controller for importing Wiki references.
	 */
	public static final int IMPORT_PODCAST = 6;

	private static final String PACKAGE = Util.getPackageName(ImportReferencesController.class);
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(ImportReferencesController.class);

	private final CourseNode node;
	private final int importMode;

	private final Translator translator;
	private final VelocityContainer main;
	private final Link importButton;
	private final Link reattachButton;
	private final Link noopButton;
	private Link continueButton;
	private RepositorySearchController searchController;
	private final RepositoryEntryImportExport importExport;
	private DetailsReadOnlyForm repoDetailsForm;
	private final Panel mainPanel;

	public ImportReferencesController(final UserRequest ureq, final WindowControl wControl, final CourseNode node, final int importMode,
			final RepositoryEntryImportExport importExport) {
		super(ureq, wControl);
		this.node = node;
		this.importMode = importMode;
		this.importExport = importExport;
		translator = new PackageTranslator(PACKAGE, ureq.getLocale());
		main = new VelocityContainer("ref", VELOCITY_ROOT + "/import_repo.html", translator, this);
		importButton = LinkFactory.createButton("import.import.action", main, this);
		reattachButton = LinkFactory.createButton("import.reattach.action", main, this);
		noopButton = LinkFactory.createButton("import.noop.action", main, this);

		main.contextPut("nodename", node.getShortTitle());
		main.contextPut("type", translator.translate("node." + node.getType()));
		main.contextPut("displayname", importExport.getDisplayName());
		main.contextPut("resourcename", importExport.getResourceName());
		main.contextPut("description", importExport.getDescription());
		mainPanel = new Panel("mainPanel");
		mainPanel.setContent(main);

		putInitialPanel(mainPanel);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == reattachButton) {
			String type;
			switch (importMode) {
				case IMPORT_CP:
					type = ImsCPFileResource.TYPE_NAME;
					break;
				case IMPORT_TEST:
					type = TestFileResource.TYPE_NAME;
					break;
				case IMPORT_SURVEY:
					type = SurveyFileResource.TYPE_NAME;
					break;
				case IMPORT_SCORM:
					type = ScormCPFileResource.TYPE_NAME;
					break;
				case IMPORT_WIKI:
					type = WikiResource.TYPE_NAME;
					break;
				case IMPORT_BLOG:
					type = BlogFileResource.TYPE_NAME;
					break;
				case IMPORT_PODCAST:
					type = PodcastFileResource.TYPE_NAME;
					break;
				default:
					throw new AssertException("Declared import type is not supported.");
			}
			removeAsListenerAndDispose(searchController);
			searchController = new RepositorySearchController(translator.translate("command.linkresource"), ureq, getWindowControl(), true, false, type);
			listenTo(searchController);
			searchController.doSearchByOwnerLimitType(ureq.getIdentity(), type);
			// brasato:: check in gui
			// was: getWindowControl().pushAsModalDialog(translator.translate("command.linkresource"), searchController.getInitialComponent());
			mainPanel.setContent(searchController.getInitialComponent());
		} else if (source == importButton) {
			final RepositoryEntry importedRepositoryEntry = doImport(importExport, node, importMode, false, ureq.getIdentity());
			// If not successfull, return. Any error messages have bean already set.
			if (importedRepositoryEntry == null) {
				getWindowControl().setError("Import failed.");
				return;
			}
			String typeName = null;
			switch (importMode) {
				case IMPORT_CP:
					typeName = ImsCPFileResource.TYPE_NAME;
					break;
				case IMPORT_TEST:
					typeName = TestFileResource.TYPE_NAME;
					break;
				case IMPORT_SURVEY:
					typeName = SurveyFileResource.TYPE_NAME;
					break;
				case IMPORT_SCORM:
					typeName = ScormCPFileResource.TYPE_NAME;
					break;
				case IMPORT_WIKI:
					typeName = WikiResource.TYPE_NAME;
					break;
				case IMPORT_BLOG:
					typeName = BlogFileResource.TYPE_NAME;
					break;
				case IMPORT_PODCAST:
					typeName = PodcastFileResource.TYPE_NAME;
					break;
				default:
					throw new AssertException("Declared import type is not supported.");
			}
			final Translator repoTranslator = new PackageTranslator(Util.getPackageName(RepositoryManager.class), ureq.getLocale());
			removeAsListenerAndDispose(repoDetailsForm);
			repoDetailsForm = new DetailsReadOnlyForm(ureq, getWindowControl(), importedRepositoryEntry, typeName, false);
			listenTo(repoDetailsForm);
			main.put("repoDetailsForm", repoDetailsForm.getInitialComponent());
			main.setPage(VELOCITY_ROOT + "/import_repo_details.html");
			continueButton = LinkFactory.createButton("import.redetails.continue", main, this);
			return;
		} else if (source == noopButton) {
			// delete reference
			switch (importMode) {
				case IMPORT_CP:
					CPEditController.removeCPReference(node.getModuleConfiguration());
					break;
				case IMPORT_TEST:
					IQEditController.removeIQReference(node.getModuleConfiguration());
					break;
				case IMPORT_SURVEY:
					IQEditController.removeIQReference(node.getModuleConfiguration());
					break;
				case IMPORT_SCORM:
					ScormEditController.removeScormCPReference(node.getModuleConfiguration());
					break;
				case IMPORT_WIKI:
					WikiEditController.removeWikiReference(node.getModuleConfiguration());
					break;
				case IMPORT_BLOG:
					FeedNodeEditController.removeReference(node.getModuleConfiguration());
					break;
				case IMPORT_PODCAST:
					FeedNodeEditController.removeReference(node.getModuleConfiguration());
					break;
				default:
					throw new AssertException("Declared import type is not supported.");
			}
			fireEvent(ureq, Event.DONE_EVENT);
		} else if (source == continueButton) {
			fireEvent(ureq, Event.DONE_EVENT);
		}
	}

	/**
	 * Import a referenced repository entry.
	 * 
	 * @param importExport
	 * @param node
	 * @param importMode Type of import.
	 * @param keepSoftkey If true, no new softkey will be generated.
	 * @param owner
	 * @return
	 */
	public static RepositoryEntry doImport(final RepositoryEntryImportExport importExport, final CourseNode node, final int importMode, final boolean keepSoftkey,
			final Identity owner) {
		final File fExportedFile = importExport.importGetExportedFile();
		FileResource fileResource = null;
		try {
			fileResource = FileResourceManager.getInstance().addFileResource(fExportedFile, fExportedFile.getName());
		} catch (final AddingResourceException e) {
			// e.printStackTrace();
			if (fileResource == null) {
				Tracing.logWarn("Error adding file resource during repository reference import: " + importExport.getDisplayName(), ImportReferencesController.class);
				return null;
			}
		}

		// create repository entry
		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry importedRepositoryEntry = rm.createRepositoryEntryInstance(owner.getName());
		importedRepositoryEntry.setDisplayname(importExport.getDisplayName());
		importedRepositoryEntry.setResourcename(importExport.getResourceName());
		importedRepositoryEntry.setDescription(importExport.getDescription());
		if (keepSoftkey) {
			final String theSoftKey = importExport.getSoftkey();
			if (rm.lookupRepositoryEntryBySoftkey(theSoftKey, false) != null) {
				/*
				 * keepSoftKey == true -> is used for importing in unattended mode. "Importing and keeping the soft key" only works if there is not an already existing
				 * soft key. In the case both if's are taken the respective IMS resource is not imported. It is important to be aware that the resource which triggered
				 * the import process still keeps the soft key reference, and thus points to the already existing resource.
				 */
				return null;
			}
			importedRepositoryEntry.setSoftkey(importExport.getSoftkey());
		}

		// Set the resource on the repository entry.
		final OLATResource ores = OLATResourceManager.getInstance().findOrPersistResourceable(fileResource);
		importedRepositoryEntry.setOlatResource(ores);
		final RepositoryHandler rh = RepositoryHandlerFactory.getInstance().getRepositoryHandler(importedRepositoryEntry);
		importedRepositoryEntry.setCanLaunch(rh.supportsLaunch(importedRepositoryEntry));

		// create security group
		final BaseSecurity securityManager = BaseSecurityManager.getInstance();
		final SecurityGroup newGroup = securityManager.createAndPersistSecurityGroup();
		// member of this group may modify member's membership
		securityManager.createAndPersistPolicy(newGroup, Constants.PERMISSION_ACCESS, newGroup);
		// members of this group are always authors also
		securityManager.createAndPersistPolicy(newGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);
		securityManager.addIdentityToSecurityGroup(owner, newGroup);
		importedRepositoryEntry.setOwnerGroup(newGroup);
		rm.saveRepositoryEntry(importedRepositoryEntry);

		if (!keepSoftkey) {
			setReference(importedRepositoryEntry, node, importMode);
		}

		return importedRepositoryEntry;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == searchController) {
			mainPanel.setContent(main);
			if (event.getCommand().equals(RepositoryTableModel.TABLE_ACTION_SELECT_LINK)) {
				// repository search controller done
				final RepositoryEntry re = searchController.getSelectedEntry();
				if (re != null) {
					setReference(re, node, importMode);
					getWindowControl().setInfo(translator.translate("import.reattach.success"));
					fireEvent(ureq, Event.DONE_EVENT);
				}
				// else cancelled repo search, display import options again.
			}
		}
	}

	private static void setReference(final RepositoryEntry re, final CourseNode node, final int importMode) {
		// attach repository entry to the node
		switch (importMode) {
			case IMPORT_CP:
				CPEditController.setCPReference(re, node.getModuleConfiguration());
				break;
			case IMPORT_TEST:
				IQEditController.setIQReference(re, node.getModuleConfiguration());
				break;
			case IMPORT_SURVEY:
				IQEditController.setIQReference(re, node.getModuleConfiguration());
				break;
			case IMPORT_SCORM:
				ScormEditController.setScormCPReference(re, node.getModuleConfiguration());
				break;
			case IMPORT_WIKI:
				WikiEditController.setWikiRepoReference(re, node.getModuleConfiguration());
				break;
			case IMPORT_BLOG:
				FeedNodeEditController.setReference(re, node.getModuleConfiguration());
				break;
			case IMPORT_PODCAST:
				FeedNodeEditController.setReference(re, node.getModuleConfiguration());
				break;
			default:
				throw new AssertException("Declared import type is not supported.");
		}
	}

	@Override
	protected void doDispose() {
		// Controllers autodisposed by BasicController
	}

}
