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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarUtils;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.SelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.formelements.SpacerElement;
import org.olat.core.gui.formelements.VisibilityDependsOnSelectionRule;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.StringHelper;

public class KalendarEntryForm extends FormBasicController {

	public static final String SUBMIT_MULTI = "multi";
	public static final String SUBMIT_SINGLE = "single";

	public static final String RECURRENCE_NONE = "NONE";

	private final KalendarEvent event;
	private final KalendarRenderWrapper choosenWrapper;
	private StaticTextElement calendarName;
	private SingleSelection chooseCalendar;
	private TextElement subject, location;
	private SelectionElement allDayEvent;
	private DateChooser begin, end;
	private SingleSelection classification;
	private final boolean readOnly, isNew;
	private SingleSelection chooseRecurrence;
	private DateChooser recurrenceEnd;
	private SpacerElement spacer;
	private final List<KalendarRenderWrapper> writeableCalendars;
	private FormLink multi;
	private boolean isMulti;

	private final String[] calendarKeys, calendarValues;
	private final String[] keysRecurrence, valuesRecurrence;
	private final String[] classKeys, classValues;

	/**
	 * Display an event for modification or to add a new event.
	 * 
	 * @param name
	 * @param event
	 * @param choosenWrapper
	 * @param availableCalendars At least one calendar must be editable if this is a new event.
	 * @param isNew If it is a new event, display a list of calendars to choose from.
	 * @param locale
	 */
	public KalendarEntryForm(final UserRequest ureq, final WindowControl wControl, final KalendarEvent event, final KalendarRenderWrapper choosenWrapper,
			final Collection<KalendarRenderWrapper> availableCalendars, final boolean isNew) {
		super(ureq, wControl);

		setBasePackage(CalendarManager.class);

		this.event = event;
		this.choosenWrapper = choosenWrapper;
		this.readOnly = choosenWrapper.getAccess() == KalendarRenderWrapper.ACCESS_READ_ONLY;
		this.isNew = isNew;
		this.isMulti = false;

		writeableCalendars = new ArrayList<KalendarRenderWrapper>();
		for (final Iterator<KalendarRenderWrapper> iter = availableCalendars.iterator(); iter.hasNext();) {
			final KalendarRenderWrapper calendarRenderWrapper = iter.next();
			if (calendarRenderWrapper.getAccess() == KalendarRenderWrapper.ACCESS_READ_WRITE) {
				writeableCalendars.add(calendarRenderWrapper);
			}
		}

		calendarKeys = new String[writeableCalendars.size()];
		calendarValues = new String[writeableCalendars.size()];
		for (int i = 0; i < writeableCalendars.size(); i++) {
			final KalendarRenderWrapper cw = writeableCalendars.get(i);
			calendarKeys[i] = cw.getKalendar().getCalendarID();
			calendarValues[i] = cw.getKalendarConfig().getDisplayName();
		}

		final String currentRecur = CalendarUtils.getRecurrence(event.getRecurrenceRule());
		final VisibilityDependsOnSelectionRule rule;
		keysRecurrence = new String[] { RECURRENCE_NONE, KalendarEvent.DAILY, KalendarEvent.WORKDAILY, KalendarEvent.WEEKLY, KalendarEvent.BIWEEKLY,
				KalendarEvent.MONTHLY, KalendarEvent.YEARLY };
		valuesRecurrence = new String[] { translate("cal.form.recurrence.none"), translate("cal.form.recurrence.daily"), translate("cal.form.recurrence.workdaily"),
				translate("cal.form.recurrence.weekly"), translate("cal.form.recurrence.biweekly"), translate("cal.form.recurrence.monthly"),
				translate("cal.form.recurrence.yearly") };

		// classification
		classKeys = new String[] { "0", "1", "2" };
		classValues = new String[] { getTranslator().translate("cal.form.class.private"), getTranslator().translate("cal.form.class.freebusy"),
				getTranslator().translate("cal.form.class.public") };

		initForm(ureq);
	}

