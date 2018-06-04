package com.nisovin.shopkeepers.compat.api;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.util.Vector;

import com.nisovin.shopkeepers.api.util.TradingRecipe;

public interface NMSCallProvider {

	public String getVersionId();

	public boolean openTradeWindow(String title, List<TradingRecipe> recipes, Player player);

	public TradingRecipe getUsedTradingRecipe(MerchantInventory merchantInventory);

	public void overwriteLivingEntityAI(LivingEntity entity);

	// whether tickAI and getCollisionDistance are supported
	public default boolean supportsCustomMobAI() {
		return true;
	}

	public void tickAI(LivingEntity entity);

	// returns the distance to the nearest block collision in the range of the given direction vector
	// note: this uses the blocks collision bounding boxes (so this goes through passable blocks, like liquids, etc.)
	// note: does not modify the start location and direction vector
	public double getCollisionDistance(Location start, Vector direction);

	public void setOnGround(Entity entity, boolean onGround);

	public void setEntitySilent(Entity entity, boolean silent);

	public void setNoAI(LivingEntity entity);

	// on some MC versions (ex. MC 1.9, 1.10) NoAI only disables AI
	public default boolean isNoAIDisablingGravity() {
		return true;
	}

	public void setGravity(Entity entity, boolean gravity);

	public void setNoclip(Entity entity);

	public ItemStack loadItemAttributesFromString(ItemStack item, String data);

	public String saveItemAttributesToString(ItemStack item);

	public boolean isMainHandInteraction(PlayerInteractEvent event);

	public boolean isMainHandInteraction(PlayerInteractEntityEvent event);

	public boolean supportsSpawnEggEntityType();

	public void setSpawnEggEntityType(ItemStack spawnEggItem, EntityType entityType);

	public EntityType getSpawnEggEntityType(ItemStack spawnEggItem);

	/**
	 * Checks if the <code>provided</code> itemstack fulfills the requirements of a trading recipe requiring the given
	 * <code>required</code> itemstack.
	 * <p>
	 * This mimics minecraft's item comparison: This checks if the itemstacks are either both emtpy, or of same type and
	 * durability and the provided itemstack's metadata contains all the contents of the required itemstack's metadata
	 * (with list metadata having to be equal).
	 * 
	 * @param provided
	 *            the provided itemstack
	 * @param required
	 *            the required itemstack
	 * @return true if the provided itemstack matches the required itemstack
	 */
	public boolean matches(ItemStack provided, ItemStack required);
}
