/**
 * BPS Bildungsportal Sachsen GmbH<br>
 * Bahnhofstrasse 6<br>
 * 09111 Chemnitz<br>
 * Germany<br>
 * Copyright (c) 2005-2009 by BPS Bildungsportal Sachsen GmbH<br>
 * http://www.bps-system.de<br>
 * All rights reserved.
 */
package de.bps.course.nodes.den;

import java.util.BitSet;
import java.util.List;

import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableMultiSelectEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.OLATResourceable;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;

import de.bps.course.nodes.DENCourseNode;

public class DENManageDatesController extends BasicController {

	private final DENCourseNode denCourseNode;
	private final OLATResourceable ores;

	// objects for dates management view
	private CloseableModalController editDateModalCntrll;
	private final DENDatesForm manageDatesForm;
	private DENDatesForm editSingleDateForm, editMultipleDatesForm;
	private final DENEditTableDataModel editTableData;
	private TableController editDENTable;
	private List<KalendarEvent> editTableDataList;
	private final VelocityContainer manageVc;
	private BitSet selectedDates;

	private final DENManager denManager;

	public DENManageDatesController(final UserRequest ureq, final WindowControl wControl, final OLATResourceable ores, final DENCourseNode courseNode) {
		super(ureq, wControl);

		this.ores = ores;
		this.denCourseNode = courseNode;
		this.denManager = DENManager.getInstance();

		// prepare form for managing dates
		final ICourse course = CourseFactory.loadCourse(ores);

		manageVc = createVelocityContainer("datemanagement");
		manageDatesForm = new DENDatesForm(ureq, getWindowControl(), getTranslator(), DENDatesForm.CREATE_DATES_LAYOUT);
		manageDatesForm.addControllerListener(this);
		editTableDataList = denManager.getDENEvents(course.getResourceableId(), denCourseNode.getIdent());
		editTableData = new DENEditTableDataModel(editTableDataList, getTranslator());
		editDENTable = denManager.createManageDatesTable(ureq, getWindowControl(), getTranslator(), editTableData);
		listenTo(editDENTable);
		// add Components
		manageVc.put("datesForm", manageDatesForm.getInitialComponent());
		manageVc.put("datesTable", editDENTable.getInitialComponent());

		putInitialPanel(manageVc);
	}

	@Override
	protected void doDispose() {
		if (editDENTable != null) {
			removeAsListenerAndDispose(editDENTable);
			editDENTable = null;
		}
	}

	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		final ICourse course = CourseFactory.loadCourse(ores);
		if (manageDatesForm == source) {
			// generate the dates and put them into the table
			editTableDataList = denManager.generateDates(manageDatesForm.getSubject(), manageDatesForm.getComment(), manageDatesForm.getLocation(),
					manageDatesForm.getDuration(), manageDatesForm.getPause(), manageDatesForm.getBeginDate(), manageDatesForm.getRetakes(),
					manageDatesForm.getNumParts(), editTableDataList, denCourseNode.getIdent());
			denManager.persistDENSettings(editTableDataList, course, denCourseNode);
			editDENTable.setTableDataModel(editTableData);
		} else if (source == editDENTable) {
			final TableMultiSelectEvent tmse = (TableMultiSelectEvent) event;
			selectedDates = tmse.getSelection();
			// clicked button to edit one date or more dates
			if (tmse.getAction().equals(DENEditTableDataModel.CHANGE_ACTION) && selectedDates.cardinality() > 0) {
				if (selectedDates.cardinality() == 1) {
					// if only one date is choosen, we can prefill some entries
					removeAsListenerAndDispose(editSingleDateForm);
					editSingleDateForm = new DENDatesForm(ureq, getWindowControl(), getTranslator(), DENDatesForm.EDIT_SINGLE_DATE_LAYOUT);
					listenTo(editSingleDateForm);

					final KalendarEvent calEvent = (KalendarEvent) editTableData.getObjects(selectedDates).get(0);// in this case only one date is possible
					editSingleDateForm.setSubject(calEvent.getSubject());
					editSingleDateForm.setComment(calEvent.getComment());
					editSingleDateForm.setLocation(calEvent.getLocation());
					editSingleDateForm.setNumParts(calEvent.getNumParticipants());
					editSingleDateForm.setFormDate(calEvent.getBegin());
					editSingleDateForm.setDuration(denManager.getDurationAsString(calEvent));

					removeAsListenerAndDispose(editDateModalCntrll);
					editDateModalCntrll = new CloseableModalController(getWindowControl(), "close", editSingleDateForm.getInitialComponent(), true,
							translate("dates.edit"));
					listenTo(editDateModalCntrll);

				} else if (selectedDates.cardinality() > 1) {
					removeAsListenerAndDispose(editMultipleDatesForm);
					editMultipleDatesForm = new DENDatesForm(ureq, getWindowControl(), getTranslator(), DENDatesForm.EDIT_MULTIPLE_DATES_LAYOUT);
					listenTo(editMultipleDatesForm);

					removeAsListenerAndDispose(editDateModalCntrll);
					editDateModalCntrll = new CloseableModalController(getWindowControl(), "close", editMultipleDatesForm.getInitialComponent(), true,
							translate("dates.edit"));
					listenTo(editDateModalCntrll);
				}
				// persist dates
				denManager.persistDENSettings(editTableData.getObjects(), course, denCourseNode);
				editDateModalCntrll.activate();
			} else if (tmse.getAction().equals(DENEditTableDataModel.DELETE_ACTION)) {
				// delete selected dates
				editTableData.removeEntries(tmse.getSelection());
				editDENTable.setTableDataModel(editTableData);
				// persist dates
				denManager.persistDENSettings(editTableData.getObjects(), course, denCourseNode);
			}
		} else if (source == editSingleDateForm) {
			// save changes for one date
			editTableData.setObjects(denManager.updateDateInList(editSingleDateForm.getSubject(), editSingleDateForm.getComment(), editSingleDateForm.getLocation(),
					editSingleDateForm.getDuration(), editSingleDateForm.getBeginDate(), editSingleDateForm.getNumParts(), editTableData.getObjects(),
					selectedDates.nextSetBit(0)));// only one bit is set
			editDENTable.setTableDataModel(editTableData);
			denManager.persistDENSettings(editTableData.getObjects(), course, denCourseNode);
			editDateModalCntrll.deactivate();
		} else if (source == editMultipleDatesForm) {
			// save changes for multiple dates
			editTableData.setObjects(denManager.updateMultipleDatesInList(editMultipleDatesForm.getSubject(), editMultipleDatesForm.getComment(),
					editMultipleDatesForm.getLocation(), editMultipleDatesForm.getMovementGap(), editMultipleDatesForm.getNumParts(), editTableData.getObjects(),
					selectedDates));
			editDENTable.setTableDataModel(editTableData);
			denManager.persistDENSettings(editTableData.getObjects(), course, denCourseNode);
			editDateModalCntrll.deactivate();
		}
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// nothing to do
	}

}
