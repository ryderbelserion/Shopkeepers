package com.nisovin.shopkeepers.api.ui;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

public interface UISession {

	/**
	 * Gets the {@link UIType}.
	 * 
	 * @return the UI type, not <code>null</code>
	 */
	public UIType getUIType();

	/**
	 * Gets the player involved in this UI session.
	 * 
	 * @return the player, not <code>null</code>
	 */
	public Player getPlayer();

	/**
	 * Gets the {@link Shopkeeper} associated with this UI session.
	 * 
	 * @return the shopkeeper, or <code>null</code> if there is no associated shopkeeper
	 */
	public Shopkeeper getShopkeeper();

	/**
	 * Checks if the UI is active currently.
	 * <p>
	 * When the UI has been {@link #deactivateUI() deactivated} any inventory interactions by the player are ignored
	 * (they get cancelled, but not processed any further beyond that).
	 * <p>
	 * A typical use for this is to deactivate the UI when the session is about to end / when the inventory view is
	 * about to get closed. This is usually required because it is not safe to immediately close the inventory view as
	 * reaction to inventory interactions by the player. Instead the closing of the inventory view is postponed to
	 * happen the next tick. The UI can be deactivated in this case to prevent the processing of any further inventory
	 * interactions that might otherwise happen in the meantime.
	 * 
	 * @return <code>true</code> if the UI is active
	 */
	public boolean isUIActive();

	/**
	 * Deactivates the UI.
	 * 
	 * @see #isUIActive()
	 */
	public void deactivateUI();

	/**
	 * Activates the UI.
	 * 
	 * @see #isUIActive()
	 */
	public void activateUI();

	/**
	 * Checks if this session is still valid.
	 * <p>
	 * The session becomes invalid once the player has closed the inventory view.
	 * 
	 * @return <code>true</code> if the session is still valid
	 */
	public boolean isValid();
}
