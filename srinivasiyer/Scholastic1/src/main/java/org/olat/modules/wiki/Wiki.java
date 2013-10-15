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

package org.olat.modules.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.jamwiki.utils.Utilities;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Formatter;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.filters.VFSLeafFilter;
import org.olat.modules.wiki.gui.components.wikiToHtml.FilterUtil;
import org.olat.modules.wiki.versioning.DifferenceService;

/**
 * Description:<br>
 * Abstract model of a single Wiki where a wikiWord is unique and used as a key to get it's content. The pages are hold in an map where the key is an base64 (excluding
 * the "/" of the base64 transformation) id of the wiki name. The base64 key is in the wikiManager used for persisting an single page to an fileSystem. Normal viewing
 * operations are performed by the map where all pages are accessible, writing actions are handled by the wikiManager.
 * <P>
 * Initial Date: May 7, 2006 <br>
 * 
 * @author guido schnider
 */
public class Wiki implements WikiContainer, Serializable {
	public static final String CSS_CLASS_WIKI_ICON = "o_wiki_icon";

	// synchronized map of all pages of a wiki as the whole model (wiki object) gets cached this object itself is save for cluster mode
	private final Map<String, WikiPage> wikiPages;// o_clusterOK by gs
	private final DifferenceService diffService;
	private final VFSContainer versionsContainer, pageContainer, mediaContainer;
	protected static final String NEW_PAGE = "O_new_page";
	private final String IMAGE_NAMESPACE = "Image:";
	private final String MEDIA_NAMESPACE = "Media:";
	OLog log = Tracing.createLoggerFor(this.getClass());

	protected Wiki(final VFSContainer wikiRootContainer) {
		if (wikiRootContainer == null) { throw new AssertException("null values are not allowed for the wiki constructor!"); }
		wikiPages = Collections.synchronizedMap(new HashMap<String, WikiPage>());
		this.diffService = WikiManager.getInstance().getDiffService();
		versionsContainer = (VFSContainer) wikiRootContainer.resolve(WikiManager.VERSION_FOLDER_NAME);
		pageContainer = (VFSContainer) wikiRootContainer.resolve(WikiManager.WIKI_RESOURCE_FOLDER_NAME);
		mediaContainer = (VFSContainer) wikiRootContainer.resolve(WikiContainer.MEDIA_FOLDER_NAME);
	}

	/**
	 * Return a wiki page but normally without content yet (performance issues) but with all other attributes. To get the page content call getPage(pageId, true)
	 * 
	 * @param pageId use either the page name or the pageID
	 * @see org.olat.modules.wiki.WikiPage.getPageId() as key;
	 * @return the wikiPage or null if not found
	 */
	public WikiPage getPage(final String pageId) {
		WikiPage page = null;
		page = wikiPages.get(pageId);
		// try also the pageName, may be someone tried to access with the name
		// instead of the page id
		if (page == null) {
			page = wikiPages.get(this.generatePageId(pageId));
		}
		if (page == null) {
			page = wikiPages.get(this.generatePageId(FilterUtil.normalizeWikiLink(pageId)));
		}
		if (page == null) {
			page = new WikiPage(WikiPage.WIKI_ERROR);
			page.setContent("wiki.error.page.not.found");
		}
		return page;
	}

	/**
	 * @param pageId
	 * @param loadContent loads the page content (article) lazy (performance issues) when set true
	 * @return if loadContent == true the WikiPage with the article otherwise the WikiPage with all other attributes
	 */
	public WikiPage getPage(final String pageId, final boolean loadContent) {
		final WikiPage page = getPage(pageId);
		// if not empty content is already loaded
		if (!page.getContent().equals("")) { return page; }
		if (loadContent) {
			final VFSLeaf leaf = (VFSLeaf) pageContainer.resolve(page.getPageId() + "." + WikiManager.WIKI_FILE_SUFFIX);
			page.setContent(FileUtils.load(leaf.getInputStream(), "utf-8"));
		}
		return page;
	}

