package com.nisovin.shopkeepers.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Utils;

public final class FailedHandler implements NMSCallProvider {

	// Minecraft
	private final Class<?> nmsEntityClass;
	private final Field nmsNoclipField;
	private final Field nmsOnGroundField;

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

	public FailedHandler() throws Exception {
		String cbVersion = Utils.getServerCBVersion();
		String nmsPackageString = "net.minecraft.server." + cbVersion + ".";
		// String bukkitPackageString = "org.bukkit.";
		String obcPackageString = "org.bukkit.craftbukkit." + cbVersion + ".";

		// Minecraft
		nmsItemStackClass = Class.forName(nmsPackageString + "ItemStack");
		nmsGetTagMethod = nmsItemStackClass.getDeclaredMethod("getTag");

		nmsEntityClass = Class.forName(nmsPackageString + "Entity");
		nmsNoclipField = nmsEntityClass.getDeclaredField("noclip");
		nmsNoclipField.setAccessible(true);
		nmsOnGroundField = nmsEntityClass.getDeclaredField("onGround");
		nmsOnGroundField.setAccessible(true);

		nmsGameProfileSerializerClass = Class.forName(nmsPackageString + "GameProfileSerializer");
		nmsNBTBaseClass = Class.forName(nmsPackageString + "NBTBase");
		nmsAreNBTMatchingMethod = nmsGameProfileSerializerClass.getDeclaredMethod("a", nmsNBTBaseClass, nmsNBTBaseClass, boolean.class);

		// CraftBukkit
		obcCraftItemStackClass = Class.forName(obcPackageString + "inventory.CraftItemStack");
		obcAsNMSCopyMethod = obcCraftItemStackClass.getDeclaredMethod("asNMSCopy", ItemStack.class);

		obcCraftEntityClass = Class.forName(obcPackageString + "entity.CraftEntity");
		obcGetHandleMethod = obcCraftEntityClass.getDeclaredMethod("getHandle");
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
			nmsOnGroundField.set(mcEntity, onGround);
		} catch (Exception e) {
			// Ignoring, since this is not that important if it doesn't work.
		}
	}

	@Override
	public void setNoclip(Entity entity) {
		try {
			// When gravity gets disabled, we are able to also disable collisions/pushing of mobs via the noclip flag.
			// This might not properly work for Vex, since they disable noclip again after their movement.
			Object mcEntity = obcGetHandleMethod.invoke(entity);
			nmsNoclipField.set(mcEntity, true);
		} catch (Exception e) {
			// This is optional, ignore if not possible for some reason.
		}
	}

	@Override
	public void setCanJoinRaid(Raider raider, boolean canJoinRaid) {
		// Not supported :( Raider mobs might interfere with nearby raids TODO
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
