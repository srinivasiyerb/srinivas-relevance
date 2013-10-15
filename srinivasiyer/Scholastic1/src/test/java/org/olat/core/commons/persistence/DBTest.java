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

package org.olat.core.commons.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.olat.core.logging.DBRuntimeException;
import org.olat.core.logging.Tracing;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;
import org.olat.test.OlatTestCase;

/**
 * A <b>DBTest</b> is used to test the persistence package.
 * 
 * @author Andreas Ch. Kapp
 */
public class DBTest extends OlatTestCase {

	/**
	 * testCloseOfUninitializedSession
	 */
	@Test
	public void testCloseOfUninitializedSession() {
		// first get a initialized db
		DB db = DBImpl.getInstance(false);
		// close it
		db.closeSession();
		// then get a uninitialized db
		db = DBImpl.getInstance(false);
		// and close it.
		db.closeSession();

	}

	/**
	 * testErrorHandling
	 */
	@Test
	public void testErrorHandling() {
		final TestTable entry = new TestTable();
		entry.setField1("foo");
		entry.setField2(1234354566776L);
		DBImpl db = DBImpl.getInstance();
		try {
			db.saveObject(entry);
			fail("Should generate an error");
		} catch (final DBRuntimeException dre) {
			assertTrue(db.isError());
			assertNotNull(db.getError());
		}

		db.closeSession();
		// in a transaction
		db = DBImpl.getInstance();
		final TestTable entryTwo = new TestTable();
		entryTwo.setField1("bar");
		entryTwo.setField2(2221234354566776L);
		try {
			db.saveObject(entryTwo);
			db.closeSession();
			fail("Should generate an error");
		} catch (final DBRuntimeException dre) {
			assertTrue(db.isError());
			assertNotNull(db.getError());
		}
	}

	@Test
	public void testRollback() {
		DB db = DBFactory.getInstance();
		final String propertyKey = "testRollback-1";
		final String testValue = "testRollback-1";
		try {
			final PropertyManager pm = PropertyManager.getInstance();
			final Property p1 = pm.createPropertyInstance(null, null, null, null, propertyKey, null, null, testValue, null);
			pm.saveProperty(p1);
			final String propertyKey2 = "testRollback-2";
			final String testValue2 = "testRollback-2";
			// name is null => generated DB error => rollback
			final Property p2 = pm.createPropertyInstance(null, null, null, null, null, null, null, testValue2, null);
			pm.saveProperty(p2);
			fail("Should generate error for rollback.");
		} catch (final Exception ex) {
			db.closeSession();
		}
		// check if p1 is rollbacked
		db = DBFactory.getInstance();
		final PropertyManager pm = PropertyManager.getInstance();
		final Property p = pm.findProperty(null, null, null, null, propertyKey);
		assertNull("Property.save is NOT rollbacked", p);
	}

	@Test
	public void testMixedNonTransactional_Transactional() {
		final DB db = DBFactory.getInstance();
		final String propertyKey1 = "testMixed-1";
		final String testValue1 = "testMixed-1";
		final String propertyKey2 = "testMixed-2";
		final String testValue2 = "testMixed-2";
		final String propertyKey3 = "testMixed-3";
		final String testValue3 = "testMixed-3";
		try {
			// outside of transaction
			final PropertyManager pm = PropertyManager.getInstance();
			final Property p1 = pm.createPropertyInstance(null, null, null, null, propertyKey1, null, null, testValue1, null);
			pm.saveProperty(p1);
			// inside of transaction
			final Property p2 = pm.createPropertyInstance(null, null, null, null, propertyKey2, null, null, testValue2, null);
			pm.saveProperty(p2);
			// name is null => generated DB error => rollback
			final Property p3 = pm.createPropertyInstance(null, null, null, null, null, null, null, testValue3, null);
			pm.saveProperty(p3);
			fail("Should generate error for rollback.");
			db.closeSession();
		} catch (final Exception ex) {
			db.closeSession();
		}
		// check if p1&p2 is rollbacked
		final PropertyManager pm = PropertyManager.getInstance();
		final Property p_1 = pm.findProperty(null, null, null, null, propertyKey1);
		assertNull("Property1 is NOT rollbacked", p_1);
		final Property p_2 = pm.findProperty(null, null, null, null, propertyKey2);
		assertNull("Property2 is NOT rollbacked", p_2);
	}

