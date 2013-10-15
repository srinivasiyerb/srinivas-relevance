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

package org.olat.modules.webFeed.portfolio;

import org.olat.fileresource.types.BlogFileResource;
import org.olat.modules.webFeed.managers.FeedManager;
import org.olat.modules.webFeed.models.Feed;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;

/**
 * Description:<br>
 * The LifeBlogArtefact integrated a full featured blog in a map.
 * <P>
 * Initial Date: 8 nov. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class LiveBlogArtefact extends AbstractArtefact {

	public static final String TYPE = "liveblog";

	@Override
	public String getResourceableTypeName() {
		return TYPE;
	}

	@Override
	public String getIcon() {
		return "o_blog_icon";
	}

	public Feed getFeedLight() {
		final String businessPath = getBusinessPath();
		final Long resid = Long.parseLong(businessPath.substring(10, businessPath.length() - 1));
		final OLATResource ores = OLATResourceManager.getInstance().findResourceable(resid, BlogFileResource.TYPE_NAME);
		return FeedManager.getInstance().getFeed(ores);
	}

}
