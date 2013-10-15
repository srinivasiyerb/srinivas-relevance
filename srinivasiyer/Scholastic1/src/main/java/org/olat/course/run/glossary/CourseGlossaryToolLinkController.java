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
package org.olat.course.run.glossary;

import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.fullWebApp.popup.BaseFullWebappPopupLayoutFactory;
import org.olat.core.commons.modules.glossary.GlossaryMainController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.control.generic.textmarker.GlossaryMarkupItemController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.prefs.Preferences;
import org.olat.course.ICourse;
import org.olat.course.config.CourseConfig;
import org.olat.course.run.RunMainController;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

/**
 * Description:<br>
 * Toolbox link that shows a link to open the glossary in read or read/write mode and a toggle link to enable/disable the glossary terms identifying process in the given
 * text marker controller.
 * <P>
 * Initial Date: Dec 06 2006 <br>
 * 
 * @author Florian Gnägi, frentix GmbH, http://www.frentix.com
 */
public class CourseGlossaryToolLinkController extends BasicController {

	private final VelocityContainer mainVC;
	private Link onCommand, offCommand;
	private final String guiPrefsKey;
	boolean allowGlossaryEditing;
	private final CourseEnvironment courseEnvir;
	private final GlossaryMarkupItemController glossMarkupItmCtr;

	public CourseGlossaryToolLinkController(final WindowControl wControl, final UserRequest ureq, final ICourse course, final Translator translator,
			final boolean allowGlossaryEditing, final CourseEnvironment courseEnvironment, final GlossaryMarkupItemController glossMarkupItmCtr) {
		super(ureq, wControl, translator);
		setBasePackage(RunMainController.class);
		this.allowGlossaryEditing = allowGlossaryEditing;
		courseEnvir = courseEnvironment;
		guiPrefsKey = CourseGlossaryFactory.createGuiPrefsKey(course);

		mainVC = createVelocityContainer("glossaryToolLink");

		final Preferences prefs = ureq.getUserSession().getGuiPreferences();
		final Boolean state = (Boolean) prefs.get(CourseGlossaryToolLinkController.class, guiPrefsKey);
		if (state == null || !state.booleanValue()) {
			onCommand = LinkFactory.createLink("command.glossary.on", mainVC, this);
			onCommand.setTitle("command.glossary.on.alt");
			onCommand.setCustomEnabledLinkCSS("b_toolbox_toggle");
		} else {
			offCommand = LinkFactory.createLink("command.glossary.off", mainVC, this);
			offCommand.setTitle("command.glossary.off.alt");
			offCommand.setCustomEnabledLinkCSS("b_toolbox_toggle");
		}

		// keep reference to textMarkerContainerCtr for later enabling/disabling
		this.glossMarkupItmCtr = glossMarkupItmCtr;

		putInitialPanel(mainVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == onCommand) {
			// toggle on
			final Preferences prefs = ureq.getUserSession().getGuiPreferences();
			prefs.put(CourseGlossaryToolLinkController.class, guiPrefsKey, Boolean.TRUE);
			prefs.save();
			// update gui
			mainVC.remove(onCommand);
			offCommand = LinkFactory.createLink("command.glossary.off", mainVC, this);
			offCommand.setTitle("command.glossary.off.alt");
			offCommand.setCustomEnabledLinkCSS("b_toolbox_toggle");
			// notify textmarker controller
			glossMarkupItmCtr.setTextMarkingEnabled(true);
			fireEvent(ureq, new Event("glossaryOn"));

		} else if (source == offCommand) {
			// toggle off
			final Preferences prefs = ureq.getUserSession().getGuiPreferences();
			prefs.put(CourseGlossaryToolLinkController.class, guiPrefsKey, Boolean.FALSE);
			prefs.save();
			// update gui
			mainVC.remove(offCommand);
			onCommand = LinkFactory.createLink("command.glossary.on", mainVC, this);
			onCommand.setTitle("command.glossary.on.alt");
			onCommand.setCustomEnabledLinkCSS("b_toolbox_toggle");
			// notify textmarker controller
			glossMarkupItmCtr.setTextMarkingEnabled(false);
			fireEvent(ureq, new Event("glossaryOff"));
		} else if (source == mainVC && event.getCommand().equals("command.glossary")) {
			// start glossary in window
			final CourseConfig cc = courseEnvir.getCourseConfig(); // do not cache cc, not save

			// if glossary had been opened from LR as Tab before, warn user:
			final DTabs dts = (DTabs) Windows.getWindows(ureq).getWindow(ureq).getAttribute("DTabs");
			final RepositoryEntry repoEntry = RepositoryManager.getInstance().lookupRepositoryEntryBySoftkey(cc.getGlossarySoftKey(), false);
			final DTab dt = dts.getDTab(repoEntry.getOlatResource());
			if (dt != null) {
				dts.activate(ureq, dt, ((Boolean) allowGlossaryEditing).toString());
			} else {
				final ControllerCreator ctrlCreator = new ControllerCreator() {
					@Override
					public Controller createController(final UserRequest lureq, final WindowControl lwControl) {
						final GlossaryMainController glossaryController = CourseGlossaryFactory.createCourseGlossaryMainRunController(lwControl, lureq, cc,
								allowGlossaryEditing);
						if (glossaryController == null) {
							// happens in the unlikely event of a user who is in a course and
							// now
							// tries to access the glossary
							final String text = translate("error.noglossary");
							return MessageUIFactory.createInfoMessage(lureq, lwControl, null, text);
						} else {
							// use a one-column main layout
							final LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(lureq, lwControl, null, null,
									glossaryController.getInitialComponent(), null);
							// dispose glossary on layout dispose
							layoutCtr.addDisposableChildController(glossaryController);
							return layoutCtr;
						}
					}
				};

				final ControllerCreator layoutCtrlr = BaseFullWebappPopupLayoutFactory.createAuthMinimalPopupLayout(ureq, ctrlCreator);
				// open in new browser window
				openInNewBrowserWindow(ureq, layoutCtrlr);
				return;// immediate return after opening new browser window!
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// no need to dispose the textMarkerContainerCtr - should be done by parent
		// controller
	}
}
