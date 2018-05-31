package com.nisovin.shopkeepers.api.ui;

import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;

public interface DefaultUITypes {

	public List<? extends UIType> getAllUITypes();

	public UIType getEditorUIType();

	public UIType getTradingUIType();

	public UIType getHiringUIType();

	// STATICS (for convenience):

	public static DefaultUITypes getInstance() {
		return ShopkeepersPlugin.getInstance().getDefaultUITypes();
	}

	public static UIType EDITOR() {
		return getInstance().getEditorUIType();
	}

	public static UIType TRADING() {
		return getInstance().getTradingUIType();
	}

	public static UIType HIRING() {
		return getInstance().getHiringUIType();
	}
}
