package com.nisovin.shopkeepers.ui;

import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;

public class SKUISession implements UISession {

	// reference shopkeeper directly and not by id, because the id might change or currently be invalid
	// (for inactive shopkeepers).. especially important for remotely opened windows
	private final AbstractShopkeeper shopkeeper;
	private final UIHandler uiHandler;

	public SKUISession(AbstractShopkeeper shopkeeper, UIHandler handler) {
		this.shopkeeper = shopkeeper;
		this.uiHandler = handler;
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
}
