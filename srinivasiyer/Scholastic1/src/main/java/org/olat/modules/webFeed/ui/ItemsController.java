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
package org.olat.modules.webFeed.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.controllers.navigation.Dated;
import org.olat.core.commons.controllers.navigation.NavigationEvent;
import org.olat.core.commons.controllers.navigation.YearNavigationController;
import org.olat.core.commons.services.commentAndRating.CommentAndRatingService;
import org.olat.core.commons.services.commentAndRating.impl.ui.UserCommentsAndRatingsController;
import org.olat.core.defaults.dispatcher.ClassPathStaticDispatcher;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.date.DateComponentFactory;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.modules.webFeed.FeedSecurityCallback;
import org.olat.modules.webFeed.FeedViewHelper;
import org.olat.modules.webFeed.RSSFeed;
import org.olat.modules.webFeed.managers.FeedManager;
import org.olat.modules.webFeed.models.Feed;
import org.olat.modules.webFeed.models.Item;
import org.olat.modules.webFeed.models.ItemPublishDateComparator;
import org.olat.portfolio.EPUIFactory;
import org.olat.user.HomePageConfigManager;
import org.olat.user.HomePageConfigManagerImpl;
import org.olat.user.UserInfoMainController;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * This class is responsible for dealing with items. For internal podcasts, items can be created, edited and deleted. Initial Date: Mar 2, 2009 <br>
 * 
 * @author gwassmann
 */
// ClosableModalController is deprecated. No alternative implemented.
@SuppressWarnings("deprecation")
public class ItemsController extends BasicController {

	private final VelocityContainer vcItems;
	private ArrayList<Link> editButtons;
	private ArrayList<Link> deleteButtons;
	private ArrayList<Link> itemLinks;
	private Map<Item, Controller> artefactLinks;
	private Link addItemButton, makeInternalButton, makeExternalButton;
	private final Link olderItemsLink, newerItemsLink, startpageLink;
	private FormBasicController itemFormCtr;
	private CloseableModalController cmc;
	private DialogBoxController confirmDialogCtr;
	private final Feed feedResource;
	private Item currentItem;
	private final FeedViewHelper helper;
	private final FeedUIFactory uiFactory;
	private final YearNavigationController naviCtr;
	private final FeedSecurityCallback callback;
	private final Panel mainPanel;
	private ItemController itemCtr;
	private int allItemsCount = 0;
	// Only one lock variable is needed, since only one item can be edited
	// at a time.
	private LockResult lock;
	public static Event HANDLE_NEW_EXTERNAL_FEED_DIALOG_EVENT = new Event("cmd.handle.new.external.feed.dialog");
	public static Event FEED_INFO_IS_DIRTY_EVENT = new Event("cmd.feed.info.is.dirty");

	/**
	 * Constructor
	 * 
	 * @param ureq
	 * @param control
	 */
	public ItemsController(final UserRequest ureq, final WindowControl wControl, final Feed feed, final FeedViewHelper helper, final FeedUIFactory uiFactory,
			final FeedSecurityCallback callback, final VelocityContainer vcRightColumn) {
		super(ureq, wControl);
		this.feedResource = feed;
		this.helper = helper;
		this.uiFactory = uiFactory;
		this.callback = callback;
		setTranslator(uiFactory.getTranslator());

		vcItems = uiFactory.createItemsVelocityContainer(this);
		vcItems.contextPut("feed", feed);
		vcItems.contextPut("callback", callback);

		final String baseStaticPath = ClassPathStaticDispatcher.getInstance().getMapperBasePath(RSSFeed.class);
		vcItems.contextPut("baseStaticPath", baseStaticPath);

		vcItems.contextPut("helper", helper);

		olderItemsLink = LinkFactory.createLink("feed.older.items", vcItems, this);
		olderItemsLink.setCustomEnabledLinkCSS("b_table_backward");
		newerItemsLink = LinkFactory.createLink("feed.newer.items", vcItems, this);
		newerItemsLink.setCustomEnabledLinkCSS("b_table_forward");
		startpageLink = LinkFactory.createLink("feed.startpage", vcItems, this);
		startpageLink.setCustomEnabledLinkCSS("b_table_first_page");

		if (callback.mayEditItems() || callback.mayCreateItems()) {
			createEditButtons(ureq, feed);
		}
		// Add item details page link
		createItemLinks(feed);
		// Add item user comments link and rating
		createCommentsAndRatingsLinks(ureq, feed);
		// Add date components
		createDateComponents(ureq, feed);

		// The year/month navigation
		final List<? extends Dated> items = feed.getFilteredItems(callback, ureq.getIdentity());
		allItemsCount = items.size();
		naviCtr = new YearNavigationController(ureq, wControl, getTranslator(), items);
		listenTo(naviCtr);
		vcRightColumn.put("navi", naviCtr.getInitialComponent());

		mainPanel = new Panel("mainPanel");
		mainPanel.setContent(vcItems);
		this.putInitialPanel(mainPanel);
	}

