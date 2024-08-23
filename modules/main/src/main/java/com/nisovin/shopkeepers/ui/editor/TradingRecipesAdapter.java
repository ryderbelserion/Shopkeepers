package com.nisovin.shopkeepers.ui.editor;

import java.util.List;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;

/**
 * Different types of merchants differ in how they internally represent their offers. This
 * {@link TradingRecipesAdapter} converts between the merchant's offers and the
 * {@link TradingRecipeDraft}s that are visualized and edited in the editor.
 * <p>
 * Instead of mapping the merchant's offers one by one to {@link TradingRecipeDraft}s,
 * {@link #getTradingRecipes()} returns the complete list of trading recipe drafts to initially show
 * in the editor. This list may also contain partially setup but still invalid trading recipe
 * drafts, for example to fill the editor with suggestions or examples for the editing player on
 * possible trading recipes.
 * <p>
 * Once the player exists the editor, {@link #updateTradingRecipes(Player, List)} is invoked with
 * the list of the potentially edited trading recipe drafts. This method is then responsible to
 * update the merchant's offers based on these trading recipe drafts, and to report back whether and
 * how many offers have changed.
 */
public interface TradingRecipesAdapter {

	/**
	 * Gets the list of {@link TradingRecipeDraft}s to show in the editor.
	 * <p>
	 * The returned list has to be modifiable: It is not copied, but edited directly.
	 * 
	 * @return the trading recipe drafts
	 */
	public List<TradingRecipeDraft> getTradingRecipes();

	/**
	 * Updates the merchant's offers based on the given {@link TradingRecipeDraft}s from the editor.
	 * <p>
	 * These trading recipe drafts may contain {@link TradingRecipeDraft#isValid() invalid} trading
	 * recipes.
	 * <p>
	 * Implementations of this method can assume that the given trading recipe drafts and their
	 * items are no longer used by the editor afterwards. Implementations are therefore allowed to
	 * reuse these objects.
	 * 
	 * @param player
	 *            the editing player, not <code>null</code>
	 * @param recipes
	 *            the trading recipe drafts from the editor, not <code>null</code>
	 * @return the (estimate) number of merchant offers that were changed, or <code>0</code> if no
	 *         offers changed
	 */
	public int updateTradingRecipes(Player player, List<? extends TradingRecipeDraft> recipes);
}
