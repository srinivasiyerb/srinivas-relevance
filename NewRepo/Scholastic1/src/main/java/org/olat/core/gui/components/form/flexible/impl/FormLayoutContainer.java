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
package org.olat.core.gui.components.form.flexible.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.olat.core.gui.GUIInterna;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.FormItemDependencyRule;
import org.olat.core.gui.components.form.flexible.FormLayouter;
import org.olat.core.gui.components.form.flexible.FormMultipartItem;
import org.olat.core.gui.components.form.flexible.elements.Submit;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Disposable;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;

/**
 * Description:<br>
 * TODO: patrickb Class Description for FormVelocityContainer
 * <P>
 * Initial Date: 22.11.2006 <br>
 * 
 * @author patrickb
 */
public class FormLayoutContainer extends FormItemImpl implements FormItemContainer, FormLayouter, Disposable {
	//
	private static final String PACKAGE = Util.getPackageName(FormLayoutContainer.class);
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(PACKAGE);
	private static final String LAYOUT_DEFAULT = VELOCITY_ROOT + "/form_default.html";
	private static final String LAYOUT_HORIZONTAL = VELOCITY_ROOT + "/form_horizontal.html";
	private static final String LAYOUT_VERTICAL = VELOCITY_ROOT + "/form_vertical.html";
	private static final String LAYOUT_SELBOX = VELOCITY_ROOT + "/form_selbox.html";
	private static final String LAYOUT_BUTTONGROUP = VELOCITY_ROOT + "/form_buttongroup.html";
	//

	/**
	 * manage the form components of this form container
	 */
	protected VelocityContainer formLayoutContainer;
	/**
	 * formComponents and formComponentNames are managed together, change something here needs a change there. formComponents contain the FormItem based on their name
	 * formComponentsNames is use in the velocity to render according to the registered name. The addXXX method adds elements -> The register method register an element
	 * only -> used for setErrorComponent / setLabelComponent.
	 */
	protected Map<String, FormItem> formComponents;
	protected List formComponentsNames;
	protected Map<String, FormItem> listeningOnlyFormComponents;
	private boolean hasRootForm = false;
	private HashMap<String, Map> dependencyRules = null;

