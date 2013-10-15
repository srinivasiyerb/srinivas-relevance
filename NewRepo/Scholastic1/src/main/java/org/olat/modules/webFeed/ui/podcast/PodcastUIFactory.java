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
package org.olat.modules.webFeed.ui.podcast;

import java.util.Locale;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.course.ICourse;
import org.olat.course.nodes.AbstractFeedCourseNode;
import org.olat.course.nodes.feed.podcast.PodcastNodeEditController;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.webFeed.models.Feed;
import org.olat.modules.webFeed.models.Item;
import org.olat.modules.webFeed.ui.FeedMainController;
import org.olat.modules.webFeed.ui.FeedUIFactory;
import org.olat.repository.controllers.IAddController;
import org.olat.repository.controllers.RepositoryAddCallback;

/**
 * UI factory for podcast controllers.
 * <P>
 * Initial Date: Jun 8, 2009 <br>
 * 
 * @author gwassmann
 */
public class PodcastUIFactory extends FeedUIFactory {

	private Translator translator;

	public PodcastUIFactory() {
		super();
	}

	private PodcastUIFactory(final Locale locale) {
		super();
		setTranslator(locale);
	}

	// TODO:GW comments (or refactor?)
	public static PodcastUIFactory getInstance(final Locale locale) {
		return new PodcastUIFactory(locale);
	}

	@Override
	public Translator getTranslator() {
		return translator;
	}

	/**
	 * @see org.olat.modules.webFeed.ui.FeedUIFactory#setTranslator(java.util.Locale)
	 */
	@Override
	public void setTranslator(final Locale locale) {
		final Translator fallbackTans = Util.createPackageTranslator(FeedMainController.class, locale);
		translator = Util.createPackageTranslator(PodcastUIFactory.class, locale, fallbackTans);
	}

	/**
	 * @see org.olat.modules.webFeed.ui.FeedUIFactory#createAddController(org.olat.repository.controllers.RepositoryAddCallback, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public IAddController createAddController(final RepositoryAddCallback addCallback, final UserRequest ureq, final WindowControl wControl) {
		return new CreatePodcastController(addCallback, ureq, wControl);
	}

	/**
	 * @see org.olat.modules.webFeed.FeedUIFactory#getInfoVelocityContainer()
	 */
	@Override
	public VelocityContainer createInfoVelocityContainer(final BasicController controller) {
		return new VelocityContainer(VC_INFO_NAME, this.getClass(), VC_INFO_NAME, translator, controller);
	}

	/**
	 * @see org.olat.modules.webFeed.FeedUIFactory#getInfoVelocityContainer()
	 */
	@Override
	public VelocityContainer createItemsVelocityContainer(final BasicController controller) {
		return new VelocityContainer(VC_ITEMS_NAME, this.getClass(), "episodes", translator, controller);
	}

	/**
	 * @see org.olat.modules.webFeed.ui.FeedUIFactory#createItemVelocityContainer(org.olat.core.gui.control.controller.BasicController)
	 */
	@Override
	public VelocityContainer createItemVelocityContainer(final BasicController controller) {
		return new VelocityContainer(VC_ITEM_NAME, this.getClass(), "episode", translator, controller);
	}

	/**
	 * @see org.olat.modules.webFeed.ui.FeedUIFactory#createRightColumnVelocityContainer(org.olat.core.gui.control.controller.BasicController)
	 */
	@Override
	public VelocityContainer createRightColumnVelocityContainer(final BasicController controller) {
		return new VelocityContainer(VC_RIGHT_NAME, this.getClass(), VC_RIGHT_NAME, translator, controller);
	}

	/**
	 * @see org.olat.modules.webFeed.ui.FeedUIFactory#createItemFormController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl,
	 *      org.olat.modules.webFeed.models.Item, org.olat.modules.webFeed.models.Feed)
	 */
	@Override
	public FormBasicController createItemFormController(final UserRequest ureq, final WindowControl wControl, final Item item, final Feed feed) {
		return new EpisodeFormController(ureq, wControl, item, feed, getTranslator());
	}

	/**
	 * @see org.olat.modules.webFeed.ui.FeedUIFactory#createNodeEditController(org.olat.course.nodes.AbstractFeedCourseNode, org.olat.course.ICourse,
	 *      org.olat.course.run.userview.UserCourseEnvironment, org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public TabbableController createNodeEditController(final AbstractFeedCourseNode courseNode, final ICourse course, final UserCourseEnvironment uce,
			final UserRequest ureq, final WindowControl control) {
		return new PodcastNodeEditController(courseNode, course, uce, ureq, control);
	}
}
