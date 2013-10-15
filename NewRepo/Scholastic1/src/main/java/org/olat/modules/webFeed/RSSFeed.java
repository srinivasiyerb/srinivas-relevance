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
package org.olat.modules.webFeed;

import java.util.ArrayList;
import java.util.List;

import org.olat.commons.servlets.RSSServlet;
import org.olat.core.id.Identity;
import org.olat.core.util.filter.FilterFactory;
import org.olat.modules.webFeed.models.Enclosure;
import org.olat.modules.webFeed.models.Feed;
import org.olat.modules.webFeed.models.Item;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.feed.synd.SyndImageImpl;

/**
 * Creates a podcast feed (syndication feed) from a podcast resource.
 * <P>
 * Initial Date: Feb 25, 2009 <br>
 * 
 * @author Gregor Wassmann
 */
public class RSSFeed extends SyndFeedImpl {

	/**
	 * Constructor. The identityKey is needed to generate personal URLs for the corresponding user.
	 */
	public RSSFeed(final Feed feed, final Identity identity, final Long courseId, final String nodeId) {
		super();

		// This helper object is required for generating the appropriate URLs for
		// the given user (identity)
		final FeedViewHelper helper = new FeedViewHelper(feed, identity, courseId, nodeId);

		setFeedType("rss_2.0");
		setEncoding(RSSServlet.DEFAULT_ENCODING);
		setTitle(feed.getTitle());
		// According to the rss specification, the feed channel description is not
		// (explicitly) allowed to contain html tags.
		String strippedDescription = FilterFactory.getHtmlTagsFilter().filter(feed.getDescription());
		strippedDescription = strippedDescription.replaceAll("&nbsp;", " "); // TODO: remove when filter
		// does it
		setDescription(strippedDescription);
		setLink(helper.getJumpInLink());

		setPublishedDate(feed.getLastModified());
		// The image
		if (feed.getImageName() != null) {
			final SyndImage image = new SyndImageImpl();
			image.setDescription(feed.getDescription());
			image.setTitle(feed.getTitle());
			image.setLink(getLink());
			image.setUrl(helper.getImageUrl());
			setImage(image);
		}

		final List<SyndEntry> episodes = new ArrayList<SyndEntry>();
		for (final Item item : feed.getPublishedItems()) {
			final SyndEntry entry = new SyndEntryImpl();
			entry.setTitle(item.getTitle());

			final SyndContent itemDescription = new SyndContentImpl();
			itemDescription.setType("text/plain");
			itemDescription.setValue(helper.getItemDescriptionForBrowser(item));
			entry.setDescription(itemDescription);

			// Link will also be converted to the rss guid tag. Except if there's an
			// enclosure, then the enclosure url is used.
			// Use jump-in link far all entries. This will be overriden if the item
			// has an enclosure.
			entry.setLink(helper.getJumpInLink() + "#" + item.getGuid());
			entry.setPublishedDate(item.getPublishDate());
			entry.setUpdatedDate(item.getLastModified());

			// The enclosure is the media (audio or video) file of the episode
			final Enclosure media = item.getEnclosure();
			if (media != null) {
				final SyndEnclosure enclosure = new SyndEnclosureImpl();
				enclosure.setUrl(helper.getMediaUrl(item));
				enclosure.setType(media.getType());
				enclosure.setLength(media.getLength());
				// Also set the item link to point to the enclosure
				entry.setLink(helper.getMediaUrl(item));
				final List<SyndEnclosure> enclosures = new ArrayList<SyndEnclosure>();
				enclosures.add(enclosure);
				entry.setEnclosures(enclosures);
			}

			episodes.add(entry);
		}
		setEntries(episodes);
	}
}
