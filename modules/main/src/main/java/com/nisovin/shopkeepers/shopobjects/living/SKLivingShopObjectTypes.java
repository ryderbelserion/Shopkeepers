package com.nisovin.shopkeepers.shopobjects.living;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObjectTypes;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.shopobjects.living.types.AxolotlShop;
import com.nisovin.shopkeepers.shopobjects.living.types.BabyableShop;
import com.nisovin.shopkeepers.shopobjects.living.types.CatShop;
import com.nisovin.shopkeepers.shopobjects.living.types.ChestedHorseShop;
import com.nisovin.shopkeepers.shopobjects.living.types.CreeperShop;
import com.nisovin.shopkeepers.shopobjects.living.types.FoxShop;
import com.nisovin.shopkeepers.shopobjects.living.types.FrogShop;
import com.nisovin.shopkeepers.shopobjects.living.types.GlowSquidShop;
import com.nisovin.shopkeepers.shopobjects.living.types.GoatShop;
import com.nisovin.shopkeepers.shopobjects.living.types.HorseShop;
import com.nisovin.shopkeepers.shopobjects.living.types.LlamaShop;
import com.nisovin.shopkeepers.shopobjects.living.types.MagmaCubeShop;
import com.nisovin.shopkeepers.shopobjects.living.types.MooshroomShop;
import com.nisovin.shopkeepers.shopobjects.living.types.PandaShop;
import com.nisovin.shopkeepers.shopobjects.living.types.ParrotShop;
import com.nisovin.shopkeepers.shopobjects.living.types.PigShop;
import com.nisovin.shopkeepers.shopobjects.living.types.PufferFishShop;
import com.nisovin.shopkeepers.shopobjects.living.types.RabbitShop;
import com.nisovin.shopkeepers.shopobjects.living.types.SheepShop;
import com.nisovin.shopkeepers.shopobjects.living.types.ShulkerShop;
import com.nisovin.shopkeepers.shopobjects.living.types.SlimeShop;
import com.nisovin.shopkeepers.shopobjects.living.types.SnowmanShop;
import com.nisovin.shopkeepers.shopobjects.living.types.TropicalFishShop;
import com.nisovin.shopkeepers.shopobjects.living.types.VillagerShop;
import com.nisovin.shopkeepers.shopobjects.living.types.WanderingTraderShop;
import com.nisovin.shopkeepers.shopobjects.living.types.WolfShop;
import com.nisovin.shopkeepers.shopobjects.living.types.ZombieShop;
import com.nisovin.shopkeepers.shopobjects.living.types.ZombieVillagerShop;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.java.ClassUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Notes about individual differences and issues for specific entity types: All non-listed entity
 * types are completely untested and therefore 'experimental' as well.
 * <ul>
 * <li>VILLAGER: okay, default, MC 1.14: shake their head when clicked (TODO might be an upstream
 * bug)
 * <li>BAT: experimental: requires NoAI, sleeping by default, but starts flying when 'hit'
 * <li>BLAZE: experimental: starts flying upwards -> requires NoAI, seems okay
 * <li>CAVE_SPIDER: okay
 * <li>CHICKEN: might still lays eggs (TODO re-check: this might no longer be the case), seems okay
 * <li>COW: okay
 * <li>CREEPER: okay
 * <li>ENDER_DRAGON: experimental: requires NoAI, plays no animation without AI (client-sided),
 * shows boss bar on older versions, not clickable! (body parts are server-sided, so the client
 * reports no interactions with them).
 * <li>ENDERMAN: experimental: requires NoAI, still teleports away if hit by projectile, starts
 * starring
 * <li>GHAST: seems okay
 * <li>GIANT: seems okay
 * <li>IRON_GOLEM: okay
 * <li>MAGMA_CUBE: okay, would usually spawn with random size, weird behavior in water (no longer
 * the case, maybe due to using NoAI)
 * <li>MUSHROOM_COW: okay
 * <li>OCELOT: okay
 * <li>PIG: okay
 * <li>PIG_ZOMBIE: okay; replaced by ZOMBIFIED_PIGLIN in MC 1.16
 * <li>SHEEP: okay
 * <li>SILVERFISH: experimental, strange movement when the player is standing behind it -> requires
 * NoAI
 * <li>SKELETON: okay
 * <li>SLIME: okay, would usually spawn with random size
 * <li>SNOWMAN: okay
 * <li>SPIDER: okay
 * <li>SQUID: seems okay, slightly weird movement in water
 * <li>WITCH: okay
 * <li>WITHER: experimental: requires NoAI, shows boss bar
 * <li>WOLF: okay
 * <li>ZOMBIE: okay, spawns randomly as baby
 * <li>RABBIT: okay; the killer rabbit type requires some special handling because Minecraft resets
 * the pathfinder goals and applies a custom name if the entity doesn't have one already
 * <li>ENDERMITE: seems to work, however it shows strange movement
 * <li>GUARDIAN: does not work, error when trying to apply common AI goals
 * <li>ARMOR_STAND: cannot be clicked / accessed yet
 * <li>SHULKER: okay, dynamically attaches to another adjacent block when the block they are
 * attached to is broken TODO peek state
 * <li>HORSE: okay
 * <li>MULE: okay
 * <li>DONKEY: okay
 * <li>SKELETON_HORSE: okay
 * <li>ZOMBIE_HORSE: okay # 1.11
 * <li>ELDER_GUARDIAN: same issues as guardian
 * <li>WITHER_SKELETON: okay
 * <li>STRAY: okay
 * <li>HUSK: okay, spawns randomly as baby
 * <li>ZOMBIE_VILLAGER: spawns with random profession, seems okay
 * <li>SKELETON_HORSE: same issues as horse
 * <li>ZOMBIE_HORSE: same issues as horse
 * <li>DONKEY: same issues as horse
 * <li>MULE: same issues as horse
 * <li>EVOKER: okay
 * <li>VEX: starts gliding into the ground once spawned and occasionally, other than that it seems
 * to work fine
 * <li>VINDICATOR: okay
 * <li>LLAMA: okay # 1.12
 * <li>ILLUSIONER: okay
 * <li>PARROT: okay, dances, spawns with random color # 1.13
 * <li>TURTLE: okay
 * <li>PHANTOM: burns in sun, can be pushed around (probably client-sided) before it teleports back,
 * does not rotate towards nearby players
 * <li>COD: okay
 * <li>SALMON: okay
 * <li>PUFFERFISH: okay
 * <li>TROPICAL_FISH: okay
 * <li>DROWNED: okay
 * <li>DOLPHIN: okay, slightly gliding inside water # 1.14
 * <li>CAT: okay
 * <li>PANDA: okay
 * <li>PILLAGER: okay
 * <li>RAVAGER: okay
 * <li>TRADER_LLAMA: okay
 * <li>WANDERING_VILLAGER: okay
 * <li>FOX: okay, randomly spawns with an item in its mouth (gets cleared) # 1.15
 * <li>BEE: okay, turning towards nearby players is jerky (body rotation instead of head rotation),
 * occasionally starts flapping its wings # 1.16
 * <li>ZOMBIFIED_PIGLIN: okay, replaces PIG_ZOMBIE
 * <li>PIGLIN: okay, spawns with random gear (gets cleared), TODO add baby property
 * <li>HOGLIN: okay
 * <li>ZOGLIN: okay, TODO add baby property
 * <li>STRIDER: okay, shakes outside the nether, randomly spawns with passenger (gets cleared),
 * randomly spawns with saddle (gets cleared), TODO saddle property, shivering property (may require
 * continuously updating the entity state) # 1.16.2
 * <li>PIGLIN_BRUTE: okay, TODO add baby property # 1.17
 * <li>AXOLOTL: okay, spawns with random variant in vanilla
 * <li>GLOW_SQUID: okay
 * <li>GOAT: okay, randomly spawns as screaming variant in vanilla
 * <li>ALLAY: okay
 * <li>FROG: okay, starts running animation when touched, has no baby variant
 * <li>TADPOLE: okay
 * <li>WARDEN: okay
 * </ul>
 **/
