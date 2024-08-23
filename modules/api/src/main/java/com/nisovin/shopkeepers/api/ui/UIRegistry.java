package com.nisovin.shopkeepers.api.ui;

import java.util.Collection;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.types.TypeRegistry;

/**
 * Registry of {@link UIType}s, and keeps track of the active {@link UISession}s of all players.
 *
 * @param <T>
 *            the internal type of {@link UIType} that is managed by this registry
 */
public interface UIRegistry<T extends UIType> extends TypeRegistry<T> {

	/**
	 * Gets all currently active {@link UISession}s.
	 * 
	 * @return an unmodifiable view on the current {@link UISession}s
	 */
	public Collection<? extends UISession> getUISessions();

	/**
	 * Gets all currently active {@link UISession}s that involve the specified {@link Shopkeeper}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @return an unmodifiable view on the current {@link UISession}s
	 */
	public Collection<? extends UISession> getUISessions(Shopkeeper shopkeeper);

	/**
	 * Gets all currently active {@link UISession}s of the specified {@link UIType} that involve the
	 * specified {@link Shopkeeper}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param uiType
	 *            the UI type, not <code>null</code>
	 * @return an unmodifiable view on the current {@link UISession}s
	 */
	public Collection<? extends UISession> getUISessions(
			Shopkeeper shopkeeper,
			UIType uiType
	);

	/**
	 * Gets all currently active {@link UISession}s of the specified {@link UIType}.
	 * 
	 * @param uiType
	 *            the UI type, not <code>null</code>
	 * @return an unmodifiable view on the current {@link UISession}s
	 */
	public Collection<? extends UISession> getUISessions(UIType uiType);

	/**
	 * Gets the currently active {@link UISession} of the specified player.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return the {@link UISession}, or <code>null</code> if there is none
	 */
	public @Nullable UISession getUISession(Player player);

	/**
	 * {@link UISession#abort() Aborts} all {@link UISession}s.
	 * <p>
	 * This may not be safe to call during UI / inventory events.
	 */
	public void abortUISessions();

	/**
	 * {@link UISession#abort() Aborts} all {@link UISession}s that involve the specified
	 * {@link Shopkeeper}.
	 * <p>
	 * This may not be safe to call during UI / inventory events.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 */
	public void abortUISessions(Shopkeeper shopkeeper);

	/**
	 * {@link UISession#deactivateUI() Deactivates} all UIs that involve the specified
	 * {@link Shopkeeper} and {@link UISession#abort() aborts} the {@link UISession}s after a short
	 * delay (within the next tick).
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 */
	public void abortUISessionsDelayed(Shopkeeper shopkeeper);
}
