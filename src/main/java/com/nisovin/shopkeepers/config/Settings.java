package com.nisovin.shopkeepers.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.config.lib.Config;
import com.nisovin.shopkeepers.config.lib.ConfigLoadException;
import com.nisovin.shopkeepers.config.migration.ConfigMigrations;
import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.EntityUtils;
import com.nisovin.shopkeepers.util.ItemData;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.PermissionUtils;

public class Settings extends Config {

	/*
	 * General Settings
	 */
	public static int configVersion = 4;
	public static boolean debug = false;
	// See DebugOptions for all available options.
	public static List<String> debugOptions = new ArrayList<>(0);
	public static boolean enableMetrics = true;

	/*
	 * Messages
	 */
	public static String language = "en-default";

	/*
	 * Shopkeeper Data
	 */
	public static String fileEncoding = "UTF-8";
	public static boolean saveInstantly = true;

	public static boolean convertPlayerItems = false;
	public static boolean convertAllPlayerItems = true;
	public static List<ItemData> convertPlayerItemsExceptions = new ArrayList<>();

	/*
	 * Plugin Compatibility
	 */
	public static boolean enableSpawnVerifier = false;
	public static boolean bypassSpawnBlocking = true;
	public static boolean checkShopInteractionResult = false;

	public static boolean enableWorldGuardRestrictions = false;
	public static boolean requireWorldGuardAllowShopFlag = false;
	public static boolean registerWorldGuardAllowShopFlag = true;

	public static boolean enableTownyRestrictions = false;

	/*
	 * Shop Creation (and removal)
	 */
	public static ItemData shopCreationItem = new ItemData(Material.VILLAGER_SPAWN_EGG, "&aShopkeeper", null);
	public static boolean preventShopCreationItemRegularUsage = true;
	public static boolean deletingPlayerShopReturnsCreationItem = false;

	public static boolean createPlayerShopWithCommand = false;

	public static boolean requireContainerRecentlyPlaced = true;
	public static int maxContainerDistance = 15;
	public static int maxShopsPerPlayer = -1;
	public static String maxShopsPermOptions = "10,15,25";

