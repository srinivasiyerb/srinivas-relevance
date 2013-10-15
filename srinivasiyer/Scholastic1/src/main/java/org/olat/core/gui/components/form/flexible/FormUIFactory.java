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
 * Copyright (c) 1999-2008 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.core.gui.components.form.flexible;

import java.util.List;
import java.util.Map;

import org.olat.core.commons.controllers.linkchooser.CustomLinkTreeModel;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElment;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.FormToggle;
import org.olat.core.gui.components.form.flexible.elements.IntegerElement;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.SpacerElement;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextBoxListElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormItemImpl;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.components.SimpleExampleText;
import org.olat.core.gui.components.form.flexible.impl.components.SimpleFormErrorText;
import org.olat.core.gui.components.form.flexible.impl.components.SimpleLabelText;
import org.olat.core.gui.components.form.flexible.impl.elements.FileElementImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.FormCancel;
import org.olat.core.gui.components.form.flexible.impl.elements.FormLinkImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.components.form.flexible.impl.elements.FormToggleImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.IntegerElementImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.JSDateChooser;
import org.olat.core.gui.components.form.flexible.impl.elements.MultiSelectionTree;
import org.olat.core.gui.components.form.flexible.impl.elements.MultipleSelectionElementImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.SingleSelectionImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.SpacerElementImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.StaticTextElementImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.TextAreaElementImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.TextBoxListElementImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.TextElementImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.richText.RichTextElementImpl;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableElementImpl;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.tree.TreeModel;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;
import org.olat.core.util.UserSession;
import org.olat.core.util.tree.INodeFilter;
import org.olat.core.util.vfs.VFSContainer;

/**
 * Factory class to create the flexible form elements.
 * 
 * @author patrickb
 */
public class FormUIFactory {
	// inject later via spring
	private static FormUIFactory INSTANCE = new FormUIFactory();

	FormUIFactory() {
		// no public constructors.
	}

	/**
	 * @return
	 */
	public static FormUIFactory getInstance() {
		return INSTANCE;
	}

	/**
	 * helper for all factory methods, to check if a label should be set or not. each addXXXX method in the factory should have a smallest possible one, which is using
	 * the "name" as "i18nkey" for the label. And another method with at least one parameter more, the "string i18nkeylabel". Furthermore the latter method should use the
	 * setLabelIfNotNull method to decide whether a label is set (and translated).
	 * 
	 * @param i18nLabel the i18n key to set the label, or <code>null</code> to disable the label.
	 * @param fi
	 */
	static FormItem setLabelIfNotNull(String i18nLabel, FormItem fi) {
		if (StringHelper.containsNonWhitespace(i18nLabel)) {
			fi.setLabel(i18nLabel, null);
			fi.showLabel(true);
		} else {
			fi.showLabel(false);
		}
		return fi;
	}

	/**
	 * Date chooser is a text field with an icon, which on click shows a java script calendar to choose a date/time. This method uses the name to set the i18nkey of the
	 * label.
	 * <p>
	 * If no label is desired use the {@link FormUIFactory#addDateChooser(String, String, String, FormItemContainer)} with <code>null</code> as i18nLabel.
	 * 
	 * @param name
	 * @param initValue
	 * @param formLayout
	 * @return
	 */
	public DateChooser addDateChooser(String name, String initValue, FormItemContainer formLayout) {
		return addDateChooser(name, name, initValue, formLayout);
	}

	/**
	 * Date chooser is a text field with an icon, which on click shows a java script calendar to choose a date/time.
	 * 
	 * @param name
	 * @param initValue
	 * @param i18nLabel
	 * @param formLayout
	 * @return
	 */
	public DateChooser addDateChooser(String name, String i18nLabel, String initValue, FormItemContainer formLayout) {
		JSDateChooser tmp = new JSDateChooser(name, initValue);
		setLabelIfNotNull(i18nLabel, tmp);
		formLayout.add(tmp);
		return tmp;
	}

	/**
	 * create an integer Element. This method uses the name to set the i18nkey of the label.
	 * <p>
	 * If no label is desired use the {@link FormUIFactory#addIntegerElement(String, String, int, FormItemContainer)} with <code>null</code> as i18nLabel.
	 * 
	 * @param name
	 * @param initVal
	 * @param formLayout
	 * @return
	 */
	public IntegerElement addIntegerElement(String name, int initVal, FormItemContainer formLayout) {
		return addIntegerElement(name, name, initVal, formLayout);
	}

