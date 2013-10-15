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

package org.olat.portfolio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.core.id.Persistable;
import org.olat.core.id.Roles;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.manager.EPStructureManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.structel.EPStructureElement;
import org.olat.portfolio.model.structel.EPStructureToStructureLink;
import org.olat.portfolio.model.structel.EPStructuredMap;
import org.olat.portfolio.model.structel.EPTargetResource;
import org.olat.portfolio.model.structel.PortfolioStructure;
import org.olat.portfolio.model.structel.PortfolioStructureMap;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Description:<br>
 * Integration test for the DB
 * <P>
 * Initial Date: 24 juin 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
public class EPStructureManagerTest extends OlatTestCase {

	@Autowired
	private DB dbInstance;

	@Autowired
	private EPStructureManager epStructureManager;

	@Autowired
	private EPFrontendManager epFrontendManager;

	@Autowired
	private RepositoryManager repositoryManager;

	@Autowired
	private OLATResourceManager resourceManager;

	@Autowired
	private BaseSecurity securityManager;

	private static Identity ident1, ident2;
	private static boolean isInitialized = false;

	@Before
	public void setUp() {
		if (!isInitialized) {
			ident1 = JunitTestHelper.createAndPersistIdentityAsUser(UUID.randomUUID().toString());
			ident2 = JunitTestHelper.createAndPersistIdentityAsUser(UUID.randomUUID().toString());
		}
	}

	@After
	public void tearDown() {
		dbInstance.commitAndCloseSession();
	}

	@Test
	public void testManagers() {
		assertNotNull(dbInstance);
		assertNotNull(epStructureManager);
	}

	@Test
	public void testGetStructureElementsForUser() {
		final PortfolioStructure el = epFrontendManager.createAndPersistPortfolioDefaultMap(ident1, "users-test-map", "a-map-to-test-get-afterwards");
		assertNotNull(el);
		dbInstance.commitAndCloseSession();

		final List<SecurityGroup> secGroups = securityManager.getSecurityGroupsForIdentity(ident1);
		assertNotNull(secGroups);
		assertTrue(secGroups.size() >= 1);

		final List<PortfolioStructure> elRes = epFrontendManager.getStructureElementsForUser(ident1);
		assertNotNull(elRes);
		assertTrue(elRes.size() == 1);
		assertEquals(((EPStructureElement) elRes.get(0)).getTitle(), "users-test-map");

		// get another map
		final PortfolioStructure el2 = epFrontendManager.createAndPersistPortfolioDefaultMap(ident1, "users-test-map-2", "2-a-map-to-test-get-afterwards");
		assertNotNull(el2);
		dbInstance.commitAndCloseSession();
		final List<PortfolioStructure> elRes2 = epFrontendManager.getStructureElementsForUser(ident1);
		assertNotNull(elRes2);
		assertTrue(elRes2.size() == 2);
	}

	@Test
	public void testGetReferencedMapsForArtefact() {
		PortfolioStructure el = epFrontendManager.createAndPersistPortfolioStructureElement(null, "structure-el", "structure-element");
		dbInstance.commitAndCloseSession();

		final AbstractArtefact artefact = epFrontendManager.createAndPersistArtefact(ident1, "Forum");
		epFrontendManager.addArtefactToStructure(ident1, artefact, el);
		dbInstance.commitAndCloseSession();

		// get the referenced maps
		final List<PortfolioStructure> mapList = epFrontendManager.getReferencedMapsForArtefact(artefact);
		assertTrue(((Persistable) el).equalsByPersistableKey((Persistable) mapList.get(0)));
		dbInstance.commitAndCloseSession();

		// make the test more complex
		// reload the structure element
		el = epFrontendManager.loadPortfolioStructureByKey(el.getKey());
		// add artefact to substructure (page) and check for the same map
		final PortfolioStructure childEl = epFrontendManager.createAndPersistPortfolioStructureElement(el, "child-structure-el", "child-structure-element");
		el = epFrontendManager.removeArtefactFromStructure(artefact, el);

		epFrontendManager.addArtefactToStructure(ident1, artefact, childEl);
		dbInstance.commitAndCloseSession();

		// get the referenced maps
		final List<PortfolioStructure> mapList2 = epFrontendManager.getReferencedMapsForArtefact(artefact);
		assertTrue(((Persistable) el).equalsByPersistableKey((Persistable) mapList2.get(0)));
		dbInstance.commitAndCloseSession();

		// add artefact to 3 maps and check to get all of them
		final PortfolioStructure el2 = epFrontendManager.createAndPersistPortfolioStructureElement(null, "structure-el-2", "structure-element-2");
		epFrontendManager.addArtefactToStructure(ident1, artefact, el2);

		final PortfolioStructure el3 = epFrontendManager.createAndPersistPortfolioStructureElement(null, "structure-el-3", "structure-element-3");
		epFrontendManager.addArtefactToStructure(ident1, artefact, el3);

		final List<PortfolioStructure> mapList3 = epFrontendManager.getReferencedMapsForArtefact(artefact);
		assertEquals(3, mapList3.size());
		boolean found = false;
		for (final PortfolioStructure mapValue : mapList3) {
			if (((Persistable) mapValue).equalsByPersistableKey((Persistable) el)) {
				found = true;
			}
		}
		assertTrue(found);

	}

