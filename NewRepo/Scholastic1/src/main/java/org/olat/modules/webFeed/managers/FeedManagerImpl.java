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
package org.olat.modules.webFeed.managers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.services.commentAndRating.CommentAndRatingService;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.Formatter;
import org.olat.core.util.ImageHelper;
import org.olat.core.util.StringHelper;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.cache.n.CacheWrapper;
import org.olat.core.util.coordinate.Coordinator;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.coordinate.SyncerExecutor;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSMediaResource;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.BlogFileResource;
import org.olat.fileresource.types.FeedFileResource;
import org.olat.fileresource.types.PodcastFileResource;
import org.olat.modules.webFeed.RSSFeed;
import org.olat.modules.webFeed.SyndFeedMediaResource;
import org.olat.modules.webFeed.dispatching.FeedMediaDispatcher;
import org.olat.modules.webFeed.models.Enclosure;
import org.olat.modules.webFeed.models.Feed;
import org.olat.modules.webFeed.models.Item;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.controllers.RepositoryEntryImageController;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.ParsingFeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import com.thoughtworks.xstream.XStream;

/**
 * This is the actual feed manager implementation. It handles all operations on the various feeds and items.
 * <P>
 * Initial Date: Feb 17, 2009 <br>
 * 
 * @author Gregor Wassmann
 */
public abstract class FeedManagerImpl extends FeedManager {
	private static final int PICTUREWIDTH = 570; // same as in repository metadata image upload

	private RepositoryManager repositoryManager;
	private final Coordinator coordinator;
	private final OLATResourceManager resourceManager;
	private final FileResourceManager fileResourceManager;

	private final XStream xstream;

	// Better performance when protected (apparently)
	protected CacheWrapper feedCache;
	private final OLog log;

	/**
	 * spring only
	 */
	protected FeedManagerImpl(final OLATResourceManager resourceManager, final FileResourceManager fileResourceManager, final CoordinatorManager coordinatorManager) {

		this.resourceManager = resourceManager;
		this.fileResourceManager = fileResourceManager;
		INSTANCE = this;
		this.log = getLogger();
		xstream = new XStream();
		xstream.alias("feed", Feed.class);
		xstream.alias("item", Item.class);
		this.coordinator = coordinatorManager.getCoordinator();
	}

	/**
	 * @param repositoryManager
	 */
	public void setRepositoryManager(final RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}

	/**
	 * Creates a blank feed object and writes it to the (virtual) file system
	 * 
	 * @see org.olat.modules.webFeed.managers.FeedManager#createPodcastResource()
	 */
	@Override
	public OLATResourceable createPodcastResource() {
		final FeedFileResource podcastResource = new PodcastFileResource();
		return createFeedResource(podcastResource);
	}

	/**
	 * Creates a blank feed object and writes it to the file system
	 * 
	 * @see org.olat.modules.webFeed.managers.FeedManager#createPodcastResource()
	 */
	@Override
	public OLATResourceable createBlogResource() {
		final FeedFileResource blogResource = new BlogFileResource();
		return createFeedResource(blogResource);
	}

	/**
	 * @param feedResource
	 * @return The feed resourcable after creation on file system
	 */
	private OLATResourceable createFeedResource(final FeedFileResource feedResource) {
		final OLATResource ores = resourceManager.createOLATResourceInstance(feedResource);
		resourceManager.saveOLATResource(ores);
		final Feed feed = new Feed(feedResource);
		final VFSContainer podcastContainer = getFeedContainer(feedResource);
		final VFSLeaf leaf = podcastContainer.createChildLeaf(FEED_FILE_NAME);
		podcastContainer.createChildContainer(MEDIA_DIR);
		podcastContainer.createChildContainer(ITEMS_DIR);
		XStreamHelper.writeObject(xstream, leaf, feed);
		return feedResource;
	}

