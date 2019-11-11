package com.nisovin.shopkeepers.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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

	// Bukkit
	private final AttributeModifier movementSpeedModifier;

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

		// Bukkit
		movementSpeedModifier = new AttributeModifier("ShopkeepersFreeze", -1.0D, Operation.MULTIPLY_SCALAR_1);
	}

	@Override
	public String getVersionId() {
		return "FailedHandler";
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		// workaround to make mobs stationary:
		AttributeInstance attributeInstance = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
		if (attributeInstance != null) {
			// remove first, in case the entity already has this modifier:
			attributeInstance.removeModifier(movementSpeedModifier);
			attributeInstance.addModifier(movementSpeedModifier);
		}
	}

	@Override
	public boolean supportsCustomMobAI() {
		// not supported, uses the regular mob AI and gravity
		// relies on overwriteLivingEntityAI being able to make the entities stationary
		return false;
	}

	@Override
	public void tickAI(LivingEntity entity) {
		// not supported
	}

	@Override
	public void setOnGround(Entity entity, boolean onGround) {
		try {
			Object mcEntity = obcGetHandleMethod.invoke(entity);
			nmsOnGroundField.set(mcEntity, onGround);
		} catch (Exception e) {
			// ignoring, since this is not that important if it doesn't work
		}
	}

	@Override
	public void setNoclip(Entity entity) {
		try {
			// when gravity gets disabled, we are able to also disable collisions/pushing of mobs via the noclip flag:
			// this might not properly work for Vec, since those disable noclip again after their movement:
			Object mcEntity = obcGetHandleMethod.invoke(entity);
			nmsNoclipField.set(mcEntity, true);
		} catch (Exception e) {
			// this is optional, ignore if not possible for some reason
		}
	}

	@Override
	public boolean matches(ItemStack provided, ItemStack required) {
		if (provided == required) return true;
		// if the required item is empty, then the provided item has to be empty as well:
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
			// fallback: checking for metadata equality
			// note: in this case the behavior of this method is no longer equivalent to minecraft's item comparison
			// behavior!
			return provided.isSimilar(required);
		}
	}

	@Override
	public void updateTrades(Player player) {
	}

	@Override
	public String getItemSNBT(ItemStack itemStack) {
		return null; // not supported
	}

	@Override
	public String getItemTypeTranslationKey(Material material) {
		return null; // not supported
	}
}
