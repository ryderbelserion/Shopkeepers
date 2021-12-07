package com.nisovin.shopkeepers.util.inventory;

import java.util.Objects;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Utility functions related to inventories.
 */
public final class InventoryUtils {

	private static final ItemStack[] EMPTY_ITEMSTACK_ARRAY = new ItemStack[0];

	/**
	 * Returns an empty array of {@link ItemStack ItemStacks}.
	 * 
	 * @return the empty array
	 */
	public static ItemStack[] emptyItemStackArray() {
		return EMPTY_ITEMSTACK_ARRAY;
	}

	/**
	 * Checks whether the given {@link Player} is currently viewing an inventory.
	 * <p>
	 * Because opening the own inventory does not inform the server, this method cannot detect if the player is
	 * currently viewing his own inventory or the creative mode inventory.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return <code>true</code> if the player has currently an inventory open (that is not his own inventory)
	 */
	public static boolean hasInventoryOpen(Player player) {
		InventoryType inventoryType = player.getOpenInventory().getType();
		return inventoryType != InventoryType.CRAFTING && inventoryType != InventoryType.CREATIVE;
	}

	/**
	 * Checks if the given contents contains at least the specified amount of items that are accepted by the given
	 * {@link Predicate}.
	 * <p>
	 * The given Predicate is only invoked for {@link ItemUtils#isEmpty(ItemStack) non-empty} ItemStacks.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param predicate
	 *            the predicate, not <code>null</code>
	 * @param amount
	 *            the amount of items to check for
	 * @return <code>true</code> if at least the specified amount of items was found
	 */
	public static boolean containsAtLeast(@ReadOnly ItemStack @ReadOnly [] contents, Predicate<@ReadOnly ItemStack> predicate, int amount) {
		Validate.notNull(predicate, "predicate is null");
		if (amount <= 0) return true;
		if (contents == null) return false;
		int remainingAmount = amount;
		for (ItemStack itemStack : contents) {
			if (ItemUtils.isEmpty(itemStack)) continue;
			if (!predicate.test(itemStack)) continue;
			remainingAmount -= itemStack.getAmount();
			if (remainingAmount <= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the given contents contains at least the specified amount of items that
	 * {@link ItemData#matches(ItemStack) match} the given {@link ItemData}.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param itemData
	 *            the item data to check for, not <code>null</code>
	 * @param amount
	 *            the amount of items to check for
	 * @return <code>true</code> if at least the specified amount of items was found
	 */
	public static boolean containsAtLeast(@ReadOnly ItemStack @ReadOnly [] contents, ItemData itemData, int amount) {
		return containsAtLeast(contents, ItemUtils.matchingItems(itemData), amount);
	}

	/**
	 * Checks if the given contents contains at least the specified amount of items that are
	 * {@link ItemStack#isSimilar(ItemStack) similar} to the given {@link ItemStack}.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param itemStack
	 *            the item stack to check for, not <code>null</code>
	 * @param amount
	 *            the amount of items to check for
	 * @return <code>true</code> if at least the specified amount of items was found
	 */
	public static boolean containsAtLeast(@ReadOnly ItemStack @ReadOnly [] contents, @ReadOnly ItemStack itemStack, int amount) {
		return containsAtLeast(contents, ItemUtils.similarItems(itemStack), amount);
	}

	/**
	 * Checks if the given contents contains at least the specified amount of items that are
	 * {@link UnmodifiableItemStack#isSimilar(ItemStack) similar} to the given {@link ItemStack}.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param itemStack
	 *            the item stack to check for, not <code>null</code>
	 * @param amount
	 *            the amount of items to check for
	 * @return <code>true</code> if at least the specified amount of items was found
	 */
	public static boolean containsAtLeast(@ReadOnly ItemStack @ReadOnly [] contents, UnmodifiableItemStack itemStack, int amount) {
		return containsAtLeast(contents, ItemUtils.similarItems(itemStack), amount);
	}

	/**
	 * Checks if the given contents contains at least one item that {@link ItemData#matches(ItemStack) matches} the
	 * given {@link ItemData}.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param itemData
	 *            the item data to check for, not <code>null</code>
	 * @return <code>true</code> if an item was found
	 */
	public static boolean contains(@ReadOnly ItemStack @ReadOnly [] contents, ItemData itemData) {
		return containsAtLeast(contents, itemData, 1);
	}

	/**
	 * Checks if the given contents contains at least one item that is {@link ItemStack#isSimilar(ItemStack) similar} to
	 * the given {@link ItemStack}.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param itemStack
	 *            the item stack to check for, not <code>null</code>
	 * @return <code>true</code> if an item was found
	 */
	public static boolean contains(@ReadOnly ItemStack @ReadOnly [] contents, @ReadOnly ItemStack itemStack) {
		return containsAtLeast(contents, itemStack, 1);
	}

	// ItemStack Iterable

	/**
	 * Checks if the given contents contains at least the specified amount of items that are accepted by the given
	 * {@link Predicate}.
	 * <p>
	 * The given Predicate is only invoked for {@link ItemUtils#isEmpty(ItemStack) non-empty} ItemStacks.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param predicate
	 *            the predicate, not <code>null</code>
	 * @param amount
	 *            the amount of items to check for
	 * @return <code>true</code> if at least the specified amount of items was found
	 */
	public static boolean containsAtLeast(Iterable<@ReadOnly ItemStack> contents, Predicate<@ReadOnly ItemStack> predicate, int amount) {
		Validate.notNull(predicate, "predicate is null");
		if (amount <= 0) return true;
		if (contents == null) return false;
		int remainingAmount = amount;
		for (ItemStack itemStack : contents) {
			if (ItemUtils.isEmpty(itemStack)) continue;
			if (!predicate.test(itemStack)) continue;
			remainingAmount -= itemStack.getAmount();
			if (remainingAmount <= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the given contents contains at least the specified amount of items that
	 * {@link ItemData#matches(ItemStack) match} the given {@link ItemData}.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param itemData
	 *            the item data to check for, not <code>null</code>
	 * @param amount
	 *            the amount of items to check for
	 * @return <code>true</code> if at least the specified amount of items was found
	 */
	public static boolean containsAtLeast(Iterable<@ReadOnly ItemStack> contents, ItemData itemData, int amount) {
		return containsAtLeast(contents, ItemUtils.matchingItems(itemData), amount);
	}

	/**
	 * Checks if the given contents contains at least the specified amount of items that are
	 * {@link ItemStack#isSimilar(ItemStack) similar} to the given {@link ItemStack}.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param itemStack
	 *            the item stack to check for, not <code>null</code>
	 * @param amount
	 *            the amount of items to check for
	 * @return <code>true</code> if at least the specified amount of items was found
	 */
	public static boolean containsAtLeast(Iterable<@ReadOnly ItemStack> contents, @ReadOnly ItemStack itemStack, int amount) {
		return containsAtLeast(contents, ItemUtils.similarItems(itemStack), amount);
	}

	/**
	 * Checks if the given contents contains at least one item that {@link ItemData#matches(ItemStack) matches} the
	 * given {@link ItemData}.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param itemData
	 *            the item data to check for, not <code>null</code>
	 * @return <code>true</code> if an item was found
	 */
	public static boolean contains(Iterable<@ReadOnly ItemStack> contents, ItemData itemData) {
		return containsAtLeast(contents, itemData, 1);
	}

	/**
	 * Checks if the given contents contains at least one item that is {@link ItemStack#isSimilar(ItemStack) similar} to
	 * the given {@link ItemStack}.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param itemStack
	 *            the item stack to check for, not <code>null</code>
	 * @return <code>true</code> if an item was found
	 */
	public static boolean contains(Iterable<@ReadOnly ItemStack> contents, @ReadOnly ItemStack itemStack) {
		return containsAtLeast(contents, itemStack, 1);
	}

	// -----

	/**
	 * Adds the given {@link ItemStack} to the given contents.
	 * <p>
	 * See {@link #addItems(ItemStack[], UnmodifiableItemStack, int)}.
	 * 
	 * @param contents
	 *            the contents to add the items to
	 * @param itemStack
	 *            the item stack to add
	 * @return the amount of items that could not be added, <code>0</code> on complete success
	 */
	public static int addItems(@ReadWrite ItemStack @ReadOnly [] contents, @ReadOnly ItemStack itemStack) {
		return addItems(contents, itemStack, ItemUtils.getItemStackAmount(itemStack));
	}

	/**
	 * Adds the specified amount of items of the given {@link ItemStack} to the given contents.
	 * <p>
	 * See {@link #addItems(ItemStack[], UnmodifiableItemStack, int)}.
	 * 
	 * @param contents
	 *            the contents to add the items to
	 * @param item
	 *            the item to add
	 * @param amount
	 *            the amount to add
	 * @return the amount of items that could not be added, <code>0</code> on complete success
	 */
	public static int addItems(@ReadWrite ItemStack @ReadOnly [] contents, @ReadOnly ItemStack item, int amount) {
		return addItems(contents, UnmodifiableItemStack.of(item), amount);
	}

	/**
	 * Adds the given {@link UnmodifiableItemStack} to the given contents.
	 * <p>
	 * See {@link #addItems(ItemStack[], UnmodifiableItemStack, int)}.
	 * 
	 * @param contents
	 *            the contents to add the items to
	 * @param itemStack
	 *            the item stack
	 * @return the amount of items that could not be added, <code>0</code> on complete success
	 */
	public static int addItems(@ReadWrite ItemStack @ReadOnly [] contents, UnmodifiableItemStack itemStack) {
		return addItems(contents, itemStack, ItemUtils.getItemStackAmount(itemStack));
	}

	/**
	 * Adds the specified amount of items of the given {@link UnmodifiableItemStack} to the given contents.
	 * <p>
	 * This first tries to fill similar partial item stacks in the contents up to the item's max stack size. Afterwards,
	 * it inserts the remaining amount of items into empty slots, splitting at the item's max stack size.
	 * <p>
	 * The given item stack to add is copied before it is inserted into empty slots of the contents array.
	 * <p>
	 * This operation does not modify the original item stacks in the contents array: If it has to modify the amount of
	 * an item stack, it first replaces it with a copy inside the contents array. Consequently, if the item stacks of
	 * the given contents array are mirroring changes to their Minecraft counterparts, those underlying Minecraft item
	 * stacks are not affected by this operation until the modified contents array is applied back to the Minecraft
	 * inventory.
	 * 
	 * @param contents
	 *            the contents to add the items to
	 * @param item
	 *            the item to add
	 * @param amount
	 *            the amount to add
	 * @return the amount of items that could not be added, <code>0</code> on complete success
	 */
	public static int addItems(@ReadWrite ItemStack @ReadOnly [] contents, UnmodifiableItemStack item, int amount) {
		Validate.notNull(contents, "contents is null");
		Validate.notNull(item, "item is null");
		Validate.isTrue(amount >= 0, "amount is negative");
		if (amount == 0) return 0;

		// Search for partially fitting item stacks:
		int maxStackSize = item.getMaxStackSize();
		int size = contents.length;
		for (int slot = 0; slot < size; slot++) {
			ItemStack slotItem = contents[slot];

			// Slot empty? - Skip, because we are currently filling existing item stacks up.
			if (ItemUtils.isEmpty(slotItem)) continue;

			// Slot already full?
			int slotAmount = slotItem.getAmount();
			if (slotAmount >= maxStackSize) continue;

			if (item.isSimilar(slotItem)) {
				// Copy ItemStack, so we don't modify the original ItemStack:
				slotItem = slotItem.clone();
				contents[slot] = slotItem;

				int newAmount = slotAmount + amount;
				if (newAmount <= maxStackSize) {
					// Remaining amount did fully fit into this stack:
					slotItem.setAmount(newAmount);
					return 0;
				} else {
					// Did not fully fit:
					slotItem.setAmount(maxStackSize);
					amount -= (maxStackSize - slotAmount);
					assert amount != 0;
				}
			}
		}

		// We have items remaining:
		assert amount > 0;

		// Search for empty slots:
		for (int slot = 0; slot < size; slot++) {
			ItemStack slotItem = contents[slot];
			if (ItemUtils.isEmpty(slotItem)) {
				// Found an empty slot:
				if (amount > maxStackSize) {
					// Add full stack:
					ItemStack stack = item.copy();
					stack.setAmount(maxStackSize);
					contents[slot] = stack;
					amount -= maxStackSize;
				} else {
					// The remaining amount completely fits as a single stack:
					ItemStack stack = item.copy();
					stack.setAmount(amount);
					contents[slot] = stack;
					return 0;
				}
			}
		}

		// Not all items did fit into the inventory:
		return amount;
	}

	/**
	 * Removes the specified amount of items that match the specified {@link ItemData} from the given contents.
	 * 
	 * @param contents
	 *            the contents to remove the items from
	 * @param itemData
	 *            the item data to match
	 * @param amount
	 *            the amount of matching items to remove
	 * @return the amount of items that could not be removed, or <code>0</code> if all items were removed
	 * @see #removeItems(ItemStack[], Predicate, int)
	 */
	public static int removeItems(@ReadWrite ItemStack @ReadOnly [] contents, ItemData itemData, int amount) {
		return removeItems(contents, ItemUtils.matchingItems(itemData), amount);
	}

	/**
	 * Removes the given {@link ItemStack} from the given contents.
	 * 
	 * @param contents
	 *            the contents to remove the items from
	 * @param itemStack
	 *            the item stack to remove
	 * @return the amount of items that could not be removed, or <code>0</code> if all items were removed
	 * @see #removeItems(ItemStack[], Predicate, int)
	 */
	public static int removeItems(@ReadWrite ItemStack @ReadOnly [] contents, @ReadOnly ItemStack itemStack) {
		return removeItems(contents, ItemUtils.similarItems(itemStack), itemStack.getAmount());
	}

	/**
	 * Removes the given {@link UnmodifiableItemStack} from the given contents.
	 * 
	 * @param contents
	 *            the contents to remove the items from
	 * @param itemStack
	 *            the item stack to remove
	 * @return the amount of items that could not be removed, or <code>0</code> if all items were removed
	 * @see #removeItems(ItemStack[], Predicate, int)
	 */
	public static int removeItems(@ReadWrite ItemStack @ReadOnly [] contents, UnmodifiableItemStack itemStack) {
		return removeItems(contents, ItemUtils.similarItems(itemStack), itemStack.getAmount());
	}

	/**
	 * Removes the specified amount of items accepted by the given {@link Predicate} from the given contents.
	 * <p>
	 * If the specified amount is {@link Integer#MAX_VALUE}, then all items matching the Predicate are removed from the
	 * contents.
	 * <p>
	 * This operation does not modify the original item stacks in the contents array: If it has to modify the amount of
	 * an item stack, it first replaces it with a copy inside the contents array. Consequently, if the item stacks of
	 * the given contents array are mirroring changes to their Minecraft counterparts, those underlying Minecraft item
	 * stacks are not affected by this operation until the modified contents array is applied back to the Minecraft
	 * inventory.
	 * 
	 * @param contents
	 *            the contents to remove the items from
	 * @param itemMatcher
	 *            the item matcher
	 * @param amount
	 *            the amount of items to remove
	 * @return the amount of items that could not be removed, or <code>0</code> if all items were removed
	 */
	public static int removeItems(@ReadWrite ItemStack @ReadOnly [] contents, Predicate<@ReadOnly ItemStack> itemMatcher, int amount) {
		Validate.notNull(contents, "contents is null");
		Validate.notNull(itemMatcher, "itemMatcher is null");
		Validate.isTrue(amount >= 0, "amount is negative");
		if (amount == 0) return 0;

		boolean removeAll = (amount == Integer.MAX_VALUE);
		for (int slot = 0; slot < contents.length; slot++) {
			ItemStack slotItem = contents[slot];
			if (ItemUtils.isEmpty(slotItem)) continue;
			if (!itemMatcher.test(slotItem)) continue;

			if (removeAll) {
				contents[slot] = null;
			} else {
				int newAmount = slotItem.getAmount() - amount;
				if (newAmount > 0) {
					// Copy the ItemStack, so that we do not modify the original ItemStack (in case that we do not want
					// to apply the changed inventory contents afterwards):
					slotItem = slotItem.clone();
					contents[slot] = slotItem;
					slotItem.setAmount(newAmount);
					// All items were removed:
					return 0;
				} else {
					contents[slot] = null;
					amount = -newAmount;
					if (amount == 0) {
						// All items were removed:
						return 0;
					}
				}
			}
		}

		if (removeAll) return 0;
		return amount;
	}

	public static void setStorageContents(Inventory inventory, @ReadOnly ItemStack @ReadOnly [] contents) {
		setContents(inventory, contents);
	}

	public static void setContents(Inventory inventory, @ReadOnly ItemStack @ReadOnly [] contents) {
		setContents(inventory, 0, contents);
	}

	public static void setContents(Inventory inventory, int slotOffset, @ReadOnly ItemStack @ReadOnly [] contents) {
		Validate.notNull(inventory, "inventory is null");
		Validate.notNull(contents, "contents is null");
		// Assert: slotOffset is valid.
		final int length = contents.length;
		for (int slot = 0; slot < length; ++slot) {
			ItemStack newItem = contents[slot];
			int inventorySlot = slotOffset + slot;
			ItemStack currentItem = inventory.getItem(inventorySlot);
			// Only update slots that actually changed. This avoids sending the player slot update packets that are not
			// required.
			// We skip the slot if the current item already equals the new item (similar and same stack sizes). For
			// unchanged items (CraftItemStack wrappers) and items with changed stack size this is quite performant.
			if (Objects.equals(newItem, currentItem)) {
				continue;
			}
			inventory.setItem(inventorySlot, newItem); // This copies the item internally
		}
	}

	public static void updateInventoryLater(Inventory inventory) {
		// If the inventory belongs to a player, always update it for that player:
		Player owner = null;
		if (inventory instanceof PlayerInventory) {
			assert inventory.getHolder() instanceof Player;
			owner = (Player) inventory.getHolder();
			assert owner != null;
			updateInventoryLater(owner);
		}
		// If there are any (other) currently viewing players, update for those as well:
		for (HumanEntity viewer : inventory.getViewers()) {
			if (viewer instanceof Player) {
				if (!viewer.equals(owner)) {
					updateInventoryLater((Player) viewer);
				}
			}
		}
	}

	public static void updateInventoryLater(Player player) {
		Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), player::updateInventory);
	}

	// Only closes the player's open inventory view if it is still the specified view after the delay:
	public static void closeInventoryDelayed(InventoryView inventoryView) {
		Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
			InventoryView openInventoryView = inventoryView.getPlayer().getOpenInventory();
			if (inventoryView == openInventoryView) {
				inventoryView.close(); // Same as player.closeInventory()
			}
		});
	}

	public static void closeInventoryDelayed(Player player) {
		Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), player::closeInventory);
	}

	// This can for example be used during the handling of inventory interaction events.
	public static void setItemDelayed(Inventory inventory, int slot, @ReadOnly ItemStack itemStack) {
		Validate.notNull(inventory, "inventory is null");
		Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
			inventory.setItem(slot, itemStack); // This copies the item internally
		});
	}

	// TODO Replace this with the corresponding Bukkit API method added in late 1.15.2
	// See https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/commits/da9ef3c55fa3bce91f7fdcd77d50171be7297d7d
	public static ItemStack getItem(PlayerInventory playerInventory, EquipmentSlot slot) {
		if (playerInventory == null || slot == null) return null;
		switch (slot) {
		case HAND:
			return playerInventory.getItemInMainHand();
		case OFF_HAND:
			return playerInventory.getItemInOffHand();
		case FEET:
			return playerInventory.getBoots();
		case LEGS:
			return playerInventory.getLeggings();
		case CHEST:
			return playerInventory.getChestplate();
		case HEAD:
			return playerInventory.getHelmet();
		default:
			return null;
		}
	}

	private InventoryUtils() {
	}
}
