package com.nisovin.shopkeepers.ui.editor;

import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.bukkit.SoundEffect;

public abstract class Button {

	// Volume 0.25 matches Minecraft's default button click volume.
	protected static final SoundEffect DEFAULT_BUTTON_CLICK_SOUND = new SoundEffect(Sound.UI_BUTTON_CLICK)
			.withVolume(0.25f);

	static final int NO_SLOT = -1;

	private final boolean placeAtEnd;

	private @Nullable AbstractEditorHandler editorHandler;
	private int slot = NO_SLOT;

	public Button() {
		this(false);
	}

	public Button(boolean placeAtEnd) {
		this.placeAtEnd = placeAtEnd;
	}

	void setEditorHandler(AbstractEditorHandler editorHandler) {
		if (this.editorHandler != null) {
			throw new IllegalStateException(
					"The button has already been added to some editor handler!"
			);
		}
		this.editorHandler = editorHandler;
	}

	boolean isPlaceAtEnd() {
		return placeAtEnd;
	}

	int getSlot() {
		return slot;
	}

	void setSlot(int slot) {
		this.slot = slot;
	}

	protected boolean isApplicable(AbstractEditorHandler editorHandler) {
		return true;
	}

	protected @Nullable AbstractEditorHandler getEditorHandler() {
		return editorHandler;
	}

	public abstract @Nullable ItemStack getIcon(EditorSession editorSession);

	// Updates the icon in all sessions.
	// Note: Cannot deal with changes to the registered buttons (the button's slot) while the
	// inventory is open.
	protected final void updateIcon() {
		if (slot != NO_SLOT && editorHandler != null) {
			editorHandler.updateButtonInAllSessions(this);
		}
	}

	// Updates all icons in all sessions.
	protected final void updateAllIcons() {
		if (editorHandler != null) {
			editorHandler.updateButtonsInAllSessions();
		}
	}

	protected abstract void onClick(EditorSession editorSession, InventoryClickEvent clickEvent);
}
