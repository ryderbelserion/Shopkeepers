package com.nisovin.shopkeepers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.chestprotection.ProtectedChests;
import com.nisovin.shopkeepers.commands.Commands;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.metrics.CitizensChart;
import com.nisovin.shopkeepers.metrics.FeaturesChart;
import com.nisovin.shopkeepers.metrics.GringottsChart;
import com.nisovin.shopkeepers.metrics.PlayerShopsChart;
import com.nisovin.shopkeepers.metrics.ShopkeepersCountChart;
import com.nisovin.shopkeepers.metrics.TownyChart;
import com.nisovin.shopkeepers.metrics.VaultEconomyChart;
import com.nisovin.shopkeepers.metrics.WorldGuardChart;
import com.nisovin.shopkeepers.metrics.WorldsChart;
import com.nisovin.shopkeepers.naming.ShopkeeperNaming;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.shopcreation.ShopkeeperCreation;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.SKShopTypesRegistry;
import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.SKShopObjectTypesRegistry;
import com.nisovin.shopkeepers.shopobjects.citizens.CitizensShops;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.sign.SignShops;
import com.nisovin.shopkeepers.storage.SKShopkeeperStorage;
import com.nisovin.shopkeepers.tradelogging.TradeFileLogger;
import com.nisovin.shopkeepers.ui.SKUIRegistry;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.SchedulerUtils;
import com.nisovin.shopkeepers.util.TradingCountListener;
import com.nisovin.shopkeepers.villagers.BlockVillagerSpawnListener;
import com.nisovin.shopkeepers.villagers.VillagerInteractionListener;

public class SKShopkeepersPlugin extends JavaPlugin implements ShopkeepersPlugin {

	private static final int ASYNC_TASKS_TIMEOUT_SECONDS = 10;

	private static SKShopkeepersPlugin plugin;

	public static SKShopkeepersPlugin getInstance() {
		return plugin;
	}

	// shop types and shop object types registry:
	private final SKShopTypesRegistry shopTypesRegistry = new SKShopTypesRegistry();
	private final SKShopObjectTypesRegistry shopObjectTypesRegistry = new SKShopObjectTypesRegistry();

	// default shop and shop object types:
	private final SKDefaultShopTypes defaultShopTypes = new SKDefaultShopTypes();
	private final SKDefaultShopObjectTypes defaultShopObjectTypes = new SKDefaultShopObjectTypes(this);

	// ui registry:
	private final SKUIRegistry uiRegistry = new SKUIRegistry(this);
	private final SKDefaultUITypes defaultUITypes = new SKDefaultUITypes();

	// shopkeeper registry:
	private final SKShopkeeperRegistry shopkeeperRegistry = new SKShopkeeperRegistry(this);

	// shopkeeper storage:
	private final SKShopkeeperStorage shopkeeperStorage = new SKShopkeeperStorage(this);

	private final Commands commands = new Commands(this);
	private final ShopkeeperNaming shopkeeperNaming = new ShopkeeperNaming(this);
	private final ShopkeeperCreation shopkeeperCreation = new ShopkeeperCreation(this);

	private final ProtectedChests protectedChests = new ProtectedChests(this);
	private final LivingShops livingShops = new LivingShops(this);
	private final SignShops signShops = new SignShops(this);
	private final CitizensShops citizensShops = new CitizensShops(this);

