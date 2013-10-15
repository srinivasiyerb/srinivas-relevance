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

package org.olat.commons.calendar.ui;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.ICalTokenGenerator;
import org.olat.commons.calendar.model.KalendarConfig;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.commons.calendar.ui.events.KalendarGUIAddEvent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
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
import org.olat.course.run.calendar.CourseCalendarSubscription;

public class KalendarConfigurationController extends BasicController {

	private static final String PACKAGE = Util.getPackageName(CalendarManager.class);
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(CalendarManager.class);

	private static final Object CMD_ADD = "add";
	private static final Object CMD_TOGGLE_DISPLAY = "tglvis";
	private static final Object CMD_CHOOSE_COLOR = "cc";
	private static final Object CMD_ICAL_FEED = "if";
	private static final Object CMD_ICAL_REGENERATE = "rf";
	private static final Object CMD_ICAL_REMOVE_FEED = "rmif";
	private static final Object CMD_UNSUBSCRIBE = "unsub";
	private static final String PARAM_ID = "id";

	private final VelocityContainer configVC;
	private List<KalendarRenderWrapper> calendars;
	private CalendarColorChooserController colorChooser;
	private KalendarRenderWrapper lastCalendarWrapper;
	private CloseableModalController cmc;
	private String currentCalendarID;
	private CalendarExportController exportController;
	private DialogBoxController confirmRemoveDialog;
	private DialogBoxController confirmRegenerateDialog;

	private List<String> subscriptionIds;

	public KalendarConfigurationController(final List<KalendarRenderWrapper> calendars, final UserRequest ureq, final WindowControl wControl,
			final boolean insideManager, final boolean canUnsubscribe) {
		super(ureq, wControl);
		setTranslator(new PackageTranslator(PACKAGE, ureq.getLocale()));

		configVC = new VelocityContainer("calEdit", VELOCITY_ROOT + "/calConfig.html", getTranslator(), this);
		setCalendars(ureq, calendars);
		configVC.contextPut("insideManager", insideManager);
		configVC.contextPut("identity", ureq.getIdentity());
		configVC.contextPut("removeFromPersonalCalendar", Boolean.TRUE);
		putInitialPanel(configVC);
	}

	public void setEnableRemoveFromPersonalCalendar(final boolean enable) {
		configVC.contextPut("removeFromPersonalCalendar", new Boolean(enable));
	}

	public void setCalendars(final UserRequest ureq, final List<KalendarRenderWrapper> calendars) {
		subscriptionIds = CourseCalendarSubscription.getSubscribedCourseCalendarIDs(ureq.getUserSession().getGuiPreferences());
		setCalendars(calendars);
	}

	public void setCalendars(final List<KalendarRenderWrapper> calendars) {
		this.calendars = calendars;
		for (final KalendarRenderWrapper calendar : calendars) {
			calendar.setSubscribed(subscriptionIds.contains(calendar.getKalendar().getCalendarID()));
		}

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
			} else if (command.equals(CMD_ICAL_FEED)) {
				final String calendarID = ureq.getParameter(PARAM_ID);
				final KalendarRenderWrapper calendarWrapper = findKalendarRenderWrapper(calendarID);
				final String calFeedLink = ICalTokenGenerator.getIcalFeedLink(calendarWrapper.getKalendar().getType(), calendarID, ureq.getIdentity());
				exportController = new CalendarExportController(getLocale(), getWindowControl(), calFeedLink);
				listenTo(exportController);
				removeAsListenerAndDispose(cmc);
				cmc = new CloseableModalController(getWindowControl(), translate("close"), exportController.getInitialComponent());
				cmc.activate();
				listenTo(cmc);
			} else if (command.equals(CMD_ICAL_REGENERATE)) {
				currentCalendarID = ureq.getParameter(PARAM_ID);
				confirmRegenerateDialog = activateOkCancelDialog(ureq, translate("cal.icalfeed.regenerate.title"), translate("cal.icalfeed.regenerate.warning"),
						confirmRegenerateDialog);
			} else if (command.equals(CMD_ICAL_REMOVE_FEED)) {
				currentCalendarID = ureq.getParameter(PARAM_ID);
				confirmRemoveDialog = activateOkCancelDialog(ureq, translate("cal.icalfeed.remove.title"), translate("cal.icalfeed.remove.confirmation_message"),
						confirmRemoveDialog);
			} else if (command.equals(CMD_UNSUBSCRIBE)) {
				currentCalendarID = ureq.getParameter(PARAM_ID);
				final KalendarRenderWrapper calendarWrapper = findKalendarRenderWrapper(currentCalendarID);
				final CalendarSubscription subscription = new CourseCalendarSubscription(calendarWrapper.getKalendar(), ureq.getUserSession().getGuiPreferences());
				subscription.unsubscribe();

				for (final Iterator<KalendarRenderWrapper> it = calendars.iterator(); it.hasNext();) {
					final KalendarRenderWrapper calendar = it.next();
					if (calendarWrapper.getKalendar().getCalendarID().equals(calendar.getKalendar().getCalendarID())) {
						it.remove();
					}
				}
				configVC.contextPut("calendars", calendars);
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
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
				final KalendarRenderWrapper calendarWrapper = findKalendarRenderWrapper(currentCalendarID);
				ICalTokenGenerator.destroyIcalAuthToken(calendarWrapper.getKalendar().getType(), currentCalendarID, ureq.getIdentity());
				showInfo("cal.icalfeed.remove.info");
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
		} else if (source == confirmRegenerateDialog) {
			if (DialogBoxUIFactory.isOkEvent(event)) {
				final KalendarRenderWrapper calendarWrapper = findKalendarRenderWrapper(currentCalendarID);
				final String regeneratedIcalFeedLink = ICalTokenGenerator.regenerateIcalAuthToken(calendarWrapper.getKalendar().getType(), currentCalendarID,
						ureq.getIdentity());
				final String calFeedLink = ICalTokenGenerator.getIcalFeedLink(calendarWrapper.getKalendar().getType(), currentCalendarID, ureq.getIdentity());
				exportController = new CalendarExportController(getLocale(), getWindowControl(), calFeedLink);
				listenTo(exportController);
				removeAsListenerAndDispose(cmc);
				cmc = new CloseableModalController(getWindowControl(), translate("close"), exportController.getInitialComponent());
				cmc.activate();
				listenTo(cmc);
			}
		}
		configVC.setDirty(true);
	}

	private KalendarRenderWrapper findKalendarRenderWrapper(final String calendarID) {
		for (final KalendarRenderWrapper calendarWrapper : calendars) {
			if (calendarWrapper.getKalendar().getCalendarID().equals(calendarID)) { return calendarWrapper; }
		}
		return null;
	}

	@Override
	protected void doDispose() {
		// controllers disposed by BasicController
		cmc = null;
		colorChooser = null;
	}

}