	protected void addPage(final WikiPage page) {
		final String pageId = page.getPageId();
		if (!wikiPages.containsKey(pageId)) {
			wikiPages.put(pageId, page);
		}
	}

	/**
	 * @param pageId, or filename for media and image files
	 * @see WikiPage.generateId(name) as pages are stored by pageId
	 * @return
	 */
	@Override
	public boolean pageExists(final String pageId) {
		if (log.isDebug()) {
			final boolean exists = wikiPages.containsKey(pageId);
			log.debug("\n\nChecking for existence of page with id in this wiki: " + pageId + " located in: " + pageContainer);
			log.debug("Does page exists?: " + exists);
			if (exists) {
				log.debug("Page has spoken name: " + getPage(pageId).getPageName());
			}
		}
		final boolean isImage = pageId.startsWith(IMAGE_NAMESPACE);
		final boolean isMedia = pageId.startsWith(MEDIA_NAMESPACE);
		if (isImage || isMedia) {
			if (isImage) { return mediaFileExists(pageId.substring(IMAGE_NAMESPACE.length(), pageId.length())); }
			if (isMedia) { return mediaFileExists(pageId.substring(MEDIA_NAMESPACE.length(), pageId.length())); }
		}
		return wikiPages.containsKey(pageId);
	}

	protected void removePage(final WikiPage page) {
		final String name = page.getPageName();
		if (name.equals(WikiPage.WIKI_INDEX_PAGE) || name.equals(WikiPage.WIKI_MENU_PAGE)) { return; }
		wikiPages.remove(page.getPageId());
	}

	protected int getNumberOfPages() {
		return this.wikiPages.size();
	}

	protected List<String> getDiff(final WikiPage page, final int version1, final int version2) {
		final WikiPage v1 = loadVersion(page, version1);
		final WikiPage v2 = loadVersion(page, version2);

		if (log.isDebug()) {
			log.debug("comparing wiki page versions: " + version1 + " <--> " + version2);
			log.debug("version 1:\n" + v1.toString());
			log.debug("version 2:\n" + v2.toString());
		}

		return diffService.diff(v1.getContent(), v2.getContent());
	}

	protected List<WikiPage> getHistory(final WikiPage page) {
		final List<WikiPage> versions = new ArrayList<WikiPage>();
		final List<VFSItem> leafs = versionsContainer.getItems(new VFSLeafFilter());
		if (leafs.size() > 0) {
			for (final Iterator<VFSItem> iter = leafs.iterator(); iter.hasNext();) {
				final VFSLeaf leaf = (VFSLeaf) iter.next();
				final String filename = leaf.getName();
				// TODO:gs:a needs better filtering only for pagename.properties-xy
				// try this: List leafs = wikiCont.getItems(new VFSItemSuffixFilter(new String[]{WikiManager.WIKI_PROPERTIES_SUFFIX}));
				if (filename.indexOf(WikiManager.WIKI_PROPERTIES_SUFFIX) != -1 && filename.startsWith(page.getPageId())) {
					versions.add(assignPropertiesToPage(leaf));
				}
			}
			// add also the current version but only if saved once
		}
		if (page.getModificationTime() > 0) {
			versions.add(page);
		}
		return versions;
	}

	protected WikiPage loadVersion(final WikiPage page, final int version) {
		// if version matches recent version the current pags is requested
		if (version == page.getVersion() && !page.getContent().equals("")) { return page; }
		if (version == 0) { return new WikiPage("dummy"); }
		if (page.getPageName().equals("dummy")) { return page; }
		WikiPage pageVersion = null;
		final VFSLeaf versionLeaf = (VFSLeaf) versionsContainer.resolve(page.getPageId() + "." + WikiManager.WIKI_PROPERTIES_SUFFIX + "-" + version);
		pageVersion = assignPropertiesToPage(versionLeaf);
		final VFSLeaf contentLeaf = (VFSLeaf) versionsContainer.resolve(page.getPageId() + "." + WikiManager.WIKI_FILE_SUFFIX + "-" + version);
		if (contentLeaf != null) {
			pageVersion.setContent(FileUtils.load(contentLeaf.getInputStream(), "utf-8"));
		} else {
			pageVersion.setContent("");
		}
		return pageVersion;

	}

