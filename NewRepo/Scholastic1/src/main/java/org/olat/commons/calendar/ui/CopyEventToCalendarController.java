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

package org.olat.commons.calendar.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.model.Kalendar;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.components.tree.SelectionTree;
import org.olat.core.gui.components.tree.TreeEvent;
import org.olat.core.gui.components.tree.TreeModel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.core.util.xml.XStreamHelper;

public class CopyEventToCalendarController extends DefaultController {

	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(CalendarManager.class);

	private final Translator translator;
	private final VelocityContainer mainVC;
	private final SelectionTree calendarSelectionTree;
	private final KalendarEvent kalendarEvent;

	public CopyEventToCalendarController(final KalendarEvent kalendarEvent, final Collection calendars, final Translator translator, final WindowControl wControl) {
		super(wControl);
		this.translator = translator;
		this.kalendarEvent = kalendarEvent;

		mainVC = new VelocityContainer("calCopy", VELOCITY_ROOT + "/calCopy.html", translator, this);
		calendarSelectionTree = new SelectionTree("calSelection", translator);
		calendarSelectionTree.addListener(this);
		calendarSelectionTree.setMultiselect(true);
		calendarSelectionTree.setFormButtonKey("cal.copy.submit");
		calendarSelectionTree.setShowCancelButton(true);
		calendarSelectionTree.setTreeModel(new CalendarSelectionModel(calendars, kalendarEvent.getCalendar(), translator));
		mainVC.put("tree", calendarSelectionTree);

		setInitialComponent(mainVC);
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == calendarSelectionTree) {
			final TreeEvent te = (TreeEvent) event;
			if (event.getCommand().equals(TreeEvent.COMMAND_TREENODES_SELECTED)) {
				// rebuild kalendar event links
				final List selectedNodesIDS = te.getNodeIds();
				final TreeModel model = calendarSelectionTree.getTreeModel();
				final CalendarManager calendarManager = CalendarManagerFactory.getInstance().getCalendarManager();
				for (final Iterator iter = selectedNodesIDS.iterator(); iter.hasNext();) {
					final String nodeId = (String) iter.next();
					final GenericTreeNode node = (GenericTreeNode) model.getNodeById(nodeId);
					final KalendarRenderWrapper calendarWrapper = (KalendarRenderWrapper) node.getUserObject();
					final Kalendar cal = calendarWrapper.getKalendar();
					final KalendarEvent clonedKalendarEvent = (KalendarEvent) XStreamHelper.xstreamClone(kalendarEvent);
					if (clonedKalendarEvent.getKalendarEventLinks().size() != 0) {
						clonedKalendarEvent.setKalendarEventLinks(new ArrayList());
					}
					calendarManager.addEventTo(cal, clonedKalendarEvent);
					// calendarManager.persistCalendar(cal);
				}
				fireEvent(ureq, Event.DONE_EVENT);
			} else {
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
		}
	}

	@Override
	protected void doDispose() {
		// nothing to do here
	}

}

class CalendarSelectionModel extends GenericTreeModel {

	public CalendarSelectionModel(final Collection calendars, final Kalendar excludeKalendar, final Translator translator) {
		final GenericTreeNode rootNode = new GenericTreeNode(translator.translate("cal.copy.rootnode"), null);
		for (final Iterator iter_calendars = calendars.iterator(); iter_calendars.hasNext();) {
			final KalendarRenderWrapper calendarWrapper = (KalendarRenderWrapper) iter_calendars.next();
			final GenericTreeNode node = new GenericTreeNode(calendarWrapper.getKalendarConfig().getDisplayName(), calendarWrapper);
			node.setIdent(calendarWrapper.getKalendar().getCalendarID());
			if (calendarWrapper.getKalendar().getCalendarID().equals(excludeKalendar.getCalendarID())) {
				// this is the calendar, the event comes from
				node.setSelected(true);
				node.setAccessible(false);
			} else {
				node.setAccessible(calendarWrapper.getAccess() == KalendarRenderWrapper.ACCESS_READ_WRITE);
			}
			rootNode.addChild(node);
		}
		setRootNode(rootNode);
	}

}