	@Test
	public void testCreateAndSaveElement() {
		final PortfolioStructure el = epFrontendManager.createAndPersistPortfolioStructureElement(null, "structure-el", "structure-element");
		dbInstance.commitAndCloseSession();

		assertNotNull(el);
		assertNotNull(el.getOlatResource());

		final PortfolioStructure retrievedEl = epFrontendManager.loadPortfolioStructureByKey(el.getKey());
		assertNotNull(retrievedEl);
		assertNotNull(retrievedEl.getOlatResource());

		final OLATResource resource = resourceManager.findResourceable(el.getResourceableId(), el.getResourceableTypeName());
		assertNotNull(resource);
	}

	@Test
	public void testCreateAndSaveTreeOfElements() {
		// test save parent and child
		final PortfolioStructure parentEl = epFrontendManager.createAndPersistPortfolioStructureElement(null, "parent-structure-el", "parent-structure-element");
		final PortfolioStructure childEl = epFrontendManager.createAndPersistPortfolioStructureElement(parentEl, "child-structure-el", "child-structure-element");
		dbInstance.commitAndCloseSession();

		// test load by key
		final PortfolioStructure retrievedParentEl = epFrontendManager.loadPortfolioStructureByKey(parentEl.getKey());
		assertNotNull(retrievedParentEl);
		assertNotNull(retrievedParentEl.getOlatResource());

		// test load by key
		final PortfolioStructure retrievedChildEl = epFrontendManager.loadPortfolioStructureByKey(childEl.getKey());
		final PortfolioStructure retrievedParentEl2 = epFrontendManager.loadStructureParent(retrievedChildEl);
		assertNotNull(retrievedChildEl);
		assertNotNull(retrievedChildEl.getOlatResource());
		assertNotNull(retrievedParentEl2);
		assertEquals(parentEl.getKey(), retrievedParentEl2.getKey());
		dbInstance.commitAndCloseSession();

		// test get children
		final List<PortfolioStructure> retrievedChilrenEl = epFrontendManager.loadStructureChildren(parentEl);
		assertNotNull(retrievedChilrenEl);
		assertEquals(1, retrievedChilrenEl.size());
		assertEquals(childEl.getKey(), retrievedChilrenEl.get(0).getKey());
		assertNotNull(((EPStructureElement) retrievedChilrenEl.get(0)).getRoot());
		assertEquals(parentEl.getKey(), ((EPStructureElement) retrievedChilrenEl.get(0)).getRoot().getKey());
	}

	@Test
	public void testCreateAndRetrieveElement() {
		final PortfolioStructure el = epFrontendManager.createAndPersistPortfolioStructureElement(null, "structure-el-2", "structure-element-2");
		dbInstance.commitAndCloseSession();

		final PortfolioStructure el2 = epStructureManager.loadPortfolioStructure(el.getOlatResource());
		assertNotNull(el2);
	}

