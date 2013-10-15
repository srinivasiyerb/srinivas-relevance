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

package org.olat.admin.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.table.BaseTableDataModelWithoutFilter;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.StaticColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableDataModel;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;

/**
 * Description:<BR/>
 * TODO: Class Description for DockingController
 * <P/>
 * Initial Date: Jul 13, 2005
 * 
 * @author Felix Jost
 */
public class AllCachesController extends BasicController {

	OLog log = Tracing.createLoggerFor(this.getClass());

	private final VelocityContainer myContent;
	private final TableController tableCtr;
	private final TableDataModel tdm;
	private CacheManager cm;
	private final String[] cnames;
	private DialogBoxController dc;

	/**
	 * @param ureq
	 * @param wControl
	 */
	public AllCachesController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		// create page
		myContent = createVelocityContainer("index");

		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();

		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("cache.name", 0, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("cache.disk", 1, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("cache.hitcnt", 2, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("cache.mcexp", 3, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("cache.mcnotfound", 4, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("cache.quickcount", 5, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("cache.tti", 6, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("cache.ttl", 7, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("cache.maxElements", 8, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new StaticColumnDescriptor("empty", "cache.empty", translate("action.choose")));
		listenTo(tableCtr);
		myContent.contextPut("title", translate("caches.title"));

		// eh cache
		try {
			cm = CacheManager.getInstance();
		} catch (final CacheException e) {
			throw new AssertException("could not get cache", e);
		}
		cnames = cm.getCacheNames();
		tdm = new AllCachesTableDataModel(cnames);
		tableCtr.setTableDataModel(tdm);
		myContent.put("cachetable", tableCtr.getInitialComponent());

		// returned panel is not needed here, because this controller only shows content with the index.html velocity page
		putInitialPanel(myContent);

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == tableCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				final TableEvent te = (TableEvent) event;
				final String actionid = te.getActionId();
				if (actionid.equals("empty")) {
					final int rowid = te.getRowId();
					final String cname = cnames[rowid];
					final Cache toEmpty = cm.getCache(cname);

					// provide dc as argument, this ensures that dc is disposed before newly created
					dc = activateYesNoDialog(ureq, null, translate("confirm.emptycache"), dc);
					// remember Cache to be emptied if yes is chosen
					dc.setUserObject(toEmpty);
					// activateYesNoDialog means that this controller listens to it, and dialog is shown on screen.
					// nothing further to do here!
					return;
				}
			}
		} else if (source == dc) {
			// the dialogbox is already removed from the gui stack - do not use getWindowControl().pop(); to remove dialogbox
			if (DialogBoxUIFactory.isYesEvent(event)) { // ok
				String cacheName = null;
				try {
					// delete cache
					final Cache c = (Cache) dc.getUserObject();
					cacheName = c.getName();
					c.removeAll();
				} catch (final IllegalStateException e) {
					// ignore
					log.error("Cannot remove Cache:" + cacheName, e);
				}
				// update tablemodel
			}// else no was clicked or dialog box was cancelled (close icon clicked)
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// tableCtr is registerd with listenTo and gets disposed in BasicController
		// dialogbox dc gets disposed by BasicController
	}

}

class AllCachesTableDataModel extends BaseTableDataModelWithoutFilter {
	private final String[] cnames;
	private final CacheManager cacheManager;

	protected AllCachesTableDataModel(final String[] cnames) {
		this.cnames = cnames;
		this.cacheManager = CacheManager.getInstance();
	}

	@Override
	public int getColumnCount() {
		return 9;
	}

	@Override
	public int getRowCount() {
		return cnames.length;
	}

	@Override
	public Object getValueAt(final int row, final int col) {
		final String cname = cnames[row];
		final Cache c = cacheManager.getCache(cname);
		// todo: use Statistics stat = c.getStatistics();
		switch (col) {
			case 0:
				return cname;
			case 1:
				return c.isDiskPersistent() ? Boolean.TRUE : Boolean.FALSE;
			case 2:
				return new Long(c.getHitCount());
			case 3:
				return new Long(c.getMissCountExpired());
			case 4:
				return new Long(c.getMissCountNotFound());
			case 5:
				return new Long(c.getKeysNoDuplicateCheck().size());
			case 6:
				return new Long(c.getTimeToIdleSeconds());
			case 7:
				return new Long(c.getTimeToLiveSeconds());
			case 8:
				return new Long(c.getMaxElementsInMemory());
			default:
				throw new AssertException("nonexisting column:" + col);
		}
	}
}