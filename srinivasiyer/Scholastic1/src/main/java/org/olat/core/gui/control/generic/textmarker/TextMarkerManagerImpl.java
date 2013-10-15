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

package org.olat.core.gui.control.generic.textmarker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.olat.core.gui.UserRequest;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.xml.XMLParser;

/**
 * Description: Implementation of the TextMarkerManager that uses SAX to persist the TextMarker objects FIXME: remove after all glossaries were migrated to new format.
 * 
 * @author gnaegi <www.goodsolutions.ch> Initial Date: Jul 14, 2006
 */
public class TextMarkerManagerImpl extends BasicManager implements TextMarkerManager {
	public static final String XML_ROOT_ELEMENT = "textMarkerList";
	public static final String XML_VERSION_ATTRIBUTE = "version";
	public static final int VERSION = 1;

	private static final TextMarkerManager INSTANCE = new TextMarkerManagerImpl();

	/**
	 * @return singleton instance
	 */
	public static TextMarkerManager getInstance() {
		return INSTANCE;
	}

	/**
	 * @see org.olat.core.gui.control.generic.textmarker.TextMarkerManager#loadTextMarkerList(org.olat.core.util.vfs.VFSLeaf)
	 */
	@Override
	public List<TextMarker> loadTextMarkerList(VFSLeaf textMarkerFile) {
		if (textMarkerFile == null) {
			// filename not defined at all
			return new ArrayList<TextMarker>();
		}
		XMLParser parser = new XMLParser();
		InputStream stream = textMarkerFile.getInputStream();
		if (stream == null) {
			// e.g. file was removed
			return new ArrayList<TextMarker>();
		}
		Document doc = parser.parse(stream, false);
		Element root = doc.getRootElement();
		if (root == null) {
			// file was empty;
			return new ArrayList<TextMarker>();
		}
		// Do version check. Not needed now, for future lazy migration code...
		Attribute versionAttribute = root.attribute(XML_VERSION_ATTRIBUTE);
		int version = (versionAttribute == null ? 1 : Integer.parseInt(versionAttribute.getStringValue()));
		if (version != VERSION) {
			// complain about version conflict or solve it
			throw new OLATRuntimeException("Could not load glossary entries due to version conflict. Loaded version was::" + version, null);
		}
		// parse text marker objects and put them into a list
		List markersElements = root.elements("textMarker");
		List<TextMarker> markers = new ArrayList<TextMarker>();
		Iterator iter = markersElements.iterator();
		while (iter.hasNext()) {
			Element textMarkerElement = (Element) iter.next();
			TextMarker textMarker = new TextMarker(textMarkerElement);
			markers.add(textMarker);
		}
		try {
			stream.close();
		} catch (IOException e) {
			throw new OLATRuntimeException(this.getClass(), "Error while closing text marker file stream", e);
		}
		return markers;
	}

	/**
	 * @see org.olat.core.gui.control.generic.textmarker.TextMarkerManager#saveToFile(org.olat.core.util.vfs.VFSLeaf, java.util.List)
	 */
	@Override
	public void saveToFile(VFSLeaf textMarkerFile, List textMarkerList) {
		DocumentFactory df = DocumentFactory.getInstance();
		Document doc = df.createDocument();
		// create root element with version information
		Element root = df.createElement(XML_ROOT_ELEMENT);
		root.addAttribute(XML_VERSION_ATTRIBUTE, String.valueOf(VERSION));
		doc.setRootElement(root);
		// add TextMarker elements
		Iterator iter = textMarkerList.iterator();
		while (iter.hasNext()) {
			TextMarker textMarker = (TextMarker) iter.next();
			textMarker.addToElement(root);
		}
		OutputStream stream = textMarkerFile.getOutputStream(false);
		try {
			XMLWriter writer = new XMLWriter(stream);
			writer.write(doc);
			writer.close();
			stream.close();
		} catch (UnsupportedEncodingException e) {
			Tracing.logError("Error while saving text marker file", e, TextMarkerManagerImpl.class);
		} catch (IOException e) {
			Tracing.logError("Error while saving text marker file", e, TextMarkerManagerImpl.class);
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.textmarker.TextMarkerManager#loadFileAsIndexableString(org.olat.core.util.vfs.VFSLeaf)
	 */
	@Override
	public String loadFileAsString(VFSLeaf textMarkerFile) {
		StringBuilder sb = new StringBuilder();
		List<TextMarker> markers = loadTextMarkerList(textMarkerFile);
		for (TextMarker marker : markers) {
			sb.append(marker.getMarkedText());
			sb.append("\n");
			sb.append(marker.getHooverText());
			sb.append("\n\n");
		}
		return sb.toString();
	}

	/**
	 * @see org.olat.core.gui.control.generic.textmarker.TextMarkerManager#isTextmarkingEnabled(org.olat.core.gui.UserRequest, org.olat.core.id.OLATResourceable)
	 */
	@Override
	public boolean isTextmarkingEnabled(UserRequest ureq, OLATResourceable ores) {
		if (ores != null) {
			// Glossary always on for guests. OLAT-4241
			if (ureq.getUserSession().getRoles().isGuestOnly()) { return true; }
			Object pref = ureq.getUserSession().getGuiPreferences().findPrefByKey("glossary.enabled.course." + ores.getResourceableId());
			if (pref != null) { return ((Boolean) pref).booleanValue(); }
			return false;
		}
		return false;
	}

}
