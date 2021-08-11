package com.nisovin.shopkeepers.ui.editor;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * The editor state of a player.
 */
public class Session {

	private final Player player;
	private final List<TradingRecipeDraft> recipes;
	private final Inventory inventory;
	private int currentPage = 1;

	protected Session(Player player, List<TradingRecipeDraft> recipes, Inventory inventory) {
		Validate.notNull(player, "player is null");
		Validate.notNull(recipes, "recipes is null");
		Validate.notNull(inventory, "inventory is null");
		this.player = player;
		this.recipes = recipes;
		this.inventory = inventory;
	}

	public final Player getPlayer() {
		return player;
	}

	public final Inventory getInventory() {
		return inventory;
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
