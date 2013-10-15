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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Formatter;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionExpression;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.fo.FOCourseNodeEditController;
import org.olat.course.nodes.fo.FOCourseNodeRunController;
import org.olat.course.nodes.fo.FOPeekviewController;
import org.olat.course.nodes.fo.FOPreviewController;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.properties.PersistingCoursePropertyManager;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.ForumCallback;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.archiver.ForumArchiveManager;
import org.olat.modules.fo.archiver.formatters.ForumRTFFormatter;
import org.olat.properties.Property;
import org.olat.repository.RepositoryEntry;
import org.olat.testutils.codepoints.server.Codepoint;

/**
 * Initial Date: Feb 9, 2004
 * 
 * @author Mike Stock Comment:
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class FOCourseNode extends GenericCourseNode {

	private static final String PACKAGE_FO = Util.getPackageName(FOCourseNodeRunController.class);
	private static final String TYPE = "fo";
	private Condition preConditionReader, preConditionPoster, preConditionModerator;
	// null means no precondition / always accessible
	public static final String FORUM_KEY = "forumKey";

	/**
	 * Default constructor to create a forum course node
	 */
	public FOCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
		// restrict moderator access to course admins and course coaches
		preConditionModerator = getPreConditionModerator();
		preConditionModerator.setEasyModeCoachesAndAdmins(true);
		preConditionModerator.setConditionExpression(preConditionModerator.getConditionFromEasyModeConfiguration());
		preConditionModerator.setExpertMode(false);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(final UserRequest ureq, final WindowControl wControl, final ICourse course, final UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		final FOCourseNodeEditController childTabCntrllr = new FOCourseNodeEditController(ureq, wControl, this, course, euce);
		final CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, course.getCourseEnvironment().getCourseGroupManager(), euce,
				childTabCntrllr);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(final UserRequest ureq, WindowControl wControl, final UserCourseEnvironment userCourseEnv,
			final NodeEvaluation ne, final String nodecmd) {

		final Forum theForum = loadOrCreateForum(userCourseEnv);
		final boolean isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		final boolean isGuestOnly = ureq.getUserSession().getRoles().isGuestOnly();
		// Add message id to business path if nodemcd is available
		if (nodecmd != null) {
			try {
				final Long messageId = Long.valueOf(nodecmd);
				final BusinessControlFactory bcf = BusinessControlFactory.getInstance();
				final BusinessControl businessControl = bcf.createFromString("[Message:" + messageId + "]");
				wControl = bcf.createBusinessWindowControl(businessControl, wControl);
			} catch (final NumberFormatException e) {
				// ups, nodecmd is not a message, what the heck is it then?
				Tracing.createLoggerFor(this.getClass()).warn("Could not create message ID from given nodemcd::" + nodecmd, e);
			}
		}
		// Create subscription context and run controller
		final SubscriptionContext forumSubContext = CourseModule.createSubscriptionContext(userCourseEnv.getCourseEnvironment(), this);
		final FOCourseNodeRunController forumC = new FOCourseNodeRunController(ureq, userCourseEnv, wControl, theForum, new ForumNodeForumCallback(ne, isOlatAdmin,
				isGuestOnly, forumSubContext), this);
		return new NodeRunConstructionResult(forumC);
	}

	/**
	 * Private helper method to load the forum from the configuration or create on if it does not yet exist
	 * 
	 * @param userCourseEnv
	 * @return the loaded forum
	 */
	private Forum loadOrCreateForum(final UserCourseEnvironment userCourseEnv) {
		updateModuleConfigDefaults(false);
		final ForumManager fom = ForumManager.getInstance();
		final CoursePropertyManager cpm = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		final CourseNode thisCourseNode = this;
		Forum theForum = null;

		Codepoint.codepoint(FOCourseNode.class, "findCourseNodeProperty");
		final Property forumKeyProp = cpm.findCourseNodeProperty(thisCourseNode, null, null, FORUM_KEY);
		// System.out.println("System.out.println - findCourseNodeProperty");
		if (forumKeyProp != null) {
			// Forum does already exist, load forum with key from properties
			final Long forumKey = forumKeyProp.getLongValue();
			theForum = fom.loadForum(forumKey);
			if (theForum == null) { throw new OLATRuntimeException(FOCourseNode.class, "Tried to load forum with key " + forumKey.longValue() + " in course "
					+ userCourseEnv.getCourseEnvironment().getCourseResourceableId() + " for node " + thisCourseNode.getIdent()
					+ " as defined in course node property but forum manager could not load forum.", null); }
		} else {
			// creates resourceable from FOCourseNode.class and the current node id as key
			final OLATResourceable courseNodeResourceable = OresHelper.createOLATResourceableInstance(FOCourseNode.class, new Long(this.getIdent()));
			Codepoint.codepoint(FOCourseNode.class, "beforeDoInSync");
			// System.out.println("System.out.println - beforeDoInSync");
			// o_clusterOK by:ld
			theForum = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(courseNodeResourceable, new SyncerCallback<Forum>() {
				@Override
				public Forum execute() {
					Forum forum = null;
					Long forumKey;
					Codepoint.codepoint(FOCourseNode.class, "doInSync");
					// System.out.println("Codepoint - doInSync");
					Property forumKeyProperty = cpm.findCourseNodeProperty(thisCourseNode, null, null, FORUM_KEY);
					if (forumKeyProperty == null) {
						// First call of forum, create new forum and save forum key as property
						forum = fom.addAForum();
						forumKey = forum.getKey();
						forumKeyProperty = cpm.createCourseNodePropertyInstance(thisCourseNode, null, null, FORUM_KEY, null, forumKey, null, null);
						cpm.saveProperty(forumKeyProperty);
						// System.out.println("Forum added");
					} else {
						// Forum does already exist, load forum with key from properties
						forumKey = forumKeyProperty.getLongValue();
						forum = fom.loadForum(forumKey);
						if (forum == null) { throw new OLATRuntimeException(FOCourseNode.class, "Tried to load forum with key " + forumKey.longValue() + " in course "
								+ userCourseEnv.getCourseEnvironment().getCourseResourceableId() + " for node " + thisCourseNode.getIdent()
								+ " as defined in course node property but forum manager could not load forum.", null); }
					}
					// System.out.println("Forum already exists");
					return forum;
				}
			});
		}
		return theForum;
	}

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
	 * implementation of the previewController for forumnode
	 * 
	 * @see org.olat.course.nodes.CourseNode#createPreviewController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPreviewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		return new FOPreviewController(ureq, wControl, this, ne);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPeekViewRunController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPeekViewRunController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final NodeEvaluation ne) {
		if (ne.isAtLeastOneAccessible()) {
			// Create a forum peekview controller that shows the latest two messages
			final Forum theForum = loadOrCreateForum(userCourseEnv);
			final Controller peekViewController = new FOPeekviewController(ureq, wControl, theForum, ne.getCourseNode().getIdent(), 2);
			return peekViewController;
		} else {
			// use standard peekview
			return super.createPeekViewRunController(ureq, wControl, userCourseEnv, ne);
		}
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
		oneClickStatusCache = null;
		// only here we know which translator to take for translating condition error messages
		final String translatorStr = Util.getPackageName(FOCourseNodeEditController.class);
		final List sds = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
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
	 * @see org.olat.course.nodes.CourseNode#archiveNodeData(java.util.Locale, org.olat.course.ICourse, java.io.File, java.lang.String)
	 */
	@Override
	public boolean archiveNodeData(final Locale locale, final ICourse course, final File exportDirectory, final String charset) {
		boolean dataFound = false;
		final CoursePropertyManager cpm = course.getCourseEnvironment().getCoursePropertyManager();
		final Property forumKeyProperty = cpm.findCourseNodeProperty(this, null, null, FORUM_KEY);
		if (forumKeyProperty != null) {
			final Long forumKey = forumKeyProperty.getLongValue();
			if (ForumManager.getInstance().getMessagesByForumID(forumKey).size() > 0) {
				String forumName = Formatter.makeStringFilesystemSave(this.getShortTitle());

				// append export timestamp to avoid overwriting previous export
				final Date tmp = new Date(System.currentTimeMillis());
				final java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss_SSS");
				forumName += "_" + formatter.format(tmp);

				VFSContainer container = new LocalFolderImpl(exportDirectory);
				VFSItem vfsItem = container.resolve(forumName);

				if (vfsItem == null || !(vfsItem instanceof VFSContainer)) {
					vfsItem = container.createChildContainer(forumName);
				}
				container = (VFSContainer) vfsItem;

				final ForumRTFFormatter rtff = new ForumRTFFormatter(container, false);
				final ForumArchiveManager fam = ForumArchiveManager.getInstance();
				fam.applyFormatter(rtff, forumKey.longValue(), null);
				dataFound = true;
			}
		}
		return dataFound;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#informOnDelete(org.olat.core.gui.UserRequest, org.olat.course.ICourse)
	 */
	@Override
	public String informOnDelete(final Locale locale, final ICourse course) {
		final CoursePropertyManager cpm = PersistingCoursePropertyManager.getInstance(course);
		final Property forumKeyProperty = cpm.findCourseNodeProperty(this, null, null, FORUM_KEY);
		if (forumKeyProperty == null) { return null; // no forum created yet
		}
		return new PackageTranslator(PACKAGE_FO, locale).translate("warn.forumdelete");
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#cleanupOnDelete(org.olat.course.ICourse)
	 */
	@Override
	public void cleanupOnDelete(final ICourse course) {
		// mark the subscription to this node as deleted
		final SubscriptionContext forumSubContext = CourseModule.createTechnicalSubscriptionContext(course.getCourseEnvironment(), this);
		NotificationsManager.getInstance().delete(forumSubContext);

		// delete the forum, if there is one (is created on demand only)
		final CoursePropertyManager cpm = PersistingCoursePropertyManager.getInstance(course);
		final Property forumKeyProperty = cpm.findCourseNodeProperty(this, null, null, FORUM_KEY);
		if (forumKeyProperty == null) { return; // no forum created yet
		}
		final Long forumKey = forumKeyProperty.getLongValue();
		ForumManager.getInstance().deleteForum(forumKey); // delete the forum
		cpm.deleteProperty(forumKeyProperty); // delete the property
	}

	/**
	 * Update the module configuration to have all mandatory configuration flags set to usefull default values
	 * 
	 * @param isNewNode true: an initial configuration is set; false: upgrading from previous node configuration version, set default to maintain previous behaviour
	 */
	@Override
	public void updateModuleConfigDefaults(final boolean isNewNode) {
		final ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode || config.getConfigurationVersion() < 2) {
			// use defaults for new course building blocks
			config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, Boolean.FALSE.booleanValue());
			config.setConfigurationVersion(2);
		}
		// else node is up-to-date - nothing to do
		config.remove(NodeEditController.CONFIG_INTEGRATION);
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
		String coS = getPreConditionModerator().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			final ConditionExpression ce = new ConditionExpression(getPreConditionModerator().getConditionId());
			ce.setExpressionString(getPreConditionModerator().getConditionExpression());
			retVal.add(ce);
		}
		coS = getPreConditionPoster().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			final ConditionExpression ce = new ConditionExpression(getPreConditionPoster().getConditionId());
			ce.setExpressionString(getPreConditionPoster().getConditionExpression());
			retVal.add(ce);
		}
		coS = getPreConditionReader().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			final ConditionExpression ce = new ConditionExpression(getPreConditionReader().getConditionId());
			ce.setExpressionString(getPreConditionReader().getConditionExpression());
			retVal.add(ce);
		}
		//
		return retVal;
	}

}

