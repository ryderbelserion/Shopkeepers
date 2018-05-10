package com.nisovin.shopkeepers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.nisovin.shopkeepers.abstractTypes.SelectableTypeRegistry;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.events.CreatePlayerShopkeeperEvent;
import com.nisovin.shopkeepers.events.ShopkeeperCreatedEvent;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.pluginhandlers.TownyHandler;
import com.nisovin.shopkeepers.pluginhandlers.WorldGuardHandler;
import com.nisovin.shopkeepers.shopobjects.CitizensShop;
import com.nisovin.shopkeepers.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.SignShop;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityShop;
import com.nisovin.shopkeepers.shoptypes.DefaultShopTypes;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;
import com.nisovin.shopkeepers.tradelogging.TradeFileLogger;
import com.nisovin.shopkeepers.ui.UIManager;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;
import com.nisovin.shopkeepers.util.ChunkCoords;
import com.nisovin.shopkeepers.util.SchedulerUtils;
import com.nisovin.shopkeepers.util.Utils;

public class ShopkeepersPlugin extends JavaPlugin implements ShopkeepersAPI {

	private static final long ASYNC_TASKS_TIMEOUT_SECONDS = 10L;

	private static ShopkeepersPlugin plugin;

	public static ShopkeepersPlugin getInstance() {
		return plugin;
	}

	// shop types manager:
	private final SelectableTypeRegistry<ShopType<?>> shopTypesManager = new SelectableTypeRegistry<ShopType<?>>() {

		@Override
		protected String getTypeName() {
			return "shop type";
		}

		@Override
		public boolean canBeSelected(Player player, ShopType<?> type) {
			// TODO This currently skips the admin shop type. Maybe included the admin shop types here for players
			// which are admins, because there /could/ be different types of admin shops in the future (?)
			return super.canBeSelected(player, type) && type.isPlayerShopType();
		}
	};

	// shop object types manager:
	private final SelectableTypeRegistry<ShopObjectType> shopObjectTypesManager = new SelectableTypeRegistry<ShopObjectType>() {

		@Override
		protected String getTypeName() {
			return "shop object type";
		}
	};

	// default shop and shop object types:
	private DefaultShopTypes defaultShopTypes;
	private DefaultShopObjectTypes defaultShopObjectTypes;

	// ui manager:
	private final UIManager uiManager = new UIManager();

	// all shopkeepers:
	private final Map<UUID, Shopkeeper> shopkeepersById = new LinkedHashMap<UUID, Shopkeeper>();
	private final Collection<Shopkeeper> allShopkeepersView = Collections.unmodifiableCollection(shopkeepersById.values());
	private int nextShopSessionId = 1;
	private final Map<Integer, Shopkeeper> shopkeepersBySessionId = new LinkedHashMap<Integer, Shopkeeper>();
	private final Map<ChunkCoords, List<Shopkeeper>> shopkeepersByChunk = new HashMap<ChunkCoords, List<Shopkeeper>>();
	private final Map<String, Shopkeeper> activeShopkeepers = new HashMap<String, Shopkeeper>(); // TODO remove this (?)
	private final Collection<Shopkeeper> activeShopkeepersView = Collections.unmodifiableCollection(activeShopkeepers.values());

	private final Map<String, ConfirmEntry> confirming = new HashMap<String, ConfirmEntry>();
	private final Map<String, Shopkeeper> naming = Collections.synchronizedMap(new HashMap<String, Shopkeeper>());
	private final Map<String, List<String>> recentlyPlacedChests = new HashMap<String, List<String>>();
	private final Map<String, Block> selectedChest = new HashMap<String, Block>();

	// protected chests:
	private final ProtectedChests protectedChests = new ProtectedChests();

	// saving:
	// flag to (temporary) turn off saving
	private boolean skipSaving = false;
	private long lastSavingErrorMsgTimeStamp = 0L;
	private boolean dirty = false;
	private int delayedSaveTaskId = -1;
	// the task which performs async file io during a save
	private int saveIOTask = -1;
	// determines if there was another saveReal()-request while the saveIOTask was still in progress
	private boolean saveRealAgain = false;

	// listeners:
	private CreatureForceSpawnListener creatureForceSpawnListener = null;
	private SignShopListener signShopListener = null;

	@Override
	public void onEnable() {
		plugin = this;

		// reset a bunch of variables:
		skipSaving = false;
		lastSavingErrorMsgTimeStamp = 0L;
		dirty = false;
		delayedSaveTaskId = -1;
		saveIOTask = -1;
		saveRealAgain = false;

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
		boolean settingsMissing = Settings.loadConfiguration(config);
		if (settingsMissing) {
			// if settings were missing -> add those to the config and save it
			// TODO persist comments somehow
			this.saveConfig();
		}

		// load lang config:
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

		// initialize default shop and shop object types (after config has been loaded):
		defaultShopTypes = new DefaultShopTypes();
		defaultShopObjectTypes = new DefaultShopObjectTypes();

		// register default stuff:
		shopTypesManager.registerAll(defaultShopTypes.getAllShopTypes());
		shopObjectTypesManager.registerAll(defaultShopObjectTypes.getAllObjectTypes());
		uiManager.registerAll(DefaultUIs.getAllUITypes());

		// inform ui manager (registers ui event handlers):
		uiManager.onEnable(this);

		// inform ProtectedChests:
		protectedChests.onEnable(this);

		// register events
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new PluginListener(), this);
		pm.registerEvents(new WorldListener(this), this);
		pm.registerEvents(new PlayerJoinQuitListener(this), this);
		pm.registerEvents(new ShopNamingListener(this), this);
		pm.registerEvents(new ChestListener(this), this);
		pm.registerEvents(new CreateListener(this), this);
		pm.registerEvents(new VillagerInteractionListener(this), this);
		pm.registerEvents(new LivingEntityShopListener(this), this);
		pm.registerEvents(new TradeFileLogger(this.getDataFolder()), this);

		if (Settings.enableSignShops) {
			this.signShopListener = new SignShopListener(this);
			pm.registerEvents(signShopListener, this);
		}

		// enable citizens handler:
		CitizensHandler.enable();

		if (Settings.blockVillagerSpawns) {
			pm.registerEvents(new BlockVillagerSpawnListener(), this);
		}

		if (Settings.protectChests) {
			pm.registerEvents(new ChestProtectListener(this), this);
		}
		if (Settings.deleteShopkeeperOnBreakChest) {
			pm.registerEvents(new RemoveShopOnChestBreakListener(this), this);
		}

