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

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.olat.core.gui.components.form.ValidationError;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.formelements.FormElement;
import org.olat.core.gui.formelements.TextElement;
import org.olat.core.id.User;
import org.olat.core.util.StringHelper;

/**
 * <h3>Description:</h3> The phne property provides a user property that contains a valid phone number.
 * <p>
 * Initial Date: 27.07.2007 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public class PhonePropertyHandler extends Generic127CharTextPropertyHandler {

	// Regexp to define valid phone numbers
	private static final Pattern VALID_PHONE_PATTERN_IP = Pattern.compile("[0-9/\\-+' ]+");

	/**
	 * @see org.olat.user.propertyhandlers.Generic127CharTextPropertyHandler#getFormElement(java.util.Locale, org.olat.core.id.User, java.lang.String, boolean)
	 */
	@Override
	public FormElement getFormElement(final Locale locale, final User user, final String usageIdentifyer, final boolean isAdministrativeUser) {
		final TextElement ui = (TextElement) super.getFormElement(locale, user, usageIdentifyer, isAdministrativeUser);
		ui.setExample("+41 12 345 67 89");
		return ui;
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.user.propertyhandlers.Generic127CharTextPropertyHandler#addFormItem(java.util.Locale, org.olat.core.id.User, java.lang.String, boolean,
	 * org.olat.core.gui.components.form.flexible.FormItemContainer)
	 */
	@Override
	public FormItem addFormItem(final Locale locale, final User user, final String usageIdentifyer, final boolean isAdministrativeUser,
			final FormItemContainer formItemContainer) {
		final org.olat.core.gui.components.form.flexible.elements.TextElement textElement = (org.olat.core.gui.components.form.flexible.elements.TextElement) super
				.addFormItem(locale, user, usageIdentifyer, isAdministrativeUser, formItemContainer);
		textElement.setExampleKey("form.example.phone", null);
		return textElement;
	}

	/**
	 * @see org.olat.user.AbstractUserPropertyHandler#getUserPropertyAsHTML(org.olat.core.id.User, java.util.Locale)
	 */
	@Override
	public String getUserPropertyAsHTML(final User user, final Locale locale) {
		final String phonenr = getUserProperty(user, locale);
		if (StringHelper.containsNonWhitespace(phonenr)) {
			final StringBuffer sb = new StringBuffer();
			sb.append("<a href=\"callto:");
			sb.append(phonenr);
			sb.append("\" class=\"b_link_call\">");
			sb.append(phonenr);
			sb.append("</a>");
			return sb.toString();
		}
		return null;
	}

	/**
	 * @see org.olat.user.propertyhandlers.Generic127CharTextPropertyHandler#isValid(org.olat.core.gui.formelements.FormElement)
	 */
	@Override
	public boolean isValid(final FormElement ui, final Map formContext) {
		// check parent rules first: check if mandatory and empty
		if (!super.isValid(ui, formContext)) { return false; }

		final TextElement uiPhone = (TextElement) ui;
		final String value = uiPhone.getValue();
		if (StringHelper.containsNonWhitespace(value)) {
			// check phone address syntax
			if (!VALID_PHONE_PATTERN_IP.matcher(value).matches()) {
				ui.setErrorKey(i18nFormElementLabelKey() + ".error.valid");
				return false;
			}
		}
		// everthing ok
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.user.propertyhandlers.Generic127CharTextPropertyHandler#isValid(org.olat.core.gui.components.form.flexible.FormItem, java.util.Map)
	 */
	@Override
	public boolean isValid(final FormItem formItem, final Map formContext) {
		// check parent rules first: check if mandatory and empty
		if (!super.isValid(formItem, formContext)) { return false; }

		final org.olat.core.gui.components.form.flexible.elements.TextElement textElement = (org.olat.core.gui.components.form.flexible.elements.TextElement) formItem;
		final String value = textElement.getValue();

		if (StringHelper.containsNonWhitespace(value)) {
			// check phone address syntax
			if (!VALID_PHONE_PATTERN_IP.matcher(value).matches()) {
				formItem.setErrorKey(i18nFormElementLabelKey() + ".error.valid", null);
				return false;
			}
		}
		// everthing ok
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.olat.user.propertyhandlers.Generic127CharTextPropertyHandler#isValidValue(java.lang.String, org.olat.core.gui.components.form.ValidationError,
	 * java.util.Locale)
	 */
	@Override
	public boolean isValidValue(final String value, final ValidationError validationError, final Locale locale) {
		if (!super.isValidValue(value, validationError, locale)) { return false; }

		if (StringHelper.containsNonWhitespace(value)) {
			// check phone address syntax
			if (!VALID_PHONE_PATTERN_IP.matcher(value).matches()) {
				validationError.setErrorKey(i18nFormElementLabelKey() + ".error.valid");
				return false;
			}
		}
		return true;
	}

}
