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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.commons.modules.bc.components;

import java.text.Collator;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.olat.core.commons.controllers.linkchooser.CustomLinkTreeModel;
import org.olat.core.commons.modules.bc.FolderLoggingAction;
import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.commons.modules.bc.commands.FolderCommandFactory;
import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.commons.modules.bc.meta.tagged.MetaTagged;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.JSAndCSSAdder;
import org.olat.core.gui.control.generic.folder.FolderHelper;
import org.olat.core.gui.render.ValidationResult;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.logging.activity.CoreLoggingResourceable;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.Util;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.core.util.vfs.filters.VFSItemExcludePrefixFilter;
import org.olat.core.util.vfs.filters.VFSItemFilter;
import org.olat.core.util.vfs.version.Versionable;

/**
 * Initial Date: Feb 11, 2004
 * 
 * @author Mike Stock
 */
public class FolderComponent extends Component {
	private static final ComponentRenderer RENDERER = new FolderComponentRenderer();

	public static final String SORT_NAME = "name";
	public static final String SORT_SIZE = "size";
	public static final String SORT_DATE = "date";
	public static final String SORT_TYPE = "type";
	public static final String SORT_REV = "revision";
	public static final String SORT_LOCK = "lock";

	// see MessagesEditController
	// see OLAT-4182/OLAT-4219 and OLAT-4259
	// the filtering of .nfs is sort of temporary until we make sure that we no longer reference
	// attached files anywhere at the time of deleting it
	// likely to be resolved after user logs out, caches get cleared - and if not the server
	// restart overnight definitely removes those .nfs files.
	protected static final String[] ATTACHMENT_EXCLUDE_PREFIXES = new String[] { ".nfs", ".CVS", ".DS_Store" };

	protected boolean sortAsc = true; // asc or desc?
	protected String sortCol = ""; // column to sort

	private IdentityEnvironment identityEnv;
	private VFSContainer rootContainer;
	private VFSContainer currentContainer;
	private String currentContainerPath;
	// need to know our children in advance in order to be able to identify them later...
	private List<VFSItem> currentContainerChildren;
	Collator collator;
	private Comparator<VFSItem> comparator;
	protected Translator translator;
	private VFSItemFilter filter;
	private final DateFormat dateTimeFormat;
	private VFSItemExcludePrefixFilter exclFilter;
	private CustomLinkTreeModel customLinkTreeModel;

	/**
	 * Wraps the folder module as a component.
	 * 
	 * @param ureq The user request
	 * @param name The component name
	 * @param rootContainer The base container of this component
	 * @param filter A file filter or NULL to not use a filter
	 * @param customLinkTreeModel A custom link tree model used in the HTML editor or NULL to not use this feature.
	 */
	public FolderComponent(UserRequest ureq, String name, VFSContainer rootContainer, VFSItemFilter filter, CustomLinkTreeModel customLinkTreeModel) {
		super(name);
		this.identityEnv = ureq.getUserSession().getIdentityEnvironment();
		this.filter = filter;
		this.customLinkTreeModel = customLinkTreeModel;
		exclFilter = new VFSItemExcludePrefixFilter(ATTACHMENT_EXCLUDE_PREFIXES);
		Locale locale = ureq.getLocale();
		collator = Collator.getInstance(locale);
		translator = Util.createPackageTranslator(FolderRunController.class, locale);
		sort(SORT_NAME);
		this.rootContainer = rootContainer;
		setCurrentContainerPath("/");

		dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
	}