	@Test
	public void testCreateAndRetrieveCollectRestrictionElement() {
		final PortfolioStructure el = epFrontendManager.createAndPersistPortfolioStructureElement(null, "structure-el-3", "structure-element-3");
		epStructureManager.addCollectRestriction(el, "Forum", "minimum", 3);
		epStructureManager.savePortfolioStructure(el);
		dbInstance.commitAndCloseSession();

		final PortfolioStructure retrievedEl = epStructureManager.loadPortfolioStructure(el.getOlatResource());
		assertNotNull(retrievedEl);
		assertTrue(retrievedEl instanceof EPStructureElement);
		final EPStructureElement retrievedStructEl = (EPStructureElement) retrievedEl;
		assertNotNull(retrievedStructEl.getCollectRestrictions());
		assertEquals("Forum", retrievedStructEl.getCollectRestrictions().get(0).getArtefactType());
		assertEquals("minimum", retrievedStructEl.getCollectRestrictions().get(0).getRestriction());
		assertEquals(3, retrievedStructEl.getCollectRestrictions().get(0).getAmount());
	}

	@Test
	public void testChildrenBetweenSeveralSessions() {
		// test save parent and child
		final PortfolioStructure parentEl = epFrontendManager.createAndPersistPortfolioStructureElement(null, "parent-structure-el", "parent-structure-element");
		final PortfolioStructure childEl1 = epFrontendManager.createAndPersistPortfolioStructureElement(parentEl, "multi-session-structure-el-1",
				"child-structure-element");
		dbInstance.commitAndCloseSession();

		final PortfolioStructure childEl2 = epFrontendManager.createAndPersistPortfolioStructureElement(parentEl, "multi-session-structure-el-2",
				"child-structure-element");
		dbInstance.commitAndCloseSession();

		final PortfolioStructure childEl3 = epFrontendManager.createAndPersistPortfolioStructureElement(parentEl, "multi-session-structure-el-3",
				"child-structure-element");
		((EPStructureElement) parentEl).setTitle("parent-structure-el-prime");
		epStructureManager.savePortfolioStructure(parentEl);
		dbInstance.commitAndCloseSession();

		// test if all children are saved
		final List<PortfolioStructure> retrievedChilrenEl = epFrontendManager.loadStructureChildren(parentEl);
		assertNotNull(retrievedChilrenEl);
		assertEquals(3, retrievedChilrenEl.size());
		// test if they are ordered
		assertEquals(childEl1.getKey(), retrievedChilrenEl.get(0).getKey());
		assertEquals(childEl2.getKey(), retrievedChilrenEl.get(1).getKey());
		assertEquals(childEl3.getKey(), retrievedChilrenEl.get(2).getKey());
		// test the title too (why not?)
		assertEquals("multi-session-structure-el-1", ((EPStructureElement) retrievedChilrenEl.get(0)).getTitle());
		assertEquals("multi-session-structure-el-2", ((EPStructureElement) retrievedChilrenEl.get(1)).getTitle());
		assertEquals("multi-session-structure-el-3", ((EPStructureElement) retrievedChilrenEl.get(2)).getTitle());

		// test if the change to the parent was not lost
		final PortfolioStructure retrievedParentEl = epFrontendManager.loadPortfolioStructureByKey(parentEl.getKey());
		assertEquals("parent-structure-el-prime", ((EPStructureElement) retrievedParentEl).getTitle());
		dbInstance.commitAndCloseSession();

		// test that the children are not always loaded
		final PortfolioStructure retrievedParentEl2 = epFrontendManager.loadPortfolioStructureByKey(parentEl.getKey());
		dbInstance.commitAndCloseSession();

		boolean failedToLazyLoadChildren;
		try {
			final List<EPStructureToStructureLink> children = ((EPStructureElement) retrievedParentEl2).getInternalChildren();
			failedToLazyLoadChildren = (children == null || children.isEmpty());
		} catch (final Exception e) {
			failedToLazyLoadChildren = true;
		}
		assertTrue(failedToLazyLoadChildren);
		dbInstance.commitAndCloseSession();

		// test load parent
		final PortfolioStructure retrievedParentEl3 = epFrontendManager.loadStructureParent(childEl1);
		assertNotNull(retrievedParentEl3);
		assertEquals(parentEl.getKey(), retrievedParentEl3.getKey());
		final PortfolioStructure retrievedParentEl4 = epFrontendManager.loadStructureParent(childEl2);
		assertNotNull(retrievedParentEl4);
		assertEquals(parentEl.getKey(), retrievedParentEl4.getKey());
		final PortfolioStructure retrievedParentEl5 = epFrontendManager.loadStructureParent(childEl3);
		assertNotNull(retrievedParentEl5);
		assertEquals(parentEl.getKey(), retrievedParentEl5.getKey());
	}

