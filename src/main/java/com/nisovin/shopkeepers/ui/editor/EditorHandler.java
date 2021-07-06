package com.nisovin.shopkeepers.ui.editor;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerDeleteShopkeeperEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.event.ShopkeeperEventHelper;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.ui.AbstractUIType;
import com.nisovin.shopkeepers.ui.ShopkeeperUIHandler;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUI;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUIConfig;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class EditorHandler extends AbstractEditorHandler implements ShopkeeperUIHandler {

	private static final ConfirmationUIConfig CONFIRMATION_UI_CONFIG_DELETE_SHOP = new ConfirmationUIConfig() {
		@Override
		public String getTitle() {
			return Messages.confirmationUiDeleteShopTitle;
		}

		@Override
		public List<String> getConfirmationLore() {
			return Messages.confirmationUiDeleteShopConfirmLore;
		}
	};

	private final AbstractShopkeeper shopkeeper;

	protected EditorHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper, TradingRecipesAdapter tradingRecipesAdapter) {
		super(uiType, tradingRecipesAdapter);
		Validate.notNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	@Override
	public AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	@Override
	protected String getEditorTitle() {
		return Messages.editorTitle;
	}

	// EDITOR BUTTONS

	// A button for simple one-click shopkeeper editing actions. Successful actions trigger a ShopkeeperEditedEvent and
	// a save of the shopkeeper.
	public static abstract class ShopkeeperActionButton extends ActionButton {

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

			// Call event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

			// Save:
			shopkeeper.save();
		}
	}

	@Override
	protected ItemStack createTradeSetupIcon() {
		ShopType<?> shopType = this.getShopkeeper().getType();
		String itemName = StringUtils.replaceArguments(Messages.tradeSetupDescHeader, "shopType", shopType.getDisplayName());
		List<String> itemLore = shopType.getTradeSetupDescription();
		return ItemUtils.setDisplayNameAndLore(Settings.tradeSetupItem.createItemStack(), itemName, itemLore);
	}

	@Override
	protected final void setupButtons() {
		super.setupButtons();
		this.setupShopkeeperButtons();
		this.setupShopObjectButtons();
	}

	protected void setupShopkeeperButtons() {
		this.addButtonOrIgnore(this.createDeleteButton());
		this.addButtonOrIgnore(this.createNamingButton());
	}

	protected void setupShopObjectButtons() {
		this.addButtonsOrIgnore(shopkeeper.getShopObject().createEditorButtons());
	}

	protected Button createDeleteButton() {
		return new ActionButton(true) {
			@Override
			public ItemStack getIcon(Session session) {
				return Settings.createDeleteButtonItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				getUISession(player).closeDelayedAndRunTask(() -> requestConfirmationDeleteShop(player));
				return true;
			}
		};
	}

	private void requestConfirmationDeleteShop(Player player) {
		ConfirmationUI.requestConfirmation(player, CONFIRMATION_UI_CONFIG_DELETE_SHOP, () -> {
			// Delete confirmed.
			if (!player.isValid()) return;
			if (!shopkeeper.isValid()) {
				// The shopkeeper has already been removed in the meantime.
				TextUtils.sendMessage(player, Messages.shopAlreadyRemoved);
				return;
			}

			// Call event:
			PlayerDeleteShopkeeperEvent deleteEvent = ShopkeeperEventHelper.callPlayerDeleteShopkeeperEvent(shopkeeper, player);
			Bukkit.getPluginManager().callEvent(deleteEvent);
			if (!deleteEvent.isCancelled()) {
				// Delete the shopkeeper and save:
				shopkeeper.delete(player);
				shopkeeper.save();

				TextUtils.sendMessage(player, Messages.shopRemoved);
			}
			// Else: Cancelled by another plugin.
			// Note: We don't send a message in this case here, because we expect that the other plugin sends a
			// more specific message anyways if it wants to inform the player.
		}, () -> {
			// Delete cancelled.
			if (!player.isValid()) return;
			if (!shopkeeper.isValid()) return;

			// Try to open the editor again:
			// Note: This may currently not remember the previous editor state (such as the selected trades page).
			SKShopkeepersPlugin.getInstance().getUIRegistry().requestUI(this.getUIType(), shopkeeper, player);
		});
	}

	protected Button createNamingButton() {
		boolean useNamingButton = true;
		if (shopkeeper.getType() instanceof PlayerShopType) {
			// Naming via button enabled?
			if (Settings.namingOfPlayerShopsViaItem) {
				useNamingButton = false;
			} else {
				// No naming button for Citizens player shops if renaming is disabled for those.
				// TODO Restructure this to allow for dynamic editor buttons depending on shop (object) types and
				// settings.
				if (!Settings.allowRenamingOfPlayerNpcShops && shopkeeper.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN()) {
					useNamingButton = false;
				}
			}
		}
		if (!useNamingButton) return null;

		return new ActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return Settings.createNameButtonItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				// Also triggers save:
				getUISession(player).closeDelayed();

				// Start naming:
				SKShopkeepersPlugin.getInstance().getShopkeeperNaming().startNaming(player, shopkeeper);
				TextUtils.sendMessage(player, Messages.typeNewName);
				return true;
			}
		};
	}

	@Override
	protected void saveRecipes(Session session) {
		assert shopkeeper.isValid(); // UI sessions are aborted (i.e. not saved) when the shopkeeper is removed
		Player player = session.getPlayer();
		int changedOffers = tradingRecipesAdapter.updateTradingRecipes(player, session.getRecipes());
		if (changedOffers == 0) {
			Log.debug("No shopkeeper offers changed.");
		} else {
			Log.debug(() -> changedOffers + " shopkeeper offer(s) changed.");

			// Call event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

			// TODO Close all other UI sessions for the shopkeeper (eg. trading players)? Also send a message to them.
		}

		// Even if no trades have changed, the shopkeeper might have been marked as dirty due to other editor options.
		// If this is the case, we trigger a save here. Otherwise, we omit the save.
		if (shopkeeper.isDirty()) {
			shopkeeper.save();
		}
	}
}
