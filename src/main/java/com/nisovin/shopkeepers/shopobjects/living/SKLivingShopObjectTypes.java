package com.nisovin.shopkeepers.shopobjects.living;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObjectTypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.types.CatShop;
import com.nisovin.shopkeepers.shopobjects.living.types.CreeperShop;
import com.nisovin.shopkeepers.shopobjects.living.types.PigZombieShop;
import com.nisovin.shopkeepers.shopobjects.living.types.SheepShop;
import com.nisovin.shopkeepers.shopobjects.living.types.VillagerShop;
import com.nisovin.shopkeepers.shopobjects.living.types.ZombieShop;
import com.nisovin.shopkeepers.util.StringUtils;

public class SKLivingShopObjectTypes implements LivingShopObjectTypes {
	/*
	 * Notes about individual differences and issues for specific entity types:
	 * All non-listed entity types are completely untested and therefore 'experimental' as well.
	 * 
	 * <ul>
	 * <li> VILLAGER: okay, default
	 * <li> BAT: experimental: requires NoAI, sleeping by default, but starts flying when 'hit'
	 * <li> BLAZE: experimental: starts flying upwards -> NoAI for now, seems okay
	 * <li> CAVE_SPIDER: okay
	 * <li> CHICKEN: might still lays eggs, seems okay
	 * <li> COW: okay
	 * <li> CREEPER: okay
	 * <li> ENDER_DRAGON: experimental: requires NoAI, shows boss bar, not clickable..
	 * <li> ENDERMAN: experimental: requires NoAI, still teleports away if hit by projectile, starts starring
	 * <li> GHAST: seems okay
	 * <li> GIANT: seems okay
	 * <li> HORSE: experimental: randomly spawning as baby, if not baby and if clicking with empty hand, the player turns into fixed direction (horse direction?)
	 * <li> IRON_GOLEM: okay
	 * <li> MAGMA_CUBE: spawns with random size, weird behavior in water, seems okay
	 * <li> MUSHROOM_COW: okay
	 * <li> OCELOT: okay
	 * <li> PIG: okay
	 * <li> PIG_ZOMBIE: okay, spawns randomly as baby
	 * <li> SHEEP: okay
	 * <li> SILVERFISH: experimental, strange movement when the player is standing behind it -> NoAI for now
	 * <li> SKELETON: okay
	 * <li> SLIME: spawns with random size, okay
	 * <li> SNOWMAN: okay
	 * <li> SPIDER: okay
	 * <li> SQUID: seems okay, slightly weird movement in water
	 * <li> WITCH: okay
	 * <li> WITHER: experimental: requires NoAI, shows boss bar
	 * <li> WOLF: okay
	 * <li> ZOMBIE: okay, spawns randomly as baby
	 * <li> RABBIT: okay
	 * <li> ENDERMITE: seems to work, however it shows strange movement
	 * <li> GUARDIAN: does not work, error when trying to apply common AI goals
	 * <li> ARMOR_STAND: cannot be clicked / accessed yet
	 * <li> SHULKER: seems to work on first glance, though it is rather uninteresting because it stays in closed form
	 * # 1.11
	 * <li> ELDER_GUARDIAN: same issues as guardian
	 * <li> WITHER_SKELETON: okay
	 * <li> STRAY: okay
	 * <li> HUSK: okay, spawns randomly as baby
	 * <li> ZOMBIE_VILLAGER: spawns with random profession, seems okay
	 * <li> SKELETON_HORSE:  same issues as horse
	 * <li> ZOMBIE_HORSE: same issues as horse
	 * <li> DONKEY: same issues as horse
	 * <li> MULE: same issues as horse
	 * <li> EVOKER: okay
	 * <li> VEX: starts gliding into the ground once spawned and occasionally,other than that it seems to work fine
	 * <li> VINDICATOR: okay
	 * <li> LLAMA: same issues as horse
	 * # 1.12
	 * <li> ILLUSIONER: okay
	 * <li> PARROT: okay, dances, spawns with random color
	 * </ul>
	 */

	private static final Map<EntityType, List<String>> ALIASES; // deeply unmodifiable

	private static List<String> prepareAliases(List<String> aliases) {
		return Collections.unmodifiableList(StringUtils.normalize(aliases));
	}

	static {
		Map<EntityType, List<String>> aliases = new HashMap<>();
		aliases.put(EntityType.MUSHROOM_COW, prepareAliases(Arrays.asList("mooshroom")));
		ALIASES = Collections.unmodifiableMap(aliases);
	}

	private final LivingShops livingShops;
	// order is specified by the 'enabled-living-shops' config setting:
	private final Map<EntityType, SKLivingShopObjectType<?>> objectTypes = new LinkedHashMap<>();
	private final Collection<SKLivingShopObjectType<?>> objectTypesView = Collections.unmodifiableCollection(objectTypes.values());

	public SKLivingShopObjectTypes(LivingShops livingShops) {
		this.livingShops = livingShops;
		// first, create the enabled living object types, in the same order as specified in the config:
		for (String entityTypeId : Settings.enabledLivingShops) {
			EntityType entityType = Settings.matchEntityType(entityTypeId);
			if (entityType != null && entityType.isAlive() && entityType.isSpawnable() && !objectTypes.containsKey(entityType)) {
				// not using aliases (yet?)
				objectTypes.put(entityType, this.createLivingEntityObjectType(entityType, getAliases(entityType)));
			}
		}

		// register object types for all other remaining living entity types:
		for (EntityType entityType : EntityType.values()) {
			if (entityType.isAlive() && entityType.isSpawnable() && !objectTypes.containsKey(entityType)) {
				// not using aliases (yet?)
				objectTypes.put(entityType, this.createLivingEntityObjectType(entityType, getAliases(entityType)));
			}
		}
	}

	@Override
	public List<String> getAliases(EntityType entityType) {
		List<String> aliases = ALIASES.get(entityType);
		return aliases != null ? aliases : Collections.emptyList();
	}

	@Override
	public Collection<SKLivingShopObjectType<?>> getAll() {
		return objectTypesView;
	}

	@Override
	public SKLivingShopObjectType<?> get(EntityType entityType) {
		return objectTypes.get(entityType);
	}

	private SKLivingShopObjectType<?> createLivingEntityObjectType(EntityType entityType, List<String> aliases) {
		// this determines the permission name, the actual used type name will be further normalized however:
		String typeName = entityType.name().toLowerCase(Locale.ROOT);
		String permission = "shopkeeper.entity." + typeName;

		SKLivingShopObjectType<?> objectType;

		switch (entityType) {
		case VILLAGER:
			objectType = new SKLivingShopObjectType<VillagerShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public VillagerShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new VillagerShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case CREEPER:
			objectType = new SKLivingShopObjectType<CreeperShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public CreeperShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new CreeperShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case OCELOT:
			objectType = new SKLivingShopObjectType<CatShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public CatShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new CatShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case SHEEP:
			objectType = new SKLivingShopObjectType<SheepShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public SheepShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new SheepShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case ZOMBIE:
			objectType = new SKLivingShopObjectType<ZombieShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public ZombieShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new ZombieShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case PIG_ZOMBIE:
			objectType = new SKLivingShopObjectType<PigZombieShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public PigZombieShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new PigZombieShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		default:
			objectType = new SKLivingShopObjectType<SKLivingShopObject>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public SKLivingShopObject createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new SKLivingShopObject(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		}

		assert objectType != null;
		return objectType;
	}
}
