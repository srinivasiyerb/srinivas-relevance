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

package org.olat.commons.info.restapi;

import static org.olat.restapi.security.RestSecurityHelper.getUserRequest;
import static org.olat.restapi.security.RestSecurityHelper.isAuthor;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.commons.info.manager.InfoMessageFrontendManager;
import org.olat.commons.info.model.InfoMessage;
import org.olat.core.gui.UserRequest;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;

/**
 * Description:<br>
 * <P>
 * Initial Date: 29 jul. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
@Path("infomessages")
public class InfoMessagesWebService {

	private static final String VERSION = "1.0";

	/**
	 * The version of the Info messages Web Service
	 * 
	 * @response.representation.200.mediaType text/plain
	 * @response.representation.200.doc The version of this specific Web Service
	 * @response.representation.200.example 1.0
	 * @return
	 */
	@GET
	@Path("version")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getVersion() {
		return Response.ok(VERSION).build();
	}

	/**
	 * Creates a new info message
	 * 
	 * @response.representation.200.qname {http://www.example.com}infoMessageVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The info message
	 * @response.representation.200.example {@link org.olat.commons.info.restapi.Examples#SAMPLE_INFOMESSAGEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @param resName The OLAT Resourceable name
	 * @param resId The OLAT Resourceable id
	 * @param resSubPath The resource sub path (optional)
	 * @param businessPath The business path
	 * @param authorKey The identity key of the author
	 * @param title The title
	 * @param message The message
	 * @param request The HTTP request
	 * @return It returns the id of the newly info message
	 */
	@PUT
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response createEmptyCourse(final @QueryParam("resName") String resName, final @QueryParam("resId") Long resId,
			@QueryParam("resSubPath") final String resSubPath, @QueryParam("businessPath") final String businessPath, @QueryParam("authorKey") final Long authorKey,
			@QueryParam("title") final String title, @QueryParam("message") final String message, @Context final HttpServletRequest request) {

		if (!isAuthor(request)) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }

		final OLATResourceable ores = new OLATResourceable() {
			@Override
			public String getResourceableTypeName() {
				return resName;
			}

			@Override
			public Long getResourceableId() {
				return resId;
			}
		};

		Identity author;
		final UserRequest ureq = getUserRequest(request);
		if (authorKey == null) {
			author = ureq.getIdentity();
		} else {
			final BaseSecurity securityManager = BaseSecurityManager.getInstance();
			author = securityManager.loadIdentityByKey(authorKey, false);
			if (author == null) { return Response.serverError().status(Status.UNAUTHORIZED).build(); }
		}

		final InfoMessageFrontendManager messageManager = InfoMessageFrontendManager.getInstance();
		final InfoMessage msg = messageManager.createInfoMessage(ores, resSubPath, businessPath, author);
		msg.setTitle(title);
		msg.setMessage(message);
		messageManager.sendInfoMessage(msg, null, ureq.getLocale(), Collections.<Identity> emptyList());
		final InfoMessageVO infoVO = new InfoMessageVO(msg);
		return Response.ok(infoVO).build();
	}

	@Path("{infoMessageKey}")
	public InfoMessageWebService getInfoMessageWebservice(@PathParam("infoMessageKey") final Long infoMessageKey) {
		final InfoMessageFrontendManager messageManager = InfoMessageFrontendManager.getInstance();
		final InfoMessage msg = messageManager.loadInfoMessage(infoMessageKey);
		return new InfoMessageWebService(msg);
	}
}
