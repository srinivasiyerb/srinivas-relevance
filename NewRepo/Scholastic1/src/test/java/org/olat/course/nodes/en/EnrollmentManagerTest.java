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

package org.olat.course.nodes.en;

// um click emulieren:
/*
 * 1) generiere Persistentes Object 2) -> DB...evict() entferne Instanz aus HibernateSession 3) aktionen testen, z.b. update failed, falls object nicht in session
 */
// DB.getInstance().evict();
// DB.getInstance().loadObject(); püft ob schon in hibernate session.
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.WindowBackOffice;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.info.WindowControlInfo;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.util.Util;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.nodes.ENCourseNode;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManager;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.group.context.BGContext;
import org.olat.group.context.BGContextManager;
import org.olat.group.context.BGContextManagerImpl;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;

/**
 * Description: <BR/>
 * TODO: Class Description for BusinessGroupManagerImplTest
 * <P/>
 * Initial Date: Jul 28, 2004
 * 
 * @author patrick
 */

public class EnrollmentManagerTest extends OlatTestCase implements WindowControl {
	//
	private static Logger log = Logger.getLogger(EnrollmentManagerTest.class.getName());
	/*
	 * ::Test Setup::
	 */
	private static Identity id1 = null;
	// For WaitingGroup tests
	private static Identity wg1 = null;
	private static Identity wg2 = null;
	private static Identity wg3 = null;

	// For WaitingGroup tests
	private static Translator testTranslator = null;
	private static BusinessGroup bgWithWaitingList = null;

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setup() throws Exception {
		// Identities
		id1 = JunitTestHelper.createAndPersistIdentityAsUser("id1");
		DBFactory.getInstance().closeSession();
		final BusinessGroupManager bgManager = BusinessGroupManagerImpl.getInstance();
		// create business-group with waiting-list
		final String bgWithWaitingListName = "Group with WaitingList";
		final String bgWithWaitingListDesc = "some short description for Group with WaitingList";
		final Boolean enableWaitinglist = new Boolean(true);
		final Boolean enableAutoCloseRanks = new Boolean(true);
		final BGContextManager bgcm = BGContextManagerImpl.getInstance();
		final BGContext groupContext = bgcm.createAndPersistBGContext("c1name", "c1desc", BusinessGroup.TYPE_LEARNINGROUP, null, true);
		System.out.println("testAddToWaitingListAndFireEvent: groupContext=" + groupContext);
		bgWithWaitingList = bgManager.createAndPersistBusinessGroup(BusinessGroup.TYPE_LEARNINGROUP, id1, bgWithWaitingListName, bgWithWaitingListDesc, null, null,
				enableWaitinglist, enableAutoCloseRanks, groupContext);
		bgWithWaitingList.setMaxParticipants(new Integer(2));
		System.out.println("TEST bgWithWaitingList=" + bgWithWaitingList);
		System.out.println("TEST bgWithWaitingList.getMaxParticipants()=" + bgWithWaitingList.getMaxParticipants());
		System.out.println("TEST bgWithWaitingList.getWaitingListEnabled()=" + bgWithWaitingList.getWaitingListEnabled());
		// create mock objects
		final String PACKAGE = Util.getPackageName(EnrollmentManagerTest.class);
		testTranslator = new PackageTranslator(PACKAGE, new Locale("de"));
		// Identities
		wg1 = JunitTestHelper.createAndPersistIdentityAsUser("wg1");
		wg2 = JunitTestHelper.createAndPersistIdentityAsUser("wg2");
		wg3 = JunitTestHelper.createAndPersistIdentityAsUser("wg3");
		DBFactory.getInstance().closeSession();

	}

