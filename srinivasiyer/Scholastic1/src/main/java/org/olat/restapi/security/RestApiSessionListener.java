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
package org.olat.restapi.security;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.olat.core.CoreSpringFactory;

/**
 * Description:<br>
 * TODO: srosse Class Description for RestApiSessionListener
 * <P>
 * Initial Date: 7 apr. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
public class RestApiSessionListener implements HttpSessionListener {

	@Override
	public void sessionCreated(final HttpSessionEvent se) {
		//
	}

	@Override
	public void sessionDestroyed(final HttpSessionEvent se) {
		final RestSecurityBean securityBean = (RestSecurityBean) CoreSpringFactory.getBean(RestSecurityBean.class);
		final HttpSession session = se.getSession();
		securityBean.unbindTokenToSession(session);
	}
}
