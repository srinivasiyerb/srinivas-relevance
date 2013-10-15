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

package org.olat.course.nodes.portfolio;

import java.util.Locale;

import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.course.nodes.AbstractCourseNodeConfiguration;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.CourseNodeConfiguration;
import org.olat.course.nodes.PortfolioCourseNode;

/**
 * Description:<br>
 * TODO: srosse Class Description for PortfolioCourseNodeConfiguration
 * <P>
 * Initial Date: 6 oct. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class PortfolioCourseNodeConfiguration extends AbstractCourseNodeConfiguration implements CourseNodeConfiguration {

	public static final String MAP_KEY = "map-key";
	public static final String REPO_SOFT_KEY = "repo-soft-key";
	public static final String NODE_TEXT = "node_text";
	public static final String DEADLINE_MONTH = "deadline_month";
	public static final String DEADLINE_WEEK = "deadline_week";
	public static final String DEADLINE_DAY = "deadline_day";
	public static final String DEADLINE_DATE = "deadline_date";
	public static final String DEADLINE_TYPE = "deadline_type";

	public enum DeadlineType {
		none, absolut, relative
	}

	private PortfolioCourseNodeConfiguration() {
		super();
	}

	@Override
	public String getAlias() {
		return "ep";
	}

	@Override
	public CourseNode getInstance() {
		return new PortfolioCourseNode();
	}

	@Override
	public String getLinkText(final Locale locale) {
		final Translator fallback = Util.createPackageTranslator(CourseNodeConfiguration.class, locale);
		final Translator translator = Util.createPackageTranslator(this.getClass(), locale, fallback);
		return translator.translate("title_info");
	}

	@Override
	public String getIconCSSClass() {
		return "o_ep_icon";
	}

	@Override
	public String getLinkCSSClass() {
		return null;
	}

}
