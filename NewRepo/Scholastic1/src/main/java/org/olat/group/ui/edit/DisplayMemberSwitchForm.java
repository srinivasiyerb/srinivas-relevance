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
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.group.ui.edit;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;

/**
 * Description:<BR>
 * Form having a save button which applies to two checkboxes asking for showing/hiding the owners, partipiciants respectively, to the partipiciants.
 * <P>
 * Initial Date: Sep 22, 2004
 * 
 * @author patrick
 */

public class DisplayMemberSwitchForm extends FormBasicController {

	private SelectionElement showOwners, showPartips, showWaitingList;
	private final boolean hasOwners, hasPartips, hasWaitingList;

	/**
	 * @param name
	 * @param transl
	 * @param hasPartips
	 * @param hasOwners
	 */
	public DisplayMemberSwitchForm(final UserRequest ureq, final WindowControl wControl, final boolean hasOwners, final boolean hasPartips, final boolean hasWaitingList) {
		super(ureq, wControl);
		this.hasOwners = hasOwners;
		this.hasPartips = hasPartips;
		this.hasWaitingList = hasWaitingList;

		initForm(ureq);
	}

	/**
	 * wheter the Show Owners checkbox is checked or not
	 * 
	 * @return boolean
	 */
	public boolean getShowOwners() {
		if (showOwners == null) { return false; }
		return showOwners.isSelected(0);
	}

	/**
	 * wheter the Show Partipicants checkbox is checked or not
	 * 
	 * @return boolean
	 */
	public boolean getShowPartipiciants() {
		if (showPartips == null) { return false; }
		return showPartips.isSelected(0);
	}

	/**
	 * whether the Show WaitingList checkbox is checked or not
	 * 
	 * @return boolean
	 */
	public boolean getShowWaitingList() {
		if (showWaitingList == null) { return false; }
		return showWaitingList.isSelected(0);
	}

	/**
	 * wheter the Show Owners checkbox is checked or not
	 * 
	 * @param show
	 */
	public void setShowOwnersChecked(final boolean show) {
		showOwners.select("xx", show);
	}

	/**
	 * wheter the Show Partipicants checkbox is checked or not
	 * 
	 * @param show
	 */
	public void setShowPartipsChecked(final boolean show) {
		showPartips.select("xx", show);
	}

	/**
	 * wheter the Show WaitingList checkbox is checked or not
	 * 
	 * @param show
	 */
	public void setShowWaitingListChecked(final boolean show) {
		showWaitingList.select("xx", show);
	}

	/**
	 * @see org.olat.core.gui.components.Form#validate(org.olat.core.gui.UserRequest)
	 */
	public boolean validate() {
		return true;
	}

	public void setWaitingListReadOnly(final boolean b) {
		showWaitingList.setEnabled(b);
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		//
	}

	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		fireEvent(ureq, Event.CHANGED_EVENT);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		showOwners = uifactory.addCheckboxesVertical("ShowOwners", "chkBox.show.owners", formLayout, new String[] { "xx" }, new String[] { "" }, null, 1);
		showOwners.setVisible(hasOwners);
		showPartips = uifactory.addCheckboxesVertical("ShowPartips", "chkBox.show.partips", formLayout, new String[] { "xx" }, new String[] { "" }, null, 1);
		showPartips.setVisible(hasPartips);
		showWaitingList = uifactory.addCheckboxesVertical("ShowWaitingList", "chkBox.show.waitingList", formLayout, new String[] { "xx" }, new String[] { "" }, null, 1);
		showWaitingList.setVisible(hasWaitingList);

		showOwners.addActionListener(this, FormEvent.ONCLICK);
		showPartips.addActionListener(this, FormEvent.ONCLICK);
		showWaitingList.addActionListener(this, FormEvent.ONCLICK);
	}

	@Override
	protected void doDispose() {
		//
	}

}
