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

package org.olat.modules.fo;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.type.Type;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.commons.services.mark.MarkingService;
import org.olat.core.commons.services.text.TextService;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Felix Jost
 */
@Service
public class ForumManager extends BasicManager {

	private static ForumManager INSTANCE;
	@Autowired
	private TextService txtService;

	/**
	 * [spring]
	 */
	private ForumManager() {
		INSTANCE = this;
	}

	/**
	 * @return the singleton
	 */
	public static ForumManager getInstance() {
		return INSTANCE;
	}

	/**
	 * @param msgid msg id of the topthread
	 * @return List messages
	 */
	public List<Message> getThread(final Long msgid) {

		// we make a scalar query so that not only the messages, but also the users
		// are loaded into the cache as well.
		// TODO : Otherwise, hibernate will fetch the user for each message (100
		// messages = 101 SQL Queries!)
		// FIXME: use join fetch instead
		long rstart = 0;
		if (isLogDebugEnabled()) {
			rstart = System.currentTimeMillis();
		}
		final List scalar = DBFactory.getInstance().find(
				"select msg, cr, usercr " + "from org.olat.modules.fo.MessageImpl as msg" + ", org.olat.core.id.Identity as cr " + ", org.olat.user.UserImpl as usercr"
						+ " where msg.creator = cr and cr.user = usercr " + " and (msg.key = ? or msg.threadtop.key = ?) order by msg.creationDate",
				new Object[] { msgid, msgid }, new Type[] { Hibernate.LONG, Hibernate.LONG });
		final int size = scalar.size();
		final List<Message> messages = new ArrayList<Message>(size);
		for (int i = 0; i < size; i++) {
			final Object[] o = (Object[]) scalar.get(i);
			final Message m = (Message) o[0];
			messages.add(m);
		}
		if (isLogDebugEnabled()) {
			final long rstop = System.currentTimeMillis();
			logDebug("time to fetch thread with topmsg_id " + msgid + " :" + (rstop - rstart), null);
		}
		return messages;
	}

	public List<Long> getAllForumKeys() {
		final List<Long> tmpRes = DBFactory.getInstance().find("select key from org.olat.modules.fo.ForumImpl");
		return tmpRes;
	}

	public List<Message> getMessagesByForum(final Forum forum) {
		return getMessagesByForumID(forum.getKey());
	}

	/**
	 * @param forum
	 * @return List messages
	 */
	public List<Message> getMessagesByForumID(final Long forum_id) {
		long rstart = 0;
		if (isLogDebugEnabled()) {
			rstart = System.currentTimeMillis();
		}
		final List scalar = DBFactory.getInstance().find(
				"select msg, cr, usercr " + "from org.olat.modules.fo.MessageImpl as msg" + ", org.olat.core.id.Identity as cr " + ", org.olat.user.UserImpl as usercr"
						+ " where msg.creator = cr and cr.user = usercr and msg.forum.key = ?", forum_id, Hibernate.LONG);
		final int size = scalar.size();
		final List<Message> messages = new ArrayList<Message>(size);
		for (int i = 0; i < size; i++) {
			final Object[] o = (Object[]) scalar.get(i);
			final Message m = (Message) o[0];
			messages.add(m);
		}
		if (isLogDebugEnabled()) {
			final long rstop = System.currentTimeMillis();
			logDebug("time to fetch forum with forum_id " + forum_id + " :" + (rstop - rstart), null);
		}
		return messages;
	}

	/**
	 * @param forumkey
	 * @return the count of all messages by this forum
	 */
	public Integer countMessagesByForumID(final Long forumkey) {
		final List msgCount = DBFactory.getInstance().find("select count(msg.title) from org.olat.modules.fo.MessageImpl as msg where msg.forum.key = ?", forumkey,
				Hibernate.LONG);
		return new Integer(((Long) msgCount.get(0)).intValue());
	}

	/**
	 * Implementation with one entry per message.
	 * 
	 * @param identity
	 * @param forumkey
	 * @return number of read messages
	 */
	public int countReadMessagesByUserAndForum(final Identity identity, final Long forumkey) {
		final List<ReadMessage> itemList = DBFactory.getInstance().find(
				"select msg from msg in class org.olat.modules.fo.ReadMessageImpl where msg.identity = ? and msg.forum = ?",
				new Object[] { identity.getKey(), forumkey }, new Type[] { Hibernate.LONG, Hibernate.LONG });
		return itemList.size();
	}

