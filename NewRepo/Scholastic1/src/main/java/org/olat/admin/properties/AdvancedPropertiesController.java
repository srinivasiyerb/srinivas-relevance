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

package org.olat.admin.properties;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.properties.PropertyManager;

/**
 * Description:<br>
 * is the controller for
 * 
 * @author Felix Jost
 */
public class AdvancedPropertiesController extends BasicController {

	private final Panel myPanel;
	private final AdvancedPropertySearchForm searchForm;
	private final VelocityContainer vcSearchForm;

	private TableController tableCtr;

	/**
	 * caller of this constructor must make sure only olat admins come here
	 * 
	 * @param ureq
	 * @param wControl
	 */
	public AdvancedPropertiesController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		// TODO: make special security check as soon as this controller can also modify polices (at the moment: read only)

		myPanel = new Panel("myPanel");
		myPanel.addListener(this);

		searchForm = new AdvancedPropertySearchForm(ureq, wControl);
		listenTo(searchForm);

		vcSearchForm = createVelocityContainer("searchForm");
		vcSearchForm.put("searchForm", searchForm.getInitialComponent());
		myPanel.setContent(vcSearchForm);
		putInitialPanel(myPanel);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == searchForm && event == Event.DONE_EVENT) {

			final String resourceTypeName = searchForm.getResourceTypeName();
			final String resourceTypeId = searchForm.getResourceTypeId();
			Long resTypeId = null;
			if (resourceTypeId != null && !resourceTypeId.equals("")) {
				resTypeId = Long.valueOf(resourceTypeId);
			}
			String category = searchForm.getCategory();
			if (category != null && category.equals("")) {
				category = null;
			}
			String propertyName = searchForm.getPropertyName();
			if (propertyName != null && propertyName.equals("")) {
				propertyName = null;
			}

			final List entries = PropertyManager.getInstance().listProperties(searchForm.getIdentity(), null, resourceTypeName, resTypeId, category, propertyName);
			final PropertiesTableDataModel ptdm = new PropertiesTableDataModel(entries);

			final TableGuiConfiguration tableConfig = new TableGuiConfiguration();

			removeAsListenerAndDispose(tableCtr);
			tableCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
			// use null as listener argument because we are using listenTo(..) from basiccontroller
			tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.userName", 0, null, ureq.getLocale()));
			tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.resourceTypeName", 1, null, ureq.getLocale()));
			tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.resourceTypeId", 2, null, ureq.getLocale(), ColumnDescriptor.ALIGNMENT_RIGHT));
			tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.category", 3, null, ureq.getLocale()));
			tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.name", 4, null, ureq.getLocale()));
			tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.floatValue", 5, null, ureq.getLocale(), ColumnDescriptor.ALIGNMENT_RIGHT));
			tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.stringValue", 6, null, ureq.getLocale()));
			tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.longValue", 10, null, ureq.getLocale()));
			tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.textValue", 7, null, ureq.getLocale()));
			tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.creationdate", 8, null, ureq.getLocale()));
			tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.lastmodified", 9, null, ureq.getLocale()));
			tableCtr.setTableDataModel(ptdm);
			listenTo(tableCtr);

			myPanel.setContent(tableCtr.getInitialComponent());
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}
}
