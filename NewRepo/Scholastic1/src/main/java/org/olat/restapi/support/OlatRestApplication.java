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

package org.olat.restapi.support;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.olat.core.CoreSpringFactory;

/**
 * Description:<br>
 * REST Application
 * <P>
 * Initial Date: 15 apr. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
public class OlatRestApplication extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(JacksonJaxbJsonProvider.class);
		classes.addAll(getRestRegistrationService().getClasses());
		return classes;
	}

	@Override
	public Set<Object> getSingletons() {
		return getRestRegistrationService().getSingletons();
	}

	public RestRegistrationService getRestRegistrationService() {
		return (RestRegistrationService) CoreSpringFactory.getBean(RestRegistrationService.class);
	}
}
