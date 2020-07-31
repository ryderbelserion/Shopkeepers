package com.nisovin.shopkeepers.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryCloseEvent;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerOpenUIEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperOpenUIEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.types.AbstractTypeRegistry;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

public class SKUIRegistry extends AbstractTypeRegistry<AbstractUIType> implements UIRegistry<AbstractUIType> {

	private final ShopkeepersPlugin plugin;
	// Player id -> UI session
	private final Map<UUID, SKUISession> playerSessions = new HashMap<>();
	private UIListener uiListener = null;

	public SKUIRegistry(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		assert uiListener == null;
		uiListener = new UIListener(plugin, this);
		Bukkit.getPluginManager().registerEvents(uiListener, plugin);
	}

	public void onDisable() {
		assert uiListener != null;
		// Close all open UIs:
		this.closeAll();
		HandlerList.unregisterAll(uiListener);
		uiListener = null;
	}

	@Override
	protected String getTypeName() {
		return "UI type";
	}

	public boolean requestUI(UIType uiType, AbstractShopkeeper shopkeeper, Player player) {
		Validate.notNull(uiType, "uiHandler is null");
		Validate.notNull(shopkeeper, "shopkeeper is null");
		// Player is validated in the following.

		String uiIdentifier = uiType.getIdentifier();
		UIHandler uiHandler = shopkeeper.getUIHandler(uiType);
		if (uiHandler == null) {
			Log.debug(() -> "Cannot open UI '" + uiIdentifier + "': This shopkeeper is not handling/supporting this type of user interface.");
			return false;
		}
		return requestUI(uiHandler, player);
	}

	public boolean requestUI(UIHandler uiHandler, Player player) {
		Validate.notNull(uiHandler, "uiHandler is null");
		Validate.notNull(player, "player is null");
		UIType uiType = uiHandler.getUIType();
		String uiIdentifier = uiType.getIdentifier();
		AbstractShopkeeper shopkeeper = null; // Can be null
		if (uiHandler instanceof ShopkeeperUIHandler) {
			shopkeeper = ((ShopkeeperUIHandler) uiHandler).getShopkeeper();
		}

		String playerName = player.getName();
		if (!uiHandler.canOpen(player)) {
			Log.debug(() -> "The player '" + playerName + "' cannot open UI '" + uiIdentifier + "'.");
			return false;
		}

		SKUISession oldSession = this.getSession(player);
		// Filter out duplicate open requests:
		if (oldSession != null && oldSession.getUIHandler().equals(uiHandler)) {
			Log.debug(() -> "UI '" + uiIdentifier + "'" + " is already open for '" + playerName + "'.");
			return false;
		}

		// Call event:
		PlayerOpenUIEvent openUIEvent;
		if (shopkeeper == null) {
			openUIEvent = new PlayerOpenUIEvent(uiType, player);
		} else {
			openUIEvent = new ShopkeeperOpenUIEvent(shopkeeper, uiType, player);
		}
		Bukkit.getPluginManager().callEvent(openUIEvent);
		if (openUIEvent.isCancelled()) {
			Log.debug(() -> "Opening of UI '" + uiIdentifier + "' for player '" + playerName + "' got cancelled by a plugin.");
			return false;
		}

		// Opening a window should automatically trigger a close of the previous window.
		// However, we do this manually here just in case, because we cannot be sure what the UI handler is actually
		// doing inside openWindow.
		// Close previous window:
		if (oldSession != null) {
			Log.debug(() -> "Closing previous UI '" + uiIdentifier + "' for player '" + playerName + "'.");
			player.closeInventory(); // This will call a PlayerCloseInventoryEvent
		}

		Log.debug(() -> "Opening UI '" + uiIdentifier + "' ...");
		boolean isOpen = uiHandler.openWindow(player);
		if (isOpen) {
			assert playerSessions.get(player.getUniqueId()) == null;
			SKUISession session = new SKUISession(uiHandler, player, shopkeeper);
			playerSessions.put(player.getUniqueId(), session);
			this.onSessionStart(session);
			return true;
		} else {
			Log.debug(() -> "UI '" + uiIdentifier + "' NOT opened!");
			return false;
		}
	}

	@Override
	public SKUISession getSession(Player player) {
		Validate.notNull(player, "Player is null!");
		return playerSessions.get(player.getUniqueId());
	}

	@Override
	public AbstractUIType getOpenUIType(Player player) {
		SKUISession session = this.getSession(player);
		return (session != null ? session.getUIType() : null);
	}

	// closeEvent might be null.
	void onInventoryClose(Player player, InventoryCloseEvent closeEvent) {
		assert player != null;
		SKUISession session = playerSessions.remove(player.getUniqueId());
		if (session == null) return;

		UIHandler uiHandler = session.getUIHandler();
		Log.debug(() -> "Player " + player.getName() + " closed UI '" + uiHandler.getUIType().getIdentifier() + "'.");
		this.onSessionEnd(session, closeEvent);
	}

	public void onPlayerQuit(Player player) {
		// TODO This might have no effect, because CraftBukkit triggers an inventory close event prior to the player
		// quitting
		this.onInventoryClose(player, null);
	}

	private void onSessionStart(SKUISession session) {
		Log.debug(() -> "UI '" + session.getUIType().getIdentifier() + "' session started for player '" + session.getPlayer().getName() + "'.");
	}

	// closeEvent can be null.
	private void onSessionEnd(SKUISession session, InventoryCloseEvent closeEvent) {
		Log.debug(() -> "UI '" + session.getUIType().getIdentifier() + "' session ended for player '" + session.getPlayer().getName() + "'.");
		session.getUIHandler().onInventoryClose(session.getPlayer(), closeEvent); // Inform UI handler
		session.onSessionEnd(); // Inform session
	}

	@Override
	public void closeAll(Shopkeeper shopkeeper) {
		if (shopkeeper == null) return;
		assert shopkeeper != null;
		Iterator<Entry<UUID, SKUISession>> iterator = playerSessions.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<UUID, SKUISession> entry = iterator.next();
			SKUISession session = entry.getValue();
			if (session.getShopkeeper() == shopkeeper) {
				iterator.remove();
				this.onSessionEnd(session, null);
				Player player = session.getPlayer();
				player.closeInventory();
			}
		}
	}

	@Override
	public void closeAllDelayed(Shopkeeper shopkeeper) {
		// Ignore during disable: All UIs get closed anyways already.
		if (shopkeeper == null || !plugin.isEnabled()) return;

		// Deactivate currently active UIs for this shopkeeper:
		this.deactivateUIs(shopkeeper);

		// Delayed because this may get called from within inventory events:
		Bukkit.getScheduler().runTask(plugin, () -> {
			this.closeAll(shopkeeper);
		});
	}

	private void deactivateUIs(Shopkeeper shopkeeper) {
		for (SKUISession session : playerSessions.values()) {
			if (session.getShopkeeper() == shopkeeper) {
				session.deactivateUI();
			}
		}
	}

	@Override
	public void closeAll() {
		Iterator<Entry<UUID, SKUISession>> iterator = playerSessions.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<UUID, SKUISession> entry = iterator.next();
			SKUISession session = entry.getValue();
			iterator.remove();
			this.onSessionEnd(session, null);
			Player player = session.getPlayer();
			player.closeInventory();
		}
		playerSessions.clear(); // Just in case
	}
}
