package com.nisovin.shopkeepers.shopobjects.living;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Llama;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.Zombie;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObjectTypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.types.BabyableShop;
import com.nisovin.shopkeepers.shopobjects.living.types.CatShop;
import com.nisovin.shopkeepers.shopobjects.living.types.ChestedHorseShop;
import com.nisovin.shopkeepers.shopobjects.living.types.CreeperShop;
import com.nisovin.shopkeepers.shopobjects.living.types.FoxShop;
import com.nisovin.shopkeepers.shopobjects.living.types.HorseShop;
import com.nisovin.shopkeepers.shopobjects.living.types.LlamaShop;
import com.nisovin.shopkeepers.shopobjects.living.types.MooshroomShop;
import com.nisovin.shopkeepers.shopobjects.living.types.PandaShop;
import com.nisovin.shopkeepers.shopobjects.living.types.ParrotShop;
import com.nisovin.shopkeepers.shopobjects.living.types.PigShop;
import com.nisovin.shopkeepers.shopobjects.living.types.RabbitShop;
import com.nisovin.shopkeepers.shopobjects.living.types.SheepShop;
import com.nisovin.shopkeepers.shopobjects.living.types.VillagerShop;
import com.nisovin.shopkeepers.shopobjects.living.types.WolfShop;
import com.nisovin.shopkeepers.shopobjects.living.types.ZombieShop;
import com.nisovin.shopkeepers.shopobjects.living.types.ZombieVillagerShop;
import com.nisovin.shopkeepers.util.StringUtils;

public class SKLivingShopObjectTypes implements LivingShopObjectTypes {
	/*
	 * Notes about individual differences and issues for specific entity types:
	 * All non-listed entity types are completely untested and therefore 'experimental' as well.
	 * 
	 * <ul>
	 * <li> VILLAGER: okay, default, MC 1.14: shake their head when clicked (TODO might be a upstream bug)
	 * <li> BAT: experimental: requires NoAI, sleeping by default, but starts flying when 'hit'
	 * <li> BLAZE: experimental: starts flying upwards -> NoAI for now, seems okay
	 * <li> CAVE_SPIDER: okay
	 * <li> CHICKEN: might still lays eggs (TODO re-check: this might no longer be the case), seems okay
	 * <li> COW: okay
	 * <li> CREEPER: okay
	 * <li> ENDER_DRAGON: experimental: requires NoAI, shows boss bar, not clickable..
	 * <li> ENDERMAN: experimental: requires NoAI, still teleports away if hit by projectile, starts starring
	 * <li> GHAST: seems okay
	 * <li> GIANT: seems okay
	 * <li> IRON_GOLEM: okay
	 * <li> MAGMA_CUBE: spawns with random size, weird behavior in water, seems okay
	 * <li> MUSHROOM_COW: okay
	 * <li> OCELOT: okay
	 * <li> PIG: okay
	 * <li> PIG_ZOMBIE: okay; replaced by ZOMBIFIED_PIGLIN in MC 1.16
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
	 * <li> RABBIT: okay; the killer rabbit type requires some special handling because Minecraft resets the pathfinder goals and applies a custom name if the entity doesn't have one already
	 * <li> ENDERMITE: seems to work, however it shows strange movement
	 * <li> GUARDIAN: does not work, error when trying to apply common AI goals
	 * <li> ARMOR_STAND: cannot be clicked / accessed yet
	 * <li> SHULKER: seems to work on first glance, though it is rather uninteresting because it stays in closed form
	 * <li> HORSE: okay
	 * <li> MULE: okay
	 * <li> DONKEY: okay
	 * <li> SKELETON_HORSE: okay
	 * <li> ZOMBIE_HORSE: okay
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
	 * <li> VEX: starts gliding into the ground once spawned and occasionally, other than that it seems to work fine
	 * <li> VINDICATOR: okay
	 * <li> LLAMA: okay
	 * # 1.12
	 * <li> ILLUSIONER: okay
	 * <li> PARROT: okay, dances, spawns with random color
	 * # 1.13
	 * <li> TURTLE: okay
	 * <li> PHANTOM: okay, burns in sun
	 * <li> COD: okay
	 * <li> SALMON: okay
	 * <li> PUFFERFISH: okay
	 * <li> TROPICAL_FISH: okay
	 * <li> DROWNED: okay
	 * <li> DOLPHIN: okay, slightly gliding inside water
	 * # 1.14
	 * <li> CAT: okay
	 * <li> PANDA: okay
	 * <li> PILLAGER: okay
	 * <li> RAVAGER: okay
	 * <li> TRADER_LLAMA: okay
	 * <li> WANDERING_VILLAGER: okay
	 * <li> FOX: okay, randomly spawns with an item in its mouth (gets cleared)
	 * # 1.15
	 * <li> BEE: okay, turning towards nearby players is jerky (body rotation instead of head rotation), occasionally starts flapping its wings
	 * # 1.16
	 * <li> ZOMBIFIED_PIGLIN: okay, replaces PIG_ZOMBIE
	 * <li> PIGLIN: okay, spawns with random gear (gets cleared), TODO add baby property
	 * <li> HOGLIN: okay
	 * <li> ZOGLIN: okay, TODO add baby property
	 * <li> STRIDER: okay, shakes outside the nether, randomly spawns with passenger (gets cleared), randomly spawns with saddle (TODO)
	 * </ul>
	 */