	/**
	 * Creates all necessary buttons for editing the feed's items
	 * 
	 * @param feed the current feed object
	 */
	private void createEditButtons(final UserRequest ureq, final Feed feed) {
		final List<Item> items = feed.getItems();

		editButtons = new ArrayList<Link>();
		deleteButtons = new ArrayList<Link>();
		artefactLinks = new HashMap<Item, Controller>();
		if (feed.isInternal()) {
			addItemButton = LinkFactory.createButtonSmall("feed.add.item", vcItems, this);
			if (items != null) {
				for (final Item item : items) {
					createButtonsForItem(ureq, item);
				}
			}
		} else if (feed.isUndefined()) {
			// The feed is whether internal nor external:
			// That is,
			// - it has just been created,
			// - all items have been removed or
			// - the feed url of an external feed has been set empty.
			// In such a case, the user can decide whether to make it internal or
			// external.
			makeInternalAndExternalButtons();
		}
	}

	/**
	 * Create the comments and rating components for each feed item
	 * 
	 * @param ureq
	 * @param feed
	 */
	private void createCommentsAndRatingsLinks(final UserRequest ureq, final Feed feed) {
		final List<Item> items = feed.getItems();
		if (items != null) {
			for (final Item item : items) {
				// Add rating and commenting controller
				createCommentsAndRatingsLink(ureq, feed, item);
			}
		}
	}

	/**
	 * Create comments and rating component link for given feed item
	 * 
	 * @param ureq
	 * @param feed
	 * @param item
	 */
	private void createCommentsAndRatingsLink(final UserRequest ureq, final Feed feed, final Item item) {
		if (CoreSpringFactory.containsBean(CommentAndRatingService.class)) {
			final CommentAndRatingService commentAndRatingService = (CommentAndRatingService) CoreSpringFactory.getBean(CommentAndRatingService.class);
			commentAndRatingService.init(getIdentity(), feed, item.getGuid(), callback.mayEditMetadata(), ureq.getUserSession().getRoles().isGuestOnly());
			final UserCommentsAndRatingsController commentsAndRatingCtr = commentAndRatingService
					.createUserCommentsAndRatingControllerMinimized(ureq, getWindowControl());
			commentsAndRatingCtr.addUserObject(item);
			listenTo(commentsAndRatingCtr);
			final String guid = item.getGuid();
			vcItems.put("commentsAndRating." + guid, commentsAndRatingCtr.getInitialComponent());
		}
	}

	/**
	 * Create a GUI component to display a nicely formatted date
	 * 
	 * @param ureq
	 * @param feed
	 */
	private void createDateComponents(final UserRequest ureq, final Feed feed) {
		final List<Item> items = feed.getItems();
		if (items != null) {
			for (final Item item : items) {
				final String guid = item.getGuid();
				if (item.getDate() != null) {
					DateComponentFactory.createDateComponentWithYear("date." + guid, item.getDate(), vcItems);
				}
			}
		}
	}

	private void createItemLinks(final Feed feed) {
		final List<Item> items = feed.getItems();
		itemLinks = new ArrayList<Link>();
		if (items != null) {
			for (final Item item : items) {
				createItemLink(item);
			}
		}
	}

	/**
	 * @param item
	 */
	private void createItemLink(final Item item) {
		final String guid = item.getGuid();
		final Link itemLink = LinkFactory.createCustomLink("link.to." + guid, "link.to." + guid, "feed.link.more", Link.LINK, vcItems, this);
		itemLink.setCustomEnabledLinkCSS("b_link_forward");
		itemLink.setUserObject(item);
		itemLinks.add(itemLink);
	}

	/**
	 * Instantiates the makeInternal and the makeExternal-Buttons and puts it to the items velocity container's context.
	 */
	public void makeInternalAndExternalButtons() {
		makeInternalButton = LinkFactory.createButton("feed.make.internal", vcItems, this);
		makeExternalButton = LinkFactory.createButton("feed.make.external", vcItems, this);
	}