	protected void setEntry(final KalendarEvent kalendarEvent) {
		// subject
		if (readOnly && kalendarEvent.getClassification() == KalendarEvent.CLASS_X_FREEBUSY) {
			subject.setValue(getTranslator().translate("cal.form.subject.hidden"));
		} else {
			subject.setValue(kalendarEvent.getSubject());
		}
		// location
		if (readOnly && kalendarEvent.getClassification() == KalendarEvent.CLASS_X_FREEBUSY) {
			location.setValue(getTranslator().translate("cal.form.location.hidden"));
		} else {
			location.setValue(kalendarEvent.getLocation());
		}
		begin.setDate(kalendarEvent.getBegin());
		end.setDate(kalendarEvent.getEnd());
		allDayEvent.select("xx", kalendarEvent.isAllDayEvent());
		switch (kalendarEvent.getClassification()) {
			case KalendarEvent.CLASS_PRIVATE:
				classification.select("0", true);
				break;
			case KalendarEvent.CLASS_X_FREEBUSY:
				classification.select("1", true);
				break;
			case KalendarEvent.CLASS_PUBLIC:
				classification.select("2", true);
				break;
			default:
				classification.select("0", true);
		}
		final String recurrence = CalendarUtils.getRecurrence(kalendarEvent.getRecurrenceRule());
		if (recurrence != null && !recurrence.equals("") && !recurrence.equals(RECURRENCE_NONE)) {
			chooseRecurrence.select(recurrence, true);
			final Date recurEnd = CalendarUtils.getRecurrenceEndDate(kalendarEvent.getRecurrenceRule());
			if (recurEnd != null) {
				recurrenceEnd.setDate(recurEnd);
			}
		} else {
			chooseRecurrence.select(RECURRENCE_NONE, true);
		}
		isMulti = false;
	}

	protected boolean isMulti() {
		return isMulti;
	}

	@Override
	protected boolean validateFormLogic(final UserRequest ureq) {

		if (begin.getDate() == null) {
			begin.setErrorKey("cal.form.error.date", null);
			return false;
		}

		if (end.getDate() == null) {
			end.setErrorKey("cal.form.error.date", null);
			return false;
		}

		if (end.getDate().before(begin.getDate())) {
			end.setErrorKey("cal.form.error.endbeforebegin", null);
			return false;
		}

		final boolean hasEnd = !chooseRecurrence.getSelectedKey().equals(RECURRENCE_NONE);

		if (hasEnd && recurrenceEnd.getDate() == null) {
			recurrenceEnd.setErrorKey("cal.form.error.date", null);
			return false;
		}

		if (hasEnd && recurrenceEnd.getDate().before(begin.getDate())) {
			recurrenceEnd.setErrorKey("cal.form.error.endbeforebegin", null);
			return false;
		}

		return true;
	}

	/**
	 * Get event with updated values.
	 * 
	 * @return
	 */
	public KalendarEvent getUpdatedKalendarEvent() {
		// subject
		event.setSubject(subject.getValue());

		// location
		event.setLocation(location.getValue());

		// date / time
		event.setBegin(begin.getDate());
		event.setEnd(end.getDate());
		event.setLastModified(new Date().getTime());
		if (event.getCreated() == 0) {
			event.setCreated(new Date().getTime());
		}

		// allday event?
		event.setAllDayEvent(allDayEvent.isSelected(0));

		// classification
		switch (classification.getSelected()) {
			case 0:
				event.setClassification(KalendarEvent.CLASS_PRIVATE);
				break;
			case 1:
				event.setClassification(KalendarEvent.CLASS_X_FREEBUSY);
				break;
			case 2:
				event.setClassification(KalendarEvent.CLASS_PUBLIC);
				break;
			default:
				throw new OLATRuntimeException("getSelected() in KalendarEntryForm.classification returned weitrd value", null);
		}

		// recurrence
		if (chooseRecurrence.getSelectedKey().equals(RECURRENCE_NONE)) {
			event.setRecurrenceRule(null);
		} else {
			final String rrule = CalendarUtils.getRecurrenceRule(chooseRecurrence.getSelectedKey(), recurrenceEnd.getDate());
			event.setRecurrenceRule(rrule);
		}

		return event;
	}

