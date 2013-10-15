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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import org.olat.core.configuration.Destroyable;
import org.olat.core.configuration.Initializable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Description:<br>
 * tests if the init / destory method calls are in the spring config when the initializable or destoryable interfaces are used
 * <P>
 * Initial Date: 17.03.2010 <br>
 * 
 * @author guido
 */
public class SpringInitDestroyVerficationTest extends OlatTestCase {

	@Test
	public void testInitMethodCalls() {
		final XmlWebApplicationContext context = (XmlWebApplicationContext) applicationContext;
		final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

		final Map<String, Initializable> beans = applicationContext.getBeansOfType(Initializable.class);
		for (final Iterator iterator = beans.keySet().iterator(); iterator.hasNext();) {
			final String beanName = (String) iterator.next();
			try {
				final GenericBeanDefinition beanDef = (GenericBeanDefinition) beanFactory.getBeanDefinition(beanName);
				assertNotNull("Spring Bean (" + beanName + ") of type Initializable does not have the required init-method attribute or the method name is not init!",
						beanDef.getInitMethodName());
				if (beanDef.getDestroyMethodName() != null) {
					assertTrue("Spring Bean (" + beanName + ") of type Initializable does not have the required init-method attribute or the method name is not init!",
							beanDef.getInitMethodName().equals("init"));
				}
			} catch (final NoSuchBeanDefinitionException e) {
				System.out.println("testInitMethodCalls: Error while trying to analyze bean with name: " + beanName + " :" + e);
			} catch (final Exception e) {
				System.out.println("testInitMethodCalls: Error while trying to analyze bean with name: " + beanName + " :" + e);
			}
		}
	}

	@Test
	public void testDestroyMethodCalls() {
		final XmlWebApplicationContext context = (XmlWebApplicationContext) applicationContext;
		final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

		final Map<String, Destroyable> beans = applicationContext.getBeansOfType(Destroyable.class);
		for (final Iterator iterator = beans.keySet().iterator(); iterator.hasNext();) {
			final String beanName = (String) iterator.next();
			try {
				final GenericBeanDefinition beanDef = (GenericBeanDefinition) beanFactory.getBeanDefinition(beanName);
				assertNotNull(
						"Spring Bean (" + beanName + ") of type Destroyable does not have the required destroy-method attribute or the method name is not destroy!",
						beanDef.getDestroyMethodName());
				if (beanDef.getDestroyMethodName() != null) {
					assertTrue("Spring Bean (" + beanName
							+ ") of type Destroyable does not have the required destroy-method attribute or the method name is not destroy!", beanDef
							.getDestroyMethodName().equals("destroy"));
				}
			} catch (final NoSuchBeanDefinitionException e) {
				System.out.println("testDestroyMethodCalls: Error while trying to analyze bean with name: " + beanName + " :" + e);
			} catch (final Exception e) {
				System.out.println("testDestroyMethodCalls: Error while trying to analyze bean with name: " + beanName + " :" + e);
			}
		}
	}

	@Test
	public void testAnnotatedInitMethodCalls() {
		// TODO implement init methods annoteded with postconstruct annotation
	}

	@Test
	public void testAnnotatedDestroyMethodCalls() {
		// TODO implement destroy methods annoteded with predestroy annotation
	}

}