	/**
	 * @param forumKey
	 * @param latestRead
	 * @return a List of Object[] with a key(Long), title(String), a creator(Identity), and the lastmodified(Date) of the messages of the forum with the given key and
	 *         with last modification after the "latestRead" Date
	 */
	public List<Message> getNewMessageInfo(final Long forumKey, final Date latestRead) {
		// FIXME:fj: lastModified has no index -> test performance with forum with
		// 200 messages
		final String query = "select msg from org.olat.modules.fo.MessageImpl as msg"
				+ " where msg.forum.key = :forumKey and msg.lastModified > :latestRead order by msg.lastModified desc";
		final DBQuery dbquery = DBFactory.getInstance().createQuery(query);
		dbquery.setLong("forumKey", forumKey.longValue());
		dbquery.setTimestamp("latestRead", latestRead);
		dbquery.setCacheable(true);
		return dbquery.list();
	}

	/**
	 * @return the newly created and persisted forum
	 */
	public Forum addAForum() {
		final Forum fo = createForum();
		saveForum(fo);
		return fo;
	}

	/**
	 * @param forumKey
	 * @return the forum with the given key
	 */
	public Forum loadForum(final Long forumKey) {
		final ForumImpl fo = (ForumImpl) DBFactory.getInstance().loadObject(ForumImpl.class, forumKey);
		return fo;
	}

	private Forum saveForum(final Forum forum) {
		final DB db = DBFactory.getInstance();
		db.saveObject(forum);
		return forum;
	}

	/**
	 * @param forumKey
	 */
	public void deleteForum(final Long forumKey) {
		final Forum foToDel = loadForum(forumKey);
		if (foToDel == null) { throw new AssertException("forum to delete was not found: key=" + forumKey); }
		// delete properties, messages and the forum itself
		doDeleteForum(foToDel);
		// delete directory for messages with attachments
		deleteForumContainer(forumKey);
	}

	/**
	 * deletes all messages belonging to this forum and the forum entry itself
	 * 
	 * @param forum
	 */
	private void doDeleteForum(final Forum forum) {
		final Long forumKey = forum.getKey();
		final DB db = DBFactory.getInstance();
		// delete read messsages
		db.delete("from readMsg in class org.olat.modules.fo.ReadMessageImpl where readMsg.forum = ? ", forumKey, Hibernate.LONG);
		// delete messages
		db.delete("from message in class org.olat.modules.fo.MessageImpl where message.forum = ?", forumKey, Hibernate.LONG);
		// delete forum
		db.delete("from forum in class org.olat.modules.fo.ForumImpl where forum.key = ?", forumKey, Hibernate.LONG);
		// delete properties

		// delete all flags
		final MarkingService markingService = (MarkingService) CoreSpringFactory.getBean(MarkingService.class);
		final OLATResourceable ores = OresHelper.createOLATResourceableInstance(Forum.class, forum.getKey());
		markingService.getMarkManager().deleteMark(ores);
	}

	/**
	 * create (in RAM only) a new Forum
	 */
	private ForumImpl createForum() {
		return new ForumImpl();
	}

	/**
	 * sets the parent and threadtop of the message automatically
	 * 
	 * @param newMessage the new message which has title and body set
	 * @param creator
	 * @param replyToMessage
	 */
	public void replyToMessage(final Message newMessage, final Identity creator, final Message replyToMessage) {
		newMessage.setForum(replyToMessage.getForum());
		final Message top = replyToMessage.getThreadtop();
		newMessage.setThreadtop((top != null ? top : replyToMessage));
		newMessage.setParent(replyToMessage);
		newMessage.setCreator(creator);
		saveMessage(newMessage);
	}

	/**
	 * @param creator
	 * @param forum
	 * @param topMessage
	 */
	public void addTopMessage(final Identity creator, final Forum forum, final Message topMessage) {
		topMessage.setForum(forum);
		topMessage.setParent(null);
		topMessage.setThreadtop(null);
		topMessage.setCreator(creator);

		saveMessage(topMessage);
	}

