package com.nisovin.shopkeepers.ui;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.Validate;

public final class SKUISession implements UISession {

	private final UIHandler uiHandler;
	private final Player player;
	// Can be null for UIs that are not associated with some shopkeeper:
	private final AbstractShopkeeper shopkeeper;
	private boolean uiActive = true;
	private boolean valid = true;

	public SKUISession(UIHandler uiHandler, Player player, AbstractShopkeeper shopkeeper) {
		Validate.notNull(uiHandler, "uiHandler is null");
		Validate.notNull(player, "player is null");
		this.uiHandler = uiHandler;
		this.player = player;
		this.shopkeeper = shopkeeper; // Can be null
	}

	@Override
	public final AbstractUIType getUIType() {
		return uiHandler.getUIType();
	}

	public final UIHandler getUIHandler() {
		return uiHandler;
	}

	@Override
	public final Player getPlayer() {
		return player;
	}

	@Override
	public final AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	@Override
	public boolean isUIActive() {
		return uiActive;
	}

	@Override
	public void deactivateUI() {
		uiActive = false;
	}

	@Override
	public void activateUI() {
		uiActive = true;
	}

	final void onSessionEnd() {
		valid = false;
	}

	@Override
	public final boolean isValid() {
		return valid;
	}
}
