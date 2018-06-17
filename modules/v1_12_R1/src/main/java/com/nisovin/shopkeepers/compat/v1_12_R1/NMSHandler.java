package com.nisovin.shopkeepers.compat.v1_12_R1;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.util.Vector;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.ItemUtils;

import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.EntityInsentient;
import net.minecraft.server.v1_12_R1.EntityLiving;
import net.minecraft.server.v1_12_R1.GameProfileSerializer;
import net.minecraft.server.v1_12_R1.MovingObjectPosition;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import net.minecraft.server.v1_12_R1.PathfinderGoalFloat;
import net.minecraft.server.v1_12_R1.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_12_R1.PathfinderGoalSelector;
import net.minecraft.server.v1_12_R1.Vec3D;

public final class NMSHandler implements NMSCallProvider {

	@Override
	public String getVersionId() {
		return "1_12_R1";
	}

	// TODO this can be moved out of nms handler once we only support 1.11 upwards
	@Override
	public boolean openTradeWindow(String title, List<TradingRecipe> recipes, Player player) {
		// create empty merchant:
		Merchant merchant = Bukkit.createMerchant(title);

		// create list of merchant recipes:
		List<MerchantRecipe> merchantRecipes = new ArrayList<MerchantRecipe>();
		for (TradingRecipe recipe : recipes) {
			// create and add merchant recipe:
			merchantRecipes.add(this.createMerchantRecipe(recipe.getItem1(), recipe.getItem2(), recipe.getResultItem()));
		}

		// set merchant's recipes:
		merchant.setRecipes(merchantRecipes);

		// increase 'talked-to-villager' statistic:
		player.incrementStatistic(Statistic.TALKED_TO_VILLAGER);

		// open merchant:
		return player.openMerchant(merchant, true) != null;
	}

	private MerchantRecipe createMerchantRecipe(ItemStack buyItem1, ItemStack buyItem2, ItemStack sellingItem) {
		assert !ItemUtils.isEmpty(sellingItem) && !ItemUtils.isEmpty(buyItem1);
		MerchantRecipe recipe = new MerchantRecipe(sellingItem, 10000); // no max-uses limit
		recipe.setExperienceReward(false); // no experience rewards
		recipe.addIngredient(buyItem1);
		if (!ItemUtils.isEmpty(buyItem2)) {
			recipe.addIngredient(buyItem2);
		}
		return recipe;
	}

	// TODO this can be moved out of nms handler once we only support 1.11 upwards
	@Override
	public TradingRecipe getUsedTradingRecipe(MerchantInventory merchantInventory) {
		MerchantRecipe merchantRecipe = merchantInventory.getSelectedRecipe();
		List<ItemStack> ingredients = merchantRecipe.getIngredients();
		ItemStack item1 = ingredients.get(0);
		ItemStack item2 = null;
		if (ingredients.size() > 1) {
			ItemStack buyItem2 = ingredients.get(1);
			if (!ItemUtils.isEmpty(buyItem2)) {
				item2 = buyItem2;
			}
		}
		ItemStack resultItem = merchantRecipe.getResult();
		return ShopkeepersAPI.createTradingRecipe(resultItem, item1, item2);
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			// setting the entity non-collidable:
			// TODO this can be moved out of the nms handler once we support 1.9 upwards
			entity.setCollidable(false);

			EntityLiving mcLivingEntity = ((CraftLivingEntity) entity).getHandle();
			// example: armor stands are living, but not insentient
			if (!(mcLivingEntity instanceof EntityInsentient)) return;

			// make goal selector items accessible:
			Field bField = PathfinderGoalSelector.class.getDeclaredField("b");
			bField.setAccessible(true);
			Field cField = PathfinderGoalSelector.class.getDeclaredField("c");
			cField.setAccessible(true);

			// overwrite goal selector:
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(mcLivingEntity);

			// clear old goals:
			Set<?> goals_b = (Set<?>) bField.get(goals);
			goals_b.clear();
			Set<?> goals_c = (Set<?>) cField.get(goals);
			goals_c.clear();

			// add new goals:
			goals.a(0, new PathfinderGoalFloat((EntityInsentient) mcLivingEntity));
			goals.a(1, new PathfinderGoalLookAtPlayer((EntityInsentient) mcLivingEntity, EntityHuman.class, 12.0F, 1.0F));

			// overwrite target selector:
			Field targetsField = EntityInsentient.class.getDeclaredField("targetSelector");
			targetsField.setAccessible(true);
			PathfinderGoalSelector targets = (PathfinderGoalSelector) targetsField.get(mcLivingEntity);

			// clear old target goals:
			Set<?> targets_b = (Set<?>) bField.get(targets);
			targets_b.clear();
			Set<?> targets_c = (Set<?>) cField.get(targets);
			targets_c.clear();
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
		// detects (full) liquid blocks if used with ignoreBlockWithoutBoundingBox=(inLiquid ? false : true)
		// allowing entities to stand on full water blocks
		/*BlockPosition blockPosition = mcEntity.getChunkCoordinates();
		boolean inLiquid = mcEntity.getWorld().getType(blockPosition).getMaterial().isLiquid()
				|| mcEntity.getWorld().getType(blockPosition.down()).getMaterial().isLiquid();*/
		Vec3D startPos = new Vec3D(start.getX(), start.getY(), start.getZ());
		Vec3D endPos = startPos.add(direction.getX(), direction.getY(), direction.getZ()); // creates a new vector
		MovingObjectPosition hitResult = ((CraftWorld) start.getWorld()).getHandle().rayTrace(startPos, endPos, true, true, false);
		if (hitResult == null) return direction.length(); // no collisions within the checked range
		return distance(start, hitResult.pos);
	}

