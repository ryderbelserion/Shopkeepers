package com.nisovin.shopkeepers.ui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default abstract skeleton implementation of a {@link TradingRecipesAdapter}.
 * <p>
 * By default, we reuse the merchant's previous offers and only replace those that changed. The
 * previous offers and the new offers derived from the trading recipe drafts are matched by their
 * index.
 *
 * @param <O>
 *            the type of offer the merchant uses to represent its trading recipes
 */
public abstract class DefaultTradingRecipesAdapter<@NonNull O> implements TradingRecipesAdapter {

	protected DefaultTradingRecipesAdapter() {
	}

	/**
	 * See {@link TradingRecipesAdapter#getTradingRecipes()}.
	 * <p>
	 * The order of these trading recipe drafts is expected to match the order of the corresponding
	 * offers returned by {@link #getOffers()}.
	 */
	@Override
	public abstract List<TradingRecipeDraft> getTradingRecipes();

	// TODO Maybe keep track of which TradingRecipeDrafts have actually been edited (i.e. clicked at
	// in the editor or marked as 'dirty' by some other mean), and then avoid creating offers and
	// comparing items for them.
	// TODO When creating new offers, we can reuse the items of the trading recipe draft. Currently,
	// these items are copied most of the time.
	/**
	 * See {@link TradingRecipesAdapter#updateTradingRecipes(Player, List)}.
	 * <p>
	 * Any encountered {@link TradingRecipeDraft#isValid() invalid} trading recipe drafts are passed
	 * to {@link #handleInvalidTradingRecipe(Player, TradingRecipeDraft)} for further processing and
	 * are then ignored.
	 */
	@Override
	public int updateTradingRecipes(Player player, List<? extends TradingRecipeDraft> recipes) {
		Validate.notNull(player, "player is null");
		Validate.notNull(recipes, "recipes is null");
		assert this.getOffers() != null && !CollectionUtils.containsNull(this.getOffers());

		List<@Nullable O> newOffers = new ArrayList<>(this.getOffers());
		final int oldOffersSize = newOffers.size();
		int changedOffers = 0;
		boolean clearedAtLeastOneOffer = false;
		for (int index = 0; index < recipes.size(); index++) {
			TradingRecipeDraft recipe = recipes.get(index);
			Validate.notNull(recipe, "recipes contains null");
			// Null if invalid:
			// The recipe is also considered invalid if the created offer is null.
			@Nullable O newOffer = recipe.isValid() ? this.createOffer(recipe) : null;
			if (newOffer == null) {
				this.handleInvalidTradingRecipe(player, recipe);
				if (index < oldOffersSize) {
					newOffers.set(index, null); // Mark as cleared
					clearedAtLeastOneOffer = true;
					changedOffers += 1;
				}
			} else {
				if (index < oldOffersSize) {
					// Only replace the old offer if it has actually changed:
					@NonNull O oldOffer = Unsafe.assertNonNull(newOffers.get(index));
					if (!this.areOffersEqual(oldOffer, newOffer)) {
						newOffers.set(index, newOffer);
						changedOffers += 1;
					} // Else: Keep the old offer.
				} else {
					newOffers.add(newOffer);
					changedOffers += 1;
				}
			}
		}

		if (changedOffers > 0) {
			// Remove null markers:
			if (clearedAtLeastOneOffer) {
				newOffers.removeIf(Objects::isNull);
			}

			// Apply the new offers:
			this.setOffers(Unsafe.cast(newOffers));
		}

		return changedOffers;
	}

	/**
	 * Gets the merchant's current offers.
	 * <p>
	 * The order of these offers is expected to match the order of the corresponding trading recipe
	 * drafts returned by {@link #getTradingRecipes()}.
	 * 
	 * @return the current offers
	 */
	protected abstract List<? extends @NonNull O> getOffers();

	/**
	 * Updates the merchant's offers to the given list of offers.
	 * 
	 * @param newOffers
	 *            the new offers
	 */
	protected abstract void setOffers(List<? extends @NonNull O> newOffers);

	/**
	 * Creates a new offer for the given {@link TradingRecipeDraft}.
	 * <p>
	 * The given trading recipe draft is {@link TradingRecipeDraft#isValid() valid}. However, this
	 * method may implement additional verifications and return <code>null</code> if it considers
	 * the trading recipe draft to be invalid according to these checks: If this is the case, no
	 * offer is added to the merchant for the given trading recipe draft, any previous offer is
	 * removed, and {@link #handleInvalidTradingRecipe(Player, TradingRecipeDraft)} is invoked.
	 * <p>
	 * Implementations of this method can assume that the given trading recipe draft and its items
	 * are no longer used by the editor afterwards. Implementations are therefore allowed to reuse
	 * these objects.
	 * 
	 * @param recipe
	 *            the trading recipe draft, not <code>null</code> or
	 *            {@link TradingRecipeDraft#isValid() invalid}
	 * @return the offer, or <code>null</code> to consider the trading recipe draft as invalid
	 */
	protected abstract @Nullable O createOffer(TradingRecipeDraft recipe);

	/**
	 * Checks whether the given old and new offers are considered equivalent.
	 * <p>
	 * This method is called when the new offers {@link #createOffer(TradingRecipeDraft) created}
	 * based on the trading recipe drafts from the editor are compared with the merchant's previous
	 * offers. This method serves two purposes:
	 * <ul>
	 * <li>It is used to detect whether the offers have changed.
	 * <li>It allows to decide which of these two offers shall be further used by the merchant: If
	 * the given offers are considered equal by this method, the previous offer is reused when
	 * building the list of new offers for the merchant. Otherwise, the new offer replaces the
	 * previous one.
	 * </ul>
	 * <p>
	 * By default, this method compares the offers based on {@link Object#equals(Object)}. However,
	 * this method may be overridden if there are cases in which the offers may not be fully equal,
	 * but equal enough that the old offer is meant to be reused nevertheless and not be replaced by
	 * the new offer. This situation can for example arise if the editor does not represent all
	 * aspects of the merchant's previous offers.
	 * 
	 * @param oldOffer
	 *            the previous offer, not <code>null</code>
	 * @param newOffer
	 *            the new offer derived from a trading recipe draft, not <code>null</code>
	 * @return <code>true</code> if the offers are considered equal and the old offer shall not be
	 *         replaced
	 */
	protected boolean areOffersEqual(@NonNull O oldOffer, @NonNull O newOffer) {
		return oldOffer.equals(newOffer);
	}

	/**
	 * When the trading recipes from the editor are applied to the merchant, this is called for
	 * every {@link TradingRecipeDraft#isValid() invalid} trading recipe draft, as well as for every
	 * trading recipe draft that is considered invalid by {@link #createOffer(TradingRecipeDraft)}.
	 * 
	 * @param player
	 *            the editing player
	 * @param invalidRecipe
	 *            the invalid trading recipe draft
	 */
	protected void handleInvalidTradingRecipe(Player player, TradingRecipeDraft invalidRecipe) {
	}
}
