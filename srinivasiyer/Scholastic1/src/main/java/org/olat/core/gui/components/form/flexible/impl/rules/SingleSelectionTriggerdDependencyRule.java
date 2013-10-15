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
package org.olat.core.gui.components.form.flexible.impl.rules;

import java.util.Set;

import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemDependencyRule;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;

/**
 * Description:<br>
 * TODO: patrickb Class Description for VisibilityRule
 * <P>
 * Initial Date: 09.02.2007 <br>
 * 
 * @author patrickb
 */
class SingleSelectionTriggerdDependencyRule extends FormItemDependencyRuleImpl implements FormItemDependencyRule {

	/**
	 * for type
	 * <ul>
	 * <li>{@link FormItemDependencyRuleImpl#MAKE_INVISIBLE}</li>
	 * <li>{@link FormItemDependencyRuleImpl#MAKE_VISIBLE}</li>
	 * <li>{@link FormItemDependencyRuleImpl#MAKE_READONLY}</li>
	 * <li>{@link FormItemDependencyRuleImpl#MAKE_WRITABLE}</li>
	 * 
	 * @param triggerElement
	 * @param triggerValue
	 * @param targets
	 * @param type
	 */
	public SingleSelectionTriggerdDependencyRule(SingleSelection triggerElement, String triggerValue, Set<FormItem> targets, int type) {
		super(triggerElement, triggerValue, targets, type);
	}

	@Override
	protected boolean doesTrigger() {
		SingleSelection singlsel = (SingleSelection) this.triggerElement;
		String selected = singlsel.getSelectedKey();
		return selected.equals(this.triggerVal);
	}

}
