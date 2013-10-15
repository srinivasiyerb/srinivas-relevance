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
 * Copyright (c) 2008 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.user.notification;

import java.util.Date;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.id.Identity;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.notifications.PublisherData;
import org.olat.core.util.notifications.Subscriber;
import org.olat.core.util.notifications.SubscriptionContext;

/**
 * Description:<br>
 * This implementation help to manager the subscription for notification of "new identity created".
 * <P>
 * Initial Date: 18 august 2009 <br>
 * 
 * @author srosse
 */
public abstract class UsersSubscriptionManager extends BasicManager {

	public static final UsersSubscriptionManager getInstance() {
		return (UsersSubscriptionManager) CoreSpringFactory.getBean(UsersSubscriptionManager.class.getCanonicalName());
	}

	public abstract SubscriptionContext getNewUsersSubscriptionContext();

	public abstract PublisherData getNewUsersPublisherData();

	public abstract Subscriber getNewUsersSubscriber(Identity identity);

	public abstract void subscribe(Identity identity);

	public abstract void unsubscribe(Identity identity);

	public abstract void markPublisherNews();

	public abstract List<Identity> getNewIdentityCreated(Date From);
}
