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
package org.olat.core.gui.components.form.flexible;

import java.util.Collection;
import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.impl.Form;

/**
 * Description:<br>
 * FormContainer and FormComponent -> Composite Pattern Implementors of FormContainer should also extend the olat Container and override its put(.., Component) methods by
 * throwing an exception -> use add instead.<br>
 * <P>
 * Initial Date: 24.11.2006 <br>
 * 
 * @author patrickb
 */
public interface FormItemContainer extends FormItem {

	/**
	 * add a formelement or container by adding subcomponents
	 * <ul>
	 * <li>name_LABEL</li>
	 * <li>name_ERROR</li>
	 * <li>name_EXAMPLE</li>
	 * </ul>
	 * 
	 * @param name
	 * @param formComp
	 */
	public void add(FormItem formComp);

	/**
	 * add with different name
	 * 
	 * @param name
	 * @param formComp
	 */
	public void add(String name, FormItem formComp);

	/**
	 * register only, does not addsubcomponents, does not expose formItem in the velocity. In 99% of the cases you should use an addXX method instead.
	 * 
	 * @param formComp
	 */
	public void register(FormItem formComp);

	/**
	 * remove the component from this container
	 * 
	 * @param formComp
	 */
	public void remove(FormItem formComp);

	/**
	 * remove the component with the give name from this container
	 * 
	 * @param name
	 */
	public void remove(String name);

	/**
	 * the form components managed by this container
	 * 
	 * @return
	 */
	public Map<String, FormItem> getFormComponents();

	/**
	 * @param name
	 * @return
	 */
	public FormItem getFormComponent(String name);

	/**
	 * the getter is defined on FormItem
	 * 
	 * @param rootForm
	 */
	@Override
	public void setRootForm(Form rootForm);

	/**
	 * add a dependency rule between a form item source and typically one or more form item targets. See {@link FormItemDependencyRule} for more information.
	 * 
	 * @see FormItemDependencyRule
	 * @param depRule
	 */
	public void addDependencyRule(FormItemDependencyRule depRule);

	/**
	 * @param ureq
	 * @param dispatchFormItem
	 */
	public void evalDependencyRuleSetFor(UserRequest ureq, FormItem formItem);

	/**
	 * @param string
	 * @param stepTitleLinks
	 */
	public void add(String string, Collection<FormItem> stepTitleLinks);
}