	@Test
	public void testDeleteChildren() {
		// test save parent and 3 children
		final PortfolioStructure parentEl = epFrontendManager.createAndPersistPortfolioStructureElement(null, "remove-parent-structure-el", "parent-structure-element");
		final PortfolioStructure childEl1 = epFrontendManager.createAndPersistPortfolioStructureElement(parentEl, "remove-structure-el-1",
				"remove-child-structure-element");
		final PortfolioStructure childEl2 = epFrontendManager.createAndPersistPortfolioStructureElement(parentEl, "remove-structure-el-2",
				"remove-child-structure-element");
		final PortfolioStructure childEl3 = epFrontendManager.createAndPersistPortfolioStructureElement(parentEl, "remove-structure-el-3",
				"remove-child-structure-element");
		dbInstance.commitAndCloseSession();

		// remove a child
		epStructureManager.removeStructure(parentEl, childEl2);
		dbInstance.commitAndCloseSession();

		// check if the structure element has been removed
		final List<PortfolioStructure> retrievedChildrenEl = epFrontendManager.loadStructureChildren(parentEl);
		assertNotNull(retrievedChildrenEl);
		assertEquals(2, retrievedChildrenEl.size());
		assertEquals(childEl1.getKey(), retrievedChildrenEl.get(0).getKey());
		assertEquals(childEl3.getKey(), retrievedChildrenEl.get(1).getKey());
	}

	@Test
	public void testChildrenPaging() {
		// save parent and 20 children
		final PortfolioStructure parentEl = epFrontendManager.createAndPersistPortfolioStructureElement(null, "paged-parent-structure-el", "parent-structure-element");

		final List<PortfolioStructure> children = new ArrayList<PortfolioStructure>();
		for (int i = 0; i < 20; i++) {
			final PortfolioStructure childEl = epFrontendManager.createAndPersistPortfolioStructureElement(parentEl, "paged-structure-el-" + i,
					"paged-child-structure-element");
			children.add(childEl);
		}
		dbInstance.commitAndCloseSession();

		// check if the paging return the right children
		final List<PortfolioStructure> childrenSubset = epFrontendManager.loadStructureChildren(parentEl, 15, 10);
		assertNotNull(childrenSubset);
		assertEquals(5, childrenSubset.size());
		assertEquals(children.get(15).getKey(), childrenSubset.get(0).getKey());
		assertEquals(children.get(16).getKey(), childrenSubset.get(1).getKey());
		assertEquals(children.get(17).getKey(), childrenSubset.get(2).getKey());
		assertEquals(children.get(18).getKey(), childrenSubset.get(3).getKey());
		assertEquals(children.get(19).getKey(), childrenSubset.get(4).getKey());
	}

	@Test
	public void testCreateStructureMapTemplate() {
		// save parent and 20 children
		final PortfolioStructureMap template = epStructureManager.createPortfolioMapTemplate(ident1, "paged-parent-structure-el", "parent-structure-element");
		epStructureManager.savePortfolioStructure(template);
		dbInstance.commitAndCloseSession();

		// not very usefull but...
		assertNotNull(template);
		// check if the olat resource is persisted
		final OLATResource resource = resourceManager.findResourceable(template.getResourceableId(), template.getResourceableTypeName());
		assertNotNull(resource);
		// check if the repository entry is persisted
		final RepositoryEntry re = repositoryManager.lookupRepositoryEntry(resource, false);
		assertNotNull(re);
	}

