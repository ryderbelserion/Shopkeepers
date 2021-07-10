package com.nisovin.shopkeepers.api.ui;

import java.util.Collection;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.types.TypeRegistry;

/**
 * Acts as registry for UI types and keeps track of which player has which UI currently opened.
 */
public interface UIRegistry<T extends UIType> extends TypeRegistry<T> {

	/**
	 * Gets all currently active UI sessions.
	 * 
	 * @return an unmodifiable view on the current UI sessions
	 */
	public Collection<? extends UISession> getUISessions();

	/**
	 * Gets all currently active UI sessions involving the given {@link Shopkeeper}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper
	 * @return an unmodifiable view on the current UI sessions
	 */
	public Collection<? extends UISession> getUISessions(Shopkeeper shopkeeper);

	/**
	 * Gets all currently active UI sessions involving the given {@link Shopkeeper} and the specified {@link UIType}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper
	 * @param uiType
	 *            the UI type
	 * @return an unmodifiable view on the current UI sessions
	 */
	public Collection<? extends UISession> getUISessions(Shopkeeper shopkeeper, UIType uiType);

	/**
	 * Gets all currently active UI sessions involving the specified {@link UIType}.
	 * 
	 * @param uiType
	 *            the UI type
	 * @return an unmodifiable view on the current UI sessions
	 */
	public Collection<? extends UISession> getUISessions(UIType uiType);

	/**
	 * Gets the currently active {@link UISession} for the given player.
	 * 
	 * @param player
	 *            the player
	 * @return the UI session, or <code>null</code> if there is none
	 */
	public UISession getUISession(Player player);

	/**
	 * Gets the currently active {@link UISession} for the given player.
	 * 
	 * @param player
	 *            the player
	 * @return the UI session, or <code>null</code> if there is none
	 * @deprecated {@link #getUISession(Player)}
	 */
	public UISession getSession(Player player);

	/**
	 * @deprecated Use {@link #getSession(Player)} directly
	 */
	public UIType getOpenUIType(Player player);

	/**
	 * {@link UISession#abort() Aborts} all UI sessions.
	 * <p>
	 * This may not be safe to call during UI / inventory events.
	 */
	public void abortUISessions();

	/**
	 * {@link UISession#abort() Aborts} all UI sessions involving the given {@link Shopkeeper}.
	 * <p>
	 * This may not be safe to call during UI / inventory events.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper
	 */
	public void abortUISessions(Shopkeeper shopkeeper);

	/**
	 * {@link UISession#deactivateUI() Deactivates} all UIs involving the given {@link Shopkeeper} and
	 * {@link UISession#abort() aborts} the UI sessions after a short delay (within the next tick).
	 * 
	 * @param shopkeeper
	 *            the shopkeeper
	 */
	public void abortUISessionsDelayed(Shopkeeper shopkeeper);

	/**
	 * Closes all UI sessions involving the given {@link Shopkeeper}.
	 * <p>
	 * This may not be safe to call during UI / inventory events.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper
	 * @deprecated {@link #abortUISessions(Shopkeeper)}
	 */
	public void closeAll(Shopkeeper shopkeeper);

	/**
	 * {@link UISession#deactivateUI() Deactivates} all UI sessions involving the given {@link Shopkeeper} and closes
	 * them after a short delay (within the next tick).
	 * 
	 * @param shopkeeper
	 *            the shopkeeper
	 * @deprecated {@link #abortUISessionsDelayed(Shopkeeper)}
	 */
	public void closeAllDelayed(Shopkeeper shopkeeper);

	/**
	 * Closes all UI sessions.
	 * <p>
	 * This may not be safe to call during UI / inventory events.
	 * 
	 * @deprecated {@link #abortUISessions()}
	 */
	public void closeAll();
}