	/**
	 * create an integer Element
	 * 
	 * @param name
	 * @param initVal
	 * @param formLayout
	 * @return
	 */
	public IntegerElement addIntegerElement(String name, String i18nLabel, int initVal, FormItemContainer formLayout) {
		IntegerElement tmp = new IntegerElementImpl(name, initVal);
		setLabelIfNotNull(i18nLabel, tmp);
		formLayout.add(tmp);
		return tmp;
	}

	/**
	 * Create a multiple selection element with check-boxes horizontal aligned. This method uses the name to set the i18nkey of the label.
	 * <p>
	 * If no label is desired use the {@link FormUIFactory#addCheckboxesHorizontal(String, String, FormItemContainer, String[], String[], String[])} with
	 * <code>null</code> as i18nLabel.
	 * 
	 * @param name
	 * @param layouter
	 * @param keys
	 * @param values
	 * @param cssClasses
	 * @return
	 */
	public MultipleSelectionElement addCheckboxesHorizontal(String name, FormItemContainer formLayout, String[] keys, String values[], String[] cssClasses) {
		return addCheckboxesHorizontal(name, name, formLayout, keys, values, cssClasses);
	}

	/**
	 * Create a multiple selection element with check-boxes horizontal aligned.
	 * 
	 * @param name
	 * @param i18nLabel
	 * @param formLayout
	 * @param keys
	 * @param values
	 * @param cssClasses
	 * @return
	 */
	public MultipleSelectionElement addCheckboxesHorizontal(String name, String i18nLabel, FormItemContainer formLayout, String[] keys, String values[],
			String[] cssClasses) {
		MultipleSelectionElement mse = new MultipleSelectionElementImpl(name, MultipleSelectionElementImpl.createHorizontalLayout(name));
		mse.setKeysAndValues(keys, values, cssClasses);
		setLabelIfNotNull(i18nLabel, mse);
		formLayout.add(mse);
		return mse;
	}

	/**
	 * Create a multiple selection element with check-boxes that is rendered in vertical columns This method uses the name to set the i18nkey of the label.
	 * <p>
	 * If no label is desired use the {@link FormUIFactory#addCheckboxesVertical(String, String, FormItemContainer, String[], String[], String[], int)} with
	 * <code>null</code> as i18nLabel.
	 * 
	 * @param name
	 * @param layouter
	 * @param keys
	 * @param values
	 * @param cssClasses
	 * @param columns Currently 1 and 2 columns are supported
	 * @return
	 */
	public MultipleSelectionElement addCheckboxesVertical(String name, FormItemContainer formLayout, String[] keys, String values[], String[] cssClasses, int columns) {
		return addCheckboxesVertical(name, name, formLayout, keys, values, cssClasses, columns);
	}

	/**
	 * Create a multiple selection element with check-boxes that is rendered in vertical columns
	 * 
	 * @param name
	 * @param i18nLabel
	 * @param formLayout
	 * @param keys
	 * @param values
	 * @param cssClasses
	 * @param columns
	 * @return
	 */
	public MultipleSelectionElement addCheckboxesVertical(String name, String i18nLabel, FormItemContainer formLayout, String[] keys, String values[],
			String[] cssClasses, int columns) {
		MultipleSelectionElement mse = new MultipleSelectionElementImpl(name, MultipleSelectionElementImpl.createVerticalLayout(name, columns));
		mse.setKeysAndValues(keys, values, cssClasses);
		setLabelIfNotNull(i18nLabel, mse);
		formLayout.add(mse);
		return mse;
	}

	/**
	 * Create a multiple selection element as a drop-down This method uses the name to set the i18nkey of the label.
	 * <p>
	 * If no label is desired use the {@link FormUIFactory#addDropdownMultiselect(String, String, FormItemContainer)} with <code>null</code> as i18nLabel.
	 * 
	 * @param name
	 * @param formLayout
	 * @return
	 */
	public MultipleSelectionElement addDropdownMultiselect(String name, FormItemContainer formLayout) {
		return addDropdownMultiselect(name, name, formLayout);
	}

	/**
	 * @param name
	 * @param i18nLabel
	 * @param formLayout
	 * @return
	 */
	public MultipleSelectionElement addDropdownMultiselect(String name, String i18nLabel, FormItemContainer formLayout) {
		MultipleSelectionElement mse = new MultipleSelectionElementImpl(name, MultipleSelectionElementImpl.createSelectboxLayouter(name));
		setLabelIfNotNull(i18nLabel, mse);
		formLayout.add(mse);
		return mse;
	}

