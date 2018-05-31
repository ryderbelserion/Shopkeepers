package com.nisovin.shopkeepers.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.Bukkit;
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
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.SKTradingRecipe;

public final class FailedHandler implements NMSCallProvider {

	// Minecraft
	private final Class<?> nmsWorldClass;

	private final Class<?> nmsEntityHumanClass;
	private final Class<?> nmsIMerchantClass;
	private final Class<?> nmsEntityVillagerClass;
	private final Constructor<?> nmsEntityVillagerConstructor;
	private final Field nmsRecipeListField;

	private final Method nmsSetTradingPlayerMethod;
	private final Method nmsOpenTradeMethod;

	private final Class<?> nmsEntityClass;
	private final Method nmsGetWorldMethod;
	private final Field nmsNoclipField;
	private final Field nmsOnGroundField;

	private final Class<?> nmsEntityInsentientClass;
	private final Method nmsSetCustomNameMethod;

	private final Class<?> nmsMerchantRecipeListClass;

	private final Class<?> nmsItemStackClass;
	private final Method nmsGetTagMethod;

	private final Class<?> nmsMerchantRecipeClass;
	private final Constructor<?> nmsMerchantRecipeConstructor;
	private final Field nmsMaxUsesField;
	private final Method nmsGetBuyItem1Method;
	private final Method nmsGetBuyItem2Method;
	private final Method nmsGetBuyItem3Method;

	private final Class<?> nmsInventoryMerchantClass;
	private final Method nmsGetRecipeMethod;

	private final Class<?> nmsGameProfileSerializerClass;
	private final Class<?> nmsNBTBaseClass;
	private final Method nmsAreNBTMatchingMethod;

	// CraftBukkit
	private final Class<?> obcCraftInventoryClass;
	private final Method obcGetInventoryMethod;

	private final Class<?> obcCraftItemStackClass;
	private final Method obcAsNMSCopyMethod;
	private final Method obcAsBukkitCopyMethod;

	private final Class<?> obcCraftEntityClass;
	private final Method obcGetHandleMethod;

	// Bukkit
	private final Method bukkitSetAIMethod;
	private final Method bukkitSetCollidableMethod;
	private final Method bukkitSetGravityMethod;
	private final Method bukkitSetSilentMethod;

	private final Class<?> bukkitAttributeClass;
	private final Class<?> bukkitAttributeInstanceClass;
	private final Class<?> bukkitAttributeModifierClass;
	private final Class<?> bukkitOperationClass;
	private final Object bukkitMultiplyScalar1Operation;
	private final Constructor<?> bukkitAttributeModifierConstructor;
	private final Method bukkitGetAttributeMethod;
	private final Method bukkitRemoveModifierMethod;
	private final Method bukkitAddModifierMethod;
	private final Object genericMovementSpeedAttribute;
	private final Object movementSpeedModifier;