	/**
	 * @see org.olat.core.gui.components.Component#dispatchRequest(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void doDispatchRequest(UserRequest ureq) {
		if (ureq.getParameter(ListRenderer.PARAM_EDTID) != null) {
			fireEvent(ureq, new Event(FolderCommandFactory.COMMAND_EDIT));
			return;
		} else if (ureq.getParameter(ListRenderer.PARAM_CONTENTEDITID) != null) {
			fireEvent(ureq, new Event(FolderCommandFactory.COMMAND_EDIT_CONTENT));
			return;
		} else if (ureq.getParameter(ListRenderer.PARAM_SERV) != null) {
			// this is a link on a file... deliver it
			fireEvent(ureq, new Event(FolderCommandFactory.COMMAND_SERV));
			// don't redraw the file listing when serving a resource -> timestamp not consumed
			setDirty(false);
			return;
		} else if (ureq.getParameter(ListRenderer.PARAM_SORTID) != null) { // user clicked on table header for sorting column
			setSortAsc(ureq.getParameter(ListRenderer.PARAM_SORTID));
			sort(ureq.getParameter(ListRenderer.PARAM_SORTID)); // just pass selected column
			return;
		} else if (ureq.getParameter("cid") != null) { // user clicked add layer...
			fireEvent(ureq, new Event(ureq.getParameter("cid")));
			return;
		} else if (ureq.getParameter(ListRenderer.PARAM_VERID) != null) {
			fireEvent(ureq, new Event(FolderCommandFactory.COMMAND_VIEW_VERSION));
			return;
		} else if (ureq.getParameter(ListRenderer.PARAM_EPORT) != null) {
			fireEvent(ureq, new Event(FolderCommandFactory.COMMAND_ADD_EPORTFOLIO));
			return;
		} else if (ureq.getParameter(ListRenderer.PARAM_SERV_THUMBNAIL) != null) {
			// this is a link on a file... deliver it
			fireEvent(ureq, new Event(FolderCommandFactory.COMMAND_SERV_THUMBNAIL));
			// don't redraw the file listing when serving a resource -> timestamp not consumed
			setDirty(false);
			return;
		}

		// regular browsing, set current container
		setCurrentContainerPath(ureq.getModuleURI());
		// do logging
		ThreadLocalUserActivityLogger.log(FolderLoggingAction.BC_FOLDER_READ, getClass(), CoreLoggingResourceable.wrapBCFile(getCurrentContainerPath()));
		fireEvent(ureq, new Event(FolderCommandFactory.COMMAND_BROWSE));
	}

	private void setSortAsc(String col) {
		if (col == null) col = SORT_NAME; // "clicked" column not existent
		if (!sortCol.equals(col)) { // if not same col as before, change sort col and sort asc
			sortCol = col;
			sortAsc = true;
		} else { // if same col as before, just change sorting to desc
			sortAsc = !sortAsc;
		}
	}

	/**
	 * Sorts the bc folder components table
	 * 
	 * @param col The column to sort
	 */
	private void sort(String col) {
		if (col.equals(SORT_NAME)) { // sort after file name?
			comparator = new Comparator<VFSItem>() {
				@Override
				public int compare(VFSItem o1, VFSItem o2) {
					if (sortAsc) {
						if ((o1 instanceof VFSLeaf && o2 instanceof VFSLeaf) || (!(o1 instanceof VFSLeaf) && !(o2 instanceof VFSLeaf))) {
							return collator.compare(o1.getName(), o2.getName());
						} else {
							if (!(o1 instanceof VFSLeaf)) {

								return -1;
							} else {
								return 1;
							}
						}
					} else {
						if ((o1 instanceof VFSLeaf && o2 instanceof VFSLeaf) || (!(o1 instanceof VFSLeaf) && !(o2 instanceof VFSLeaf))) {
							return collator.compare(o2.getName(), o1.getName());
						} else {
							if (!(o1 instanceof VFSLeaf)) {

								return -1;
							} else {
								return 1;
							}
						}
					}
				}
			};
		} else if (col.equals(SORT_DATE)) { // sort after modification date (if same, then name)
			comparator = new Comparator<VFSItem>() {
				@Override
				public int compare(VFSItem o1, VFSItem o2) {
					if (o1.getLastModified() < o2.getLastModified()) return ((sortAsc) ? -1 : 1);
					else if (o1.getLastModified() > o2.getLastModified()) return ((sortAsc) ? 1 : -1);
					else {
						if (sortAsc) return collator.compare(o1.getName(), o2.getName());
						else return collator.compare(o2.getName(), o1.getName());
					}
				}
			};
		} else if (col.equals(SORT_SIZE)) { // sort after file size, folders always on top
			comparator = new Comparator<VFSItem>() {
				@Override
				public int compare(VFSItem o1, VFSItem o2) {
					VFSLeaf leaf1 = null;
					if (o1 instanceof VFSLeaf) {
						leaf1 = (VFSLeaf) o1;
					}
					VFSLeaf leaf2 = null;
					if (o2 instanceof VFSLeaf) {
						leaf2 = (VFSLeaf) o2;
					}
					if (leaf1 == null && leaf2 != null) return -1; // folders are always smaller
					else if (leaf1 != null && leaf2 == null) return 1; // folders are always smaller
					else if (leaf1 == null && leaf2 == null) // if two folders, sort after name
					if (sortAsc) return collator.compare(o1.getName(), o2.getName());
					else return collator.compare(o2.getName(), o1.getName());
					else // if two leafes, sort after size
					if (sortAsc) return ((leaf1.getSize() < leaf2.getSize()) ? -1 : 1);
					else return ((leaf1.getSize() < leaf2.getSize()) ? 1 : -1);
				}
			};
		} else if (col.equals(SORT_TYPE)) { // sort after file type, folders always on top
			comparator = new Comparator<VFSItem>() {
				@Override
				public int compare(VFSItem o1, VFSItem o2) {
					String type1 = FolderHelper.extractFileType(o1.getName(), translator.getLocale());
					String type2 = FolderHelper.extractFileType(o2.getName(), translator.getLocale());
					if (o1 instanceof VFSLeaf) {
						if (!FolderHelper.isKnownFileType(type1)) type1 = translator.translate("UnknownFile");
					} else {
						type1 = (sortAsc) ? "" : "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"; // it's a folder
					}
					if (o2 instanceof VFSLeaf) {
						if (!FolderHelper.isKnownFileType(type2)) type2 = translator.translate("UnknownFile");
					} else {
						type2 = (sortAsc) ? "" : "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"; // it's a folder
					}
					return (sortAsc) ? collator.compare(type1, type2) : collator.compare(type2, type1);
				}
			};
		} else if (col.equals(SORT_REV)) { // sort after revision number, folders always on top
			comparator = new Comparator<VFSItem>() {
				@Override
				public int compare(VFSItem o1, VFSItem o2) {
					Versionable v1 = null;
					Versionable v2 = null;
					if (o1 instanceof Versionable) {
						v1 = (Versionable) o1;
					}
					if (o2 instanceof Versionable) {
						v2 = (Versionable) o2;
					}
					if (v1 == null) {
						return -1;
					} else if (v2 == null) { return 1; }

					String r1 = v1.getVersions().getRevisionNr();
					String r2 = v2.getVersions().getRevisionNr();
					if (v1 == null) {
						return -1;
					} else if (v2 == null) { return 1; }
					return (sortAsc) ? collator.compare(r1, r2) : collator.compare(r2, r1);
				}
			};
		} else if (col.equals(SORT_LOCK)) { // sort after modification date (if same, then name)
			comparator = new Comparator<VFSItem>() {
				@Override
				public int compare(VFSItem o1, VFSItem o2) {
					boolean l1 = false;
					if (o1 instanceof MetaTagged) {
						MetaInfo meta = ((MetaTagged) o1).getMetaInfo();
						l1 = meta == null ? false : meta.isLocked();
					}

					boolean l2 = false;
					if (o2 instanceof MetaTagged) {
						MetaInfo meta = ((MetaTagged) o2).getMetaInfo();
						l2 = meta == null ? false : meta.isLocked();
					}

					if (l1 && !l2) return sortAsc ? -1 : 1;
					if (!l1 && l2) return sortAsc ? 1 : -1;

					if (sortAsc) {
						if ((o1 instanceof VFSLeaf && o2 instanceof VFSLeaf) || (!(o1 instanceof VFSLeaf) && !(o2 instanceof VFSLeaf))) {
							return collator.compare(o1.getName(), o2.getName());
						} else {
							if (!(o1 instanceof VFSLeaf)) {

								return -1;
							} else {
								return 1;
							}
						}
					} else {
						if ((o1 instanceof VFSLeaf && o2 instanceof VFSLeaf) || (!(o1 instanceof VFSLeaf) && !(o2 instanceof VFSLeaf))) {
							return collator.compare(o2.getName(), o1.getName());
						} else {
							if (!(o1 instanceof VFSLeaf)) {

								return -1;
							} else {
								return 1;
							}
						}
					}

				}
			};
		}
		if (currentContainerChildren != null) updateChildren(); // if not empty the update list
	}

