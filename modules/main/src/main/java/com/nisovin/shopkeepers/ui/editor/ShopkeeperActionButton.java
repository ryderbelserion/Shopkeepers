package com.nisovin.shopkeepers.ui.editor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

/**
 * An {@link ActionButton} for simple one-click shopkeeper editing actions.
 * <p>
 * Successful actions trigger a {@link ShopkeeperEditedEvent} and a save of the shopkeeper.
 */
public abstract class ShopkeeperActionButton extends ActionButton {

	public ShopkeeperActionButton() {
		this(false);
	}

	public ShopkeeperActionButton(boolean placeAtEnd) {
		super(placeAtEnd);
	}

	@Override
	protected boolean isApplicable(AbstractEditorHandler editorHandler) {
		return super.isApplicable(editorHandler) && (editorHandler instanceof EditorHandler);
	}

	protected Shopkeeper getShopkeeper() {
		assert this.getEditorHandler() instanceof EditorHandler;
		return ((EditorHandler) this.getEditorHandler()).getShopkeeper();
	}

	@Override
	protected void onActionSuccess(InventoryClickEvent clickEvent, Player player) {
		Shopkeeper shopkeeper = this.getShopkeeper();

		// Call shopkeeper edited event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

		// Save:
		shopkeeper.save();
	}
}