	@Test
	public void testUseStructureMapTemplate() {
		// save parent and 20 children
		final PortfolioStructureMap template = epStructureManager.createPortfolioMapTemplate(ident1, "paged-parent-structure-el", "parent-structure-element");
		epStructureManager.savePortfolioStructure(template);
		dbInstance.commitAndCloseSession();

		final PortfolioStructureMap map = epFrontendManager.createAndPersistPortfolioStructuredMap(template, ident1, "cloned-map", "cloned-map-from-template", null,
				null, null);

		((EPStructuredMap) map).setReturnDate(new Date());
		final EPTargetResource targetResource = ((EPStructuredMap) map).getTargetResource();
		targetResource.setResourceableTypeName("CourseModule");
		targetResource.setResourceableId(234l);
		targetResource.setSubPath("3894580");
		targetResource.setBusinessPath("[RepositoryEntry:23647598][CourseNode:934598]");

		epStructureManager.savePortfolioStructure(map);
		dbInstance.commitAndCloseSession();

		// test
		final PortfolioStructureMap retrievedMap = (PortfolioStructureMap) epFrontendManager.loadPortfolioStructureByKey(map.getKey());
		assertNotNull(retrievedMap);
		assertNotNull(((EPStructuredMap) retrievedMap).getReturnDate());
		assertNotNull(((EPStructuredMap) retrievedMap).getStructuredMapSource());
		assertNotNull(((EPStructuredMap) retrievedMap).getTargetResource());

		final EPTargetResource retriviedTargetResource = ((EPStructuredMap) retrievedMap).getTargetResource();
		assertEquals("CourseModule", retriviedTargetResource.getResourceableTypeName());
		assertEquals(new Long(234l), retriviedTargetResource.getResourceableId());
		assertEquals("3894580", retriviedTargetResource.getSubPath());
		assertEquals("[RepositoryEntry:23647598][CourseNode:934598]", retriviedTargetResource.getBusinessPath());
	}

	@Test
	public void testMoveUp() {
		// save parent and 5 children
		final PortfolioStructure parentEl = epFrontendManager.createAndPersistPortfolioStructureElement(null, "move-up-parent-structure-el-1",
				"move-up-structure-element");

		final List<PortfolioStructure> children = new ArrayList<PortfolioStructure>();
		for (int i = 0; i < 5; i++) {
			final PortfolioStructure childEl = epFrontendManager.createAndPersistPortfolioStructureElement(parentEl, "paged-structure-el-" + i,
					"paged-child-structure-element");
			children.add(childEl);
		}
		dbInstance.commitAndCloseSession();

		// check if the paging return the right children
		final List<PortfolioStructure> childrenSubset = epFrontendManager.loadStructureChildren(parentEl);
		assertNotNull(childrenSubset);
		assertEquals(5, childrenSubset.size());
		assertEquals(children.get(0).getKey(), childrenSubset.get(0).getKey());
		assertEquals(children.get(1).getKey(), childrenSubset.get(1).getKey());
		assertEquals(children.get(2).getKey(), childrenSubset.get(2).getKey());
		assertEquals(children.get(3).getKey(), childrenSubset.get(3).getKey());
		assertEquals(children.get(4).getKey(), childrenSubset.get(4).getKey());
		dbInstance.commitAndCloseSession();

		// move up the first place
		epStructureManager.moveUp(parentEl, children.get(0));
		dbInstance.commitAndCloseSession();
		// check that all is the same
		final List<PortfolioStructure> persistedChildren1 = epFrontendManager.loadStructureChildren(parentEl);
		assertNotNull(persistedChildren1);
		assertEquals(5, persistedChildren1.size());
		assertEquals(children.get(0).getKey(), persistedChildren1.get(0).getKey());
		assertEquals(children.get(1).getKey(), persistedChildren1.get(1).getKey());
		assertEquals(children.get(2).getKey(), persistedChildren1.get(2).getKey());
		assertEquals(children.get(3).getKey(), persistedChildren1.get(3).getKey());
		assertEquals(children.get(4).getKey(), persistedChildren1.get(4).getKey());
		dbInstance.commitAndCloseSession();

		// move the second to the first place
		epStructureManager.moveUp(parentEl, children.get(1));
		dbInstance.commitAndCloseSession();
		// check that all is the same
		final List<PortfolioStructure> persistedChildren2 = epFrontendManager.loadStructureChildren(parentEl);
		assertNotNull(persistedChildren2);
		assertEquals(5, persistedChildren2.size());
		assertEquals(children.get(1).getKey(), persistedChildren2.get(0).getKey());
		assertEquals(children.get(0).getKey(), persistedChildren2.get(1).getKey());
		assertEquals(children.get(2).getKey(), persistedChildren2.get(2).getKey());
		assertEquals(children.get(3).getKey(), persistedChildren2.get(3).getKey());
		assertEquals(children.get(4).getKey(), persistedChildren2.get(4).getKey());
		dbInstance.commitAndCloseSession();

		// move up the last
		epStructureManager.moveUp(parentEl, children.get(4));
		epStructureManager.savePortfolioStructure(parentEl);
		dbInstance.commitAndCloseSession();
		// check that all is the same
		final List<PortfolioStructure> persistedChildren3 = epFrontendManager.loadStructureChildren(parentEl);
		assertNotNull(persistedChildren3);
		assertEquals(5, persistedChildren3.size());
		assertEquals(children.get(1).getKey(), persistedChildren3.get(0).getKey());
		assertEquals(children.get(0).getKey(), persistedChildren3.get(1).getKey());
		assertEquals(children.get(2).getKey(), persistedChildren3.get(2).getKey());
		assertEquals(children.get(4).getKey(), persistedChildren3.get(3).getKey());
		assertEquals(children.get(3).getKey(), persistedChildren3.get(4).getKey());
	}