	/**
	 * Create a multiple selection element as a tree. This method uses the name to set the i18nkey of the label.
	 * <p>
	 * If no label is desired use the {@link FormUIFactory#addDropdownMultiselect(String, String, FormItemContainer)} with <code>null</code> as i18nLabel.
	 * 
	 * @param name
	 * @param formLayout
	 * @param treemodel
	 * @return
	 */
	public MultipleSelectionElement addTreeMultiselect(String name, FormItemContainer formLayout, TreeModel treemodel, INodeFilter selectableFilter) {
		return addTreeMultiselect(name, name, formLayout, treemodel, selectableFilter);
	}

	/**
	 * Create a multiple selection element as a tree.
	 * 
	 * @param name
	 * @param i18nLabel
	 * @param formLayout
	 * @param treemodel
	 * @param selectableFilter
	 * @return
	 */
	public MultipleSelectionElement addTreeMultiselect(String name, String i18nLabel, FormItemContainer formLayout, TreeModel treemodel, INodeFilter selectableFilter) {
		MultipleSelectionElement mse = new MultiSelectionTree(name, treemodel, selectableFilter);
		setLabelIfNotNull(i18nLabel, mse);
		formLayout.add(mse);
		return mse;
	}

	/**
	 * Add horizontal aligned radio buttons. <br>
	 * This method uses the name to set the i18nkey of the label.
	 * <p>
	 * If no label is desired use the {@link FormUIFactory#addRadiosHorizontal(String, String, FormItemContainer, String[], String[])} with <code>null</code> as
	 * i18nLabel.
	 * 
	 * @param name item identifier and i18n key for the label
	 * @param formLayout
	 * @param theKeys the radio button keys
	 * @param theValues the radio button display values
	 * @return
	 */
	public SingleSelection addRadiosHorizontal(final String name, FormItemContainer formLayout, final String[] theKeys, final String[] theValues) {
		return addRadiosHorizontal(name, name, formLayout, theKeys, theValues);
	}

	/**
	 * Add horizontal aligned radio buttons. <br>
	 * 
	 * @param name
	 * @param i18nLabel
	 * @param formLayout
	 * @param theKeys
	 * @param theValues
	 * @return
	 */
	public SingleSelection addRadiosHorizontal(final String name, final String i18nLabel, FormItemContainer formLayout, final String[] theKeys, final String[] theValues) {
		SingleSelection ss = new SingleSelectionImpl(name, SingleSelectionImpl.createHorizontalLayout(name)) {
			{
				this.keys = theKeys;
				this.values = theValues;
			}
		};
		setLabelIfNotNull(i18nLabel, ss);
		formLayout.add(ss);
		return ss;
	}

	/**
	 * Add vertical aligned radio buttons<br>
	 * This method uses the name to set the i18nkey of the label.
	 * <p>
	 * If no label is desired use the {@link FormUIFactory#addRadiosVertical(String, String, FormItemContainer, String[], String[])} with <code>null</code> as i18nLabel.
	 * 
	 * @param name item identifier and i18n key for the label
	 * @param formLayout
	 * @param theKeys the radio button keys
	 * @param theValues the radio button display values
	 * @return
	 */
	public SingleSelection addRadiosVertical(final String name, FormItemContainer formLayout, final String[] theKeys, final String[] theValues) {
		return addRadiosVertical(name, name, formLayout, theKeys, theValues);
	}

	/**
	 * Add vertical aligned radio buttons<br>
	 * 
	 * @param name
	 * @param i18nLabel
	 * @param formLayout
	 * @param theKeys
	 * @param theValues
	 * @return
	 */
	public SingleSelection addRadiosVertical(final String name, final String i18nLabel, FormItemContainer formLayout, final String[] theKeys, final String[] theValues) {
		SingleSelection ss = new SingleSelectionImpl(name, SingleSelectionImpl.createVerticalLayout(name)) {
			{
				this.keys = theKeys;
				this.values = theValues;
			}
		};
		setLabelIfNotNull(i18nLabel, ss);
		formLayout.add(ss);
		return ss;
	}

