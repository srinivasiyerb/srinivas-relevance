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
 * Copyright (c) 1999-2007 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.commons.calendar.ui;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.ImportCalendarManager;
import org.olat.commons.calendar.model.KalendarConfig;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.commons.calendar.ui.events.KalendarGUIAddEvent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.util.Util;

public class ImportedCalendarConfigurationController extends BasicController {

	private static final String PACKAGE = Util.getPackageName(CalendarManager.class);
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(CalendarManager.class);

	private static final Object CMD_ADD = "add";
	private static final Object CMD_TOGGLE_DISPLAY = "tglvis";
	private static final Object CMD_CHOOSE_COLOR = "cc";
	private static final Object CMD_REMOVE_CALENDAR = "rm";
	private static final String PARAM_ID = "id";

	private final VelocityContainer configVC;
	private List importedCalendarWrappers;
	private CalendarColorChooserController colorChooser;
	private KalendarRenderWrapper lastCalendarWrapper;
	private CloseableModalController cmc;
	private DialogBoxController confirmRemoveDialog;
	private String currentCalendarID;
	private final Link manageCalendarsButton;
	private ManageCalendarsController manageCalendarsController;

	public ImportedCalendarConfigurationController(final List importedCalendarWrappers, final UserRequest ureq, final WindowControl wControl, final boolean insideManager) {
		super(ureq, wControl);
		this.importedCalendarWrappers = importedCalendarWrappers;
		setTranslator(new PackageTranslator(PACKAGE, ureq.getLocale()));

		configVC = new VelocityContainer("calEdit", VELOCITY_ROOT + "/importedCalConfig.html", getTranslator(), this);
		configVC.contextPut("calendars", importedCalendarWrappers);
		configVC.contextPut("insideManager", insideManager);
		manageCalendarsButton = LinkFactory.createButton("cal.managecalendars", configVC, this);

		putInitialPanel(configVC);
	}

	public void setCalendars(final List calendars) {
		this.importedCalendarWrappers = calendars;
		configVC.contextPut("calendars", calendars);
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == configVC) {
			final String command = event.getCommand();
			if (command.equals(CMD_ADD)) {
				// add new event to calendar
				final String calendarID = ureq.getParameter(PARAM_ID);
				fireEvent(ureq, new KalendarGUIAddEvent(calendarID, new Date()));
			} else if (command.equals(CMD_TOGGLE_DISPLAY)) {
				final String calendarID = ureq.getParameter(PARAM_ID);
				final KalendarRenderWrapper calendarWrapper = findKalendarRenderWrapper(calendarID);
				final KalendarConfig config = calendarWrapper.getKalendarConfig();
				config.setVis(!config.isVis());
				CalendarManagerFactory.getInstance().getCalendarManager().saveKalendarConfigForIdentity(config, calendarWrapper.getKalendar(), ureq);
				fireEvent(ureq, Event.CHANGED_EVENT);
			} else if (command.equals(CMD_CHOOSE_COLOR)) {
				final String calendarID = ureq.getParameter(PARAM_ID);
				lastCalendarWrapper = findKalendarRenderWrapper(calendarID);
				removeAsListenerAndDispose(colorChooser);
				colorChooser = new CalendarColorChooserController(getLocale(), getWindowControl(), lastCalendarWrapper.getKalendarConfig().getCss());
				listenTo(colorChooser);
				removeAsListenerAndDispose(cmc);
				cmc = new CloseableModalController(getWindowControl(), translate("close"), colorChooser.getInitialComponent());
				cmc.activate();
				listenTo(cmc);
			} else if (command.equals(CMD_REMOVE_CALENDAR)) {
				currentCalendarID = ureq.getParameter(PARAM_ID);
				confirmRemoveDialog = activateOkCancelDialog(ureq, translate("cal.import.remove.title"), translate("cal.import.remove.confirmation_message"),
						confirmRemoveDialog);
			}
		} else if (source == manageCalendarsButton) {
			removeAsListenerAndDispose(manageCalendarsController);
			importedCalendarWrappers = ImportCalendarManager.getImportedCalendarsForIdentity(ureq);
			manageCalendarsController = new ManageCalendarsController(ureq, ureq.getLocale(), getWindowControl(), importedCalendarWrappers);
			listenTo(manageCalendarsController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), this.translate("close"), manageCalendarsController.getInitialComponent());
			cmc.activate();
			listenTo(cmc);
		}
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == colorChooser) {
			cmc.deactivate();
			if (event == Event.DONE_EVENT) {
				final String choosenColor = colorChooser.getChoosenColor();
				final KalendarConfig config = lastCalendarWrapper.getKalendarConfig();
				config.setCss(choosenColor);
				CalendarManagerFactory.getInstance().getCalendarManager().saveKalendarConfigForIdentity(config, lastCalendarWrapper.getKalendar(), ureq);
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
		} else if (source == confirmRemoveDialog) {
			if (DialogBoxUIFactory.isOkEvent(event)) {
				// remove the imported calendar
				ImportCalendarManager.deleteCalendar(currentCalendarID, ureq);

				// update the calendar list
				importedCalendarWrappers = ImportCalendarManager.getImportedCalendarsForIdentity(ureq);
				configVC.contextPut("calendars", importedCalendarWrappers);

				// show the information that the calendar has been deleted
				showInfo("cal.import.remove.info");
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
		} else if (source == cmc) {
			importedCalendarWrappers = ImportCalendarManager.getImportedCalendarsForIdentity(ureq);
			configVC.setDirty(true);
			fireEvent(ureq, Event.CHANGED_EVENT);
		}
	}

	private KalendarRenderWrapper findKalendarRenderWrapper(final String calendarID) {
		for (final Iterator iter = importedCalendarWrappers.iterator(); iter.hasNext();) {
			final KalendarRenderWrapper calendarWrapper = (KalendarRenderWrapper) iter.next();
			if (calendarWrapper.getKalendar().getCalendarID().equals(calendarID)) { return calendarWrapper; }
		}
		return null;
	}

	private String getCalendarType(final String calendarID) {
		final KalendarRenderWrapper calendarWrapper = findKalendarRenderWrapper(calendarID);
		return calendarWrapper.getKalendar().getType();
	}

	@Override
	protected void doDispose() {
		// controllers disposed by BasicController
		cmc = null;
		colorChooser = null;
	}

}
