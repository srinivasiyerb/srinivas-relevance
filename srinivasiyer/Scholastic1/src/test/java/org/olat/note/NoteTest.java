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

package org.olat.note;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.resource.OLATResourceManager;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;

/**
 * Initial Date: Dec 9, 2004
 * 
 * @author Alexander Schneider Comment:
 */
public class NoteTest extends OlatTestCase implements OLATResourceable {

	private final long RESOURCE_ID = 42;
	private final String RESOURCE_TYPE = "org.olat.note.NoteTest";
	private static Logger log = Logger.getLogger(NoteTest.class);
	private static boolean isInitialized = false;
	private static Identity identity = null;
	private static org.olat.resource.OLATResource res = null;
	private static NoteManager nm;

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setup() {
		if (NoteTest.isInitialized == false) {
			try {
				nm = NoteManager.getInstance();
				// identity with null User should be ok for test case
				identity = JunitTestHelper.createAndPersistIdentityAsUser("foo");
				res = OLATResourceManager.getInstance().createOLATResourceInstance(this);
				OLATResourceManager.getInstance().saveOLATResource(res);

				NoteTest.isInitialized = true;
			} catch (final Exception e) {
				log.error("Error while generating database tables or opening hibernate session: " + e);
			}
		}
	}

	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	@After
	public void tearDown() {
		try {
			DBFactory.getInstance().closeSession();
		} catch (final Exception e) {
			log.error("tearDown failed: ", e);
		}
	}

	/**
	 * 
	 *
	 */
	@Test
	public void testGenericLoadDeleteNote() {
		final Long resourceTypeId = res.getResourceableId();
		final String resourceTypeName = res.getResourceableTypeName();
		final Note n = nm.loadNoteOrCreateInRAM(identity, resourceTypeName, resourceTypeId);
		n.setNoteTitle("Notiz Titel");
		n.setNoteText("Notiz Text");
		nm.saveNote(n);

		DBFactory.getInstance().closeSession();

		final Note note = nm.loadNoteOrCreateInRAM(identity, resourceTypeName, resourceTypeId);
		assertNotNull(note);

		nm.deleteNote(note);

		final List notes = nm.listUserNotes(identity);
		assertTrue(notes.size() == 0);

	}

	/**
	 * @see org.olat.core.id.OLATResourceablegetResourceableTypeName()
	 */
	@Override
	public String getResourceableTypeName() {
		return RESOURCE_TYPE;
	}

	/**
	 * @see org.olat.core.id.OLATResourceablegetResourceableId()
	 */
	@Override
	public Long getResourceableId() {
		return new Long(RESOURCE_ID);
	}
}