	public static boolean protectContainers = true;
	public static boolean preventItemMovement = true;
	public static boolean deleteShopkeeperOnBreakContainer = false;

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
			EntityType.HUSK.name(), // MC 1.11
			EntityType.GIANT.name(),
			EntityType.GHAST.name(),
			EntityType.SLIME.name(),
			EntityType.MAGMA_CUBE.name(),
			EntityType.SQUID.name(),
			EntityType.HORSE.name(),
			EntityType.MULE.name(),
			EntityType.DONKEY.name(),
			EntityType.SKELETON_HORSE.name(),
			EntityType.ZOMBIE_HORSE.name(),
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
			EntityType.DOLPHIN.name(), // MC 1.13
			EntityType.CAT.name(), // MC 1.14
			EntityType.PANDA.name(), // MC 1.14
			EntityType.PILLAGER.name(), // MC 1.14
			EntityType.RAVAGER.name(), // MC 1.14
			EntityType.LLAMA.name(), // MC 1.11
			EntityType.TRADER_LLAMA.name(), // MC 1.14
			EntityType.WANDERING_TRADER.name(), // MC 1.14
			EntityType.FOX.name(), // MC 1.14
			"BEE", // MC 1.15
			"ZOMBIFIED_PIGLIN", // MC 1.16, replaced PIG_ZOMBIE
			"PIGLIN", // MC 1.16
			"HOGLIN", // MC 1.16
			"ZOGLIN", // MC 1.16
			"STRIDER", // MC 1.16
			"PIGLIN_BRUTE" // MC 1.16.2
	);

	public static boolean disableGravity = false;
	public static int gravityChunkRange = 4;
	public static boolean silenceLivingShopEntities = true;

	public static boolean showNameplates = true;
	public static boolean alwaysShowNameplates = false;

	public static boolean enableCitizenShops = true;

	public static boolean enableSignShops = true;
	public static boolean enableSignPostShops = true;

	/*
	 * Naming
	 */
	public static String nameRegex = "[A-Za-z0-9 ]{3,32}";
	public static boolean namingOfPlayerShopsViaItem = false;
	public static boolean allowRenamingOfPlayerNpcShops = false;

	/*
	 * Editor Menu
	 */
	public static int maxTradesPages = 5;

	public static ItemData previousPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData nextPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData currentPageItem = new ItemData(Material.WRITABLE_BOOK);
	public static ItemData tradeSetupItem = new ItemData(Material.PAPER);

	public static ItemData nameItem = new ItemData(Material.NAME_TAG);

	public static boolean enableContainerOptionOnPlayerShop = true;
	public static ItemData containerItem = new ItemData(Material.CHEST);

	public static ItemData deleteItem = new ItemData(Material.BONE);

	/*
	 * Non-shopkeeper villagers
	 */
	public static boolean disableOtherVillagers = false;
	public static boolean blockVillagerSpawns = false;
	public static boolean disableZombieVillagerCuring = false;
	public static boolean hireOtherVillagers = false;

	public static boolean disableWanderingTraders = false;
	public static boolean blockWanderingTraderSpawns = false;
	public static boolean hireWanderingTraders = false;

	public static boolean editRegularVillagers = true;
	public static boolean editRegularWanderingTraders = true;

	/*
	 * Hiring
	 */
	public static ItemData hireItem = new ItemData(Material.EMERALD);
	public static int hireOtherVillagersCosts = 1;
	public static boolean hireRequireCreationPermission = true;

	/*
	 * Trading
	 */
	public static boolean preventTradingWithOwnShop = true;
	public static boolean preventTradingWhileOwnerIsOnline = false;
	public static boolean useStrictItemComparison = false;
	public static boolean enablePurchaseLogging = false;
	public static boolean incrementVillagerStatistics = false;

	public static int taxRate = 0;
	public static boolean taxRoundUp = false;

	/*
	 * Currencies
	 */
	public static ItemData currencyItem = new ItemData(Material.EMERALD);
	public static ItemData zeroCurrencyItem = new ItemData(Material.BARRIER);
	public static ItemData highCurrencyItem = new ItemData(Material.EMERALD_BLOCK);
	public static ItemData zeroHighCurrencyItem = new ItemData(Material.BARRIER);

	// Note: This can in general be larger than 64!
	public static int highCurrencyValue = 9;
	public static int highCurrencyMinCost = 20;

	///// DERIVED SETTINGS

	public static class MaxShopsPermission implements Comparable<MaxShopsPermission> {

		// Integer.MAX_VALUE indicates no limit.
		public final int maxShops;
		public final String permission;

		private MaxShopsPermission(int maxShops, String permission) {
			this.maxShops = maxShops;
			this.permission = permission;
		}

		@Override
		public int compareTo(MaxShopsPermission other) {
			return Integer.compare(this.maxShops, other.maxShops);
		}
	}

	// Stores derived settings which get setup after loading the config.
	public static class DerivedSettings {

		public static ItemData namingItemData;

		// Button items:
		public static ItemData nameButtonItem;
		public static ItemData containerButtonItem;
		public static ItemData deleteButtonItem;
		public static ItemData hireButtonItem;

		public static ItemData deleteVillagerButtonItem;
		public static ItemData villagerInventoryButtonItem;

		public static Pattern shopNamePattern;

		// Sorted in descending order:
		public static final List<MaxShopsPermission> maxShopsPermissions = new ArrayList<>();

		public static final Set<EntityType> enabledLivingShops = new LinkedHashSet<>();

		static {
			// Initial setup of default values:
			setup();
		}

		// Gets called after setting values have changed (eg. after the config has been loaded):
		private static void setup() {
			// Ignore display name (which is used for specifying the new shopkeeper name):
			namingItemData = new ItemData(ItemUtils.setItemStackName(nameItem.createItemStack(), null));

			// Button items:
			nameButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(nameItem.createItemStack(), Messages.buttonName, Messages.buttonNameLore));
			containerButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(containerItem.createItemStack(), Messages.buttonContainer, Messages.buttonContainerLore));
			deleteButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(deleteItem.createItemStack(), Messages.buttonDelete, Messages.buttonDeleteLore));
			hireButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(hireItem.createItemStack(), Messages.buttonHire, Messages.buttonHireLore));

			// Note: These use the same item types as the corresponding shopkeeper buttons.
			deleteVillagerButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(deleteItem.createItemStack(), Messages.buttonDeleteVillager, Messages.buttonDeleteVillagerLore));
			villagerInventoryButtonItem = new ItemData(ItemUtils.setItemStackNameAndLore(containerItem.createItemStack(), Messages.buttonVillagerInventory, Messages.buttonVillagerInventoryLore));

			// Shop name pattern:
			try {
				shopNamePattern = Pattern.compile("^" + Settings.nameRegex + "$");
			} catch (PatternSyntaxException e) {
				Log.warning(INSTANCE.getLogPrefix() + "'name-regex' is not a valid regular expression ('" + Settings.nameRegex + "'). Reverting to default.");
				Settings.nameRegex = "[A-Za-z0-9 ]{3,32}";
				shopNamePattern = Pattern.compile("^" + Settings.nameRegex + "$");
			}

			// Maximum shops permissions:
			maxShopsPermissions.clear();
			// Add permission for an unlimited number of shops:
			maxShopsPermissions.add(new MaxShopsPermission(Integer.MAX_VALUE, ShopkeepersPlugin.MAXSHOPS_UNLIMITED_PERMISSION));
			String[] maxShopsPermOptions = Settings.maxShopsPermOptions.replace(" ", "").split(",");
			for (String permOption : maxShopsPermOptions) {
				// Validate:
				Integer maxShops = ConversionUtils.parseInt(permOption);
				if (maxShops == null || maxShops <= 0) {
					Log.warning(INSTANCE.getLogPrefix() + "Ignoring invalid entry in 'max-shops-perm-options': " + permOption);
					continue;
				}
				String permission = "shopkeeper.maxshops." + permOption;
				maxShopsPermissions.add(new MaxShopsPermission(maxShops, permission));
			}
			Collections.sort(maxShopsPermissions, Collections.reverseOrder()); // Descending order

			// Enabled living shop types:
			enabledLivingShops.clear();
			boolean foundInvalidEntityType = false;
			for (String entityTypeId : Settings.enabledLivingShops) {
				EntityType entityType = EntityUtils.matchEntityType(entityTypeId);
				if (entityType == null || !entityType.isAlive() || !entityType.isSpawnable()) {
					foundInvalidEntityType = true;
					if ("PIG_ZOMBIE".equals(entityTypeId)) {
						// Migration note for MC 1.16 TODO Remove this again at some point?
						Log.warning(INSTANCE.getLogPrefix() + "Ignoring mob type 'PIG_ZOMBIE' in setting 'enabled-living-shops'. This mob no longer exist since MC 1.16. Consider replacing it with 'ZOMBIFIED_PIGLIN'.");
					} else {
						Log.warning(INSTANCE.getLogPrefix() + "Invalid living entity type name in 'enabled-living-shops': " + entityTypeId);
					}
				} else {
					enabledLivingShops.add(entityType);
				}
			}
			if (foundInvalidEntityType) {
				Log.warning(INSTANCE.getLogPrefix() + "All existing entity type names can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html");
			}
		}

		private DerivedSettings() {
		}
	}

	// Cached values for settings which are used asynchronously.
	public static final class AsyncSettings {

		private static volatile AsyncSettings INSTANCE = new AsyncSettings();

		private static void refresh() {
			INSTANCE = new AsyncSettings();
		}

		public final boolean debug;
		public final List<String> debugOptions;
		public final String fileEncoding;

		private AsyncSettings() {
			this.debug = Settings.debug;
			this.debugOptions = new ArrayList<>(Settings.debugOptions);
			this.fileEncoding = Settings.fileEncoding;
		}

	}

	public static AsyncSettings async() {
		return AsyncSettings.INSTANCE;
	}

	// Can also be called externally, when settings are changed externally.
	public static void onSettingsChanged() {
		// Update derived settings:
		DerivedSettings.setup();

		// Refresh async settings cache:
		AsyncSettings.refresh();
	}

	// ITEMS

	// Shop creation item:
	public static ItemStack createShopCreationItem() {
		return shopCreationItem.createItemStack();
	}

	public static boolean isShopCreationItem(ItemStack item) {
		return shopCreationItem.matches(item);
	}

	// Naming item:
	public static boolean isNamingItem(ItemStack item) {
		return DerivedSettings.namingItemData.matches(item);
	}

	public static ItemStack createNameButtonItem() {
		return DerivedSettings.nameButtonItem.createItemStack();
	}

	// Container button:
	public static ItemStack createContainerButtonItem() {
		return DerivedSettings.containerButtonItem.createItemStack();
	}

	// Delete button:
	public static ItemStack createDeleteButtonItem() {
		return DerivedSettings.deleteButtonItem.createItemStack();
	}

	// Hire item:
	public static ItemStack createHireButtonItem() {
		return DerivedSettings.hireButtonItem.createItemStack();
	}

	public static boolean isHireItem(ItemStack item) {
		return hireItem.matches(item);
	}

	// CURRENCY

	// Currency item:
	public static ItemStack createCurrencyItem(int amount) {
		return currencyItem.createItemStack(amount);
	}

	public static boolean isCurrencyItem(ItemStack item) {
		return currencyItem.matches(item);
	}

	// High currency item:
	public static boolean isHighCurrencyEnabled() {
		return (highCurrencyValue > 0 && highCurrencyItem.getType() != Material.AIR);
	}

	public static ItemStack createHighCurrencyItem(int amount) {
		if (!isHighCurrencyEnabled()) return null;
		return highCurrencyItem.createItemStack(amount);
	}

	public static boolean isHighCurrencyItem(ItemStack item) {
		if (!isHighCurrencyEnabled()) return false;
		return highCurrencyItem.matches(item);
	}

	// Zero currency item:
	public static ItemStack createZeroCurrencyItem() {
		if (zeroCurrencyItem.getType() == Material.AIR) return null;
		return zeroCurrencyItem.createItemStack();
	}

	public static boolean isZeroCurrencyItem(ItemStack item) {
		if (zeroCurrencyItem.getType() == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return zeroCurrencyItem.matches(item);
	}

	// Zero high currency item:
	public static ItemStack createZeroHighCurrencyItem() {
		if (zeroHighCurrencyItem.getType() == Material.AIR) return null;
		return zeroHighCurrencyItem.createItemStack();
	}

	public static boolean isZeroHighCurrencyItem(ItemStack item) {
		if (zeroHighCurrencyItem.getType() == Material.AIR) {
			return ItemUtils.isEmpty(item);
		}
		return zeroHighCurrencyItem.matches(item);
	}

	// VARIOUS

	// Integer.MAX_VALUE indicates no limit.
	public static int getMaxShopsLimit(Player player) {
		if (Settings.maxShopsPerPlayer == -1) {
			return Integer.MAX_VALUE; // No limit by default
		}
		int maxShops = Settings.maxShopsPerPlayer; // Default
		for (MaxShopsPermission entry : DerivedSettings.maxShopsPermissions) {
			// Note: The max shops permission entries are sorted in descending order.
			if (entry.maxShops <= maxShops) {
				break;
			}
			if (PermissionUtils.hasPermission(player, entry.permission)) {
				maxShops = entry.maxShops;
				break;
			}
		}
		return maxShops;
	}

	///// LOADING

	// Returns null on success, otherwise a severe issue prevented loading the config.
	public static ConfigLoadException loadConfig(Plugin plugin) {
		Log.info("Loading config.");

		// Save default config in case the config file does not exist:
		plugin.saveDefaultConfig();

		// Load config:
		plugin.reloadConfig();
		Configuration config = plugin.getConfig();

		// Load settings from config:
		boolean configChanged = false;
		try {
			configChanged = loadConfig(config);
		} catch (ConfigLoadException e) {
			// Config loading failed with a severe issue:
			return e;
		}

		if (configChanged) {
			// If the config was modified (migrations, adding missing settings, ..), save it:
			// TODO Persist comments somehow.
			plugin.saveConfig();
		}
		return null; // Config loaded successfully
	}

	// Returns true, if the config has changed and needs to be saved.
	private static boolean loadConfig(Configuration config) throws ConfigLoadException {
		boolean configChanged = false;

		// Perform config migrations:
		boolean migrated = ConfigMigrations.applyMigrations(config);
		if (migrated) {
			configChanged = true;
		}

		// Insert default values for settings missing inside the config:
		boolean insertedDefaults = Settings.INSTANCE.insertMissingDefaultValues(config);
		if (insertedDefaults) {
			configChanged = true;
		}

		// Load and validate settings:
		Settings.INSTANCE.load(config);

		onSettingsChanged();
		return configChanged;
	}

	/////

	private static final Settings INSTANCE = new Settings();

	private Settings() {
	}

	@Override
	protected void validateSettings() {
		if (maxContainerDistance > 50) {
			Log.warning(this.getLogPrefix() + "'max-container-distance' can be at most 50.");
			maxContainerDistance = 50;
		}
		if (gravityChunkRange < 0) {
			Log.warning(this.getLogPrefix() + "'gravity-chunk-range' cannot be negative.");
			gravityChunkRange = 0;
		}
		// Certain items cannot be of type AIR:
		if (shopCreationItem.getType() == Material.AIR) {
			Log.warning(this.getLogPrefix() + "'shop-creation-item' can not be AIR.");
			shopCreationItem = shopCreationItem.withType(Material.VILLAGER_SPAWN_EGG);
		}
		if (hireItem.getType() == Material.AIR) {
			Log.warning(this.getLogPrefix() + "'hire-item' can not be AIR.");
			hireItem = hireItem.withType(Material.EMERALD);
		}
		if (currencyItem.getType() == Material.AIR) {
			Log.warning(this.getLogPrefix() + "'currency-item' can not be AIR.");
			currencyItem = currencyItem.withType(Material.EMERALD);
		}
		if (namingOfPlayerShopsViaItem) {
			if (nameItem.getType() == Material.AIR) {
				Log.warning(this.getLogPrefix() + "'name-item' can not be AIR if naming-of-player-shops-via-item is enabled!");
				nameItem = nameItem.withType(Material.NAME_TAG);
			}
		}
		if (maxTradesPages < 1) {
			Log.warning(this.getLogPrefix() + "'max-trades-pages' can not be less than 1!");
			maxTradesPages = 1;
		} else if (maxTradesPages > 10) {
			Log.warning(this.getLogPrefix() + "'max-trades-pages' can not be greater than 10!");
			maxTradesPages = 10;
		}
		if (taxRate < 0) {
			Log.warning(this.getLogPrefix() + "'tax-rate' can not be less than 0!");
			taxRate = 0;
		} else if (taxRate > 100) {
			Log.warning(this.getLogPrefix() + "'tax-rate' can not be larger than 100!");
			taxRate = 100;
		}
	}
}