	/**
	 * Add a drop down menu (also called pulldown menu), with a label's i18n key being the same as the <code>name<code>.
	 * If you do not want a label, use the {@link FormUIFactory#addDropdownSingleselect(String, String, FormItemContainer, String[], String[], String[])} 
	 * method with the <code>i18nKey</code> and set it <code>null</code>
	 * 
	 * @param name item identifier and i18n key for the label
	 * @param formLayout
	 * @param theKeys the menu selection keys
	 * @param theValues the menu display values
	 * @param theCssClasses the css classes to style the menu items or NULL to use no special styling
	 * @return
	 */
	public SingleSelection addDropdownSingleselect(final String name, FormItemContainer formLayout, final String[] theKeys, final String[] theValues,
			final String[] theCssClasses) {
		return addDropdownSingleselect(name, name, formLayout, theKeys, theValues, theCssClasses);
	}

	/**
	 * Add a drop down menu (also called pulldown menu).
	 * 
	 * @param name
	 * @param labelKey i18n key for the label, may be <code>null</code> indicating no label.
	 * @param formLayout
	 * @param theKeys
	 * @param theValues
	 * @param theCssClasses
	 * @return
	 */
	public SingleSelection addDropdownSingleselect(final String name, final String i18nLabel, FormItemContainer formLayout, final String[] theKeys,
			final String[] theValues, final String[] theCssClasses) {
		SingleSelection ss = new SingleSelectionImpl(name, SingleSelectionImpl.createSelectboxLayouter(name)) {
			{
				this.keys = theKeys;
				this.values = theValues;
				this.cssClasses = theCssClasses;
			}
		};
		setLabelIfNotNull(i18nLabel, ss);
		formLayout.add(ss);
		return ss;
	}

	/**
	 * Add a static text, with a label's i18n key being the same as the <code>name<code>.
	 * If you do not want a label, use the {@link FormUIFactory#addStaticTextElement(String, String, String, FormItemContainer)} 
	 * method with the <code>i18nKey</code> and set it <code>null</code>
	 * 
	 * @param name
	 * @param translatedText
	 * @param formLayout
	 * @return
	 */
	public StaticTextElement addStaticTextElement(String name, String translatedText, FormItemContainer formLayout) {
		return addStaticTextElement(name, name, translatedText, formLayout);
	}

	/**
	 * Add a static text.
	 * 
	 * @param name
	 * @param i18nLabel
	 * @param translatedText
	 * @param formLayout
	 * @return
	 */
	public StaticTextElement addStaticTextElement(String name, String i18nLabel, String translatedText, FormItemContainer formLayout) {
		StaticTextElement ste = new StaticTextElementImpl(name, translatedText);
		setLabelIfNotNull(i18nLabel, ste);
		formLayout.add(ste);
		return ste;
	}

	public TextElement addInlineTextElement(String name, String value, FormItemContainer formLayout, FormBasicController listener) {
		TextElement ie = new TextElementImpl(name, value, TextElementImpl.HTML_INPUT_TYPE_TEXT, true);
		ie.addActionListener(listener, FormEvent.ONCLICK);
		if (listener != null) {
			formLayout.add(ie);
		}
		return ie;
	}

	public IntegerElement addInlineIntegerElement(String name, int initVal, FormItemContainer formLayout, FormBasicController listener) {
		IntegerElement iie = new IntegerElementImpl(name, initVal, true);
		iie.addActionListener(listener, FormEvent.ONCLICK);
		if (listener != null) {
			formLayout.add(iie);
		}
		return iie;
	}

	/**
	 * Inserts an HTML horizontal bar (&lt;HR&gt;) element.
	 * 
	 * @param name
	 * @param formLayout
	 * @return
	 */
	public SpacerElement addSpacerElement(String name, FormItemContainer formLayout, boolean onlySpaceAndNoLine) {
		SpacerElement spacer = new SpacerElementImpl(name);
		if (onlySpaceAndNoLine) {
			spacer.setSpacerCssClass("b_form_spacer_noline");
		}
		formLayout.add(spacer);
		return spacer;
	}

	/**
	 * adds a given text formatted in example style as part of the form.
	 * 
	 * @param name
	 * @param text
	 * @param formLayout
	 * @return
	 */
	public FormItem addStaticExampleText(String name, String text, FormItemContainer formLayout) {
		return addStaticExampleText(name, name, text, formLayout);
	}

