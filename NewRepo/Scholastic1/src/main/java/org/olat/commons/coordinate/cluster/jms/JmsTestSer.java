package org.olat.commons.coordinate.cluster.jms;

import java.util.Date;

import org.olat.basesecurity.SecurityGroup;
import org.olat.core.commons.chiefcontrollers.ChiefControllerMessageEvent;
import org.olat.core.id.Identity;
import org.olat.core.id.Persistable;
import org.olat.core.id.User;
import org.olat.core.util.ObjectCloner;
import org.olat.course.assessment.AssessmentChangedEvent;
import org.olat.group.BusinessGroup;
import org.olat.group.context.BGContext;
import org.olat.group.ui.edit.BusinessGroupModifiedEvent;

public class JmsTestSer {

	/**
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(final String[] args) throws Exception {

		final Identity ident = new Identity() {

			@SuppressWarnings("unused")
			public Date getDeleteEmailDate() {
				return null;
			}

			@Override
			public Date getLastLogin() {
				return null;
			}

			@Override
			public String getName() {
				return null;
			}

			@Override
			public Integer getStatus() {
				return Identity.STATUS_ACTIV;
			}

			@Override
			public User getUser() {
				return null;
			}

			public void setDeleteEmailDate(final Date newDeleteEmail) {
				//
			}

			@Override
			public void setLastLogin(final Date loginDate) {
				//
			}

			@Override
			public void setStatus(final Integer newStatus) {
				//
			}

			@Override
			public Date getCreationDate() {
				return null;
			}

			public Date getLastModified() {
				return null;
			}

			@Override
			public boolean equalsByPersistableKey(final Persistable persistable) {
				return persistable.getKey().equals(getKey());
			}

			@Override
			public Long getKey() {
				return new Long(12345);
			}

			@Override
			public void setName(final String name) {
				// TODO Auto-generated method stub

			}
		};
		/*
		 * Object o = new String("test"); ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("data.out")); //out.writeObject("Data storage");
		 * out.writeObject(o); out.close(); ObjectInputStream in = new ObjectInputStream(new FileInputStream("data.out")); Object o2 = in.readObject();
		 * System.out.println("received:"+o2);
		 */
		System.out.println("start!");
		// Object o = new MultiUserEvent("test1234");
		/*
		 * Object o = new ObjectAccessEvent(1, new OLATResourceable(){ public Long getResourceableId() { return new Long(456); } public String getResourceableTypeName() {
		 * return "firstargtype"; }}); Object o2 = ObjectCloner.deepCopy(o); System.out.println("done! "+o2);
		 */

		/*
		 * --------------------------------------------------------------------------------------------------------------
		 */
		final AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_SCORE_EVAL_CHANGED, ident);

		System.out.println("result:" + ObjectCloner.deepCopy(ace));

		final ChiefControllerMessageEvent ccme = new ChiefControllerMessageEvent();
		ccme.setMsg("yes, it is a message");
		System.out.println("result:" + ObjectCloner.deepCopy(ccme));

		final BusinessGroupModifiedEvent bgme = new BusinessGroupModifiedEvent("com", new BusinessGroup() {

			@Override
			public Boolean getAutoCloseRanksEnabled() {
				return null;
			}

			@Override
			public String getDescription() {
				return null;
			}

			@Override
			public BGContext getGroupContext() {
				return null;
			}

			@Override
			public Date getLastUsage() {
				return null;
			}

			@Override
			public Integer getMaxParticipants() {
				return null;
			}

			@Override
			public Integer getMinParticipants() {
				return null;
			}

			@Override
			public String getName() {
				return null;
			}

			@Override
			public SecurityGroup getOwnerGroup() {
				return null;
			}

			@Override
			public SecurityGroup getPartipiciantGroup() {
				return null;
			}

			@Override
			public String getType() {
				return null;
			}

			@Override
			public SecurityGroup getWaitingGroup() {
				return null;
			}

			@Override
			public Boolean getWaitingListEnabled() {
				return null;
			}

			@Override
			public void setAutoCloseRanksEnabled(final Boolean autoCloseRanksEnabled) {
				//
			}

			@Override
			public void setDescription(final String description) {
				//
			}

			@Override
			public void setGroupContext(final BGContext groupContext) {
				//
			}

			@Override
			public void setLastUsage(final Date lastUsage) {
				//
			}

			@Override
			public void setMaxParticipants(final Integer maxParticipants) {
				//
			}

			@Override
			public void setMinParticipants(final Integer minParticipants) {
				//
			}

			@Override
			public void setName(final String name) {
				//
			}

			@Override
			public void setWaitingGroup(final SecurityGroup waitingGroup) {
				//
			}

			@Override
			public void setWaitingListEnabled(final Boolean waitingListEnabled) {
				//
			}

			@Override
			public boolean equalsByPersistableKey(final Persistable persistable) {
				return false;
			}

			@Override
			public Long getKey() {
				return new Long(678);
			}

			@Override
			public Date getCreationDate() {
				return null;
			}

			@Override
			public Date getLastModified() {
				return null;
			}

			@Override
			public Long getResourceableId() {
				return null;
			}

			@Override
			public String getResourceableTypeName() {
				return null;
			}

			public Date getDeleteEmailDate() {
				return null;
			}

			public void setDeleteEmailDate(final Date deleteEmailDate) {
				//
			}

			@Override
			public void setLastModified(final Date date) {
				//

			}
		}, ident);
		System.out.println("bgme result:" + ObjectCloner.deepCopy(bgme));

	}

}
