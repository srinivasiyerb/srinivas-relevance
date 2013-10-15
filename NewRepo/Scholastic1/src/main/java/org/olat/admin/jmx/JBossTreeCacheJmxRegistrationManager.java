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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.admin.jmx;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.jmx.JmxRegistrationManager;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * Helper class to register all JBoss Tree Cache MBeans at JMX Server.
 * 
 * @author Christian Guretzki
 */
public class JBossTreeCacheJmxRegistrationManager {
	OLog log = Tracing.createLoggerFor(this.getClass());

	private MBeanServer server;

	public JBossTreeCacheJmxRegistrationManager() {
		//
	}

	/**
	 * Register StatisticsService as MBean for JMX support.
	 * 
	 * @param mySessionFactory
	 */
	private void registerAllJBossTreeCacheMBeans() {
		try {
			log.info("start to register all JBoss Treecache MBeans...");
			final CacheFactory factory = new DefaultCacheFactory();
			final Cache cache = factory.createCache("treecache.xml");
			final ObjectName cacheObjectName = new ObjectName("jboss.cache:service=Cache");
			final JmxRegistrationManager jmxRegistrationManager = new JmxRegistrationManager(server, cache, cacheObjectName);
			jmxRegistrationManager.registerAllMBeans();
			log.info("registered all JBoss Treecache MBeans");
		} catch (final MalformedObjectNameException e) {
			log.warn("JMX-Error : Can not register as MBean, MalformedObjectNameException=", e);
		} catch (final NoSuchBeanDefinitionException e) {
			log.warn("JMX-Error : Can not register as MBean, NoSuchBeanDefinitionException=", e);
		}
	}

	public void setServer(final MBeanServer server) {
		this.server = server;
		registerAllJBossTreeCacheMBeans();
	}

}
