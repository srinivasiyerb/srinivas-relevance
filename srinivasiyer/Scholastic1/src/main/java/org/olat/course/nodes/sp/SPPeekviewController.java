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

import org.olat.core.commons.modules.singlepage.SinglePageController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.download.DownloadComponent;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;

/**
 * Description:<br>
 * This is the implementatino of the peekview for the sp course node. Files of type html, htm or xhtml are displayed in the peekview with a 75% scaling. For other types
 * only a download link is displayed.
 * <P>
 * Initial Date: 09.12.2009 <br>
 * 
 * @author gnaegi
 */
public class SPPeekviewController extends BasicController {

	/**
	 * Constructor for the sp peek view
	 * 
	 * @param ureq
	 * @param wControl
	 * @param userCourseEnv
	 * @param config
	 * @param ores
	 */
	public SPPeekviewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv, final ModuleConfiguration config,
			final OLATResourceable ores) {
		super(ureq, wControl);
		// just display the page
		final String file = config.getStringValue(SPEditController.CONFIG_KEY_FILE);
		final String fileLC = file.toLowerCase();
		if (fileLC.endsWith(".html") || fileLC.endsWith(".htm") || fileLC.endsWith(".xhtml")) {
			// Render normal view but scaled down to 75%
			final SinglePageController spController = new SinglePageController(ureq, wControl, userCourseEnv.getCourseEnvironment().getCourseFolderContainer(), file,
					null, config.getBooleanEntry(SPEditController.CONFIG_KEY_ALLOW_RELATIVE_LINKS), ores);
			// but add scaling to fit preview into minimized space
			spController.setScaleFactorAndHeight(0.75f, 400, true);
			listenTo(spController);
			putInitialPanel(spController.getInitialComponent());
		} else {
			// Render a download link for file
			final VFSContainer courseFolder = userCourseEnv.getCourseEnvironment().getCourseFolderContainer();
			final VFSItem downloadItem = courseFolder.resolve(file);
			if (file != null && downloadItem instanceof VFSLeaf) {
				final DownloadComponent downloadComp = new DownloadComponent("downloadComp", (VFSLeaf) downloadItem);
				final VelocityContainer peekviewVC = createVelocityContainer("peekview");
				peekviewVC.put("downloadComp", downloadComp);
				putInitialPanel(peekviewVC);
			} else {
				// boy, can't find file, use an empty panel
				putInitialPanel(new Panel("empty"));
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// autodisposed by basic controller
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		// no events to catch
	}

}