	public FailedHandler() throws Exception {
		String versionString = Bukkit.getServer().getClass().getName().replace("org.bukkit.craftbukkit.", "").replace("CraftServer", "");
		String nmsPackageString = "net.minecraft.server." + versionString;
		String bukkitPackageString = "org.bukkit.";
		String obcPackageString = "org.bukkit.craftbukkit." + versionString;

		// Minecraft
		nmsWorldClass = Class.forName(nmsPackageString + "World");

		nmsEntityHumanClass = Class.forName(nmsPackageString + "EntityHuman");
		nmsIMerchantClass = Class.forName(nmsPackageString + "IMerchant");
		nmsEntityVillagerClass = Class.forName(nmsPackageString + "EntityVillager");
		nmsEntityVillagerConstructor = nmsEntityVillagerClass.getConstructor(nmsWorldClass);
		Field nmsRecipeListField = null;
		for (Field field : nmsEntityVillagerClass.getDeclaredFields()) {
			if (field.getType().getName().endsWith("MerchantRecipeList")) {
				nmsRecipeListField = field;
				nmsRecipeListField.setAccessible(true);
				break;
			}
		}
		this.nmsRecipeListField = nmsRecipeListField;
		if (nmsRecipeListField == null) {
			throw new Exception("nmsRecipeListField not found");
		}

		nmsSetTradingPlayerMethod = nmsEntityVillagerClass.getDeclaredMethod("setTradingPlayer", nmsEntityHumanClass);
		nmsSetTradingPlayerMethod.setAccessible(true);
		nmsOpenTradeMethod = nmsEntityHumanClass.getDeclaredMethod("openTrade", nmsIMerchantClass);
		nmsOpenTradeMethod.setAccessible(true);

		nmsEntityInsentientClass = Class.forName(nmsPackageString + "EntityInsentient");
		nmsSetCustomNameMethod = nmsEntityInsentientClass.getMethod("setCustomName", String.class);

		nmsItemStackClass = Class.forName(nmsPackageString + "ItemStack");
		nmsGetTagMethod = nmsItemStackClass.getDeclaredMethod("getTag");

		nmsMerchantRecipeClass = Class.forName(nmsPackageString + "MerchantRecipe");
		nmsMerchantRecipeConstructor = nmsMerchantRecipeClass.getConstructor(nmsItemStackClass, nmsItemStackClass, nmsItemStackClass);
		nmsMaxUsesField = nmsMerchantRecipeClass.getDeclaredField("maxUses");
		nmsMaxUsesField.setAccessible(true);
		nmsGetBuyItem1Method = nmsMerchantRecipeClass.getDeclaredMethod("getBuyItem1");
		nmsGetBuyItem2Method = nmsMerchantRecipeClass.getDeclaredMethod("getBuyItem2");
		nmsGetBuyItem3Method = nmsMerchantRecipeClass.getDeclaredMethod("getBuyItem3");

		nmsInventoryMerchantClass = Class.forName(nmsPackageString + "InventoryMerchant");
		nmsGetRecipeMethod = nmsInventoryMerchantClass.getDeclaredMethod("getRecipe");

		nmsMerchantRecipeListClass = Class.forName(nmsPackageString + "MerchantRecipeList");

		nmsEntityClass = Class.forName(nmsPackageString + "Entity");
		nmsGetWorldMethod = nmsEntityClass.getDeclaredMethod("getWorld");
		nmsNoclipField = nmsEntityClass.getDeclaredField("noclip");
		nmsNoclipField.setAccessible(true);
		nmsOnGroundField = nmsEntityClass.getDeclaredField("onGround");
		nmsOnGroundField.setAccessible(true);

		nmsGameProfileSerializerClass = Class.forName(nmsPackageString + "GameProfileSerializer");
		nmsNBTBaseClass = Class.forName(nmsPackageString + "NBTBase");
		nmsAreNBTMatchingMethod = nmsGameProfileSerializerClass.getDeclaredMethod("a", nmsNBTBaseClass, nmsNBTBaseClass, boolean.class);

		// CraftBukkit
		obcCraftInventoryClass = Class.forName(obcPackageString + "inventory.CraftInventory");
		obcGetInventoryMethod = obcCraftInventoryClass.getDeclaredMethod("getInventory");

		obcCraftItemStackClass = Class.forName(obcPackageString + "inventory.CraftItemStack");
		obcAsNMSCopyMethod = obcCraftItemStackClass.getDeclaredMethod("asNMSCopy", ItemStack.class);
		obcAsBukkitCopyMethod = obcCraftItemStackClass.getDeclaredMethod("asBukkitCopy", nmsItemStackClass);

		obcCraftEntityClass = Class.forName(obcPackageString + "entity.CraftEntity");
		obcGetHandleMethod = obcCraftEntityClass.getDeclaredMethod("getHandle");

		// Bukkit
		bukkitSetAIMethod = LivingEntity.class.getDeclaredMethod("setAI", boolean.class);
		bukkitSetCollidableMethod = LivingEntity.class.getDeclaredMethod("setCollidable", boolean.class);
		bukkitSetGravityMethod = Entity.class.getDeclaredMethod("setGravity", boolean.class);
		bukkitSetSilentMethod = Entity.class.getDeclaredMethod("setSilent", boolean.class);

		bukkitAttributeClass = Class.forName(bukkitPackageString + "attribute.Attribute");
		bukkitAttributeInstanceClass = Class.forName(bukkitPackageString + "attribute.AttributeInstance");
		bukkitAttributeModifierClass = Class.forName(bukkitPackageString + "attribute.AttributeModifier");
		bukkitOperationClass = Class.forName(bukkitPackageString + "attribute.AttributeModifier$Operation");
		bukkitMultiplyScalar1Operation = bukkitOperationClass.getDeclaredField("MULTIPLY_SCALAR_1").get(null);
		bukkitAttributeModifierConstructor = bukkitAttributeModifierClass.getConstructor(String.class, double.class, bukkitOperationClass);
		bukkitGetAttributeMethod = LivingEntity.class.getMethod("getAttribute", bukkitAttributeClass);
		bukkitRemoveModifierMethod = bukkitAttributeInstanceClass.getDeclaredMethod("removeModifier", bukkitAttributeModifierClass);
		bukkitAddModifierMethod = bukkitAttributeInstanceClass.getDeclaredMethod("addModifier", bukkitAttributeModifierClass);
		genericMovementSpeedAttribute = bukkitAttributeClass.getDeclaredField("GENERIC_MOVEMENT_SPEED").get(null);
		movementSpeedModifier = bukkitAttributeModifierConstructor.newInstance("ShopkeepersFreeze", -1.0D, bukkitMultiplyScalar1Operation);
	}

