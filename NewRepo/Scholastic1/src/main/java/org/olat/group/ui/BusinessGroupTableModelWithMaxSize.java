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

package org.olat.group.ui;

import java.util.Iterator;
import java.util.List;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.core.gui.components.table.TableDataModel;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Formatter;
import org.olat.core.util.filter.FilterFactory;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManager;
import org.olat.group.BusinessGroupManagerImpl;

/**
 * Description:<BR>
 * Extended business gropu table model with max size and current number of participants as additional rows
 * <P>
 * Initial Date: Sep 9, 2004
 * 
 * @author gnaegi
 */
public class BusinessGroupTableModelWithMaxSize extends DefaultTableDataModel implements TableDataModel {
	private static final int COLUMN_COUNT = 7;
	private final List members;
	private final Translator trans;
	private final Identity identity;
	private final boolean cancelEnrollEnabled;
	private final BaseSecurity securityManager;
	private final BusinessGroupManager businessGroupManager;

	/**
	 * @param groups List of business groups
	 * @param members List containing the number of participants for each group. The index of the list corresponds with the index of the group list
	 * @param trans
	 */
	public BusinessGroupTableModelWithMaxSize(final List groups, final List members, final Translator trans, final Identity identity, final boolean cancelEnrollEnabled) {
		super(groups);
		this.members = members;
		this.trans = trans;
		this.identity = identity;
		securityManager = BaseSecurityManager.getInstance();
		businessGroupManager = BusinessGroupManagerImpl.getInstance();
		this.cancelEnrollEnabled = cancelEnrollEnabled;
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return COLUMN_COUNT;
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(final int row, final int col) {
		final BusinessGroup businessGroup = (BusinessGroup) objects.get(row);
		final Integer numbParts = (Integer) members.get(row);
		final Integer max = businessGroup.getMaxParticipants();
		switch (col) {
			case 0:
				return businessGroup.getName();
			case 1:
				String description = businessGroup.getDescription();
				description = FilterFactory.getHtmlTagsFilter().filter(description);
				description = Formatter.truncate(description, 256);
				return description;
			case 2:
				// Belegt/Plätze
				if (max == null) {
					// no limit => return only members
					return numbParts;
				}
				// return format 2/10
				final StringBuilder buf = new StringBuilder();
				buf.append(numbParts);
				buf.append(trans.translate("grouplist.table.partipiciant.delimiter"));
				buf.append(businessGroup.getMaxParticipants());
				if (numbParts > businessGroup.getMaxParticipants()) {
					Tracing.logInfo(
							"Group overflow detected for the group: " + businessGroup + ", participants: " + numbParts + " maxParticipamts: "
									+ businessGroup.getMaxParticipants(), BusinessGroupTableModelWithMaxSize.class);
				}
				return buf.toString();
			case 3:
				// Waiting-list
				if (businessGroup.getWaitingListEnabled().booleanValue()) {
					// Waitinglist is enabled => show current size
					final int intValue = securityManager.countIdentitiesOfSecurityGroup(businessGroup.getWaitingGroup());
					return new Integer(intValue);
				}
				return trans.translate("grouplist.table.noWaitingList");
			case 4:
				// Status
				if (securityManager.isIdentityInSecurityGroup(this.identity, businessGroup.getPartipiciantGroup())) {
					return trans.translate("grouplist.table.state.onPartipiciantList");
				} else if (securityManager.isIdentityInSecurityGroup(this.identity, businessGroup.getWaitingGroup())) {
					final int pos = businessGroupManager.getPositionInWaitingListFor(identity, businessGroup);
					final String[] onWaitingListArgs = new String[] { Integer.toString(pos) };
					return trans.translate("grouplist.table.state.onWaitingList", onWaitingListArgs);
				} else if (max != null && !businessGroup.getWaitingListEnabled().booleanValue() && (numbParts.intValue() >= max.intValue())) {
					return trans.translate("grouplist.table.state.enroll.full");
				} else if (max != null && businessGroup.getWaitingListEnabled().booleanValue() && (numbParts.intValue() >= max.intValue())) { return trans
						.translate("grouplist.table.state.WaitingList"); }
				return trans.translate("grouplist.table.state.notEnrolled");
			case 5:
				// Action enroll
				if (isEnrolledInAnyGroup(identity)) {
					// Allready enrolled => does not show action-link 'enroll'
					return Boolean.FALSE;
				}
				if (max != null && !businessGroup.getWaitingListEnabled().booleanValue() && (numbParts.intValue() >= max.intValue())) {
					// group is full => => does not show action-link 'enroll'
					return Boolean.FALSE;
				}
				return Boolean.TRUE;
			case 6:
				// Action cancel enrollment
				if (isEnrolledIn(businessGroup, identity)) {
					// check if user is on waiting-list
					if (securityManager.isIdentityInSecurityGroup(this.identity, businessGroup.getWaitingGroup())) {
						// user is on waitinglist => show allways action cancelEnrollment for waitinglist
						return Boolean.TRUE;
					}
					// user is not on waitinglist => show action cancelEnrollment only if enabled
					if (cancelEnrollEnabled) { return Boolean.TRUE; }
				}
				return Boolean.FALSE;
			default:
				return "ERROR";
		}
	}

	/**
	 * @param owned
	 */
	public void setEntries(final List owned) {
		this.objects = owned;
	}

	/**
	 * @param row
	 * @return the business group at the given row
	 */
	public BusinessGroup getBusinessGroupAt(final int row) {
		return (BusinessGroup) objects.get(row);
	}

	/**
	 * Check if an identity is in certain security-group.
	 * 
	 * @param businessGroup
	 * @param identity
	 * @return true: Found identity in PartipiciantGroup or WaitingGroup.
	 */
	private boolean isEnrolledIn(final BusinessGroup businessGroup, final Identity identity) {
		if (securityManager.isIdentityInSecurityGroup(identity, businessGroup.getPartipiciantGroup())
				|| securityManager.isIdentityInSecurityGroup(identity, businessGroup.getWaitingGroup())) { return true; }
		return false;
	}

	/**
	 * Check if an identity is in any security-group.
	 * 
	 * @param identity
	 * @return true: Found identity in any security-group of this table model.
	 */
	private boolean isEnrolledInAnyGroup(final Identity identity) {
		BusinessGroup businessGroup;
		// loop over all business-groups
		for (final Iterator iter = objects.iterator(); iter.hasNext();) {
			businessGroup = (BusinessGroup) iter.next();
			if (isEnrolledIn(businessGroup, identity)) { return true; }
		}
		return false;
	}

}