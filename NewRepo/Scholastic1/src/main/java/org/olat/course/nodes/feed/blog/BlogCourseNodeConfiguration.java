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
package org.olat.course.nodes.feed.blog;

import java.util.List;
import java.util.Locale;

import org.olat.core.extensions.ExtensionResource;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.course.nodes.AbstractCourseNodeConfiguration;
import org.olat.course.nodes.BlogCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.CourseNodeConfiguration;

/**
 * The blog course node configuration class
 * <P>
 * Initial Date: Mar 31, 2009 <br>
 * 
 * @author gwassmann
 */
public class BlogCourseNodeConfiguration extends AbstractCourseNodeConfiguration implements CourseNodeConfiguration {

	private static final String ICON_CSS_CLASS = "o_blog_icon";

	private BlogCourseNodeConfiguration() {
		super();
	}

	/**
	 * @see org.olat.course.nodes.CourseNodeConfiguration#getAlias()
	 */
	@Override
	public String getAlias() {
		return BlogCourseNode.TYPE;
	}

	/**
	 * @see org.olat.course.nodes.CourseNodeConfiguration#getIconCSSClass()
	 */
	@Override
	public String getIconCSSClass() {
		return ICON_CSS_CLASS;
	}

	/**
	 * @see org.olat.course.nodes.CourseNodeConfiguration#getInstance()
	 */
	@Override
	public CourseNode getInstance() {
		return new BlogCourseNode();
	}

	/**
	 * @see org.olat.course.nodes.CourseNodeConfiguration#getLinkCSSClass()
	 */
	@Override
	public String getLinkCSSClass() {
		// No particular styles
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNodeConfiguration#getLinkText(java.util.Locale)
	 */
	@Override
	public String getLinkText(final Locale locale) {
		final Translator fallback = Util.createPackageTranslator(CourseNodeConfiguration.class, locale);
		final Translator translator = Util.createPackageTranslator(this.getClass(), locale, fallback);
		return translator.translate("title_blog");
	}

	/**
	 * @see org.olat.core.extensions.OLATExtension#getExtensionCSS()
	 */
	public ExtensionResource getExtensionCSS() {
		return null;
	}

	/**
	 * @see org.olat.core.extensions.OLATExtension#getExtensionResources()
	 */
	public List getExtensionResources() {
		// TODO: What is this? No extensions so far.
		return null;
	}

	/**
	 * @see org.olat.core.extensions.OLATExtension#getName()
	 */
	public String getName() {
		return getAlias();
	}

}
