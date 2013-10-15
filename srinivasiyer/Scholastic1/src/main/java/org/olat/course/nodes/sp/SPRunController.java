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

package org.olat.course.nodes.sp;

import java.util.Map;

import org.olat.core.commons.controllers.linkchooser.CustomLinkTreeModel;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.fullWebApp.popup.BaseFullWebappPopupLayoutFactory;
import org.olat.core.commons.modules.singlepage.SinglePageController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.htmlsite.NewInlineUriEvent;
import org.olat.core.gui.components.htmlsite.OlatCmdEvent;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.gui.control.generic.clone.CloneController;
import org.olat.core.gui.control.generic.clone.CloneLayoutControllerCreatorCallback;
import org.olat.core.gui.control.generic.clone.CloneableController;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.groupsandrights.CourseRights;
import org.olat.course.nodes.SPCourseNode;
import org.olat.course.nodes.TitledWrapperHelper;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseInternalLinkTreeModel;
import org.olat.modules.ModuleConfiguration;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description:<br>
 * Displays a regular HTML page either in an iframe or integrated within OLAT. If the user is course administrator or has course editor rights an edit links allows the
 * editing of the page.
 * <p>
 * EVENTS: to listening controllers: - OlatCmdEvent (which has to be accepted by calling accept() on the event) A run controller for single page course nodes Initial
 * Date: Oct 12, 2004
 * 
 * @author Felix Jost
 */
public class SPRunController extends BasicController {

	private static final String KEY_CURRENT_URI = "cururi";

	private final SPCourseNode courseNode;
	private final Panel main;
	private SinglePageController spCtr;
	private final ModuleConfiguration config;

	private final VFSContainer courseFolderContainer;
	private final String fileName;
	private final Map tempstorage;

	private final boolean hasEditRights;
	private CustomLinkTreeModel linkTreeModel;
	private CloneController cloneC;

	private final UserCourseEnvironment userCourseEnv;

	private static final String[] EDITABLE_TYPES = new String[] { "html", "htm", "xml", "xhtml" };

	/**
	 * Constructor for single page run controller
	 * 
	 * @param wControl
	 * @param ureq
	 * @param userCourseEnv
	 * @param courseNode
	 * @param courseFolderPath The course folder which contains the single page html file
	 */
	public SPRunController(final WindowControl wControl, final UserRequest ureq, final Map tempstorage, final UserCourseEnvironment userCourseEnv,
			final SPCourseNode courseNode, final VFSContainer courseFolderContainer) {
		super(ureq, wControl);
		this.tempstorage = tempstorage;
		this.courseNode = courseNode;
		this.config = courseNode.getModuleConfiguration();
		this.userCourseEnv = userCourseEnv;

		addLoggingResourceable(LoggingResourceable.wrap(courseNode));

		// set up single page init parameters
		this.fileName = (String) config.get(SPEditController.CONFIG_KEY_FILE);
		if (fileName == null) { throw new AssertException("bad configuration at lauchtime: fileName cannot be null in SinglePage!"); }
		this.courseFolderContainer = courseFolderContainer;

		final CourseGroupManager cgm = userCourseEnv.getCourseEnvironment().getCourseGroupManager();
		hasEditRights = isFileTypeEditable(fileName)
				&& (cgm.isIdentityCourseAdministrator(ureq.getIdentity()) || cgm.hasRight(ureq.getIdentity(), CourseRights.RIGHT_COURSEEDITOR));

		if (hasEditRights) {
			linkTreeModel = new CourseInternalLinkTreeModel(userCourseEnv.getCourseEnvironment().getRunStructure().getRootNode());
		}

		// init main panel and do start page or direct launch
		main = new Panel("sprunmain");
		doInlineIntegration(ureq, hasEditRights);
		putInitialPanel(main);
	}

	private boolean isFileTypeEditable(final String filename) {
		final String name = filename.toLowerCase();
		for (int i = 0; i < EDITABLE_TYPES.length; i++) {
			if (name.endsWith("." + EDITABLE_TYPES[i])) { return true; }
		}
		return false;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == spCtr) {
			if (event instanceof OlatCmdEvent) {
				// refire to listening controllers
				fireEvent(ureq, event);
			} else if (event instanceof NewInlineUriEvent) {
				final NewInlineUriEvent nue = (NewInlineUriEvent) event;
				tempstorage.put(KEY_CURRENT_URI, nue.getNewUri());
			}
		}
	}

	private void doInlineIntegration(final UserRequest ureq, final boolean hasEditRightsTo) {
		final Boolean allowRelativeLinks = config.getBooleanEntry(SPEditController.CONFIG_KEY_ALLOW_RELATIVE_LINKS);
		// create the possibility to float
		final OLATResourceable ores = OresHelper.createOLATResourceableInstance(ICourse.class, userCourseEnv.getCourseEnvironment().getCourseResourceableId());
		spCtr = new SinglePageController(ureq, getWindowControl(), courseFolderContainer, fileName, null, allowRelativeLinks.booleanValue(), ores);
		spCtr.setAllowDownload(true);

		// only for inline integration: register for controller event to forward a olatcmd to the course,
		// and also to remember latest position in the script
		this.listenTo(spCtr);
		// enable edit mode if user has the according rights
		if (hasEditRightsTo) {
			spCtr.allowPageEditing();
			// set the link tree model to internal for the HTML editor
			if (this.linkTreeModel != null) {
				spCtr.setInternalLinkTreeModel(linkTreeModel);
			}
		}

		// create clone wrapper layout
		final CloneLayoutControllerCreatorCallback clccc = new CloneLayoutControllerCreatorCallback() {
			@Override
			public ControllerCreator createLayoutControllerCreator(final UserRequest ureq, final ControllerCreator contentControllerCreator) {
				return BaseFullWebappPopupLayoutFactory.createAuthMinimalPopupLayout(ureq, new ControllerCreator() {
					@Override
					@SuppressWarnings("synthetic-access")
					public Controller createController(final UserRequest lureq, final WindowControl lwControl) {
						// Wrap in column layout, popup window needs a layout controller
						final Controller ctr = contentControllerCreator.createController(lureq, lwControl);
						final LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(lureq, lwControl, null, null, ctr.getInitialComponent(), null);
						layoutCtr.setCustomCSS(CourseFactory.getCustomCourseCss(lureq.getUserSession(), userCourseEnv.getCourseEnvironment()));
						// Controller titledCtrl = TitledWrapperHelper.getWrapper(lureq, lwControl, ctr, courseNode, "o_sp_icon");
						layoutCtr.addDisposableChildController(ctr);
						return layoutCtr;
					}
				});
			}
		};

		final Controller ctrl = TitledWrapperHelper.getWrapper(ureq, getWindowControl(), spCtr, courseNode, "o_sp_icon");
		if (ctrl instanceof CloneableController) {
			cloneC = new CloneController(ureq, getWindowControl(), (CloneableController) ctrl, clccc);
			listenTo(cloneC);
			main.setContent(cloneC.getInitialComponent());
		} else {
			throw new AssertException("Controller must be cloneable");
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// child controller registered with listenTo gets disposed in BasicController
	}

}