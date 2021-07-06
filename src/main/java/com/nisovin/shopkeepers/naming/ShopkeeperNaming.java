package com.nisovin.shopkeepers.naming;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.chatinput.ChatInput;
import com.nisovin.shopkeepers.chatinput.ChatInput.Request;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

public class ShopkeeperNaming {

	private final ChatInput chatInput;

	public ShopkeeperNaming(ChatInput chatInput) {
		Validate.notNull(chatInput, "chatInput is null");
		this.chatInput = chatInput;
	}

	public void onEnable() {
	}

	public void onDisable() {
	}

	public void startNaming(Player player, Shopkeeper shopkeeper) {
		Validate.notNull(player, "player is null");
		Validate.notNull(shopkeeper, "shopkeeper is null");
		chatInput.request(player, new ShopkeeperNamingChatInputRequest(player, shopkeeper));
	}

	private class ShopkeeperNamingChatInputRequest implements Request {

		private final Player player;
		private final Shopkeeper shopkeeper;

		ShopkeeperNamingChatInputRequest(Player player, Shopkeeper shopkeeper) {
			assert player != null && shopkeeper != null;
			this.player = player;
			this.shopkeeper = shopkeeper;
		}

		@Override
		public void onChatInput(String message) {
			requestNameChange(player, shopkeeper, message);
		}
	}

	public void abortNaming(Player player) {
		Validate.notNull(player, "player is null");
		Request request = chatInput.getRequest(player);
		if (request instanceof ShopkeeperNamingChatInputRequest) {
			chatInput.abortRequest(player);
		}
	}

	public boolean requestNameChange(Player player, Shopkeeper shopkeeper, String newName) {
		Validate.notNull(player, "player is null");
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.notNull(newName, "newName is null");
		if (!shopkeeper.isValid()) return false;

		// Prepare the new name:
		newName = newName.trim();

		// Update name:
		if (newName.isEmpty() || newName.equals("-")) {
			// Remove name:
			newName = "";
		} else {
			// Validate name:
			if (!((AbstractShopkeeper) shopkeeper).isValidName(newName)) {
				TextUtils.sendMessage(player, Messages.nameInvalid);
				return false;
			}
		}

		// Apply new name:
		String oldName = shopkeeper.getName();
		shopkeeper.setName(newName);

		// Compare to previous name:
		if (oldName.equals(shopkeeper.getName())) {
			TextUtils.sendMessage(player, Messages.nameHasNotChanged);
			return false;
		}

		// Inform player:
		TextUtils.sendMessage(player, Messages.nameSet);

		// Close all open windows:
		shopkeeper.abortUISessionsDelayed(); // TODO Really needed?

		// Call event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

		// Save:
		shopkeeper.save();

		return true;
	}
}