	// Test for WaitingList
	// /////////////////////
	/**
	 * Enroll 3 idenities (group with max-size=2 and waiting-list). Cancel enrollment. Check size after each step.
	 */
	@Test
	public void testEnroll() throws Exception {
		System.out.println("testEnroll: start...");
		final EnrollmentManager enrollmentManager = EnrollmentManager.getInstance();
		final ENCourseNode enNode = new ENCourseNode();
		final OLATResourceable ores = OresHelper.createOLATResourceableTypeWithoutCheck("TestCourse");
		final CourseEnvironment cenv = CourseFactory.createEmptyCourse(ores, "Test", "Test", "learningObjectives").getCourseEnvironment();
		// 1. enroll wg1 user
		IdentityEnvironment ienv = new IdentityEnvironment();
		ienv.setIdentity(wg1);
		UserCourseEnvironment userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		CoursePropertyManager coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		System.out.println("enrollmentManager=" + enrollmentManager);
		System.out.println("bgWithWaitingList=" + bgWithWaitingList);
		assertTrue("bgWithWaitingList is null", bgWithWaitingList != null);
		System.out.println("userCourseEnv=" + userCourseEnv);
		System.out.println("userCourseEnv.getCourseEnvironment()=" + userCourseEnv.getCourseEnvironment());
		enrollmentManager.doEnroll(wg1, bgWithWaitingList, enNode, coursePropertyManager, this /* WindowControl mock */, testTranslator,
				new ArrayList()/* enrollableGroupNames */, new ArrayList()/* enrollableAreaNames */, userCourseEnv.getCourseEnvironment().getCourseGroupManager());
		assertTrue("Enrollment failed, user='wg1'", BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg1, bgWithWaitingList));
		int participantsCounter = BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(bgWithWaitingList.getPartipiciantGroup());
		assertTrue("Wrong number of participants," + participantsCounter, participantsCounter == 1);
		// 2. enroll wg2 user
		ienv = new IdentityEnvironment();
		ienv.setIdentity(wg2);
		userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		enrollmentManager.doEnroll(wg2, bgWithWaitingList, enNode, coursePropertyManager, this /* WindowControl mock */, testTranslator,
				new ArrayList()/* enrollableGroupNames */, new ArrayList()/* enrollableAreaNames */, userCourseEnv.getCourseEnvironment().getCourseGroupManager());
		assertTrue("Enrollment failed, user='wg2'", BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg2, bgWithWaitingList));
		assertTrue("Enrollment failed, user='wg1'", BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg1, bgWithWaitingList));
		participantsCounter = BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(bgWithWaitingList.getPartipiciantGroup());
		assertTrue("Wrong number of participants," + participantsCounter, participantsCounter == 2);
		// 3. enroll wg3 user => list is full => waiting-list
		ienv = new IdentityEnvironment();
		ienv.setIdentity(wg3);
		userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		enrollmentManager.doEnroll(wg3, bgWithWaitingList, enNode, coursePropertyManager, this /* WindowControl mock */, testTranslator,
				new ArrayList()/* enrollableGroupNames */, new ArrayList()/* enrollableAreaNames */, userCourseEnv.getCourseEnvironment().getCourseGroupManager());
		assertFalse("Wrong enrollment, user='wg3' is in PartipiciantGroup, must be on waiting-list",
				BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg3, bgWithWaitingList));
		assertFalse("Wrong enrollment, user='wg3' is in PartipiciantGroup, must be on waiting-list",
				BaseSecurityManager.getInstance().isIdentityInSecurityGroup(wg3, bgWithWaitingList.getPartipiciantGroup()));
		assertTrue("Wrong enrollment, user='wg3' must be on waiting-list",
				BaseSecurityManager.getInstance().isIdentityInSecurityGroup(wg3, bgWithWaitingList.getWaitingGroup()));
		assertTrue("Enrollment failed, user='wg2'", BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg2, bgWithWaitingList));
		assertTrue("Enrollment failed, user='wg1'", BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg1, bgWithWaitingList));
		participantsCounter = BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(bgWithWaitingList.getPartipiciantGroup());
		assertTrue("Wrong number of participants," + participantsCounter, participantsCounter == 2);
		int waitingListCounter = BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(bgWithWaitingList.getWaitingGroup());
		assertTrue("Wrong number of waiting-list, must be 1, is " + waitingListCounter, waitingListCounter == 1);
		// cancel enrollment for wg2 => transfer wg3 from waiting-list to participants
		ienv = new IdentityEnvironment();
		ienv.setIdentity(wg2);
		userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		enrollmentManager.doCancelEnrollment(wg2, bgWithWaitingList, enNode, coursePropertyManager, this /* WindowControl mock */, testTranslator);
		assertFalse("Cancel enrollment failed, user='wg2' is still participants.",
				BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg2, bgWithWaitingList));
		assertTrue("Enrollment failed, user='wg3'", BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg3, bgWithWaitingList));
		assertTrue("Enrollment failed, user='wg1'", BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg1, bgWithWaitingList));
		participantsCounter = BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(bgWithWaitingList.getPartipiciantGroup());
		assertTrue("Wrong number of participants, must be 2, is " + participantsCounter, participantsCounter == 2);
		waitingListCounter = BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(bgWithWaitingList.getWaitingGroup());
		assertTrue("Wrong number of waiting-list, must be 0, is " + waitingListCounter, waitingListCounter == 0);
		// cancel enrollment for wg1
		ienv = new IdentityEnvironment();
		ienv.setIdentity(wg1);
		userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		enrollmentManager.doCancelEnrollment(wg1, bgWithWaitingList, enNode, coursePropertyManager, this /* WindowControl mock */, testTranslator);
		assertFalse("Cancel enrollment failed, user='wg2' is still participants.",
				BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg2, bgWithWaitingList));
		assertFalse("Cancel enrollment failed, user='wg1' is still participants.",
				BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg1, bgWithWaitingList));
		assertTrue("Enrollment failed, user='wg3'", BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg3, bgWithWaitingList));
		participantsCounter = BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(bgWithWaitingList.getPartipiciantGroup());
		assertTrue("Wrong number of participants, must be 1, is " + participantsCounter, participantsCounter == 1);
		waitingListCounter = BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(bgWithWaitingList.getWaitingGroup());
		assertTrue("Wrong number of waiting-list, must be 0, is " + waitingListCounter, waitingListCounter == 0);
		// cancel enrollment for wg3
		ienv = new IdentityEnvironment();
		ienv.setIdentity(wg3);
		userCourseEnv = new UserCourseEnvironmentImpl(ienv, cenv);
		coursePropertyManager = userCourseEnv.getCourseEnvironment().getCoursePropertyManager();
		enrollmentManager.doCancelEnrollment(wg3, bgWithWaitingList, enNode, coursePropertyManager, this /* WindowControl mock */, testTranslator);
		assertFalse("Cancel enrollment failed, user='wg3' is still participants.",
				BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg3, bgWithWaitingList));
		assertFalse("Cancel enrollment failed, user='wg2' is still participants.",
				BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg2, bgWithWaitingList));
		assertFalse("Cancel enrollment failed, user='wg1' is still participants.",
				BusinessGroupManagerImpl.getInstance().isIdentityInBusinessGroup(wg1, bgWithWaitingList));
		participantsCounter = BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(bgWithWaitingList.getPartipiciantGroup());
		assertTrue("Wrong number of participants, must be 0, is " + participantsCounter, participantsCounter == 0);
		waitingListCounter = BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(bgWithWaitingList.getWaitingGroup());
		assertTrue("Wrong number of waiting-list, must be 0, is " + waitingListCounter, waitingListCounter == 0);

		System.out.println("testEnroll: done...");
	}

	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	@After
	public void tearDown() throws Exception {
		try {
			DBFactory.getInstance().closeSession();
		} catch (final Exception e) {
			log.error("tearDown failed: ", e);
		}
	}

	// Implements interface WindowControl
	// ///////////////////////////////////
	@Override
	public void pushToMainArea(final Component comp) {};

	@Override
	public void pushAsModalDialog(final Component comp) {};

	@Override
	public void pop() {};

	@Override
	public void setInfo(final String string) {};

	@Override
	public void setError(final String string) {};

	@Override
	public void setWarning(final String string) {};

	public DTabs getDTabs() {
		return null;
	};

	@Override
	public WindowControlInfo getWindowControlInfo() {
		return null;
	};

	@Override
	public void makeFlat() {};

	@Override
	public BusinessControl getBusinessControl() {
		return null;
	}

	@Override
	public WindowBackOffice getWindowBackOffice() {
		// TODO Auto-generated method stub
		return null;
	};

}
