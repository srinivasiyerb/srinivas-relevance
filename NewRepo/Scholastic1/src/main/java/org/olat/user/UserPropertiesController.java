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

package org.olat.user;

import java.util.Date;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.core.gui.components.table.StaticColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.Identity;
import org.olat.group.BusinessGroup;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;

/**
 * Initial Date: Jul 29, 2003
 * 
 * @author Florian Gnaegi
 */
public class UserPropertiesController extends BasicController {

	private Property foundProp;
	private final PropTableDataModel tdm;
	private final TableController tableCtr;

	/**
	 * Administer properties of a user.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param identity
	 */
	public UserPropertiesController(final UserRequest ureq, final WindowControl wControl, final Identity identity) {
		super(ureq, wControl);
		final PropertyManager pm = PropertyManager.getInstance();
		final List l = pm.listProperties(identity, null, null, null, null);
		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(getTranslator().translate("error.no.props.found"));
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		listenTo(tableCtr);
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.prop.category", 0, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.prop.grp", 1, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.prop.resource", 2, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.prop.name", 3, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.prop.value", 4, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.prop.creatdat", 5, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.prop.moddat", 6, null, ureq.getLocale()));
		// property selection / id only for admins
		if (ureq.getUserSession().getRoles().isOLATAdmin()) {
			tableCtr.addColumnDescriptor(new StaticColumnDescriptor("choose", "table.header.action", translate("action.choose")));
		}
		tdm = new PropTableDataModel(l);
		tableCtr.setTableDataModel(tdm);
		putInitialPanel(tableCtr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// no events to catch
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == tableCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				final TableEvent te = (TableEvent) event;
				final String actionid = te.getActionId();
				if (actionid.equals("choose")) {
					final int rowid = te.getRowId();
					foundProp = (Property) tdm.getObject(rowid);
					// Tell parentController that a subject has been found
					fireEvent(ureq, new PropFoundEvent(foundProp));
				}
			}
		}
	}

	/**
	 * Get the property that was found by this workflow
	 * 
	 * @return Property The found property
	 */
	public Property getFoundProp() {
		return foundProp;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}
}

/**
 * Comment: The prop table data model.
 */

class PropTableDataModel extends DefaultTableDataModel {

	/**
	 * Table model holding list of properties.
	 * 
	 * @param objects
	 */
	public PropTableDataModel(final List objects) {
		super(objects);
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getValueAt(int, int)
	 */
	@Override
	public final Object getValueAt(final int row, final int col) {
		final Property p = (Property) getObject(row);
		switch (col) {
			case 0:
				final String cat = p.getCategory();
				return (cat == null ? "n/a" : cat);
			case 1:
				final BusinessGroup grp = p.getGrp();
				return (grp == null ? "n/a" : grp.getKey().toString());
			case 2:
				final String resType = p.getResourceTypeName();
				return (resType == null ? "n/a" : resType);
			case 3:
				final String name = p.getName();
				return (name == null ? "n/a" : name);
			case 4:
				final Float floatvalue = p.getFloatValue();
				final Long longvalue = p.getLongValue();
				final String stringvalue = p.getStringValue();
				final String textvalue = p.getTextValue();
				String val;
				if (floatvalue != null) {
					val = floatvalue.toString();
				} else if (longvalue != null) {
					val = longvalue.toString();
				} else if (stringvalue != null) {
					val = stringvalue;
				} else if (textvalue != null) {
					val = textvalue;
				} else {
					val = "n/a";
				}
				return val;
			case 5:
				final Date dateCD = p.getCreationDate();
				return (dateCD == null ? new Date() : dateCD);
			case 6:
				final Date dateLM = p.getLastModified();
				return (dateLM == null ? new Date() : dateLM);
			default:
				return "error";
		}
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return 7;
	}
}