	/**
	 * @param name
	 * @param i18nLabel i18n key for label, null to disable
	 * @param text
	 * @param formLayout
	 * @return
	 */
	public FormItem addStaticExampleText(String name, String i18nLabel, String text, FormItemContainer formLayout) {
		final SimpleExampleText set = new SimpleExampleText(name, text);
		// wrap the SimpleExampleText Component within a FormItem
		FormItem fiWrapper = new FormItemImpl("simpleExampleTextWrapper_" + name) {

			@Override
			protected Component getFormItemComponent() {
				return set;
			}

			@Override
			@SuppressWarnings({ "unused", "unchecked" })
			public void validate(List validationResults) {
				// nothing to do
			}

			@Override
			protected void rootFormAvailable() {
				// nothing to do
			}

			@Override
			public void reset() {
				// nothing to do
			}

			@Override
			@SuppressWarnings("unused")
			public void evalFormRequest(UserRequest ureq) {
				// nothing to do
			}

		};
		setLabelIfNotNull(i18nLabel, fiWrapper);
		formLayout.add(fiWrapper);
		return fiWrapper;
	}

	/**
	 * @param name
	 * @param maxLen
	 * @param initialValue
	 * @param i18nLabel
	 * @param formLayout
	 * @return
	 */
	public TextElement addTextElement(String name, final String i18nLabel, final int maxLen, String initialValue, FormItemContainer formLayout) {
		TextElement te = new TextElementImpl(name, initialValue);
		te.setNotLongerThanCheck(maxLen, "text.element.error.notlongerthan");
		setLabelIfNotNull(i18nLabel, te);
		te.setMaxLength(maxLen);
		formLayout.add(te);
		return te;
	}

	/**
	 * adds a component to choose text elements with autocompletion see also TextBoxListComponent
	 * 
	 * @param name
	 * @param i18nLabel
	 * @param inputHint if empty ("") a default will be used
	 * @param initialItems
	 * @param formLayout
	 * @param translator
	 * @return
	 */
	public TextBoxListElement addTextBoxListElement(String name, final String i18nLabel, String inputHint, Map<String, String> initialItems,
			FormItemContainer formLayout, Translator translator) {
		TextBoxListElement tbe = new TextBoxListElementImpl(name, inputHint, initialItems, translator);
		setLabelIfNotNull(i18nLabel, tbe);
		formLayout.add(tbe);
		return tbe;
	}

	public TextElement addPasswordElement(String name, final String i18nLabel, final int maxLen, String initialValue, FormItemContainer formLayout) {
		TextElement te = new TextElementImpl(name, initialValue, TextElementImpl.HTML_INPUT_TYPE_PASSWORD);
		te.setNotLongerThanCheck(maxLen, "text.element.error.notlongerthan");
		setLabelIfNotNull(i18nLabel, te);
		te.setMaxLength(maxLen);
		formLayout.add(te);
		return te;
	}

	/**
	 * Add a multi line text element, using the provided name as i18n key for the label, no max length check set, and fits content hight at maximium (100lnes).
	 * 
	 * @see FormUIFactory#addTextAreaElement(String, String, int, int, int, boolean, String, FormItemContainer)
	 * @param name
	 * @param rows
	 * @param cols
	 * @param initialValue
	 * @param formLayout
	 * @return
	 */
	public TextElement addTextAreaElement(String name, final int rows, final int cols, String initialValue, FormItemContainer formLayout) {
		return addTextAreaElement(name, name, -1, rows, cols, true, initialValue, formLayout);
	}

	/**
	 * Add a multi line text element
	 * 
	 * @param name
	 * @param i18nLabel i18n key for the label or null to set no label at all.
	 * @param maxLen
	 * @param rows the number of lines or -1 to use default value
	 * @param cols the number of characters per line or -1 to use 100% of the available space
	 * @param isAutoHeightEnabled true: element expands to fit content height, (max 100 lines); false: specified rows used
	 * @param initialValue Initial value
	 * @param formLayout
	 * @return
	 */
	public TextElement addTextAreaElement(String name, final String i18nLabel, final int maxLen, final int rows, final int cols, boolean isAutoHeightEnabled,
			String initialValue, FormItemContainer formLayout) {
		TextElement te = new TextAreaElementImpl(name, initialValue, rows, cols, isAutoHeightEnabled) {
			{
				setNotLongerThanCheck(maxLen, "text.element.error.notlongerthan");
				// the text.element.error.notlongerthan uses a variable {0} that
				// contains the length maxLen
			}
		};
		setLabelIfNotNull(i18nLabel, te);
		formLayout.add(te);
		return te;
	}

