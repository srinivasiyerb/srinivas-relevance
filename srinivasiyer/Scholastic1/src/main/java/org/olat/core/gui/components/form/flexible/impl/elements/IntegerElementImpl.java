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
 * Copyright (c) 1999-2007 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.core.gui.components.form.flexible.impl.elements;

import java.util.ArrayList;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormBaseComponentIdProvider;
import org.olat.core.gui.components.form.flexible.elements.InlineIntegerElement;
import org.olat.core.gui.components.form.flexible.elements.IntegerElement;
import org.olat.core.logging.AssertException;
import org.olat.core.util.ValidationStatus;
import org.olat.core.util.ValidationStatusImpl;

/**
 * Description:<br>
 * TODO: patrickb Class Description for IntegerElement
 * <P>
 * Initial Date: 22.06.2007 <br>
 * 
 * @author patrickb
 */
public class IntegerElementImpl extends TextElementImpl implements IntegerElement, InlineIntegerElement {

	private boolean hasMinCheck = false;
	private boolean hasMaxCheck = false;
	private boolean hasEqualCheck = false;

	private int maxValue = 0;
	private int minValue = 0;
	private int equalValue = 0;

	private String equalValueErrorKey;
	private String maxValueErrorKey;
	private String minValueErrorKey;

	// generic error message for number errors.
	private String intValueErrorKey = "integer.element.int.error";

	private int originalInt;

	/**
	 * @param name
	 * @param predefinedValue
	 */
	public IntegerElementImpl(String name, int predefinedValue) {
		this(name, predefinedValue, false);
	}

	public IntegerElementImpl(String name, int predefinedValue, boolean asInline) {
		super(name, String.valueOf(predefinedValue), asInline);
		originalInt = predefinedValue;
		setIntValue(predefinedValue);
	}

	@Override
	public int getIntValue() {
		return Integer.parseInt(getValue());
	}

	@Override
	public void setIntValue(int value) {
		super.setValue(String.valueOf(value));
	}

	@Override
	public void setIntValueCheck(String errorKey) {
		intValueErrorKey = errorKey;
	}

	private boolean intValueCheck() {
		try {
			Integer.parseInt(getValue());
		} catch (NumberFormatException nfe) {
			setErrorKey(intValueErrorKey, null);
			return false;
		}
		return true;
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormItemImpl#evalFormRequest(org.olat.core.gui.UserRequest)
	 */
	@Override
	public void evalFormRequest(UserRequest ureq) {
		if (isInlineEditingElement()) return;

		String paramId = String.valueOf(component.getFormDispatchId());
		String invalue = getRootForm().getRequestParameter(paramId);
		if (invalue != null) {
			this.value = invalue.trim();
			// mark associated component dirty, that it gets rerendered
			component.setDirty(true);
		}
	}

	@Override
	protected void dispatchFormRequest(UserRequest ureq) {
		if (isInlineEditingElement()) {
			dispatchFormRequestInline(ureq);
		} else {
			super.dispatchFormRequest(ureq);
		}
	}

	@Override
	protected void dispatchFormRequestInline(UserRequest ureq) {
		// click to go back display mode only -> submit -> trigger formOk -> saving
		// value(s)
		String paramId = String.valueOf(((FormBaseComponentIdProvider) getInlineEditingComponent()).getFormDispatchId());
		String paramVal = getRootForm().getRequestParameter(paramId);
		if (paramVal != null) {
			paramVal = paramVal.trim();
			// if value has changed -> set new value and submit
			// otherwise nothing has changed, just switch the inlinde editing mode.

			// validate the inline element to check for error
			transientValue = getValue();
			super.setValue(paramVal);
			validate(new ArrayList());
			if (hasError()) {
				// in any case, if an error is there -> set Inline Editing on
				isInlineEditingOn(true);
			}
			getRootForm().submit(ureq);// submit validates again!

			if (hasError()) {
				super.setValue(transientValue);// error with paramVal -> fallback to previous
			}
			transientValue = paramVal;// this value shows in error case up in inline field along with error

		}
		if (!hasError()) {
			if (isInlineEditingOn()) {
				isInlineEditingOn(false);
			} else {
				isInlineEditingOn(true);
			}
		}
		// mark associated component dirty, that it gets rerendered
		getInlineEditingComponent().setDirty(true);
	}

	@Override
	public void reset() {
		setIntValue(originalInt);
		clearError();
	}

	/**
	 * set a value by string is not allowed - use setIntValue instead.
	 * 
	 * @see org.olat.core.gui.components.form.flexible.impl.elements.AbstractTextElement#setValue(java.lang.String)
	 */
	@Override
	@SuppressWarnings("unused")
	public void setValue(String value) {
		throw new AssertException("Please use setIntValue for an IntegerElement!");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void validate(java.util.List validationResults) {
		//
		super.validate(validationResults);
		if (hasError()) { return; // stop if super found already an error
		}
		// go further with specialized checks

		if (!intValueCheck()) {
			// int check is always done
			validationResults.add(new ValidationStatusImpl(ValidationStatus.ERROR));
			return;
		}
		if (hasEqualCheck && !isEqualCheck()) {
			validationResults.add(new ValidationStatusImpl(ValidationStatus.ERROR));
			return;
		}
		if (!isMaxValueCheck()) {
			validationResults.add(new ValidationStatusImpl(ValidationStatus.ERROR));
			return;
		}
		if (!isMinValueCheck()) {
			validationResults.add(new ValidationStatusImpl(ValidationStatus.ERROR));
			return;
		}
		// else no error
		clearError();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.elements.IntegerElement#setIsEqualCheck(int, java.lang.String)
	 */
	@Override
	public void setIsEqualCheck(int equalValue, String errorKey) {
		hasEqualCheck = true;
		this.equalValue = equalValue;
		equalValueErrorKey = errorKey;
	}

	@Override
	@SuppressWarnings("unused")
	public void setIsEqualCheck(String otherValue, String errorKey) {
		throw new AssertException("Please use setIsEqualCheck(int otherValue, String errorKey) for an IntegerElement!");
	}

	private boolean isEqualCheck() {
		if (hasEqualCheck && getIntValue() != equalValue) {
			setErrorKey(equalValueErrorKey, null);
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void setMaxValueCheck(int maxValue, String errorKey) {
		hasMaxCheck = true;
		this.maxValue = maxValue;
		if (errorKey != null) {
			maxValueErrorKey = errorKey;
		} else {
			maxValueErrorKey = "text.element.error.maxvalue";
		}
	}

	private boolean isMaxValueCheck() {
		if (hasMaxCheck && getIntValue() > maxValue) {
			setErrorKey(maxValueErrorKey, new String[] { String.valueOf(maxValue) });
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void setMinValueCheck(int minValue, String errorKey) {
		hasMinCheck = true;
		this.minValue = minValue;
		if (errorKey != null) {
			minValueErrorKey = errorKey;
		} else {
			minValueErrorKey = "text.element.error.minvalue";
		}

	}

	private boolean isMinValueCheck() {
		if (hasMinCheck && getIntValue() < minValue) {
			setErrorKey(minValueErrorKey, new String[] { String.valueOf(minValue) });
			return false;
		} else {
			return true;
		}
	}

}
