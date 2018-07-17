package com.nisovin.shopkeepers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.material.MaterialData;

public class LegacyConversion {

	private LegacyConversion() {
	}

	private static final Map<EntityType, Material> SPAWN_EGGS = new LinkedHashMap<>();

	static {
		// note: only includes pre 1.13 entities
		SPAWN_EGGS.put(EntityType.BAT, Material.BAT_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.BLAZE, Material.BLAZE_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.CAVE_SPIDER, Material.CAVE_SPIDER_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.CHICKEN, Material.CHICKEN_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.COW, Material.COW_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.CREEPER, Material.CREEPER_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.DONKEY, Material.DONKEY_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.ELDER_GUARDIAN, Material.ELDER_GUARDIAN_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.ENDERMAN, Material.ENDERMAN_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.ENDERMITE, Material.ENDERMITE_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.EVOKER, Material.EVOKER_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.GHAST, Material.GHAST_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.GUARDIAN, Material.GUARDIAN_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.HORSE, Material.HORSE_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.HUSK, Material.HUSK_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.LLAMA, Material.LLAMA_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.MAGMA_CUBE, Material.MAGMA_CUBE_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.MUSHROOM_COW, Material.MOOSHROOM_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.MULE, Material.MULE_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.OCELOT, Material.OCELOT_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.PARROT, Material.PARROT_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.PIG, Material.PIG_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.POLAR_BEAR, Material.POLAR_BEAR_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.RABBIT, Material.RABBIT_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.SHEEP, Material.SHEEP_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.SHULKER, Material.SHULKER_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.SILVERFISH, Material.SILVERFISH_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.SKELETON_HORSE, Material.SKELETON_HORSE_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.SKELETON, Material.SKELETON_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.SLIME, Material.SLIME_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.SPIDER, Material.SPIDER_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.SQUID, Material.SQUID_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.STRAY, Material.STRAY_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.VEX, Material.VEX_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.VILLAGER, Material.VILLAGER_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.VINDICATOR, Material.VINDICATOR_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.WITCH, Material.WITCH_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.WITHER_SKELETON, Material.WITHER_SKELETON_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.WOLF, Material.WOLF_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.ZOMBIE_HORSE, Material.ZOMBIE_HORSE_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.PIG_ZOMBIE, Material.ZOMBIE_PIGMAN_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.ZOMBIE, Material.ZOMBIE_SPAWN_EGG);
		SPAWN_EGGS.put(EntityType.ZOMBIE_VILLAGER, Material.ZOMBIE_VILLAGER_SPAWN_EGG);
	}

	public static Material fromLegacySpawnEgg(EntityType entityType) {
		return SPAWN_EGGS.get(entityType);
	}

	public static Material fromLegacy(Material material, byte data) {
		return Bukkit.getUnsafe().fromLegacy(new MaterialData(material, data));
	}
}
