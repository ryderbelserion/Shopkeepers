package com.nisovin.shopkeepers.ui;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.types.TypeRegistry;

/**
 * Acts as registry for UI types and keeps track of which player has which UI currently opened.
 */
public interface UIRegistry extends TypeRegistry<UIType> {

	public boolean requestUI(UIType uiType, Shopkeeper shopkeeper, Player player);

	public UISession getSession(Player player);

	public UIType getOpenUIType(Player player);

	public void onInventoryClose(Player player);

	public void closeAll(Shopkeeper shopkeeper);

	public void closeAllDelayed(Shopkeeper shopkeeper);

	public void closeAll();
}
