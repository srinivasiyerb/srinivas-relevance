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
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.fileresource.types;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.XPath;
import org.olat.ims.resources.IMSLoader;

/**
 * Initial Date: Apr 6, 2004
 * 
 * @author Mike Stock
 */
public class ScormCPFileResource extends FileResource {

	/**
	 * SCORM IMS CP file resource identifier.
	 */
	public static final String TYPE_NAME = "FileResource.SCORMCP";

	/**
	 * Standard constructor.
	 */
	public ScormCPFileResource() {
		super.setTypeName(TYPE_NAME);
	}

	/**
	 * Check for title and at least one resource.
	 * 
	 * @param unzippedDir
	 * @return True if is of type.
	 */
	public static boolean validate(final File unzippedDir) throws AddingResourceException {
		final File fManifest = new File(unzippedDir, "imsmanifest.xml");
		final Document doc = IMSLoader.loadIMSDocument(fManifest);
		// do not throw exception already here, as it might be only a generic zip file
		if (doc == null) { return false; }

		String adluri = null;
		String seqencingUri = null;
		String simpleSeqencingUri = null;
		// get all organization elements. need to set namespace
		final Element rootElement = doc.getRootElement();
		final String nsuri = rootElement.getNamespace().getURI();
		// look for the adl cp namespace that differs a scorm package from a normal cp package
		final Namespace nsADL = rootElement.getNamespaceForPrefix("adlcp");
		if (nsADL != null) {
			adluri = nsADL.getURI();
		}
		final Namespace nsADLSeq = rootElement.getNamespaceForPrefix("adlseq");
		if (nsADLSeq != null) {
			seqencingUri = nsADLSeq.getURI();
		}
		final Namespace nsADLSS = rootElement.getNamespaceForPrefix("imsss");
		if (nsADLSS != null) {
			simpleSeqencingUri = nsADLSS.getURI();
		}
		// we can only support scorm 1.2 so far.
		if (adluri != null && !((adluri.indexOf("adlcp_rootv1p2") != -1) || (adluri.indexOf("adlcp_rootv1p3") != -1))) {
			// we dont have have scorm 1.2 or 1.3 namespace so it can't be a scorm package
			throw new AddingResourceException("scorm.no.scorm.namespace");
		}

		final Map nsuris = new HashMap(5);
		nsuris.put("ns", nsuri);
		nsuris.put("adluri", adluri);
		// we might have a scorm 2004 which we do not yet support
		if (seqencingUri != null) {
			nsuris.put("adlseq", seqencingUri);
		}
		if (simpleSeqencingUri != null) {
			nsuris.put("imsss", simpleSeqencingUri);
		}

		// Check for organiztaion element. Must provide at least one... title gets ectracted from either
		// the (optional) <title> element or the mandatory identifier attribute.
		// This makes sure, at least a root node gets created in CPManifestTreeModel.
		final XPath meta = rootElement.createXPath("//ns:organization");
		meta.setNamespaceURIs(nsuris);
		final Element orgaEl = (Element) meta.selectSingleNode(rootElement); // TODO: accept several organizations?
		if (orgaEl == null) { throw new AddingResourceException("resource.no.organisation"); }

		// Check for at least one <item> element referencing a <resource> of adlcp:scormtype="sco" or "asset",
		// which will serve as an entry point.
		final XPath resourcesXPath = rootElement.createXPath("//ns:resources");
		resourcesXPath.setNamespaceURIs(nsuris);
		final Element elResources = (Element) resourcesXPath.selectSingleNode(rootElement);
		if (elResources == null) { throw new AddingResourceException("resource.no.resource"); // no <resources> element.
		}
		final XPath itemsXPath = rootElement.createXPath("//ns:item");
		itemsXPath.setNamespaceURIs(nsuris);
		final List items = itemsXPath.selectNodes(rootElement);
		if (items.size() == 0) { throw new AddingResourceException("scorm.no.item"); // no <item> element.
		}

		// check for scorm 2004 simple sequencing stuff which we do not yet support
		if (seqencingUri != null) {
			final XPath seqencingXPath = rootElement.createXPath("//ns:imsss");
			final List sequences = seqencingXPath.selectNodes(rootElement);
			if (sequences.size() > 0) { throw new AddingResourceException("scorm.found.seqencing"); // seqencing elements found -> scorm 2004
			}
		}

		final Set set = new HashSet();
		for (final Iterator iter = items.iterator(); iter.hasNext();) {
			final Element item = (Element) iter.next();
			final String identifier = item.attributeValue("identifier");
			// check if identifiers are unique, reject if not so
			if (!set.add(identifier)) { throw new AddingResourceException("resource.general.error");// TODO:create special error message for non unique ids
			}
		}

		for (final Iterator iter = items.iterator(); iter.hasNext();) {
			final Element item = (Element) iter.next();
			final String identifierref = item.attributeValue("identifierref");
			if (identifierref == null) {
				continue;
			}
			final XPath resourceXPath = rootElement.createXPath("//ns:resource[@identifier='" + identifierref + "']");
			resourceXPath.setNamespaceURIs(nsuris);
			final Element elResource = (Element) resourceXPath.selectSingleNode(elResources);
			if (elResource == null) { throw new AddingResourceException("resource.no.matching.resource"); }
			// check for scorm attribute
			final Attribute scormAttr = elResource.attribute("scormtype");
			// some packages have attribute written like "scormType"
			final Attribute scormAttrUpper = elResource.attribute("scormType");
			if (scormAttr == null && scormAttrUpper == null) { throw new AddingResourceException("scorm.no.attribute.scormtype"); }
			String attr = "";
			if (scormAttr != null) {
				attr = scormAttr.getStringValue();
			}
			if (scormAttrUpper != null) {
				attr = scormAttrUpper.getStringValue();
			}
			if (attr == null) { throw new AddingResourceException("scorm.no.attribute.value"); }
			if (elResource.attributeValue("href") != null && (attr.equalsIgnoreCase("sco") || attr.equalsIgnoreCase("asset"))) { return true; // success.
			}
		}
		throw new AddingResourceException("resource.general.error");
	}
}