	public static WikiPage assignPropertiesToPage(final VFSLeaf leaf) {
		final Properties p = new Properties();
		if (leaf != null) {
			try {
				final InputStream is = leaf.getInputStream();
				p.load(is);
				is.close();
			} catch (final IOException e) {
				throw new OLATRuntimeException("Wiki page couldn't be read! Pagename:" + leaf.getName(), e);
			}
			final String pageName = p.getProperty(WikiManager.PAGENAME);
			if (pageName == null) {
				final OLog log = Tracing.createLoggerFor(Wiki.class);
				log.warn("wiki properties page is persent but without content. Name:" + leaf.getName());
				return null;
			}
			final WikiPage page = new WikiPage(pageName);
			page.setCreationTime(p.getProperty(WikiManager.C_TIME));
			page.setVersion(p.getProperty(WikiManager.VERSION));
			page.setForumKey(p.getProperty(WikiManager.FORUM_KEY));
			page.setInitalAuthor(p.getProperty(WikiManager.INITIAL_AUTHOR));
			page.setModificationTime(p.getProperty(WikiManager.M_TIME));
			page.setModifyAuthor(p.getProperty(WikiManager.MODIFY_AUTHOR));
			page.setViewCount(p.getProperty(WikiManager.VIEW_COUNT));
			page.setUpdateComment(p.getProperty(WikiManager.UPDATE_COMMENT));
			return page;
		} else {
			return new WikiPage("dummy");
		}
	}

	protected List<VFSItem> getMediaFileListWithMetadata() {
		return mediaContainer.getItems();
	}

	protected List<VFSItem> getMediaFileList() {
		final List<VFSItem> allFiles = mediaContainer.getItems();
		final List<VFSItem> mediaFilesOnly = new ArrayList<VFSItem>();
		for (final Iterator<VFSItem> iter = allFiles.iterator(); iter.hasNext();) {
			final VFSItem element = iter.next();
			if (!element.getName().endsWith(WikiMainController.METADATA_SUFFIX)) {
				mediaFilesOnly.add(element);
			}
		}
		return mediaFilesOnly;
	}

	public String getAllPageNamesSorted() {
		final ArrayList<WikiPage> pages = new ArrayList<WikiPage>(wikiPages.values());
		Collections.sort(pages, WikiPageSort.PAGENAME_ORDER);
		final StringBuilder sb = new StringBuilder();
		for (final Iterator<WikiPage> iter = pages.iterator(); iter.hasNext();) {
			final WikiPage page = iter.next();
			if (!page.getPageName().startsWith("O_")) {
				sb.append("* ");
				if (log.isDebug()) {
					sb.append(page.getPageId()).append("  -->  ");
				}
				sb.append("[[");
				sb.append(page.getPageName());
				sb.append("]]\n");
			}
		}
		return sb.toString();
	}

	protected String getRecentChanges(final Locale locale) {
		if (locale == null) { throw new AssertException("param was null which is not allowed"); }
		final int MAX_RESULTS = 5;
		final ArrayList<WikiPage> pages = new ArrayList<WikiPage>(wikiPages.values());
		Collections.sort(pages, WikiPageSort.MODTIME_ORDER);
		final StringBuilder sb = new StringBuilder();
		int counter = 0;
		final Formatter f = Formatter.getInstance(locale);
		for (final Iterator<WikiPage> iter = pages.iterator(); iter.hasNext();) {
			if (counter > MAX_RESULTS) {
				break;
			}
			final WikiPage page = iter.next();
			if (!page.getPageName().startsWith("O_") && !page.getPageName().startsWith(WikiPage.WIKI_MENU_PAGE)) {
				sb.append("* [[");
				sb.append(page.getPageName());
				sb.append("]] ");
				sb.append(f.formatDateAndTime(new Date(page.getModificationTime())));
				sb.append(" Author: ");
				final long author = page.getModifyAuthor();
				if (author != 0) {
					sb.append(BaseSecurityManager.getInstance().loadIdentityByKey(Long.valueOf(page.getModifyAuthor())).getName());
				}
				sb.append("\n");
				counter++;
			}
		}
		return sb.toString();
	}

