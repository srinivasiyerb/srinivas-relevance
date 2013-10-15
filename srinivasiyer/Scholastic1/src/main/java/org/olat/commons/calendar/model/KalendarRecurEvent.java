package org.olat.commons.calendar.model;

import java.util.Date;
import java.util.List;

/**
 * Description:<br>
 * Kalendar Event for recurring events
 * <P>
 * Initial Date: 08.04.2009 <br>
 * 
 * @author skoeber
 */
public class KalendarRecurEvent extends KalendarEvent {

	KalendarEvent sourceEvent;

	public KalendarRecurEvent(final String id, final String subject, final Date begin, final Date end) {
		super(id, subject, begin, end);
	}

	public KalendarRecurEvent(final String id, final String subject, final Date begin, final int duration) {
		super(id, subject, begin, duration);
	}

	public KalendarRecurEvent(final String id, final String subject, final Date begin, final int duration, final String recurrenceRule) {
		super(id, subject, begin, duration, recurrenceRule);
	}

	public KalendarRecurEvent(final String id, final String subject, final Date begin, final Date end, final String recurrenceRule) {
		super(id, subject, begin, end, recurrenceRule);
	}

	/**
	 * @return source event for this recurrence
	 */
	public KalendarEvent getSourceEvent() {
		return sourceEvent;
	}

	/**
	 * @param source event for this recurrence
	 */
	public void setSourceEvent(final KalendarEvent sourceEvent) {
		this.sourceEvent = sourceEvent;
	}

	@Override
	public Kalendar getCalendar() {
		return sourceEvent.getCalendar();
	}

	@Override
	public int getClassification() {
		return sourceEvent.getClassification();
	}

	@Override
	public String getComment() {
		return sourceEvent.getComment();
	}

	@Override
	public long getCreated() {
		return sourceEvent.getCreated();
	}

	@Override
	public String getCreatedBy() {
		return sourceEvent.getCreatedBy();
	}

	@Override
	public String getDescription() {
		return sourceEvent.getDescription();
	}

	@Override
	public String getID() {
		return sourceEvent.getID();
	}

	@Override
	public List getKalendarEventLinks() {
		return sourceEvent.getKalendarEventLinks();
	}

	@Override
	public long getLastModified() {
		return sourceEvent.getLastModified();
	}

	@Override
	public String getLocation() {
		return sourceEvent.getLocation();
	}

	@Override
	public Integer getNumParticipants() {
		return sourceEvent.getNumParticipants();
	}

	@Override
	public String[] getParticipants() {
		return sourceEvent.getParticipants();
	}

	@Override
	public String getRecurrenceRule() {
		return sourceEvent.getRecurrenceRule();
	}

	@Override
	public String getSourceNodeId() {
		return sourceEvent.getSourceNodeId();
	}

	@Override
	public String getSubject() {
		return sourceEvent.getSubject();
	}

	@Override
	public boolean isAllDayEvent() {
		return sourceEvent.isAllDayEvent();
	}
}