	private double distance(Location from, Vec3D to) {
		double dx = to.x - from.getX();
		double dy = to.y - from.getY();
		double dz = to.z - from.getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public void setOnGround(org.bukkit.entity.Entity entity, boolean onGround) {
		Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.onGround = onGround;
	}

	@Override
	public void setEntitySilent(org.bukkit.entity.Entity entity, boolean silent) {
		entity.setSilent(silent);
	}

	@Override
	public void setNoAI(LivingEntity entity) {
		entity.setAI(false);

		// making sure that Spigot's entity activation range does not keep this entity ticking, because it assumes that
		// it is currently falling:
		this.setOnGround(entity, true);
	}

	@Override
	public void setGravity(org.bukkit.entity.Entity entity, boolean gravity) {
		entity.setGravity(gravity);

		if (!gravity) {
			// making sure that Spigot's entity activation range does not keep this entity ticking, because it assumes
			// that it is currently falling:
			this.setOnGround(entity, true);
		}
	}

	@Override
	public void setNoclip(org.bukkit.entity.Entity entity) {
		// when gravity gets disabled, we are able to also disable collisions/pushing of mobs via the noclip flag:
		// this might not properly work for Vec, since those disable noclip again after their movement:
		Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.noclip = true;
	}

	@Override
	public ItemStack loadItemAttributesFromString(ItemStack item, String data) {
		// since somewhere in late bukkit 1.8, bukkit saves item attributes on its own (inside the internal data)
		// this is currently kept in, in case some old shopkeeper data gets imported, for which attributes weren't yet
		// serialized to the internal data by bukkit
		// TODO remove this in the future
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
				// MC 1.9 addition: not needed, as Slot-serialization wasn't ever published
				/*if (attrData.length >= 7) {
					attr.setString("Slot", attrData[6]);
				}*/
				list.add(attr);
			}
		}
		net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tag = nmsItem.getTag();
		if (tag == null) {
			tag = new NBTTagCompound();
			nmsItem.setTag(tag);
		}
		tag.set("AttributeModifiers", list);
		return CraftItemStack.asBukkitCopy(nmsItem);
	}

	@Override
	public String saveItemAttributesToString(ItemStack item) {
		// since somewhere in late bukkit 1.8, bukkit saves item attributes on its own (inside the internal data)
		return null;
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEvent event) {
		return event.getHand() == EquipmentSlot.HAND;
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEntityEvent event) {
		return event.getHand() == EquipmentSlot.HAND;
	}

	@Override
	public boolean supportsSpawnEggEntityType() {
		return true;
	}

	@Override
	public void setSpawnEggEntityType(ItemStack spawnEggItem, EntityType entityType) {
		assert spawnEggItem != null && spawnEggItem.getType() == org.bukkit.Material.MONSTER_EGG;
		if (entityType == null && !spawnEggItem.hasItemMeta()) return;
		SpawnEggMeta itemMeta = (SpawnEggMeta) spawnEggItem.getItemMeta();
		itemMeta.setSpawnedType(entityType);
		spawnEggItem.setItemMeta(itemMeta);
	}

	@Override
	public EntityType getSpawnEggEntityType(ItemStack spawnEggItem) {
		assert spawnEggItem != null && spawnEggItem.getType() == org.bukkit.Material.MONSTER_EGG;
		if (!spawnEggItem.hasItemMeta()) return null;
		SpawnEggMeta itemMeta = (SpawnEggMeta) spawnEggItem.getItemMeta();
		return itemMeta.getSpawnedType();
	}

	@Override
	public boolean matches(ItemStack provided, ItemStack required) {
		if (provided == required) return true;
		// if the required item is empty, then the provided item has to be empty as well:
		if (ItemUtils.isEmpty(required)) return ItemUtils.isEmpty(provided);
		else if (ItemUtils.isEmpty(provided)) return false;
		if (provided.getType() != required.getType()) return false;
		if (provided.getDurability() != required.getDurability()) return false;
		net.minecraft.server.v1_12_R1.ItemStack nmsProvided = CraftItemStack.asNMSCopy(provided);
		net.minecraft.server.v1_12_R1.ItemStack nmsRequired = CraftItemStack.asNMSCopy(required);
		NBTTagCompound providedTag = nmsProvided.getTag();
		NBTTagCompound requiredTag = nmsRequired.getTag();
		return GameProfileSerializer.a(requiredTag, providedTag, false);
	}
}
