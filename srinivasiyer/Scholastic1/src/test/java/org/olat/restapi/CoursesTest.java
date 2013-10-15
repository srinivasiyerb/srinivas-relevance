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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.course.ICourse;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.restapi.repository.course.CoursesWebService;
import org.olat.restapi.support.vo.CourseVO;
import org.olat.test.OlatJerseyTestCase;

public class CoursesTest extends OlatJerseyTestCase {

	private static final OLog log = Tracing.createLoggerFor(CoursesTest.class);

	private Identity admin;
	private ICourse course1, course2;

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
			course1 = CoursesWebService.createEmptyCourse(admin, "courses1", "courses1 long name", null);
			course2 = CoursesWebService.createEmptyCourse(admin, "courses2", "courses2 long name", null);

			DBFactory.getInstance().closeSession();
		} catch (final Exception e) {
			log.error("Exception in setUp(): " + e);
		}
	}

	@Test
	public void testGetCourses() throws IOException {
		final HttpClient c = loginWithCookie("administrator", "olat");

		final HttpMethod method = createGet("/repo/courses", MediaType.APPLICATION_JSON, true);
		final int code = c.executeMethod(method);
		assertEquals(code, 200);
		final String body = method.getResponseBodyAsString();
		final List<CourseVO> courses = parseCourseArray(body);
		assertNotNull(courses);
		assertTrue(courses.size() >= 2);

		CourseVO vo1 = null;
		CourseVO vo2 = null;
		for (final CourseVO course : courses) {
			if (course.getTitle().equals("courses1")) {
				vo1 = course;
			} else if (course.getTitle().equals("courses2")) {
				vo2 = course;
			}
		}
		assertNotNull(vo1);
		assertEquals(vo1.getKey(), course1.getResourceableId());
		assertNotNull(vo2);
		assertEquals(vo2.getKey(), course2.getResourceableId());
	}

	@Test
	public void testCreateEmptyCourse() throws IOException {
		final HttpClient c = loginWithCookie("administrator", "olat");

		final URI uri = UriBuilder.fromUri(getContextURI()).path("repo").path("courses").queryParam("shortTitle", "course3").queryParam("title", "course3 long name")
				.build();
		final PutMethod method = createPut(uri, MediaType.APPLICATION_JSON, true);

		final int code = c.executeMethod(method);
		assertEquals(code, 200);
		final String body = method.getResponseBodyAsString();
		final CourseVO course = parse(body, CourseVO.class);
		assertNotNull(course);
		assertEquals("course3", course.getTitle());
		// check repository entry
		final RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(course.getRepoEntryKey());
		assertNotNull(re);
		assertNotNull(re.getOlatResource());
		assertNotNull(re.getOwnerGroup());
	}

	protected List<CourseVO> parseCourseArray(final String body) {
		try {
			final ObjectMapper mapper = new ObjectMapper(jsonFactory);
			return mapper.readValue(body, new TypeReference<List<CourseVO>>() {/* */});
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}