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

package org.olat.commons.info.notification;

import java.util.List;

import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.notifications.PublisherData;
import org.olat.core.util.notifications.Subscriber;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.prefs.Preferences;

/**
 * Description:<br>
 * Abstract class of the subscriptions manager.
 * <P>
 * Initial Date: 27 jul. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public abstract class InfoSubscriptionManager {

	protected static InfoSubscriptionManager INSTANCE;

	public static InfoSubscriptionManager getInstance() {
		return INSTANCE;
	}

	public abstract SubscriptionContext getInfoSubscriptionContext(OLATResourceable resource, String subPath);

	public abstract PublisherData getInfoPublisherData(OLATResourceable resource, String businessPath);

	public abstract InfoSubscription getInfoSubscription(Preferences prefs);

	public abstract Subscriber getInfoSubscriber(Identity identity, OLATResourceable resource, String subPath);

	public abstract List<Identity> getInfoSubscribers(OLATResourceable resource, String subPath);

	public abstract void subscribe(OLATResourceable resource, String resSubPath, String businessPath, Identity identity);

	public abstract void unsubscribe(OLATResourceable resource, String subPath, Identity identity);

	public abstract void markPublisherNews(OLATResourceable resource, String subPath);
}
