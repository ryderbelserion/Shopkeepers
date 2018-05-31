package com.nisovin.shopkeepers.types;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.types.SelectableTypeRegistry;

public abstract class AbstractSelectableTypeRegistry<T extends AbstractSelectableType> extends AbstractTypeRegistry<T> implements SelectableTypeRegistry<T> {

	private static class Link<T> {
		private T next = null;
	}

	private final Map<String, Link<T>> links = new HashMap<>();
	private T first = null;
	private T last = null;

	protected AbstractSelectableTypeRegistry() {
	}

	@Override
	public void register(T type) {
		super.register(type);
		if (first == null) {
			first = type;
		}
		Link<T> link = new Link<>();
		links.put(type.getIdentifier(), link);
		if (last != null) {
			links.get(last.getIdentifier()).next = type;
		}
		last = type;
	}

	protected T getFirst() {
		return first;
	}

	protected T getNext(T current) {
		Link<T> link = (current != null) ? links.get(current.getIdentifier()) : null;
		return (link == null || link.next == null) ? first : link.next;
	}

	protected boolean canBeSelected(Player player, T type) {
		assert player != null && type != null;
		return type.isEnabled() && type.hasPermission(player);
	}

	protected T getNext(Player player, T current) {
		assert player != null;
		T next = current;

		int count = this.getRegisteredTypes().size();
		while (count > 0) {
			// automatically selects the first type, if next is null or if next is the last type
			next = this.getNext(next);
			if (this.canBeSelected(player, next)) {
				break;
			}
			count--;
		}

		// use the currently selected type (can be null) after it went through all types and didn't find one the player
		// can use:
		if (count == 0) {
			// check if the currently selected type can still be used by this player:
			if (current != null && !this.canBeSelected(player, current)) current = null;
			next = current;
		}
		return next;
	}

	// SELECTION MANAGEMENT

	// player name -> selected type
	protected final Map<String, T> selections = new HashMap<>();

	@Override
	public T getDefaultSelection(Player player) {
		return this.getNext(player, null);
	}

	@Override
	public T getSelection(Player player) {
		Validate.notNull(player);
		String playerName = player.getName();
		T current = selections.get(playerName);
		// if none is currently selected, let's search for the first type this player can use:
		if (current == null || !this.canBeSelected(player, current)) current = this.getNext(player, current);
		return current; // returns null if the player can use no type at all
	}

	@Override
	public T selectNext(Player player) {
		Validate.notNull(player);
		String playerName = player.getName();
		T current = selections.get(playerName);
		T next = this.getNext(player, current);
		if (next != null) {
			selections.put(playerName, next);
			this.onSelect(next, player);
		} else {
			// for now remember the current selection
			// selections.remove(playerName);
		}
		return next;
	}

	protected void onSelect(T type, Player selectedBy) {
		// inform type:
		type.onSelect(selectedBy);
	}

	@Override
	public void clearSelection(Player player) {
		assert player != null;
		String playerName = player.getName();
		selections.remove(playerName);
	}

	@Override
	public void clearAllSelections() {
		selections.clear();
	}
}
