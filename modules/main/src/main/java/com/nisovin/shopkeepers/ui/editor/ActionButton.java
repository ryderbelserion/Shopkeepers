package com.nisovin.shopkeepers.ui.editor;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * A {@link Button} for simple one-click actions.
 */
public abstract class ActionButton extends Button {

	public ActionButton() {
		super();
	}

	public ActionButton(boolean placeAtEnd) {
		super(placeAtEnd);
	}

	protected void playButtonClickSound(Player player, boolean actionSuccess) {
		DEFAULT_BUTTON_CLICK_SOUND.play(player);
	}

	@Override
	protected final void onClick(EditorSession editorSession, InventoryClickEvent clickEvent) {
		if (clickEvent.getClick() == ClickType.DOUBLE_CLICK) return; // Ignore double clicks

		// Run action:
		boolean success = this.runAction(editorSession, clickEvent);
		if (!success) return;

		// Post-processing:
		this.onActionSuccess(editorSession, clickEvent);

		// Play sound:
		this.playButtonClickSound(editorSession.getPlayer(), success);

		// Icon might have changed:
		this.updateIcon();
	}

	// Returns true on success:
	protected abstract boolean runAction(
			EditorSession editorSession,
			InventoryClickEvent clickEvent
	);

	protected void onActionSuccess(EditorSession editorSession, InventoryClickEvent clickEvent) {
		// Nothing by default.
	}
}
