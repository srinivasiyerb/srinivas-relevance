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

package org.olat.course.nodes.projectbroker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SpacerElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.course.editor.NodeEditController;
import org.olat.course.nodes.projectbroker.datamodel.CustomField;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerModuleConfiguration;

/**
 * @author guretzki
 */

public class CustomfieldsFormController extends FormBasicController {

	private static final int MAX_NBR_CUSTOM_FIELDS = 5;

	private final ProjectBrokerModuleConfiguration config;

	private final String ADD_FIELD_LINK = "customfield.add.field.link";

	String[] keys = new String[] { "customfield.table.enabled" };
	String[] values = new String[] { translate("customfield.table.enabled") };

	private FormSubmit formSubmit;
	private final List<TextElement> customFieldNameElementList;
	private final List<TextElement> customFieldValueElementList;
	private final List<MultipleSelectionElement> customFieldTableFlagElementList;
	private final List<FormLink> customFieldLinkElementList;
	private final List<SpacerElement> customFieldSpacerElementList;

	private final List<CustomField> customFields;

	/**
	 * Modules selection form.
	 * 
	 * @param name
	 * @param config
	 */
	public CustomfieldsFormController(final UserRequest ureq, final WindowControl wControl, final ProjectBrokerModuleConfiguration config) {
		super(ureq, wControl);
		this.config = config;
		customFieldNameElementList = new ArrayList<TextElement>();
		customFieldValueElementList = new ArrayList<TextElement>();
		customFieldTableFlagElementList = new ArrayList<MultipleSelectionElement>();
		customFieldLinkElementList = new ArrayList<FormLink>();
		customFieldSpacerElementList = new ArrayList<SpacerElement>();
		customFields = config.getCustomFields();
		initForm(this.flc, this, ureq);
	}

	/**
	 * @see org.olat.core.gui.components.Form#validate(org.olat.core.gui.UserRequest)
	 */
	public boolean validate() {
		return true;
	}

	/**
	 * Initialize form.
	 */
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		uifactory.addFormLink(ADD_FIELD_LINK, formLayout, Link.BUTTON_SMALL);
		uifactory.addSpacerElement("spacer", formLayout, false);

		createFormElements(formLayout);
	}

	private void createFormElements(final FormItemContainer formLayout) {

		// create form elements
		int i = 0;
		for (final Iterator<CustomField> iterator = customFields.iterator(); iterator.hasNext();) {
			final CustomField customField = iterator.next();
			createFormElemente(formLayout, i++, customField);
		}
		formSubmit = uifactory.addFormSubmitButton("save", formLayout);
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		boolean changed = false;
		// loop over all Element to store values
		for (int i = 0; i < customFields.size(); i++) {
			final TextElement nameTextElement = customFieldNameElementList.get(i);
			if (!customFields.get(i).getName().equals(nameTextElement.getValue())) {
				customFields.get(i).setName(nameTextElement.getValue());
				changed = true;
			}
			final TextElement valueTextElement = customFieldValueElementList.get(i);
			if (!customFields.get(i).getValue().equals(valueTextElement.getValue())) {
				customFields.get(i).setValue(valueTextElement.getValue());
				changed = true;
			}
			final MultipleSelectionElement tableViewElement = customFieldTableFlagElementList.get(i);
			if (customFields.get(i).isTableViewEnabled() != tableViewElement.isSelected(0)) {
				customFields.get(i).setTableViewEnabled(tableViewElement.isSelected(0));
				changed = true;
			}
		}
		config.setCustomFields(customFields);
		fireEvent(ureq, Event.DONE_EVENT);
		fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
	}

	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {

		if (source instanceof FormLink) {
			final FormLink link = (FormLink) source;
			if (link.getName().equals(ADD_FIELD_LINK)) {
				if (customFields.size() < MAX_NBR_CUSTOM_FIELDS) {
					// Add new custom-field
					final int indexNewCustomField = customFields.size();
					customFields.add(new CustomField("", "", false));
					// first remove existing submit button, add it again at the end
					flc.remove(formSubmit);
					createFormElemente(this.flc, indexNewCustomField, customFields.get(indexNewCustomField));
					formSubmit = uifactory.addFormSubmitButton("save", this.flc);
				} else {
					this.showInfo("info.max.nbr.custom.fields");
				}
			} else {
				final int deleteElementNumber = ((Integer) link.getUserObject()).intValue();
				getLogger().debug("remove customfield #=" + deleteElementNumber);
				final CustomField customField = customFields.remove(deleteElementNumber);
				initFormElements(flc);
			}
		}

		this.flc.setDirty(true);
	}

	private void initFormElements(final FormLayoutContainer flc) {
		removeAllFormElements(flc);
		createFormElements(flc);
	}

	private void createFormElemente(final FormItemContainer formLayout, final int i, final CustomField customField) {
		final TextElement nameElement = uifactory.addTextElement("customfield_name_" + i, "-", 50, customField.getName(), formLayout);
		nameElement.setLabel("customfield.name.label", null);
		if (i == 0) {
			nameElement.setExampleKey("customfield.example.name", null);
		}
		customFieldNameElementList.add(nameElement);

		final TextElement valueElement = uifactory.addTextAreaElement("customfield_value_" + i, "-", 2500, 5, 2, true, customField.getValue(), formLayout);
		valueElement.setLabel("customfield.value.label", null);
		if (i == 0) {
			valueElement.setExampleKey("customfield.example.value", null);
		}
		customFieldValueElementList.add(valueElement);

		final MultipleSelectionElement tableEnabledElement = uifactory.addCheckboxesHorizontal("customfield.table.enabled." + i, null, formLayout, keys, values, null);
		tableEnabledElement.select(keys[0], customField.isTableViewEnabled());
		customFieldTableFlagElementList.add(tableEnabledElement);

		final FormLink deleteLink = uifactory.addFormLink("customfield.delete.link." + i, formLayout, Link.BUTTON_SMALL);
		deleteLink.setUserObject(new Integer(i));
		customFieldLinkElementList.add(deleteLink);

		final SpacerElement spacerElement = uifactory.addSpacerElement("spacer" + i, formLayout, false);
		customFieldSpacerElementList.add(spacerElement);
	}

	private void removeAllFormElements(final FormLayoutContainer flc) {
		// remove all name fields
		for (final Iterator<TextElement> iterator = customFieldNameElementList.iterator(); iterator.hasNext();) {
			flc.remove(iterator.next());
		}
		customFieldNameElementList.clear();
		// remove all value fields
		for (final Iterator<TextElement> iterator = customFieldValueElementList.iterator(); iterator.hasNext();) {
			flc.remove(iterator.next());
		}
		customFieldValueElementList.clear();
		// remove all table view checkboxes
		for (final Iterator<MultipleSelectionElement> iterator = customFieldTableFlagElementList.iterator(); iterator.hasNext();) {
			flc.remove(iterator.next());
		}
		customFieldTableFlagElementList.clear();
		// remove all delete links
		for (final Iterator<FormLink> iterator = customFieldLinkElementList.iterator(); iterator.hasNext();) {
			flc.remove(iterator.next());
		}
		customFieldLinkElementList.clear();
		// remove all spacer elements
		for (final Iterator<SpacerElement> iterator = customFieldSpacerElementList.iterator(); iterator.hasNext();) {
			flc.remove(iterator.next());
		}
		customFieldSpacerElementList.clear();
		flc.remove(formSubmit);
	}

	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {}

	@Override
	protected void doDispose() {
		// nothing
	}

}