	/**
	 * @return VFSContainer
	 */
	public VFSContainer getRootContainer() {
		return rootContainer;
	}

	/**
	 * @return VFSContainer
	 */
	public VFSContainer getCurrentContainer() {
		return currentContainer;
	}

	public String getCurrentContainerPath() {
		return currentContainerPath;
	}

	/**
	 * Return the children of the folder of this FolderComponent. The children are already alphabetically sorted.
	 * 
	 * @return
	 */
	public List<VFSItem> getCurrentContainerChildren() {
		return currentContainerChildren;
	}

	public void updateChildren() {
		setDirty(true);
		// check if the container is still up-to-date, if not -> return to root
		if (!VFSManager.exists(currentContainer)) {
			currentContainer = rootContainer;
			currentContainerPath = "/";
		}

		// get the children and sort them alphabetically
		if (filter != null) {
			currentContainerChildren = currentContainer.getItems(filter);
		} else {
			currentContainerChildren = currentContainer.getItems();
		}
		// OLAT-5256: filter .nfs files
		Iterator<VFSItem> it = currentContainerChildren.iterator();
		while (it.hasNext()) {
			if (!exclFilter.accept(it.next())) {
				it.remove();
			}
		}
		Collections.sort(currentContainerChildren, comparator);
	}

	/**
	 * @param relPath
	 */
	public boolean setCurrentContainerPath(String relPath) {
		// get the container
		setDirty(true);
		if (relPath == null) relPath = "/";
		if (!(relPath.charAt(0) == '/')) relPath = "/" + relPath;
		VFSItem vfsItem = rootContainer.resolve(relPath);
		if (vfsItem == null || !(vfsItem instanceof VFSContainer)) {
			// unknown path, reset to root contaner...
			currentContainer = rootContainer;
			relPath = "";
			return false;
		}

		this.currentContainer = (VFSContainer) vfsItem;
		this.currentContainerPath = relPath;
		updateChildren();
		return true;
	}

