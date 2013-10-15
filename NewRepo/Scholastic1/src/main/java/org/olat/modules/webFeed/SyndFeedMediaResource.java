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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.olat.commons.servlets.RSSServlet;
import org.olat.core.gui.media.MediaResource;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * The media resource for synd feeds. Used for dispatching.
 * <P>
 * Initial Date: Mar 12, 2009 <br>
 * 
 * @author gwassmann
 */
public class SyndFeedMediaResource implements MediaResource {

	private final SyndFeed feed;
	private String feedString;
	private static final String CONTENT_TYPE = "application/rss+xml";

	public SyndFeedMediaResource(final SyndFeed feed) {
		this.feed = feed;

		feedString = null;
		try {
			final SyndFeedOutput output = new SyndFeedOutput();
			feedString = output.outputString(feed);
		} catch (final FeedException e) {
			// cannot convert feed to string or something
		}
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getContentType()
	 */
	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getInputStream()
	 */
	@Override
	public InputStream getInputStream() {
		ByteArrayInputStream inputStream = null;
		try {
			inputStream = new ByteArrayInputStream(feedString.getBytes(RSSServlet.DEFAULT_ENCODING));
		} catch (final UnsupportedEncodingException e) {
			// log something
		}
		return inputStream;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getLastModified()
	 */
	@Override
	public Long getLastModified() {
		Long lastModified = null;
		final Date date = feed.getPublishedDate();
		if (date != null) {
			lastModified = Long.valueOf(date.getTime());
		}
		return lastModified;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getSize()
	 */
	@Override
	public Long getSize() {
		return new Long(feedString.getBytes().length);
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#prepare(javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void prepare(@SuppressWarnings("unused") final HttpServletResponse hres) {
		// nothing to prepare
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#release()
	 */
	@Override
	public void release() {
		// nothing to release
	}

}
