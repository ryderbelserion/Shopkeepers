package com.nisovin.shopkeepers.compat.api;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;

import com.nisovin.shopkeepers.TradingRecipe;

public interface NMSCallProvider {

	public String getVersionId();

	public boolean openTradeWindow(String title, List<TradingRecipe> recipes, Player player);

	public TradingRecipe getUsedTradingRecipe(MerchantInventory merchantInventory);

	public void overwriteLivingEntityAI(LivingEntity entity);

	public void setEntitySilent(Entity entity, boolean silent);

	public void setNoAI(LivingEntity bukkitEntity);

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
