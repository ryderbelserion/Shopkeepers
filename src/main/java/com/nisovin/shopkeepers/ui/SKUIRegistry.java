package com.nisovin.shopkeepers.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.AbstractShopkeeper;
import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.types.AbstractTypeRegistry;
import com.nisovin.shopkeepers.util.Log;

public class SKUIRegistry extends AbstractTypeRegistry<AbstractUIType> implements UIRegistry<AbstractUIType> {

	private final ShopkeepersPlugin plugin;
	// player name -> ui session
	private final Map<String, SKUISession> playerSessions = new HashMap<>();
	private UIListener uiListener = null;

	public SKUIRegistry(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		assert uiListener == null;
		uiListener = new UIListener(this);
		Bukkit.getPluginManager().registerEvents(uiListener, plugin);
	}

	public void onDisable() {
		assert uiListener != null;
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
			Log.debug("Cannot open UI '" + uiIdentifier + "': This shopkeeper is not handling/supporting this type of user interface.");
			return false;
		}

		String playerName = player.getName();
		if (!uiHandler.canOpen(player)) {
			Log.debug("Cannot open UI '" + uiIdentifier + "' for '" + playerName + "'.");
			return false;
		}

		SKUISession oldSession = this.getSession(player);
		// filtering out duplicate open requests:
		if (oldSession != null && oldSession.getShopkeeper().equals(shopkeeper) && oldSession.getUIHandler().equals(uiHandler)) {
			Log.debug("UI '" + uiIdentifier + "'" + " is already opened for '" + playerName + "'.");
			return false;
		}

		Log.debug("Opening UI '" + uiIdentifier + "' ...");
		boolean isOpen = uiHandler.openWindow(player);
		if (isOpen) {
			Log.debug(uiIdentifier + " opened");
			// old window already should automatically have been closed by the new window.. no need currently, to do
			// that here
			playerSessions.put(playerName, new SKUISession(shopkeeper, uiHandler));
			return true;
		} else {
			Log.debug(uiIdentifier + " NOT opened");
			return false;
		}
	}

	@Override
	public SKUISession getSession(Player player) {
		Validate.notNull(player, "Player is null!");
		return playerSessions.get(player.getName());
	}

	@Override
	public AbstractUIType getOpenUIType(Player player) {
		SKUISession session = this.getSession(player);
		return (session != null ? session.getUIType() : null);
	}

	@Override
	public void onInventoryClose(Player player) {
		if (player == null) return;
		playerSessions.remove(player.getName());
	}

	@Override
	public void closeAll(Shopkeeper shopkeeper) {
		if (shopkeeper == null) return;
		assert shopkeeper != null;
		Iterator<Entry<String, SKUISession>> iter = playerSessions.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, SKUISession> entry = iter.next();
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

	@Override
	public void closeAllDelayed(Shopkeeper shopkeeper) {
		if (shopkeeper == null) return;

		// deactivate currently active UIs:
		shopkeeper.deactivateUI();

		// delayed because this is/was originally called from inside the PlayerCloseInventoryEvent
		Bukkit.getScheduler().runTask(plugin, () -> {
			closeAll(shopkeeper);

			// reactivate UIs:
			shopkeeper.activateUI();
		});
	}

	@Override
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
