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
package org.olat.modules.cp;

import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsPreviewController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayout3ColumnsController;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.controller.OLATResourceableListeningWrapperController;
import org.olat.core.util.vfs.VFSContainer;

/**
 * Description:<br>
 * The CPUIFactory provides methods to create content packaging display controllers for various setups.
 * <P>
 * Initial Date: 08.10.2007 <br>
 * 
 * @author Felix Jost, http://www.goodsolutions.ch
 * @author Florian Gnägi, http://www.frentix.com
 */
public class CPUIFactory {
	private static CPUIFactory INSTANCE = new CPUIFactory();

	private CPUIFactory() {
		// singleton
	}

	public static CPUIFactory getInstance() {
		return INSTANCE;
	}

	/**
	 * Creates a controller that displays only the content part of a content packaging. Using the public method of the CPDisplayController one has access to the
	 * corresponding menu tree.
	 * <p>
	 * Use this to embedd a CP something where the layout in handled by another controller, e.b. with in course
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootContainer The VFS root container where the CP is found on disk
	 * @param activateFirstPage true to automatically activate the first node with content
	 * @param initialUri can be NULL, will use first page then
	 * @return a CPDisplayController
	 */
	public CPDisplayController createContentOnlyCPDisplayController(final UserRequest ureq, final WindowControl wControl, final VFSContainer rootContainer,
			final boolean activateFirstPage, final String initialUri, final OLATResourceable ores) {
		return new CPDisplayController(ureq, wControl, rootContainer, false, activateFirstPage, initialUri, ores);
	}

	/**
	 * Creates a main layout controller. The layout uses one or two columns depending the the showMenu flag.
	 * <p>
	 * Use this where you have no main layout present, e.g. in a pop up in a stand-alone view
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootContainer The VFS root container where the CP is found on disk
	 * @param showMenu true to display the menu, false to hide the menu
	 * @param activateFirstPage true to automatically activate the first node with content
	 * @param initialUri can be NULL, will use first page then
	 * @return A main layout controller
	 */
	public MainLayout3ColumnsController createMainLayoutController(final UserRequest ureq, final WindowControl wControl, final VFSContainer rootContainer,
			final boolean showMenu, final boolean activateFirstPage, final String initialUri, final OLATResourceable ores) {
		return createMainLayoutController(ureq, wControl, rootContainer, showMenu, null, null, activateFirstPage, initialUri, ores);
	}

	/**
	 * Creates a main layout controller. The layout uses one or two columns depending the the showMenu flag.
	 * <p>
	 * Use this where you have no main layout present, e.g. in a pop up in a stand-alone view
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootContainer The VFS root container where the CP is found on disk
	 * @param showMenu true to display the menu, false to hide the menu
	 * @param contentEncoding A specific encoding for the content
	 * @param jsEncoding A specific encoding for javascripts
	 * @param activateFirstPage true to automatically activate the first node with content
	 * @param initialUri can be NULL, will use first page then
	 * @return A main layout controller
	 */
	public MainLayout3ColumnsController createMainLayoutController(final UserRequest ureq, final WindowControl wControl, final VFSContainer rootContainer,
			final boolean showMenu, final String contentEncoding, final String jsEncoding, final boolean activateFirstPage, final String initialUri,
			final OLATResourceable ores) {
		final CPDisplayController cpCtr = new CPDisplayController(ureq, wControl, rootContainer, showMenu, activateFirstPage, initialUri, ores);
		cpCtr.setContentEncoding(contentEncoding);
		cpCtr.setJSEncoding(jsEncoding);
		final MainLayout3ColumnsController layoutCtr = new LayoutMain3ColsController(ureq, wControl, cpCtr.getMenuComponent(), null, cpCtr.getInitialComponent(),
				rootContainer.getName());
		layoutCtr.addDisposableChildController(cpCtr); // cascade disposing requests
		return layoutCtr;
	}

	/**
	 * Creates a main layout controller. The layout uses one or two columns depending the the showMenu flag.
	 * <p>
	 * Use this where you have no main layout present, e.g. in a pop up in a stand-alone view
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootContainer The VFS root container where the CP is found on disk
	 * @param showMenu true to display the menu, false to hide the menu
	 * @return A main layout controller
	 */
	public MainLayout3ColumnsController createMainLayoutController(final UserRequest ureq, final WindowControl wControl, final VFSContainer rootContainer,
			final boolean showMenu) {
		return createMainLayoutController(ureq, wControl, rootContainer, showMenu, null, null, true, null, null);
	}

	/**
	 * Creates a main layout controller that is wrapped with a resource listener that automatically disposes the controller whenever something changes on the resource.
	 * The layout uses one or two columns depending the the showMenu flag.
	 * <p>
	 * Use this where you have no main layout present, e.g. in a pop up in a stand-alone view
	 * 
	 * @param res The OLAT resource to listen to
	 * @param ureq
	 * @param rootContainer The VFS root container where the CP is found on disk
	 * @param showMenu true to display the menu, false to hide the menu
	 * @param activateFirstPage true to automatically activate the first node with content
	 * @param initialUri can be NULL, will use first page then
	 * @return A main layout controller
	 * @return the resource listening wrapper
	 */
	public OLATResourceableListeningWrapperController createMainLayoutResourceableListeningWrapperController(final OLATResourceable res, final UserRequest ureq,
			final WindowControl wControl, final VFSContainer rootContainer, final boolean showMenu, final boolean activateFirstPage, final String initialUri) {
		final MainLayout3ColumnsController layoutCtr = createMainLayoutController(ureq, wControl, rootContainer, showMenu, activateFirstPage, initialUri, res);
		return new OLATResourceableListeningWrapperController(ureq, wControl, res, layoutCtr, ureq.getIdentity());
	}

	/**
	 * Creates a main layout controller that is wrapped with a resource listener that automatically disposes the controller whenever something changes on the resource.
	 * The layout uses one or two columns depending the the showMenu flag.
	 * <p>
	 * Use this where you have no main layout present, e.g. in a pop up in a stand-alone view
	 * 
	 * @param res The OLAT resource to listen to
	 * @param ureq
	 * @param rootContainer The VFS root container where the CP is found on disk
	 * @param showMenu true to display the menu, false to hide the menu
	 * @return A main layout controller
	 * @return the resource listening wrapper
	 */
	public OLATResourceableListeningWrapperController createMainLayoutResourceableListeningWrapperController(final OLATResourceable res, final UserRequest ureq,
			final WindowControl wControl, final VFSContainer rootContainer) {
		return createMainLayoutResourceableListeningWrapperController(res, ureq, wControl, rootContainer, true, true, null);
	}

	/**
	 * Creates a main layout controller that can be activated. It provides a "close preview" link that automatically deactivates this controller form the GUI stack
	 * <p>
	 * Use this when you want the user to be able to preview a CP
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootContainer The VFS root container where the CP is found on disk
	 * @param showMenu true to display the menu, false to hide the menu
	 * @return A main layout preview controller
	 */
	public LayoutMain3ColsPreviewController createMainLayoutPreviewController(final UserRequest ureq, final WindowControl wControl, final VFSContainer rootContainer,
			final boolean showMenu) {
		final CPDisplayController cpCtr = new CPDisplayController(ureq, wControl, rootContainer, showMenu, true, null, null);
		final LayoutMain3ColsPreviewController layoutCtr = new LayoutMain3ColsPreviewController(ureq, wControl, cpCtr.getMenuComponent(), null,
				cpCtr.getInitialComponent(), rootContainer.getName());
		layoutCtr.addDisposableChildController(cpCtr); // cascade disposing requests
		return layoutCtr;
	}

}
