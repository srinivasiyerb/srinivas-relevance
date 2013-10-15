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

package org.olat.ims.qti.editor.tree;

import java.util.List;

import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.ims.qti.editor.QTIEditorPackage;
import org.olat.ims.qti.editor.beecom.objects.Assessment;
import org.olat.ims.qti.editor.beecom.objects.Item;
import org.olat.ims.qti.editor.beecom.objects.Section;

/**
 * The QTIEditorTreeModel reflects the IMS QTI Standard tree within OLAT Initial Date: Nov 11, 2004 <br>
 * 
 * @author patrick
 */
public class QTIEditorTreeModel extends GenericTreeModel {

	private final QTIEditorPackage qtiPackage;

	/**
	 * builds a TreeModel for an existing, valid QTI Document
	 * 
	 * @param qtiPackage
	 */
	public QTIEditorTreeModel(final QTIEditorPackage qtiPackage) {
		this.qtiPackage = qtiPackage;
		init();
	}

	/**
	 * @return the node as a GenericQtiNode
	 */
	public GenericQtiNode getQtiRootNode() {
		return (GenericQtiNode) super.getRootNode();
	}

	/**
	 * @param nodeId
	 * @return the GenericQtiNode associated with the nodeId
	 */
	public GenericQtiNode getQtiNode(final String nodeId) {
		final GenericQtiNode retVal = (GenericQtiNode) getNodeById(nodeId);
		return retVal;
	}

	/**
	 * takes the assessment object tree and converts it to a QTIEditorTreeModel
	 */
	private void init() {
		final Assessment ass = qtiPackage.getQTIDocument().getAssessment();
		final GenericQtiNode rootNode = new AssessmentNode(ass, qtiPackage);
		this.setRootNode(rootNode);
		// Sections with their Items
		final List sections = ass.getSections();
		for (int i = 0; i < sections.size(); i++) {
			// get section data
			final Section elem = (Section) sections.get(i);
			final GenericQtiNode sectionNode = new SectionNode(elem, qtiPackage);
			final List items = elem.getItems();
			for (int j = 0; j < items.size(); j++) {
				// get item data
				final Item elem2 = (Item) items.get(j);
				final GenericQtiNode itemNode = new ItemNode(elem2, qtiPackage);
				// add item to its parent section
				sectionNode.addChild(itemNode);
			}
			// add section with its items to the rootNode
			rootNode.addChild(sectionNode);
		}

	}
}