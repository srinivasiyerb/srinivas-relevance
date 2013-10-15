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

package org.olat.ims.cp;

import org.dom4j.tree.DefaultDocument;
import org.dom4j.tree.DefaultElement;
import org.olat.core.id.OLATResourceable;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.ims.cp.objects.CPOrganization;
import org.olat.ims.cp.objects.CPResource;
import org.olat.ims.cp.ui.CPPage;

/**
 * Description:<br>
 * abstract class of the IMS-CP Manager
 * <P>
 * Initial Date: 26.06.2008 <br>
 * 
 * @author sergio
 */
public abstract class CPManager extends BasicManager {
	protected static CPManager INSTANCE;

	public static CPManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Generates a new Instance of ContentPackage and loads the xmlmanifest
	 * 
	 * @param directory
	 * @return
	 */
	public abstract ContentPackage load(VFSContainer directory, OLATResourceable ores);

	/**
	 * creates a new, empty ContentPackage
	 * 
	 * @param ores
	 * @return the newly created CP
	 */
	public abstract ContentPackage createNewCP(OLATResourceable ores, String initalPageTitle);

	/**
	 * Determines if the given Resource is referenced / linked only by one item-element. In other words: if the given resource is used only bye one item, this will return
	 * true
	 * 
	 * @param res
	 * @param cp
	 * @return if given Resource is only used by one item-element, this returns true
	 */
	public abstract boolean isSingleUsedResource(CPResource res, ContentPackage cp);

	/**
	 * Adds a blank new <item> to the end of the organization
	 * 
	 * @return
	 */
	public abstract String addBlankPage(ContentPackage cp, String title);

	/**
	 * Adds a blank new <item> as a child of the node with given id
	 * 
	 * @param parentNodeID
	 * @return
	 */
	public abstract String addBlankPage(ContentPackage cp, String title, String parentNodeID);

	/**
	 * updates the according elements in the datastructure
	 * 
	 * @param cp
	 * @param page
	 */
	public abstract void updatePage(ContentPackage cp, CPPage page);

	/**
	 * @param cp
	 * @param newElement
	 * @return
	 */
	public abstract boolean addElement(ContentPackage cp, DefaultElement newElement);

	/**
	 * Adds a new Element to the manifest. the new Element is inserted as a child of the element with id "parentIdentifier" if new position is null, element is added at
	 * the end
	 * 
	 * @param cp
	 * @param newElement the new Element to add
	 * @param parentIdentifier the identifier of the parent element
	 * @param position: the new position of the moved element, can be null!
	 * @return true if successfully added
	 */
	public abstract boolean addElement(ContentPackage cp, DefaultElement newElement, String parentIdentifier, int position);

	/**
	 * adds a element to the manifest. Element is inserted after the Element with id "identifier"
	 * 
	 * @param cp the contentPackage where the manipulation is performed on
	 * @param newElement the new Element to Add
	 * @param identifier the identifier of the previous element
	 * @return true if successfully added
	 */
	public abstract boolean addElementAfter(ContentPackage cp, DefaultElement newElement, String identifier);

	/**
	 * removes a element from the manifest. If deleting an <item>, all referenced resources are removed from the manifest as well.
	 * 
	 * @return
	 */
	public abstract void removeElement(ContentPackage cp, String identifier, boolean deleteResource);

	/**
	 * Moves a Element(with id "sourceID") in cp. Inserts it into (as child) Element with id "targetID"
	 * 
	 * @param cp
	 * @param sourceID
	 * @param targetID
	 */
	public abstract void moveElement(ContentPackage cp, String nodeID, String newParentID, int position);

	/**
	 * duplicates the element with id "sourceID" and inserts it right after the source-Element
	 * 
	 * @param cp
	 * @param sourceID
	 * @return String the identifier of the newly generated element
	 */
	public abstract String copyElement(ContentPackage cp, String sourceID);

	/**
	 * Returns the DefaultDocument of the ContentPackage cp
	 * 
	 * @return the xml Document of the cp
	 */
	public abstract DefaultDocument getDocument(ContentPackage cp);

	/**
	 * returns the title of an item with given id. if element with id is not found or is not an <item>, an empty String is returned
	 * 
	 * @param cp
	 * @param itemID
	 * @return the title of the <item>
	 */
	public abstract String getItemTitle(ContentPackage cp, String itemID);

	/**
	 * Returns the CPTreeDataModel which is needed by the TreeController used for gui
	 * 
	 * @param cp
	 * @return
	 */
	public abstract CPTreeDataModel getTreeDataModel(ContentPackage cp);

	/**
	 * Returns the top most <organization> element (if there are more than one)
	 * 
	 * @param cp
	 * @return
	 */
	public abstract CPOrganization getFirstOrganizationInManifest(ContentPackage cp);

	/**
	 * Returns the first (topmost) resource to display
	 * 
	 * @param cp
	 * @return
	 */
	public abstract CPPage getFirstPageToDisplay(ContentPackage cp);

	/**
	 * @param cp
	 * @param itemIDentifier
	 * @return
	 */
	public abstract String getPageByItemId(ContentPackage cp, String itemIDentifier);

	/**
	 * writes the ContentPackage-tree to the manifest-xml-file
	 * 
	 * @param cp
	 */
	public abstract void writeToFile(ContentPackage cp);

	/**
	 * Writes the cp to a zip file.
	 * 
	 * @param cp
	 * @return The VFS zip archive
	 */
	public abstract VFSLeaf writeToZip(ContentPackage cp);

	/**
	 * this is case-sensitive!
	 * 
	 * @param identifier
	 * @return an Element by its IDENTIFIER attribute starting at the manifests root element. This will do a deep recursive search
	 */
	public abstract DefaultElement getElementByIdentifier(ContentPackage cp, String identifier);

}