package com.nisovin.shopkeepers.compat.v1_8_R3;

import java.lang.reflect.Field;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventoryMerchant;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.util.Vector;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.ItemUtils;

import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityVillager;
import net.minecraft.server.v1_8_R3.GameProfileSerializer;
import net.minecraft.server.v1_8_R3.InventoryMerchant;
import net.minecraft.server.v1_8_R3.MerchantRecipe;
import net.minecraft.server.v1_8_R3.MerchantRecipeList;
import net.minecraft.server.v1_8_R3.MovingObjectPosition;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.PathfinderGoalFloat;
import net.minecraft.server.v1_8_R3.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_8_R3.PathfinderGoalSelector;
import net.minecraft.server.v1_8_R3.StatisticList;
import net.minecraft.server.v1_8_R3.Vec3D;

public final class NMSHandler implements NMSCallProvider {

	@Override
	public String getVersionId() {
		return "1_8_R3";
	}

	@Override
	public boolean openTradeWindow(String title, List<TradingRecipe> recipes, Player player) {
		try {
			EntityVillager villager = new EntityVillager(((CraftPlayer) player).getHandle().world, 0);
			// custom name:
			if (title != null && !title.isEmpty()) {
				villager.setCustomName(title);
			}
			// career level (to prevent trade progression):
			Field careerLevelField = EntityVillager.class.getDeclaredField("by");
			careerLevelField.setAccessible(true);
			careerLevelField.set(villager, 10);

			// recipes:
			Field recipeListField = EntityVillager.class.getDeclaredField("br");
			recipeListField.setAccessible(true);
			MerchantRecipeList recipeList = (MerchantRecipeList) recipeListField.get(villager);
			if (recipeList == null) {
				recipeList = new MerchantRecipeList();
				recipeListField.set(villager, recipeList);
			}
			recipeList.clear();
			for (TradingRecipe recipe : recipes) {
				recipeList.add(createMerchantRecipe(recipe.getItem1(), recipe.getItem2(), recipe.getResultItem()));
			}

			// set trading player:
			villager.a_(((CraftPlayer) player).getHandle());
			// open trade window:
			((CraftPlayer) player).getHandle().openTrade(villager);
			// minecraft statistics:
			((CraftPlayer) player).getHandle().b(StatisticList.F);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public TradingRecipe getUsedTradingRecipe(MerchantInventory merchantInventory) {
		try {
			InventoryMerchant handle = (InventoryMerchant) ((CraftInventoryMerchant) merchantInventory).getInventory();
			MerchantRecipe merchantRecipe = handle.getRecipe();
			ItemStack item1 = merchantRecipe.getBuyItem1() != null ? CraftItemStack.asBukkitCopy(merchantRecipe.getBuyItem1()) : null;
			ItemStack item2 = merchantRecipe.getBuyItem2() != null ? CraftItemStack.asBukkitCopy(merchantRecipe.getBuyItem2()) : null;
			ItemStack resultItem = merchantRecipe.getBuyItem3() != null ? CraftItemStack.asBukkitCopy(merchantRecipe.getBuyItem3()) : null;
			return ShopkeepersAPI.createTradingRecipe(resultItem, item1, item2);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			EntityLiving ev = ((CraftLivingEntity) entity).getHandle();
			if (!(ev instanceof EntityInsentient)) return;

			// overwrite goal selector:
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);

			Field listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			List<?> list = (List<?>) listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("c");
			listField.setAccessible(true);
			list = (List<?>) listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat((EntityInsentient) ev));
			goals.a(1, new PathfinderGoalLookAtPlayer((EntityInsentient) ev, EntityHuman.class, 12.0F, 1.0F));

			// overwrite target selector:
			Field targetsField = EntityInsentient.class.getDeclaredField("targetSelector");
			targetsField.setAccessible(true);
			PathfinderGoalSelector targets = (PathfinderGoalSelector) targetsField.get(ev);

			Field listField2 = PathfinderGoalSelector.class.getDeclaredField("b");
			listField2.setAccessible(true);
			List<?> list2 = (List<?>) listField.get(targets);
			list2.clear();
			listField2 = PathfinderGoalSelector.class.getDeclaredField("c");
			listField2.setAccessible(true);
			list2 = (List<?>) listField.get(targets);
			list2.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void tickAI(LivingEntity entity) {
		EntityLiving mcLivingEntity = ((CraftLivingEntity) entity).getHandle();
		// example: armor stands are living, but not insentient
		if (!(mcLivingEntity instanceof EntityInsentient)) return;
		EntityInsentient mcInsentientEntity = ((EntityInsentient) mcLivingEntity);
		mcInsentientEntity.goalSelector.a();
		mcInsentientEntity.getControllerLook().a();
	}

	@Override
	public double getCollisionDistance(Location start, Vector direction) {
		// rayTrace parameters: (Vec3d start, Vec3d end, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox,
		// boolean returnLastUncollidableBlock)
		Vec3D startPos = new Vec3D(start.getX(), start.getY(), start.getZ());
		Vec3D endPos = startPos.add(direction.getX(), direction.getY(), direction.getZ()); // creates a new vector
		MovingObjectPosition hitResult = ((CraftWorld) start.getWorld()).getHandle().rayTrace(startPos, endPos, true, true, false);
		if (hitResult == null) return direction.length(); // no collisions within the checked range
		return distance(start, hitResult.pos);
	}

	private double distance(Location from, Vec3D to) {
		double dx = to.a - from.getX();
		double dy = to.b - from.getY();
		double dz = to.c - from.getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public void setOnGround(org.bukkit.entity.Entity entity, boolean onGround) {
		Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.onGround = onGround;
	}

	@Override
	public void setEntitySilent(org.bukkit.entity.Entity entity, boolean silent) {
		Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.b(silent);
	}

	@Override
	public void setNoAI(LivingEntity entity) {
		EntityLiving mcLivingEntity = ((CraftLivingEntity) entity).getHandle();
		NBTTagCompound tag = mcLivingEntity.getNBTTag();
		if (tag == null) {
			tag = new NBTTagCompound();
		}
		mcLivingEntity.c(tag);
		tag.setInt("NoAI", 1);
		mcLivingEntity.f(tag);

		// making sure that Spigot's entity activation range does not keep this entity ticking, because it assumes that
		// it is currently falling:
		this.setOnGround(entity, true);
	}

	@Override
	public void setGravity(org.bukkit.entity.Entity entity, boolean gravity) {
		// not supported
	}

	@Override
	public void setNoclip(org.bukkit.entity.Entity entity) {
		// not supported
	}

	private MerchantRecipe createMerchantRecipe(org.bukkit.inventory.ItemStack item1, org.bukkit.inventory.ItemStack item2, org.bukkit.inventory.ItemStack item3) {
		MerchantRecipe recipe = new MerchantRecipe(convertItemStack(item1), convertItemStack(item2), convertItemStack(item3));
		try {
			// max uses:
			Field maxUsesField = MerchantRecipe.class.getDeclaredField("maxUses");
			maxUsesField.setAccessible(true);
			maxUsesField.set(recipe, 10000);

			// reward exp:
			Field rewardExpField = MerchantRecipe.class.getDeclaredField("rewardExp");
			rewardExpField.setAccessible(true);
			rewardExpField.set(recipe, false);
		} catch (Exception e) {
		}
		return recipe;
	}

	private net.minecraft.server.v1_8_R3.ItemStack convertItemStack(org.bukkit.inventory.ItemStack item) {
		if (item == null) return null;
		return org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack.asNMSCopy(item);
	}

	@Override
	public org.bukkit.inventory.ItemStack loadItemAttributesFromString(org.bukkit.inventory.ItemStack item, String data) {
		NBTTagList list = new NBTTagList();
		String[] attrs = data.split(";");
		for (String s : attrs) {
			if (!s.isEmpty()) {
				String[] attrData = s.split(",");
				NBTTagCompound attr = new NBTTagCompound();
				attr.setString("Name", attrData[0]);
				attr.setString("AttributeName", attrData[1]);
				attr.setDouble("Amount", Double.parseDouble(attrData[2]));
				attr.setInt("Operation", Integer.parseInt(attrData[3]));
				attr.setLong("UUIDLeast", Long.parseLong(attrData[4]));
				attr.setLong("UUIDMost", Long.parseLong(attrData[5]));
				list.add(attr);
			}
		}
		net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tag = nmsItem.getTag();
		if (tag == null) {
			tag = new NBTTagCompound();
			nmsItem.setTag(tag);
		}
		tag.set("AttributeModifiers", list);
		return CraftItemStack.asBukkitCopy(nmsItem);
	}

	@Override
	public String saveItemAttributesToString(org.bukkit.inventory.ItemStack item) {
		net.minecraft.server.v1_8_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		if (nmsItem == null) return null;
		NBTTagCompound tag = nmsItem.getTag();
		if (tag == null || !tag.hasKey("AttributeModifiers")) {
			return null;
		}
		String data = "";
		NBTTagList list = tag.getList("AttributeModifiers", 10);
		for (int i = 0; i < list.size(); i++) {
			NBTTagCompound attr = list.get(i);
			data += attr.getString("Name") + ","
					+ attr.getString("AttributeName") + ","
					+ attr.getDouble("Amount") + ","
					+ attr.getInt("Operation") + ","
					+ attr.getLong("UUIDLeast") + ","
					+ attr.getLong("UUIDMost") + ";";
		}
		return data;
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
		net.minecraft.server.v1_8_R3.ItemStack nmsProvided = CraftItemStack.asNMSCopy(provided);
		net.minecraft.server.v1_8_R3.ItemStack nmsRequired = CraftItemStack.asNMSCopy(required);
		NBTTagCompound providedTag = nmsProvided.getTag();
		NBTTagCompound requiredTag = nmsRequired.getTag();
		return GameProfileSerializer.a(requiredTag, providedTag, false);
	}
}