	protected WikiPage findPage(final String name) {
		WikiPage page = null;
		if (pageExists(WikiManager.generatePageId(FilterUtil.normalizeWikiLink(name)))) { return getPage(name, true); }
		page = new WikiPage(NEW_PAGE);
		page.setContent("[[" + name + "]]");
		return page;
	}

	/**
	 * @see org.olat.core.commons.modules.wiki.WikiContainer#generatePageId(java.lang.String)
	 */
	@Override
	public String generatePageId(final String pageName) {
		if (log.isDebug()) {
			log.debug("Generating page id from page name: " + pageName + " to id: " + WikiManager.generatePageId(pageName));
		}
		return WikiManager.generatePageId(pageName);
	}

	/**
	 * @return a List of all pages in a wiki ordered by date
	 */
	protected List<WikiPage> getPagesByDate() {
		final ArrayList<WikiPage> pages = new ArrayList<WikiPage>(wikiPages.values());
		Collections.sort(pages, WikiPageSort.MODTIME_ORDER);
		return pages;
	}

	/**
	 * @return a List containing all pages names of the wiki sorted alphabetically
	 */
	protected List<String> getListOfAllPageNames() {
		final ArrayList<WikiPage> pages = new ArrayList<WikiPage>(wikiPages.values());
		final ArrayList<String> pageNames = new ArrayList<String>(pages.size());
		Collections.sort(pages, WikiPageSort.PAGENAME_ORDER);
		for (final Iterator<WikiPage> iter = pages.iterator(); iter.hasNext();) {
			final WikiPage page = iter.next();
			if (!page.getPageName().startsWith("O_")) {
				pageNames.add(page.getPageName());
			}
		}
		return pageNames;
	}

	/**
	 * @return a List of all pages in a wiki
	 */
	public List<WikiPage> getAllPagesWithContent() {
		return getAllPagesWithContent(false);
	}

	public List<WikiPage> getAllPagesWithContent(final boolean includeSpecialPages) {
		final ArrayList<WikiPage> pages = new ArrayList<WikiPage>();
		for (final Iterator<String> keyes = wikiPages.keySet().iterator(); keyes.hasNext();) {
			final String pageId = keyes.next();
			WikiPage wikiPage = getPage(pageId);
			// check if the page is a content page
			if (includeSpecialPages) {
				if (wikiPage.getContent().equals("")) {
					// wikiPage has empty content => try to load content
					if (!wikiPage.getPageName().startsWith("O_")) {
						wikiPage = getPage(pageId, true);
					}
				}
				pages.add(wikiPage);
			} else {
				if (!wikiPage.getPageName().startsWith("O_")) {
					if (wikiPage.getContent().equals("")) {
						// wikiPage has empty content => try to load content
						wikiPage = getPage(pageId, true);
					}
					pages.add(wikiPage);
				}
			}
		}
		return pages;
	}

	/**
	 * FIXME:gs increase performance
	 * 
	 * @param imageName
	 * @return
	 */
	public boolean mediaFileExists(final String imageName) {
		final List<VFSItem> mediaFiles = getMediaFileList();
		if (mediaFiles.size() == 0) { return false; }
		for (final Iterator<VFSItem> iter = mediaFiles.iterator(); iter.hasNext();) {
			final VFSLeaf leaf = (VFSLeaf) iter.next();
			if (leaf.getName().equals(Utilities.encodeForURL(imageName))) { return true; }
		}
		return false;
	}

}
