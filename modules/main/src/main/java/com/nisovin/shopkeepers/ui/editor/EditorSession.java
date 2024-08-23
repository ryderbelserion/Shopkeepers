package com.nisovin.shopkeepers.ui.editor;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * The editor state of a player.
 */
public class EditorSession {

	private final UISession uiSession;
	private final List<TradingRecipeDraft> recipes;
	private final Inventory inventory;
	private int currentPage = 1;

	protected EditorSession(
			UISession uiSession,
			List<TradingRecipeDraft> recipes,
			Inventory inventory
	) {
		Validate.notNull(uiSession, "uiSession is null");
		Validate.notNull(recipes, "recipes is null");
		Validate.notNull(inventory, "inventory is null");
		this.uiSession = uiSession;
		this.recipes = recipes;
		this.inventory = inventory;
	}

	public final UISession getUISession() {
		return uiSession;
	}

	public final Player getPlayer() {
		return uiSession.getPlayer();
	}

	public final Inventory getInventory() {
		return inventory;
	}

	public final void updateInventory() {
		this.getPlayer().updateInventory();
	}

	// Starts at 1.
	public final int getCurrentPage() {
		return currentPage;
	}

	void setPage(int newPage) {
		this.currentPage = newPage;
	}

	public final List<TradingRecipeDraft> getRecipes() {
		return recipes;
	}
}
