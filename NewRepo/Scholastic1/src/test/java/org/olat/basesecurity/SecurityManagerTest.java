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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.UserConstants;
import org.olat.core.util.resource.OresHelper;
import org.olat.resource.OLATResource;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;

/**
 * SecurityTestSuite is a container of all Tests in this package.
 * 
 * @author Andreas Ch. Kapp
 */
public class SecurityManagerTest extends OlatTestCase {

	private static Logger log = Logger.getLogger(SecurityManagerTest.class.getName());
	private Identity s1, s2, s3, testAdmin;
	private static String testLogin = "test-login";
	private OLATResourceable olatres, olatres2;
	private BaseSecurity sm;

	// Already tested in BusinessGroupTest :
	// - getGroupsWithPermissionOnOlatResourceable
	// - getIdentitiesWithPermissionOnOlatResourceable
	/**
	 * 
	 */
	@Test
	public void testGetIdentitiesByPowerSearch() {
		// test using visibility search
		List userList = sm.getVisibleIdentitiesByPowerSearch(testLogin, null, true, null, null, null, null, null);
		assertEquals(1, userList.size());
		Identity identity = (Identity) userList.get(0);
		assertEquals(testLogin, identity.getName());
		// test using powser search
		userList = sm.getIdentitiesByPowerSearch(testLogin, null, true, null, null, null, null, null, null, null, null);
		assertEquals(1, userList.size());
		identity = (Identity) userList.get(0);
		assertEquals(testLogin, identity.getName());
	}

	@Test
	public void testGetIdentitiesByPowerSearchWithuserProperties() {
		final Map<String, String> userProperties = new HashMap<String, String>();
		userProperties.put(UserConstants.FIRSTNAME, "first" + testLogin);
		userProperties.put(UserConstants.LASTNAME, "last" + testLogin);
		// test using visibility search
		List userList = sm.getVisibleIdentitiesByPowerSearch(testLogin, userProperties, true, null, null, null, null, null);
		assertEquals(1, userList.size());
		Identity identity = (Identity) userList.get(0);
		assertEquals("first" + testLogin, identity.getUser().getProperty(UserConstants.FIRSTNAME, null));
		// test using powser search
		userList = sm.getIdentitiesByPowerSearch(testLogin, userProperties, true, null, null, null, null, null, null, null, null);
		assertEquals(1, userList.size());
		identity = (Identity) userList.get(0);
		assertEquals("first" + testLogin, identity.getUser().getProperty(UserConstants.FIRSTNAME, null));
	}

	@Test
	public void testGetIdentitiesByPowerSearchWithConjunctionFlag() {
		// 1) two fields that match to two different users
		Map<String, String> userProperties = new HashMap<String, String>();
		userProperties.put(UserConstants.FIRSTNAME, s1.getUser().getProperty(UserConstants.FIRSTNAME, null));
		userProperties.put(UserConstants.LASTNAME, s2.getUser().getProperty(UserConstants.LASTNAME, null));
		// with AND search (conjunction) no identity is found
		List userList = sm.getIdentitiesByPowerSearch(null, userProperties, true, null, null, null, null, null, null, null, null);
		assertEquals(0, userList.size());
		// with OR search both identities are found
		userList = sm.getIdentitiesByPowerSearch(null, userProperties, false, null, null, null, null, null, null, null, null);
		assertEquals(2, userList.size());

		// 2) two fields wheras only one matches to one single user
		userProperties = new HashMap<String, String>();
		userProperties.put(UserConstants.FIRSTNAME, s1.getUser().getProperty(UserConstants.FIRSTNAME, null));
		userProperties.put(UserConstants.LASTNAME, "some nonexisting value");
		// with AND search (conjunction) no identity is found
		userList = sm.getIdentitiesByPowerSearch(null, userProperties, true, null, null, null, null, null, null, null, null);
		assertEquals(0, userList.size());
		// with OR search first identity ist found
		userList = sm.getIdentitiesByPowerSearch(null, userProperties, false, null, null, null, null, null, null, null, null);
		assertEquals(1, userList.size());
	}