	@Test
	public void testRollbackNonTransactional() {
		final DB db = DBFactory.getInstance();
		final String propertyKey1 = "testNonTransactional-1";
		final String testValue1 = "testNonTransactional-1";
		final String propertyKey2 = "testNonTransactional-2";
		final String testValue2 = "testNonTransactional-2";
		final String propertyKey3 = "testNonTransactional-3";
		final String testValue3 = "testNonTransactional-3";
		try {
			final PropertyManager pm = PropertyManager.getInstance();
			final Property p1 = pm.createPropertyInstance(null, null, null, null, propertyKey1, null, null, testValue1, null);
			pm.saveProperty(p1);
			final Property p2 = pm.createPropertyInstance(null, null, null, null, propertyKey2, null, null, testValue2, null);
			pm.saveProperty(p2);
			// name is null => generated DB error => rollback ?
			final Property p3 = pm.createPropertyInstance(null, null, null, null, null, null, null, testValue3, null);
			pm.saveProperty(p3);
			fail("Should generate error for rollback.");
			db.closeSession();
		} catch (final Exception ex) {
			db.closeSession();
		}
		// check if p1 & p2 is NOT rollbacked
		final PropertyManager pm = PropertyManager.getInstance();
		final Property p_1 = pm.findProperty(null, null, null, null, propertyKey1);
		assertNull("Property1 is NOT rollbacked", p_1);
		final Property p_2 = pm.findProperty(null, null, null, null, propertyKey2);
		assertNull("Property2 is NOT rollbacked", p_2);
	}

	/**
	 * Test concurrent updating. DbWorker threads updates concurrent db.
	 */
	@Test
	public void testConcurrentUpdate() {
		final int maxWorkers = 5;
		final int loops = 100;
		Tracing.logInfo("start testConcurrentUpdate maxWorkers=" + maxWorkers + "  loops=" + loops, this.getClass());
		final DbWorker[] dbWorkers = new DbWorker[maxWorkers];
		for (int i = 0; i < maxWorkers; i++) {
			dbWorkers[i] = new DbWorker(i, loops);
		}
		boolean allDbWorkerFinished = false;
		while (!allDbWorkerFinished) {
			allDbWorkerFinished = true;
			for (int i = 0; i < maxWorkers; i++) {
				if (!dbWorkers[i].isfinished()) {
					allDbWorkerFinished = false;
				}
			}
			try {
				Thread.currentThread();
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
				Tracing.logWarn("testConcurrentUpdate InterruptedException=" + e, this.getClass());
			}
		}
		for (int i = 0; i < maxWorkers; i++) {
			assertEquals(0, dbWorkers[i].getErrorCounter());
		}
		Tracing.logInfo("finished testConcurrentUpdate ", this.getClass());
	}

	@Test
	public void testDbPerf() {
		final int loops = 1000;
		long timeWithoutTransction = 0;
		Tracing.logInfo("start testDbPerf with loops=" + loops, this.getClass());
		try {
			final long startTime = System.currentTimeMillis();
			for (int loopCounter = 0; loopCounter < loops; loopCounter++) {
				final String propertyKey = "testDbPerfKey-" + loopCounter;
				final DB db = DBFactory.getInstance();
				final PropertyManager pm = PropertyManager.getInstance();
				final String testValue = "testDbPerfValue-" + loopCounter;
				final Property p = pm.createPropertyInstance(null, null, null, null, propertyKey, null, null, testValue, null);
				pm.saveProperty(p);
				// forget session cache etc.
				db.closeSession();
				pm.deleteProperty(p);
			}
			final long endTime = System.currentTimeMillis();
			timeWithoutTransction = endTime - startTime;
			Tracing.logInfo("testDbPerf without transaction takes :" + timeWithoutTransction + "ms", this.getClass());
		} catch (final Exception ex) {
			fail("Exception in testDbPerf without transaction ex=" + ex);
		}

		try {
			final long startTime = System.currentTimeMillis();
			for (int loopCounter = 0; loopCounter < loops; loopCounter++) {
				final String propertyKey = "testDbPerfKey-" + loopCounter;
				DB db = DBFactory.getInstance();
				final PropertyManager pm = PropertyManager.getInstance();
				final String testValue = "testDbPerfValue-" + loopCounter;
				final Property p = pm.createPropertyInstance(null, null, null, null, propertyKey, null, null, testValue, null);
				pm.saveProperty(p);
				// forget session cache etc.
				db.closeSession();
				db = DBFactory.getInstance();
				pm.deleteProperty(p);
			}
			final long endTime = System.currentTimeMillis();
			final long timeWithTransction = endTime - startTime;
			Tracing.logInfo("testDbPerf with transaction takes :" + timeWithTransction + "ms", this.getClass());
			Tracing.logInfo("testDbPerf diff between transaction and without transaction :" + (timeWithTransction - timeWithoutTransction) + "ms", this.getClass());
		} catch (final Exception ex) {
			fail("Exception in testDbPerf with transaction ex=" + ex);
		}
	}

