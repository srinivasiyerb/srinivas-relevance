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

package org.olat.course.statistic;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.components.form.flexible.impl.elements.JSDateChooser;
import org.olat.core.gui.components.form.flexible.impl.elements.SpacerElementImpl;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.Util;

/**
 * Initial Date: 03.12.2009 <br>
 * 
 * @author bja
 */
public class DateChooserForm extends FormBasicController {

	private static final String SIMPLE_DATE_FORMAT_PATTERN = "dd.MM.yyyy";
	private static final String JS_DATE_FORMAT_PATTERN = "%d.%m.%Y";

	private FormLayoutContainer titleContainer;
	private JSDateChooser fromDate;
	private JSDateChooser toDate;
	private FormSubmit subm;
	private final long numDaysRange_;

	public DateChooserForm(final UserRequest ureq, final WindowControl wControl, final long numDaysRange) {
		super(ureq, wControl);

		numDaysRange_ = numDaysRange;
		initForm(this.flc, this, ureq);
	}

	public Date getFromDate() {
		if (fromDate != null && fromDate.getDate() != null) {
			return fromDate.getDate();
		} else {
			return null;
		}
	}

	public Date getToDate() {
		if (toDate != null && toDate.getDate() != null) {
			return toDate.getDate();
		} else {
			return null;
		}
	}

	@Override
	@SuppressWarnings("unused")
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		/*
		 * workaround: catch each inner event to set whole form dirty, in order to make the $f.hasError("group") having an effect in ajax mode. E.g. removing table tr's
		 * in the layouting velocity container.
		 */
		this.flc.setDirty(true);
		/*
		 * 
		 */

	}

	@Override
	@SuppressWarnings("unused")
	protected boolean validateFormLogic(final UserRequest ureq) {
		boolean retVal = true;
		// datefields are valid
		// check complex rules involving checks over multiple elements
		final Date fromDateVal = fromDate.getDate();
		final Date toDateVal = toDate.getDate();
		if (!fromDate.hasError() && !toDate.hasError()) {
			// check valid dates
			// if both are set, check from < to
			if (fromDateVal != null && toDateVal != null) {
				/*
				 * bugfix http://bugs.olat.org/jira/browse/OLAT-813 valid dates and not empty, in easy mode we assume that Start and End date should implement the meaning
				 * of ----false---|S|-----|now|-TRUE---------|E|---false--->t ............. Thus we check for Startdate < Enddate, error otherwise
				 */
				if (fromDateVal.after(toDateVal)) {
					fromDate.setTranslator(Util.createPackageTranslator(org.olat.course.condition.Condition.class, ureq.getLocale(), fromDate.getTranslator()));
					fromDate.setErrorKey("form.easy.error.bdateafteredate", null);
					retVal = false;
				}
			} else {
				if (fromDateVal == null && !fromDate.isEmpty()) {
					// not a correct begin date
					fromDate.setTranslator(Util.createPackageTranslator(org.olat.course.condition.Condition.class, ureq.getLocale(), fromDate.getTranslator()));
					fromDate.setErrorKey("form.easy.error.bdate", null);
					retVal = false;
				}
				if (toDateVal == null && !toDate.isEmpty()) {
					toDate.setTranslator(Util.createPackageTranslator(org.olat.course.condition.Condition.class, ureq.getLocale(), toDate.getTranslator()));
					toDate.setErrorKey("form.easy.error.edate", null);
					retVal = false;
				}
			}
		}
		return retVal;
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		titleContainer = FormLayoutContainer.createHorizontalFormLayout("titleLayout", getTranslator());
		formLayout.add(titleContainer);

		// spacer
		formLayout.add(new SpacerElementImpl("spacer1"));

		final SimpleDateFormat sdf = new SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN);
		final long defaultWeekRange = numDaysRange_ * 24 * 60 * 60 * 1000;

		// from date
		fromDate = new JSDateChooser("fromDate", sdf.format(new Date(new Date().getTime() - defaultWeekRange))) {
			{
				setLabel("datechooser.bdate", null);
				displaySize = 17;
				setExampleKey("datechooser.example.bdate", null);
				// time is enabled
				setDateChooserTimeEnabled(false);
				// not i18n'ified yet
				setDateChooserDateFormat(JS_DATE_FORMAT_PATTERN);
				setCustomDateFormat(SIMPLE_DATE_FORMAT_PATTERN);
				displaySize = getExampleDateString().length();
			}
		};
		formLayout.add(fromDate);
		// end date
		toDate = new JSDateChooser("toDate", sdf.format(new Date())) {
			{
				setLabel("datechooser.edate", null);
				setExampleKey("datechooser.example.edate", null);
				// time is enabled
				setDateChooserTimeEnabled(false);
				// not i18n'ified yet
				setDateChooserDateFormat(JS_DATE_FORMAT_PATTERN);
				setCustomDateFormat(SIMPLE_DATE_FORMAT_PATTERN);
				setDisplaySize(getExampleDateString().length());
			}
		};
		formLayout.add(toDate);

		// submit button
		subm = new FormSubmit("subm", "datechooser.generate");
		formLayout.add(subm);

		formLayout.add(new SpacerElementImpl("spacer2"));
	}

	@Override
	protected void doDispose() {
		// nothing to be done here
	}

}
