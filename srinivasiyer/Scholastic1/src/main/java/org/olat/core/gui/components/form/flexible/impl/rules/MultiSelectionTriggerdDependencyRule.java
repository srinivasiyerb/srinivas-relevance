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
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;

/**
 * Description:<br>
 * TODO: patrickb Class Description for MultiSelectionTriggerdDependencyRule
 * <P>
 * Initial Date: 11.02.2007 <br>
 * 
 * @author patrickb
 */
class MultiSelectionTriggerdDependencyRule extends FormItemDependencyRuleImpl implements FormItemDependencyRule {

	public MultiSelectionTriggerdDependencyRule(MultipleSelectionElement triggerElement, String triggerValue, Set<FormItem> targets, int type) {
		super(triggerElement, triggerValue, targets, type);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.rules.FormItemDependencyRuleImpl#doesTrigger()
	 */
	@Override
	protected boolean doesTrigger() {
		MultipleSelectionElement mse = (MultipleSelectionElement) this.triggerElement;
		Set<String> selectedKeys = mse.getSelectedKeys();
		//
		boolean retval = false;
		if (this.triggerVal == null) {
			// meaning no selection in a multi selection element
			retval = selectedKeys.size() == 0;
		} else {
			// check that value is in selection
			retval = selectedKeys.contains(this.triggerVal);
		}
		//
		return retval;
		//
	}

}