/**
 * Description:<br>
 * ForumCallback implementation.
 */
class ForumNodeForumCallback implements ForumCallback {

	private final NodeEvaluation ne;
	private final boolean isOlatAdmin;
	private final boolean isGuestOnly;
	private final SubscriptionContext subscriptionContext;

	/**
	 * @param ne the nodeevaluation for this coursenode
	 * @param isOlatAdmin true if the user is olat-admin
	 * @param isGuestOnly true if the user is olat-guest
	 * @param subscriptionContext
	 */
	public ForumNodeForumCallback(final NodeEvaluation ne, final boolean isOlatAdmin, final boolean isGuestOnly, final SubscriptionContext subscriptionContext) {
		this.ne = ne;
		this.isOlatAdmin = isOlatAdmin;
		this.isGuestOnly = isGuestOnly;
		this.subscriptionContext = subscriptionContext;
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#mayOpenNewThread()
	 */
	@Override
	public boolean mayOpenNewThread() {
		if (isGuestOnly) { return false; }
		return ne.isCapabilityAccessible("poster") || ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#mayReplyMessage()
	 */
	@Override
	public boolean mayReplyMessage() {
		if (isGuestOnly) { return false; }
		return ne.isCapabilityAccessible("poster") || ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#mayEditMessageAsModerator()
	 */
	@Override
	public boolean mayEditMessageAsModerator() {
		if (isGuestOnly) { return false; }
		return ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#mayDeleteMessageAsModerator()
	 */
	@Override
	public boolean mayDeleteMessageAsModerator() {
		if (isGuestOnly) { return false; }
		return ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#mayArchiveForum()
	 */
	@Override
	public boolean mayArchiveForum() {
		if (isGuestOnly) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#mayFilterForUser()
	 */
	@Override
	public boolean mayFilterForUser() {
		if (isGuestOnly) { return false; }
		return ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#getSubscriptionContext()
	 */
	@Override
	public SubscriptionContext getSubscriptionContext() {
		// SubscriptionContext sc = new SubscriptionContext("coourseli", new
		// Long(123), "subident", "Einfuehrung in die Blabla", "Knoten gugus");
		// do not offer subscription to forums for guests
		return (isGuestOnly ? null : subscriptionContext);
	}

}
