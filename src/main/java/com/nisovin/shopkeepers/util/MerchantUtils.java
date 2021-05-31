package com.nisovin.shopkeepers.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;

/**
 * Utilities related to merchants and trading.
 */
public class MerchantUtils {

	private MerchantUtils() {
	}

	public static SKTradingRecipe getSelectedTradingRecipe(MerchantInventory merchantInventory) {
		MerchantRecipe merchantRecipe = merchantInventory.getSelectedRecipe();
		return createTradingRecipe(merchantRecipe);
	}

	public static SKTradingRecipe createTradingRecipe(MerchantRecipe merchantRecipe) {
		if (merchantRecipe == null) return null;
		List<ItemStack> ingredients = merchantRecipe.getIngredients();
		ItemStack item1 = ingredients.get(0);
		ItemStack item2 = null;
		if (ingredients.size() > 1) {
			ItemStack buyItem2 = ingredients.get(1);
			if (!ItemUtils.isEmpty(buyItem2)) {
				item2 = buyItem2;
			}
		}
		ItemStack resultItem = merchantRecipe.getResult();
		return new SKTradingRecipe(resultItem, item1, item2);
	}

	public static TradingRecipeDraft createTradingRecipeDraft(MerchantRecipe merchantRecipe) {
		if (merchantRecipe == null) return null;
		List<ItemStack> ingredients = merchantRecipe.getIngredients();
		ItemStack item1 = ingredients.get(0);
		ItemStack item2 = null;
		if (ingredients.size() > 1) {
			item2 = ItemUtils.getNullIfEmpty(ingredients.get(1));
		}
		ItemStack resultItem = merchantRecipe.getResult();
		return new TradingRecipeDraft(resultItem, item1, item2);
	}

	public static MerchantRecipe createMerchantRecipe(TradingRecipe recipe) {
		if (recipe == null) return null;
		// CraftBukkit always fills both ingredients, possibly with empty ItemStacks. We do the same in order to be able
		// to more easily compare merchant recipes.
		ItemStack buyItem1 = recipe.getItem1();
		ItemStack buyItem2 = ItemUtils.getOrEmpty(recipe.getItem2());
		ItemStack resultItem = recipe.getResultItem();
		assert !ItemUtils.isEmpty(resultItem) && !ItemUtils.isEmpty(buyItem1);

		MerchantRecipe merchantRecipe = new MerchantRecipe(resultItem, Integer.MAX_VALUE); // No max-uses limit
		if (recipe.isOutOfStock()) {
			// Except if out of stock:
			// 'uses' is 0 by default as well, so the trade shows as blocked.
			merchantRecipe.setMaxUses(0);
		}
		merchantRecipe.setExperienceReward(false); // No experience rewards
		merchantRecipe.addIngredient(buyItem1);
		merchantRecipe.addIngredient(buyItem2);
		return merchantRecipe;
	}

	public static MerchantRecipe createMerchantRecipe(TradingRecipeDraft recipe) {
		if (recipe == null) return null;
		assert recipe.isValid();
		// CraftBukkit always fills both ingredients, possibly with empty ItemStacks. We do the same in order to be able
		// to more easily compare merchant recipes.
		ItemStack buyItem1 = ItemUtils.getOrEmpty(recipe.getItem1());
		ItemStack buyItem2 = ItemUtils.getOrEmpty(recipe.getItem2());
		ItemStack resultItem = recipe.getResultItem();
		assert !ItemUtils.isEmpty(resultItem) && !ItemUtils.isEmpty(buyItem1);

		MerchantRecipe merchantRecipe = new MerchantRecipe(resultItem, Integer.MAX_VALUE); // No max-uses limit
		merchantRecipe.setExperienceReward(false); // No experience rewards
		merchantRecipe.addIngredient(buyItem1);
		merchantRecipe.addIngredient(buyItem2);
		return merchantRecipe;
	}

	public static List<TradingRecipeDraft> createTradingRecipeDrafts(List<MerchantRecipe> merchantRecipes) {
		List<TradingRecipeDraft> tradingRecipeDrafts = new ArrayList<>(merchantRecipes.size());
		merchantRecipes.forEach(merchantRecipe -> {
			tradingRecipeDrafts.add(createTradingRecipeDraft(merchantRecipe));
		});
		return tradingRecipeDrafts;
	}

	public static List<MerchantRecipe> createMerchantRecipes(List<? extends TradingRecipe> recipes) {
		List<MerchantRecipe> merchantRecipes = new ArrayList<>(recipes.size());
		recipes.forEach(recipe -> {
			merchantRecipes.add(createMerchantRecipe(recipe));
		});
		return merchantRecipes;
	}

	public static abstract class MerchantRecipeComparator {

		public abstract boolean equals(MerchantRecipe recipe1, MerchantRecipe recipe2);

		public boolean equals(List<MerchantRecipe> recipes1, List<MerchantRecipe> recipes2) {
			if (recipes1 == recipes2) return true;
			if (recipes1 == null || recipes2 == null) return false;
			if (recipes1.size() != recipes2.size()) return false;
			for (int i = 0; i < recipes1.size(); ++i) {
				MerchantRecipe recipe1 = recipes1.get(i);
				MerchantRecipe recipe2 = recipes2.get(i);
				if (!this.equals(recipe1, recipe2)) {
					return false;
				}
			}
			return true;
		}
	}

	public static final MerchantRecipeComparator MERCHANT_RECIPES_EQUAL_ITEMS = new MerchantRecipeComparator() {
		@Override
		public boolean equals(MerchantRecipe recipe1, MerchantRecipe recipe2) {
			if (recipe1 == recipe2) return true;
			if (recipe1 == null || recipe2 == null) return false;
			if (!recipe1.getResult().equals(recipe2.getResult())) return false;
			if (!recipe1.getIngredients().equals(recipe2.getIngredients())) return false;
			return true;
		}
	};

	// Does not compare the exact amounts of uses and max-uses, but the 'is blocked' state has to match.
	public static final MerchantRecipeComparator MERCHANT_RECIPES_IGNORE_USES_EXCEPT_BLOCKED = new MerchantRecipeComparator() {
		@Override
		public boolean equals(MerchantRecipe recipe1, MerchantRecipe recipe2) {
			if (recipe1 == recipe2) return true;
			if (recipe1 == null || recipe2 == null) return false;

			boolean isBlocked1 = (recipe1.getUses() >= recipe1.getMaxUses());
			boolean isBlocked2 = (recipe2.getUses() >= recipe2.getMaxUses());
			if (isBlocked1 != isBlocked2) return false;
			if (recipe1.hasExperienceReward() != recipe2.hasExperienceReward()) return false;
			if (recipe1.getPriceMultiplier() != recipe2.getPriceMultiplier()) return false;
			if (recipe1.getVillagerExperience() != recipe2.getVillagerExperience()) return false;

			if (!recipe1.getResult().equals(recipe2.getResult())) return false;
			if (!recipe1.getIngredients().equals(recipe2.getIngredients())) return false;
			return true;
		}
	};
}