	@Test
	public void testDBUTF8capable() {
		// FIXME:fj: move this test to the db module

		final DB db = DBFactory.getInstance();
		final PropertyManager pm = PropertyManager.getInstance();
		final String unicodetest = "a-greek a\u03E2a\u03EAa\u03E8 arab \u0630a\u0631 chinese:\u3150a\u3151a\u3152a\u3153a\u3173a\u3110-z";
		final Property p = pm.createPropertyInstance(null, null, null, null, "superbluberkey", null, null, unicodetest, null);
		pm.saveProperty(p);
		// forget session cache etc.
		db.closeSession();

		final Property p2 = pm.findProperty(null, null, null, null, "superbluberkey");
		final String lStr = p2.getStringValue();
		assertEquals(unicodetest, lStr);

	}

	@Test
	public void testFindObject() {
		// 1. create a property to have an object
		final Property p = PropertyManager.getInstance().createPropertyInstance(null, null, null, null, "testFindObject", null, null, "testFindObject_Value", null);
		PropertyManager.getInstance().saveProperty(p);
		final long propertyKey = p.getKey();
		// forget session cache etc.
		DBFactory.getInstance().closeSession();
		// 2. try to find object
		Object testObject = DBFactory.getInstance().findObject(Property.class, propertyKey);
		assertNotNull(testObject);
		// 3. Delete object
		PropertyManager.getInstance().deleteProperty((Property) testObject);
		DBFactory.getInstance().closeSession();
		// 4. try again to find object, now no-one should be found, must return null
		testObject = DBFactory.getInstance().findObject(Property.class, propertyKey);
		assertNull(testObject);
	}

	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	@After
	public void tearDown() throws Exception {
		DBFactory.getInstance().closeSession();
	}

}

class DbWorker implements Runnable {

	private Thread workerThread = null;
	private final int numberOfLoops;
	private final String workerId;
	private int errorCounter = 0;
	private boolean isfinished = false;

	public DbWorker(final int id, final int numberOfLoops) {
		this.numberOfLoops = numberOfLoops;
		this.workerId = Integer.toString(id);
		if ((workerThread == null) || !workerThread.isAlive()) {
			Tracing.logInfo("start DbWorker thread id=" + id, this.getClass());
			workerThread = new Thread(this, "TestWorkerThread-" + id);
			workerThread.setPriority(Thread.MAX_PRIORITY);
			workerThread.setDaemon(true);
			workerThread.start();
		}
	}

	@Override
	public void run() {
		int loopCounter = 0;
		try {
			while (loopCounter++ < numberOfLoops) {
				final String propertyKey = "DbWorkerKey-" + workerId + "-" + loopCounter;
				DB db = DBFactory.getInstance();
				final PropertyManager pm = PropertyManager.getInstance();
				final String testValue = "DbWorkerValue-" + workerId + "-" + loopCounter;
				final Property p = pm.createPropertyInstance(null, null, null, null, propertyKey, null, null, testValue, null);
				pm.saveProperty(p);
				// forget session cache etc.
				db.closeSession();

				db = DBFactory.getInstance();
				final Property p2 = pm.findProperty(null, null, null, null, propertyKey);
				final String lStr = p2.getStringValue();
				if (!testValue.equals(lStr)) {
					Tracing.logInfo("Property ERROR testValue=" + testValue + ": lStr=" + lStr, this.getClass());
					errorCounter++;
				}
				db.closeSession();

				Thread.currentThread();
				Thread.sleep(5);
			}
		} catch (final Exception ex) {
			Tracing.logInfo("ERROR workerId=" + workerId + ": Exception=" + ex, this.getClass());
			errorCounter++;
		}
		isfinished = true;
	}

	protected int getErrorCounter() {
		return errorCounter;
	}

	protected boolean isfinished() {
		return isfinished;
	}
}