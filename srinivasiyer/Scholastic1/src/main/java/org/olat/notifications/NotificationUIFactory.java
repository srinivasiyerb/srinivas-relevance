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
package org.olat.notifications;

import java.util.Date;

import org.olat.ControllerFactory;
import org.olat.NewControllerFactory;
import org.olat.commons.calendar.ui.CalendarController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.util.Util;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.Publisher;
import org.olat.core.util.notifications.Subscriber;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseModule;
import org.olat.group.BusinessGroupManagerImpl;

/**
 * Description:<br>
 * TODO: gnaegi Class Description for NotificationUIFactory
 * <P>
 * Initial Date: 22.12.2009 <br>
 * 
 * @author gnaegi
 */
public class NotificationUIFactory {

	/**
	 * Create a controller that displays the subscriptions for a specific user
	 * 
	 * @param subscriberIdentity
	 * @param ureq
	 * @param windowControl
	 * @return
	 */
	public static NotificationSubscriptionController createSubscriptionListingController(final Identity subscriberIdentity, final UserRequest ureq,
			final WindowControl windowControl) {
		return new NotificationSubscriptionController(subscriberIdentity, ureq, windowControl);
	}

	/**
	 * Create a controller that displays the users news from his subscriptions
	 * 
	 * @param subscriberIdentity
	 * @param ureq
	 * @param windowControl
	 * @param newsSinceDate optional date that represents the lower boundary of the news period or NULL to use the default value
	 * @return
	 */
	public static NotificationNewsController createNewsListingController(final Identity subscriberIdentity, final UserRequest ureq, final WindowControl windowControl,
			final Date newsSinceDate) {
		return new NotificationNewsController(subscriberIdentity, ureq, windowControl, newsSinceDate);
	}

	/**
	 * Create a controller which shows the users subscriptions and the generated news since the given date in one view
	 * 
	 * @param subscriberIdentity
	 * @param ureq
	 * @param windowControl
	 * @param newsSinceDate optional date that represents the lower boundary of the news period or NULL to use the default value
	 * @return
	 */
	public static NotificationSubscriptionAndNewsController createCombinedSubscriptionsAndNewsController(final Identity subscriberIdentity, final UserRequest ureq,
			final WindowControl windowControl, final Date newsSinceDate) {
		return new NotificationSubscriptionAndNewsController(subscriberIdentity, ureq, windowControl, newsSinceDate);
	}

	/**
	 * Create a controller which shows the users subscriptions and the generated news in one view that has been accumulated during the configured notification period
	 * 
	 * @param subscriberIdentity
	 * @param ureq
	 * @param windowControl
	 * @return
	 */
	public static NotificationSubscriptionAndNewsController createCombinedSubscriptionsAndNewsController(final Identity subscriberIdentity, final UserRequest ureq,
			final WindowControl windowControl) {
		final NotificationsManager notiMgr = NotificationsManager.getInstance();
		// default use the interval
		final Date compareDate = notiMgr.getCompareDateFromInterval(notiMgr.getUserIntervalOrDefault(ureq.getIdentity()));
		return new NotificationSubscriptionAndNewsController(subscriberIdentity, ureq, windowControl, compareDate);
	}

	/**
	 * launch a subscription resource in its tab
	 * 
	 * @param ureq
	 * @param windowControl
	 * @param sub
	 */
	public static void launchSubscriptionResource(final UserRequest ureq, final WindowControl windowControl, final Subscriber sub) {
		final Publisher pub = sub.getPublisher();
		if (!NotificationsManager.getInstance().isPublisherValid(pub)) {
			final Translator trans = Util.createPackageTranslator(NotificationUIFactory.class, ureq.getLocale());
			windowControl.setError(trans.translate("error.publisherdeleted"));
			return;
		}
		String resName = pub.getResName();
		final Long resId = pub.getResId();
		final String subidentifier = pub.getSubidentifier();
		// Special case update course and group name for calendars (why?)
		if (subidentifier.equals(CalendarController.ACTION_CALENDAR_COURSE)) {
			resName = CourseModule.ORES_TYPE_COURSE;
		}
		if (subidentifier.equals(CalendarController.ACTION_CALENDAR_GROUP)) {
			resName = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(pub.getResId(), true).getResourceableTypeName();
		}
		final OLATResourceable ores = OresHelper.createOLATResourceableInstance(resName, resId);
		final String title = NotificationsManager.getInstance().getNotificationsHandler(sub.getPublisher()).createTitleInfo(sub, ureq.getLocale());
		// Launch in dtab
		final DTabs dts = (DTabs) Windows.getWindows(ureq).getWindow(ureq).getAttribute("DTabs");
		DTab dt = dts.getDTab(ores);
		if (dt == null) {
			// Does not yet exist -> create and add
			dt = dts.createDTab(ores, title);
			if (dt == null) {
				// huh, no tabs available? don't know what to do here
				return;
			}
			final Controller launchController = ControllerFactory.createLaunchController(ores, subidentifier, ureq, dt.getWindowControl(), false);
			// Try with the new factory controller too
			boolean newFactory = false;
			if (launchController == null) {
				try {
					final String resourceUrl = "[" + resName + ":0][notifications]";
					final BusinessControl bc = BusinessControlFactory.getInstance().createFromString(resourceUrl);
					final WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(bc, windowControl);
					NewControllerFactory.getInstance().launch(ureq, bwControl);
					newFactory = true;
				} catch (final Exception ex) {
					// fail silently
				}
			}
			if (newFactory) {
				// hourra
			} else if (launchController == null) { // not possible to launch anymore
				final Translator trans = Util.createPackageTranslator(NotificationUIFactory.class, ureq.getLocale());
				windowControl.setWarning(trans.translate("warn.nolaunch"));
			} else {
				dt.setController(launchController);
				dts.addDTab(dt);
				dts.activate(ureq, dt, null); // null: do not reactivate to a
				// certain view here, this
				// happened in
				// ControllerFactory.createLaunchController
			}
		} else {
			dts.activate(ureq, dt, subidentifier);
		}
	}

}
