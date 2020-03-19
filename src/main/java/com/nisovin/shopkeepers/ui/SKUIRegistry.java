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
	// player id -> ui session
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
		// close all open UIs:
		this.closeAll();
		HandlerList.unregisterAll(uiListener);
		uiListener = null;
	}

	@Override
	protected String getTypeName() {
		return "UI type";
	}

	public boolean requestUI(UIType uiType, AbstractShopkeeper shopkeeper, Player player) {
		Validate.notNull(uiType, "UI type is null!");
		Validate.notNull(shopkeeper, "Shopkeeper is null!");
		Validate.notNull(player, "Player is null!");

		String uiIdentifier = uiType.getIdentifier();
		UIHandler uiHandler = shopkeeper.getUIHandler(uiType);
		if (uiHandler == null) {
			Log.debug(() -> "Cannot open UI '" + uiIdentifier + "': This shopkeeper is not handling/supporting this type of user interface.");
			return false;
		}

		String playerName = player.getName();
		if (!uiHandler.canOpen(player)) {
			Log.debug(() -> "The player '" + playerName + "' cannot open UI '" + uiIdentifier + "'.");
			return false;
		}

		SKUISession oldSession = this.getSession(player);
		// filtering out duplicate open requests:
		if (oldSession != null && oldSession.getShopkeeper().equals(shopkeeper) && oldSession.getUIHandler().equals(uiHandler)) {
			Log.debug(() -> "UI '" + uiIdentifier + "'" + " is already open for '" + playerName + "'.");
			return false;
		}

		// call event:
		ShopkeeperOpenUIEvent openUIEvent = new ShopkeeperOpenUIEvent(shopkeeper, uiType, player);
		Bukkit.getPluginManager().callEvent(openUIEvent);
		if (openUIEvent.isCancelled()) {
			Log.debug(() -> "Opening of UI '" + uiIdentifier + "' for player '" + playerName + "' got cancelled by a plugin.");
			return false;
		}

		// opening a window should automatically trigger a close of the previous window
		// however, we do this manually here just in case, because we cannot be sure what the UI handler is actually
		// doing inside openWindow
		// closing previous window:
		if (oldSession != null) {
			Log.debug(() -> "Closing previous UI '" + uiIdentifier + "' for player '" + playerName + "'.");
			player.closeInventory(); // this will call a PlayerCloseInventoryEvent
		}

		Log.debug(() -> "Opening UI '" + uiIdentifier + "' ...");
		boolean isOpen = uiHandler.openWindow(player);
		if (isOpen) {
			assert playerSessions.get(player.getUniqueId()) == null;
			SKUISession session = new SKUISession(shopkeeper, uiHandler, player);
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

	// closeEvent might be null
	void onInventoryClose(Player player, InventoryCloseEvent closeEvent) {
		assert player != null;
		SKUISession session = playerSessions.remove(player.getUniqueId());
		if (session == null) return;

		UIHandler uiHandler = session.getUIHandler();
		Log.debug(() -> "Player " + player.getName() + " closed UI '" + uiHandler.getUIType().getIdentifier() + "'.");
		this.onSessionEnd(session, closeEvent);
	}

	public void onPlayerQuit(Player player) {
		// TODO this might have no effect, because CraftBukkit triggers an inventory close event prior to the player
		// quitting
		this.onInventoryClose(player, null);
	}

	private void onSessionStart(SKUISession session) {
		Log.debug(() -> "UI '" + session.getUIType().getIdentifier() + "' session started for player '" + session.getPlayer().getName() + "'.");
	}

	// closeEvent can be null
	private void onSessionEnd(SKUISession session, InventoryCloseEvent closeEvent) {
		Log.debug(() -> "UI '" + session.getUIType().getIdentifier() + "' session ended for player '" + session.getPlayer().getName() + "'.");
		session.getUIHandler().onInventoryClose(session.getPlayer(), closeEvent); // inform UI handler
		session.onSessionEnd(); // inform session
	}

	@Override
	public void closeAll(Shopkeeper shopkeeper) {
		if (shopkeeper == null) return;
		assert shopkeeper != null;
		Iterator<Entry<UUID, SKUISession>> iterator = playerSessions.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<UUID, SKUISession> entry = iterator.next();
			SKUISession session = entry.getValue();
			if (session.getShopkeeper().equals(shopkeeper)) {
				iterator.remove();
				this.onSessionEnd(session, null);
				Player player = session.getPlayer();
				player.closeInventory();
			}
		}
	}

	@Override
	public void closeAllDelayed(Shopkeeper shopkeeper) {
		// ignore during disable: all UIs get closed anyways already
		if (shopkeeper == null || !plugin.isEnabled()) return;

		// deactivate currently active UIs:
		shopkeeper.deactivateUI();

		// delayed because this is/was originally called from inside the PlayerCloseInventoryEvent
		Bukkit.getScheduler().runTask(plugin, () -> {
			this.closeAll(shopkeeper);

			// reactivate UIs:
			shopkeeper.activateUI();
		});
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
		playerSessions.clear(); // just in case
	}
}
