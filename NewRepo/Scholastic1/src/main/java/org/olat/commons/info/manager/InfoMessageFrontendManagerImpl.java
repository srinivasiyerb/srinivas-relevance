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

package org.olat.commons.info.manager;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.olat.commons.info.model.InfoMessage;
import org.olat.commons.info.notification.InfoSubscriptionManager;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.Emailer;

/**
 * Description:<br>
 * <P>
 * Initial Date: 28 juil. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoMessageFrontendManagerImpl extends InfoMessageFrontendManager {

	private CoordinatorManager coordinatorManager;
	private InfoMessageManager infoMessageManager;
	private InfoSubscriptionManager infoSubscriptionManager;

	/**
	 * [used by Spring]
	 */
	private InfoMessageFrontendManagerImpl() {
		INSTANCE = this;
	}

	/**
	 * [used by Spring]
	 * 
	 * @param coordinatorManager
	 */
	public void setCoordinatorManager(final CoordinatorManager coordinatorManager) {
		this.coordinatorManager = coordinatorManager;
	}

	/**
	 * [used by Spring]
	 * 
	 * @param infoMessageManager
	 */
	public void setInfoMessageManager(final InfoMessageManager infoMessageManager) {
		this.infoMessageManager = infoMessageManager;
	}

	/**
	 * [used by Spring]
	 * 
	 * @param infoSubscriptionManager
	 */
	public void setInfoSubscriptionManager(final InfoSubscriptionManager infoSubscriptionManager) {
		this.infoSubscriptionManager = infoSubscriptionManager;
	}

	@Override
	public InfoMessage loadInfoMessage(final Long key) {
		return infoMessageManager.loadInfoMessageByKey(key);
	}

	@Override
	public InfoMessage createInfoMessage(final OLATResourceable ores, final String subPath, final String businessPath, final Identity author) {
		return infoMessageManager.createInfoMessage(ores, subPath, businessPath, author);
	}

	@Override
	public boolean sendInfoMessage(final InfoMessage infoMessage, final MailFormatter mailFormatter, final Locale locale, final List<Identity> tos) {
		infoMessageManager.saveInfoMessage(infoMessage);

		boolean send = false;
		if (tos != null && !tos.isEmpty()) {
			final Emailer mailer = new Emailer(locale);
			final Set<Long> identityKeySet = new HashSet<Long>();
			final ContactList contactList = new ContactList("Infos");
			for (final Identity to : tos) {
				if (identityKeySet.contains(to.getKey())) {
					continue;
				}
				contactList.add(to);
				identityKeySet.add(to.getKey());
			}
			final List<ContactList> contacts = Collections.singletonList(contactList);
			try {
				String subject = null;
				String body = null;
				if (mailFormatter != null) {
					subject = mailFormatter.getSubject(infoMessage);
					body = mailFormatter.getBody(infoMessage);
				}
				if (!StringHelper.containsNonWhitespace(subject)) {
					subject = infoMessage.getTitle();
				}
				if (!StringHelper.containsNonWhitespace(body)) {
					body = infoMessage.getMessage();
				}
				send = mailer.sendEmail(contacts, subject, body);
			} catch (final AddressException e) {
				logError("Cannot send info messages", e);
			} catch (final MessagingException e) {
				logError("Cannot send info messages", e);
			}
		}

		infoSubscriptionManager.markPublisherNews(infoMessage.getOLATResourceable(), infoMessage.getResSubPath());
		final MultiUserEvent mue = new MultiUserEvent("new_info_message");
		coordinatorManager.getCoordinator().getEventBus().fireEventToListenersOf(mue, oresFrontend);
		return send;
	}

	@Override
	public void deleteInfoMessage(final InfoMessage infoMessage) {
		infoMessageManager.deleteInfoMessage(infoMessage);
		infoSubscriptionManager.markPublisherNews(infoMessage.getOLATResourceable(), infoMessage.getResSubPath());
	}

	@Override
	public List<InfoMessage> loadInfoMessageByResource(final OLATResourceable ores, final String subPath, final String businessPath, final Date after, final Date before,
			final int firstResult, final int maxReturn) {
		return infoMessageManager.loadInfoMessageByResource(ores, subPath, businessPath, after, before, firstResult, maxReturn);
	}

	@Override
	public int countInfoMessageByResource(final OLATResourceable ores, final String subPath, final String businessPath, final Date after, final Date before) {
		return infoMessageManager.countInfoMessageByResource(ores, subPath, businessPath, after, before);
	}

	@Override
	public List<Identity> getInfoSubscribers(final OLATResourceable resource, final String subPath) {
		return infoSubscriptionManager.getInfoSubscribers(resource, subPath);
	}
}
