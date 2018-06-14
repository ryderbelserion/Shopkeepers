package com.nisovin.shopkeepers.api.ui;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

public interface UISession {

	public Shopkeeper getShopkeeper();

	public UIType getUIType();
}
