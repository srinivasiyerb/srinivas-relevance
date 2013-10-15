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

package org.olat.course.archiver;

import java.io.File;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.ExportUtil;
import org.olat.core.util.Util;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.user.UserManager;

/**
 * Description: Course-Results-Archiver using ScoreAccountingHelper.class Initial Date: Sep 23, 2004
 * 
 * @author gnaegi
 */
public class ScoreAccountingArchiveController extends DefaultController {
	private static final String PACKAGE = Util.getPackageName(ScoreAccountingArchiveController.class);
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(PACKAGE);

	private static final String CMD_START = "cmd.start";

	private final OLATResourceable ores;
	private final Panel myPanel;
	private final VelocityContainer myContent;
	private VelocityContainer vcFeedback;
	private final Translator t;
	private final Link startButton;

	/**
	 * Constructor for the score accounting archive controller
	 * 
	 * @param ureq
	 * @param course
	 */
	public ScoreAccountingArchiveController(final UserRequest ureq, final WindowControl wControl, final OLATResourceable ores) {
		super(wControl);
		this.ores = ores;

		this.t = new PackageTranslator(PACKAGE, ureq.getLocale());

		this.myPanel = new Panel("myPanel");
		myPanel.addListener(this);

		myContent = new VelocityContainer("myContent", VELOCITY_ROOT + "/start.html", t, this);
		startButton = LinkFactory.createButtonSmall("cmd.start", myContent, this);

		myPanel.setContent(myContent);
		setInitialComponent(myPanel);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == startButton) {
			final ICourse course = CourseFactory.loadCourse(ores);
			final List users = ScoreAccountingHelper.loadUsers(course.getCourseEnvironment());
			final List nodes = ScoreAccountingHelper.loadAssessableNodes(course.getCourseEnvironment());

			final String result = ScoreAccountingHelper.createCourseResultsOverviewTable(users, nodes, course, ureq.getLocale());

			final String courseTitle = course.getCourseTitle();

			final String fileName = ExportUtil.createFileNameWithTimeStamp(courseTitle, "xls");
			// location for data export
			final File exportDirectory = CourseFactory.getOrCreateDataExportDirectory(ureq.getIdentity(), courseTitle);
			// the user's charset
			final UserManager um = UserManager.getInstance();
			final String charset = um.getUserCharset(ureq.getIdentity());

			ExportUtil.writeContentToFile(fileName, result, exportDirectory, charset);

			vcFeedback = new VelocityContainer("feedback", VELOCITY_ROOT + "/feedback.html", t, this);
			vcFeedback.contextPut("body", vcFeedback.getTranslator().translate("course.res.feedback", new String[] { fileName }));
			myPanel.setContent(vcFeedback);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// nothing to dispose
	}

}