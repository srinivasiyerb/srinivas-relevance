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

package org.olat.course.groupsandrights;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.resource.OresHelper;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManager;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.group.area.BGArea;
import org.olat.group.area.BGAreaManager;
import org.olat.group.area.BGAreaManagerImpl;
import org.olat.group.context.BGContext;
import org.olat.group.context.BGContextManager;
import org.olat.group.context.BGContextManagerImpl;
import org.olat.group.right.BGRightManager;
import org.olat.group.right.BGRightManagerImpl;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;

/**
 * Description:<BR/>
 * Initial Date: Aug 18, 2004
 * 
 * @author gnaegi
 */
public class CourseGroupManagementTest extends OlatTestCase {

	private static Logger log = Logger.getLogger(CourseGroupManagementTest.class.getName());
	private Identity id1, id2, id3;
	private OLATResource course1;

	@Before
	public void setUp() {
		try {
			id1 = JunitTestHelper.createAndPersistIdentityAsUser("one");
			id2 = JunitTestHelper.createAndPersistIdentityAsUser("twoo");
			id3 = JunitTestHelper.createAndPersistIdentityAsUser("three");

			final OLATResourceManager rm = OLATResourceManager.getInstance();
			// create course and persist as OLATResourceImpl
			final OLATResourceable resourceable = OresHelper.createOLATResourceableInstance("junitcourse", System.currentTimeMillis());
			course1 = rm.createOLATResourceInstance(resourceable);
			DBFactory.getInstance().saveObject(course1);

			DBFactory.getInstance().closeSession();
		} catch (final Exception e) {
			log.error("Exception in setUp(): " + e);
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() throws Exception {
		try {
			DBFactory.getInstance().closeSession();
		} catch (final Exception e) {
			log.error("Exception in tearDown(): " + e);
			e.printStackTrace();
			throw e;
		}
	}

	/** rights tests */
	@Test
	public void testHasRightIsInMethods() {
		final BGContextManager cm = BGContextManagerImpl.getInstance();
		final BusinessGroupManager bgm = BusinessGroupManagerImpl.getInstance();
		final BaseSecurity secm = BaseSecurityManager.getInstance();
		final BGRightManager rm = BGRightManagerImpl.getInstance();
		final BGAreaManager am = BGAreaManagerImpl.getInstance();

		// 1) context one: learning groups
		final BGContext c1 = cm.createAndAddBGContextToResource("c1name", course1, BusinessGroup.TYPE_LEARNINGROUP, id1, true);
		// create groups without waitinglist
		final BusinessGroup g1 = bgm.createAndPersistBusinessGroup(BusinessGroup.TYPE_LEARNINGROUP, null, "g1", null, new Integer(0), new Integer(10), false, false, c1);
		final BusinessGroup g2 = bgm.createAndPersistBusinessGroup(BusinessGroup.TYPE_LEARNINGROUP, null, "g2", null, new Integer(0), new Integer(10), false, false, c1);
		// members
		secm.addIdentityToSecurityGroup(id1, g2.getOwnerGroup());
		secm.addIdentityToSecurityGroup(id1, g1.getPartipiciantGroup());
		secm.addIdentityToSecurityGroup(id2, g1.getPartipiciantGroup());
		secm.addIdentityToSecurityGroup(id2, g2.getPartipiciantGroup());
		secm.addIdentityToSecurityGroup(id3, g1.getOwnerGroup());
		// areas
		final BGArea a1 = am.createAndPersistBGAreaIfNotExists("a1", "desca1", c1);
		final BGArea a2 = am.createAndPersistBGAreaIfNotExists("a2", null, c1);
		final BGArea a3 = am.createAndPersistBGAreaIfNotExists("a3", null, c1);
		am.addBGToBGArea(g1, a1);
		am.addBGToBGArea(g2, a1);
		am.addBGToBGArea(g1, a2);
		am.addBGToBGArea(g2, a3);

		// 2) context two: right groups
		final BGContext c2 = cm.createAndAddBGContextToResource("c2name", course1, BusinessGroup.TYPE_RIGHTGROUP, id2, true);
		// groups
		final BusinessGroup g3 = bgm.createAndPersistBusinessGroup(BusinessGroup.TYPE_RIGHTGROUP, null, "g3", null, null, null, null/* enableWaitinglist */,
				null/* enableAutoCloseRanks */, c2);
		final BusinessGroup g4 = bgm.createAndPersistBusinessGroup(BusinessGroup.TYPE_RIGHTGROUP, null, "g4", null, null, null, null/* enableWaitinglist */,
				null/* enableAutoCloseRanks */, c2);
		// members
		secm.addIdentityToSecurityGroup(id1, g3.getPartipiciantGroup());
		secm.addIdentityToSecurityGroup(id1, g4.getPartipiciantGroup());
		secm.addIdentityToSecurityGroup(id3, g4.getPartipiciantGroup());
		// rights
		rm.addBGRight(CourseRights.RIGHT_ARCHIVING, g3);
		rm.addBGRight(CourseRights.RIGHT_COURSEEDITOR, g3);
		rm.addBGRight(CourseRights.RIGHT_ARCHIVING, g4);
		rm.addBGRight(CourseRights.RIGHT_GROUPMANAGEMENT, g4);

		DBFactory.getInstance().closeSession(); // simulate user clicks

		// test groups
		final CourseGroupManager gm = PersistingCourseGroupManager.getInstance(course1);
		assertTrue(gm.isIdentityInLearningGroup(id1, g1.getName()));
		assertTrue(gm.isIdentityInLearningGroup(id1, g2.getName()));
		assertFalse(gm.isIdentityInLearningGroup(id1, g3.getName())); // not a learning group
		assertFalse(gm.isIdentityInLearningGroup(id1, g4.getName())); // not a learning group

		assertTrue(gm.isIdentityInLearningGroup(id2, g1.getName()));
		assertTrue(gm.isIdentityInLearningGroup(id2, g2.getName()));
		assertFalse(gm.isIdentityInLearningGroup(id2, g3.getName())); // not a learning group
		assertFalse(gm.isIdentityInLearningGroup(id2, g4.getName())); // not a learning group

		DBFactory.getInstance().closeSession();
		assertTrue(gm.isIdentityInLearningGroup(id3, g1.getName()));
		assertFalse(gm.isIdentityInLearningGroup(id3, g2.getName()));
		assertFalse(gm.isIdentityInLearningGroup(id3, g3.getName())); // not a learning group
		assertFalse(gm.isIdentityInLearningGroup(id3, g4.getName())); // not a learning group

		assertTrue(gm.isIdentityInLearningGroup(id1, g1.getName(), c1.getName()));
		assertFalse(gm.isIdentityInLearningGroup(id1, g1.getName(), c2.getName()));
		assertTrue(gm.isIdentityInLearningGroup(id3, g1.getName(), c1.getName()));
		assertFalse(gm.isIdentityInLearningGroup(id3, g1.getName(), c2.getName()));

		// test areas
		DBFactory.getInstance().closeSession();
		assertTrue(gm.isIdentityInLearningArea(id1, a1.getName()));
		assertTrue(gm.isIdentityInLearningArea(id1, a2.getName()));
		assertTrue(gm.isIdentityInLearningArea(id1, a3.getName()));

		assertTrue(gm.isIdentityInLearningArea(id2, a1.getName()));
		assertTrue(gm.isIdentityInLearningArea(id2, a2.getName()));
		assertTrue(gm.isIdentityInLearningArea(id2, a3.getName()));

		DBFactory.getInstance().closeSession();
		assertTrue(gm.isIdentityInLearningArea(id3, a1.getName()));
		assertTrue(gm.isIdentityInLearningArea(id3, a2.getName()));
		assertFalse(gm.isIdentityInLearningArea(id3, a3.getName()));

		DBFactory.getInstance().closeSession();
		assertTrue(gm.getLearningAreasOfGroupFromAllContexts(g1.getName()).size() == 2);
		assertTrue(gm.getLearningAreasOfGroupFromAllContexts(g2.getName()).size() == 2);

		// test rights
		DBFactory.getInstance().closeSession();
		assertTrue(gm.hasRight(id1, CourseRights.RIGHT_ARCHIVING));
		assertTrue(gm.hasRight(id1, CourseRights.RIGHT_COURSEEDITOR));
		assertTrue(gm.hasRight(id1, CourseRights.RIGHT_GROUPMANAGEMENT));
		assertFalse(gm.hasRight(id1, CourseRights.RIGHT_ASSESSMENT));
		assertTrue(gm.hasRight(id1, CourseRights.RIGHT_COURSEEDITOR, c2.getName()));
		assertFalse(gm.hasRight(id1, CourseRights.RIGHT_COURSEEDITOR, c1.getName()));
		assertFalse(gm.hasRight(id2, CourseRights.RIGHT_COURSEEDITOR));

		// test context
		DBFactory.getInstance().closeSession();
		assertTrue(gm.isIdentityInGroupContext(id1, c1.getName()));
		assertTrue(gm.isIdentityInGroupContext(id1, c2.getName()));
		assertTrue(gm.isIdentityInGroupContext(id2, c1.getName()));
		assertFalse(gm.isIdentityInGroupContext(id2, c2.getName()));
		assertTrue(gm.isIdentityInGroupContext(id3, c1.getName()));
		assertTrue(gm.isIdentityInGroupContext(id3, c2.getName()));
	}

}