	@Override
	public void onEnable() {
		plugin = this;
		ShopkeepersAPI.enable(this);

		// making sure that certain classes, that are needed during shutdown, are loaded:
		// this helps for hot reloads (when the plugin gets disabled, but the original jar got replaced and is therefore
		// no longer available)
		// TODO pre-load all classes?
		try {
			Class.forName(SchedulerUtils.class.getName());
			Class.forName(ShopkeeperRemoveEvent.class.getName());
			Class.forName(ShopkeeperRemoveEvent.Cause.class.getName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// validate that this is running a minimum required version of Spigot:
		// TODO add proper version parsing
		/*String cbVersion = Utils.getServerCBVersion(); // 1_13_R2
		String bukkitVersion = Bukkit.getBukkitVersion(); // 1.13.1-R0.1-SNAPSHOT*/
		try {
			// this has been added with the corresponding new ray tracing functions:
			Class.forName("org.bukkit.util.RayTraceResult");
		} catch (ClassNotFoundException e1) {
			Log.severe("Outdated server version (" + Bukkit.getVersion()
					+ "): Shopkeepers cannot be enabled. Please update your server!");
			this.setEnabled(false);
			return;
		}

		// try to load suitable NMS code:
		NMSManager.load(this);
		if (NMSManager.getProvider() == null) {
			Log.severe("Incompatible server version: Shopkeepers cannot be enabled.");
			this.setEnabled(false);
			return;
		}

		// load config:
		File file = new File(this.getDataFolder(), "config.yml");
		if (!file.exists()) {
			this.saveDefaultConfig();
		}
		this.reloadConfig();
		Configuration config = this.getConfig();
		boolean configChanged = Settings.loadConfiguration(config);
		if (configChanged) {
			// if missing settings were added -> save the modified config
			// TODO persist comments somehow
			this.saveConfig();
		}

		// load language config:
		String lang = Settings.language;
		File langFile = new File(this.getDataFolder(), "language-" + lang + ".yml");
		if (!langFile.exists() && this.getResource("language-" + lang + ".yml") != null) {
			this.saveResource("language-" + lang + ".yml", false);
		}
		if (langFile.exists()) {
			try {
				YamlConfiguration langConfig = new YamlConfiguration();
				langConfig.load(langFile);
				Settings.loadLanguageConfiguration(langConfig);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// process additional permissions
		String[] perms = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : perms) {
			if (Bukkit.getPluginManager().getPermission("shopkeeper.maxshops." + perm) == null) {
				Bukkit.getPluginManager().addPermission(new Permission("shopkeeper.maxshops." + perm, PermissionDefault.FALSE));
			}
		}

		// inform ui registry (registers ui event handlers):
		uiRegistry.onEnable();
		uiRegistry.registerAll(defaultUITypes.getAllUITypes());

		// enable ProtectedChests:
		protectedChests.enable();

		// register events:
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new PlayerJoinQuitListener(this), this);
		pm.registerEvents(new TradingCountListener(this), this);
		pm.registerEvents(new TradeFileLogger(this.getDataFolder()), this);

		// DEFAULT SHOP TYPES

		shopTypesRegistry.registerAll(defaultShopTypes.getAll());

		// DEFAULT SHOP OBJECT TYPES

		// enable living entity shops:
		livingShops.onEnable();

		// enable sign shops:
		signShops.onEnable();

		// enable citizens shops:
		citizensShops.onEnable();

		// register default shop object types:
		shopObjectTypesRegistry.registerAll(defaultShopObjectTypes.getAll());

		//

		// handling of regular villagers:
		pm.registerEvents(new VillagerInteractionListener(this), this);
		if (Settings.blockVillagerSpawns) {
			pm.registerEvents(new BlockVillagerSpawnListener(), this);
		}

		// enable commands:
		commands.onEnable();

		// enable shopkeeper naming:
		shopkeeperNaming.onEnable();

		// enable shopkeeper creation:
		shopkeeperCreation.onEnable();

		// enable shopkeeper storage:
		shopkeeperStorage.onEnable();

		// enable shopkeeper registry:
		shopkeeperRegistry.onEnable();

		// load shopkeepers from saved data:
		boolean loadingSuccessful = shopkeeperStorage.reload();
		if (!loadingSuccessful) {
			// detected an issue during loading
			// disabling the plugin without saving, to prevent loss of shopkeeper data:
			Log.severe("Detected an issue during the loading of the shopkeepers data! Disabling the plugin!");
			shopkeeperStorage.disableSaving();
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// activate (spawn) shopkeepers in loaded chunks:
		shopkeeperRegistry.loadShopkeepersInAllWorlds();

		Bukkit.getScheduler().runTaskLater(this, () -> {
			// remove inactive player shopkeepers:
			this.removeInactivePlayerShops();
		}, 5L);

		// let's update the shopkeepers for all already online players:
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (CitizensHandler.isNPC(player)) continue;
			this.updateShopkeepersForPlayer(player.getUniqueId(), player.getName());
		}

		// write back all updated data:
		if (shopkeeperStorage.isDirty()) {
			shopkeeperStorage.saveNow();
		}

		// setup metrics:
		if (Settings.enableMetrics) {
			this.setupMetrics();
		}
	}

	@Override
	public void onDisable() {
		// wait for async tasks to complete:
		SchedulerUtils.awaitAsyncTasksCompletion(this, ASYNC_TASKS_TIMEOUT_SECONDS, this.getLogger());

		// inform ui registry about disable:
		uiRegistry.onDisable();

		// despawn all active shopkeepers:
		// TODO really required here? maybe replace with deactivate all, and also prevent re-activation during disable
		// (due to new chunk loads)?
		shopkeeperRegistry.despawnAllShopkeepers();

		// disable living entity shops:
		livingShops.onDisable();

		// disable sign shops:
		signShops.onDisable();

		// disable citizens shops:
		citizensShops.onDisable();

		// save shopkeepers:
		shopkeeperStorage.saveImmediateIfDirty();

		// disable protected chests:
		protectedChests.disable();

		// disable registry:
		shopkeeperRegistry.onDisable();

		// disable storage:
		shopkeeperStorage.onDisable();

		shopTypesRegistry.clearAllSelections();
		shopObjectTypesRegistry.clearAllSelections();

		// disable commands:
		commands.onDisable();

		shopkeeperNaming.onDisable();
		shopkeeperCreation.onDisable();

		// clear all types of registers:
		shopTypesRegistry.clearAll();
		shopObjectTypesRegistry.clearAll();
		uiRegistry.clearAll();

		HandlerList.unregisterAll(this);
		Bukkit.getScheduler().cancelTasks(this);

		ShopkeepersAPI.disable();
		plugin = null;
	}

	/**
	 * Reloads the plugin.
	 */
	public void reload() {
		this.onDisable();
		this.onEnable();
	}

	// METRICS

	private void setupMetrics() {
		Metrics metrics = new Metrics(this);
		metrics.addCustomChart(new CitizensChart());
		metrics.addCustomChart(new WorldGuardChart());
		metrics.addCustomChart(new TownyChart());
		metrics.addCustomChart(new VaultEconomyChart());
		metrics.addCustomChart(new GringottsChart());
		metrics.addCustomChart(new ShopkeepersCountChart(shopkeeperRegistry));
		metrics.addCustomChart(new PlayerShopsChart(shopkeeperRegistry));
		metrics.addCustomChart(new FeaturesChart());
		metrics.addCustomChart(new WorldsChart(shopkeeperRegistry));
	}

	// PLAYER JOINING AND QUITTING

	void onPlayerJoin(Player player) {
		this.updateShopkeepersForPlayer(player.getUniqueId(), player.getName());
	}

	void onPlayerQuit(Player player) {
		// player cleanup:
		shopTypesRegistry.clearSelection(player);
		shopObjectTypesRegistry.clearSelection(player);
		uiRegistry.onInventoryClose(player);

		shopkeeperNaming.onPlayerQuit(player);
		shopkeeperCreation.onPlayerQuit(player);
		commands.onPlayerQuit(player);
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

	// UI

	@Override
	public SKUIRegistry getUIRegistry() {
		return uiRegistry;
	}

	@Override
	public SKDefaultUITypes getDefaultUITypes() {
		return defaultUITypes;
	}

	// PROTECTED CHESTS:

	public ProtectedChests getProtectedChests() {
		return protectedChests;
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

	/**
	 * Gets the default shop object type.
	 * 
	 * <p>
	 * Usually this will be the villager entity shop object type. However, there are no guarantees that this might not
	 * get changed or be configurable in the future.
	 * 
	 * @return the default shop object type
	 */
	public AbstractShopObjectType<?> getDefaultShopObjectType() {
		// default: villager entity shop object type:
		return this.getDefaultShopObjectTypes().getLivingShopObjectTypes().get(EntityType.VILLAGER);
	}

	// SHOPKEEPER NAMING

	public ShopkeeperNaming getShopkeeperNaming() {
		return shopkeeperNaming;
	}

	// SHOPKEEPER CREATION:

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
		Validate.notNull(shopCreationData, "CreationData is null!");
		ShopType<?> rawShopType = shopCreationData.getShopType();
		Validate.isTrue(rawShopType instanceof AbstractShopType,
				"Expecting an AbstractShopType, got " + rawShopType.getClass().getName());
		AbstractShopType<?> shopType = (AbstractShopType<?>) rawShopType;
		// forward to shop type:
		return shopType.handleShopkeeperCreation(shopCreationData);
	}

	// INACTIVE SHOPS

	private void removeInactivePlayerShops() {
		if (Settings.playerShopkeeperInactiveDays <= 0) return;

		Set<UUID> playerUUIDs = new HashSet<>();
		for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
				playerUUIDs.add(playerShop.getOwnerUUID());
			}
		}
		if (playerUUIDs.isEmpty()) {
			// no player shops found:
			return;
		}

		// fetch OfflinePlayers async:
		int playerShopkeeperInactiveDays = Settings.playerShopkeeperInactiveDays;
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			List<OfflinePlayer> inactivePlayers = new ArrayList<>();
			long now = System.currentTimeMillis();
			for (UUID uuid : playerUUIDs) {
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
				if (!offlinePlayer.hasPlayedBefore()) continue;

				long lastPlayed = offlinePlayer.getLastPlayed();
				if ((lastPlayed > 0) && ((now - lastPlayed) / 86400000 > playerShopkeeperInactiveDays)) {
					inactivePlayers.add(offlinePlayer);
				}
			}

			if (inactivePlayers.isEmpty()) {
				// no inactive players found:
				return;
			}

			// continue in main thread:
			SchedulerUtils.runTaskOrOmit(SKShopkeepersPlugin.this, () -> {
				List<PlayerShopkeeper> forRemoval = new ArrayList<>();
				for (OfflinePlayer inactivePlayer : inactivePlayers) {
					// remove all shops of this inactive player:
					UUID playerUUID = inactivePlayer.getUniqueId();

					for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
						if (shopkeeper instanceof PlayerShopkeeper) {
							PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
							UUID ownerUUID = playerShop.getOwnerUUID();
							if (ownerUUID.equals(playerUUID)) {
								forRemoval.add(playerShop);
							}
						}
					}
				}

				// remove those shopkeepers:
				if (!forRemoval.isEmpty()) {
					for (PlayerShopkeeper shopkeeper : forRemoval) {
						shopkeeper.delete();
						Log.info("Shopkeeper owned by " + shopkeeper.getOwnerAsString() + " at "
								+ shopkeeper.getPositionString() + " has been removed for owner inactivity.");
					}

					// save:
					shopkeeperStorage.save();
				}
			});
		});
	}

	// HANDLING PLAYER NAME CHANGES:

	// updates owner names for the shopkeepers of the specified player:
	private void updateShopkeepersForPlayer(UUID playerUUID, String playerName) {
		boolean dirty = false;
		for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
				UUID ownerUUID = playerShop.getOwnerUUID();
				String ownerName = playerShop.getOwnerName();

				if (ownerUUID.equals(playerUUID)) {
					if (!ownerName.equals(playerName)) {
						// update the stored name, because the player must have changed it:
						playerShop.setOwner(playerUUID, playerName);
						dirty = true;
					} else {
						// The shop was already updated to uuid based identification and the player's name hasn't
						// changed.
						// If we assume that this is consistent among all shops of this player
						// we can stop checking the other shops here:
						return;
					}
				}
			}
		}

		if (dirty) {
			shopkeeperStorage.save();
		}
	}

	//

	@Override
	public TradingRecipe createTradingRecipe(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		return new SKTradingRecipe(resultItem, item1, item2);
	}
}