	@Test
	public void testGetIdentitiesByPowerSearchWithGroups() {
		final SecurityGroup[] groups = { sm.findSecurityGroupByName(Constants.GROUP_OLATUSERS) };
		List userList = sm.getVisibleIdentitiesByPowerSearch(testLogin, null, true, groups, null, null, null, null);
		assertEquals(1, userList.size());
		final Identity identity = (Identity) userList.get(0);
		assertEquals(testLogin, identity.getName());
		final SecurityGroup[] authors = { sm.findSecurityGroupByName(Constants.GROUP_AUTHORS) };
		userList = sm.getVisibleIdentitiesByPowerSearch(testLogin, null, true, authors, null, null, null, null);
		assertEquals(0, userList.size());
	}

	@Test
	public void testGetIdentitiesByPowerSearchWithAuthProviders() {
		// 1) only auth providers and login
		final String[] authProviders = { BaseSecurityModule.getDefaultAuthProviderIdentifier() };
		for (int i = 0; i < authProviders.length; i++) {
			assertTrue("Provider name.length must be <= 8", authProviders[i].length() <= 8);
		}
		List userList = sm.getVisibleIdentitiesByPowerSearch(testLogin, null, true, null, null, authProviders, null, null);
		assertEquals(1, userList.size());
		final Identity identity = (Identity) userList.get(0);
		assertEquals(testLogin, identity.getName());
		final String[] nonAuthProviders = { "NonAuth" };
		for (int i = 0; i < nonAuthProviders.length; i++) {
			assertTrue("Provider name.length must be <= 8", nonAuthProviders[i].length() <= 8);
		}
		userList = sm.getVisibleIdentitiesByPowerSearch(testLogin, null, true, null, null, nonAuthProviders, null, null);
		assertEquals(0, userList.size());

		// 2) two fields wheras only one matches to one single user
		Map<String, String> userProperties = new HashMap<String, String>();
		userProperties.put(UserConstants.FIRSTNAME, s1.getUser().getProperty(UserConstants.FIRSTNAME, null));
		userProperties.put(UserConstants.LASTNAME, "some nonexisting value");
		// with AND search (conjunction) no identity is found
		userList = sm.getIdentitiesByPowerSearch(null, userProperties, true, null, null, authProviders, null, null, null, null, null);
		assertEquals(0, userList.size());
		// with OR search first identity ist found
		userList = sm.getIdentitiesByPowerSearch(null, userProperties, false, null, null, authProviders, null, null, null, null, null);
		assertEquals(1, userList.size());

		// 3) two fields wheras only one matches to one single user
		sm.createAndPersistAuthentication(s1, "mytest_p", s1.getName(), "sdf");
		final String[] myProviders = new String[] { "mytest_p", "non-prov" };
		for (int i = 0; i < myProviders.length; i++) {
			assertTrue("Provider name.length must be <= 8", myProviders[i].length() <= 8);
		}
		userProperties = new HashMap<String, String>();
		userProperties.put(UserConstants.FIRSTNAME, s1.getUser().getProperty(UserConstants.FIRSTNAME, null));
		userProperties.put(UserConstants.LASTNAME, "some nonexisting value");
		// with AND search (conjunction) no identity is found
		userList = sm.getIdentitiesByPowerSearch(null, userProperties, true, null, null, myProviders, null, null, null, null, null);
		assertEquals(0, userList.size());
		// with OR search identity is found via auth provider and via first name
		userList = sm.getIdentitiesByPowerSearch(null, userProperties, false, null, null, myProviders, null, null, null, null, null);
		assertEquals(1, userList.size());
	}

