package com.nisovin.shopkeepers.api.events;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.google.common.base.Preconditions;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.ApiInternals;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * This event is for example called when {@link ShopkeepersPlugin#updateItems()} or
 * {@link Shopkeeper#updateItems()} is invoked and can be used by other plugins to update the data
 * of the items stored by the Shopkeepers plugin.
 * <p>
 * A typical usecase is the integration with third-party custom item plugins that store an
 * identifier in each item stack in order to be able to automatically update the items in the world
 * whenever a server admin has made changes to the item configuration. This event can be used to
 * also automatically update the data of these custom items stored inside the Shopkeepers plugin.
 * <p>
 * Notes:
 * <ul>
 * <li>This event is not called for empty items, i.e. unset items, items of type
 * {@link Material#AIR}, or with an amount less than {@code 1}.
 * <li>This event is not only called for trade offer items, but any items stored by the Shopkeepers
 * plugin, such as shopkeeper equipment items, player shop hiring cost items, items stored inside
 * the configuration such as currency items, etc.
 * <li>This event might not be called for all items stored or handled by the Shopkeepers plugin. See
 * {@link ShopkeepersPlugin#updateItems()} and {@link Shopkeeper#updateItems()} for examples.
 * <li>In order to be able to also use this event for a broad range of potential future contexts
 * without requiring changes in the plugins reacting to this event, this event intentionally does
 * not provide any context information, such as the involved {@link Shopkeeper}, trade offer, etc.
 * All item update implementations shall be agnostic of the context in which the item is being used
 * by the Shopkeepers plugin and only update the item based on the item data itself.
 * <li>Depending on the context in which the item is used, certain item data might not be accepted.
 * Trying to update the item to something invalid results in either an exception (e.g. when trying
 * to set the item to an empty item stack), the particular invalid item property to be ignored, the
 * original item or item property to be preserved, or some default item data to be used
 * instead.<br />
 * Examples: This event cannot be used to clear or remove trades. If the event is called for a
 * currency item stored inside the configuration, the item stack's amount is ignored.<br />
 * Since all item updates performed by handlers of this event shall be agnostic of the context in
 * which the item is used, this event prohibits clearing the item for any context, i.e. set it to
 * <code>null</code>, an item of type AIR, or an item stack with an amount less than {@code 1}.
 * </ul>
 */
public class UpdateItemEvent extends Event {

	private final UnmodifiableItemStack orginalItem;

	private UnmodifiableItemStack item;
	private boolean itemAltered = false;

	/**
	 * Creates a new {@link UpdateItemEvent}.
	 * 
	 * @param originalItem
	 *            the original item, not <code>null</code> or empty
	 */
	public UpdateItemEvent(UnmodifiableItemStack originalItem) {
		Preconditions.checkArgument(!ApiInternals.getInstance().isEmpty(originalItem),
				"originalItem is empty");
		this.orginalItem = originalItem;
		this.item = originalItem;
	}

	/**
	 * Gets the original item for which this event is being invoked.
	 * 
	 * @return the original item, not <code>null</code>
	 */
	public UnmodifiableItemStack getOriginalItem() {
		return orginalItem;
	}

	/**
	 * Gets the item to use, i.e. either the {@link #getOriginalItem() original item} if the item
	 * was not yet {@link #setItem(UnmodifiableItemStack) altered}, or the provided updated item.
	 * 
	 * @return the item to use, not <code>null</code>
	 */
	public UnmodifiableItemStack getItem() {
		return item;
	}

	/**
	 * Sets the item to use instead of the {@link #getOriginalItem() original item}. This has no
	 * effect if the given item equals the current {@link #getItem()}.
	 * 
	 * @param item
	 *            the item to use, not <code>null</code>
	 */
	public void setItem(UnmodifiableItemStack item) {
		Preconditions.checkArgument(!ApiInternals.getInstance().isEmpty(item), "item is empty");
		if (this.item.equals(item)) return;

		this.itemAltered = true;
		this.item = item;
	}

	/**
	 * Whether the item was {@link #setItem(UnmodifiableItemStack) altered}.
	 * <p>
	 * For performance reasons, we don't compare the updated item with the original item here, but
	 * only detect whether {@link #setItem(UnmodifiableItemStack)} has been called with a different
	 * item at least once.
	 * 
	 * @return <code>true</code> if the item was altered
	 */
	public boolean isItemAltered() {
		return itemAltered;
	}

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * Gets the {@link HandlerList} of this event.
	 * 
	 * @return the handler list
	 */
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
