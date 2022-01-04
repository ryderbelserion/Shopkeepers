package com.nisovin.shopkeepers;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.shopkeepers.api.events.ShopkeepersStartupEvent;
import com.nisovin.shopkeepers.api.internal.ApiInternals;
import com.nisovin.shopkeepers.api.internal.InternalShopkeepersAPI;
import com.nisovin.shopkeepers.api.internal.InternalShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.chatinput.ChatInput;
import com.nisovin.shopkeepers.commands.Commands;
import com.nisovin.shopkeepers.compat.MC_1_16;
import com.nisovin.shopkeepers.compat.MC_1_17;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.compat.ServerAssumptionsTest;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.lib.ConfigLoadException;
import com.nisovin.shopkeepers.container.protection.ProtectedContainers;
import com.nisovin.shopkeepers.container.protection.RemoveShopOnContainerBreak;
import com.nisovin.shopkeepers.debug.Debug;
import com.nisovin.shopkeepers.debug.events.EventDebugger;
import com.nisovin.shopkeepers.debug.trades.TradingCountListener;
import com.nisovin.shopkeepers.dependencies.worldguard.WorldGuardDependency;
import com.nisovin.shopkeepers.internals.SKApiInternals;
import com.nisovin.shopkeepers.itemconversion.ItemConversions;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.metrics.PluginMetrics;
import com.nisovin.shopkeepers.naming.ShopkeeperNaming;
import com.nisovin.shopkeepers.playershops.PlayerShops;
import com.nisovin.shopkeepers.shopcreation.ShopkeeperCreation;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.SKShopTypesRegistry;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.SKShopObjectTypesRegistry;
import com.nisovin.shopkeepers.shopobjects.citizens.CitizensShops;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.sign.SignShops;
import com.nisovin.shopkeepers.spigot.SpigotFeatures;
import com.nisovin.shopkeepers.storage.SKShopkeeperStorage;
import com.nisovin.shopkeepers.tradelog.TradeLoggers;
import com.nisovin.shopkeepers.tradenotifications.TradeNotifications;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.SKUIRegistry;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.java.ClassUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;
import com.nisovin.shopkeepers.villagers.RegularVillagers;

public class SKShopkeepersPlugin extends JavaPlugin implements InternalShopkeepersPlugin {

	private static final int ASYNC_TASKS_TIMEOUT_SECONDS = 10;

	private static SKShopkeepersPlugin plugin;

	public static SKShopkeepersPlugin getInstance() {
		return plugin;
	}

	private final ApiInternals apiInternals = new SKApiInternals();

	// Shop types and shop object types registry:
	private final SKShopTypesRegistry shopTypesRegistry = new SKShopTypesRegistry();
	private final SKShopObjectTypesRegistry shopObjectTypesRegistry = new SKShopObjectTypesRegistry();

	// Default shop and shop object types:
	private final SKDefaultShopTypes defaultShopTypes = new SKDefaultShopTypes();
	private final SKDefaultShopObjectTypes defaultShopObjectTypes = new SKDefaultShopObjectTypes(this);

	// UI registry:
	private final SKUIRegistry uiRegistry = new SKUIRegistry(this);
	private final SKDefaultUITypes defaultUITypes = new SKDefaultUITypes();

	// Shopkeeper registry:
	private final SKShopkeeperRegistry shopkeeperRegistry = new SKShopkeeperRegistry(this);

	// Shopkeeper storage:
	private final SKShopkeeperStorage shopkeeperStorage = new SKShopkeeperStorage(this);

	private final ItemConversions itemConversions = new ItemConversions(this);
	private final Commands commands = new Commands(this);
	private final ChatInput chatInput = new ChatInput(this);
	private final ShopkeeperNaming shopkeeperNaming = new ShopkeeperNaming(chatInput);
	private final ShopkeeperCreation shopkeeperCreation = new ShopkeeperCreation(this);
	private final TradeLoggers tradeLoggers = new TradeLoggers(this);
	private final TradeNotifications tradeNotifications = new TradeNotifications(this);
	private final EventDebugger eventDebugger = new EventDebugger(this);

