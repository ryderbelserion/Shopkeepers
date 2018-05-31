package com.nisovin.shopkeepers.types;

import org.bukkit.entity.Player;

public interface SelectableTypeRegistry<T extends SelectableType> extends TypeRegistry<T> {

	// SELECTION MANAGEMENT

	/**
	 * Gets the first select-able type for this player, starting at the default one.
	 * 
	 * @param player
	 *            a player
	 * @return the first select-able type for this player, or <code>null</code> if this player can't select or use any
	 *         type at all
	 */
	public T getDefaultSelection(Player player);

	/**
	 * Gets the first select-able type for this player, starting at the currently selected one.
	 * 
	 * @param player
	 *            the player
	 * @return the first select-able type for this player, or <code>null</code> if this player can't select or use any
	 *         type at all
	 */
	public T getSelection(Player player);

	public T selectNext(Player player);

	public void clearSelection(Player player);

	public void clearAllSelections();
}
