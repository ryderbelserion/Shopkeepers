package com.nisovin.shopkeepers.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.types.AbstractTypeRegistry;
import com.nisovin.shopkeepers.util.bukkit.SchedulerUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKUIRegistry extends AbstractTypeRegistry<AbstractUIType> implements UIRegistry<AbstractUIType> {

	private final ShopkeepersPlugin plugin;
	private final UIListener uiListener;

	// Player id -> UI session
	private final Map<UUID, SKUISession> uiSessions = new HashMap<>();
	private final Collection<SKUISession> uiSessionsView = Collections.unmodifiableCollection(uiSessions.values());

	public SKUIRegistry(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.uiListener = new UIListener(this);
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(uiListener, plugin);
	}

	public void onDisable() {
		// Close all open UIs:
		this.abortUISessions();
		HandlerList.unregisterAll(uiListener);
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
		return this.requestUI(uiHandler, player);
	}

	public boolean requestUI(UIHandler uiHandler, Player player) {
		return this.requestUI(uiHandler, player, false);
	}

	public boolean requestUI(UIHandler uiHandler, Player player, boolean silentRequest) {
		Validate.notNull(uiHandler, "uiHandler is null");
		Validate.notNull(player, "player is null");
		UIType uiType = uiHandler.getUIType();
		String uiIdentifier = uiType.getIdentifier();
		AbstractShopkeeper shopkeeper = null; // Can be null
		if (uiHandler instanceof ShopkeeperUIHandler) {
			shopkeeper = ((ShopkeeperUIHandler) uiHandler).getShopkeeper();
		}

		String playerName = player.getName();
		if (!uiHandler.canOpen(player, silentRequest)) {
			Log.debug(() -> "The player '" + playerName + "' cannot open UI '" + uiIdentifier + "'.");
			return false;
		}

		SKUISession oldSession = this.getUISession(player);
		// Filter out duplicate open requests:
		if (oldSession != null && oldSession.getUIHandler().equals(uiHandler)) {
			Log.debug(() -> "UI '" + uiIdentifier + "'" + " is already open for '" + playerName + "'.");
			return false;
		}

		// Call event:
		PlayerOpenUIEvent openUIEvent;
		if (shopkeeper == null) {
			openUIEvent = new PlayerOpenUIEvent(uiType, player, silentRequest);
		} else {
			openUIEvent = new ShopkeeperOpenUIEvent(shopkeeper, uiType, player, silentRequest);
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
			assert uiSessions.get(player.getUniqueId()) == null;
			SKUISession session = new SKUISession(uiHandler, player, shopkeeper);
			uiSessions.put(player.getUniqueId(), session);
			this.onSessionStart(session);
			return true;
		} else {
			Log.debug(() -> "UI '" + uiIdentifier + "' NOT opened!");
			return false;
		}
	}

	@Override
	public Collection<? extends SKUISession> getUISessions() {
		return uiSessionsView;
	}

	@Override
	public Collection<? extends SKUISession> getUISessions(Shopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		List<SKUISession> sessions = new ArrayList<>();
		for (SKUISession session : uiSessionsView) {
			if (session.getShopkeeper() == shopkeeper) {
				sessions.add(session);
			}
		}
		return sessions;
	}

	@Override
	public Collection<? extends SKUISession> getUISessions(Shopkeeper shopkeeper, UIType uiType) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.notNull(uiType, "uiType is null");
		List<SKUISession> sessions = new ArrayList<>();
		for (SKUISession session : uiSessionsView) {
			if (session.getShopkeeper() == shopkeeper && session.getUIType() == uiType) {
				sessions.add(session);
			}
		}
		return sessions;
	}

	@Override
	public Collection<? extends UISession> getUISessions(UIType uiType) {
		Validate.notNull(uiType, "uiType is null");
		List<SKUISession> sessions = new ArrayList<>();
		for (SKUISession session : uiSessionsView) {
			if (session.getUIType() == uiType) {
				sessions.add(session);
			}
		}
		return sessions;
	}

	@Override
	public SKUISession getUISession(Player player) {
		Validate.notNull(player, "player is null");
		return uiSessions.get(player.getUniqueId());
	}

	void onInventoryClose(Player player, InventoryCloseEvent closeEvent) {
		assert player != null;
		UISession session = this.getUISession(player);
		if (session == null) return;

		Log.debug(() -> "Player " + player.getName() + " closed UI '" + session.getUIType().getIdentifier() + "'.");
		this.endUISession(player, closeEvent);
	}

	public void onPlayerQuit(Player player) {
		// TODO This might have no effect, because CraftBukkit triggers an inventory close event prior to the player
		// quitting.
		this.endUISession(player, null);
	}

	// closeEvent might be null.
	void endUISession(Player player, InventoryCloseEvent closeEvent) {
		assert player != null;
		SKUISession session = uiSessions.remove(player.getUniqueId());
		if (session == null) return;

		this.onSessionEnd(session, closeEvent);
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

	// Called by SKUISession.
	void abort(SKUISession uiSession) {
		assert uiSession != null;
		if (!uiSession.isValid()) return;
		Player player = uiSession.getPlayer();
		this.endUISession(player, null);
		player.closeInventory();
	}

	@Override
	public void abortUISessions() {
		// Copy to prevent concurrent modifications:
		for (UISession session : new ArrayList<>(this.getUISessions())) {
			session.abort();
		}
		assert uiSessions.isEmpty();
	}

	@Override
	public void abortUISessions(Shopkeeper shopkeeper) {
		// Note: The returned collection is already a copy.
		for (UISession session : this.getUISessions(shopkeeper)) {
			session.abort();
		}
	}

	@Override
	public void abortUISessionsDelayed(Shopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");

		// Deactivate currently active UIs for this shopkeeper:
		this.deactivateUIs(shopkeeper);

		SchedulerUtils.runTaskOrOmit(plugin, () -> {
			this.abortUISessions(shopkeeper);
		});
	}

	private void deactivateUIs(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		for (SKUISession session : uiSessionsView) {
			if (session.getShopkeeper() == shopkeeper) {
				session.deactivateUI();
			}
		}
	}
}