	private final PlayerShops playerShops = new PlayerShops(this);

	private final ProtectedContainers protectedContainers = new ProtectedContainers(this);
	private final RemoveShopOnContainerBreak removeShopOnContainerBreak = new RemoveShopOnContainerBreak(this, protectedContainers);
	private final LivingShops livingShops = new LivingShops(this);
	private final SignShops signShops = new SignShops(this);
	private final CitizensShops citizensShops = new CitizensShops(this);

	private final RegularVillagers regularVillagers = new RegularVillagers(this);

	private final PluginMetrics pluginMetrics = new PluginMetrics(this);

	private boolean outdatedServer = false;
	private boolean incompatibleServer = false;
	private ConfigLoadException configLoadError = null; // null on success

	private void loadAllPluginClasses() {
		File pluginJarFile = this.getFile();
		long startNanos = System.nanoTime();
		boolean success = ClassUtils.loadAllClassesFromJar(pluginJarFile, className -> {
			// Skip version dependent classes:
			if (className.startsWith("com.nisovin.shopkeepers.compat.")) return false;
			// Skip classes that interact with optional dependencies:
			if (className.equals("com.nisovin.shopkeepers.dependencies.worldguard.WorldGuardDependency$Internal")) return false;
			if (className.equals("com.nisovin.shopkeepers.dependencies.citizens.CitizensUtils$Internal")) return false;
			if (className.equals("com.nisovin.shopkeepers.shopobjects.citizens.CitizensShopkeeperTrait")) return false;
			if (className.equals("com.nisovin.shopkeepers.spigot.text.SpigotText$Internal")) return false;
			return true;
		}, this.getLogger());
		if (success) {
			long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
			Log.info("Loaded all plugin classes (" + durationMillis + " ms).");
		}
	}

	// Returns true if server is outdated.
	private boolean isOutdatedServerVersion() {
		// Validate that this server is running a minimum required version:
		// TODO Add proper version parsing.
		/*String cbVersion = Utils.getServerCBVersion(); // E.g. 1_13_R2
		String bukkitVersion = Bukkit.getBukkitVersion(); // E.g. 1.13.1-R0.1-SNAPSHOT*/
		try {
			// This has been added with the recent changes to PlayerBedEnterEvent: TODO outdated
			Class.forName("org.bukkit.event.player.PlayerBedEnterEvent$BedEnterResult");
			return false;
		} catch (ClassNotFoundException e) {
			return true;
		}
	}

	// Returns false if no compatible NMS version, nor the fallback handler could be set up.
	private boolean setupNMS() {
		NMSManager.load(this);
		return (NMSManager.getProvider() != null);
	}

	private void registerDefaults() {
		Log.info("Registering defaults.");
		livingShops.onRegisterDefaults();
		uiRegistry.registerAll(defaultUITypes.getAllUITypes());
		shopTypesRegistry.registerAll(defaultShopTypes.getAll());
		shopObjectTypesRegistry.registerAll(defaultShopObjectTypes.getAll());
	}

	public SKShopkeepersPlugin() {
		super();
	}

	@Override
	public void onLoad() {
		Log.setLogger(this.getLogger()); // Set up logger early
		// Setting plugin reference early, so it is also available for any code running here:
		plugin = this;
		InternalShopkeepersAPI.enable(this);

		// Loading all plugin classes up front ensures that we don't run into missing classes (usually during shutdown)
		// when the plugin jar gets replaced during runtime (e.g. for hot reloads):
		this.loadAllPluginClasses();

		// Validate that this server is running a minimum required version:
		this.outdatedServer = this.isOutdatedServerVersion();
		if (this.outdatedServer) {
			return;
		}

		// Try to load suitable NMS (or fallback) code:
		this.incompatibleServer = !this.setupNMS();
		if (this.incompatibleServer) {
			return;
		}

		// Load config:
		this.configLoadError = Settings.loadConfig(this);
		if (this.configLoadError != null) {
			return;
		}

		// Load language file:
		Messages.loadLanguageFile();

		// WorldGuard only allows registering flags before it gets enabled.
		// Note: Changing the config setting has no effect until the next server restart or server reload.
		if (Settings.registerWorldGuardAllowShopFlag) {
			WorldGuardDependency.registerAllowShopFlag();
		}

		// Register defaults:
		this.registerDefaults();
	}