	/**
	 * Add a rich text formattable element that offers basic formatting functionality and loads the data form the given string value. Use item.getEditorConfiguration() to
	 * add more editor features if you need them
	 * 
	 * @param name Name of the form item
	 * @param i18nLabel The i18n key of the label or NULL when no label is used
	 * @param initialValue The initial value or NULL if no initial value is available
	 * @param rows The number of lines the editor should offer. Use -1 to indicate no specific height
	 * @param cols The number of characters width the editor should offer. Use -1 to indicate no specific width
	 * @param externalToolbar true: use an external toolbar that is only visible when the user clicks into the text area; false: use the static toolbar
	 * @param formLayout The form item container where to add the rich text element
	 * @param usess The user session that dispatches the images
	 * @param wControl the current window controller
	 * @param wControl the current window controller
	 * @return The rich text element instance
	 */
	public RichTextElement addRichTextElementForStringDataMinimalistic(String name, final String i18nLabel, String initialHTMLValue, final int rows, final int cols,
			boolean externalToolbar, FormItemContainer formLayout, UserSession usess, WindowControl wControl) {
		// Create richt text element with bare bone configuration
		RichTextElement rte = new RichTextElementImpl(name, initialHTMLValue, rows, cols, formLayout.getRootForm(), wControl.getWindowBackOffice());
		setLabelIfNotNull(i18nLabel, rte);
		// Now configure editor
		rte.getEditorConfiguration().setConfigProfileFormEditorMinimalistic(usess, externalToolbar, wControl.getWindowBackOffice().getWindow().getGuiTheme());
		// Add to form and finish
		formLayout.add(rte);
		return rte;
	}

	/**
	 * Add a rich text formattable element that offers simple formatting functionality and loads the data form the given string value. Use item.getEditorConfiguration()
	 * to add more editor features if you need them
	 * 
	 * @param name Name of the form item
	 * @param i18nLabel The i18n key of the label or NULL when no label is used
	 * @param initialValue The initial value or NULL if no initial value is available
	 * @param rows The number of lines the editor should offer. Use -1 to indicate no specific height
	 * @param cols The number of characters width the editor should offer. Use -1 to indicate no specific width
	 * @param externalToolbar true: use an external toolbar that is only visible when the user clicks into the text area; false: use the static toolbar
	 * @param fullProfile false: load only the necessary plugins; true: load all plugins from the full profile
	 * @param baseContainer The VFS container where to load resources from (images etc) or NULL to not allow embedding of media files at all
	 * @param formLayout The form item container where to add the richt text element
	 * @param customLinkTreeModel A custom link tree model or NULL not not use a custom model
	 * @param formLayout The form item container where to add the rich text element
	 * @param usess The user session that dispatches the images
	 * @param wControl the current window controller
	 * @param wControl the current window controller
	 * @return The rich text element instance
	 */
	public RichTextElement addRichTextElementForStringData(String name, final String i18nLabel, String initialHTMLValue, final int rows, final int cols,
			boolean externalToolbar, boolean fullProfile, VFSContainer baseContainer, CustomLinkTreeModel customLinkTreeModel, FormItemContainer formLayout,
			UserSession usess, WindowControl wControl) {
		// Create richt text element with bare bone configuration
		RichTextElement rte = new RichTextElementImpl(name, initialHTMLValue, rows, cols, formLayout.getRootForm(), wControl.getWindowBackOffice());
		setLabelIfNotNull(i18nLabel, rte);
		// Now configure editor
		rte.getEditorConfiguration().setConfigProfileFormEditor(fullProfile, usess, externalToolbar, wControl.getWindowBackOffice().getWindow().getGuiTheme(),
				baseContainer, customLinkTreeModel);
		// Add to form and finish
		formLayout.add(rte);
		return rte;
	}

