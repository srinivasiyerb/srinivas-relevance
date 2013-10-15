/**
 * BPS Bildungsportal Sachsen GmbH<br>
 * Bahnhofstrasse 6<br>
 * 09111 Chemnitz<br>
 * Germany<br>
 * Copyright (c) 2005-2008 by BPS Bildungsportal Sachsen GmbH<br>
 * http://www.bps-system.de<br>
 * All rights reserved.
 */
package de.bps.olat.user.propertyhandlers;

import java.util.Locale;
import java.util.Map;

import org.olat.core.gui.components.form.ValidationError;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.FormUIFactory;
import org.olat.core.gui.components.form.flexible.elements.SelectionElement;
import org.olat.core.gui.formelements.CheckBoxElement;
import org.olat.core.gui.formelements.FormElement;
import org.olat.core.id.User;
import org.olat.user.AbstractUserPropertyHandler;
import org.olat.user.UserManager;

/**
 * Description: Checkbox property handler.<br>
 * <P>
 * Initial Date: 06.08.2008 <br>
 * 
 * @author bja
 */
public class GenericCheckboxPropertyHandler extends AbstractUserPropertyHandler {

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#addFormItem(java.util.Locale, org.olat.core.id.User, java.lang.String, boolean,
	 *      org.olat.core.gui.components.form.flexible.FormItemContainer)
	 */
	@Override
	public FormItem addFormItem(final Locale locale, final User user, final String usageIdentifyer, final boolean isAdministrativeUser,
			final FormItemContainer formItemContainer) {
		SelectionElement sElem = null;
		sElem = FormUIFactory.getInstance().addCheckboxesVertical(getName(), i18nFormElementLabelKey(), formItemContainer, new String[] { getName() },
				new String[] { "" }, null, 1);

		final UserManager um = UserManager.getInstance();
		if (um.isUserViewReadOnly(usageIdentifyer, this) && !isAdministrativeUser) {
			sElem.setEnabled(false);
		}
		if (um.isMandatoryUserProperty(usageIdentifyer, this)) {
			sElem.setMandatory(true);
		}
		return sElem;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getFormElement(java.util.Locale, org.olat.core.id.User, java.lang.String, boolean)
	 */
	@Override
	public FormElement getFormElement(final Locale locale, final User user, final String usageIdentifyer, final boolean isAdministrativeUser) {
		CheckBoxElement ui = null;
		final UserManager um = UserManager.getInstance();

		final String value = getInternalValue(user);
		final boolean isEnabled = value != null && value.equals("true") ? Boolean.TRUE : Boolean.FALSE;
		ui = new CheckBoxElement(i18nFormElementLabelKey(), isEnabled);
		if (um.isUserViewReadOnly(usageIdentifyer, this) && !isAdministrativeUser) {
			ui.setReadOnly(true);
		}
		if (um.isMandatoryUserProperty(usageIdentifyer, this)) {
			ui.setMandatory(true);
		}
		return ui;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getStringValue(org.olat.core.gui.formelements.FormElement)
	 */
	@Override
	public String getStringValue(final FormElement ui) {
		String value = "";
		if (((CheckBoxElement) ui).isChecked()) {
			value = "true";
		} else {
			value = "false";
		}

		return value;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getStringValue(org.olat.core.gui.components.form.flexible.FormItem)
	 */
	@Override
	public String getStringValue(final FormItem formItem) {
		String value = "";
		if (((org.olat.core.gui.components.form.flexible.elements.SelectionElement) formItem).isSelected(0)) {
			value = "true";
		} else {
			value = "false";
		}

		return value;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getStringValue(java.lang.String, java.util.Locale)
	 */
	@Override
	public String getStringValue(final String displayValue, final Locale locale) {
		return displayValue;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#isValid(org.olat.core.gui.formelements.FormElement, java.util.Map)
	 */
	@Override
	public boolean isValid(final FormElement ui, final Map formContext) {
		return true;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#isValid(org.olat.core.gui.components.form.flexible.FormItem, java.util.Map)
	 */
	@Override
	public boolean isValid(final FormItem formItem, final Map formContext) {
		return true;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#isValidValue(java.lang.String, org.olat.core.gui.components.form.ValidationError, java.util.Locale)
	 */
	@Override
	public boolean isValidValue(final String value, final ValidationError validationError, final Locale locale) {
		return true;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#updateFormElementFromUser(org.olat.core.gui.formelements.FormElement, org.olat.core.id.User)
	 */
	@Override
	public void updateFormElementFromUser(final FormElement ui, final User user) {
		((CheckBoxElement) ui).setName(getInternalValue(user));
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

}
