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

package org.olat.core.gui.media;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.olat.core.logging.Tracing;

/**
 * @author Felix Jost
 */
public class RedirectMediaResource implements MediaResource {

	private String redirectURL;

	/**
	 * @param redirectURL
	 */
	public RedirectMediaResource(String redirectURL) {
		this.redirectURL = redirectURL;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getContentType()
	 */
	@Override
	public String getContentType() {
		return null;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getSize()
	 */
	@Override
	public Long getSize() {
		return null;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getInputStream()
	 */
	@Override
	public InputStream getInputStream() {
		return null;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getLastModified()
	 */
	@Override
	public Long getLastModified() {
		return null;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#prepare(javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void prepare(HttpServletResponse hres) {
		try {
			hres.sendRedirect(redirectURL);
		} catch (IOException e) {
			// if redirect failed, we do nothing; the browser may have stopped the
			// tcp/ip or whatever
			Tracing.logError("redirect failed: url=" + redirectURL, e, RedirectMediaResource.class);
		} catch (IllegalStateException ise) {
			// redirect failed, to find out more about the strange null null exception
			// FIXME:pb:a decide if this catch has to be removed again, after finding problem.
			Tracing.logError("redirect failed: url=" + redirectURL, ise, RedirectMediaResource.class);
			// introduced only more debug information but behavior is still the same
			throw (ise);
		}
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#release()
	 */
	@Override
	public void release() {
		// nothing to do
	}

}