	/**
	 * Add a rich text formattable element that offers complex formatting functionality and loads the data from the given file path. Use item.getEditorConfiguration() to
	 * add more editor features if you need them
	 * 
	 * @param name Name of the form item
	 * @param i18nLabel The i18n key of the label or NULL when no label is used
	 * @param initialValue The initial value or NULL if no initial value is available
	 * @param rows The number of lines the editor should offer. Use -1 to indicate no specific height
	 * @param cols The number of characters width the editor should offer. Use -1 to indicate no specific width
	 * @param externalToolbar true: use an external toolbar that is only visible when the user clicks into the text area; false: use the static toolbar
	 * @param baseContainer The VFS container where to load resources from (images etc) or NULL to not allow embedding of media files at all
	 * @param relFilePath The path to the file relative to the baseContainer
	 * @param customLinkTreeModel A custom link tree model or NULL not not use a custom model
	 * @param formLayout The form item container where to add the rich text element
	 * @param usess The user session that dispatches the images
	 * @param wControl the current window controller
	 * @return The richt text element instance
	 */
	public RichTextElement addRichTextElementForFileData(String name, final String i18nLabel, String initialValue, final int rows, final int cols,
			boolean externalToolbar, VFSContainer baseContainer, String relFilePath, CustomLinkTreeModel customLinkTreeModel, FormItemContainer formLayout,
			UserSession usess, WindowControl wControl) {
		// Create richt text element with bare bone configuration
		RichTextElement rte = new RichTextElementImpl(name, initialValue, rows, cols, formLayout.getRootForm(), wControl.getWindowBackOffice());
		setLabelIfNotNull(i18nLabel, rte);
		// Now configure editor
		rte.getEditorConfiguration().setConfigProfileFileEditor(usess, externalToolbar, wControl.getWindowBackOffice().getWindow().getGuiTheme(), baseContainer,
				relFilePath, customLinkTreeModel);
		// Add to form and finish
		formLayout.add(rte);
		return rte;
	}

	public FormItem createSimpleLabelText(final String name, final String translatedText) {
		FormItem wrapper = new FormItemImpl(name) {

			SimpleLabelText mySimpleLabelTextC = new SimpleLabelText(name, translatedText);

			@SuppressWarnings("unchecked")
			@Override
			public void validate(@SuppressWarnings("unused") List validationResults) {
				// nothing to do

			}

			@Override
			protected void rootFormAvailable() {
				// nothing to do

			}

			@Override
			public void reset() {
				// nothing to do

			}

			@Override
			protected Component getFormItemComponent() {
				return mySimpleLabelTextC;
			}

			@Override
			public void evalFormRequest(@SuppressWarnings("unused") UserRequest ureq) {
				// nothing to do

			}

		};

		return wrapper;
	}

	/**
	 * Static text with the error look and feel.
	 * 
	 * @param name in velocity for <code>$r.render("name")</code>
	 * @param translatedText already translated text that should be displayed.
	 * @return
	 */
	public FormItem createSimpleErrorText(final String name, final String translatedText) {
		FormItem wrapper = new FormItemImpl(name) {

			SimpleFormErrorText mySimpleErrorTextC = new SimpleFormErrorText(name, translatedText);

			@SuppressWarnings("unchecked")
			@Override
			public void validate(@SuppressWarnings("unused") List validationResults) {
				// nothing to do

			}

			@Override
			protected void rootFormAvailable() {
				// nothing to do

			}

			@Override
			public void reset() {
				// nothing to do

			}

			@Override
			protected Component getFormItemComponent() {
				return mySimpleErrorTextC;
			}

			@Override
			public void evalFormRequest(@SuppressWarnings("unused") UserRequest ureq) {
				// nothing to do

			}

		};

		return wrapper;
	}

	/**
	 * @see FlexiTableDataModel and its implementations
	 * @param name
	 * @param tableModel
	 * @param formLayout
	 * @return
	 */
	public FlexiTableElment addTableElement(String name, FlexiTableDataModel tableModel, FormItemContainer formLayout) {
		FlexiTableElementImpl fte = new FlexiTableElementImpl(name, tableModel);
		formLayout.add(fte);
		return fte;
	}

	/**
	 * creates a form link with the given name which acts also as command, i18n and component name.
	 * 
	 * @param name
	 * @param formLayout
	 * @return
	 */
	public FormLink addFormLink(String name, FormItemContainer formLayout) {
		FormLinkImpl fte = new FormLinkImpl(name);
		formLayout.add(fte);
		return fte;
	}

	/**
	 * Add a form link with the option to choose the presentation, the <code>name</code> parameter is taken as to be used in <code>$r.render("<name>")</code>, as i18nkey
	 * for the link text, and also the cmd string.
	 * <p>
	 * If different values are needed for name, i18nkey link text, use the {@link FormUIFactory#addFormLink(String, String, String, FormItemContainer, int)}. This allows
	 * also to set the i18n key for label.
	 * 
	 * @param name The name of the form element (identifyer), also used as i18n key
	 * @param formLayout
	 * @param presentation See Link.BUTTON etc
	 * @return
	 */
	public FormLink addFormLink(String name, FormItemContainer formLayout, int presentation) {
		FormLinkImpl fte = new FormLinkImpl(name, name, name, presentation);
		formLayout.add(fte);
		return fte;
	}