	/**
	 * Form layout is provided by caller, access the form item to render inside the velocity container by
	 * <ul>
	 * <li>name to get the form field</li>
	 * <li>name_LABEL</li>
	 * <li>name_ERROR</li>
	 * <li>name_EXAMPLE</li>
	 * </ul>
	 * You can also access form item information like
	 * <ul>
	 * <li>$f.hasError(name)</li>
	 * <li>$f.hasLabel(name)</li>
	 * <li>$f.hasExample(name)</li>
	 * </ul>
	 * which helps you for layouting the form correct.
	 * 
	 * @param name
	 * @param translator
	 */
	private FormLayoutContainer(String name, Translator formTranslator, String page) {
		super(name);
		//
		this.formLayoutContainer = new VelocityContainer(name, page, formTranslator, null);
		this.translator = formTranslator;//
		// add the form decorator for the $f.hasError("ddd") etc.
		formLayoutContainer.contextPut("f", new FormDecoratorImpl(this));
		// this container manages the form items, the GUI form item componentes are
		// managed in the associated velocitycontainer
		formComponentsNames = new ArrayList(5);
		formLayoutContainer.contextPut("formitemnames", formComponentsNames);
		formComponents = new HashMap<String, FormItem>();
		//
		listeningOnlyFormComponents = new HashMap<String, FormItem>();
		//
		dependencyRules = new HashMap<String, Map>();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormComponent#rememberFormRequest(org.olat.core.gui.UserRequest, long[], int)
	 */
	@Override
	@SuppressWarnings("unused")
	public void evalFormRequest(UserRequest ureq) {
		// form layouter has no values to store temporary
	}

	@Override
	@SuppressWarnings("unused")
	public void validate(List validationResults) {
		// form layouter is not validating
	}

	@Override
	public void reset() {
		// form layouter can not be resetted
	}

	@Override
	protected void rootFormAvailable() {
		// could initialize all formComponents with rootform
		// simpler -> you can not add before adding rootform
		hasRootForm = true;
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormContainer#add(java.lang.String, org.olat.core.gui.components.form.flexible.FormComponentImpl)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void add(FormItem formComp) {
		add(formComp.getName(), formComp);
	}

	@Override
	public void add(String name, FormItem formComp) {
		if (!hasRootForm) { throw new AssertionError("first ensure that the layout container knows about its rootform!!"); }
		// set the formtranslator, and parent
		Translator itemTranslator = formComp.getTranslator();
		if (formComp.getTranslator() != null && itemTranslator instanceof PackageTranslator) {
			// let the FormItem provide a more specialized translator
			PackageTranslator itemPt = (PackageTranslator) itemTranslator;
			itemTranslator = PackageTranslator.cascadeTranslators(itemPt, translator);
		} else {
			itemTranslator = translator;
		}
		formComp.setTranslator(itemTranslator);
		formComp.setRootForm(getRootForm());
		//
		String formCompName = name;
		// book keeping of FormComponent order
		formComponentsNames.add(formCompName);
		formComponents.put(formCompName, formComp);

		if (GUIInterna.isLoadPerformanceMode()) {
			// sort of "register" the new FormItem
			// so that it will have a replayableID
			getRootForm().getReplayableDispatchID(formComp.getComponent());
		}

		/*
		 * add the gui representation
		 */
		formLayoutContainer.put(formCompName, formComp.getComponent());
		formLayoutContainer.put(formCompName + FormItem.ERRORC, formComp.getErrorC());
		formLayoutContainer.put(formCompName + FormItem.EXAMPLEC, formComp.getExampleC());
		formLayoutContainer.put(formCompName + FormItem.LABELC, formComp.getLabelC());
		/*
		 * recognize submits and register it for the IE enter-pressing
		 */
		if (formComp instanceof Submit) {
			getRootForm().registerSubmit(formComp);
		}
		// Check for multipart data, add upload limit to form
		if (formComp instanceof FormMultipartItem) {
			FormMultipartItem mpItem = (FormMultipartItem) formComp;
			getRootForm().setMultipartEnabled(true, mpItem.getMaxUploadSizeKB());
		}

	}

	@Override
	public void add(String name, Collection<FormItem> foItems) {

		// remove if already in
		if (formLayoutContainer.getContext().containsKey(name)) {
			// remove existing collection
			formLayoutContainer.contextRemove(name);
			// remove all associated form items
			for (FormItem formItem : foItems) {
				remove(formItem);
			}
		}

		// make collection accessible with <name> in the container.
		// collection contains then only the names.
		List<String> foItemsCollectionAsNames = new ArrayList<String>();
		formLayoutContainer.contextPut(name, foItemsCollectionAsNames);

		// add all items as form items to the container
		for (FormItem formItem : foItems) {
			String foName = formItem.getName();
			add(foName, formItem);
			foItemsCollectionAsNames.add(foName);
		}

	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormItemContainer#register(org.olat.core.gui.components.form.flexible.FormItem)
	 */
	@Override
	public void register(FormItem formComp) {
		if (!hasRootForm) { throw new AssertionError("first ensure that the layout container knows about its rootform!!"); }
		// set the formtranslator, and parent
		Translator itemTranslator = formComp.getTranslator();
		if (formComp.getTranslator() != null && itemTranslator instanceof PackageTranslator) {
			// let the FormItem provide a more specialized translator
			PackageTranslator itemPt = (PackageTranslator) itemTranslator;
			itemTranslator = PackageTranslator.cascadeTranslators(itemPt, translator);
		} else {
			itemTranslator = translator;
		}
		formComp.setTranslator(itemTranslator);
		formComp.setRootForm(getRootForm());
		//
		String formCompName = formComp.getName();
		// book keeping of FormComponent order

		listeningOnlyFormComponents.put(formCompName, formComp);

		if (GUIInterna.isLoadPerformanceMode()) {
			// sort of "register" the new FormItem
			// so that it will have a replayableID
			getRootForm().getReplayableDispatchID(formComp.getComponent());
		}

	}

	@Override
	public void remove(FormItem toBeRemoved) {
		String formCompName = toBeRemoved.getName();
		remove(formCompName, toBeRemoved);
	}

	public void replace(FormItem toBeReplaced, FormItem with) {
		String formCompName = toBeReplaced.getName();
		int pos = formComponentsNames.indexOf(formCompName);
		formComponentsNames.add(pos, with.getName());
		formComponentsNames.remove(formCompName);
		/*
		 * remove the gui representation
		 */
		formLayoutContainer.remove(toBeReplaced.getComponent());
		formLayoutContainer.remove(toBeReplaced.getErrorC());
		formLayoutContainer.remove(toBeReplaced.getExampleC());
		formLayoutContainer.remove(toBeReplaced.getLabelC());

		// set the formtranslator, and parent
		Translator itemTranslator = with.getTranslator();
		if (with.getTranslator() != null && itemTranslator instanceof PackageTranslator) {
			// let the FormItem provide a more specialized translator
			PackageTranslator itemPt = (PackageTranslator) itemTranslator;
			itemTranslator = PackageTranslator.cascadeTranslators(itemPt, translator);
		} else {
			itemTranslator = translator;
		}
		with.setTranslator(itemTranslator);
		with.setRootForm(getRootForm());

		formComponents.put(formCompName, with);
		/*
		 * add the gui representation
		 */
		formLayoutContainer.put(formCompName, with.getComponent());
		formLayoutContainer.put(formCompName + FormItem.ERRORC, with.getErrorC());
		formLayoutContainer.put(formCompName + FormItem.EXAMPLEC, with.getExampleC());
		formLayoutContainer.put(formCompName + FormItem.LABELC, with.getLabelC());
		/*
		 * recognize submits and register it for the IE enter-pressing
		 */
		if (with instanceof Submit) {
			getRootForm().registerSubmit(with);
		}
		// Check for multipart data, add upload limit to form
		if (with instanceof FormMultipartItem) {
			FormMultipartItem mpItem = (FormMultipartItem) with;
			getRootForm().setMultipartEnabled(true, mpItem.getMaxUploadSizeKB());
		}

	}

	/**
	 * remove the component with the give name from this container
	 * 
	 * @param name
	 */
	@Override
	public void remove(String formCompName) {
		FormItem toBeRemoved = getFormComponent(formCompName);
		remove(formCompName, toBeRemoved);
	}

	private void remove(String formCompName, FormItem toBeRemoved) {
		// book keeping of FormComponent order
		formComponentsNames.remove(formCompName);
		formComponents.remove(formCompName);
		/*
		 * remove the gui representation
		 */
		formLayoutContainer.remove(toBeRemoved.getComponent());
		formLayoutContainer.remove(toBeRemoved.getErrorC());
		formLayoutContainer.remove(toBeRemoved.getExampleC());
		formLayoutContainer.remove(toBeRemoved.getLabelC());
	}

	@Override
	protected Component getFormItemComponent() {
		return formLayoutContainer;
	}

	@Override
	public Map<String, FormItem> getFormComponents() {
		Map<String, FormItem> merged = new HashMap<String, FormItem>(formComponents);
		merged.putAll(listeningOnlyFormComponents);
		return Collections.unmodifiableMap(merged);
	}

	@Override
	public FormItem getFormComponent(String name) {
		return getFormComponents().get(name);
	}

	@Override
	public void contextPut(String key, Object value) {
		formLayoutContainer.contextPut(key, value);
	}

	public void contextRemove(String key) {
		formLayoutContainer.contextRemove(key);
	}

	@Override
	public void put(String name, Component component) {
		formLayoutContainer.put(name, component);
	}

	@Override
	public void remove(Component component) {
		formLayoutContainer.remove(component);
	}

	@Override
	public Component getComponent(String name) {
		return formLayoutContainer.getComponent(name);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormLayouter#setDirty(boolean)
	 */
	@Override
	public void setDirty(boolean dirty) {
		formLayoutContainer.setDirty(dirty);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.api.FormItemContainer#addDependencyRule(org.olat.core.gui.components.form.flexible.api.FormItemDependencyRule)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void addDependencyRule(FormItemDependencyRule depRule) {
		String key = depRule.getTriggerElement().getName();
		Map<String, FormItemDependencyRule> rules;
		if (dependencyRules.containsKey(key)) {
			// already rules for this element
			rules = dependencyRules.get(key);
		} else {
			// no rules yet, create
			rules = new HashMap<String, FormItemDependencyRule>();
			dependencyRules.put(key, rules);
		}
		rules.put(depRule.getIdentifier(), depRule);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.api.FormItemContainer#evalDependencyRuleSetFor(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.api.FormItem)
	 */
	@Override
	public void evalDependencyRuleSetFor(UserRequest ureq, FormItem dispatchFormItem) {
		String key = dispatchFormItem.getName();
		if (dependencyRules.containsKey(key)) {
			Map<String, FormItemDependencyRule> ruleSet = dependencyRules.get(key);
			Collection<FormItemDependencyRule> rules = ruleSet.values();
			for (Iterator iter = rules.iterator(); iter.hasNext();) {
				FormItemDependencyRule tmp = (FormItemDependencyRule) iter.next();
				if (tmp.applyRule(this)) this.setDirty(true);
			}
		}

	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormItemImpl#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean isEnabled) {
		// enable / disable this
		super.setEnabled(isEnabled);
		// iterate over all components and disable / enable them
		Collection formItems = getFormComponents().values();
		for (Iterator iter = formItems.iterator(); iter.hasNext();) {
			FormItem element = (FormItem) iter.next();
			element.setEnabled(isEnabled);
		}
	}

	/**
	 * Create a default layout container with the standard label - element alignment.
	 * 
	 * @param name
	 * @param formTranslator
	 * @return
	 */
	public static FormLayoutContainer createDefaultFormLayout(String name, Translator formTranslator) {
		FormLayoutContainer tmp = new FormLayoutContainer(name, formTranslator, LAYOUT_DEFAULT);
		return tmp;
	}

	/**
	 * Create a layout container that renders the form elements and its labels vertically.
	 * 
	 * @param name
	 * @param formTranslator
	 * @return
	 */
	public static FormLayoutContainer createHorizontalFormLayout(String name, Translator formTranslator) {
		FormLayoutContainer tmp = new FormLayoutContainer(name, formTranslator, LAYOUT_HORIZONTAL);
		return tmp;
	}

	/**
	 * Create a layout container that renders the form elements and its labels vertically. This means that the label of an element is forced to be on a separate line
	 * without any left indent.
	 * 
	 * @param name
	 * @param formTranslator
	 * @return
	 */
	public static FormLayoutContainer createVerticalFormLayout(String name, Translator formTranslator) {
		FormLayoutContainer tmp = new FormLayoutContainer(name, formTranslator, LAYOUT_VERTICAL);
		return tmp;
	}

	/**
	 * Create a layout container that should be only used to render buttons using a b_button_group css wrapper. Buttons are ususally rendered on one line without indent
	 * 
	 * @param name
	 * @param formTranslator
	 * @return
	 */
	public static FormLayoutContainer createButtonLayout(String name, Translator formTranslator) {
		FormLayoutContainer tmp = new FormLayoutContainer(name, formTranslator, LAYOUT_BUTTONGROUP);
		return tmp;
	}

	/**
	 * workaround FIXME:pb
	 */
	public static FormLayouter createSelbox(String name, Translator formTranslator) {
		FormLayouter tmp = new FormLayoutContainer(name, formTranslator, LAYOUT_SELBOX);
		return tmp;
	}

	/**
	 * @param name
	 * @param formTranslator
	 * @param page
	 * @return
	 */
	public static FormLayoutContainer createCustomFormLayout(String name, Translator formTranslator, String page) {
		FormLayoutContainer tmp = new FormLayoutContainer(name, formTranslator, page);
		return tmp;
	}

	@Override
	public void setTranslator(Translator translator) {
		super.setTranslator(translator);
		// set also translator on velocity container delegate
		this.formLayoutContainer.setTranslator(translator);
	}

	/**
	 * Dispose all child elements from this container
	 * 
	 * @see org.olat.core.gui.control.Disposable#dispose()
	 */
	@Override
	public void dispose() {
		// Dispose also disposable form items (such as file uploads that needs to
		// cleanup temporary files)
		Map<String, FormItem> formItems = this.getFormComponents();
		for (FormItem formItem : formItems.values()) {
			if (formItem instanceof Disposable) {
				Disposable disposableFormItem = (Disposable) formItem;
				disposableFormItem.dispose();
			}
		}
	}

}
