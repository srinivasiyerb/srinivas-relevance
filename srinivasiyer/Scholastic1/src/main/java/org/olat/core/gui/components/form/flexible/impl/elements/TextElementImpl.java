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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.core.gui.components.form.flexible.impl.elements;

import java.util.ArrayList;

import org.apache.commons.lang.StringEscapeUtils;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.components.form.flexible.FormBaseComponentIdProvider;
import org.olat.core.gui.components.form.flexible.elements.InlineTextElement;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormJSHelper;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.RenderingState;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;

/**
 * Description:<br>
 * TODO: patrickb Class Description for TextElement
 * <P>
 * Initial Date: 25.11.2006 <br>
 * 
 * @author patrickb
 */
public class TextElementImpl extends AbstractTextElement implements InlineTextElement {

	protected TextElementComponent component;
	// set text input type as default
	private String htmlInputType = HTML_INPUT_TYPE_TEXT;

	public final static String HTML_INPUT_TYPE_TEXT = "text";
	public final static String HTML_INPUT_TYPE_PASSWORD = "password";

	// inline stuff
	protected String transientValue;// last submitted value, which may be good or wrong
	OLog log = Tracing.createLoggerFor(this.getClass());

	public TextElementImpl(String name, String predefinedValue) {
		this(name, predefinedValue, HTML_INPUT_TYPE_TEXT);
	}

	/**
	 * TODO: check if htmlInputType is valid
	 * 
	 * @param name
	 * @param predefinedValue
	 * @param htmlInputType
	 */
	public TextElementImpl(String name, String predefinedValue, String htmlInputType) {
		this(name, predefinedValue, htmlInputType, false);
	}

	public TextElementImpl(String name, String predefinedValue, boolean asInline) {
		super(name, asInline);
		if (asInline) {
			initInlineEditing(predefinedValue);
		} else {
			this.component = new TextElementComponent(this);
		}
	}

	/**
	 * for specialized TextElements, i.e. IntegerElementImpl.
	 * 
	 * @param name
	 */
	protected TextElementImpl(String name) {
		// if you change something here, please see if other constructors need a change too.
		super(name);
		this.component = new TextElementComponent(this);
	}

	public TextElementImpl(String name, String predefinedValue, String htmlInputType, boolean asInlineEditingElement) {
		super(name, asInlineEditingElement);
		setValue(predefinedValue);
		if (HTML_INPUT_TYPE_TEXT.equals(htmlInputType) || HTML_INPUT_TYPE_PASSWORD.equals(htmlInputType)) {
			this.htmlInputType = htmlInputType;
		} else {
			throw new AssertException(htmlInputType + " html input type not supported!");
		}

		if (asInlineEditingElement) {
			initInlineEditing(predefinedValue);
		} else {
			// init the standard element component
			this.component = new TextElementComponent(this);
		}

	}

