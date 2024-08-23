package com.nisovin.shopkeepers.types;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.types.SelectableTypeRegistry;
import com.nisovin.shopkeepers.util.java.Validate;

public abstract class AbstractSelectableTypeRegistry<T extends AbstractSelectableType>
		extends AbstractTypeRegistry<T> implements SelectableTypeRegistry<T> {

	private static class Link<T> {
		private @Nullable T prev = null;
		private @Nullable T next = null;
	}

	private final Map<String, Link<T>> links = new HashMap<>();
	private @Nullable T first = null;
	private @Nullable T last = null;

	protected AbstractSelectableTypeRegistry() {
	}

	@Override
	public void register(@NonNull T type) {
		super.register(type);
		if (first == null) {
			first = type;
		}
		Link<T> link = new Link<>();
		links.put(type.getIdentifier(), link);
		if (last != null) {
			Link<T> lastLink = Unsafe.assertNonNull(links.get(last.getIdentifier()));
			lastLink.next = type;
			link.prev = last;
		}
		last = type;
	}

	protected @Nullable T getFirst() {
		return first;
	}

	protected @Nullable T getLast() {
		return last;
	}

	protected @Nullable T getNext(@Nullable T current) {
		Link<T> link = (current != null) ? links.get(current.getIdentifier()) : null;
		if (link == null) return first;
		return (link.next == null) ? first : link.next;
	}

	protected @Nullable T getPrevious(@Nullable T current) {
		Link<T> link = (current != null) ? links.get(current.getIdentifier()) : null;
		if (link == null) return first;
		return (link.prev == null) ? last : link.prev;
	}

	protected boolean canBeSelected(Player player, @NonNull T type) {
		assert player != null && type != null;
		return type.isEnabled() && type.hasPermission(player);
	}

	protected @Nullable T getNext(Player player, @Nullable T current) {
		assert player != null;
		@Nullable T next = current;

		int count = this.getRegisteredTypes().size();
		while (count > 0) {
			// Automatically selects the first type if next is null or if next is the last type:
			next = Unsafe.assertNonNull(this.getNext(next));
			if (this.canBeSelected(player, next)) {
				break;
			}
			count--;
		}

		// Use the currently selected type (can be null) after we went through all types and didn't
		// find one the player can use:
		if (count == 0) {
			// Check if the currently selected type can still be used by this player:
			if (current != null && !this.canBeSelected(player, current)) {
				next = null;
			} else {
				next = current;
			}
		}
		return next;
	}

	protected @Nullable T getPrevious(Player player, @Nullable T current) {
		assert player != null;
		@Nullable T previous = current;

		int count = this.getRegisteredTypes().size();
		while (count > 0) {
			// Automatically selects the first type if prev is null, or the last type if prev is the
			// first type:
			previous = Unsafe.assertNonNull(this.getPrevious(previous));
			if (this.canBeSelected(player, previous)) {
				break;
			}
			count--;
		}

		// Use the currently selected type (can be null) after we went through all types and didn't
		// find one the player can use:
		if (count == 0) {
			// Check if the currently selected type can still be used by this player:
			if (current != null && !this.canBeSelected(player, current)) {
				previous = null;
			} else {
				previous = current;
			}
		}
		return previous;
	}

	// SELECTION MANAGEMENT

	// Player name -> selected type
	protected final Map<String, @NonNull T> selections = new HashMap<>();

	@Override
	public @Nullable T getDefaultSelection(Player player) {
		return this.getNext(player, null);
	}

	@Override
	public @Nullable T getSelection(Player player) {
		Validate.notNull(player, "player is null");
		String playerName = Unsafe.assertNonNull(player.getName());
		@Nullable T current = selections.get(playerName);
		// If none is currently selected, let's search for the first type this player can use:
		if (current == null || !this.canBeSelected(player, current)) {
			current = this.getNext(player, current);
		}
		return current; // Returns null if the player can not use any type
	}

	@Override
	public @Nullable T selectNext(Player player) {
		Validate.notNull(player, "player is null");
		String playerName = Unsafe.assertNonNull(player.getName());
		@Nullable T current = selections.get(playerName);
		@Nullable T next = this.getNext(player, current);
		if (next != null) {
			selections.put(playerName, next);
			this.onSelect(next, player);
		} else {
			// For now remember the current selection.
			// selections.remove(playerName);
		}
		return next;
	}

	@Override
	public @Nullable T selectPrevious(Player player) {
		Validate.notNull(player, "player is null");
		String playerName = Unsafe.assertNonNull(player.getName());
		@Nullable T current = selections.get(playerName);
		@Nullable T prev = this.getPrevious(player, current);
		if (prev != null) {
			selections.put(playerName, prev);
			this.onSelect(prev, player);
		} else {
			// For now remember the current selection.
			// selections.remove(playerName);
		}
		return prev;
	}

	protected void onSelect(@NonNull T type, Player selectedBy) {
		// Inform type:
		type.onSelect(selectedBy);
	}

	@Override
	public void clearSelection(Player player) {
		Validate.notNull(player, "player is null");
		String playerName = Unsafe.assertNonNull(player.getName());
		selections.remove(playerName);
	}

	@Override
	public void clearAllSelections() {
		selections.clear();
	}
}