public final class SKLivingShopObjectTypes implements LivingShopObjectTypes {

	// IDENTIFIERS

	private static String getIdentifier(EntityType entityType) {
		assert entityType != null;
		return StringUtils.normalize(entityType.name());
	}

	// ALIASES

	// Deeply unmodifiable:
	private static final Map<? extends @NonNull EntityType, ? extends @NonNull List<? extends @NonNull String>> ALIASES;

	static {
		Map<@NonNull EntityType, @NonNull List<? extends @NonNull String>> aliases = new HashMap<>();
		aliases.put(EntityType.MUSHROOM_COW, prepareAliases(Arrays.asList(
				"mooshroom",
				"mooshroom-cow",
				"mushroom"
		)));
		ALIASES = Collections.unmodifiableMap(aliases);
	}

	private static List<? extends @NonNull String> prepareAliases(
			List<? extends @NonNull String> aliases
	) {
		return Collections.unmodifiableList(StringUtils.normalize(aliases));
	}

	private static List<? extends @NonNull String> getAliasesFor(EntityType entityType) {
		Validate.notNull(entityType, "entityType is null");
		List<? extends @NonNull String> aliases = ALIASES.get(entityType);
		if (aliases != null) return aliases;
		return Collections.emptyList();
	}

	// PERMISSIONS

	private static final String PERMISSION_PREFIX = "shopkeeper.entity.";

	private static String getPermission(EntityType entityType) {
		assert entityType != null;
		String typeName = entityType.name().toLowerCase(Locale.ROOT);
		String permission = PERMISSION_PREFIX + typeName;
		return permission;
	}

