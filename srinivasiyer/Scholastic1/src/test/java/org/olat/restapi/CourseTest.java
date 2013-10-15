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

package org.olat.restapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.olat.admin.securitygroup.gui.IdentitiesAddEvent;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.restapi.repository.course.CoursesWebService;
import org.olat.restapi.support.vo.CourseVO;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatJerseyTestCase;
import org.olat.user.restapi.UserVO;

public class CourseTest extends OlatJerseyTestCase {

	private static final OLog log = Tracing.createLoggerFor(CourseTest.class);

	private Identity admin, auth0, auth1, auth2;
	private ICourse course1;

	/**
	 * SetUp is called before each test.
	 */
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		try {
			// create course and persist as OLATResourceImpl
			admin = BaseSecurityManager.getInstance().findIdentityByName("administrator");
			auth0 = JunitTestHelper.createAndPersistIdentityAsUser("rest-zero");
			auth1 = JunitTestHelper.createAndPersistIdentityAsUser("rest-one");
			auth2 = JunitTestHelper.createAndPersistIdentityAsUser("rest-two");

			course1 = CoursesWebService.createEmptyCourse(admin, "course1", "course1 long name", null);

			DBFactory.getInstance().closeSession();
		} catch (final Exception e) {
			log.error("Exception in setUp(): " + e);
		}
	}

	@Test
	public void testGetCourse() throws IOException {
		final HttpClient c = loginWithCookie("administrator", "olat");
		final GetMethod method = createGet("/repo/courses/" + course1.getResourceableId(), MediaType.APPLICATION_JSON, true);
		final int code = c.executeMethod(method);
		assertEquals(code, 200);
		final String body = method.getResponseBodyAsString();
		final CourseVO course = parse(body, CourseVO.class);
		assertNotNull(course);
		assertEquals(course1.getResourceableId(), course.getKey());
		assertEquals(course1.getCourseTitle(), course.getTitle());
	}

	@Test
	public void testGetCourseRunStructure() throws IOException {
		final HttpClient c = loginWithCookie("administrator", "olat");
		final GetMethod method = createGet("/repo/courses/" + course1.getResourceableId() + "/runstructure", MediaType.APPLICATION_XML, true);
		final int code = c.executeMethod(method);
		assertEquals(code, 200);
		final String body = method.getResponseBodyAsString();
		assertNotNull(body);
		assertTrue(body.length() > 100);
		assertTrue(body.indexOf("<org.olat.course.Structure>") >= 0);
	}

	@Test
	public void testGetCourseEditorTreeModel() throws IOException {
		final HttpClient c = loginWithCookie("administrator", "olat");
		final GetMethod method = createGet("/repo/courses/" + course1.getResourceableId() + "/editortreemodel", MediaType.APPLICATION_XML, true);
		final int code = c.executeMethod(method);
		assertEquals(code, 200);
		final String body = method.getResponseBodyAsString();
		assertNotNull(body);
		assertTrue(body.length() > 100);
		assertTrue(body.indexOf("<org.olat.course.tree.CourseEditorTreeModel>") >= 0);
	}

	@Test
	public void testDeleteCourses() throws IOException {
		final ICourse course = CoursesWebService.createEmptyCourse(admin, "courseToDel", "course to delete", null);
		DBFactory.getInstance().intermediateCommit();

		final HttpClient c = loginWithCookie("administrator", "olat");
		final DeleteMethod method = createDelete("/repo/courses/" + course.getResourceableId(), MediaType.APPLICATION_JSON, true);
		final int code = c.executeMethod(method);
		assertEquals(code, 200);

		final List<String> courseType = new ArrayList<String>();
		courseType.add(CourseModule.getCourseTypeName());
		final Roles roles = new Roles(true, true, true, true, false, true, false);
		final List<RepositoryEntry> repoEntries = RepositoryManager.getInstance().genericANDQueryWithRolesRestriction("*", "*", "*", courseType, roles, "");
		assertNotNull(repoEntries);

		for (final RepositoryEntry entry : repoEntries) {
			assertNotSame(entry.getOlatResource().getResourceableId(), course.getResourceableId());
		}
	}

	@Test
	public void testAddAuthor() throws IOException {
		final HttpClient c = loginWithCookie("administrator", "olat");
		final String uri = "/repo/courses/" + course1.getResourceableId() + "/authors/" + auth0.getKey();
		final PutMethod method = createPut(uri, MediaType.APPLICATION_JSON, true);
		final int code = c.executeMethod(method);
		assertEquals(code, 200);

		// is auth0 author
		final BaseSecurity securityManager = BaseSecurityManager.getInstance();
		final SecurityGroup authorGroup = securityManager.findSecurityGroupByName(Constants.GROUP_AUTHORS);
		final boolean isAuthor = securityManager.isIdentityInSecurityGroup(auth0, authorGroup);
		DBFactory.getInstance().intermediateCommit();
		assertTrue(isAuthor);

		// is auth0 owner
		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course1, true);
		final SecurityGroup ownerGroup = repositoryEntry.getOwnerGroup();
		final boolean isOwner = securityManager.isIdentityInSecurityGroup(auth0, ownerGroup);
		DBFactory.getInstance().intermediateCommit();
		assertTrue(isOwner);
	}

	@Test
	public void testGetAuthors() throws IOException {
		// make auth1 and auth2 authors
		final BaseSecurity securityManager = BaseSecurityManager.getInstance();
		final SecurityGroup authorGroup = securityManager.findSecurityGroupByName(Constants.GROUP_AUTHORS);
		if (!securityManager.isIdentityInSecurityGroup(auth1, authorGroup)) {
			securityManager.addIdentityToSecurityGroup(auth1, authorGroup);
		}
		if (!securityManager.isIdentityInSecurityGroup(auth2, authorGroup)) {
			securityManager.addIdentityToSecurityGroup(auth2, authorGroup);
		}
		DBFactory.getInstance().intermediateCommit();

		// make auth1 and auth2 owner
		final RepositoryManager rm = RepositoryManager.getInstance();
		final RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course1, true);
		final List<Identity> authors = new ArrayList<Identity>();
		authors.add(auth1);
		authors.add(auth2);
		final IdentitiesAddEvent identitiesAddedEvent = new IdentitiesAddEvent(authors);
		rm.addOwners(admin, identitiesAddedEvent, repositoryEntry);
		DBFactory.getInstance().intermediateCommit();

		// get them
		final HttpClient c = loginWithCookie("administrator", "olat");
		final String uri = "/repo/courses/" + course1.getResourceableId() + "/authors";
		final GetMethod method = createGet(uri, MediaType.APPLICATION_JSON, true);
		final int code = c.executeMethod(method);
		assertEquals(code, 200);
		final String body = method.getResponseBodyAsString();
		assertNotNull(body);

		final List<UserVO> authorVOs = parseUserArray(body);
		assertNotNull(authorVOs);
	}

	@Test
	public void testRemoveAuthor() throws IOException {
		// make auth1 and auth2 authors
		final BaseSecurity securityManager = BaseSecurityManager.getInstance();
		final SecurityGroup authorGroup = securityManager.findSecurityGroupByName(Constants.GROUP_AUTHORS);
		if (!securityManager.isIdentityInSecurityGroup(auth1, authorGroup)) {
			securityManager.addIdentityToSecurityGroup(auth1, authorGroup);
		}
		if (!securityManager.isIdentityInSecurityGroup(auth2, authorGroup)) {
			securityManager.addIdentityToSecurityGroup(auth2, authorGroup);
		}
		DBFactory.getInstance().intermediateCommit();

		// make auth1 and auth2 owner
		final RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course1, true);
		final List<Identity> authors = new ArrayList<Identity>();
		authors.add(auth1);
		authors.add(auth2);
		final IdentitiesAddEvent identitiesAddedEvent = new IdentitiesAddEvent(authors);
		rm.addOwners(admin, identitiesAddedEvent, repositoryEntry);
		DBFactory.getInstance().intermediateCommit();
		// end setup

		// test
		final HttpClient c = loginWithCookie("administrator", "olat");
		String uri = "/repo/courses/" + course1.getResourceableId() + "/authors/" + auth1.getKey();
		DeleteMethod method = createDelete(uri, MediaType.APPLICATION_JSON, true);
		int code = c.executeMethod(method);
		assertEquals(code, 200);

		uri = "/repo/courses/" + course1.getResourceableId() + "/authors/" + auth2.getKey();
		method = createDelete(uri, MediaType.APPLICATION_JSON, true);
		code = c.executeMethod(method);
		assertEquals(code, 200);

		// control
		repositoryEntry = rm.lookupRepositoryEntry(course1, true);
		final SecurityGroup ownerGroup = repositoryEntry.getOwnerGroup();
		assertFalse(securityManager.isIdentityInSecurityGroup(auth1, ownerGroup));
		assertFalse(securityManager.isIdentityInSecurityGroup(auth2, ownerGroup));
		DBFactory.getInstance().intermediateCommit();
	}

	protected List<UserVO> parseUserArray(final String body) {
		try {
			final ObjectMapper mapper = new ObjectMapper(jsonFactory);
			return mapper.readValue(body, new TypeReference<List<UserVO>>() {/* */});
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}