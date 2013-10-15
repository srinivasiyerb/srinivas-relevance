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

package org.olat.basesecurity;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.hibernate.type.Type;
import org.olat.admin.quota.GenericQuotaEditController;
import org.olat.admin.quota.QuotaController;
import org.olat.admin.sysinfo.SysinfoController;
import org.olat.admin.user.UserAdminController;
import org.olat.admin.user.UserChangePasswordController;
import org.olat.admin.user.UserCreateController;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.basesecurity.events.NewIdentityCreatedEvent;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.resource.OresHelper;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.user.ChangePasswordController;
import org.olat.user.PersonalSettingsController;
import org.olat.user.UserManager;

import com.ibm.icu.util.Calendar;

/**
 * <h3>Description:</h3> The PersistingManager implements the security manager and provide methods to manage identities and user objects based on a database persistence
 * mechanism using hibernate.
 * <p>
 * 
 * @author Felix Jost, Florian Gnaegi
 */
public class BaseSecurityManager extends BasicManager implements BaseSecurity {
	private OLATResourceManager orm;
	private String dbVendor = "";
	private static BaseSecurityManager INSTANCE;
	private static String GUEST_USERNAME_PREFIX = "guest_";
	public static final OLATResourceable IDENTITY_EVENT_CHANNEL = OresHelper.lookupType(Identity.class);

	/**
	 * [used by spring]
	 */
	private BaseSecurityManager() {
		INSTANCE = this;
	}

	/**
	 * @return the manager
	 */
	public static BaseSecurity getInstance() {
		return INSTANCE;
	}

	/**
	 * @see org.olat.basesecurity.Manager#init()
	 */
	public void init() { // called only once at startup and only from one thread
		// init the system level groups and its policies
		initSysGroupAdmin();
		DBFactory.getInstance(false).intermediateCommit();
		initSysGroupAuthors();
		DBFactory.getInstance(false).intermediateCommit();
		initSysGroupGroupmanagers();
		DBFactory.getInstance(false).intermediateCommit();
		initSysGroupUsermanagers();
		DBFactory.getInstance(false).intermediateCommit();
		initSysGroupUsers();
		DBFactory.getInstance(false).intermediateCommit();
		initSysGroupAnonymous();
		DBFactory.getInstance(false).intermediateCommit();
		initSysGroupInstitutionalResourceManager();
		DBFactory.getInstance(false).intermediateCommit();
	}

	/**
	 * OLAT system administrators, root, good, whatever you name it...
	 */
	private void initSysGroupAdmin() {
		SecurityGroup adminGroup = findSecurityGroupByName(Constants.GROUP_ADMIN);
		if (adminGroup == null) {
			adminGroup = createAndPersistNamedSecurityGroup(Constants.GROUP_ADMIN);
		}

		// we check everthing by policies, so we must give admins the hasRole
		// permission on the type resource "Admin"
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_ADMIN);

