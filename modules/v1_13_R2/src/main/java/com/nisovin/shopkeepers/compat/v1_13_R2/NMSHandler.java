package com.nisovin.shopkeepers.compat.v1_13_R2;

import java.lang.reflect.Field;
import java.util.Set;

import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftMerchant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.ItemUtils;

import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityHuman;
import net.minecraft.server.v1_13_R2.EntityInsentient;
import net.minecraft.server.v1_13_R2.EntityLiving;
import net.minecraft.server.v1_13_R2.GameProfileSerializer;
import net.minecraft.server.v1_13_R2.IMerchant;
import net.minecraft.server.v1_13_R2.MerchantRecipeList;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.PacketDataSerializer;
import net.minecraft.server.v1_13_R2.PacketPlayOutCustomPayload;
import net.minecraft.server.v1_13_R2.PathfinderGoalFloat;
import net.minecraft.server.v1_13_R2.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_13_R2.PathfinderGoalSelector;

public final class NMSHandler implements NMSCallProvider {

	@Override
	public String getVersionId() {
		return "1_13_R2";
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		try {
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
		mcInsentientEntity.goalSelector.doTick();
		mcInsentientEntity.getControllerLook().a();
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
		// when gravity gets disabled, we are able to also disable collisions/pushing of mobs via the noclip flag:
		// this might not properly work for Vec, since those disable noclip again after their movement:
		Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.noclip = true;
	}

	@Override
	public boolean matches(ItemStack provided, ItemStack required) {
		if (provided == required) return true;
		// if the required item is empty, then the provided item has to be empty as well:
		if (ItemUtils.isEmpty(required)) return ItemUtils.isEmpty(provided);
		else if (ItemUtils.isEmpty(provided)) return false;
		if (provided.getType() != required.getType()) return false;
		net.minecraft.server.v1_13_R2.ItemStack nmsProvided = CraftItemStack.asNMSCopy(provided);
		net.minecraft.server.v1_13_R2.ItemStack nmsRequired = CraftItemStack.asNMSCopy(required);
		// this makes sure that we have a 'damage' tag even if the damage is 0, so in case the required item for some
		// reason has a damage tag of 0 the items are still considered equal by the following tag comparison:
		if (ItemUtils.isDamageable(provided.getType())) {
			nmsProvided.setDamage(nmsProvided.getDamage());
		}
		NBTTagCompound providedTag = nmsProvided.getTag();
		NBTTagCompound requiredTag = nmsRequired.getTag();
		return GameProfileSerializer.a(requiredTag, providedTag, false);
	}

	@Override
	public void updateTrades(Player player, Merchant merchant) {
		Inventory inventory = player.getOpenInventory().getTopInventory();
		if (!(inventory instanceof MerchantInventory)) {
			return;
		}

		// MerchantInventory merchantInventory = (MerchantInventory) inventory;
		IMerchant nmsMerchant;
		if (merchant instanceof Villager) {
			nmsMerchant = ((CraftVillager) merchant).getHandle();
		} else {
			nmsMerchant = ((CraftMerchant) merchant).getMerchant();
		}

		MerchantRecipeList newRecipeList = new MerchantRecipeList();
		MerchantRecipeList merchantRecipeList = nmsMerchant.getOffers(((CraftPlayer) player).getHandle());
		if (merchantRecipeList != null) {
			newRecipeList.addAll(merchantRecipeList);
		}

		// insert dummy entries, because reducing the size below the selected index can crash the client:
		// TODO: actually, this might still not be safe since the player can concurrently select another trading
		// recipe..
		// It's left to the caller to ensure that the number of recipes does not get reduced
		/*int selectedRecipeIndex = merchantInventory.getSelectedRecipeIndex();
		if (selectedRecipeIndex > 0) {
			for (int i = merchantRecipeList.size(); i <= selectedRecipeIndex; ++i) {
				net.minecraft.server.v1_13_R2.ItemStack empty = CraftItemStack.asNMSCopy(null);
				newRecipeList.add(new net.minecraft.server.v1_13_R2.MerchantRecipe(empty, empty, empty, 0, 0));
			}
		}*/
		PacketDataSerializer packetdataserializer = new PacketDataSerializer(Unpooled.buffer());
		packetdataserializer.writeInt(((CraftPlayer) player).getHandle().activeContainer.windowId);
		newRecipeList.a(packetdataserializer); // serialize
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutCustomPayload(PacketPlayOutCustomPayload.a, packetdataserializer)); // TRADER_LIST
	}
}
