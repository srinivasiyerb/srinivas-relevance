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
 * Copyright (c) 1999-2007 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.ims.cp.objects;

import org.dom4j.tree.DefaultElement;
import org.olat.ims.cp.CPCore;

/**
 * Description:<br>
 * This class represents a metadata-element in a IMS-manifest-file
 * <P>
 * Initial Date: 02.07.2008 <br>
 * 
 * @author Sergio Trentini
 */
public class CPMetadata extends DefaultElement implements CPNode {

	public static final String STATUS_DRAFT = "s_0";
	public static final String STATUS_FINAL = "s_1";
	public static final String STATUS_REVISED = "s_2";
	public static final String STATUS_UNAVAILABLE = "s_3";

	private int position;
	private DefaultElement parent;

	public CPMetadata(final DefaultElement me) {
		super(me.getName());
		setContent(me.content());
		// TODO: parse xml to LOM-Object!
	}

	/**
	 * constructor to generate an empty metadata-element
	 */
	public CPMetadata() {
		super(CPCore.METADATA);
	}

	@Override
	public void buildChildren() {
		validateElement();
	}

	@Override
	public boolean validateElement() {
		return true;
	}

	/**
	 * @see org.olat.ims.cp.objects.CPNode#buildDocument(org.dom4j.tree.DefaultElement)
	 */
	@Override
	public void buildDocument(final DefaultElement parent) {
		final DefaultElement metaElement = new DefaultElement(CPCore.METADATA);
		metaElement.setContent(this.content());
		parent.add(metaElement);
	}

	/**
	 * removes this metadata-element from the manfifest
	 */
	public void removeFromManifest() {
		final DefaultElement p = getParentElement();

		if (p instanceof CPManifest) {
			final CPManifest mani = (CPManifest) p;
			mani.setMetadata(null);
		} else if (p instanceof CPOrganization) {
			final CPOrganization org = (CPOrganization) p;
			org.setMetadata(null);
		} else if (p instanceof CPItem) {
			final CPItem item = (CPItem) p;
			item.setMetadata(null);
		} else if (p instanceof CPResource) {
			final CPResource res = (CPResource) p;
			res.setMetadata(null);
		} else if (p instanceof CPFile) {
			final CPFile res = (CPFile) p;
			res.setMetadata(null);
		}
	}

	// *** GETTERS ***

	@Override
	public DefaultElement getElementByIdentifier(final String id) {
		// <metadata>-elements do not have an identifier and do not have children...
		return null;
	}

	@Override
	public int getPosition() {
		return position;
	}

	public DefaultElement getParentElement() {
		return parent;
	}

	public String getTitle() {
		return "";
	}

	/**
	 * Returns the description
	 * 
	 * @return the description of this metadata
	 */
	public String getDescription() {
		return "";
	}

	/**
	 * Returns the keywords as a comma-seperated string
	 * 
	 * @return the comma-seperated keywords
	 */
	public String getKeywords() {
		// not supported
		return "";
	}

	public String getVersion() {
		return "";
	}

	public String getStatus() {
		return "";
	}

	public String getFormat() {
		return "";
	}

	public String getAuthor() {
		return "";
	}

	public String getDate() {
		return "";
	}

	// *** SETTERS ***

	@Override
	public void setPosition(final int pos) {
		position = pos;
	}

	public void setParentElement(final DefaultElement parent) {
		this.parent = parent;
	}

	/**
	 * Setter for the LOMImpl-object
	 */
	protected void setLOM() {
		// not supported
	}

	public void setTitle(final String val) {
		// not supported
	}

	public void setDescription(final String val) {
		// not supported
	}

	public void setKeywords(final String val) {
		// not supported
	}

	public void setVersion(final String val) {
		// not supported
	}

	public void setStatus(final String val) {
		// not supported
	}

	public void setFormat(final String val) {
		// not supported
	}

	public void setAuthor(final String val) {
		// not supported
	}

	public void setDate(final String val) {
		// not supported
	}

}