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

import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dom4j.Namespace;
import org.dom4j.tree.DefaultAttribute;
import org.dom4j.tree.DefaultDocument;
import org.dom4j.tree.DefaultElement;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.ims.cp.CPCore;

/**
 * Description:<br>
 * This class represents a manifest-element of a IMS-manifest-file it is the root element of a ContentPackage
 * <P>
 * Initial Date: 07.07.2008 <br>
 * 
 * @author sergio
 */
public class CPManifest extends DefaultElement implements CPNode {

	private static final String DEFAULT_SCHEMALOC = "http://www.imsglobal.org/xsd/imscp_v1p1 imscp_v1p1.xsd http://www.imsglobal.org/xsd/imsmd_v1p2 imsmd_v1p2p2.xsd";
	private static final String DEFAULT_NMS = "http://www.imsglobal.org/xsd/imsmd_v1p2";
	private CPOrganizations organizations;
	private CPResources resources;
	private final String identifier;
	private final String schemaLocation;
	private CPCore cp;
	private CPMetadata metadata;

	private final Logger log;

	private final Vector<String> errors;

	/**
	 * this constructor is used when building up the cp (parsing XML)
	 * 
	 * @param me
	 */
	public CPManifest(final CPCore cp, final DefaultElement me) {
		super(me.getName());
		log = Logger.getLogger(CPManifest.class);
		errors = new Vector<String>();
		this.identifier = me.attributeValue(CPCore.IDENTIFIER);
		this.schemaLocation = me.attributeValue(CPCore.SCHEMALOCATION);
		this.setNamespace(me.getNamespace());
		this.cp = cp;
		// FIXME: namespaces ! xmlns
		setContent(me.content());
	}

	/**
	 * This constructor is used when creating a new CP
	 * 
	 * @param cp the cpcore to which this manifest belongs
	 * @param identifier the identifier of the manifest
	 */
	public CPManifest(final CPCore cp, final String identifier) {
		super(CPCore.MANIFEST);
		log = Logger.getLogger(CPManifest.class);
		this.identifier = identifier;
		schemaLocation = CPManifest.DEFAULT_SCHEMALOC;
		setNamespace(new Namespace("imsmd", DEFAULT_NMS));
		organizations = new CPOrganizations();
		resources = new CPResources();
		errors = new Vector<String>();
		this.cp = cp;
	}

	public CPManifest(final CPCore cp) {
		this(cp, CodeHelper.getGlobalForeverUniqueID());
	}

	/**
	 * @see org.olat.ims.cp.objects.CPNode#buildChildren()
	 */
	@Override
	public void buildChildren() {
		final Iterator<DefaultElement> children = this.elementIterator();
		boolean organizationsAdded = false;
		boolean resourcesAdded = false;

		while (children.hasNext()) {
			final DefaultElement child = children.next();
			if (child.getName().equals(CPCore.ORGANIZATIONS)) {
				if (organizationsAdded) {
					errors.add("Invalid IMS-Manifest ( only one <organizations> element is allowed )");
				}

				final CPOrganizations org = new CPOrganizations(child);
				org.buildChildren();
				org.setParentElement(this);
				organizations = org;
				organizationsAdded = true;
			} else if (child.getName().equals(CPCore.RESOURCES)) {
				if (resourcesAdded) {
					errors.add("Invalid IMS-Manifest ( only one <resources> element is allowed )");
				}

				final CPResources res = new CPResources(child);
				res.setParentElement(this);
				res.buildChildren();
				resources = res;
				resourcesAdded = true;
			} else if (child.getName().equals(CPCore.METADATA)) {
				// TODO: implement LOM METADATA
				metadata = new CPMetadata(child);
				metadata.setParentElement(this);
			}
		}

		this.clearContent();
		validateElement();
	}

	/**
	 * checks whether required child-elements are present
	 */
	@Override
	public boolean validateElement() {
		if (this.organizations == null) {
			errors.add("Invalid IMS-Manifest ( missing <organizations> element )");
			return false;
		}
		if (this.resources == null) {
			errors.add("Invalid IMS-Manifest ( missing <resurces> element )");
			return false;
		}

		// just to check on duplicate identifiers..
		getAllIdentifiers();

		return true;
	}

