package com.nisovin.shopkeepers.shopkeeper.admin.regular;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.editor.DefaultTradingRecipesAdapter;
import com.nisovin.shopkeepers.ui.editor.EditorHandler;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class RegularAdminShopEditorHandler extends EditorHandler {

	private static class TradingRecipesAdapter extends DefaultTradingRecipesAdapter<@NonNull TradeOffer> {

		private final SKRegularAdminShopkeeper shopkeeper;

		private TradingRecipesAdapter(SKRegularAdminShopkeeper shopkeeper) {
			assert shopkeeper != null;
			this.shopkeeper = shopkeeper;
		}

		@Override
		public List<@NonNull TradingRecipeDraft> getTradingRecipes() {
			// Add the shopkeeper's offers:
			List<? extends @NonNull TradeOffer> offers = shopkeeper.getOffers();
			// With heuristic initial capacity:
			List<@NonNull TradingRecipeDraft> recipes = new ArrayList<>(offers.size() + 8);
			offers.forEach(offer -> {
				// The offer returns immutable items, so there is no need to copy them.
				TradingRecipeDraft recipe = new TradingRecipeDraft(
						offer.getResultItem(),
						offer.getItem1(),
						offer.getItem2()
				);
				recipes.add(recipe);
			});
			return recipes;
		}

		@Override
		protected List<? extends @NonNull TradeOffer> getOffers() {
			return shopkeeper.getOffers();
		}

		@Override
		protected void setOffers(List<? extends @NonNull TradeOffer> newOffers) {
			shopkeeper.setOffers(newOffers);
		}

		@Override
		protected @Nullable TradeOffer createOffer(TradingRecipeDraft recipe) {
			assert recipe != null && recipe.isValid();
			// We can reuse the trading recipe draft's items without copying them first.
			UnmodifiableItemStack resultItem = Unsafe.assertNonNull(recipe.getResultItem());
			UnmodifiableItemStack item1 = Unsafe.assertNonNull(recipe.getRecipeItem1());
			UnmodifiableItemStack item2 = recipe.getRecipeItem2();
			return TradeOffer.create(resultItem, item1, item2);
		}

		// TODO Remove this? Maybe handle the trades setup similar to the player trading shop:
		// Copying the selected items into the editor.
		@Override
		protected void handleInvalidTradingRecipe(Player player, TradingRecipeDraft invalidRecipe) {
			// Return unused items to the player's inventory:
			// Inventory#addItem might modify the stack sizes of the input items, so we need to copy
			// them.
			ItemStack resultItem = ItemUtils.copyOrNull(invalidRecipe.getResultItem());
			ItemStack item1 = ItemUtils.copyOrNull(invalidRecipe.getItem1());
			ItemStack item2 = ItemUtils.copyOrNull(invalidRecipe.getItem2());
			PlayerInventory playerInventory = player.getInventory();

			// Note: If the items don't fit the inventory, we ignore them rather than dropping them.
			// This is usually safer than having admins accidentally drop items when they are
			// setting up admin shops.
			if (item1 != null) {
				playerInventory.addItem(item1);
			}
			if (item2 != null) {
				playerInventory.addItem(item2);
			}
			if (resultItem != null) {
				playerInventory.addItem(resultItem);
			}
		}
	}

	protected RegularAdminShopEditorHandler(SKRegularAdminShopkeeper shopkeeper) {
		super(SKDefaultUITypes.EDITOR(), shopkeeper, new TradingRecipesAdapter(shopkeeper));
	}

	@Override
	public SKRegularAdminShopkeeper getShopkeeper() {
		return (SKRegularAdminShopkeeper) super.getShopkeeper();
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		if (!super.canOpen(player, silent)) return false;
		return this.getShopkeeper().getType().hasPermission(player);
	}

	// The admin shop editor does not restrict the interactions with the trade slots. If we were to
	// insert non-empty placeholder items here, the editing player would be able to pick them up.
	@Override
	protected TradingRecipeDraft getEmptyTrade() {
		return TradingRecipeDraft.EMPTY;
	}

	@Override
	protected TradingRecipeDraft getEmptyTradeSlotItems() {
		return TradingRecipeDraft.EMPTY;
	}
}
