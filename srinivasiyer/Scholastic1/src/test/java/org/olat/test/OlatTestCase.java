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

package org.olat.test;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.junit.Before;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.OLATLocalSessionFactoryBean;
import org.olat.core.helpers.Settings;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Initial Date: 25.10.2002
 * 
 * @author Florian Gnaegi
 * @author guido This class is common parent to all JUnit Use Case tests in OLAT framework integration tests.
 */
@ContextConfiguration(loader = MockServletContextWebContextLoader.class, locations = { "classpath:/org/olat/core/util/vfs/version/_spring/versioningCorecontext.xml",
		"classpath:/org/olat/core/util/i18n/_spring/i18nCorecontext.xml", "classpath:/org/olat/core/util/_spring/utilCorecontext.xml",
		"classpath:/org/olat/core/util/i18n/devtools/_spring/devtoolsCorecontext.xml",
		"classpath:/org/olat/core/util/event/_spring/frameworkStartedEventCorecontext.xml",

		"classpath:/org/olat/core/commons/persistence/_spring/databaseCorecontext.xml",
		"classpath:/org/olat/core/commons/taskExecutor/_spring/taskExecutorCorecontext.xml",
		"classpath:/org/olat/core/commons/fullWebApp/util/_spring/StickyMessageCorecontext.xml",
		"classpath:/org/olat/core/commons/modules/bc/_spring/folderModuleCorecontext.xml",
		"classpath:/org/olat/core/commons/modules/glossary/_spring/glossaryCorecontext.xml",
		"classpath:/org/olat/core/commons/contextHelp/_spring/contextHelpCorecontext.xml",

		"classpath:/org/olat/core/logging/_spring/loggingCorecontext.xml", "classpath:/org/olat/core/logging/activity/_spring/activityCorecontext.xml",
		"classpath:/org/olat/core/_spring/mainCorecontext.xml", "classpath:/org/olat/core/dispatcher/jumpin/_spring/jumpinCorecontext.xml",

		"classpath:/serviceconfig/org/olat/core/gui/components/form/flexible/impl/elements/richText/_spring/richTextCorecontext.xml",
		"classpath:/serviceconfig/org/olat/core/commons/scheduler/_spring/schedulerCorecontext.xml",
		"classpath:/serviceconfig/org/olat/core/commons/modules/glossary/_spring/glossaryCorecontext.xml",
		"classpath:/serviceconfig/org/olat/core/commons/services/commentAndRating/_spring/commentsAndRatingCorecontext.xml",
		"classpath:/org/olat/core/commons/services/tagging/_spring/taggingContext.xml",
		"classpath:/serviceconfig/org/olat/core/commons/linkchooser/_spring/linkchooserCorecontext.xml",
		"classpath:/serviceconfig/org/olat/core/_spring/mainCorecontext.xml", "classpath:/org/olat/core/commons/services/_spring/servicesCorecontext.xml",

		"classpath*:**/serviceconfig/**/_spring/olatcoreconfig.xml", "classpath*:**/serviceconfig/**/_spring/olatdefaultconfig.xml",
		"classpath*:**/serviceconfig/**/_spring/olatextconfig.xml", "classpath*:**/serviceconfig/brasatoconfig.xml", "classpath*:**/_spring/brasatoconfigpart.xml",
		"classpath*:**/_spring/brasatoalias.xml", "classpath*:**/_spring/*Context.xml", "classpath*:*Context.xml" })
public abstract class OlatTestCase extends AbstractJUnit4SpringContextTests {

	private boolean hsqlDBConfigured = false;

	/**
	 * If you like to disable a test method for some time just add the
	 * 
	 * @Ignore("not today") annotation The normal flow is that the spring context gets loaded and befor each test method the @before will be executed and after the the
	 *              method each time the @after will be executed
	 */

	/**
	 * @param arg0
	 */
	public OlatTestCase() {
		Settings.setJUnitTest(true);
	}

	@Before
	public void printBanner() {
		final OLATLocalSessionFactoryBean bean = (OLATLocalSessionFactoryBean) CoreSpringFactory.getBean(OLATLocalSessionFactoryBean.class);
		final Configuration configuration = bean.getConfiguration();

		final Properties properties = configuration.getProperties();

		final String[] propsOfInterest = new String[] { "hibernate.connection.driver_class", "hibernate.connection.provider_class", "hibernate.connection.url",
				"hibernate.connection.username", };

		final String connectionURL = (String) properties.get("hibernate.connection.url");
		hsqlDBConfigured = connectionURL != null && connectionURL.toLowerCase().indexOf("hsqldb") > 0;

		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		for (int i = 0; i < propsOfInterest.length; i++) {
			System.out.println("++" + propsOfInterest[i] + " -> " + properties.getProperty(propsOfInterest[i]));
		}

		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		printOlatLocalProperties();
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println("+ OLAT configuration initialized, starting now with junit tests +");
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

	}

	@SuppressWarnings("unchecked")
	private void printOlatLocalProperties() {
		final Resource overwritePropertiesRes = new ClassPathResource("olat.local.properties");
		try {
			final Properties overwriteProperties = new Properties();
			overwriteProperties.load(overwritePropertiesRes.getInputStream());
			final Enumeration<String> propNames = (Enumeration<String>) overwriteProperties.propertyNames();

			System.out.println("### olat.local.properties : ###");
			while (propNames.hasMoreElements()) {
				final String propName = propNames.nextElement();
				System.out.println("++" + propName + "='" + overwriteProperties.getProperty(propName) + "'");
			}
		} catch (final IOException e) {
			System.err.println("Could not load properties files from classpath! Exception=" + e);
		}

	}

	/**
	 * @return
	 */
	protected boolean getHsqlDbConfigured() {
		return hsqlDBConfigured;
	}

}