		// register force-creature-spawn event handler:
		if (Settings.bypassSpawnBlocking) {
			creatureForceSpawnListener = new CreatureForceSpawnListener();
			Bukkit.getPluginManager().registerEvents(creatureForceSpawnListener, this);
		}

		// register command handler:
		CommandManager commandManager = new CommandManager(this);
		this.getCommand("shopkeeper").setExecutor(commandManager);

		// load shopkeeper saved data:
		boolean loadError = false;
		try {
			loadError = !this.load();
		} catch (Exception e) {
			Log.severe("Something completely unexpected went wrong during loading of the save data!");
			e.printStackTrace();
			loadError = true;
		}
		if (loadError) {
			// detected issue during loading, disable plugin without saving, to prevent loss of shopkeeper data:
			Log.severe("Detected an issue during loading of the shopkeeper data! Disabling plugin!");
			skipSaving = true;
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// activate (spawn) shopkeepers in loaded chunks:
		for (World world : Bukkit.getWorlds()) {
			this.loadShopkeepersInWorld(world);
		}

		Bukkit.getScheduler().runTaskLater(this, new Runnable() {

			@Override
			public void run() {
				// remove invalid citizens shopkeepers:
				CitizensHandler.removeInvalidCitizensShopkeepers();
				// remove inactive player shopkeepers:
				removeInactivePlayerShops();
			}
		}, 5L);

		// start teleporter task:
		Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
			public void run() {
				List<Shopkeeper> readd = new ArrayList<Shopkeeper>();
				Iterator<Map.Entry<String, Shopkeeper>> iter = activeShopkeepers.entrySet().iterator();
				while (iter.hasNext()) {
					Shopkeeper shopkeeper = iter.next().getValue();
					boolean update = shopkeeper.check();
					if (update) {
						// if the shopkeeper had to be respawned its shop id changed:
						// this removes the entry which was stored with the old shop id and later adds back the
						// shopkeeper with it's new id
						readd.add(shopkeeper);
						iter.remove();
					}
				}
				if (!readd.isEmpty()) {
					for (Shopkeeper shopkeeper : readd) {
						if (shopkeeper.isActive()) {
							_activateShopkeeper(shopkeeper);
						}
					}

					// shopkeepers might have been respawned, request save:
					save();
				}
			}
		}, 200, 200); // 10 seconds

		// start verifier task:
		if (Settings.enableSpawnVerifier) {
			Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
				public void run() {
					int count = 0;
					for (Entry<ChunkCoords, List<Shopkeeper>> chunkEntry : shopkeepersByChunk.entrySet()) {
						ChunkCoords chunk = chunkEntry.getKey();
						if (chunk.isChunkLoaded()) {
							List<Shopkeeper> shopkeepers = chunkEntry.getValue();
							for (Shopkeeper shopkeeper : shopkeepers) {
								if (shopkeeper.needsSpawning() && !shopkeeper.isActive()) {
									// deactivate by old object id:
									_deactivateShopkeeper(shopkeeper);

									boolean spawned = shopkeeper.spawn();
									if (spawned) {
										// activate with new object id:
										_activateShopkeeper(shopkeeper);
										count++;
									} else {
										Log.debug("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
									}
								}
							}
						}
					}
					if (count > 0) {
						Log.debug("Spawn verifier: " + count + " shopkeepers respawned");
						save();
					}
				}
			}, 600, 1200); // 30,60 seconds
		}

		// start save task:
		if (!Settings.saveInstantly) {
			Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
				public void run() {
					if (dirty) {
						saveReal();
					}
				}
			}, 6000, 6000); // 5 minutes
		}

		// let's update the shopkeepers for all already online players:
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (Utils.isNPC(player)) continue;
			this.updateShopkeepersForPlayer(player.getUniqueId(), player.getName());
		}
	}

	@Override
	public void onDisable() {
		// wait for async tasks to complete:
		final long asyncTasksTimeoutMillis = ASYNC_TASKS_TIMEOUT_SECONDS * 1000L;
		final long asyncTasksStart = System.nanoTime();
		int activeAsyncTasks = SchedulerUtils.getActiveAsyncTasks(this);
		if (activeAsyncTasks > 0) {
			Log.info("Waiting up to " + ASYNC_TASKS_TIMEOUT_SECONDS + " seconds for "
					+ activeAsyncTasks + " remaining async tasks to finish ..");
		}
		while (SchedulerUtils.getActiveAsyncTasks(this) > 0) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			final long asyncTasksWaitedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - asyncTasksStart);
			if (asyncTasksWaitedMillis > asyncTasksTimeoutMillis) {
				// timeout reached, disable anyways..
				break;
			}
		}
		final long asyncTasksWaitedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - asyncTasksStart);
		if (asyncTasksWaitedMillis > 1) {
			Log.info("Waited " + asyncTasksWaitedMillis + " ms for async tasks to finish.");
		}
		activeAsyncTasks = SchedulerUtils.getActiveAsyncTasks(this);
		if (activeAsyncTasks > 0) {
			Log.severe("There are still " + activeAsyncTasks + " async tasks remaining! Disabling anyways now ..");
		}

		// close all open windows:
		uiManager.closeAll();
		// inform ui manager about disable:
		uiManager.onDisable(this);

		// despawn shopkeepers:
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
			shopkeeper.despawn();
		}

		// disable citizens handler:
		CitizensHandler.disable();

		// save:
		if (dirty) {
			this.saveReal(false); // not async here
		}

		// inform ProtectedChests:
		protectedChests.onDisable(this);

		// cleanup:
		activeShopkeepers.clear();
		shopkeepersByChunk.clear();
		shopkeepersById.clear();
		shopkeepersBySessionId.clear();
		nextShopSessionId = 1;

		shopTypesManager.clearAllSelections();
		shopObjectTypesManager.clearAllSelections();

		confirming.clear();
		naming.clear();
		selectedChest.clear();

		// clear all types of registers:
		shopTypesManager.clearAll();
		shopObjectTypesManager.clearAll();
		uiManager.clearAll();

		HandlerList.unregisterAll(this);
		Bukkit.getScheduler().cancelTasks(this);

		plugin = null;
	}

	/**
	 * Reloads the plugin.
	 */
	public void reload() {
		this.onDisable();
		this.onEnable();
	}

	void onPlayerQuit(Player player) {
		String playerName = player.getName();
		shopTypesManager.clearSelection(player);
		shopObjectTypesManager.clearSelection(player);
		uiManager.onInventoryClose(player);

		selectedChest.remove(playerName);
		recentlyPlacedChests.remove(playerName);
		naming.remove(playerName);
		this.endConfirmation(player);
	}

	// bypassing creature blocking plugins ('region protection' plugins):
	public void forceCreatureSpawn(Location location, EntityType entityType) {
		if (creatureForceSpawnListener != null && Settings.bypassSpawnBlocking) {
			creatureForceSpawnListener.forceCreatureSpawn(location, entityType);
		}
	}

	public void cancelNextBlockPhysics(Location location) {
		if (signShopListener != null) {
			signShopListener.cancelNextBlockPhysics(location);
		}
	}

	// UI

	public UIManager getUIManager() {
		return uiManager;
	}

	// PROTECTED CHESTS:

	public ProtectedChests getProtectedChests() {
		return protectedChests;
	}

	// SHOP TYPES

	public SelectableTypeRegistry<ShopType<?>> getShopTypeRegistry() {
		return shopTypesManager;
	}

	public DefaultShopTypes getDefaultShopTypes() {
		return defaultShopTypes;
	}

	// SHOP OBJECT TYPES

	public SelectableTypeRegistry<ShopObjectType> getShopObjectTypeRegistry() {
		return shopObjectTypesManager;
	}

	public DefaultShopObjectTypes getDefaultShopObjectTypes() {
		return defaultShopObjectTypes;
	}

	/**
	 * Gets the default shop object type.
	 * 
	 * <p>
	 * Usually this will be the villager entity shop object type.<br>
	 * However, there are no guarantees that this might not get changed or be configurable in the future.
	 * </p>
	 * 
	 * @return the default shop object type
	 */
	public ShopObjectType getDefaultShopObjectType() {
		// default: villager entity shop object type:
		return this.getDefaultShopObjectTypes().getLivingEntityObjectTypes().getObjectType(EntityType.VILLAGER);
	}

	// RECENTLY PLACED CHESTS

	void onChestPlacement(Player player, Block chest) {
		assert player != null && chest != null && Utils.isChest(chest.getType());
		String playerName = player.getName();
		List<String> recentlyPlaced = recentlyPlacedChests.get(playerName);
		if (recentlyPlaced == null) {
			recentlyPlaced = new LinkedList<String>();
			recentlyPlacedChests.put(playerName, recentlyPlaced);
		}
		recentlyPlaced.add(chest.getWorld().getName() + "," + chest.getX() + "," + chest.getY() + "," + chest.getZ());
		if (recentlyPlaced.size() > 5) {
			recentlyPlaced.remove(0);
		}
	}

	public boolean wasRecentlyPlaced(Player player, Block chest) {
		assert player != null && chest != null && Utils.isChest(chest.getType());
		String playerName = player.getName();
		List<String> recentlyPlaced = recentlyPlacedChests.get(playerName);
		return recentlyPlaced != null && recentlyPlaced.contains(chest.getWorld().getName() + "," + chest.getX() + "," + chest.getY() + "," + chest.getZ());
	}

	// SELECTED CHEST

	void selectChest(Player player, Block chest) {
		assert player != null;
		String playerName = player.getName();
		if (chest == null) selectedChest.remove(playerName);
		else {
			assert Utils.isChest(chest.getType());
			selectedChest.put(playerName, chest);
		}
	}

	public Block getSelectedChest(Player player) {
		assert player != null;
		return selectedChest.get(player.getName());
	}

	// COMMAND CONFIRMING

	void waitForConfirm(final Player player, Runnable action, int delay) {
		assert player != null && delay > 0;
		int taskId = new BukkitRunnable() {

			@Override
			public void run() {
				endConfirmation(player);
				Utils.sendMessage(player, Settings.msgConfirmationExpired);
			}
		}.runTaskLater(this, delay).getTaskId();
		ConfirmEntry oldEntry = confirming.put(player.getName(), new ConfirmEntry(action, taskId));
		if (oldEntry != null) {
			// end old confirmation task:
			Bukkit.getScheduler().cancelTask(oldEntry.getTaskId());
		}
	}

	Runnable endConfirmation(Player player) {
		ConfirmEntry entry = confirming.remove(player.getName());
		if (entry != null) {
			// end confirmation task:
			Bukkit.getScheduler().cancelTask(entry.getTaskId());

			// return action:
			return entry.getAction();
		}
		return null;
	}

	void onConfirm(Player player) {
		assert player != null;
		Runnable action = this.endConfirmation(player);
		if (action != null) {
			// execute confirmed task:
			action.run();
		} else {
			Utils.sendMessage(player, Settings.msgNothingToConfirm);
		}
	}

	// SHOPKEEPER NAMING

	void onNaming(Player player, Shopkeeper shopkeeper) {
		assert player != null && shopkeeper != null;
		naming.put(player.getName(), shopkeeper);
	}

	Shopkeeper getCurrentlyNamedShopkeeper(Player player) {
		assert player != null;
		return naming.get(player.getName());
	}

	boolean isNaming(Player player) {
		assert player != null;
		return this.getCurrentlyNamedShopkeeper(player) != null;
	}

	Shopkeeper endNaming(Player player) {
		assert player != null;
		return naming.remove(player.getName());
	}

	// SHOPKEEPER MEMORY STORAGE

	private void addShopkeeperToChunk(Shopkeeper shopkeeper, ChunkCoords chunkCoords) {
		List<Shopkeeper> list = shopkeepersByChunk.get(chunkCoords);
		if (list == null) {
			list = new ArrayList<Shopkeeper>();
			shopkeepersByChunk.put(chunkCoords, list);
		}
		list.add(shopkeeper);
	}

	private void removeShopkeeperFromChunk(Shopkeeper shopkeeper, ChunkCoords chunkCoords) {
		List<Shopkeeper> byChunk = shopkeepersByChunk.get(chunkCoords);
		if (byChunk == null) return;
		if (byChunk.remove(shopkeeper) && byChunk.isEmpty()) {
			shopkeepersByChunk.remove(chunkCoords);
		}
	}

	// this needs to be called right after a new shopkeeper was created..
	void registerShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		// assert !this.isRegistered(shopkeeper);

		// add default trading handler, if none is provided:
		if (shopkeeper.getUIHandler(DefaultUIs.TRADING_WINDOW) == null) {
			shopkeeper.registerUIHandler(new TradingHandler(DefaultUIs.TRADING_WINDOW, shopkeeper));
		}

		// store by unique id:
		shopkeepersById.put(shopkeeper.getUniqueId(), shopkeeper);

		// assign session id:
		int shopSessionId = nextShopSessionId;
		nextShopSessionId++;
		shopkeepersBySessionId.put(shopSessionId, shopkeeper);

		// inform shopkeeper:
		shopkeeper.onRegistration(shopSessionId);

		// add shopkeeper to chunk:
		ChunkCoords chunkCoords = shopkeeper.getChunkCoords();
		this.addShopkeeperToChunk(shopkeeper, chunkCoords);

		// activate shopkeeper:
		if (!shopkeeper.needsSpawning()) {
			// activate shopkeeper once at registration:
			this._activateShopkeeper(shopkeeper);
		} else if (chunkCoords.isChunkLoaded()) {
			// activate shopkeeper due to loaded chunk:
			this.activateShopkeeper(shopkeeper);
		}
	}

	@Override
	public Shopkeeper getShopkeeper(UUID shopkeeperUUID) {
		return shopkeepersById.get(shopkeeperUUID);
	}

	@Override
	public Shopkeeper getShopkeeper(int shopkeeperSessionId) {
		return shopkeepersBySessionId.get(shopkeeperSessionId);
	}

	@Override
	public Shopkeeper getShopkeeperByName(String shopName) {
		if (shopName == null) return null;
		shopName = ChatColor.stripColor(shopName);
		for (Shopkeeper shopkeeper : this.getAllShopkeepers()) {
			String shopkeeperName = shopkeeper.getName();
			if (shopkeeperName != null && ChatColor.stripColor(shopkeeperName).equalsIgnoreCase(shopName)) {
				return shopkeeper;
			}
		}
		return null;
	}

	@Override
	public Shopkeeper getShopkeeperByEntity(Entity entity) {
		if (entity == null) return null;
		// check if the entity is a living entity shopkeeper:
		Shopkeeper shopkeeper = this.getLivingEntityShopkeeper(entity);
		if (shopkeeper != null) return shopkeeper;
		// check if the entity is a citizens npc shopkeeper:
		return this.getCitizensShopkeeper(entity);
	}

	public Shopkeeper getLivingEntityShopkeeper(Entity entity) {
		if (entity == null) return null;
		return this.getActiveShopkeeper(LivingEntityShop.getId(entity));
	}

	public Shopkeeper getCitizensShopkeeper(Entity entity) {
		if (entity == null) return null;
		Integer npcId = CitizensHandler.getNPCId(entity);
		if (npcId == null) return null;
		return this.getActiveShopkeeper(CitizensShop.getId(npcId));
	}

	@Override
	public Shopkeeper getShopkeeperByBlock(Block block) {
		if (block == null) return null;
		return this.getActiveShopkeeper(SignShop.getId(block));
	}

	public Shopkeeper getActiveShopkeeper(String objectId) {
		return activeShopkeepers.get(objectId);
	}

	@Override
	public boolean isShopkeeper(Entity entity) {
		return this.getShopkeeperByEntity(entity) != null;
	}

	@Override
	public Collection<Shopkeeper> getAllShopkeepers() {
		return allShopkeepersView;
	}

	@Override
	public Collection<List<Shopkeeper>> getAllShopkeepersByChunks() {
		return Collections.unmodifiableCollection(shopkeepersByChunk.values());
	}

	@Override
	public Collection<Shopkeeper> getActiveShopkeepers() {
		return activeShopkeepersView;
	}

	@Override
	public List<Shopkeeper> getShopkeepersInChunk(ChunkCoords chunkCoords) {
		List<Shopkeeper> byChunk = shopkeepersByChunk.get(chunkCoords);
		if (byChunk == null) return null;
		return Collections.unmodifiableList(byChunk);
	}

	// LOADING/UNLOADING/REMOVAL

	// performs some validation before actually activating a shopkeeper:
	// returns false if some validation failed
	private boolean _activateShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		String objectId = shopkeeper.getObjectId();
		if (objectId == null) {
			// currently only null is considered invalid,
			// prints 'null' to log then:
			Log.warning("Detected shopkeeper with invalid object id: " + objectId);
			return false;
		} else if (activeShopkeepers.containsKey(objectId)) {
			Log.warning("Detected shopkeepers with duplicate object id: " + objectId);
			return false;
		} else {
			// activate shopkeeper:
			activeShopkeepers.put(objectId, shopkeeper);
			return true;
		}
	}

	private boolean _deactivateShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		String objectId = shopkeeper.getObjectId();
		if (activeShopkeepers.get(objectId) == shopkeeper) {
			activeShopkeepers.remove(objectId);
			return true;
		}
		return false;
	}

	private void activateShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		if (shopkeeper.needsSpawning() && !shopkeeper.isActive()) {
			// deactivate shopkeeper by old shop object id, in case there is one:
			if (this._deactivateShopkeeper(shopkeeper)) {
				if (Settings.debug && shopkeeper.getShopObject() instanceof LivingEntityShop) {
					LivingEntityShop livingShop = (LivingEntityShop) shopkeeper.getShopObject();
					LivingEntity oldEntity = livingShop.getEntity();
					Log.debug("Old, active shopkeeper was found (unloading probably has been skipped earlier): "
							+ (oldEntity == null ? "null" : (oldEntity.getUniqueId() + " | " + (oldEntity.isDead() ? "dead | " : "alive | ")
									+ (oldEntity.isValid() ? "valid" : "invalid"))));
				}
			}

			// spawn and activate:
			boolean spawned = shopkeeper.spawn();
			if (spawned) {
				// activate with new object id:
				this._activateShopkeeper(shopkeeper);
			} else {
				Log.warning("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
			}
		}
	}

	private void deactivateShopkeeper(Shopkeeper shopkeeper, boolean closeWindows) {
		assert shopkeeper != null;
		if (closeWindows) {
			// delayed closing of all open windows:
			shopkeeper.closeAllOpenWindows();
		}
		this._deactivateShopkeeper(shopkeeper);
		shopkeeper.despawn();
	}

	public void deleteShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		// deactivate shopkeeper:
		this.deactivateShopkeeper(shopkeeper, true);

		// inform shopkeeper:
		shopkeeper.onDeletion();

		// remove shopkeeper by id and session id:
		shopkeepersById.remove(shopkeeper.getUniqueId());
		shopkeepersBySessionId.remove(shopkeeper.getSessionId());

		// remove shopkeeper from chunk:
		ChunkCoords chunkCoords = shopkeeper.getChunkCoords();
		this.removeShopkeeperFromChunk(shopkeeper, chunkCoords);
	}

	public void onShopkeeperMove(Shopkeeper shopkeeper, ChunkCoords oldChunk) {
		assert oldChunk != null;
		ChunkCoords newChunk = shopkeeper.getChunkCoords();
		if (!oldChunk.equals(newChunk)) {
			// remove from old chunk:
			this.removeShopkeeperFromChunk(shopkeeper, oldChunk);

			// add to new chunk:
			this.addShopkeeperToChunk(shopkeeper, newChunk);
		}
	}

	/**
	 * Loads (activates) all shopkeepers in the given chunk.
	 * 
	 * @param chunk
	 *            the chunk
	 * @return the number of shops in the affected chunk
	 */
	int loadShopkeepersInChunk(Chunk chunk) {
		assert chunk != null;
		int affectedShops = 0;
		List<Shopkeeper> shopkeepers = shopkeepersByChunk.get(new ChunkCoords(chunk));
		if (shopkeepers != null) {
			affectedShops = shopkeepers.size();
			Log.debug("Loading " + affectedShops + " shopkeepers in chunk " + chunk.getX() + "," + chunk.getZ());
			for (Shopkeeper shopkeeper : shopkeepers) {
				// inform shopkeeper about chunk load:
				shopkeeper.onChunkLoad();

				// activate:
				this.activateShopkeeper(shopkeeper);
			}

			// save delayed:
			this.saveDelayed();
		}
		return affectedShops;
	}

	/**
	 * Unloads (deactivates) all shopkeepers in the given chunk.
	 * 
	 * @param chunk
	 *            the chunk
	 * @return the number of shops in the affected chunk
	 */
	int unloadShopkeepersInChunk(Chunk chunk) {
		assert chunk != null;
		int affectedShops = 0;
		List<Shopkeeper> shopkeepers = this.getShopkeepersInChunk(new ChunkCoords(chunk));
		if (shopkeepers != null) {
			affectedShops = shopkeepers.size();
			Log.debug("Unloading " + affectedShops + " shopkeepers in chunk " + chunk.getX() + "," + chunk.getZ());
			for (Shopkeeper shopkeeper : shopkeepers) {
				// inform shopkeeper about chunk unload:
				shopkeeper.onChunkUnload();

				// skip shopkeepers which are kept active all the time (ex. sign, citizens shops):
				if (!shopkeeper.needsSpawning()) continue;

				// deactivate:
				this.deactivateShopkeeper(shopkeeper, false);
			}
		}
		return affectedShops;
	}

	/**
	 * Loads all shopkeepers in the given world.
	 * 
	 * @param world
	 *            the world
	 */
	void loadShopkeepersInWorld(World world) {
		assert world != null;
		int affectedShops = 0;
		for (Chunk chunk : world.getLoadedChunks()) {
			affectedShops += this.loadShopkeepersInChunk(chunk);
		}
		Log.debug("Loaded " + affectedShops + " shopkeepers in world " + world.getName());
	}

	/**
	 * Unloads all shopkeepers in the given world.
	 * 
	 * @param world
	 *            the world
	 */
	void unloadShopkeepersInWorld(World world) {
		assert world != null;
		int affectedShops = 0;
		for (Chunk chunk : world.getLoadedChunks()) {
			affectedShops += this.unloadShopkeepersInChunk(chunk);
		}
		Log.debug("Unloaded " + affectedShops + " shopkeepers in world " + world.getName());
	}

	// SHOPKEEPER CREATION:

	@Override
	public boolean hasCreatePermission(Player player) {
		if (player == null) return false;
		return shopTypesManager.getSelection(player) != null && shopObjectTypesManager.getSelection(player) != null;
	}

	@Override
	public Shopkeeper createNewAdminShopkeeper(ShopCreationData creationData) {
		try {
			if (creationData == null || creationData.spawnLocation == null || creationData.objectType == null) {
				throw new ShopkeeperCreateException("null");
			}
			if (creationData.shopType == null) {
				creationData.shopType = DefaultShopTypes.ADMIN();
			} else if (creationData.shopType.isPlayerShopType()) {
				// we are expecting an admin shop type here..
				throw new ShopkeeperCreateException("Expecting admin shop type, got player shop type!");
			}

			// create the shopkeeper (and spawn it):
			Shopkeeper shopkeeper = creationData.shopType.createShopkeeper(creationData);
			if (shopkeeper == null) {
				throw new ShopkeeperCreateException("ShopType returned null shopkeeper!");
			}
			assert shopkeeper != null;

			// save:
			this.save();

			// send creation message to creator:
			Utils.sendMessage(creationData.creator, creationData.shopType.getCreatedMessage());

			// run shopkeeper-created-event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperCreatedEvent(creationData.creator, shopkeeper));

			return shopkeeper;
		} catch (ShopkeeperCreateException e) {
			Log.warning("Couldn't create admin shopkeeper: " + e.getMessage());
			return null;
		}
	}

	@Override
	public Shopkeeper createNewPlayerShopkeeper(ShopCreationData creationData) {
		try {
			if (creationData == null || creationData.shopType == null || creationData.objectType == null
					|| creationData.creator == null || creationData.chest == null || creationData.spawnLocation == null) {
				throw new ShopkeeperCreateException("null");
			}

			// check if this chest is already used by some other shopkeeper:
			if (this.getProtectedChests().isChestProtected(creationData.chest, null)) {
				Utils.sendMessage(creationData.creator, Settings.msgShopCreateFail);
				return null;
			}

			// check worldguard:
			if (Settings.enableWorldGuardRestrictions) {
				if (!WorldGuardHandler.isShopAllowed(creationData.creator, creationData.spawnLocation)) {
					Utils.sendMessage(creationData.creator, Settings.msgShopCreateFail);
					return null;
				}
			}

			// check towny:
			if (Settings.enableTownyRestrictions) {
				if (!TownyHandler.isCommercialArea(creationData.spawnLocation)) {
					Utils.sendMessage(creationData.creator, Settings.msgShopCreateFail);
					return null;
				}
			}

			int maxShops = this.getMaxShops(creationData.creator);

			// call event:
			CreatePlayerShopkeeperEvent event = new CreatePlayerShopkeeperEvent(creationData, maxShops);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				Log.debug("CreatePlayerShopkeeperEvent was cancelled!");
				return null;
			} else {
				creationData.spawnLocation = event.getSpawnLocation();
				creationData.shopType = event.getType();
				maxShops = event.getMaxShopsForPlayer();
			}

			// count owned shops:
			if (maxShops > 0) {
				int count = this.countShopsOfPlayer(creationData.creator);
				if (count >= maxShops) {
					Utils.sendMessage(creationData.creator, Settings.msgTooManyShops);
					return null;
				}
			}

			// create and spawn the shopkeeper:
			Shopkeeper shopkeeper = creationData.shopType.createShopkeeper(creationData);
			if (shopkeeper == null) {
				throw new ShopkeeperCreateException("ShopType returned null shopkeeper!");
			}
			assert shopkeeper != null;

			// save:
			this.save();

			// send creation message to creator:
			Utils.sendMessage(creationData.creator, creationData.shopType.getCreatedMessage());

			// run shopkeeper-created-event
			Bukkit.getPluginManager().callEvent(new ShopkeeperCreatedEvent(creationData.creator, shopkeeper));

			return shopkeeper;
		} catch (ShopkeeperCreateException e) {
			Log.warning("Couldn't create player shopkeeper: " + e.getMessage());
			return null;
		}
	}

	public int countShopsOfPlayer(Player player) {
		int count = 0;
		for (Shopkeeper shopkeeper : shopkeepersById.values()) {
			if (shopkeeper instanceof PlayerShopkeeper && ((PlayerShopkeeper) shopkeeper).isOwner(player)) {
				count++;
			}
		}
		return count;
	}

	public int getMaxShops(Player player) {
		int maxShops = Settings.maxShopsPerPlayer;
		String[] maxShopsPermOptions = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : maxShopsPermOptions) {
			if (Utils.hasPermission(player, "shopkeeper.maxshops." + perm)) {
				maxShops = Integer.parseInt(perm);
			}
		}
		return maxShops;
	}

	// INACTIVE SHOPS

	private void removeInactivePlayerShops() {
		if (Settings.playerShopkeeperInactiveDays <= 0) return;

		final Set<UUID> playerUUIDs = new HashSet<UUID>();
		for (Shopkeeper shopkeeper : shopkeepersById.values()) {
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
		final int playerShopkeeperInactiveDays = Settings.playerShopkeeperInactiveDays;
		Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {

			@Override
			public void run() {
				final List<OfflinePlayer> inactivePlayers = new ArrayList<OfflinePlayer>();
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
				SchedulerUtils.runTaskOrOmit(ShopkeepersPlugin.this, new Runnable() {

					@Override
					public void run() {
						List<PlayerShopkeeper> forRemoval = new ArrayList<PlayerShopkeeper>();
						for (OfflinePlayer inactivePlayer : inactivePlayers) {
							// remove all shops of this inactive player:
							UUID playerUUID = inactivePlayer.getUniqueId();

							for (Shopkeeper shopkeeper : shopkeepersById.values()) {
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
							save();
						}
					}
				});
			}
		});
	}

	// HANDLING PLAYER NAME CHANGES:

	// updates owner names for the shopkeepers of the specified player:
	void updateShopkeepersForPlayer(UUID playerUUID, String playerName) {
		boolean dirty = false;
		for (Shopkeeper shopkeeper : shopkeepersById.values()) {
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
			this.save();
		}
	}

	// SHOPS LOADING AND SAVING

	private static class SaveResult {

		// note: synchronization for those values is not needed, because they don't get modified by the main and async
		// task at the same time, and because the bukkit scheduler already includes memory-barriers when starting the
		// async IO task and when going back to the main thread by starting a sync task, so changes become visible to
		// each thread involved

		private boolean async;
		private long startTime;
		private long packingDuration;
		private long asyncTaskDelay;
		private long ioLockAcquireDuration;
		private long ioDuration;
		private long totalDuration;
		private boolean success;

		public void printDebugInfo() {
			Log.debug("Saved shopkeeper data (" + totalDuration + "ms (Data packing: " + packingDuration + "ms, "
					+ (async ? "AsyncTask delay: " + asyncTaskDelay + "ms, " : "")
					+ ((ioLockAcquireDuration > 1) ? "IO lock delay: " + ioLockAcquireDuration + "ms, " : "")
					+ (async ? "Async " : "Sync ") + "IO: " + ioDuration + "ms))"
					+ (success ? "" : ": Saving failed!"));
		}
	}

	private File getSaveFile() {
		return new File(this.getDataFolder(), "save.yml");
	}

	private File getTempSaveFile() {
		File saveFile = this.getSaveFile();
		return new File(saveFile.getParentFile(), saveFile.getName() + ".temp");
	}

	// returns false if there was some issue during loading
	private boolean load() {
		File saveFile = this.getSaveFile();
		if (!saveFile.exists()) {
			File tempSaveFile = this.getTempSaveFile();
			if (tempSaveFile.exists()) {
				// load from temporary save file instead:
				Log.warning("Found no save file, but an existing temporary save file! (" + tempSaveFile.getName() + ")");
				Log.warning("This might indicate an issue during a previous saving attempt!");
				Log.warning("Trying to load the shopkeepers data from this temporary save file instead!");

				saveFile = tempSaveFile;
			} else {
				// save file does not exist yet -> no shopkeeper data available
				return true;
			}
		}

		YamlConfiguration shopkeepersConfig = new YamlConfiguration();
		try {
			if (!Utils.isEmpty(Settings.fileEncoding)) {
				// load with specified charset:
				try (	FileInputStream stream = new FileInputStream(saveFile);
						InputStreamReader reader = new InputStreamReader(stream, Settings.fileEncoding)) {
					shopkeepersConfig.load(reader);
				}
			} else {
				// load with default charset handling:
				shopkeepersConfig.load(saveFile);
			}
		} catch (Exception e) {
			Log.severe("Failed to load save file!");
			e.printStackTrace();
			return false; // disable without save
		}

		Set<String> ids = shopkeepersConfig.getKeys(false);
		Log.info("Loading data of " + ids.size() + " shopkeepers..");
		for (String id : ids) {
			ConfigurationSection shopkeeperSection = shopkeepersConfig.getConfigurationSection(id);
			ShopType<?> shopType = shopTypesManager.get(shopkeeperSection.getString("type"));
			// unknown shop type
			if (shopType == null) {
				// got an owner entry? -> default to normal player shop type
				if (shopkeeperSection.contains("owner")) {
					Log.warning("No valid shop type specified for shopkeeper '" + id + "': defaulting to "
							+ DefaultShopTypes.PLAYER_NORMAL().getIdentifier());
					shopType = DefaultShopTypes.PLAYER_NORMAL();
				} else {
					// no valid shop type given..
					Log.severe("Failed to load shopkeeper '" + id + "': unknown type");
					return false; // disable without save
				}
			}

			// MC 1.11: convert old skeleton and zombie variants to new mob types
			// TODO remove again in future updates
			boolean hasStrayType = false;
			boolean hasWitherSkeletonType = false;
			boolean hasZombieVillagerType = false;
			try {
				hasStrayType = (EntityType.valueOf("STRAY") != null);
			} catch (Exception e) {
			}
			try {
				hasWitherSkeletonType = (EntityType.valueOf("WITHER_SKELETON") != null);
			} catch (Exception e) {
			}
			try {
				hasZombieVillagerType = (EntityType.valueOf("ZOMBIE_VILLAGER") != null);
			} catch (Exception e) {
			}

			if (hasStrayType || hasWitherSkeletonType || hasZombieVillagerType) {
				String objectType = shopkeeperSection.getString("object");
				if ("skeleton".equalsIgnoreCase(objectType)) {
					String skeletonType = shopkeeperSection.getString("skeletonType");
					if (hasStrayType && "STRAY".equalsIgnoreCase(skeletonType)) {
						Log.warning("Converting skeleton shopkeeper '" + id + "' with stray variant to new stray shopkeeper.");
						shopkeeperSection.set("object", "stray");
					} else if (hasWitherSkeletonType && "WITHER".equalsIgnoreCase(skeletonType)) {
						Log.warning("Converting skeleton shopkeeper '" + id + "' with wither variant to new wither-skeleton shopkeeper.");
						shopkeeperSection.set("object", "wither_skeleton");
					}
				}
				if ("zombie".equalsIgnoreCase(objectType)) {
					if (hasZombieVillagerType && shopkeeperSection.getBoolean("villagerZombie")) {
						Log.warning("Converting zombie shopkeeper '" + id + "' with zombie-villager variant to new zombie-villager shopkeeper.");
						shopkeeperSection.set("object", "zombie_villager");
					}
				}
			}

			// load shopkeeper:
			try {
				Shopkeeper shopkeeper = shopType.loadShopkeeper(shopkeeperSection);
				if (shopkeeper == null) {
					throw new ShopkeeperCreateException("ShopType returned null shopkeeper!");
				}
			} catch (Exception e) {
				Log.severe("Failed to load shopkeeper '" + id + "': " + e.getMessage());
				e.printStackTrace();
				return false; // disable without save
			}
		}
		return true;
	}

	@Override
	public void save() {
		if (Settings.saveInstantly) {
			this.saveReal();
		} else {
			dirty = true;
		}
	}

	@Override
	public void saveDelayed() {
		dirty = true;
		if (Settings.saveInstantly) {
			if (delayedSaveTaskId == -1) {
				delayedSaveTaskId = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						if (dirty) {
							saveReal();
						}
						delayedSaveTaskId = -1;
					}
				}, 600).getTaskId(); // 30 seconds delay
			}
		}
	}

	@Override
	public void saveReal() {
		this.saveReal(true);
	}

	// should only get called with parameter async=false on disable:
	private void saveReal(final boolean async) {
		if (skipSaving) {
			Log.warning("Skipped saving due to previous issue.");
			return;
		}

		// is another async save task already running?
		if (async && saveIOTask != -1) {
			// set flag which triggers a new save once that current task is done:
			saveRealAgain = true;
			return;
		}

		// keeps track of statistics and information about this saving attempt:
		final SaveResult saveResult = new SaveResult();
		saveResult.async = async;
		saveResult.startTime = System.currentTimeMillis();

		// store shopkeeper data into memory configuration:
		final YamlConfiguration config = new YamlConfiguration();
		int counter = 1;
		for (Shopkeeper shopkeeper : shopkeepersById.values()) {
			String sectionKey = String.valueOf(counter++);
			ConfigurationSection section = config.createSection(sectionKey);
			try {
				shopkeeper.save(section);
			} catch (Exception e) {
				// error while saving shopkeeper data:
				// skip this shopkeeper and print warning + stacktrace:
				config.set(sectionKey, null);
				Log.warning("Couldn't save shopkeeper '" + shopkeeper.getUniqueId() + "' at " + shopkeeper.getPositionString() + "!");
				e.printStackTrace();
			}
		}
		// time to store shopkeeper data in memory configuration:
		saveResult.packingDuration = System.currentTimeMillis() - saveResult.startTime;

		// note: the dirty flag might get reverted again after saving, if saving failed
		// however, the flag gets reset here (and not just after successful saving), so that any saving requests that
		// arrive in the meantime get noticed and can cause another save later:
		dirty = false;

		// called sync:
		final Runnable syncSavingCallback = new Runnable() {

			@Override
			public void run() {
				// print debug info:
				saveResult.printDebugInfo();

				// saving failed?
				if (!saveResult.success) {
					// mark as dirty, as there is potentially unsaved data, and request another delayed save:
					dirty = true;
					saveDelayed();

					// inform admins about saving issue:
					// 4 min error message throttle (slightly less than the saving interval)
					if (Math.abs(System.currentTimeMillis() - lastSavingErrorMsgTimeStamp) > (4 * 60 * 1000L)) {
						lastSavingErrorMsgTimeStamp = System.currentTimeMillis();
						String errorMsg = ChatColor.DARK_RED + "[Shopkeepers] " + ChatColor.RED + "Saving shop data failed! Please check out the server log(s) and look into the issue!";
						for (Player player : Bukkit.getOnlinePlayers()) {
							if (player.hasPermission(ShopkeepersAPI.ADMIN_PERMISSION)) {
								player.sendMessage(errorMsg);
							}
						}
					}
				}

				if (async) {
					saveIOTask = -1;

					// did we get another request to saveReal() in the meantime?
					if (saveRealAgain) {
						// trigger another full save with latest data:
						saveRealAgain = false;
						saveReal();
					}
				}
			}
		};
		// called possibly async:
		final Runnable savingCallback = new Runnable() {

			@Override
			public void run() {
				// ensure that we continue on main thread:
				SchedulerUtils.runOnMainThreadOrOmit(ShopkeepersPlugin.this, syncSavingCallback);
			}
		};

		if (!async) {
			// sync file io:
			this.saveDataToFile(config, saveResult, savingCallback);
		} else {
			// async file io:
			final long asyncTaskSubmittedTime = System.currentTimeMillis();
			saveIOTask = Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {

				@Override
				public void run() {
					saveResult.asyncTaskDelay = System.currentTimeMillis() - asyncTaskSubmittedTime;
					saveDataToFile(config, saveResult, savingCallback);
				}
			}).getTaskId();
		}
	}

	// max total delay: 500ms
	private static final int SAVING_MAX_ATTEMPTS = 20;
	private static final long SAVING_ATTEMPTS_DELAY_MILLIS = 25;
	private static final Object SAVING_IO_LOCK = new Object();

	// can be run async and sync
	private void saveDataToFile(FileConfiguration config, SaveResult saveResult, Runnable callback) {
		assert config != null && saveResult != null;

		// synchronization, so that only one thread at a time attempts to mess with the save files
		final long ioLockStartTime = System.currentTimeMillis();
		synchronized (SAVING_IO_LOCK) {
			saveResult.ioLockAcquireDuration = System.currentTimeMillis() - ioLockStartTime;

			// actual IO:
			final long ioStartTime = System.currentTimeMillis();

			File saveFile = this.getSaveFile();
			File tempSaveFile = this.getTempSaveFile();

			// saving procedure:
			// inside a retry-loop:
			// * if there is a temporary save file:
			// * * if there is no save file: rename temporary save file to save file
			// * * else: remove temporary save file
			// * create parent directories
			// * create new temporary save file
			// * save data to temporary save file
			// * remove old save file
			// * rename temporary save file to save file

			int savingAttempt = 0;
			boolean problem = false;
			String error = null;
			Exception exception;
			boolean printStacktrace = true;

			while (++savingAttempt <= SAVING_MAX_ATTEMPTS) {
				// reset problem variables:
				problem = false;
				error = null;
				exception = null;

				try {
					// handle already existing temporary save file:
					if (!problem) {
						if (tempSaveFile.exists()) {
							// check write permission:
							if (!tempSaveFile.canWrite()) {
								error = "Cannot write to temporary save file! (" + tempSaveFile.getName() + ")";
								problem = true;
							}

							if (!problem) {
								if (!saveFile.exists()) {
									// if only the temporary file exists, but the actual save file does not, this might
									// indicate, that a previous saving attempt saved to the temporary file and removed
									// the
									// actual save file, but wasn't able to then rename the temporary file to the actual
									// save file
									// -> the temporary file might contain the only backup of saved data, don't remove
									// it!
									// -> instead we try to rename it to make it the new 'actual save file' and then
									// continue the saving procedure

									Log.warning("Found an already existing temporary save file, but no old save file! (" + tempSaveFile.getName() + ")");
									Log.warning("This might indicate an issue during a previous saving attempt!");
									Log.warning("Trying to rename the temporary save file to use it as 'existing old save data', and then continue the saving!");

									// rename temporary save file:
									if (!tempSaveFile.renameTo(saveFile)) {
										error = "Couldn't rename temporary save file! (" + tempSaveFile.getName() + " to " + saveFile.getName() + ")";
										problem = true;
									}
								} else {
									// remove old temporary save file:
									if (!tempSaveFile.delete()) {
										error = "Couldn't delete existing temporary save file! (" + tempSaveFile.getName() + ")";
										problem = true;
									}
								}
							}
						}
					}

					// make sure that the parent directories exist:
					if (!problem) {
						File parentDir = tempSaveFile.getParentFile();
						if (parentDir != null && !parentDir.exists()) {
							if (!parentDir.mkdirs()) {
								error = "Couldn't create parent directories for temporary save file! (" + parentDir.getAbsolutePath() + ")";
								problem = true;
							}
						}
					}

					// create new temporary save file:
					if (!problem) {
						try {
							tempSaveFile.createNewFile();
						} catch (Exception e) {
							error = "Couldn't create temporary save file! (" + tempSaveFile.getName() + ") : " + e.getMessage();
							exception = e;
							problem = true;
						}
					}

					// write shopkeeper data to temporary save file:
					if (!problem) {
						PrintWriter writer = null;
						try {
							if (Settings.fileEncoding != null && !Settings.fileEncoding.isEmpty()) {
								writer = new PrintWriter(tempSaveFile, Settings.fileEncoding);
								writer.write(config.saveToString());
							} else {
								config.save(tempSaveFile);
							}
						} catch (Exception e) {
							error = "Couldn't save data to temporary save file! (" + tempSaveFile.getName() + ") : " + e.getMessage();
							exception = e;
							problem = true;
						} finally {
							if (writer != null) {
								writer.close();
							}
						}
					}

					// delete old save file:
					if (!problem) {
						if (saveFile.exists()) {
							// check write permission:
							if (!saveFile.canWrite()) {
								error = "Cannot write to save file! (" + saveFile.getName() + ")";
								problem = true;
							} else {
								// delete old save file:
								if (!saveFile.delete()) {
									error = "Couldn't delete existing old save file! (" + saveFile.getName() + ")";
									problem = true;
								}
							}
						}
					}

					// rename temporary save file:
					if (!problem) {
						if (!tempSaveFile.renameTo(saveFile)) {
							error = "Couldn't rename temporary save file! (" + tempSaveFile.getName() + " to " + saveFile.getName() + ")";
							problem = true;
						}
					}
				} catch (Exception e) {
					// catching any exceptions not explicitly caught above already:
					error = e.getMessage();
					exception = e;
					problem = true;
				}

				// handle problem situation:
				if (problem) {
					// don't spam with errors and stacktraces, only print them once for the first saving attempt:
					if (exception != null && printStacktrace) {
						printStacktrace = false;
						exception.printStackTrace();
					}
					Log.severe("Saving attempt " + savingAttempt + " failed: " + (error != null ? error : "Unknown error"));

					if (savingAttempt < SAVING_MAX_ATTEMPTS) {
						// try again after a small delay:
						try {
							Thread.sleep(SAVING_ATTEMPTS_DELAY_MILLIS);
						} catch (InterruptedException e) {
						}
					} else {
						// saving failed even after a bunch of retries:
						saveResult.success = false;
						Log.severe("Saving failed! Save data might be lost! :(");
						break;
					}
				} else {
					// saving was successful:
					saveResult.success = true;
					break;
				}
			}

			final long now = System.currentTimeMillis();
			saveResult.ioDuration = now - ioStartTime; // time for pure io
			saveResult.totalDuration = now - saveResult.startTime; // time from saveReal() call to finished save
		}
		// file IO over

		// run callback:
		if (callback != null) {
			callback.run();
		}
	}

	private static class ConfirmEntry {

		private final Runnable action;
		private final int taskId;

		public ConfirmEntry(Runnable action, int taskId) {
			this.taskId = taskId;
			this.action = action;
		}

		public int getTaskId() {
			return taskId;
		}

		public Runnable getAction() {
			return action;
		}
	}
}