	/**
	 * Set a custom link tree model that is used in the HTML editor to create links
	 * 
	 * @param customLinkTreeModel The link tree model or NULL to not use this feature in the editor
	 */
	public void setCustomLinkTreeModel(CustomLinkTreeModel customLinkTreeModel) {
		this.customLinkTreeModel = customLinkTreeModel;
	}

	/**
	 * Get the custom link tree model to build links in the editor
	 * 
	 * @return The custom link tree model or NULL if no such model is used.
	 */
	public CustomLinkTreeModel getCustomLinkTreeModel() {
		return this.customLinkTreeModel;
	}

	/**
	 * @see org.olat.core.gui.components.Component#validate(org.olat.core.gui.UserRequest, org.olat.core.gui.render.ValidationResult)
	 */
	@Override
	public void validate(UserRequest ureq, ValidationResult vr) {
		super.validate(ureq, vr);
		// include needed css and js files
		JSAndCSSAdder jsa = vr.getJsAndCSSAdder();
		jsa.addRequiredJsFile(FolderComponent.class, "js/folder.js");
	}

	@Override
	public ComponentRenderer getHTMLRendererSingleton() {
		return RENDERER;
	}

	public IdentityEnvironment getIdentityEnvironnement() {
		return identityEnv;
	}

	public DateFormat getDateTimeFormat() {
		return dateTimeFormat;
	}
}