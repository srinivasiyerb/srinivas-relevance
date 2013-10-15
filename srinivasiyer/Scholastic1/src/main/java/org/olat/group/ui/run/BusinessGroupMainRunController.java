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

package org.olat.group.ui.run;

import java.util.List;

import org.olat.ControllerFactory;
import org.olat.admin.securitygroup.gui.GroupController;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.collaboration.CollaborationTools;
import org.olat.collaboration.CollaborationToolsFactory;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.components.tree.MenuTree;
import org.olat.core.gui.components.tree.TreeModel;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.MainLayoutBasicController;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.activity.OlatResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.UserSession;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.ContactMessage;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.nodes.iq.AssessmentEvent;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.group.GroupLoggingAction;
import org.olat.group.context.BGContextManager;
import org.olat.group.context.BGContextManagerImpl;
import org.olat.group.properties.BusinessGroupPropertyManager;
import org.olat.group.ui.BGConfigFlags;
import org.olat.group.ui.BGControllerFactory;
import org.olat.group.ui.BGTranslatorFactory;
import org.olat.group.ui.edit.BusinessGroupEditController;
import org.olat.group.ui.edit.BusinessGroupModifiedEvent;
import org.olat.instantMessaging.InstantMessagingModule;
import org.olat.instantMessaging.groupchat.InstantMessagingGroupChatController;
import org.olat.modules.co.ContactFormController;
import org.olat.modules.wiki.WikiManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryTableModel;
import org.olat.resource.OLATResource;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description: <BR>
 * Runtime environment for a business group. Use the BGControllerFactory and not the constructor to create an instance of this controller.
 * <P>
 * 
 * @version Initial Date: Aug 11, 2004
 * @author patrick
 */

public class BusinessGroupMainRunController extends MainLayoutBasicController implements GenericEventListener {

	private static final String INITVIEW_TOOLFOLDER = "toolfolder";
	public static final OLATResourceable ORES_TOOLFOLDER = OresHelper.createOLATResourceableType(INITVIEW_TOOLFOLDER);

	private static final String INITVIEW_TOOLFORUM = "toolforum";
	public static final OLATResourceable ORES_TOOLFORUM = OresHelper.createOLATResourceableType(INITVIEW_TOOLFORUM);

	private static final String INITVIEW_TOOLWIKI = WikiManager.WIKI_RESOURCE_FOLDER_NAME;
	public static final OLATResourceable ORES_TOOLWIKI = OresHelper.createOLATResourceableType(INITVIEW_TOOLWIKI);

	private static final String INITVIEW_TOOLPORTFOLIO = "toolportfolio";
	public static final OLATResourceable ORES_TOOLPORTFOLIO = OresHelper.createOLATResourceableType(INITVIEW_TOOLPORTFOLIO);

	public static final String INITVIEW_TOOLCAL = "action.calendar.group";
	public static final OLATResourceable ORES_TOOLCAL = OresHelper.createOLATResourceableType(INITVIEW_TOOLCAL);

	private static final String PACKAGE = Util.getPackageName(BusinessGroupMainRunController.class);

	// activity identifyers are used as menu user objects and for the user
	// activity events
	// change value with care, used in logfiles etc!!
	/** activity identitfyer: user selected overview in menu * */
	public static final String ACTIVITY_MENUSELECT_OVERVIEW = "MENU_OVERVIEW";
	/** activity identitfyer: user selected information in menu * */
	public static final String ACTIVITY_MENUSELECT_INFORMATION = "MENU_INFORMATION";
	/** activity identitfyer: user selected memberlist in menu * */
	public static final String ACTIVITY_MENUSELECT_MEMBERSLIST = "MENU_MEMBERLIST";
	/** activity identitfyer: user selected contactform in menu * */
	public static final String ACTIVITY_MENUSELECT_CONTACTFORM = "MENU_CONTACTFORM";
	/** activity identitfyer: user selected forum in menu * */
	public static final String ACTIVITY_MENUSELECT_FORUM = "MENU_FORUM";
	/** activity identitfyer: user selected folder in menu * */
	public static final String ACTIVITY_MENUSELECT_FOLDER = "MENU_FOLDER";
	/** activity identitfyer: user selected chat in menu * */
	public static final String ACTIVITY_MENUSELECT_CHAT = "MENU_CHAT";
	/** activity identitfyer: user selected calendar in menu * */
	public static final String ACTIVITY_MENUSELECT_CALENDAR = "MENU_CALENDAR";
	/** activity identitfyer: user selected administration in menu * */
	public static final String ACTIVITY_MENUSELECT_ADMINISTRATION = "MENU_ADMINISTRATION";
	/** activity identitfyer: user selected show resources in menu * */
	public static final String ACTIVITY_MENUSELECT_SHOW_RESOURCES = "MENU_SHOW_RESOURCES";
	public static final String ACTIVITY_MENUSELECT_WIKI = "MENU_SHOW_WIKI";
	/* activity identitfyer: user selected show portoflio in menu */
	public static final String ACTIVITY_MENUSELECT_PORTFOLIO = "MENU_SHOW_PORTFOLIO";

