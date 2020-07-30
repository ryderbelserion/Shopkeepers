package com.nisovin.shopkeepers.ui;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;

public class SKUISession implements UISession {

	// Reference shopkeeper directly and not by id, because the id might change or currently be invalid
	// (for inactive shopkeepers).. especially important for remotely opened windows.
	private final AbstractShopkeeper shopkeeper;
	private final UIHandler uiHandler;
	private final Player player;
	private boolean valid = true;

	public SKUISession(AbstractShopkeeper shopkeeper, UIHandler handler, Player player) {
		this.shopkeeper = shopkeeper;
		this.uiHandler = handler;
		this.player = player;
	}

	@Override
	public AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	public UIHandler getUIHandler() {
		return uiHandler;
	}

	@Override
	public AbstractUIType getUIType() {
		return uiHandler.getUIType();
	}

	public Player getPlayer() {
		return player;
	}

	void onSessionEnd() {
		valid = false;
	}

	@Override
	public boolean isValid() {
		return valid;
	}
}
