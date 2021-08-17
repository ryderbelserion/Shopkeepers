package com.nisovin.shopkeepers.ui;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Basic implementation of {@link ShopkeeperUIHandler}.
 */
public abstract class AbstractShopkeeperUIHandler extends UIHandler implements ShopkeeperUIHandler {

	private final AbstractShopkeeper shopkeeper; // Not null

	protected AbstractShopkeeperUIHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		super(uiType);
		Validate.notNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	@Override
	public AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	@Override
	protected void debugNotOpeningUI(Player player, String reason) {
		Validate.notNull(player, "player is null");
		Validate.notEmpty(reason, "reason is null or empty");
		Log.debug(() -> shopkeeper.getLogPrefix() + "Not opening UI '" + this.getUIType().getIdentifier()
				+ "' for player " + player.getName() + ": " + reason);
	}
}