	@Test
	public void testGetPoliciesOfIdentity() {
		final SecurityGroup olatUsersGroup = sm.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		sm.createAndPersistPolicy(olatUsersGroup, Constants.PERMISSION_ACCESS, olatres);
		final List policies = sm.getPoliciesOfIdentity(s1);
		boolean foundPolicy = false;
		for (final Iterator iterator = policies.iterator(); iterator.hasNext();) {
			final Object[] policy = (Object[]) iterator.next();
			for (int i = 0; i < policy.length; i++) {
				final Long resourcableId = ((OLATResource) policy[2]).getResourceableId();
				if ((resourcableId != null) && (resourcableId.equals(olatres.getResourceableId()))) {
					System.out.println("ok, we found the right resource");
					assertEquals(olatUsersGroup.getKey(), ((SecurityGroup) policy[0]).getKey());
					assertEquals(Constants.PERMISSION_ACCESS, ((Policy) policy[1]).getPermission());
					assertEquals(olatres.getResourceableId(), ((OLATResource) policy[2]).getResourceableId());
					foundPolicy = true;
				}
			}
		}
		assertTrue("Does not found policy", foundPolicy);
	}

	@Test
	public void testRemoveIdentityFromSecurityGroup() {
		final SecurityGroup olatUsersGroup = sm.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		assertTrue(sm.isIdentityInSecurityGroup(s1, olatUsersGroup));
		sm.removeIdentityFromSecurityGroup(s1, olatUsersGroup);
		assertFalse(sm.isIdentityInSecurityGroup(s1, olatUsersGroup));
		sm.addIdentityToSecurityGroup(s1, olatUsersGroup);
		assertTrue(sm.isIdentityInSecurityGroup(s1, olatUsersGroup));
	}

	@Test
	public void testGetIdentitiesAndDateOfSecurityGroup() {
		final SecurityGroup olatUsersGroup = sm.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		List identities = sm.getIdentitiesAndDateOfSecurityGroup(olatUsersGroup, false);// not sortedByAddDate
		assertTrue("Found no users", identities.size() > 0);
		final Object[] firstIdentity = (Object[]) identities.get(0);
		assertTrue("Wrong type, Identity[0] must be an Identity", firstIdentity[0] instanceof Identity);
		assertTrue("Wrong type, Identity[1] must be a Date", firstIdentity[1] instanceof Date);

		identities = sm.getIdentitiesAndDateOfSecurityGroup(olatUsersGroup, true);// sortedByAddDate
		assertTrue("Found no users", identities.size() > 0);
		Date addedDateBefore = null;
		for (final Iterator iterator = identities.iterator(); iterator.hasNext();) {
			final Object[] object = (Object[]) iterator.next();
			final Identity identity = (Identity) object[0];
			final Date addedDate = (Date) object[1];
			if (addedDateBefore != null) {
				assertTrue("Not sorted by AddDate ", (addedDate.compareTo(addedDateBefore) == 1) || (addedDate.compareTo(addedDateBefore) == 0));
			}
			addedDateBefore = addedDate;
		}

	}

