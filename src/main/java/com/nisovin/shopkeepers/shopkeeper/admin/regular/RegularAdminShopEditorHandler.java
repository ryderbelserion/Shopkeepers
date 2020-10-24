package com.nisovin.shopkeepers.shopkeeper.admin.regular;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradingOffer;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;

public class RegularAdminShopEditorHandler extends EditorHandler {

	protected RegularAdminShopEditorHandler(SKRegularAdminShopkeeper shopkeeper) {
		super(SKDefaultUITypes.EDITOR(), shopkeeper);
	}

	@Override
	public SKRegularAdminShopkeeper getShopkeeper() {
		return (SKRegularAdminShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean canOpen(Player player, boolean silent) {
		if (!super.canOpen(player, silent)) return false;
		return this.getShopkeeper().getType().hasPermission(player);
	}

	@Override
	protected List<TradingRecipeDraft> getTradingRecipes() {
		SKRegularAdminShopkeeper shopkeeper = this.getShopkeeper();
		List<TradingRecipeDraft> recipes = new ArrayList<>();

		// Add the shopkeeper's offers:
		for (TradingOffer offer : shopkeeper.getOffers()) {
			TradingRecipeDraft recipe = new TradingRecipeDraft(offer.getResultItem(), offer.getItem1(), offer.getItem2());
			recipes.add(recipe);
		}
		return recipes;
	}

	@Override
	protected void clearRecipes() {
		SKRegularAdminShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.clearOffers();
	}

	@Override
	protected void addRecipe(Player player, TradingRecipeDraft recipe) {
		assert recipe != null && recipe.isValid();
		SKRegularAdminShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.addOffer(ShopkeepersAPI.createTradingOffer(recipe.getResultItem(), recipe.getItem1(), recipe.getItem2()));
	}

	// TODO Remove this? Maybe handle the trades setup similar to the player trading shop: Copying the selected items
	// into the editor.
	@Override
	protected void handleInvalidRecipeDraft(Player player, TradingRecipeDraft recipe) {
		super.handleInvalidRecipeDraft(player, recipe);

		// Return unused items to inventory:
		ItemStack resultItem = recipe.getResultItem();
		ItemStack item1 = recipe.getItem1();
		ItemStack item2 = recipe.getItem2();
		PlayerInventory playerInventory = player.getInventory();

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
