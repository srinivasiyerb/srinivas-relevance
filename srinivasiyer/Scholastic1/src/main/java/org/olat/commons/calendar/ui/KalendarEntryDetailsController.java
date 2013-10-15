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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.model.Kalendar;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.tabbedpane.TabbedPaneChangedEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;

public class KalendarEntryDetailsController extends BasicController {

	private final Collection<KalendarRenderWrapper> availableCalendars;
	private final boolean isNew, isReadOnly;
	private KalendarEvent kalendarEvent;
	private final Panel mainPanel;
	private final VelocityContainer mainVC, eventVC, linkVC;
	private final TabbedPane pane;
	private final KalendarEntryForm eventForm;
	private LinkProvider activeLinkProvider;
	private DialogBoxController deleteYesNoController;
	private CopyEventToCalendarController copyEventToCalendarController;
	private final Link deleteButton;

	public KalendarEntryDetailsController(final UserRequest ureq, final KalendarEvent kalendarEvent, final KalendarRenderWrapper calendarWrapper,
			final List<KalendarRenderWrapper> availableCalendars, final boolean isNew, final String caller, final WindowControl wControl) {
		super(ureq, wControl);

		setBasePackage(CalendarManager.class);

		this.availableCalendars = availableCalendars;
		this.kalendarEvent = kalendarEvent;
		this.isNew = isNew;
		// setTranslator(new PackageTranslator(CalendarManager.class.getPackage().getName(), getLocale()));
		// mainVC = new VelocityContainer("calEditMain", VELOCITY_ROOT + "/calEditMain.html", getTranslator(), this);
		mainVC = createVelocityContainer("calEditMain");
		mainVC.contextPut("caller", caller);
		pane = new TabbedPane("pane", getLocale());
		pane.addListener(this);
		mainVC.put("pane", pane);

		// eventVC = new VelocityContainer("calEditDetails", VELOCITY_ROOT + "/calEditDetails.html", getTranslator(), this);
		eventVC = createVelocityContainer("calEditDetails");
		deleteButton = LinkFactory.createButton("cal.edit.delete", eventVC, this);
		eventVC.contextPut("caller", caller);
		eventForm = new KalendarEntryForm(ureq, wControl, kalendarEvent, calendarWrapper, availableCalendars, isNew);
		listenTo(eventForm);
		eventVC.put("eventForm", eventForm.getInitialComponent());
		eventVC.contextPut("isNewEvent", new Boolean(isNew));
		isReadOnly = calendarWrapper.getAccess() == KalendarRenderWrapper.ACCESS_READ_ONLY;
		eventVC.contextPut("isReadOnly", new Boolean(isReadOnly));
		pane.addTab(translate("tab.event"), eventVC);

		// linkVC = new VelocityContainer("calEditLinks", VELOCITY_ROOT + "/calEditLinks.html", getTranslator(), this);
		linkVC = createVelocityContainer("calEditLinks");
		linkVC.contextPut("caller", caller);
		if (!isReadOnly) {
			pane.addTab(translate("tab.links"), linkVC);
		}

		// wrap everything in a panel
		mainPanel = putInitialPanel(mainVC);
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == pane) {
			if (event instanceof TabbedPaneChangedEvent) {
				// prepare links tab
				final TabbedPaneChangedEvent tpce = (TabbedPaneChangedEvent) event;
				if (tpce.getNewComponent().equals(linkVC)) {
					// display link provider if any
					final String calendarID = eventForm.getChoosenKalendarID();
					KalendarRenderWrapper calendarWrapper = null;
					for (final Iterator iter = availableCalendars.iterator(); iter.hasNext();) {
						calendarWrapper = (KalendarRenderWrapper) iter.next();
						if (calendarWrapper.getKalendar().getCalendarID().equals(calendarID)) {
							break;
						}
					}

					if (activeLinkProvider == null) {
						activeLinkProvider = calendarWrapper.getLinkProvider();
						if (activeLinkProvider != null) {
							activeLinkProvider.addControllerListener(this);
							activeLinkProvider.setKalendarEvent(kalendarEvent);
							activeLinkProvider.setDisplayOnly(isReadOnly);
							linkVC.put("linkprovider", activeLinkProvider.getControler().getInitialComponent());
							linkVC.contextPut("hasLinkProvider", Boolean.TRUE);
						} else {
							linkVC.contextPut("hasLinkProvider", Boolean.FALSE);
						}
					}
				}
			}
		} else if (source == deleteButton) {
			// delete calendar entry
			deleteYesNoController = activateYesNoDialog(ureq, null, translate("cal.delete.dialogtext"), deleteYesNoController);
			return;
		}
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == deleteYesNoController) {
			if (DialogBoxUIFactory.isYesEvent(event)) {
				final Kalendar cal = kalendarEvent.getCalendar();
				CalendarManagerFactory.getInstance().getCalendarManager().removeEventFrom(cal, kalendarEvent);
				fireEvent(ureq, Event.DONE_EVENT);
			}
		} else if (source == copyEventToCalendarController) {
			if (event.equals(Event.DONE_EVENT)) {
				fireEvent(ureq, Event.DONE_EVENT);
			} else if (event.equals(Event.CANCELLED_EVENT)) {
				mainPanel.setContent(mainVC);
			}
		} else if (source == activeLinkProvider) {
			fireEvent(ureq, Event.DONE_EVENT);
		} else if (source == eventForm) {
			if (event == Event.DONE_EVENT) {
				// ok, save edited entry
				kalendarEvent = eventForm.getUpdatedKalendarEvent();
				boolean doneSuccessfully = true;
				if (isNew) {
					// this is a new event, add event to calendar
					final String calendarID = eventForm.getChoosenKalendarID();
					for (final Iterator iter = availableCalendars.iterator(); iter.hasNext();) {
						final KalendarRenderWrapper calendarWrapper = (KalendarRenderWrapper) iter.next();
						if (!calendarWrapper.getKalendar().getCalendarID().equals(calendarID)) {
							continue;
						}
						final Kalendar cal = calendarWrapper.getKalendar();
						final boolean result = CalendarManagerFactory.getInstance().getCalendarManager().addEventTo(cal, kalendarEvent);
						if (result == false) {
							// if one failed => done not successfully
							doneSuccessfully = false;
						}
					}
				} else {
					// this is an existing event, so we get the previousely assigned calendar from the event
					final Kalendar cal = kalendarEvent.getCalendar();
					doneSuccessfully = CalendarManagerFactory.getInstance().getCalendarManager().updateEventFrom(cal, kalendarEvent);
				}
				// check if event is still available
				if (!doneSuccessfully) {
					showError("cal.error.save");
					fireEvent(ureq, Event.FAILED_EVENT);
					return;
				}

				if (eventForm.isMulti()) {
					// offer to copy event to multiple calendars.
					removeAsListenerAndDispose(copyEventToCalendarController);
					copyEventToCalendarController = new CopyEventToCalendarController(kalendarEvent, availableCalendars, getTranslator(), getWindowControl());
					listenTo(copyEventToCalendarController);
					// copyEventToCalendarController.addControllerListener(this);
					mainPanel.setContent(copyEventToCalendarController.getInitialComponent());
					return;
				}

				// saving was ok, finish workflow
				fireEvent(ureq, Event.DONE_EVENT);

			} else if (event == Event.CANCELLED_EVENT) {
				eventForm.setEntry(kalendarEvent);
				// user canceled, finish workflow
				fireEvent(ureq, Event.DONE_EVENT);
			}
		}
	}

	@Override
	protected void doDispose() {
		//
	}

	public KalendarEvent getKalendarEvent() {
		return kalendarEvent;
	}

}
