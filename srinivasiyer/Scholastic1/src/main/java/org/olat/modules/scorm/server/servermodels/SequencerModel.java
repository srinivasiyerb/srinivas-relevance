/**
 * RELOAD TOOLS Copyright (c) 2003 Oleg Liber, Bill Olivier, Phillip Beauvoir, Paul Sharples Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to
 * the following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE
 * IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. Project Management Contact: Oleg Liber Bolton
 * Institute of Higher Education Deane Road Bolton BL3 5AB UK e-mail: o.liber@bolton.ac.uk Technical Contact: Phillip Beauvoir e-mail: p.beauvoir@bolton.ac.uk Paul
 * Sharples e-mail: p.sharples@bolton.ac.uk Web: http://www.reload.ac.uk
 */
package org.olat.modules.scorm.server.servermodels;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Parent;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.Tracing;
import org.olat.modules.scorm.ISettingsHandler;

import uk.ac.reload.jdom.XMLDocument;

/**
 * This class is responsible for creating the xml file which persists the current state of the package
 * 
 * @author Paul Sharples
 */
public class SequencerModel extends XMLDocument {
	protected static final String ROOT_NODE_NAME = "navigation";
	protected static final String ORG_NODE = "organization";
	protected static final String ITEM_NODE = "item";
	protected static final String ITEM_IDENTIFIER = "id";
	/**
	 * Comment for <code>ITEM_NOT_ATTEMPTED</code>
	 */
	public static final String ITEM_NOT_ATTEMPTED = "not attempted";
	/**
	 * Comment for <code>ITEM_COMPLETED</code>
	 */
	public static final String ITEM_COMPLETED = "completed";
	protected static final String ITEM_INCOMPLETE = "incomplete";
	/**
	 * Comment for <code>ITEM_PASSED</code>
	 */
	public static final String ITEM_PASSED = "passed";
	protected static final String ITEM_FAILED = "failed";
	protected static final String MANIFEST_MODIFIED = "manifest_last_modified";

	/**
	 * A vector used to house which items have been added to the navigation - used to determine which nodes have been deleted since last import
	 */
	Vector _items = new Vector();

	/**
	 * Our unique signature
	 */
	static final String[] scorm_comments = { "This is a version SCORM 1.2 Sequencer Model", "Spawned from Reload Scorm Player - http://www.reload.ac.uk" };

	/**
	 * Constructor takes a file (the nav xml file ref) and will try to load it either as new file or an existing file
	 * 
	 * @param file
	 */
	public SequencerModel(final File file, final ISettingsHandler settings) {
		if (!file.exists()) {
			super.setFile(file);
			init();
		} else {
			try {
				super.loadDocument(file);
			} catch (final JDOMException ex) {
				throw new OLATRuntimeException(this.getClass(), "JDOM Exception trying to load sequencer model", ex);
			} catch (final IOException ex) {
				throw new OLATRuntimeException(this.getClass(), "Could not load sequencer model", ex);
			}
		}
	}

	/**
	 * @param orgName
	 */
	public void setDefaultOrg(final String orgName) {
		getDocument().getRootElement().setAttribute("default", orgName);
	}

	/**
	 * @return the default organisation
	 */
	public String getDefaultOrg() {
		return getDocument().getRootElement().getAttributeValue("default");
	}

	/**
	 * @param lastModified
	 */
	public void setManifestModifiedDate(final long lastModified) {
		final Element root = getDocument().getRootElement();
		if (root.getChild(MANIFEST_MODIFIED) == null) {
			final Element time = new Element(MANIFEST_MODIFIED);
			time.setText(Long.toString(lastModified));
			root.addContent(time);
		} else {
			root.getChild(MANIFEST_MODIFIED).setText(Long.toString(lastModified));
		}
	}

	/**
	 * @return the modification date
	 */
	public String getManifestModifiedDate() {
		return getDocument().getRootElement().getChild(MANIFEST_MODIFIED).getText();
	}

	/**
	 * Method to return all of the items found during a parse of the manifest
	 * 
	 * @return all items of the manifest
	 */
	protected String[] getItems() {
		final String[] legitimateIds = new String[_items.size()];
		_items.copyInto(legitimateIds);
		return legitimateIds;
	}

	/**
	 * Method to commit the navigation xml file to disk
	 * 
	 * @param sco
	 * @param status
	 */
	public void updateDiskModel(final String sco, final String status) {
		final List itemList = getDocument().getRootElement().getChildren(ITEM_NODE);
		final Iterator itemListElement = itemList.iterator();
		while (itemListElement.hasNext()) {
			final Element anItem = (Element) itemListElement.next();
			if (anItem.getAttributeValue(ITEM_IDENTIFIER).equals(sco)) {
				anItem.setText(status);
			}
		}
		try {
			saveDocument();
		} catch (final IOException ex) {
			throw new OLATRuntimeException(this.getClass(), "could not save sequencer model.", ex);
		}
	}

