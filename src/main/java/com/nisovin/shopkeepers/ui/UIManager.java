package com.nisovin.shopkeepers.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.types.TypeRegistry;
import com.nisovin.shopkeepers.util.Log;

/**
 * Acts as registry for ui types and keeps track of which player has which ui currently opened.
 */
public class UIManager extends TypeRegistry<UIType> {

	// player name -> ui session
	private final Map<String, UISession> playerSessions = new HashMap<String, UISession>();
	private UIListener uiListener = null;

	public UIManager() {
	}

	public void onEnable(ShopkeepersPlugin plugin) {
		assert uiListener == null;
		uiListener = new UIListener(this);
		Bukkit.getPluginManager().registerEvents(uiListener, plugin);
	}

	public void onDisable(ShopkeepersPlugin plugin) {
		assert uiListener != null;
		HandlerList.unregisterAll(uiListener);
		uiListener = null;
	}

	@Override
	protected String getTypeName() {
		return "UIManager";
	}

	public boolean requestUI(UIType uiType, Shopkeeper shopkeeper, Player player) {
		Validate.notNull(uiType, "UI type is null!");
		Validate.notNull(shopkeeper, "Shopkeeper is null!");
		Validate.notNull(player, "Player is null!");

		String uiIdentifier = uiType.getIdentifier();
		UIHandler uiHandler = shopkeeper.getUIHandler(uiType);
		if (uiHandler == null) {
			Log.debug("Cannot open " + uiIdentifier + ": This shopkeeper is not handling/supporting this type of user interface.");
			return false;
		}

		String playerName = player.getName();
		if (!uiHandler.canOpen(player)) {
			Log.debug("Cannot open " + uiIdentifier + " for '" + playerName + "'.");
			return false;
		}

		UISession oldSession = this.getSession(player);
		// filtering out duplicate open requests:
		if (oldSession != null && oldSession.getShopkeeper().equals(shopkeeper) && oldSession.getUIHandler().equals(uiHandler)) {
			Log.debug(uiIdentifier + " is already opened for '" + playerName + "'.");
			return false;
		}

		Log.debug("Opening " + uiIdentifier + " ...");
		boolean isOpen = uiHandler.openWindow(player);
		if (isOpen) {
			Log.debug(uiIdentifier + " opened");
			// old window already should automatically have been closed by the new window.. no need currently, to do
			// that here
			playerSessions.put(playerName, new UISession(shopkeeper, uiHandler));
			return true;
		} else {
			Log.debug(uiIdentifier + " NOT opened");
			return false;
		}
	}

	UISession getSession(Player player) {
		if (player != null) {
			return playerSessions.get(player.getName());
		}
		return null;
	}

	public UIType getOpenInterface(Player player) {
		UISession session = this.getSession(player);
		return session != null ? session.getUIType() : null;
	}

	public void onInventoryClose(Player player) {
		if (player == null) return;
		playerSessions.remove(player.getName());
	}

	public void closeAll(Shopkeeper shopkeeper) {
		if (shopkeeper == null) return;
		assert shopkeeper != null;
		Iterator<Entry<String, UISession>> iter = playerSessions.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, UISession> entry = iter.next();
			UISession session = entry.getValue();
			if (session.getShopkeeper().equals(shopkeeper)) {
				iter.remove();
				Player player = Bukkit.getPlayerExact(entry.getKey());
				if (player != null) {
					player.closeInventory();
				}
			}
		}
	}

	public void closeAllDelayed(Shopkeeper shopkeeper) {
		if (shopkeeper == null) return;

		// deactivate currently active UIs:
		shopkeeper.deactivateUI();

		// delayed because this is/was originally called from inside the PlayerCloseInventoryEvent
		Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
			closeAll(shopkeeper);

			// reactivate UIs:
			shopkeeper.activateUI();
		});
	}

	public void closeAll() {
		for (String playerName : playerSessions.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
				player.closeInventory();
			}
		}
		playerSessions.clear();
	}
}
