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
package org.olat.admin.extensions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.olat.OlatBeanTypes;
import org.olat.admin.SystemAdminMainController;
import org.olat.core.CoreBeanTypes;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * @author Christian Guretzki
 */
public class ExtensionsAdminController extends BasicController {
	private static OLog log = Tracing.createLoggerFor(ExtensionsAdminController.class);

	private final VelocityContainer content;
	private final Panel mainPanel;

	public ExtensionsAdminController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		mainPanel = new Panel("extensionsPanel");
		// use combined translator from system admin main
		setTranslator(Util.createPackageTranslator(SystemAdminMainController.class, ureq.getLocale(), getTranslator()));
		content = createVelocityContainer("extensionsAdmin");

		final Map extensionList = new HashMap();
		for (final CoreBeanTypes coreExtensionTypes : CoreBeanTypes.values()) {
			extensionList.put(coreExtensionTypes.name(), getBeanDefListFor(coreExtensionTypes.getExtensionTypeClass()));
		}
		for (final OlatBeanTypes olatExtensionTypes : OlatBeanTypes.values()) {
			extensionList.put(olatExtensionTypes.name(), getBeanDefListFor(olatExtensionTypes.getExtensionTypeClass()));
		}
		content.contextPut("extensionList", extensionList);

		// getOverwrittenBeans();

		mainPanel.setContent(content);
		putInitialPanel(mainPanel);
	}

	private Map<String, GenericBeanDefinition> getBeanDefListFor(final Class clazz) {
		final ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(CoreSpringFactory.servletContext);
		final XmlWebApplicationContext context = (XmlWebApplicationContext) applicationContext;
		final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

		final Map<String, GenericBeanDefinition> beanDefinitionList = new HashMap<String, GenericBeanDefinition>();

		final String[] beanNames = beanFactory.getBeanNamesForType(clazz);
		for (int i = 0; i < beanNames.length; i++) {
			try {
				log.debug(">>> beanNames=" + beanNames[i]);
				final GenericBeanDefinition beanDef = (GenericBeanDefinition) beanFactory.getBeanDefinition(beanNames[i]);
				final ConstructorArgumentValues args = beanDef.getConstructorArgumentValues();
				final List<ValueHolder> values = args.getGenericArgumentValues();
				for (final Iterator iterator = values.iterator(); iterator.hasNext();) {
					final ValueHolder valueHolder = (ValueHolder) iterator.next();
					log.debug("valueHolder=" + valueHolder);
					log.debug("valueHolder.getType()=" + valueHolder.getType());
					log.debug("valueHolder.getName()=" + valueHolder.getName());
					log.debug("valueHolder.getValue()=" + valueHolder.getValue());
				}
				beanDefinitionList.put(beanNames[i], beanDef);
			} catch (final NoSuchBeanDefinitionException e) {
				log.warn("Error while trying to analyze bean with name: " + beanNames[i] + " :" + e);
			} catch (final Exception e) {
				log.warn("Error while trying to analyze bean with name: " + beanNames[i] + " :" + e);
			}
		}
		return beanDefinitionList;
	}

	/**
	 * hmmm, does not work yet, how to get a list with all overwritten beans like the output from to the log.info
	 * http://www.docjar.com/html/api/org/springframework/beans/factory/support/DefaultListableBeanFactory.java.html
	 * 
	 * @return
	 */
	private List getOverwrittenBeans() {
		final ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(CoreSpringFactory.servletContext);
		final XmlWebApplicationContext context = (XmlWebApplicationContext) applicationContext;
		final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		final String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
		for (int i = 0; i < beanDefinitionNames.length; i++) {
			final String beanName = beanDefinitionNames[i];
			if (!beanName.contains("#")) {
				final BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
				// System.out.println(beanDef.getOriginatingBeanDefinition());
			}
		}
		return null;
	}

	@Override
	protected void doDispose() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		// TODO Auto-generated method stub

	}

}