	/**
	 * Method to get all item identifiers under a given organization
	 * 
	 * @param org
	 * @return - has table with idenitifer and current status
	 */
	public Hashtable getItemsAsHash(final String org) {
		final Hashtable hash = new Hashtable();
		final List itemList = getDocument().getRootElement().getChildren(ITEM_NODE);
		final Iterator itemListElement = itemList.iterator();
		while (itemListElement.hasNext()) {
			final Element anItem = (Element) itemListElement.next();
			hash.put(anItem.getAttributeValue(ITEM_IDENTIFIER), anItem.getText());
		}
		return hash;
	}

	/**
	 * Initilise this JDOM doc - adding comment and setting root node
	 */
	protected void init() {
		Document _model;
		final Element root = new Element(ROOT_NODE_NAME);
		_model = new Document(root);
		for (int i = 0; i < scorm_comments.length; i++) {
			final Comment comment = new Comment(scorm_comments[i]);
			_model.getContent().add(0, comment);
		}
		this.setDocument(_model);
	}

	/**
	 * Method to add a new item (or update an existing item) to the navigation file - used to persist package status
	 * 
	 * @param itemId
	 * @param orgId
	 * @param value
	 */
	public void addTrackedItem(final String itemId, final String orgId, final String value) {
		boolean itemFound = false;

		// check to see if the item is there already, if so reset it
		final List itemList = getDocument().getRootElement().getChildren(ITEM_NODE);
		if (itemList != null && !itemList.isEmpty()) {
			final Iterator itemListElement = itemList.iterator();
			while (itemListElement.hasNext()) {
				final Element anItem = (Element) itemListElement.next();
				if (anItem.getAttributeValue(ITEM_IDENTIFIER).equals(itemId)) {
					anItem.setText(value);
					_items.add(itemId);
					itemFound = true;
				}
			}
		}
		// otherwise add it as a new node.
		if (itemFound == false) {
			final Element node = new Element(ITEM_NODE);
			node.setText(value);
			node.setAttribute(ITEM_IDENTIFIER, itemId);
			node.setAttribute(ORG_NODE, orgId);
			getDocument().getRootElement().addContent(node);
			_items.add(itemId);
		}

	}

	/**
	 * Overide super method so we can decide if we want to cleanup the JDOM tree first - default - no cleanup
	 * 
	 * @throws IOException
	 */
	@Override
	public void saveDocument() throws IOException {
		saveDocument(false);
	}

	/**
	 * @param cleanUp - a boolean to decide if the JDOM tree needs to be validated
	 * @throws IOException
	 */
	public void saveDocument(final boolean cleanUp) throws IOException {
		if (cleanUp) {
			removeOldNodes(getItems());
		}
		super.saveDocument();
		_items.clear();
	}

	/**
	 * Method to determine if the navigation file has changed since it was last imported. If so then we need to check for old <items> and remove them from the document
	 * 
	 * @param items
	 */
	protected void removeOldNodes(final String[] items) {
		final Vector v = new Vector();
		final List itemList = getDocument().getRootElement().getChildren(ITEM_NODE);
		if (itemList != null && !itemList.isEmpty()) {
			final Iterator itemListElement = itemList.iterator();
			while (itemListElement.hasNext()) {
				final Element anItem = (Element) itemListElement.next();
				if (!doesItemExist(anItem.getAttributeValue(ITEM_IDENTIFIER), items)) {
					// mark this node - (can't delete at the same time as looping
					// otherwise get concurrent access errors)
					v.add(anItem);
				}
			}
			final Iterator itemsToRemove = v.iterator();
			while (itemsToRemove.hasNext()) {
				final Element anItemToDelete = (Element) itemsToRemove.next();
				final Parent parent = anItemToDelete.getParent();
				if (parent != null) {
					Tracing.logWarn("item no longer exists so remove " + anItemToDelete.getAttributeValue(ITEM_IDENTIFIER), null, SequencerModel.class);
					parent.removeContent(anItemToDelete);
				}
			}
		}
		v.clear();
	}

	/**
	 * Utility method which takes a string(item) and a string array The method searches the string array for the value in item, if it exists returns true, otherwise
	 * false.
	 * 
	 * @param item
	 * @param legitimateItems
	 * @return true is item exists
	 */
	protected boolean doesItemExist(final String item, final String[] legitimateItems) {
		for (int i = 0; i < legitimateItems.length; i++) {
			if (legitimateItems[i].equals(item)) { return true; }
		}
		return false;
	}
}