	/**
	 * @param item
	 */
	private void createButtonsForItem(final UserRequest ureq, final Item item) {
		final String guid = item.getGuid();
		final Link editButton = LinkFactory.createCustomLink("feed.edit.item." + guid, "feed.edit.item." + guid, "feed.edit.item", Link.BUTTON_XSMALL, vcItems, this);
		final Link deleteButton = LinkFactory.createCustomLink("delete." + guid, "delete." + guid, "delete", Link.BUTTON_XSMALL, vcItems, this);

		if (feedResource.isInternal() && getIdentity().getKey() != null && getIdentity().getKey().equals(item.getAuthorKey())) {
			String businessPath = BusinessControlFactory.getInstance().getAsString(getWindowControl().getBusinessControl());
			businessPath += "[item=" + item.getGuid() + ":0]";
			final Controller artefactCtrl = EPUIFactory.createArtefactCollectWizzardController(ureq, getWindowControl(), feedResource, businessPath);
			if (artefactCtrl != null) {
				artefactLinks.put(item, artefactCtrl);
				vcItems.put("feed.artefact.item." + guid, artefactCtrl.getInitialComponent());
			}
		}

		editButton.setUserObject(item);
		deleteButton.setUserObject(item);
		editButtons.add(editButton);
		deleteButtons.add(deleteButton);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// make sure the lock is released
		FeedManager.getInstance().releaseLock(lock);
		// Dispose confirm deletion dialog controller since it isn't listend to.
		if (confirmDialogCtr != null) {
			removeAsListenerAndDispose(confirmDialogCtr);
		}

		if (artefactLinks != null) {
			for (final Controller ctrl : artefactLinks.values()) {
				ctrl.dispose();
			}
			artefactLinks.clear();
			artefactLinks = null;
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Component source, @SuppressWarnings("unused") final Event event) {
		final FeedManager feedManager = FeedManager.getInstance();
		final Feed feed = feedManager.getFeed(feedResource);
		if (source == addItemButton) {
			currentItem = new Item();
			currentItem.setDraft(true);
			currentItem.setAuthorKey(ureq.getIdentity().getKey());
			// Generate new GUID for item, needed for media files that are stored relative to the GUID
			currentItem.setGuid(CodeHelper.getGlobalForeverUniqueID());
			// Create item and media containers
			feedManager.createItemContainer(feed, currentItem);
			itemFormCtr = uiFactory.createItemFormController(ureq, getWindowControl(), currentItem, feed);
			activateModalDialog(itemFormCtr);

		} else if (editButtons != null && editButtons.contains(source)) {
			currentItem = (Item) ((Link) source).getUserObject();
			lock = feedManager.acquireLock(feed, currentItem, getIdentity());
			if (lock.isSuccess()) {

				itemFormCtr = uiFactory.createItemFormController(ureq, getWindowControl(), currentItem, feed);
				activateModalDialog(itemFormCtr);
			} else {
				showInfo("feed.item.is.being.edited.by", lock.getOwner().getName());
			}
		} else if (deleteButtons != null && deleteButtons.contains(source)) {
			final Item item = (Item) ((Link) source).getUserObject();
			confirmDialogCtr = activateYesNoDialog(ureq, null, translate("feed.item.confirm.delete"), confirmDialogCtr);
			confirmDialogCtr.setUserObject(item);

		} else if (itemLinks != null && itemLinks.contains(source)) {
			final Item item = (Item) ((Link) source).getUserObject();
			displayItemController(ureq, item);

		} else if (source == makeInternalButton) {
			feedManager.updateFeedMode(Boolean.FALSE, feed);
			addItemButton = LinkFactory.createButton("feed.add.item", vcItems, this);
			currentItem = new Item();
			currentItem.setDraft(true);
			currentItem.setAuthorKey(ureq.getIdentity().getKey());
			// Generate new GUID for item, needed for media files that are stored relative to the GUID
			currentItem.setGuid(CodeHelper.getGlobalForeverUniqueID());
			// Create item and media containers
			feedManager.createItemContainer(feed, currentItem);
			itemFormCtr = uiFactory.createItemFormController(ureq, getWindowControl(), currentItem, feed);
			activateModalDialog(itemFormCtr);
			// do logging
			ThreadLocalUserActivityLogger.log(FeedLoggingAction.FEED_EDIT, getClass(), LoggingResourceable.wrap(feed));

		} else if (source == makeExternalButton) {
			feedManager.updateFeedMode(Boolean.TRUE, feed);
			vcItems.setDirty(true);
			// Ask listening FeedMainController to open and handle a new external
			// feed dialog.
			fireEvent(ureq, HANDLE_NEW_EXTERNAL_FEED_DIALOG_EVENT);
			// do logging
			ThreadLocalUserActivityLogger.log(FeedLoggingAction.FEED_EDIT, getClass(), LoggingResourceable.wrap(feed));

		} else if (source == olderItemsLink) {
			helper.olderItems();
			vcItems.setDirty(true);

		} else if (source == newerItemsLink) {
			helper.newerItems();
			vcItems.setDirty(true);

		} else if (source == startpageLink) {
			helper.startpage();
			vcItems.setDirty(true);

		} else if (source instanceof Link) {
			// if it's a link try to get attached identity and assume that user wants
			// to see the users home page
			final Link userLink = (Link) source;
			final Object userObject = userLink.getUserObject();
			if (userObject instanceof Identity) {
				final Identity chosenIdentity = (Identity) userObject;
				final HomePageConfigManager hpcm = HomePageConfigManagerImpl.getInstance();
				final OLATResourceable ores = hpcm.loadConfigFor(chosenIdentity.getName());
				final DTabs dts = (DTabs) Windows.getWindows(ureq).getWindow(ureq).getAttribute("DTabs");
				// was brasato:: DTabs dts = getWindowControl().getDTabs();
				DTab dt = dts.getDTab(ores);
				if (dt == null) {
					// does not yet exist -> create and add
					dt = dts.createDTab(ores, chosenIdentity.getName());
					if (dt == null) { return; }
					final UserInfoMainController uimc = new UserInfoMainController(ureq, dt.getWindowControl(), chosenIdentity);
					dt.setController(uimc);
					dts.addDTab(dt);
				}
				dts.activate(ureq, dt, null);
			}
		}

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(final UserRequest ureq, final Controller source, final Event event) {
		final FeedManager feedManager = FeedManager.getInstance();
		final Feed feed = feedManager.getFeed(feedResource);
		if (source == cmc) {
			if (event.equals(CloseableModalController.CLOSE_MODAL_EVENT)) {
				removeAsListenerAndDispose(cmc);
				cmc = null;
				removeAsListenerAndDispose(itemFormCtr);
				itemFormCtr = null;
				// Check if this item has ever been added to the feed. If not, remove the temp dir
				cleanupTmpItemMediaDir(currentItem, feed, feedManager);
				// If there were no items and the user doesn't want to save the
				// first item, go back to the decision whether to make the feed
				// internally or subscribe to an external feed.
				if (!feed.hasItems()) {
					feedManager.updateFeedMode(null, feed);
					makeInternalAndExternalButtons();
				}
				// release lock
				feedManager.releaseLock(lock);
			}
		} else if (source == confirmDialogCtr && DialogBoxUIFactory.isYesEvent(event)) {
			// The user confirmed that the item shall be deleted
			final Item item = (Item) ((DialogBoxController) source).getUserObject();
			lock = feedManager.acquireLock(feed, item, getIdentity());
			if (lock.isSuccess()) {
				// remove the item from the naviCtr
				naviCtr.remove(item);
				// remove the item also from the helper (cached selection)
				helper.removeItem(item);
				// permanently remove item
				feedManager.remove(item, feed);
				// remove delete and edit buttons of this item
				deleteButtons.remove(source);
				for (final Link editButton : editButtons) {
					if (editButton.getUserObject() == item) {
						editButtons.remove(editButton);
						break;
					}
				}
				// If the last item has been deleted, provide buttons for adding
				// items manually or from an external source/feed.
				if (!feed.hasItems()) {
					makeInternalAndExternalButtons();
					// The subscription/feed url from the feed info is obsolete
					fireEvent(ureq, ItemsController.FEED_INFO_IS_DIRTY_EVENT);
				}
				vcItems.setDirty(true);
				// in case we were in single item view, show all items
				mainPanel.setContent(vcItems);
				feedManager.releaseLock(lock);
				lock = null;
				// do logging
				ThreadLocalUserActivityLogger.log(FeedLoggingAction.FEED_ITEM_DELETE, getClass(), LoggingResourceable.wrap(item));

			} else {
				showInfo("feed.item.is.being.edited.by", lock.getOwner().getName());
			}

		} else if (source == itemFormCtr) {
			if (event.equals(Event.CHANGED_EVENT) || event.equals(Event.CANCELLED_EVENT)) {
				if (event.equals(Event.CHANGED_EVENT)) {
					final FileElement mediaFile = currentItem.getMediaFile();
					if (feedManager.getItemContainer(currentItem, feed) == null) {
						// ups, deleted in the meantime by someone else
						// remove the item from the naviCtr
						naviCtr.remove(currentItem);
						// remove the item also from the helper (cached selection)
						helper.removeItem(currentItem);
					} else {
						if (!feed.getItems().contains(currentItem)) {
							// Add the modified item if it is not part of the feed
							feedManager.addItem(currentItem, mediaFile, feed);
							createButtonsForItem(ureq, currentItem);
							createItemLink(currentItem);
							// Add date component
							final String guid = currentItem.getGuid();
							if (currentItem.getDate() != null) {
								DateComponentFactory.createDateComponentWithYear("date." + guid, currentItem.getDate(), vcItems);
							}
							// Add comments and rating
							createCommentsAndRatingsLink(ureq, feed, currentItem);
							// add it to the navigation controller
							naviCtr.add(currentItem);
							// ... and also to the helper
							helper.addItem(currentItem);
							if (feed.getItems().size() == 1) {
								// First item added, show feed url (for subscription)
								fireEvent(ureq, ItemsController.FEED_INFO_IS_DIRTY_EVENT);
								// Set the base URI of the feed for the current user. All users
								// have unique URIs.
								helper.setURIs();
							}
							// do logging
							ThreadLocalUserActivityLogger.log(FeedLoggingAction.FEED_ITEM_CREATE, getClass(), LoggingResourceable.wrap(currentItem));
						} else {
							// Write item file
							feedManager.updateItem(currentItem, mediaFile, feed);
							// Update current item in the users view, replace in helper cache of
							// current selected items.
							helper.updateItem(currentItem);
							// Do logging
							ThreadLocalUserActivityLogger.log(FeedLoggingAction.FEED_ITEM_EDIT, getClass(), LoggingResourceable.wrap(currentItem));
						}
					}
					vcItems.setDirty(true);
					// if the current item is displayed, update the view
					if (itemCtr != null) {
						itemCtr.getInitialComponent().setDirty(true);
					}

				} else if (event.equals(Event.CANCELLED_EVENT)) {
					// Check if this item has ever been added to the feed. If not, remove the temp dir
					cleanupTmpItemMediaDir(currentItem, feed, feedManager);
					// If there were no items and the user doesn't want to save the
					// first item, go back to the decision whether to make the feed
					// internally or subscribe to an external feed.
					if (!feed.hasItems()) {
						feedManager.updateFeedMode(null, feed);
						makeInternalAndExternalButtons();
					}
				}
				// release the lock
				feedManager.releaseLock(lock);

				// Dispose the cmc and the podcastFormCtr.
				cmc.deactivate();
				removeAsListenerAndDispose(cmc);
				cmc = null;
				removeAsListenerAndDispose(itemFormCtr);
				itemFormCtr = null;
			}
		} else if (source == naviCtr && event instanceof NavigationEvent) {
			final List<? extends Dated> selItems = ((NavigationEvent) event).getSelectedItems();
			final List<Item> items = new ArrayList<Item>();
			for (final Dated item : selItems) {
				if (item instanceof Item) {
					items.add((Item) item);
				}
			}
			// make sure items are sorted properly
			Collections.sort(items, new ItemPublishDateComparator());
			helper.setSelectedItems(items);
			vcItems.setDirty(true);
			mainPanel.setContent(vcItems);

		} else if (source == itemCtr) {
			if (event == Event.BACK_EVENT) {
				mainPanel.setContent(vcItems);
			}

		} else if (source instanceof UserCommentsAndRatingsController) {
			final UserCommentsAndRatingsController commentsRatingsCtr = (UserCommentsAndRatingsController) source;
			if (event == UserCommentsAndRatingsController.EVENT_COMMENT_LINK_CLICKED) {
				// go to details page
				final Item item = (Item) commentsRatingsCtr.getUserObject();
				final ItemController myItemCtr = displayItemController(ureq, item);
				myItemCtr.activate(ureq, ItemController.ACTIVATION_KEY_COMMENTS);
			}
		}

		// Check if someone else added an item, reload everything
		if (feed.getFilteredItems(callback, ureq.getIdentity()).size() != allItemsCount) {
			resetItems(ureq, feed);
		}
	}

	/**
	 * Private helper to remove any temp media files created for this feed
	 * 
	 * @param tmpItem
	 * @param feed
	 * @param feedManager
	 */
	private void cleanupTmpItemMediaDir(final Item tmpItem, final Feed feed, final FeedManager feedManager) {
		// Add GUID null check to not accidentally delete the entire feed directory
		// in case there is somewhere a programming error
		if (!feed.getItems().contains(tmpItem) && tmpItem.getGuid() != null) {
			final VFSContainer itemContainer = feedManager.getItemContainer(tmpItem, feed);
			if (itemContainer != null) {
				itemContainer.delete();
			}
		}
	}

	/**
	 * @param controller The <code>FormBasicController</code> to be displayed in the modal dialog.
	 */
	private void activateModalDialog(final FormBasicController controller) {
		listenTo(controller);
		cmc = new CloseableModalController(getWindowControl(), translate("close"), controller.getInitialComponent());
		listenTo(cmc);
		cmc.activate();
	}

	/**
	 * Sets the items view dirty.
	 * 
	 * @param ureq
	 * @param feed the current feed
	 */
	public void resetItems(final UserRequest ureq, final Feed feed) {
		FeedManager.getInstance().loadItems(feed);
		final List<Item> items = feed.getFilteredItems(callback, ureq.getIdentity());
		helper.setSelectedItems(items);
		naviCtr.setDatedObjects(items);
		allItemsCount = items.size();
		// Add item details page link
		createItemLinks(feed);
		// Add item user comments link and rating
		createCommentsAndRatingsLinks(ureq, feed);
		// Add date components
		createDateComponents(ureq, feed);
		vcItems.setDirty(true);
	}

	/**
	 * Sets the edit mode to editable
	 * 
	 * @param editable
	 * @param feed the current feed
	 */
	public void setEditMode(final UserRequest ureq, final boolean editable, final Feed feed) {
		vcItems.contextPut("editModeEnabled", editable);
		if (editable) {
			createEditButtons(ureq, feed);
		} else {
			removeEditButtons();
		}
	}

	/**
	 * Remove the edit buttons
	 */
	private void removeEditButtons() {
		vcItems.contextRemove(velocity_root);
		vcItems.remove(addItemButton);
		vcItems.remove(makeExternalButton);
		vcItems.remove(makeInternalButton);
		for (final Link button : editButtons) {
			vcItems.remove(button);
		}
		for (final Link button : deleteButtons) {
			vcItems.remove(button);
		}
		addItemButton = null;
		makeInternalButton = null;
		makeExternalButton = null;
		editButtons = null;
		deleteButtons = null;
	}

	/**
	 * Displays the item in the mainPanel of this controller.
	 * 
	 * @param ureq
	 * @param item
	 */
	private ItemController displayItemController(final UserRequest ureq, final Item item) {
		removeAsListenerAndDispose(itemCtr);
		final Link editButton = getButtonByUserObject(item, editButtons);
		final Link deleteButton = getButtonByUserObject(item, deleteButtons);
		final Controller artefactLink = getArtefactLinkByUserObject(item);
		final FeedManager feedManager = FeedManager.getInstance();
		final Feed feed = feedManager.getFeed(feedResource);
		itemCtr = new ItemController(ureq, getWindowControl(), item, feed, helper, uiFactory, callback, editButton, deleteButton, artefactLink);
		listenTo(itemCtr);
		mainPanel.setContent(itemCtr.getInitialComponent());
		return itemCtr;
	}

	/**
	 * @param item
	 */
	public void activate(final UserRequest ureq, final Item item) {
		displayItemController(ureq, item);
	}

	/**
	 * @param item
	 * @param buttons
	 * @return The Link in buttons which has the item attached as user object or null
	 */
	private Link getButtonByUserObject(final Item item, final List<Link> buttons) {
		Link result = null;
		if (buttons != null && item != null) {
			for (final Link button : buttons) {
				if (button.getUserObject() == item) {
					result = button;
					break;
				}
			}
		}
		return result;
	}

	private Controller getArtefactLinkByUserObject(final Item item) {
		final Controller result = null;
		if (artefactLinks != null && artefactLinks.containsKey(item)) { return artefactLinks.get(item); }
		return result;
	}
}
