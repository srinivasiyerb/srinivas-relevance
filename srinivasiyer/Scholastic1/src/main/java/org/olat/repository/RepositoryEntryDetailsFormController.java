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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.repository;

import org.olat.ControllerFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.resource.OLATResource;

/**
 * Description:<br>
 * This form controller allows users to edit the repository details and upload an image
 * <P>
 * Initial Date: 16.07.2009 <br>
 * 
 * @author gnaegi
 */
public class RepositoryEntryDetailsFormController extends FormBasicController {

	private final boolean isSubWorkflow;
	private final RepositoryEntry entry;

	private TextElement displayName;
	private RichTextElement description;

	public RepositoryEntryDetailsFormController(final UserRequest ureq, final WindowControl wControl, final RepositoryEntry entry, final boolean isSubWorkflow) {
		super(ureq, wControl);
		this.entry = entry;
		this.isSubWorkflow = isSubWorkflow;
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		setFormTitle("details.entryinfoheader");
		setFormContextHelp("org.olat.repository", "rep-meta-desc.html", "help.hover.rep.detail");
		// Add static fields
		uifactory.addStaticTextElement("cif.id", entry.getResourceableId() == null ? "-" : entry.getResourceableId().toString(), formLayout);
		uifactory.addStaticTextElement("cif.initialAuthor", entry.getInitialAuthor() == null ? "-" : entry.getInitialAuthor().toString(), formLayout);
		// Add resource type
		String typeName = null;
		final OLATResource res = entry.getOlatResource();
		if (res != null) {
			typeName = res.getResourceableTypeName();
		}
		final StringBuilder typeDisplayText = new StringBuilder(100);
		if (typeName != null) { // add image and typename code
			final RepositoryEntryIconRenderer reir = new RepositoryEntryIconRenderer(ureq.getLocale());
			typeDisplayText.append("<span class=\"b_with_small_icon_left ");
			typeDisplayText.append(reir.getIconCssClass(entry));
			typeDisplayText.append("\">");
			final String tName = ControllerFactory.translateResourceableTypeName(typeName, ureq.getLocale());
			typeDisplayText.append(tName);
			typeDisplayText.append("</span>");
		} else {
			typeDisplayText.append(translate("cif.type.na"));
		}
		uifactory.addStaticExampleText("cif.type", typeDisplayText.toString(), formLayout);
		//
		uifactory.addSpacerElement("spacer1", formLayout, false);
		//
		displayName = uifactory.addTextElement("cif.displayname", "cif.displayname", 100, entry.getDisplayname(), formLayout);
		displayName.setDisplaySize(30);
		displayName.setMandatory(true);
		//
		description = uifactory.addRichTextElementForStringDataMinimalistic("cif.description", "cif.description",
				(entry.getDescription() != null ? entry.getDescription() : " "), 10, -1, false, formLayout, ureq.getUserSession(), getWindowControl());
		description.setMandatory(true);
		//
		final FormLayoutContainer buttonContainer = FormLayoutContainer.createButtonLayout("buttonContainer", getTranslator());
		formLayout.add("buttonContainer", buttonContainer);

		uifactory.addFormSubmitButton("submit", buttonContainer);
		if (!isSubWorkflow) {
			uifactory.addFormCancelButton("cancel", buttonContainer, ureq, getWindowControl());
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#validateFormLogic(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected boolean validateFormLogic(final UserRequest ureq) {
		// Check for empty display name
		if (!StringHelper.containsNonWhitespace(displayName.getValue())) {
			displayName.setErrorKey("cif.error.displayname.empty", new String[] {});
			return false;
		}
		displayName.clearError();
		// Check for empty description
		if (!StringHelper.containsNonWhitespace(description.getValue())) {
			description.setErrorKey("cif.error.description.empty", new String[] {});
			return false;
		}
		description.clearError();
		// Ok, passed all checks
		return super.validateFormLogic(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(final UserRequest ureq) {
		// update model
		entry.setDisplayname(displayName.getValue().trim());
		entry.setDescription(description.getValue().trim());
		// notify parent controller
		fireEvent(ureq, Event.CHANGED_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formCancelled(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formCancelled(final UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

}