	@Test
	public void testMoveDown() {
		// save parent and 5 children
		final PortfolioStructure parentEl = epFrontendManager.createAndPersistPortfolioStructureElement(null, "move-up-parent-structure-el-1",
				"move-up-structure-element");

		final List<PortfolioStructure> children = new ArrayList<PortfolioStructure>();
		for (int i = 0; i < 5; i++) {
			final PortfolioStructure childEl = epFrontendManager.createAndPersistPortfolioStructureElement(parentEl, "paged-structure-el-" + i,
					"paged-child-structure-element");
			children.add(childEl);
		}
		dbInstance.commitAndCloseSession();

		// check if the paging return the right children
		final List<PortfolioStructure> childrenSubset = epFrontendManager.loadStructureChildren(parentEl);
		assertNotNull(childrenSubset);
		assertEquals(5, childrenSubset.size());
		assertEquals(children.get(0).getKey(), childrenSubset.get(0).getKey());
		assertEquals(children.get(1).getKey(), childrenSubset.get(1).getKey());
		assertEquals(children.get(2).getKey(), childrenSubset.get(2).getKey());
		assertEquals(children.get(3).getKey(), childrenSubset.get(3).getKey());
		assertEquals(children.get(4).getKey(), childrenSubset.get(4).getKey());
		dbInstance.commitAndCloseSession();

		// move down the last
		epStructureManager.moveDown(parentEl, children.get(4));
		dbInstance.commitAndCloseSession();
		// check that all is the same
		final List<PortfolioStructure> persistedChildren1 = epFrontendManager.loadStructureChildren(parentEl);
		assertNotNull(persistedChildren1);
		assertEquals(5, persistedChildren1.size());
		assertEquals(children.get(0).getKey(), persistedChildren1.get(0).getKey());
		assertEquals(children.get(1).getKey(), persistedChildren1.get(1).getKey());
		assertEquals(children.get(2).getKey(), persistedChildren1.get(2).getKey());
		assertEquals(children.get(3).getKey(), persistedChildren1.get(3).getKey());
		assertEquals(children.get(4).getKey(), persistedChildren1.get(4).getKey());
		dbInstance.commitAndCloseSession();

		// move down to the last place
		epStructureManager.moveDown(parentEl, children.get(3));
		dbInstance.commitAndCloseSession();
		// check that all is the same
		final List<PortfolioStructure> persistedChildren2 = epFrontendManager.loadStructureChildren(parentEl);
		assertNotNull(persistedChildren2);
		assertEquals(5, persistedChildren2.size());
		assertEquals(children.get(0).getKey(), persistedChildren2.get(0).getKey());
		assertEquals(children.get(1).getKey(), persistedChildren2.get(1).getKey());
		assertEquals(children.get(2).getKey(), persistedChildren2.get(2).getKey());
		assertEquals(children.get(4).getKey(), persistedChildren2.get(3).getKey());
		assertEquals(children.get(3).getKey(), persistedChildren2.get(4).getKey());
		dbInstance.commitAndCloseSession();

		// move down the first to the second position
		epStructureManager.moveDown(parentEl, children.get(0));
		epStructureManager.savePortfolioStructure(parentEl);
		dbInstance.commitAndCloseSession();
		// check that all is the same
		final List<PortfolioStructure> persistedChildren3 = epFrontendManager.loadStructureChildren(parentEl);
		assertNotNull(persistedChildren3);
		assertEquals(5, persistedChildren3.size());
		assertEquals(children.get(1).getKey(), persistedChildren3.get(0).getKey());
		assertEquals(children.get(0).getKey(), persistedChildren3.get(1).getKey());
		assertEquals(children.get(2).getKey(), persistedChildren3.get(2).getKey());
		assertEquals(children.get(4).getKey(), persistedChildren3.get(3).getKey());
		assertEquals(children.get(3).getKey(), persistedChildren3.get(4).getKey());
	}

