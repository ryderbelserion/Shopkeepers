package com.nisovin.shopkeepers.api.ui;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.types.TypeRegistry;

/**
 * Acts as registry for UI types and keeps track of which player has which UI currently opened.
 */
public interface UIRegistry<T extends UIType> extends TypeRegistry<T> {

	public UISession getSession(Player player);

	public UIType getOpenUIType(Player player);

	public void onInventoryClose(Player player);

	public void closeAll(Shopkeeper shopkeeper);

	public void closeAllDelayed(Shopkeeper shopkeeper);

	public void closeAll();
}
