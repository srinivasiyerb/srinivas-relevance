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
 * Copyright (c) 2007 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.user.propertyhandlers;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.olat.core.gui.components.form.ValidationError;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.FormUIFactory;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.formelements.DateElement;
import org.olat.core.gui.formelements.FormElement;
import org.olat.core.id.User;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.ValidationStatus;
import org.olat.core.util.i18n.I18nModule;
import org.olat.user.AbstractUserPropertyHandler;
import org.olat.user.UserManager;

/**
 * <h3>Description:</h3> The DatePropertyHandler offers the functionality of a date. It can be used to store something like a birth date.
 * <p>
 * Initial Date: 26.07.2007 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public class DatePropertyHandler extends AbstractUserPropertyHandler {

	/**
	 * Format internal values as yyyyMMdd string e.g. "19751210". So it is possible to use the less, greater and equal operators.
	 */
	private final DateFormat INTERNAL_DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd", Locale.GERMAN);

	/**
	 * @Override
	 * @see org.olat.user.AbstractUserPropertyHandler#getUserProperty(org.olat.core.id.User, java.util.Locale)
	 */
	@Override
	public String getUserProperty(final User user, Locale locale) {
		final Date date = decode(getInternalValue(user));
		if (date == null) { return null; }
		if (locale == null) {
			locale = I18nModule.getDefaultLocale();
		}
		return Formatter.getInstance(locale).formatDate(date);
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#updateUserFromFormElement(org.olat.core.id.User, org.olat.core.gui.formelements.FormElement)
	 */
	@Override
	public void updateUserFromFormElement(final User user, final FormElement ui) {
		final String internalValue = getStringValue(ui);
		setInternalValue(user, internalValue);
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#updateUserFromFormItem(org.olat.core.id.User, org.olat.core.gui.components.form.flexible.FormItem)
	 */
	@Override
	public void updateUserFromFormItem(final User user, final FormItem formItem) {
		final String internalValue = getStringValue(formItem);
		setInternalValue(user, internalValue);
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getStringValue(org.olat.core.gui.formelements.FormElement)
	 */
	@Override
	public String getStringValue(final FormElement ui) {
		final Date date = ((DateElement) ui).getDate();
		return encode(date);
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getStringValue(org.olat.core.gui.components.form.flexible.FormItem)
	 */
	@Override
	public String getStringValue(final FormItem formItem) {
		final Date date = ((org.olat.core.gui.components.form.flexible.elements.DateChooser) formItem).getDate();
		return encode(date);
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getStringValue(java.lang.String, java.util.Locale)
	 */
	@Override
	public String getStringValue(final String displayValue, final Locale locale) {
		if (StringHelper.containsNonWhitespace(displayValue)) {
			final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
			df.setLenient(false);
			try {
				final Date date = df.parse(displayValue.trim());
				return encode(date);
			} catch (final ParseException e) {
				// catch but do nothing, return null in the end
			}
		}
		return null;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getFormElement(java.util.Locale, org.olat.core.id.User, java.lang.String, boolean)
	 */
	@Override
	public FormElement getFormElement(final Locale locale, final User user, final String usageIdentifyer, final boolean isAdministrativeUser) {
		// default is no element
		final UserManager um = UserManager.getInstance();
		DateElement ui = null;
		ui = new DateElement(i18nFormElementLabelKey(), locale);
		updateFormElementFromUser(ui, user);
		if (!um.isUserViewReadOnly(usageIdentifyer, this) || isAdministrativeUser) {
			ui.setExample(Formatter.getInstance(locale).formatDate(new Date()));
		} else {
			ui.setReadOnly(true);
		}
		if (um.isMandatoryUserProperty(usageIdentifyer, this)) {
			ui.setMandatory(true);
		}
		return ui;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#addFormItem(java.util.Locale, org.olat.core.id.User, java.lang.String, boolean,
	 *      org.olat.core.gui.components.form.flexible.FormItemContainer)
	 */
	@Override
	public FormItem addFormItem(final Locale locale, final User user, final String usageIdentifyer, final boolean isAdministrativeUser,
			final FormItemContainer formItemContainer) {
		org.olat.core.gui.components.form.flexible.elements.DateChooser dateElem = null;
		dateElem = FormUIFactory.getInstance().addDateChooser(getName(), i18nFormElementLabelKey(), getInternalValue(user), formItemContainer);
		dateElem.setItemValidatorProvider(this);
		final UserManager um = UserManager.getInstance();
		if (um.isUserViewReadOnly(usageIdentifyer, this) && !isAdministrativeUser) {
			dateElem.setEnabled(false);
		}
		if (um.isMandatoryUserProperty(usageIdentifyer, this)) {
			dateElem.setMandatory(true);
		}

		dateElem.setExampleKey("form.example.free", new String[] { Formatter.getInstance(locale).formatDate(new Date()) });
		return dateElem;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#updateFormElementFromUser(org.olat.core.gui.formelements.FormElement, org.olat.core.id.User)
	 */
	@Override
	public void updateFormElementFromUser(final FormElement ui, final User user) {
		final Date date = decode(getInternalValue(user));
		((DateElement) ui).setDate(date);
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#isValid(org.olat.core.gui.formelements.FormElement)
	 */
	@Override
	public boolean isValid(final FormElement ui, final Map formContext) {

		final DateElement uiDate = (DateElement) ui;

		if (uiDate.getValue().length() == 0) {
			if (!ui.isMandatory()) { return true; }
			ui.setErrorKey(i18nFormElementLabelKey() + ".error.empty");
			return false;
		}

		return uiDate.validDate(i18nFormElementLabelKey() + ".error");
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#isValid(org.olat.core.gui.components.form.flexible.FormItem, java.util.Map)
	 */
	@Override
	public boolean isValid(final FormItem formItem, final Map formContext) {

		final DateChooser dateElem = (DateChooser) formItem;

		if (dateElem.isEmpty()) { return !dateElem.isMandatory() || !dateElem.isEmpty("new.form.mandatory"); }
		final List<ValidationStatus> validation = new ArrayList<ValidationStatus>();
		dateElem.validate(validation);
		if (validation.size() == 0) {
			return true;
		} else {
			// errorkey should be set by dateElem.validate formItem.setErrorKey(i18nFormElementLabelKey()+ ".error", null);
			return false;
		}
	}

	/**
	 * Helper to encode the date as a String
	 * 
	 * @param date
	 * @return
	 */
	private String encode(final Date date) {
		if (date == null) { return null; }
		return INTERNAL_DATE_FORMATTER.format(date);
	}

	/**
	 * Helper to decode a String value to a date
	 * 
	 * @param value
	 * @return
	 */
	private Date decode(final String value) {
		if (!StringHelper.containsNonWhitespace(value)) { return null; }
		try {
			return INTERNAL_DATE_FORMATTER.parse(value.trim());
		} catch (final ParseException e) {
			Tracing.logWarn("Could not parse BirthDayField from database", e, this.getClass());
			return null;
		}
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#isValidValue(java.lang.String, org.olat.core.gui.components.form.ValidationError, java.util.Locale)
	 */
	@Override
	public boolean isValidValue(final String value, final ValidationError validationError, final Locale locale) {
		if (StringHelper.containsNonWhitespace(value)) {
			final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
			df.setLenient(false);
			try {
				df.parse(value.trim());
			} catch (final ParseException e) {
				validationError.setErrorKey(i18nFormElementLabelKey() + ".error");
				return false;
			}
			return true;
		}
		// null values are ok
		return true;
	}

}
