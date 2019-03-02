package com.nisovin.shopkeepers;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public class Settings {

	/*
	 * General Settings
	 */
	public static int configVersion = 1;
	public static boolean debug = false;
	public static boolean enableMetrics = true;

	/*
	 * Shopkeeper Data
	 */
	public static String fileEncoding = "UTF-8";
	public static boolean saveInstantly = true;

	/*
	 * Plugin Compatibility
	 */
	public static boolean enableSpawnVerifier = false;
	public static boolean bypassSpawnBlocking = true;
	public static boolean bypassShopInteractionBlocking = false;

	public static boolean enableWorldGuardRestrictions = false;
	public static boolean requireWorldGuardAllowShopFlag = false;
	public static boolean enableTownyRestrictions = false;

	/*
	 * Shop Creation (and removal)
	 */
	public static Material shopCreationItem = Material.VILLAGER_SPAWN_EGG;
	public static String shopCreationItemName = "";
	public static List<String> shopCreationItemLore = new ArrayList<>(0);
	public static boolean preventShopCreationItemRegularUsage = false;
	public static boolean deletingPlayerShopReturnsCreationItem = false;

	public static boolean createPlayerShopWithCommand = false;

	public static boolean requireChestRecentlyPlaced = true;
	public static int maxChestDistance = 15;
	public static int maxShopsPerPlayer = 0;
	public static String maxShopsPermOptions = "10,15,25";

	public static boolean protectChests = true;
	public static boolean preventItemMovement = true;
	public static boolean deleteShopkeeperOnBreakChest = false;

	public static int playerShopkeeperInactiveDays = 0;

	/*
	 * Shop (Object) Types
	 */
	public static List<String> enabledLivingShops = Arrays.asList(
			EntityType.VILLAGER.name(),
			EntityType.COW.name(),
			EntityType.MUSHROOM_COW.name(),
			EntityType.SHEEP.name(),
			EntityType.PIG.name(),
			EntityType.CHICKEN.name(),
			EntityType.OCELOT.name(),
			EntityType.RABBIT.name(),
			EntityType.WOLF.name(),
			EntityType.SNOWMAN.name(),
			EntityType.IRON_GOLEM.name(),
			EntityType.BLAZE.name(),
			EntityType.SILVERFISH.name(),
			EntityType.POLAR_BEAR.name(), // MC 1.10
			EntityType.SKELETON.name(),
			EntityType.STRAY.name(), // MC 1.11
			EntityType.WITHER_SKELETON.name(), // MC 1.11
			EntityType.SPIDER.name(),
			EntityType.CAVE_SPIDER.name(),
			EntityType.CREEPER.name(),
			EntityType.WITCH.name(),
			EntityType.ENDERMAN.name(),
			EntityType.ZOMBIE.name(),
			EntityType.ZOMBIE_VILLAGER.name(), // MC 1.11
			EntityType.PIG_ZOMBIE.name(),
			EntityType.HUSK.name(), // MC 1.11
			EntityType.GIANT.name(),
			EntityType.GHAST.name(),
			EntityType.SLIME.name(),
			EntityType.MAGMA_CUBE.name(),
			EntityType.SQUID.name(),
			EntityType.EVOKER.name(), // MC 1.11
			EntityType.VEX.name(), // MC 1.11
			EntityType.VINDICATOR.name(), // MC 1.11
			EntityType.ILLUSIONER.name(), // MC 1.12
			EntityType.PARROT.name(), // MC 1.12
			EntityType.TURTLE.name(), // MC 1.13
			EntityType.PHANTOM.name(), // MC 1.13
			EntityType.COD.name(), // MC 1.13
			EntityType.SALMON.name(), // MC 1.13
			EntityType.PUFFERFISH.name(), // MC 1.13
			EntityType.TROPICAL_FISH.name(), // MC 1.13
			EntityType.DROWNED.name(), // MC 1.13
			EntityType.DOLPHIN.name() // MC 1.13
	);

	public static boolean useLegacyMobBehavior = false;
	public static boolean disableGravity = false;
	public static int gravityChunkRange = 4;
	public static boolean silenceLivingShopEntities = true;

	public static boolean showNameplates = true;
	public static boolean alwaysShowNameplates = false;
	public static String nameplatePrefix = "&a";

	public static boolean enableCitizenShops = false;

	public static boolean enableSignShops = true;
	public static boolean enableSignPostShops = true;
	public static String signShopFirstLine = "[SHOP]";

	/*
	 * Naming
	 */
	public static String nameRegex = "[A-Za-z0-9 ]{3,32}";
	public static boolean namingOfPlayerShopsViaItem = false;
	public static boolean allowRenamingOfPlayerNpcShops = false;

	/*
	 * Editor Menu
	 */
	public static String editorTitle = "Shopkeeper Editor";

	public static Material previousPageItem = Material.WRITABLE_BOOK;
	public static Material nextPageItem = Material.WRITABLE_BOOK;
	public static Material currentPageItem = Material.WRITABLE_BOOK;

	public static Material nameItem = Material.NAME_TAG;
	public static List<String> nameItemLore = new ArrayList<>(0);

	public static boolean enableChestOptionOnPlayerShop = false;
	public static Material chestItem = Material.CHEST;

	public static Material deleteItem = Material.BONE;

	/*
	 * Non-shopkeeper villagers
	 */
	public static boolean disableOtherVillagers = false;
	public static boolean blockVillagerSpawns = false;
	public static boolean hireOtherVillagers = false;

	/*
	 * Hiring
	 */
	public static Material hireItem = Material.EMERALD;
	public static String hireItemName = "";
	public static List<String> hireItemLore = new ArrayList<>(0);
	public static int hireOtherVillagersCosts = 1;
	public static String forHireTitle = "For Hire";
	public static boolean hireRequireCreationPermission = true;

	/*
	 * Trading
	 */
	public static boolean preventTradingWithOwnShop = true;
	public static boolean preventTradingWhileOwnerIsOnline = false;
	public static boolean useStrictItemComparison = false;
	public static boolean enablePurchaseLogging = false;

	public static int taxRate = 0;
	public static boolean taxRoundUp = false;

	/*
	 * Currencies
	 */
	public static Material currencyItem = Material.EMERALD;
	public static String currencyItemName = "";
	public static List<String> currencyItemLore = new ArrayList<>(0);

	public static Material zeroCurrencyItem = Material.BARRIER;
	public static String zeroCurrencyItemName = "";
	public static List<String> zeroCurrencyItemLore = new ArrayList<>(0);

	public static Material highCurrencyItem = Material.EMERALD_BLOCK;
	public static String highCurrencyItemName = "";
	public static List<String> highCurrencyItemLore = new ArrayList<>(0);

	// note: this can in general be larger than 64!
	public static int highCurrencyValue = 9;
	public static int highCurrencyMinCost = 20;

	public static Material highZeroCurrencyItem = Material.BARRIER;
	public static String highZeroCurrencyItemName = "";
	public static List<String> highZeroCurrencyItemLore = new ArrayList<>(0);

	/*
	 * Messages
	 */
	public static String language = "en";

	public static String msgShopTypeAdminRegular = "admin";
	public static String msgShopTypeSelling = "selling";
	public static String msgShopTypeBuying = "buying";
	public static String msgShopTypeTrading = "trading";
	public static String msgShopTypeBook = "book";

	public static String msgShopTypeDescAdminRegular = "trades items with players";
	public static String msgShopTypeDescSelling = "sells items to players";
	public static String msgShopTypeDescBuying = "buys items from players";
	public static String msgShopTypeDescTrading = "trades items with players";
	public static String msgShopTypeDescBook = "sells books";

	public static String msgShopObjectTypeLiving = "{type}";
	public static String msgShopObjectTypeSign = "sign";
	public static String msgShopObjectTypeNpc = "npc";

	public static String msgSelectedShopType = "&aSelected shop type: &6{type} &7({description})";
	public static String msgSelectedShopObjectType = "&aSelected object type: &6{type}";

	public static String msgCreationItemSelected = "&aShop creation:\n"
			+ "&e  Left/Right-click to select the shop type.\n"
			+ "&e  Sneak + left/right-click to select the object type.\n"
			+ "&e  Right-click a chest to select it.\n"
			+ "&e  Then right-click a block to place the shopkeeper.";

	public static String msgButtonPreviousPage = "&6<- Previous page ({prev_page} of {max_page})";
	public static List<String> msgButtonPreviousPageLore = Arrays.asList();
	public static String msgButtonNextPage = "&6Next page ({next_page} of {max_page}) ->";
	public static List<String> msgButtonNextPageLore = Arrays.asList();
	public static String msgButtonCurrentPage = "&6Page {page} of {max_page}";
	public static List<String> msgButtonCurrentPageLore = Arrays.asList();

	public static String msgButtonName = "&aSet Shop Name";
	public static List<String> msgButtonNameLore = Arrays.asList("Lets you rename", "your shopkeeper");
	public static String msgButtonChest = "&aView Chest Inventory";
	public static List<String> msgButtonChestLore = Arrays.asList("Lets you view the inventory", " your shopkeeper is using");
	public static String msgButtonType = "&aChoose Appearance";
	public static List<String> msgButtonTypeLore = Arrays.asList("Changes the look", "of your shopkeeper");
	public static String msgButtonDelete = "&4Delete";
	public static List<String> msgButtonDeleteLore = Arrays.asList("Closes and removes", "this shopkeeper");
	public static String msgButtonHire = "&aHire";
	public static List<String> msgButtonHireLore = Arrays.asList("Buy this shop");

	public static String msgTradingTitlePrefix = "&2";
	public static String msgTradingTitleDefault = "Shopkeeper";

	public static String msgSelectedChest = "&aChest selected! Right-click a block to place your shopkeeper.";
	public static String msgMustSelectChest = "&7You must right-click a chest before placing your shopkeeper.";
	public static String msgNoChestSelected = "&7The selected block is not a chest!";
	public static String msgChestTooFar = "&7The shopkeeper's chest is too far away!";
	public static String msgChestNotPlaced = "&7You must select a chest you have recently placed!";
	public static String msgChestAlreadyInUse = "&7Another shopkeeper is already using the selected chest!";
	public static String msgNoChestAccess = "&7You cannot access the selected chest!";
	public static String msgTooManyShops = "&7You have too many shops!";
	public static String msgNoAdminShopTypeSelected = "&7You have to select an admin shop type!";
	public static String msgNoPlayerShopTypeSelected = "&7You have to select a player shop type!";
	public static String msgShopCreateFail = "&7You cannot create a shopkeeper there.";

	public static String msgTypeNewName = "&aPlease type the shop's name into the chat.\n"
			+ "  &aType a dash (-) to remove the name.";
	public static String msgNameSet = "&aThe shop's name has been set!";
	public static String msgNameHasNotChanged = "&aThe shop's name has not changed.";
	public static String msgNameInvalid = "&aThat name is not valid!";

	public static String msgShopTypeDisabled = "&7The shop type '&6{type}&7' is disabled.";
	public static String msgShopObjectTypeDisabled = "&7The shop object type '&6{type}&7' is disabled.";

	public static String msgMustTargetShop = "&7You have to target a shopkeeper.";
	public static String msgMustTargetAdminShop = "&7You have to target an admin shopkeeper.";
	public static String msgMustTargetPlayerShop = "&7You have to target a player shopkeeper.";
	public static String msgTargetEntityIsNoShop = "&7The targeted entity is no shopkeeper.";
	public static String msgTargetShopIsNoAdminShop = "&7The targeted shopkeeper is no admin shopkeeper.";
	public static String msgTargetShopIsNoPlayerShop = "&7The targeted shopkeeper is no player shopkeeper.";
	public static String msgUnusedChest = "&7No shopkeeper is using this chest.";
	public static String msgNotOwner = "&7You are not the owner of this shopkeeper.";
	// placeholders: {owner} -> new owners name
	public static String msgOwnerSet = "&aNew owner was set to &e{owner}";
	public static String msgShopCreationItemsGiven = "&aPlayer &e{player}&a has received &e{amount}&a shop creation item(s)!";
	public static String msgUnknownBookAuthor = "Unknown";

	public static String msgTradePermSet = "&aThe shop's trading permission has been set to '&e{perm}&a'!";
	public static String msgTradePermRemoved = "&aThe shop's trading permission '&e{perm}&a' has been removed!";
	public static String msgTradePermView = "&aThe shop's current trading permission is '&e{perm}&a'.";

	public static String msgMustHoldHireItem = "&7You have to hold the required hire item in your hand.";
	public static String msgSetForHire = "&aThe Shopkeeper was set for hire.";
	public static String msgHired = "&aYou have hired this shopkeeper!";
	public static String msgMissingHirePerm = "&7You do not have the permission to hire shopkeepers.";
	public static String msgCantHire = "&aYou cannot afford to hire this shopkeeper.";
	public static String msgCantHireShopType = "&7You do not have the permission to hire this type of shopkeeper.";
	// placeholders: {costs}, {hire-item}
	public static String msgVillagerForHire = "&aThe villager offered his services as a shopkeeper in exchange for &6{costs}x {hire-item}&a.";

	public static String msgMissingTradePerm = "&7You do not have the permission to trade with this shop.";
	public static String msgMissingCustomTradePerm = "&7You do not have the permission to trade with this shop.";
	public static String msgCantTradeWhileOwnerOnline = "&7You cannot trade while the owner of this shop ('&e{owner}&7') is online.";

	public static String msgShopkeeperCreated = "&aShopkeeper created: &6{type} &7({description})\n{setupDesc}";

	public static String msgShopSetupDescSelling = "&e  Add items you want to sell to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescBuying = "&e  Add one of each item you want to buy to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescTrading = "&e  Add items you want to sell to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescBook = "&e  Add written books and blank books to your chest, then\n"
			+ "&e  right-click the shop while sneaking to modify costs.";
	public static String msgShopSetupDescAdminRegular = "&e  Right-click the shop while sneaking to modify trades.";

	public static String msgListAdminShopsHeader = "&9There are &e{shopsCount} &9admin shops: &e(Page {page} of {maxPage})";
	public static String msgListPlayerShopsHeader = "&9Player '&e{player}&9' has &e{shopsCount} &9shops: &e(Page {page} of {maxPage})";
	public static String msgListShopsEntry = "  &e{shopIndex}) &8{shopName}&r&7at &8({location})&7, type: &8{shopType}&7, object type: &8{objectType}";

	public static String msgRemovedAdminShops = "&e{shopsCount} &aadmin shops were removed.";
	public static String msgRemovedPlayerShops = "&e{shopsCount} &ashops of player '&e{player}&a' were removed.";
	public static String msgRemovedAllPlayerShops = "&aAll &e{shopsCount} &aplayer shops were removed.";

	public static String msgConfirmRemoveAdminShops = "&cYou are about to irrevocable remove all admin shops!";
	public static String msgConfirmRemoveOwnShops = "&cYou are about to irrevocable remove all your shops!";
	public static String msgConfirmRemovePlayerShops = "&cYou are about to irrevocable remove all shops of player &6{player}&c!";
	public static String msgConfirmRemoveAllPlayerShops = "&cYou are about to irrevocable remove all player shops of all players!";

	public static String msgConfirmationRequired = "&7Please confirm this action by typing &6/shopkeepers confirm";
	public static String msgConfirmationExpired = "&cConfirmation expired.";
	public static String msgNothingToConfirm = "&cThere is nothing to confirm currently.";

	public static String msgNoPermission = "&cYou don't have the permission to do that.";

	public static String msgCommandUnknown = "&cUnknown command '&e{command}&c'!";
	public static String msgCommandArgumentUnexpected = "&cUnexpected argument '&e{argument}&c'.";
	public static String msgCommandArgumentMissing = "&cMissing argument '&e{argumentFormat}&c'.";
	public static String msgCommandArgumentInvalid = "&cInvalid argument '&e{argument}&c'.";
	public static String msgCommandPlayerArgumentMissing = "&cNo player specified for '&e{argumentFormat}&c'.";
	public static String msgCommandPlayerArgumentInvalid = "&cNo player found for '&e{argument}&c'.";
	public static String msgCommandShopTypeArgumentInvalid = "&cUnknown shop type '&e{argument}&c'.";
	public static String msgCommandShopObjectTypeArgumentInvalid = "&cUnknown shop object type '&e{argument}&c'.";
	public static String msgCommandShopkeeperArgumentInvalid = "&cNo shopkeeper found for '&e{argument}&c'.";
	public static String msgCommandShopkeeperArgumentNoAdminShop = "&cShopkeeper '&e{argument}&c' is no admin shopkeeper.";
	public static String msgCommandShopkeeperArgumentNoPlayerShop = "&cShopkeeper '&e{argument}&c' is no player shopkeeper.";

	public static String msgCommandHelpTitle = "&9***** &8[&6Shopkeepers v{version}&8] &9*****";
	public static String msgCommandHelpUsageFormat = "&e{usage}";
	public static String msgCommandHelpDescriptionFormat = "&8 - &3{description}";

	public static String msgCommandDescriptionShopkeeper = "Creates a shopkeeper.";
	public static String msgCommandDescriptionHelp = "Shows this help page.";
	public static String msgCommandDescriptionReload = "Reloads this plugin.";
	public static String msgCommandDescriptionDebug = "Toggles debug mode on and off.";
	public static String msgCommandDescriptionList = "Lists all shops for the specified player, or all admin shops.";
	public static String msgCommandDescriptionRemove = "Removes all shops for the specified player, all players, or all admin shops.";
	public static String msgCommandDescriptionGive = "Gives shop creation item(s) to the specified player.";
	public static String msgCommandDescriptionRemote = "Remotely opens a shop.";
	public static String msgCommandDescriptionRemoteEdit = "Remotely edits a shop.";
	public static String msgCommandDescriptionTransfer = "Transfers the ownership of a shop.";
	public static String msgCommandDescriptionSettradeperm = "Sets, removes (-) or displays (?) the trading permission.";
	public static String msgCommandDescriptionSetforhire = "Sets one of your shops for sale.";

	// /////

	private static String toConfigKey(String fieldName) {
		return fieldName.replaceAll("([A-Z][a-z]+)", "-$1").toLowerCase();
	}

	// returns true, if the config misses values which need to be saved
	public static boolean loadConfiguration(Configuration config) {
		boolean configChanged = false;

		// perform config migrations (if the config is not empty):
		if (!config.getKeys(false).isEmpty()) {
			int configVersion = config.getInt("config-version", 0); // default value is important here
			if (configVersion <= 0) {
				migrateConfig_0_to_1(config);
				configChanged = true;
			}
		}

		// exempt a few string / string list settings from color conversion:
		List<String> noColorConversionKeys = Arrays.asList(
				toConfigKey("fileEncoding"), toConfigKey("shopCreationItemSpawnEggEntityType"),
				toConfigKey("maxShopsPermOptions"), toConfigKey("enabledLivingShops"),
				toConfigKey("nameRegex"), toConfigKey("language"));
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				if (field.isSynthetic()) continue;
				Class<?> typeClass = field.getType();
				String configKey = toConfigKey(field.getName());

				// initialize the setting with the default value, if it is missing in the config
				if (!config.isSet(configKey)) {
					Log.warning("Config: Inserting default value for missing config entry: " + configKey);

					// determine default value:
					Object defaultValue = null;
					Configuration defaults = config.getDefaults(); // might be null or miss values
					if (defaults != null) {
						defaultValue = defaults.get(configKey);
					}
					if (defaultValue == null) {
						// fallback to the current value:
						defaultValue = field.get(null);
					}

					// validate default value:
					if (defaultValue == null) {
						Log.warning("Config: Missing default value for missing config entry: " + configKey);
						continue;
					} else if (!Utils.isAssignableFrom(typeClass, defaultValue.getClass())) {
						Log.warning("Config: Default value for missing config entry '" + configKey + "' is of wrong type: "
								+ "Got " + defaultValue.getClass().getName() + ", expecting " + typeClass.getName());
						continue;
					}

					// set default value:
					if (typeClass == Material.class) {
						config.set(configKey, ((Material) defaultValue).name());
					} else if (typeClass == String.class) {
						// decolorize, if not exempted:
						if (!noColorConversionKeys.contains(configKey)) {
							defaultValue = Utils.decolorize((String) defaultValue);
						}
						config.set(configKey, defaultValue);
					} else if (typeClass == List.class && (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] == String.class) {
						// decolorize, if not exempted:
						if (!noColorConversionKeys.contains(configKey)) {
							defaultValue = Utils.decolorize(ConversionUtils.toStringList((List<?>) defaultValue));
						}
						config.set(configKey, defaultValue);
					} else {
						config.set(configKey, defaultValue);
					}
					configChanged = true;
				}

				if (typeClass == String.class) {
					String string = config.getString(configKey);
					// colorize, if not exempted:
					if (!noColorConversionKeys.contains(configKey)) {
						string = Utils.colorize(string);
					}
					field.set(null, string);
				} else if (typeClass == int.class) {
					field.set(null, config.getInt(configKey));
				} else if (typeClass == short.class) {
					field.set(null, (short) config.getInt(configKey));
				} else if (typeClass == boolean.class) {
					field.set(null, config.getBoolean(configKey));
				} else if (typeClass == Material.class) {
					// this assumes that legacy item conversion has already been performed
					Material material = loadMaterial(config, configKey, false);
					if (material != null) {
						field.set(null, material);
					} else {
						Log.warning("Config: Unknown material for config entry '" + configKey + "': " + config.get(configKey));
						Log.warning("Config: All valid material names can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html");
					}
				} else if (typeClass == List.class) {
					Class<?> genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
					if (genericType == String.class) {
						List<String> stringList = config.getStringList(configKey);
						// colorize, if not exempted:
						if (!noColorConversionKeys.contains(configKey)) {
							stringList = Utils.colorize(stringList);
						}
						field.set(null, stringList);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// validation:

		boolean foundInvalidEntityType = false;
		for (String entityTypeId : enabledLivingShops) {
			EntityType entityType = matchEntityType(entityTypeId);
			if (entityType == null || !entityType.isAlive() || !entityType.isSpawnable()) {
				foundInvalidEntityType = true;
				Log.warning("Config: Invalid living entity type name in 'enabled-living-shops': " + entityTypeId);
			}
		}
		if (foundInvalidEntityType) {
			Log.warning("Config: All existing entity type names can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html");
		}

		if (maxChestDistance > 50) {
			Log.warning("Config: 'max-chest-distance' can be at most 50.");
			maxChestDistance = 50;
		}
		if (gravityChunkRange < 0) {
			Log.warning("Config: 'gravity-chunk-range' cannot be negative.");
			gravityChunkRange = 0;
		}
		if (highCurrencyValue <= 0 && highCurrencyItem != Material.AIR) {
			Log.debug("Config: 'high-currency-item' disabled because of 'high-currency-value' being less than 1.");
			highCurrencyItem = Material.AIR;
		}
		// certain items cannot be of type AIR:
		if (shopCreationItem == Material.AIR) {
			Log.warning("Config: 'shop-creation-item' can not be AIR.");
			shopCreationItem = Material.VILLAGER_SPAWN_EGG;
		}
		if (hireItem == Material.AIR) {
			Log.warning("Config: 'hire-item' can not be AIR.");
			hireItem = Material.EMERALD;
		}
		if (currencyItem == Material.AIR) {
			Log.warning("Config: 'currency-item' can not be AIR.");
			currencyItem = Material.EMERALD;
		}
		if (namingOfPlayerShopsViaItem) {
			if (nameItem == Material.AIR) {
				Log.warning("Config: 'name-item' can not be AIR if naming-of-player-shops-via-item is enabled!");
				nameItem = Material.NAME_TAG;
			}
		}
		if (taxRate < 0) {
			Log.warning("Config: 'tax-rate' can not be less than 0!");
			taxRate = 0;
		} else if (taxRate > 100) {
			Log.warning("Config: 'tax-rate' can not be larger than 100!");
			taxRate = 100;
		}

		return configChanged;
	}

	private static Material loadMaterial(ConfigurationSection config, String key, boolean checkLegacy) {
		String materialName = config.getString(key); // note: takes defaults into account
		if (materialName == null) return null;
		Material material = Material.matchMaterial(materialName);
		if (material == null && checkLegacy) {
			// check for legacy material:
			String legacyMaterialName = Material.LEGACY_PREFIX + materialName;
			material = Material.matchMaterial(legacyMaterialName);
		}
		return material;
	}

	private static void migrateConfig_0_to_1(Configuration config) {
		// pre 1.13 to 1.13:
		Log.info("Migrating config to version 1 ..");

		// migrate shop creation item, if present:
		String shopCreationItemTypeName = config.getString("shop-creation-item", null);
		if (shopCreationItemTypeName != null) {
			// note: this takes defaults into account:
			Material shopCreationItem = loadMaterial(config, "shop-creation-item", true);
			String shopCreationItemSpawnEggEntityType = config.getString("shop-creation-item-spawn-egg-entity-type");
			if (shopCreationItem == Material.LEGACY_MONSTER_EGG && !StringUtils.isEmpty(shopCreationItemSpawnEggEntityType)) {
				// migrate spawn egg (ignores the data value): spawn eggs are different materials now
				EntityType spawnEggEntityType = null;
				try {
					spawnEggEntityType = EntityType.valueOf(shopCreationItemSpawnEggEntityType);
				} catch (IllegalArgumentException e) {
					// unknown entity type
				}
				Material newShopCreationItem = LegacyConversion.fromLegacySpawnEgg(spawnEggEntityType);

				boolean usingDefault = false;
				if (newShopCreationItem == null || newShopCreationItem == Material.AIR) {
					// fallback to default:
					newShopCreationItem = Material.VILLAGER_SPAWN_EGG;
					usingDefault = true;
				}
				assert newShopCreationItem != null;

				Log.info("  Migrating 'shop-creation-item' from '" + shopCreationItemTypeName + "' and spawn egg entity type '"
						+ shopCreationItemSpawnEggEntityType + "' to '" + newShopCreationItem + "'" + (usingDefault ? " (default)" : "") + ".");
				config.set("shop-creation-item", newShopCreationItem.name());
			} else {
				// regular material + data value migration:
				migrateLegacyItemData(config, "shop-creation-item", "shop-creation-item", "shop-creation-item-data", Material.VILLAGER_SPAWN_EGG);
			}
		}

		// remove shop-creation-item-spawn-egg-entity-type from config:
		if (config.isSet("shop-creation-item-spawn-egg-entity-type")) {
			Log.info("  Removing 'shop-creation-item-spawn-egg-entity-type' (previously '" + config.get("shop-creation-item-spawn-egg-entity-type", null) + "').");
			config.set("shop-creation-item-spawn-egg-entity-type", null);
		}

		// remove shop-creation-item-data-value from config:
		if (config.isSet("shop-creation-item-data")) {
			Log.info("  Removing 'shop-creation-item-data' (previously '" + config.get("shop-creation-item-data", null) + "').");
			config.set("shop-creation-item-data", null);
		}

		// name item:
		migrateLegacyItemData(config, "name-item", "name-item", "name-item-data", Material.NAME_TAG);

		// chest item:
		migrateLegacyItemData(config, "chest-item", "chest-item", "chest-item-data", Material.CHEST);

		// delete item:
		migrateLegacyItemData(config, "delete-item", "delete-item", "delete-item-data", Material.BONE);

		// hire item:
		migrateLegacyItemData(config, "hire-item", "hire-item", "hire-item-data", Material.EMERALD);

		// currency item:
		migrateLegacyItemData(config, "currency-item", "currency-item", "currency-item-data", Material.EMERALD);

		// zero currency item:
		migrateLegacyItemData(config, "zero-currency-item", "zero-currency-item", "zero-currency-item-data", Material.BARRIER);

		// high currency item:
		migrateLegacyItemData(config, "high-currency-item", "high-currency-item", "high-currency-item-data", Material.EMERALD_BLOCK);

		// high zero currency item:
		migrateLegacyItemData(config, "high-zero-currency-item", "high-zero-currency-item", "high-zero-currency-item-data", Material.BARRIER);

		// update config version:
		config.set("config-version", 1);
		Log.info("Config migration to version 1 done.");
	}

	// convert legacy material + data value to new material, returns true if migrations took place
	private static boolean migrateLegacyItemData(ConfigurationSection config, String migratedItemId, String itemTypeKey, String itemDataKey, Material defaultType) {
		boolean migrated = false;

		// migrate material, if present:
		String itemTypeName = config.getString(itemTypeKey, null);
		if (itemTypeName != null) {
			Material newItemType = null;
			int itemData = config.getInt(itemDataKey, 0);
			Material itemType = loadMaterial(config, itemTypeKey, true);
			if (itemType != null) {
				newItemType = LegacyConversion.fromLegacy(itemType, (byte) itemData);
			}
			boolean usingDefault = false;
			if (newItemType == null || newItemType == Material.AIR) {
				// fallback to default:
				newItemType = defaultType;
				usingDefault = true;
			}
			if (itemType != newItemType) {
				Log.info("  Migrating '" + migratedItemId + "' from type '" + itemTypeName + "' and data value '" + itemData + "' to type '"
						+ (newItemType == null ? "" : newItemType.name()) + "'" + (usingDefault ? " (default)" : "") + ".");
				config.set(itemTypeKey, (newItemType != null ? newItemType.name() : null));
				migrated = true;
			}
		}

		// remove data value from config:
		if (config.isSet(itemDataKey)) {
			Log.info("  Removing '" + itemDataKey + "' (previously '" + config.get(itemDataKey, null) + "').");
			config.set(itemDataKey, null);
			migrated = true;
		}
		return migrated;
	}

	public static void loadLanguageConfiguration(Configuration config) {
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				if (field.getType() == String.class && field.getName().startsWith("msg")) {
					String configKey = toConfigKey(field.getName());
					field.set(null, Utils.colorize(config.getString(configKey, (String) field.get(null))));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// item utilities:

	// creation item:
	public static ItemStack createShopCreationItem() {
		return ItemUtils.createItemStack(shopCreationItem, 1, shopCreationItemName, shopCreationItemLore);
	}

	public static boolean isShopCreationItem(ItemStack item) {
		return ItemUtils.isSimilar(item, Settings.shopCreationItem, Settings.shopCreationItemName, Settings.shopCreationItemLore);
	}

	// naming item:
	public static ItemStack createNameButtonItem() {
		return ItemUtils.createItemStack(nameItem, 1, msgButtonName, msgButtonNameLore);
	}

	public static boolean isNamingItem(ItemStack item) {
		return ItemUtils.isSimilar(item, nameItem, null, Settings.nameItemLore);
	}

	// chest button:
	public static ItemStack createChestButtonItem() {
		return ItemUtils.createItemStack(chestItem, 1, msgButtonChest, msgButtonChestLore);
	}

	// delete button:
	public static ItemStack createDeleteButtonItem() {
		return ItemUtils.createItemStack(deleteItem, 1, msgButtonDelete, msgButtonDeleteLore);
	}

	// hire item:
	public static ItemStack createHireButtonItem() {
		return ItemUtils.createItemStack(hireItem, 1, msgButtonHire, msgButtonHireLore);
	}

	public static boolean isHireItem(ItemStack item) {
		return ItemUtils.isSimilar(item, hireItem, hireItemName, hireItemLore);
	}

	// currency item:
	public static ItemStack createCurrencyItem(int amount) {
		return ItemUtils.createItemStack(Settings.currencyItem, amount, Settings.currencyItemName, Settings.currencyItemLore);
	}

	public static boolean isCurrencyItem(ItemStack item) {
		return ItemUtils.isSimilar(item, Settings.currencyItem, Settings.currencyItemName, Settings.currencyItemLore);
	}

	// high currency item:
	public static boolean isHighCurrencyEnabled() {
		return (Settings.highCurrencyItem != Material.AIR);
	}

	public static ItemStack createHighCurrencyItem(int amount) {
		if (!isHighCurrencyEnabled()) return null;
		return ItemUtils.createItemStack(Settings.highCurrencyItem, amount, Settings.highCurrencyItemName, Settings.highCurrencyItemLore);
	}

	public static boolean isHighCurrencyItem(ItemStack item) {
		if (!isHighCurrencyEnabled()) return false;
		return ItemUtils.isSimilar(item, Settings.highCurrencyItem, Settings.highCurrencyItemName, Settings.highCurrencyItemLore);
	}

	// zero currency item:
	public static ItemStack createZeroCurrencyItem() {
		if (Settings.zeroCurrencyItem == Material.AIR) return null;
		return ItemUtils.createItemStack(Settings.zeroCurrencyItem, 1, Settings.zeroCurrencyItemName, Settings.zeroCurrencyItemLore);
	}

	public static boolean isZeroCurrencyItem(ItemStack item) {
		if (Settings.zeroCurrencyItem == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return ItemUtils.isSimilar(item, Settings.zeroCurrencyItem, Settings.zeroCurrencyItemName, Settings.zeroCurrencyItemLore);
	}

	// high zero currency item:
	public static ItemStack createHighZeroCurrencyItem() {
		if (Settings.highZeroCurrencyItem == Material.AIR) return null;
		return ItemUtils.createItemStack(Settings.highZeroCurrencyItem, 1, Settings.highZeroCurrencyItemName, Settings.highZeroCurrencyItemLore);
	}

	public static boolean isHighZeroCurrencyItem(ItemStack item) {
		if (Settings.highZeroCurrencyItem == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return ItemUtils.isSimilar(item, Settings.highZeroCurrencyItem, Settings.highZeroCurrencyItemName, Settings.highZeroCurrencyItemLore);
	}

	//

	public static int getMaxShops(Player player) {
		int maxShops = Settings.maxShopsPerPlayer;
		String[] maxShopsPermOptions = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : maxShopsPermOptions) {
			if (Utils.hasPermission(player, "shopkeeper.maxshops." + perm)) {
				maxShops = Integer.parseInt(perm);
			}
		}
		return maxShops;
	}

	public static EntityType matchEntityType(String entityTypeId) {
		if (StringUtils.isEmpty(entityTypeId)) return null;
		// get by bukkit id:
		String normalizedEntityTypeId = entityTypeId.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
		try {
			return EntityType.valueOf(normalizedEntityTypeId);
		} catch (IllegalArgumentException e) {
			// unknown entity type:
			return null;
		}
	}
}