	/**
	 * {@link PluginManager#addPermission(Permission) Registers} the permission of the given living
	 * shop object type, if it is not already registered.
	 */
	private static void registerPermission(SKLivingShopObjectType<?> shopObjectType) {
		String permission = Unsafe.assertNonNull(shopObjectType.getPermission());
		PermissionUtils.registerPermission(permission, node -> createPermission(shopObjectType));
	}

	private static Permission createPermission(SKLivingShopObjectType<?> shopObjectType) {
		String permission = Unsafe.assertNonNull(shopObjectType.getPermission());
		String description = "Create shopkeepers of the specific mob type";
		return new Permission(permission, description, PermissionDefault.FALSE);
	}

	// ----

	// Unordered, unmodifiable:
	private final Map<? extends @NonNull EntityType, ? extends @NonNull SKLivingShopObjectType<?>> objectTypes;

	// Order is specified by the 'enabled-living-shops' config setting:
	private final List<@NonNull SKLivingShopObjectType<?>> orderedObjectTypes = new ArrayList<>();
	private final List<? extends @NonNull SKLivingShopObjectType<?>> orderedObjectTypesView = Collections.unmodifiableList(orderedObjectTypes);

	SKLivingShopObjectTypes(LivingShops livingShops) {
		this.objectTypes = createShopObjectTypes(livingShops);
	}

	private static Map<? extends @NonNull EntityType, ? extends @NonNull SKLivingShopObjectType<?>> createShopObjectTypes(
			LivingShops livingShops
	) {
		Map<@NonNull EntityType, @NonNull SKLivingShopObjectType<?>> objectTypes = new HashMap<>();
		for (EntityType entityType : EntityType.values()) {
			if (entityType.isAlive() && entityType.isSpawnable()) {
				objectTypes.put(entityType, createLivingShopObjectType(livingShops, entityType));
			}
		}
		return Collections.unmodifiableMap(objectTypes);
	}

	public void onRegisterDefaults() {
		this.reorderShopObjectTypes();
		this.registerPermissions();
	}

	private void reorderShopObjectTypes() {
		// Update the order of the living shop object types based on how they are ordered inside the
		// config:
		orderedObjectTypes.clear();

		// Add the enabled living shop object types, in the same order as specified inside the
		// config:
		for (EntityType entityType : DerivedSettings.enabledLivingShops) {
			assert entityType != null && entityType.isAlive() && entityType.isSpawnable();
			SKLivingShopObjectType<?> objectType = Unsafe.assertNonNull(this.get(entityType));
			orderedObjectTypes.add(objectType);
		}

		// Add all remaining living shop object types:
		objectTypes.values().forEach(objectType -> {
			if (!DerivedSettings.enabledLivingShops.contains(objectType.getEntityType())) {
				orderedObjectTypes.add(objectType);
			}
		});
	}

	private void registerPermissions() {
		// Register the dynamic mob type specific permissions for all living shop object types, if
		// they are not already registered:
		// Note: These permissions are registered once, and then never unregistered again until the
		// next server restart or full reload. This is not a problem.
		orderedObjectTypesView.forEach(SKLivingShopObjectTypes::registerPermission);
	}

	@Override
	public List<? extends @NonNull String> getAliases(EntityType entityType) {
		return getAliasesFor(entityType);
	}

	@Override
	public Collection<? extends @NonNull SKLivingShopObjectType<?>> getAll() {
		return orderedObjectTypesView;
	}

	@Override
	public @Nullable SKLivingShopObjectType<?> get(EntityType entityType) {
		return objectTypes.get(entityType);
	}

