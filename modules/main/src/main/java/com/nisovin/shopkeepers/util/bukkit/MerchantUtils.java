package com.nisovin.shopkeepers.util.bukkit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Utilities related to merchants and trading.
 */
public final class MerchantUtils {

	public static @Nullable TradingRecipe getActiveTradingRecipe(
			MerchantInventory merchantInventory
	) {
		MerchantRecipe merchantRecipe = merchantInventory.getSelectedRecipe();
		if (merchantRecipe == null) return null;
		return createTradingRecipe(merchantRecipe);
	}

	public static @Nullable TradingRecipe getSelectedTradingRecipe(
			MerchantInventory merchantInventory
	) {
		int selectedRecipeIndex = merchantInventory.getSelectedRecipeIndex();
		List<MerchantRecipe> merchantRecipes = merchantInventory.getMerchant().getRecipes();
		if (merchantRecipes.isEmpty()) {
			return null;
		}
		// The index is expected to be in valid bounds:
		MerchantRecipe merchantRecipe = merchantRecipes.get(selectedRecipeIndex);
		return createTradingRecipe(merchantRecipe);
	}

	public static TradingRecipe createTradingRecipe(MerchantRecipe merchantRecipe) {
		Validate.notNull(merchantRecipe, "merchantRecipe is null");
		// The returned ingredients are copies of the internal item stacks:
		List<ItemStack> ingredients = merchantRecipe.getIngredients();
		UnmodifiableItemStack item1 = UnmodifiableItemStack.ofNonNull(ingredients.get(0));
		UnmodifiableItemStack item2 = null;
		if (ingredients.size() > 1) {
			item2 = UnmodifiableItemStack.of(ItemUtils.getNullIfEmpty(ingredients.get(1)));
		}
		// The returned result item is not copied, so we copy it ourselves:
		UnmodifiableItemStack resultItem = UnmodifiableItemStack.ofNonNull(
				merchantRecipe.getResult().clone()
		);
		return new SKTradingRecipe(resultItem, item1, item2);
	}

	public static TradingRecipeDraft createTradingRecipeDraft(MerchantRecipe merchantRecipe) {
		Validate.notNull(merchantRecipe, "merchantRecipe is null");
		// The returned ingredients are copies of the internal item stacks:
		List<ItemStack> ingredients = merchantRecipe.getIngredients();
		UnmodifiableItemStack item1 = UnmodifiableItemStack.ofNonNull(ingredients.get(0));
		UnmodifiableItemStack item2 = null;
		if (ingredients.size() > 1) {
			item2 = UnmodifiableItemStack.of(ItemUtils.getNullIfEmpty(ingredients.get(1)));
		}
		// The returned result item is not copied, so we copy it ourselves:
		UnmodifiableItemStack resultItem = UnmodifiableItemStack.ofNonNull(
				merchantRecipe.getResult().clone()
		);
		return new TradingRecipeDraft(resultItem, item1, item2);
	}

	public static MerchantRecipe createMerchantRecipe(TradingRecipe recipe) {
		Validate.notNull(recipe, "recipe is null");
		MerchantRecipe merchantRecipe = createMerchantRecipe(
				recipe.getResultItem(),
				recipe.getItem1(),
				recipe.getItem2()
		);
		if (recipe.isOutOfStock()) {
			// If out of stock: Block the trade by setting 'max-uses' to 0. 'uses' is 0 by default.
			merchantRecipe.setMaxUses(0);
		}
		return merchantRecipe;
	}

	public static MerchantRecipe createMerchantRecipe(TradingRecipeDraft recipe) {
		Validate.notNull(recipe, "recipe is null");
		Validate.isTrue(recipe.isValid(), "recipe is not valid");
		return createMerchantRecipe(
				Unsafe.assertNonNull(recipe.getResultItem()),
				Unsafe.assertNonNull(recipe.getRecipeItem1()),
				recipe.getRecipeItem2()
		);
	}

