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
package org.olat.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.ajax.autocompletion.AutoCompleterController;
import org.olat.core.gui.control.generic.ajax.autocompletion.EntriesChosenEvent;
import org.olat.core.gui.control.generic.ajax.autocompletion.ListProvider;
import org.olat.core.gui.control.generic.ajax.autocompletion.ListReceiver;
import org.olat.core.gui.control.generic.layout.GenericMainController;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;

/**
 * Description:<br>
 * GUIDemoMainController shows GuiDemo-Controllers configured in olat_extensions.xml
 * <P>
 * Initial Date: 11.09.2007 <br>
 * 
 * @author Lavinia Dumitrescu
 * @author refactored to use GenericMainController by Roman Haag, frentix GmbH
 */
public class GUIDemoMainController extends GenericMainController {

	public GUIDemoMainController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		final GenericTreeNode gtn = new GenericTreeNode();
		gtn.setTitle("Autocompletion");
		gtn.setUserObject("guidemo-autocompletion");
		gtn.setAltText("Input field autocompletion");
		addChildNodeToAppend(gtn);

		init(ureq);
	}

	@Override
	protected Controller handleOwnMenuTreeEvent(final Object uobject, final UserRequest ureq) {
		if (uobject.equals("guidemo-autocompletion")) {
			// for a demo of autocompletion, do a user search
			final ListProvider provider = new ListProvider() {
				@Override
				public void getResult(final String searchValue, final ListReceiver receiver) {
					final Map<String, String> userProperties = new HashMap<String, String>();
					// We can only search in mandatory User-Properties due to problems
					// with hibernate query with join and not existing rows
					userProperties.put(UserConstants.FIRSTNAME, searchValue);
					userProperties.put(UserConstants.LASTNAME, searchValue);
					userProperties.put(UserConstants.EMAIL, searchValue);
					final List<Identity> res = BaseSecurityManager.getInstance().getVisibleIdentitiesByPowerSearch(searchValue, userProperties, false, null, null, null,
							null, null);
					int maxEntries = 15;
					boolean hasMore = false;
					for (final Iterator<Identity> it_res = res.iterator(); (hasMore = it_res.hasNext()) && maxEntries > 0;) {
						maxEntries--;
						final Identity ident = it_res.next();
						final User u = ident.getUser();
						final String key = ident.getKey().toString();
						final String displayKey = ident.getName();
						final String first = u.getProperty(UserConstants.FIRSTNAME, getLocale());
						final String last = u.getProperty(UserConstants.LASTNAME, getLocale());
						final String displayText = last + " " + first;
						receiver.addEntry(key, displayKey, displayText, CSSHelper.CSS_CLASS_USER);
					}
					if (hasMore) {
						receiver.addEntry("-1", ".....");
					}
				}
			};

			final Controller c = new AutoCompleterController(ureq, getWindowControl(), provider, null, true, 60, 3, null);

			// for demo only, normally use in parent controller
			c.addControllerListener(new ControllerEventListener() {
				@Override
				public void dispatchEvent(final UserRequest ureq, final Controller source, final Event event) {
					final EntriesChosenEvent ece = (EntriesChosenEvent) event;
					final String sel = ece.getEntries().toString();
					getWindowControl().setInfo("selected entrie(s):" + sel);

				}
			});
			return c;
		}
		return null;
	}

}