	private static final Map<EntityType, List<String>> ALIASES; // deeply unmodifiable

	private static List<String> prepareAliases(List<String> aliases) {
		return Collections.unmodifiableList(StringUtils.normalize(aliases));
	}

	static {
		Map<EntityType, List<String>> aliases = new HashMap<>();
		aliases.put(EntityType.MUSHROOM_COW, prepareAliases(Arrays.asList("mooshroom", "mooshroom-cow", "mushroom")));
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
				objectTypes.put(entityType, this.createLivingEntityObjectType(entityType, this.getAliases(entityType)));
			}
		}

		// register object types for all other remaining living entity types:
		for (EntityType entityType : EntityType.values()) {
			if (entityType.isAlive() && entityType.isSpawnable() && !objectTypes.containsKey(entityType)) {
				// not using aliases (yet?)
				objectTypes.put(entityType, this.createLivingEntityObjectType(entityType, this.getAliases(entityType)));
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
		case PIG:
			objectType = new SKLivingShopObjectType<PigShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public PigShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new PigShop(livingShops, this, shopkeeper, creationData);
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
		case CAT:
			objectType = new SKLivingShopObjectType<CatShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public CatShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new CatShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case RABBIT:
			objectType = new SKLivingShopObjectType<RabbitShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public RabbitShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new RabbitShop(livingShops, this, shopkeeper, creationData);
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
			objectType = new SKLivingShopObjectType<ZombieShop<Zombie>>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public ZombieShop<Zombie> createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new ZombieShop<>(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case ZOMBIE_VILLAGER:
			objectType = new SKLivingShopObjectType<ZombieVillagerShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public ZombieVillagerShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new ZombieVillagerShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case FOX:
			objectType = new SKLivingShopObjectType<FoxShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public FoxShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new FoxShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case PARROT:
			objectType = new SKLivingShopObjectType<ParrotShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public ParrotShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new ParrotShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case WOLF:
			objectType = new SKLivingShopObjectType<WolfShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public WolfShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new WolfShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case HORSE:
			objectType = new SKLivingShopObjectType<HorseShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public HorseShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new HorseShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case LLAMA:
			objectType = new SKLivingShopObjectType<LlamaShop<Llama>>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public LlamaShop<Llama> createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new LlamaShop<>(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case TRADER_LLAMA:
			objectType = new SKLivingShopObjectType<LlamaShop<TraderLlama>>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public LlamaShop<TraderLlama> createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new LlamaShop<>(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case PANDA:
			objectType = new SKLivingShopObjectType<PandaShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public PandaShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new PandaShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		case MUSHROOM_COW:
			objectType = new SKLivingShopObjectType<MooshroomShop>(livingShops, entityType, aliases, typeName, permission) {
				@Override
				public MooshroomShop createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
					return new MooshroomShop(livingShops, this, shopkeeper, creationData);
				}
			};
			break;
		default:
			Class<? extends Entity> entityClass = entityType.getEntityClass();
			if (ChestedHorse.class.isAssignableFrom(entityType.getEntityClass())) {
				objectType = new SKLivingShopObjectType<ChestedHorseShop<ChestedHorse>>(livingShops, entityType, aliases, typeName, permission) {
					@Override
					public ChestedHorseShop<ChestedHorse> createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
						return new ChestedHorseShop<>(livingShops, this, shopkeeper, creationData);
					}
				};
			} else if (Zombie.class.isAssignableFrom(entityClass)) {
				objectType = new SKLivingShopObjectType<ZombieShop<Zombie>>(livingShops, entityType, aliases, typeName, permission) {
					@Override
					public ZombieShop<Zombie> createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
						return new ZombieShop<Zombie>(livingShops, this, shopkeeper, creationData);
					}
				};
			} else if (Ageable.class.isAssignableFrom(entityType.getEntityClass()) && entityType != EntityType.WANDERING_TRADER) {
				// baby state is not supported for the wandering trader:
				objectType = new SKLivingShopObjectType<BabyableShop<Ageable>>(livingShops, entityType, aliases, typeName, permission) {
					@Override
					public BabyableShop<Ageable> createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
						return new BabyableShop<Ageable>(livingShops, this, shopkeeper, creationData);
					}
				};
			} else {
				objectType = new SKLivingShopObjectType<SKLivingShopObject<?>>(livingShops, entityType, aliases, typeName, permission) {
					@Override
					public SKLivingShopObject<?> createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
						return new SKLivingShopObject<>(livingShops, this, shopkeeper, creationData);
					}
				};
			}
			break;
		}
		assert objectType != null;
		return objectType;
	}
}