	private void initInlineEditing(String predefinedValue) {
		// init the inline editing element component.
		transientValue = predefinedValue;
		AbstractInlineElementComponent aiec = new AbstractInlineElementComponent(this, new ComponentRenderer() {

			@Override
			public void renderHeaderIncludes(Renderer renderer, StringOutput sb, Component source, URLBuilder ubu, Translator translator, RenderingState rstate) {
				// nothing to do here
			}

			@Override
			public void renderBodyOnLoadJSFunctionCall(Renderer renderer, StringOutput sb, Component source, RenderingState rstate) {
				// nothing to do here

			}

			@Override
			public void render(Renderer renderer, StringOutput sb, Component source, URLBuilder ubu, Translator translator, RenderResult renderResult, String[] args) {

				AbstractInlineElementComponent aiec = (AbstractInlineElementComponent) source;

				InlineTextElement itei = (InlineTextElement) aiec.getInlineElement();
				StringBuilder htmlVal = new StringBuilder();

				/**
				 * in case of an error show the test which caused the error which must be stored by the textelement in the transientValue. the last valid value is always
				 * set over setValue(..) by the textelement, and thus can be retrieved as such here.
				 */
				String tmpVal;
				if (itei.hasError()) {
					tmpVal = StringHelper.containsNonWhitespace(transientValue) ? transientValue : itei.getEmptyDisplayText();
				} else {
					tmpVal = StringHelper.containsNonWhitespace(getValue()) ? getValue() : itei.getEmptyDisplayText();
				}
				// the html safe value
				htmlVal.append(StringEscapeUtils.escapeHtml(tmpVal));

				if (!itei.isEnabled()) {
					// RO view and not clickable
					String id = aiec.getFormDispatchId();

					sb.append("<span id=\"");
					sb.append(id);
					sb.append("\" ");
					sb.append(" >");
					sb.append(htmlVal); //
					sb.append("</span>");

				} else {
					//
					// Editable view
					// which can be left
					// .......with clicking outside -> onBlur saves the value
					// .......pressing ENTER/RETURN or TAB -> onBlur saves the value
					// .......presssing ESC -> restore previous value and submit this one.
					if (itei.isInlineEditingOn()) {
						String id = aiec.getFormDispatchId();
						// read write view
						sb.append("<input type=\"").append("input").append("\" id=\"");
						sb.append(id);
						sb.append("\" name=\"");
						sb.append(id);
						sb.append("\" size=\"");
						sb.append("30");
						// if(itei.maxlength > -1){
						// sb.append("\" maxlength=\"");
						// sb.append(itei.maxlength);
						// }
						sb.append("\" value=\"");
						sb.append(htmlVal);
						sb.append("\" ");
						sb.append(" />");

						// Javascript
						sb.append(FormJSHelper.getJSStart());
						// clicking outside or pressing enter -> OK, pressing ESC -> Cancel
						FormJSHelper.getInlineEditOkCancelJS(sb, id, StringEscapeUtils.escapeHtml(getValue()), itei.getRootForm());
						sb.append(FormJSHelper.getJSEnd());

					} else {
						// RO<->RW view which can be clicked
						String id = aiec.getFormDispatchId();

						sb.append("<span id=\"");
						sb.append(id);
						sb.append("\" class=\"b_inline_editable\" ");
						sb.append(FormJSHelper.getRawJSFor(itei.getRootForm(), id, itei.getAction()));
						sb.append(" >");
						sb.append(htmlVal); //
						sb.append("</span>");
					}

				}// endif
			}

		});
		setInlineEditingComponent(aiec);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormItemImpl#evalFormRequest(org.olat.core.gui.UserRequest)
	 */
	@Override
	public void evalFormRequest(UserRequest ureq) {
		if (isInlineEditingElement()) {
			evalFormRequestInline(ureq);
		} else {
			evalFormRequestStandard(ureq);
		}
	}

	private void evalFormRequestStandard(UserRequest ureq) {
		String paramId = String.valueOf(component.getFormDispatchId());
		String value = getRootForm().getRequestParameter(paramId);
		if (value != null) {
			setValue(value);
			// mark associated component dirty, that it gets rerendered
			component.setDirty(true);
		}
	}

	private void evalFormRequestInline(UserRequest ureq) {
		// not used yet
	}

	@Override
	protected void dispatchFormRequest(UserRequest ureq) {
		if (isInlineEditingElement()) {
			dispatchFormRequestInline(ureq);
		} else {
			super.dispatchFormRequest(ureq);
		}

	}

	protected void dispatchFormRequestInline(UserRequest ureq) {
		// click to go back display mode only -> submit -> trigger formOk -> saving
		// value(s)
		String paramId = String.valueOf(((FormBaseComponentIdProvider) getInlineEditingComponent()).getFormDispatchId());
		String paramVal = getRootForm().getRequestParameter(paramId);
		if (paramVal != null) {
			// if value has changed -> set new value and submit
			// otherwise nothing has changed, just switch the inlinde editing mode.

			// validate the inline element to check for error
			transientValue = getValue();
			setValue(paramVal);
			validate(new ArrayList());
			if (hasError()) {
				// in any case, if an error is there -> set Inline Editing on
				isInlineEditingOn(true);
			}
			getRootForm().submit(ureq);// submit validates again!

			if (hasError()) {
				setValue(transientValue);// error with paramVal -> fallback to previous
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
	protected Component getFormItemComponent() {
		return component;
	}

	protected String getHtmlInputType() {
		return htmlInputType;
	}

	@Override
	public void setTranslator(Translator translator) {
		// wrap package translator with fallback form translator
		// hint: do not take this.getClass() but the real class! for package translator creation
		Translator elmTranslator = Util.createPackageTranslator(TextElementImpl.class, translator.getLocale(), translator);
		super.setTranslator(elmTranslator);
	}

	/**
	 * DO NOT USE THE ONCHANGE EVENT with TEXTFIELDS!
	 * 
	 * @see org.olat.core.gui.components.form.flexible.impl.FormItemImpl#addActionListener(org.olat.core.gui.control.Controller, int) TODO: add an onkeypress listener
	 *      which will post do background instead, this could then also be used for an autocomplete textfield
	 */
	@Override
	public void addActionListener(Controller listener, int action) {
		super.addActionListener(listener, action);
		if (action == FormEvent.ONCHANGE) {
			log.warn("Do not use the onChange event in Textfields / TextAreas as this has often unwanted side effects. "
					+ "As the onchange event is only tiggered when you click outside a field or navigate with the tab to the next element "
					+ "it will suppress the first attempt to the submit click as by clicking "
					+ "the submit button first the onchange event will be triggered and you have to click twice to submit the data. ");
		}
	}

}