		// admins have role "authors" by default
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);

		// admins have a groupmanager policy and access permissions to groupmanaging tools
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GROUPMANAGER);

		// admins have a usemanager policy and access permissions to usermanagement tools
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_USERMANAGER);

		// admins are also regular users
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_USERS);

		// olat admins have access to all security groups
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_ACCESS, Constants.ORESOURCE_SECURITYGROUPS);

		// and to all courses
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_ADMIN, Constants.ORESOURCE_COURSES);

		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(SysinfoController.class));
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(UserAdminController.class));
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(UserChangePasswordController.class));
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(UserCreateController.class));
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(QuotaController.class));
		createAndPersistPolicyIfNotExists(adminGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(GenericQuotaEditController.class));
	}

	/**
	 * Every active user that is an active user is in the user group. exceptions: logonDenied and anonymous users
	 */
	private void initSysGroupUsers() {
		SecurityGroup olatuserGroup = findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		if (olatuserGroup == null) {
			olatuserGroup = createAndPersistNamedSecurityGroup(Constants.GROUP_OLATUSERS);
		}

		// users have a user policy
		createAndPersistPolicyIfNotExists(olatuserGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_USERS);

		createAndPersistPolicyIfNotExists(olatuserGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(ChangePasswordController.class));
		createAndPersistPolicyIfNotExists(olatuserGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(PersonalSettingsController.class));
	}

	/**
	 * Users with access to group context management (groupmanagement that can be used in multiple courses
	 */
	private void initSysGroupGroupmanagers() {
		SecurityGroup olatGroupmanagerGroup = findSecurityGroupByName(Constants.GROUP_GROUPMANAGERS);
		if (olatGroupmanagerGroup == null) {
			olatGroupmanagerGroup = createAndPersistNamedSecurityGroup(Constants.GROUP_GROUPMANAGERS);
		}
		// gropumanagers have a groupmanager policy and access permissions to groupmanaging tools
		createAndPersistPolicyIfNotExists(olatGroupmanagerGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GROUPMANAGER);
	}

	/**
	 * Users with access to user management
	 */
	private void initSysGroupUsermanagers() {
		SecurityGroup olatUsermanagerGroup = findSecurityGroupByName(Constants.GROUP_USERMANAGERS);
		if (olatUsermanagerGroup == null) {
			olatUsermanagerGroup = createAndPersistNamedSecurityGroup(Constants.GROUP_USERMANAGERS);
		}
		// gropumanagers have a groupmanager policy and access permissions to groupmanaging tools
		createAndPersistPolicyIfNotExists(olatUsermanagerGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_USERMANAGER);
		createAndPersistPolicyIfNotExists(olatUsermanagerGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(UserAdminController.class));
		createAndPersistPolicyIfNotExists(olatUsermanagerGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(UserChangePasswordController.class));
		createAndPersistPolicyIfNotExists(olatUsermanagerGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(UserCreateController.class));
		createAndPersistPolicyIfNotExists(olatUsermanagerGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(GenericQuotaEditController.class));
	}

	/**
	 * Users with access to the authoring parts of the learning ressources repository
	 */
	private void initSysGroupAuthors() {
		SecurityGroup olatauthorGroup = findSecurityGroupByName(Constants.GROUP_AUTHORS);
		if (olatauthorGroup == null) {
			olatauthorGroup = createAndPersistNamedSecurityGroup(Constants.GROUP_AUTHORS);
		}
		// authors have a author policy and access permissions to authoring tools
		createAndPersistPolicyIfNotExists(olatauthorGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);
	}

	/**
	 * Users with access to the authoring parts of the learning ressources repository (all resources in his university)
	 */
	private void initSysGroupInstitutionalResourceManager() {
		SecurityGroup institutionalResourceManagerGroup = findSecurityGroupByName(Constants.GROUP_INST_ORES_MANAGER);
		if (institutionalResourceManagerGroup == null) {
			institutionalResourceManagerGroup = createAndPersistNamedSecurityGroup(Constants.GROUP_INST_ORES_MANAGER);
		}
		// manager have a author policy and access permissions to authoring tools
		createAndPersistPolicyIfNotExists(institutionalResourceManagerGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_INSTORESMANAGER);
		createAndPersistPolicyIfNotExists(institutionalResourceManagerGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(QuotaController.class));
		createAndPersistPolicyIfNotExists(institutionalResourceManagerGroup, Constants.PERMISSION_ACCESS, OresHelper.lookupType(GenericQuotaEditController.class));
	}

	/**
	 * Unknown users with guest only rights
	 */
	private void initSysGroupAnonymous() {
		SecurityGroup guestGroup = findSecurityGroupByName(Constants.GROUP_ANONYMOUS);
		if (guestGroup == null) {
			guestGroup = createAndPersistNamedSecurityGroup(Constants.GROUP_ANONYMOUS);
		}
		// guest(=anonymous) have a guest policy
		createAndPersistPolicyIfNotExists(guestGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GUESTONLY);
	}

	/**
	 * @see org.olat.basesecurity.Manager#getGroupsWithPermissionOnOlatResourceable(java.lang.String, org.olat.core.id.OLATResourceable
	 */
	public List getGroupsWithPermissionOnOlatResourceable(final String permission, final OLATResourceable olatResourceable) {
		Long oresid = olatResourceable.getResourceableId();
		if (oresid == null) {
			oresid = new Long(0); // TODO: make a method in
		}
		// OLATResorceManager, since this
		// is implementation detail
		final String oresName = olatResourceable.getResourceableTypeName();

		final List res = DBFactory.getInstance().find(
				"select sgi from" + " org.olat.basesecurity.SecurityGroupImpl as sgi," + " org.olat.basesecurity.PolicyImpl as poi,"
						+ " org.olat.resource.OLATResourceImpl as ori" + " where poi.securityGroup = sgi and poi.permission = ?" + " and poi.olatResource = ori"
						+ " and (ori.resId = ? or ori.resId = 0) and ori.resName = ?", new Object[] { permission, oresid, oresName },
				new Type[] { Hibernate.STRING, Hibernate.LONG, Hibernate.STRING });
		return res;
	}

	/**
	 * @see org.olat.basesecurity.Manager#getIdentitiesWithPermissionOnOlatResourceable(java.lang.String, org.olat.core.id.OLATResourceable
	 */
	public List getIdentitiesWithPermissionOnOlatResourceable(final String permission, final OLATResourceable olatResourceable) {
		Long oresid = olatResourceable.getResourceableId();
		if (oresid == null) {
			oresid = new Long(0); // TODO: make a method in
		}
		// OLATResorceManager, since this
		// is implementation detail
		final String oresName = olatResourceable.getResourceableTypeName();

		// if the olatResourceable is not persisted as OLATResource, then the answer
		// is false,
		// therefore we can use the query assuming there is an OLATResource
		final List res = DBFactory.getInstance().find(
				"select distinct im from" + " org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi," + " org.olat.basesecurity.IdentityImpl as im,"
						+ " org.olat.basesecurity.PolicyImpl as poi," + " org.olat.resource.OLATResourceImpl as ori" + " where im = sgmsi.identity"
						+ " and sgmsi.securityGroup = poi.securityGroup" + " and poi.permission = ?" + " and poi.olatResource = ori"
						+ " and (ori.resId = ? or ori.resId = 0) and ori.resName = ?",
				// if you have a type right, you autom. have all instance rights
				new Object[] { permission, oresid, oresName }, new Type[] { Hibernate.STRING, Hibernate.LONG, Hibernate.STRING });
		return res;
	}

	/**
	 * @see org.olat.basesecurity.Manager#getPoliciesOfSecurityGroup(org.olat.basesecurity.SecurityGroup)
	 */
	public List getPoliciesOfSecurityGroup(final SecurityGroup secGroup) {
		final List res = DBFactory.getInstance().find("select poi from org.olat.basesecurity.PolicyImpl as poi where poi.securityGroup = ?",
				new Object[] { secGroup.getKey() }, new Type[] { Hibernate.LONG });
		return res;
	}

	/**
	 * @see org.olat.basesecurity.BaseSecurity#getPoliciesOfResource(org.olat.core.id.OLATResourceable)
	 */
	@Override
	public List<Policy> getPoliciesOfResource(final OLATResourceable olatResourceable, final SecurityGroup secGroup) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select poi from ").append(PolicyImpl.class.getName()).append(" poi where ").append("(poi.olatResource.resId=:resId or poi.olatResource.resId=0)")
				.append(" and poi.olatResource.resName=:resName");
		if (secGroup != null) {
			sb.append(" and poi.securityGroup=:secGroup");
		}

		final DBQuery query = DBFactory.getInstance().createQuery(sb.toString());
		query.setLong("resId", olatResourceable.getResourceableId());
		query.setString("resName", olatResourceable.getResourceableTypeName());
		if (secGroup != null) {
			query.setEntity("secGroup", secGroup);
		}
		return query.list();
	}

	/**
	 * @see org.olat.basesecurity.Manager#isIdentityPermittedOnResourceable(org.olat.core.id.Identity, java.lang.String, org.olat.core.id.OLATResourceable
	 */
	/*
	 * MAY BE USED LATER - do not remove please public boolean isGroupPermittedOnResourceable(SecurityGroup secGroup, String permission, OLATResourceable
	 * olatResourceable) { Long oresid = olatResourceable.getResourceableId(); if (oresid == null) oresid = new Long(0); //TODO: make a method in OLATResorceManager,
	 * since this is implementation detail String oresName = olatResourceable.getResourceableTypeName(); List res = DB.getInstance().find("select count(poi) from"+ "
	 * org.olat.basesecurity.SecurityGroupImpl as sgi,"+ " org.olat.basesecurity.PolicyImpl as poi,"+ " org.olat.resource.OLATResourceImpl as ori"+ " where sgi.key =
	 * ?" + " and poi.securityGroup = sgi and poi.permission = ?"+ " and poi.olatResource = ori" + " and (ori.resId = ? or ori.resId = 0) and ori.resName = ?", new
	 * Object[] { secGroup.getKey(), permission, oresid, oresName }, new Type[] { Hibernate.LONG, Hibernate.STRING, Hibernate.LONG, Hibernate.STRING } ); Integer cntI =
	 * (Integer)res.get(0); int cnt = cntI.intValue(); return (cnt > 0); // can be > 1 if identity is in more the one group having the permission on the olatresourceable
	 * }
	 */

	public boolean isIdentityPermittedOnResourceable(final Identity identity, final String permission, final OLATResourceable olatResourceable) {
		return isIdentityPermittedOnResourceable(identity, permission, olatResourceable, true);
	}

	/**
	 * @see org.olat.basesecurity.Manager#isIdentityPermittedOnResourceable(org.olat.core.id.Identity, java.lang.String, org.olat.core.id.OLATResourceable boolean)
	 */
	public boolean isIdentityPermittedOnResourceable(final Identity identity, final String permission, final OLATResourceable olatResourceable,
			final boolean checkTypeRight) {
		final IdentityImpl iimpl = getImpl(identity);
		Long oresid = olatResourceable.getResourceableId();
		if (oresid == null) {
			oresid = new Long(0); // TODO: make a method in
		}
		// OLATResorceManager, since this
		// is implementation detail
		final String oresName = olatResourceable.getResourceableTypeName();
		// if the olatResourceable is not persisted as OLATResource, then the answer
		// is false,
		// therefore we can use the query assuming there is an OLATResource

		String queryString;
		if (checkTypeRight) {
			queryString = "select count(poi) from" + " org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi," + " org.olat.basesecurity.PolicyImpl as poi,"
					+ " org.olat.resource.OLATResourceImpl as ori" + " where sgmsi.identity = :identitykey and sgmsi.securityGroup =  poi.securityGroup"
					+ " and poi.permission = :permission and poi.olatResource = ori" + " and (ori.resId = :resid or ori.resId = 0) and ori.resName = :resname";
		} else {
			queryString = "select count(poi) from" + " org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi," + " org.olat.basesecurity.PolicyImpl as poi,"
					+ " org.olat.resource.OLATResourceImpl as ori" + " where sgmsi.identity = :identitykey and sgmsi.securityGroup =  poi.securityGroup"
					+ " and poi.permission = :permission and poi.olatResource = ori" + " and (ori.resId = :resid) and ori.resName = :resname";
		}
		final DBQuery query = DBFactory.getInstance().createQuery(queryString);
		query.setLong("identitykey", iimpl.getKey());
		query.setString("permission", permission);
		query.setLong("resid", oresid);
		query.setString("resname", oresName);
		query.setCacheable(true);
		final List res = query.list();
		final Long cntL = (Long) res.get(0);
		return (cntL.longValue() > 0); // can be > 1 if identity is in more the one group having
		// the permission on the olatresourceable
	}

	/**
	 * @see org.olat.basesecurity.Manager#getRoles(org.olat.core.id.Identity)
	 */
	public Roles getRoles(final Identity identity) {
		final boolean isAdmin = isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_ADMIN);
		final boolean isAuthor = isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);
		final boolean isGroupManager = isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GROUPMANAGER);
		final boolean isUserManager = isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_USERMANAGER);
		final boolean isGuestOnly = isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_GUESTONLY);
		final boolean isInstitutionalResourceManager = isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_INSTORESMANAGER);
		final boolean isInvitee = isIdentityInvited(identity);
		final Roles roles = new Roles(isAdmin, isUserManager, isGroupManager, isAuthor, isGuestOnly, isInstitutionalResourceManager, isInvitee);
		return roles;
	}

	/**
	 * scalar query : select sgi, poi, ori
	 * 
	 * @param identity
	 * @return List of policies
	 */
	public List getPoliciesOfIdentity(final Identity identity) {
		final IdentityImpl iimpl = getImpl(identity);
		final List res = DBFactory.getInstance().find(
				"select sgi, poi, ori from" + " org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi," + " org.olat.basesecurity.SecurityGroupImpl as sgi,"
						+ " org.olat.basesecurity.PolicyImpl as poi," + " org.olat.resource.OLATResourceImpl as ori"
						+ " where sgmsi.identity = ? and sgmsi.securityGroup = sgi" + " and poi.securityGroup = sgi and poi.olatResource = ori",
				new Object[] { iimpl.getKey() }, new Type[] { Hibernate.LONG });
		// scalar query, we get a List of Object[]'s
		return res;
	}

	@Override
	public void updatePolicy(final Policy policy, final Date from, final Date to) {
		((PolicyImpl) policy).setFrom(from);
		((PolicyImpl) policy).setTo(to);
		DBFactory.getInstance().updateObject(policy);
	}

	/**
	 * @see org.olat.basesecurity.Manager#isIdentityInSecurityGroup(org.olat.core.id.Identity, org.olat.basesecurity.SecurityGroup)
	 */
	public boolean isIdentityInSecurityGroup(final Identity identity, final SecurityGroup secGroup) {
		if (secGroup == null) { return false; }
		final String queryString = "select count(sgmsi) from  org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi where sgmsi.identity = :identitykey and sgmsi.securityGroup = :securityGroup";
		final DBQuery query = DBFactory.getInstance().createQuery(queryString);
		query.setLong("identitykey", identity.getKey());
		query.setLong("securityGroup", secGroup.getKey());
		query.setCacheable(true);
		final List res = query.list();
		final Long cntL = (Long) res.get(0);
		if (cntL.longValue() != 0 && cntL.longValue() != 1) { throw new AssertException("unique n-to-n must always yield 0 or 1"); }
		return (cntL.longValue() == 1);
	}

	/**
	 * @see org.olat.basesecurity.Manager#createAndPersistSecurityGroup()
	 */
	public SecurityGroup createAndPersistSecurityGroup() {
		final SecurityGroupImpl sgi = new SecurityGroupImpl();
		DBFactory.getInstance().saveObject(sgi);
		return sgi;
	}

	/**
	 * @see org.olat.basesecurity.Manager#deleteSecurityGroup(org.olat.basesecurity.SecurityGroup)
	 */
	public void deleteSecurityGroup(SecurityGroup secGroup) {
		// we do not use hibernate cascade="delete", but implement our own (to be
		// sure to understand our code)
		final DB db = DBFactory.getInstance();
		// FIXME: fj: Please review: Create rep entry, restart olat, delete the rep
		// entry. previous implementation resulted in orange screen
		// secGroup = (SecurityGroup)db.loadObject(secGroup); // so we can later
		// delete it (hibernate needs an associated session)
		secGroup = (SecurityGroup) db.loadObject(secGroup);
		// o_clusterREVIEW
		// db.reputInHibernateSessionCache(secGroup);

		/*
		 * if (!db.contains(secGroup)) { secGroup = (SecurityGroupImpl) db.loadObject(SecurityGroupImpl.class, secGroup.getKey()); }
		 */
		// 1) delete associated users (need to do it manually, hibernate knows
		// nothing about
		// the membership, modeled manually via many-to-one and not via set)
		db.delete("from org.olat.basesecurity.SecurityGroupMembershipImpl as msi where msi.securityGroup.key = ?", new Object[] { secGroup.getKey() },
				new Type[] { Hibernate.LONG });
		// 2) delete all policies
		db.delete("from org.olat.basesecurity.PolicyImpl as poi where poi.securityGroup = ?", new Object[] { secGroup.getKey() }, new Type[] { Hibernate.LONG });
		// 3) delete security group
		db.deleteObject(secGroup);
	}

	/**
	 * @see org.olat.basesecurity.Manager#addIdentityToSecurityGroup(org.olat.core.id.Identity, org.olat.basesecurity.SecurityGroup)
	 */
	public void addIdentityToSecurityGroup(final Identity identity, final SecurityGroup secGroup) {
		final SecurityGroupMembershipImpl sgmsi = new SecurityGroupMembershipImpl();
		sgmsi.setIdentity(identity);
		sgmsi.setSecurityGroup(secGroup);
		sgmsi.setLastModified(new Date());
		DBFactory.getInstance().saveObject(sgmsi);
		// TODO: tracing
	}

	/**
	 * @see org.olat.basesecurity.Manager#removeIdentityFromSecurityGroup(org.olat.core.id.Identity, org.olat.basesecurity.SecurityGroup)
	 */
	public void removeIdentityFromSecurityGroup(final Identity identity, final SecurityGroup secGroup) {
		final IdentityImpl iimpl = getImpl(identity);
		DBFactory.getInstance().delete("from org.olat.basesecurity.SecurityGroupMembershipImpl as msi where msi.identity.key = ? and msi.securityGroup.key = ?",
				new Object[] { iimpl.getKey(), secGroup.getKey() }, new Type[] { Hibernate.LONG, Hibernate.LONG });
	}

	/**
	 * @see org.olat.basesecurity.Manager#createAndPersistPolicy(org.olat.basesecurity.SecurityGroup, java.lang.String, org.olat.core.id.OLATResourceable
	 */
	@Override
	public Policy createAndPersistPolicy(final SecurityGroup secGroup, final String permission, final OLATResourceable olatResourceable) {
		return createAndPersistPolicy(secGroup, permission, null, null, olatResourceable);
	}

	/**
	 * @see org.olat.basesecurity.BaseSecurity#createAndPersistPolicy(org.olat.basesecurity.SecurityGroup, java.lang.String, java.util.Date, java.util.Date,
	 *      org.olat.core.id.OLATResourceable)
	 */
	@Override
	public Policy createAndPersistPolicy(final SecurityGroup secGroup, final String permission, final Date from, final Date to, final OLATResourceable olatResourceable) {
		final OLATResource olatResource = orm.findOrPersistResourceable(olatResourceable);
		return createAndPersistPolicyWithResource(secGroup, permission, from, to, olatResource);
	}

	/**
	 * @see org.olat.basesecurity.BaseSecurity#createAndPersistPolicyWithResource(org.olat.basesecurity.SecurityGroup, java.lang.String, org.olat.resource.OLATResource)
	 */
	@Override
	public Policy createAndPersistPolicyWithResource(final SecurityGroup secGroup, final String permission, final OLATResource olatResource) {
		return createAndPersistPolicyWithResource(secGroup, permission, null, null, olatResource);
	}

	/**
	 * Creates a policy and persists on the database
	 * 
	 * @see org.olat.basesecurity.BaseSecurity#createAndPersistPolicyWithResource(org.olat.basesecurity.SecurityGroup, java.lang.String, java.util.Date, java.util.Date,
	 *      org.olat.resource.OLATResource)
	 */
	@Override
	public Policy createAndPersistPolicyWithResource(final SecurityGroup secGroup, final String permission, final Date from, final Date to,
			final OLATResource olatResource) {
		final PolicyImpl pi = new PolicyImpl();
		pi.setSecurityGroup(secGroup);
		pi.setOlatResource(olatResource);
		pi.setPermission(permission);
		pi.setFrom(from);
		pi.setTo(to);
		DBFactory.getInstance().saveObject(pi);
		return pi;
	}

	/**
	 * Helper method that only creates a policy only if no such policy exists in the database
	 * 
	 * @param secGroup
	 * @param permission
	 * @param olatResourceable
	 * @return Policy
	 */
	private Policy createAndPersistPolicyIfNotExists(final SecurityGroup secGroup, final String permission, final OLATResourceable olatResourceable) {
		final OLATResource olatResource = orm.findOrPersistResourceable(olatResourceable);
		final Policy existingPolicy = findPolicy(secGroup, permission, olatResource);
		if (existingPolicy == null) { return createAndPersistPolicy(secGroup, permission, olatResource); }
		return existingPolicy;
	}

	Policy findPolicy(final SecurityGroup secGroup, final String permission, final OLATResource olatResource) {
		final Long secKey = secGroup.getKey();
		final Long orKey = olatResource.getKey();

		final List res = DBFactory.getInstance().find(
				" select poi from org.olat.basesecurity.PolicyImpl as poi" + " where poi.permission = ? and poi.olatResource = ? and poi.securityGroup = ?",
				new Object[] { permission, orKey, secKey }, new Type[] { Hibernate.STRING, Hibernate.LONG, Hibernate.LONG });
		if (res.size() == 0) {
			return null;
		} else {
			final PolicyImpl poi = (PolicyImpl) res.get(0);
			return poi;
		}
	}

	private void deletePolicy(final Policy policy) {
		DBFactory.getInstance().deleteObject(policy);
	}

	/**
	 * @see org.olat.basesecurity.Manager#deletePolicy(org.olat.basesecurity.SecurityGroup, java.lang.String, org.olat.core.id.OLATResourceable
	 */
	public void deletePolicy(final SecurityGroup secGroup, final String permission, final OLATResourceable olatResourceable) {
		final OLATResource olatResource = orm.findResourceable(olatResourceable);
		if (olatResource == null) { throw new AssertException("cannot delete policy of a null olatresourceable!"); }
		final Policy p = findPolicy(secGroup, permission, olatResource);
		// fj: introduced strict testing here on purpose
		if (p == null) { throw new AssertException("findPolicy returned null, cannot delete policy"); }
		deletePolicy(p);
	}

	/**
	 * @see org.olat.basesecurity.BaseSecurity#createAndPersistInvitation()
	 */
	@Override
	public Invitation createAndPersistInvitation() {
		final SecurityGroup secGroup = new SecurityGroupImpl();
		DBFactory.getInstance().saveObject(secGroup);

		final InvitationImpl invitation = new InvitationImpl();
		invitation.setToken(UUID.randomUUID().toString());
		invitation.setSecurityGroup(secGroup);
		DBFactory.getInstance().saveObject(invitation);
		return invitation;
	}

	/**
	 * @see org.olat.basesecurity.BaseSecurity#updateInvitation(org.olat.basesecurity.Invitation)
	 */
	@Override
	public void updateInvitation(final Invitation invitation) {
		DBFactory.getInstance().updateObject(invitation);
	}

	/**
	 * @see org.olat.basesecurity.BaseSecurity#hasInvitationPolicies(java.lang.String, java.util.Date)
	 */
	@Override
	public boolean hasInvitationPolicies(final String token, final Date atDate) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select count(policy) from ").append(PolicyImpl.class.getName()).append(" as policy, ").append(InvitationImpl.class.getName())
				.append(" as invitation ").append(" inner join policy.securityGroup secGroup ").append(" where invitation.securityGroup=secGroup ")
				.append(" and invitation.token=:token");
		if (atDate != null) {
			sb.append(" and (policy.from is null or policy.from<=:date)").append(" and (policy.to is null or policy.to>=:date)");
		}

		final DBQuery query = DBFactory.getInstance().createQuery(sb.toString());
		query.setString("token", token);
		if (atDate != null) {
			query.setDate("date", atDate);
		}

		final Number counter = (Number) query.uniqueResult();
		return counter.intValue() > 0;
	}

	/**
	 * @see org.olat.basesecurity.BaseSecurity#findInvitation(org.olat.basesecurity.SecurityGroup)
	 */
	@Override
	public Invitation findInvitation(final SecurityGroup secGroup) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select invitation from ").append(InvitationImpl.class.getName()).append(" as invitation ").append(" where invitation.securityGroup=:secGroup ");

		final DBQuery query = DBFactory.getInstance().createQuery(sb.toString());
		query.setEntity("secGroup", secGroup);

		final List<Invitation> invitations = (List<Invitation>) query.list();
		if (invitations.isEmpty()) { return null; }
		return invitations.get(0);
	}

	/**
	 * @see org.olat.basesecurity.BaseSecurity#findInvitation(java.lang.String)
	 */
	@Override
	public Invitation findInvitation(final String token) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select invitation from ").append(InvitationImpl.class.getName()).append(" as invitation ").append(" where invitation.token=:token");

		final DBQuery query = DBFactory.getInstance().createQuery(sb.toString());
		query.setString("token", token);

		final List<Invitation> invitations = (List<Invitation>) query.list();
		if (invitations.isEmpty()) { return null; }
		return invitations.get(0);
	}

	/**
	 * @see org.olat.basesecurity.BaseSecurity#isIdentityInvited(org.olat.core.id.Identity)
	 */
	@Override
	public boolean isIdentityInvited(final Identity identity) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select count(invitation) from ").append(InvitationImpl.class.getName()).append(" as invitation ")
				.append("inner join invitation.securityGroup secGroup ").append("where secGroup in (").append(" select membership.securityGroup from ")
				.append(SecurityGroupMembershipImpl.class.getName()).append(" as membership").append("  where membership.identity=:identity").append(")");

		final DBQuery query = DBFactory.getInstance().createQuery(sb.toString());
		query.setEntity("identity", identity);

		final Number invitations = (Number) query.uniqueResult();
		return invitations.intValue() > 0;
	}

	/**
	 * @see org.olat.basesecurity.BaseSecurity#deleteInvitation(org.olat.basesecurity.Invitation)
	 */
	@Override
	public void deleteInvitation(final Invitation invitation) {
		DBFactory.getInstance().deleteObject(invitation);
	}

	/**
	 * @see org.olat.basesecurity.BaseSecurity#cleanUpInvitations()
	 */
	@Override
	public void cleanUpInvitations() {
		final Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		final Date currentTime = cal.getTime();
		cal.add(Calendar.HOUR, -6);
		final Date dateLimit = cal.getTime();

		final StringBuilder sb = new StringBuilder();
		sb.append("select invitation from ").append(InvitationImpl.class.getName()).append(" as invitation ").append(" inner join invitation.securityGroup secGroup ")
				.append(" where invitation.creationDate<:dateLimit")
				// someone can create an invitation but not add it to a policy within millisecond
				.append(" and secGroup not in (")
				// select all valid policies from this security group
				.append("  select policy.securityGroup from ").append(PolicyImpl.class.getName()).append(" as policy ")
				.append("   where (policy.from is null or policy.from<=:currentDate)").append("   and (policy.to is null or policy.to>=:currentDate)").append("  )");

		final DBQuery query = DBFactory.getInstance().createQuery(sb.toString());
		query.setDate("currentDate", currentTime);
		query.setDate("dateLimit", dateLimit);
		final List<Invitation> oldInvitations = (List<Invitation>) query.list();
		if (oldInvitations.isEmpty()) { return; }

		final SecurityGroup olatUserSecGroup = findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		for (final Invitation invitation : oldInvitations) {
			final List<Identity> identities = getIdentitiesOfSecurityGroup(invitation.getSecurityGroup());
			// normally only one identity
			for (final Identity identity : identities) {
				if (identity.getStatus().compareTo(Identity.STATUS_VISIBLE_LIMIT) >= 0) {
					// already deleted
				} else if (isIdentityInSecurityGroup(identity, olatUserSecGroup)) {
					// out of scope
				} else {
					// delete user
					UserDeletionManager.getInstance().deleteIdentity(identity);
				}
			}
			DBFactory.getInstance().deleteObject(invitation);
			DBFactory.getInstance().intermediateCommit();
		}
	}

	private IdentityImpl getImpl(final Identity identity) {
		// since we are a persistingmanager, we also only accept real identities
		if (!(identity instanceof IdentityImpl)) { throw new AssertException("identity was not of type identityimpl, but " + identity.getClass().getName()); }
		final IdentityImpl iimpl = (IdentityImpl) identity;
		return iimpl;
	}

	/**
	 * @param username the username
	 * @param user the presisted User
	 * @param authusername the username used as authentication credential (=username for provider "OLAT")
	 * @param provider the provider of the authentication ("OLAT" or "AAI"). If null, no authentication token is generated.
	 * @param credential the credentials or null if not used
	 * @return Identity
	 */
	public Identity createAndPersistIdentity(final String username, final User user, final String provider, final String authusername, final String credential) {
		final IdentityImpl iimpl = new IdentityImpl(username, user);
		DBFactory.getInstance().saveObject(iimpl);
		if (provider != null) {
			createAndPersistAuthentication(iimpl, provider, authusername, credential);
		}
		notifyNewIdentityCreated(iimpl);
		return iimpl;
	}

	/**
	 * @param username the username
	 * @param user the unpresisted User
	 * @param authusername the username used as authentication credential (=username for provider "OLAT")
	 * @param provider the provider of the authentication ("OLAT" or "AAI"). If null, no authentication token is generated.
	 * @param credential the credentials or null if not used
	 * @return Identity
	 */
	public Identity createAndPersistIdentityAndUser(final String username, final User user, final String provider, final String authusername, final String credential) {
		DBFactory.getInstance().saveObject(user);
		final IdentityImpl iimpl = new IdentityImpl(username, user);
		DBFactory.getInstance().saveObject(iimpl);
		if (provider != null) {
			createAndPersistAuthentication(iimpl, provider, authusername, credential);
		}
		notifyNewIdentityCreated(iimpl);
		return iimpl;
	}

	private void notifyNewIdentityCreated(final Identity newIdentity) {
		// Save the identity on the DB. So can the listeners of the event retrieve it
		// in cluster mode
		DBFactory.getInstance().intermediateCommit();
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new NewIdentityCreatedEvent(newIdentity), IDENTITY_EVENT_CHANNEL);
	}

	/**
	 * @see org.olat.basesecurity.Manager#getIdentitiesOfSecurityGroup(org.olat.basesecurity.SecurityGroup)
	 */
	public List getIdentitiesOfSecurityGroup(final SecurityGroup secGroup) {
		if (secGroup == null) { throw new AssertException("getIdentitiesOfSecurityGroup: ERROR secGroup was null !!"); }
		final DB db = DBFactory.getInstance();
		if (db == null) { throw new AssertException("getIdentitiesOfSecurityGroup: ERROR db was null !!"); }
		final List idents = DBFactory.getInstance().find(
				"select ii from" + " org.olat.basesecurity.IdentityImpl as ii inner join fetch ii.user as iuser, "
						+ " org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi " + " where sgmsi.securityGroup = ? and sgmsi.identity = ii",
				new Object[] { secGroup.getKey() }, new Type[] { Hibernate.LONG });

		return idents;

	}

	/**
	 * @see org.olat.basesecurity.Manager#getIdentitiesAndDateOfSecurityGroup(org.olat.basesecurity.SecurityGroup)
	 */
	public List getIdentitiesAndDateOfSecurityGroup(final SecurityGroup secGroup) {
		return getIdentitiesAndDateOfSecurityGroup(secGroup, false);
	}

	/**
	 * @see org.olat.basesecurity.Manager#getIdentitiesAndDateOfSecurityGroup(org.olat.basesecurity.SecurityGroup)
	 * @param sortedByAddDate true= return list of idenities sorted by added date
	 */
	public List getIdentitiesAndDateOfSecurityGroup(final SecurityGroup secGroup, final boolean sortedByAddDate) {
		final StringBuilder queryString = new StringBuilder();
		queryString.append("select ii, sgmsi.lastModified from" + " org.olat.basesecurity.IdentityImpl as ii inner join fetch ii.user as iuser, "
				+ " org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi " + " where sgmsi.securityGroup = ? and sgmsi.identity = ii");
		if (sortedByAddDate) {
			queryString.append(" order by sgmsi.lastModified");
		}
		final List identAndDate = DBFactory.getInstance().find(queryString.toString(), new Object[] { secGroup.getKey() }, new Type[] { Hibernate.LONG });
		return identAndDate;
	}

	/**
	 * @see org.olat.basesecurity.Manager#getSecurityGroupJoinDateForIdentity(org.olat.basesecurity.SecurityGroup, org.olat.core.id.Identity)
	 */
	public Date getSecurityGroupJoinDateForIdentity(final SecurityGroup secGroup, final Identity identity) {
		final String query = "select creationDate from " + "  org.olat.basesecurity.SecurityGroupMembershipImpl as sgi "
				+ " where sgi.securityGroup = :secGroup and sgi.identity = :identId";

		final DB db = DBFactory.getInstance();
		final DBQuery dbq = db.createQuery(query);
		dbq.setLong("identId", identity.getKey().longValue());
		dbq.setLong("secGroup", secGroup.getKey());
		final List result = dbq.list();
		if (result.size() == 0) {
			return null;
		} else {
			return (Date) result.get(0);
		}
	}

	/**
	 * @see org.olat.basesecurity.Manager#countIdentitiesOfSecurityGroup(org.olat.basesecurity.SecurityGroup)
	 */
	public int countIdentitiesOfSecurityGroup(final SecurityGroup secGroup) {
		final DB db = DBFactory.getInstance();
		final String q = "select count(sgm) from org.olat.basesecurity.SecurityGroupMembershipImpl sgm where sgm.securityGroup = :group";
		final DBQuery query = db.createQuery(q);
		query.setEntity("group", secGroup);
		final int result = ((Long) query.list().get(0)).intValue();
		return result;
	}

	/**
	 * @see org.olat.basesecurity.Manager#createAndPersistNamedSecurityGroup(java.lang.String)
	 */
	public SecurityGroup createAndPersistNamedSecurityGroup(final String groupName) {
		final SecurityGroup secG = createAndPersistSecurityGroup();
		final NamedGroupImpl ngi = new NamedGroupImpl(groupName, secG);
		DBFactory.getInstance().saveObject(ngi);
		return secG;
	}

	/**
	 * @see org.olat.basesecurity.Manager#findSecurityGroupByName(java.lang.String)
	 */
	public SecurityGroup findSecurityGroupByName(final String securityGroupName) {
		final List group = DBFactory.getInstance().find(
				"select sgi from" + " org.olat.basesecurity.NamedGroupImpl as ngroup," + " org.olat.basesecurity.SecurityGroupImpl as sgi"
						+ " where ngroup.groupName = ? and ngroup.securityGroup = sgi", new Object[] { securityGroupName }, new Type[] { Hibernate.STRING });
		final int size = group.size();
		if (size == 0) { return null; }
		if (size != 1) { throw new AssertException("non unique name in namedgroup: " + securityGroupName); }
		final SecurityGroup sg = (SecurityGroup) group.get(0);
		return sg;
	}

	/**
	 * @see org.olat.basesecurity.Manager#findIdentityByName(java.lang.String)
	 */
	public Identity findIdentityByName(final String identityName) {
		if (identityName == null) { throw new AssertException("findIdentitybyName: name was null"); }
		final List identities = DBFactory.getInstance().find("select ident from org.olat.basesecurity.IdentityImpl as ident where ident.name = ?",
				new Object[] { identityName }, new Type[] { Hibernate.STRING });
		final int size = identities.size();
		if (size == 0) { return null; }
		if (size != 1) { throw new AssertException("non unique name in identites: " + identityName); }
		final Identity identity = (Identity) identities.get(0);
		return identity;
	}

	/**
	 * @see org.olat.basesecurity.Manager#loadIdentityByKey(java.lang.Long)
	 */
	public Identity loadIdentityByKey(final Long identityKey) {
		if (identityKey == null) { throw new AssertException("findIdentitybyKey: key is null"); }
		if (identityKey.equals(Long.valueOf(0))) { return null; }
		return (Identity) DBFactory.getInstance().loadObject(IdentityImpl.class, identityKey);
	}

	/**
	 * @see org.olat.basesecurity.Manager#loadIdentityByKey(java.lang.Long,boolean)
	 */
	public Identity loadIdentityByKey(final Long identityKey, final boolean strict) {
		if (strict) { return loadIdentityByKey(identityKey); }

		final String queryStr = "select ident from org.olat.basesecurity.IdentityImpl as ident where ident.key=:identityKey";
		final DBQuery dbq = DBFactory.getInstance().createQuery(queryStr);
		dbq.setLong("identityKey", identityKey);
		final List<Identity> identities = dbq.list();
		if (identities.size() == 1) { return identities.get(0); }
		return null;
	}

	/**
	 * @see org.olat.basesecurity.Manager#countUniqueUserLoginsSince(java.util.Date)
	 */
	public Long countUniqueUserLoginsSince(final Date lastLoginLimit) {
		final String queryStr = "Select count(ident) from org.olat.core.id.Identity as ident where "
				+ "ident.lastLogin > :lastLoginLimit and ident.lastLogin != ident.creationDate";
		final DBQuery dbq = DBFactory.getInstance().createQuery(queryStr);
		dbq.setDate("lastLoginLimit", lastLoginLimit);
		final List res = dbq.list();
		final Long cntL = (Long) res.get(0);
		return cntL;
	}

	/**
	 * @see org.olat.basesecurity.Manager#getAuthentications(org.olat.core.id.Identity)
	 */
	public List<Authentication> getAuthentications(final Identity identity) {
		return DBFactory.getInstance().find(
				"select auth from  org.olat.basesecurity.AuthenticationImpl as auth " + "inner join fetch auth.identity as ident where ident.key = ?",
				new Object[] { identity.getKey() }, new Type[] { Hibernate.LONG });
	}

	/**
	 * @see org.olat.basesecurity.Manager#createAndPersistAuthentication(org.olat.core.id.Identity, java.lang.String, java.lang.String, java.lang.String)
	 */
	public Authentication createAndPersistAuthentication(final Identity ident, final String provider, final String authUserName, final String credential) {
		final AuthenticationImpl authImpl = new AuthenticationImpl(ident, provider, authUserName, credential);
		DBFactory.getInstance().saveObject(authImpl);
		return authImpl;
	}

	/**
	 * @see org.olat.basesecurity.Manager#findAuthentication(org.olat.core.id.Identity, java.lang.String)
	 */
	public Authentication findAuthentication(final Identity identity, final String provider) {
		if (identity == null) { throw new IllegalArgumentException("identity must not be null"); }
		final DB dbInstance = DBFactory.getInstance();
		if (dbInstance == null) { throw new IllegalStateException("DBFactory.getInstance() returned null"); }
		final List results = dbInstance.find("select auth from org.olat.basesecurity.AuthenticationImpl as auth where auth.identity.key = ? and auth.provider = ?",
				new Object[] { identity.getKey(), provider }, new Type[] { Hibernate.LONG, Hibernate.STRING });
		if (results == null || results.size() == 0) { return null; }
		if (results.size() > 1) { throw new AssertException("Found more than one Authentication for a given subject and a given provider."); }
		return (Authentication) results.get(0);
	}

	/**
	 * @see org.olat.basesecurity.Manager#deleteAuthentication(org.olat.basesecurity.Authentication)
	 */
	public void deleteAuthentication(final Authentication auth) {
		DBFactory.getInstance().deleteObject(auth);
	}

	/**
	 * @see org.olat.basesecurity.Manager#findAuthenticationByAuthusername(java.lang.String, java.lang.String)
	 */
	public Authentication findAuthenticationByAuthusername(final String authusername, final String provider) {
		final List results = DBFactory.getInstance().find("from org.olat.basesecurity.AuthenticationImpl as auth where auth.provider = ? and auth.authusername = ?",
				new Object[] { provider, authusername }, new Type[] { Hibernate.STRING, Hibernate.STRING });
		if (results.size() == 0) { return null; }
		if (results.size() != 1) { throw new AssertException(
				"more than one entry for the a given authusername and provider, should never happen (even db has a unique constraint on those columns combined) "); }
		final Authentication auth = (Authentication) results.get(0);
		return auth;
	}

	/**
	 * @see org.olat.basesecurity.Manager#getVisibleIdentitiesByPowerSearch(java.lang.String, java.util.Map, boolean, org.olat.basesecurity.SecurityGroup[],
	 *      org.olat.basesecurity.PermissionOnResourceable[], java.lang.String[], java.util.Date, java.util.Date)
	 */
	public List getVisibleIdentitiesByPowerSearch(final String login, final Map<String, String> userproperties, final boolean userPropertiesAsIntersectionSearch,
			final SecurityGroup[] groups, final PermissionOnResourceable[] permissionOnResources, final String[] authProviders, final Date createdAfter,
			final Date createdBefore) {
		return getIdentitiesByPowerSearch(login, userproperties, userPropertiesAsIntersectionSearch, groups, permissionOnResources, authProviders, createdAfter,
				createdBefore, null, null, Identity.STATUS_VISIBLE_LIMIT);
	}

	/**
	 * @see org.olat.basesecurity.Manager#getIdentitiesByPowerSearch(java.lang.String, java.util.Map, boolean, org.olat.basesecurity.SecurityGroup[],
	 *      org.olat.basesecurity.PermissionOnResourceable[], java.lang.String[], java.util.Date, java.util.Date, java.lang.Integer)
	 */
	public List getIdentitiesByPowerSearch(String login, final Map<String, String> userproperties, final boolean userPropertiesAsIntersectionSearch,
			final SecurityGroup[] groups, final PermissionOnResourceable[] permissionOnResources, final String[] authProviders, final Date createdAfter,
			final Date createdBefore, final Date userLoginAfter, final Date userLoginBefore, final Integer status) {
		final boolean hasGroups = (groups != null && groups.length > 0);
		final boolean hasPermissionOnResources = (permissionOnResources != null && permissionOnResources.length > 0);
		final boolean hasAuthProviders = (authProviders != null && authProviders.length > 0);

		// select identity and inner join with user to optimize query
		StringBuilder sb = new StringBuilder();
		if (hasAuthProviders) {
			// I know, it looks wrong but I need to do the join reversed since it is not possible to
			// do this query with a left join that starts with the identity using hibernate HQL. A left
			// or right join is necessary since it is totally ok to have null values as authentication
			// providers (e.g. when searching for users that do not have any authentication providers at all!).
			// It took my quite a while to make this work, so think twice before you change anything here!
			sb = new StringBuilder("select distinct ident from org.olat.basesecurity.AuthenticationImpl as auth right join auth.identity as ident ");
		} else {
			sb = new StringBuilder("select distinct ident from org.olat.core.id.Identity as ident ");
		}
		// In any case join with the user. Don't join-fetch user, this breaks the query
		// because of the user fields (don't know exactly why this behaves like
		// this)
		sb.append(" join ident.user as user ");

		if (hasGroups) {
			// join over security group memberships
			sb.append(" ,org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi ");

		}
		if (hasPermissionOnResources) {
			// join over policies
			sb.append(" ,org.olat.basesecurity.SecurityGroupMembershipImpl as policyGroupMembership ");
			sb.append(" ,org.olat.basesecurity.PolicyImpl as policy ");
			sb.append(" ,org.olat.resource.OLATResourceImpl as resource ");
		}

		// complex where clause only when values are available
		if (login != null || (userproperties != null && !userproperties.isEmpty()) || createdAfter != null || createdBefore != null || hasAuthProviders || hasGroups
				|| hasPermissionOnResources || status != null) {
			sb.append(" where ");
			boolean needsAnd = false;
			boolean needsUserPropertiesJoin = false;

			// treat login and userProperties as one element in this query
			if (login != null && (userproperties != null && !userproperties.isEmpty())) {
				sb.append(" ( ");
			}
			// append queries for user attributes
			if (login != null) {
				login = makeFuzzyQueryString(login);
				if (login.contains("_") && (dbVendor.equals("hsqldb") || dbVendor.equals("oracle"))) {
					// hsqldb needs special ESCAPE sequence to search for escaped strings
					sb.append(" ident.name like :login ESCAPE '\\'");
				} else {
					sb.append(" ident.name like :login");
				}
				// if user fields follow a join element is needed
				needsUserPropertiesJoin = true;
				// at least one user field used, after this and is required
				needsAnd = true;
			}

			// append queries for user fields
			if (userproperties != null && !userproperties.isEmpty()) {
				final HashMap<String, String> emailProperties = new HashMap<String, String>();
				final HashMap<String, String> otherProperties = new HashMap<String, String>();

				// split the user fields into two groups
				for (final String key : userproperties.keySet()) {
					if (key.toLowerCase().contains("email")) {
						emailProperties.put(key, userproperties.get(key));
					} else {
						otherProperties.put(key, userproperties.get(key));
					}
				}

				// handle email fields special: search in all email fields
				if (!emailProperties.isEmpty()) {
					needsUserPropertiesJoin = checkIntersectionInUserProperties(sb, needsUserPropertiesJoin, userPropertiesAsIntersectionSearch);
					final boolean moreThanOne = emailProperties.size() > 1;
					if (moreThanOne) {
						sb.append("(");
					}
					boolean needsOr = false;
					for (final String key : emailProperties.keySet()) {
						if (needsOr) {
							sb.append(" or ");
						}
						sb.append(" user.properties['").append(key).append("'] like :").append(key).append("_value ");
						if (dbVendor.equals("hsqldb") || dbVendor.equals("oracle")) {
							sb.append(" escape '\\'");
						}
						needsOr = true;
					}
					if (moreThanOne) {
						sb.append(")");
					}
					// cleanup
					emailProperties.clear();
				}

				// add other fields
				for (final String key : otherProperties.keySet()) {
					needsUserPropertiesJoin = checkIntersectionInUserProperties(sb, needsUserPropertiesJoin, userPropertiesAsIntersectionSearch);
					sb.append(" user.properties['").append(key).append("'] like :").append(key).append("_value ");
					if (dbVendor.equals("hsqldb") || dbVendor.equals("oracle")) {
						sb.append(" escape '\\'");
					}
					needsAnd = true;
				}
				// cleanup
				otherProperties.clear();
				// at least one user field used, after this and is required
				needsAnd = true;
			}
			// end of user fields and login part
			if (login != null && (userproperties != null && !userproperties.isEmpty())) {
				sb.append(" ) ");
			}
			// now continue with the other elements. They are joined with an AND connection

			// append query for named security groups
			if (hasGroups) {
				needsAnd = checkAnd(sb, needsAnd);
				sb.append(" (");
				for (int i = 0; i < groups.length; i++) {
					sb.append(" sgmsi.securityGroup=:group_").append(i);
					if (i < (groups.length - 1)) {
						sb.append(" or ");
					}
				}
				sb.append(") ");
				sb.append(" and sgmsi.identity=ident ");
			}

			// append query for policies
			if (hasPermissionOnResources) {
				needsAnd = checkAnd(sb, needsAnd);
				sb.append(" (");
				for (int i = 0; i < permissionOnResources.length; i++) {
					sb.append(" (");
					sb.append(" policy.permission=:permission_").append(i);
					sb.append(" and policy.olatResource = resource ");
					sb.append(" and resource.resId = :resourceId_").append(i);
					sb.append(" and resource.resName = :resourceName_").append(i);
					sb.append(" ) ");
					if (i < (permissionOnResources.length - 1)) {
						sb.append(" or ");
					}
				}
				sb.append(") ");
				sb.append(" and policy.securityGroup=policyGroupMembership.securityGroup ");
				sb.append(" and policyGroupMembership.identity=ident ");
			}

			// append query for authentication providers
			if (hasAuthProviders) {
				needsAnd = checkAnd(sb, needsAnd);
				sb.append(" (");
				for (int i = 0; i < authProviders.length; i++) {
					// special case for null auth provider
					if (authProviders[i] == null) {
						sb.append(" auth is null ");
					} else {
						sb.append(" auth.provider=:authProvider_").append(i);
					}
					if (i < (authProviders.length - 1)) {
						sb.append(" or ");
					}
				}
				sb.append(") ");
			}

			// append query for creation date restrictions
			if (createdAfter != null) {
				needsAnd = checkAnd(sb, needsAnd);
				sb.append(" ident.creationDate >= :createdAfter ");
			}
			if (createdBefore != null) {
				needsAnd = checkAnd(sb, needsAnd);
				sb.append(" ident.creationDate <= :createdBefore ");
			}
			if (userLoginAfter != null) {
				needsAnd = checkAnd(sb, needsAnd);
				sb.append(" ident.lastLogin >= :lastloginAfter ");
			}
			if (userLoginBefore != null) {
				needsAnd = checkAnd(sb, needsAnd);
				sb.append(" ident.lastLogin <= :lastloginBefore ");
			}

			if (status != null) {
				if (status.equals(Identity.STATUS_VISIBLE_LIMIT)) {
					// search for all status smaller than visible limit
					needsAnd = checkAnd(sb, needsAnd);
					sb.append(" ident.status < :status ");
				} else {
					// search for certain status
					needsAnd = checkAnd(sb, needsAnd);
					sb.append(" ident.status = :status ");
				}
			}
		}

		// create query object now from string
		final String query = sb.toString();
		final DB db = DBFactory.getInstance();
		final DBQuery dbq = db.createQuery(query);

		// add user attributes
		if (login != null) {
			dbq.setString("login", login);
		}

		// add user properties attributes
		if (userproperties != null && !userproperties.isEmpty()) {
			for (final String key : userproperties.keySet()) {
				String value = userproperties.get(key);
				value = makeFuzzyQueryString(value);
				dbq.setString(key + "_value", value.toLowerCase());
			}
		}

		// add named security group names
		if (hasGroups) {
			for (int i = 0; i < groups.length; i++) {
				final SecurityGroupImpl group = (SecurityGroupImpl) groups[i]; // need to work with impls
				dbq.setEntity("group_" + i, group);
			}
		}

		// add policies
		if (hasPermissionOnResources) {
			for (int i = 0; i < permissionOnResources.length; i++) {
				final PermissionOnResourceable permissionOnResource = permissionOnResources[i];
				dbq.setString("permission_" + i, permissionOnResource.getPermission());
				final Long id = permissionOnResource.getOlatResourceable().getResourceableId();
				dbq.setLong("resourceId_" + i, (id == null ? 0 : id.longValue()));
				dbq.setString("resourceName_" + i, permissionOnResource.getOlatResourceable().getResourceableTypeName());
			}
		}

		// add authentication providers
		if (hasAuthProviders) {
			for (int i = 0; i < authProviders.length; i++) {
				final String authProvider = authProviders[i];
				if (authProvider != null) {
					dbq.setString("authProvider_" + i, authProvider);
				}
				// ignore null auth provider, already set to null in query
			}
		}

		// add date restrictions
		if (createdAfter != null) {
			dbq.setDate("createdAfter", createdAfter);
		}
		if (createdBefore != null) {
			dbq.setDate("createdBefore", createdBefore);
		}
		if (userLoginAfter != null) {
			dbq.setDate("lastloginAfter", userLoginAfter);
		}
		if (userLoginBefore != null) {
			dbq.setDate("lastloginBefore", userLoginBefore);
		}

		if (status != null) {
			dbq.setInteger("status", status);
		}
		// execute query
		return dbq.list();
	}

	/**
	 * @param dbVendor
	 */
	public void setDbVendor(final String dbVendor) {
		this.dbVendor = dbVendor;
	}

	/**
	 * @see org.olat.basesecurity.Manager#isIdentityVisible(java.lang.String)
	 */
	public boolean isIdentityVisible(final String identityName) {
		if (identityName == null) { throw new AssertException("findIdentitybyName: name was null"); }
		final String queryString = "select count(ident) from org.olat.basesecurity.IdentityImpl as ident where ident.name = :identityName and ident.status < :status";
		final DBQuery dbq = DBFactory.getInstance().createQuery(queryString);
		dbq.setString("identityName", identityName);
		dbq.setInteger("status", Identity.STATUS_VISIBLE_LIMIT);
		final List res = dbq.list();
		final Long cntL = (Long) res.get(0);
		return (cntL.longValue() > 0);
	}

	private boolean checkAnd(final StringBuilder sb, final boolean needsAnd) {
		if (needsAnd) {
			sb.append(" and ");
		}
		return true;
	}

	private boolean checkIntersectionInUserProperties(final StringBuilder sb, final boolean needsJoin, final boolean userPropertiesAsIntersectionSearch) {
		if (needsJoin) {
			if (userPropertiesAsIntersectionSearch) {
				sb.append(" and ");
			} else {
				sb.append(" or ");
			}
		}
		return true;
	}

	/**
	 * Helper method that replaces * with % and appends and prepends % to the string to make fuzzy SQL match when using like
	 * 
	 * @param email
	 * @return fuzzized string
	 */
	private String makeFuzzyQueryString(String string) {
		// By default only fuzzyfy at the end. Usually it makes no sense to do a
		// fuzzy search with % at the beginning, but it makes the query very very
		// slow since it can not use any index and must perform a fulltext search.
		// User can always use * to make it a really fuzzy search query
		string = string.replace('*', '%');
		string = string + "%";
		// with 'LIKE' the character '_' is a wildcard which matches exactly one character.
		// To test for literal instances of '_', we have to escape it.
		string = string.replace("_", "\\_");
		return string;
	}

	/**
	 * @see org.olat.basesecurity.Manager#saveIdentityStatus(org.olat.core.id.Identity)
	 */
	public void saveIdentityStatus(Identity identity, final Integer status) {
		// FIXME: cg: would be nice if the updated identity is returned. no loading required afterwards.
		identity = (Identity) DBFactory.getInstance().loadObject(identity.getClass(), identity.getKey());
		identity.setStatus(status);
		DBFactory.getInstance().updateObject(identity);
	}

	public List<SecurityGroup> getSecurityGroupsForIdentity(final Identity identity) {
		final DB db = DBFactory.getInstance();
		final List<SecurityGroup> secGroups = db.find("select sgi from" + " org.olat.basesecurity.SecurityGroupImpl as sgi,"
				+ " org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi " + " where sgmsi.securityGroup = sgi and sgmsi.identity = ?",
				new Object[] { identity.getKey() }, new Type[] { Hibernate.LONG });
		return secGroups;
	}

	/**
	 * @see org.olat.basesecurity.Manager#getAndUpdateAnonymousUserForLanguage(java.util.Locale)
	 */
	public Identity getAndUpdateAnonymousUserForLanguage(final Locale locale) {
		final Translator trans = Util.createPackageTranslator(UserManager.class, locale);
		final String guestUsername = GUEST_USERNAME_PREFIX + locale.toString();
		Identity guestIdentity = findIdentityByName(guestUsername);
		if (guestIdentity == null) {
			// Create it lazy on demand
			final User guestUser = UserManager.getInstance().createUser(trans.translate("user.guest"), null, null);
			guestUser.getPreferences().setLanguage(locale.toString());
			guestIdentity = createAndPersistIdentityAndUser(guestUsername, guestUser, null, null, null);
			final SecurityGroup anonymousGroup = findSecurityGroupByName(Constants.GROUP_ANONYMOUS);
			addIdentityToSecurityGroup(guestIdentity, anonymousGroup);
			return guestIdentity;
		} else {
			// Check if guest name has been updated in the i18n tool
			if (!guestIdentity.getUser().getProperty(UserConstants.FIRSTNAME, locale).equals(trans.translate("user.guest"))) {
				guestIdentity.getUser().setProperty(UserConstants.FIRSTNAME, trans.translate("user.guest"));
				DBFactory.getInstance().updateObject(guestIdentity);
			}
			return guestIdentity;
		}
	}

	/**
	 * [used by spring]
	 * 
	 * @param orm
	 */
	public void setResourceManager(final OLATResourceManager orm) {
		this.orm = orm;
	}

}