	/**
	 * returns a vector which holds all identifiers that occur in the manifest
	 * 
	 * @return
	 */
	public Vector<String> getAllIdentifiers() {
		final Vector<String> ids = new Vector<String>();

		for (final Iterator<CPOrganization> it = organizations.getOrganizationIterator(); it.hasNext();) {
			final CPOrganization org = it.next();
			ids.add(org.getIdentifier());

			final Vector<CPItem> allItems = new Vector<CPItem>();
			for (final Iterator<CPItem> itemIt = org.getItemIterator(); itemIt.hasNext();) {
				final CPItem item = itemIt.next();
				allItems.addAll(item.getAllItems());
			}

			for (final CPItem item : allItems) {
				if (!ids.contains(item.getIdentifier())) {
					ids.add(item.getIdentifier());
				} else {
					errors.add("Invalid IMS-Manifest ( duplicate identifier " + item.getIdentifier() + " )");
				}
			}
		}

		for (final Iterator<CPResource> resIt = resources.getResourceIterator(); resIt.hasNext();) {
			final CPResource res = resIt.next();
			if (!ids.contains(res.getIdentifier())) {
				ids.add(res.getIdentifier());
			} else {
				errors.add("Invalid IMS-Manifest ( duplicate identifier " + res.getIdentifier() + " )");
			}
		}

		return ids;
	}

	/**
	 * @param doc
	 */
	public void buildDocument(final DefaultDocument doc) {
		// Manifest is the root-node of the document, therefore we need to pass the
		// "doc"
		final DefaultElement manifestElement = new DefaultElement(CPCore.MANIFEST);

		manifestElement.add(new DefaultAttribute(CPCore.IDENTIFIER, this.identifier));
		manifestElement.add(new DefaultAttribute(CPCore.SCHEMALOCATION, this.schemaLocation));
		// manifestElement.setNamespace(this.getNamespace()); //FIXME: namespace

		doc.add(manifestElement);

		if (metadata != null) {
			metadata.buildDocument(manifestElement);
		}
		organizations.buildDocument(manifestElement);
		resources.buildDocument(manifestElement);

	}

	/**
	 * @see org.olat.ims.cp.objects.CPNode#buildDocument(org.dom4j.tree.DefaultElement)
	 */
	@Override
	public void buildDocument(final DefaultElement parent) {
		// because the Manifest is the root-element of the document, we need "public
		// void buildDocument(DefaultDocument doc)" instead...
	}

	// *** getters ***

	public CPOrganizations getOrganizations() {
		return organizations;
	}

	public CPResources getResources() {
		return resources;
	}

	public String getIdentifier() {
		return identifier;
	}

	public DefaultElement getMetadata() {
		return metadata;
	}

	/**
	 * @see org.olat.ims.cp.objects.CPNode#getElementByIdentifier(java.lang.String)
	 */
	@Override
	public DefaultElement getElementByIdentifier(final String id) {
		if (id.equals(identifier)) { return this; }
		if (id.equals(CPCore.ORGANIZATIONS)) { return organizations; }

		DefaultElement e = organizations.getElementByIdentifier(id);
		if (e != null) { return e; }
		e = resources.getElementByIdentifier(id);

		if (e == null) {
			log.info("Element with id \"" + id + "\" not found in manifest!");
		}
		return e;
	}

	@Override
	public int getPosition() {
		// there is only one <manifest> element
		return 0;
	}

	public CPCore getCP() {
		return cp;
	}

	public VFSContainer getRootDir() {
		return cp.getRootDir();
	}

	public String getLastError() {
		if (errors.size() == 0) {
			return organizations.getLastError();
		} else {
			return errors.lastElement();
		}

	}

	// *** SETTERS ***

	@Override
	public void setPosition(final int pos) {
		// there is only one <manifest> element
	}

	public void setMetadata(final CPMetadata md) {
		this.metadata = md;
	}

	public void setCP(final CPCore cpcore) {
		cp = cpcore;
	}

}
