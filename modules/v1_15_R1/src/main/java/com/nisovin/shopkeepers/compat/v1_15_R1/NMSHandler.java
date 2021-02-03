package com.nisovin.shopkeepers.compat.v1_15_R1;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftMerchant;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftMagicNumbers;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.ItemUtils;

import net.minecraft.server.v1_15_R1.Entity;
import net.minecraft.server.v1_15_R1.EntityHuman;
import net.minecraft.server.v1_15_R1.EntityInsentient;
import net.minecraft.server.v1_15_R1.EntityLiving;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.GameProfileSerializer;
import net.minecraft.server.v1_15_R1.IMerchant;
import net.minecraft.server.v1_15_R1.MerchantRecipeList;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_15_R1.PathfinderGoalSelector;

public final class NMSHandler implements NMSCallProvider {

	@Override
	public String getVersionId() {
		return "1_15_R1";
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			EntityLiving mcLivingEntity = ((CraftLivingEntity) entity).getHandle();
			// Example: Armor stands are living, but not insentient.
			if (!(mcLivingEntity instanceof EntityInsentient)) return;
			EntityInsentient mcInsentientEntity = (EntityInsentient) mcLivingEntity;

			// Make the goal selector items accessible:
			Field cField = PathfinderGoalSelector.class.getDeclaredField("c"); // Active goals
			cField.setAccessible(true);
			Field dField = PathfinderGoalSelector.class.getDeclaredField("d"); // Registered goals
			dField.setAccessible(true);

			// Overwrite the goal selector:
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(mcLivingEntity);

			// Clear old goals:
			Map<?, ?> goals_c = (Map<?, ?>) cField.get(goals);
			goals_c.clear();
			Set<?> goals_d = (Set<?>) dField.get(goals);
			goals_d.clear();

			// Add new goals:
			goals.a(0, new PathfinderGoalLookAtPlayer(mcInsentientEntity, EntityHuman.class, 12.0F, 1.0F));

			// Overwrite the target selector:
			Field targetsField = EntityInsentient.class.getDeclaredField("targetSelector");
			targetsField.setAccessible(true);
			PathfinderGoalSelector targets = (PathfinderGoalSelector) targetsField.get(mcLivingEntity);

			// Clear old target goals:
			Map<?, ?> targets_c = (Map<?, ?>) cField.get(targets);
			targets_c.clear();
			Set<?> targets_d = (Set<?>) dField.get(targets);
			targets_d.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void tickAI(LivingEntity entity, int ticks) {
		EntityLiving mcLivingEntity = ((CraftLivingEntity) entity).getHandle();
		// Example: Armor stands are living, but not insentient.
		if (!(mcLivingEntity instanceof EntityInsentient)) return;
		EntityInsentient mcInsentientEntity = (EntityInsentient) mcLivingEntity;
		mcInsentientEntity.getEntitySenses().a(); // Clear sensing cache
		// The sensing cache is reused for the indivual ticks.
		for (int i = 0; i < ticks; ++i) {
			mcInsentientEntity.goalSelector.doTick();
			mcInsentientEntity.getControllerLook().a(); // Tick look controller
		}
		mcInsentientEntity.getEntitySenses().a(); // Clear sensing cache
	}

	@Override
	public void setOnGround(org.bukkit.entity.Entity entity, boolean onGround) {
		Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.onGround = onGround;
	}

	@Override
	public boolean isNoAIDisablingGravity() {
		return true;
	}

	@Override
	public void setNoclip(org.bukkit.entity.Entity entity) {
		Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.noclip = true;
	}

	@Override
	public void setCanJoinRaid(Raider raider, boolean canJoinRaid) {
		// Only works in the latest versions of Bukkit 1.15.1 and upwards:
		raider.setCanJoinRaid(canJoinRaid);
	}

	@Override
	public boolean matches(ItemStack provided, ItemStack required) {
		if (provided == required) return true;
		// If the required item is empty, then the provided item has to be empty as well:
		if (ItemUtils.isEmpty(required)) return ItemUtils.isEmpty(provided);
		else if (ItemUtils.isEmpty(provided)) return false;
		if (provided.getType() != required.getType()) return false;
		net.minecraft.server.v1_15_R1.ItemStack nmsProvided = CraftItemStack.asNMSCopy(provided);
		net.minecraft.server.v1_15_R1.ItemStack nmsRequired = CraftItemStack.asNMSCopy(required);
		NBTTagCompound providedTag = nmsProvided.getTag();
		NBTTagCompound requiredTag = nmsRequired.getTag();
		// Compare the tags according to Minecraft's matching rules (imprecise):
		return GameProfileSerializer.a(requiredTag, providedTag, false);
	}

	@Override
	public void updateTrades(Player player) {
		Inventory openInventory = player.getOpenInventory().getTopInventory();
		if (!(openInventory instanceof MerchantInventory)) {
			return;
		}
		MerchantInventory merchantInventory = (MerchantInventory) openInventory;

		// Update the merchant inventory on the server (updates the result item, etc.):
		merchantInventory.setItem(0, merchantInventory.getItem(0));

		Merchant merchant = merchantInventory.getMerchant();
		IMerchant nmsMerchant;
		boolean regularVillager = false;
		boolean canRestock = false;
		// Note: When using the 'is-regular-villager'-flag, using level 0 allows hiding the level name suffix.
		int merchantLevel = 1;
		int merchantExperience = 0;
		if (merchant instanceof Villager) {
			nmsMerchant = ((CraftVillager) merchant).getHandle();
			Villager villager = (Villager) merchant;
			regularVillager = true;
			canRestock = true;
			merchantLevel = villager.getVillagerLevel();
			merchantExperience = villager.getVillagerExperience();
		} else if (merchant instanceof AbstractVillager) {
			nmsMerchant = ((CraftAbstractVillager) merchant).getHandle();
		} else {
			nmsMerchant = ((CraftMerchant) merchant).getMerchant();
			merchantLevel = 0; // Hide name suffix
		}
		MerchantRecipeList merchantRecipeList = nmsMerchant.getOffers();
		if (merchantRecipeList == null) merchantRecipeList = new MerchantRecipeList(); // Just in case

		// Send PacketPlayOutOpenWindowMerchant packet: window id, recipe list, merchant level (1: Novice, .., 5:
		// Master), merchant total experience, is-regular-villager flag (false: hides some gui elements), can-restock
		// flag (false: hides restock message if out of stock)
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		nmsPlayer.openTrade(nmsPlayer.activeContainer.windowId, merchantRecipeList, merchantLevel, merchantExperience, regularVillager, canRestock);
	}

	@Override
	public String getItemSNBT(ItemStack itemStack) {
		if (itemStack == null) return null;
		net.minecraft.server.v1_15_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
		NBTTagCompound itemNBT = nmsItem.save(new NBTTagCompound());
		return itemNBT.toString();
	}

	@Override
	public String getItemTypeTranslationKey(Material material) {
		if (material == null) return null;
		net.minecraft.server.v1_15_R1.Item nmsItem = CraftMagicNumbers.getItem(material);
		if (nmsItem == null) return null;
		return nmsItem.getName();
	}
}
