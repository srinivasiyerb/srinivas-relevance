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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.core.commons.service.tagging;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.services.tagging.manager.SimpleTagProposalManager;
import org.olat.core.commons.services.tagging.manager.TaggingManager;
import org.olat.core.commons.services.tagging.model.Tag;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Description:<br>
 * Test the SimpleTagProposalManager. This is more an integration test as a unit test. This goal is to check if the DB queries are OK. It's not in olatcore because it
 * needs Identity for the TaggingManager and the database.
 * <P>
 * Initial Date: 19 jul. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class SimpleTagProposalManagerTest extends OlatTestCase {

	private static boolean isInitialized = false;
	private static Identity ident;

	@Autowired
	private DB dbInstance;

	@Autowired
	private TaggingManager taggingManager;

	@Autowired
	private SimpleTagProposalManager simpleTagProposalManager;

	@Before
	public void setUp() throws Exception {
		if (isInitialized == false) {
			ident = JunitTestHelper.createAndPersistIdentityAsUser("s-t-p-user");
			dbInstance.commitAndCloseSession();
			isInitialized = true;
		}
	}

	@Test
	public void testManager() {
		assertNotNull(dbInstance);
		assertNotNull(taggingManager);
		assertNotNull(simpleTagProposalManager);
	}

	@Test
	public void testSimpleProposalManager() {
		// create some tags to populate the DB
		final TestOLATResource ores = new TestOLATResource();
		final Tag tag1 = taggingManager.createAndPersistTag(ident, "Hello", ores, null, null);
		final Tag tag2 = taggingManager.createAndPersistTag(ident, "world", ores, null, null);
		final Tag tag3 = taggingManager.createAndPersistTag(ident, "Jupiter", ores, null, null);
		dbInstance.commitAndCloseSession();

		assertNotNull(tag1);
		assertNotNull(tag2);
		assertNotNull(tag3);

		final String text = "Hello world, i'm from Jupiter, especially from Titan. It's a nice world, a little cold, but definitively nice";
		final List<String> proposedTags = simpleTagProposalManager.proposeTagsForInputText(text, true);
		assertNotNull(proposedTags);
		assertTrue(2 < proposedTags.size());
		assertTrue(proposedTags.contains("Hello"));
		assertTrue(proposedTags.contains("world"));
		assertTrue(proposedTags.contains("Jupiter"));
	}

	private class TestOLATResource implements OLATResourceable {

		public TestOLATResource() {
			//
		}

		@Override
		public String getResourceableTypeName() {
			return "simple-proposal";
		}

		@Override
		public Long getResourceableId() {
			return 3650l;
		}
	}

}
