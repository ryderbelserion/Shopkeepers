package com.nisovin.shopkeepers.itemconversion;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class ItemConversions {

	private final ShopkeepersPlugin plugin;
	private final ItemConversionListener listener = new ItemConversionListener();

	public ItemConversions(ShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(listener, plugin);
	}

	public void onDisable() {
		HandlerList.unregisterAll(listener);
	}

	/////

	// Note: This is not a lambda, because we use the object's identity for performance optimization
	// purposes.
	private static final Predicate<@Nullable ItemStack> NO_ITEMS_AFFECTED = new Predicate<@Nullable ItemStack>() {
		@Override
		public boolean test(@Nullable ItemStack item) {
			return false;
		}
	};

	// Returns NO_ITEMS_AFFECTED if we can guarantee that no items are affected.
	private static Predicate<@Nullable ItemStack> affectedItemsFilter() {
		if (Settings.convertAllPlayerItems) {
			// Item exceptions is a blacklist:
			return Unsafe.assertNonNull(
					ItemUtils.matchingItems(Settings.convertPlayerItemsExceptions).negate()
			);
		} else {
			// Item exceptions is a white list:
			if (Settings.convertPlayerItemsExceptions.isEmpty()) {
				return NO_ITEMS_AFFECTED;
			} else {
				return ItemUtils.matchingItems(Settings.convertPlayerItemsExceptions);
			}
		}
	}

	public static int convertAffectedItems(
			Player player,
			Shopkeeper shopkeeper,
			boolean informPlayer
	) {
		Validate.notNull(player, "player is null");
		Validate.notNull(shopkeeper, "shopkeeper is null");

		// Convert player items:
		int convertedStacks = convertAffectedPlayerItems(player);

		// Convert shop container items:
		if (shopkeeper instanceof PlayerShopkeeper) {
			PlayerShopkeeper playerShopkeeper = (PlayerShopkeeper) shopkeeper;
			Block containerBlock = playerShopkeeper.getContainer();
			if (containerBlock != null
					&& ShopContainers.isSupportedContainer(containerBlock.getType())) {
				long startNanos = System.nanoTime();
				// Returns the complete inventory for double chests.
				// Inventory changes are directly reflected by the container block in the world.
				Inventory containerInventory = ShopContainers.getInventory(containerBlock);
				int convertedContainerStacks = convertAffectedItems(containerInventory);
				long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
				// The conversion always has some performance impact, even if no items were actually
				// converted. To enable debugging these item conversion timings, we always print
				// this debug message, even if no items were converted.
				Log.debug(DebugOptions.itemConversions,
						() -> shopkeeper.getLogPrefix() + "Player '" + player.getName()
								+ "' triggered the conversion of " + convertedContainerStacks
								+ " affected item stacks inside the shop container (took "
								+ durationMillis + " ms)."
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
		long startNanos = System.nanoTime();
		int convertedStacks = convertAffectedItems(inventory);
		long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
		// Note: The conversion always has some performance impact, even if no items got actually
		// converted. We therefore always print the debug messages to allow debugging the item
		// conversion times.
		Log.debug(DebugOptions.itemConversions,
				() -> "Converted " + convertedStacks
						+ " affected item stacks in the inventory of player '" + player.getName()
						+ "' (took " + durationMillis + " ms)."
		);
		return convertedStacks;
	}

	private static int convertAffectedItems(Inventory inventory) {
		Validate.notNull(inventory, "inventory is null");
		int convertedStacks = 0;
		Predicate<@Nullable ItemStack> affectedItemsFilter = affectedItemsFilter();
		if (affectedItemsFilter != NO_ITEMS_AFFECTED) {
			// Convert items and update viewers if there have been changes:
			convertedStacks = ItemConversion.convertItems(inventory, affectedItemsFilter, true);
		}
		return convertedStacks;
	}
}