	@Override
	public void onEnable() {
		assert Log.getLogger() != null; // Log should already have been set up
		// Plugin instance and API might already have been set during onLoad:
		boolean alreadySetUp = true;
		if (plugin == null) {
			alreadySetUp = false;
			plugin = this;
			InternalShopkeepersAPI.enable(this);
		}

		// Validate that this server is running a minimum required version:
		if (this.outdatedServer) {
			Log.severe("Outdated server version (" + Bukkit.getVersion() + "): Shopkeepers cannot be enabled. Please update your server!");
			this.setEnabled(false); // also calls onDisable
			return;
		}

		// Check if the server version is incompatible:
		if (this.incompatibleServer) {
			Log.severe("Incompatible server version: Shopkeepers cannot be enabled.");
			this.setEnabled(false); // Also calls onDisable
			return;
		}

		// Load config (if not already loaded during onLoad):
		if (!alreadySetUp) {
			this.configLoadError = Settings.loadConfig(this);
		} else {
			Log.debug("Config already loaded.");
		}
		if (this.configLoadError != null) {
			Log.severe("Could not load the config!", configLoadError);
			this.setEnabled(false); // Also calls onDisable
			return;
		}

		// Load language file (if not already loaded during onLoad):
		if (!alreadySetUp) {
			Messages.loadLanguageFile();
		} else {
			Log.debug("Language file already loaded.");
		}

		// Check for and initialize version dependent utilities:
		MC_1_16.init();
		MC_1_17.init();

		// Inform about Spigot exclusive features:
		if (SpigotFeatures.isSpigotAvailable()) {
			Log.debug("Spigot-based server found: Enabling Spigot exclusive features.");
		} else {
			Log.info("No Spigot-based server found: Disabling Spigot exclusive features!");
		}

		// Test server assumptions:
		if (!ServerAssumptionsTest.run()) {
			if (Settings.ignoreFailedServerAssumptionTests) {
				Log.severe("Server incompatibility detected! But we continue to enable the plugin anyway,"
						+ " because setting 'ignore-failed-server-assumption-tests' is enabled."
						+ " Do not expect support for problems you encounter while running in this mode!");
			} else {
				Log.severe("Server incompatibility detected! Disabling the plugin!");
				this.setEnabled(false); // Also calls onDisable
				return;
			}
		}

		// Register defaults (if not already set up during onLoad):
		if (!alreadySetUp) {
			this.registerDefaults();
		} else {
			Log.debug("Defaults already registered.");
		}

		// Call startup event so that other plugins can make their registrations:
		// TODO This event doesn't make much sense, because dependent plugins are enabled after us, so they were not yet
		// able to register their event handlers.
		// An option could be to enable the Shopkeepers plugin 1 tick after all other plugins have been enabled. But
		// then any performance intensive startup tasks (loading shops, ..) would potentially be interpreted as lag by
		// the server.
		// Another option is for these plugins to perform their setup during onLoad (similar to how we register default
		// shop types, etc., during onLoad).
		Bukkit.getPluginManager().callEvent(new ShopkeepersStartupEvent());

		// Inform UI registry (registers UI event handlers):
		uiRegistry.onEnable();

		// Enable container protection:
		protectedContainers.enable();
		removeShopOnContainerBreak.onEnable();

		// Register events:
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new PlayerJoinQuitListener(this), this);
		new TradingCountListener(this).onEnable();

		// DEFAULT SHOP OBJECT TYPES

		// Enable living entity shops:
		livingShops.onEnable();

		// Enable sign shops:
		// Note: This has to be enabled before the shop creation listener, so that interactions with sign shops take
		// precedence over interactions with the shop creation item.
		signShops.onEnable();

