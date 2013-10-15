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

import org.olat.core.gui.components.form.ValidationError;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.ItemValidatorProvider;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.formelements.FormElement;
import org.olat.core.id.User;

/**
 * <h3>Description:</h3> A UserPropertyHandler represents a user field and its configuration. It offers the following functionality:
 * <ul>
 * <li>Set user attributes of this user field type</li>
 * <li>Get user attributes of this user field type</li>
 * <li>Get a form element to modify a users attribute of this type</li>
 * <li>Get a table column descriptor to display a user attribute of this type in a table</li>
 * </ul>
 * The UserPropertyHander offers both method for the legacy and the new flexi form infrastrucutre.
 * <p>
 * The UserPropertyHandler is a manager object, it does not contain actual user data.
 * <p>
 * Initial Date: 20.07.2007 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */

public interface UserPropertyHandler extends ItemValidatorProvider {

	/**
	 * @return The identifyer string of this UserPropertyHandler
	 */
	public String getName();

	/**
	 * Use the group identifyer to visually group user fields together when displaying them to users
	 * 
	 * @return The group identifyer string of this UserPropertyHandler
	 */
	public String getGroup();

	/**
	 * @param user the user for which we want to get the value
	 * @param locale the current users locale or NULL if default locale should be used.
	 * @return The value or NULL if no value exists
	 */
	public String getUserProperty(User user, Locale locale);

	/**
	 * @param user the user for which we want to get the value
	 * @param locale the current users locale or NULL if default locale should be used.
	 * @return The value formatted in HTML or an empty String if no value exists
	 */
	public String getUserPropertyAsHTML(User user, Locale locale);

	/**
	 * @param user The user for which we want to set the value
	 * @param value The new value or NULL to remove the value
	 */
	public void setUserProperty(User user, String value);

	/*
	 * Form handling
	 */

	/**
	 * Create a (non-flexi) form element for this UserPropertyHandler. The usageIdentifyer indicates in which form the element is to be used.
	 * 
	 * @param locale The current users locale
	 * @param user The user containing data to be prefilled or NULL if it should be left empty
	 * @param usageIdentifyer The identifyer of the form where this form element is used
	 * @param isAdministrativeUser true: Form element will be set to administrative mode. false: the element is set to user mode. In some cases the field is then
	 *            read-only
	 * @return The form element or NULL if in this context the element is not displayed
	 * @deprecated Use FlexiForms instead of the old forms.
	 */
	@Deprecated
	public FormElement getFormElement(Locale locale, User user, String usageIdentifyer, boolean isAdministrativeUser);

	/**
	 * Adds a flexi-form Item for this UserPropertyHandler. The usageIdentifyer indicates in which form the element is to be used.
	 * 
	 * @param locale The current users locale
	 * @param user The user containing data to be prefilled or NULL if it should be left empty
	 * @param usageIdentifyer The identifyer of the form where this form element is used
	 * @param isAdministrativeUser true: Form element will be set to administrative mode. false: the element is set to user mode. In some cases the field is then
	 *            read-only
	 * @param formItemContainer Container to which Form Item has to be added
	 * @return The formItem will be added to formItemContainer
	 */
	public FormItem addFormItem(Locale locale, User user, String usageIdentifyer, boolean isAdministrativeUser, FormItemContainer formItemContainer);

	/**
	 * Put the current value from this UserPropertyHandler into the given form element
	 * 
	 * @param ui The form element previously created with the getFormElement method
	 * @param user The user that contains the data
	 * @deprecated Use FlexiForms instead of the old forms.
	 */
	@Deprecated
	public void updateFormElementFromUser(FormElement ui, User user);

	/**
	 * Reads the value of the given form element and updates the user
	 * 
	 * @param user The user to be updated
	 * @param ui The form element previously created with the getFormElement method
	 * @return The value or NULL
	 * @deprecated Use FlexiForms instead of the old forms.
	 */
	@Deprecated
	public void updateUserFromFormElement(User user, FormElement ui);

	/**
	 * Reads the value of the given form item and updates the user
	 * 
	 * @param user The user to be updated
	 * @param formItem The form item previously created with the addFormItem method
	 * @return The value or NULL
	 */
	public void updateUserFromFormItem(User user, FormItem formItem);

	/**
	 * @return The i18n key for the UserPropertyHandler form label
	 */
	public String i18nFormElementLabelKey();

	/**
	 * @return The i18n group name key form label
	 */
	public String i18nFormElementGroupKey();

	/**
	 * Check if this form is valid
	 * 
	 * @param ui The form element previously created with the getFormElement method
	 * @param formContext Map containing some variables used in this form context, e.g. for cross form value checks. NULL to not use any form context variables
	 * @return true: the entered value is ok; false: the entered value is not accepted (Validation error)
	 * @deprecated Use FlexiForms instead of the old forms.
	 */
	@Deprecated
	public boolean isValid(FormElement ui, Map formContext);

	/**
	 * Checks if a form item for a property has a valid value and sets the appropriate error key if necessary. This is different from
	 * {@link UserPropertyHandler#isValidValue(String, ValidationError, Locale)} since it takes the value of the form item associated with this user property into
	 * account. Do all validations which depend on a form item here.
	 * 
	 * @param formItem The flexi form item previously created with the addFormItem method
	 * @param formContext Map containing some variables used in this form context, e.g. for cross form value checks. NULL to not use any form context variables
	 * @return true: the entered value is ok; false: the entered value is not accepted (Validation error)
	 */
	public boolean isValid(FormItem formItem, Map formContext);

	/**
	 * Checks if the given value is a valid value for this property (e.g. syntax checking). This is not connected to any form item, in contrast to
	 * {@link UserPropertyHandler#isValid(FormItem, Map)}, so all validations which do not depend on a form item can be done here.
	 * 
	 * @param value The value to be checked
	 * @param validationError Callback to get the error key in case of validation=false
	 * @param local The display users locale
	 * @return true: value is valid, false: value is invalid. Check validationError to get the error message
	 */
	@Override
	public boolean isValidValue(String value, ValidationError validationError, Locale locale);

	/**
	 * Get the value from this form element formatted as string. The returned value is formatted in a way it can be stored in the database, thus it might not be the right
	 * value to display to a user.
	 * 
	 * @param ui
	 * @return
	 * @deprecated Use FlexiForms instead of the old forms.
	 */
	@Deprecated
	public String getStringValue(FormElement ui);

	/**
	 * Get the value from this form item formatted as string. The returned value is formatted in a way it can be stored in the database, thus it might not be the right
	 * value to display to a user.
	 * 
	 * @param ui
	 * @return
	 */
	public String getStringValue(FormItem formItem);

	/**
	 * Get the value from this GUI formatted string The returned value is formatted in a way it can be stored in the database, thus it might not be the right value to
	 * display to a user.
	 * 
	 * @param displayValue
	 * @param locale The locale to be used to parse the display value
	 * @return
	 */
	public String getStringValue(String displayValue, Locale locale);

	/*
	 * Table handling
	 */

	/**
	 * Create a table column descriptor for this user field
	 * 
	 * @param position
	 * @param action
	 * @param locale
	 * @return
	 */
	public ColumnDescriptor getColumnDescriptor(int position, String action, Locale locale);

	/**
	 * @return The i18n key for the UserPropertyHandler column label
	 */
	public String i18nColumnDescriptorLabelKey();

	/**
	 * @return true: this field can be deleted when deleting a user; false: this field should be archived when deleting a user
	 */
	public boolean isDeletable();

}
