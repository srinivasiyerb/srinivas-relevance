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

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.util.Formatter;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.CourseNode;
import org.olat.course.tree.CourseEditorTreeNode;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.controllers.IAddController;
import org.olat.repository.controllers.RepositoryAddCallback;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;

/**
 * Description:<BR/>
 * Implementation of the repository add controller for OLAT courses
 * <P/>
 * Initial Date: Oct 12, 2004
 * 
 * @author gnaegi
 */
public class CreateNewCourseController extends BasicController implements IAddController {

	// private static final String PACKAGE_REPOSITORY = Util.getPackageName(RepositoryManager.class);
	private final OLATResource newCourseResource;
	private ICourse course;// o_clusterOK: creation process

	/**
	 * Constructor for the add course controller
	 * 
	 * @param addCallback
	 * @param ureq
	 */
	public CreateNewCourseController(final RepositoryAddCallback addCallback, final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		setBasePackage(RepositoryManager.class);

		// do prepare course now
		newCourseResource = OLATResourceManager.getInstance().createOLATResourceInstance(CourseModule.class);
		if (addCallback != null) {
			addCallback.setResourceable(newCourseResource);
			addCallback.setDisplayName(translate(newCourseResource.getResourceableTypeName()));
			addCallback.setResourceName("-");
			addCallback.finished(ureq);
		}
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#getTransactionComponent()
	 */
	@Override
	public Component getTransactionComponent() {
		return getInitialComponent();
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#transactionFinishBeforeCreate()
	 */
	@Override
	public boolean transactionFinishBeforeCreate() {
		// Create course and persist course resourceable.
		course = CourseFactory.createEmptyCourse(newCourseResource, "New Course", "New Course", "");
		// initialize course groupmanagement
		final CourseGroupManager cgm = course.getCourseEnvironment().getCourseGroupManager();
		cgm.createCourseGroupmanagement(course.getResourceableId().toString());

		return true;
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#transactionAborted()
	 */
	@Override
	public void transactionAborted() {
		// Nothing to do here... no course has been created yet.
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// nothing to listen to
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		// nothing to listen to
	}

	/**
	 * @see org.olat.repository.controllers.IAddController#repositoryEntryCreated(org.olat.repository.RepositoryEntry)
	 */
	@Override
	public void repositoryEntryCreated(final RepositoryEntry re) {
		// Create course admin policy for owner group of repository entry
		// -> All owners of repository entries are course admins
		final BaseSecurity secMgr = BaseSecurityManager.getInstance();
		secMgr.createAndPersistPolicy(re.getOwnerGroup(), Constants.PERMISSION_ADMIN, re.getOlatResource());
		// set root node title

		course = CourseFactory.openCourseEditSession(re.getOlatResource().getResourceableId());
		final String displayName = re.getDisplayname();
		course.getRunStructure().getRootNode().setShortTitle(Formatter.truncateOnly(displayName, 25)); // do not use truncate!
		course.getRunStructure().getRootNode().setLongTitle(displayName);

		final CourseNode rootNode = ((CourseEditorTreeNode) course.getEditorTreeModel().getRootNode()).getCourseNode();
		rootNode.setShortTitle(Formatter.truncateOnly(displayName, 25)); // do not use truncate!
		rootNode.setLongTitle(displayName);

		CourseFactory.saveCourse(course.getResourceableId());
		CourseFactory.closeCourseEditSession(course.getResourceableId(), true);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// nothing to do here
	}
}