		// Enable citizens shops:
		citizensShops.onEnable();

		// -----

		// Features related to regular villagers:
		regularVillagers.onEnable();

		// Item conversions:
		itemConversions.onEnable();

		// Enable commands:
		commands.onEnable();

		// Chat input:
		chatInput.onEnable();

		// Enable shopkeeper naming:
		shopkeeperNaming.onEnable();

		// Enable shopkeeper creation:
		shopkeeperCreation.onEnable();

		// Enable shopkeeper storage:
		shopkeeperStorage.onEnable();

		// Enable shopkeeper registry:
		shopkeeperRegistry.onEnable();

		// Debug log the registered shopkeeper data migrations:
		if (Debug.isDebugging()) {
			ShopkeeperDataMigrator.logRegisteredMigrations();
		}

		// Load shopkeepers from saved data:
		boolean loadingSuccessful = shopkeeperStorage.reload();
		if (!loadingSuccessful) {
			// Detected an issue during loading.
			// Disabling the plugin without saving, to prevent loss of shopkeeper data:
			Log.severe("Detected an issue during the loading of the saved shopkeepers data! Disabling the plugin!");
			shopkeeperStorage.disableSaving();
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// Activate (spawn) shopkeepers in loaded chunks of all loaded worlds:
		shopkeeperRegistry.getChunkActivator().activateShopkeepersInAllWorlds();

		// Player shops:
		playerShops.onEnable();

		// Trade loggers:
		tradeLoggers.onEnable();

		// Trade notifications:
		tradeNotifications.onEnable();

		// Save all updated shopkeeper data (e.g. after data migrations):
		shopkeeperStorage.saveIfDirty();

		// Plugin metrics:
		pluginMetrics.onEnable();

		// Event debugger:
		eventDebugger.onEnable();
	}

	@Override
	public void onDisable() {
		// Wait for async tasks to complete:
		SchedulerUtils.awaitAsyncTasksCompletion(this, ASYNC_TASKS_TIMEOUT_SECONDS, this.getLogger());

		// Inform UI registry about disable:
		uiRegistry.onDisable();

		// Deactivate (despawn) all shopkeepers (prior to saving shopkeepers data and before unloading all shopkeepers):
		shopkeeperRegistry.getChunkActivator().deactivateShopkeepersInAllWorlds();

		// Disable living entity shops:
		livingShops.onDisable();

		// Disable sign shops:
		signShops.onDisable();

		// Disable citizens shops:
		citizensShops.onDisable();

		// Disable protected containers:
		protectedContainers.disable();
		removeShopOnContainerBreak.onDisable();

		// Disable shopkeeper registry: Unloads all shopkeepers.
		shopkeeperRegistry.onDisable();

		// Shutdown shopkeeper storage (saves shopkeepers):
		shopkeeperStorage.onDisable();

		shopTypesRegistry.clearAllSelections();
		shopObjectTypesRegistry.clearAllSelections();

		// Disable commands:
		commands.onDisable();

		// Chat input:
		chatInput.onDisable();

		// Item conversions:
		itemConversions.onDisable();

		// Regular villagers:
		regularVillagers.onDisable();

		shopkeeperNaming.onDisable();
		shopkeeperCreation.onDisable();

		// Player shops:
		playerShops.onDisable();

		// Trade loggers:
		tradeLoggers.onDisable();

		// Trade notifications:
		tradeNotifications.onDisable();

		// Clear all types of registers:
		shopTypesRegistry.clearAll();
		shopObjectTypesRegistry.clearAll();
		uiRegistry.clearAll();

		// Plugin metrics:
		pluginMetrics.onDisable();

		// Event debugger:
		eventDebugger.onDisable();

		HandlerList.unregisterAll(this);
		Bukkit.getScheduler().cancelTasks(this);

		InternalShopkeepersAPI.disable();
		plugin = null;
	}

	/**
	 * Reloads the plugin.
	 */
	public void reload() {
		this.onDisable();
		this.onEnable();
	}