	@Test
	public void testGetSecurityGroupJoinDateForIdentity() {
		final SecurityGroup secGroup = sm.createAndPersistNamedSecurityGroup("testGroup");
		sm.addIdentityToSecurityGroup(s1, secGroup);
		DBFactory.getInstance().commit();
		try {
			Thread.currentThread();
			// we have to sleep for a short time to have different time
			Thread.sleep(10);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		final Date now = new Date();
		assertTrue(sm.getSecurityGroupJoinDateForIdentity(secGroup, s1).getTime() < now.getTime());
		assertNotNull(sm.getSecurityGroupJoinDateForIdentity(secGroup, s1));
		if (!sm.getSecurityGroupsForIdentity(s2).contains(secGroup)) {
			assertNull(sm.getSecurityGroupJoinDateForIdentity(secGroup, s2));
		}
	}

	@Test
	public void testGetAuthentications() {
		final List authentications = sm.getAuthentications(s1);
		final Authentication authentication = (Authentication) authentications.get(0);
		assertEquals(testLogin, authentication.getAuthusername());
	}

	@Test
	public void testFindAuthenticationByAuthusername() {
		final Authentication authentication = sm.findAuthenticationByAuthusername(testLogin, BaseSecurityModule.getDefaultAuthProviderIdentifier());
		assertEquals(testLogin, authentication.getAuthusername());
	}

	@Test
	public void testCountUniqueUserLoginsSince() {

		// Set lastLogin for 4 test users to now
		final DB db = DBFactory.getInstance();
		final Date now = new Date();
		s1.setLastLogin(now);
		db.updateObject(s1);
		s2.setLastLogin(now);
		db.updateObject(s2);
		s3.setLastLogin(now);
		db.updateObject(s3);
		testAdmin.setLastLogin(now);
		db.updateObject(testAdmin);
		db.closeSession();

		Calendar c1 = Calendar.getInstance();
		c1.add(Calendar.DAY_OF_YEAR, -100);// -100
		assertTrue("Found no user-logins", sm.countUniqueUserLoginsSince(c1.getTime()) >= 4);
		final Long initialUserLogins = sm.countUniqueUserLoginsSince(c1.getTime());

		// Set lastLogin for the 4 test-users
		c1 = Calendar.getInstance();
		c1.add(Calendar.DAY_OF_YEAR, -100);
		s1.setLastLogin(c1.getTime());
		c1 = Calendar.getInstance();
		c1.add(Calendar.DAY_OF_YEAR, -15);
		s2.setLastLogin(c1.getTime());
		s3.setLastLogin(c1.getTime());
		testAdmin.setLastLogin(c1.getTime());

		// Set lastLogin for 4 test users
		db.updateObject(s1);
		db.updateObject(s2);
		db.updateObject(s3);
		db.updateObject(testAdmin);
		db.closeSession();

		Calendar c2 = Calendar.getInstance();
		c2.add(Calendar.DAY_OF_YEAR, -2);
		assertEquals(initialUserLogins - 4, sm.countUniqueUserLoginsSince(c2.getTime()).intValue());
		c2 = Calendar.getInstance();
		c2.add(Calendar.DAY_OF_YEAR, -14);
		assertEquals(initialUserLogins - 4, sm.countUniqueUserLoginsSince(c2.getTime()).intValue());
		c2 = Calendar.getInstance();
		c2.add(Calendar.DAY_OF_YEAR, -16);
		assertEquals(initialUserLogins - 1, sm.countUniqueUserLoginsSince(c2.getTime()).intValue());
		c2 = Calendar.getInstance();
		c2.add(Calendar.DAY_OF_YEAR, -99);
		assertEquals(initialUserLogins - 1, sm.countUniqueUserLoginsSince(c2.getTime()).intValue());
		c2 = Calendar.getInstance();
		c2.add(Calendar.DAY_OF_YEAR, -101);
		assertEquals(initialUserLogins, sm.countUniqueUserLoginsSince(c2.getTime()));
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setup() throws Exception {
		sm = BaseSecurityManager.getInstance();
		s1 = JunitTestHelper.createAndPersistIdentityAsUser(testLogin);
		s2 = JunitTestHelper.createAndPersistIdentityAsUser("coop");
		s3 = JunitTestHelper.createAndPersistIdentityAsAuthor("diesbach");
		testAdmin = JunitTestHelper.createAndPersistIdentityAsAdmin("testAdmin");

		olatres = OresHelper.createOLATResourceableInstance("Kürs", new Long("123"));
		olatres2 = OresHelper.createOLATResourceableInstance("Kürs_2", new Long("124"));
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@After
	public void tearDown() throws Exception {
		try {
			// DB.getInstance().delete("select * from o_bookmark");
			DBFactory.getInstance().closeSession();
		} catch (final Exception e) {
			log.error("tearDown failed: ", e);
		}
	}
}