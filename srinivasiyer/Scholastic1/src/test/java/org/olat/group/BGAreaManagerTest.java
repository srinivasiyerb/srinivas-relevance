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

package org.olat.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.group.area.BGArea;
import org.olat.group.area.BGAreaManagerImpl;
import org.olat.group.context.BGContext;
import org.olat.group.context.BGContextManager;
import org.olat.group.context.BGContextManagerImpl;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;

/**
 * @author Christian Guretzki
 */
public class BGAreaManagerTest extends OlatTestCase {

	private static Logger log = Logger.getLogger(BGAreaManagerTest.class.getName());
	private Identity id1;
	private BGContext c1, c2;

	@Before
	public void setUp() {
		try {
			id1 = JunitTestHelper.createAndPersistIdentityAsUser("one");

			// create two test context
			final BGContextManager bgcm = BGContextManagerImpl.getInstance();
			c1 = bgcm.createAndPersistBGContext("c1name", "c1desc", BusinessGroup.TYPE_LEARNINGROUP, null, true);
			c2 = bgcm.createAndPersistBGContext("c2name", "c2desc", BusinessGroup.TYPE_LEARNINGROUP, id1, false);

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

	/**
	 * Do in different threads ant check that no exception happens : 1. create BG-Area 5. delete
	 */
	@Test
	public void testSynchronisationCreateBGArea() {

		final int maxLoop = 400; // => 400 x 100ms => 40sec => finished in 50sec
		final String areaName = "BGArea_1";

		final List<Exception> exceptionHolder = Collections.synchronizedList(new ArrayList<Exception>(1));
		final AtomicInteger finfishCount = new AtomicInteger(0);

		final BGArea bgArea = BGAreaManagerImpl.getInstance().findBGArea(areaName, c1);
		assertNull(bgArea);

		startThreadCreateDeleteBGArea(areaName, maxLoop, exceptionHolder, 100, 20, finfishCount);
		startThreadCreateDeleteBGArea(areaName, maxLoop, exceptionHolder, 30, 40, finfishCount);
		startThreadCreateDeleteBGArea(areaName, maxLoop, exceptionHolder, 15, 20, finfishCount);

		// sleep until t1 and t2 should have terminated/excepted
		sleep(50000);

		// if not -> they are in deadlock and the db did not detect it
		for (final Exception exception : exceptionHolder) {
			System.err.println("exception: " + exception.getMessage());
			exception.printStackTrace();
		}
		assertTrue("Exceptions #" + exceptionHolder.size(), exceptionHolder.size() == 0);
		assertEquals("Not all threads has finished", 3, finfishCount.intValue());
	}

	/**
	 * thread 1 : try to create - sleep - delete sleep
	 * 
	 * @param areaName
	 * @param maxLoop
	 * @param exceptionHolder
	 * @param sleepAfterCreate
	 * @param sleepAfterDelete
	 */
	private void startThreadCreateDeleteBGArea(final String areaName, final int maxLoop, final List<Exception> exceptionHolder, final int sleepAfterCreate,
			final int sleepAfterDelete, final AtomicInteger finishedCount) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < maxLoop; i++) {
					try {
						final BGArea bgArea = BGAreaManagerImpl.getInstance().createAndPersistBGAreaIfNotExists(areaName, "description:" + areaName, c1);
						if (bgArea != null) {
							DBFactory.getInstance().closeSession();
							// created a new bg area
							sleep(sleepAfterCreate);
							BGAreaManagerImpl.getInstance().deleteBGArea(bgArea);
						}
					} catch (final Exception e) {
						exceptionHolder.add(e);
					} finally {
						try {
							DBFactory.getInstance().closeSession();
						} catch (final Exception e) {
							// ignore
						}
						;
					}
					sleep(sleepAfterDelete);
				}
				finishedCount.getAndIncrement();
			}
		}).start();
	}

	/**
	 * Do in different threads ant check that no exception happens : 1. create BG-Area 5. delete
	 */
	@Test
	public void testSynchronisationUpdateBGArea() {

		final int maxLoop = 400; // => 400 x 100ms => 40sec => finished in 50sec
		final String areaName = "BGArea_2";

		final List<Exception> exceptionHolder = Collections.synchronizedList(new ArrayList<Exception>(1));
		final AtomicInteger finfishCount = new AtomicInteger(0);

		BGArea bgArea = BGAreaManagerImpl.getInstance().findBGArea(areaName, c1);
		assertNull(bgArea);
		bgArea = BGAreaManagerImpl.getInstance().createAndPersistBGAreaIfNotExists(areaName, "description:" + areaName, c1);
		assertNotNull(bgArea);

		startThreadUpdateBGArea(areaName, maxLoop, exceptionHolder, 20, finfishCount);
		startThreadUpdateBGArea(areaName, maxLoop, exceptionHolder, 40, finfishCount);
		startThreadUpdateBGArea(areaName, maxLoop, exceptionHolder, 15, finfishCount);

		// sleep until t1 and t2 should have terminated/excepted
		sleep(50000);

		// if not -> they are in deadlock and the db did not detect it
		for (final Exception exception : exceptionHolder) {
			System.err.println("exception: " + exception.getMessage());
			exception.printStackTrace();
		}
		assertTrue("Exceptions #" + exceptionHolder.size(), exceptionHolder.size() == 0);
		assertEquals("Not all threads has finished", 3, finfishCount.intValue());
	}

	private void startThreadUpdateBGArea(final String areaName, final int maxLoop, final List<Exception> exceptionHolder, final int sleepTime,
			final AtomicInteger finishedCount) {
		// thread 2 : update,copy
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < maxLoop; i++) {
					try {
						final BGArea bgArea = BGAreaManagerImpl.getInstance().findBGArea(areaName, c1);
						DBFactory.getInstance().closeSession();// Detached the bg-area object with closing session
						if (bgArea != null) {
							bgArea.setDescription("description:" + areaName + i);
							BGAreaManagerImpl.getInstance().updateBGArea(bgArea);
						}
					} catch (final Exception e) {
						exceptionHolder.add(e);
					} finally {
						try {
							DBFactory.getInstance().closeSession();
						} catch (final Exception e) {
							// ignore
						}
						;
					}
					sleep(sleepTime);
				}
				finishedCount.getAndIncrement();
			}
		}).start();
	}

	/**
	 * @param millis the duration in milliseconds to sleep
	 */
	private void sleep(final int millis) {
		try {
			Thread.sleep(millis);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

}