	/**
	 * @param messageKey
	 * @return the message with the given messageKey
	 */
	public Message loadMessage(final Long messageKey) {
		final Message msg = doloadMessage(messageKey);
		return msg;
	}

	private Message doloadMessage(final Long messageKey) {
		final Message msg = (Message) DBFactory.getInstance().loadObject(MessageImpl.class, messageKey);
		return msg;
	}

	private void saveMessage(final Message m) {
		// TODO: think about where maxlenrestriction comes: manager or controller
		updateCounters(m);
		m.setLastModified(new Date());
		DBFactory.getInstance().saveObject(m);
	}

	/**
	 * creates (in RAM only) a new Message<br>
	 * fill the values and use saveMessage to make it persistent
	 * 
	 * @return the message
	 * @see ForumManager#saveMessage(Message)
	 */
	public Message createMessage() {
		return new MessageImpl();
	}

	/**
	 * Update message and fire MultiUserEvent, if any provided. If a not null ForumChangedEvent object is provided, then fire event to listeners.
	 * 
	 * @param m
	 * @param event
	 */
	public void updateMessage(final Message m, final ForumChangedEvent event) {
		updateCounters(m);
		m.setLastModified(new Date());
		DBFactory.getInstance().updateObject(m);
		if (event != null) {
			CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new ForumChangedEvent("hide"), m.getForum());
		}
	}

	/**
	 * @param forumKey
	 * @param m
	 */
	public void deleteMessageTree(final Long forumKey, final Message m) {
		deleteMessageRecursion(forumKey, m);
	}

	private void deleteMessageRecursion(final Long forumKey, Message m) {
		deleteMessageContainer(forumKey, m.getKey());
		final DB db = DBFactory.getInstance();
		final Long message_id = m.getKey();
		final List messages = db.find("select msg from msg in class org.olat.modules.fo.MessageImpl where msg.parent = ?", message_id, Hibernate.LONG);

		for (final Iterator iter = messages.iterator(); iter.hasNext();) {
			final Message element = (Message) iter.next();
			deleteMessageRecursion(forumKey, element);
		}

		/*
		 * if (! db.contains(m)){ log.debug("Message " + m.getKey() + " not in hibernate session, reloading before delete"); m = loadMessage(m.getKey()); }
		 */
		// make sure the message is reloaded if it is not in the hibernate session
		// cache
		m = (Message) db.loadObject(m);
		// delete all properties of one single message
		deleteMessageProperties(forumKey, m);
		db.deleteObject(m);

		// delete all flags
		final MarkingService markingService = (MarkingService) CoreSpringFactory.getBean(MarkingService.class);
		final OLATResourceable ores = OresHelper.createOLATResourceableInstance(Forum.class, forumKey);
		markingService.getMarkManager().deleteMark(ores, m.getKey().toString());

		if (isLogDebugEnabled()) {
			logDebug("Deleting message ", m.getKey().toString());
		}
	}

	/**
	 * @param m
	 * @return true if the message has children
	 */
	public boolean hasChildren(final Message m) {
		boolean children = false;
		final DB db = DBFactory.getInstance();
		final Long message_id = m.getKey();
		final String q = " select count(msg) from org.olat.modules.fo.MessageImpl msg where msg.parent = :input ";

		final DBQuery query = db.createQuery(q);
		query.setLong("input", message_id.longValue());
		final List result = query.list();
		final int count = ((Long) result.get(0)).intValue();

		if (count > 0) {
			children = true;
		}

		return children;
	}

	/**
	 * deletes entry of one message
	 */
	private void deleteMessageProperties(final Long forumKey, final Message m) {
		final DB db = DBFactory.getInstance();
		final Long messageKey = m.getKey();

		final StringBuilder query = new StringBuilder();
		query.append("from readMsg in class org.olat.modules.fo.ReadMessageImpl ");
		query.append("where readMsg.forum = ? ");
		query.append("and readMsg.message = ? ");

		db.delete(query.toString(), new Object[] { forumKey, messageKey }, new Type[] { Hibernate.LONG, Hibernate.LONG });
	}

	/**
	 * @param forumKey
	 * @param messageKey
	 * @return the valid container for the attachments to place into
	 */
	public OlatRootFolderImpl getMessageContainer(final Long forumKey, final Long messageKey) {
		final String fKey = forumKey.toString();
		final String mKey = messageKey.toString();
		final StringBuilder sb = new StringBuilder();
		sb.append("/forum/");
		sb.append(fKey);
		sb.append("/");
		sb.append(mKey);
		final String pathToMsgDir = sb.toString();
		final OlatRootFolderImpl messageContainer = new OlatRootFolderImpl(pathToMsgDir, null);
		final File baseFile = messageContainer.getBasefile();
		baseFile.mkdirs();
		return messageContainer;
	}

	private void moveMessageContainer(final Long fromForumKey, final Long fromMessageKey, final Long toForumKey, final Long toMessageKey) {
		// copy message container
		final OlatRootFolderImpl toMessageContainer = getMessageContainer(toForumKey, toMessageKey);
		final OlatRootFolderImpl fromMessageContainer = getMessageContainer(fromForumKey, fromMessageKey);
		for (final VFSItem vfsItem : fromMessageContainer.getItems()) {
			toMessageContainer.copyFrom(vfsItem);
		}
	}

	private void deleteMessageContainer(final Long forumKey, final Long messageKey) {
		final VFSContainer mContainer = getMessageContainer(forumKey, messageKey);
		mContainer.delete();
	}

	private void deleteForumContainer(final Long forumKey) {
		final VFSContainer fContainer = getForumContainer(forumKey);
		fContainer.delete();
	}

	private OlatRootFolderImpl getForumContainer(final Long forumKey) {
		final String fKey = forumKey.toString();
		final StringBuilder sb = new StringBuilder();
		sb.append("/forum/");
		sb.append(fKey);
		final String pathToForumDir = sb.toString();
		final OlatRootFolderImpl fContainer = new OlatRootFolderImpl(pathToForumDir, null);
		final File baseFile = fContainer.getBasefile();
		baseFile.mkdirs();
		return fContainer;
	}

	public Message findMessage(final Long messageId) {
		return (Message) DBFactory.getInstance().findObject(MessageImpl.class, messageId);
	}

	/**
	 * Splits the current thread starting from the current message. It updates the messages of the selected subthread by setting the Parent and the Threadtop.
	 * 
	 * @param msgid
	 * @return the top message of the newly created thread.
	 */
	public Message splitThread(final Message msg) {
		Message newTopMessage = null;
		if (msg.getThreadtop() == null) {
			newTopMessage = msg;
		} else {
			// it only make sense to split a thread if the current message is not a threadtop message.
			final List<Message> threadList = this.getThread(msg.getThreadtop().getKey());
			final List<Message> subthreadList = new ArrayList<Message>();
			subthreadList.add(msg);
			getSubthread(msg, threadList, subthreadList);

			final Iterator<Message> messageIterator = subthreadList.iterator();
			Message firstMessage = null;
			final DB db = DBFactory.getInstance();
			if (messageIterator.hasNext()) {
				firstMessage = messageIterator.next();
				firstMessage = (Message) db.loadObject(firstMessage);
				firstMessage.setParent(null);
				firstMessage.setThreadtop(null);
				this.updateMessage(firstMessage, new ForumChangedEvent("split"));
				newTopMessage = firstMessage;
			}
			while (firstMessage != null && messageIterator.hasNext()) {
				Message message = messageIterator.next();
				message = (Message) db.loadObject(message);
				message.setThreadtop(firstMessage);
				this.updateMessage(message, null);
			}
		}
		return newTopMessage;
	}

	/**
	 * Moves the current message from the current thread in another thread.
	 * 
	 * @param msg
	 * @param topMsg
	 * @return the moved message
	 */
	@SuppressWarnings("unchecked")
	public Message moveMessage(final Message msg, final Message topMsg) {
		final DB db = DBFactory.getInstance();
		final List<Message> oldThreadList = getThread(msg.getThreadtop().getKey());
		final List<Message> subThreadList = new ArrayList<Message>();
		this.getSubthread(msg, oldThreadList, subThreadList);
		// one has to set a new parent for all childs of the moved message
		// first message of sublist has to get the parent from the moved message
		for (Message childMessage : subThreadList) {
			childMessage = (Message) db.loadObject(childMessage);
			childMessage.setParent(msg.getParent());
			updateMessage(childMessage, null);
		}
		// now move the message to the choosen thread
		final Message oldMessage = (Message) db.loadObject(msg);
		final Message message = createMessage();
		message.setCreator(oldMessage.getCreator());
		message.setForum(oldMessage.getForum());
		message.setModifier(oldMessage.getModifier());
		message.setTitle(oldMessage.getTitle());
		message.setBody(oldMessage.getBody());
		message.setThreadtop(topMsg);
		message.setParent(topMsg);
		final Status status = Status.getStatus(oldMessage.getStatusCode());
		status.setMoved(true);
		message.setStatusCode(Status.getStatusCode(status));
		saveMessage(message);

		// move marks
		final MarkingService markingService = (MarkingService) CoreSpringFactory.getBean(MarkingService.class);
		final OLATResourceable ores = OresHelper.createOLATResourceableInstance(Forum.class, msg.getForum().getKey());
		markingService.getMarkManager().moveMarks(ores, msg.getKey().toString(), message.getKey().toString());

		moveMessageContainer(oldMessage.getForum().getKey(), oldMessage.getKey(), message.getForum().getKey(), message.getKey());
		deleteMessageRecursion(oldMessage.getForum().getKey(), oldMessage);
		return message;
	}

	/**
	 * This is a recursive method. The subthreadList in an ordered list with all descendents of the input msg.
	 * 
	 * @param msg
	 * @param threadList
	 * @param subthreadList
	 */
	private void getSubthread(final Message msg, final List<Message> threadList, final List<Message> subthreadList) {
		final Iterator<Message> listIterator = threadList.iterator();
		while (listIterator.hasNext()) {
			final Message currMessage = listIterator.next();
			if (currMessage.getParent() != null && currMessage.getParent().getKey().equals(msg.getKey())) {
				subthreadList.add(currMessage);
				getSubthread(currMessage, threadList, subthreadList);
			}
		}
	}

	/**
	 * @param identity
	 * @param forum
	 * @return a set with the read messages keys for the input identity and forum.
	 */
	public Set<Long> getReadSet(final Identity identity, final Forum forum) {
		final List<ReadMessage> itemList = DBFactory.getInstance().find(
				"select msg from msg in class org.olat.modules.fo.ReadMessageImpl where msg.identity = ? and msg.forum = ?",
				new Object[] { identity.getKey(), forum.getKey() }, new Type[] { Hibernate.LONG, Hibernate.LONG });

		final Set<Long> readSet = new HashSet<Long>();
		final Iterator<ReadMessage> listIterator = itemList.iterator();
		while (listIterator.hasNext()) {
			final Long msgKey = listIterator.next().getMessage().getKey();
			readSet.add(msgKey);
		}
		return readSet;
	}

	/**
	 * Implementation with one entry per forum message. Adds a new entry into the ReadMessage for the input message and identity.
	 * 
	 * @param msg
	 * @param identity
	 */
	public void markAsRead(final Identity identity, final Message msg) {
		// Check if the message was not already deleted
		final Message retrievedMessage = findMessage(msg.getKey());
		if (retrievedMessage != null) {
			final ReadMessageImpl readMessage = new ReadMessageImpl();
			readMessage.setIdentity(identity);
			readMessage.setMessage(msg);
			readMessage.setForum(msg.getForum());
			DBFactory.getInstance().saveObject(readMessage);
		}
	}

	/**
	 * Update the counters for words and characters
	 * 
	 * @param m the message
	 */
	public void updateCounters(final Message m) {
		final String body = m.getBody();
		final String unQuotedBody = new QuoteAndTagFilter().filter(body);
		final Locale suggestedLocale = txtService.detectLocale(unQuotedBody);
		m.setNumOfWords(txtService.wordCount(unQuotedBody, suggestedLocale));
		m.setNumOfCharacters(txtService.characterCount(unQuotedBody, suggestedLocale));
	}
}
