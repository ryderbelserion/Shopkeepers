package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.util.TradingRecipe;
import com.nisovin.shopkeepers.compat.NMSManager;

/**
 * Utility functions related to materials, items and inventories.
 */
public final class ItemUtils {

	private ItemUtils() {
	}

	// material utilities:

	public static boolean isChest(Material material) {
		return material == Material.CHEST || material == Material.TRAPPED_CHEST;
	}

	public static boolean isSign(Material material) {
		return material == Material.WALL_SIGN || material == Material.SIGN_POST || material == Material.SIGN;
	}

	// itemstack utilities:

	public static boolean isEmpty(ItemStack item) {
		return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
	}

	public static ItemStack getNullIfEmpty(ItemStack item) {
		return isEmpty(item) ? null : item;
	}

	// TODO temporary, due to a bukkit bug custom head item can currently not be saved
	public static boolean isCustomHeadItem(ItemStack item) {
		if (item == null) return false;
		if (item.getType() != Material.SKULL_ITEM) {
			return false;
		}
		if (item.getDurability() != SkullType.PLAYER.ordinal()) {
			return false;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta instanceof SkullMeta) {
			SkullMeta skullMeta = (SkullMeta) meta;
			if (skullMeta.hasOwner() && skullMeta.getOwner() == null) {
				// custom head items usually don't have a valid owner
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates a clone of the given {@link ItemStack} with amount <code>1</code>.
	 * 
	 * @param item
	 *            the item to get a normalized version of
	 * @return the normalized item
	 */
	public static ItemStack getNormalizedItem(ItemStack item) {
		if (item == null) return null;
		ItemStack normalizedClone = item.clone();
		normalizedClone.setAmount(1);
		return normalizedClone;
	}

	public static ItemStack createItemStack(Material type, int amount, short data, String displayName, List<String> lore) {
		// TODO return null in case of type AIR?
		ItemStack item = new ItemStack(type, amount, data);
		return ItemUtils.setItemStackNameAndLore(item, displayName, lore);
	}

	public static ItemStack setItemStackNameAndLore(ItemStack item, String displayName, List<String> lore) {
		if (item == null) return null;
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(displayName);
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
		return item;
	}

	public static String getSimpleItemInfo(ItemStack item) {
		if (item == null) return "empty";
		StringBuilder sb = new StringBuilder();
		sb.append(item.getAmount()).append('x').append(item.getType());
		if (item.getDurability() != 0) {
			sb.append('~').append(item.getDurability());
		}
		return sb.toString();
	}

	public static String getSimpleRecipeInfo(TradingRecipe recipe) {
		if (recipe == null) return "none";
		StringBuilder sb = new StringBuilder();
		sb.append("[item1=").append(getSimpleItemInfo(recipe.getItem1()))
				.append(", item2=").append(getSimpleItemInfo(recipe.getItem2()))
				.append(", result=").append(getSimpleItemInfo(recipe.getResultItem())).append("]");
		return sb.toString();
	}

	/**
	 * Same as {@link ItemStack#isSimilar(ItemStack)}, but taking into account that both given ItemStacks might be
	 * <code>null</code>.
	 * 
	 * @param item1
	 *            an itemstack
	 * @param item2
	 *            another itemstack
	 * @return <code>true</code> if the given item stacks are both <code>null</code> or similar
	 */
	public static boolean isSimilar(ItemStack item1, ItemStack item2) {
		if (item1 == null) return (item2 == null);
		return item1.isSimilar(item2);
	}

	/**
	 * Checks if the given item matches the specified attributes.
	 * 
	 * @param item
	 *            the item
	 * @param type
	 *            the item type
	 * @param data
	 *            the data value/durability, or <code>-1</code> to ignore it
	 * @param displayName
	 *            the displayName, or <code>null</code> or empty to ignore it
	 * @param lore
	 *            the item lore, or <code>null</code> or empty to ignore it
	 * @return <code>true</code> if the item has similar attributes
	 */
	public static boolean isSimilar(ItemStack item, Material type, short data, String displayName, List<String> lore) {
		if (item == null) return false;
		if (item.getType() != type) return false;
		if (data != -1 && item.getDurability() != data) return false;

		ItemMeta itemMeta = null;
		// compare display name:
		if (displayName != null && !displayName.isEmpty()) {
			if (!item.hasItemMeta()) return false;
			itemMeta = item.getItemMeta();
			if (itemMeta == null) return false;

			if (!itemMeta.hasDisplayName() || !displayName.equals(itemMeta.getDisplayName())) {
				return false;
			}
		}

		// compare lore:
		if (lore != null && !lore.isEmpty()) {
			if (itemMeta == null) {
				if (!item.hasItemMeta()) return false;
				itemMeta = item.getItemMeta();
				if (itemMeta == null) return false;
			}

			if (!itemMeta.hasLore() || !lore.equals(itemMeta.getLore())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Increases the amount of the given {@link ItemStack}.
	 * <p>
	 * This makes sure that the itemstack's amount ends up to be at most {@link ItemStack#getMaxStackSize()}, and that
	 * empty itemstacks are represented by <code>null</code>.
	 * 
	 * @param itemStack
	 *            the itemstack, can be empty
	 * @param amountToIncrease
	 *            the amount to increase, can be negative to decrease
	 * @return the resulting item, or <code>null</code> if the item ends up being empty
	 */
	public static ItemStack increaseItemAmount(ItemStack itemStack, int amountToIncrease) {
		if (isEmpty(itemStack)) return null;
		int newAmount = Math.min(itemStack.getAmount() + amountToIncrease, itemStack.getMaxStackSize());
		if (newAmount <= 0) return null;
		itemStack.setAmount(newAmount);
		return itemStack;
	}

	/**
	 * Decreases the amount of the given {@link ItemStack}.
	 * <p>
	 * This makes sure that the itemstack's amount ends up to be at most {@link ItemStack#getMaxStackSize()}, and that
	 * empty itemstacks are represented by <code>null</code>.
	 * 
	 * @param itemStack
	 *            the itemstack, can be empty
	 * @param amountToDescrease
	 *            the amount to decrease, can be negative to increase
	 * @return the resulting item, or <code>null</code> if the item ends up being empty
	 */
	public static ItemStack descreaseItemAmount(ItemStack itemStack, int amountToDescrease) {
		return increaseItemAmount(itemStack, -amountToDescrease);
	}

	/**
	 * Gets an itemstack's amount and returns <code>0</code> for empty itemstacks.
	 * 
	 * @param itemStack
	 *            the itemstack, can be empty
	 * @return the itemstack's amount, or <code>0</code> if the itemstack is empty
	 */
	public static int getItemStackAmount(ItemStack itemStack) {
		return (isEmpty(itemStack) ? 0 : itemStack.getAmount());
	}

	// save and load itemstacks from config, including attributes:

	/**
	 * Saves the given {@link ItemStack} to the given configuration section.
	 * Also saves the item's attributes in the same section at '{node}_attributes'.
	 * 
	 * @param section
	 *            a configuration section
	 * @param node
	 *            where to save the item stack inside the section
	 * @param item
	 *            the item stack to save, can be <code>null</code>
	 */
	public static void saveItem(ConfigurationSection section, String node, ItemStack item) {
		assert section != null && node != null;
		section.set(node, item);
		// saving attributes manually, as they weren't saved by bukkit in the past:
		String attributes = NMSManager.getProvider().saveItemAttributesToString(item);
		if (attributes != null && !attributes.isEmpty()) {
			String attributesNode = node + "_attributes";
			section.set(attributesNode, attributes);
		}
	}

	/**
	 * Loads an {@link ItemStack} from the given configuration section.
	 * Also attempts to load attributes saved at '{node}_attributes'.
	 * 
	 * @param section
	 *            a configuration section
	 * @param node
	 *            where to load the item stack from inside the section
	 * @return the loaded item stack, possibly <code>null</code>
	 */
	public static ItemStack loadItem(ConfigurationSection section, String node) {
		assert section != null && node != null;
		ItemStack item = section.getItemStack(node);
		// loading separately stored attributes:
		String attributesNode = node + "_attributes";
		if (item != null && section.contains(attributesNode)) {
			String attributes = section.getString(attributesNode);
			if (attributes != null && !attributes.isEmpty()) {
				item = NMSManager.getProvider().loadItemAttributesFromString(item, attributes);
			}
		}
		return item;
	}

	// inventory utilities:

	// somewhere in early 1.9 getStorageContents was introduced and the previous behavior of getContents was changed for
	// player inventories to now return the combined inventory contents
	// TODO remove this once we support 1.9+
	private static final int PLAYER_INVENTORY_STORAGE_SIZE = 36;

	/**
	 * Gets the storage contents from the specified inventory.
	 * 
	 * @param inventory
	 *            the inventory
	 * @return the storage contents
	 */
	public static ItemStack[] getStorageContents(Inventory inventory) {
		assert inventory != null;
		ItemStack[] storageContents = inventory.getContents();
		if (inventory instanceof PlayerInventory) {
			storageContents = Arrays.copyOf(storageContents, PLAYER_INVENTORY_STORAGE_SIZE);
		}
		return storageContents;
	}

	/**
	 * Sets the storage contents of the specified inventory.
	 * 
	 * @param inventory
	 *            the inventory
	 * @param storageContents
	 *            the new storage contents
	 */
	public static void setStorageContents(Inventory inventory, ItemStack[] storageContents) {
		assert inventory != null;
		// storage contents are always stored at the beginning of the inventory
		for (int slotId = 0; slotId < storageContents.length; slotId++) {
			inventory.setItem(slotId, storageContents[slotId]);
		}
	}

	public static List<ItemCount> countItems(ItemStack[] contents, Filter<ItemStack> filter) {
		List<ItemCount> itemCounts = new ArrayList<>();
		if (contents == null) return itemCounts;
		for (ItemStack item : contents) {
			if (isEmpty(item)) continue;
			if (filter != null && !filter.accept(item)) continue;

			// check if we already have a counter for this type of item:
			ItemCount itemCount = ItemCount.findSimilar(itemCounts, item);
			if (itemCount != null) {
				// increase item count:
				itemCount.addAmount(item.getAmount());
			} else {
				// add new item entry:
				itemCounts.add(new ItemCount(item, item.getAmount()));
			}
		}
		return itemCounts;
	}

	/**
	 * Checks if the given contents contains at least the specified amount of items matching the specified attributes.
	 * 
	 * @param contents
	 *            the contents to search through
	 * @param type
	 *            the item type
	 * @param data
	 *            the data value/durability, or <code>-1</code> to ignore it
	 * @param displayName
	 *            the displayName, or <code>null</code> to ignore it
	 * @param lore
	 *            the item lore, or <code>null</code> or empty to ignore it
	 * @param amount
	 *            the amount of items to look for
	 * @return <code>true</code> if the at least specified amount of matching items was found
	 */
	public static boolean containsAtLeast(ItemStack[] contents, Material type, short data, String displayName, List<String> lore, int amount) {
		if (contents == null) return false;
		int remainingAmount = amount;
		for (ItemStack itemStack : contents) {
			if (!isSimilar(itemStack, type, data, displayName, lore)) continue;
			int currentAmount = itemStack.getAmount() - remainingAmount;
			if (currentAmount >= 0) {
				return true;
			} else {
				remainingAmount = -currentAmount;
			}
		}
		return false;
	}

	/**
	 * Removes the specified amount of items which match the specified attributes from the given contents.
	 * 
	 * @param contents
	 *            the contents
	 * @param type
	 *            the item type
	 * @param data
	 *            the data value/durability, or <code>-1</code> to ignore it
	 * @param displayName
	 *            the display name, or <code>null</code> to ignore it
	 * @param lore
	 *            the item lore, or <code>null</code> or empty to ignore it
	 * @param amount
	 *            the amount of matching items to remove
	 * @return the amount of items that couldn't be removed (<code>0</code> on full success)
	 */
	public static int removeItems(ItemStack[] contents, Material type, short data, String displayName, List<String> lore, int amount) {
		if (contents == null) return amount;
		int remainingAmount = amount;
		for (int slotId = 0; slotId < contents.length; slotId++) {
			ItemStack itemStack = contents[slotId];
			if (!isSimilar(itemStack, type, data, displayName, lore)) continue;
			int newAmount = itemStack.getAmount() - remainingAmount;
			if (newAmount > 0) {
				itemStack.setAmount(newAmount);
				break;
			} else {
				contents[slotId] = null;
				remainingAmount = -newAmount;
				if (remainingAmount == 0) break;
			}
		}
		return remainingAmount;
	}

	/**
	 * Adds the given {@link ItemStack} to the given contents.
	 * 
	 * <p>
	 * This will first try to fill similar partial {@link ItemStack}s in the contents up to the item's max stack size.
	 * Afterwards it will insert the remaining amount into empty slots, splitting at the item's max stack size.
	 * <p>
	 * This does not modify the original item stacks in the given array. If it has to modify the amount of an item
	 * stack, it first replaces it with a copy. So in case those item stacks are mirroring changes to their minecraft
	 * counterpart, those don't get affected directly.<br>
	 * The item being added gets copied as well before it gets inserted in an empty slot.
	 * 
	 * @param contents
	 *            the contents to add the given {@link ItemStack} to
	 * @param item
	 *            the {@link ItemStack} to add
	 * @return the amount of items which couldn't be added (<code>0</code> on full success)
	 */
	public static int addItems(ItemStack[] contents, ItemStack item) {
		Validate.notNull(contents);
		Validate.notNull(item);
		int amount = item.getAmount();
		Validate.isTrue(amount >= 0);
		if (amount == 0) return 0;

		// search for partially fitting item stacks:
		int maxStackSize = item.getMaxStackSize();
		int size = contents.length;
		for (int slot = 0; slot < size; slot++) {
			ItemStack slotItem = contents[slot];

			// slot empty? - skip, because we are currently filling existing item stacks up
			if (isEmpty(slotItem)) continue;

			// slot already full?
			int slotAmount = slotItem.getAmount();
			if (slotAmount >= maxStackSize) continue;

			if (slotItem.isSimilar(item)) {
				// copy itemstack, so we don't modify the original itemstack:
				slotItem = slotItem.clone();
				contents[slot] = slotItem;

				int newAmount = slotAmount + amount;
				if (newAmount <= maxStackSize) {
					// remaining amount did fully fit into this stack:
					slotItem.setAmount(newAmount);
					return 0;
				} else {
					// did not fully fit:
					slotItem.setAmount(maxStackSize);
					amount -= (maxStackSize - slotAmount);
					assert amount != 0;
				}
			}
		}

		// we have items remaining:
		assert amount > 0;

		// search for empty slots:
		for (int slot = 0; slot < size; slot++) {
			ItemStack slotItem = contents[slot];
			if (isEmpty(slotItem)) {
				// found empty slot:
				if (amount > maxStackSize) {
					// add full stack:
					ItemStack stack = item.clone();
					stack.setAmount(maxStackSize);
					contents[slot] = stack;
					amount -= maxStackSize;
				} else {
					// completely fits:
					ItemStack stack = item.clone(); // create a copy, just in case
					stack.setAmount(amount); // stack of remaining amount
					contents[slot] = stack;
					return 0;
				}
			}
		}

		// not all items did fit into the inventory:
		return amount;
	}

	/**
	 * Removes the given {@link ItemStack} from the given contents.
	 * 
	 * <p>
	 * If the amount of the given {@link ItemStack} is {@link Integer#MAX_VALUE}, then all similar items are being
	 * removed from the contents.<br>
	 * This does not modify the original item stacks. If it has to modify the amount of an item stack, it first replaces
	 * it with a copy. So in case those item stacks are mirroring changes to their minecraft counterpart, those don't
	 * get affected directly.
	 * </p>
	 * 
	 * @param contents
	 *            the contents to remove the given {@link ItemStack} from
	 * @param item
	 *            the {@link ItemStack} to remove from the given contents
	 * @return the amount of items which couldn't be removed (<code>0</code> on full success)
	 */
	public static int removeItems(ItemStack[] contents, ItemStack item) {
		Validate.notNull(contents);
		Validate.notNull(item);
		int amount = item.getAmount();
		Validate.isTrue(amount >= 0);
		if (amount == 0) return 0;

		boolean removeAll = (amount == Integer.MAX_VALUE);
		for (int slot = 0; slot < contents.length; slot++) {
			ItemStack slotItem = contents[slot];
			if (slotItem == null) continue;
			if (item.isSimilar(slotItem)) {
				if (removeAll) {
					contents[slot] = null;
				} else {
					int newAmount = slotItem.getAmount() - amount;
					if (newAmount > 0) {
						// copy itemstack, so we don't modify the original itemstack:
						slotItem = slotItem.clone();
						contents[slot] = slotItem;
						slotItem.setAmount(newAmount);
						// all items were removed:
						return 0;
					} else {
						contents[slot] = null;
						amount = -newAmount;
						if (amount == 0) {
							// all items were removed:
							return 0;
						}
					}
				}
			}
		}

		if (removeAll) return 0;
		return amount;
	}

	@SuppressWarnings("deprecation")
	public static void updateInventoryLater(Player player) {
		Bukkit.getScheduler().runTask(SKShopkeepersPlugin.getInstance(), () -> player.updateInventory());
	}
}