	private static SKLivingShopObjectType<?> createLivingShopObjectType(
			LivingShops livingShops,
			EntityType entityType
	) {
		assert entityType.isAlive() && entityType.isSpawnable();
		String identifier = getIdentifier(entityType);
		List<? extends @NonNull String> aliases = getAliasesFor(entityType);
		String permission = getPermission(entityType);

		SKLivingShopObjectType<?> objectType = null;
		switch (entityType) {
		case VILLAGER:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					VillagerShop.class,
					VillagerShop::new
			);
			break;
		case WANDERING_TRADER:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					WanderingTraderShop.class,
					WanderingTraderShop::new
			);
			break;
		case PIG:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					PigShop.class,
					PigShop::new
			);
			break;
		case CREEPER:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					CreeperShop.class,
					CreeperShop::new
			);
			break;
		case CAT:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					CatShop.class,
					CatShop::new
			);
			break;
		case RABBIT:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					RabbitShop.class,
					RabbitShop::new
			);
			break;
		case SHEEP:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					SheepShop.class,
					SheepShop::new
			);
			break;
		case ZOMBIE:
			objectType = new SKLivingShopObjectType<@NonNull ZombieShop<@NonNull Zombie>>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					ClassUtils.parameterized(ZombieShop.class),
					ZombieShop::new
			);
			break;
		case ZOMBIE_VILLAGER:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					ZombieVillagerShop.class,
					ZombieVillagerShop::new
			);
			break;
		case FOX:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					FoxShop.class,
					FoxShop::new
			);
			break;
		case PARROT:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					ParrotShop.class,
					ParrotShop::new
			);
			break;
		case WOLF:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					WolfShop.class,
					WolfShop::new
			);
			break;
		case HORSE:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					HorseShop.class,
					HorseShop::new
			);
			break;
		case LLAMA:
			objectType = new SKLivingShopObjectType<@NonNull LlamaShop<@NonNull Llama>>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					ClassUtils.parameterized(LlamaShop.class),
					LlamaShop::new
			);
			break;
		case TRADER_LLAMA:
			objectType = new SKLivingShopObjectType<@NonNull LlamaShop<@NonNull TraderLlama>>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					ClassUtils.parameterized(LlamaShop.class),
					LlamaShop::new
			);
			break;
		case PANDA:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					PandaShop.class,
					PandaShop::new
			);
			break;
		case MUSHROOM_COW:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					MooshroomShop.class,
					MooshroomShop::new
			);
			break;
		case SLIME:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					SlimeShop.class,
					SlimeShop::new
			);
			break;
		case MAGMA_CUBE:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					MagmaCubeShop.class,
					MagmaCubeShop::new);
			break;
		case SNOWMAN:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					SnowmanShop.class,
					SnowmanShop::new
			);
			break;
		case SHULKER:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					ShulkerShop.class,
					ShulkerShop::new
			);
			break;
		case TROPICAL_FISH:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					TropicalFishShop.class,
					TropicalFishShop::new
			);
			break;
		case PUFFERFISH:
			objectType = new SKLivingShopObjectType<>(
					livingShops,
					entityType,
					identifier,
					aliases,
					permission,
					PufferFishShop.class,
					PufferFishShop::new
			);
			break;
		default:
			switch (entityType.name()) {
			case "AXOLOTL": // TODO Move up once we only support MC 1.17 upwards.
				objectType = new SKLivingShopObjectType<>(
						livingShops,
						entityType,
						identifier,
						aliases,
						permission,
						AxolotlShop.class,
						AxolotlShop::new
				);
				break;
			case "GLOW_SQUID": // TODO Move up once we only support MC 1.17 upwards.
				objectType = new SKLivingShopObjectType<>(
						livingShops,
						entityType,
						identifier,
						aliases,
						permission,
						GlowSquidShop.class,
						GlowSquidShop::new
				);
				break;
			case "GOAT": // TODO Move up once we only support MC 1.17 upwards.
				objectType = new SKLivingShopObjectType<>(
						livingShops,
						entityType,
						identifier,
						aliases,
						permission,
						GoatShop.class,
						GoatShop::new
				);
				break;
			case "FROG": // TODO Move up once we only support MC 1.19 upwards.
				objectType = new SKLivingShopObjectType<>(
						livingShops,
						entityType,
						identifier,
						aliases,
						permission,
						FrogShop.class,
						FrogShop::new
				);
				break;
			default:
				break;
			}
			break;
		}

		if (objectType == null) {
			Class<? extends Entity> entityClass = Unsafe.assertNonNull(entityType.getEntityClass());
			if (ChestedHorse.class.isAssignableFrom(entityClass)) {
				objectType = new SKLivingShopObjectType<@NonNull ChestedHorseShop<@NonNull ChestedHorse>>(
						livingShops,
						entityType,
						identifier,
						aliases,
						permission,
						ClassUtils.parameterized(ChestedHorseShop.class),
						ChestedHorseShop::new
				);
			} else if (Zombie.class.isAssignableFrom(entityClass)) {
				objectType = new SKLivingShopObjectType<@NonNull ZombieShop<@NonNull Zombie>>(
						livingShops,
						entityType,
						identifier,
						aliases,
						permission,
						ClassUtils.parameterized(ZombieShop.class),
						ZombieShop::new
				);
			} else if (Ageable.class.isAssignableFrom(entityClass)) {
				objectType = new SKLivingShopObjectType<@NonNull BabyableShop<@NonNull Ageable>>(
						livingShops,
						entityType,
						identifier,
						aliases,
						permission,
						ClassUtils.parameterized(BabyableShop.class),
						BabyableShop::new
				);
			} else {
				objectType = new SKLivingShopObjectType<>(
						livingShops,
						entityType,
						identifier,
						aliases,
						permission,
						ClassUtils.parameterized(SKLivingShopObject.class),
						SKLivingShopObject::new
				);
			}
		}

		assert objectType != null;
		return objectType;
	}
}
