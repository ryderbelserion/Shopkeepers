package com.nisovin.shopkeepers.itemconversion;

import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public class ItemConversions {

	private final ShopkeepersPlugin plugin;
	private final ItemConversionListener listener = new ItemConversionListener();

	public ItemConversions(ShopkeepersPlugin plugin) {
		assert plugin != null;
		this.plugin = plugin;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(listener, plugin);
	}

	public void onDisable() {
		HandlerList.unregisterAll(listener);
	}

	/////

	private static final Predicate<ItemStack> NO_ITEMS_AFFECTED = new Predicate<ItemStack>() {
		@Override
		public boolean test(ItemStack item) {
			return false;
		}
	};

	// Returns NO_ITEMS_AFFECTED if we can guarantee that no items are affected.
	private static Predicate<ItemStack> affectedItemsFilter() {
		if (Settings.convertAllPlayerItems) {
			// Item exceptions is a blacklist:
			return ItemUtils.matchingItems(Settings.convertPlayerItemsExceptions).negate();
		} else {
			// Item exceptions is a white list:
			if (Settings.convertPlayerItemsExceptions.isEmpty()) {
				return NO_ITEMS_AFFECTED;
			} else {
				return ItemUtils.matchingItems(Settings.convertPlayerItemsExceptions);
			}
		}
	}

	public static int convertAffectedItems(Player player, Shopkeeper shopkeeper, boolean informPlayer) {
		Validate.notNull(player, "player is null");
		Validate.notNull(shopkeeper, "shopkeeper is null");

		// Convert player items:
		int convertedStacks = convertAffectedPlayerItems(player);

		// Convert shop container items:
		if (shopkeeper instanceof PlayerShopkeeper) {
			PlayerShopkeeper playerShopkeeper = (PlayerShopkeeper) shopkeeper;
			Block containerBlock = playerShopkeeper.getContainer();
			assert containerBlock != null;
			if (ShopContainers.isSupportedContainer(containerBlock.getType())) {
				long start = System.nanoTime();
				// Note: Returns the complete inventory for double chests.
				Inventory containerInventory = ShopContainers.getInventory(containerBlock);
				int convertedContainerStacks = convertAffectedItems(containerInventory);
				// Note: Inventory changes are directly reflected by the container block in the world.
				long durationMillis = (System.nanoTime() - start) / 1000000L;
				// Note: The conversion always has some performance impact, even if no items got actually converted. We
				// therefore always print the debug messages to allow debugging the item conversion times.
				Log.debug(DebugOptions.itemConversions,
						() -> "Converted " + convertedContainerStacks + " affected item stacks in the container of shopkeeper "
								+ shopkeeper.getId() + ", triggered by player '" + player.getName()
								+ "' (took " + durationMillis + " ms)."
				);
				convertedStacks += convertedContainerStacks;
			}
		}

		// Inform player:
		if (convertedStacks > 0 && informPlayer) {
			TextUtils.sendMessage(player, Messages.itemsConverted,
					"count", convertedStacks
			);
		}
		return convertedStacks;
	}

	private static int convertAffectedPlayerItems(Player player) {
		Validate.notNull(player, "player is null");
		// Convert player inventory contents (includes armor and off hand slots, and cursor):
		Inventory inventory = player.getInventory();
		long start = System.nanoTime();
		int convertedStacks = convertAffectedItems(inventory);
		long durationMillis = (System.nanoTime() - start) / 1000000L;
		// Note: The conversion always has some performance impact, even if no items got actually converted. We
		// therefore always print the debug messages to allow debugging the item conversion times.
		Log.debug(DebugOptions.itemConversions,
				() -> "Converted " + convertedStacks + " affected item stacks in the inventory of player '"
						+ player.getName() + "' (took " + durationMillis + " ms)."
		);
		return convertedStacks;
	}

	private static int convertAffectedItems(Inventory inventory) {
		Validate.notNull(inventory, "inventory is null");
		int convertedStacks = 0;
		Predicate<ItemStack> affectedItemsFilter = affectedItemsFilter();
		if (affectedItemsFilter != NO_ITEMS_AFFECTED) {
			// Convert items and update viewers if there have been changes:
			convertedStacks = ItemUtils.convertItems(inventory, affectedItemsFilter, true);
		}
		return convertedStacks;
	}
}
