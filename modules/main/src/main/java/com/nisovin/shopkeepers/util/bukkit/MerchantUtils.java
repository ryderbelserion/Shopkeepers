package com.nisovin.shopkeepers.util.bukkit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.checkerframework.checker.nullness.qual.NonNull;
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
		List<@NonNull MerchantRecipe> merchantRecipes = Unsafe.castNonNull(
				merchantInventory.getMerchant().getRecipes()
		);
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
		List<@NonNull ItemStack> ingredients = Unsafe.castNonNull(merchantRecipe.getIngredients());
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
		List<@NonNull ItemStack> ingredients = Unsafe.castNonNull(merchantRecipe.getIngredients());
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
		// CraftBukkit always fills both ingredients, possibly with empty ItemStacks. We do the same
		// in order to be able to more easily compare merchant recipes.
		// The items are already copied on various occasions (addIngredient, getIngredients, when
		// converting to a Minecraft recipe, etc.), so we do not need to copy them ourselves here.
		// The only exception to this is the result item: The MerchantRecipe does not copy it during
		// construction, nor during getResult. Copying the result item here ensures that we do not
		// accidentally encounter unmodifiable merchant recipe result items in contexts in which we
		// do not expect them.
		ItemStack resultItem = recipe.getResultItem().copy();
		ItemStack buyItem1 = recipe.getItem1().asItemStack();
		ItemStack buyItem2 = ItemUtils.getOrEmpty(ItemUtils.asItemStackOrNull(recipe.getItem2()));
		assert !ItemUtils.isEmpty(resultItem) && !ItemUtils.isEmpty(buyItem1);

		// No max-uses limit:
		MerchantRecipe merchantRecipe = new MerchantRecipe(resultItem, Integer.MAX_VALUE);
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
		Validate.notNull(recipe, "recipe is null");
		Validate.isTrue(recipe.isValid(), "recipe is not valid");

		UnmodifiableItemStack draftResultItem = Unsafe.assertNonNull(recipe.getResultItem());
		UnmodifiableItemStack draftItem1 = Unsafe.assertNonNull(recipe.getRecipeItem1());
		UnmodifiableItemStack draftItem2 = recipe.getRecipeItem2();

		// The items are already copied on various occasions (addIngredient, getIngredients, when
		// converting to a Minecraft recipe, etc.), so we do not need to copy them ourselves here.
		// The only exception to this is the result item: The MerchantRecipe does not copy it during
		// construction, nor during getResult. Copying the result item here ensures that we do not
		// accidentally encounter unmodifiable merchant recipe result items in contexts in which we
		// do not expect them.
		ItemStack resultItem = draftResultItem.copy();
		ItemStack buyItem1 = draftItem1.asItemStack();
		// CraftBukkit always fills both ingredients, possibly with empty ItemStacks. We do the same
		// in order to be able to more easily compare merchant recipes.
		ItemStack buyItem2 = ItemUtils.getOrEmpty(ItemUtils.asItemStackOrNull(draftItem2));
		assert !ItemUtils.isEmpty(resultItem) && !ItemUtils.isEmpty(buyItem1);

		// No max-uses limit
		MerchantRecipe merchantRecipe = new MerchantRecipe(resultItem, Integer.MAX_VALUE);
		merchantRecipe.setExperienceReward(false); // No experience rewards
		merchantRecipe.addIngredient(buyItem1);
		merchantRecipe.addIngredient(buyItem2);
		return merchantRecipe;
	}

	public static List<@NonNull TradingRecipeDraft> createTradingRecipeDrafts(
			List<? extends @NonNull MerchantRecipe> merchantRecipes
	) {
		List<@NonNull TradingRecipeDraft> tradingRecipeDrafts = new ArrayList<>(merchantRecipes.size());
		merchantRecipes.forEach(merchantRecipe -> {
			tradingRecipeDrafts.add(createTradingRecipeDraft(merchantRecipe));
		});
		return tradingRecipeDrafts;
	}

	public static List<@NonNull MerchantRecipe> createMerchantRecipes(
			List<? extends @NonNull TradingRecipe> recipes
	) {
		List<@NonNull MerchantRecipe> merchantRecipes = new ArrayList<>(recipes.size());
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
			if (!recipe1.getIngredients().equals(recipe2.getIngredients())) return false;
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
			if (!recipe1.getIngredients().equals(recipe2.getIngredients())) return false;
			return true;
		}
	};

	private MerchantUtils() {
	}
}
