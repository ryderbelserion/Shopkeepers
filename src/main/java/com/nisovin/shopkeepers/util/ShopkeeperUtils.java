package com.nisovin.shopkeepers.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopTypeCategory;

/**
 * Utility functions related to shopkeepers and trading.
 */
public class ShopkeeperUtils {

	private ShopkeeperUtils() {
	}

	private static final int SHOPKEEPER_TARGET_RANGE = 10;

	public static TradingRecipe getSelectedTradingRecipe(MerchantInventory merchantInventory) {
		MerchantRecipe merchantRecipe = merchantInventory.getSelectedRecipe();
		return createTradingRecipe(merchantRecipe);
	}

	public static TradingRecipe createTradingRecipe(MerchantRecipe merchantRecipe) {
		if (merchantRecipe == null) return null;
		List<ItemStack> ingredients = merchantRecipe.getIngredients();
		ItemStack item1 = ingredients.get(0);
		ItemStack item2 = null;
		if (ingredients.size() > 1) {
			ItemStack buyItem2 = ingredients.get(1);
			if (!ItemUtils.isEmpty(buyItem2)) {
				item2 = buyItem2;
			}
		}
		ItemStack resultItem = merchantRecipe.getResult();
		return ShopkeepersAPI.createTradingRecipe(resultItem, item1, item2);
	}

	// type is null to allow any shopkeeper type to be returned
	public static List<? extends Shopkeeper> getTargetedShopkeepers(Player player, ShopTypeCategory type, boolean message) {
		Location playerLoc = player.getEyeLocation();
		World world = playerLoc.getWorld();
		Vector viewDirection = playerLoc.getDirection();

		// ray trace to check for the closest block and entity collision:
		// not ignoring passable blocks, in case some type of shopkeeper object makes use of them
		RayTraceResult rayTraceResult = world.rayTrace(playerLoc, viewDirection, SHOPKEEPER_TARGET_RANGE, FluidCollisionMode.NEVER, false, 0.0D, (entity) -> {
			return !entity.equals(player);
		});

		// determine targeted shopkeeper, and print context dependent failure messages:
		if (rayTraceResult != null) {
			Shopkeeper shopkeeper = null;
			Block targetBlock = rayTraceResult.getHitBlock();
			if (targetBlock != null) {
				// get shopkeeper by targeted block:
				shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByBlock(targetBlock);
				if (shopkeeper == null) {
					// get player shopkeepers by targeted chest:
					if (ItemUtils.isChest(targetBlock.getType())) {
						List<PlayerShopkeeper> shopkeepers = SKShopkeepersPlugin.getInstance().getProtectedChests().getShopkeepersUsingChest(targetBlock);
						if (shopkeepers.isEmpty()) {
							if (message) {
								Utils.sendMessage(player, Settings.msgUnusedChest);
							}
							return Collections.emptyList();
						} else if (type == ShopTypeCategory.ADMIN) {
							if (message) {
								Utils.sendMessage(player, Settings.msgTargetShopIsNoAdminShop);
							}
							return Collections.emptyList();
						}
						return shopkeepers;
					}
				}
			} else {
				Entity targetEntity = rayTraceResult.getHitEntity();
				assert targetEntity != null;
				shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByEntity(targetEntity);
				if (shopkeeper == null) {
					if (message) {
						Utils.sendMessage(player, Settings.msgTargetEntityIsNoShop);
					}
					return Collections.emptyList();
				}
			}

			// check if found shopkeeper is a player shopkeeper:
			if (shopkeeper != null) {
				if (type == ShopTypeCategory.PLAYER && !(shopkeeper instanceof PlayerShopkeeper)) {
					if (message) {
						Utils.sendMessage(player, Settings.msgTargetShopIsNoPlayerShop);
					}
					return Collections.emptyList();
				} else if (type == ShopTypeCategory.ADMIN && !(shopkeeper instanceof AdminShopkeeper)) {
					if (message) {
						Utils.sendMessage(player, Settings.msgTargetShopIsNoAdminShop);
					}
					return Collections.emptyList();
				}
				return Arrays.asList(shopkeeper);
			}
		}

		// no targeted shopkeeper found:
		if (message) {
			if (type == ShopTypeCategory.PLAYER) {
				Utils.sendMessage(player, Settings.msgMustTargetPlayerShop);
			} else if (type == ShopTypeCategory.ADMIN) {
				Utils.sendMessage(player, Settings.msgMustTargetAdminShop);
			} else {
				Utils.sendMessage(player, Settings.msgMustTargetShop);
			}
		}
		return Collections.emptyList();
	}
}