	@Override
	public String getVersionId() {
		return "FailedHandler";
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean openTradeWindow(String title, List<TradingRecipe> recipes, Player player) {
		// TODO replace this with new custom merchant api once only MC 1.11 upwards is supported
		try {
			Object mcPlayer = obcGetHandleMethod.invoke(player);
			Object mcWorld = nmsGetWorldMethod.invoke(mcPlayer);
			Object villager = nmsEntityVillagerConstructor.newInstance(mcWorld);
			if (title != null && !title.isEmpty()) {
				nmsSetCustomNameMethod.invoke(villager, title);
			}

			List<?> recipeList = (List<?>) nmsRecipeListField.get(villager);
			if (recipeList == null) {
				recipeList = (List<?>) nmsMerchantRecipeListClass.newInstance();
				nmsRecipeListField.set(villager, recipeList);
			}
			recipeList.clear();
			for (TradingRecipe recipe : recipes) {
				Object mcRecipe = createMerchantRecipe(recipe.getItem1(), recipe.getItem2(), recipe.getResultItem());
				if (mcRecipe != null) {
					((List) recipeList).add(mcRecipe);
				}
			}

			// set trading player:
			nmsSetTradingPlayerMethod.invoke(villager, mcPlayer);
			// open trade window:
			nmsOpenTradeMethod.invoke(mcPlayer, villager);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public TradingRecipe getUsedTradingRecipe(MerchantInventory merchantInventory) {
		try {
			Object inventoryMerchant = obcGetInventoryMethod.invoke(merchantInventory);
			Object merchantRecipe = nmsGetRecipeMethod.invoke(inventoryMerchant);
			ItemStack item1 = asBukkitCopy(nmsGetBuyItem1Method.invoke(merchantRecipe));
			ItemStack item2 = asBukkitCopy(nmsGetBuyItem2Method.invoke(merchantRecipe));
			ItemStack resultItem = asBukkitCopy(nmsGetBuyItem3Method.invoke(merchantRecipe));
			return new SKTradingRecipe(resultItem, item1, item2);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		// workaround to make mobs stationary:
		try {
			Object attributeInstance = bukkitGetAttributeMethod.invoke(entity, genericMovementSpeedAttribute);
			if (attributeInstance != null) {
				// remove first, in case the entity already has this modifier:
				bukkitRemoveModifierMethod.invoke(attributeInstance, movementSpeedModifier);
				bukkitAddModifierMethod.invoke(attributeInstance, movementSpeedModifier);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// setting the entity non-collidable:
		// TODO this can be moved out of the nms handler once we support 1.9 upwards
		try {
			bukkitSetCollidableMethod.invoke(entity, false);
		} catch (Exception e) {
			// not supported by some bukkit versions (1.8)
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
	public double getCollisionDistance(Location start, Vector direction) {
		// not supported
		return 0.0D;
	}

	@Override
	public void setEntitySilent(Entity entity, boolean silent) {
		try {
			bukkitSetSilentMethod.invoke(entity, silent);
		} catch (Exception e) {
			// ignoring, since this feature is not that important
		}
	}

	@Override
	public void setNoAI(LivingEntity entity) {
		try {
			bukkitSetAIMethod.invoke(entity, false);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		try {
			// making sure that Spigot's entity activation range does not keep this entity ticking, because it assumes
			// that it is currently falling:
			Object mcEntity = obcGetHandleMethod.invoke(entity);
			nmsOnGroundField.set(mcEntity, true);
		} catch (Exception e) {
			// optional, ignore if not possible for some reason
		}
	}

	@Override
	public void setGravity(Entity entity, boolean gravity) {
		try {
			bukkitSetGravityMethod.invoke(entity, gravity);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		if (!gravity) {
			try {
				// making sure that Spigot's entity activation range does not keep this entity ticking, because it
				// assumes that it is currently falling:
				Object mcEntity = obcGetHandleMethod.invoke(entity);
				nmsOnGroundField.set(mcEntity, true);
			} catch (Exception e) {
				// optional, ignore if not possible for some reason
			}
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

	private Object createMerchantRecipe(ItemStack item1, ItemStack item2, ItemStack item3) {
		try {
			Object recipe = nmsMerchantRecipeConstructor.newInstance(convertItemStack(item1), convertItemStack(item2), convertItemStack(item3));
			nmsMaxUsesField.set(recipe, 10000);
			return recipe;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private Object convertItemStack(org.bukkit.inventory.ItemStack item) {
		try {
			return obcAsNMSCopyMethod.invoke(null, item);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private ItemStack asBukkitCopy(Object nmsItem) {
		if (nmsItem == null) return null;
		try {
			return (ItemStack) obcAsBukkitCopyMethod.invoke(null, nmsItem);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ItemStack loadItemAttributesFromString(ItemStack item, String data) {
		return null;
	}

	@Override
	public String saveItemAttributesToString(ItemStack item) {
		return null;
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEvent event) {
		return true;
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEntityEvent event) {
		return true;
	}

	@Override
	public boolean supportsSpawnEggEntityType() {
		// not supported
		return false;
	}

	@Override
	public void setSpawnEggEntityType(ItemStack spawnEggItem, EntityType entityType) {
		// not supported
	}

	@Override
	public EntityType getSpawnEggEntityType(ItemStack spawnEggItem) {
		// not supported
		return null;
	}

	@Override
	public boolean matches(ItemStack provided, ItemStack required) {
		if (provided == required) return true;
		// if the required item is empty, then the provided item has to be empty as well:
		if (ItemUtils.isEmpty(required)) return ItemUtils.isEmpty(provided);
		else if (ItemUtils.isEmpty(provided)) return false;
		if (provided.getType() != required.getType()) return false;
		if (provided.getDurability() != required.getDurability()) return false;
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
}