	@Test
	public void testAddAuthorToMap() {
		// save the map
		final PortfolioStructureMap map = epStructureManager.createPortfolioMapTemplate(ident1, "add-author-map-1", "add-an-author-to-map-template");
		epStructureManager.savePortfolioStructure(map);
		dbInstance.commitAndCloseSession();

		// add an author
		epStructureManager.addAuthor(map, ident2);
		dbInstance.commitAndCloseSession();

		// check that the author are in the
		final OLATResource resource = resourceManager.findResourceable(map.getResourceableId(), map.getResourceableTypeName());
		assertNotNull(resource);
		final RepositoryEntry re = repositoryManager.lookupRepositoryEntry(resource, false);
		assertNotNull(re);
		final SecurityGroup secGroup = re.getOwnerGroup();
		assertNotNull(secGroup);
		final List<Identity> authors = securityManager.getIdentitiesOfSecurityGroup(secGroup);
		assertEquals(2, authors.size());
		assertTrue(authors.contains(ident1));// owner
		assertTrue(authors.contains(ident2));// owner
	}

	@Test
	public void testRemoveAuthorToMap() {
		// save the map
		final PortfolioStructureMap map = epStructureManager.createPortfolioMapTemplate(ident1, "add-author-map-1", "add-an-author-to-map-template");
		epStructureManager.savePortfolioStructure(map);
		dbInstance.commitAndCloseSession();

		// add an author
		epStructureManager.addAuthor(map, ident2);
		dbInstance.commitAndCloseSession();

		// check that the author are in the
		final OLATResource resource = resourceManager.findResourceable(map.getResourceableId(), map.getResourceableTypeName());
		assertNotNull(resource);
		final RepositoryEntry re = repositoryManager.lookupRepositoryEntry(resource, false);
		assertNotNull(re);
		final SecurityGroup secGroup = re.getOwnerGroup();
		assertNotNull(secGroup);
		final List<Identity> authors = securityManager.getIdentitiesOfSecurityGroup(secGroup);
		assertEquals(2, authors.size());
		dbInstance.commitAndCloseSession();

		// and remove the author
		epStructureManager.removeAuthor(map, ident2);
		dbInstance.commitAndCloseSession();

		final List<Identity> singleAuthor = securityManager.getIdentitiesOfSecurityGroup(secGroup);
		assertEquals(1, singleAuthor.size());
		assertTrue(singleAuthor.contains(ident1));// owner
		assertFalse(singleAuthor.contains(ident2));// owner

		securityManager.getSecurityGroupsForIdentity(ident1);
		repositoryManager.queryReferencableResourcesLimitType(ident1, new Roles(false, false, false, false, false, false, false), null, null, null, null);
	}

}