	public static MerchantRecipe createMerchantRecipe(
			UnmodifiableItemStack resultItem,
			UnmodifiableItemStack buyItem1,
			@Nullable UnmodifiableItemStack buyItem2
	) {
		assert !ItemUtils.isEmpty(resultItem) && !ItemUtils.isEmpty(buyItem1);
		// The items are already copied on various occasions (addIngredient, getIngredients, when
		// converting to a Minecraft recipe, etc.), so we do not need to copy them ourselves here.
		// The only exception to this is the result item: The MerchantRecipe does not copy it during
		// construction, nor during getResult. Copying the result item here ensures that we do not
		// accidentally encounter unmodifiable merchant recipe result items in contexts in which we
		// do not expect them.
		// No max-uses limit
		MerchantRecipe merchantRecipe = new MerchantRecipe(resultItem.copy(), Integer.MAX_VALUE);
		merchantRecipe.setExperienceReward(false); // No experience rewards
		merchantRecipe.addIngredient(ItemUtils.asItemStack(buyItem1));
		if (buyItem2 != null) {
			merchantRecipe.addIngredient(ItemUtils.asItemStack(buyItem2));
		}
		return merchantRecipe;
	}

	public static List<TradingRecipeDraft> createTradingRecipeDrafts(
			List<? extends MerchantRecipe> merchantRecipes
	) {
		List<TradingRecipeDraft> tradingRecipeDrafts = new ArrayList<>(merchantRecipes.size());
		merchantRecipes.forEach(merchantRecipe -> {
			tradingRecipeDrafts.add(createTradingRecipeDraft(merchantRecipe));
		});
		return tradingRecipeDrafts;
	}

	public static List<MerchantRecipe> createMerchantRecipes(
			List<? extends TradingRecipe> recipes
	) {
		List<MerchantRecipe> merchantRecipes = new ArrayList<>(recipes.size());
		recipes.forEach(recipe -> {
			merchantRecipes.add(createMerchantRecipe(recipe));
		});
		return merchantRecipes;
	}

	public static abstract class MerchantRecipeComparator {

		public abstract boolean equals(
				@Nullable MerchantRecipe recipe1,
				@Nullable MerchantRecipe recipe2
		);

		public boolean equals(
				@Nullable List<? extends @Nullable MerchantRecipe> recipes1,
				@Nullable List<? extends @Nullable MerchantRecipe> recipes2
		) {
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
		public boolean equals(@Nullable MerchantRecipe recipe1, @Nullable MerchantRecipe recipe2) {
			if (recipe1 == recipe2) return true;
			if (recipe1 == null || recipe2 == null) return false;
			if (!recipe1.getResult().equals(recipe2.getResult())) return false;

			List<ItemStack> ingredients1 = recipe1.getIngredients();
			ingredients1.removeIf(ItemUtils::isEmpty);

			List<ItemStack> ingredients2 = recipe2.getIngredients();
			ingredients2.removeIf(ItemUtils::isEmpty);

			if (!ingredients1.equals(ingredients2)) return false;
			return true;
		}
	};

	// Does not compare the exact amounts of uses and max-uses, but the 'is blocked' state has to
	// match.
	public static final MerchantRecipeComparator MERCHANT_RECIPES_IGNORE_USES_EXCEPT_BLOCKED = new MerchantRecipeComparator() {
		@Override
		public boolean equals(@Nullable MerchantRecipe recipe1, @Nullable MerchantRecipe recipe2) {
			if (recipe1 == recipe2) return true;
			if (recipe1 == null || recipe2 == null) return false;

			boolean isBlocked1 = (recipe1.getUses() >= recipe1.getMaxUses());
			boolean isBlocked2 = (recipe2.getUses() >= recipe2.getMaxUses());
			if (isBlocked1 != isBlocked2) return false;
			if (recipe1.hasExperienceReward() != recipe2.hasExperienceReward()) return false;
			if (recipe1.getPriceMultiplier() != recipe2.getPriceMultiplier()) return false;
			if (recipe1.getVillagerExperience() != recipe2.getVillagerExperience()) return false;
			if (!recipe1.getResult().equals(recipe2.getResult())) return false;

			List<ItemStack> ingredients1 = recipe1.getIngredients();
			ingredients1.removeIf(ItemUtils::isEmpty);

			List<ItemStack> ingredients2 = recipe2.getIngredients();
			ingredients2.removeIf(ItemUtils::isEmpty);

			if (!ingredients1.equals(ingredients2)) return false;

			return true;
		}
	};

	private MerchantUtils() {
	}
}
