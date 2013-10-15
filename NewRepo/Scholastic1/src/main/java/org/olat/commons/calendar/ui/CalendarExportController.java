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
 * Copyright (c) 1999-2007 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.commons.calendar.ui;

import java.util.Locale;

import org.olat.commons.calendar.CalendarManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;

public class CalendarExportController extends DefaultController {

	private static final String PACKAGE = Util.getPackageName(CalendarManager.class);
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(CalendarManager.class);

	private final Translator translator;
	private final VelocityContainer colorVC;

	public CalendarExportController(final Locale locale, final WindowControl wControl, final String icalFeedLink) {
		super(wControl);
		translator = new PackageTranslator(PACKAGE, locale);

		colorVC = new VelocityContainer("calEdit", VELOCITY_ROOT + "/calIcalFeed.html", translator, this);
		colorVC.contextPut("icalFeedLink", icalFeedLink);

		setInitialComponent(colorVC);
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {}

	@Override
	protected void doDispose() {
		// nothing to dispose
	}

}
