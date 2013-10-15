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

package org.olat.core.commons.controllers.filechooser;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.vfs.MergeSource;
import org.olat.core.util.vfs.NamedContainerImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.filters.VFSContainerFilter;
import org.olat.core.util.vfs.filters.VFSItemFilter;

/**
 * <h3>Description:</h3> UI Factory to handle the file chooser package
 * <p>
 * Initial Date: 13.06.2008 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */

public class FileChooserUIFactory {
	private static final VFSItemFilter containerFilter = new VFSContainerFilter();

	/**
	 * Factory method to create a file chooser workflow controller that allows the usage of a custom vfs item filter. The tree will display a title and a description
	 * above the tree.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootContainer The root container that should be selected from
	 * @param customItemFilter The custom filter to be used or NULL to not use any filter at all
	 * @param onlyLeafsSelectable true: container elements can't be selected; false: all items can be selected
	 */
	public static FileChooserController createFileChooserController(UserRequest ureq, WindowControl wControl, VFSContainer rootContainer, VFSItemFilter customItemFilter,
			boolean onlyLeafsSelectable) {
		return new FileChooserController(ureq, wControl, rootContainer, customItemFilter, onlyLeafsSelectable, true);
	}

	/**
	 * Factory method to create a file chooser workflow controller that allows the usage of a custom vfs item filter. The tree will not have a title, just the tree
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootContainer The root container that should be selected from
	 * @param customItemFilter The custom filter to be used or NULL to not use any filter at all
	 * @param onlyLeafsSelectable true: container elements can't be selected; false: all items can be selected
	 */
	public static FileChooserController createFileChooserControllerWithoutTitle(UserRequest ureq, WindowControl wControl, VFSContainer rootContainer,
			VFSItemFilter customItemFilter, boolean onlyLeafsSelectable) {
		return new FileChooserController(ureq, wControl, rootContainer, customItemFilter, onlyLeafsSelectable, false);
	}

	/**
	 * Factory method to create a file chooser workflow controller allows filtering of files by setting a boolean. The tree will display a title and a description above
	 * the tree.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootContainer The root container that should be selected from
	 * @param showLeafs true: show directories and files; false: show only directories
	 * @param onlyLeafsSelectable true: container elements can't be selected; false: all items can be selected
	 */
	public static FileChooserController createFileChooserController(UserRequest ureq, WindowControl wControl, VFSContainer rootContainer, boolean showLeafs,
			boolean onlyLeafsSelectable) {
		return new FileChooserController(ureq, wControl, rootContainer, (showLeafs ? null : containerFilter), onlyLeafsSelectable, true);
	}

	/**
	 * Factory method to create a file chooser workflow controller allows filtering of files by setting a boolean. The tree will not have a title, just the tree
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootContainer The root container that should be selected from
	 * @param showLeafs true: show directories and files; false: show only directories
	 * @param onlyLeafsSelectable true: container elements can't be selected; false: all items can be selected
	 */
	public static FileChooserController createFileChooserControllerWithoutTitle(UserRequest ureq, WindowControl wControl, VFSContainer rootContainer, boolean showLeafs,
			boolean onlyLeafsSelectable) {
		return new FileChooserController(ureq, wControl, rootContainer, (showLeafs ? null : containerFilter), onlyLeafsSelectable, false);
	}

	/**
	 * Get the vfs item that was selected by the user
	 * 
	 * @param event The file choosen event
	 * @return
	 */
	public static VFSItem getSelectedItem(FileChoosenEvent event) {
		return event.getSelectedItem();
	}

	/**
	 * Get the path as string of the selected item relative to the root container and the relative base path
	 * 
	 * @param event The file choosen event
	 * @param rootContainer The root container for which the relative path should be calculated
	 * @param relativeBasePath when NULL, the path will be calculated relative to the rootContainer; when NULL, the relativeBasePath must represent a relative path within
	 *            the root container that serves as the base. In this case, the calculated relative item path will start from this relativeBasePath
	 * @return
	 */
	public static String getSelectedRelativeItemPath(FileChoosenEvent event, VFSContainer rootContainer, String relativeBasePath) {
		// 1) Create path absolute to the root container
		VFSItem selectedItem = event.getSelectedItem();
		if (selectedItem == null) return null;
		String absPath = "";
		VFSItem tmpItem = selectedItem;
		// Check for merged containers to fix problems with named containers, see OLAT-3848
		List<NamedContainerImpl> namedRootChilds = new ArrayList<NamedContainerImpl>();
		for (VFSItem rootItem : rootContainer.getItems()) {
			if (rootItem instanceof NamedContainerImpl) {
				namedRootChilds.add((NamedContainerImpl) rootItem);
			}
		}
		// Check if root container is the same as the item and vice versa. It is
		// necessary to perform the check on both containers to catch all potential
		// cases with MergedSource and NamedContainer where the check in one
		// direction is not necessarily the same as the opposite check
		while (tmpItem != null && !rootContainer.isSame(tmpItem) && !tmpItem.isSame(rootContainer)) {
			String itemFileName = tmpItem.getName();
			// Special case: check if this is a named container, see OLAT-3848
			for (NamedContainerImpl namedRootChild : namedRootChilds) {
				if (namedRootChild.isSame(tmpItem)) {
					itemFileName = namedRootChild.getName();
				}
			}
			absPath = "/" + itemFileName + absPath;
			tmpItem = tmpItem.getParentContainer();
			if (tmpItem != null) {
				// test if this this is a merge source child container, see OLAT-5726
				VFSContainer grandParent = tmpItem.getParentContainer();
				if (grandParent instanceof MergeSource) {
					MergeSource mergeGrandParent = (MergeSource) grandParent;
					if (mergeGrandParent.isContainersChild((VFSContainer) tmpItem)) {
						// skip this parent container and use the merge grand-parent
						// instead, otherwhise path contains the container twice
						tmpItem = mergeGrandParent;
					}
				}
			}
		}

		if (relativeBasePath == null) { return absPath; }
		// 2) Compute rel path to base dir of the current file

		// selpath = /a/irwas/subsub/nochsub/note.html 5
		// filenam = /a/irwas/index.html 3
		// --> subsub/nochsub/note.gif

		// or /a/irwas/bla/index.html
		// to /a/other/b/gugus.gif
		// --> ../../ other/b/gugus.gif

		// or /a/other/b/main.html
		// to /a/irwas/bla/goto.html
		// --> ../../ other/b/gugus.gif

		String base = relativeBasePath; // assume "/" is here
		if (!(base.indexOf("/") == 0)) {
			base = "/" + base;
		}

		String[] baseA = base.split("/");
		String[] targetA = absPath.split("/");
		int sp = 1;
		for (; sp < Math.min(baseA.length, targetA.length); sp++) {
			if (!baseA[sp].equals(targetA[sp])) {
				break;
			}
		}
		// special case: self-reference
		if (absPath.equals(base)) {
			sp = 1;
		}
		StringBuilder buffer = new StringBuilder();
		for (int i = sp; i < baseA.length - 1; i++) {
			buffer.append("../");
		}
		for (int i = sp; i < targetA.length; i++) {
			buffer.append(targetA[i] + "/");
		}
		buffer.deleteCharAt(buffer.length() - 1);
		String path = buffer.toString();

		String trimmed = path; // selectedPath.substring(1);
		return trimmed;
	}

}
