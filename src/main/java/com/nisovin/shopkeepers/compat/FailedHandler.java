package com.nisovin.shopkeepers.compat;

import java.lang.reflect.Method;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public final class FailedHandler implements NMSCallProvider {

	// Minecraft
	private final Class<?> nmsEntityClass;
	private final Method nmsEntitySetOnGroundMethod;

	private final Class<?> nmsItemStackClass;
	private final Method nmsGetTagMethod;

	private final Class<?> nmsGameProfileSerializerClass;
	private final Class<?> nmsNBTBaseClass;
	private final Method nmsAreNBTMatchingMethod;

	// CraftBukkit
	private final Class<?> obcCraftItemStackClass;
	private final Method obcAsNMSCopyMethod;

	private final Class<?> obcCraftEntityClass;
	private final Method obcGetHandleMethod;

	// Bukkit
	private final Method raiderSetCanJoinRaidMethod;

	public FailedHandler() throws Exception {
		String cbVersion = ServerUtils.getCraftBukkitVersion();
		String obcPackageString = "org.bukkit.craftbukkit." + cbVersion + ".";

		// Minecraft
		nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
		nmsGetTagMethod = nmsItemStackClass.getDeclaredMethod("getTag");

		nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
		nmsEntitySetOnGroundMethod = nmsEntityClass.getDeclaredMethod("setOnGround", boolean.class);

		nmsGameProfileSerializerClass = Class.forName("net.minecraft.nbt.GameProfileSerializer");
		nmsNBTBaseClass = Class.forName("net.minecraft.nbt.NBTBase");
		nmsAreNBTMatchingMethod = nmsGameProfileSerializerClass.getDeclaredMethod("a", nmsNBTBaseClass, nmsNBTBaseClass, boolean.class);

		// CraftBukkit
		obcCraftItemStackClass = Class.forName(obcPackageString + "inventory.CraftItemStack");
		obcAsNMSCopyMethod = obcCraftItemStackClass.getDeclaredMethod("asNMSCopy", ItemStack.class);

		obcCraftEntityClass = Class.forName(obcPackageString + "entity.CraftEntity");
		obcGetHandleMethod = obcCraftEntityClass.getDeclaredMethod("getHandle");

		// Bukkit
		// Only available on Bukkit 1.15.1 and upwards:
		raiderSetCanJoinRaidMethod = Raider.class.getDeclaredMethod("setCanJoinRaid", boolean.class);
	}

	@Override
	public String getVersionId() {
		return "FailedHandler";
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
	}

	@Override
	public boolean supportsCustomMobAI() {
		// Not supported. Mobs will be stationary and not react towards nearby players due to the NoAI flag.
		return false;
	}

	@Override
	public void tickAI(LivingEntity entity, int ticks) {
		// Not supported.
	}

	@Override
	public void setOnGround(Entity entity, boolean onGround) {
		try {
			Object mcEntity = obcGetHandleMethod.invoke(entity);
			nmsEntitySetOnGroundMethod.invoke(mcEntity, onGround);
		} catch (Exception e) {
			// Ignoring, since this is not that important if it doesn't work.
		}
	}

	@Override
	public void setNoclip(Entity entity) {
		// Not supported, but also not necessarily required (just provides a small performance benefit).
	}

	@Override
	public void setCanJoinRaid(Raider raider, boolean canJoinRaid) {
		try {
			raiderSetCanJoinRaidMethod.invoke(raider, canJoinRaid);
		} catch (Exception e) {
			// Not supported. Raider mobs might interfere with nearby raids :(
		}
	}

	@Override
	public boolean matches(ItemStack provided, ItemStack required) {
		if (provided == required) return true;
		// If the required item is empty, then the provided item has to be empty as well:
		if (ItemUtils.isEmpty(required)) return ItemUtils.isEmpty(provided);
		else if (ItemUtils.isEmpty(provided)) return false;
		if (provided.getType() != required.getType()) return false;
		try {
			Object nmsProvided = obcAsNMSCopyMethod.invoke(null, provided);
			Object nmsRequired = obcAsNMSCopyMethod.invoke(null, required);
			Object providedTag = nmsGetTagMethod.invoke(nmsProvided);
			Object requiredTag = nmsGetTagMethod.invoke(nmsRequired);
			return (Boolean) nmsAreNBTMatchingMethod.invoke(null, requiredTag, providedTag, false);
		} catch (Exception e) {
			// Fallback: Check for metadata equality. In this case the behavior of this method is no longer equivalent
			// to Minecraft's item comparison behavior!
			// The direction of this check is important, because the required item stack might be an
			// UnmodifiableItemStack.
			return required.isSimilar(provided);
		}
	}

	@Override
	public void updateTrades(Player player) {
		// Not supported.
	}

	@Override
	public String getItemSNBT(ItemStack itemStack) {
		return null; // Not supported.
	}

	@Override
	public String getItemTypeTranslationKey(Material material) {
		return null; // Not supported.
	}
}
