/**
 * BPS Bildungsportal Sachsen GmbH<br>
 * Bahnhofstrasse 6<br>
 * 09111 Chemnitz<br>
 * Germany<br>
 * Copyright (c) 2005-2008 by BPS Bildungsportal Sachsen GmbH<br>
 * http://www.bps-system.de<br>
 * All rights reserved.
 */

package de.bps.onyx.plugin;

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.olat.core.gui.media.MediaResource;

/**
 * @author Ingmar Kroll
 */

public class StreamMediaResource implements MediaResource {

	private final InputStream is;
	private final String fileName;
	private final Long size;
	private final Long lastModified;

	/**
	 * file assumed to exist, but if it does not exist or cannot be read, getInputStream() will return null and the class will behave properly.
	 * 
	 * @param file
	 */
	public StreamMediaResource(final InputStream is, final String fileName, final Long size, final Long lastModified) {
		this.is = is;
		this.fileName = fileName;
		this.size = size;
		this.lastModified = lastModified;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getContentType()
	 */
	@Override
	public String getContentType() {
		return "application/octet-stream";
	}

	/**
	 * @return
	 * @see org.olat.core.gui.media.MediaRequest#getSize()
	 */
	@Override
	public Long getSize() {
		return size;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getInputStream()
	 */
	@Override
	public InputStream getInputStream() {
		return is;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#getLastModified()
	 */
	@Override
	public Long getLastModified() {
		return lastModified;
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#release()
	 */
	@Override
	public void release() {
		// void
	}

	/**
	 * @see org.olat.core.gui.media.MediaResource#prepare(javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void prepare(final HttpServletResponse hres) {
		hres.setHeader("Content-Disposition", "attachment; filename=" + this.fileName);
	}

}