	private final Panel mainPanel;
	private final VelocityContainer main;
	private VelocityContainer vc_sendToChooserForm, resourcesVC;
	private final Identity identity;
	private final PackageTranslator resourceTrans;

	private BusinessGroup businessGroup;

	private final MenuTree bgTree;
	private final LayoutMain3ColsController columnLayoutCtr;
	private final Panel all;

	private Controller collabToolCtr;
	private Controller chatCtr;

	private BusinessGroupEditController bgEditCntrllr;
	private TableController resourcesCtr;

	private BusinessGroupSendToChooserForm sendToChooserForm;

	private GroupController gownersC;
	private GroupController gparticipantsC;
	private GroupController waitingListController;

	private final boolean isAdmin;

	private final BGConfigFlags flags;

	private BusinessGroupPropertyManager bgpm;
	private final UserSession userSession;
	private String adminNodeId; // reference to admin menu item

	// not null indicates tool is enabled
	private GenericTreeNode nodeFolder;
	private GenericTreeNode nodeForum;
	private GenericTreeNode nodeWiki;
	private GenericTreeNode nodeCal;
	private GenericTreeNode nodePortfolio;
	private boolean groupRunDisabled;
	private final OLATResourceable assessmentEventOres;

	/**
	 * Do not use this constructor! Use the BGControllerFactory instead!
	 * 
	 * @param ureq
	 * @param control
	 * @param currBusinessGroup
	 * @param flags
	 * @param initialViewIdentifier supported are null, "toolforum", "toolfolder"
	 */
	public BusinessGroupMainRunController(final UserRequest ureq, final WindowControl control, final BusinessGroup currBusinessGroup, final BGConfigFlags flags,
			final String initialViewIdentifier) {
		super(ureq, control);
		addLoggingResourceable(LoggingResourceable.wrap(currBusinessGroup));
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_OPEN, getClass());
		this.bgpm = new BusinessGroupPropertyManager(currBusinessGroup);
		this.flags = flags;
		this.businessGroup = currBusinessGroup;
		this.identity = ureq.getIdentity();
		this.userSession = ureq.getUserSession();
		this.assessmentEventOres = OresHelper.createOLATResourceableType(AssessmentEvent.class);

