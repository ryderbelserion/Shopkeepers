package com.nisovin.shopkeepers.compat.v1_8_R1;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.craftbukkit.v1_8_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftInventoryMerchant;
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;

import com.nisovin.shopkeepers.TradingRecipe;
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.ItemUtils;

import net.minecraft.server.v1_8_R1.Entity;
import net.minecraft.server.v1_8_R1.EntityHuman;
import net.minecraft.server.v1_8_R1.EntityInsentient;
import net.minecraft.server.v1_8_R1.EntityLiving;
import net.minecraft.server.v1_8_R1.EntityVillager;
import net.minecraft.server.v1_8_R1.GameProfileSerializer;
import net.minecraft.server.v1_8_R1.InventoryMerchant;
import net.minecraft.server.v1_8_R1.MerchantRecipe;
import net.minecraft.server.v1_8_R1.MerchantRecipeList;
import net.minecraft.server.v1_8_R1.NBTBase;
import net.minecraft.server.v1_8_R1.NBTTagCompound;
import net.minecraft.server.v1_8_R1.NBTTagList;
import net.minecraft.server.v1_8_R1.PathfinderGoalFloat;
import net.minecraft.server.v1_8_R1.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_8_R1.PathfinderGoalSelector;
import net.minecraft.server.v1_8_R1.StatisticList;

public final class NMSHandler implements NMSCallProvider {

	@Override
	public String getVersionId() {
		return "1_8_R1";
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean openTradeWindow(String title, List<TradingRecipe> recipes, Player player) {
		try {
			EntityVillager villager = new EntityVillager(((CraftPlayer) player).getHandle().getWorld(), 0);
			// custom name:
			if (title != null && !title.isEmpty()) {
				villager.setCustomName(title);
			}
			// career level (to prevent trade progression):
			Field careerLevelField = EntityVillager.class.getDeclaredField("bw");
			careerLevelField.setAccessible(true);
			careerLevelField.set(villager, 10);

			// recipes:
			Field recipeListField = EntityVillager.class.getDeclaredField("bp");
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
			return new TradingRecipe(resultItem, item1, item2);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			EntityLiving ev = ((CraftLivingEntity) entity).getHandle();
			if (!(ev instanceof EntityInsentient)) return;

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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setEntitySilent(org.bukkit.entity.Entity entity, boolean silent) {
		Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.b(silent);
	}

	@Override
	public void setNoAI(LivingEntity bukkitEntity) {
		net.minecraft.server.v1_8_R1.Entity nmsEntity = ((CraftEntity) bukkitEntity).getHandle();
		NBTTagCompound tag = nmsEntity.getNBTTag();
		if (tag == null) {
			tag = new NBTTagCompound();
		}
		nmsEntity.c(tag);
		tag.setInt("NoAI", 1);
		nmsEntity.f(tag);
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

	private net.minecraft.server.v1_8_R1.ItemStack convertItemStack(org.bukkit.inventory.ItemStack item) {
		if (item == null) return null;
		return org.bukkit.craftbukkit.v1_8_R1.inventory.CraftItemStack.asNMSCopy(item);
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
		net.minecraft.server.v1_8_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
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
		net.minecraft.server.v1_8_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
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
		net.minecraft.server.v1_8_R1.ItemStack nmsProvided = CraftItemStack.asNMSCopy(provided);
		net.minecraft.server.v1_8_R1.ItemStack nmsRequired = CraftItemStack.asNMSCopy(required);
		NBTTagCompound providedTag = nmsProvided.getTag();
		NBTTagCompound requiredTag = nmsRequired.getTag();
		// very early versions of MC 1.8 used the previous item matching of only comparing item types and durability,
		// but later versions of MC 1.8 might already use the new item comparison:
		try {
			Method areNBTMatchingMethod = GameProfileSerializer.class.getDeclaredMethod("a", NBTBase.class, NBTBase.class, boolean.class);
			return (Boolean) areNBTMatchingMethod.invoke(null, requiredTag, providedTag, false);
		} catch (Exception e) {
			// item type and durability have been checked above already:
			return true;
		}
	}
}
