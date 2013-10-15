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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.util.mail;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.olat.core.id.Identity;

/**
 * Description:<br>
 * TODO: patrick Class Description for ContactMessage
 * <P>
 * Initial Date: Jan 22, 2006 <br>
 * 
 * @author patrick
 */
public class ContactMessage {

	private Hashtable contactLists = new Hashtable();
	private List<Identity> disabledIdentities;
	private String bodyText;
	private String subject;
	private Identity from;

	/**
	 * @param from
	 */
	public ContactMessage(Identity from) {
		this.from = from;
		disabledIdentities = new ArrayList<Identity>();
	}

	public Identity getFrom() {
		return this.from;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getSubject() {
		return subject;
	}

	public void setBodyText(String bodyText) {
		this.bodyText = bodyText;
	}

	public String getBodyText() {
		return bodyText;
	}

	/**
	 * add a ContactList as EmailTo:
	 * 
	 * @param emailList
	 */
	public void addEmailTo(ContactList emailList) {
		emailList = cleanEMailList(emailList);
		if (emailList != null) {
			if (contactLists.containsKey(emailList.getName())) {
				// there is already a ContactList with this name...
				ContactList existing = (ContactList) contactLists.get(emailList.getName());
				// , merge their values.
				existing.add(emailList);
			} else {
				// a new ContactList, put it into contactLists
				contactLists.put(emailList.getName(), emailList);
			}
		}
	}

	/**
	 * @return Returns the disabledIdentities.
	 */
	public List<Identity> getDisabledIdentities() {
		return disabledIdentities;
	}

	private ContactList cleanEMailList(ContactList emailList) {
		Hashtable identityEmails = emailList.getIdentiEmails();
		Enumeration enumeration = identityEmails.elements();
		String value = "";
		while (enumeration.hasMoreElements()) {
			Identity identity = (Identity) enumeration.nextElement();
			List<Identity> singleIdentityList = new ArrayList<Identity>();
			singleIdentityList.add(identity);
			MailerResult result = new MailerResult();
			if (MailHelper.removeDisabledMailAddress(singleIdentityList, result).getFailedIdentites().size() > 0) {
				emailList.remove(identity);
				if (!disabledIdentities.contains(identity)) {
					disabledIdentities.add(identity);
				}
			}
		}
		if (emailList.getIdentiEmails().size() == 0 && emailList.getStringEmails().size() == 0) {
			emailList = null;
		}
		return emailList;
	}

	/**
	 * a List with ContactLists as elements is returned
	 * 
	 * @return
	 */
	public List getEmailToContactLists() {
		ArrayList retVal = new ArrayList();
		Enumeration enumeration = this.contactLists.elements();
		while (enumeration.hasMoreElements()) {
			retVal.add(enumeration.nextElement());
		}
		return retVal;
	}

}