	/**
	 * @param name to be used to render in velocity <code>$r.render("name")</code>
	 * @param i18nLink i18n key for the link text
	 * @param i18nLabel i18n key for the link elements label, maybe <code>null</code>
	 * @param formLayout FormLink is added as element here
	 * @param presentation See Link.BUTTON etc.
	 * @return
	 */
	public FormLink addFormLink(String name, String i18nLink, String i18nLabel, FormItemContainer formLayout, int presentation) {
		FormLinkImpl fte = new FormLinkImpl(name, name, i18nLink, presentation);
		setLabelIfNotNull(i18nLabel, fte);
		formLayout.add(fte);
		return fte;
	}

	/**
	 * Add a form link with a special css class
	 * 
	 * @param name The name of the form element (identifyer), also used as i18n key
	 * @param formLayout
	 * @param css class
	 * @return
	 */
	public FormLink addFormLink(String name, FormItemContainer formLayout, String customEnabledLinkCSS) {
		FormLinkImpl fte = new FormLinkImpl(name);
		fte.setCustomEnabledLinkCSS(customEnabledLinkCSS);
		formLayout.add(fte);
		return fte;
	}

	/**
	 * add a toggle which handles on/off state itself and can be asked for status with " isOn() ".
	 * 
	 * @param name the name of the element (identifier), also used as i18n key
	 * @param toggleText null if the i18n key should be used and translated, or a text to be on the toggle
	 * @param formLayout
	 * @param toggledOnCSS a special css class for the on state, or null for default
	 * @param toggledOffCSS a special css class for the off state, or null for default
	 * @return
	 */
	public FormToggle addToggleButton(String name, String toggleText, FormItemContainer formLayout, String toggledOnCSS, String toggledOffCSS) {
		FormToggleImpl fte;
		if (StringHelper.containsNonWhitespace(toggleText)) {
			fte = new FormToggleImpl(name, name, toggleText, Link.NONTRANSLATED);
		} else {
			fte = new FormToggleImpl(name, name, name);
		}
		if (toggledOnCSS != null) fte.setToggledOnCSS(toggledOnCSS);
		if (toggledOffCSS != null) fte.setToggledOffCSS(toggledOffCSS);
		formLayout.add(fte);
		return fte;
	}

	/**
	 * Add a file upload element, with a label's i18n key being the same as the <code>name<code>.
	 * If you do not want a label, use the {@link FormUIFactory#addFileElement(String, String, FormItemContainer)} 
	 * method with <code>null</code> value for the <code>i18nKey</code>.
	 * 
	 * @param name
	 * @param formLayout
	 * @return
	 */
	public FileElement addFileElement(String name, FormItemContainer formLayout) {
		return addFileElement(name, name, formLayout);
	}

	/**
	 * Add a file upload element
	 * 
	 * @param name
	 * @param i18nKey
	 * @param formLayout
	 * @return
	 */
	public FileElement addFileElement(String name, String i18nLabel, FormItemContainer formLayout) {
		FileElement fileElement = new FileElementImpl(name);
		setLabelIfNotNull(i18nLabel, fileElement);
		formLayout.add(fileElement);
		return fileElement;
	}

	/**
	 * Add a form submit button.
	 * 
	 * @param name the button name (identifyer) and at the same time the i18n key of the button
	 * @param formItemContiner The container where to add the button
	 * @return the new form button
	 */
	public FormSubmit addFormSubmitButton(String name, FormItemContainer formLayout) {
		return addFormSubmitButton(name, name, formLayout);
	}

	/**
	 * Add a form submit button.
	 * 
	 * @param name the button name (identifyer)
	 * @param i18nKey The display key
	 * @param formItemContiner The container where to add the button
	 * @return the new form button
	 */
	public FormSubmit addFormSubmitButton(String name, String i18nKey, FormItemContainer formLayout) {
		FormSubmit subm = new FormSubmit(name, i18nKey);
		formLayout.add(subm);
		return subm;
	}

	/**
	 * Add a form cancel button. You must implement the formCancelled() method in your FormBasicController to get events fired by this button
	 * 
	 * @param name
	 * @param formLayoutContainer
	 * @param ureq
	 * @param wControl
	 * @return
	 */
	public FormCancel addFormCancelButton(String name, FormLayoutContainer formLayoutContainer, UserRequest ureq, WindowControl wControl) {
		FormCancel cancel = new FormCancel(name, formLayoutContainer, ureq, wControl);
		formLayoutContainer.add(cancel);
		return cancel;
	}

}