	// PLAYER JOINING AND QUITTING

	void onPlayerJoin(Player player) {
	}

	void onPlayerQuit(Player player) {
		// Player cleanup:
		shopTypesRegistry.clearSelection(player);
		shopObjectTypesRegistry.clearSelection(player);
		uiRegistry.onPlayerQuit(player);

		shopkeeperCreation.onPlayerQuit(player);
		commands.onPlayerQuit(player);
	}

	@Override
	public ApiInternals getApiInternals() {
		return apiInternals;
	}

	// SHOPKEEPER REGISTRY

	@Override
	public SKShopkeeperRegistry getShopkeeperRegistry() {
		return shopkeeperRegistry;
	}

	// SHOPKEEPER STORAGE

	@Override
	public SKShopkeeperStorage getShopkeeperStorage() {
		return shopkeeperStorage;
	}

	// COMMANDS

	public Commands getCommands() {
		return commands;
	}

	// CHAT INPUT

	public ChatInput getChatInput() {
		return chatInput;
	}

	// UI

	@Override
	public SKUIRegistry getUIRegistry() {
		return uiRegistry;
	}

	@Override
	public SKDefaultUITypes getDefaultUITypes() {
		return defaultUITypes;
	}

	// PROTECTED CONTAINERS

	public ProtectedContainers getProtectedContainers() {
		return protectedContainers;
	}

	// SHOPKEEPER REMOVAL ON CONTAINER BREAKING

	public RemoveShopOnContainerBreak getRemoveShopOnContainerBreak() {
		return removeShopOnContainerBreak;
	}

	// LIVING ENTITY SHOPS

	public LivingShops getLivingShops() {
		return livingShops;
	}

	// SIGN SHOPS

	public SignShops getSignShops() {
		return signShops;
	}

	// CITIZENS SHOPS

	public CitizensShops getCitizensShops() {
		return citizensShops;
	}

	// SHOP TYPES

	@Override
	public SKShopTypesRegistry getShopTypeRegistry() {
		return shopTypesRegistry;
	}

	@Override
	public SKDefaultShopTypes getDefaultShopTypes() {
		return defaultShopTypes;
	}

	// SHOP OBJECT TYPES

	@Override
	public SKShopObjectTypesRegistry getShopObjectTypeRegistry() {
		return shopObjectTypesRegistry;
	}

	@Override
	public SKDefaultShopObjectTypes getDefaultShopObjectTypes() {
		return defaultShopObjectTypes;
	}

	// SHOPKEEPER NAMING

	public ShopkeeperNaming getShopkeeperNaming() {
		return shopkeeperNaming;
	}

	// REGULAR VILLAGERS

	public RegularVillagers getRegularVillagers() {
		return regularVillagers;
	}

	// SHOPKEEPER CREATION

	public ShopkeeperCreation getShopkeeperCreation() {
		return shopkeeperCreation;
	}

	@Override
	public boolean hasCreatePermission(Player player) {
		if (player == null) return false;
		return (shopTypesRegistry.getSelection(player) != null) && (shopObjectTypesRegistry.getSelection(player) != null);
	}

	@Override
	public AbstractShopkeeper handleShopkeeperCreation(ShopCreationData shopCreationData) {
		Validate.notNull(shopCreationData, "shopCreationData is null");
		ShopType<?> rawShopType = shopCreationData.getShopType();
		Validate.isTrue(rawShopType instanceof AbstractShopType,
				"ShopType of shopCreationData is not of type AbstractShopType, but: " + rawShopType.getClass().getName());
		AbstractShopType<?> shopType = (AbstractShopType<?>) rawShopType;
		// Forward to shop type:
		return shopType.handleShopkeeperCreation(shopCreationData);
	}

	// PLAYER SHOPS

	public PlayerShops getPlayerShops() {
		return playerShops;
	}

	// TRADE NOTIFICATIONS

	public TradeNotifications getTradeNotifications() {
		return tradeNotifications;
	}
}
