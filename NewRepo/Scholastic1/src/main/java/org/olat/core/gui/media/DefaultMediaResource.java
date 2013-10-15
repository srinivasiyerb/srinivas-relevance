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

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Felix Jost
 */
public class DefaultMediaResource implements MediaResource {

	private String contentType;
	private Long size;
	private InputStream inputStream;
	private Long lastModified;

	/**
	 * @see org.olat.core.gui.media.MediaResource#getContentType()
	 */
	@Override
	public String getContentType() {
		return contentType;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getInputStream()
	 */
	@Override
	public InputStream getInputStream() {
		return inputStream;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getLastModified()
	 */
	@Override
	public Long getLastModified() {
		return lastModified;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getSize()
	 */
	@Override
	public Long getSize() {
		return size;
	}

	/**
	 * @param contentType
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * @param inputStream
	 */
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	/**
	 * @param lastModified
	 */
	public void setLastModified(Long lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * @param size
	 */
	public void setSize(Long size) {
		this.size = size;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#prepare(javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void prepare(HttpServletResponse hres) {
		//
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#release()
	 */
	@Override
	public void release() {
		//
	}

}