		final boolean isOwner = BaseSecurityManager.getInstance().isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_ACCESS, businessGroup);
		this.isAdmin = isOwner || flags.isEnabled(BGConfigFlags.IS_GM_ADMIN);

		// Initialize translator:
		// package translator with default group fallback translators and type
		// translator
		setTranslator(BGTranslatorFactory.createBGPackageTranslator(PACKAGE, currBusinessGroup.getType(), ureq.getLocale()));
		this.resourceTrans = new PackageTranslator(Util.getPackageName(RepositoryTableModel.class), ureq.getLocale(), getTranslator());

		// main component layed out in panel
		main = createVelocityContainer("bgrun");
		exposeGroupDetailsToVC(currBusinessGroup);

		mainPanel = new Panel("p_buddygroupRun");
		mainPanel.setContent(main);
		//
		bgTree = new MenuTree("bgTree");
		final TreeModel trMdl = buildTreeModel();
		bgTree.setTreeModel(trMdl);
		bgTree.addListener(this);
		//
		columnLayoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), bgTree, null, mainPanel, "grouprun");
		listenTo(columnLayoutCtr); // cleanup on dispose

		//
		all = putInitialPanel(columnLayoutCtr.getInitialComponent());
		// register for AssessmentEvents triggered by this user
		userSession.getSingleUserEventCenter().registerFor(this, userSession.getIdentity(), assessmentEventOres);
		/*
		 * lastUsage, update lastUsage if group is run if you can acquire the lock on the group for a very short time. If this is not possible, then the lastUsage is
		 * already up to date within one-day-precision.
		 */

		BusinessGroupManagerImpl.getInstance().setLastUsageFor(currBusinessGroup);

		// disposed message controller
		// must be created beforehand
		final Panel empty = new Panel("empty");// empty panel set as "menu" and "tool"
		final Controller disposedBusinessGroup = new DisposedBusinessGroup(ureq, getWindowControl());
		final LayoutMain3ColsController disposedController = new LayoutMain3ColsController(ureq, getWindowControl(), empty, empty,
				disposedBusinessGroup.getInitialComponent(), "disposed grouprun");
		disposedController.addDisposableChildController(disposedBusinessGroup);
		setDisposedMsgController(disposedController);

		// add as listener to BusinessGroup so we are being notified about changes.
		CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(this, ureq.getIdentity(), currBusinessGroup);

		// show disabled message when collaboration is disabled (e.g. in a test)
		if (AssessmentEvent.isAssessmentStarted(ureq.getUserSession())) {
			groupRunDisabled = true;
			this.showError("grouprun.disabled");
		}

		// REVIEW:PB:2009-05-31: consolidate ContextEntry <-> initialViewIdentifier Concept -> go for ContextEntry at the end.
		// first step -> if initialViewIdentifier != null -> map initialViewIdentifier to ore with OresHelper
		// how to remove initialViewIdentifier and replace by ContextEntry Path?

		// jump to either the forum or the folder if the business-launch-path says so.
		final BusinessControl bc = getWindowControl().getBusinessControl();
		final ContextEntry ce = bc.popLauncherContextEntry();
		if (ce != null) { // a context path is left for me
			final OLATResourceable ores = ce.getOLATResourceable();
			if (OresHelper.equals(ores, ORES_TOOLFORUM)) {
				// start the forum
				if (nodeForum != null) {
					handleTreeActions(ureq, ACTIVITY_MENUSELECT_FORUM);
					bgTree.setSelectedNode(nodeForum);
				} else { // not enabled
					final String text = translate("warn.forumnotavailable");
					final Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
					listenTo(mc); // cleanup on dispose
					mainPanel.setContent(mc.getInitialComponent());
				}
			} else if (OresHelper.equals(ores, ORES_TOOLFOLDER)) {
				if (nodeFolder != null) {
					handleTreeActions(ureq, ACTIVITY_MENUSELECT_FOLDER);
					bgTree.setSelectedNode(nodeFolder);
				} else { // not enabled
					final String text = translate("warn.foldernotavailable");
					final Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
					listenTo(mc); // cleanup on dispose
					mainPanel.setContent(mc.getInitialComponent());
				}
			} else if (OresHelper.equals(ores, ORES_TOOLWIKI)) {
				if (nodeWiki != null) {
					handleTreeActions(ureq, ACTIVITY_MENUSELECT_WIKI);
					bgTree.setSelectedNode(nodeWiki);
				} else { // not enabled
					final String text = translate("warn.wikinotavailable");
					final Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
					listenTo(mc); // cleanup on dispose
					mainPanel.setContent(mc.getInitialComponent());
				}
			} else if (OresHelper.equals(ores, ORES_TOOLCAL)) {
				if (nodeCal != null) {
					handleTreeActions(ureq, ACTIVITY_MENUSELECT_CALENDAR);
					bgTree.setSelectedNode(nodeCal);
				} else { // not enabled
					final String text = translate("warn.calnotavailable");
					final Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
					listenTo(mc); // cleanup on dispose
					mainPanel.setContent(mc.getInitialComponent());
				}
			} else if (OresHelper.equals(ores, ORES_TOOLPORTFOLIO)) {
				if (nodePortfolio != null) {
					handleTreeActions(ureq, ACTIVITY_MENUSELECT_PORTFOLIO);
					bgTree.setSelectedNode(nodePortfolio);
				} else { // not enabled
					final String text = translate("warn.portfolionotavailable");
					final Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
					listenTo(mc); // cleanup on dispose
					mainPanel.setContent(mc.getInitialComponent());
				}
			}
		}

		// jump to node if initialViewIdent says so.
		if (initialViewIdentifier != null) {
			if (initialViewIdentifier.equals(INITVIEW_TOOLFORUM)) {
				if (nodeForum != null) {
					handleTreeActions(ureq, ACTIVITY_MENUSELECT_FORUM);
					bgTree.setSelectedNode(nodeForum);
				} else { // not enabled
					final String text = translate("warn.forumnotavailable");
					final Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
					listenTo(mc); // cleanup on dispose
					mainPanel.setContent(mc.getInitialComponent());
				}
			} else if (initialViewIdentifier.equals(INITVIEW_TOOLFOLDER)) {
				if (nodeFolder != null) {
					handleTreeActions(ureq, ACTIVITY_MENUSELECT_FOLDER);
					bgTree.setSelectedNode(nodeFolder);
				} else { // not enabled
					final String text = translate("warn.foldernotavailable");
					final Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
					listenTo(mc); // cleanup on dispose
					mainPanel.setContent(mc.getInitialComponent());
				}
			} else if (initialViewIdentifier.equals(INITVIEW_TOOLWIKI)) {
				if (nodeWiki != null) {
					handleTreeActions(ureq, ACTIVITY_MENUSELECT_WIKI);
					bgTree.setSelectedNode(nodeWiki);
				} else { // not enabled
					final String text = translate("warn.wikinotavailable");
					final Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
					listenTo(mc); // cleanup on dispose
					mainPanel.setContent(mc.getInitialComponent());
				}
			} else if (initialViewIdentifier.equals(INITVIEW_TOOLCAL)) {
				if (nodeCal != null) {
					handleTreeActions(ureq, ACTIVITY_MENUSELECT_CALENDAR);
					bgTree.setSelectedNode(nodeCal);
				} else { // not enabled
					final String text = translate("warn.calnotavailable");
					final Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
					listenTo(mc); // cleanup on dispose
					mainPanel.setContent(mc.getInitialComponent());
				}
			} else {
				throw new AssertException("unknown initialViewIdentifier:'" + initialViewIdentifier + "'");
			}
		}
	}

	private void exposeGroupDetailsToVC(final BusinessGroup currBusinessGroup) {
		main.contextPut("BuddyGroup", currBusinessGroup);
		main.contextPut("hasOwners", new Boolean(flags.isEnabled(BGConfigFlags.GROUP_OWNERS)));
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// events from menutree
		if (source == bgTree) { // user chose news, contactform, forum, folder or
			// administration
			if (!groupRunDisabled && event.getCommand().equals(MenuTree.COMMAND_TREENODE_CLICKED)) {
				final TreeNode selTreeNode = bgTree.getSelectedNode();
				final String cmd = (String) selTreeNode.getUserObject();
				handleTreeActions(ureq, cmd);
			} else if (groupRunDisabled) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_OVERVIEW);
				this.showError("grouprun.disabled");
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == collabToolCtr) {
			if (event == Event.CANCELLED_EVENT || event == Event.DONE_EVENT || event == Event.BACK_EVENT || event == Event.FAILED_EVENT) {
				// In all cases (success or failure) we
				// go back to the group overview page.
				bgTree.setSelectedNodeId(bgTree.getTreeModel().getRootNode().getIdent());
				mainPanel.setContent(main);
			}
		} else if (source == bgEditCntrllr) {
			// changes from the admin controller
			if (event == Event.CHANGED_EVENT) {
				final TreeModel trMdl = buildTreeModel();
				bgTree.setTreeModel(trMdl);
			} else if (event == Event.CANCELLED_EVENT) {
				// could not get lock on business group, back to inital screen
				bgTree.setSelectedNodeId(bgTree.getTreeModel().getRootNode().getIdent());
				mainPanel.setContent(main);
			}

		} else if (source == resourcesCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				final TableEvent te = (TableEvent) event;
				final String actionid = te.getActionId();
				final int rowid = te.getRowId();
				final RepositoryTableModel repoTableModel = (RepositoryTableModel) resourcesCtr.getTableDataModel();
				final RepositoryEntry currentRepoEntry = (RepositoryEntry) repoTableModel.getObject(rowid);
				if (actionid.equals(RepositoryTableModel.TABLE_ACTION_SELECT_LINK)) {
					final OLATResource ores = currentRepoEntry.getOlatResource();
					if (ores == null) { throw new AssertException("repoEntry had no olatresource, repoKey = " + currentRepoEntry.getKey()); }

					addLoggingResourceable(LoggingResourceable.wrap(ores, OlatResourceableType.genRepoEntry));
					final String title = currentRepoEntry.getDisplayname();

					final DTabs dts = (DTabs) Windows.getWindows(ureq).getWindow(ureq).getAttribute("DTabs");
					// was brasato:: DTabs dts = getWindowControl().getDTabs();
					DTab dt = dts.getDTab(ores);
					if (dt == null) {
						// does not yet exist -> create and add
						dt = dts.createDTab(ores, title);
						if (dt == null) { return; }
						final Controller ctrl = ControllerFactory.createLaunchController(ores, null, ureq, dt.getWindowControl(), true);
						dt.setController(ctrl);
						dts.addDTab(dt);
					}
					dts.activate(ureq, dt, null); // null: do not activate to a certain
													// view
				}
			}
		} else if (source == sendToChooserForm) {
			if (event == Event.DONE_EVENT) {
				removeAsListenerAndDispose(collabToolCtr);
				collabToolCtr = createContactFormController(ureq);
				listenTo(collabToolCtr);
				mainPanel.setContent(collabToolCtr.getInitialComponent());
			} else if (event == Event.CANCELLED_EVENT) {
				// back to group overview
				bgTree.setSelectedNodeId(bgTree.getTreeModel().getRootNode().getIdent());
				mainPanel.setContent(main);
			}
		}

	}

	/**
	 * generates the email adress list.
	 * 
	 * @param ureq
	 * @return a contact form controller for this group
	 */
	private ContactFormController createContactFormController(final UserRequest ureq) {
		final BaseSecurity scrtMngr = BaseSecurityManager.getInstance();

		final ContactMessage cmsg = new ContactMessage(ureq.getIdentity());
		// two named ContactLists, the new way using the contact form
		// the same name as in the checkboxes are taken as contactlist names
		ContactList ownerCntctLst;// = new ContactList(translate("sendtochooser.form.chckbx.owners"));
		ContactList partipCntctLst;// = new ContactList(translate("sendtochooser.form.chckbx.partip"));
		ContactList waitingListContactList;// = new ContactList(translate("sendtochooser.form.chckbx.waitingList"));
		if (flags.isEnabled(BGConfigFlags.GROUP_OWNERS)) {
			if (sendToChooserForm.ownerChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_ALL)) {
				ownerCntctLst = new ContactList(translate("sendtochooser.form.radio.owners.all"));
				final SecurityGroup owners = businessGroup.getOwnerGroup();
				final List<Identity> ownerList = scrtMngr.getIdentitiesOfSecurityGroup(owners);
				ownerCntctLst.addAllIdentites(ownerList);
				cmsg.addEmailTo(ownerCntctLst);
			} else {
				if (sendToChooserForm.ownerChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_CHOOSE)) {
					ownerCntctLst = new ContactList(translate("sendtochooser.form.radio.owners.choose"));
					final SecurityGroup owners = businessGroup.getOwnerGroup();
					final List<Identity> ownerList = scrtMngr.getIdentitiesOfSecurityGroup(owners);
					final List<Identity> changeableOwnerList = scrtMngr.getIdentitiesOfSecurityGroup(owners);
					for (final Identity identity : ownerList) {
						boolean keyIsSelected = false;
						for (final Long key : sendToChooserForm.getSelectedOwnerKeys()) {
							if (key.equals(identity.getKey())) {
								keyIsSelected = true;
								break;
							}
						}
						if (!keyIsSelected) {
							changeableOwnerList.remove(changeableOwnerList.indexOf(identity));
						}
					}
					ownerCntctLst.addAllIdentites(changeableOwnerList);
					cmsg.addEmailTo(ownerCntctLst);
				}
			}
		}
		if (sendToChooserForm != null) {
			if (sendToChooserForm.participantChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_ALL)) {
				partipCntctLst = new ContactList(translate("sendtochooser.form.radio.partip.all"));
				final SecurityGroup participants = businessGroup.getPartipiciantGroup();
				final List<Identity> participantsList = scrtMngr.getIdentitiesOfSecurityGroup(participants);
				partipCntctLst.addAllIdentites(participantsList);
				cmsg.addEmailTo(partipCntctLst);
			} else {
				if (sendToChooserForm.participantChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_CHOOSE)) {
					partipCntctLst = new ContactList(translate("sendtochooser.form.radio.partip.choose"));
					final SecurityGroup participants = businessGroup.getPartipiciantGroup();
					final List<Identity> participantsList = scrtMngr.getIdentitiesOfSecurityGroup(participants);
					final List<Identity> changeableParticipantsList = scrtMngr.getIdentitiesOfSecurityGroup(participants);
					for (final Identity identity : participantsList) {
						boolean keyIsSelected = false;
						for (final Long key : sendToChooserForm.getSelectedPartipKeys()) {
							if (key.equals(identity.getKey())) {
								keyIsSelected = true;
								break;
							}
						}
						if (!keyIsSelected) {
							changeableParticipantsList.remove(changeableParticipantsList.indexOf(identity));
						}
					}
					partipCntctLst.addAllIdentites(changeableParticipantsList);
					cmsg.addEmailTo(partipCntctLst);
				}
			}

		}
		if (sendToChooserForm != null && isAdmin && businessGroup.getWaitingListEnabled().booleanValue()) {
			if (sendToChooserForm.waitingListChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_ALL)) {
				waitingListContactList = new ContactList(translate("sendtochooser.form.radio.waitings.all"));
				final SecurityGroup waitingList = businessGroup.getWaitingGroup();
				final List<Identity> waitingListIdentities = scrtMngr.getIdentitiesOfSecurityGroup(waitingList);
				waitingListContactList.addAllIdentites(waitingListIdentities);
				cmsg.addEmailTo(waitingListContactList);
			} else {
				if (sendToChooserForm.waitingListChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_CHOOSE)) {
					waitingListContactList = new ContactList(translate("sendtochooser.form.radio.waitings.choose"));
					final SecurityGroup waitingList = businessGroup.getWaitingGroup();
					final List<Identity> waitingListIdentities = scrtMngr.getIdentitiesOfSecurityGroup(waitingList);
					final List<Identity> changeableWaitingListIdentities = scrtMngr.getIdentitiesOfSecurityGroup(waitingList);
					for (final Identity indentity : waitingListIdentities) {
						boolean keyIsSelected = false;
						for (final Long key : sendToChooserForm.getSelectedWaitingKeys()) {
							if (key.equals(indentity.getKey())) {
								keyIsSelected = true;
								break;
							}
						}
						if (!keyIsSelected) {
							changeableWaitingListIdentities.remove(changeableWaitingListIdentities.indexOf(indentity));
						}
					}
					waitingListContactList.addAllIdentites(changeableWaitingListIdentities);
					cmsg.addEmailTo(waitingListContactList);
				}
			}
		}

		cmsg.setSubject(translate("businessgroup.contact.subject", businessGroup.getName()));
		final String restUrl = BusinessControlFactory.getInstance().getAsURIString(getWindowControl().getBusinessControl(), true);
		cmsg.setBodyText(getTranslator().translate("businessgroup.contact.bodytext", new String[] { businessGroup.getName(), restUrl }));

		final CollaborationTools collabTools = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(businessGroup);
		final ContactFormController cofocntrllr = collabTools.createContactFormController(ureq, getWindowControl(), cmsg);
		return cofocntrllr;
	}

	/**
	 * handles the different tree actions
	 * 
	 * @param ureq
	 * @param cmd
	 */
	private void handleTreeActions(final UserRequest ureq, final String cmd) {
		// release edit lock if available
		removeAsListenerAndDispose(bgEditCntrllr);

		final CollaborationTools collabTools = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(businessGroup);

		// dispose current tool controller if available except for IM which should be available even while changing collabtool
		if (collabToolCtr instanceof InstantMessagingGroupChatController) {
			//
		} else {
			removeAsListenerAndDispose(collabToolCtr);
		}
		// init new controller according to user click
		if (ACTIVITY_MENUSELECT_OVERVIEW.equals(cmd)) {
			// root node clicked display overview
			mainPanel.setContent(main);
		} else if (ACTIVITY_MENUSELECT_FORUM.equals(cmd)) {
			addLoggingResourceable(LoggingResourceable.wrap(ORES_TOOLFORUM, OlatResourceableType.forum));
			final SubscriptionContext sc = new SubscriptionContext(businessGroup, INITVIEW_TOOLFORUM);

			WindowControl bwControl = getWindowControl();
			// calculate the new businesscontext for the forum clicked
			final ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLFORUM);
			bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, bwControl);

			collabToolCtr = collabTools.createForumController(ureq, bwControl, isAdmin, ureq.getUserSession().getRoles().isGuestOnly(), sc);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_CHAT.equals(cmd)) {

			if (chatCtr != null) {
				collabToolCtr = chatCtr;
			} else {
				collabToolCtr = collabTools.createChatController(ureq, getWindowControl(), this.businessGroup);
				chatCtr = collabToolCtr;
			}

			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_CALENDAR.equals(cmd)) {
			addLoggingResourceable(LoggingResourceable.wrap(ORES_TOOLCAL, OlatResourceableType.calendar));

			WindowControl bwControl = getWindowControl();
			// calculate the new businesscontext for the forum clicked
			final ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLCAL);
			bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, bwControl);

			collabToolCtr = collabTools.createCalendarController(ureq, bwControl, this.businessGroup, isAdmin);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_INFORMATION.equals(cmd)) {
			collabToolCtr = collabTools.createNewsController(ureq, getWindowControl());
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_FOLDER.equals(cmd)) {
			addLoggingResourceable(LoggingResourceable.wrap(ORES_TOOLFOLDER, OlatResourceableType.sharedFolder));

			final SubscriptionContext sc = new SubscriptionContext(businessGroup, INITVIEW_TOOLFOLDER);

			WindowControl bwControl = getWindowControl();
			// calculate the new businesscontext for the forum clicked
			final ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLFOLDER);
			bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, bwControl);

			collabToolCtr = collabTools.createFolderController(ureq, bwControl, sc);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_MEMBERSLIST.equals(cmd)) {
			doShowMembers(ureq);
		} else if (ACTIVITY_MENUSELECT_CONTACTFORM.equals(cmd)) {
			doContactForm(ureq);
		} else if (ACTIVITY_MENUSELECT_ADMINISTRATION.equals(cmd)) {
			doAdministration(ureq);
		} else if (ACTIVITY_MENUSELECT_SHOW_RESOURCES.equals(cmd)) {
			doShowResources(ureq);
		} else if (ACTIVITY_MENUSELECT_WIKI.equals(cmd)) {
			addLoggingResourceable(LoggingResourceable.wrap(ORES_TOOLWIKI, OlatResourceableType.wiki));
			WindowControl bwControl = getWindowControl();
			// calculate the new businesscontext for the wiki clicked
			final ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLWIKI);
			bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, bwControl);
			ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapWikiOres(ce.getOLATResourceable()));

			collabToolCtr = collabTools.createWikiController(ureq, bwControl);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_PORTFOLIO.equals(cmd)) {
			addLoggingResourceable(LoggingResourceable.wrap(ORES_TOOLPORTFOLIO, OlatResourceableType.portfolio));
			WindowControl bwControl = getWindowControl();
			// calculate the new businesscontext for the wiki clicked
			final ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLPORTFOLIO);
			bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, bwControl);
			ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapPortfolioOres(ce.getOLATResourceable()));

			collabToolCtr = collabTools.createPortfolioController(ureq, bwControl, businessGroup);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		}
	}

	private void doAdministration(final UserRequest ureq) {
		removeAsListenerAndDispose(bgEditCntrllr);
		bgEditCntrllr = BGControllerFactory.getInstance().createEditControllerFor(ureq, getWindowControl(), businessGroup);
		listenTo(bgEditCntrllr);
		mainPanel.setContent(bgEditCntrllr.getInitialComponent());
	}

	private void doContactForm(final UserRequest ureq) {
		if (vc_sendToChooserForm == null) {
			vc_sendToChooserForm = createVelocityContainer("cosendtochooser");
		}
		removeAsListenerAndDispose(sendToChooserForm);
		sendToChooserForm = new BusinessGroupSendToChooserForm(ureq, getWindowControl(), businessGroup, isAdmin);
		listenTo(sendToChooserForm);
		vc_sendToChooserForm.put("vc_sendToChooserForm", sendToChooserForm.getInitialComponent());
		mainPanel.setContent(vc_sendToChooserForm);
	}

	private void doShowMembers(final UserRequest ureq) {
		final VelocityContainer membersVc = createVelocityContainer("ownersandmembers");
		// 1. show owners if configured with Owners
		if (flags.isEnabled(BGConfigFlags.GROUP_OWNERS) && bgpm.showOwners()) {
			removeAsListenerAndDispose(gownersC);
			gownersC = new GroupController(ureq, getWindowControl(), false, true, false, businessGroup.getOwnerGroup());
			listenTo(gownersC);
			membersVc.put("owners", gownersC.getInitialComponent());
			membersVc.contextPut("showOwnerGroups", Boolean.TRUE);
		} else {
			membersVc.contextPut("showOwnerGroups", Boolean.FALSE);
		}
		// 2. show participants if configured with Participants
		if (bgpm.showPartips()) {
			removeAsListenerAndDispose(gparticipantsC);
			gparticipantsC = new GroupController(ureq, getWindowControl(), false, true, false, businessGroup.getPartipiciantGroup());
			listenTo(gparticipantsC);

			membersVc.put("participants", gparticipantsC.getInitialComponent());
			membersVc.contextPut("showPartipsGroups", Boolean.TRUE);
		} else {
			membersVc.contextPut("showPartipsGroups", Boolean.FALSE);
		}
		// 3. show waiting-list if configured
		membersVc.contextPut("hasWaitingList", new Boolean(businessGroup.getWaitingListEnabled()));
		if (bgpm.showWaitingList()) {
			removeAsListenerAndDispose(waitingListController);
			waitingListController = new GroupController(ureq, getWindowControl(), false, true, false, businessGroup.getWaitingGroup());
			listenTo(waitingListController);
			membersVc.put("waitingList", waitingListController.getInitialComponent());
			membersVc.contextPut("showWaitingList", Boolean.TRUE);
		} else {
			membersVc.contextPut("showWaitingList", Boolean.FALSE);
		}
		mainPanel.setContent(membersVc);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {

		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_CLOSED, getClass());

		if (chatCtr != null) {

			final InstantMessagingGroupChatController chat = (InstantMessagingGroupChatController) chatCtr;

			if (chat.isChatWindowOpen()) {
				chat.close();
				getWindowControl().getWindowBackOffice().sendCommandTo(chat.getCloseCommand());
			}

			removeAsListenerAndDispose(chatCtr);
		}

		CoordinatorManager.getInstance().getCoordinator().getEventBus().deregisterFor(this, this.businessGroup);

		userSession.getSingleUserEventCenter().deregisterFor(this, assessmentEventOres);
	}

	/**
	 * @see org.olat.core.util.event.GenericEventListener#event(org.olat.core.gui.control.Event)
	 */
	public void event(final Event event) {
		if (event instanceof OLATResourceableJustBeforeDeletedEvent) {
			final OLATResourceableJustBeforeDeletedEvent delEvent = (OLATResourceableJustBeforeDeletedEvent) event;
			if (!delEvent.targetEquals(businessGroup)) { throw new AssertException("receiving a delete event for a olatres we never registered for!!!:"
					+ delEvent.getDerivedOres()); }
			dispose();

		} else if (event instanceof BusinessGroupModifiedEvent) {
			final BusinessGroupModifiedEvent bgmfe = (BusinessGroupModifiedEvent) event;
			if (event.getCommand().equals(BusinessGroupModifiedEvent.CONFIGURATION_MODIFIED_EVENT)) {
				// reset business group property manager
				this.bgpm = new BusinessGroupPropertyManager(this.businessGroup);
				// update reference to update business group object
				this.businessGroup = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(this.businessGroup);
				main.contextPut("BuddyGroup", this.businessGroup);
				final TreeModel trMdl = buildTreeModel();
				bgTree.setTreeModel(trMdl);
				if (bgEditCntrllr == null) {
					// change didn't origin by our own edit controller
					showInfo(translate("grouprun.configurationchanged"));
					bgTree.setSelectedNodeId(trMdl.getRootNode().getIdent());
					mainPanel.setContent(main);
				} else {
					// Activate edit menu item
					bgTree.setSelectedNodeId(ACTIVITY_MENUSELECT_ADMINISTRATION);
				}
			} else if (bgmfe.wasMyselfRemoved(identity)) {
				// nothing more here!! The message will be created and displayed upon disposing
				dispose();// disposed message controller will be set
			}
		} else if (event instanceof AssessmentEvent) {
			if (((AssessmentEvent) event).getEventType().equals(AssessmentEvent.TYPE.STARTED)) {
				groupRunDisabled = true;
			} else if (((AssessmentEvent) event).getEventType().equals(AssessmentEvent.TYPE.STOPPED)) {
				groupRunDisabled = false;
			}
		}
	}

	private void doShowResources(final UserRequest ureq) {
		// always refresh data model, maybe it has changed
		final RepositoryTableModel repoTableModel = new RepositoryTableModel(resourceTrans);
		final BGContextManager contextManager = BGContextManagerImpl.getInstance();
		final List<RepositoryEntry> repoTableModelEntries = contextManager.findRepositoryEntriesForBGContext(businessGroup.getGroupContext());
		repoTableModel.setObjects(repoTableModelEntries);
		// init table controller only once
		if (resourcesCtr == null) {
			final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
			tableConfig.setTableEmptyMessage(translate("resources.noresources"));
			// removeAsListenerAndDispose(resourcesCtr);
			resourcesCtr = new TableController(tableConfig, ureq, getWindowControl(), resourceTrans);
			listenTo(resourcesCtr);

			resourcesVC = createVelocityContainer("resources");
			repoTableModel.addColumnDescriptors(resourcesCtr, translate("resources.launch"), false);
			resourcesVC.put("resources", resourcesCtr.getInitialComponent());
		}
		// add table model to table
		resourcesCtr.setTableDataModel(repoTableModel);
		mainPanel.setContent(resourcesVC);
	}

	/**
	 * Activates the administration menu item. Make sure you have the rights to do this, otherwhise this will throw a nullpointer exception
	 * 
	 * @param ureq
	 */
	public void activateAdministrationMode(final UserRequest ureq) {
		doAdministration(ureq);
		bgTree.setSelectedNodeId(adminNodeId);
	}

	/**
	 * @return The menu tree model
	 */
	private TreeModel buildTreeModel() {
		GenericTreeNode gtnChild, root;

		final GenericTreeModel gtm = new GenericTreeModel();
		root = new GenericTreeNode();
		root.setTitle(businessGroup.getName());
		root.setUserObject(ACTIVITY_MENUSELECT_OVERVIEW);
		root.setAltText(translate("menutree.top.alt") + " " + businessGroup.getName());
		root.setIconCssClass("b_group_icon");
		gtm.setRootNode(root);

		final CollaborationTools collabTools = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(this.businessGroup);

		if (collabTools.isToolEnabled(CollaborationTools.TOOL_NEWS)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.news"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_INFORMATION);
			gtnChild.setAltText(translate("menutree.news.alt"));
			gtnChild.setIconCssClass("o_news_icon");
			root.addChild(gtnChild);
		}

		if (collabTools.isToolEnabled(CollaborationTools.TOOL_CALENDAR)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.calendar"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_CALENDAR);
			gtnChild.setAltText(translate("menutree.calendar.alt"));
			gtnChild.setIconCssClass("o_calendar_icon");
			root.addChild(gtnChild);
			nodeCal = gtnChild;
		}

		if (flags.isEnabled(BGConfigFlags.SHOW_RESOURCES)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.resources"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_SHOW_RESOURCES);
			gtnChild.setAltText(translate("menutree.resources.alt"));
			gtnChild.setIconCssClass("o_course_icon");
			root.addChild(gtnChild);
		}

		if ((flags.isEnabled(BGConfigFlags.GROUP_OWNERS) && bgpm.showOwners()) || bgpm.showPartips()) {
			// either owners or participants, or both are visible
			// otherwise the node is not visible
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.members"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_MEMBERSLIST);
			gtnChild.setAltText(translate("menutree.members.alt"));
			gtnChild.setIconCssClass("b_group_icon");
			root.addChild(gtnChild);
		}

		if (collabTools.isToolEnabled(CollaborationTools.TOOL_CONTACT)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.contactform"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_CONTACTFORM);
			gtnChild.setAltText(translate("menutree.contactform.alt"));
			gtnChild.setIconCssClass("o_co_icon");
			root.addChild(gtnChild);
		}

		if (collabTools.isToolEnabled(CollaborationTools.TOOL_FOLDER)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.folder"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_FOLDER);
			gtnChild.setAltText(translate("menutree.folder.alt"));
			gtnChild.setIconCssClass("o_bc_icon");
			root.addChild(gtnChild);
			nodeFolder = gtnChild;
		}

		if (collabTools.isToolEnabled(CollaborationTools.TOOL_FORUM)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.forum"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_FORUM);
			gtnChild.setAltText(translate("menutree.forum.alt"));
			gtnChild.setIconCssClass("o_fo_icon");
			root.addChild(gtnChild);
			nodeForum = gtnChild;
		}

		if (InstantMessagingModule.isEnabled() && collabTools.isToolEnabled(CollaborationTools.TOOL_CHAT)
				&& (!businessGroup.getType().equals(BusinessGroup.TYPE_LEARNINGROUP) || InstantMessagingModule.isSyncLearningGroups() // whether LearningGroups can have
																																		// chat or not
				)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.chat"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_CHAT);
			gtnChild.setAltText(translate("menutree.chat.alt"));
			gtnChild.setIconCssClass("o_chat_icon");
			root.addChild(gtnChild);
		}

		if (collabTools.isToolEnabled(CollaborationTools.TOOL_WIKI)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.wiki"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_WIKI);
			gtnChild.setAltText(translate("menutree.wiki.alt"));
			gtnChild.setIconCssClass("o_wiki_icon");
			root.addChild(gtnChild);
			nodeWiki = gtnChild;
		}

		if (collabTools.isToolEnabled(CollaborationTools.TOOL_PORTFOLIO)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.portfolio"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_PORTFOLIO);
			gtnChild.setAltText(translate("menutree.portfolio.alt"));
			gtnChild.setIconCssClass("o_ep_icon");
			root.addChild(gtnChild);
			nodePortfolio = gtnChild;
		}

		if (isAdmin) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.administration"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_ADMINISTRATION);
			gtnChild.setIdent(ACTIVITY_MENUSELECT_ADMINISTRATION);
			gtnChild.setAltText(translate("menutree.administration.alt"));
			gtnChild.setIconCssClass("o_admin_icon");
			root.addChild(gtnChild);
			adminNodeId = gtnChild.getIdent();
		}

		return gtm;
	}

}