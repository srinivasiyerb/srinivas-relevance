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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.util.vfs.VFSItem;
import org.olat.course.ICourse;
import org.olat.course.nodes.BCCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.CourseNodeConfiguration;
import org.olat.course.nodes.CourseNodeFactory;
import org.olat.restapi.repository.course.CoursesWebService;
import org.olat.test.OlatJerseyTestCase;

public class CoursesFoldersTest extends OlatJerseyTestCase {

	private static ICourse course1;
	private static CourseNode bcNode;
	private static Identity admin;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		admin = BaseSecurityManager.getInstance().findIdentityByName("administrator");
		course1 = CoursesWebService.createEmptyCourse(admin, "course1", "course1 long name", null);
		DBFactory.getInstance().intermediateCommit();

		// create a folder
		final CourseNodeConfiguration newNodeConfig = CourseNodeFactory.getInstance().getCourseNodeConfiguration("bc");
		bcNode = newNodeConfig.getInstance();
		bcNode.setShortTitle("Folder");
		bcNode.setLearningObjectives("Folder objectives");
		bcNode.setNoAccessExplanation("You don't have access");
		course1.getEditorTreeModel().addCourseNode(bcNode, course1.getRunStructure().getRootNode());
		DBFactory.getInstance().intermediateCommit();
	}

	@Test
	public void testUploadFile() throws IOException, URISyntaxException {
		final HttpClient c = loginWithCookie("administrator", "olat");

		final URI uri = UriBuilder.fromUri(getNodeURI()).path("files").build();

		// create single page
		final URL fileUrl = RepositoryEntriesTest.class.getResource("singlepage.html");
		assertNotNull(fileUrl);
		final File file = new File(fileUrl.toURI());

		final PutMethod method = createPut(uri, MediaType.APPLICATION_JSON, true);
		method.addRequestHeader("Content-Type", MediaType.MULTIPART_FORM_DATA);
		final Part[] parts = { new FilePart("file", file), new StringPart("filename", file.getName()) };
		method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));
		final int code = c.executeMethod(method);
		assertEquals(code, 200);

		final OlatNamedContainerImpl folder = BCCourseNode.getNodeFolderContainer((BCCourseNode) bcNode, course1.getCourseEnvironment());
		final VFSItem item = folder.resolve(file.getName());
		assertNotNull(item);
	}

	private URI getNodeURI() {
		return UriBuilder.fromUri(getContextURI()).path("repo").path("courses").path(course1.getResourceableId().toString()).path("elements").path("folders")
				.path(bcNode.getIdent()).build();
	}
}
