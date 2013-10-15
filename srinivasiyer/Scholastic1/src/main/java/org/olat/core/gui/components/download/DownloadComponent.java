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
package org.olat.core.gui.components.download;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSMediaResource;

/**
 * Description:<br>
 * The download component displays a link which when pressed triggers a file download in a new window.
 * <P>
 * Initial Date: 09.12.2009 <br>
 * 
 * @author gnaegi
 */
public class DownloadComponent extends Component {
	private static final ComponentRenderer RENDERER = new DownloadComponentRenderer();
	private VFSMediaResource mediaResource;
	private String linkText;
	private String linkToolTip;
	private String linkCssIconClass;

	/**
	 * Constructor to create a download component that will use the file name as display text and the appropriate file icon
	 * 
	 * @param name
	 * @param downloadItem
	 */
	public DownloadComponent(String name, VFSLeaf downloadItem) {
		this(name, downloadItem, downloadItem.getName(), null, getCssIconClass(downloadItem.getName()));
	}

	/**
	 * Detailed constructor
	 * 
	 * @param name The component name
	 * @param downloadFile The VFS item to be downloaded
	 * @param linkText an optional link text
	 * @param linkToolTip an optional tool tip (hover text over link)
	 * @param linkCssIconClass an optional css icon class. Note that b_with_small_icon_left will be added when this argument is used. Use the render argument when you
	 *            want to provide additional CSS classes.
	 */
	public DownloadComponent(String name, VFSLeaf downloadItem, String linkText, String linkToolTip, String linkCssIconClass) {
		super(name);
		setDownloadItem(downloadItem);
		setLinkText(linkText);
		setLinkToolTip(linkToolTip);
		setLinkCssIconClass(linkCssIconClass);
	}

	/**
	 * @param downloadItem the VFS item to download
	 */
	public void setDownloadItem(VFSLeaf downloadItem) {
		if (downloadItem == null) {
			this.mediaResource = null;
		} else {
			this.mediaResource = new VFSMediaResource(downloadItem);
		}
		this.setDirty(true);
	}

	/**
	 * Package scope getter method for file download media resource
	 * 
	 * @return
	 */
	VFSMediaResource getDownloadMediaResoruce() {
		return this.mediaResource;
	}

	/**
	 * @return The optional link text or NULL to only display an icon
	 */
	public String getLinkText() {
		return linkText;
	}

	/**
	 * @param linkText
	 */
	public void setLinkText(String linkText) {
		this.linkText = linkText;
		this.setDirty(true);
	}

	/**
	 * @return The optional link tooltip or NULL if not available
	 */
	public String getLinkToolTip() {
		return linkToolTip;
	}

	/**
	 * @param linkToolTip The optional link tooltip or NULL if not available
	 */
	public void setLinkToolTip(String linkToolTip) {
		this.linkToolTip = linkToolTip;
		this.setDirty(true);
	}

	/**
	 * @return The link icon css class or NULL if no css should be used
	 */
	public String getLinkCssIconClass() {
		return linkCssIconClass;
	}

	/**
	 * @param linkCssIconClass The link icon css class or NULL if no css should be used. Note that b_with_small_icon_left will be added when this argument is used. Use
	 *            the render argument when you want to provide additional CSS classes.
	 */
	public void setLinkCssIconClass(String linkCssIconClass) {
		this.linkCssIconClass = linkCssIconClass;
		this.setDirty(true);
	}

	/**
	 * @see org.olat.core.gui.components.Component#doDispatchRequest(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void doDispatchRequest(UserRequest ureq) {
		if (this.mediaResource != null) {
			// ServletUtil.serveResource(ureq.getHttpReq(), ureq.getHttpResp(),
			// this.mediaResource);
			// Since downloaded in new window the link in the main window should
			// not get dirty - can be reused many times.
			ureq.getDispatchResult().setResultingMediaResource(mediaResource);
			setDirty(false);
		}
	}

	/**
	 * @see org.olat.core.gui.components.Component#getHTMLRendererSingleton()
	 */
	@Override
	public ComponentRenderer getHTMLRendererSingleton() {
		return RENDERER;
	}

	/**
	 * Helper method to create the css class for the given file type from brasato.css
	 * 
	 * @param fileName
	 * @return
	 */
	private static String getCssIconClass(String fileName) {
		int typePos = fileName.lastIndexOf(".");
		if (typePos > 0) { return "b_filetype_" + fileName.substring(typePos + 1); }
		return null;
	}

}
