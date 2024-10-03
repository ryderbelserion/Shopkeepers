package com.nisovin.shopkeepers.compat;

import java.lang.reflect.Method;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public final class FailedHandler implements NMSCallProvider {

	// Minecraft
	private final Class<?> nmsEntityClass;
	private final Method nmsEntitySetOnGroundMethod;

	// CraftBukkit
	private final Class<?> obcCraftEntityClass;
	private final Method obcGetHandleMethod;

	// Bukkit

	// null if not supported yet (e.g. pre 1.21):
	private @Nullable Method b_EntityExplodeEvent_GetExplosionResultMethod = null;
	private @Nullable Method b_BlockExplodeEvent_GetExplosionResultMethod = null;

	public FailedHandler() throws Exception {
		String cbPackage = ServerUtils.getCraftBukkitPackage();

		// Minecraft

		nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
		nmsEntitySetOnGroundMethod = nmsEntityClass.getDeclaredMethod(
				"d", // setOnGround
				boolean.class
		);

		// CraftBukkit

		obcCraftEntityClass = Class.forName(cbPackage + ".entity.CraftEntity");
		obcGetHandleMethod = obcCraftEntityClass.getDeclaredMethod("getHandle");

		// Bukkit

		try {
			b_EntityExplodeEvent_GetExplosionResultMethod = EntityExplodeEvent.class.getMethod("getExplosionResult");
			b_BlockExplodeEvent_GetExplosionResultMethod = BlockExplodeEvent.class.getMethod("getExplosionResult");
		} catch (NoSuchMethodException e) {
			// Not found, e.g. pre 1.21.
		}
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
		// Not supported. Mobs will be stationary and not react towards nearby players due to the
		// NoAI flag.
		return false;
	}

	@Override
	public void tickAI(LivingEntity entity, int ticks) {
		// Not supported.
	}

	@Override
	public void setOnGround(Entity entity, boolean onGround) {
		try {
			Object mcEntity = Unsafe.assertNonNull(obcGetHandleMethod.invoke(entity));
			nmsEntitySetOnGroundMethod.invoke(mcEntity, onGround);
		} catch (Exception e) {
			// Ignoring, since this is not that important if it doesn't work.
		}
	}

	@Override
	public void setNoclip(Entity entity) {
		// Not supported, but also not necessarily required (just provides a small performance
		// benefit).
	}

	@Override
	public boolean matches(@Nullable ItemStack provided, @Nullable ItemStack required) {
		if (provided == required) return true;
		// If the required item is empty, then the provided item has to be empty as well:
		if (ItemUtils.isEmpty(required)) return ItemUtils.isEmpty(provided);
		else if (ItemUtils.isEmpty(provided)) return false;
		assert required != null && provided != null;
		if (provided.getType() != required.getType()) return false;

		// TODO Minecraft 1.20.5+ uses DataComponentPredicates for matching items. Implement a
		// reflection-based fallback?

		// Fallback: Check for metadata equality. This behavior is stricter than vanilla Minecraft's
		// item comparison.
		return required.isSimilar(provided);
	}

	@Override
	public void updateTrades(Player player) {
		// Not supported.
	}

	@Override
	public @Nullable String getItemSNBT(ItemStack itemStack) {
		return null; // Not supported.
	}

	// MC 1.21+ TODO Can be removed once we only support Bukkit 1.21+

	@Override
	public boolean isDestroyingBlocks(EntityExplodeEvent event) {
		if (b_EntityExplodeEvent_GetExplosionResultMethod == null) return true;

		try {
			assert b_EntityExplodeEvent_GetExplosionResultMethod != null;
			var explosionResult = b_EntityExplodeEvent_GetExplosionResultMethod.invoke(event);
			assert explosionResult != null;
			return isDestroyingBlocks(explosionResult);
		} catch (Exception e) {
			// Something unexpected went wrong. Assume pre 1.21 behavior. Wind charges may break
			// player shops when container protection is disabled (#921).
			return true;
		}
	}

	@Override
	public boolean isDestroyingBlocks(BlockExplodeEvent event) {
		if (b_BlockExplodeEvent_GetExplosionResultMethod == null) return true;

		try {
			assert b_BlockExplodeEvent_GetExplosionResultMethod != null;
			var explosionResult = b_BlockExplodeEvent_GetExplosionResultMethod.invoke(event);
			assert explosionResult != null;
			return isDestroyingBlocks(explosionResult);
		} catch (Exception e) {
			// Something unexpected went wrong. Assume pre 1.21 behavior. Wind charges may break
			// player shops when container protection is disabled (#921).
			return true;
		}
	}

	private static boolean isDestroyingBlocks(Object explosionResult) {
		var explosionResultString = explosionResult.toString();
		return explosionResultString.equals("DESTROY")
				|| explosionResultString.equals("DESTROY_WITH_DECAY");
	}
}
