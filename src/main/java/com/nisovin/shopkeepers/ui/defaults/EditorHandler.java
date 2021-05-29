package com.nisovin.shopkeepers.ui.defaults;

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
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.ui.AbstractUIType;
import com.nisovin.shopkeepers.ui.ShopkeeperUIHandler;
import com.nisovin.shopkeepers.ui.defaults.confirmations.ConfirmationUIConfig;
import com.nisovin.shopkeepers.ui.defaults.confirmations.ConfirmationUI;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

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

	protected EditorHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		super(uiType);
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
					// Note: This may not remember the previous editor state currently (such as the selected trades
					// page).
					SKShopkeepersPlugin.getInstance().getUIRegistry().requestUI(this.getEditorHandler().getUIType(), shopkeeper, player);
				});
				return true;
			}
		};
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
	protected abstract List<TradingRecipeDraft> getTradingRecipes();

	@Override
	protected void saveRecipes(Session session) {
		Player player = session.getPlayer();
		this.setRecipes(player, session.getRecipes());

		// Save the shopkeeper:
		shopkeeper.save();
	}

	/**
	 * Applies the given trading recipe drafts to the shopkeeper.
	 * <p>
	 * Any {@link TradingRecipeDraft#isValid() invalid} trading recipe drafts are passed to
	 * {@link #handleInvalidRecipeDraft(Player, TradingRecipeDraft)} and then ignored.
	 * 
	 * @param player
	 *            the editing player
	 * @param recipes
	 *            the trading recipe drafts
	 */
	protected void setRecipes(Player player, List<TradingRecipeDraft> recipes) {
		this.clearRecipes();
		for (TradingRecipeDraft recipe : recipes) {
			if (!recipe.isValid()) {
				this.handleInvalidRecipeDraft(player, recipe);
				continue;
			}
			this.addRecipe(recipe);
		}
	}

	/**
	 * This is called for every {@link TradingRecipeDraft#isValid() invalid} trading recipe draft when the trading
	 * recipes from the editor are applied to the shopkeeper.
	 * 
	 * @param player
	 *            the editing player
	 * @param recipe
	 *            the invalid trading recipe draft
	 */
	protected void handleInvalidRecipeDraft(Player player, TradingRecipeDraft recipe) {
	}

	/**
	 * Clears the shopkeeper's trading recipes.
	 */
	protected abstract void clearRecipes();

	/**
	 * Adds the given ({@link TradingRecipeDraft#isValid() valid}) trading recipe to the shopkeeper.
	 * 
	 * @param recipe
	 *            the trading recipe draft
	 */
	protected abstract void addRecipe(TradingRecipeDraft recipe);
}
