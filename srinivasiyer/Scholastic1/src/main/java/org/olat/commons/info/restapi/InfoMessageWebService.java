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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.olat.commons.info.model.InfoMessage;

/**
 * Description:<br>
 * Resource for Info Message
 * <P>
 * Initial Date: 29 jul. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class InfoMessageWebService {

	private final InfoMessage msg;

	public InfoMessageWebService(final InfoMessage msg) {
		this.msg = msg;
	}

	/**
	 * Get an new info message by key
	 * 
	 * @response.representation.200.qname {http://www.example.com}infoMessageVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The info message
	 * @response.representation.200.example {@link org.olat.commons.info.restapi.Examples#SAMPLE_INFOMESSAGEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @param infoMessageKey The key
	 * @param request The HTTP request
	 * @return It returns the newly info message
	 */
	@GET
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public Response getMessage(@Context final HttpServletRequest request) {
		final InfoMessageVO msgVO = new InfoMessageVO(msg);
		return Response.ok(msgVO).build();
	}
}