	/**
	 * Instanciates or just returns the feed cache. (Protected for better performance)
	 * 
	 * @return The feed cache
	 */
	protected CacheWrapper initFeedCache() {
		if (feedCache == null) {
			final OLATResourceable ores = OresHelper.createOLATResourceableType(Feed.class);
			coordinator.getSyncer().doInSync(ores, new SyncerExecutor() {
				@Override
				@SuppressWarnings("synthetic-access")
				public void execute() {
					if (feedCache == null) {
						feedCache = coordinator.getCacher().getOrCreateCache(this.getClass(), "feed");
					}
				}
			});
		}
		return feedCache;
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#delete(org.olat.core.id.OLATResourceable)
	 */
	@Override
	public void delete(OLATResourceable feed) {
		fileResourceManager.deleteFileResource(feed);
		// Delete comments and ratings
		final CommentAndRatingService commentAndRatingService = getCommentAndRatingService();
		if (commentAndRatingService != null) {
			commentAndRatingService.init(null, feed, null, true, false);
			commentAndRatingService.deleteAllIgnoringSubPath();
		}
		//
		feed = null;
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#getFeed(org.olat.core.id.OLATResourceable)
	 */
	@Override
	public Feed getFeed(final OLATResourceable ores) {
		return getFeed(ores, true);
	}

	/**
	 * Load the feed and all the items and put it into the cache. If it's already in the cache, use the version from the cache. The feed object is shared between users.
	 * 
	 * @param ores
	 * @param inSync
	 * @return
	 */
	private Feed getFeed(final OLATResourceable ores, final boolean inSync) {
		// Attempt to fetch the feed from the cache
		Feed myFeed = (Feed) initFeedCache().get(ores.getResourceableId().toString());
		if (myFeed == null) {
			// Load the feed from file and put it to the cache
			final VFSContainer feedContainer = getFeedContainer(ores);
			myFeed = readFeedFile(feedContainer);
			if (myFeed != null) {
				// Reset the feed id. (This is necessary for imported feeds.)
				myFeed.setId(ores.getResourceableId());
				// Load all items
				getItems(myFeed);
				// See if there are some version issues that need to be fixed now
				fixFeedVersionIssues(myFeed);
				// Get repository entry information
				enrichFeedByRepositoryEntryInfromation(myFeed);
				// must be final for sync
				final Feed feed = myFeed;
				syncedFeedCacheUpdate(feed, inSync);
			}
		}
		return myFeed;
	}

	/**
	 * Puts the feed to the feedCache in a synchronized manner.
	 * 
	 * @param ores
	 * @param feed
	 */
	void syncedFeedCacheUpdate(final Feed feed, final boolean inSync) {
		initFeedCache();
		if (inSync) {
			coordinator.getSyncer().doInSync(feed, new SyncerExecutor() {
				@Override
				public void execute() {
					// update and put behaves the same way
					feedCache.update(feed.getResourceableId().toString(), feed);
				}
			});
		} else {
			feedCache.update(feed.getResourceableId().toString(), feed);
		}
	}

	/**
	 * Gets the items of the feed from the feed or load them from the files system.
	 * 
	 * @param feed
	 * @return The items of the feed
	 */
	private List<Item> getItems(final Feed feed) {
		List<Item> items = new ArrayList<Item>();
		if (feed.isExternal() && (feed.getItemIds() == null || feed.getItemIds().size() == 0)) {
			items = getItemsFromFeed(feed);
		} else if (feed.getItemIds() != null) {
			if (feed.getItems() != null && feed.getItems().size() == feed.getItemIds().size()) {
				// items already loaded, use the loaded items
				items = feed.getItems();
			} else {
				// reload all items
				items = loadItems(feed);
			}
		}
		feed.setItems(items);
		return items;
	}

	/**
	 * Update the feed resource with the latest set properties in the repository entry.
	 * <p>
	 * Properties are:
	 * <ul>
	 * <li>Title
	 * <li>Author
	 * <li>Descripion (wiki style in repository)
	 * <li>Image
	 * </ul>
	 * 
	 * @param feed
	 */
	private void enrichFeedByRepositoryEntryInfromation(final Feed feed) {
		final RepositoryEntry entry = getRepositoryEntry(feed);
		if (entry != null && feed != null) {
			final Date whenTheFeedWasLastModified = feed.getLastModified();
			if (whenTheFeedWasLastModified == null || entry.getLastModified().after(whenTheFeedWasLastModified)) {
				// entry is newer then feed, update feed
				feed.setTitle(entry.getDisplayname());
				// Formatter.formatWikiMarkup(entry.getDescription())
				feed.setDescription(entry.getDescription());
				feed.setAuthor(entry.getInitialAuthor());
				// Update the image
				final VFSContainer repoHome = new LocalFolderImpl(new File(FolderConfig.getCanonicalRoot() + FolderConfig.getRepositoryHome()));
				final String imageFilename = RepositoryEntryImageController.getImageFilename(entry);
				final VFSItem repoEntryImage = repoHome.resolve(imageFilename);
				if (repoEntryImage != null) {
					getFeedMediaContainer(feed).copyFrom(repoEntryImage);
					final VFSLeaf newImage = (VFSLeaf) getFeedMediaContainer(feed).resolve(imageFilename);
					if (newImage != null) {
						feed.setImageName(imageFilename);
					}
				} else {
					// There's no repo entry image -> delete the feed image as well.
					deleteImage(feed);
				}
			}
		}
	}

	/**
	 * @param ores
	 * @return The repository entry of ores or null
	 */
	private RepositoryEntry getRepositoryEntry(final OLATResourceable ores) {
		return repositoryManager.lookupRepositoryEntry(ores, false);
	}

	/**
	 * Returns the feed in the given container. It is public to be accessible by PodcastFileResource.
	 * <p>
	 * Note: this does ONLY read the file from disk, it does NOT put the feed into the feed cache nor does it load the associated items. Do not use this method generally,
	 * use getFeedLight() instead!
	 * 
	 * @param feedContainer
	 * @return The Feed upon success
	 */
	@Override
	public Feed readFeedFile(final VFSContainer feedContainer) {
		Feed myFeed = null;
		if (feedContainer != null) {
			final VFSLeaf leaf = (VFSLeaf) feedContainer.resolve(FEED_FILE_NAME);
			if (leaf != null) {
				myFeed = (Feed) XStreamHelper.readObject(xstream, leaf.getInputStream());
			}
		} else {
			log.error("Feed xml-file could not be found on file system. Feed container: " + feedContainer);
		}
		return myFeed;
	}

	/**
	 * Method that checks the current feed data model version and applies necessary fixes to the model. Since feeds can be exported and imported this fixes must apply on
	 * the fly and can't be implemented with the system upgrade mechanism.
	 * 
	 * @param feed
	 */
	private void fixFeedVersionIssues(final Feed feed) {
		if (feed == null) { return; }
		if (feed.getModelVersion() < 2) {
			// The model version of models before the introduction of the model version
			// will have a model version=0 (set by xstream)
			if (PodcastFileResource.TYPE_NAME.equals(feed.getResourceableTypeName())) {
				if (feed.isInternal()) {
					// In model 1 the podcast episode items were set as drafts which resulted
					// in invisible episodes. They have to be set to published. (OLAT-5767)
					for (final Item episode : feed.getItems()) {
						// Mark episode as published and persist the item file on disk
						episode.setDraft(false);
						updateItemFileWithoutDoInSync(episode, feed);
					}
				}
			}
			// Set feed model to newest version and persist feed file on disk
			feed.setModelVersion(Feed.CURRENT_MODEL_VERSION);
			final VFSContainer container = getFeedContainer(feed);
			final VFSLeaf leaf = (VFSLeaf) container.resolve(FEED_FILE_NAME);
			XStreamHelper.writeObject(xstream, leaf, feed);
			//
			log.info("Updated feed::" + feed.getResourceableTypeName() + "::" + feed.getResourceableId() + " to version::" + Feed.CURRENT_MODEL_VERSION);
		}
	}

	/**
	 * Load all items of the feed (from file system or the external feed)
	 * 
	 * @param feed
	 */
	@Override
	public List<Item> loadItems(final Feed feed) {
		List<Item> items = new ArrayList<Item>();

		if (feed.isExternal()) {
			items = getItemsFromFeed(feed);

		} else if (feed.isInternal()) {
			// Load from virtual file system
			final VFSContainer itemsContainer = getItemsContainer(feed);

			for (final String itemId : feed.getItemIds()) {
				final VFSItem itemContainer = itemsContainer.resolve(itemId);
				final Item item = loadItem(itemContainer);
				if (item != null) {
					items.add(item);
				}
			}
		}
		// else, this feed is undefined and should have no items. It probably has
		// just been created.
		feed.setItems(items);
		return items;
	}

	/**
	 * Read the items of an external feed url
	 * 
	 * @param feedURL
	 * @return The list of all items
	 */
	// ROME library uses untyped lists
	@SuppressWarnings("unchecked")
	private List<Item> getItemsFromFeed(final Feed extFeed) {
		final List<Item> items = new ArrayList<Item>();
		final SyndFeed feed = getSyndFeed(extFeed);
		if (feed != null) {
			final List<SyndEntry> entries = feed.getEntries();
			for (final SyndEntry entry : entries) {
				final Item item = convertToItem(entry);
				items.add(item);
			}
		}
		return items;
	}

	/**
	 * @param extFeed
	 * @param items
	 */
	private SyndFeed getSyndFeed(final Feed extFeed) {
		SyndFeed feed = null;
		final SyndFeedInput input = new SyndFeedInput();
		final String feedURL = extFeed.getExternalFeedUrl();
		try {
			final URL url = new URL(feedURL);
			feed = input.build(new XmlReader(url));
			// also add the external image url just in case we'll need it later
			addExternalImageURL(feed, extFeed);
		} catch (final MalformedURLException e) {
			log.info("The externalFeedUrl is invalid: " + feedURL);
		} catch (final FeedException e) {
			log.info("The read feed is invalid: " + feedURL);
		} catch (final IOException e) {
			log.info("Cannot read from feed: " + feedURL);
		} finally {
			// No streams to be closed
		}
		return feed;
	}

	/**
	 * @param extFeed
	 * @param feed
	 */
	private void addExternalImageURL(final SyndFeed feed, final Feed extFeed) {
		final SyndImage img = feed.getImage();
		if (img != null) {
			extFeed.setExternalImageURL(img.getUrl());
		} else {
			extFeed.setExternalImageURL(null);
		}
	}

	/**
	 * Converts a <code>SyndEntry</code> into an <code>Item</code>
	 * 
	 * @param entry The SyndEntry
	 * @return The Item
	 */
	private Item convertToItem(final SyndEntry entry) {
		// A SyncEntry can potentially have many attributes like title, description,
		// guid, link, enclosure or content. In OLAT, however, items are limited
		// to the attributes, title, description and one media file (called
		// enclosure in RSS) for simplicity.
		final Item e = new Item();
		e.setTitle(entry.getTitle());
		e.setDescription(entry.getDescription() != null ? entry.getDescription().getValue() : null);
		// Extract content objects from syndication item
		final StringBuffer sb = new StringBuffer();
		for (final SyndContent content : (List<SyndContent>) entry.getContents()) {
			// we don't check for type, assume it is html or txt
			if (sb.length() > 0) {
				sb.append("<p />");
			}
			sb.append(content.getValue());
		}
		// Set aggregated content from syndication item as our content
		if (sb.length() > 0) {
			e.setContent(sb.toString());
		}
		e.setGuid(entry.getUri());
		e.setExternalLink(entry.getLink());
		e.setLastModified(entry.getUpdatedDate());
		e.setPublishDate(entry.getPublishedDate());

		for (final Object enclosure : entry.getEnclosures()) {
			if (enclosure instanceof SyndEnclosure) {
				final SyndEnclosure syndEnclosure = (SyndEnclosure) enclosure;
				final Enclosure media = new Enclosure();
				media.setExternalUrl(syndEnclosure.getUrl());
				media.setType(syndEnclosure.getType());
				media.setLength(syndEnclosure.getLength());
				e.setEnclosure(media);
			}
			// Break after one cycle because only one media file is supported
			break;
		}
		return e;
	}

	/**
	 * Loads an item from file. Used for validation in PodcastFileResource, that's why its public and static.
	 * 
	 * @param container
	 * @return The item
	 */
	@Override
	public Item loadItem(final VFSItem container) {
		VFSLeaf itemLeaf = null;
		Item item = null;

		if (container != null) {
			itemLeaf = (VFSLeaf) container.resolve(ITEM_FILE_NAME);
			if (itemLeaf != null) {
				item = (Item) XStreamHelper.readObject(xstream, itemLeaf.getInputStream());
			}
		}
		return item;
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#remove(org.olat.modules.webFeed.models.Item, org.olat.modules.webFeed.models.Feed)
	 */
	@Override
	public void remove(final Item item, final Feed feed) {
		// synchronize all feed item CUD operations on this feed to prevend
		// overwriting of changes
		// o_clusterOK by:fg
		coordinator.getSyncer().doInSync(feed, new SyncerCallback<Object>() {
			@Override
			public VFSLeaf execute() {
				// reload feed to prevent stale feed overwriting
				@SuppressWarnings("synthetic-access")
				final Feed reloadedFeed = getFeed(feed, false);
				reloadedFeed.remove(item);
				// If the last item has been removed, set the feed to undefined.
				// The user can then newly decide whether to add items manually or from
				// an external source.
				if (!reloadedFeed.hasItems()) {
					// set undefined
					reloadedFeed.setExternal(null);
				}

				// Delete the item's container on the virtual file system.
				final VFSContainer itemContainer = getItemContainer(item, reloadedFeed);
				if (itemContainer != null) {
					itemContainer.delete();
				}

				// Update feed
				reloadedFeed.setLastModified(new Date());
				update(reloadedFeed, false);

				// Delete comments and ratings
				final CommentAndRatingService commentAndRatingService = getCommentAndRatingService();
				if (commentAndRatingService != null) {
					commentAndRatingService.init(null, feed, item.getGuid(), true, false);
					commentAndRatingService.deleteAll();
				}
				//
				return null;
			}
		});
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#addItem(org.olat.modules.webFeed.models.Item, org.olat.core.gui.components.form.flexible.elements.FileElement,
	 *      org.olat.modules.webFeed.models.Feed)
	 */
	@Override
	public void addItem(final Item item, final FileElement file, final Feed feed) {
		if (feed.isInternal()) {
			// synchronize all feed item CUD operations on this feed to prevent
			// overwriting of changes
			// o_clusterOK by:fg
			coordinator.getSyncer().doInSync(feed, new SyncerCallback<Object>() {
				@Override
				@SuppressWarnings("synthetic-access")
				public VFSLeaf execute() {
					// reload feed to prevent stale feed overwriting
					final Feed reloadedFeed = getFeed(feed, false);
					// Set the current date as published date.
					if (item.getPublishDate() == null) {
						item.setPublishDate(new Date());
					}

					// Set the file properties to the item and store the file
					setEnclosure(file, item, reloadedFeed);

					// Write the item.xml file
					final VFSContainer itemContainer = createItemContainer(feed, item);
					final VFSLeaf itemFile = itemContainer.createChildLeaf(ITEM_FILE_NAME);
					XStreamHelper.writeObject(xstream, itemFile, item);

					// finally add the item to the feed
					reloadedFeed.add(item);
					reloadedFeed.setLastModified(item.getLastModified());

					// Save the feed (needed because of itemIds list)
					update(reloadedFeed, false);
					return null;
				}
			});
		}
	}

	/**
	 * Method to write the feed object to disk using xstream.
	 * <p>
	 * This method MUST be called from a cluster-synced block with the most recent feed object from the cluster feed cache!
	 * 
	 * @param feed
	 */
	void update(final Feed feed, final boolean inSync) {
		feed.setLastModified(new Date());

		// If the feed url has changed, the items must be reloaded.
		if (feed.isExternal()) {
			final String oldFeed = getFeed(feed, inSync).getExternalFeedUrl();
			final String newFeed = feed.getExternalFeedUrl();
			if (newFeed != null && !newFeed.equals("")) {
				if (!newFeed.equals(oldFeed)) {
					loadItems(feed);
				}
			}
		}
		// Write the feed file. (Items are saved when adding them.)
		feed.sortItems();
		final VFSContainer container = getFeedContainer(feed);
		final VFSLeaf leaf = (VFSLeaf) container.resolve(FEED_FILE_NAME);
		XStreamHelper.writeObject(xstream, leaf, feed);
		initFeedCache().update(feed.getResourceableId().toString(), feed);
		enrichRepositoryEntryByFeedInformation(feed);
	}

	/**
	 * A unique key for the item of the feed. Can be used e.g. for locking and caching.
	 * 
	 * @param itemId
	 * @param feedId
	 * @return A unique key for the item of the feed
	 */
	private String itemKey(final String itemId, final String feedId) {
		final StringBuffer key = new StringBuffer();
		key.append("feed").append(feedId);
		key.append("_item_").append(itemId);
		return key.toString();
	}

	/**
	 * A unique key for the item of the feed. Can be used e.g. for locking and caching. (Protected for performance reasons)
	 * 
	 * @param item
	 * @param feed
	 * @return A unique key for the item of the feed
	 */
	protected String itemKey(final Item item, final OLATResourceable feed) {
		final String key = itemKey(item.getGuid(), feed.getResourceableId().toString());
		return key;
	}

	/**
	 * The unique keys for the items of the feed. (Protected for performance reasons)
	 * 
	 * @param feed
	 * @return The unique keys for the items of the feed
	 */
	protected String[] itemKeys(final Feed feed) {
		final List<Item> items = feed.getItems();
		String[] keys = null;
		if (items != null) {
			final int size = items.size();
			keys = new String[size];
			for (int i = 0; i < size; i++) {
				keys[i] = itemKey(items.get(i), feed);
			}
		}
		return keys;
	}

	/**
	 * Update the repository entry with the latest set properties in the feed resource.
	 * <p>
	 * Properties are:
	 * <ul>
	 * <li>Title
	 * <li>Author
	 * <li>Descripion (wiki style in repository)
	 * <li>Image
	 * </ul>
	 * 
	 * @param feed
	 */
	void enrichRepositoryEntryByFeedInformation(final Feed feed) {
		final RepositoryEntry entry = getRepositoryEntry(feed);
		if (entry != null && feed != null) {
			final Date whenTheFeedWasLastModified = feed.getLastModified();
			if (whenTheFeedWasLastModified != null && entry.getLastModified().before(whenTheFeedWasLastModified)) {
				// feed is newer than repository entry, update repository entry
				entry.setDisplayname(feed.getTitle());
				entry.setDescription(feed.getDescription());
				// Update the image
				final VFSContainer repoHome = new LocalFolderImpl(new File(FolderConfig.getCanonicalRoot() + FolderConfig.getRepositoryHome()));
				final String imageFilename = RepositoryEntryImageController.getImageFilename(entry);
				final VFSItem oldEntryImage = repoHome.resolve(imageFilename);
				if (oldEntryImage != null) {
					// Delete the old File
					oldEntryImage.delete();
				}
				// Copy the feed image to the repository home folder unless it was
				// deleted.
				final String feedImage = feed.getImageName();
				if (feedImage != null) {
					final VFSItem newImage = getFeedMediaContainer(feed).resolve(feedImage);
					if (newImage == null) {
						// huh? image defined but not found on disk - remove image from feed
						deleteImage(feed);
					} else {
						repoHome.copyFrom(newImage);
						final VFSItem newEntryImage = repoHome.resolve(feed.getImageName());
						newEntryImage.rename(imageFilename);
					}
				}
			}
		}
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#getFeedContainer(org.olat.core.id.OLATResourceable)
	 */
	@Override
	public VFSContainer getFeedContainer(final OLATResourceable ores) {
		VFSContainer feedDir = null;

		if (ores != null) {
			final VFSContainer resourceDir = getResourceContainer(ores);
			final String feedDirName = getFeedKind(ores);
			feedDir = (VFSContainer) resourceDir.resolve(feedDirName);
			if (feedDir == null) {
				// If the folder does not exist create it.
				feedDir = resourceDir.createChildContainer(feedDirName);
			}
		}
		return feedDir;
	}

	/**
	 * @param ores
	 * @return The resource (root) container of the feed
	 */
	private VFSContainer getResourceContainer(final OLATResourceable ores) {
		return fileResourceManager.getFileResourceRootImpl(ores);
	}

	/**
	 * @param file
	 * @param item
	 * @param feed
	 */
	public void setEnclosure(final FileElement file, final Item item, final Feed feed) {
		if (file != null) {
			final VFSContainer itemMediaContainer = (VFSContainer) getItemContainer(item, feed).resolve(MEDIA_DIR);

			// Empty the container and write the new media file (called 'enclosure' in
			// rss)
			for (final VFSItem fileItem : itemMediaContainer.getItems()) {
				if (!fileItem.getName().startsWith(".")) {
					fileItem.delete();
				}
			}
			// Move uploaded file to our container
			final VFSItem movedItem = file.moveUploadFileTo(itemMediaContainer);
			// Rename to something save for the file system
			final String saveFileName = Formatter.makeStringFilesystemSave(file.getUploadFileName());

			movedItem.rename(saveFileName);

			// Update the enclosure meta data
			final Enclosure enclosure = new Enclosure();
			enclosure.setFileName(saveFileName);
			enclosure.setLength(file.getUploadSize());
			enclosure.setType(file.getUploadMimeType());

			item.setEnclosure(enclosure);
		}
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#getItemContainer(org.olat.modules.webFeed.models.Item, org.olat.modules.webFeed.models.Feed)
	 */
	@Override
	public VFSContainer getItemContainer(final Item item, final Feed feed) {
		return (VFSContainer) getItemsContainer(feed).resolve(item.getGuid());
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#getItemMediaContainer(org.olat.modules.webFeed.models.Item, org.olat.modules.webFeed.models.Feed)
	 */
	@Override
	public VFSContainer getItemMediaContainer(final Item item, final Feed feed) {
		VFSContainer itemMediaContainer = null;
		final VFSContainer itemContainer = getItemContainer(item, feed);
		if (itemContainer != null) {
			itemMediaContainer = (VFSContainer) itemContainer.resolve(MEDIA_DIR);
		}

		return itemMediaContainer;
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#getItemMediaContainer(org.olat.modules.webFeed.models.Item, org.olat.modules.webFeed.models.Feed)
	 */
	@Override
	public File getItemEnclosureFile(final Item item, final Feed feed) {
		final VFSContainer mediaDir = getItemMediaContainer(item, feed);
		final Enclosure enclosure = item.getEnclosure();
		File file = null;
		if (mediaDir != null && enclosure != null) {
			final VFSLeaf mediaFile = (VFSLeaf) mediaDir.resolve(enclosure.getFileName());
			if (mediaFile != null && mediaFile instanceof LocalFileImpl) {
				file = ((LocalFileImpl) mediaFile).getBasefile();
			}
		}
		return file;
	}

	/**
	 * Returns the items container of feed
	 * 
	 * @param feed
	 * @return The container of all items
	 */
	private VFSContainer getItemsContainer(final OLATResourceable feed) {
		VFSContainer items = null;
		final VFSContainer feedContainer = getFeedContainer(feed);
		// If feed container is null we're in trouble
		items = (VFSContainer) feedContainer.resolve(ITEMS_DIR);
		if (items == null) {
			items = feedContainer.createChildContainer(ITEMS_DIR);
		}
		return items;
	}

	/**
	 * Returns the container of media files
	 * 
	 * @param feed
	 * @return The feed media container
	 */
	public VFSContainer getFeedMediaContainer(final OLATResourceable feed) {
		VFSContainer media = null;
		final VFSContainer feedContainer = getFeedContainer(feed);
		// If feed container is null we're in trouble
		media = (VFSContainer) feedContainer.resolve(MEDIA_DIR);
		if (media == null) {
			media = feedContainer.createChildContainer(MEDIA_DIR);
		}
		return media;
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#updateItem(org.olat.modules.webFeed.models.Item,
	 *      org.olat.core.gui.components.form.flexible.elements.FileElement, org.olat.modules.webFeed.models.Feed)
	 */
	@Override
	public void updateItem(final Item item, final FileElement file, final Feed feed) {
		if (feed.isInternal()) {
			// synchronize all feed item CUD operations on this feed to prevent
			// overwriting of changes
			// o_clusterOK by:fg
			coordinator.getSyncer().doInSync(feed, new SyncerCallback<Object>() {
				@Override
				@SuppressWarnings("synthetic-access")
				public VFSLeaf execute() {
					// reload feed to prevent stale feed overwriting
					final Feed reloadedFeed = getFeed(feed, false);
					if (reloadedFeed.getItemIds().contains(item.getGuid())) {
						if (file != null) {
							setEnclosure(file, item, reloadedFeed);
						}
						updateItemFileWithoutDoInSync(item, reloadedFeed);
						update(feed, false);
					} else {
						// do nothing, item was deleted by someone in the meantime
					}
					return null;
				}
			});
		}
	}

	/**
	 * Internal helper method to update an item without adding the necessary do-in-sync wrapper. This should only be called from within a code block that is
	 * cluster-synced!
	 * 
	 * @param item
	 * @param feed
	 */
	private void updateItemFileWithoutDoInSync(final Item item, final Feed feed) {
		// Write the item.xml file
		final VFSLeaf itemFile = (VFSLeaf) getItemContainer(item, feed).resolve(ITEM_FILE_NAME);
		XStreamHelper.writeObject(xstream, itemFile, item);
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#updateFeedMode(java.lang.Boolean, org.olat.modules.webFeed.models.Feed)
	 */
	@Override
	public Feed updateFeedMode(final Boolean external, final Feed feed) {
		return coordinator.getSyncer().doInSync(feed, new SyncerCallback<Feed>() {
			@Override
			@SuppressWarnings("synthetic-access")
			public Feed execute() {
				// reload feed to prevent stale feed overwriting
				final Feed reloadedFeed = getFeed(feed, false);
				reloadedFeed.setExternal(external);
				update(reloadedFeed, false);
				return reloadedFeed;
			}
		});
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#updateFeedMetadata(org.olat.modules.webFeed.models.Feed)
	 */
	@Override
	public Feed updateFeedMetadata(final Feed feed) {
		// reload feed to prevent stale feed overwriting
		final Feed reloadedFeed = getFeed(feed);
		reloadedFeed.setAuthor(feed.getAuthor());
		reloadedFeed.setDescription(feed.getDescription());
		reloadedFeed.setExternalFeedUrl(feed.getExternalFeedUrl());
		reloadedFeed.setExternalImageURL(feed.getExternalImageURL());
		reloadedFeed.setImageName(feed.getImageName());
		reloadedFeed.setTitle(feed.getTitle());

		return coordinator.getSyncer().doInSync(feed, new SyncerCallback<Feed>() {
			@Override
			public Feed execute() {
				update(reloadedFeed, false);
				return reloadedFeed;
			}
		});
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#createMediaResource(java.lang.Long, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public MediaResource createItemMediaFile(final OLATResourceable feed, final String itemId, final String fileName) {
		VFSMediaResource mediaResource = null;
		// Brute force method for fast delivery
		try {
			VFSItem item = getItemsContainer(feed);
			item = item.resolve(itemId);
			item = item.resolve(MEDIA_DIR);
			item = item.resolve(fileName);
			mediaResource = new VFSMediaResource((VFSLeaf) item);
		} catch (final NullPointerException e) {
			log.debug("Media resource could not be created from file: ", fileName);
		}
		return mediaResource;
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#createMediaResource(java.lang.Long, java.lang.String, java.lang.String)
	 */
	@Override
	public MediaResource createFeedMediaFile(final OLATResourceable feed, final String fileName) {
		VFSMediaResource mediaResource = null;
		// Brute force method for fast delivery
		try {
			VFSItem item = getFeedMediaContainer(feed);
			item = item.resolve(fileName);
			mediaResource = new VFSMediaResource((VFSLeaf) item);
		} catch (final NullPointerException e) {
			log.debug("Media resource could not be created from file: ", fileName);
		}
		return mediaResource;
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#getFeedUri(org.olat.modules.webFeed.models.Feed, java.lang.Long)
	 */
	@Override
	public String getFeedBaseUri(final Feed feed, final Identity identity, final Long courseId, final String nodeId) {
		return FeedMediaDispatcher.getFeedBaseUri(feed, identity, courseId, nodeId);
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#createFeedMediaResource(java.lang.Long, java.lang.String, java.lang.Long)
	 */
	@Override
	public MediaResource createFeedFile(final OLATResourceable ores, final Identity identity, final Long courseId, final String nodeId) {
		MediaResource media = null;
		final Feed feed = getFeed(ores);

		if (feed != null) {
			final SyndFeed rssFeed = new RSSFeed(feed, identity, courseId, nodeId);
			media = new SyndFeedMediaResource(rssFeed);
		}
		return media;
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#isValidFeedUrl(java.lang.String)
	 */
	@Override
	public ValidatedURL validateFeedUrl(String url, final String type) {
		final SyndFeedInput input = new SyndFeedInput();

		boolean modifiedProtocol = false;
		try {
			if (url != null) {
				url = url.trim();
			}
			if (url.startsWith("feed") || url.startsWith("itpc")) {
				// accept feed(s) urls like generated in safari browser
				url = "http" + url.substring(4);
				modifiedProtocol = true;
			}
			final URL realUrl = new URL(url);
			final SyndFeed feed = input.build(new XmlReader(realUrl));
			if (!feed.getEntries().isEmpty()) {
				// check for enclosures
				final SyndEntry entry = (SyndEntry) feed.getEntries().get(0);
				if (type != null && type.indexOf("BLOG") >= 0) { return new ValidatedURL(url, ValidatedURL.State.VALID); }
				if (entry.getEnclosures().isEmpty()) { return new ValidatedURL(url, ValidatedURL.State.NO_ENCLOSURE); }
			}
			// The feed was read successfully
			return new ValidatedURL(url, ValidatedURL.State.VALID);
		} catch (final ParsingFeedException e) {
			if (modifiedProtocol) {
				// fallback for SWITCHcast itpc -> http -> https
				url = "https" + url.substring(4);
				return validateFeedUrl(url, type);
			}
			return new ValidatedURL(url, ValidatedURL.State.NOT_FOUND);
		} catch (final FileNotFoundException e) {
			return new ValidatedURL(url, ValidatedURL.State.NOT_FOUND);
		} catch (final MalformedURLException e) {
			// The url is invalid
		} catch (final FeedException e) {
			// The feed couldn't be read
		} catch (final IOException e) {
			// Maybe network or file problems
		} catch (final IllegalArgumentException e) {
			// something very wrong with the feed
		}
		return new ValidatedURL(url, ValidatedURL.State.MALFORMED);
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#copy(org.olat.core.id.OLATResourceable)
	 */
	@Override
	public OLATResourceable copy(final OLATResourceable feed) {
		final FileResourceManager manager = FileResourceManager.getInstance();
		final OLATResourceable copyResource = manager.createCopy(feed, getFeedKind(feed));
		// Adjust resource ID in copy to new resource ID, bypass any caches, read
		// and write directly
		final VFSContainer copyContainer = getFeedContainer(copyResource);
		final VFSLeaf leaf = (VFSLeaf) copyContainer.resolve(FEED_FILE_NAME);
		if (leaf != null) {
			final Feed copyFeed = (Feed) XStreamHelper.readObject(xstream, leaf.getInputStream());
			copyFeed.setId(copyResource.getResourceableId());
			XStreamHelper.writeObject(xstream, leaf, copyFeed);
		}
		//
		return copyResource;
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#getFeedArchiveMediaResource(org.olat.core.id.OLATResourceable)
	 */
	@Override
	public VFSMediaResource getFeedArchiveMediaResource(final OLATResourceable resource) {
		final VFSLeaf zip = getFeedArchive(resource);
		return new VFSMediaResource(zip);
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#getFeedArchive(org.olat.core.id.OLATResourceable)
	 */
	@Override
	public VFSLeaf getFeedArchive(final OLATResourceable resource) {
		final VFSContainer rootContainer, feedContainer;
		rootContainer = getResourceContainer(resource);
		feedContainer = getFeedContainer(resource);

		// prepare fallback for author if needed
		final Feed feed = getFeed(resource, true);
		if (feed.isInternal()) {
			coordinator.getSyncer().doInSync(feed, new SyncerCallback<Boolean>() {
				@Override
				@SuppressWarnings("synthetic-access")
				public Boolean execute() {
					for (final Item item : getItems(feed)) {
						if (!item.isAuthorFallbackSet()) {
							// get used authorKey first
							final String author = item.getAuthor();
							if (StringHelper.containsNonWhitespace(author)) {
								// set author fallback
								item.setAuthor(author);
								// update item.xml
								final VFSContainer itemContainer = getItemContainer(item, feed);
								if (itemContainer != null) {
									final VFSLeaf itemFile = (VFSLeaf) itemContainer.resolve(ITEM_FILE_NAME);
									XStreamHelper.writeObject(xstream, itemFile, item);
								}
							}
						}
					}
					return Boolean.TRUE;
				}
			});
		}

		// synchronize all zip processes for this feed
		// o_clusterOK by:fg
		final VFSLeaf zip = coordinator.getSyncer().doInSync(resource, new SyncerCallback<VFSLeaf>() {
			@Override
			public VFSLeaf execute() {
				// Delete the old archive and recreate it from scratch
				final String zipFileName = getZipFileName(resource);
				final VFSItem oldArchive = rootContainer.resolve(zipFileName);
				if (oldArchive != null) {
					oldArchive.delete();
				}
				ZipUtil.zip(feedContainer.getItems(), rootContainer.createChildLeaf(zipFileName), false);
				return (VFSLeaf) rootContainer.resolve(zipFileName);
			}
		});
		return zip;
	}

	/**
	 * Returns the file name of the archive that is to be exported. Depends on the kind of the resource.
	 * 
	 * @param resource
	 * @return The zip archive file name
	 */
	String getZipFileName(final OLATResourceable resource) {
		return getFeedKind(resource) + ".zip";
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#releaseLock(org.olat.core.util.coordinate.LockResult)
	 */
	@Override
	public void releaseLock(final LockResult lock) {
		if (lock != null) {
			coordinator.getLocker().releaseLock(lock);
		}
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#acquireLock(org.olat.core.id.OLATResourceable, org.olat.core.id.Identity)
	 */
	@Override
	public LockResult acquireLock(final OLATResourceable feed, final Identity identity) {
		// OLATResourceable itemLock =
		// OresHelper.createOLATResourceableInstance("podcastlock_" +
		// feed.getResourceableId() + "_meta", item.getId())
		final LockResult lockResult = coordinator.getLocker().acquireLock(feed, identity, null);
		return lockResult;
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#acquireLock(org.olat.core.id.OLATResourceable, org.olat.modules.webFeed.models.Item, org.olat.core.id.Identity)
	 */
	@Override
	public LockResult acquireLock(final OLATResourceable feed, final Item item, final Identity identity) {
		final String key = itemKey(item, feed);
		final OLATResourceable itemResource = OresHelper.createOLATResourceableType(key);
		final LockResult lockResult = coordinator.getLocker().acquireLock(itemResource, identity, key);
		return lockResult;
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#setImage(org.olat.core.gui.components.form.flexible.elements.FileElement, org.olat.modules.webFeed.models.Feed)
	 */
	@Override
	public void setImage(final FileElement image, final Feed feed) {
		if (image != null) {
			// Delete the old image
			deleteImage(feed);
			// Save the new image
			VFSLeaf imageLeaf = image.moveUploadFileTo(getFeedMediaContainer(feed));
			// Resize to same dimension box as with repo meta image
			final VFSLeaf tmp = getFeedMediaContainer(feed).createChildLeaf("" + CodeHelper.getRAMUniqueID());
			ImageHelper.scaleImage(imageLeaf, tmp, PICTUREWIDTH, PICTUREWIDTH);
			imageLeaf.delete();
			imageLeaf = tmp;
			// Make file system save
			final String saveFileName = Formatter.makeStringFilesystemSave(image.getUploadFileName());
			imageLeaf.rename(saveFileName);
			// Update metadata
			feed.setImageName(saveFileName);
		}
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#deleteImage(org.olat.modules.webFeed.models.Feed)
	 */
	@Override
	public void deleteImage(final Feed feed) {
		final VFSContainer mediaContainer = getFeedMediaContainer(feed);
		final String imageName = feed.getImageName();
		if (imageName != null) {
			final VFSLeaf image = (VFSLeaf) mediaContainer.resolve(imageName);
			if (image != null) {
				image.delete();
			}
			feed.setImageName(null);
		}
	}

	/**
	 * @see org.olat.modules.webFeed.managers.FeedManager#createItemContainer(org.olat.modules.webFeed.models.Feed, org.olat.modules.webFeed.models.Item)
	 */
	@Override
	public VFSContainer createItemContainer(final Feed feed, final Item item) {
		VFSContainer itemContainer = getItemContainer(item, feed);
		if (itemContainer == null) {
			if (!StringHelper.containsNonWhitespace(item.getGuid())) { throw new AssertException(
					"Programming error, item has no GUID set, can not create an item container for this item"); }
			itemContainer = getItemsContainer(feed).createChildContainer(item.getGuid());
		}
		// prepare media container
		final VFSContainer mediaContainer = getItemMediaContainer(item, feed);
		if (mediaContainer == null) {
			itemContainer.createChildContainer(MEDIA_DIR);
		}
		return itemContainer;
	}

}