	public String getChoosenKalendarID() {
		if (chooseCalendar == null) { return choosenWrapper.getKalendar().getCalendarID(); }
		return chooseCalendar.getSelectedKey();
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void formCancelled(final UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {

		chooseCalendar = uifactory.addDropdownSingleselect("cal.form.chooseCalendar", formLayout, calendarKeys, calendarValues, null);
		chooseCalendar.select(choosenWrapper.getKalendar().getCalendarID(), true);
		chooseCalendar.setVisible(isNew);

		calendarName = uifactory.addStaticTextElement("calendarname", "cal.form.calendarname", choosenWrapper.getKalendarConfig().getDisplayName(), formLayout);
		calendarName.setVisible(!isNew);

		final boolean fb = readOnly && event.getClassification() == KalendarEvent.CLASS_X_FREEBUSY;

		subject = uifactory
				.addTextAreaElement("subject", "cal.form.subject", -1, 3, 40, true, fb ? translate("cal.form.subject.hidden") : event.getSubject(), formLayout);
		subject.setMandatory(true);
		subject.setNotEmptyCheck("cal.form.error.mandatory");

		location = uifactory.addTextAreaElement("location", "cal.form.location", -1, 3, 40, true, fb ? translate("cal.form.location.hidden") : event.getLocation(),
				formLayout);

		begin = uifactory.addDateChooser("begin", "cal.form.begin", null, formLayout);
		begin.setDisplaySize(21);
		begin.setDateChooserTimeEnabled(true);
		begin.setMandatory(true);
		begin.setDate(event.getBegin());

		end = uifactory.addDateChooser("end", "cal.form.end", null, formLayout);
		end.setDisplaySize(21);
		end.setDateChooserTimeEnabled(true);
		end.setMandatory(true);
		end.setDate(event.getEnd());

		allDayEvent = uifactory.addCheckboxesVertical("allday", "cal.form.allday", formLayout, new String[] { "xx" }, new String[] { null }, null, 1);
		allDayEvent.select("xx", event.isAllDayEvent());

		chooseRecurrence = uifactory.addDropdownSingleselect("cal.form.recurrence", formLayout, keysRecurrence, valuesRecurrence, null);
		final String currentRecur = CalendarUtils.getRecurrence(event.getRecurrenceRule());
		final boolean rk = currentRecur != null && !currentRecur.equals("");
		chooseRecurrence.select(rk ? currentRecur : RECURRENCE_NONE, true);
		chooseRecurrence.addActionListener(this, FormEvent.ONCHANGE);

		recurrenceEnd = uifactory.addDateChooser("recurrence", "cal.form.recurrence.end", null, formLayout);
		recurrenceEnd.setDisplaySize(21);
		recurrenceEnd.setDateChooserTimeEnabled(true);
		recurrenceEnd.setMandatory(true);
		final Date recurEnd = CalendarUtils.getRecurrenceEndDate(event.getRecurrenceRule());
		if (recurEnd != null) {
			recurrenceEnd.setDate(recurEnd);
		}
		recurrenceEnd.setVisible(!chooseRecurrence.getSelectedKey().equals(RECURRENCE_NONE));

		classification = uifactory.addRadiosVertical("classification", "cal.form.class", formLayout, classKeys, classValues);
		switch (event.getClassification()) {
			case KalendarEvent.CLASS_PRIVATE:
				classification.select("0", true);
				break;
			case KalendarEvent.CLASS_X_FREEBUSY:
				classification.select("1", true);
				break;
			case KalendarEvent.CLASS_PUBLIC:
				classification.select("2", true);
				break;
			default:
				classification.select("0", true);
		}

		final StringBuilder buf = new StringBuilder();
		if (event.getCreated() != 0) {
			buf.append(StringHelper.formatLocaleDateTime(event.getCreated(), getTranslator().getLocale()));
			if (event.getCreatedBy() != null && !event.getCreatedBy().equals("")) {
				buf.append(" ");
				buf.append(getTranslator().translate("cal.form.created.by"));
				buf.append(" ");
				buf.append(event.getCreatedBy());
			}
		} else {
			buf.append("-");
		}
		uifactory.addStaticTextElement("cal.form.created.label", buf.toString(), formLayout);

		final FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("button_layout", getTranslator());
		formLayout.add(buttonLayout);
		uifactory.addFormSubmitButton(SUBMIT_SINGLE, "cal.form.submitSingle", buttonLayout);
		if (writeableCalendars.size() > 1) {
			multi = uifactory.addFormLink("cal.form.submitMulti", buttonLayout, "b_button");
		}
		uifactory.addFormCancelButton("cancel", buttonLayout, ureq, getWindowControl());

		if (readOnly) {
			flc.setEnabled(false);
		}
	}

	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (source == chooseRecurrence) {
			recurrenceEnd.setVisible(!chooseRecurrence.getSelectedKey().equals(RECURRENCE_NONE));
		} else if (source == multi) {
			if (validateFormLogic(ureq)) {
				isMulti = true;
				formOK(ureq);
			}
		}
	}

	@Override
	protected void doDispose() {
		//
	}
}
