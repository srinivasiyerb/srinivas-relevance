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
package org.olat.fileresource.types;

import java.io.File;
import java.util.List;

import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.fileresource.FileResourceManager;
import org.olat.modules.webFeed.managers.FeedManager;
import org.olat.modules.webFeed.models.Feed;
import org.olat.modules.webFeed.models.Item;

/**
 * Abstract feed file resource class. Used to decrease redundancy.
 * <P>
 * Initial Date: Aug 3, 2009 <br>
 * 
 * @author gwassmann
 */
public abstract class FeedFileResource extends FileResource {

	public FeedFileResource(final String type) {
		super.setTypeName(type);
	}

	public FeedFileResource(final File root, final File resourceFolder, final String type) {
		super.setTypeName(type);
		// After unziping the uploaded folder, I would like to copy it to the
		// appropriate location right away (and not on the next read). So, I put the
		// code here. Note that this constructor is also called on copying a
		// resource. We know that the resource folder is valid.

		// Let's now copy the resource folder to the root folder.
		VFSContainer rootContainer = new LocalFolderImpl(root);
		final String folderName = FeedManager.getInstance().getFeedKind(this);
		if (rootContainer.resolve(folderName) == null) {
			// If the podcast directory doesn't exist yet, create it and copy content
			// from uploaded folder
			rootContainer = rootContainer.createChildContainer(folderName);
			final VFSContainer resourceContainer = new LocalFolderImpl(resourceFolder);
			for (final VFSItem item : resourceContainer.getItems()) {
				rootContainer.copyFrom(item);
				// Delete the item if it is located in the _unzipped_ dir.
				// Remember that the resource folder could be a valid folder of a
				// different resource (when copying the resource).
				if (resourceContainer.getName().equals(FileResourceManager.ZIPDIR)) {
					item.delete();
				}
			}
		}
	}

	/**
	 * Validates the uploaded resource directory
	 * 
	 * @param directory
	 * @return True if it is falid
	 */
	static boolean validate(final File directory, final String type) {
		boolean valid = false;
		if (directory != null) {
			// Verify the directory structure:
			// /root
			// __feed.xml
			// __/items
			// ____/item
			// ______item.xml
			// ______/media.xml
			// ________...
			// ____/item
			// ______...
			final VFSContainer root = new LocalFolderImpl(directory);
			// try to read podcast
			try {
				final Feed feed = FeedManager.getInstance().readFeedFile(root);
				if (feed != null) {
					// The feed is valid, let's check the items
					if (feed.isInternal()) {
						final List<String> itemIds = feed.getItemIds();
						final VFSContainer itemsContainer = (VFSContainer) root.resolve(FeedManager.ITEMS_DIR);
						if (itemsContainer == null) {
							valid = itemIds.isEmpty(); // empty podcast
						} else {
							int validItemsCount = 0;
							for (final String itemId : itemIds) {
								// Try loading each item
								final VFSItem itemContainer = itemsContainer.resolve(itemId);
								final Item item = FeedManager.getInstance().loadItem(itemContainer);
								if (item != null) {
									// This item is valid, increase the counter
									validItemsCount++;
								}
							}
							if (validItemsCount == itemIds.size()) {
								// The feed and all items are valid
								valid = true;
							}
						}
					} else if (feed.isExternal()) {
						// assume the feed url is valid.
						valid = true;
					} else if (feed.isUndefined()) {
						// the feed is empty.
						valid = true;
					}
					// check type
					if (!type.equals(feed.getResourceableTypeName())) {
						valid = false;
					}
				}
			} catch (final Exception e) {
				// Reading feed failed, the directory is hence invalid
			}
		}
		return valid;
	}
}
